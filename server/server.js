"use strict";

const http = require("http");
const crypto = require("crypto");
const { WebSocketServer } = require("ws");

const PORT = process.env.PORT || 8080;
const MAX_ROOMS = Number(process.env.MAX_ROOMS || 500);
const MAX_PLAYERS_PER_ROOM = 8;
const HEARTBEAT_INTERVAL = 25000;
const ROOM_IDLE_TIMEOUT = 1000 * 60 * 30;

const rooms = new Map();
const peers = new Map();

function makeId(bytes) {
    return crypto.randomBytes(bytes).toString("hex");
}

function hashPassword(value) {
    if (!value) {
        return "";
    }
    return crypto.createHash("sha256").update(String(value)).digest("hex");
}

function sanitise(value, maxLength) {
    if (typeof value !== "string") {
        return "";
    }
    return value.replace(/[\u0000-\u001f\u007f]/g, "").trim().slice(0, maxLength);
}

function send(socket, payload) {
    if (socket.readyState !== socket.OPEN) {
        return;
    }
    try {
        socket.send(JSON.stringify(payload));
    } catch (error) {
        // ignore write failures, the close handler will clean up
    }
}

function publicRoom(room) {
    return {
        id: room.id,
        name: room.name,
        hostName: room.hostName,
        scenario: room.scenario,
        scenarioName: room.scenarioName,
        players: room.members.length,
        maxPlayers: room.maxPlayers,
        locked: room.passwordHash !== "",
        difficulty: room.difficulty,
        aggression: room.aggression,
        started: room.started
    };
}

function roomList(query) {
    const needle = (query || "").toLowerCase();
    const results = [];
    for (const room of rooms.values()) {
        if (room.started) {
            continue;
        }
        if (needle && !room.name.toLowerCase().includes(needle)
            && !room.hostName.toLowerCase().includes(needle)
            && room.id !== needle) {
            continue;
        }
        results.push(publicRoom(room));
    }
    results.sort((first, second) => second.players - first.players);
    return results.slice(0, 60);
}

function broadcastRoom(room, payload, exceptPeerId) {
    for (const memberId of room.members) {
        if (memberId === exceptPeerId) {
            continue;
        }
        const member = peers.get(memberId);
        if (member) {
            send(member.socket, payload);
        }
    }
}

function memberSummaries(room) {
    return room.members.map((memberId) => {
        const member = peers.get(memberId);
        return {
            peerId: memberId,
            name: member ? member.name : "unknown",
            host: room.hostPeerId === memberId
        };
    });
}

function leaveRoom(peer, notify) {
    if (!peer.roomId) {
        return;
    }
    const room = rooms.get(peer.roomId);
    peer.roomId = null;
    if (!room) {
        return;
    }

    room.members = room.members.filter((memberId) => memberId !== peer.id);
    room.updatedAt = Date.now();

    if (room.hostPeerId === peer.id || room.members.length === 0) {
        for (const memberId of room.members) {
            const member = peers.get(memberId);
            if (member) {
                member.roomId = null;
                send(member.socket, { t: "roomClosed", reason: "host_left" });
            }
        }
        rooms.delete(room.id);
        return;
    }

    if (notify) {
        broadcastRoom(room, { t: "peerLeft", peerId: peer.id, members: memberSummaries(room) });
    }
}

function handleCreate(peer, message) {
    if (rooms.size >= MAX_ROOMS) {
        send(peer.socket, { t: "error", code: "server_full" });
        return;
    }

    leaveRoom(peer, true);

    const name = sanitise(message.name, 40) || (peer.name + "'s game");
    const maxPlayers = Math.max(2, Math.min(MAX_PLAYERS_PER_ROOM, Number(message.maxPlayers) || 4));

    const room = {
        id: makeId(3).toUpperCase(),
        name,
        hostPeerId: peer.id,
        hostName: peer.name,
        passwordHash: hashPassword(sanitise(message.password, 64)),
        scenario: sanitise(message.scenario, 64),
        scenarioName: sanitise(message.scenarioName, 80),
        maxPlayers,
        difficulty: Math.max(0, Math.min(3, Number(message.difficulty) || 0)),
        aggression: Math.max(1, Math.min(100, Number(message.aggression) || 50)),
        members: [peer.id],
        started: false,
        createdAt: Date.now(),
        updatedAt: Date.now()
    };

    rooms.set(room.id, room);
    peer.roomId = room.id;

    send(peer.socket, { t: "created", room: publicRoom(room), members: memberSummaries(room) });
}

function handleJoin(peer, message) {
    const roomId = sanitise(message.roomId, 16).toUpperCase();
    const room = rooms.get(roomId);

    if (!room) {
        send(peer.socket, { t: "error", code: "room_not_found" });
        return;
    }
    if (room.started) {
        send(peer.socket, { t: "error", code: "already_started" });
        return;
    }
    if (room.members.length >= room.maxPlayers) {
        send(peer.socket, { t: "error", code: "room_full" });
        return;
    }
    if (room.passwordHash && room.passwordHash !== hashPassword(sanitise(message.password, 64))) {
        send(peer.socket, { t: "error", code: "wrong_password" });
        return;
    }

    leaveRoom(peer, true);

    room.members.push(peer.id);
    room.updatedAt = Date.now();
    peer.roomId = room.id;

    send(peer.socket, { t: "joined", room: publicRoom(room), members: memberSummaries(room) });
    broadcastRoom(room, {
        t: "peerJoined",
        peer: { peerId: peer.id, name: peer.name, host: false },
        members: memberSummaries(room)
    }, peer.id);
}

function handleUpdate(peer, message) {
    const room = peer.roomId ? rooms.get(peer.roomId) : null;
    if (!room || room.hostPeerId !== peer.id) {
        return;
    }
    if (message.scenario !== undefined) {
        room.scenario = sanitise(message.scenario, 64);
    }
    if (message.scenarioName !== undefined) {
        room.scenarioName = sanitise(message.scenarioName, 80);
    }
    if (message.difficulty !== undefined) {
        room.difficulty = Math.max(0, Math.min(3, Number(message.difficulty) || 0));
    }
    if (message.aggression !== undefined) {
        room.aggression = Math.max(1, Math.min(100, Number(message.aggression) || 50));
    }
    if (message.started !== undefined) {
        room.started = Boolean(message.started);
    }
    room.updatedAt = Date.now();
    broadcastRoom(room, { t: "roomUpdated", room: publicRoom(room), members: memberSummaries(room) });
}

function handleRelay(peer, message) {
    const room = peer.roomId ? rooms.get(peer.roomId) : null;
    if (!room) {
        return;
    }
    if (typeof message.data !== "string" || message.data.length > 2000000) {
        return;
    }
    room.updatedAt = Date.now();

    const envelope = { t: "relay", from: peer.id, data: message.data };

    if (message.to && message.to !== "*") {
        if (!room.members.includes(message.to)) {
            return;
        }
        const target = peers.get(message.to);
        if (target) {
            send(target.socket, envelope);
        }
        return;
    }

    if (message.toHost) {
        const host = peers.get(room.hostPeerId);
        if (host) {
            send(host.socket, envelope);
        }
        return;
    }

    broadcastRoom(room, envelope, peer.id);
}

const server = http.createServer((request, response) => {
    if (request.url === "/health") {
        response.writeHead(200, { "Content-Type": "application/json" });
        response.end(JSON.stringify({ status: "ok", rooms: rooms.size, peers: peers.size }));
        return;
    }
    response.writeHead(200, { "Content-Type": "text/plain" });
    response.end("Age Of History Of Conquest lobby server");
});

const wss = new WebSocketServer({ server, maxPayload: 4 * 1024 * 1024 });

wss.on("connection", (socket) => {
    const peer = {
        id: makeId(8),
        name: "player",
        socket,
        roomId: null,
        alive: true
    };
    peers.set(peer.id, peer);

    socket.on("pong", () => {
        peer.alive = true;
    });

    socket.on("message", (raw) => {
        let message;
        try {
            message = JSON.parse(raw.toString());
        } catch (error) {
            return;
        }
        if (!message || typeof message.t !== "string") {
            return;
        }

        switch (message.t) {
            case "hello":
                peer.name = sanitise(message.name, 24) || "player";
                send(socket, { t: "welcome", peerId: peer.id });
                break;
            case "list":
                send(socket, { t: "rooms", rooms: roomList(sanitise(message.query, 40)) });
                break;
            case "create":
                handleCreate(peer, message);
                break;
            case "join":
                handleJoin(peer, message);
                break;
            case "update":
                handleUpdate(peer, message);
                break;
            case "leave":
                leaveRoom(peer, true);
                send(socket, { t: "left" });
                break;
            case "relay":
                handleRelay(peer, message);
                break;
            default:
                break;
        }
    });

    socket.on("close", () => {
        leaveRoom(peer, true);
        peers.delete(peer.id);
    });

    socket.on("error", () => {
        leaveRoom(peer, true);
        peers.delete(peer.id);
    });
});

setInterval(() => {
    for (const peer of peers.values()) {
        if (!peer.alive) {
            peer.socket.terminate();
            continue;
        }
        peer.alive = false;
        try {
            peer.socket.ping();
        } catch (error) {
            // ignore
        }
    }

    const cutoff = Date.now() - ROOM_IDLE_TIMEOUT;
    for (const room of [...rooms.values()]) {
        if (room.updatedAt < cutoff) {
            rooms.delete(room.id);
        }
    }
}, HEARTBEAT_INTERVAL);

server.listen(PORT, () => {
    console.log("Lobby server listening on port " + PORT);
});

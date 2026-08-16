import { getStore } from "@netlify/blobs";

const ROOM_PREFIX = "room/";
const MESSAGE_PREFIX = "msg/";
const MAX_PLAYERS = 8;
const ROOM_IDLE_MILLISECONDS = 1000 * 60 * 30;
const MESSAGE_LIFETIME_MILLISECONDS = 1000 * 60 * 5;

function store() {
  return getStore({ name: "aohc-lobby", consistency: "strong" });
}

function json(body, status = 200) {
  return new Response(JSON.stringify(body), {
    status,
    headers: { "Content-Type": "application/json" }
  });
}

function clean(value, maxLength) {
  if (typeof value !== "string") {
    return "";
  }
  return value.replace(/[\u0000-\u001f\u007f]/g, "").trim().slice(0, maxLength);
}

async function hash(value) {
  if (!value) {
    return "";
  }
  const data = new TextEncoder().encode(String(value));
  const digest = await crypto.subtle.digest("SHA-256", data);
  return Array.from(new Uint8Array(digest))
    .map((byte) => byte.toString(16).padStart(2, "0"))
    .join("");
}

function makeRoomId() {
  const alphabet = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
  const bytes = crypto.getRandomValues(new Uint8Array(6));
  let result = "";
  for (const byte of bytes) {
    result += alphabet[byte % alphabet.length];
  }
  return result;
}

function makePeerId() {
  const bytes = crypto.getRandomValues(new Uint8Array(8));
  return Array.from(bytes).map((byte) => byte.toString(16).padStart(2, "0")).join("");
}

function messageKey(roomId) {
  const stamp = String(Date.now()).padStart(14, "0");
  const salt = Math.floor(Math.random() * 1000000).toString().padStart(6, "0");
  return `${MESSAGE_PREFIX}${roomId}/${stamp}-${salt}`;
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
    locked: Boolean(room.passwordHash),
    difficulty: room.difficulty,
    aggression: room.aggression,
    started: Boolean(room.started)
  };
}

async function readRoom(blobs, roomId) {
  if (!roomId) {
    return null;
  }
  return await blobs.get(ROOM_PREFIX + roomId, { type: "json" });
}

async function writeRoom(blobs, room) {
  room.updatedAt = Date.now();
  await blobs.setJSON(ROOM_PREFIX + room.id, room);
}

async function handleList(blobs, body) {
  const query = clean(body.query, 40).toLowerCase();
  const { blobs: entries } = await blobs.list({ prefix: ROOM_PREFIX });
  const now = Date.now();
  const rooms = [];

  for (const entry of entries) {
    const room = await blobs.get(entry.key, { type: "json" });
    if (!room) {
      continue;
    }
    if (now - (room.updatedAt || 0) > ROOM_IDLE_MILLISECONDS) {
      await blobs.delete(entry.key);
      continue;
    }
    if (room.started) {
      continue;
    }
    if (query
      && !room.name.toLowerCase().includes(query)
      && !room.hostName.toLowerCase().includes(query)
      && room.id.toLowerCase() !== query) {
      continue;
    }
    rooms.push(publicRoom(room));
  }

  rooms.sort((first, second) => second.players - first.players);
  return json({ t: "rooms", rooms: rooms.slice(0, 60) });
}

async function handleCreate(blobs, body) {
  const peerId = makePeerId();
  const hostName = clean(body.name, 24) || "player";
  const room = {
    id: makeRoomId(),
    name: clean(body.roomName, 40) || `${hostName} room`,
    hostPeerId: peerId,
    hostName,
    passwordHash: await hash(clean(body.password, 64)),
    scenario: clean(body.scenario, 64),
    scenarioName: clean(body.scenarioName, 80),
    maxPlayers: Math.max(2, Math.min(MAX_PLAYERS, Number(body.maxPlayers) || 4)),
    difficulty: Math.max(0, Math.min(3, Number(body.difficulty) || 0)),
    aggression: Math.max(1, Math.min(100, Number(body.aggression) || 50)),
    members: [{ peerId, name: hostName, host: true }],
    started: false,
    createdAt: Date.now()
  };

  await writeRoom(blobs, room);
  return json({ t: "created", peerId, room: publicRoom(room), members: room.members });
}

async function handleJoin(blobs, body) {
  const roomId = clean(body.roomId, 16).toUpperCase();
  const room = await readRoom(blobs, roomId);

  if (!room) {
    return json({ t: "error", code: "room_not_found" }, 404);
  }
  if (room.started) {
    return json({ t: "error", code: "already_started" }, 409);
  }
  if (room.members.length >= room.maxPlayers) {
    return json({ t: "error", code: "room_full" }, 409);
  }
  if (room.passwordHash && room.passwordHash !== await hash(clean(body.password, 64))) {
    return json({ t: "error", code: "wrong_password" }, 403);
  }

  const peerId = makePeerId();
  const name = clean(body.name, 24) || "player";
  room.members.push({ peerId, name, host: false });
  await writeRoom(blobs, room);

  return json({ t: "joined", peerId, room: publicRoom(room), members: room.members });
}

async function handleUpdate(blobs, body) {
  const room = await readRoom(blobs, clean(body.roomId, 16).toUpperCase());
  if (!room) {
    return json({ t: "error", code: "room_not_found" }, 404);
  }
  if (room.hostPeerId !== clean(body.peerId, 32)) {
    return json({ t: "error", code: "not_host" }, 403);
  }

  if (body.scenario !== undefined) {
    room.scenario = clean(body.scenario, 64);
  }
  if (body.scenarioName !== undefined) {
    room.scenarioName = clean(body.scenarioName, 80);
  }
  if (body.difficulty !== undefined) {
    room.difficulty = Math.max(0, Math.min(3, Number(body.difficulty) || 0));
  }
  if (body.aggression !== undefined) {
    room.aggression = Math.max(1, Math.min(100, Number(body.aggression) || 50));
  }
  if (body.started !== undefined) {
    room.started = Boolean(body.started);
  }

  await writeRoom(blobs, room);
  return json({ t: "roomUpdated", room: publicRoom(room), members: room.members });
}

async function handleLeave(blobs, body) {
  const roomId = clean(body.roomId, 16).toUpperCase();
  const peerId = clean(body.peerId, 32);
  const room = await readRoom(blobs, roomId);
  if (!room) {
    return json({ t: "left" });
  }

  if (room.hostPeerId === peerId) {
    await blobs.delete(ROOM_PREFIX + room.id);
    return json({ t: "left" });
  }

  room.members = room.members.filter((member) => member.peerId !== peerId);
  await writeRoom(blobs, room);
  return json({ t: "left" });
}

async function handleSend(blobs, body) {
  const roomId = clean(body.roomId, 16).toUpperCase();
  const peerId = clean(body.peerId, 32);
  const room = await readRoom(blobs, roomId);
  if (!room) {
    return json({ t: "error", code: "room_not_found" }, 404);
  }
  if (typeof body.data !== "string" || body.data.length > 3000000) {
    return json({ t: "error", code: "bad_payload" }, 400);
  }

  const target = body.toHost ? room.hostPeerId : clean(body.to, 32);
  await blobs.setJSON(messageKey(roomId), {
    from: peerId,
    to: target || "",
    data: body.data,
    at: Date.now()
  });

  room.updatedAt = Date.now();
  await blobs.setJSON(ROOM_PREFIX + room.id, room);

  return json({ t: "sent" });
}

async function handlePoll(blobs, body) {
  const roomId = clean(body.roomId, 16).toUpperCase();
  const peerId = clean(body.peerId, 32);
  const since = clean(body.since, 40);

  const room = await readRoom(blobs, roomId);
  if (!room) {
    return json({ t: "error", code: "room_not_found" }, 404);
  }

  const prefix = `${MESSAGE_PREFIX}${roomId}/`;
  const { blobs: entries } = await blobs.list({ prefix });
  entries.sort((first, second) => first.key.localeCompare(second.key));

  const now = Date.now();
  const messages = [];
  let cursor = since;

  for (const entry of entries) {
    const suffix = entry.key.slice(prefix.length);
    const stamp = Number(suffix.split("-")[0]);

    if (now - stamp > MESSAGE_LIFETIME_MILLISECONDS) {
      await blobs.delete(entry.key);
      continue;
    }
    if (since && suffix <= since) {
      continue;
    }

    const message = await blobs.get(entry.key, { type: "json" });
    if (!message) {
      continue;
    }

    cursor = suffix;

    if (message.from === peerId) {
      continue;
    }
    if (message.to && message.to !== peerId) {
      continue;
    }
    messages.push({ from: message.from, data: message.data });
  }

  return json({
    t: "poll",
    cursor,
    room: publicRoom(room),
    members: room.members,
    messages
  });
}

export default async (request) => {
  const url = new URL(request.url);

  if (url.pathname.endsWith("/health")) {
    const blobs = store();
    const { blobs: entries } = await blobs.list({ prefix: ROOM_PREFIX });
    return json({ status: "ok", rooms: entries.length });
  }

  if (request.method !== "POST") {
    return json({ t: "error", code: "method_not_allowed" }, 405);
  }

  let body;
  try {
    body = await request.json();
  } catch (error) {
    return json({ t: "error", code: "bad_json" }, 400);
  }

  const blobs = store();
  const action = clean(body.action, 16);

  try {
    switch (action) {
      case "list":
        return await handleList(blobs, body);
      case "create":
        return await handleCreate(blobs, body);
      case "join":
        return await handleJoin(blobs, body);
      case "update":
        return await handleUpdate(blobs, body);
      case "leave":
        return await handleLeave(blobs, body);
      case "send":
        return await handleSend(blobs, body);
      case "poll":
        return await handlePoll(blobs, body);
      default:
        return json({ t: "error", code: "unknown_action" }, 400);
    }
  } catch (error) {
    return json({ t: "error", code: "server_error", detail: String(error) }, 500);
  }
};

export const config = {
  path: ["/api/lobby", "/api/lobby/health"]
};

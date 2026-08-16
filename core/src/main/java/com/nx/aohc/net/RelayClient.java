package com.nx.aohc.net;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.JsonReader;
import com.badlogic.gdx.utils.JsonValue;
import com.badlogic.gdx.utils.JsonWriter;
import com.badlogic.gdx.utils.Json;

import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;

import java.net.URI;
import java.util.concurrent.ConcurrentLinkedQueue;

public class RelayClient {

    public static class RoomInfo {
        public String id = "";
        public String name = "";
        public String hostName = "";
        public String scenario = "";
        public String scenarioName = "";
        public int players;
        public int maxPlayers;
        public boolean locked;
        public int difficulty;
        public int aggression;
        public boolean started;
    }

    public static class PeerInfo {
        public String peerId = "";
        public String name = "";
        public boolean host;
    }

    public interface Listener {
        void onConnected(String peerId);

        void onRoomList(Array<RoomInfo> rooms);

        void onRoomEntered(RoomInfo room, Array<PeerInfo> members, boolean asHost);

        void onRoomUpdated(RoomInfo room, Array<PeerInfo> members);

        void onPeerJoined(PeerInfo peer, Array<PeerInfo> members);

        void onPeerLeft(String peerId, Array<PeerInfo> members);

        void onRelay(String fromPeerId, String data);

        void onServerError(String code);

        void onClosed(String reason);
    }

    private final String serverUrl;
    private final String playerName;
    private final ConcurrentLinkedQueue<String> inbox = new ConcurrentLinkedQueue<String>();
    private final Json json = new Json(JsonWriter.OutputType.json);

    private WebSocketClient socket;
    private Listener listener;
    private volatile boolean open;
    private String peerId = "";

    public RelayClient(String serverUrl, String playerName) {
        this.serverUrl = serverUrl;
        this.playerName = playerName;
    }

    public void connect() {
        try {
            socket = new WebSocketClient(new URI(serverUrl)) {
                @Override
                public void onOpen(ServerHandshake handshake) {
                    open = true;
                    sendJson("{\"t\":\"hello\",\"name\":" + quote(playerName) + "}");
                }

                @Override
                public void onMessage(String message) {
                    inbox.add(message);
                }

                @Override
                public void onClose(int code, String reason, boolean remote) {
                    open = false;
                    inbox.add("{\"t\":\"closed\",\"reason\":" + quote(reason == null ? "" : reason) + "}");
                }

                @Override
                public void onError(Exception exception) {
                    Gdx.app.error("RelayClient", "Socket error", exception);
                    inbox.add("{\"t\":\"closed\",\"reason\":\"error\"}");
                }
            };
            socket.setConnectionLostTimeout(45);
            socket.connect();
        } catch (Exception exception) {
            Gdx.app.error("RelayClient", "Could not connect", exception);
            inbox.add("{\"t\":\"closed\",\"reason\":\"connect_failed\"}");
        }
    }

    private String quote(String value) {
        return json.toJson(value == null ? "" : value);
    }

    private void sendJson(String payload) {
        if (socket == null || !open) {
            return;
        }
        try {
            socket.send(payload);
        } catch (Exception exception) {
            Gdx.app.error("RelayClient", "Send failed", exception);
        }
    }

    public void requestRoomList(String query) {
        sendJson("{\"t\":\"list\",\"query\":" + quote(query) + "}");
    }

    public void createRoom(String name, String password, String scenarioId, String scenarioName,
                           int maxPlayers, int difficulty, int aggression) {
        StringBuilder builder = new StringBuilder();
        builder.append("{\"t\":\"create\",\"name\":").append(quote(name));
        builder.append(",\"password\":").append(quote(password));
        builder.append(",\"scenario\":").append(quote(scenarioId));
        builder.append(",\"scenarioName\":").append(quote(scenarioName));
        builder.append(",\"maxPlayers\":").append(maxPlayers);
        builder.append(",\"difficulty\":").append(difficulty);
        builder.append(",\"aggression\":").append(aggression).append("}");
        sendJson(builder.toString());
    }

    public void joinRoom(String roomId, String password) {
        sendJson("{\"t\":\"join\",\"roomId\":" + quote(roomId) + ",\"password\":" + quote(password) + "}");
    }

    public void updateRoom(String scenarioId, String scenarioName, int difficulty, int aggression, boolean started) {
        StringBuilder builder = new StringBuilder();
        builder.append("{\"t\":\"update\",\"scenario\":").append(quote(scenarioId));
        builder.append(",\"scenarioName\":").append(quote(scenarioName));
        builder.append(",\"difficulty\":").append(difficulty);
        builder.append(",\"aggression\":").append(aggression);
        builder.append(",\"started\":").append(started).append("}");
        sendJson(builder.toString());
    }

    public void leaveRoom() {
        sendJson("{\"t\":\"leave\"}");
    }

    public void relayToAll(String data) {
        sendJson("{\"t\":\"relay\",\"data\":" + quote(data) + "}");
    }

    public void relayToHost(String data) {
        sendJson("{\"t\":\"relay\",\"toHost\":true,\"data\":" + quote(data) + "}");
    }

    public void relayToPeer(String targetPeerId, String data) {
        sendJson("{\"t\":\"relay\",\"to\":" + quote(targetPeerId) + ",\"data\":" + quote(data) + "}");
    }

    public void poll() {
        if (listener == null) {
            return;
        }
        String raw;
        while ((raw = inbox.poll()) != null) {
            try {
                dispatch(new JsonReader().parse(raw));
            } catch (Exception exception) {
                Gdx.app.error("RelayClient", "Bad message", exception);
            }
        }
    }

    private void dispatch(JsonValue root) {
        String type = root.getString("t", "");

        if ("welcome".equals(type)) {
            peerId = root.getString("peerId", "");
            listener.onConnected(peerId);
        } else if ("rooms".equals(type)) {
            listener.onRoomList(readRooms(root.get("rooms")));
        } else if ("created".equals(type)) {
            listener.onRoomEntered(readRoom(root.get("room")), readMembers(root.get("members")), true);
        } else if ("joined".equals(type)) {
            listener.onRoomEntered(readRoom(root.get("room")), readMembers(root.get("members")), false);
        } else if ("roomUpdated".equals(type)) {
            listener.onRoomUpdated(readRoom(root.get("room")), readMembers(root.get("members")));
        } else if ("peerJoined".equals(type)) {
            listener.onPeerJoined(readPeer(root.get("peer")), readMembers(root.get("members")));
        } else if ("peerLeft".equals(type)) {
            listener.onPeerLeft(root.getString("peerId", ""), readMembers(root.get("members")));
        } else if ("relay".equals(type)) {
            listener.onRelay(root.getString("from", ""), root.getString("data", ""));
        } else if ("error".equals(type)) {
            listener.onServerError(root.getString("code", "unknown"));
        } else if ("roomClosed".equals(type)) {
            listener.onClosed(root.getString("reason", "room_closed"));
        } else if ("closed".equals(type)) {
            listener.onClosed(root.getString("reason", ""));
        }
    }

    private Array<RoomInfo> readRooms(JsonValue array) {
        Array<RoomInfo> results = new Array<RoomInfo>();
        if (array == null) {
            return results;
        }
        for (JsonValue entry = array.child; entry != null; entry = entry.next) {
            results.add(readRoom(entry));
        }
        return results;
    }

    private RoomInfo readRoom(JsonValue value) {
        RoomInfo room = new RoomInfo();
        if (value == null) {
            return room;
        }
        room.id = value.getString("id", "");
        room.name = value.getString("name", "");
        room.hostName = value.getString("hostName", "");
        room.scenario = value.getString("scenario", "");
        room.scenarioName = value.getString("scenarioName", "");
        room.players = value.getInt("players", 0);
        room.maxPlayers = value.getInt("maxPlayers", 0);
        room.locked = value.getBoolean("locked", false);
        room.difficulty = value.getInt("difficulty", 0);
        room.aggression = value.getInt("aggression", 50);
        room.started = value.getBoolean("started", false);
        return room;
    }

    private Array<PeerInfo> readMembers(JsonValue array) {
        Array<PeerInfo> results = new Array<PeerInfo>();
        if (array == null) {
            return results;
        }
        for (JsonValue entry = array.child; entry != null; entry = entry.next) {
            results.add(readPeer(entry));
        }
        return results;
    }

    private PeerInfo readPeer(JsonValue value) {
        PeerInfo peer = new PeerInfo();
        if (value == null) {
            return peer;
        }
        peer.peerId = value.getString("peerId", "");
        peer.name = value.getString("name", "");
        peer.host = value.getBoolean("host", false);
        return peer;
    }

    public boolean isOpen() {
        return open;
    }

    public String getPeerId() {
        return peerId;
    }

    public void setListener(Listener listener) {
        this.listener = listener;
    }

    public void close() {
        if (socket != null) {
            try {
                socket.close();
            } catch (Exception ignored) {
            }
        }
        open = false;
    }
}

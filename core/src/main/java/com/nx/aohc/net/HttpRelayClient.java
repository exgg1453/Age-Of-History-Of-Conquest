package com.nx.aohc.net;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Json;
import com.badlogic.gdx.utils.JsonReader;
import com.badlogic.gdx.utils.JsonValue;
import com.badlogic.gdx.utils.JsonWriter;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.ConcurrentLinkedQueue;

public class HttpRelayClient implements Relay {

    private static final long POLL_INTERVAL_MILLISECONDS = 1200L;
    private static final int TIMEOUT_MILLISECONDS = 15000;

    private final String endpoint;
    private final String playerName;
    private final ConcurrentLinkedQueue<String> inbox = new ConcurrentLinkedQueue<String>();
    private final Json json = new Json(JsonWriter.OutputType.json);

    private Listener listener;
    private volatile boolean running;
    private volatile String peerId = "";
    private volatile String roomId = "";
    private volatile String cursor = "";
    private volatile boolean hostRole;
    private volatile int lastMemberCount = -1;
    private Thread pollThread;

    public HttpRelayClient(String baseUrl, String playerName) {
        String trimmed = baseUrl.trim();
        while (trimmed.endsWith("/")) {
            trimmed = trimmed.substring(0, trimmed.length() - 1);
        }
        this.endpoint = trimmed.endsWith("/api/lobby") ? trimmed : trimmed + "/api/lobby";
        this.playerName = playerName;
    }

    private String quote(String value) {
        return json.toJson(value == null ? "" : value);
    }

    private String request(String payload) {
        HttpURLConnection connection = null;
        try {
            connection = (HttpURLConnection) new URL(endpoint).openConnection();
            connection.setRequestMethod("POST");
            connection.setDoOutput(true);
            connection.setConnectTimeout(TIMEOUT_MILLISECONDS);
            connection.setReadTimeout(TIMEOUT_MILLISECONDS);
            connection.setRequestProperty("Content-Type", "application/json");

            OutputStream output = connection.getOutputStream();
            output.write(payload.getBytes("UTF-8"));
            output.flush();
            output.close();

            int status = connection.getResponseCode();
            InputStream input = status >= 400 ? connection.getErrorStream() : connection.getInputStream();
            if (input == null) {
                return "";
            }

            BufferedReader reader = new BufferedReader(new InputStreamReader(input, "UTF-8"));
            StringBuilder builder = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                builder.append(line);
            }
            reader.close();
            return builder.toString();
        } catch (Exception exception) {
            Gdx.app.error("HttpRelayClient", "Request failed", exception);
            return "{\"t\":\"error\",\"code\":\"network\"}";
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    private void requestAsync(final String payload) {
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                String response = request(payload);
                if (!response.isEmpty()) {
                    inbox.add(response);
                }
            }
        }, "aohc-http-request");
        thread.setDaemon(true);
        thread.start();
    }

    private void startPolling() {
        if (pollThread != null) {
            return;
        }
        running = true;
        pollThread = new Thread(new Runnable() {
            @Override
            public void run() {
                while (running) {
                    try {
                        Thread.sleep(POLL_INTERVAL_MILLISECONDS);
                    } catch (InterruptedException exception) {
                        return;
                    }
                    if (roomId.isEmpty() || peerId.isEmpty()) {
                        continue;
                    }
                    StringBuilder builder = new StringBuilder();
                    builder.append("{\"action\":\"poll\",\"roomId\":").append(quote(roomId));
                    builder.append(",\"peerId\":").append(quote(peerId));
                    builder.append(",\"since\":").append(quote(cursor)).append("}");
                    String response = request(builder.toString());
                    if (!response.isEmpty()) {
                        inbox.add(response);
                    }
                }
            }
        }, "aohc-http-poll");
        pollThread.setDaemon(true);
        pollThread.start();
    }

    @Override
    public void connect() {
        running = true;
        inbox.add("{\"t\":\"ready\"}");
    }

    @Override
    public void setListener(Listener listener) {
        this.listener = listener;
    }

    @Override
    public void requestRoomList(String query) {
        requestAsync("{\"action\":\"list\",\"query\":" + quote(query) + "}");
    }

    @Override
    public void createRoom(String name, String password, String scenarioId, String scenarioName,
                           int maxPlayers, int difficulty, int aggression) {
        hostRole = true;
        StringBuilder builder = new StringBuilder();
        builder.append("{\"action\":\"create\",\"name\":").append(quote(playerName));
        builder.append(",\"roomName\":").append(quote(name));
        builder.append(",\"password\":").append(quote(password));
        builder.append(",\"scenario\":").append(quote(scenarioId));
        builder.append(",\"scenarioName\":").append(quote(scenarioName));
        builder.append(",\"maxPlayers\":").append(maxPlayers);
        builder.append(",\"difficulty\":").append(difficulty);
        builder.append(",\"aggression\":").append(aggression).append("}");
        requestAsync(builder.toString());
    }

    @Override
    public void joinRoom(String targetRoomId, String password) {
        hostRole = false;
        StringBuilder builder = new StringBuilder();
        builder.append("{\"action\":\"join\",\"roomId\":").append(quote(targetRoomId));
        builder.append(",\"name\":").append(quote(playerName));
        builder.append(",\"password\":").append(quote(password)).append("}");
        requestAsync(builder.toString());
    }

    @Override
    public void updateRoom(String scenarioId, String scenarioName, int difficulty, int aggression, boolean started) {
        StringBuilder builder = new StringBuilder();
        builder.append("{\"action\":\"update\",\"roomId\":").append(quote(roomId));
        builder.append(",\"peerId\":").append(quote(peerId));
        builder.append(",\"scenario\":").append(quote(scenarioId));
        builder.append(",\"scenarioName\":").append(quote(scenarioName));
        builder.append(",\"difficulty\":").append(difficulty);
        builder.append(",\"aggression\":").append(aggression);
        builder.append(",\"started\":").append(started).append("}");
        requestAsync(builder.toString());
    }

    @Override
    public void leaveRoom() {
        if (roomId.isEmpty() || peerId.isEmpty()) {
            return;
        }
        requestAsync("{\"action\":\"leave\",\"roomId\":" + quote(roomId)
                + ",\"peerId\":" + quote(peerId) + "}");
        roomId = "";
    }

    private void send(String data, String target, boolean toHost) {
        if (roomId.isEmpty() || peerId.isEmpty()) {
            return;
        }
        StringBuilder builder = new StringBuilder();
        builder.append("{\"action\":\"send\",\"roomId\":").append(quote(roomId));
        builder.append(",\"peerId\":").append(quote(peerId));
        builder.append(",\"toHost\":").append(toHost);
        builder.append(",\"to\":").append(quote(target == null ? "" : target));
        builder.append(",\"data\":").append(quote(data)).append("}");
        requestAsync(builder.toString());
    }

    @Override
    public void relayToAll(String data) {
        send(data, "", false);
    }

    @Override
    public void relayToHost(String data) {
        send(data, "", true);
    }

    @Override
    public void relayToPeer(String targetPeerId, String data) {
        send(data, targetPeerId, false);
    }

    @Override
    public void poll() {
        if (listener == null) {
            return;
        }
        String raw;
        while ((raw = inbox.poll()) != null) {
            try {
                dispatch(new JsonReader().parse(raw));
            } catch (Exception exception) {
                Gdx.app.error("HttpRelayClient", "Bad response", exception);
            }
        }
    }

    private void dispatch(JsonValue root) {
        String type = root.getString("t", "");

        if ("ready".equals(type)) {
            listener.onConnected("");
            return;
        }

        if ("rooms".equals(type)) {
            listener.onRoomList(readRooms(root.get("rooms")));
            return;
        }

        if ("created".equals(type) || "joined".equals(type)) {
            peerId = root.getString("peerId", "");
            RelayClient.RoomInfo room = readRoom(root.get("room"));
            roomId = room.id;
            cursor = "";
            Array<RelayClient.PeerInfo> members = readMembers(root.get("members"));
            lastMemberCount = members.size;
            startPolling();
            listener.onRoomEntered(room, members, "created".equals(type));
            return;
        }

        if ("roomUpdated".equals(type)) {
            listener.onRoomUpdated(readRoom(root.get("room")), readMembers(root.get("members")));
            return;
        }

        if ("poll".equals(type)) {
            cursor = root.getString("cursor", cursor);

            Array<RelayClient.PeerInfo> members = readMembers(root.get("members"));
            RelayClient.RoomInfo room = readRoom(root.get("room"));
            if (members.size != lastMemberCount) {
                lastMemberCount = members.size;
                listener.onRoomUpdated(room, members);
            }

            JsonValue messages = root.get("messages");
            if (messages != null) {
                for (JsonValue entry = messages.child; entry != null; entry = entry.next) {
                    listener.onRelay(entry.getString("from", ""), entry.getString("data", ""));
                }
            }
            return;
        }

        if ("error".equals(type)) {
            String code = root.getString("code", "unknown");
            if ("room_not_found".equals(code) && !roomId.isEmpty()) {
                roomId = "";
                listener.onClosed("room_closed");
                return;
            }
            listener.onServerError(code);
        }
    }

    private Array<RelayClient.RoomInfo> readRooms(JsonValue array) {
        Array<RelayClient.RoomInfo> results = new Array<RelayClient.RoomInfo>();
        if (array == null) {
            return results;
        }
        for (JsonValue entry = array.child; entry != null; entry = entry.next) {
            results.add(readRoom(entry));
        }
        return results;
    }

    private RelayClient.RoomInfo readRoom(JsonValue value) {
        RelayClient.RoomInfo room = new RelayClient.RoomInfo();
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

    private Array<RelayClient.PeerInfo> readMembers(JsonValue array) {
        Array<RelayClient.PeerInfo> results = new Array<RelayClient.PeerInfo>();
        if (array == null) {
            return results;
        }
        for (JsonValue entry = array.child; entry != null; entry = entry.next) {
            RelayClient.PeerInfo peer = new RelayClient.PeerInfo();
            peer.peerId = entry.getString("peerId", "");
            peer.name = entry.getString("name", "");
            peer.host = entry.getBoolean("host", false);
            results.add(peer);
        }
        return results;
    }

    @Override
    public boolean isOpen() {
        return running;
    }

    @Override
    public String getPeerId() {
        return peerId;
    }

    @Override
    public void close() {
        running = false;
        if (pollThread != null) {
            pollThread.interrupt();
            pollThread = null;
        }
    }
}

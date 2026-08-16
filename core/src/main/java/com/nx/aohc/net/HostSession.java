package com.nx.aohc.net;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.utils.Array;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CopyOnWriteArrayList;

public class HostSession implements NetworkSession {

    private static class ClientConnection {
        Socket socket;
        BufferedWriter writer;
        String playerId;
    }

    private final String lobbyName;
    private final Array<LobbyPlayer> players = new Array<LobbyPlayer>();
    private final CopyOnWriteArrayList<ClientConnection> connections = new CopyOnWriteArrayList<ClientConnection>();
    private final ConcurrentLinkedQueue<String> inbox = new ConcurrentLinkedQueue<String>();

    private ServerSocket serverSocket;
    private DatagramSocket discoverySocket;
    private Listener listener;
    private volatile boolean running = true;
    private int nextClientId = 1;
    private String scenarioId = "";

    public HostSession(String hostPlayerName, String lobbyName) {
        this.lobbyName = lobbyName;

        LobbyPlayer host = new LobbyPlayer("host", hostPlayerName);
        host.host = true;
        host.local = true;
        players.add(host);

        startServer();
        startDiscoveryResponder();
    }

    private void startServer() {
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    serverSocket = new ServerSocket(GAME_PORT);
                    while (running) {
                        Socket socket = serverSocket.accept();
                        socket.setTcpNoDelay(true);
                        handleNewClient(socket);
                    }
                } catch (Exception exception) {
                    if (running) {
                        Gdx.app.error("HostSession", "Server socket failed", exception);
                    }
                }
            }
        }, "aohc-host-accept");
        thread.setDaemon(true);
        thread.start();
    }

    private void handleNewClient(final Socket socket) {
        final ClientConnection connection = new ClientConnection();
        connection.socket = socket;

        try {
            connection.writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(), "UTF-8"));
        } catch (Exception exception) {
            Gdx.app.error("HostSession", "Could not open client stream", exception);
            return;
        }

        connections.add(connection);

        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream(), "UTF-8"));
                    String line;
                    while (running && (line = reader.readLine()) != null) {
                        handleClientLine(connection, line);
                    }
                } catch (Exception exception) {
                    Gdx.app.log("HostSession", "Client disconnected");
                } finally {
                    dropConnection(connection);
                }
            }
        }, "aohc-host-client");
        thread.setDaemon(true);
        thread.start();
    }

    private void handleClientLine(ClientConnection connection, String line) {
        int separator = line.indexOf(':');
        if (separator < 0) {
            return;
        }
        String header = line.substring(0, separator);
        String body = line.substring(separator + 1);

        if (MESSAGE_JOIN.equals(header)) {
            String playerId = "client" + (nextClientId++);
            connection.playerId = playerId;
            LobbyPlayer player = new LobbyPlayer(playerId, body);
            synchronized (players) {
                players.add(player);
            }
            sendTo(connection, MESSAGE_JOIN + ":" + playerId);
            inbox.add(MESSAGE_LOBBY + ":refresh");
            broadcastLobby();
            return;
        }

        if (MESSAGE_CLAIM.equals(header)) {
            String[] parts = body.split("\u001f", -1);
            if (parts.length >= 2) {
                applyClaim(parts[0], parts[1]);
                broadcastLobby();
                inbox.add(MESSAGE_LOBBY + ":refresh");
            }
            return;
        }

        if (MESSAGE_REQUEST.equals(header)) {
            inbox.add(MESSAGE_REQUEST + ":" + body);
            return;
        }

        if (MESSAGE_CHAT.equals(header)) {
            broadcastRaw(MESSAGE_CHAT + ":" + body);
            inbox.add(MESSAGE_CHAT + ":" + body);
        }
    }

    private void applyClaim(String playerId, String countryId) {
        synchronized (players) {
            for (int index = 0; index < players.size; index++) {
                LobbyPlayer player = players.get(index);
                if (player.countryId != null && player.countryId.equals(countryId) && !player.id.equals(playerId)) {
                    return;
                }
            }
            for (int index = 0; index < players.size; index++) {
                if (players.get(index).id.equals(playerId)) {
                    players.get(index).countryId = countryId.isEmpty() ? null : countryId;
                    return;
                }
            }
        }
    }

    private void dropConnection(ClientConnection connection) {
        connections.remove(connection);
        if (connection.playerId != null) {
            synchronized (players) {
                for (int index = players.size - 1; index >= 0; index--) {
                    if (players.get(index).id.equals(connection.playerId)) {
                        players.removeIndex(index);
                    }
                }
            }
            broadcastLobby();
            inbox.add(MESSAGE_LOBBY + ":refresh");
        }
        try {
            connection.socket.close();
        } catch (Exception ignored) {
        }
    }

    private void startDiscoveryResponder() {
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    discoverySocket = new DatagramSocket(DISCOVERY_PORT);
                    discoverySocket.setBroadcast(true);
                    byte[] buffer = new byte[256];
                    while (running) {
                        DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                        discoverySocket.receive(packet);
                        String message = new String(packet.getData(), 0, packet.getLength(), "UTF-8");
                        if (!DISCOVERY_QUERY.equals(message.trim())) {
                            continue;
                        }
                        String reply = DISCOVERY_REPLY + "\u001f" + lobbyName + "\u001f" + players.size;
                        byte[] data = reply.getBytes("UTF-8");
                        discoverySocket.send(new DatagramPacket(data, data.length, packet.getAddress(), packet.getPort()));
                    }
                } catch (Exception exception) {
                    if (running) {
                        Gdx.app.error("HostSession", "Discovery responder failed", exception);
                    }
                }
            }
        }, "aohc-host-discovery");
        thread.setDaemon(true);
        thread.start();
    }

    private void sendTo(ClientConnection connection, String line) {
        try {
            connection.writer.write(line);
            connection.writer.write("\n");
            connection.writer.flush();
        } catch (Exception exception) {
            dropConnection(connection);
        }
    }

    private void broadcastRaw(String line) {
        for (ClientConnection connection : connections) {
            sendTo(connection, line);
        }
    }

    private void broadcastLobby() {
        StringBuilder builder = new StringBuilder();
        synchronized (players) {
            for (int index = 0; index < players.size; index++) {
                LobbyPlayer player = players.get(index);
                if (index > 0) {
                    builder.append("\u001e");
                }
                builder.append(player.id).append("\u001f")
                        .append(player.name).append("\u001f")
                        .append(player.countryId == null ? "" : player.countryId).append("\u001f")
                        .append(player.host ? 1 : 0);
            }
        }
        broadcastRaw(MESSAGE_LOBBY + ":" + builder.toString());
    }

    @Override
    public int getMode() {
        return MODE_HOST;
    }

    @Override
    public boolean isAuthoritative() {
        return true;
    }

    @Override
    public String getLocalPlayerId() {
        return "host";
    }

    @Override
    public Array<LobbyPlayer> getPlayers() {
        return players;
    }

    @Override
    public LobbyPlayer getLocalPlayer() {
        return players.size > 0 ? players.first() : null;
    }

    @Override
    public void setListener(Listener listener) {
        this.listener = listener;
    }

    @Override
    public void claimCountry(String playerId, String countryId) {
        applyClaim(playerId, countryId);
        broadcastLobby();
        if (listener != null) {
            listener.onLobbyChanged();
        }
    }

    @Override
    public void startGame(String scenarioId) {
        this.scenarioId = scenarioId;
        broadcastLobby();
        broadcastRaw(MESSAGE_START + ":" + scenarioId);
        if (listener != null) {
            listener.onGameStarted(scenarioId);
        }
    }

    @Override
    public void submitCommand(GameCommand command) {
        if (listener != null) {
            listener.onCommandReceived(command);
        }
    }

    @Override
    public void broadcastCommand(GameCommand command) {
        broadcastRaw(MESSAGE_COMMAND + ":" + command.encode());
    }

    @Override
    public void broadcastSnapshot(String payload) {
        broadcastRaw(MESSAGE_SNAPSHOT + ":" + payload);
    }

    @Override
    public void broadcastTurn(int activePlayerIndex) {
        broadcastRaw(MESSAGE_TURN + ":" + activePlayerIndex);
    }

    @Override
    public void sendChat(String message) {
        LobbyPlayer local = getLocalPlayer();
        String name = local != null ? local.name : "host";
        broadcastRaw(MESSAGE_CHAT + ":" + name + "\u001f" + message);
        if (listener != null) {
            listener.onChatReceived(name, message);
        }
    }

    @Override
    public void poll() {
        if (listener == null) {
            return;
        }
        String line;
        while ((line = inbox.poll()) != null) {
            int separator = line.indexOf(':');
            if (separator < 0) {
                continue;
            }
            String header = line.substring(0, separator);
            String body = line.substring(separator + 1);

            if (MESSAGE_LOBBY.equals(header)) {
                listener.onLobbyChanged();
            } else if (MESSAGE_REQUEST.equals(header)) {
                GameCommand command = GameCommand.decode(body);
                if (command != null) {
                    listener.onCommandReceived(command);
                }
            } else if (MESSAGE_CHAT.equals(header)) {
                String[] parts = body.split("\u001f", 2);
                if (parts.length == 2) {
                    listener.onChatReceived(parts[0], parts[1]);
                }
            }
        }
    }

    @Override
    public void close() {
        running = false;
        for (ClientConnection connection : connections) {
            try {
                connection.socket.close();
            } catch (Exception ignored) {
            }
        }
        connections.clear();
        try {
            if (serverSocket != null) {
                serverSocket.close();
            }
        } catch (Exception ignored) {
        }
        if (discoverySocket != null) {
            discoverySocket.close();
        }
    }

    public String getScenarioId() {
        return scenarioId;
    }
}

package com.nx.aohc.net;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.utils.Array;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.concurrent.ConcurrentLinkedQueue;

public class ClientSession implements NetworkSession {

    private final String hostAddress;
    private final String playerName;
    private final Array<LobbyPlayer> players = new Array<LobbyPlayer>();
    private final ConcurrentLinkedQueue<String> inbox = new ConcurrentLinkedQueue<String>();

    private Socket socket;
    private BufferedWriter writer;
    private Listener listener;
    private volatile boolean running = true;
    private volatile String localPlayerId = "";
    private volatile boolean connected;

    public ClientSession(String hostAddress, String playerName) {
        this.hostAddress = hostAddress;
        this.playerName = playerName;
        connect();
    }

    private void connect() {
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    socket = new Socket();
                    socket.connect(new InetSocketAddress(hostAddress, GAME_PORT), 6000);
                    socket.setTcpNoDelay(true);
                    writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(), "UTF-8"));

                    sendRaw(MESSAGE_JOIN + ":" + playerName);
                    connected = true;

                    BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream(), "UTF-8"));
                    String line;
                    while (running && (line = reader.readLine()) != null) {
                        inbox.add(line);
                    }
                } catch (Exception exception) {
                    Gdx.app.error("ClientSession", "Connection failed", exception);
                    inbox.add("ERROR:" + exception.getClass().getSimpleName());
                } finally {
                    connected = false;
                    if (running) {
                        inbox.add("ERROR:disconnected");
                    }
                }
            }
        }, "aohc-client");
        thread.setDaemon(true);
        thread.start();
    }

    private void sendRaw(String line) {
        try {
            if (writer == null) {
                return;
            }
            writer.write(line);
            writer.write("\n");
            writer.flush();
        } catch (Exception exception) {
            Gdx.app.error("ClientSession", "Send failed", exception);
        }
    }

    private void parseLobby(String body) {
        players.clear();
        if (body.isEmpty()) {
            return;
        }
        String[] entries = body.split("\u001e", -1);
        for (int index = 0; index < entries.length; index++) {
            String[] parts = entries[index].split("\u001f", -1);
            if (parts.length < 4) {
                continue;
            }
            LobbyPlayer player = new LobbyPlayer(parts[0], parts[1]);
            player.countryId = parts[2].isEmpty() ? null : parts[2];
            player.host = "1".equals(parts[3]);
            player.local = player.id.equals(localPlayerId);
            players.add(player);
        }
    }

    public boolean isConnected() {
        return connected;
    }

    @Override
    public int getMode() {
        return MODE_CLIENT;
    }

    @Override
    public boolean isAuthoritative() {
        return false;
    }

    @Override
    public String getLocalPlayerId() {
        return localPlayerId;
    }

    @Override
    public Array<LobbyPlayer> getPlayers() {
        return players;
    }

    @Override
    public LobbyPlayer getLocalPlayer() {
        for (int index = 0; index < players.size; index++) {
            if (players.get(index).id.equals(localPlayerId)) {
                return players.get(index);
            }
        }
        return null;
    }

    @Override
    public void setListener(Listener listener) {
        this.listener = listener;
    }

    @Override
    public void claimCountry(String playerId, String countryId) {
        sendRaw(MESSAGE_CLAIM + ":" + playerId + "\u001f" + (countryId == null ? "" : countryId));
    }

    @Override
    public void startGame(String scenarioId) {
    }

    @Override
    public void submitCommand(GameCommand command) {
        sendRaw(MESSAGE_REQUEST + ":" + command.encode());
    }

    @Override
    public void broadcastCommand(GameCommand command) {
    }

    @Override
    public void broadcastSnapshot(String payload) {
    }

    @Override
    public void broadcastTurn(int activePlayerIndex) {
    }

    @Override
    public void sendChat(String message) {
        LobbyPlayer local = getLocalPlayer();
        String name = local != null ? local.name : playerName;
        sendRaw(MESSAGE_CHAT + ":" + name + "\u001f" + message);
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

            if (MESSAGE_JOIN.equals(header)) {
                localPlayerId = body;
            } else if (MESSAGE_LOBBY.equals(header)) {
                parseLobby(body);
                listener.onLobbyChanged();
            } else if (MESSAGE_START.equals(header)) {
                listener.onGameStarted(body);
            } else if (MESSAGE_COMMAND.equals(header)) {
                GameCommand command = GameCommand.decode(body);
                if (command != null) {
                    listener.onCommandReceived(command);
                }
            } else if (MESSAGE_SNAPSHOT.equals(header)) {
                listener.onSnapshotReceived(body);
            } else if (MESSAGE_TURN.equals(header)) {
                try {
                    listener.onTurnChanged(Integer.parseInt(body));
                } catch (NumberFormatException ignored) {
                }
            } else if (MESSAGE_CHAT.equals(header)) {
                String[] parts = body.split("\u001f", 2);
                if (parts.length == 2) {
                    listener.onChatReceived(parts[0], parts[1]);
                }
            } else if ("ERROR".equals(header)) {
                listener.onDisconnected(body);
            }
        }
    }

    @Override
    public void close() {
        running = false;
        try {
            if (socket != null) {
                socket.close();
            }
        } catch (Exception ignored) {
        }
    }
}

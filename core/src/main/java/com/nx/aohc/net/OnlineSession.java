package com.nx.aohc.net;

import com.badlogic.gdx.utils.Array;

public class OnlineSession implements NetworkSession, RelayClient.Listener {

    public interface RoomListener {
        void onRoomStateChanged(RelayClient.RoomInfo room);

        void onServerError(String code);
    }

    private final RelayClient relay;
    private final boolean hostRole;
    private final Array<LobbyPlayer> players = new Array<LobbyPlayer>();

    private Listener listener;
    private RoomListener roomListener;
    private RelayClient.RoomInfo room;
    private String localPeerId = "";
    private String scenarioId = "";

    public OnlineSession(RelayClient relay, boolean hostRole) {
        this.relay = relay;
        this.hostRole = hostRole;
        this.relay.setListener(this);
    }

    public RelayClient getRelay() {
        return relay;
    }

    public RelayClient.RoomInfo getRoom() {
        return room;
    }

    public void setRoomListener(RoomListener roomListener) {
        this.roomListener = roomListener;
    }

    private LobbyPlayer findPlayer(String id) {
        for (int index = 0; index < players.size; index++) {
            if (players.get(index).id.equals(id)) {
                return players.get(index);
            }
        }
        return null;
    }

    private void syncPlayersFromMembers(Array<RelayClient.PeerInfo> members) {
        Array<LobbyPlayer> rebuilt = new Array<LobbyPlayer>();
        for (int index = 0; index < members.size; index++) {
            RelayClient.PeerInfo info = members.get(index);
            LobbyPlayer existing = findPlayer(info.peerId);
            LobbyPlayer player = existing != null ? existing : new LobbyPlayer(info.peerId, info.name);
            player.name = info.name;
            player.host = info.host;
            player.local = info.peerId.equals(localPeerId);
            rebuilt.add(player);
        }
        players.clear();
        players.addAll(rebuilt);
    }

    private String encodeLobby() {
        StringBuilder builder = new StringBuilder();
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
        return builder.toString();
    }

    private void decodeLobby(String body) {
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
            player.local = player.id.equals(localPeerId);
            players.add(player);
        }
    }

    private void applyClaim(String playerId, String countryId) {
        for (int index = 0; index < players.size; index++) {
            LobbyPlayer player = players.get(index);
            if (player.countryId != null && player.countryId.equals(countryId) && !player.id.equals(playerId)) {
                return;
            }
        }
        LobbyPlayer player = findPlayer(playerId);
        if (player != null) {
            player.countryId = countryId == null || countryId.isEmpty() ? null : countryId;
        }
    }

    private void broadcastLobby() {
        relay.relayToAll(MESSAGE_LOBBY + ":" + encodeLobby());
    }

    @Override
    public int getMode() {
        return hostRole ? MODE_HOST : MODE_CLIENT;
    }

    @Override
    public boolean isAuthoritative() {
        return hostRole;
    }

    @Override
    public String getLocalPlayerId() {
        return localPeerId;
    }

    @Override
    public Array<LobbyPlayer> getPlayers() {
        return players;
    }

    @Override
    public LobbyPlayer getLocalPlayer() {
        return findPlayer(localPeerId);
    }

    @Override
    public void setListener(Listener listener) {
        this.listener = listener;
    }

    @Override
    public void claimCountry(String playerId, String countryId) {
        if (hostRole) {
            applyClaim(playerId, countryId);
            broadcastLobby();
            if (listener != null) {
                listener.onLobbyChanged();
            }
        } else {
            relay.relayToHost(MESSAGE_CLAIM + ":" + playerId + "\u001f" + (countryId == null ? "" : countryId));
        }
    }

    @Override
    public void startGame(String scenarioId) {
        if (!hostRole) {
            return;
        }
        this.scenarioId = scenarioId;
        broadcastLobby();
        relay.relayToAll(MESSAGE_START + ":" + scenarioId);
        if (listener != null) {
            listener.onGameStarted(scenarioId);
        }
    }

    @Override
    public void submitCommand(GameCommand command) {
        if (hostRole) {
            if (listener != null) {
                listener.onCommandReceived(command);
            }
        } else {
            relay.relayToHost(MESSAGE_REQUEST + ":" + command.encode());
        }
    }

    @Override
    public void broadcastCommand(GameCommand command) {
        if (hostRole) {
            relay.relayToAll(MESSAGE_COMMAND + ":" + command.encode());
        }
    }

    @Override
    public void broadcastSnapshot(String payload) {
        if (hostRole) {
            relay.relayToAll(MESSAGE_SNAPSHOT + ":" + payload);
        }
    }

    @Override
    public void broadcastTurn(int activePlayerIndex) {
        if (hostRole) {
            relay.relayToAll(MESSAGE_TURN + ":" + activePlayerIndex);
        }
    }

    @Override
    public void sendChat(String message) {
        LobbyPlayer local = getLocalPlayer();
        String name = local != null ? local.name : "player";
        relay.relayToAll(MESSAGE_CHAT + ":" + name + "\u001f" + message);
        if (listener != null) {
            listener.onChatReceived(name, message);
        }
    }

    @Override
    public void poll() {
        relay.poll();
    }

    @Override
    public void close() {
        relay.leaveRoom();
        relay.setListener(null);
        relay.close();
    }

    @Override
    public void onConnected(String peerId) {
        localPeerId = peerId;
    }

    @Override
    public void onRoomList(Array<RelayClient.RoomInfo> rooms) {
    }

    @Override
    public void onRoomEntered(RelayClient.RoomInfo enteredRoom, Array<RelayClient.PeerInfo> members, boolean asHost) {
        this.room = enteredRoom;
        syncPlayersFromMembers(members);
        if (roomListener != null) {
            roomListener.onRoomStateChanged(enteredRoom);
        }
        if (listener != null) {
            listener.onLobbyChanged();
        }
    }

    @Override
    public void onRoomUpdated(RelayClient.RoomInfo updatedRoom, Array<RelayClient.PeerInfo> members) {
        this.room = updatedRoom;
        if (hostRole) {
            syncPlayersFromMembers(members);
        }
        if (roomListener != null) {
            roomListener.onRoomStateChanged(updatedRoom);
        }
        if (listener != null) {
            listener.onLobbyChanged();
        }
    }

    @Override
    public void onPeerJoined(RelayClient.PeerInfo peer, Array<RelayClient.PeerInfo> members) {
        if (hostRole) {
            syncPlayersFromMembers(members);
            broadcastLobby();
        }
        if (listener != null) {
            listener.onLobbyChanged();
        }
    }

    @Override
    public void onPeerLeft(String peerId, Array<RelayClient.PeerInfo> members) {
        if (hostRole) {
            syncPlayersFromMembers(members);
            broadcastLobby();
        }
        if (listener != null) {
            listener.onLobbyChanged();
        }
    }

    @Override
    public void onRelay(String fromPeerId, String data) {
        int separator = data.indexOf(':');
        if (separator < 0) {
            return;
        }
        String header = data.substring(0, separator);
        String body = data.substring(separator + 1);

        if (hostRole) {
            if (MESSAGE_CLAIM.equals(header)) {
                String[] parts = body.split("\u001f", -1);
                if (parts.length >= 2) {
                    applyClaim(parts[0], parts[1]);
                    broadcastLobby();
                    if (listener != null) {
                        listener.onLobbyChanged();
                    }
                }
            } else if (MESSAGE_REQUEST.equals(header)) {
                GameCommand command = GameCommand.decode(body);
                if (command != null && listener != null) {
                    listener.onCommandReceived(command);
                }
            } else if (MESSAGE_CHAT.equals(header)) {
                String[] parts = body.split("\u001f", 2);
                if (parts.length == 2) {
                    relay.relayToAll(data);
                    if (listener != null) {
                        listener.onChatReceived(parts[0], parts[1]);
                    }
                }
            }
            return;
        }

        if (MESSAGE_LOBBY.equals(header)) {
            decodeLobby(body);
            if (listener != null) {
                listener.onLobbyChanged();
            }
        } else if (MESSAGE_START.equals(header)) {
            scenarioId = body;
            if (listener != null) {
                listener.onGameStarted(body);
            }
        } else if (MESSAGE_COMMAND.equals(header)) {
            GameCommand command = GameCommand.decode(body);
            if (command != null && listener != null) {
                listener.onCommandReceived(command);
            }
        } else if (MESSAGE_SNAPSHOT.equals(header)) {
            if (listener != null) {
                listener.onSnapshotReceived(body);
            }
        } else if (MESSAGE_TURN.equals(header)) {
            try {
                if (listener != null) {
                    listener.onTurnChanged(Integer.parseInt(body));
                }
            } catch (NumberFormatException ignored) {
            }
        } else if (MESSAGE_CHAT.equals(header)) {
            String[] parts = body.split("\u001f", 2);
            if (parts.length == 2 && listener != null) {
                listener.onChatReceived(parts[0], parts[1]);
            }
        }
    }

    @Override
    public void onServerError(String code) {
        if (roomListener != null) {
            roomListener.onServerError(code);
        }
    }

    @Override
    public void onClosed(String reason) {
        if (listener != null) {
            listener.onDisconnected(reason);
        }
        if (roomListener != null) {
            roomListener.onServerError("disconnected");
        }
    }

    public String getScenarioId() {
        return scenarioId;
    }
}

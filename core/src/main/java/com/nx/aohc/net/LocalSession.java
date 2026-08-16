package com.nx.aohc.net;

import com.badlogic.gdx.utils.Array;

public class LocalSession implements NetworkSession {

    private final int mode;
    private final Array<LobbyPlayer> players = new Array<LobbyPlayer>();
    private Listener listener;

    public LocalSession(int mode, Array<String> playerNames) {
        this.mode = mode;
        for (int index = 0; index < playerNames.size; index++) {
            LobbyPlayer player = new LobbyPlayer("local" + index, playerNames.get(index));
            player.local = true;
            player.host = index == 0;
            players.add(player);
        }
    }

    public LobbyPlayer addPlayer(String name) {
        LobbyPlayer player = new LobbyPlayer("local" + players.size, name);
        player.local = true;
        players.add(player);
        if (listener != null) {
            listener.onLobbyChanged();
        }
        return player;
    }

    public void removePlayer(int index) {
        if (index <= 0 || index >= players.size) {
            return;
        }
        players.removeIndex(index);
        if (listener != null) {
            listener.onLobbyChanged();
        }
    }

    @Override
    public int getMode() {
        return mode;
    }

    @Override
    public boolean isAuthoritative() {
        return true;
    }

    @Override
    public String getLocalPlayerId() {
        return players.size > 0 ? players.first().id : "local0";
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
        for (int index = 0; index < players.size; index++) {
            if (players.get(index).id.equals(playerId)) {
                players.get(index).countryId = countryId;
                break;
            }
        }
        if (listener != null) {
            listener.onLobbyChanged();
        }
    }

    @Override
    public void startGame(String scenarioId) {
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
    }

    @Override
    public void broadcastSnapshot(String payload) {
    }

    @Override
    public void broadcastTurn(int activePlayerIndex) {
    }

    @Override
    public void sendChat(String message) {
    }

    @Override
    public void poll() {
    }

    @Override
    public void close() {
    }
}

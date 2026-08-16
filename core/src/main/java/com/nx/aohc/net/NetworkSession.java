package com.nx.aohc.net;

import com.badlogic.gdx.utils.Array;

public interface NetworkSession {

    int MODE_SINGLE = 0;
    int MODE_HOTSEAT = 1;
    int MODE_HOST = 2;
    int MODE_CLIENT = 3;

    String MESSAGE_JOIN = "JOIN";
    String MESSAGE_LOBBY = "LOBBY";
    String MESSAGE_CLAIM = "CLAIM";
    String MESSAGE_START = "START";
    String MESSAGE_REQUEST = "REQ";
    String MESSAGE_COMMAND = "CMD";
    String MESSAGE_SNAPSHOT = "SNAP";
    String MESSAGE_TURN = "TURN";
    String MESSAGE_CHAT = "CHAT";

    int DISCOVERY_PORT = 45455;
    int GAME_PORT = 45456;
    String DISCOVERY_QUERY = "AOHC_DISCOVER";
    String DISCOVERY_REPLY = "AOHC_HOST";

    interface Listener {
        void onLobbyChanged();

        void onGameStarted(String scenarioId);

        void onCommandReceived(GameCommand command);

        void onSnapshotReceived(String payload);

        void onTurnChanged(int activePlayerIndex);

        void onChatReceived(String playerName, String message);

        void onDisconnected(String reason);
    }

    int getMode();

    boolean isAuthoritative();

    String getLocalPlayerId();

    Array<LobbyPlayer> getPlayers();

    LobbyPlayer getLocalPlayer();

    void setListener(Listener listener);

    void claimCountry(String playerId, String countryId);

    void startGame(String scenarioId);

    void submitCommand(GameCommand command);

    void broadcastCommand(GameCommand command);

    void broadcastSnapshot(String payload);

    void broadcastTurn(int activePlayerIndex);

    void sendChat(String message);

    void poll();

    void close();
}

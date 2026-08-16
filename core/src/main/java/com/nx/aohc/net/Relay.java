package com.nx.aohc.net;

import com.badlogic.gdx.utils.Array;

public interface Relay {

    interface Listener {
        void onConnected(String peerId);

        void onRoomList(Array<RelayClient.RoomInfo> rooms);

        void onRoomEntered(RelayClient.RoomInfo room, Array<RelayClient.PeerInfo> members, boolean asHost);

        void onRoomUpdated(RelayClient.RoomInfo room, Array<RelayClient.PeerInfo> members);

        void onPeerJoined(RelayClient.PeerInfo peer, Array<RelayClient.PeerInfo> members);

        void onPeerLeft(String peerId, Array<RelayClient.PeerInfo> members);

        void onRelay(String fromPeerId, String data);

        void onServerError(String code);

        void onClosed(String reason);
    }

    void connect();

    void setListener(Listener listener);

    void requestRoomList(String query);

    void createRoom(String name, String password, String scenarioId, String scenarioName,
                    int maxPlayers, int difficulty, int aggression);

    void joinRoom(String roomId, String password);

    void updateRoom(String scenarioId, String scenarioName, int difficulty, int aggression, boolean started);

    void leaveRoom();

    void relayToAll(String data);

    void relayToHost(String data);

    void relayToPeer(String targetPeerId, String data);

    void poll();

    boolean isOpen();

    String getPeerId();

    void close();
}

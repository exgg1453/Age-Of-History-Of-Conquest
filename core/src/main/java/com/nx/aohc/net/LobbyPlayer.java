package com.nx.aohc.net;

public class LobbyPlayer {

    public String id;
    public String name;
    public String countryId;
    public boolean host;
    public boolean ready;
    public boolean local;

    public LobbyPlayer(String id, String name) {
        this.id = id;
        this.name = name;
        this.countryId = null;
    }
}

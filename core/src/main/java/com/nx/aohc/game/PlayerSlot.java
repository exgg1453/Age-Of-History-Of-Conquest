package com.nx.aohc.game;

public class PlayerSlot {

    public String playerId;
    public String name;
    public String countryId;
    public boolean local;
    public boolean defeated;

    public PlayerSlot(String playerId, String name, String countryId, boolean local) {
        this.playerId = playerId;
        this.name = name;
        this.countryId = countryId;
        this.local = local;
    }
}

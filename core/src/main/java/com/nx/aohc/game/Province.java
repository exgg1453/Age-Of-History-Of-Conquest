package com.nx.aohc.game;

public class Province {

    public final int id;
    public final String name;
    public final String originalCountry;
    public final int pixelCount;
    public final float centroidX;
    public final float centroidY;
    public final String terrain;
    public final int[] neighbours;

    public String owner;
    public int population;
    public int economy;
    public int army;

    public Province(int id, String name, String originalCountry, int pixelCount, float centroidX, float centroidY, String terrain, int[] neighbours) {
        this.id = id;
        this.name = name;
        this.originalCountry = originalCountry;
        this.pixelCount = pixelCount;
        this.centroidX = centroidX;
        this.centroidY = centroidY;
        this.terrain = terrain;
        this.neighbours = neighbours;
        this.owner = originalCountry;
    }
}

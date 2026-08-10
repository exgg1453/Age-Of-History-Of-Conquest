package com.nx.aohc.game;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.utils.IntArray;

public class Country {

    public final String id;
    public String name;
    public Color color;
    public long population;
    public long economy;
    public long gold;
    public long manpower;
    public long incomePerTurn;
    public long manpowerPerTurn;
    public int capitalProvince;
    public String government = "republic";
    public String religion = "secular";
    public boolean playerControlled;
    public final IntArray ownedProvinces = new IntArray();

    public Country(String id, String name, Color color) {
        this.id = id;
        this.name = name;
        this.color = color;
        this.capitalProvince = 0;
        this.playerControlled = false;
    }

    public boolean isAlive() {
        return ownedProvinces.size > 0;
    }
}

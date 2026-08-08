package com.nx.aohc.game;

import com.badlogic.gdx.utils.ObjectIntMap;
import com.badlogic.gdx.utils.ObjectMap;

public class Diplomacy {

    public static final int STATE_PEACE = 0;
    public static final int STATE_WAR = 1;
    public static final int STATE_TRUCE = 2;
    public static final int STATE_ALLIANCE = 3;

    private static final int TRUCE_LENGTH = 6;

    private final ObjectIntMap<String> states = new ObjectIntMap<String>();
    private final ObjectIntMap<String> truceExpiry = new ObjectIntMap<String>();
    private final ObjectMap<String, ObjectIntMap<String>> warExhaustion = new ObjectMap<String, ObjectIntMap<String>>();

    private static String pairKey(String first, String second) {
        if (first.compareTo(second) <= 0) {
            return first + "|" + second;
        }
        return second + "|" + first;
    }

    public int getState(String first, String second) {
        if (first == null || second == null || first.equals(second)) {
            return STATE_PEACE;
        }
        return states.get(pairKey(first, second), STATE_PEACE);
    }

    public boolean isAtWar(String first, String second) {
        return getState(first, second) == STATE_WAR;
    }

    public boolean isAllied(String first, String second) {
        return getState(first, second) == STATE_ALLIANCE;
    }

    public boolean canDeclareWar(String first, String second) {
        if (first == null || second == null || first.equals(second)) {
            return false;
        }
        int state = getState(first, second);
        return state == STATE_PEACE;
    }

    public void declareWar(String first, String second) {
        if (!canDeclareWar(first, second)) {
            return;
        }
        states.put(pairKey(first, second), STATE_WAR);
        setExhaustion(first, second, 0);
        setExhaustion(second, first, 0);
    }

    public void makePeace(String first, String second, int currentTurn) {
        if (first == null || second == null) {
            return;
        }
        String key = pairKey(first, second);
        states.put(key, STATE_TRUCE);
        truceExpiry.put(key, currentTurn + TRUCE_LENGTH);
        setExhaustion(first, second, 0);
        setExhaustion(second, first, 0);
    }

    public void formAlliance(String first, String second) {
        if (first == null || second == null || first.equals(second)) {
            return;
        }
        if (isAtWar(first, second)) {
            return;
        }
        states.put(pairKey(first, second), STATE_ALLIANCE);
    }

    public void breakAlliance(String first, String second) {
        if (first == null || second == null) {
            return;
        }
        String key = pairKey(first, second);
        if (states.get(key, STATE_PEACE) == STATE_ALLIANCE) {
            states.put(key, STATE_PEACE);
        }
    }

    public void expireTruces(int currentTurn) {
        ObjectIntMap.Keys<String> keys = truceExpiry.keys();
        com.badlogic.gdx.utils.Array<String> expired = new com.badlogic.gdx.utils.Array<String>();
        while (keys.hasNext()) {
            String key = keys.next();
            if (truceExpiry.get(key, 0) <= currentTurn) {
                expired.add(key);
            }
        }
        for (int index = 0; index < expired.size; index++) {
            String key = expired.get(index);
            truceExpiry.remove(key, 0);
            if (states.get(key, STATE_PEACE) == STATE_TRUCE) {
                states.put(key, STATE_PEACE);
            }
        }
    }

    public int getExhaustion(String owner, String against) {
        ObjectIntMap<String> table = warExhaustion.get(owner);
        if (table == null) {
            return 0;
        }
        return table.get(against, 0);
    }

    public void addExhaustion(String owner, String against, int amount) {
        setExhaustion(owner, against, getExhaustion(owner, against) + amount);
    }

    private void setExhaustion(String owner, String against, int value) {
        ObjectIntMap<String> table = warExhaustion.get(owner);
        if (table == null) {
            table = new ObjectIntMap<String>();
            warExhaustion.put(owner, table);
        }
        table.put(against, Math.max(0, value));
    }

    public String stateKey(int state) {
        switch (state) {
            case STATE_WAR:
                return "diplomacy.war";
            case STATE_TRUCE:
                return "diplomacy.truce";
            case STATE_ALLIANCE:
                return "diplomacy.alliance";
            default:
                return "diplomacy.peace";
        }
    }
}

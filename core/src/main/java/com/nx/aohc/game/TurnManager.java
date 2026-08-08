package com.nx.aohc.game;

import com.badlogic.gdx.utils.IntMap;

public class TurnManager {

    public static final int ACTION_INVALID = 0;
    public static final int ACTION_REINFORCED = 1;
    public static final int ACTION_MOVED = 2;
    public static final int ACTION_CAPTURED = 3;
    public static final int ACTION_REPELLED = 4;

    public static class ActionResult {
        public int type = ACTION_INVALID;
        public int attackerLosses;
        public int defenderLosses;
        public String messageKey = "action.invalid";
        public String previousOwner;
    }

    public static final int RECRUIT_BATCH = 1000;
    public static final int RECRUIT_GOLD_COST = 20;
    public static final int RECRUIT_MANPOWER_COST = 1000;

    private final GameState gameState;

    public TurnManager(GameState gameState) {
        this.gameState = gameState;
    }

    public void initialiseCountries() {
        IntMap<Province> provinces = gameState.getProvinceMap().getProvinces();
        for (IntMap.Entry<Province> entry : provinces.entries()) {
            Province province = entry.value;
            if (province.owner == null) {
                continue;
            }
            if (province.army == 0) {
                province.army = Math.max(200, province.pixelCount * 4);
            }
            province.defenceBonus = 1.25f;
            province.hasActedThisTurn = false;
        }

        for (int index = 0; index < gameState.getCountryList().size; index++) {
            Country country = gameState.getCountryList().get(index);
            recomputeIncome(country);
            country.gold = country.incomePerTurn * 3;
            country.manpower = country.manpowerPerTurn * 3;
        }
    }

    public void recomputeIncome(Country country) {
        country.incomePerTurn = Math.max(10, country.economy / 8);
        country.manpowerPerTurn = Math.max(500, country.population / 900);
    }

    public boolean canRecruit(Country country, Province province) {
        if (country == null || province == null) {
            return false;
        }
        if (!country.id.equals(province.owner)) {
            return false;
        }
        return country.gold >= RECRUIT_GOLD_COST && country.manpower >= RECRUIT_MANPOWER_COST;
    }

    public boolean recruit(Country country, Province province) {
        if (!canRecruit(country, province)) {
            return false;
        }
        country.gold -= RECRUIT_GOLD_COST;
        country.manpower -= RECRUIT_MANPOWER_COST;
        province.army += RECRUIT_BATCH;
        return true;
    }

    public boolean areAdjacent(Province origin, Province target) {
        if (origin == null || target == null) {
            return false;
        }
        for (int index = 0; index < origin.neighbours.length; index++) {
            if (origin.neighbours[index] == target.id) {
                return true;
            }
        }
        return false;
    }

    public ActionResult performAction(Country actingCountry, Province origin, Province target) {
        ActionResult result = new ActionResult();

        if (actingCountry == null || origin == null || target == null) {
            return result;
        }
        if (!actingCountry.id.equals(origin.owner)) {
            result.messageKey = "action.notYourProvince";
            return result;
        }
        if (origin.hasActedThisTurn) {
            result.messageKey = "action.alreadyActed";
            return result;
        }
        if (origin.army <= 0) {
            result.messageKey = "action.noTroops";
            return result;
        }
        if (!areAdjacent(origin, target)) {
            result.messageKey = "action.notAdjacent";
            return result;
        }

        if (actingCountry.id.equals(target.owner)) {
            target.army += origin.army;
            origin.army = 0;
            origin.hasActedThisTurn = true;
            result.type = ACTION_REINFORCED;
            result.messageKey = "action.reinforced";
            return result;
        }

        CombatResolver.CombatResult combat = CombatResolver.resolve(origin.army, target.army, target.defenceBonus);
        result.attackerLosses = combat.attackerLosses;
        result.defenderLosses = combat.defenderLosses;
        result.previousOwner = target.owner;

        origin.army = 0;
        origin.hasActedThisTurn = true;

        if (combat.provinceCaptured) {
            target.army = combat.survivingAttackers;
            gameState.transferProvince(target.id, actingCountry.id);
            target.hasActedThisTurn = true;
            result.type = ACTION_CAPTURED;
            result.messageKey = "action.captured";
        } else {
            target.army = combat.survivingDefenders;
            result.type = ACTION_REPELLED;
            result.messageKey = "action.repelled";
        }

        return result;
    }

    public void endTurn() {
        IntMap<Province> provinces = gameState.getProvinceMap().getProvinces();
        for (IntMap.Entry<Province> entry : provinces.entries()) {
            entry.value.hasActedThisTurn = false;
        }

        gameState.recomputeCountryStatistics();

        for (int index = 0; index < gameState.getCountryList().size; index++) {
            Country country = gameState.getCountryList().get(index);
            recomputeIncome(country);
            country.gold += country.incomePerTurn;
            country.manpower += country.manpowerPerTurn;
        }

        gameState.advanceTurn();
    }
}

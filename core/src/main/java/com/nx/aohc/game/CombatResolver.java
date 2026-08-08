package com.nx.aohc.game;

import com.badlogic.gdx.math.MathUtils;

public class CombatResolver {

    public static class CombatResult {
        public boolean provinceCaptured;
        public int attackerLosses;
        public int defenderLosses;
        public int survivingAttackers;
        public int survivingDefenders;
    }

    private static final float MINIMUM_ATTACKER_SURVIVAL = 0.10f;
    private static final float RANDOM_SPREAD = 0.18f;

    public static CombatResult resolve(int attackingTroops, int defendingTroops, float defenceBonus) {
        CombatResult result = new CombatResult();

        if (attackingTroops <= 0) {
            result.survivingDefenders = defendingTroops;
            return result;
        }

        if (defendingTroops <= 0) {
            result.provinceCaptured = true;
            result.survivingAttackers = attackingTroops;
            return result;
        }

        float attackerRoll = 1f + MathUtils.random(-RANDOM_SPREAD, RANDOM_SPREAD);
        float defenderRoll = 1f + MathUtils.random(-RANDOM_SPREAD, RANDOM_SPREAD);

        float attackerPower = attackingTroops * attackerRoll;
        float defenderPower = defendingTroops * defenceBonus * defenderRoll;

        if (attackerPower > defenderPower) {
            float ratio = defenderPower / attackerPower;
            int losses = Math.round(attackingTroops * ratio * 0.85f);
            int survivors = Math.max(Math.round(attackingTroops * MINIMUM_ATTACKER_SURVIVAL), attackingTroops - losses);

            result.provinceCaptured = true;
            result.attackerLosses = attackingTroops - survivors;
            result.defenderLosses = defendingTroops;
            result.survivingAttackers = survivors;
            result.survivingDefenders = 0;
        } else {
            float ratio = attackerPower / defenderPower;
            int defenderLosses = Math.round(defendingTroops * ratio * 0.70f);

            result.provinceCaptured = false;
            result.attackerLosses = attackingTroops;
            result.defenderLosses = defenderLosses;
            result.survivingAttackers = 0;
            result.survivingDefenders = Math.max(1, defendingTroops - defenderLosses);
        }

        return result;
    }
}

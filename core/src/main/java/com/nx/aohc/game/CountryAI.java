package com.nx.aohc.game;

import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.IntArray;
import com.badlogic.gdx.utils.ObjectMap;

public class CountryAI {

    public static class TurnReport {
        public int warsDeclaredOnPlayer;
        public int peaceOffersAcceptedByPlayer;
        public int provincesTakenFromPlayer;
        public int provincesLostToPlayer;
        public final Array<String> attackersOnPlayer = new Array<String>();
    }

    private static final float ATTACK_STRENGTH_MARGIN = 1.35f;
    private static final float WAR_DECLARATION_MARGIN = 1.6f;
    private static final int MAXIMUM_WARS = 3;
    private static final int PEACE_EXHAUSTION_THRESHOLD = 4;
    private static final float GOLD_RESERVE_FRACTION = 0.25f;

    private final GameState gameState;
    private final TurnManager turnManager;
    private final Diplomacy diplomacy;

    private final IntArray borderProvinces = new IntArray();
    private final IntArray candidateTargets = new IntArray();

    public CountryAI(GameState gameState, TurnManager turnManager, Diplomacy diplomacy) {
        this.gameState = gameState;
        this.turnManager = turnManager;
        this.diplomacy = diplomacy;
    }

    public TurnReport runAllCountries() {
        TurnReport report = new TurnReport();
        Country player = gameState.getPlayerCountry();
        String playerId = player != null ? player.id : null;

        Array<Country> countries = new Array<Country>(gameState.getCountryList());
        for (int index = 0; index < countries.size; index++) {
            Country country = countries.get(index);
            if (country.playerControlled || !country.isAlive()) {
                continue;
            }
            runCountry(country, playerId, report);
        }

        gameState.removeDeadCountries();
        return report;
    }

    private void runCountry(Country country, String playerId, TurnReport report) {
        collectBorderProvinces(country);
        if (borderProvinces.size == 0) {
            spendGoldOnDefence(country);
            return;
        }

        handleDiplomacy(country, playerId, report);
        spendGoldOnDefence(country);
        performMilitaryMoves(country, playerId, report);
    }

    private void collectBorderProvinces(Country country) {
        borderProvinces.clear();
        for (int index = 0; index < country.ownedProvinces.size; index++) {
            Province province = gameState.getProvinceMap().getProvince(country.ownedProvinces.get(index));
            if (province == null) {
                continue;
            }
            if (isBorderProvince(country, province)) {
                borderProvinces.add(province.id);
            }
        }
    }

    private boolean isBorderProvince(Country country, Province province) {
        for (int index = 0; index < province.neighbours.length; index++) {
            Province neighbour = gameState.getProvinceMap().getProvince(province.neighbours[index]);
            if (neighbour == null) {
                continue;
            }
            if (neighbour.owner == null || !neighbour.owner.equals(country.id)) {
                return true;
            }
        }
        return false;
    }

    private void handleDiplomacy(Country country, String playerId, TurnReport report) {
        int activeWars = countActiveWars(country);

        for (int index = 0; index < borderProvinces.size; index++) {
            Province province = gameState.getProvinceMap().getProvince(borderProvinces.get(index));
            if (province == null) {
                continue;
            }
            for (int neighbourIndex = 0; neighbourIndex < province.neighbours.length; neighbourIndex++) {
                Province neighbour = gameState.getProvinceMap().getProvince(province.neighbours[neighbourIndex]);
                if (neighbour == null || neighbour.owner == null || neighbour.owner.equals(country.id)) {
                    continue;
                }

                Country other = gameState.getCountry(neighbour.owner);
                if (other == null) {
                    continue;
                }

                if (diplomacy.isAtWar(country.id, other.id)) {
                    considerPeace(country, other, playerId, report);
                    continue;
                }

                if (activeWars >= MAXIMUM_WARS) {
                    continue;
                }
                if (!diplomacy.canDeclareWar(country.id, other.id)) {
                    continue;
                }

                float ownStrength = estimateStrength(country);
                float otherStrength = estimateStrength(other);
                if (ownStrength < otherStrength * WAR_DECLARATION_MARGIN) {
                    continue;
                }
                if (MathUtils.random() > 0.35f) {
                    continue;
                }

                diplomacy.declareWar(country.id, other.id);
                activeWars++;

                if (other.id.equals(playerId)) {
                    report.warsDeclaredOnPlayer++;
                    if (!report.attackersOnPlayer.contains(country.name, false)) {
                        report.attackersOnPlayer.add(country.name);
                    }
                }
            }
        }
    }

    public boolean considerPlayerAllianceOffer(Country aiCountry, Country player) {
        if (aiCountry == null || player == null) {
            return false;
        }
        if (diplomacy.isAtWar(aiCountry.id, player.id) || diplomacy.isAllied(aiCountry.id, player.id)) {
            return false;
        }

        float aiStrength = estimateStrength(aiCountry);
        float playerStrength = estimateStrength(player);
        if (playerStrength < aiStrength * 0.55f) {
            return false;
        }
        if (countActiveWars(player) > MAXIMUM_WARS) {
            return false;
        }
        if (MathUtils.random() > 0.55f) {
            return false;
        }

        diplomacy.formAlliance(aiCountry.id, player.id);
        return true;
    }

    public boolean considerPlayerPeaceOffer(Country aiCountry, Country player) {
        if (aiCountry == null || player == null) {
            return false;
        }
        if (!diplomacy.isAtWar(aiCountry.id, player.id)) {
            return false;
        }

        float aiStrength = estimateStrength(aiCountry);
        float playerStrength = estimateStrength(player);
        int exhaustion = diplomacy.getExhaustion(aiCountry.id, player.id);

        float acceptanceScore = playerStrength / Math.max(1f, aiStrength) + exhaustion * 0.12f;
        if (acceptanceScore < 1.1f) {
            return false;
        }

        diplomacy.makePeace(aiCountry.id, player.id, gameState.getTurnNumber());
        return true;
    }

    private void considerPeace(Country country, Country other, String playerId, TurnReport report) {
        int exhaustion = diplomacy.getExhaustion(country.id, other.id);
        if (exhaustion < PEACE_EXHAUSTION_THRESHOLD) {
            return;
        }
        float ownStrength = estimateStrength(country);
        float otherStrength = estimateStrength(other);
        if (ownStrength > otherStrength * 0.9f) {
            return;
        }
        if (MathUtils.random() > 0.4f) {
            return;
        }

        diplomacy.makePeace(country.id, other.id, gameState.getTurnNumber());
        if (other.id.equals(playerId)) {
            report.peaceOffersAcceptedByPlayer++;
        }
    }

    private int countActiveWars(Country country) {
        int wars = 0;
        ObjectMap<String, Country> countries = gameState.getCountries();
        for (ObjectMap.Entry<String, Country> entry : countries) {
            if (diplomacy.isAtWar(country.id, entry.key)) {
                wars++;
            }
        }
        return wars;
    }

    private float estimateStrength(Country country) {
        long troops = 0;
        for (int index = 0; index < country.ownedProvinces.size; index++) {
            Province province = gameState.getProvinceMap().getProvince(country.ownedProvinces.get(index));
            if (province != null) {
                troops += province.army;
            }
        }
        return troops + country.manpower * 0.35f + country.gold * 8f;
    }

    private void spendGoldOnDefence(Country country) {
        int reserve = (int) (country.incomePerTurn * GOLD_RESERVE_FRACTION);
        int guard = 0;

        while (country.gold > reserve && guard < 12) {
            Province weakest = findWeakestBorderProvince(country);
            if (weakest == null) {
                break;
            }
            if (!turnManager.recruit(country, weakest)) {
                break;
            }
            guard++;
        }
    }

    private Province findWeakestBorderProvince(Country country) {
        Province weakest = null;
        IntArray source = borderProvinces.size > 0 ? borderProvinces : country.ownedProvinces;
        for (int index = 0; index < source.size; index++) {
            Province province = gameState.getProvinceMap().getProvince(source.get(index));
            if (province == null) {
                continue;
            }
            if (weakest == null || province.army < weakest.army) {
                weakest = province;
            }
        }
        return weakest;
    }

    private void performMilitaryMoves(Country country, String playerId, TurnReport report) {
        for (int index = 0; index < borderProvinces.size; index++) {
            Province origin = gameState.getProvinceMap().getProvince(borderProvinces.get(index));
            if (origin == null || origin.hasActedThisTurn || origin.army <= 0) {
                continue;
            }
            if (!country.id.equals(origin.owner)) {
                continue;
            }

            Province target = selectAttackTarget(country, origin);
            if (target == null) {
                continue;
            }

            String defenderId = target.owner;
            TurnManager.ActionResult result = turnManager.performAction(country, origin, target);

            if (result.type == TurnManager.ACTION_CAPTURED) {
                diplomacy.addExhaustion(defenderId, country.id, 2);
                if (defenderId != null && defenderId.equals(playerId)) {
                    report.provincesTakenFromPlayer++;
                }
            } else if (result.type == TurnManager.ACTION_REPELLED) {
                diplomacy.addExhaustion(country.id, defenderId, 2);
            }
        }

        reinforceFrontLine(country);
    }

    private Province selectAttackTarget(Country country, Province origin) {
        candidateTargets.clear();
        Province best = null;
        float bestScore = 0f;

        for (int index = 0; index < origin.neighbours.length; index++) {
            Province neighbour = gameState.getProvinceMap().getProvince(origin.neighbours[index]);
            if (neighbour == null) {
                continue;
            }
            if (neighbour.owner != null && neighbour.owner.equals(country.id)) {
                continue;
            }
            if (neighbour.owner != null && !diplomacy.isAtWar(country.id, neighbour.owner)) {
                continue;
            }

            float defence = neighbour.army * neighbour.defenceBonus;
            if (neighbour.owner != null && origin.army < defence * ATTACK_STRENGTH_MARGIN) {
                continue;
            }

            float score = neighbour.economy + neighbour.population * 0.001f - defence * 0.5f;
            if (best == null || score > bestScore) {
                best = neighbour;
                bestScore = score;
            }
        }
        return best;
    }

    private void reinforceFrontLine(Country country) {
        for (int index = 0; index < country.ownedProvinces.size; index++) {
            Province province = gameState.getProvinceMap().getProvince(country.ownedProvinces.get(index));
            if (province == null || province.hasActedThisTurn || province.army <= 0) {
                continue;
            }
            if (isBorderProvince(country, province)) {
                continue;
            }

            Province destination = null;
            for (int neighbourIndex = 0; neighbourIndex < province.neighbours.length; neighbourIndex++) {
                Province neighbour = gameState.getProvinceMap().getProvince(province.neighbours[neighbourIndex]);
                if (neighbour == null || neighbour.owner == null || !neighbour.owner.equals(country.id)) {
                    continue;
                }
                if (!isBorderProvince(country, neighbour)) {
                    continue;
                }
                if (destination == null || neighbour.army < destination.army) {
                    destination = neighbour;
                }
            }

            if (destination != null) {
                turnManager.performAction(country, province, destination);
            }
        }
    }
}

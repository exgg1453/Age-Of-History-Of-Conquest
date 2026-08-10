package com.nx.aohc.game;

import com.nx.aohc.map.ProvinceMap;

public class RelationCalculator {

    public static class RelationBreakdown {
        public int powerScore;
        public int distanceScore;
        public int governmentScore;
        public int religionScore;
        public int difficultyScore;
        public int total;
        public double distanceKilometres;
        public String weakestFactorKey = "relation.factor.none";
    }

    public static final int ACCEPTANCE_THRESHOLD = 50;
    private static final int BASE_SCORE = 45;
    private static final double EARTH_RADIUS_KILOMETRES = 6371.0;

    private final GameState gameState;
    private final GameSettings settings;

    public RelationCalculator(GameState gameState, GameSettings settings) {
        this.gameState = gameState;
        this.settings = settings;
    }

    public RelationBreakdown evaluate(Country first, Country second) {
        RelationBreakdown breakdown = new RelationBreakdown();
        if (first == null || second == null) {
            return breakdown;
        }

        breakdown.powerScore = scorePower(first, second);
        breakdown.distanceKilometres = distanceBetweenCapitals(first, second);
        breakdown.distanceScore = scoreDistance(breakdown.distanceKilometres);
        breakdown.governmentScore = scoreGovernment(first.government, second.government);
        breakdown.religionScore = scoreReligion(first.religion, second.religion);
        breakdown.difficultyScore = settings.getAllianceDifficultyModifier();

        breakdown.total = BASE_SCORE
                + breakdown.powerScore
                + breakdown.distanceScore
                + breakdown.governmentScore
                + breakdown.religionScore
                + breakdown.difficultyScore;

        breakdown.weakestFactorKey = findWeakestFactor(breakdown);
        return breakdown;
    }

    private String findWeakestFactor(RelationBreakdown breakdown) {
        int worst = 0;
        String key = "relation.factor.none";

        if (breakdown.powerScore < worst) {
            worst = breakdown.powerScore;
            key = "relation.factor.power";
        }
        if (breakdown.distanceScore < worst) {
            worst = breakdown.distanceScore;
            key = "relation.factor.distance";
        }
        if (breakdown.governmentScore < worst) {
            worst = breakdown.governmentScore;
            key = "relation.factor.government";
        }
        if (breakdown.religionScore < worst) {
            worst = breakdown.religionScore;
            key = "relation.factor.religion";
        }
        return key;
    }

    private int scorePower(Country first, Country second) {
        double firstPower = estimatePower(first);
        double secondPower = estimatePower(second);
        if (firstPower <= 0 || secondPower <= 0) {
            return -20;
        }

        double ratio = Math.min(firstPower, secondPower) / Math.max(firstPower, secondPower);
        return (int) Math.round(ratio * 45.0 - 25.0);
    }

    private double estimatePower(Country country) {
        double troops = 0;
        for (int index = 0; index < country.ownedProvinces.size; index++) {
            Province province = gameState.getProvinceMap().getProvince(country.ownedProvinces.get(index));
            if (province != null) {
                troops += province.army;
            }
        }
        return troops + country.economy * 0.6 + country.ownedProvinces.size * 250.0;
    }

    public double distanceBetweenCapitals(Country first, Country second) {
        Province firstCapital = resolveCapital(first);
        Province secondCapital = resolveCapital(second);
        if (firstCapital == null || secondCapital == null) {
            return 12000.0;
        }

        ProvinceMap map = gameState.getProvinceMap();
        double firstLongitude = Math.toRadians(firstCapital.centroidX / map.getWidth() * 360.0 - 180.0);
        double firstLatitude = Math.toRadians(90.0 - firstCapital.centroidY / map.getHeight() * 180.0);
        double secondLongitude = Math.toRadians(secondCapital.centroidX / map.getWidth() * 360.0 - 180.0);
        double secondLatitude = Math.toRadians(90.0 - secondCapital.centroidY / map.getHeight() * 180.0);

        double deltaLatitude = secondLatitude - firstLatitude;
        double deltaLongitude = secondLongitude - firstLongitude;

        double a = Math.sin(deltaLatitude / 2.0) * Math.sin(deltaLatitude / 2.0)
                + Math.cos(firstLatitude) * Math.cos(secondLatitude)
                * Math.sin(deltaLongitude / 2.0) * Math.sin(deltaLongitude / 2.0);
        double c = 2.0 * Math.atan2(Math.sqrt(a), Math.sqrt(1.0 - a));
        return EARTH_RADIUS_KILOMETRES * c;
    }

    private Province resolveCapital(Country country) {
        Province capital = gameState.getProvinceMap().getProvince(country.capitalProvince);
        if (capital != null && country.id.equals(capital.owner)) {
            return capital;
        }
        if (country.ownedProvinces.size == 0) {
            return null;
        }
        return gameState.getProvinceMap().getProvince(country.ownedProvinces.get(0));
    }

    private int scoreDistance(double kilometres) {
        if (kilometres <= 1500.0) {
            return 10;
        }
        double penalty = (kilometres - 1500.0) / 1000.0 * 5.0;
        return (int) Math.round(10.0 - Math.min(45.0, penalty));
    }

    private int scoreGovernment(String first, String second) {
        if (first == null || second == null) {
            return 0;
        }
        if (first.equals(second)) {
            return 16;
        }

        if (isOpposed(first, second)) {
            return -28;
        }
        if (isCompatible(first, second)) {
            return 4;
        }
        return -10;
    }

    private boolean isOpposed(String first, String second) {
        return pairMatches(first, second, "fascism", "communism")
                || pairMatches(first, second, "fascism", "democracy")
                || pairMatches(first, second, "communism", "democracy")
                || pairMatches(first, second, "theocracy", "communism")
                || pairMatches(first, second, "theocracy", "democracy");
    }

    private boolean isCompatible(String first, String second) {
        return pairMatches(first, second, "democracy", "republic")
                || pairMatches(first, second, "monarchy", "republic")
                || pairMatches(first, second, "authoritarian", "communism")
                || pairMatches(first, second, "authoritarian", "monarchy")
                || pairMatches(first, second, "authoritarian", "theocracy")
                || pairMatches(first, second, "authoritarian", "fascism");
    }

    private boolean pairMatches(String first, String second, String left, String right) {
        return (first.equals(left) && second.equals(right)) || (first.equals(right) && second.equals(left));
    }

    private int scoreReligion(String first, String second) {
        if (first == null || second == null) {
            return 0;
        }
        if (first.equals(second)) {
            return 14;
        }
        if ("secular".equals(first) || "secular".equals(second)) {
            return -2;
        }

        String firstFamily = religionFamily(first);
        String secondFamily = religionFamily(second);
        if (firstFamily.equals(secondFamily)) {
            return 5;
        }
        return -14;
    }

    private String religionFamily(String religion) {
        if (religion.endsWith("islam")) {
            return "islam";
        }
        if ("catholic".equals(religion) || "protestant".equals(religion) || "orthodox".equals(religion)) {
            return "christian";
        }
        if ("hinduism".equals(religion) || "buddhism".equals(religion)) {
            return "dharmic";
        }
        return religion;
    }
}

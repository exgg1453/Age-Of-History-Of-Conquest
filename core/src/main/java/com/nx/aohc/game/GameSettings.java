package com.nx.aohc.game;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Preferences;

public class GameSettings {

    public static final int DIFFICULTY_EASY = 0;
    public static final int DIFFICULTY_NORMAL = 1;
    public static final int DIFFICULTY_HARD = 2;
    public static final int DIFFICULTY_IMPOSSIBLE = 3;

    public static final int MINIMUM_AGGRESSION = 1;
    public static final int MAXIMUM_AGGRESSION = 100;
    public static final int DEFAULT_AGGRESSION = 50;

    private static final String PREFERENCES_NAME = "aohc-gameplay";
    private static final String KEY_DIFFICULTY = "difficulty";
    private static final String KEY_AGGRESSION = "aggression";

    private final Preferences preferences;

    private int difficulty;
    private int aiAggression;

    public GameSettings() {
        this.preferences = Gdx.app.getPreferences(PREFERENCES_NAME);
        this.difficulty = preferences.getInteger(KEY_DIFFICULTY, DIFFICULTY_NORMAL);
        this.aiAggression = clampAggression(preferences.getInteger(KEY_AGGRESSION, DEFAULT_AGGRESSION));
    }

    private int clampAggression(int value) {
        return Math.max(MINIMUM_AGGRESSION, Math.min(MAXIMUM_AGGRESSION, value));
    }

    public int getDifficulty() {
        return difficulty;
    }

    public void setDifficulty(int value) {
        difficulty = Math.max(DIFFICULTY_EASY, Math.min(DIFFICULTY_IMPOSSIBLE, value));
        preferences.putInteger(KEY_DIFFICULTY, difficulty);
        preferences.flush();
    }

    public int cycleDifficulty() {
        int next = difficulty + 1;
        if (next > DIFFICULTY_IMPOSSIBLE) {
            next = DIFFICULTY_EASY;
        }
        setDifficulty(next);
        return difficulty;
    }

    public String getDifficultyKey() {
        switch (difficulty) {
            case DIFFICULTY_EASY:
                return "difficulty.easy";
            case DIFFICULTY_HARD:
                return "difficulty.hard";
            case DIFFICULTY_IMPOSSIBLE:
                return "difficulty.impossible";
            default:
                return "difficulty.normal";
        }
    }

    public int getAiAggression() {
        return aiAggression;
    }

    public void setAiAggression(int value) {
        aiAggression = clampAggression(value);
        preferences.putInteger(KEY_AGGRESSION, aiAggression);
        preferences.flush();
    }

    public void adjustAiAggression(int delta) {
        setAiAggression(aiAggression + delta);
    }

    public int getAllianceDifficultyModifier() {
        switch (difficulty) {
            case DIFFICULTY_EASY:
                return 25;
            case DIFFICULTY_HARD:
                return -20;
            case DIFFICULTY_IMPOSSIBLE:
                return -45;
            default:
                return 0;
        }
    }

    public float getAiIncomeMultiplier() {
        switch (difficulty) {
            case DIFFICULTY_EASY:
                return 0.75f;
            case DIFFICULTY_HARD:
                return 1.35f;
            case DIFFICULTY_IMPOSSIBLE:
                return 1.8f;
            default:
                return 1f;
        }
    }

    public float getAiCombatMultiplier() {
        switch (difficulty) {
            case DIFFICULTY_EASY:
                return 0.85f;
            case DIFFICULTY_HARD:
                return 1.15f;
            case DIFFICULTY_IMPOSSIBLE:
                return 1.35f;
            default:
                return 1f;
        }
    }

    public float getWarDeclarationMargin() {
        float normalised = (aiAggression - MINIMUM_AGGRESSION) / (float) (MAXIMUM_AGGRESSION - MINIMUM_AGGRESSION);
        return 2.4f - normalised * 1.35f;
    }

    public float getWarDeclarationChance() {
        float normalised = (aiAggression - MINIMUM_AGGRESSION) / (float) (MAXIMUM_AGGRESSION - MINIMUM_AGGRESSION);
        return 0.05f + normalised * 0.65f;
    }

    public float getAttackStrengthMargin() {
        float normalised = (aiAggression - MINIMUM_AGGRESSION) / (float) (MAXIMUM_AGGRESSION - MINIMUM_AGGRESSION);
        return 1.75f - normalised * 0.65f;
    }

    public int getMaximumSimultaneousWars() {
        return 1 + Math.round(aiAggression / 22f);
    }
}

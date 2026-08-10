package com.nx.aohc.achievement;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Preferences;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.ObjectMap;

import com.nx.aohc.formable.Formable;
import com.nx.aohc.localization.Localization;

public class AchievementManager {

    public interface UnlockListener {
        void onAchievementUnlocked(Achievement achievement);
    }

    public static final String WELCOME = "WELCOME";
    public static final String FIRST_CONQUEST = "FIRST_CONQUEST";
    public static final String FIRST_PEACE = "FIRST_PEACE";
    public static final String FIRST_ALLIANCE = "FIRST_ALLIANCE";
    public static final String WORLD_CONQUEST = "WORLD_CONQUEST";
    public static final String FORMABLE_PREFIX = "FORM_";

    private static final String PREFERENCES_NAME = "aohc-achievements";

    private final Preferences preferences;
    private final Localization localization;
    private final Array<Achievement> achievements = new Array<Achievement>();
    private final ObjectMap<String, Achievement> byId = new ObjectMap<String, Achievement>();

    private UnlockListener listener;

    public AchievementManager(Localization localization) {
        this.localization = localization;
        this.preferences = Gdx.app.getPreferences(PREFERENCES_NAME);
    }

    public void register(Array<Achievement> definitions) {
        for (int index = 0; index < definitions.size; index++) {
            Achievement achievement = definitions.get(index);
            if (achievement.id == null || byId.containsKey(achievement.id)) {
                continue;
            }
            achievements.add(achievement);
            byId.put(achievement.id, achievement);
        }
    }

    public void registerFormables(Array<Formable> formables) {
        for (int index = 0; index < formables.size; index++) {
            Formable formable = formables.get(index);
            String identifier = FORMABLE_PREFIX + formable.id;
            if (byId.containsKey(identifier)) {
                continue;
            }

            Achievement achievement = new Achievement();
            achievement.id = identifier;
            achievement.sourceMod = formable.sourceMod;
            achievement.localizedNames.put("en", formable.getDisplayName("en"));
            achievement.localizedNames.put("tr", formable.getDisplayName("tr"));
            achievement.localizedDescriptions.put("en",
                    "Proclaim " + formable.getDisplayName("en") + ".");
            achievement.localizedDescriptions.put("tr",
                    formable.getDisplayName("tr") + " devletini ilan et.");

            achievements.add(achievement);
            byId.put(identifier, achievement);
        }
    }

    public boolean isUnlocked(String id) {
        return preferences.getBoolean(id, false);
    }

    public boolean unlock(String id) {
        if (id == null || isUnlocked(id)) {
            return false;
        }
        Achievement achievement = byId.get(id);
        if (achievement == null) {
            return false;
        }

        preferences.putBoolean(id, true);
        preferences.flush();

        if (listener != null) {
            listener.onAchievementUnlocked(achievement);
        }
        return true;
    }

    public boolean unlockFormable(String formableId) {
        return unlock(FORMABLE_PREFIX + formableId);
    }

    public void resetAll() {
        for (int index = 0; index < achievements.size; index++) {
            preferences.remove(achievements.get(index).id);
        }
        preferences.flush();
    }

    public int getUnlockedCount() {
        int count = 0;
        for (int index = 0; index < achievements.size; index++) {
            if (isUnlocked(achievements.get(index).id)) {
                count++;
            }
        }
        return count;
    }

    public Array<Achievement> getAchievements() {
        return achievements;
    }

    public Localization getLocalization() {
        return localization;
    }

    public void setUnlockListener(UnlockListener listener) {
        this.listener = listener;
    }
}

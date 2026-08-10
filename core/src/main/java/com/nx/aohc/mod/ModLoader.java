package com.nx.aohc.mod;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.JsonReader;
import com.badlogic.gdx.utils.JsonValue;

import com.nx.aohc.achievement.Achievement;
import com.nx.aohc.achievement.AchievementLoader;
import com.nx.aohc.formable.Formable;
import com.nx.aohc.formable.FormableLoader;
import com.nx.aohc.localization.Localization;
import com.nx.aohc.scenario.Scenario;
import com.nx.aohc.scenario.ScenarioLoader;

public class ModLoader {

    private final Array<Mod> mods = new Array<Mod>();
    private final Array<Scenario> scenarios = new Array<Scenario>();
    private final Array<Formable> formables = new Array<Formable>();
    private final Array<Achievement> achievements = new Array<Achievement>();

    public void loadBaseContent(Localization localization) {
        localization.loadDirectory(Gdx.files.internal("localization"));

        FileHandle scenarioDirectory = Gdx.files.internal("scenarios");
        if (scenarioDirectory.exists()) {
            for (FileHandle file : scenarioDirectory.list(".json")) {
                try {
                    scenarios.add(ScenarioLoader.load(file, "base"));
                } catch (Exception exception) {
                    Gdx.app.error("ModLoader", "Failed to load base scenario " + file.name(), exception);
                }
            }
        }

        FileHandle formableDirectory = Gdx.files.internal("formables");
        if (formableDirectory.exists()) {
            for (FileHandle file : formableDirectory.list(".json")) {
                try {
                    formables.addAll(FormableLoader.load(file, "base"));
                } catch (Exception exception) {
                    Gdx.app.error("ModLoader", "Failed to load base formables " + file.name(), exception);
                }
            }
        }

        FileHandle achievementDirectory = Gdx.files.internal("achievements");
        if (achievementDirectory.exists()) {
            for (FileHandle file : achievementDirectory.list(".json")) {
                try {
                    achievements.addAll(AchievementLoader.load(file, "base"));
                } catch (Exception exception) {
                    Gdx.app.error("ModLoader", "Failed to load base achievements " + file.name(), exception);
                }
            }
        }
    }

    public void loadExternalMods(FileHandle modsDirectory, Localization localization) {
        if (modsDirectory == null || !modsDirectory.exists()) {
            return;
        }
        for (FileHandle candidate : modsDirectory.list()) {
            if (!candidate.isDirectory()) {
                continue;
            }
            FileHandle descriptor = candidate.child("mod.json");
            if (!descriptor.exists()) {
                continue;
            }
            try {
                Mod mod = readDescriptor(descriptor, candidate);
                if (!mod.enabled) {
                    continue;
                }
                mods.add(mod);
                localization.loadDirectory(candidate.child("localization"));

                FileHandle scenarioDirectory = candidate.child("scenarios");
                if (scenarioDirectory.exists()) {
                    for (FileHandle file : scenarioDirectory.list(".json")) {
                        try {
                            scenarios.add(ScenarioLoader.load(file, mod.id));
                        } catch (Exception exception) {
                            Gdx.app.error("ModLoader", "Failed to load scenario " + file.name() + " from mod " + mod.id, exception);
                        }
                    }
                }
                FileHandle achievementDirectory = candidate.child("achievements");
                if (achievementDirectory.exists()) {
                    for (FileHandle file : achievementDirectory.list(".json")) {
                        try {
                            achievements.addAll(AchievementLoader.load(file, mod.id));
                        } catch (Exception exception) {
                            Gdx.app.error("ModLoader", "Failed to load achievements " + file.name() + " from mod " + mod.id, exception);
                        }
                    }
                }

                FileHandle formableDirectory = candidate.child("formables");
                if (formableDirectory.exists()) {
                    for (FileHandle file : formableDirectory.list(".json")) {
                        try {
                            formables.addAll(FormableLoader.load(file, mod.id));
                        } catch (Exception exception) {
                            Gdx.app.error("ModLoader", "Failed to load formables " + file.name() + " from mod " + mod.id, exception);
                        }
                    }
                }
            } catch (Exception exception) {
                Gdx.app.error("ModLoader", "Failed to load mod " + candidate.name(), exception);
            }
        }
    }

    private Mod readDescriptor(FileHandle descriptor, FileHandle root) {
        JsonValue value = new JsonReader().parse(descriptor);
        Mod mod = new Mod();
        mod.id = value.getString("id", root.name());
        mod.name = value.getString("name", mod.id);
        mod.author = value.getString("author", "unknown");
        mod.version = value.getString("version", "1.0.0");
        mod.description = value.getString("description", "");
        mod.gameVersion = value.getString("gameVersion", "");
        mod.enabled = value.getBoolean("enabled", true);
        mod.root = root;
        return mod;
    }

    public FileHandle resolveAsset(String relativePath) {
        for (int index = mods.size - 1; index >= 0; index--) {
            FileHandle candidate = mods.get(index).root.child(relativePath);
            if (candidate.exists()) {
                return candidate;
            }
        }
        return Gdx.files.internal(relativePath);
    }

    public Array<Mod> getMods() {
        return mods;
    }

    public Array<Scenario> getScenarios() {
        return scenarios;
    }

    public Array<Formable> getFormables() {
        return formables;
    }

    public Array<Achievement> getAchievements() {
        return achievements;
    }

    public Scenario findScenario(String id) {
        for (int index = 0; index < scenarios.size; index++) {
            if (scenarios.get(index).id.equals(id)) {
                return scenarios.get(index);
            }
        }
        return null;
    }
}

package com.nx.aohc.achievement;

import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.JsonReader;
import com.badlogic.gdx.utils.JsonValue;
import com.badlogic.gdx.utils.ObjectMap;

public class AchievementLoader {

    public static Array<Achievement> load(FileHandle file, String sourceMod) {
        Array<Achievement> results = new Array<Achievement>();
        JsonValue root = new JsonReader().parse(file);
        JsonValue array = root.get("achievements");
        if (array == null) {
            return results;
        }

        for (JsonValue entry = array.child; entry != null; entry = entry.next) {
            Achievement achievement = new Achievement();
            achievement.id = entry.getString("id");
            achievement.sourceMod = sourceMod;
            achievement.hidden = entry.getBoolean("hidden", false);
            readBlock(entry.get("name"), achievement.localizedNames);
            readBlock(entry.get("description"), achievement.localizedDescriptions);
            results.add(achievement);
        }
        return results;
    }

    private static void readBlock(JsonValue value, ObjectMap<String, String> target) {
        if (value == null) {
            return;
        }
        if (value.isString()) {
            target.put("en", value.asString());
            return;
        }
        for (JsonValue entry = value.child; entry != null; entry = entry.next) {
            target.put(entry.name, entry.asString());
        }
    }
}

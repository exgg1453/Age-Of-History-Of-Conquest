package com.nx.aohc.localization;

import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.utils.JsonReader;
import com.badlogic.gdx.utils.JsonValue;
import com.badlogic.gdx.utils.ObjectMap;

public class Localization {

    private static final String FALLBACK_LANGUAGE = "en";

    private final ObjectMap<String, ObjectMap<String, String>> languages = new ObjectMap<String, ObjectMap<String, String>>();
    private String activeLanguage = FALLBACK_LANGUAGE;

    public void loadFile(FileHandle file) {
        if (file == null || !file.exists()) {
            return;
        }
        String languageCode = file.nameWithoutExtension().toLowerCase();
        ObjectMap<String, String> table = languages.get(languageCode);
        if (table == null) {
            table = new ObjectMap<String, String>();
            languages.put(languageCode, table);
        }
        JsonValue root = new JsonReader().parse(file);
        for (JsonValue entry = root.child; entry != null; entry = entry.next) {
            table.put(entry.name, entry.asString());
        }
    }

    public void loadDirectory(FileHandle directory) {
        if (directory == null || !directory.exists()) {
            return;
        }
        for (FileHandle file : directory.list(".json")) {
            loadFile(file);
        }
    }

    public String get(String key) {
        ObjectMap<String, String> table = languages.get(activeLanguage);
        if (table != null) {
            String value = table.get(key);
            if (value != null) {
                return value;
            }
        }
        ObjectMap<String, String> fallback = languages.get(FALLBACK_LANGUAGE);
        if (fallback != null) {
            String value = fallback.get(key);
            if (value != null) {
                return value;
            }
        }
        return key;
    }

    public String format(String key, Object... arguments) {
        String pattern = get(key);
        for (int index = 0; index < arguments.length; index++) {
            pattern = pattern.replace("{" + index + "}", String.valueOf(arguments[index]));
        }
        return pattern;
    }

    public boolean hasLanguage(String code) {
        return languages.containsKey(code);
    }

    public String getActiveLanguage() {
        return activeLanguage;
    }

    public void setActiveLanguage(String code) {
        if (code == null) {
            return;
        }
        String normalized = code.toLowerCase();
        if (languages.containsKey(normalized)) {
            activeLanguage = normalized;
        }
    }

    public com.badlogic.gdx.utils.Array<String> getAvailableLanguages() {
        com.badlogic.gdx.utils.Array<String> result = new com.badlogic.gdx.utils.Array<String>();
        for (ObjectMap.Entry<String, ObjectMap<String, String>> entry : languages) {
            result.add(entry.key);
        }
        result.sort();
        return result;
    }
}

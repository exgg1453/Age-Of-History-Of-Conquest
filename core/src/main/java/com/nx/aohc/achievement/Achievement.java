package com.nx.aohc.achievement;

import com.badlogic.gdx.utils.ObjectMap;

public class Achievement {

    public String id;
    public String sourceMod = "base";
    public boolean hidden;
    public final ObjectMap<String, String> localizedNames = new ObjectMap<String, String>();
    public final ObjectMap<String, String> localizedDescriptions = new ObjectMap<String, String>();

    public String getDisplayName(String language) {
        return resolve(localizedNames, language, id);
    }

    public String getDescription(String language) {
        return resolve(localizedDescriptions, language, "");
    }

    private String resolve(ObjectMap<String, String> table, String language, String fallback) {
        String value = table.get(language);
        if (value != null) {
            return value;
        }
        value = table.get("en");
        return value != null ? value : fallback;
    }
}

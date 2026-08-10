package com.nx.aohc.scenario;

import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.ObjectMap;

public class Scenario {

    public static class ScenarioCountry {
        public String id;
        public final ObjectMap<String, String> localizedNames = new ObjectMap<String, String>();
        public float red = 0.5f;
        public float green = 0.5f;
        public float blue = 0.5f;
        public int capitalProvince;
        public String government;
        public String religion;
        public final Array<Integer> provinces = new Array<Integer>();
        public boolean playable = true;
    }

    public String id = "unnamed";
    public String sourceMod = "base";
    public int startYear;
    public boolean inheritDefaultOwnership;
    public final ObjectMap<String, String> localizedNames = new ObjectMap<String, String>();
    public final ObjectMap<String, String> localizedDescriptions = new ObjectMap<String, String>();
    public final Array<ScenarioCountry> countries = new Array<ScenarioCountry>();

    public String getDisplayName(String language) {
        String value = localizedNames.get(language);
        if (value != null) {
            return value;
        }
        value = localizedNames.get("en");
        return value != null ? value : id;
    }

    public String getDescription(String language) {
        String value = localizedDescriptions.get(language);
        if (value != null) {
            return value;
        }
        value = localizedDescriptions.get("en");
        return value != null ? value : "";
    }
}

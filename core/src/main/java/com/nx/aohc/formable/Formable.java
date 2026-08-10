package com.nx.aohc.formable;

import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.IntArray;
import com.badlogic.gdx.utils.ObjectMap;

public class Formable {

    public String id;
    public String sourceMod = "base";
    public final ObjectMap<String, String> localizedNames = new ObjectMap<String, String>();
    public float red = 0.5f;
    public float green = 0.5f;
    public float blue = 0.5f;
    public final Array<String> allowedCountries = new Array<String>();
    public final IntArray requiredProvinces = new IntArray();
    public int capitalProvince;
    public int goldBonus;
    public int manpowerBonus;

    public String getDisplayName(String language) {
        String value = localizedNames.get(language);
        if (value != null) {
            return value;
        }
        value = localizedNames.get("en");
        return value != null ? value : id;
    }

    public boolean isAllowedFor(String countryId) {
        if (allowedCountries.size == 0) {
            return true;
        }
        return allowedCountries.contains(countryId, false);
    }
}

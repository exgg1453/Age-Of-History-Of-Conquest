package com.nx.aohc.game;

import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.utils.JsonReader;
import com.badlogic.gdx.utils.JsonValue;
import com.badlogic.gdx.utils.ObjectMap;

import com.nx.aohc.map.ProvinceMap;

public class GameAssets {

    private final ProvinceMap provinceMap;
    private final ObjectMap<String, Country> defaultCountries = new ObjectMap<String, Country>();

    public GameAssets(FileHandle provinceRaster, FileHandle provinceMetadata, FileHandle countryFile) {
        this.provinceMap = new ProvinceMap(provinceRaster, provinceMetadata);
        loadDefaultCountries(countryFile);
    }

    private void loadDefaultCountries(FileHandle countryFile) {
        if (countryFile == null || !countryFile.exists()) {
            return;
        }
        JsonValue root = new JsonReader().parse(countryFile);
        JsonValue array = root.get("countries");
        if (array == null) {
            return;
        }
        for (JsonValue entry = array.child; entry != null; entry = entry.next) {
            String id = entry.getString("id");
            String name = entry.getString("name", id);
            JsonValue colorValue = entry.get("color");
            Color color = new Color(0.5f, 0.5f, 0.5f, 1f);
            if (colorValue != null && colorValue.size >= 3) {
                color.set(colorValue.getInt(0) / 255f, colorValue.getInt(1) / 255f, colorValue.getInt(2) / 255f, 1f);
            }
            Country country = new Country(id, name, color);
            country.capitalProvince = entry.getInt("capital", 0);
            country.government = entry.getString("government", "republic");
            country.religion = entry.getString("religion", "secular");
            defaultCountries.put(id, country);
        }
    }

    public ProvinceMap getProvinceMap() {
        return provinceMap;
    }

    public ObjectMap<String, Country> getDefaultCountries() {
        return defaultCountries;
    }

    public void dispose() {
        provinceMap.dispose();
    }
}

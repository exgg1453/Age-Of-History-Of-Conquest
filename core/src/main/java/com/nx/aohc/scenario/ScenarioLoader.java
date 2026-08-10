package com.nx.aohc.scenario;

import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.utils.Json;
import com.badlogic.gdx.utils.JsonReader;
import com.badlogic.gdx.utils.JsonValue;
import com.badlogic.gdx.utils.JsonWriter;

public class ScenarioLoader {

    public static Scenario load(FileHandle file, String sourceMod) {
        JsonValue root = new JsonReader().parse(file);
        Scenario scenario = new Scenario();
        scenario.id = root.getString("id", file.nameWithoutExtension());
        scenario.sourceMod = sourceMod;
        scenario.startYear = root.getInt("startYear", 0);
        scenario.inheritDefaultOwnership = root.getBoolean("inheritDefaultOwnership", false);

        readLocalizedBlock(root.get("name"), scenario.localizedNames);
        readLocalizedBlock(root.get("description"), scenario.localizedDescriptions);

        JsonValue countryArray = root.get("countries");
        if (countryArray != null) {
            for (JsonValue entry = countryArray.child; entry != null; entry = entry.next) {
                Scenario.ScenarioCountry country = new Scenario.ScenarioCountry();
                country.id = entry.getString("id");
                readLocalizedBlock(entry.get("name"), country.localizedNames);

                JsonValue colorValue = entry.get("color");
                if (colorValue != null && colorValue.size >= 3) {
                    country.red = colorValue.getInt(0) / 255f;
                    country.green = colorValue.getInt(1) / 255f;
                    country.blue = colorValue.getInt(2) / 255f;
                }

                country.capitalProvince = entry.getInt("capital", 0);
                country.playable = entry.getBoolean("playable", true);
                country.government = entry.getString("government", null);
                country.religion = entry.getString("religion", null);

                JsonValue provinceValue = entry.get("provinces");
                if (provinceValue != null) {
                    for (int index = 0; index < provinceValue.size; index++) {
                        country.provinces.add(provinceValue.getInt(index));
                    }
                }
                scenario.countries.add(country);
            }
        }
        return scenario;
    }

    private static void readLocalizedBlock(JsonValue value, com.badlogic.gdx.utils.ObjectMap<String, String> target) {
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

    public static void save(Scenario scenario, FileHandle file) {
        Json json = new Json(JsonWriter.OutputType.json);
        StringBuilder builder = new StringBuilder();
        builder.append("{\n");
        builder.append("\"id\":").append(json.toJson(scenario.id)).append(",\n");
        builder.append("\"startYear\":").append(scenario.startYear).append(",\n");
        builder.append("\"inheritDefaultOwnership\":").append(scenario.inheritDefaultOwnership).append(",\n");

        builder.append("\"name\":{");
        appendLocalizedBlock(builder, json, scenario.localizedNames);
        builder.append("},\n");

        builder.append("\"description\":{");
        appendLocalizedBlock(builder, json, scenario.localizedDescriptions);
        builder.append("},\n");

        builder.append("\"countries\":[\n");
        for (int index = 0; index < scenario.countries.size; index++) {
            Scenario.ScenarioCountry country = scenario.countries.get(index);
            builder.append("{");
            builder.append("\"id\":").append(json.toJson(country.id)).append(",");
            builder.append("\"name\":{");
            appendLocalizedBlock(builder, json, country.localizedNames);
            builder.append("},");
            builder.append("\"color\":[")
                    .append(Math.round(country.red * 255f)).append(",")
                    .append(Math.round(country.green * 255f)).append(",")
                    .append(Math.round(country.blue * 255f)).append("],");
            builder.append("\"capital\":").append(country.capitalProvince).append(",");
            builder.append("\"playable\":").append(country.playable).append(",");
            builder.append("\"provinces\":[");
            for (int provinceIndex = 0; provinceIndex < country.provinces.size; provinceIndex++) {
                if (provinceIndex > 0) {
                    builder.append(",");
                }
                builder.append(country.provinces.get(provinceIndex).intValue());
            }
            builder.append("]}");
            if (index < scenario.countries.size - 1) {
                builder.append(",");
            }
            builder.append("\n");
        }
        builder.append("]\n}");
        file.writeString(builder.toString(), false, "UTF-8");
    }

    private static void appendLocalizedBlock(StringBuilder builder, Json json, com.badlogic.gdx.utils.ObjectMap<String, String> values) {
        boolean first = true;
        for (com.badlogic.gdx.utils.ObjectMap.Entry<String, String> entry : values) {
            if (!first) {
                builder.append(",");
            }
            builder.append(json.toJson(entry.key)).append(":").append(json.toJson(entry.value));
            first = false;
        }
    }
}

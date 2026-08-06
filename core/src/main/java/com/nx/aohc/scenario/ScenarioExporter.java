package com.nx.aohc.scenario;

import com.badlogic.gdx.files.FileHandle;

import com.nx.aohc.game.Country;
import com.nx.aohc.game.GameState;

public class ScenarioExporter {

    private static final String USER_MOD_FOLDER = "UserScenarios";

    public static FileHandle export(GameState gameState, String scenarioId, int startYear, FileHandle modsDirectory) {
        if (modsDirectory == null) {
            throw new IllegalStateException("Mods directory is not available on this platform");
        }
        FileHandle modRoot = modsDirectory.child(USER_MOD_FOLDER);
        modRoot.mkdirs();

        FileHandle descriptor = modRoot.child("mod.json");
        if (!descriptor.exists()) {
            descriptor.writeString("{\n"
                    + "\"id\":\"user_scenarios\",\n"
                    + "\"name\":\"User Scenarios\",\n"
                    + "\"author\":\"local\",\n"
                    + "\"version\":\"1.0.0\",\n"
                    + "\"description\":\"Scenarios created with the in game editor\",\n"
                    + "\"enabled\":true\n"
                    + "}", false, "UTF-8");
        }

        FileHandle scenarioDirectory = modRoot.child("scenarios");
        scenarioDirectory.mkdirs();

        Scenario scenario = new Scenario();
        scenario.id = scenarioId;
        scenario.startYear = startYear;
        scenario.inheritDefaultOwnership = false;
        scenario.localizedNames.put("en", scenarioId);
        scenario.localizedNames.put("tr", scenarioId);
        scenario.localizedDescriptions.put("en", "Created with the in game scenario editor");
        scenario.localizedDescriptions.put("tr", "Oyun ici senaryo duzenleyicisi ile olusturuldu");

        for (int index = 0; index < gameState.getCountryList().size; index++) {
            Country country = gameState.getCountryList().get(index);
            if (country.ownedProvinces.size == 0) {
                continue;
            }
            Scenario.ScenarioCountry definition = new Scenario.ScenarioCountry();
            definition.id = country.id;
            definition.localizedNames.put("en", country.name);
            definition.localizedNames.put("tr", country.name);
            definition.red = country.color.r;
            definition.green = country.color.g;
            definition.blue = country.color.b;
            definition.capitalProvince = country.capitalProvince;
            for (int provinceIndex = 0; provinceIndex < country.ownedProvinces.size; provinceIndex++) {
                definition.provinces.add(country.ownedProvinces.get(provinceIndex));
            }
            scenario.countries.add(definition);
        }

        FileHandle output = scenarioDirectory.child(scenarioId + ".json");
        ScenarioLoader.save(scenario, output);
        return output;
    }
}

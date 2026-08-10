package com.nx.aohc.game;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.IntMap;
import com.badlogic.gdx.utils.ObjectMap;

import com.nx.aohc.map.MapRenderer;
import com.nx.aohc.map.ProvinceMap;
import com.nx.aohc.scenario.Scenario;

public class GameState {

    private final ProvinceMap provinceMap;
    private final ObjectMap<String, Country> countries = new ObjectMap<String, Country>();
    private final Array<Country> countryList = new Array<Country>();
    private final Color unclaimedColor = new Color(0.55f, 0.55f, 0.55f, 1f);

    private Scenario scenario;
    private int currentYear;
    private int turnNumber = 1;
    private Country playerCountry;

    public GameState(ProvinceMap provinceMap) {
        this.provinceMap = provinceMap;
    }

    public void applyScenario(Scenario scenario, ObjectMap<String, Country> defaultCountries, String language) {
        this.scenario = scenario;
        this.currentYear = scenario.startYear;
        this.turnNumber = 1;
        countries.clear();
        countryList.clear();

        IntMap<Province> provinces = provinceMap.getProvinces();
        for (IntMap.Entry<Province> entry : provinces.entries()) {
            entry.value.owner = null;
        }

        if (scenario.inheritDefaultOwnership && defaultCountries != null) {
            for (ObjectMap.Entry<String, Country> entry : defaultCountries) {
                Country source = entry.value;
                Country country = new Country(source.id, source.name, new Color(source.color));
                country.capitalProvince = source.capitalProvince;
                country.government = source.government;
                country.religion = source.religion;
                registerCountry(country);
            }
            for (IntMap.Entry<Province> entry : provinces.entries()) {
                Province province = entry.value;
                Country country = countries.get(province.originalCountry);
                if (country != null) {
                    province.owner = country.id;
                    country.ownedProvinces.add(province.id);
                }
            }
        }

        for (Scenario.ScenarioCountry definition : scenario.countries) {
            Country country = countries.get(definition.id);
            String displayName = definition.localizedNames.get(language);
            if (displayName == null) {
                displayName = definition.localizedNames.get("en");
            }
            if (displayName == null) {
                displayName = definition.id;
            }

            if (country == null) {
                country = new Country(definition.id, displayName, new Color(definition.red, definition.green, definition.blue, 1f));
                registerCountry(country);
            } else {
                country.name = displayName;
                country.color.set(definition.red, definition.green, definition.blue, 1f);
            }

            if (definition.provinces.size > 0) {
                for (int index = 0; index < definition.provinces.size; index++) {
                    int provinceId = definition.provinces.get(index);
                    transferProvince(provinceId, country.id);
                }
            }

            if (definition.capitalProvince > 0) {
                country.capitalProvince = definition.capitalProvince;
            }
            if (definition.government != null) {
                country.government = definition.government;
            }
            if (definition.religion != null) {
                country.religion = definition.religion;
            }
        }

        removeEmptyCountries();
        recomputeCountryStatistics();
    }

    private void registerCountry(Country country) {
        countries.put(country.id, country);
        countryList.add(country);
    }

    private void removeEmptyCountries() {
        for (int index = countryList.size - 1; index >= 0; index--) {
            Country country = countryList.get(index);
            if (!country.isAlive()) {
                countryList.removeIndex(index);
                countries.remove(country.id);
            }
        }
    }

    public void transferProvince(int provinceId, String newOwnerId) {
        Province province = provinceMap.getProvince(provinceId);
        if (province == null) {
            return;
        }
        if (province.owner != null) {
            Country previous = countries.get(province.owner);
            if (previous != null) {
                int position = previous.ownedProvinces.indexOf(provinceId);
                if (position >= 0) {
                    previous.ownedProvinces.removeIndex(position);
                }
            }
        }
        province.owner = newOwnerId;
        if (newOwnerId != null) {
            Country target = countries.get(newOwnerId);
            if (target != null && !target.ownedProvinces.contains(provinceId)) {
                target.ownedProvinces.add(provinceId);
            }
        }
    }

    public void removeDeadCountries() {
        removeEmptyCountries();
    }

    public void recomputeCountryStatistics() {
        for (int index = 0; index < countryList.size; index++) {
            Country country = countryList.get(index);
            long population = 0;
            long economy = 0;
            for (int provinceIndex = 0; provinceIndex < country.ownedProvinces.size; provinceIndex++) {
                Province province = provinceMap.getProvince(country.ownedProvinces.get(provinceIndex));
                if (province == null) {
                    continue;
                }
                if (province.population == 0) {
                    province.population = Math.max(1000, province.pixelCount * 140);
                }
                if (province.economy == 0) {
                    province.economy = Math.max(50, province.pixelCount * 3);
                }
                population += province.population;
                economy += province.economy;
            }
            country.population = population;
            country.economy = economy;
        }
    }

    public void paintAll(MapRenderer renderer) {
        IntMap<Province> provinces = provinceMap.getProvinces();
        for (IntMap.Entry<Province> entry : provinces.entries()) {
            Province province = entry.value;
            Country country = province.owner != null ? countries.get(province.owner) : null;
            if (country != null) {
                renderer.setProvinceColor(province.id, country.color);
            } else {
                renderer.setProvinceColor(province.id, unclaimedColor);
            }
        }
    }

    public void paintProvince(MapRenderer renderer, int provinceId) {
        Province province = provinceMap.getProvince(provinceId);
        if (province == null) {
            return;
        }
        Country country = province.owner != null ? countries.get(province.owner) : null;
        renderer.setProvinceColor(provinceId, country != null ? country.color : unclaimedColor);
    }

    public Country getCountry(String id) {
        return countries.get(id);
    }

    public Country getCountryOfProvince(int provinceId) {
        Province province = provinceMap.getProvince(provinceId);
        if (province == null || province.owner == null) {
            return null;
        }
        return countries.get(province.owner);
    }

    public Array<Country> getCountryList() {
        return countryList;
    }

    public ObjectMap<String, Country> getCountries() {
        return countries;
    }

    public ProvinceMap getProvinceMap() {
        return provinceMap;
    }

    public Scenario getScenario() {
        return scenario;
    }

    public int getCurrentYear() {
        return currentYear;
    }

    public int getTurnNumber() {
        return turnNumber;
    }

    public void advanceTurn() {
        turnNumber++;
        currentYear++;
        recomputeCountryStatistics();
    }

    public Country getPlayerCountry() {
        return playerCountry;
    }

    public void setPlayerCountry(Country country) {
        if (playerCountry != null) {
            playerCountry.playerControlled = false;
        }
        playerCountry = country;
        if (country != null) {
            country.playerControlled = true;
        }
    }
}

package com.nx.aohc.net;

import com.badlogic.gdx.utils.IntMap;

import com.nx.aohc.game.Country;
import com.nx.aohc.game.GameState;
import com.nx.aohc.game.Province;

public class StateSnapshot {

    private static final String FIELD = "\u001f";
    private static final String RECORD = "\u001e";
    private static final String SECTION = "\u001d";

    public static String encode(GameState gameState, int turnNumber, int year) {
        StringBuilder builder = new StringBuilder();
        builder.append(turnNumber).append(FIELD).append(year).append(SECTION);

        IntMap<Province> provinces = gameState.getProvinceMap().getProvinces();
        boolean first = true;
        for (IntMap.Entry<Province> entry : provinces.entries()) {
            Province province = entry.value;
            if (province.owner == null) {
                continue;
            }
            if (!first) {
                builder.append(RECORD);
            }
            builder.append(province.id).append(FIELD)
                    .append(province.owner).append(FIELD)
                    .append(province.army);
            first = false;
        }

        builder.append(SECTION);

        first = true;
        for (int index = 0; index < gameState.getCountryList().size; index++) {
            Country country = gameState.getCountryList().get(index);
            if (!first) {
                builder.append(RECORD);
            }
            builder.append(country.id).append(FIELD)
                    .append(country.gold).append(FIELD)
                    .append(country.manpower).append(FIELD)
                    .append(country.name);
            first = false;
        }

        return builder.toString();
    }

    public static void apply(GameState gameState, String payload) {
        String[] sections = payload.split(SECTION, -1);
        if (sections.length < 3) {
            return;
        }

        String[] header = sections[0].split(FIELD, -1);
        if (header.length >= 2) {
            try {
                gameState.setTurnState(Integer.parseInt(header[0]), Integer.parseInt(header[1]));
            } catch (NumberFormatException ignored) {
            }
        }

        IntMap<Province> provinces = gameState.getProvinceMap().getProvinces();
        for (IntMap.Entry<Province> entry : provinces.entries()) {
            entry.value.owner = null;
            entry.value.hasActedThisTurn = false;
        }
        for (int index = 0; index < gameState.getCountryList().size; index++) {
            gameState.getCountryList().get(index).ownedProvinces.clear();
        }

        if (!sections[1].isEmpty()) {
            String[] records = sections[1].split(RECORD, -1);
            for (int index = 0; index < records.length; index++) {
                String[] parts = records[index].split(FIELD, -1);
                if (parts.length < 3) {
                    continue;
                }
                try {
                    int provinceId = Integer.parseInt(parts[0]);
                    Province province = gameState.getProvinceMap().getProvince(provinceId);
                    if (province == null) {
                        continue;
                    }
                    province.army = Integer.parseInt(parts[2]);
                    Country country = gameState.getCountry(parts[1]);
                    if (country != null) {
                        province.owner = country.id;
                        country.ownedProvinces.add(provinceId);
                    }
                } catch (NumberFormatException ignored) {
                }
            }
        }

        if (!sections[2].isEmpty()) {
            String[] records = sections[2].split(RECORD, -1);
            for (int index = 0; index < records.length; index++) {
                String[] parts = records[index].split(FIELD, -1);
                if (parts.length < 4) {
                    continue;
                }
                Country country = gameState.getCountry(parts[0]);
                if (country == null) {
                    continue;
                }
                try {
                    country.gold = Long.parseLong(parts[1]);
                    country.manpower = Long.parseLong(parts[2]);
                } catch (NumberFormatException ignored) {
                }
                country.name = parts[3];
            }
        }

        gameState.removeDeadCountries();
        gameState.recomputeCountryStatistics();
    }
}

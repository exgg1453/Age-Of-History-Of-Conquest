package com.nx.aohc.formable;

import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.ObjectSet;

import com.nx.aohc.game.Country;
import com.nx.aohc.game.GameState;
import com.nx.aohc.game.Province;

public class FormableManager {

    private final GameState gameState;
    private final Array<Formable> formables;
    private final ObjectSet<String> alreadyFormed = new ObjectSet<String>();

    public FormableManager(GameState gameState, Array<Formable> formables) {
        this.gameState = gameState;
        this.formables = formables;
    }

    public boolean isSatisfied(Country country, Formable formable) {
        if (country == null || formable == null) {
            return false;
        }
        if (alreadyFormed.contains(formable.id)) {
            return false;
        }
        if (!formable.isAllowedFor(country.id)) {
            return false;
        }
        if (formable.requiredProvinces.size == 0) {
            return false;
        }

        for (int index = 0; index < formable.requiredProvinces.size; index++) {
            Province province = gameState.getProvinceMap().getProvince(formable.requiredProvinces.get(index));
            if (province == null) {
                continue;
            }
            if (province.owner == null || !province.owner.equals(country.id)) {
                return false;
            }
        }
        return true;
    }

    public int countOwnedRequirements(Country country, Formable formable) {
        int owned = 0;
        for (int index = 0; index < formable.requiredProvinces.size; index++) {
            Province province = gameState.getProvinceMap().getProvince(formable.requiredProvinces.get(index));
            if (province != null && province.owner != null && province.owner.equals(country.id)) {
                owned++;
            }
        }
        return owned;
    }

    public Array<Formable> getCandidates(Country country) {
        Array<Formable> candidates = new Array<Formable>();
        if (country == null) {
            return candidates;
        }
        for (int index = 0; index < formables.size; index++) {
            Formable formable = formables.get(index);
            if (alreadyFormed.contains(formable.id)) {
                continue;
            }
            if (!formable.isAllowedFor(country.id)) {
                continue;
            }
            candidates.add(formable);
        }
        return candidates;
    }

    public Array<Formable> getAvailable(Country country) {
        Array<Formable> available = new Array<Formable>();
        Array<Formable> candidates = getCandidates(country);
        for (int index = 0; index < candidates.size; index++) {
            if (isSatisfied(country, candidates.get(index))) {
                available.add(candidates.get(index));
            }
        }
        return available;
    }

    public boolean form(Country country, Formable formable, String language) {
        if (!isSatisfied(country, formable)) {
            return false;
        }

        country.name = formable.getDisplayName(language);
        country.color.set(formable.red, formable.green, formable.blue, 1f);
        if (formable.capitalProvince > 0) {
            country.capitalProvince = formable.capitalProvince;
        }
        country.gold += formable.goldBonus;
        country.manpower += formable.manpowerBonus;

        alreadyFormed.add(formable.id);
        return true;
    }

    public Array<Formable> getFormables() {
        return formables;
    }
}

package com.nx.aohc.formable;

import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.JsonReader;
import com.badlogic.gdx.utils.JsonValue;

public class FormableLoader {

    public static Array<Formable> load(FileHandle file, String sourceMod) {
        Array<Formable> results = new Array<Formable>();
        JsonValue root = new JsonReader().parse(file);
        JsonValue array = root.get("formables");
        if (array == null) {
            return results;
        }

        for (JsonValue entry = array.child; entry != null; entry = entry.next) {
            Formable formable = new Formable();
            formable.id = entry.getString("id");
            formable.sourceMod = sourceMod;

            JsonValue nameValue = entry.get("name");
            if (nameValue != null) {
                if (nameValue.isString()) {
                    formable.localizedNames.put("en", nameValue.asString());
                } else {
                    for (JsonValue child = nameValue.child; child != null; child = child.next) {
                        formable.localizedNames.put(child.name, child.asString());
                    }
                }
            }

            JsonValue colorValue = entry.get("color");
            if (colorValue != null && colorValue.size >= 3) {
                formable.red = colorValue.getInt(0) / 255f;
                formable.green = colorValue.getInt(1) / 255f;
                formable.blue = colorValue.getInt(2) / 255f;
            }

            JsonValue sourceValue = entry.get("from");
            if (sourceValue != null) {
                for (int index = 0; index < sourceValue.size; index++) {
                    formable.allowedCountries.add(sourceValue.getString(index));
                }
            }

            JsonValue provinceValue = entry.get("requiredProvinces");
            if (provinceValue != null) {
                formable.requiredProvinces.addAll(provinceValue.asIntArray());
            }

            formable.capitalProvince = entry.getInt("capital", 0);
            formable.goldBonus = entry.getInt("goldBonus", 0);
            formable.manpowerBonus = entry.getInt("manpowerBonus", 0);

            results.add(formable);
        }
        return results;
    }
}

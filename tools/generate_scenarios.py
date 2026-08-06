"""
Generates the base scenario files shipped with the game.

Scenarios are plain JSON. Anything produced here can be hand edited, copied into
a mod folder, or rebuilt with different rules. This script exists so the shipped
scenarios stay consistent with whatever map data generate_map.py produced.
"""

import json
import os
import argparse


def load_map_data(map_directory):
    with open(os.path.join(map_directory, "provinces.json"), "r", encoding="utf-8") as handle:
        provinces = json.load(handle)["provinces"]
    with open(os.path.join(map_directory, "countries.json"), "r", encoding="utf-8") as handle:
        countries = json.load(handle)["countries"]
    return provinces, countries


def provinces_of(provinces, country_id):
    return [province["id"] for province in provinces if province["country"] == country_id]


def write_scenario(output_directory, payload):
    path = os.path.join(output_directory, payload["id"] + ".json")
    with open(path, "w", encoding="utf-8") as handle:
        json.dump(payload, handle, ensure_ascii=False, indent=1)
    print("wrote %s" % path)


def build_present_day():
    return {
        "id": "present_day",
        "startYear": 2026,
        "inheritDefaultOwnership": True,
        "name": {"en": "Present Day", "tr": "Günümüz"},
        "description": {
            "en": "The world as it is today. Every recognised state on the map.",
            "tr": "Dünyanın bugünkü hâli. Haritadaki tüm tanınan devletler."
        },
        "countries": []
    }


def build_taiwan_annexation(provinces, countries):
    lookup = {country["id"]: country for country in countries}
    china = lookup["CHN"]
    taiwan_provinces = provinces_of(provinces, "TWN")
    china_provinces = provinces_of(provinces, "CHN")

    return {
        "id": "china_reunification",
        "startYear": 2031,
        "inheritDefaultOwnership": True,
        "name": {"en": "Strait Crisis", "tr": "Boğaz Krizi"},
        "description": {
            "en": "An alternate timeline in which the Taiwan question has been settled by force.",
            "tr": "Tayvan meselesinin güç ile çözüldüğü alternatif bir zaman çizgisi."
        },
        "countries": [
            {
                "id": "CHN",
                "name": {"en": "China", "tr": "Çin"},
                "color": china["color"],
                "capital": china["capital"],
                "provinces": china_provinces + taiwan_provinces
            }
        ]
    }


def build_cold_war_blocs(provinces, countries):
    western = ["USA", "CAN", "GBR", "FRA", "DEU", "ITA", "ESP", "PRT", "NLD", "BEL", "LUX",
               "DNK", "NOR", "ISL", "GRC", "TUR", "JPN", "KOR", "AUS", "NZL"]
    eastern = ["RUS", "POL", "CZE", "SVK", "HUN", "ROU", "BGR", "ALB", "CHN", "PRK",
               "VNM", "CUB", "MNG", "UKR", "BLR", "KAZ", "UZB", "GEO", "ARM", "AZE"]

    lookup = {country["id"]: country for country in countries}
    entries = []

    for country_id in western:
        if country_id not in lookup:
            continue
        entries.append({
            "id": country_id,
            "name": {"en": lookup[country_id]["name"], "tr": lookup[country_id]["name"]},
            "color": [58, 110, 190],
            "capital": lookup[country_id]["capital"],
            "provinces": provinces_of(provinces, country_id)
        })

    for country_id in eastern:
        if country_id not in lookup:
            continue
        entries.append({
            "id": country_id,
            "name": {"en": lookup[country_id]["name"], "tr": lookup[country_id]["name"]},
            "color": [178, 52, 52],
            "capital": lookup[country_id]["capital"],
            "provinces": provinces_of(provinces, country_id)
        })

    return {
        "id": "cold_war_blocs",
        "startYear": 1962,
        "inheritDefaultOwnership": True,
        "name": {"en": "Cold War Blocs", "tr": "Soğuk Savaş Blokları"},
        "description": {
            "en": "A bloc coloured world used as a demonstration of scenario overrides.",
            "tr": "Senaryo geçersiz kılmalarını göstermek için blok renkli dünya."
        },
        "countries": entries
    }


def main():
    parser = argparse.ArgumentParser()
    base = os.path.dirname(__file__)
    parser.add_argument("--map", default=os.path.join(base, "..", "assets", "map"))
    parser.add_argument("--output", default=os.path.join(base, "..", "assets", "scenarios"))
    arguments = parser.parse_args()

    map_directory = os.path.abspath(arguments.map)
    output_directory = os.path.abspath(arguments.output)
    os.makedirs(output_directory, exist_ok=True)

    provinces, countries = load_map_data(map_directory)

    write_scenario(output_directory, build_present_day())
    write_scenario(output_directory, build_taiwan_annexation(provinces, countries))
    write_scenario(output_directory, build_cold_war_blocs(provinces, countries))


if __name__ == "__main__":
    main()

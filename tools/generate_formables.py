"""
Generates the formable nation definitions.

A formable is a nation a country can proclaim once it holds a specific set of
provinces. The requirement lists are built here from the same geographic
selection helpers the scenarios use, so a formable stays consistent with
whatever map data is in place.
"""

import json
import os
import sys
import argparse

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))

from generate_alt_scenarios import MapData


def formable(map_data, identifier, english, turkish, color, sources, provinces,
             gold_bonus=0, manpower_bonus=0, capital=None):
    unique = sorted(set(provinces))
    if not unique:
        return None
    return {
        "id": identifier,
        "name": {"en": english, "tr": turkish},
        "color": color,
        "from": sources,
        "requiredProvinces": unique,
        "capital": capital if capital else map_data.largest(unique),
        "goldBonus": gold_bonus,
        "manpowerBonus": manpower_bonus
    }


def build(map_data):
    pick = lambda *args, **kwargs: map_data.select(*args, exclusive=False, **kwargs)
    entries = []

    entries.append(formable(
        map_data, "GREATER_AZERBAIJAN", "Greater Azerbaijan", "Büyük Azerbaycan",
        [58, 156, 132], ["AZE"],
        pick(["AZE"]) + pick(["IRN"], longitude_range=(44.0, 49.5), latitude_range=(36.0, 39.5)),
        gold_bonus=250, manpower_bonus=30000))

    entries.append(formable(
        map_data, "SAFAVID_EMPIRE", "Safavid Empire", "Safevi İmparatorluğu",
        [140, 74, 148], ["IRN", "AZE"],
        pick(["IRN", "AZE", "ARM"]) + pick(["AFG"], longitude_range=(60.0, 68.0))
        + pick(["IRQ"], longitude_range=(43.0, 49.0)),
        gold_bonus=600, manpower_bonus=80000))

    entries.append(formable(
        map_data, "GREATER_PERSIA", "Greater Persia", "Büyük İran",
        [162, 108, 62], ["IRN"],
        pick(["IRN", "AFG", "TJK", "TKM", "IRQ", "BHR"]),
        gold_bonus=800, manpower_bonus=120000))

    entries.append(formable(
        map_data, "OTTOMAN_EMPIRE_1914", "Ottoman Empire", "Osmanlı İmparatorluğu",
        [32, 112, 72], ["TUR"],
        pick(["TUR", "SYR", "LBN", "JOR", "ISR", "IRQ", "KWT", "YEM"])
        + pick(["SAU"], longitude_range=(34.0, 45.0)),
        gold_bonus=900, manpower_bonus=150000))

    entries.append(formable(
        map_data, "OTTOMAN_EMPIRE_ZENITH", "Ottoman Empire at its Height", "Osmanlı'nın En Geniş Sınırları",
        [24, 92, 58], ["TUR", "OTT"],
        pick(["TUR", "SYR", "LBN", "JOR", "ISR", "IRQ", "KWT", "YEM", "EGY", "LBY",
              "TUN", "DZA", "GRC", "BGR", "MKD", "ALB", "SRB", "MNE", "BIH", "HRV",
              "HUN", "MDA", "ROU", "CYP", "GEO", "ARM"])
        + pick(["SAU"], longitude_range=(34.0, 45.0))
        + pick(["UKR"], latitude_range=(44.0, 48.0)),
        gold_bonus=2000, manpower_bonus=400000))

    entries.append(formable(
        map_data, "TURAN", "Turan", "Turan",
        [22, 130, 96], ["TUR", "AZE", "KAZ", "UZB", "TKM", "KGZ"],
        pick(["TUR", "AZE", "KAZ", "UZB", "TKM", "KGZ", "TJK"])
        + pick(["CHN"], longitude_range=(73.0, 96.0), latitude_range=(35.0, 50.0)),
        gold_bonus=1800, manpower_bonus=350000))

    entries.append(formable(
        map_data, "GERMAN_EMPIRE", "German Empire", "Alman İmparatorluğu",
        [96, 100, 108], ["DEU"],
        pick(["DEU"]) + pick(["POL"], longitude_range=(14.0, 19.5))
        + pick(["FRA"], longitude_range=(6.5, 8.4), latitude_range=(47.4, 49.6)),
        gold_bonus=700, manpower_bonus=110000))

    entries.append(formable(
        map_data, "GREATER_GERMANY", "Greater Germany", "Büyük Almanya",
        [66, 70, 76], ["DEU", "AUT"],
        pick(["DEU", "AUT", "CZE", "SVK", "POL", "NLD", "BEL", "LUX", "CHE", "SVN"]),
        gold_bonus=1600, manpower_bonus=300000))

    entries.append(formable(
        map_data, "ROMAN_EMPIRE", "Roman Empire", "Roma İmparatorluğu",
        [176, 62, 58], ["ITA"],
        pick(["ITA", "ESP", "PRT", "FRA", "GRC", "TUR", "SYR", "LBN", "ISR", "JOR",
              "EGY", "LBY", "TUN", "DZA", "MAR", "HRV", "SVN", "BIH", "SRB", "ALB",
              "MKD", "BGR", "ROU", "HUN", "AUT", "CHE"])
        + pick(["GBR"], latitude_range=(49.0, 55.0)),
        gold_bonus=3000, manpower_bonus=600000))

    entries.append(formable(
        map_data, "SOVIET_UNION", "Soviet Union", "Sovyetler Birliği",
        [172, 46, 46], ["RUS"],
        pick(["RUS", "UKR", "BLR", "MDA", "LTU", "LVA", "EST", "GEO", "ARM", "AZE",
              "KAZ", "UZB", "TKM", "KGZ", "TJK"]),
        gold_bonus=2200, manpower_bonus=450000))

    entries.append(formable(
        map_data, "YUGOSLAVIA", "Yugoslavia", "Yugoslavya",
        [104, 128, 150], ["SRB", "HRV", "BIH", "SVN", "MNE", "MKD"],
        pick(["SRB", "HRV", "BIH", "SVN", "MNE", "MKD"]),
        gold_bonus=300, manpower_bonus=45000))

    entries.append(formable(
        map_data, "GREATER_HUNGARY", "Greater Hungary", "Büyük Macaristan",
        [186, 146, 84], ["HUN"],
        pick(["HUN", "SVK"]) + pick(["ROU"], longitude_range=(21.0, 26.5), latitude_range=(45.0, 48.5))
        + pick(["SRB"], latitude_range=(45.0, 47.0)),
        gold_bonus=350, manpower_bonus=50000))

    entries.append(formable(
        map_data, "ARAB_UNION", "Arab Union", "Arap Birliği",
        [92, 150, 110], ["SAU", "EGY", "IRQ", "SYR", "JOR", "YEM", "OMN", "ARE", "DZA", "MAR"],
        pick(["SAU", "EGY", "IRQ", "SYR", "JOR", "YEM", "OMN", "ARE", "KWT", "QAT",
              "BHR", "LBN", "LBY", "TUN", "DZA", "MAR", "SDN", "MRT"]),
        gold_bonus=2000, manpower_bonus=400000))

    entries.append(formable(
        map_data, "AKHAND_BHARAT", "Greater India", "Büyük Hindistan",
        [206, 140, 62], ["IND"],
        pick(["IND", "PAK", "BGD", "NPL", "BTN", "LKA", "MMR", "AFG"]),
        gold_bonus=2200, manpower_bonus=500000))

    entries.append(formable(
        map_data, "GREATER_FINLAND", "Greater Finland", "Büyük Finlandiya",
        [128, 168, 196], ["FIN"],
        pick(["FIN", "EST"]) + pick(["RUS"], longitude_range=(28.0, 40.0), latitude_range=(60.0, 70.0)),
        gold_bonus=300, manpower_bonus=40000))

    entries.append(formable(
        map_data, "GRAN_COLOMBIA", "Gran Colombia", "Büyük Kolombiya",
        [196, 176, 78], ["COL", "VEN", "ECU", "PAN"],
        pick(["COL", "VEN", "ECU", "PAN"]),
        gold_bonus=500, manpower_bonus=70000))

    return [entry for entry in entries if entry]


def main():
    parser = argparse.ArgumentParser()
    base = os.path.dirname(__file__)
    parser.add_argument("--map", default=os.path.join(base, "..", "assets", "map"))
    parser.add_argument("--output", default=os.path.join(base, "..", "assets", "formables"))
    arguments = parser.parse_args()

    map_directory = os.path.abspath(arguments.map)
    output_directory = os.path.abspath(arguments.output)
    os.makedirs(output_directory, exist_ok=True)

    map_data = MapData(map_directory)
    entries = build(map_data)

    path = os.path.join(output_directory, "base.json")
    with open(path, "w", encoding="utf-8") as handle:
        json.dump({"formables": entries}, handle, ensure_ascii=False, indent=1)

    for entry in entries:
        print("%-26s %4d provinces required" % (entry["id"], len(entry["requiredProvinces"])))
    print("wrote %s" % path)


if __name__ == "__main__":
    main()

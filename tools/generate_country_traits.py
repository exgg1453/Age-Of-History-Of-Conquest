"""
Adds government and religion traits to the generated country table.

These two fields drive the relation score used for alliances. They are written
into countries.json so that a mod can edit them by hand, and any scenario can
override them per country without touching this file.
"""

import json
import os
import argparse


GOVERNMENTS = {
    "monarchy": [
        "SAU", "JOR", "MAR", "OMN", "QAT", "ARE", "KWT", "BHR", "BRN", "SWZ",
        "THA", "BTN", "GBR", "ESP", "SWE", "NOR", "DNK", "NLD", "BEL", "JPN",
        "MYS", "KHM", "LSO", "TON", "LIE", "LUX", "MCO"
    ],
    "communism": ["CHN", "PRK", "CUB", "VNM", "LAO"],
    "theocracy": ["IRN", "AFG", "VAT"],
    "authoritarian": [
        "RUS", "BLR", "TKM", "UZB", "TJK", "AZE", "ERI", "SYR", "SDN", "MMR",
        "ZWE", "GNQ", "TCD", "CAF", "COD", "SSD", "NIC", "VEN", "EGY", "DZA",
        "KAZ", "RWA", "BDI", "CMR", "COG", "GAB", "TGO", "UGA", "ETH", "SOM",
        "LBY", "YEM", "IRQ", "PAK", "BGD", "LKA", "NPL", "MNG", "SRB", "TUR"
    ],
    "democracy": [
        "USA", "CAN", "FRA", "DEU", "ITA", "PRT", "IRL", "AUT", "CHE", "FIN",
        "ISL", "GRC", "POL", "CZE", "SVK", "HUN", "ROU", "BGR", "HRV", "SVN",
        "EST", "LVA", "LTU", "AUS", "NZL", "IND", "IDN", "PHL", "KOR", "TWN",
        "ISR", "ZAF", "BRA", "ARG", "CHL", "URY", "COL", "PER", "MEX", "CRI",
        "PAN", "DOM", "JAM", "GHA", "SEN", "BWA", "NAM", "KEN", "NGA", "TUN",
        "UKR", "GEO", "ARM", "MDA", "ALB", "MKD", "MNE", "BIH", "CYP", "MLT"
    ]
}

RELIGIONS = {
    "sunni_islam": [
        "TUR", "SAU", "EGY", "DZA", "MAR", "TUN", "LBY", "SDN", "SOM", "YEM",
        "JOR", "SYR", "PSE", "KWT", "QAT", "ARE", "OMN", "AFG", "PAK", "BGD",
        "IDN", "MYS", "BRN", "UZB", "TKM", "KGZ", "TJK", "KAZ", "MLI", "NER",
        "TCD", "MRT", "SEN", "GMB", "GIN", "SLE", "BFA", "DJI", "COM", "MDV",
        "XKX", "ALB", "BIH", "NGA", "ESH"
    ],
    "shia_islam": ["IRN", "IRQ", "AZE", "BHR", "LBN"],
    "orthodox": [
        "RUS", "UKR", "BLR", "SRB", "MNE", "MKD", "BGR", "ROU", "GRC", "MDA",
        "GEO", "ARM", "CYP", "ETH", "ERI"
    ],
    "catholic": [
        "ITA", "ESP", "PRT", "FRA", "POL", "IRL", "AUT", "HRV", "SVN", "SVK",
        "HUN", "CZE", "LTU", "BEL", "LUX", "MLT", "PHL", "MEX", "BRA", "ARG",
        "CHL", "COL", "PER", "VEN", "ECU", "BOL", "PRY", "URY", "CRI", "PAN",
        "NIC", "HND", "GTM", "SLV", "DOM", "CUB", "COD", "AGO", "RWA", "BDI",
        "TLS", "VAT"
    ],
    "protestant": [
        "USA", "GBR", "DEU", "NLD", "CHE", "SWE", "NOR", "DNK", "FIN", "ISL",
        "EST", "LVA", "CAN", "AUS", "NZL", "ZAF", "KEN", "UGA", "TZA", "ZMB",
        "ZWE", "MWI", "NAM", "BWA", "GHA", "NGA", "PNG", "JAM", "LBR"
    ],
    "hinduism": ["IND", "NPL", "MUS"],
    "buddhism": ["THA", "MMR", "KHM", "LAO", "LKA", "BTN", "MNG", "JPN", "VNM"],
    "judaism": ["ISR"],
    "secular": ["CHN", "PRK", "KOR", "TWN", "CUB", "EST", "CZE"]
}


def invert(table):
    result = {}
    for value, identifiers in table.items():
        for identifier in identifiers:
            result[identifier] = value
    return result


def main():
    parser = argparse.ArgumentParser()
    base = os.path.dirname(__file__)
    parser.add_argument("--countries", default=os.path.join(base, "..", "assets", "map", "countries.json"))
    arguments = parser.parse_args()

    path = os.path.abspath(arguments.countries)
    with open(path, "r", encoding="utf-8") as handle:
        payload = json.load(handle)

    government_lookup = invert(GOVERNMENTS)
    religion_lookup = invert(RELIGIONS)

    assigned_government = 0
    assigned_religion = 0

    for country in payload["countries"]:
        identifier = country["id"]
        government = government_lookup.get(identifier, "republic")
        religion = religion_lookup.get(identifier, "secular")
        country["government"] = government
        country["religion"] = religion
        if identifier in government_lookup:
            assigned_government += 1
        if identifier in religion_lookup:
            assigned_religion += 1

    with open(path, "w", encoding="utf-8") as handle:
        json.dump(payload, handle, ensure_ascii=False, indent=1)

    total = len(payload["countries"])
    print("countries: %d" % total)
    print("explicit government: %d, defaulted to republic: %d" % (assigned_government, total - assigned_government))
    print("explicit religion: %d, defaulted to secular: %d" % (assigned_religion, total - assigned_religion))


if __name__ == "__main__":
    main()

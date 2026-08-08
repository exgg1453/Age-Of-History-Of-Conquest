"""
Generates the alternate history and historical scenarios.

The shipped province map is built from modern country geometry, so a historical
state is expressed here as a set of modern provinces. Provinces can be selected
whole country at a time, or filtered by the longitude and latitude of their
centroid, which is what makes it possible to split a modern country between
several historical powers without editing the raster.
"""

import json
import os
import argparse


class MapData:

    def __init__(self, map_directory):
        with open(os.path.join(map_directory, "provinces.json"), "r", encoding="utf-8") as handle:
            payload = json.load(handle)
        self.width = payload["width"]
        self.height = payload["height"]
        self.provinces = payload["provinces"]

        with open(os.path.join(map_directory, "countries.json"), "r", encoding="utf-8") as handle:
            self.countries = json.load(handle)["countries"]

        self.by_country = {}
        for province in self.provinces:
            self.by_country.setdefault(province["country"], []).append(province)

        self.claimed = set()

    def longitude(self, province):
        return province["centroidX"] / float(self.width) * 360.0 - 180.0

    def latitude(self, province):
        return 90.0 - province["centroidY"] / float(self.height) * 180.0

    def select(self, country_ids, longitude_range=None, latitude_range=None, exclusive=True):
        selected = []
        for country_id in country_ids:
            for province in self.by_country.get(country_id, []):
                if exclusive and province["id"] in self.claimed:
                    continue
                if longitude_range is not None:
                    longitude = self.longitude(province)
                    if longitude < longitude_range[0] or longitude > longitude_range[1]:
                        continue
                if latitude_range is not None:
                    latitude = self.latitude(province)
                    if latitude < latitude_range[0] or latitude > latitude_range[1]:
                        continue
                selected.append(province["id"])
                if exclusive:
                    self.claimed.add(province["id"])
        return sorted(selected)

    def largest(self, province_ids):
        if not province_ids:
            return 0
        lookup = {province["id"]: province for province in self.provinces}
        best = max(province_ids, key=lambda province_id: lookup[province_id]["pixels"])
        return best

    def reset_claims(self):
        self.claimed = set()


def country_entry(map_data, identifier, english, turkish, color, province_ids, capital=None):
    if not province_ids:
        return None
    return {
        "id": identifier,
        "name": {"en": english, "tr": turkish},
        "color": color,
        "capital": capital if capital else map_data.largest(province_ids),
        "provinces": province_ids
    }


def write_scenario(output_directory, payload):
    entries = [entry for entry in payload["countries"] if entry]
    payload["countries"] = entries
    path = os.path.join(output_directory, payload["id"] + ".json")
    with open(path, "w", encoding="utf-8") as handle:
        json.dump(payload, handle, ensure_ascii=False, indent=1)
    total = sum(len(entry["provinces"]) for entry in entries)
    print("wrote %-28s %3d countries %5d provinces" % (payload["id"], len(entries), total))


def build_ottoman_world_war(map_data):
    map_data.reset_claims()
    select = map_data.select

    ottoman = select(["TUR", "SYR", "LBN", "JOR", "ISR", "IRQ", "KWT", "CYP"])
    ottoman += select(["SAU"], longitude_range=(34.0, 45.0))
    ottoman += select(["YEM"])
    ottoman = sorted(ottoman)

    germany = select(["DEU", "AUT", "CZE", "SVK", "POL", "SVN", "HRV"])
    italy = select(["ITA", "ALB", "LBY", "ERI", "SOM", "ETH"])
    japan = select(["JPN", "KOR", "PRK", "TWN"])

    britain = select(["GBR", "IRL", "IND", "PAK", "BGD", "MMR", "EGY", "SDN", "ZAF", "AUS", "NZL", "CAN"])
    soviet = select(["RUS", "UKR", "BLR", "KAZ", "UZB", "TKM", "KGZ", "TJK", "GEO", "ARM", "AZE", "MDA", "MNG"])
    france = select(["FRA", "BEL", "DZA", "MAR", "TUN", "MLI", "NER", "TCD", "CAF", "COG", "MDG", "VNM", "LAO", "KHM"])
    united_states = select(["USA", "PHL", "MEX"])
    china = select(["CHN"])

    return {
        "id": "world_war_two_ottoman",
        "startYear": 1939,
        "inheritDefaultOwnership": True,
        "name": {
            "en": "The Second World War: An Ottoman Empire That Endured",
            "tr": "İkinci Dünya Savaşı: Ayakta Kalan Osmanlı"
        },
        "description": {
            "en": "An alternate 1939 in which the Ottoman Empire survived the First World War intact and enters the second as a great power in its own right.",
            "tr": "Osmanlı'nın Birinci Dünya Savaşı'ndan bütün çıktığı ve ikinci savaşa kendi başına bir büyük güç olarak girdiği alternatif bir 1939."
        },
        "countries": [
            country_entry(map_data, "OTT", "Ottoman Empire", "Osmanlı İmparatorluğu", [26, 108, 66], ottoman),
            country_entry(map_data, "DEU", "German Reich", "Alman Reich'ı", [88, 92, 100], germany),
            country_entry(map_data, "ITA", "Italy", "İtalya", [110, 130, 118], italy),
            country_entry(map_data, "JPN", "Japan", "Japonya", [178, 60, 72], japan),
            country_entry(map_data, "GBR", "British Empire", "Britanya İmparatorluğu", [180, 60, 50], britain),
            country_entry(map_data, "RUS", "Soviet Union", "Sovyetler Birliği", [168, 44, 44], soviet),
            country_entry(map_data, "FRA", "France", "Fransa", [58, 96, 176], france),
            country_entry(map_data, "USA", "United States", "Amerika Birleşik Devletleri", [72, 132, 196], united_states),
            country_entry(map_data, "CHN", "China", "Çin", [206, 168, 62], china),
        ]
    }


def build_hot_war(map_data):
    map_data.reset_claims()
    select = map_data.select

    western = select([
        "USA", "CAN", "GBR", "FRA", "DEU", "ITA", "ESP", "PRT", "NLD", "BEL", "LUX",
        "DNK", "NOR", "ISL", "GRC", "TUR", "JPN", "KOR", "AUS", "NZL", "IRL", "AUT",
        "CHE", "SWE", "FIN", "MEX", "BRA", "ARG", "CHL", "COL", "PER", "VEN", "PHL",
        "THA", "PAK", "IRN", "SAU", "ISR", "ZAF", "MAR", "TUN"
    ])

    eastern = select([
        "RUS", "POL", "CZE", "SVK", "HUN", "ROU", "BGR", "ALB", "CHN", "PRK",
        "VNM", "CUB", "MNG", "UKR", "BLR", "KAZ", "UZB", "TKM", "KGZ", "TJK",
        "GEO", "ARM", "AZE", "MDA", "SRB", "HRV", "BIH", "SVN", "MKD", "MNE",
        "LTU", "LVA", "EST", "LAO", "KHM", "AGO", "ETH", "SYR", "IRQ", "LBY", "YEM"
    ])

    neutral = select([
        "IND", "IDN", "EGY", "NGA", "DZA", "SDN", "MMR", "LKA", "NPL", "AFG",
        "BGD", "GHA", "KEN", "TZA", "COD", "ZMB", "ZWE", "MLI", "NER", "TCD",
        "SEN", "CIV", "CMR", "UGA", "MOZ", "MDG", "BOL", "ECU", "PRY", "URY"
    ])

    return {
        "id": "hot_war",
        "startYear": 1962,
        "inheritDefaultOwnership": True,
        "name": {"en": "Hot War", "tr": "Sıcak Savaş"},
        "description": {
            "en": "October 1962. The crisis was not defused. Two blocs stand as single war machines and the non aligned world is caught between them.",
            "tr": "Ekim 1962. Kriz yatıştırılamadı. İki blok tek bir savaş makinesi hâlinde karşı karşıya, bağlantısızlar ise arada kaldı."
        },
        "countries": [
            country_entry(map_data, "WST", "Western Bloc", "Batı Bloğu", [52, 104, 190], western),
            country_entry(map_data, "EST", "Eastern Bloc", "Doğu Bloğu", [176, 48, 48], eastern),
            country_entry(map_data, "NAM", "Non Aligned Movement", "Bağlantısızlar", [212, 168, 72], neutral),
        ]
    }


def build_turan(map_data):
    map_data.reset_claims()
    select = map_data.select

    turan = select(["TUR", "AZE", "TKM", "UZB", "KGZ", "TJK", "CYP"])
    turan += select(["KAZ"])
    turan += select(["CHN"], longitude_range=(73.0, 96.0), latitude_range=(35.0, 50.0))
    turan += select(["AFG"], latitude_range=(34.5, 39.0))
    turan += select(["IRN"], latitude_range=(36.0, 40.0), longitude_range=(44.0, 56.0))
    turan += select(["GEO"], longitude_range=(45.0, 47.0))
    turan = sorted(set(turan))

    russia = select(["RUS", "UKR", "BLR", "MDA"])
    persia = select(["IRN"])
    british_raj = select(["IND", "PAK", "BGD", "MMR", "LKA"])
    china = select(["CHN"])
    afghanistan = select(["AFG"])
    caucasus = select(["GEO", "ARM"])

    return {
        "id": "turan_1923",
        "startYear": 1923,
        "inheritDefaultOwnership": True,
        "name": {"en": "Turan, 1923", "tr": "Turan, 1923"},
        "description": {
            "en": "Enver Pasha's Turkestan campaign succeeded. A single Turkic and Muslim state stretches from Anatolia to the edge of Chinese Turkestan, wedged between a Soviet Russia and a British India that both want it gone.",
            "tr": "Enver Paşa'nın Türkistan seferi başarıya ulaştı. Anadolu'dan Doğu Türkistan sınırına uzanan tek bir Türk ve Müslüman devlet, onu ortadan kaldırmak isteyen Sovyet Rusya ile Britanya Hindistanı arasında sıkışmış durumda."
        },
        "countries": [
            country_entry(map_data, "TRN", "Turan", "Turan", [22, 120, 92], turan),
            country_entry(map_data, "RUS", "Soviet Russia", "Sovyet Rusya", [172, 46, 46], russia),
            country_entry(map_data, "IRN", "Persia", "İran", [140, 96, 52], persia),
            country_entry(map_data, "GBR", "British Raj", "Britanya Hindistanı", [176, 62, 52], british_raj),
            country_entry(map_data, "CHN", "Republic of China", "Çin Cumhuriyeti", [206, 170, 64], china),
            country_entry(map_data, "AFG", "Afghanistan", "Afganistan", [118, 108, 74], afghanistan),
            country_entry(map_data, "GEO", "Caucasus Republics", "Kafkas Cumhuriyetleri", [128, 84, 140], caucasus),
        ]
    }


def build_hundred_years_war(map_data):
    map_data.reset_claims()
    select = map_data.select

    scotland = select(["GBR"], latitude_range=(54.7, 61.0))
    england = select(["GBR"], latitude_range=(49.0, 54.7))
    england += select(["FRA"], longitude_range=(-2.0, 1.5), latitude_range=(42.5, 46.0))
    england += select(["FRA"], longitude_range=(-2.0, 1.0), latitude_range=(48.6, 50.5))
    england = sorted(set(england))

    ireland = select(["IRL", "GBR"])

    brittany = select(["FRA"], longitude_range=(-5.5, -1.5), latitude_range=(46.8, 49.0))
    burgundy = select(["FRA"], longitude_range=(3.5, 8.5), latitude_range=(45.5, 50.5))
    burgundy += select(["BEL", "NLD", "LUX"])
    burgundy = sorted(set(burgundy))

    france = select(["FRA"])

    granada = select(["ESP"], latitude_range=(35.0, 38.0))
    aragon = select(["ESP"], longitude_range=(-1.0, 4.5), latitude_range=(38.0, 43.5))
    navarre = select(["ESP"], longitude_range=(-3.0, -0.5), latitude_range=(41.8, 43.5))
    castile = select(["ESP"])
    portugal = select(["PRT"])

    holy_roman = select(["DEU", "AUT", "CHE", "CZE", "SVN"])
    papal = select(["ITA"], latitude_range=(41.0, 45.0))
    naples = select(["ITA"], latitude_range=(35.0, 41.0))
    milan_venice = select(["ITA"])
    hungary = select(["HUN", "SVK", "HRV", "BIH", "SRB"])
    poland = select(["POL", "LTU", "LVA", "EST", "BLR"])
    ottoman = select(["TUR", "GRC", "BGR", "ALB", "MKD", "MNE"])

    return {
        "id": "hundred_years_war",
        "startYear": 1337,
        "inheritDefaultOwnership": False,
        "name": {"en": "The Hundred Years' War", "tr": "Yüz Yıl Savaşları"},
        "description": {
            "en": "1337. Edward III claims the French crown. England holds Gascony and Normandy, Scotland stands with France, and Burgundy waits to see which way to lean.",
            "tr": "1337. III. Edward Fransa tacında hak iddia ediyor. İngiltere Gaskonya ve Normandiya'yı elinde tutuyor, İskoçya Fransa'nın yanında, Burgonya ise hangi tarafa yaslanacağını bekliyor."
        },
        "countries": [
            country_entry(map_data, "ENG", "Kingdom of England", "İngiltere Krallığı", [186, 58, 52], england),
            country_entry(map_data, "SCO", "Kingdom of Scotland", "İskoçya Krallığı", [66, 108, 176], scotland),
            country_entry(map_data, "IRE", "Irish Lordships", "İrlanda Beylikleri", [76, 138, 90], ireland),
            country_entry(map_data, "FRA", "Kingdom of France", "Fransa Krallığı", [76, 110, 196], france),
            country_entry(map_data, "BRI", "Duchy of Brittany", "Bretanya Dukalığı", [128, 156, 190], brittany),
            country_entry(map_data, "BUR", "Duchy of Burgundy", "Burgonya Dukalığı", [196, 150, 62], burgundy),
            country_entry(map_data, "CAS", "Crown of Castile", "Kastilya Krallığı", [188, 132, 74], castile),
            country_entry(map_data, "ARA", "Crown of Aragon", "Aragon Krallığı", [200, 176, 80], aragon),
            country_entry(map_data, "GRA", "Emirate of Granada", "Gırnata Emirliği", [64, 138, 116], granada),
            country_entry(map_data, "NAV", "Kingdom of Navarre", "Navarra Krallığı", [162, 116, 152], navarre),
            country_entry(map_data, "POR", "Kingdom of Portugal", "Portekiz Krallığı", [104, 148, 96], portugal),
            country_entry(map_data, "HRE", "Holy Roman Empire", "Kutsal Roma İmparatorluğu", [128, 128, 136], holy_roman),
            country_entry(map_data, "PAP", "Papal States", "Papalık Devleti", [210, 200, 186], papal),
            country_entry(map_data, "NAP", "Kingdom of Naples", "Napoli Krallığı", [176, 118, 98], naples),
            country_entry(map_data, "ITA", "Italian City States", "İtalyan Şehir Devletleri", [142, 160, 172], milan_venice),
            country_entry(map_data, "HUN", "Kingdom of Hungary", "Macaristan Krallığı", [156, 96, 76], hungary),
            country_entry(map_data, "POL", "Poland and Lithuania", "Polonya ve Litvanya", [178, 74, 118], poland),
            country_entry(map_data, "OTT", "Ottoman Beylik", "Osmanlı Beyliği", [46, 118, 74], ottoman),
        ]
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

    map_data = MapData(map_directory)

    write_scenario(output_directory, build_ottoman_world_war(map_data))
    write_scenario(output_directory, build_hot_war(map_data))
    write_scenario(output_directory, build_turan(map_data))
    write_scenario(output_directory, build_hundred_years_war(map_data))


if __name__ == "__main__":
    main()

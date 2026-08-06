"""
Map generator for Age Of History Of Conquest.

Produces:
    assets/map/provinces.png   - province index raster (R = index low byte, G = index high byte)
    assets/map/provinces.json  - province metadata (id, name, country, centroid, area, neighbours)
    assets/map/countries.json  - country definitions with colors

Index 0 is reserved for sea.

The default source is the bundled Natural Earth low resolution country dataset.
Modders can replace the source with any polygon dataset that exposes a country
name field and re-run this script to build a completely different world map.
"""

import json
import os
import sys
import math
import argparse
import colorsys

import numpy as np
from PIL import Image, ImageDraw
from scipy.spatial import cKDTree


DEFAULT_WIDTH = 4096
DEFAULT_HEIGHT = 2048
TARGET_PROVINCE_COUNT = 3600
MIN_PROVINCES_PER_COUNTRY = 1
MAX_PROVINCES_PER_COUNTRY = 220


def load_countries(source_path):
    import geopandas

    if source_path is None:
        import warnings
        with warnings.catch_warnings():
            warnings.simplefilter("ignore")
            source_path = geopandas.datasets.get_path("naturalearth_lowres")

    frame = geopandas.read_file(source_path)
    records = []
    for _, row in frame.iterrows():
        name = row.get("name") or row.get("NAME") or row.get("admin")
        if name is None:
            continue
        iso = row.get("iso_a3") or row.get("ISO_A3") or ""
        if iso in (None, "-99", ""):
            iso = "".join(ch for ch in str(name).upper() if ch.isalpha())[:3]
        population = row.get("pop_est") or 0
        gdp = row.get("gdp_md_est") or 0
        records.append({
            "name": str(name),
            "iso": str(iso),
            "population": float(population),
            "gdp": float(gdp),
            "geometry": row["geometry"],
        })
    return records


def project(longitude, latitude, width, height):
    x = (longitude + 180.0) / 360.0 * width
    y = (90.0 - latitude) / 180.0 * height
    return x, y


def polygon_to_pixels(polygon, width, height):
    exterior = [project(x, y, width, height) for x, y in polygon.exterior.coords]
    holes = []
    for interior in polygon.interiors:
        holes.append([project(x, y, width, height) for x, y in interior.coords])
    return exterior, holes


def rasterize_countries(records, width, height):
    country_raster = np.zeros((height, width), dtype=np.int32)
    for country_index, record in enumerate(records, start=1):
        geometry = record["geometry"]
        if geometry is None or geometry.is_empty:
            continue
        parts = list(geometry.geoms) if geometry.geom_type == "MultiPolygon" else [geometry]
        layer = Image.new("I", (width, height), 0)
        drawer = ImageDraw.Draw(layer)
        for part in parts:
            exterior, holes = polygon_to_pixels(part, width, height)
            if len(exterior) < 3:
                continue
            drawer.polygon(exterior, fill=country_index)
            for hole in holes:
                if len(hole) >= 3:
                    drawer.polygon(hole, fill=0)
        layer_array = np.array(layer, dtype=np.int32)
        mask = layer_array > 0
        country_raster[mask] = country_index
    return country_raster


def allocate_province_counts(records, country_raster):
    areas = {}
    for country_index in range(1, len(records) + 1):
        areas[country_index] = int(np.count_nonzero(country_raster == country_index))
    total_area = sum(areas.values())
    if total_area == 0:
        raise RuntimeError("Rasterization produced no land pixels")

    def allocate_with(multiplier):
        result = {}
        for country_index, area in areas.items():
            if area == 0:
                result[country_index] = 0
                continue
            share = area / total_area
            count = int(round(math.pow(share, 0.62) * multiplier))
            count = max(MIN_PROVINCES_PER_COUNTRY, min(MAX_PROVINCES_PER_COUNTRY, count))
            result[country_index] = count
        return result

    low = 0.01
    high = 5000.0
    allocation = allocate_with(high)
    for _ in range(60):
        middle = (low + high) / 2.0
        allocation = allocate_with(middle)
        total = sum(allocation.values())
        if total > TARGET_PROVINCE_COUNT:
            high = middle
        else:
            low = middle
    allocation = allocate_with((low + high) / 2.0)
    return allocation, areas


def seed_provinces(coordinates, count, rng):
    if count <= 1 or len(coordinates) <= count:
        return coordinates[rng.choice(len(coordinates), size=min(count, len(coordinates)), replace=False)]

    sample_size = min(len(coordinates), 20000)
    sample = coordinates[rng.choice(len(coordinates), size=sample_size, replace=False)]

    seeds = [sample[rng.integers(0, sample_size)]]
    distances = np.full(sample_size, np.inf)
    for _ in range(count - 1):
        latest = seeds[-1]
        delta = sample - latest
        current = np.einsum("ij,ij->i", delta, delta)
        distances = np.minimum(distances, current)
        total = distances.sum()
        if total <= 0:
            seeds.append(sample[rng.integers(0, sample_size)])
            continue
        probability = distances / total
        seeds.append(sample[rng.choice(sample_size, p=probability)])

    seeds = np.array(seeds, dtype=np.float64)

    for _ in range(6):
        tree = cKDTree(seeds)
        _, assignment = tree.query(sample, k=1)
        for seed_index in range(len(seeds)):
            members = sample[assignment == seed_index]
            if len(members) > 0:
                seeds[seed_index] = members.mean(axis=0)
    return seeds


def build_provinces(records, country_raster, allocation, width, height, rng):
    province_raster = np.zeros((height, width), dtype=np.int32)
    provinces = []
    next_index = 1

    for country_index, record in enumerate(records, start=1):
        count = allocation.get(country_index, 0)
        if count <= 0:
            continue
        pixels = np.argwhere(country_raster == country_index)
        if len(pixels) == 0:
            continue

        coordinates = pixels.astype(np.float64)
        seeds = seed_provinces(coordinates, count, rng)
        if len(seeds) == 0:
            continue

        tree = cKDTree(seeds)
        _, assignment = tree.query(coordinates, k=1)

        local_to_global = {}
        for local_index in range(len(seeds)):
            member_mask = assignment == local_index
            member_count = int(np.count_nonzero(member_mask))
            if member_count == 0:
                continue
            local_to_global[local_index] = next_index
            members = pixels[member_mask]
            centroid_y = float(members[:, 0].mean())
            centroid_x = float(members[:, 1].mean())
            provinces.append({
                "id": next_index,
                "name": "%s %d" % (record["name"], len(local_to_global)),
                "country": record["iso"],
                "pixels": member_count,
                "centroidX": round(centroid_x, 2),
                "centroidY": round(centroid_y, 2),
                "terrain": "plains",
                "neighbours": [],
            })
            next_index += 1

        for local_index, global_index in local_to_global.items():
            member_mask = assignment == local_index
            members = pixels[member_mask]
            province_raster[members[:, 0], members[:, 1]] = global_index

    return province_raster, provinces


def compute_neighbours(province_raster, provinces):
    lookup = {province["id"]: province for province in provinces}
    neighbour_sets = {province["id"]: set() for province in provinces}

    left = province_raster[:, :-1]
    right = province_raster[:, 1:]
    horizontal = np.argwhere((left != right) & (left > 0) & (right > 0))
    for y, x in horizontal:
        a = int(left[y, x])
        b = int(right[y, x])
        neighbour_sets[a].add(b)
        neighbour_sets[b].add(a)

    top = province_raster[:-1, :]
    bottom = province_raster[1:, :]
    vertical = np.argwhere((top != bottom) & (top > 0) & (bottom > 0))
    for y, x in vertical:
        a = int(top[y, x])
        b = int(bottom[y, x])
        neighbour_sets[a].add(b)
        neighbour_sets[b].add(a)

    for province_id, neighbours in neighbour_sets.items():
        lookup[province_id]["neighbours"] = sorted(neighbours)


def country_color(index, total):
    hue = (index * 0.61803398875) % 1.0
    saturation = 0.45 + 0.30 * ((index * 7) % 5) / 4.0
    value = 0.62 + 0.28 * ((index * 3) % 4) / 3.0
    red, green, blue = colorsys.hsv_to_rgb(hue, saturation, value)
    return [int(red * 255), int(green * 255), int(blue * 255)]


def write_province_image(province_raster, path):
    height, width = province_raster.shape
    image_array = np.zeros((height, width, 3), dtype=np.uint8)
    image_array[:, :, 0] = (province_raster & 0xFF).astype(np.uint8)
    image_array[:, :, 1] = ((province_raster >> 8) & 0xFF).astype(np.uint8)
    image_array[:, :, 2] = np.where(province_raster > 0, 255, 0).astype(np.uint8)
    Image.fromarray(image_array, mode="RGB").save(path, optimize=True)


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--source", default=None)
    parser.add_argument("--width", type=int, default=DEFAULT_WIDTH)
    parser.add_argument("--height", type=int, default=DEFAULT_HEIGHT)
    parser.add_argument("--output", default=os.path.join(os.path.dirname(__file__), "..", "assets", "map"))
    parser.add_argument("--seed", type=int, default=20240607)
    arguments = parser.parse_args()

    output_directory = os.path.abspath(arguments.output)
    os.makedirs(output_directory, exist_ok=True)

    rng = np.random.default_rng(arguments.seed)

    print("Loading country geometry")
    records = load_countries(arguments.source)
    print("Loaded %d countries" % len(records))

    print("Rasterizing at %dx%d" % (arguments.width, arguments.height))
    country_raster = rasterize_countries(records, arguments.width, arguments.height)

    allocation, areas = allocate_province_counts(records, country_raster)
    print("Building provinces")
    province_raster, provinces = build_provinces(records, country_raster, allocation, arguments.width, arguments.height, rng)
    print("Created %d provinces" % len(provinces))

    print("Computing adjacency")
    compute_neighbours(province_raster, provinces)

    countries = []
    for country_index, record in enumerate(records, start=1):
        color = country_color(country_index, len(records))
        countries.append({
            "id": record["iso"],
            "name": record["name"],
            "color": color,
            "population": int(record["population"]),
            "economy": int(record["gdp"]),
            "capital": None,
        })

    for country in countries:
        owned = [province for province in provinces if province["country"] == country["id"]]
        if owned:
            owned.sort(key=lambda province: province["pixels"], reverse=True)
            country["capital"] = owned[0]["id"]

    write_province_image(province_raster, os.path.join(output_directory, "provinces.png"))

    with open(os.path.join(output_directory, "provinces.json"), "w", encoding="utf-8") as handle:
        json.dump({
            "width": arguments.width,
            "height": arguments.height,
            "provinces": provinces,
        }, handle, ensure_ascii=False, separators=(",", ":"))

    with open(os.path.join(output_directory, "countries.json"), "w", encoding="utf-8") as handle:
        json.dump({"countries": countries}, handle, ensure_ascii=False, indent=1)

    print("Wrote map data to %s" % output_directory)


if __name__ == "__main__":
    sys.exit(main())

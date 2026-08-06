"""
Builds the low end province raster from the full resolution one.

The downscale is nearest neighbour on purpose: province ids are stored in the
red and green channels, so any interpolation would invent province ids that do
not exist. After downscaling, provinces that lost every pixel are stamped back
in at their centroid so that no province silently disappears from the game.
"""

import json
import os
import argparse

import numpy as np
from PIL import Image


def decode(image_array):
    return image_array[:, :, 0].astype(np.int32) | (image_array[:, :, 1].astype(np.int32) << 8)


def encode(index_array):
    height, width = index_array.shape
    output = np.zeros((height, width, 3), dtype=np.uint8)
    output[:, :, 0] = (index_array & 0xFF).astype(np.uint8)
    output[:, :, 1] = ((index_array >> 8) & 0xFF).astype(np.uint8)
    output[:, :, 2] = np.where(index_array > 0, 255, 0).astype(np.uint8)
    return output


def main():
    parser = argparse.ArgumentParser()
    base = os.path.dirname(__file__)
    parser.add_argument("--source", default=os.path.join(base, "..", "assets", "map", "provinces.png"))
    parser.add_argument("--metadata", default=os.path.join(base, "..", "assets", "map", "provinces.json"))
    parser.add_argument("--output", default=os.path.join(base, "..", "assets-lite", "map", "provinces.png"))
    parser.add_argument("--width", type=int, default=2048)
    parser.add_argument("--height", type=int, default=1024)
    arguments = parser.parse_args()

    source_path = os.path.abspath(arguments.source)
    metadata_path = os.path.abspath(arguments.metadata)
    output_path = os.path.abspath(arguments.output)
    os.makedirs(os.path.dirname(output_path), exist_ok=True)

    source_image = Image.open(source_path)
    source_array = np.array(source_image)
    source_index = decode(source_array)
    source_height, source_width = source_index.shape

    scale_x = source_width / float(arguments.width)
    scale_y = source_height / float(arguments.height)

    sample_x = np.clip((np.arange(arguments.width) * scale_x).astype(np.int32), 0, source_width - 1)
    sample_y = np.clip((np.arange(arguments.height) * scale_y).astype(np.int32), 0, source_height - 1)

    target_index = source_index[np.ix_(sample_y, sample_x)]

    with open(metadata_path, "r", encoding="utf-8") as handle:
        provinces = json.load(handle)["provinces"]

    present = set(np.unique(target_index).tolist())
    restored = 0
    for province in provinces:
        province_id = province["id"]
        if province_id in present:
            continue
        target_x = int(round(province["centroidX"] / scale_x))
        target_y = int(round(province["centroidY"] / scale_y))
        target_x = max(0, min(arguments.width - 1, target_x))
        target_y = max(0, min(arguments.height - 1, target_y))
        target_index[target_y, target_x] = province_id
        restored += 1

    Image.fromarray(encode(target_index), mode="RGB").save(output_path, optimize=True)

    print("wrote %s at %dx%d" % (output_path, arguments.width, arguments.height))
    print("provinces restored after downscale: %d" % restored)
    print("distinct provinces present: %d" % (len(set(np.unique(target_index).tolist())) - 1))


if __name__ == "__main__":
    main()

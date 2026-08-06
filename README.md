# Age Of History Of Conquest

An open source, fully moddable grand strategy game for Android and desktop.
The whole world is divided into provinces, every scenario is a plain JSON file,
and anyone can write their own scenarios, countries, maps and translations
without touching the game code.

---

## Legal notice

**This project is an independent, original work. It is not affiliated with,
endorsed by, sponsored by, or connected to any other game, studio, publisher or
rights holder in any way.**

Specifically, this project has **no connection whatsoever** to the commercial
games *Age of History* / *Age of Civilizations* (Łukasz Jakowski) or
*Age of Conquest* (Noble Master Games). It shares no code, no assets, no data
files, no artwork and no text with those products. It is not a clone, a port, a
continuation, a remake or a modification of them.

The similarity is limited to the fact that this is a turn based strategy game
played on a province map, which is a genre and not anyone's property. Any
resemblance in naming is coincidental in intent; if a rights holder considers
the project name to be an issue, open an issue in this repository and it will be
renamed.

All game code in this repository is written from scratch and released under the
GNU General Public License v3.0. All map data is generated from public domain
Natural Earth geographic data by the scripts in `tools/`. Fonts are licensed
under the SIL Open Font License and their license text ships with them in
`assets/fonts/`.

---

## Status

The project is in early development. Current milestone: **core engine**.

| Milestone | Contents | State |
|---|---|---|
| 1 | Province map rendering, pan and zoom, province selection, mod loader, localization, CI | done |
| 2 | Turn loop, armies, combat, economy, diplomacy | planned |
| 3 | Country AI | planned |
| 4 | Full scenario editor with country creation and map painting | in progress |
| 5 | Scenario sharing and import/export as archives | planned |
| 6 | Historical scenario content | planned |

Planned scenario content: 1444, the Balkan Wars, the First World War, the Turkish
War of Independence, the Second World War, the Cold War, Present Day, and a
speculative Third World War setting.

---

## Building

### Android

Two APK variants are produced automatically by GitHub Actions on every push to
`main`. Download them from the **Actions** tab of this repository.

| Variant | Package | Map raster | Default quality | Intended for |
|---|---|---|---|---|
| normal | `com.nx.aohc` | 4096x2048 | automatic | anything from mid range upwards |
| lite | `com.nx.aohc.lite` | 2048x1024 | low | old and very weak devices |

Both variants contain the same 3600 provinces and the same scenarios. The lite
build simply ships the quarter size raster, defaults to the low quality profile,
and drops the x86 native libraries. The two packages have different application
ids, so both can be installed side by side for comparison.

To build locally:

```
gradle :android:assembleNormalDebug
gradle :android:assembleLiteDebug
```

Requires JDK 17 and the Android SDK with platform 34.

### Graphics quality

Quality can be changed at any time from the main menu and is remembered between
sessions. It is independent of which APK is installed, so the lite build can be
raised to high and the normal build lowered to potato levels.

| Profile | Internal render scale | Borders | Texture samples per pixel |
|---|---|---|---|
| low | 55% | country borders only | 3 |
| medium | 75% | province and country borders | 9 |
| high | 100% | province and country borders | 9 |

The render scale is real: the map is drawn into an RGB565 offscreen buffer at
that fraction of the screen resolution and then stretched back up, so on low the
GPU shades roughly a third of the pixels it would otherwise. The interface is
still drawn at full resolution on top, so text stays sharp while the map itself
gets softer. That is the intended trade: the map looks worse, the game runs.

The automatic profile inspects the maximum texture size, processor count,
available heap and screen resolution at startup and picks a profile. A device
that cannot handle a 4096 texture at all is detected here and dropped to low,
which is also why the lite raster exists.

### Regenerating the low resolution raster

```
python tools/generate_lite_map.py --width 2048 --height 1024
```

The downscale is nearest neighbour, because province ids live in the pixel
values and interpolation would invent ids that do not exist. Any province that
would vanish entirely at the smaller size is stamped back in at its centroid.

### Desktop

```
gradle :desktop:run
```

---

## How the map works

The map is not a set of images per country. It is a single raster where each
pixel stores the numeric id of the province it belongs to:

* red channel — province id, low byte
* green channel — province id, high byte
* blue channel — land flag

Province id `0` is reserved for sea. A fragment shader reads this raster,
looks the owner colour up in a 256x256 palette texture, and draws province and
country borders by comparing neighbouring pixels. Repainting the whole world
therefore costs one small texture upload instead of thousands of draw calls,
which is what makes several thousand provinces practical on a phone.

The shipped map has 3600 provinces at 4096x2048. Both numbers are arguments to
the generator, so a mod can ship a completely different world.

### Regenerating the map

```
pip install geopandas shapely pillow numpy scipy
python tools/generate_map.py --width 4096 --height 2048
python tools/generate_scenarios.py
```

`--source` accepts any polygon dataset readable by GeoPandas, so a fantasy map
or a higher detail administrative dataset can be substituted directly.

---

## Modding

Mods are ordinary folders. Nothing is compiled and nothing is signed.

Android:

```
/Android/data/com.nx.aohc/files/mods/
```

Desktop:

```
~/.age-of-history-of-conquest/mods/
```

A mod looks like this:

```
mods/MyMod/
  mod.json
  localization/tr.json
  localization/en.json
  scenarios/my_scenario.json
  map/provinces.png        (optional, replaces the world)
  map/provinces.json       (optional)
  map/countries.json       (optional)
```

`mod.json`:

```json
{
  "id": "my_mod",
  "name": "My Mod",
  "author": "your name",
  "version": "1.0.0",
  "description": "What this mod does",
  "enabled": true
}
```

A scenario:

```json
{
  "id": "my_scenario",
  "startYear": 1914,
  "inheritDefaultOwnership": true,
  "name": { "en": "My Scenario", "tr": "Benim Senaryom" },
  "description": { "en": "...", "tr": "..." },
  "countries": [
    {
      "id": "TUR",
      "name": { "en": "Turkey", "tr": "Türkiye" },
      "color": [200, 40, 40],
      "capital": 1234,
      "provinces": [1234, 1235, 1236]
    }
  ]
}
```

With `inheritDefaultOwnership` set to `true` the scenario starts from the
default world and only the countries listed are changed. With `false` the
scenario defines the entire world itself.

Any province id listed under a country is taken from whoever held it before, so
moving territory between states is a matter of moving numbers between arrays.
The in game editor writes exactly this format into
`mods/UserScenarios/scenarios/`, so a scenario made on a phone can be shared as
a single file.

### Translations

Drop a JSON file named after the language code into `localization/`. The game
picks up the device language automatically and falls back to English for any
missing key. English and Turkish ship with the game.

---

## Contributing

Issues and pull requests are welcome. Historical scenarios in particular are
easier to contribute than to write alone, since each one is a data file rather
than code.

Please do not contribute assets, data or text copied from any commercial game.
Contributions must be original or from a clearly compatible free licence.

## Licence

GNU General Public License v3.0. See `LICENSE`.

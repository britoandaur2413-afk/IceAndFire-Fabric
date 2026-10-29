"""Client assets 26.x needs that the 1.18 mod did not have.

- assets/iceandfire_fabric/items/<id>.json for every item (the item model definitions), with the old model
  overrides turned into conditions / range dispatches on the mod's own model properties
- spawn egg textures: 26.x has no tinted egg template, so each egg gets its own 16x16 picture,
  drawn here from the egg's two original colours
- equipment assets for the armors drawn on the vanilla body (the 3D armors have their own renderer)

usage: python -I tools/gen_assets.py   (needs build/run/gameTest/iaf_registry.txt from runGameTest)
"""
import json
import pathlib

from PIL import Image

ROOT = pathlib.Path(__file__).resolve().parent.parent
ASSETS = ROOT / "src" / "main" / "resources" / "assets" / "iceandfire_fabric"
REGISTRY = ROOT / "build" / "run" / "gameTest" / "iaf_registry.txt"

EGGS = {
    "fire_dragon": (0x340000, 0xA52929), "ice_dragon": (0xB5DDFB, 0x7EBAF0), "lightning_dragon": (0x422367, 0x725691),
    "hippogryph": (0xD8D8D8, 0xD1B55D), "gorgon": (0xD0D99F, 0x684530), "pixie": (0xFF7F89, 0xE2CCE2), "cyclops": (0xB0826E, 0x3A1F0F),
    "siren": (0x8EE6CA, 0xF2DFC8), "hippocampus": (0x4491C7, 0x4FC56B), "death_worm": (0xD1CDA3, 0x423A3A), "cockatrice": (0x8F5005, 0x4F5A23),
    "stymphalian_bird": (0x744F37, 0x9E6C4B), "troll": (0x3D413D, 0x58433A), "myrmex_worker": (0xA16026, 0x594520),
    "myrmex_soldier": (0xA16026, 0x7D622D), "myrmex_sentinel": (0xA16026, 0xA27F3A), "myrmex_royal": (0xA16026, 0xC79B48),
    "myrmex_queen": (0xA16026, 0xECB855), "amphithere": (0x597535, 0x00AA98), "sea_serpent": (0x008299, 0xC5E6E7),
    "dread_thrall": (0xE0E6E6, 0x00FFFF), "dread_ghoul": (0xE0E6E6, 0x7B838A), "dread_beast": (0xE0E6E6, 0x38373C),
    "dread_scuttler": (0xE0E6E6, 0x4D5667), "lich": (0xE0E6E6, 0x274860), "dread_knight": (0xE0E6E6, 0x4A6C6E),
    "dread_horse": (0xE0E6E6, 0xACACAC), "hydra": (0x8B8B78, 0x2E372B), "ghost": (0xB9EDB8, 0x73B276),
}

# the egg: rows of (first column, last column) inside a 16x16 picture
EGG_ROWS = {2: (6, 9), 3: (5, 10), 4: (4, 11), 5: (4, 11), 6: (3, 12), 7: (3, 12), 8: (3, 12), 9: (3, 12),
            10: (3, 12), 11: (3, 12), 12: (4, 11), 13: (5, 10), 14: (6, 9)}
SPOTS = [(6, 4), (7, 4), (9, 6), (10, 6), (10, 7), (5, 8), (6, 8), (5, 9), (8, 10), (9, 10), (9, 11), (6, 12), (7, 12)]


def rgb(c):
    return (c >> 16) & 255, (c >> 8) & 255, c & 255


def shade(color, factor):
    return tuple(max(0, min(255, int(v * factor))) for v in color)


def egg_picture(primary, secondary):
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    base, spot = rgb(primary), rgb(secondary)
    for y, (x0, x1) in EGG_ROWS.items():
        for x in range(x0, x1 + 1):
            edge = x in (x0, x1) or y not in EGG_ROWS or (EGG_ROWS.get(y - 1) and not EGG_ROWS[y - 1][0] <= x <= EGG_ROWS[y - 1][1]) \
                or (EGG_ROWS.get(y + 1) and not EGG_ROWS[y + 1][0] <= x <= EGG_ROWS[y + 1][1]) or y in (2, 14)
            color = spot if (x, y) in SPOTS else base
            # light from the top left, shadow on the bottom right
            light = 1.0 + 0.18 * ((8 - x) + (8 - y)) / 10.0
            color = shade(color, light)
            if edge:
                color = shade(color, 0.55)
            img.putpixel((x, y), color + (255,))
    # a small highlight
    img.putpixel((5, 5), shade(base, 1.5) + (255,))
    img.putpixel((6, 4), shade(base, 1.4) + (255,)) if (6, 4) not in SPOTS else None
    return img


def model(name):
    return {"type": "minecraft:model", "model": "iceandfire_fabric:item/" + name}


def special(kind, base, transformation=None):
    out = {"type": "minecraft:special", "base": base, "model": {"type": "iceandfire_fabric:special", "kind": kind}}
    if transformation:
        out["transformation"] = transformation
    return out


# the 1.18 "builtin/entity" models keep their display transforms as the base of the special models
BUILTIN_PARTICLES = {"troll_weapon": "iceandfire_fabric:item/troll_tusk", "gorgon_head": "iceandfire_fabric:item/gorgon_head", "pixie_house": "minecraft:block/oak_planks",
                     "ghost_chest": "minecraft:block/oak_planks", "dread_portal": "minecraft:block/black_concrete",
                     "deathworm_gauntlet_red": "iceandfire_fabric:item/deathworm_tounge", "deathworm_gauntlet_white": "iceandfire_fabric:item/deathworm_tounge",
                     "deathworm_gauntlet_yellow": "iceandfire_fabric:item/deathworm_tounge", "tide_trident_throwing": "iceandfire_fabric:item/tide_trident"}


def fix_builtin_models():
    for name, particle in BUILTIN_PARTICLES.items():
        path = ASSETS / "models" / "item" / (name + ".json")
        if not path.exists():
            continue
        d = json.loads(path.read_text(encoding="utf-8"))
        if d.get("parent") in ("builtin/entity", "minecraft:builtin/entity"):
            del d["parent"]
        d.pop("overrides", None)
        d.setdefault("textures", {})["particle"] = particle
        path.write_text(json.dumps(d, indent=2) + chr(10), encoding="utf-8", newline=chr(10))


def special_definitions():
    defs = {
        "dragonbone_bow": {
            "type": "minecraft:condition", "property": "minecraft:using_item", "on_false": model("dragonbone_bow"),
            "on_true": {"type": "minecraft:range_dispatch", "property": "minecraft:use_duration", "scale": 0.05,
                        "fallback": model("dragonbone_bow_pulling_0"),
                        "entries": [{"threshold": 0.65, "model": model("dragonbone_bow_pulling_1")},
                                    {"threshold": 0.9, "model": model("dragonbone_bow_pulling_2")}]}},
        "dragon_horn": {"type": "minecraft:range_dispatch", "property": "iceandfire_fabric:horn_dragon", "fallback": model("dragon_horn"),
                        "entries": [{"threshold": 0.25, "model": model("dragon_horn_fire")},
                                    {"threshold": 0.5, "model": model("dragon_horn_ice")},
                                    {"threshold": 0.75, "model": model("dragon_horn_lightning")}]},
        "tide_trident": {"type": "minecraft:select", "property": "minecraft:display_context",
                         "cases": [{"when": ["gui", "ground", "fixed", "on_shelf"], "model": model("tide_trident_inventory")}],
                         "fallback": special("tide_trident", "minecraft:item/trident_in_hand",
                                             {"scale": [1.0, -1.0, -1.0], "translation": [0.0, 0.0, 0.0], "left_rotation": [0.0, 0.0, 0.0, 1.0],
                                              "right_rotation": [0.0, 0.0, 0.0, 1.0]})},
        "gorgon_head": special("gorgon_head", "iceandfire_fabric:item/gorgon_head"),
        "ghost_chest": special("ghost_chest", "iceandfire_fabric:item/ghost_chest"),
        "dread_portal": special("dread_portal", "iceandfire_fabric:item/dread_portal"),
    }
    for weapon in ("axe", "column", "column_forest", "column_frost", "hammer", "trunk", "trunk_frost"):
        defs["troll_weapon_" + weapon] = special("troll_weapon", "iceandfire_fabric:item/troll_weapon")
    for color in ("red", "white", "yellow"):
        defs["deathworm_gauntlet_" + color] = special("deathworm_gauntlet", "iceandfire_fabric:item/deathworm_gauntlet_" + color)
    for wood in ("birch", "dark_oak", "mushroom_brown", "mushroom_red", "oak", "spruce"):
        defs["pixie_house_" + wood] = special("pixie_house", "iceandfire_fabric:item/pixie_house")
    for kind in ("fire", "ice", "lightning"):
        name = "summoning_crystal_" + kind
        defs[name] = {"type": "minecraft:condition", "property": "iceandfire_fabric:has_dragon", "on_false": model(name), "on_true": model(name + "_active")}
    return defs


def main():
    lines = REGISTRY.read_text(encoding="utf-8").split()
    items = [lines[i + 1] for i in range(0, len(lines), 2) if lines[i] == "item"]
    fix_builtin_models()
    special = special_definitions()
    (ASSETS / "items").mkdir(exist_ok=True)
    for item in items:
        definition = special.get(item, model(item))
        (ASSETS / "items" / (item + ".json")).write_text(json.dumps({"model": definition}, indent=2) + "\n", encoding="utf-8", newline="\n")
    # spawn eggs
    for entity, (primary, secondary) in EGGS.items():
        name = "spawn_egg_" + entity
        egg_picture(primary, secondary).save(ASSETS / "textures" / "item" / (name + ".png"))
        (ASSETS / "models" / "item" / (name + ".json")).write_text(
            json.dumps({"parent": "minecraft:item/generated", "textures": {"layer0": "iceandfire_fabric:item/" + name}}, indent=2) + "\n",
            encoding="utf-8", newline="\n")
    # the tide trident is drawn by its entity model in hands; in the inventory it is its picture
    (ASSETS / "models" / "item" / "tide_trident_inventory.json").write_text(
        json.dumps({"parent": "minecraft:item/handheld", "textures": {"layer0": "iceandfire_fabric:item/tide_trident"}}, indent=2) + "\n",
        encoding="utf-8", newline="\n")
    # armors drawn on the vanilla body: equipment assets with their layers
    flat = {"myrmexdesert": ("myrmex_desert_layer_1", "myrmex_desert_layer_2"), "myrmexjungle": ("myrmex_jungle_layer_1", "myrmex_jungle_layer_2"),
            "blindfold": ("blindfold_layer_1", None), "earplugs": ("earplugs_layer_1", None), "sheep": ("sheep_disguise_layer_1", "sheep_disguise_layer_2")}
    armor_src = ASSETS / "textures" / "models" / "armor"
    for name, (body, legs) in flat.items():
        layers = {}
        for kind, src in (("humanoid", body), ("humanoid_leggings", legs)):
            if src and (armor_src / (src + ".png")).exists():
                dst = ASSETS / "textures" / "entity" / "equipment" / kind
                dst.mkdir(parents=True, exist_ok=True)
                (dst / (name + ".png")).write_bytes((armor_src / (src + ".png")).read_bytes())
                layers[kind] = [{"texture": "iceandfire_fabric:" + name}]
            elif src:
                print("missing armor texture", src)
        (ASSETS / "equipment").mkdir(exist_ok=True)
        (ASSETS / "equipment" / (name + ".json")).write_text(json.dumps({"layers": layers}, indent=2) + "\n", encoding="utf-8", newline="\n")
    print("items", len(items), "eggs", len(EGGS))


if __name__ == "__main__":
    main()

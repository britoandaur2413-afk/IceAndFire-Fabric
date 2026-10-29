"""Write the worldgen data the 1.18 mod built in code: features and placed features.

The mod's own features take no configuration ({"type": "iceandfire_fabric:<name>"}); the placement follows
the original (on the world-surface heightmap with a biome check, dragon caves only through the
mod's biome filter). Ores and lilies are vanilla features with the original sizes and counts.

usage: python -I tools/gen_worldgen.py
"""
import json
import pathlib

ROOT = pathlib.Path(__file__).resolve().parent.parent / "src" / "main" / "resources" / "data" / "iceandfire_fabric" / "worldgen"

SIMPLE = ["fire_dragon_roost", "ice_dragon_roost", "lightning_dragon_roost", "cyclops_cave", "pixie_village", "siren_island", "hydra_cave",
          "myrmex_hive_desert", "myrmex_hive_jungle", "spawn_death_worm", "spawn_dragon_skeleton_l", "spawn_dragon_skeleton_f",
          "spawn_dragon_skeleton_i", "spawn_hippocampus", "spawn_sea_serpent", "spawn_stymphalian_bird", "spawn_wandering_cyclops"]
CAVES = ["fire_dragon_cave", "ice_dragon_cave", "lightning_dragon_cave"]

SURFACE = [{"type": "minecraft:heightmap", "heightmap": "WORLD_SURFACE_WG"}, {"type": "minecraft:biome"}]


def ore_targets(block):
    return [{"state": block, "target": {"predicate_type": "minecraft:tag_match", "tag": "minecraft:stone_ore_replaceables"}}]


def write(kind, name, data):
    path = ROOT / kind / (name + ".json")
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(data, indent=2) + "\n", encoding="utf-8", newline="\n")


def main():
    for name in SIMPLE:
        write("feature", name, {"type": "iceandfire_fabric:" + name})
        write("placed_feature", name, {"feature": "iceandfire_fabric:" + name, "placement": SURFACE})
    for name in CAVES:
        write("feature", name, {"type": "iceandfire_fabric:" + name})
        write("placed_feature", name, {"feature": "iceandfire_fabric:" + name, "placement": [{"type": "iceandfire_fabric:biome_extended"}]})

    # copper and silver: 2 veins of 8 a chunk, below y 128 / 32
    for name, block, top in (("copper_ore", "iceandfire_fabric:copper_ore", 128), ("silver_ore", "iceandfire_fabric:silver_ore", 32)):
        write("feature", name, {"type": "minecraft:ore", "size": 8, "discard_chance_on_air_exposure": 0.0, "targets": ore_targets(block)})
        write("placed_feature", name, {"feature": "iceandfire_fabric:" + name, "placement": [
            {"type": "minecraft:count", "count": 2},
            {"type": "minecraft:in_square"},
            {"type": "minecraft:height_range", "height": {"type": "minecraft:uniform", "min_inclusive": {"above_bottom": 0}, "max_inclusive": {"absolute": top}}},
            {"type": "minecraft:biome"}]})
    # sapphire and amethyst (single blocks in stone, 3 to 8 a chunk), placed like vanilla emerald ore
    for name, block in (("sapphire_ore", "iceandfire_fabric:sapphire_ore"), ("amethyst_ore", "iceandfire_fabric:amythest_ore")):
        write("feature", name, {"type": "minecraft:ore", "size": 3, "discard_chance_on_air_exposure": 0.0, "targets": ore_targets(block)})
        write("placed_feature", name, {"feature": "iceandfire_fabric:" + name, "placement": [
            {"type": "minecraft:count", "count": {"type": "minecraft:uniform", "min_inclusive": 3, "max_inclusive": 8}},
            {"type": "minecraft:in_square"},
            {"type": "minecraft:height_range", "height": {"type": "minecraft:trapezoid", "min_inclusive": {"absolute": -16}, "max_inclusive": {"absolute": 480}}},
            {"type": "minecraft:biome"}]})
    # lilies: one flower a chunk on the surface, only on an empty block
    for name in ("fire_lily", "frost_lily", "lightning_lily"):
        write("feature", name, {"type": "minecraft:simple_block", "to_place": {"id": "iceandfire_fabric:" + name}})
        write("placed_feature", name, {"feature": "iceandfire_fabric:" + name, "placement": [
            {"type": "minecraft:in_square"},
            {"type": "minecraft:heightmap", "heightmap": "MOTION_BLOCKING"},
            {"type": "minecraft:block_predicate_filter", "predicate": {"type": "minecraft:matching_block_tag", "tag": "minecraft:air"}},
            {"type": "minecraft:biome"}]})
    print("ok")


if __name__ == "__main__":
    main()

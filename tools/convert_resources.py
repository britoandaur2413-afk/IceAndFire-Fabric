"""Turns the 26.3 data and assets into their 1.21.1 shape. Runs again without harm (what is done already is left).

- loot tables: "condition"/"modifier" back to "conditions"/"functions" lists, condition and function "type" back to
  "condition"/"function", match_block back to block_state_property; 26.x item ids back to 1.21.1 ones
- recipes: plain ingredient strings back to {"item"}/{"tag"} objects; data brewing recipes go (IafEffects brews in code)
- advancements: entity predicates back to {"type"}, conditions back to "condition", damage tags without '#'
- worldgen: worldgen/feature becomes worldgen/configured_feature with its "config"; block states as {"Name"} objects;
  biomes and dimension types written in the 1.21.1 fields; biome tags without biomes 1.21.1 does not have
- villager trades and trade sets go (IafVillagerRegistry adds the trades in code)
- the villager profession's name under the key 1.21.1 reads
"""
import json
import pathlib
import shutil

ROOT = pathlib.Path(__file__).resolve().parent.parent / "src/main/resources"
DATA = ROOT / "data/iceandfire_fabric"
ASSETS = ROOT / "assets/iceandfire_fabric"
NS = "iceandfire_fabric"

# 26.x ids 1.21.1 does not have, and what stands for them
ITEM_IDS = {
    "minecraft:iron_chain": "minecraft:chain",
    "minecraft:copper_nugget": f"{NS}:copper_nugget",
    "minecraft:copper_sword": f"{NS}:copper_sword",
    "minecraft:copper_shovel": f"{NS}:copper_shovel",
    "minecraft:copper_pickaxe": f"{NS}:copper_pickaxe",
    "minecraft:copper_axe": f"{NS}:copper_axe",
    "minecraft:copper_hoe": f"{NS}:copper_hoe",
    "minecraft:copper_helmet": f"{NS}:armor_copper_metal_helmet",
    "minecraft:copper_chestplate": f"{NS}:armor_copper_metal_chestplate",
    "minecraft:copper_leggings": f"{NS}:armor_copper_metal_leggings",
    "minecraft:copper_boots": f"{NS}:armor_copper_metal_boots",
}
# biomes newer than 1.21.1
NEW_BIOMES = {"minecraft:pale_garden"}


def load(path):
    return json.loads(path.read_text(encoding="utf-8"))


def save(path, obj):
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(obj, indent=2, ensure_ascii=False) + "\n", encoding="utf-8")


def ids(obj):
    """The 26.x item ids replaced everywhere in a loaded file."""
    if isinstance(obj, dict):
        return {k: ids(v) for k, v in obj.items()}
    if isinstance(obj, list):
        return [ids(v) for v in obj]
    if isinstance(obj, str):
        return ITEM_IDS.get(obj, obj)
    return obj


# ---------------------------------------------------------------- loot tables

def condition(c):
    c = dict(c)
    kind = c.pop("type", None)
    if kind is None:
        return c
    if kind == "minecraft:match_block":
        out = {"condition": "minecraft:block_state_property", "block": c["blocks"]}
        if "state" in c:
            out["properties"] = c["state"]
        return out
    out = {"condition": kind}
    for k, v in c.items():
        if k == "terms":
            out[k] = [condition(t) for t in v]
        elif k == "term":
            out[k] = condition(v)
        else:
            out[k] = v
    return out


def conditions(c):
    """A 26.x "condition" (one, perhaps all_of) as a 1.21.1 "conditions" list."""
    c = condition(c)
    if c.get("condition") == "minecraft:all_of":
        return c["terms"]
    return [c]


def function(f):
    f = dict(f)
    out = {"function": f.pop("type")}
    for k, v in f.items():
        if k == "condition":
            out["conditions"] = conditions(v)
        else:
            out[k] = v
    return out


def loot_part(o):
    """A table, pool or entry: its conditions and functions in the 1.21.1 shape, its entries and children too."""
    if isinstance(o, list):
        return [loot_part(v) for v in o]
    if not isinstance(o, dict):
        return o
    out = {}
    for k, v in o.items():
        if k == "condition":
            out["conditions"] = conditions(v)
        elif k == "modifier":
            out["functions"] = [function(f) for f in (v if isinstance(v, list) else [v])]
        elif k in ("pools", "entries", "children"):
            out[k] = loot_part(v)
        else:
            out[k] = v
    return out


def loot_tables():
    n = 0
    for f in (DATA / "loot_table").rglob("*.json"):
        t = load(f)
        new = ids(loot_part(t))
        if new != t:
            save(f, new)
            n += 1
    print("loot tables:", n)


# ---------------------------------------------------------------- recipes

def ingredient(i):
    if isinstance(i, str):
        return {"tag": i[1:]} if i.startswith("#") else {"item": i}
    if isinstance(i, list):
        return [ingredient(v) for v in i]
    return i


def recipes():
    n = 0
    brewing = DATA / "recipe/brewing"
    if brewing.exists():
        shutil.rmtree(brewing)
    for f in (DATA / "recipe").rglob("*.json"):
        t = load(f)
        r = ids(dict(t))
        kind = r["type"]
        if "key" in r:
            r["key"] = {k: ingredient(v) for k, v in r["key"].items()}
        if "ingredients" in r:
            r["ingredients"] = [ingredient(v) for v in r["ingredients"]]
        for k in ("ingredient", "template", "base", "input", "blood"):
            if k in r:
                r[k] = ingredient(r[k])
        if kind == "minecraft:smithing_transform" and r.get("addition") == "#minecraft:netherite_tool_materials":
            r["addition"] = {"item": "minecraft:netherite_ingot"}
        elif "addition" in r:
            r["addition"] = ingredient(r["addition"])
        if r != t:
            save(f, r)
            n += 1
    print("recipes:", n)


# ---------------------------------------------------------------- advancements

def entity_predicate(p):
    p = dict(p)
    if "minecraft:entity_type" in p:
        p["type"] = p.pop("minecraft:entity_type")
    return p


def criterion_entity(e):
    """26.x wraps the entity in an entity_properties condition; 1.21.1 takes the predicate itself."""
    if isinstance(e, dict) and e.get("type") == "minecraft:entity_properties" and e.get("entity", "this") == "this":
        return entity_predicate(e.get("predicate", {}))
    if isinstance(e, dict) and "type" in e and e["type"] == "minecraft:entity_properties":
        return [condition(e)]
    return e


def advancements():
    n = 0
    for f in (DATA / "advancement").rglob("*.json"):
        t = load(f)
        a = json.loads(json.dumps(t))
        for c in a.get("criteria", {}).values():
            cond = c.get("conditions", {})
            if "entity" in cond:
                cond["entity"] = criterion_entity(cond["entity"])
            dmg = cond.get("damage")
            if dmg and "type" in dmg:
                for tag in dmg["type"].get("tags", []):
                    tag["id"] = tag["id"].lstrip("#")
                if "direct_entity" in dmg["type"]:
                    dmg["type"]["direct_entity"] = entity_predicate(dmg["type"]["direct_entity"])
                if "source_entity" in dmg["type"]:
                    dmg["type"]["source_entity"] = entity_predicate(dmg["type"]["source_entity"])
        a = ids(a)
        if a != t:
            save(f, a)
            n += 1
    print("advancements:", n)


# ---------------------------------------------------------------- worldgen

def state(s):
    if isinstance(s, str):
        return {"Name": s}
    return s


def configured_features():
    old = DATA / "worldgen/feature"
    new = DATA / "worldgen/configured_feature"
    if not old.exists():
        return
    for f in old.glob("*.json"):
        t = load(f)
        kind = t.pop("type")
        if kind == "minecraft:ore":
            for target in t.get("targets", []):
                target["state"] = state(target["state"])
        elif kind == "minecraft:simple_block":
            place = t["to_place"]
            if "id" in place:
                t["to_place"] = {"type": "minecraft:simple_state_provider", "state": {"Name": place["id"]}}
        save(new / f.name, {"type": kind, "config": t})
        f.unlink()
    old.rmdir()
    print("configured features: moved")


def biome_tags():
    n = 0
    for f in (DATA / "tags/worldgen/biome").rglob("*.json"):
        t = load(f)
        values = [v for v in t["values"] if (v if isinstance(v, str) else v.get("id")) not in NEW_BIOMES]
        if values != t["values"]:
            t["values"] = values
            save(f, t)
            n += 1
    print("biome tags:", n)


def color(hex_color):
    return int(hex_color.lstrip("#")[-6:], 16)


def biomes():
    """26.x keeps sky, fog and particles in attributes; 1.21.1 has them in effects."""
    for f in (DATA / "worldgen/biome").glob("*.json"):
        t = load(f)
        attrs = t.pop("attributes", None)
        if attrs is None:
            continue
        effects = t.get("effects", {})
        water = effects.get("water_color", "#3f76e4")
        fog = attrs.get("minecraft:visual/fog_color", "#c0d8ff")
        new_effects = {
            "fog_color": color(fog),
            "sky_color": color(attrs.get("minecraft:visual/sky_color", "#78a7ff")),
            "water_color": color(water),
            "water_fog_color": color(effects.get("water_fog_color", water)),
        }
        particles = attrs.get("minecraft:visual/ambient_particles", {}).get("argument", [])
        if particles:
            # 1.21.1 has one ambient particle per biome: the likeliest
            p = max(particles, key=lambda e: e["probability"])
            new_effects["particle"] = {"options": {"type": p["particle"]["type"]}, "probability": p["probability"]}
        t["effects"] = new_effects
        if t.get("carvers") == []:
            t["carvers"] = {}
        t["spawners"] = t.get("spawners", {})
        t["spawn_costs"] = t.get("spawn_costs", {})
        save(f, t)
    print("biomes: done")


def dimension_types():
    """The 1.21.1 fields: what 26.x said with attributes, timelines and clocks."""
    for f in (DATA / "dimension_type").glob("*.json"):
        t = load(f)
        attrs = t.get("attributes")
        if attrs is None:
            continue
        bed = attrs.get("minecraft:gameplay/bed_rule", {})
        new = {
            "ultrawarm": False,
            "natural": False,
            "piglin_safe": False,
            "respawn_anchor_works": attrs.get("minecraft:gameplay/respawn_anchor_works", False),
            # 1.21.1 cannot say "no sleeping here" without the bed exploding; in these realms nothing is placed anyway
            "bed_works": bed.get("can_sleep", "always") != "never",
            "has_raids": False,
            "has_skylight": t["has_skylight"],
            "has_ceiling": t["has_ceiling"],
            "coordinate_scale": t["coordinate_scale"],
            "ambient_light": t["ambient_light"],
            "logical_height": t["logical_height"],
            "min_y": t["min_y"],
            "height": t["height"],
            "infiniburn": t["infiniburn"],
            "effects": "minecraft:the_end" if t.get("skybox") == "end" else "minecraft:overworld",
            "monster_spawn_light_level": t["monster_spawn_light_level"],
            "monster_spawn_block_light_limit": t["monster_spawn_block_light_limit"],
        }
        if t.get("has_fixed_time"):
            new["fixed_time"] = 6000
        save(f, new)
    print("dimension types: done")


# ---------------------------------------------------------------- trades, names

def trades():
    for d in (DATA / "villager_trade", DATA / "trade_set", DATA / "tags/villager_trade"):
        if d.exists():
            shutil.rmtree(d)
    print("trades: removed (in code)")


def lang():
    old_key = f"entity.minecraft.villager.{NS}.scribe"
    for f in (ASSETS / "lang").glob("*.json"):
        t = load(f)
        if old_key in t and "entity.minecraft.villager.scribe" not in t:
            t["entity.minecraft.villager.scribe"] = t[old_key]
            save(f, t)
    print("lang: scribe")


if __name__ == "__main__":
    loot_tables()
    recipes()
    advancements()
    configured_features()
    biome_tags()
    biomes()
    dimension_types()
    trades()
    lang()

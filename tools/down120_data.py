"""1.20.1, the data (runs again without harm): 1.21's data written as 1.20.1 reads it.
- folders: advancement, loot_table, recipe, structure, tags/block|item|entity_type|fluid -> their 1.20.1 plural names
- recipes: results {"id"} -> {"item"} (cooking: the id alone; stonecutting: the id and a count beside it)
- loot: enchanted_count_increase -> looting_enchant, random_chance_with_enchanted_bonus -> random_chance_with_looting,
  enchant_with_levels options -> treasure, match_tool's enchantments predicate -> 1.20.1's, loot_table entries "value" -> "name"
- advancements: icons {"id"} -> {"item"}, item predicates "items": "x" -> ["x"] ("#t" -> "tag": "t")
- worldgen: int providers (uniform, clamped, biased_to_bottom, clamped_normal) under "value", as 1.20.1 reads them
- the game's ids renamed after 1.20.1 (short_grass, turtle_scute) back to its own (grass, scute)
- convention tags: 1.21's c:ingots/silver... -> 1.20.1's c:silver_ingots..., each one the mod uses defined (with the game's
  own members), since Fabric's 1.20.1 conventions have few of them
"""
import json
import pathlib
import shutil
import sys

ROOT = pathlib.Path(__file__).resolve().parent.parent
DATA = ROOT / "src/main/resources/data"

FOLDERS = {"advancement": "advancements", "loot_table": "loot_tables", "recipe": "recipes", "structure": "structures"}
TAG_FOLDERS = {"block": "blocks", "item": "items", "entity_type": "entity_types", "fluid": "fluids", "game_event": "game_events"}

# the game's members of 1.21's convention tags the mod uses (1.20.1's Fabric has few of them)
VANILLA = {
    "ingots/iron": ["minecraft:iron_ingot"], "ingots/gold": ["minecraft:gold_ingot"], "ingots/copper": ["minecraft:copper_ingot"],
    "nuggets/gold": ["minecraft:gold_nugget"], "nuggets/iron": ["minecraft:iron_nugget"],
    "storage_blocks/iron": ["minecraft:iron_block"], "storage_blocks/gold": ["minecraft:gold_block"],
    "storage_blocks/diamond": ["minecraft:diamond_block"], "storage_blocks/copper": ["minecraft:copper_block"],
    "storage_blocks/amethyst": ["minecraft:amethyst_block"], "gems/diamond": ["minecraft:diamond"],
    "gems/emerald": ["minecraft:emerald"], "gems/amethyst": ["minecraft:amethyst_shard"], "rods/wooden": ["minecraft:stick"],
    "strings": ["minecraft:string"], "feathers": ["minecraft:feather"], "slime_balls": ["minecraft:slime_ball"],
    "obsidians": ["minecraft:obsidian", "minecraft:crying_obsidian"], "glass_blocks/colorless": ["minecraft:glass"],
    "chests/wooden": ["minecraft:chest", "minecraft:trapped_chest"], "bones": ["minecraft:bone"],
    "ores/copper": ["minecraft:copper_ore", "minecraft:deepslate_copper_ore"],
}
PLAIN = {"strings": "string", "obsidians": "obsidian", "glass_blocks/colorless": "glass_blocks", "chests/wooden": "wooden_chests",
         "rods/wooden": "wooden_rods"}
GROUPS = {"ingots": "ingots", "nuggets": "nuggets", "storage_blocks": "blocks", "ores": "ores", "scales": "scales", "bones": "bones"}


def c_name(path):
    """1.20.1's name of a 1.21 convention tag path (ingots/silver -> silver_ingots, gems/sapphire -> sapphires)."""
    if path in PLAIN:
        return PLAIN[path]
    if "/" in path:
        group, what = path.split("/", 1)
        if group == "gems":
            return what + "s"
        if group in GROUPS:
            return what + "_" + GROUPS[group]
    return path


def c_ref(ref):
    """A tag reference ("c:x/y" or "#c:x/y") with 1.20.1's name; others as they are."""
    tag = ref.startswith("#")
    r = ref[1:] if tag else ref
    if r.startswith("c:"):
        r = "c:" + c_name(r[2:])
    return ("#" if tag else "") + r


def rename_folders():
    for ns in DATA.iterdir():
        for a, b in FOLDERS.items():
            if (ns / a).is_dir():
                move(ns / a, ns / b)
        tags = ns / "tags"
        if tags.is_dir():
            for a, b in TAG_FOLDERS.items():
                if (tags / a).is_dir():
                    move(tags / a, tags / b)


def move(src, dst):
    for f in sorted(src.rglob("*")):
        if f.is_file():
            to = dst / f.relative_to(src)
            to.parent.mkdir(parents=True, exist_ok=True)
            shutil.move(str(f), str(to))
    shutil.rmtree(src)


def walk(o, fix):
    if isinstance(o, dict):
        fix(o)
        for v in list(o.values()):
            walk(v, fix)
    elif isinstance(o, list):
        for v in o:
            walk(v, fix)


def ingredient_tags(o):
    if isinstance(o.get("tag"), str):
        o["tag"] = c_ref(o["tag"])


def recipe(j):
    t = j.get("type", "")
    r = j.get("result")
    if isinstance(r, dict) and "id" in r:
        if t in ("minecraft:smelting", "minecraft:blasting", "minecraft:smoking", "minecraft:campfire_cooking"):
            j["result"] = r["id"]
        elif t == "minecraft:stonecutting":
            j["result"] = r["id"]
            j["count"] = r.get("count", 1)
        else:
            r["item"] = r.pop("id")
    if t == "minecraft:stonecutting" and "count" not in j:
        j["count"] = 1
    walk(j, ingredient_tags)


def loot(j):
    def fix(o):
        f = o.get("function")
        if f == "minecraft:enchanted_count_increase":
            o["function"] = "minecraft:looting_enchant"
            o.pop("enchantment", None)
        elif f == "minecraft:enchant_with_levels" and "options" in o:
            options = o.pop("options")
            o["treasure"] = "treasure" in json.dumps(options)
        c = o.get("condition")
        if c == "minecraft:random_chance_with_enchanted_bonus":
            bonus = o.pop("enchanted_chance", {})
            o["condition"] = "minecraft:random_chance_with_looting"
            o["chance"] = o.pop("unenchanted_chance", 0)
            o["looting_multiplier"] = bonus.get("per_level_above_first", 0) if isinstance(bonus, dict) else 0
            o.pop("enchantment", None)
        elif c == "minecraft:match_tool":
            o["predicate"] = item_predicate(o.get("predicate", {}))
        if o.get("type") == "minecraft:loot_table" and "value" in o and "name" not in o:
            o["name"] = o.pop("value")
    walk(j, fix)


def item_predicate(p):
    """A 1.21 item predicate as 1.20.1 has it: items in a list (a tag apart), enchantments without "predicates"."""
    p = dict(p)
    items = p.get("items")
    if isinstance(items, str):
        if items.startswith("#"):
            p.pop("items")
            p["tag"] = c_ref(items)[1:]
        else:
            p["items"] = [items]
    preds = p.pop("predicates", {})
    for e in preds.get("minecraft:enchantments", []):
        e = dict(e)
        if "enchantments" in e:
            ench = e.pop("enchantments")
            e["enchantment"] = ench if isinstance(ench, str) else ench[0]
        p.setdefault("enchantments", []).append(e)
    return p


def advancement(j):
    icon = j.get("display", {}).get("icon")
    if isinstance(icon, dict) and "id" in icon:
        icon["item"] = icon.pop("id")
    for c in j.get("criteria", {}).values():
        cond = c.get("conditions", {})
        if c.get("trigger") == "minecraft:inventory_changed":
            cond["items"] = [item_predicate(i) for i in cond.get("items", [])]
        for k in ("item", "tool"):
            if isinstance(cond.get(k), dict):
                cond[k] = item_predicate(cond[k])


RENAMED = {"minecraft:short_grass": "minecraft:grass", "minecraft:turtle_scute": "minecraft:scute"}


def renamed():
    n = 0
    for f in DATA.rglob("*.json"):
        text = f.read_text(encoding="utf-8")
        new = text
        for a, b in RENAMED.items():
            new = new.replace('"' + a + '"', '"' + b + '"')
        if new != text:
            f.write_text(new, encoding="utf-8")
            n += 1
    return n


INT_PROVIDERS = ("minecraft:uniform", "minecraft:biased_to_bottom", "minecraft:clamped", "minecraft:clamped_normal")


def worldgen(j):
    def fix(o):
        if o.get("type") in INT_PROVIDERS and "value" not in o:
            fields = {k: v for k, v in o.items() if k != "type"}
            # (a height provider's bounds are anchors, objects: those stay as they are)
            if fields and all(not isinstance(v, dict) or k == "source" for k, v in fields.items()):
                for k in fields:
                    o.pop(k)
                o["value"] = fields
    walk(j, fix)


def conventions():
    """The c: tags under 1.20.1's names, each one used defined with the game's members too."""
    used = set()
    for f in DATA.rglob("*.json"):
        for word in json.dumps(json.loads(f.read_text(encoding="utf-8"))).split('"'):
            if word.startswith(("c:", "#c:")):
                used.add(word.lstrip("#")[2:])
    for kind in ("items", "blocks"):
        base = DATA / "c/tags" / kind
        if not base.is_dir():
            continue
        # the mod's own c tags, renamed (two old ones may become one: their values are merged)
        for f in sorted(base.rglob("*.json")):
            path = f.relative_to(base).with_suffix("").as_posix()
            name = c_name(path)
            if name != path:
                j = json.loads(f.read_text(encoding="utf-8"))
                to = base / (name + ".json")
                if to.exists():
                    old = json.loads(to.read_text(encoding="utf-8"))
                    j["values"] = old["values"] + [v for v in j["values"] if v not in old["values"]]
                f.unlink()
                write(to, j)
        for d in sorted((p for p in base.rglob("*") if p.is_dir()), reverse=True):
            if not any(d.iterdir()):
                d.rmdir()
    items = DATA / "c/tags/items"
    for path in sorted(VANILLA):
        if path not in used and c_name(path) not in used:
            continue
        to = items / (c_name(path) + ".json")
        j = json.loads(to.read_text(encoding="utf-8")) if to.exists() else {"replace": False, "values": []}
        for v in VANILLA[path]:
            if v not in j["values"] and {"id": v, "required": False} not in j["values"]:
                j["values"].insert(0, v)
        write(to, j)
    # references to them anywhere (tag files that include them, recipes)
    for f in DATA.rglob("*.json"):
        if "tags" in f.parts:
            j = json.loads(f.read_text(encoding="utf-8"))
            vals = j.get("values", [])
            new = [c_ref(v) if isinstance(v, str) else dict(v, id=c_ref(v["id"])) for v in vals]
            if new != vals:
                j["values"] = new
                write(f, j)


def write(f, j):
    f.parent.mkdir(parents=True, exist_ok=True)
    f.write_text(json.dumps(j, indent=2, ensure_ascii=False) + "\n", encoding="utf-8")


def each(folder, convert):
    n = 0
    for ns in DATA.iterdir():
        for f in (ns / folder).rglob("*.json") if (ns / folder).is_dir() else []:
            j = json.loads(f.read_text(encoding="utf-8"))
            before = json.dumps(j, sort_keys=True)
            convert(j)
            if json.dumps(j, sort_keys=True) != before:
                write(f, j)
                n += 1
    return n


if __name__ == "__main__":
    rename_folders()
    print("recipes", each("recipes", recipe))
    print("loot", each("loot_tables", loot))
    print("advancements", each("advancements", advancement))
    print("worldgen", each("worldgen", worldgen))
    print("renamed", renamed())
    conventions()
    sys.exit(0)

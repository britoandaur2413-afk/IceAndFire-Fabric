"""Convert the 1.18 data files (recipes, loot tables, advancements, tags) to the 26.x formats.

Works in place on src/main/resources/data; running it twice is harmless (converted files are left alone).

usage: python -I tools/convert_data.py
"""
import json
import pathlib

ROOT = pathlib.Path(__file__).resolve().parent.parent / "src" / "main" / "resources" / "data"
NS = "iceandfire_fabric"


def mc(name):
    return name if ":" in name else "minecraft:" + name


def fix_tag_ref(name):
    if name.startswith("#forge:"):
        return "#c:" + name[len("#forge:"):]
    if name.startswith("forge:"):
        return "c:" + name[len("forge:"):]
    return name


# ---------------------------------------------------------------- ingredients / results

def ingredient(value):
    """1.18 ingredient ({"item"}, {"tag"} or a list of them) -> 26.x (an id, "#tag" or a list of ids)."""
    if isinstance(value, str):
        return value
    if isinstance(value, list):
        out = []
        for v in value:
            i = ingredient(v)
            if isinstance(i, list):
                out.extend(i)
            else:
                out.append(i)
        return out if len(out) != 1 else out[0]
    if "item" in value:
        return value["item"]
    if "tag" in value:
        return "#" + fix_tag_ref(value["tag"])
    raise ValueError("ingredient " + json.dumps(value))


def result(value, count=None):
    if isinstance(value, str):
        out = {"id": value}
    elif "id" in value:
        return value
    else:
        out = {"id": value["item"]}
        if value.get("count", 1) != 1:
            out["count"] = value["count"]
    if count and count != 1:
        out["count"] = count
    return out


def convert_recipe(d):
    t = mc(d["type"])
    d.pop("conditions", None)
    if t == "minecraft:crafting_shaped":
        d["key"] = {k: ingredient(v) for k, v in d["key"].items()}
        d["result"] = result(d["result"])
    elif t == "minecraft:crafting_shapeless":
        d["ingredients"] = [ingredient(v) for v in d["ingredients"]]
        d["result"] = result(d["result"])
    elif t in ("minecraft:smelting", "minecraft:blasting", "minecraft:smoking", "minecraft:campfire_cooking"):
        d["ingredient"] = ingredient(d["ingredient"])
        d["result"] = result(d["result"])
    elif t == "minecraft:stonecutting":
        d["ingredient"] = ingredient(d["ingredient"])
        d["result"] = result(d["result"], d.pop("count", None))
    elif t == NS + ":dragonforge":
        d["input"] = ingredient(d["input"])
        d["blood"] = ingredient(d["blood"])
        d["result"] = result(d["result"])
    elif t == "minecraft:smithing_transform":
        for k in ("template", "base", "addition"):
            if k in d:
                d[k] = ingredient(d[k])
        d["result"] = result(d["result"])
    else:
        raise ValueError("recipe type " + t)
    d["type"] = t
    return d


# ---------------------------------------------------------------- loot tables

def entity_predicate(old):
    """1.18 entity predicate -> 26.x component-style entity predicate."""
    new = {}
    for k, v in old.items():
        if k == "type":
            new["minecraft:entity_type"] = v
        elif k == "flags":
            new["minecraft:flags"] = v
        elif k == "nbt":
            new["minecraft:nbt"] = v
        else:
            new["minecraft:" + k] = v
    return new


def condition(c):
    c = dict(c)
    kind = mc(c.pop("condition"))
    if kind == "minecraft:alternative":
        return {"type": "minecraft:any_of", "terms": [condition(t) for t in c["terms"]]}
    if kind == "minecraft:inverted":
        return {"type": "minecraft:inverted", "term": condition(c["term"])}
    if kind == "minecraft:random_chance_with_looting":
        chance = c["chance"]
        mult = c.get("looting_multiplier", 0.0)
        return {"type": "minecraft:random_chance_with_enchanted_bonus", "enchantment": "minecraft:looting",
                "unenchanted_chance": chance,
                "enchanted_chance": {"type": "minecraft:linear", "base": round(chance + mult, 4), "per_level_above_first": mult}}
    if kind == "minecraft:block_state_property":
        out = {"type": "minecraft:match_block", "blocks": c["block"]}
        if "properties" in c:
            out["state"] = c["properties"]
        return out
    if kind == "minecraft:entity_properties":
        return {"type": "minecraft:entity_properties", "entity": c.get("entity", "this"), "predicate": entity_predicate(c.get("predicate", {}))}
    if kind == "minecraft:match_tool":
        pred = c.get("predicate", {})
        new = {}
        if "items" in pred:
            new["items"] = pred["items"] if len(pred["items"]) != 1 else pred["items"][0]
        if "item" in pred:
            new["items"] = pred["item"]
        if "tag" in pred:
            new["items"] = "#" + fix_tag_ref(pred["tag"])
        if "enchantments" in pred:
            new["predicates"] = {"minecraft:enchantments": [
                {"enchantments": e["enchantment"], "levels": e.get("levels", {"min": 1})} for e in pred["enchantments"]]}
        return {"type": "minecraft:match_tool", "predicate": new}
    out = {"type": kind}
    out.update(c)
    return out


def conditions(lst):
    lst = [condition(c) for c in lst]
    return lst[0] if len(lst) == 1 else {"type": "minecraft:all_of", "terms": lst}


def function(f):
    f = dict(f)
    kind = mc(f.pop("function"))
    conds = f.pop("conditions", None)
    if kind == "minecraft:looting_enchant":
        out = {"type": "minecraft:enchanted_count_increase", "count": f.get("count", 1), "enchantment": "minecraft:looting"}
        if "limit" in f:
            out["limit"] = f["limit"]
    elif kind == "minecraft:enchant_with_levels":
        out = {"type": kind, "levels": f["levels"], "options": "#minecraft:on_random_loot"}
    elif kind == "minecraft:set_nbt":
        out = {"type": "minecraft:set_custom_data", "tag": f["tag"]}
    else:
        out = {"type": kind}
        out.update(f)
    if conds:
        out["condition"] = conditions(conds)
    return out


def entry(e):
    e = dict(e)
    e["type"] = mc(e["type"])
    if "conditions" in e:
        e["condition"] = conditions(e.pop("conditions"))
    if "functions" in e:
        e["modifier"] = [function(f) for f in e.pop("functions")]
    if "children" in e:
        e["children"] = [entry(c) for c in e["children"]]
    if e["type"] == "minecraft:loot_table":
        e["value"] = e.pop("name")
    if e["type"] == "minecraft:tag":
        e["items"] = "#" + fix_tag_ref(e.pop("name"))
    return e


def uniform(o):
    """Number providers written as {"min", "max"} need their type now."""
    if isinstance(o, dict):
        if set(o.keys()) == {"min", "max"} and all(isinstance(v, (int, float)) for v in o.values()):
            return {"type": "minecraft:uniform", "min": o["min"], "max": o["max"]}
        return {k: uniform(v) for k, v in o.items()}
    if isinstance(o, list):
        return [uniform(v) for v in o]
    return o


def convert_loot(d):
    d.pop("__comment", None)
    if "type" in d:
        d["type"] = mc(d["type"])
    for pool in d.get("pools", []):
        pool.pop("name", None)
        pool.pop("__comment", None)
        if "conditions" in pool:
            pool["condition"] = conditions(pool.pop("conditions"))
        if "functions" in pool:
            pool["modifier"] = [function(f) for f in pool.pop("functions")]
        pool["entries"] = [entry(e) for e in pool["entries"]]
    if "functions" in d:
        d["modifier"] = [function(f) for f in d.pop("functions")]
    return d


def loot_converted(d):
    text = json.dumps(d)
    return '"functions"' not in text and '"conditions"' not in text and '"function"' not in text and '"condition": "' not in text


# ---------------------------------------------------------------- advancements

def entity_condition(old):
    if isinstance(old, list):
        return old
    if "type" in old and old.get("type", "").endswith("entity_properties"):
        return old
    return {"type": "minecraft:entity_properties", "entity": "this", "predicate": entity_predicate(old)}


def convert_advancement(d):
    disp = d.get("display")
    if disp and "icon" in disp and "item" in disp["icon"]:
        disp["icon"] = {"id": disp["icon"]["item"]}
    for c in d.get("criteria", {}).values():
        conds = c.get("conditions")
        if not conds:
            continue
        if "items" in conds:
            new_items = []
            for p in conds["items"]:
                p = dict(p)
                if "items" in p and isinstance(p["items"], list):
                    p["items"] = p["items"] if len(p["items"]) != 1 else p["items"][0]
                if "tag" in p:
                    p["items"] = "#" + fix_tag_ref(p.pop("tag"))
                new_items.append(p)
            conds["items"] = new_items
        for k in ("entity", "killing_blow"):
            if k in conds and k == "entity":
                conds[k] = entity_condition(conds[k])
    return d


# ---------------------------------------------------------------- tags

RENAMED = {"minecraft:grass": "minecraft:short_grass", "minecraft:chain": "minecraft:iron_chain",
           "#c:obsidian": "#c:obsidians", "#c:string": "#c:strings", "#c:slimeballs": "#c:slime_balls",
           "#c:glass/colorless": "#c:glass_blocks/colorless", "#minecraft:is_projectile": "#minecraft:is_projectile"}


def rename_all(o):
    """Ids and tags that were renamed since 1.18, anywhere in a file."""
    if isinstance(o, dict):
        return {k: rename_all(v) for k, v in o.items()}
    if isinstance(o, list):
        return [rename_all(v) for v in o]
    if isinstance(o, str):
        return RENAMED.get(o, o)
    return o


def convert_tag(d):
    d["values"] = [RENAMED.get(v, v) if isinstance(v, str) else v for v in d.get("values", [])]
    d["values"] = [fix_tag_ref(v) if isinstance(v, str) else dict(v, id=fix_tag_ref(v["id"])) for v in d.get("values", [])]
    return d


def main():
    changed = 0
    for path in ROOT.rglob("*.json"):
        rel = path.relative_to(ROOT).as_posix()
        parts = rel.split("/")
        try:
            d = json.loads(path.read_text(encoding="utf-8"))
        except Exception as ex:
            print("BAD JSON", rel, ex)
            continue
        before = json.dumps(d, sort_keys=True)
        try:
            if len(parts) > 2 and parts[1] == "recipe":
                d = convert_recipe(d)
            elif len(parts) > 2 and parts[1] == "loot_table":
                if not loot_converted(d):
                    d = convert_loot(d)
                d = uniform(d)
            elif len(parts) > 2 and parts[1] == "advancement":
                d = convert_advancement(d)
            elif len(parts) > 2 and parts[1] == "tags":
                d = convert_tag(d)
            else:
                continue
            d = rename_all(d)
        except Exception as ex:
            print("FAILED", rel, ex)
            continue
        if json.dumps(d, sort_keys=True) != before:
            path.write_text(json.dumps(d, indent=2) + "\n", encoding="utf-8", newline="\n")
            changed += 1
    print("changed", changed)


if __name__ == "__main__":
    main()

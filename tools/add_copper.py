"""Brings back the original mod's copper nugget, tools and armor (1.21.1 has no copper nugget, tools or armor):
textures and names from the 1.18 jar, models, recipes and the nugget tag. Written in the 26.3 style, like the rest,
so convert_resources.py turns them into 1.21.1 files with everything else. Runs again without harm."""
import json
import pathlib
import sys
import zipfile

JAR = pathlib.Path(sys.argv[1]) if len(sys.argv) > 1 else pathlib.Path.home() / "Downloads/iceandfire-2.1.12-1.18.2.jar"
RES = pathlib.Path(__file__).resolve().parent.parent / "src/main/resources"
A = RES / "assets/iceandfire_fabric"
D = RES / "data/iceandfire_fabric"
NS = "iceandfire_fabric"
TOOLS = ["sword", "shovel", "pickaxe", "axe", "hoe"]
ARMOR = ["helmet", "chestplate", "leggings", "boots"]
ITEMS = ["copper_nugget"] + ["copper_" + t for t in TOOLS] + ["armor_copper_metal_" + a for a in ARMOR]


def write(path, obj):
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(obj, indent=2, ensure_ascii=False) + "\n", encoding="utf-8")


z = zipfile.ZipFile(JAR)
for name in ITEMS:
    (A / "textures/item").mkdir(parents=True, exist_ok=True)
    (A / f"textures/item/{name}.png").write_bytes(z.read(f"assets/iceandfire/textures/item/{name}.png"))
    handheld = name.startswith("copper_") and name != "copper_nugget"
    write(A / f"models/item/{name}.json", {"parent": "item/handheld" if handheld else "item/generated",
                                           "textures": {"layer0": f"{NS}:item/{name}"}})
    write(A / f"items/{name}.json", {"model": {"type": "minecraft:model", "model": f"{NS}:item/{name}"}})
for layer in ("1", "2"):
    (A / f"textures/models/armor/armor_copper_metal_layer_{layer}.png").write_bytes(
        z.read(f"assets/iceandfire/textures/models/armor/armor_copper_metal_layer_{layer}.png"))

# names: the 1.18 translation when it had one, else English
for lang in sorted((A / "lang").glob("*.json")):
    t = json.loads(lang.read_text(encoding="utf-8"))
    try:
        old = json.loads(z.read(f"assets/iceandfire/lang/{lang.name}").decode("utf-8"))
    except KeyError:
        old = {}
    en = json.loads(z.read("assets/iceandfire/lang/en_us.json").decode("utf-8"))
    changed = False
    for name in ITEMS:
        key = f"item.{NS}.{name}"
        if key not in t:
            t[key] = old.get(f"item.iceandfire.{name}") or en[f"item.iceandfire.{name}"]
            changed = True
    if changed:
        lang.write_text(json.dumps(t, indent=2, ensure_ascii=False) + "\n", encoding="utf-8")

R = D / "recipe"
ingot, rod = "#c:ingots/copper", "#c:rods/wooden"
shapes = {
    "copper_sword": ["S", "S", "T"], "copper_pickaxe": ["SSS", " T ", " T "], "copper_axe": ["SS", "ST", " T"],
    "copper_shovel": ["S", "T", "T"], "copper_hoe": ["SS", " T", " T"],
    "armor_copper_metal_helmet": ["SSS", "S S"], "armor_copper_metal_chestplate": ["S S", "SSS", "SSS"],
    "armor_copper_metal_leggings": ["SSS", "S S", "S S"], "armor_copper_metal_boots": ["S S", "S S"],
}
for name, pattern in shapes.items():
    key = {"S": ingot}
    if "T" in "".join(pattern):
        key["T"] = rod
    file = name.replace("armor_copper_metal_", "copper_")
    write(R / f"{file}.json", {"type": "minecraft:crafting_shaped", "pattern": pattern, "key": key, "result": {"id": f"{NS}:{name}"}})
write(R / "copper_ingot_to_nuggets.json", {"type": "minecraft:crafting_shapeless", "ingredients": [ingot],
                                           "result": {"id": f"{NS}:copper_nugget", "count": 9}})
write(R / "copper_nuggets_to_ingot.json", {"type": "minecraft:crafting_shapeless", "ingredients": ["#c:nuggets/copper"] * 9,
                                           "result": {"id": "minecraft:copper_ingot"}})
gear = [f"{NS}:{n}" for n in ITEMS if n != "copper_nugget"]
write(R / "furnace/copper_nugget.json", {"type": "minecraft:smelting", "ingredient": gear, "result": {"id": f"{NS}:copper_nugget"},
                                         "experience": 0.1, "cookingtime": 200})
write(R / "furnace/copper_nugget_blasting.json", {"type": "minecraft:blasting", "ingredient": gear, "result": {"id": f"{NS}:copper_nugget"},
                                                  "experience": 0.1, "cookingtime": 100})
write(RES / "data/c/tags/item/nuggets/copper.json", {"replace": False, "values": [f"{NS}:copper_nugget"]})
print("copper:", len(ITEMS), "items")

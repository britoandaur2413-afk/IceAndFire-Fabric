"""Lists minecraft: ids used in the mod's resources that 1.21.1 does not know (items, blocks, tags, entities...)."""
import json
import pathlib
import re
import collections

TOOLS = pathlib.Path(__file__).resolve().parent
V = TOOLS / "ref/vanilla"
RES = TOOLS.parent / "src/main/resources"

lang = json.loads((V / "assets/minecraft/lang/en_us.json").read_text(encoding="utf-8"))
known = set()
for k in lang:
    p = k.split(".")
    if len(p) >= 3 and p[1] == "minecraft":
        known.add(p[2])
known |= {f.stem for f in (V / "assets/minecraft/models/item").glob("*.json")}
known |= {f.stem for f in (V / "assets/minecraft/blockstates").glob("*.json")}
data = V / "data/minecraft"
for f in data.rglob("*.json"):
    rel = f.relative_to(data).with_suffix("").as_posix()
    known.add(rel)
    known.add(rel.split("/", 1)[1] if "/" in rel else rel)
    # tags: tags/item/x -> x under kind
    if rel.startswith("tags/"):
        known.add(rel.split("/", 2)[2])
    if rel.startswith("worldgen/"):
        known.add(rel.split("/", 2)[2])
unknown = collections.defaultdict(set)
for f in RES.rglob("*.json"):
    t = f.read_text(encoding="utf-8")
    for m in re.finditer(r'"#?minecraft:([a-z0-9_/.]+)"', t):
        i = m.group(1)
        if i not in known:
            unknown[i].add(f.relative_to(RES).as_posix())
for i in sorted(unknown):
    fs = sorted(unknown[i])
    print(i, len(fs), fs[:3])

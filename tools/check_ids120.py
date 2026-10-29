"""1.20.1: the game's ids the mod's data and assets name (minecraft:...) that 1.20.1 does not have.
Known ids come from the 1.20.1 sources (blocks, items, entities, effects, enchantments, particles, sounds) and from
the game's jar (its tags, loot tables, textures, models)."""
import json
import pathlib
import re
import sys
import zipfile

ROOT = pathlib.Path(__file__).resolve().parent.parent
MC = ROOT / "mcsrc/net/minecraft"
JAR = pathlib.Path.home() / ".gradle/caches/fabric-loom/1.20.1/minecraft-client.jar"


def names(path, pattern):
    return set(re.findall(pattern, (MC / path).read_text(encoding="utf-8")))


known = set()
known |= names("world/level/block/Blocks.java", r'register\(\s*"([a-z0-9_]+)"')
known |= names("world/item/Items.java", r'registerItem\(\s*"([a-z0-9_]+)"')
known |= {b.lower() for b in names("world/item/Items.java", r'registerBlock\(\s*Blocks\.([A-Z0-9_]+)')}
known |= names("world/entity/EntityType.java", r'register\(\s*"([a-z0-9_]+)"')
known |= names("world/effect/MobEffects.java", r'register\(\s*(?:\d+,\s*)?"([a-z0-9_]+)"')
known |= names("world/item/enchantment/Enchantments.java", r'register\(\s*"([a-z0-9_]+)"')
known |= names("core/particles/ParticleTypes.java", r'register\(\s*"([a-z0-9_]+)"')
known |= names("sounds/SoundEvents.java", r'register\w*\(\s*"([a-z0-9_.]+)"')
known |= names("world/item/alchemy/Potions.java", r'register\(\s*"([a-z0-9_]+)"')
tags = set()
files = set()
with zipfile.ZipFile(JAR) as z:
    for n in z.namelist():
        m = re.match(r"data/minecraft/tags/[a-z_/]+?/(.+)\.json$", n)
        if m:
            tags.add(m.group(1))
        m = re.match(r"(?:data|assets)/minecraft/(?:[a-z_]+)/(.+)\.(?:json|png|nbt)$", n)
        if m:
            files.add(m.group(1))

missing = {}
for base in [ROOT / "src/main/resources/data", ROOT / "src/main/resources/assets"]:
    for f in base.rglob("*.json"):
        text = f.read_text(encoding="utf-8")
        for ref in re.findall(r'"(#?minecraft:[a-z0-9_/.]+)"', text):
            tag = ref.startswith("#")
            rid = ref.split(":", 1)[1]
            if tag:
                ok = rid in tags
            else:
                ok = rid in known or rid in files or rid.split("/")[-1] in known or rid in tags
            if not ok:
                missing.setdefault(ref, set()).add(f.relative_to(ROOT).as_posix())
for ref, where in sorted(missing.items()):
    print(ref, len(where), sorted(where)[:3])
sys.exit(0)

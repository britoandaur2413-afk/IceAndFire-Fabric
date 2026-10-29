"""The 1.20.1 class list (tools/mc120_classes.txt) and the Minecraft imports of the code that 1.20.1 does not have."""
import collections
import glob
import os
import pathlib
import re
import zipfile

ROOT = pathlib.Path(__file__).resolve().parent.parent
jar = [p for p in glob.glob(os.path.expanduser(
    "~/.gradle/caches/fabric-loom/minecraftMaven/net/minecraft/minecraft-merged/1.20.1-loom.mappings.1_20_1.layered+hash.2198-v2/*.jar"))
       if not p.endswith("backup")][0]
names = set()
for n in zipfile.ZipFile(jar).namelist():
    if n.endswith(".class"):
        names.add(n[:-6].replace("/", ".").replace("$", "."))
(ROOT / "tools/mc120_classes.txt").write_text("\n".join(sorted(names)) + "\n", encoding="utf-8")
missing = collections.Counter()
for f in (ROOT / "src").rglob("*.java"):
    for m in re.finditer(r"^import (?:static )?(net\.minecraft\.[\w.]+|com\.mojang\.[\w.]+);", f.read_text(encoding="utf-8"), re.M):
        c = m.group(1)
        if c not in names and c.rsplit(".", 1)[0] not in names:
            missing[c] += 1
for c, n in missing.most_common(200):
    print(n, c)
print(len(missing), "missing")

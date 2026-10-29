"""Saved data classes: the 1.21.1 save(CompoundTag, HolderLookup.Provider) calls their own save(CompoundTag), and
the storage is asked with the compat type's factory and file name."""
import pathlib
import re
import sys

sys.path.insert(0, str(pathlib.Path(__file__).parent))
from jedit import match  # noqa: E402

ROOT = pathlib.Path(__file__).resolve().parent.parent


def ensure_import(text, fq):
    if re.search(r"(?m)^import " + re.escape(fq) + ";", text):
        return text
    m = re.search(r"(?m)^package [\w.]+;\n", text)
    return text[:m.end()] + "\nimport " + fq + ";" + text[m.end():]


for f in (ROOT / "src/main/java").rglob("*.java"):
    t = f.read_text(encoding="utf-8")
    old = t
    t = t.replace("import net.minecraft.world.level.saveddata.SavedDataType;", "import com.iafabric.compat.SavedDataType;")
    t = t.replace("import net.minecraft.world.level.storage.SavedDataStorage;", "import net.minecraft.world.level.storage.DimensionDataStorage;")
    t = re.sub(r"\bSavedDataStorage\b", "DimensionDataStorage", t)
    t = re.sub(r"\.computeIfAbsent\(((?:\w+\.)*TYPE)\)", r".computeIfAbsent(\1.factory(), \1.name())", t)
    # every class extending SavedData gets the 1.21.1 save
    for m in reversed(list(re.finditer(r"\bclass\s+(\w+)\s+extends\s+SavedData\s*\{", t))):
        open_idx = m.end() - 1
        close = match(t, open_idx)
        inner = t[open_idx:close]
        if "HolderLookup.Provider registries)" in inner:
            continue
        ind = re.match(r"[ \t]*", t[t.rfind("\n", 0, m.start()) + 1:]).group(0)
        step = "\t" if "\t" in ind or "\n\t" in t else "    "
        method = (f"\n{ind}{step}@Override\n{ind}{step}public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {{\n"
                  f"{ind}{step}{step}return this.save(tag);\n{ind}{step}}}\n{ind}")
        t = t[:close] + method + t[close:]
        t = ensure_import(t, "net.minecraft.core.HolderLookup")
        t = ensure_import(t, "net.minecraft.nbt.CompoundTag")
    if t != old:
        f.write_text(t, encoding="utf-8")
        print("saved data:", f.name)

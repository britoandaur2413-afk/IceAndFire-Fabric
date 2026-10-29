"""Text-level fixes from the 26.3 sources to 1.21.1 (Mojang names). Idempotent: run it again after edits.

usage: python -I tools/back.py [src dirs...]   (default: src/main/java src/client/java src/gametest/java)
"""
import pathlib
import re
import sys

ROOT = pathlib.Path(__file__).resolve().parent.parent
CLASSES = set((ROOT / "tools" / "mc121_classes.txt").read_text(encoding="utf-8").split())

# simple name -> 1.21.1 fully qualified names (nested classes as Outer.Inner)
BY_NAME = {}
for fq in CLASSES:
    dotted = fq.replace("$", ".")
    BY_NAME.setdefault(dotted.rsplit(".", 1)[1], set()).add(dotted)
KNOWN = {fq.replace("$", ".") for fq in CLASSES}

# classes that 26.x renamed (not only moved): old 26.x simple name -> 1.21.1 fully qualified name
RENAMED = {
    "net.minecraft.resources.Identifier": "net.minecraft.resources.ResourceLocation",
    "net.minecraft.world.entity.EntitySpawnReason": "net.minecraft.world.entity.MobSpawnType",
    "net.minecraft.world.item.ItemUseAnimation": "net.minecraft.world.item.UseAnim",
    "net.minecraft.client.renderer.rendertype.RenderTypes": "net.minecraft.client.renderer.RenderType",
    "net.minecraft.world.entity.EntityTypes": "net.minecraft.world.entity.EntityType",
    "net.minecraft.world.level.storage.ValueOutput": "com.iafabric.compat.ValueOutput",
    "net.minecraft.world.level.storage.ValueInput": "com.iafabric.compat.ValueInput",
}


def ensure_import(text, fq):
    if re.search(r"(?m)^import " + re.escape(fq) + ";", text):
        return text
    m = re.search(r"(?m)^package [\w.]+;\n", text)
    return text[:m.end()] + "\nimport " + fq + ";" + text[m.end():]


# 26.x saves take a ValueOutput/ValueInput; 1.21.1 ones a CompoundTag (and block entities the registries):
# the method takes the tag and wraps it in the compat ValueOutput/ValueInput under the old name
SAVES = [
    ("addAdditionalSaveData", "ValueOutput", "public void addAdditionalSaveData(CompoundTag {n}Tag)", "this.registryAccess()", "super.addAdditionalSaveData({n}Tag)"),
    ("readAdditionalSaveData", "ValueInput", "public void readAdditionalSaveData(CompoundTag {n}Tag)", "this.registryAccess()", "super.readAdditionalSaveData({n}Tag)"),
    ("saveAdditional", "ValueOutput", "protected void saveAdditional(CompoundTag {n}Tag, HolderLookup.Provider registries)", "registries", "super.saveAdditional({n}Tag, registries)"),
    ("loadAdditional", "ValueInput", "protected void loadAdditional(CompoundTag {n}Tag, HolderLookup.Provider registries)", "registries", "super.loadAdditional({n}Tag, registries)"),
]


def saves(text):
    for method, kind, decl, regs, sup in SAVES:
        pat = re.compile(r"(?:public|protected)\s+void\s+" + method + r"\(\s*(?:final\s+)?" + kind + r"\s+(\w+)\s*\)\s*\{")
        names = []

        def rep(m):
            n = m.group(1)
            names.append(n)
            indent = "\t\t"
            return decl.format(n=n) + " {\n" + indent + f"{kind} {n} = new {kind}({n}Tag, {regs});"

        text = pat.sub(rep, text)
        for n in names:
            text = re.sub(r"super\." + method + r"\(\s*" + n + r"\s*\)", sup.format(n=n), text)
            text = ensure_import(text, "net.minecraft.nbt.CompoundTag")
            if "HolderLookup" in decl:
                text = ensure_import(text, "net.minecraft.core.HolderLookup")
    return text

SIMPLE = [
    (r"\borg\.jspecify\.annotations\.Nullable\b", "org.jetbrains.annotations.Nullable"),
    (r"\borg\.jspecify\.annotations\.NonNull\b", "org.jetbrains.annotations.NotNull"),
    (r"@NonNull\b", "@NotNull"),
    (r"(?m)^import org\.jspecify\.annotations\.NullMarked;\n", ""),
    (r"@NullMarked\s*", ""),
    (r"\bIdentifier\b", "ResourceLocation"),
    (r"\.identifier\(\)", ".location()"),
    (r"\.isClientSide\(\)", ".isClientSide"),
    (r"\bEntitySpawnReason\b", "MobSpawnType"),
    (r"\bMobSpawnType\.SPAWN_ITEM_USE\b", "MobSpawnType.SPAWN_EGG"),
    (r"\bRenderTypes\.", "RenderType."),
    (r"\bEntityTypes\.", "EntityType."),
    (r"\.hurtOrSimulate\(", ".hurt("),
    (r"\.isLocalInstanceAuthoritative\(\)", ".isControlledByLocalInstance()"),
    (r"\.applyEffectsFromBlocks\(\)", ".checkInsideBlocks()"),
    (r"\.noCollision\(\)", ".noCollission()"),
    (r"\bItemUseAnimation\b", "UseAnim"),
    (r"\.syncVelocity\b", ".hurtMarked"),
    (r"\bsyncVelocity\s*=", "hurtMarked ="),
    (r"\.getCompoundOrEmpty\(", ".getCompound("),
    (r"\.absSnapTo\(", ".absMoveTo("),
    (r"\bsnapTo\(", "moveTo("),
    (r"\bMobEffects\.SLOWNESS\b", "MobEffects.MOVEMENT_SLOWDOWN"),
    (r"\bMobEffects\.STRENGTH\b", "MobEffects.DAMAGE_BOOST"),
    (r"\bMobEffects\.HASTE\b", "MobEffects.DIG_SPEED"),
    (r"\bMobEffects\.MINING_FATIGUE\b", "MobEffects.DIG_SLOWDOWN"),
    (r"\bMobEffects\.INSTANT_HEALTH\b", "MobEffects.HEAL"),
    (r"\bMobEffects\.INSTANT_DAMAGE\b", "MobEffects.HARM"),
    (r"\bMobEffects\.JUMP_BOOST\b", "MobEffects.JUMP"),
    (r"\bMobEffects\.NAUSEA\b", "MobEffects.CONFUSION"),
    (r"\bMobEffects\.RESISTANCE\b", "MobEffects.DAMAGE_RESISTANCE"),
    (r"\bMobEffects\.SPEED\b", "MobEffects.MOVEMENT_SPEED"),
]

IMPORT = re.compile(r"(?m)^import (static )?((?:net\.minecraft|com\.mojang)\.[\w.]+);")
FQN = re.compile(r"\b((?:net\.minecraft|com\.mojang)\.(?:[a-z_][a-z0-9_]*\.)+[A-Z]\w*(?:\.[A-Z]\w*)*)")


def relocate(fq):
    """The 1.21.1 name of a 26.x class name, or None when it is fine or unknown."""
    if fq in RENAMED:
        return RENAMED[fq]
    if fq in KNOWN:
        return None
    parts = fq.split(".")
    i = next((k for k, p in enumerate(parts) if p[:1].isupper()), None)
    if i is None:
        return None
    top = ".".join(parts[:i + 1])
    rest = parts[i + 1:]
    if top in KNOWN:
        return None
    new_top = RENAMED.get(top)
    if new_top is None:
        # a top-level class of that name somewhere else in 1.21.1
        cands = {c for c in BY_NAME.get(parts[i], set()) if c.split(".")[-2][:1].islower()}
        if len(cands) != 1:
            return None
        new_top = cands.pop()
    return ".".join([new_top] + rest)


def fix(text):
    def imp(m):
        static, fq = m.group(1) or "", m.group(2)
        target = fq
        if static:
            # import static a.b.C.member
            cls, member = fq.rsplit(".", 1)
            new = relocate(cls)
            return f"import static {new}.{member};" if new else m.group(0)
        new = relocate(target)
        return f"import {new};" if new else m.group(0)

    text = IMPORT.sub(imp, text)

    def inline(m):
        new = relocate(m.group(1))
        return new if new else m.group(1)

    text = FQN.sub(inline, text)
    for pat, rep in SIMPLE:
        text = re.sub(pat, rep, text)
    text = saves(text)
    # an import of a renamed class may now be a duplicate
    seen = set()
    out = []
    for line in text.split("\n"):
        if line.startswith("import "):
            if line in seen:
                continue
            seen.add(line)
        out.append(line)
    return "\n".join(out)


def main():
    dirs = sys.argv[1:] or ["src/main/java", "src/client/java", "src/gametest/java"]
    changed = 0
    for d in dirs:
        for f in (ROOT / d).rglob("*.java"):
            old = f.read_text(encoding="utf-8")
            new = fix(old)
            if new != old:
                f.write_text(new, encoding="utf-8")
                changed += 1
    print("files changed:", changed)


if __name__ == "__main__":
    main()

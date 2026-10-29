"""1.21.1 code down to 1.20.1, the mechanical part (runs again without harm). What it changes:
- ResourceLocation.parse/fromNamespaceAndPath/withDefaultNamespace -> constructors
- synched data: defineSynchedData(Builder) -> defineSynchedData() with entityData.define
- finalizeSpawn gets its fifth argument (the spawn's tag), getBaseExperienceReward -> getExperienceReward
- block entities: load/save/update tag without the registries, container helpers too
- renamed members: getDefaultPickupItem, getAgeScale, getDefaultDimensions, igniteForSeconds, SHORT_GRASS, setTame,
  getUseDuration, canBeLeashed, attribute operations, spawn placement types, chunk status, path types
- item tooltips get the level again, item damage breaks with a callback
- entities have no registryAccess(): their level's
"""
import pathlib
import re
import sys

sys.path.insert(0, str(pathlib.Path(__file__).parent))
from jedit import args, match  # noqa: E402

ROOT = pathlib.Path(__file__).resolve().parent.parent

SIMPLE = [
    (r"\b((?:net\.minecraft\.resources\.)?)ResourceLocation\.(?:parse|fromNamespaceAndPath|withDefaultNamespace)\(", r"new \1ResourceLocation("),
    (r"\bgetBaseExperienceReward\(\)", "getExperienceReward()"),
    (r"protected int getExperienceReward\(\)", "public int getExperienceReward()"),
    (r"\bgetDefaultPickupItem\(\)", "getPickupItem()"),
    (r"\bgetAgeScale\(\)", "getScale()"),
    (r"\bgetDefaultDimensions\(Pose\b", "getDimensions(Pose"),
    (r"\bigniteForSeconds\(", "setSecondsOnFire("),
    (r"\b(Blocks|Items)\.SHORT_GRASS\b", r"\1.GRASS"),
    (r"\bOperation\.ADD_VALUE\b", "Operation.ADDITION"),
    (r"\bOperation\.ADD_MULTIPLIED_BASE\b", "Operation.MULTIPLY_BASE"),
    (r"\bOperation\.ADD_MULTIPLIED_TOTAL\b", "Operation.MULTIPLY_TOTAL"),
    (r"\bSpawnPlacementTypes\.(ON_GROUND|IN_WATER|IN_LAVA|NO_RESTRICTIONS)\b", r"SpawnPlacements.Type.\1"),
    (r"import net\.minecraft\.world\.entity\.SpawnPlacementTypes;", "import net.minecraft.world.entity.SpawnPlacements;"),
    (r"net\.minecraft\.world\.level\.chunk\.status\.ChunkStatus\b", "net.minecraft.world.level.chunk.ChunkStatus"),
    (r"import net\.minecraft\.world\.level\.pathfinder\.PathType;", "import net.minecraft.world.level.pathfinder.BlockPathTypes;"),
    (r"\bPathType\b", "BlockPathTypes"),
    (r"\bRegistryFriendlyByteBuf\b", "FriendlyByteBuf"),
    (r"import net\.minecraft\.network\.FriendlyByteBuf;\n(import net\.minecraft\.network\.FriendlyByteBuf;\n)+", "import net.minecraft.network.FriendlyByteBuf;\n"),
    (r"\bthis\.registryAccess\(\)", "this.level().registryAccess()"),
    (r"\.setTame\(([^,()]+), (?:true|false)\)", r".setTame(\1)"),
    (r"\bsetTame\(([^,()]+), (?:true|false)\);", r"setTame(\1);"),
    (r"\bContainerHelper\.(loadAllItems|saveAllItems)\(([^,()]+), ([^,()]+(?:\(\))?), ([^,()]+(?:\(\))?)\)", r"ContainerHelper.\1(\2, \3)"),
    (r"\.saveWithoutMetadata\([^()]*(?:\(\))?\)", ".saveWithoutMetadata()"),
    # loot tables are named by their ids
    (r"ResourceKey\.create\(Registries\.LOOT_TABLE, ([^(),]+(?:\([^()]*\))?)\)", r"\1"),
    (r"\bResourceKey<LootTable>", "ResourceLocation"),
    (r"\.reloadableRegistries\(\)\.getLootTable\(", ".getLootData().getLootTable("),
]


def method_body(text, start):
    o = text.index("{", start)
    return o, match(text, o)


def synched_data(t):
    m = re.search(r"(protected|public) void defineSynchedData\(\s*(?:final\s+)?(?:@\w+\s+)?(?:SynchedEntityData\.)?Builder (\w+)\s*\)", t)
    if not m:
        return t
    name = m.group(2)
    t = t[:m.start()] + m.group(1) + " void defineSynchedData()" + t[m.end():]
    o, c = method_body(t, m.start())
    body = t[o:c]
    body = re.sub(r"\bsuper\.defineSynchedData\(\s*" + name + r"\s*\)", "super.defineSynchedData()", body)
    body = re.sub(r"\b" + name + r"\.define\(", "this.entityData.define(", body)
    return t[:o] + body + t[c:]


def finalize_spawn(t):
    decl = re.compile(r"finalizeSpawn\(\s*((?:@\w+\s+)*ServerLevelAccessor \w+,\s*(?:@\w+\s+)*DifficultyInstance \w+,\s*(?:@\w+\s+)*MobSpawnType \w+,\s*"
                      r"(?:@\w+\s+)*SpawnGroupData \w+)\s*\)")
    t, n = decl.subn(r"finalizeSpawn(\1, @Nullable CompoundTag dataTag)", t)
    out = []
    pos = 0
    for m in re.finditer(r"\bfinalizeSpawn\(", t):
        paren = m.end() - 1
        if paren < pos:
            continue
        spans, close = args(t, paren)
        a = [t[s:e].strip() for s, e in spans]
        if len(a) == 4 and not a[0].startswith("ServerLevelAccessor") and "ServerLevelAccessor " not in a[0]:
            extra = "dataTag" if t[max(0, m.start() - 6):m.start()] == "super." and "CompoundTag dataTag" in t else "null"
            out.append(t[pos:close])
            out.append(", " + extra)
            pos = close
    out.append(t[pos:])
    t = "".join(out)
    return t


def block_entity(t):
    # load/save/update tag without the registries
    for name, new in (("loadAdditional", "load"), ("saveAdditional", "saveAdditional")):
        m = re.search(r"(protected|public) void " + name + r"\(\s*(?:@\w+\s+)?CompoundTag (\w+),\s*(?:@\w+\s+)?(?:HolderLookup\.)?Provider (\w+)\s*\)", t)
        if m:
            tag, prov = m.group(2), m.group(3)
            t = t[:m.start()] + f"{'public' if new == 'load' else m.group(1)} void {new}(CompoundTag {tag})" + t[m.end():]
            t = re.sub(r"\bsuper\." + name + r"\(\s*" + tag + r"\s*,\s*" + prov + r"\s*\)", f"super.{new}({tag})", t)
    m = re.search(r"public CompoundTag getUpdateTag\(\s*(?:@\w+\s+)?(?:HolderLookup\.)?Provider (\w+)\s*\)", t)
    if m:
        t = t[:m.start()] + "public CompoundTag getUpdateTag()" + t[m.end():]
    return t


def tooltips(t):
    m = re.search(r"appendHoverText\(\s*(?:@\w+\s+)?ItemStack (\w+),\s*(?:@\w+\s+)?(?:Item\.)?TooltipContext (\w+),", t)
    if not m:
        return t
    ctx = m.group(2)
    t = t[:m.start()] + f"appendHoverText(ItemStack {m.group(1)}, @Nullable Level {ctx}," + t[m.end():]
    t = re.sub(r"\b" + ctx + r"\.level\(\)", ctx, t)
    return t


def use_duration(t):
    t = re.sub(r"getUseDuration\(\s*(ItemStack \w+),\s*LivingEntity \w+\s*\)", r"getUseDuration(\1)", t)
    t = re.sub(r"\.getUseDuration\(([^,()]+(?:\(\))?),\s*[^,()]+(?:\(\))?\)", r".getUseDuration(\1)", t)
    t = re.sub(r"\bsuper\.getUseDuration\(([^,()]+),\s*[^,()]+\)", r"super.getUseDuration(\1)", t)
    return t


def leash(t):
    t = re.sub(r"public boolean canBeLeashed\(\)", "public boolean canBeLeashed(Player player)", t)
    return t


def hurt_and_break(t):
    out = []
    pos = 0
    for m in re.finditer(r"\.hurtAndBreak\(", t):
        paren = m.end() - 1
        if paren < pos:
            continue
        spans, close = args(t, paren)
        a = [t[s:e].strip() for s, e in spans]
        if len(a) == 3 and "->" not in a[2] and ("EquipmentSlot" in a[2] or "getSlotForHand" in a[2]):
            slot = re.sub(r"LivingEntity\.getSlotForHand\((\w+)\)",
                          r"(\1 == net.minecraft.world.InteractionHand.MAIN_HAND ? EquipmentSlot.MAINHAND : EquipmentSlot.OFFHAND)", a[2])
            out.append(t[pos:paren + 1])
            out.append(f"{a[0]}, {a[1]}, broken -> broken.broadcastBreakEvent({slot}))")
            pos = close + 1
    out.append(t[pos:])
    return "".join(out)


def add_import(t, imp):
    if f"import {imp};" in t:
        return t
    simple = imp.rsplit(".", 1)[1]
    if not re.search(r"\b" + simple + r"\b", t.split("\nimport ", 1)[-1] if "\nimport " in t else t):
        return t
    m = re.search(r"^package [\w.]+;\n", t, re.M)
    return t[:m.end()] + f"\nimport {imp};" + t[m.end():]


def convert(f):
    t = f.read_text(encoding="utf-8")
    o = t
    for a, b in SIMPLE:
        t = re.sub(a, b, t)
    t = synched_data(t)
    t = finalize_spawn(t)
    t = block_entity(t)
    t = tooltips(t)
    t = use_duration(t)
    t = leash(t)
    t = hurt_and_break(t)
    if t != o:
        if "@Nullable CompoundTag dataTag" in t:
            t = add_import(t, "net.minecraft.nbt.CompoundTag")
        if "@Nullable" in t and "import org.jetbrains.annotations.Nullable;" not in t and "import javax.annotation.Nullable;" not in t:
            t = add_import(t, "org.jetbrains.annotations.Nullable")
        if "@Nullable Level " in t:
            t = add_import(t, "net.minecraft.world.level.Level")
        if "canBeLeashed(Player player)" in t:
            t = add_import(t, "net.minecraft.world.entity.player.Player")
        if "SpawnPlacements.Type" in t:
            t = add_import(t, "net.minecraft.world.entity.SpawnPlacements")
        if "broadcastBreakEvent(EquipmentSlot" in t or "EquipmentSlot.MAINHAND : EquipmentSlot.OFFHAND" in t:
            t = add_import(t, "net.minecraft.world.entity.EquipmentSlot")
        f.write_text(t, encoding="utf-8")
        return True
    return False


if __name__ == "__main__":
    n = 0
    for d in sys.argv[1:] or ["src/main/java", "src/client/java", "src/gametest/java"]:
        for f in (ROOT / d).rglob("*.java"):
            n += convert(f)
    print("down120:", n, "files")

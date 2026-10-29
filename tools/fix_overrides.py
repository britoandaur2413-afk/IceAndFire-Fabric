"""Migrate overridden methods whose signature changed between 1.18.2 and 26.x.

Works on declarations (name + old parameter types), rewriting the signature and the matching
super call inside the body. Methods that no longer exist at all are removed.

usage: python -I tools/fix_overrides.py
"""
import pathlib
import re
import sys

sys.path.insert(0, str(pathlib.Path(__file__).parent))
import jtools  # noqa: E402
import fix_text  # noqa: E402

ROOT = pathlib.Path(__file__).resolve().parent.parent
SRC_DIRS = [ROOT / "src" / "main" / "java", ROOT / "src" / "client" / "java"]
SKIP_PARTS = {"citadel", "mixin"}

DECL = re.compile(r"(?P<annos>(?:@\w+(?:\([^)]*\))?\s+)*)(?P<mods>(?:(?:public|protected|private|static|final|synchronized)\s+)*)"
                  r"(?P<ret>[\w.<>\[\], ?]+?)\s+(?P<name>\w+)\s*\((?P<params>[^()]*(?:\([^()]*\)[^()]*)*)\)\s*(?:throws [\w., ]+)?\s*\{")


def param_types(params):
    out = []
    for p in jtools.split_args(params):
        p = re.sub(r"@\w+(\([^)]*\))?\s*", "", p).strip()
        p = re.sub(r"\bfinal\s+", "", p)
        if not p:
            continue
        parts = p.rsplit(None, 1)
        out.append((parts[0].strip(), parts[1].strip() if len(parts) > 1 else ""))
    return out


def simple(t):
    t = re.sub(r"<.*>", "", t).strip()
    return t.split(".")[-1]


def find_methods(text):
    """yield (start, body_open, body_close, match) for method declarations"""
    for m in DECL.finditer(text):
        if m.group("name") in ("if", "for", "while", "switch", "catch", "synchronized", "return", "new"):
            continue
        open_i = m.end() - 1
        close = jtools.match_paren(text, open_i)
        if close < 0:
            continue
        yield m, open_i, close


def remove_method(text, m, close):
    start = m.start()
    # also eat preceding @Override / blank indentation on that line
    line_start = text.rfind("\n", 0, start) + 1
    if text[line_start:start].strip() == "":
        start = line_start
    end = close + 1
    if end < len(text) and text[end] == "\n":
        end += 1
    return text[:start] + text[end:]


def super_call_replace(body, name, fn):
    return fix_text.rewrite_methods(body, name, lambda recv, args, argtext: fn(args) if recv == "super" else None)


REMOVE = {
    ("getAddEntityPacket", 0), ("getMobType", 0), ("canBeControlledByRider", 0), ("getCapability", 2), ("invalidateCaps", 0),
    ("getArmorTexture", 4), ("initializeClient", 1), ("getArmorModel", 4), ("fillItemCategory", 2),
    ("shouldCauseReequipAnimation", 3), ("getDefaultAttributeModifiers", 1), ("onDataPacket", 2), ("handleUpdateTag", 1),
    ("getItemStackRenderer", 0), ("getRenderBoundingBox", 0), ("onlyOpCanSetNbt", 0), ("canBeRiddenInWater", 1),
    ("getMyRidingOffset", 0), ("shouldRiderSit", 0), ("getPistonPushReaction", 1), ("isSaddleable", 0),
    ("isControlledByLocalInstance", 0), ("canApplyAtEnchantingTable", 2), ("isLocalInstanceAuthoritative", 0),
    ("isAddedToWorld", 0), ("getArmorSlots", 0), ("getBrightness", 0), ("isValidRepairItem", 2), ("onAboveBubbleCol", 1),
}


def migrate(text):
    changed = True
    guard = 0
    while changed and guard < 500:
        changed = False
        guard += 1
        for m, open_i, close in find_methods(text):
            name = m.group("name")
            ptypes = param_types(m.group("params"))
            types = [simple(t) for t, _ in ptypes]
            names = [n for _, n in ptypes]
            body = text[open_i:close + 1]
            new_sig = None
            new_body = body
            annos = m.group("annos")
            mods = m.group("mods")
            ret = m.group("ret")
            if "@Override" not in annos and (name, len(types)) not in {("hurt", 2)}:
                # only touch overrides (and Forge-only hooks without @Override are rare)
                if (name, len(types)) not in REMOVE:
                    continue
            if (name, len(types)) in REMOVE:
                text = remove_method(text, m, close)
                changed = True
                break
            if name == "addAdditionalSaveData" and types == ["CompoundTag"]:
                new_sig = "%s%svoid addAdditionalSaveData(ValueOutput %s) " % (annos, mods, names[0])
            elif name == "readAdditionalSaveData" and types == ["CompoundTag"]:
                new_sig = "%s%svoid readAdditionalSaveData(ValueInput %s) " % (annos, mods, names[0])
            elif name == "saveAdditional" and types == ["CompoundTag"]:
                new_sig = "%sprotected void saveAdditional(ValueOutput %s) " % (annos, names[0])
            elif name == "load" and types == ["CompoundTag"] and ret.strip() == "void":
                new_sig = "%sprotected void loadAdditional(ValueInput %s) " % (annos, names[0])
                new_body = super_call_replace(new_body, "load", lambda a: "super.loadAdditional(" + ", ".join(a) + ")")
            elif name == "getUpdateTag" and types == []:
                new_sig = "%spublic CompoundTag getUpdateTag(HolderLookup.Provider registries) " % annos
                new_body = re.sub(r"\bthis\.save\(new CompoundTag\(\)\)|\bsave\(new CompoundTag\(\)\)|\bthis\.saveWithoutMetadata\(\)|\bsaveWithoutMetadata\(\)"
                                  r"|\bthis\.saveWithFullMetadata\(\)|\bsaveWithFullMetadata\(\)",
                                  "this.saveWithoutMetadata(registries)", new_body)
            elif name == "startOpen" and types == ["Player"]:
                new_sig = "%spublic void startOpen(ContainerUser %s) " % (annos, names[0])
            elif name == "stopOpen" and types == ["Player"]:
                new_sig = "%spublic void stopOpen(ContainerUser %s) " % (annos, names[0])
            elif name == "inventoryTick" and len(types) == 5:
                new_sig = "%spublic void inventoryTick(ItemStack %s, ServerLevel %s, Entity %s, @Nullable EquipmentSlot slot) " % (
                    annos, names[0], names[1], names[2])
                new_body = re.sub(r"\b" + names[4] + r"\b", "(slot == EquipmentSlot.MAINHAND)", new_body)
                new_body = re.sub(r"\b" + names[3] + r"\b", "0", new_body)
            elif name == "onUsingTick" and len(types) == 3:
                new_sig = "%spublic void onUseTick(Level level, LivingEntity %s, ItemStack %s, int %s) " % (annos, names[1], names[0], names[2])
            elif name == "use" and types == ["Level", "Player", "InteractionHand"] and "InteractionResult" in ret:
                new_sig = "%spublic InteractionResult use(Level %s, Player %s, InteractionHand %s) " % (annos, names[0], names[1], names[2])
            elif name == "releaseUsing" and len(types) == 4 and ret.strip() == "void":
                new_sig = "%spublic boolean releaseUsing(ItemStack %s, Level %s, LivingEntity %s, int %s) " % (annos, names[0], names[1], names[2], names[3])
                new_body = re.sub(r"\breturn;", "return false;", new_body)
                new_body = new_body[:-1].rstrip() + "\n        return true;\n    }"
            elif name == "use" and len(types) == 6 and types[0] == "BlockState":
                new_sig = "%sprotected InteractionResult useItemOn(ItemStack heldStack, BlockState %s, Level %s, BlockPos %s, Player %s, InteractionHand %s, BlockHitResult %s) " % (
                    annos, names[0], names[1], names[2], names[3], names[4], names[5])
            elif name == "isPathfindable" and len(types) == 4:
                new_sig = "%sprotected boolean isPathfindable(BlockState %s, PathComputationType %s) " % (annos, names[0], names[3])
            elif name == "getAnalogOutputSignal" and len(types) == 3:
                new_sig = "%sprotected int getAnalogOutputSignal(BlockState %s, Level %s, BlockPos %s, Direction direction) " % (annos, names[0], names[1], names[2])
            elif name == "neighborChanged" and len(types) == 6:
                new_sig = "%sprotected void neighborChanged(BlockState %s, Level %s, BlockPos %s, Block %s, @Nullable Orientation orientation, boolean %s) " % (
                    annos, names[0], names[1], names[2], names[3], names[5])
            elif name == "updateShape" and len(types) == 6 and types[1] == "Direction":
                new_sig = ("%sprotected BlockState updateShape(BlockState %s, LevelReader %s, ScheduledTickAccess tickAccess, BlockPos %s, Direction %s, BlockPos %s, BlockState %s, RandomSource random) "
                           % (annos, names[0], names[3], names[4], names[1], names[5], names[2]))
                new_body = super_call_replace(new_body, "updateShape", lambda a: "super.updateShape(%s, %s, tickAccess, %s, %s, %s, %s, random)" % (a[0], a[3], a[4], a[1], a[5], a[2]))
            elif name == "finalizeSpawn" and len(types) == 5:
                new_sig = "%s%s%s finalizeSpawn(ServerLevelAccessor %s, DifficultyInstance %s, EntitySpawnReason %s, @Nullable SpawnGroupData %s) " % (
                    annos, mods, ret.strip(), names[0], names[1], names[2], names[3])
                new_body = super_call_replace(new_body, "finalizeSpawn", lambda a: "super.finalizeSpawn(" + ", ".join(a[:4]) + ")")
            elif name == "hurt" and types == ["DamageSource", "float"]:
                new_sig = "%s%sboolean hurtServer(ServerLevel level, %s %s, float %s) " % (annos, mods, ptypes[0][0], names[0], names[1])
                new_body = super_call_replace(new_body, "hurt", lambda a: "super.hurtServer(level, " + ", ".join(a) + ")")
            elif name == "doHurtTarget" and types == ["Entity"]:
                new_sig = "%s%sboolean doHurtTarget(ServerLevel level, Entity %s) " % (annos, mods, names[0])
                new_body = super_call_replace(new_body, "doHurtTarget", lambda a: "super.doHurtTarget(level, " + ", ".join(a) + ")")
            elif name == "getExperienceReward" and len(types) == 1:
                new_sig = "%s%sint getBaseExperienceReward(ServerLevel level) " % (annos, mods)
            elif name == "isInvulnerableTo" and types == ["DamageSource"]:
                new_sig = "%s%sboolean isInvulnerableTo(ServerLevel level, DamageSource %s) " % (annos, mods, names[0])
                new_body = super_call_replace(new_body, "isInvulnerableTo", lambda a: "super.isInvulnerableTo(level, " + ", ".join(a) + ")")
            elif name == "getControllingPassenger" and types == [] and simple(ret) == "Entity":
                new_sig = "%s%s@Nullable LivingEntity getControllingPassenger() " % (annos, mods)
            elif name == "onCraftedBy" and len(types) == 3:
                new_sig = "%s%svoid onCraftedBy(ItemStack %s, Player %s) " % (annos, mods, names[0], names[2])
                new_body = re.sub(r"\b" + names[1] + r"\b", names[2] + ".level()", new_body)
            elif name == "hurtEnemy" and len(types) == 3 and ret.strip() == "boolean":
                new_sig = "%s%svoid hurtEnemy(ItemStack %s, LivingEntity %s, LivingEntity %s) " % (annos, mods, names[0], names[1], names[2])
                new_body = re.sub(r"return super\.hurtEnemy\(([^;]*)\);", r"super.hurtEnemy(\1);", new_body)
                new_body = re.sub(r"return (true|false);", "return;", new_body)
            elif name == "getPickupItem" and types == []:
                new_sig = "%sprotected ItemStack getDefaultPickupItem() " % annos
            elif name == "getUseDuration" and types == ["ItemStack"]:
                new_sig = "%s%sint getUseDuration(ItemStack %s, LivingEntity user) " % (annos, mods, names[0])
            elif name == "populateDefaultEquipmentSlots" and types == ["DifficultyInstance"]:
                new_sig = "%s%svoid populateDefaultEquipmentSlots(RandomSource random, DifficultyInstance %s) " % (annos, mods, names[0])
                new_body = super_call_replace(new_body, "populateDefaultEquipmentSlots", lambda a: "super.populateDefaultEquipmentSlots(random, " + ", ".join(a) + ")")
            elif name == "killed" and types == ["ServerLevel", "LivingEntity"]:
                new_sig = "%s%sboolean killedEntity(ServerLevel %s, LivingEntity %s, DamageSource damageSource) " % (annos, mods, names[0], names[1])
                new_body = super_call_replace(new_body, "killed", lambda a: "super.killedEntity(" + ", ".join(a) + ", damageSource)")
                if "return" not in new_body:
                    new_body = new_body[:-1].rstrip() + "\n        return true;\n    }"
            elif name == "entityInside" and len(types) == 4:
                new_sig = "%s%svoid entityInside(BlockState %s, Level %s, BlockPos %s, Entity %s, InsideBlockEffectApplier effectApplier, boolean intersects) " % (
                    annos, mods, names[0], names[1], names[2], names[3])
                new_body = super_call_replace(new_body, "entityInside", lambda a: "super.entityInside(" + ", ".join(a) + ", effectApplier, intersects)")
            elif name == "getDimensions" and types == ["Pose"] and "extends Entity {" not in text:
                new_sig = "%s%sEntityDimensions getDefaultDimensions(Pose %s) " % (annos, mods, names[0])
                new_body = super_call_replace(new_body, "getDimensions", lambda a: "super.getDefaultDimensions(" + ", ".join(a) + ")")
            elif name == "kill" and types == []:
                new_sig = "%s%svoid kill(ServerLevel level) " % (annos, mods)
                new_body = super_call_replace(new_body, "kill", lambda a: "super.kill(level)")
            elif name == "dropCustomDeathLoot" and types == ["DamageSource", "int", "boolean"]:
                new_sig = "%s%svoid dropCustomDeathLoot(ServerLevel level, DamageSource %s, boolean %s) " % (annos, mods, names[0], names[2])
                new_body = super_call_replace(new_body, "dropCustomDeathLoot", lambda a: "super.dropCustomDeathLoot(level, %s, %s)" % (a[0], a[2]))
            elif name == "causeFallDamage" and len(types) == 3 and types[0] == "float":
                new_sig = "%s%sboolean causeFallDamage(double %s, float %s, DamageSource %s) " % (annos, mods, names[0], names[1], names[2])
            elif name == "positionRider" and types == ["Entity"]:
                new_sig = "%s%svoid positionRider(Entity %s, Entity.MoveFunction moveFunction) " % (annos, mods, names[0])
                new_body = super_call_replace(new_body, "positionRider", lambda a: "super.positionRider(" + ", ".join(a) + ", moveFunction)")
            elif name == "isAlliedTo" and types == ["Entity"]:
                new_sig = "%sprotected boolean considersEntityAsAlly(Entity %s) " % (annos, names[0])
                new_body = super_call_replace(new_body, "isAlliedTo", lambda a: "super.considersEntityAsAlly(" + ", ".join(a) + ")")
            elif name == "dropEquipment" and types == []:
                new_sig = "%sprotected void dropEquipment(ServerLevel level) " % annos
                new_body = super_call_replace(new_body, "dropEquipment", lambda a: "super.dropEquipment(level)")
            elif name == "canBeCollidedWith" and types == []:
                new_sig = "%s%sboolean canBeCollidedWith(@Nullable Entity other) " % (annos, mods)
                new_body = super_call_replace(new_body, "canBeCollidedWith", lambda a: "super.canBeCollidedWith(other)")
            if new_sig is not None:
                updated = text[:m.start()] + new_sig + new_body + text[close + 1:]
                if updated != text:
                    text = updated
                    changed = True
                    break
    return text


IMPORTS = {
    "ValueOutput": "net.minecraft.world.level.storage.ValueOutput",
    "ValueInput": "net.minecraft.world.level.storage.ValueInput",
    "EntitySpawnReason": "net.minecraft.world.entity.EntitySpawnReason",
    "ServerLevel": "net.minecraft.server.level.ServerLevel",
    "HolderLookup": "net.minecraft.core.HolderLookup",
    "Nullable": "org.jspecify.annotations.Nullable",
    "InsideBlockEffectApplier": "net.minecraft.world.entity.InsideBlockEffectApplier",
    "EntityDimensions": "net.minecraft.world.entity.EntityDimensions",
    "LivingEntity": "net.minecraft.world.entity.LivingEntity",
    "DamageSource": "net.minecraft.world.damagesource.DamageSource",
    "CompoundTag": "net.minecraft.nbt.CompoundTag",
    "ContainerUser": "net.minecraft.world.entity.ContainerUser",
    "EquipmentSlot": "net.minecraft.world.entity.EquipmentSlot",
    "PathComputationType": "net.minecraft.world.level.pathfinder.PathComputationType",
    "Orientation": "net.minecraft.world.level.redstone.Orientation",
    "ScheduledTickAccess": "net.minecraft.world.level.ScheduledTickAccess",
    "LevelReader": "net.minecraft.world.level.LevelReader",
    "RandomSource": "net.minecraft.util.RandomSource",
    "InteractionResult": "net.minecraft.world.InteractionResult",
    "ItemStack": "net.minecraft.world.item.ItemStack",
}


def main():
    n = 0
    for d in SRC_DIRS:
        for p in d.rglob("*.java"):
            if SKIP_PARTS & set(p.relative_to(d).parts):
                continue
            t = p.read_text(encoding="utf-8")
            o = t
            t = re.sub(r"\bMobSpawnType\b", "EntitySpawnReason", t).replace("EntitySpawnReason.SPAWN_EGG", "EntitySpawnReason.SPAWN_ITEM_USE")
            t = t.replace("import net.minecraft.world.entity.EntitySpawnReason;\nimport net.minecraft.world.entity.EntitySpawnReason;", "import net.minecraft.world.entity.EntitySpawnReason;")
            # "public @NotNull Foo bar()" hides the modifiers from the declaration pattern
            t = re.sub(r"\b(public|protected|private)(\s+(?:static\s+|final\s+)*)@(?:NotNull|Nullable|Nonnull)\s+", r"\1\2", t)
            t = migrate(t)
            if t != o:
                for simple_name, fq in IMPORTS.items():
                    if re.search(r"\b" + simple_name + r"\b", t) and ("import " + fq + ";") not in t \
                            and not re.search(r"^import [\w.]+\." + simple_name + ";", t, re.M):
                        t = fix_text.add_import(t, fq)
                t = fix_text.dedupe_imports(t)
                p.write_text(t, encoding="utf-8", newline="\n")
                n += 1
    print("changed", n)


if __name__ == "__main__":
    main()

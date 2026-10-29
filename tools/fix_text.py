"""Global text rules applied in place to the ported sources (idempotent).

usage: python -I tools/fix_text.py
"""
import pathlib
import re
import sys

sys.path.insert(0, str(pathlib.Path(__file__).parent))
import jtools  # noqa: E402

ROOT = pathlib.Path(__file__).resolve().parent.parent
SRC_DIRS = [ROOT / "src" / "main" / "java", ROOT / "src" / "client" / "java", ROOT / "src" / "gametest" / "java"]
SKIP_PARTS = {"citadel", "mixin"}


def rewrite_calls(text, key, fn):
    """rewrite every `key(...)` (key ends right before '(') with fn(args_text) -> replacement or None"""
    out = []
    i = 0
    while True:
        j = text.find(key, i)
        if j < 0:
            out.append(text[i:])
            break
        if j > 0 and (text[j - 1].isalnum() or text[j - 1] in "_$") and key[0].isalnum():
            out.append(text[i:j + 1])
            i = j + 1
            continue
        out.append(text[i:j])
        open_i = j + len(key) - 1
        close = jtools.match_paren(text, open_i)
        if close < 0:
            out.append(text[j:])
            break
        rep = fn(text[open_i + 1:close])
        if rep is None:
            out.append(text[j:close + 1])
        else:
            out.append(rep)
        i = close + 1
    return "".join(out)


def rewrite_methods(text, name, fn):
    """rewrite every `recv.name(args)` with fn(recv, args_list, args_text) -> replacement for the whole
    `recv.name(args)` span, or None to keep it. recv may be '' for an unqualified call."""
    key = name + "("
    pos = len(text)
    while True:
        j = text.rfind(key, 0, pos)
        if j < 0:
            break
        pos = j
        if j > 0 and (text[j - 1].isalnum() or text[j - 1] in "_$"):
            continue
        open_i = j + len(name)
        close = jtools.match_paren(text, open_i)
        if close < 0:
            continue
        k = j - 1
        while k >= 0 and text[k] in " \t\r\n":
            k -= 1
        if k >= 0 and text[k] == ".":
            start = jtools.find_receiver_start(text, k)
            recv = text[start:k].strip()
        else:
            if k >= 0 and (text[k].isalnum() or text[k] in "_$>]"):
                continue  # a declaration: `boolean hurt(...)`
            start = j
            recv = ""
        argtext = text[open_i + 1:close]
        rep = fn(recv, jtools.split_args(argtext), argtext)
        if rep is not None:
            text = text[:start] + rep + text[close + 1:]
    return text


DAMAGE_SOURCES = {
    "IN_FIRE": "inFire", "LIGHTNING_BOLT": "lightningBolt", "ON_FIRE": "onFire", "LAVA": "lava", "HOT_FLOOR": "hotFloor",
    "IN_WALL": "inWall", "CRAMMING": "cramming", "DROWN": "drown", "STARVE": "starve", "CACTUS": "cactus", "FALL": "fall",
    "FLY_INTO_WALL": "flyIntoWall", "OUT_OF_WORLD": "fellOutOfWorld", "GENERIC": "generic", "MAGIC": "magic", "WITHER": "wither",
    "DRAGON_BREATH": "dragonBreath", "DRY_OUT": "dryOut", "SWEET_BERRY_BUSH": "sweetBerryBush", "FREEZE": "freeze",
    "STALAGMITE": "stalagmite",
}


def entity_recv(recv):
    return recv if recv else "this"


def damage_source_expr(src, recv):
    m = re.fullmatch(r"DamageSource\.([A-Z_]+)", src.strip())
    if m and m.group(1) in DAMAGE_SOURCES:
        return entity_recv(recv) + ".damageSources()." + DAMAGE_SOURCES[m.group(1)] + "()"
    m = re.fullmatch(r"DamageSource\.(mobAttack|playerAttack|sting|thorns|indirectMagic|thrown|arrow|trident|fireball|explosion)\((.*)\)", src.strip(), re.S)
    if m:
        return entity_recv(recv) + ".damageSources()." + m.group(1) + "(" + m.group(2) + ")"
    return src


def fix_hurt(recv, args, argtext):
    if len(args) != 2 or recv == "super":
        return None
    src = damage_source_expr(args[0], recv)
    prefix = (recv + ".") if recv else ""
    return prefix + "hurtOrSimulate(" + src + ", " + args[1] + ")"


def fix_knockback(recv, args, argtext):
    if len(args) != 3:
        return None
    prefix = (recv + ".") if recv else ""
    return prefix + "knockback(" + argtext + ", " + entity_recv(recv) + ".damageSources().generic(), 0.0F)"


def fix_seconds_on_fire(recv, args, argtext):
    prefix = (recv + ".") if recv else ""
    return prefix + "igniteForSeconds(" + argtext + ")"


def fix_move_to(recv, args, argtext):
    if len(args) not in (1, 5) or "avigation" in recv or recv in ("nav", "navigator"):
        return None
    prefix = (recv + ".") if recv else ""
    return prefix + "snapTo(" + argtext + ")"


def fix_client_message(recv, args, argtext):
    if len(args) != 2:
        return None
    prefix = (recv + ".") if recv else ""
    if args[1].strip() == "true":
        return prefix + "sendOverlayMessage(" + args[0] + ")"
    if args[1].strip() == "false":
        return prefix + "sendSystemMessage(" + args[0] + ")"
    return None


def fix_set_tame(recv, args, argtext):
    if len(args) != 1:
        return None
    prefix = (recv + ".") if recv else ""
    return prefix + "setTame(" + argtext + ", true)"


def fix_entity_create(recv, args, argtext):
    if len(args) != 1 or not re.search(r"(EntityType\.\w+|IafEntityRegistry\.\w+)$", recv):
        return None
    return recv + ".create(" + argtext + ", EntitySpawnReason.TRIGGERED)"


def fix_spawn_at_location(recv, args, argtext):
    if len(args) not in (1, 2) or "ServerLevel" in args[0] or "level" in args[0].lower():
        return None
    target = recv if recv else "this"
    prefix = (recv + ".") if recv else ""
    if re.search(r"(ItemLike|Items\.|IafItemRegistry\.|Blocks\.|IafBlockRegistry\.)", args[0]) and "Stack" not in args[0]:
        stack = "new ItemStack(" + args[0] + ")"
    else:
        stack = args[0]
    rest = (", " + args[1]) if len(args) == 2 else ""
    return prefix + "spawnAtLocation((ServerLevel) " + target + ".level(), " + stack + rest + ")"


def fix_swing(recv, args, argtext):
    if len(args) != 1 or "Hand" not in args[0] and "hand" not in args[0]:
        return None
    prefix = (recv + ".") if recv else ""
    return prefix + "swing(" + argtext + ", SwingAnimation.DEFAULT, true)"


def fix_start_riding(recv, args, argtext):
    if len(args) != 2:
        return None
    prefix = (recv + ".") if recv else ""
    return prefix + "startRiding(" + argtext + ", true)"


def fix_owner_uuid(recv, args, argtext):
    if args and args[0]:
        return None
    return "EntityUtil.getOwnerUUID(" + (recv if recv else "this") + ")"


def fix_set_owner_uuid(recv, args, argtext):
    if len(args) != 1:
        return None
    return "EntityUtil.setOwnerUUID(" + (recv if recv else "this") + ", " + argtext + ")"


def fix_attribute_modifier(args):
    parts = jtools.split_args(args)
    if len(parts) != 4:
        return None
    key = parts[0].strip()
    m = re.fullmatch(r"(?:\w+\.)*([A-Za-z_][A-Za-z0-9_]*)", key)
    if m:
        ident = 'IafMod.id("' + m.group(1).lower() + '")'
    else:
        ident = 'IafMod.id(' + parts[1].strip().lower().replace(" ", "_") + ')'
    return "new AttributeModifier(" + ident + ", " + parts[2] + ", " + parts[3] + ")"


def fix_remove_modifier(recv, args, argtext):
    if len(args) != 1:
        return None
    m = re.fullmatch(r"(?:\w+\.)*([A-Z][A-Z0-9_]*)", args[0].strip())
    if not m:
        return None
    return recv + '.removeModifier(IafMod.id("' + m.group(1).lower() + '"))'


_CLASS_INDEX = None


def class_index():
    """simple name -> set of fully qualified names of 26.x Minecraft / Fabric API classes"""
    global _CLASS_INDEX
    if _CLASS_INDEX is None:
        _CLASS_INDEX = {}
        known = set()
        for line in (pathlib.Path(__file__).parent / "mc_classes.txt").read_text(encoding="utf-8").split():
            known.add(line)
            _CLASS_INDEX.setdefault(line.rsplit(".", 1)[-1], set()).add(line)
        _CLASS_INDEX["__all__"] = known
    return _CLASS_INDEX


CLIENT_FORBIDDEN = False


def fix_moved_imports(text):
    """imports of Minecraft classes that moved package: point them at the only class of that name"""
    idx = class_index()
    known = idx["__all__"]

    def repl(m):
        fq = m.group(1)
        if fq in known or not (fq.startswith("net.minecraft.") or fq.startswith("com.mojang.")):
            return m.group(0)
        simple = fq.rsplit(".", 1)[-1]
        cands = idx.get(simple, set())
        if len(cands) == 1 and not (CLIENT_FORBIDDEN and next(iter(cands)).startswith("net.minecraft.client.")):
            return "import " + next(iter(cands)) + ";"
        return m.group(0)

    return re.sub(r"^import ([\w.]+);", repl, text, flags=re.M)


def fix_ent_tag_key(args):
    return "TagKey.create(Registries.ENTITY_TYPE, " + args + ")"


def fix_registry_name(recv, args, argtext):
    if args and args[0]:
        return None
    return "RegistryUtil.getName(" + (recv if recv else "this") + ")"


def fix_indirect_mob_attack(args):
    parts = jtools.split_args(args)
    if len(parts) != 2:
        return None
    return parts[0] + ".damageSources().mobProjectile(" + args + ")"


def fix_entity_dimensions(args):
    parts = jtools.split_args(args)
    if len(parts) != 3:
        return None
    kind = "fixed" if parts[2].strip() == "true" else "scalable"
    return "EntityDimensions." + kind + "(" + parts[0] + ", " + parts[1] + ")"


def fix_do_hurt_target(recv, args, argtext):
    if len(args) != 1 or "ServerLevel" in argtext:
        return None
    target = recv if recv else "this"
    prefix = (recv + ".") if recv else ""
    if recv == "super":
        return None
    return prefix + "doHurtTarget((ServerLevel) " + ("this" if recv in ("", "this") else recv) + ".level(), " + argtext + ")"


def fix_put_uuid(recv, args, argtext):
    if len(args) != 2 or not recv:
        return None
    return recv + ".store(" + args[0] + ", UUIDUtil.CODEC, " + args[1] + ")"


def fix_get_uuid(recv, args, argtext):
    if len(args) != 1 or not recv:
        return None
    return recv + ".read(" + args[0] + ", UUIDUtil.CODEC).orElse(null)"


def fix_has_uuid(recv, args, argtext):
    if len(args) != 1 or not recv:
        return None
    return recv + ".read(" + args[0] + ", UUIDUtil.CODEC).isPresent()"


def fix_same_item(recv, args, argtext):
    if len(args) != 1 or not recv:
        return None
    return "ItemStack.isSameItem(" + recv + ", " + argtext + ")"


def fix_area_loaded(recv, args, argtext):
    if len(args) != 2:
        return None
    prefix = (recv + ".") if recv else ""
    return prefix + "isLoaded(" + args[0] + ")"


def fix_food_properties(recv, args, argtext):
    if args and args[0]:
        return None
    if not recv:
        return None
    return recv + ".components().get(DataComponents.FOOD)"


def rewrite_item_nbt(text):
    """stack.getTag()/getOrCreateTag()/setTag()/hasTag() -> ItemNbt helpers (custom_data component)"""
    # writes through the tag: stack.getTag().putX(..);  /  stack.getOrCreateTag().putX(..);
    def write_through(m):
        return m.group(0)

    out = text
    for getter in ("getOrCreateTag", "getTag"):
        key = "." + getter + "()."
        pos = len(out)
        while True:
            j = out.rfind(key, 0, pos)
            if j < 0:
                break
            pos = j
            start = jtools.find_receiver_start(out, j)
            recv = out[start:j].strip()
            if not recv:
                continue
            after = j + len(key)
            m = re.match(r"(put\w*|remove|merge)\(", out[after:])
            if m:
                open_i = after + m.end() - 1
                close = jtools.match_paren(out, open_i)
                if close < 0:
                    continue
                call = out[after:close + 1]
                rep = "ItemNbt.update(" + recv + ", nbtTag -> nbtTag." + call + ")"
                out = out[:start] + rep + out[close + 1:]
            else:
                out = out[:start] + "ItemNbt.getOrEmpty(" + recv + ")." + out[after:]
            pos = start
    out = rewrite_methods(out, "getTag", lambda r, a, t: None if (a and a[0]) or not r else "ItemNbt.getTag(" + r + ")")
    out = rewrite_methods(out, "getOrCreateTag", lambda r, a, t: None if (a and a[0]) or not r else "ItemNbt.getOrEmpty(" + r + ")")
    out = rewrite_methods(out, "hasTag", lambda r, a, t: None if (a and a[0]) or not r else "ItemNbt.hasTag(" + r + ")")
    out = rewrite_methods(out, "setTag", lambda r, a, t: None if len(a) != 1 or not r else "ItemNbt.setTag(" + r + ", " + t + ")")
    out = re.sub(r"ItemNbt\.getTag\(([^()]*(?:\([^()]*\))?[^()]*)\) == null", r"!ItemNbt.hasTag(\1)", out)
    out = re.sub(r"ItemNbt\.getTag\(([^()]*(?:\([^()]*\))?[^()]*)\) != null", r"ItemNbt.hasTag(\1)", out)
    return out


def fix_finalize_spawn(recv, args, argtext):
    if len(args) != 5:
        return None
    prefix = (recv + ".") if recv else ""
    return prefix + "finalizeSpawn(" + ", ".join(args[:4]) + ")"


def fix_set_loot_table(recv, args, argtext):
    if len(args) != 2 or "ResourceKey" in args[0] or not recv:
        return None
    return recv + ".setLootTable(ResourceKey.create(Registries.LOOT_TABLE, " + args[0] + "), " + args[1] + ")"


def fix_chunk_set_block_state(recv, args, argtext):
    if len(args) != 3 or args[2].strip() not in ("false", "true"):
        return None
    return recv + ".setBlockState(" + args[0] + ", " + args[1] + ", " + ("0" if args[2].strip() == "false" else "64") + ")"


def fix_hurt_and_break(recv, args, argtext):
    if len(args) != 3 or "->" not in args[2]:
        return None
    return recv + ".hurtAndBreak(" + args[0] + ", " + args[1] + ", EquipmentSlot.MAINHAND)"


def new_identifier(args):
    parts = jtools.split_args(args)
    if len(parts) == 2:
        return "Identifier.fromNamespaceAndPath(" + args + ")"
    return "Identifier.parse(" + args + ")"


NBT_GETTERS = {
    "getInt": "0", "getFloat": "0.0F", "getDouble": "0.0D", "getBoolean": "false", "getByte": "(byte) 0",
    "getShort": "(short) 0", "getLong": "0L", "getString": '""',
}

RULES = [
    (r"\bimport javax\.annotation\.Nullable;", "import org.jspecify.annotations.Nullable;"),
    (r"\bimport javax\.annotation\.Nonnull;", "import org.jetbrains.annotations.NotNull;"),
    (r"@Nonnull\b", "@NotNull"),
    (r"\bimport javax\.annotation\.ParametersAreNonnullByDefault;\n", ""),
    (r"@ParametersAreNonnullByDefault\s*", ""),
    (r"\bimport net\.minecraft\.network\.chat\.(TranslatableComponent|TextComponent);",
     "import net.minecraft.network.chat.Component;\nimport net.minecraft.network.chat.MutableComponent;"),
    (r"\bnew TranslatableComponent\(", "Component.translatable("),
    (r"\bnew TextComponent\(", "Component.literal("),
    (r"\bTextComponent\.EMPTY\b", "Component.empty()"),
    (r"\b(TranslatableComponent|TextComponent)\b", "MutableComponent"),
    (r"\.isOnGround\(\)", ".onGround()"),
    (r"\bprotected void defineSynchedData\(\)", "protected void defineSynchedData(SynchedEntityData.Builder builder)"),
    (r"\bpublic void defineSynchedData\(\)", "protected void defineSynchedData(SynchedEntityData.Builder builder)"),
    (r"\bsuper\.defineSynchedData\(\);", "super.defineSynchedData(builder);"),
    (r"\b(?:this\.)?entityData\.define\(", "builder.define("),
    (r"\bthis\.getEntityData\(\)\.define\(", "builder.define("),
    (r"\bgetEntityData\(\)\.define\(", "builder.define("),
    (r"\bnew Random\(\)", "RandomSource.create()"),
    (r"\bimport java\.util\.Random;", "import net.minecraft.util.RandomSource;"),
    (r"(?<![\w.])Random(?= \w+\s*[,)=;])", "RandomSource"),
    (r"\bLogManager\.getLogger\(\)", "org.slf4j.LoggerFactory.getLogger(\"iceandfire_fabric\")"),
    (r"\bMinecraft\.getInstance\(\)\.getFrameTime\(\)", "Minecraft.getInstance().getDeltaTracker().getGameTimeDeltaPartialTick(false)"),
    (r"\bDamageSource\.OUT_OF_WORLD\b", "damageSources().fellOutOfWorld()"),
    (r"\.getFeetBlockState\(\)", ".getInBlockState()"),
    (r"\.blocksMotion\(\)", ".isSolid()"),
    (r"\bEntityDataSerializers\.OPTIONAL_UUID\b", "IafDataSerializers.OPTIONAL_UUID"),
    (r"\.getRestrictCenter\(\)", ".getHomePosition()"),
    (r"\bgetRestrictCenter\(\)", "getHomePosition()"),
    (r"\bhasRestriction\(\)", "hasHome()"),
    (r"\bgetRestrictRadius\(\)", "getHomeRadius()"),
    (r"\brestrictTo\(", "setHomeTo("),
    (r"\bclearRestriction\(\)", "clearHome()"),
    (r"\bBlockPathTypes\b", "PathType"),
    (r"\.maxUpStep\b(?!\s*\()", ".maxUpStep()"),
    (r"\.dimension\(\)\.location\(\)", ".dimension().identifier()"),
    (r"\b\w+(?:\(\))?\.canRiderInteract\(\)", "false"),
    (r"\bInteractionResultHolder<ItemStack>", "InteractionResult"),
    (r"\bInteractionResultHolder\.(?:success|sidedSuccess)\([^;]*?\)(?=;)", "InteractionResult.SUCCESS"),
    (r"\bInteractionResultHolder\.pass\([^;]*?\)(?=;)", "InteractionResult.PASS"),
    (r"\bInteractionResultHolder\.fail\([^;]*?\)(?=;)", "InteractionResult.FAIL"),
    (r"\bInteractionResultHolder\.consume\([^;]*?\)(?=;)", "InteractionResult.CONSUME"),
    (r"\bnew InteractionResultHolder<>\(InteractionResult\.(\w+), [^;]*?\)(?=;)", r"InteractionResult.\1"),
    (r"\bnew InteractionResultHolder<ItemStack>\(InteractionResult\.(\w+), [^;]*?\)(?=;)", r"InteractionResult.\1"),
    (r"\bimport net\.minecraft\.world\.InteractionResultHolder;\n", ""),
    (r"\bEntityType\.([A-Z][A-Z_0-9]+)\b", r"EntityTypes.\1"),
    (r"\bGameRules\.RULE_MOBGRIEFING\b", "GameRules.MOB_GRIEFING"),
    (r"\bGameRules\.RULE_DOMOBLOOT\b", "GameRules.MOB_DROPS"),
    (r"\bGameRules\.RULE_DOENTITYDROPS\b", "GameRules.ENTITY_DROPS"),
    (r"\bGameRules\.RULE_DOBLOCKDROPS\b", "GameRules.BLOCK_DROPS"),
    (r"\bGameRules\.RULE_DOFIRETICK\b", "GameRules.FIRE_DAMAGE"),
    (r"\.getGameRules\(\)\.getBoolean\(", ".getGameRules().get("),
    (r"\.noCollission\(\)", ".noCollision()"),
    (r"\.hasImpulse\b", ".needsSync"),
    (r"\bthis\.hasImpulse\b", "this.needsSync"),
    (r"\.getThrower\(\)", ".getOwner()"),
    (r"\.hasCustomHoverName\(\)", ".has(DataComponents.CUSTOM_NAME)"),
    (r"\bPathType\.DANGER_FIRE\b", "PathType.FIRE_IN_NEIGHBOR"),
    (r"\bPathType\.DAMAGE_FIRE\b", "PathType.FIRE"),
    (r"\bPathType\.DANGER_OTHER\b", "PathType.DAMAGING_IN_NEIGHBOR"),
    (r"\bPathType\.DAMAGE_OTHER\b", "PathType.DAMAGING"),
    (r"\bPathType\.DANGER_POWDER_SNOW\b", "PathType.ON_TOP_OF_POWDER_SNOW"),
    (r"\.absMoveTo\(", ".absSnapTo("),
    (r"\bBlocks\.GRASS\b", "Blocks.SHORT_GRASS"),
    (r"\bDirectionProperty\.create\(\"facing\", Direction\.Plane\.HORIZONTAL\)", "BlockStateProperties.HORIZONTAL_FACING"),
    (r"\bDirectionProperty\b", "EnumProperty<Direction>"),
    (r"\.dropsLike\(([\w.]+)\)", r".overrideLootTable(\1.getLootTable())"),
    (r"\.getDigSpeed\(([\w.]+), [\w.]+\)", r".getDestroySpeed(\1)"),
    (r"\.getInventory\(\)\.getSelected\(\)", ".getInventory().getSelectedItem()"),
    (r"\bRenderShape\.ENTITYBLOCK_ANIMATED\b", "RenderShape.INVISIBLE"),
    (r"\bDirtPathBlock\b", "PathBlock"),
    (r"\b(Items|Blocks)\.(WHITE|ORANGE|MAGENTA|LIGHT_BLUE|YELLOW|LIME|PINK|GRAY|LIGHT_GRAY|CYAN|PURPLE|BLUE|BROWN|GREEN|RED|BLACK)_(DYE|WOOL|CARPET|BED|BANNER|STAINED_GLASS_PANE|STAINED_GLASS|CONCRETE_POWDER|CONCRETE|GLAZED_TERRACOTTA|TERRACOTTA|SHULKER_BOX|CANDLE)\b",
     lambda m: "%s.%s.%s()" % (m.group(1), {"TERRACOTTA": "DYED_TERRACOTTA", "SHULKER_BOX": "DYED_SHULKER_BOX", "CANDLE": "DYED_CANDLE"}.get(m.group(3), m.group(3)),
                               re.sub(r"_(\w)", lambda x: x.group(1).upper(), m.group(2).lower()))),
    (r"\bthis\.maxUpStep\(\) = ([^;]+);", r"this.getAttribute(Attributes.STEP_HEIGHT).setBaseValue(\1);"),
    (r"\bthis\.maxUpStep = ([^;]+);", r"this.getAttribute(Attributes.STEP_HEIGHT).setBaseValue(\1);"),
    (r"(?:this\.)?canBeControlledByRider\(\)", "(this.getControllingPassenger() != null)"),
    (r"(?<!\.)\bisControlledByLocalInstance\(\)", "isLocalInstanceAuthoritative()"),
    (r"\bthis\.isControlledByLocalInstance\(\)", "this.isLocalInstanceAuthoritative()"),
    (r"\bNetworkHooks\.openGui\(\(ServerPlayer\) (\w+), ", r"\1.openMenu("),
    (r"\bNetworkHooks\.openGui\((\w+), ", r"\1.openMenu("),
    (r"\bEnchantmentHelper\.hasVanishingCurse\(([\w.]+(?:\(\))?)\)", r"EnchantmentHelper.has(\1, EnchantmentEffectComponents.PREVENT_EQUIPMENT_DROP)"),
    (r"\bEnchantmentHelper\.getDepthStrider\(([\w.()]+)\)", r"(float) \1.getAttributeValue(Attributes.WATER_MOVEMENT_EFFICIENCY)"),
    (r"\.getName\(\)\.getContents\(\)", ".getName().getString()"),
    (r"\.getItem\(\)\.isEdible\(\)", ".has(DataComponents.FOOD)"),
    (r"(\w+)\.getItem\(\)\.components\(\)\.get\(DataComponents\.FOOD\)\.isMeat\(\)", r"\1.is(ItemTags.MEAT)"),
    (r"\bnew ItemParticleOption\(ParticleTypes\.ITEM, (\w*[sS]tack\w*|this\.getItem\(\))\)", r"new ItemParticleOption(ParticleTypes.ITEM, \1.getItem())"),
    (r"\bnew ItemParticleOption\(ParticleTypes\.ITEM, new ItemStack\(([^()]+)\)\)", r"new ItemParticleOption(ParticleTypes.ITEM, \1)"),
    (r"\bBlockEntityType\.Builder\.of\(", "BlockEntityType.Builder.of("),
    (r"\b(IAF_LECTERN|PODIUM|EGG_IN_ICE|PIXIE_HOUSE|PIXIE_JAR|MYRMEX_COCOON|DRAGONFORGE_CORE|DRAGONFORGE_BRICK|DRAGONFORGE_INPUT|DREAD_PORTAL|DREAD_SPAWNER|GHOST_CHEST)\.get\(\)", r"\1"),
    (r"\bnew ChunkPos\(((?:[\w.]+\(\)|[\w.]+))\)", r"ChunkPos.containing(\1)"),
    (r"\bNbtUtils\.writeBlockPos\(", "NbtUtils.writeBlockPos("),
    (r"([\w.]+\(\))\.getItem\(\)\.canPerformAction\(\1, ToolActions\.SHIELD_BLOCK\)", r"\1.has(DataComponents.BLOCKS_ATTACKS)"),
    (r"(\w+)\.getItem\(\)\.canPerformAction\(\1, ToolActions\.SHIELD_BLOCK\)", r"\1.has(DataComponents.BLOCKS_ATTACKS)"),
    (r"[ \t]*net\.minecraftforge\.event\.ForgeEventFactory\.onPlayerDestroyItem\([^;]*\);\n", ""),
    (r"\bUseAnim\b", "ItemUseAnimation"),
    (r"\b((?:\w+\.)*\w+(?:\(\))?)\.attackAnim\b(?!\s*=)", r"\1.getSwingAnimation(0.0F)"),
    (r"\.getTags\(\)\.add\(", ".addTag("),
    (r"\.getTags\(\)\.contains\(", ".entityTags().contains("),
    (r"\.getTags\(\)\.remove\(", ".removeTag("),
    (r"\bItemStack\.tagMatches\(([^,()]+), ([^,()]+)\)", r"ItemStack.isSameItemSameComponents(\1, \2)"),
    (r"\b(level\(\)|level|world|worldIn|this\.level\(\)|\w+\.level\(\))\.getProfiler\(\)", "Profiler.get()"),
    (r"\.getMaxBuildHeight\(\)", ".getMaxY()"),
    (r"\.getMinBuildHeight\(\)", ".getMinY()"),
    (r"\.isDay\(\)", ".isBrightOutside()"),
    (r"\.isNight\(\)", ".isDarkOutside()"),
    (r"\bMobEffects\.DAMAGE_BOOST\b", "MobEffects.STRENGTH"),
    (r"\bMobEffects\.MOVEMENT_SLOWDOWN\b", "MobEffects.SLOWNESS"),
    (r"\bMobEffects\.MOVEMENT_SPEED\b", "MobEffects.SPEED"),
    (r"\bMobEffects\.DIG_SLOWDOWN\b", "MobEffects.MINING_FATIGUE"),
    (r"\bMobEffects\.DIG_SPEED\b", "MobEffects.HASTE"),
    (r"\bMobEffects\.DAMAGE_RESISTANCE\b", "MobEffects.RESISTANCE"),
    (r"\bMobEffects\.CONFUSION\b", "MobEffects.NAUSEA"),
    (r"\bMobEffects\.JUMP\b", "MobEffects.JUMP_BOOST"),
    (r"\bMobEffects\.HEAL\b", "MobEffects.INSTANT_HEALTH"),
    (r"\bMobEffects\.HARM\b", "MobEffects.INSTANT_DAMAGE"),
    (r"\.tryCheckInsideBlocks\(\)", ".applyEffectsFromBlocks()"),
    (r"\bthis\.calculateEntityAnimation\(this, (\w+)\)", r"this.calculateEntityAnimation(\1)"),
    (r"\bcalculateEntityAnimation\(this, (\w+)\)", r"calculateEntityAnimation(\1)"),
    (r"\.msgId\b", ".getMsgId()"),
    (r"\bAttributeModifier\.Operation\.ADDITION\b", "AttributeModifier.Operation.ADD_VALUE"),
    (r"\bAttributeModifier\.Operation\.MULTIPLY_BASE\b", "AttributeModifier.Operation.ADD_MULTIPLIED_BASE"),
    (r"\bAttributeModifier\.Operation\.MULTIPLY_TOTAL\b", "AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL"),
    (r"\bgetCommandSenderWorld\(\)", "level()"),
    (r"\bRegistry\.ITEM\b", "BuiltInRegistries.ITEM"),
    (r"\bRegistry\.BLOCK\b", "BuiltInRegistries.BLOCK"),
    (r"\bRegistry\.ENTITY_TYPE\b", "BuiltInRegistries.ENTITY_TYPE"),
    (r"\bRegistry\.MOB_EFFECT\b", "BuiltInRegistries.MOB_EFFECT"),
    (r"\bRegistry\.SOUND_EVENT\b", "BuiltInRegistries.SOUND_EVENT"),
    (r"\bRegistry\.PARTICLE_TYPE\b", "BuiltInRegistries.PARTICLE_TYPE"),
    (r"\bRegistry\.BIOME_REGISTRY\b", "Registries.BIOME"),
    (r"\bRegistry\.DIMENSION_REGISTRY\b", "Registries.DIMENSION"),
    (r"\bForgeRegistries\.ITEMS\b", "BuiltInRegistries.ITEM"),
    (r"\bForgeRegistries\.BLOCKS\b", "BuiltInRegistries.BLOCK"),
    (r"\bForgeRegistries\.ENTITIES\b", "BuiltInRegistries.ENTITY_TYPE"),
    (r"\bForgeRegistries\.MOB_EFFECTS\b", "BuiltInRegistries.MOB_EFFECT"),
    (r"\bForgeRegistries\.SOUND_EVENTS\b", "BuiltInRegistries.SOUND_EVENT"),
    (r"\bForgeRegistries\.PARTICLE_TYPES\b", "BuiltInRegistries.PARTICLE_TYPE"),
    (r"\bForgeRegistries\.BLOCK_ENTITIES\b", "BuiltInRegistries.BLOCK_ENTITY_TYPE"),
    (r"\bForgeRegistries\.ENCHANTMENTS\b", "BuiltInRegistries.ENCHANTMENT"),
    (r"(BuiltInRegistries\.\w+)\.getValue\(", r"\1.getValue("),
    (r"\bimport net\.minecraftforge\.registries\.ForgeRegistries;", "import net.minecraft.core.registries.BuiltInRegistries;"),
    (r"\bimport net\.minecraft\.core\.Registry;", "import net.minecraft.core.Registry;\nimport net.minecraft.core.registries.BuiltInRegistries;\nimport net.minecraft.core.registries.Registries;"),
    (r"\.getMobType\(\) == MobType\.UNDEAD", ".is(EntityTypeTags.UNDEAD)"),
    (r"\.getMobType\(\) != MobType\.UNDEAD", ".is(EntityTypeTags.UNDEAD) == false"),
    (r"\.getMobType\(\) == MobType\.ARTHROPOD", ".is(EntityTypeTags.ARTHROPOD)"),
    (r"\.getMobType\(\) != MobType\.ARTHROPOD", ".is(EntityTypeTags.ARTHROPOD) == false"),
    (r"\.getMobType\(\) == MobType\.WATER", ".is(EntityTypeTags.AQUATIC)"),
    (r"\.getMobType\(\) == MobType\.ILLAGER", ".is(EntityTypeTags.ILLAGER)"),
    (r"\.isFire\(\)", ".is(DamageTypeTags.IS_FIRE)"),
    (r"\.isProjectile\(\)", ".is(DamageTypeTags.IS_PROJECTILE)"),
    (r"\.isExplosion\(\)", ".is(DamageTypeTags.IS_EXPLOSION)"),
    (r"\.isBypassArmor\(\)", ".is(DamageTypeTags.BYPASSES_ARMOR)"),
    (r"\.isBypassInvul\(\)", ".is(DamageTypeTags.BYPASSES_INVULNERABILITY)"),
    (r"\.isMagic\(\)", ".is(DamageTypeTags.WITCH_RESISTANT_TO)"),
    (r"(\w+(?:\(\))?) == DamageSource\.(IN_WALL|FALLING_BLOCK|CRAMMING|DROWN|FALL|IN_FIRE|ON_FIRE|LAVA|LIGHTNING_BOLT|MAGIC|WITHER|CACTUS|OUT_OF_WORLD|GENERIC|STARVE|FLY_INTO_WALL|DRAGON_BREATH|SWEET_BERRY_BUSH|FREEZE|HOT_FLOOR|DRY_OUT)\b",
     lambda m: m.group(1) + ".is(DamageTypes." + {"OUT_OF_WORLD": "FELL_OUT_OF_WORLD"}.get(m.group(2), m.group(2)) + ")"),
]


def rewrite_material(text):
    """state.getMaterial() == Material.X  ->  BlockMaterial.X.is(state)"""
    key = ".getMaterial()"
    out = text
    pos = len(out)
    while True:
        j = out.rfind(key, 0, pos)
        if j < 0:
            break
        pos = j
        after = out[j + len(key):]
        m = re.match(r"\s*(==|!=)\s*Material\.([A-Z_]+)", after)
        start = jtools.find_receiver_start(out, j)
        recv = out[start:j]
        if m:
            neg = "!" if m.group(1) == "!=" else ""
            rep = neg + "BlockMaterial." + m.group(2) + ".is(" + recv + ")"
            out = out[:start] + rep + out[j + len(key) + m.end():]
            pos = start
            continue
        m = re.match(r"\.(isSolid|blocksMotion|isReplaceable|isLiquid)\(\)", after)
        if m:
            new = {"isSolid": ".isSolid()", "blocksMotion": ".blocksMotion()", "isReplaceable": ".canBeReplaced()", "isLiquid": ".liquid()"}[m.group(1)]
            out = out[:j] + new + out[j + len(key) + m.end():]
    if "BlockMaterial." in out and "import com.iafabric.util.BlockMaterial;" not in out:
        out = add_import(out, "com.iafabric.util.BlockMaterial")
    return out


def fix(text):
    text = rewrite_material(text)
    for pat, rep in RULES:
        text = jtools.sub_code(pat, rep, text) if not pat.startswith(r"\bimport") else re.sub(pat, rep, text)
    text = rewrite_calls(text, "new Identifier(", new_identifier)
    text = rewrite_methods(text, "hurt", fix_hurt)
    text = rewrite_methods(text, "knockback", fix_knockback)
    text = rewrite_methods(text, "setSecondsOnFire", fix_seconds_on_fire)
    text = rewrite_methods(text, "moveTo", fix_move_to)
    text = rewrite_methods(text, "displayClientMessage", fix_client_message)
    text = rewrite_methods(text, "setTame", fix_set_tame)
    text = rewrite_methods(text, "create", fix_entity_create)
    text = rewrite_methods(text, "spawnAtLocation", fix_spawn_at_location)
    text = rewrite_methods(text, "swing", fix_swing)
    text = rewrite_methods(text, "startRiding", fix_start_riding)
    text = rewrite_methods(text, "getOwnerUUID", fix_owner_uuid)
    text = rewrite_methods(text, "setOwnerUUID", fix_set_owner_uuid)
    text = rewrite_methods(text, "removeModifier", fix_remove_modifier)
    text = rewrite_calls(text, "new AttributeModifier(", fix_attribute_modifier)
    text = rewrite_calls(text, "Objects.requireNonNull(BuiltInRegistries.ENTITY_TYPE.tags()).createTagKey(", fix_ent_tag_key)
    text = rewrite_calls(text, "BuiltInRegistries.ENTITY_TYPE.tags().createTagKey(", fix_ent_tag_key)
    text = rewrite_methods(text, "getRegistryName", fix_registry_name)
    text = rewrite_calls(text, "DamageSource.indirectMobAttack(", fix_indirect_mob_attack)
    text = rewrite_calls(text, "new EntityDimensions(", fix_entity_dimensions)
    text = rewrite_methods(text, "doHurtTarget", fix_do_hurt_target)
    text = rewrite_calls(text, "InteractionResult.sidedSuccess(", lambda a: "InteractionResult.SUCCESS")
    text = rewrite_item_nbt(text)
    text = rewrite_methods(text, "hurtAndBreak", fix_hurt_and_break)
    text = rewrite_methods(text, "finalizeSpawn", fix_finalize_spawn)
    text = rewrite_methods(text, "setLootTable", fix_set_loot_table)
    text = rewrite_methods(text, "setBlockState", fix_chunk_set_block_state)
    text = rewrite_methods(text, "putUUID", fix_put_uuid)
    text = rewrite_methods(text, "getUUID", fix_get_uuid)
    text = rewrite_methods(text, "hasUUID", fix_has_uuid)
    text = rewrite_methods(text, "sameItem", fix_same_item)
    text = rewrite_methods(text, "isAreaLoaded", fix_area_loaded)
    text = rewrite_methods(text, "getFoodProperties", fix_food_properties)
    text = fix_moved_imports(text)
    if "SynchedEntityData.Builder" in text and "import net.minecraft.network.syncher.SynchedEntityData;" not in text:
        text = add_import(text, "net.minecraft.network.syncher.SynchedEntityData")
    if "RandomSource" in text and "import net.minecraft.util.RandomSource;" not in text:
        text = add_import(text, "net.minecraft.util.RandomSource")
    if "Component." in text and "import net.minecraft.network.chat.Component;" not in text:
        text = add_import(text, "net.minecraft.network.chat.Component")
    if re.search(r"\bIdentifier\b", text) and "import net.minecraft.resources.Identifier;" not in text \
            and not re.search(r"^package net\.minecraft\.resources;", text, re.M):
        text = add_import(text, "net.minecraft.resources.Identifier")
    for simple, fqcn in KNOWN_IMPORTS.items():
        if re.search(r"\b" + simple + r"\b", text) and ("import " + fqcn + ";") not in text \
                and not re.search(r"^package " + re.escape(fqcn.rsplit(".", 1)[0]) + ";", text, re.M) \
                and not re.search(r"\b(class|interface|enum|record) " + simple + r"\b", text):
            text = add_import(text, fqcn)
    text = dedupe_imports(text)
    return text


KNOWN_IMPORTS = {
    "IafNetwork": "com.iafabric.network.IafNetwork",
    "IafMod": "com.iafabric.IafMod",
    "BlockMaterial": "com.iafabric.util.BlockMaterial",
    "IafBlockRegistry": "com.iafabric.block.IafBlockRegistry",
    "IafItemRegistry": "com.iafabric.item.IafItemRegistry",
    "IafEntityRegistry": "com.iafabric.entity.IafEntityRegistry",
    "IafSoundRegistry": "com.iafabric.misc.IafSoundRegistry",
    "IafConfig": "com.iafabric.IafConfig",
    "MapColor": "net.minecraft.world.level.material.MapColor",
    "EntitySpawnReason": "net.minecraft.world.entity.EntitySpawnReason",
    "ServerLevel": "net.minecraft.server.level.ServerLevel",
    "ItemStack": "net.minecraft.world.item.ItemStack",
    "Profiler": "net.minecraft.util.profiling.Profiler",
    "EntityTypeTags": "net.minecraft.tags.EntityTypeTags",
    "DamageTypeTags": "net.minecraft.tags.DamageTypeTags",
    "DamageTypes": "net.minecraft.world.damagesource.DamageTypes",
    "BuiltInRegistries": "net.minecraft.core.registries.BuiltInRegistries",
    "Registries": "net.minecraft.core.registries.Registries",
    "SwingAnimation": "net.minecraft.world.item.component.SwingAnimation",
    "EntityReference": "net.minecraft.world.entity.EntityReference",
    "Attributes": "net.minecraft.world.entity.ai.attributes.Attributes",
    "EntityUtil": "com.iafabric.util.EntityUtil",
    "IafDataSerializers": "com.iafabric.entity.util.IafDataSerializers",
    "PathType": "net.minecraft.world.level.pathfinder.PathType",
    "TagKey": "net.minecraft.tags.TagKey",
    "RegistryUtil": "com.iafabric.util.RegistryUtil",
    "EntityDimensions": "net.minecraft.world.entity.EntityDimensions",
    "ItemNbt": "com.iafabric.util.ItemNbt",
    "ResourceKey": "net.minecraft.resources.ResourceKey",
    "ChunkPos": "net.minecraft.world.level.ChunkPos",
    "BlockStateProperties": "net.minecraft.world.level.block.state.properties.BlockStateProperties",
    "EnumProperty": "net.minecraft.world.level.block.state.properties.EnumProperty",
    "PathBlock": "net.minecraft.world.level.block.PathBlock",
    "EnchantmentEffectComponents": "net.minecraft.world.item.enchantment.EnchantmentEffectComponents",
    "ItemTags": "net.minecraft.tags.ItemTags",
    "ListenedContainer": "com.iafabric.inventory.ListenedContainer",
    "IContainerListener": "com.iafabric.inventory.IContainerListener",
    "UUIDUtil": "net.minecraft.core.UUIDUtil",
    "EntityTypes": "net.minecraft.world.entity.EntityTypes",
    "DataComponents": "net.minecraft.core.component.DataComponents",
    "GameRules": "net.minecraft.world.level.gamerules.GameRules",
    "InteractionResult": "net.minecraft.world.InteractionResult",
    "ItemUseAnimation": "net.minecraft.world.item.ItemUseAnimation",
    "EquipmentSlot": "net.minecraft.world.entity.EquipmentSlot",
}


def add_import(text, fqcn):
    m = re.search(r"^package [\w.]+;\s*\n", text, re.M)
    pos = m.end() if m else 0
    return text[:pos] + "import " + fqcn + ";\n" + text[pos:]


def dedupe_imports(text):
    seen = set()
    out = []
    for line in text.split("\n"):
        s = line.strip()
        if s.startswith("import "):
            if s in seen:
                continue
            seen.add(s)
        out.append(line)
    return "\n".join(out)


def files():
    for d in SRC_DIRS:
        if not d.exists():
            continue
        for p in d.rglob("*.java"):
            if SKIP_PARTS & set(p.relative_to(d).parts):
                continue
            yield p


def main():
    changed = 0
    global CLIENT_FORBIDDEN
    for p in files():
        CLIENT_FORBIDDEN = "/src/main/" in p.as_posix()
        t = p.read_text(encoding="utf-8")
        n = fix(t)
        if n != t:
            p.write_text(n, encoding="utf-8", newline="\n")
            changed += 1
    print("changed", changed)


if __name__ == "__main__":
    main()

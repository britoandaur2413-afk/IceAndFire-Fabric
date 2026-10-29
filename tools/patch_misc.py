"""More 1.21.1 patches: menus, network, loot functions, registry aliases, worldgen helpers, pixie code."""
import pathlib
import re
import sys

sys.path.insert(0, str(pathlib.Path(__file__).parent))
from jedit import match  # noqa: E402

SRC = pathlib.Path(__file__).resolve().parent.parent / "src"
M = SRC / "main/java/com/iafabric"


def patch(rel, pairs, base=M):
    p = base / rel
    t = p.read_text(encoding="utf-8")
    for old, new in pairs:
        if old not in t:
            print("MISSING in", rel, ":", old[:90])
            continue
        t = t.replace(old, new)
    p.write_text(t, encoding="utf-8")


GLOBAL = [
    (r"\bnet\.fabricmc\.fabric\.api\.menu\.v1\.ExtendedMenuType\b", "net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerType"),
    (r"(?m)^import net\.fabricmc\.fabric\.api\.menu\.v1\.ExtendedMenuProvider;", "import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerFactory;"),
    (r"\bExtendedMenuProvider\b", "ExtendedScreenHandlerFactory"),
    (r"\bPayloadTypeRegistry\.serverboundPlay\(\)", "PayloadTypeRegistry.playC2S()"),
    (r"\bPayloadTypeRegistry\.clientboundPlay\(\)", "PayloadTypeRegistry.playS2C()"),
    (r"\.getGameRules\(\)\.get\((GameRules\.\w+)\)", r".getGameRules().getBoolean(\1)"),
    (r"(\w+)\.getOwnerReference\(\) == null \? null : \1\.getOwnerReference\(\)\.getUUID\(\)", r"\1.getOwnerUUID()"),
    (r"\.getServer\(\)\.getWorldGenSettings\(\)\.options\(\)", ".getServer().getWorldData().worldGenOptions()"),
    (r"\bSheep\.getRandomSheepColor\(([\w.]+)\.blockPosition\(\)\)", r"Sheep.getRandomSheepColor(\1.getRandom())"),
    (r"\bSheep\.getRandomSheepColor\([\w.()]+,\s*([\w.]+)\.blockPosition\(\)\)", r"Sheep.getRandomSheepColor(\1.getRandom())"),
    (r"\.coldEnoughToSnow\(([^,()]+(?:\(\))?),\s*[^,()]+(?:\(\))?\)", r".coldEnoughToSnow(\1)"),
    (r"\.isDarkOutside\(\)", ".isNight()"),
    (r"\bBlocks\.DEFAULT_BLOCK_STATE_CANDLE\b", "Blocks.CANDLE.defaultBlockState()"),
    (r"\bBlocks\.DYED_CANDLE\.pick\(", "com.iafabric.compat.Colors.candle("),
    (r"NbtUtils\.readBlockState\(BuiltInRegistries\.BLOCK,", "NbtUtils.readBlockState(BuiltInRegistries.BLOCK.asLookup(),"),
    (r"\bBoundingBox\.encapsulating\((\w+), (\w+)\)", r"\1.encapsulate(\2)"),
    (r"\.registryAccess\(\)\.lookupOrThrow\((Registries\.\w+)\)\.(holders|keySet|entrySet|stream|containsKey|getKey|size)\(", r".registryAccess().registryOrThrow(\1).\2("),
    (r"(\w+)\.getRegistryLookup\(\)", r"\1.registryAccess()"),
]
for f in (SRC / "main/java").rglob("*.java"):
    t = f.read_text(encoding="utf-8")
    o = t
    for pat, rep in GLOBAL:
        t = re.sub(pat, rep, t)
    if t != o:
        f.write_text(t, encoding="utf-8")

(M / "compat/Colors.java").write_text("""package com.iafabric.compat;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.block.Block;

/** A colored block by its color (26.x's color collections, as Blocks.DYED_CANDLE.pick(color)). */
public final class Colors {
	private Colors() {
	}

	public static Block block(DyeColor color, String kind) {
		return BuiltInRegistries.BLOCK.get(ResourceLocation.withDefaultNamespace(color.getName() + "_" + kind));
	}

	public static Block candle(DyeColor color) {
		return block(color, "candle");
	}
}
""", encoding="utf-8")

patch("inventory/IafContainerRegistry.java", [
    ("new net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerType<>(PixieMenu::new,\n        net.minecraft.network.codec.ByteBufCodecs.VAR_INT)",
     "new net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerType<>(PixieMenu::new,\n        net.minecraft.network.codec.ByteBufCodecs.VAR_INT.cast())"),
])
patch("entity/IafEntityRegistry.java", [
    ("return Registry.register(BuiltInRegistries.ENTITY_TYPE, key, builder.build(key));", "return Registry.register(BuiltInRegistries.ENTITY_TYPE, key, builder.build(key.location().toString()));"),
    ("        return builder.add(Attributes.TEMPT_RANGE, 10.0D);", "        return builder;"),
    ("    /** Attributes every mob of the mod has besides its own (TemptGoal now reads a tempt range). */", "    /** Attributes every mob of the mod has besides its own (none in 1.21.1: TemptGoal keeps its own range). */"),
])
for name, cls in (("customize_to_dragon", "CustomizeToDragon"), ("customize_to_sea_serpent", "CustomizeToSeaSerpent")):
    patch(f"loot/{cls}.java", [
        (f"public {cls}(Optional<Holder<LootItemCondition>> conditions) {{", f"public {cls}(List<LootItemCondition> conditions) {{"),
        (f"""    @Override
    public MapCodec<{cls}> codec() {{
        return MAP_CODEC;
    }}""", f"""    public static final LootItemFunctionType<{cls}> TYPE = new LootItemFunctionType<>(MAP_CODEC);

    @Override
    public LootItemFunctionType<{cls}> getType() {{
        return TYPE;
    }}"""),
    ])
    p = M / f"loot/{cls}.java"
    t = p.read_text(encoding="utf-8")
    t = t.replace("context.getOptional(LootContextParams.THIS_ENTITY)", "context.getParamOrNull(LootContextParams.THIS_ENTITY)")
    t = t.replace("dragon.shouldDropLoot()", "dragon.isOldEnoughToDropLoot()")
    for imp in ["import java.util.List;", "import net.minecraft.world.level.storage.loot.functions.LootItemFunctionType;"]:
        if imp not in t:
            t = t.replace("package com.iafabric.loot;\n", "package com.iafabric.loot;\n\n" + imp, 1)
    p.write_text(t, encoding="utf-8")
    patch("loot/IafLootRegistry.java", [(f"{cls}.MAP_CODEC);", f"{cls}.TYPE);")])

# 1.21.1 MappedRegistry: getHolder(...) gives the holder, get(...) the value
p = M / "mixin/RegistryAliasMixin.java"
t = p.read_text(encoding="utf-8")
t = t.replace('method = "get(Lnet/minecraft/resources/ResourceLocation;)Ljava/util/Optional;"', 'method = "getHolder(Lnet/minecraft/resources/ResourceLocation;)Ljava/util/Optional;"')
t = t.replace('method = "get(Lnet/minecraft/resources/ResourceKey;)Ljava/util/Optional;"', 'method = "getHolder(Lnet/minecraft/resources/ResourceKey;)Ljava/util/Optional;"')
t = t.replace('method = "getValue(Lnet/minecraft/resources/ResourceLocation;)Ljava/lang/Object;"', 'method = "get(Lnet/minecraft/resources/ResourceLocation;)Ljava/lang/Object;"')
t = t.replace('method = "getValue(Lnet/minecraft/resources/ResourceKey;)Ljava/lang/Object;"', 'method = "get(Lnet/minecraft/resources/ResourceKey;)Ljava/lang/Object;"')
t = t.replace("cir.setReturnValue(iceandfire_fabric$self().get(to));", "cir.setReturnValue(iceandfire_fabric$self().getHolder(to));")
t = t.replace("cir.setReturnValue(iceandfire_fabric$self().getValue(to));", "cir.setReturnValue(iceandfire_fabric$self().get(to));")
p.write_text(t, encoding="utf-8")

patch("entity/EntityChainTie.java", [
    ("public void dropItem(ServerLevel level, @Nullable Entity brokenEntity) {", "public void dropItem(@Nullable Entity brokenEntity) {"),
    ("public InteractionResult interact(@NotNull Player player, @NotNull InteractionHand hand, Vec3 location) {", "public InteractionResult interact(@NotNull Player player, @NotNull InteractionHand hand) {"),
])
patch("entity/EntityDreadMob.java", [
    ("protected boolean shouldDropLoot(net.minecraft.server.level.ServerLevel level) {", "protected boolean shouldDropLoot() {"),
    ("protected int getBaseExperienceReward(net.minecraft.server.level.ServerLevel level) {", "protected int getBaseExperienceReward() {"),
])
patch("entity/EntityMyrmexBase.java", [
    ("    @Override\n    public boolean stillValid(Player player) {", "    public boolean stillValid(Player player) {"),
    ("this.villagerInventory.storeAsItemList(tag.list(\"Inventory\", ItemStack.CODEC));", "tag.put(\"Inventory\", this.villagerInventory.createTag(this.registryAccess()));"),
    ("this.villagerInventory.fromItemList(tag.listOrEmpty(\"Inventory\", ItemStack.CODEC));", "this.villagerInventory.fromTag(tagTag.getList(\"Inventory\", net.minecraft.nbt.Tag.TAG_COMPOUND), this.registryAccess());"),
])
patch("entity/ai/SeaSerpentPathNavigator.java", [("    @Override\n    public boolean canNavigateGround() {", "    public boolean canNavigateGround() {")])
patch("boss/CastleWard.java", [
    ("""				refuse(player, level);
				return InteractionResult.FAIL;
			}
			return InteractionResult.PASS;
		});""", """				refuse(player, level);
				return net.minecraft.world.InteractionResultHolder.fail(player.getItemInHand(hand));
			}
			return net.minecraft.world.InteractionResultHolder.pass(player.getItemInHand(hand));
		});"""),
])
patch("event/ServerEvents.java", [
    ("type.builtInRegistryHolder().getType().is(TagKey.create(Registries.ENTITY_TYPE, loc))", "type.is(TagKey.create(Registries.ENTITY_TYPE, loc))"),
    ("UseItemCallback.EVENT.register(ServerEvents::onEntityUseItem);", "UseItemCallback.EVENT.register((player, level, hand) -> new net.minecraft.world.InteractionResultHolder<>(onEntityUseItem(player, level, hand), player.getItemInHand(hand)));"),
])
patch("pixie/FairyLocator.java", [("manager.getStartForStructure(structure.value(), access)", "manager.getStartForStructure(net.minecraft.core.SectionPos.bottomOf(access), structure.value(), access)")])
patch("world/structure/StructureRoom.java", [("context.biomeSource(), context.climateSampler(),\n", "context.biomeSource(),\n")])
patch("entity/EntityTideTrident.java", [
    ("EnchantmentHelper.doPostAttackEffectsWithItemSourceOnBreak(serverLevel, entity, source, this.getWeaponItem(), weapon -> this.kill());",
     "EnchantmentHelper.doPostAttackEffectsWithItemSource(serverLevel, entity, source, this.getWeaponItem());"),
])
patch("message/MessageMultipartInteract.java", [
    ("mob.interact(player, InteractionHand.MAIN_HAND, mob.position());",
     "if (!mob.interactAt(player, mob.position(), InteractionHand.MAIN_HAND).consumesAction()) {\n                                    player.interactOn(mob, InteractionHand.MAIN_HAND);\n                                }"),
])
patch("boss/BlackFrostFight.java", [(": com.iafabric.misc.IafSoundRegistry.BLACK_FROST_BREATH).location();", ": com.iafabric.misc.IafSoundRegistry.BLACK_FROST_BREATH).getLocation();")])
patch("entity/EntityDeathWorm.java", [("            if (state.isSolidRender()) {", "            if (state.isSolidRender(level(), pos)) {")])
patch("world/gen/WorldGenHydraCave.java", [("worldIn.registryAccess().lookupOrThrow(Registries.FEATURE).get(TreeFeatures.SWAMP_OAK)", "worldIn.registryAccess().registryOrThrow(Registries.CONFIGURED_FEATURE).getHolder(TreeFeatures.SWAMP_OAK)")])
patch("citadel/server/entity/collision/ICustomCollisions.java", [("for (Direction.Axis axis : Direction.axisStepOrder(movement)) {", "for (Direction.Axis axis : axisStepOrder(movement)) {")])
p = M / "citadel/server/entity/collision/ICustomCollisions.java"
t = p.read_text(encoding="utf-8")
if "static java.util.List<Direction.Axis> axisStepOrder" not in t:
    end = t.rstrip().rfind("}")
    t = t[:end].rstrip() + """

    /** The order an entity's move is resolved in, as 1.21.1's Entity.collideWithShapes does it: up/down first, then the smaller one of x and z last. */
    static java.util.List<Direction.Axis> axisStepOrder(Vec3 movement) {
        return Math.abs(movement.x) < Math.abs(movement.z)
            ? java.util.List.of(Direction.Axis.Y, Direction.Axis.Z, Direction.Axis.X)
            : java.util.List.of(Direction.Axis.Y, Direction.Axis.X, Direction.Axis.Z);
    }
}
"""
    p.write_text(t, encoding="utf-8")
patch("pixie/ExiledQuests.java", [("for (ItemStack stack : player.getInventory()) {", "for (ItemStack stack : com.iafabric.compat.Inv.all(player.getInventory())) {")])
(M / "compat/Inv.java").write_text("""package com.iafabric.compat;

import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/** Every stack of a player's inventory (26.x's Inventory is iterable). */
public final class Inv {
	private Inv() {
	}

	public static List<ItemStack> all(Inventory inventory) {
		List<ItemStack> out = new ArrayList<>(inventory.getContainerSize());
		for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
			out.add(inventory.getItem(slot));
		}
		return out;
	}
}
""", encoding="utf-8")
patch("world/gen/processor/GorgonTempleProcessor.java", [
    (".setBlockState(infoIn2.pos(), Blocks.AIR.defaultBlockState(), 0);", ".setBlockState(infoIn2.pos(), Blocks.AIR.defaultBlockState(), false);"),
    ("sideChunk.setBlockState(mutable, Blocks.STONE_BRICKS.defaultBlockState(), 0);", "sideChunk.setBlockState(mutable, Blocks.STONE_BRICKS.defaultBlockState(), false);"),
])
patch("pixie/PixieBonds.java", [("bond.orbs[k] = orbOf(orbs.getStringOr(k, \"\"));", "bond.orbs[k] = orbOf(orbs.getString(k));")])
patch("pixie/ExiledPixies.java", [("pixie.setOwner(player);", "pixie.setOwnerUUID(player.getUUID());")])

# tag.getCompound("K").ifPresent(x -> {...}); -> { CompoundTag x = tag.getCompound("K"); ... }
for f in (SRC / "main/java").rglob("*.java"):
    t = f.read_text(encoding="utf-8")
    o = t
    pat = re.compile(r"(\w+)\.getCompound\((\"\w+\")\)\.ifPresent\((\w+) -> \{")
    while True:
        m = pat.search(t)
        if not m:
            break
        open_brace = m.end() - 1
        close = match(t, open_brace)
        # the lambda ends with "});"
        tail = t[close:close + 3]
        if tail != "});":
            print("odd ifPresent in", f.name)
            break
        body = t[open_brace + 1:close]
        t = t[:m.start()] + "{\n" + f"CompoundTag {m.group(3)} = {m.group(1)}.getCompound({m.group(2)});" + body + "}" + t[close + 3:]
    if t != o:
        t = re.sub(r"\b(lines|told|data)\.keySet\(\)", r"\1.getAllKeys()", t)
        f.write_text(t, encoding="utf-8")
        print("compound ifPresent:", f.name)
print("done")

package com.iafabric.block;

import com.iafabric.item.IafItemRegistry;
import com.iafabric.IafMod;
import com.iafabric.enums.EnumDragonEgg;
import com.iafabric.misc.IafSoundRegistry;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public final class IafBlockRegistry {
    /** Every block of the mod, in registration order (their items are made by IafItemRegistry). */
    public static final List<Block> BLOCKS = new ArrayList<>();
    private static ResourceKey<Block> pending;

    public static final SoundType SOUND_TYPE_GOLD = new SoundType(1.0F, 1.0F, IafSoundRegistry.GOLD_PILE_BREAK, IafSoundRegistry.GOLD_PILE_STEP, IafSoundRegistry.GOLD_PILE_BREAK, IafSoundRegistry.GOLD_PILE_STEP, IafSoundRegistry.GOLD_PILE_STEP);

    public static final Block LECTERN = register("lectern", () -> new BlockLectern());
    public static final Block PODIUM_OAK = register("podium_oak", () -> new BlockPodium());
    public static final Block PODIUM_BIRCH = register("podium_birch", () -> new BlockPodium());
    public static final Block PODIUM_SPRUCE = register("podium_spruce", () -> new BlockPodium());
    public static final Block PODIUM_JUNGLE = register("podium_jungle", () -> new BlockPodium());
    public static final Block PODIUM_DARK_OAK = register("podium_dark_oak", () -> new BlockPodium());
    public static final Block PODIUM_ACACIA = register("podium_acacia", () -> new BlockPodium());
    public static final Block FIRE_LILY = register("fire_lily", () -> new BlockElementalFlower());
    public static final Block FROST_LILY = register("frost_lily", () -> new BlockElementalFlower());
    public static final Block LIGHTNING_LILY = register("lightning_lily", () -> new BlockElementalFlower());
    public static final Block GOLD_PILE = register("gold_pile", () -> new BlockGoldPile());
    public static final Block PIXIE_ALTAR = register("pixie_altar", () -> new BlockPixieAltar());
    public static final Block THRONE_ALTAR = register("throne_altar", () -> new BlockThroneAltar());
    public static final Block SILVER_PILE = register("silver_pile", () -> new BlockGoldPile());
    public static final Block COPPER_PILE = register("copper_pile", () -> new BlockGoldPile());
    public static final Block SILVER_ORE = register("silver_ore", () -> new BlockIafOre(2, 3.0F, 3.0F));
    public static final Block SAPPHIRE_ORE = register("sapphire_ore", () -> new BlockIafOre(2, 4.0F, 3.0F));
    public static final Block AMYTHEST_ORE = register("amythest_ore", () -> new BlockIafOre(2, 4.0F, 3.0F));
    public static final Block SILVER_BLOCK = register("silver_block", () -> new BlockGeneric(MapColor.METAL, 3.0F, 5.0F, SoundType.METAL));
    public static final Block SAPPHIRE_BLOCK = register("sapphire_block", () -> new BlockGeneric(MapColor.METAL, 3.0F, 6.0F, SoundType.METAL));
    public static final Block AMYTHEST_BLOCK = register("amythest_block", () -> new BlockGeneric(MapColor.METAL, 5.0F, 6.0F, SoundType.METAL));
    public static final Block CHARRED_DIRT = register("chared_dirt", () -> new BlockReturningState(MapColor.DIRT, 0.5F, 0.0F, SoundType.GRAVEL, Blocks.DIRT.defaultBlockState()));
    public static final Block CHARRED_GRASS = register("chared_grass", () -> new BlockReturningState(MapColor.GRASS, 0.6F, 0.0F, SoundType.GRAVEL, Blocks.GRASS_BLOCK.defaultBlockState()));
    public static final Block CHARRED_STONE = register("chared_stone", () -> new BlockReturningState(MapColor.STONE, 1.5F, 10.0F, SoundType.STONE, Blocks.STONE.defaultBlockState()));
    public static final Block CHARRED_COBBLESTONE = register("chared_cobblestone", () -> new BlockReturningState(MapColor.STONE, 2F, 10.0F, SoundType.STONE, Blocks.COBBLESTONE.defaultBlockState()));
    public static final Block CHARRED_GRAVEL = register("chared_gravel", () -> new BlockFallingReturningState(MapColor.DIRT, 0.6F, 0F, SoundType.GRAVEL, Blocks.GRAVEL.defaultBlockState()));
    public static final Block CHARRED_DIRT_PATH = register(BlockCharedPath.getNameFromType(0), () -> new BlockCharedPath(0));
    public static final Block ASH = register("ash", () -> new BlockFallingGeneric(MapColor.SAND, 0.5F, 0F, SoundType.SAND));
    public static final Block FROZEN_DIRT = register("frozen_dirt", () -> new BlockReturningState(MapColor.DIRT, 0.5F, 0.0F, SoundType.GLASS, true, Blocks.DIRT.defaultBlockState()));
    public static final Block FROZEN_GRASS = register("frozen_grass", () -> new BlockReturningState(MapColor.GRASS, 0.6F, 0.0F, SoundType.GLASS, true, Blocks.GRASS_BLOCK.defaultBlockState()));
    public static final Block FROZEN_STONE = register("frozen_stone", () -> new BlockReturningState(MapColor.STONE, 1.5F, 1.0F, SoundType.GLASS, true, Blocks.STONE.defaultBlockState()));
    public static final Block FROZEN_COBBLESTONE = register("frozen_cobblestone", () -> new BlockReturningState(MapColor.STONE, 2F, 2.0F, SoundType.GLASS, true, Blocks.COBBLESTONE.defaultBlockState()));
    public static final Block FROZEN_GRAVEL = register("frozen_gravel", () -> new BlockFallingReturningState(MapColor.DIRT, 0.6F, 0F, SoundType.GLASS, true, Blocks.GRAVEL.defaultBlockState()));
    public static final Block FROZEN_DIRT_PATH = register(BlockCharedPath.getNameFromType(1), () -> new BlockCharedPath(1));
    public static final Block FROZEN_SPLINTERS = register("frozen_splinters", () -> new BlockGeneric(MapColor.WOOD, 2.0F, 1.0F, SoundType.GLASS, true));
    public static final Block DRAGON_ICE = register("dragon_ice", () -> new BlockGeneric(MapColor.ICE, 0.5F, 0F, SoundType.GLASS, true));
    public static final Block DRAGON_ICE_SPIKES = register("dragon_ice_spikes", () -> new BlockIceSpikes());
    public static final Block CRACKLED_DIRT = register("crackled_dirt", () -> new BlockReturningState(MapColor.DIRT, 0.5F, 0.0F, SoundType.GRAVEL, Blocks.DIRT.defaultBlockState()));
    public static final Block CRACKLED_GRASS = register("crackled_grass", () -> new BlockReturningState(MapColor.GRASS, 0.6F, 0.0F, SoundType.GRAVEL, Blocks.GRASS_BLOCK.defaultBlockState()));
    public static final Block CRACKLED_STONE = register("crackled_stone", () -> new BlockReturningState(MapColor.STONE, 1.5F, 1.0F, SoundType.STONE, Blocks.STONE.defaultBlockState()));
    public static final Block CRACKLED_COBBLESTONE = register("crackled_cobblestone", () -> new BlockReturningState(MapColor.STONE, 2F, 2F, SoundType.STONE, Blocks.COBBLESTONE.defaultBlockState()));
    public static final Block CRACKLED_GRAVEL = register("crackled_gravel", () -> new BlockFallingReturningState(MapColor.DIRT, 0.6F, 0F, SoundType.GRAVEL, Blocks.GRAVEL.defaultBlockState()));
    public static final Block CRACKLED_DIRT_PATH = register(BlockCharedPath.getNameFromType(2), () -> new BlockCharedPath(2));
    public static final Block NEST = register("nest", () -> new BlockGeneric(MapColor.PLANT, 0.5F, 0F, SoundType.GRAVEL, false));
    public static final Block DRAGON_SCALE_RED = register("dragonscale_red", () -> new BlockDragonScales(EnumDragonEgg.RED));
    public static final Block DRAGON_SCALE_GREEN = register("dragonscale_green", () -> new BlockDragonScales(EnumDragonEgg.GREEN));
    public static final Block DRAGON_SCALE_BRONZE = register("dragonscale_bronze", () -> new BlockDragonScales(EnumDragonEgg.BRONZE));
    public static final Block DRAGON_SCALE_GRAY = register("dragonscale_gray", () -> new BlockDragonScales(EnumDragonEgg.GRAY));
    public static final Block DRAGON_SCALE_BLUE = register("dragonscale_blue", () -> new BlockDragonScales(EnumDragonEgg.BLUE));
    public static final Block DRAGON_SCALE_WHITE = register("dragonscale_white", () -> new BlockDragonScales(EnumDragonEgg.WHITE));
    public static final Block DRAGON_SCALE_SAPPHIRE = register("dragonscale_sapphire", () -> new BlockDragonScales(EnumDragonEgg.SAPPHIRE));
    public static final Block DRAGON_SCALE_SILVER = register("dragonscale_silver", () -> new BlockDragonScales(EnumDragonEgg.SILVER));
    public static final Block DRAGON_SCALE_ELECTRIC = register("dragonscale_electric", () -> new BlockDragonScales(EnumDragonEgg.ELECTRIC));
    public static final Block DRAGON_SCALE_AMYTHEST = register("dragonscale_amythest", () -> new BlockDragonScales(EnumDragonEgg.AMYTHEST));
    public static final Block DRAGON_SCALE_COPPER = register("dragonscale_copper", () -> new BlockDragonScales(EnumDragonEgg.COPPER));
    public static final Block DRAGON_SCALE_BLACK = register("dragonscale_black", () -> new BlockDragonScales(EnumDragonEgg.BLACK));
    public static final Block DRAGON_BONE_BLOCK = register("dragon_bone_block", () -> new BlockDragonBone());
    public static final Block DRAGON_BONE_BLOCK_WALL = register("dragon_bone_wall", () -> new BlockDragonBoneWall(IafBlockRegistry.copyProps(IafBlockRegistry.DRAGON_BONE_BLOCK)));
    public static final Block DRAGONFORGE_FIRE_BRICK = register(BlockDragonforgeBricks.name(0), () -> new BlockDragonforgeBricks(0));
    public static final Block DRAGONFORGE_ICE_BRICK = register(BlockDragonforgeBricks.name(1), () -> new BlockDragonforgeBricks(1));
    public static final Block DRAGONFORGE_LIGHTNING_BRICK = register(BlockDragonforgeBricks.name(2), () -> new BlockDragonforgeBricks(2));
    public static final Block DRAGONFORGE_FIRE_INPUT = register(BlockDragonforgeInput.name(0), () -> new BlockDragonforgeInput(0));
    public static final Block DRAGONFORGE_ICE_INPUT = register(BlockDragonforgeInput.name(1), () -> new BlockDragonforgeInput(1));
    public static final Block DRAGONFORGE_LIGHTNING_INPUT = register(BlockDragonforgeInput.name(2), () -> new BlockDragonforgeInput(2));
    public static final Block DRAGONFORGE_FIRE_CORE = register(BlockDragonforgeCore.name(0, true), () -> new BlockDragonforgeCore(0, true));
    public static final Block DRAGONFORGE_ICE_CORE = register(BlockDragonforgeCore.name(1, true), () -> new BlockDragonforgeCore(1, true));
    public static final Block DRAGONFORGE_LIGHTNING_CORE = register(BlockDragonforgeCore.name(2, true), () -> new BlockDragonforgeCore(2, true));
    public static final Block DRAGONFORGE_FIRE_CORE_DISABLED = register(BlockDragonforgeCore.name(0, false), () -> new BlockDragonforgeCore(0, false));
    public static final Block DRAGONFORGE_ICE_CORE_DISABLED = register(BlockDragonforgeCore.name(1, false), () -> new BlockDragonforgeCore(1, false));
    public static final Block DRAGONFORGE_LIGHTNING_CORE_DISABLED = register(BlockDragonforgeCore.name(2, false), () -> new BlockDragonforgeCore(2, false));
    public static final Block EGG_IN_ICE = register("egginice", () -> new BlockEggInIce());
    public static final Block PIXIE_HOUSE_MUSHROOM_RED = register(BlockPixieHouse.name("mushroom_red"), () -> new BlockPixieHouse());
    public static final Block PIXIE_HOUSE_MUSHROOM_BROWN = register(BlockPixieHouse.name("mushroom_brown"), () -> new BlockPixieHouse());
    public static final Block PIXIE_HOUSE_OAK = register(BlockPixieHouse.name("oak"), () -> new BlockPixieHouse());
    public static final Block PIXIE_HOUSE_BIRCH = register(BlockPixieHouse.name("birch"), () -> new BlockPixieHouse());
    public static final Block PIXIE_HOUSE_SPRUCE = register(BlockPixieHouse.name("spruce"), () -> new BlockPixieHouse());
    public static final Block PIXIE_HOUSE_DARK_OAK = register(BlockPixieHouse.name("dark_oak"), () -> new BlockPixieHouse());
    public static final Block JAR_EMPTY = register(BlockJar.name(-1), () -> new BlockJar(-1));
    /** An exiled pixie's lantern, in the middle of her ring of candles where a quest leads. */
    public static final Block EXILED_LANTERN = register("exiled_lantern", BlockExiledLantern::new);
    public static final Block JAR_PIXIE_0 = register(BlockJar.name(0), () -> new BlockJar(0));
    public static final Block JAR_PIXIE_1 = register(BlockJar.name(1), () -> new BlockJar(1));
    public static final Block JAR_PIXIE_2 = register(BlockJar.name(2), () -> new BlockJar(2));
    public static final Block JAR_PIXIE_3 = register(BlockJar.name(3), () -> new BlockJar(3));
    public static final Block JAR_PIXIE_4 = register(BlockJar.name(4), () -> new BlockJar(4));
    public static final Block MYRMEX_DESERT_RESIN = register(BlockMyrmexResin.name(false, "desert"), () -> new BlockMyrmexResin(false));
    public static final Block MYRMEX_DESERT_RESIN_STICKY = register(BlockMyrmexResin.name(true, "desert"), () -> new BlockMyrmexResin(true));
    public static final Block MYRMEX_JUNGLE_RESIN = register(BlockMyrmexResin.name(false, "jungle"), () -> new BlockMyrmexResin(false));
    public static final Block MYRMEX_JUNGLE_RESIN_STICKY = register(BlockMyrmexResin.name(true, "jungle"), () -> new BlockMyrmexResin(true));
    public static final Block DESERT_MYRMEX_COCOON = register("desert_myrmex_cocoon", () -> new BlockMyrmexCocoon());
    public static final Block JUNGLE_MYRMEX_COCOON = register("jungle_myrmex_cocoon", () -> new BlockMyrmexCocoon());
    public static final Block MYRMEX_DESERT_BIOLIGHT = register("myrmex_desert_biolight", () -> new BlockMyrmexBiolight());
    public static final Block MYRMEX_JUNGLE_BIOLIGHT = register("myrmex_jungle_biolight", () -> new BlockMyrmexBiolight());
    public static final Block MYRMEX_DESERT_RESIN_BLOCK = register(BlockMyrmexConnectedResin.name(false, false), () -> new BlockMyrmexConnectedResin(false, false));
    public static final Block MYRMEX_JUNGLE_RESIN_BLOCK = register(BlockMyrmexConnectedResin.name(true, false), () -> new BlockMyrmexConnectedResin(true, false));
    public static final Block MYRMEX_DESERT_RESIN_GLASS = register(BlockMyrmexConnectedResin.name(false, true), () -> new BlockMyrmexConnectedResin(false, true));
    public static final Block MYRMEX_JUNGLE_RESIN_GLASS = register(BlockMyrmexConnectedResin.name(true, true), () -> new BlockMyrmexConnectedResin(true, true));
    public static final Block DRAGONSTEEL_FIRE_BLOCK = register("dragonsteel_fire_block", () -> new BlockGeneric(MapColor.METAL, 10.0F, 1000.0F, SoundType.METAL));
    public static final Block DRAGONSTEEL_ICE_BLOCK = register("dragonsteel_ice_block", () -> new BlockGeneric(MapColor.METAL, 10.0F, 1000.0F, SoundType.METAL));
    public static final Block DRAGONSTEEL_LIGHTNING_BLOCK = register("dragonsteel_lightning_block", () -> new BlockGeneric(MapColor.METAL, 10.0F, 1000.0F, SoundType.METAL));
    public static final BlockDreadBase DREAD_STONE = register("dread_stone", () -> new BlockDreadBase(MapColor.STONE, -1.0F, 100000.0F, SoundType.STONE));
    public static final BlockDreadBase DREAD_STONE_BRICKS = register("dread_stone_bricks", () -> new BlockDreadBase(MapColor.STONE, -1.0F, 100000.0F, SoundType.STONE));
    public static final BlockDreadBase DREAD_STONE_BRICKS_CHISELED = register("dread_stone_bricks_chiseled", () -> new BlockDreadBase(MapColor.STONE, -1.0F, 100000.0F, SoundType.STONE));
    public static final BlockDreadBase DREAD_STONE_BRICKS_CRACKED = register("dread_stone_bricks_cracked", () -> new BlockDreadBase(MapColor.STONE, -1.0F, 100000.0F, SoundType.STONE));
    public static final BlockDreadBase DREAD_STONE_BRICKS_MOSSY = register("dread_stone_bricks_mossy", () -> new BlockDreadBase(MapColor.STONE, -1.0F, 100000.0F, SoundType.STONE));
    public static final BlockDreadBase DREAD_STONE_TILE = register("dread_stone_tile", () -> new BlockDreadBase(MapColor.STONE, -1.0F, 100000.0F, SoundType.STONE));
    public static final Block DREAD_STONE_FACE = register("dread_stone_face", () -> new BlockDreadStoneFace());
    public static final Block DREAD_TORCH = register("dread_torch", () -> new BlockDreadTorch());
    public static final Block DREAD_TORCH_WALL = register("dread_torch_wall", () -> new BlockDreadTorchWall());
    public static final Block DREAD_STONE_BRICKS_STAIRS = register("dread_stone_stairs", () -> new BlockGenericStairs(DREAD_STONE_BRICKS.defaultBlockState()));
    public static final Block DREAD_STONE_BRICKS_SLAB = register("dread_stone_slab", () -> new SlabBlock(IafBlockRegistry.props().mapColor(MapColor.STONE).strength(10F, 10000F)));
    public static final Block DREADWOOD_LOG = register("dreadwood_log", () -> new BlockDreadWoodLog());
    public static final BlockDreadBase DREADWOOD_PLANKS = register("dreadwood_planks", () -> new BlockDreadBase(MapColor.WOOD, -1.0F, 100000.0F, SoundType.WOOD));
    public static final Block DREADWOOD_PLANKS_LOCK = register("dreadwood_planks_lock", () -> new BlockDreadWoodLock());
    public static final Block DREAD_PORTAL = register("dread_portal", () -> new BlockDreadPortal());
    public static final Block DREAD_SPAWNER = register("dread_spawner", () -> new BlockDreadSpawner());
    public static final Block BURNT_TORCH = register("burnt_torch", () -> new BlockBurntTorch());
    public static final Block BURNT_TORCH_WALL = register("burnt_torch_wall", () -> new BlockBurntTorchWall());
    public static final Block GHOST_CHEST = register("ghost_chest", () -> new BlockGhostChest());
    public static final Block GRAVEYARD_SOIL = register("graveyard_soil", () -> new BlockGraveyardSoil());

    private IafBlockRegistry() {
    }

    /** Properties for the block being registered right now (blocks need their id before they exist). */
    public static BlockBehaviour.Properties props() {
        if (pending == null) {
            throw new IllegalStateException("Block properties made outside IafBlockRegistry.register");
        }
        return BlockBehaviour.Properties.of().setId(pending);
    }

    public static BlockBehaviour.Properties copyProps(BlockBehaviour block) {
        if (pending == null) {
            throw new IllegalStateException("Block properties made outside IafBlockRegistry.register");
        }
        return BlockBehaviour.Properties.ofFullCopy(block).setId(pending);
    }

    public static <T extends Block> T register(String name, Supplier<T> factory) {
        ResourceKey<Block> key = ResourceKey.create(Registries.BLOCK, IafMod.id(name));
        pending = key;
        T block;
        try {
            block = factory.get();
        } finally {
            pending = null;
        }
        Registry.register(BuiltInRegistries.BLOCK, key, block);
        BLOCKS.add(block);
        return block;
    }

    public static void init() {
        // the sea serpent scale blocks are made by their enum
        com.iafabric.enums.EnumSeaSerpent.values();
    }
}

package com.iafabric.entity.tile;

import com.iafabric.IafMod;
import com.iafabric.block.IafBlockRegistry;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;

import java.util.Set;

public final class IafTileEntityRegistry {

    public static final BlockEntityType<TileEntityLectern> IAF_LECTERN = register("lectern", TileEntityLectern::new, IafBlockRegistry.LECTERN);
    public static final BlockEntityType<TileEntityPodium> PODIUM = register("podium", TileEntityPodium::new, IafBlockRegistry.PODIUM_OAK, IafBlockRegistry.PODIUM_BIRCH, IafBlockRegistry.PODIUM_SPRUCE, IafBlockRegistry.PODIUM_JUNGLE, IafBlockRegistry.PODIUM_DARK_OAK, IafBlockRegistry.PODIUM_ACACIA);
    public static final BlockEntityType<TileEntityEggInIce> EGG_IN_ICE = register("egginice", TileEntityEggInIce::new, IafBlockRegistry.EGG_IN_ICE);
    public static final BlockEntityType<TileEntityPixieHouse> PIXIE_HOUSE = register("pixie_house", TileEntityPixieHouse::new, IafBlockRegistry.PIXIE_HOUSE_MUSHROOM_RED, IafBlockRegistry.PIXIE_HOUSE_MUSHROOM_BROWN, IafBlockRegistry.PIXIE_HOUSE_OAK, IafBlockRegistry.PIXIE_HOUSE_BIRCH, IafBlockRegistry.PIXIE_HOUSE_BIRCH, IafBlockRegistry.PIXIE_HOUSE_SPRUCE, IafBlockRegistry.PIXIE_HOUSE_DARK_OAK);
    public static final BlockEntityType<TileEntityJar> PIXIE_JAR = register("pixie_jar", TileEntityJar::new, IafBlockRegistry.JAR_EMPTY, IafBlockRegistry.JAR_PIXIE_0, IafBlockRegistry.JAR_PIXIE_1, IafBlockRegistry.JAR_PIXIE_2, IafBlockRegistry.JAR_PIXIE_3, IafBlockRegistry.JAR_PIXIE_4);
    public static final BlockEntityType<TileEntityMyrmexCocoon> MYRMEX_COCOON = register("myrmex_cocoon", TileEntityMyrmexCocoon::new, IafBlockRegistry.DESERT_MYRMEX_COCOON, IafBlockRegistry.JUNGLE_MYRMEX_COCOON);
    public static final BlockEntityType<TileEntityDragonforge> DRAGONFORGE_CORE = register("dragonforge_core", TileEntityDragonforge::new, IafBlockRegistry.DRAGONFORGE_FIRE_CORE, IafBlockRegistry.DRAGONFORGE_ICE_CORE, IafBlockRegistry.DRAGONFORGE_FIRE_CORE_DISABLED, IafBlockRegistry.DRAGONFORGE_ICE_CORE_DISABLED, IafBlockRegistry.DRAGONFORGE_LIGHTNING_CORE, IafBlockRegistry.DRAGONFORGE_LIGHTNING_CORE_DISABLED);
    public static final BlockEntityType<TileEntityDragonforgeBrick> DRAGONFORGE_BRICK = register("dragonforge_brick", TileEntityDragonforgeBrick::new, IafBlockRegistry.DRAGONFORGE_FIRE_BRICK, IafBlockRegistry.DRAGONFORGE_ICE_BRICK, IafBlockRegistry.DRAGONFORGE_LIGHTNING_BRICK);
    public static final BlockEntityType<TileEntityDragonforgeInput> DRAGONFORGE_INPUT = register("dragonforge_input", TileEntityDragonforgeInput::new, IafBlockRegistry.DRAGONFORGE_FIRE_INPUT, IafBlockRegistry.DRAGONFORGE_ICE_INPUT, IafBlockRegistry.DRAGONFORGE_LIGHTNING_INPUT);
    public static final BlockEntityType<TileEntityDreadPortal> DREAD_PORTAL = register("dread_portal", TileEntityDreadPortal::new, IafBlockRegistry.DREAD_PORTAL);
    public static final BlockEntityType<TileEntityDreadSpawner> DREAD_SPAWNER = register("dread_spawner", TileEntityDreadSpawner::new, IafBlockRegistry.DREAD_SPAWNER);
    public static final BlockEntityType<TileEntityGhostChest> GHOST_CHEST = register("ghost_chest", TileEntityGhostChest::new, IafBlockRegistry.GHOST_CHEST);

    private IafTileEntityRegistry() {
    }

    private static <T extends BlockEntity> BlockEntityType<T> register(String name, BlockEntityType.BlockEntitySupplier<T> factory, Block... blocks) {
        return Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, IafMod.id(name), new BlockEntityType<>(factory, new java.util.HashSet<>(java.util.Arrays.asList(blocks))));
    }

    public static void init() {
    }
}

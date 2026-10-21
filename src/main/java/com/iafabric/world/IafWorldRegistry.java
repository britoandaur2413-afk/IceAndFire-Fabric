package com.iafabric.world;

import com.iafabric.IafConfig;
import com.iafabric.IafMod;
import com.iafabric.config.BiomeConfig;
import com.iafabric.config.biome.IafSpawnBiomeData;
import com.iafabric.entity.IafEntityRegistry;
import com.iafabric.world.feature.SpawnDeathWorm;
import com.iafabric.world.feature.SpawnDragonSkeleton;
import com.iafabric.world.feature.SpawnHippocampus;
import com.iafabric.world.feature.SpawnSeaSerpent;
import com.iafabric.world.feature.SpawnStymphalianBird;
import com.iafabric.world.feature.SpawnWanderingCyclops;
import com.iafabric.world.gen.WorldGenCyclopsCave;
import com.iafabric.world.gen.WorldGenFireDragonCave;
import com.iafabric.world.gen.WorldGenMountainDragonRoost;
import com.iafabric.world.gen.WorldGenFireDragonRoosts;
import com.iafabric.world.gen.WorldGenHydraCave;
import com.iafabric.world.gen.WorldGenIceDragonCave;
import com.iafabric.world.gen.WorldGenIceDragonRoosts;
import com.iafabric.world.gen.WorldGenLightningDragonCave;
import com.iafabric.world.gen.WorldGenLightningDragonRoosts;
import com.iafabric.world.gen.WorldGenMyrmexHive;
import com.iafabric.world.gen.WorldGenPixieVillage;
import com.iafabric.world.gen.WorldGenSirenIsland;
import net.fabricmc.fabric.api.biome.v1.BiomeModifications;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import org.apache.commons.lang3.tuple.Pair;

import java.util.function.BooleanSupplier;

/**
 * The mod's world generation. The generators are code (Feature); how often and where they are
 * placed is data (data/iceandfire_fabric/worldgen/configured_feature and placed_feature), and the biome
 * config picks the biomes they are added to. The jigsaw structures (mausoleum, graveyard, gorgon
 * temple) are data only.
 */
public final class IafWorldRegistry {
    public static final Feature FIRE_DRAGON_ROOST = register("fire_dragon_roost", new WorldGenFireDragonRoosts(null));
    public static final Feature ICE_DRAGON_ROOST = register("ice_dragon_roost", new WorldGenIceDragonRoosts(null));
    public static final Feature LIGHTNING_DRAGON_ROOST = register("lightning_dragon_roost", new WorldGenLightningDragonRoosts(null));
    public static final Feature MOUNTAIN_DRAGON_ROOST = register("mountain_dragon_roost", new WorldGenMountainDragonRoost(null));
    public static final Feature FIRE_DRAGON_CAVE = register("fire_dragon_cave", new WorldGenFireDragonCave(null));
    public static final Feature ICE_DRAGON_CAVE = register("ice_dragon_cave", new WorldGenIceDragonCave(null));
    public static final Feature LIGHTNING_DRAGON_CAVE = register("lightning_dragon_cave", new WorldGenLightningDragonCave(null));
    public static final Feature CYCLOPS_CAVE = register("cyclops_cave", new WorldGenCyclopsCave(null));
    public static final Feature PIXIE_VILLAGE = register("pixie_village", new WorldGenPixieVillage(null));
    public static final Feature SIREN_ISLAND = register("siren_island", new WorldGenSirenIsland(null));
    public static final Feature HYDRA_CAVE = register("hydra_cave", new WorldGenHydraCave(null));
    public static final Feature MYRMEX_HIVE_DESERT = register("myrmex_hive_desert", new WorldGenMyrmexHive(false, false, null));
    public static final Feature MYRMEX_HIVE_JUNGLE = register("myrmex_hive_jungle", new WorldGenMyrmexHive(false, true, null));
    public static final Feature SPAWN_DEATH_WORM = register("spawn_death_worm", new SpawnDeathWorm(null));
    public static final Feature SPAWN_DRAGON_SKELETON_L = register("spawn_dragon_skeleton_l", new SpawnDragonSkeleton(IafEntityRegistry.LIGHTNING_DRAGON, null));
    public static final Feature SPAWN_DRAGON_SKELETON_F = register("spawn_dragon_skeleton_f", new SpawnDragonSkeleton(IafEntityRegistry.FIRE_DRAGON, null));
    public static final Feature SPAWN_DRAGON_SKELETON_I = register("spawn_dragon_skeleton_i", new SpawnDragonSkeleton(IafEntityRegistry.ICE_DRAGON, null));
    public static final Feature SPAWN_HIPPOCAMPUS = register("spawn_hippocampus", new SpawnHippocampus(null));
    public static final Feature SPAWN_SEA_SERPENT = register("spawn_sea_serpent", new SpawnSeaSerpent(null));
    public static final Feature SPAWN_STYMPHALIAN_BIRD = register("spawn_stymphalian_bird", new SpawnStymphalianBird(null));
    public static final Feature SPAWN_WANDERING_CYCLOPS = register("spawn_wandering_cyclops", new SpawnWanderingCyclops(null));

    private IafWorldRegistry() {
    }

    /** Registers the feature's codec; the feature itself is data (data/iceandfire_fabric/worldgen/feature/<name>.json). */
    private static <F extends Feature> F register(String name, F feature) {
        Registry.register(BuiltInRegistries.FEATURE_TYPE, IafMod.id(name), feature.codec());
        return feature;
    }

    public static void init() {
        Registry.register(BuiltInRegistries.PLACEMENT_MODIFIER_TYPE, IafMod.id("biome_extended"), CustomBiomeFilter.CODEC);
        IafProcessors.init();
        GenerationStep.Decoration vegetal = GenerationStep.Decoration.VEGETAL_DECORATION;
        GenerationStep.Decoration ores = GenerationStep.Decoration.UNDERGROUND_ORES;
        GenerationStep.Decoration surface = GenerationStep.Decoration.SURFACE_STRUCTURES;
        GenerationStep.Decoration underground = GenerationStep.Decoration.UNDERGROUND_STRUCTURES;
        add("fire_lily", vegetal, BiomeConfig.fireLilyBiomes, () -> true);
        add("lightning_lily", vegetal, BiomeConfig.lightningLilyBiomes, () -> true);
        add("frost_lily", vegetal, BiomeConfig.iceLilyBiomes, () -> true);
        add("silver_ore", ores, BiomeConfig.oreGenBiomes, () -> IafConfig.generateSilverOre);
        add("sapphire_ore", ores, BiomeConfig.sapphireBiomes, () -> IafConfig.generateSapphireOre);
        add("amethyst_ore", ores, BiomeConfig.amethystBiomes, () -> IafConfig.generateAmythestOre);
        // dragon roosts and dens are structures now (worldgen/structure dragon_roost, dragon_cave): /locate finds them
        add("cyclops_cave", surface, BiomeConfig.cyclopsCaveBiomes, () -> IafConfig.generateCyclopsCaves);
        add("pixie_village", surface, BiomeConfig.pixieBiomes, () -> IafConfig.spawnPixies);
        // (the hydra's cave is a structure now, HydraCaveStructure: the game and the exiled pixie can look for it)
        add("myrmex_hive_desert", surface, BiomeConfig.desertMyrmexBiomes, () -> IafConfig.generateMyrmexColonies);
        add("myrmex_hive_jungle", surface, BiomeConfig.jungleMyrmexBiomes, () -> IafConfig.generateMyrmexColonies);
        add("siren_island", surface, BiomeConfig.sirenBiomes, () -> IafConfig.generateSirenIslands);
        add("spawn_death_worm", surface, BiomeConfig.deathwormBiomes, () -> IafConfig.spawnDeathWorm);
        add("spawn_wandering_cyclops", surface, BiomeConfig.wanderingCyclopsBiomes, () -> IafConfig.generateWanderingCyclops);
        add("spawn_dragon_skeleton_l", surface, BiomeConfig.lightningDragonSkeletonBiomes, () -> IafConfig.generateDragonSkeletons);
        add("spawn_dragon_skeleton_f", surface, BiomeConfig.fireDragonSkeletonBiomes, () -> IafConfig.generateDragonSkeletons);
        add("spawn_dragon_skeleton_i", surface, BiomeConfig.iceDragonSkeletonBiomes, () -> IafConfig.generateDragonSkeletons);
        add("spawn_hippocampus", surface, BiomeConfig.hippocampusBiomes, () -> IafConfig.spawnHippocampus);
        add("spawn_sea_serpent", surface, BiomeConfig.seaSerpentBiomes, () -> IafConfig.spawnSeaSerpents);
        add("spawn_stymphalian_bird", surface, BiomeConfig.stymphalianBiomes, () -> IafConfig.spawnStymphalianBirds);
    }

    private static void add(String placed, GenerationStep.Decoration step, Pair<String, IafSpawnBiomeData> biomes, BooleanSupplier enabled) {
        if (!enabled.getAsBoolean()) {
            return;
        }
        ResourceKey<PlacedFeature> key = ResourceKey.create(Registries.PLACED_FEATURE, IafMod.id(placed));
        BiomeModifications.addFeature(ctx -> {
            try {
                return BiomeConfig.test(biomes, ctx.getBiomeHolder());
            } catch (Exception e) {
                return false;
            }
        }, step, key);
    }

    public static boolean isFarEnoughFromSpawn(final LevelAccessor level, final BlockPos position) {
        BlockPos spawn = level instanceof ServerLevel serverLevel ? serverLevel.getRespawnData().pos() : BlockPos.ZERO;
        BlockPos spawnRelative = new BlockPos(spawn.getX(), position.getY(), spawn.getZ());
        return !spawnRelative.closerThan(position, IafConfig.dangerousWorldGenDistanceLimit);
    }

    public static boolean isFarEnoughFromDangerousGen(final ServerLevelAccessor level, final BlockPos position, final String id) {
        return isFarEnoughFromDangerousGen(level, position, id, IafWorldData.FeatureType.SURFACE);
    }

    public static boolean isFarEnoughFromDangerousGen(final ServerLevelAccessor level, final BlockPos position, final String id, final IafWorldData.FeatureType type) {
        IafWorldData data = IafWorldData.get(level.getLevel());
        return data.check(type, position, id);
    }
}

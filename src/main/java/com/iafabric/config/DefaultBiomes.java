package com.iafabric.config;

import com.iafabric.config.biome.BiomeEntryType;
import com.iafabric.config.biome.IafSpawnBiomeData;

import net.fabricmc.fabric.api.tag.convention.v1.ConventionalBiomeTags;

// TODO: 1.19 remove BIOME_DICT entries
public class DefaultBiomes {

    public static final IafSpawnBiomeData OVERWORLD = new IafSpawnBiomeData()
            .addBiomeEntry(BiomeEntryType.BIOME_TAG, false, ConventionalBiomeTags.IN_OVERWORLD.location().toString(), 0);

    public static final IafSpawnBiomeData FIREDRAGON_ROOST = new IafSpawnBiomeData()
            .addBiomeEntry(BiomeEntryType.BIOME_TAG, false, ConventionalBiomeTags.IN_OVERWORLD.location().toString(), 0)
            .addBiomeEntry(BiomeEntryType.BIOME_TAG, false, ConventionalBiomeTags.CLIMATE_HOT.location().toString(), 0)
            .addBiomeEntry(BiomeEntryType.BIOME_TAG, true, ConventionalBiomeTags.JUNGLE.location().toString(), 0)
            .addBiomeEntry(BiomeEntryType.BIOME_TAG, true, ConventionalBiomeTags.BADLANDS.location().toString(), 0)
            .addBiomeEntry(BiomeEntryType.BIOME_TAG, true, ConventionalBiomeTags.SAVANNA.location().toString(), 0)
            .addBiomeEntry(BiomeEntryType.BIOME_TAG, false, ConventionalBiomeTags.IN_OVERWORLD.location().toString(), 1)
            .addBiomeEntry(BiomeEntryType.BIOME_TAG, false, ConventionalBiomeTags.PLAINS.location().toString(), 1)
            .addBiomeEntry(BiomeEntryType.BIOME_TAG, true, ConventionalBiomeTags.CLIMATE_WET.location().toString(), 1)
            .addBiomeEntry(BiomeEntryType.BIOME_TAG, true, ConventionalBiomeTags.CLIMATE_COLD.location().toString(), 1)
            .addBiomeEntry(BiomeEntryType.BIOME_TAG, true, ConventionalBiomeTags.JUNGLE.location().toString(), 1)
            .addBiomeEntry(BiomeEntryType.BIOME_TAG, true, ConventionalBiomeTags.BADLANDS.location().toString(), 1)
            .addBiomeEntry(BiomeEntryType.BIOME_TAG, true, ConventionalBiomeTags.SAVANNA.location().toString(), 1)
            .addBiomeEntry(BiomeEntryType.BIOME_TAG, true, ConventionalBiomeTags.FOREST.location().toString(), 1)
            .addBiomeEntry(BiomeEntryType.REGISTRY_NAME, false, "terralith:volcanic_crater", 2)
            .addBiomeEntry(BiomeEntryType.REGISTRY_NAME, false, "terralith:volcanic_peaks", 3)
            .addBiomeEntry(BiomeEntryType.REGISTRY_NAME, false, "terralith:steppe", 4)
            .addBiomeEntry(BiomeEntryType.REGISTRY_NAME, false, "byg:rose_fields", 5)
            .addBiomeEntry(BiomeEntryType.REGISTRY_NAME, false, "byg:prairie", 6)
            .addBiomeEntry(BiomeEntryType.REGISTRY_NAME, false, "byg:firecracker_shrubland", 7)
            .addBiomeEntry(BiomeEntryType.REGISTRY_NAME, false, "byg:autumnal_valley", 8)
            .addBiomeEntry(BiomeEntryType.REGISTRY_NAME, false, "byg:lush_stacks", 9);
            //Not sure if firedragons should generate in mountainous terrain or not
            /*.addBiomeEntry(BiomeEntryType.BIOME_DICT, false, "overworld", 2)
            .addBiomeEntry(BiomeEntryType.BIOME_DICT, false, "mountain", 2)
            .addBiomeEntry(BiomeEntryType.BIOME_DICT, false, "snowy", 2);*/
    public static final IafSpawnBiomeData FIREDRAGON_CAVE = FIREDRAGON_ROOST
                    .addBiomeEntry(BiomeEntryType.REGISTRY_NAME, false, "terralith:cave/thermal_caves", 10);
    public static final IafSpawnBiomeData ICEDRAGON_ROOST = new IafSpawnBiomeData()
            //.addBiomeEntry(BiomeEntryType.BIOME_TAG, false, ConventionalBiomeTags.IN_OVERWORLD.location().toString(), 0)
            //.addBiomeEntry(BiomeEntryType.BIOME_TAG, false, ConventionalBiomeTags.ICY.location().toString()"icy", 0) This category doesn't seem to exist anymore
            .addBiomeEntry(BiomeEntryType.BIOME_TAG, false, ConventionalBiomeTags.IN_OVERWORLD.location().toString(), 0)
            .addBiomeEntry(BiomeEntryType.BIOME_TAG, false, ConventionalBiomeTags.SNOWY.location().toString(), 0)
            .addBiomeEntry(BiomeEntryType.BIOME_TAG, false, ConventionalBiomeTags.IN_OVERWORLD.location().toString(), 1)
            .addBiomeEntry(BiomeEntryType.BIOME_TAG, false, ConventionalBiomeTags.CLIMATE_COLD.location().toString(), 1)
            .addBiomeEntry(BiomeEntryType.REGISTRY_NAME, false, "terralith:snowy_badlands", 2)
            .addBiomeEntry(BiomeEntryType.REGISTRY_NAME, false, "terralith:snowy_maple_forest", 3)
            .addBiomeEntry(BiomeEntryType.REGISTRY_NAME, false, "terralith:snowy_shield", 4)
            .addBiomeEntry(BiomeEntryType.REGISTRY_NAME, false, "terralith:wintry_forest", 5)
            .addBiomeEntry(BiomeEntryType.REGISTRY_NAME, false, "terralith:wintry_lowlands", 6)
            .addBiomeEntry(BiomeEntryType.REGISTRY_NAME, false, "byg:shattered_glacier", 7)
            .addBiomeEntry(BiomeEntryType.REGISTRY_NAME, false, "byg:frosted_taiga", 8)
            .addBiomeEntry(BiomeEntryType.REGISTRY_NAME, false, "byg:frosted_coniferous_forest", 9);

    public static final IafSpawnBiomeData ICEDRAGON_CAVE = ICEDRAGON_ROOST
            .addBiomeEntry(BiomeEntryType.REGISTRY_NAME, false, "terralith:cave/ice_caves", 10);

    public static final IafSpawnBiomeData LIGHTNING_ROOST = new IafSpawnBiomeData()
            .addBiomeEntry(BiomeEntryType.BIOME_TAG, false, ConventionalBiomeTags.IN_OVERWORLD.location().toString(), 0)
            .addBiomeEntry(BiomeEntryType.BIOME_TAG, false, ConventionalBiomeTags.JUNGLE.location().toString(), 0)
            .addBiomeEntry(BiomeEntryType.BIOME_TAG, false, ConventionalBiomeTags.IN_OVERWORLD.location().toString(), 1)
            .addBiomeEntry(BiomeEntryType.BIOME_TAG, false, ConventionalBiomeTags.BADLANDS.location().toString(), 1)
            .addBiomeEntry(BiomeEntryType.BIOME_TAG, false, ConventionalBiomeTags.IN_OVERWORLD.location().toString(), 2)
            .addBiomeEntry(BiomeEntryType.BIOME_TAG, false, ConventionalBiomeTags.SAVANNA.location().toString(), 2)
            .addBiomeEntry(BiomeEntryType.REGISTRY_NAME, false, "terralith:ashen_savanna", 3)
            .addBiomeEntry(BiomeEntryType.REGISTRY_NAME, false, "terralith:savanna_badlands", 4)
            .addBiomeEntry(BiomeEntryType.REGISTRY_NAME, false, "terralith:savanna_slopes", 5)
            .addBiomeEntry(BiomeEntryType.REGISTRY_NAME, false, "byg:guiana_shield", 6)
            .addBiomeEntry(BiomeEntryType.REGISTRY_NAME, false, "byg:temperate_rainforest", 7)
            .addBiomeEntry(BiomeEntryType.REGISTRY_NAME, false, "byg:tropical_rainforest", 8)
            .addBiomeEntry(BiomeEntryType.REGISTRY_NAME, false, "byg:jacaranda_forest", 8)
            .addBiomeEntry(BiomeEntryType.REGISTRY_NAME, false, "byg:sierra_badlands", 9);

    public static final IafSpawnBiomeData LIGHTNING_CAVE = LIGHTNING_ROOST;
    public static final IafSpawnBiomeData VERY_HOT = new IafSpawnBiomeData()
            .addBiomeEntry(BiomeEntryType.BIOME_TAG, false, ConventionalBiomeTags.CLIMATE_HOT.location().toString(), 0)
            .addBiomeEntry(BiomeEntryType.REGISTRY_NAME, false, "terralith:shrubland", 1)
            .addBiomeEntry(BiomeEntryType.REGISTRY_NAME, false, "terralith:volcanic_crater", 2)
            .addBiomeEntry(BiomeEntryType.REGISTRY_NAME, false, "terralith:volcanic_peaks", 3)
            .addBiomeEntry(BiomeEntryType.REGISTRY_NAME, false, "byg:firecracker_shrubland", 4);

    public static final IafSpawnBiomeData VERY_SNOWY = new IafSpawnBiomeData()
            .addBiomeEntry(BiomeEntryType.BIOME_TAG, false, ConventionalBiomeTags.IN_OVERWORLD.location().toString(), 0)
            .addBiomeEntry(BiomeEntryType.BIOME_TAG, false, ConventionalBiomeTags.SNOWY.location().toString(), 0)
            .addBiomeEntry(BiomeEntryType.BIOME_TAG, true, ConventionalBiomeTags.CLIMATE_WET.location().toString(), 0)
            .addBiomeEntry(BiomeEntryType.REGISTRY_NAME, false, "terralith:wintry_forest", 1)
            .addBiomeEntry(BiomeEntryType.REGISTRY_NAME, false, "terralith:wintry_lowlands", 2);
    //.addBiomeEntry(BiomeEntryType.BIOME_DICT, false, "overworld", 1)
    //.addBiomeEntry(BiomeEntryType.BIOME_CATEGORY, false, "icy", 1); // Doesn't exist anymore, might need a replacement

    public static final IafSpawnBiomeData MAUSOLEUM = new IafSpawnBiomeData()
            /*.addBiomeEntry(BiomeEntryType.BIOME_TAG, false, ConventionalBiomeTags.IN_OVERWORLD.location().toString(), 0)
            .addBiomeEntry(BiomeEntryType.BIOME_TAG, false, ConventionalBiomeTags.SNOWY.location().toString(), 0)
            .addBiomeEntry(BiomeEntryType.BIOME_TAG, true, ConventionalBiomeTags.CLIMATE_WET.location().toString(), 0)
            .addBiomeEntry(BiomeEntryType.BIOME_TAG, true, ConventionalBiomeTags.OCEAN.location().toString(), 0)
            .addBiomeEntry(BiomeEntryType.BIOME_TAG, true, ConventionalBiomeTags.RIVER.location().toString(), 0)*/
            .addBiomeEntry(BiomeEntryType.BIOME_DICT, false, "overworld", 0)
            .addBiomeEntry(BiomeEntryType.BIOME_DICT, false, "snowy", 0)
            .addBiomeEntry(BiomeEntryType.BIOME_DICT, true, "ocean", 0)
            .addBiomeEntry(BiomeEntryType.BIOME_DICT, false, "overworld", 1)
            .addBiomeEntry(BiomeEntryType.BIOME_CATEGORY, false, "icy", 1)
            .addBiomeEntry(BiomeEntryType.BIOME_DICT, true, "ocean", 1)
            .addBiomeEntry(BiomeEntryType.REGISTRY_NAME, false, "terralith:wintry_forest", 2)
            .addBiomeEntry(BiomeEntryType.REGISTRY_NAME, false, "terralith:wintry_lowlands", 3);

    public static final IafSpawnBiomeData SNOWY = new IafSpawnBiomeData()
            .addBiomeEntry(BiomeEntryType.BIOME_TAG, false, ConventionalBiomeTags.IN_OVERWORLD.location().toString(), 0)
            .addBiomeEntry(BiomeEntryType.BIOME_TAG, false, ConventionalBiomeTags.SNOWY.location().toString(), 0);

    public static final IafSpawnBiomeData VERY_HILLY = new IafSpawnBiomeData()
            .addBiomeEntry(BiomeEntryType.BIOME_TAG, false, ConventionalBiomeTags.IN_OVERWORLD.location().toString(), 0)
            .addBiomeEntry(BiomeEntryType.BIOME_TAG, false, ConventionalBiomeTags.MOUNTAIN.location().toString(), 0)
            .addBiomeEntry(BiomeEntryType.BIOME_TAG, false, ConventionalBiomeTags.IN_OVERWORLD.location().toString(), 1)
            .addBiomeEntry(BiomeEntryType.BIOME_TAG, false, ConventionalBiomeTags.EXTREME_HILLS.location().toString(), 1);

    public static final IafSpawnBiomeData WOODLAND = new IafSpawnBiomeData()
            .addBiomeEntry(BiomeEntryType.BIOME_TAG, false, ConventionalBiomeTags.IN_OVERWORLD.location().toString(), 0)
            .addBiomeEntry(BiomeEntryType.BIOME_CATEGORY, false, "forest", 0);

    public static final IafSpawnBiomeData SAVANNAS = new IafSpawnBiomeData()
            .addBiomeEntry(BiomeEntryType.BIOME_TAG, false, ConventionalBiomeTags.IN_OVERWORLD.location().toString(), 0)
            .addBiomeEntry(BiomeEntryType.BIOME_TAG, false, ConventionalBiomeTags.SAVANNA.location().toString(), 0)
            .addBiomeEntry(BiomeEntryType.REGISTRY_NAME, false, "terralith:fractured_savanna", 1)
            .addBiomeEntry(BiomeEntryType.REGISTRY_NAME, false, "terralith:savanna_badlands", 2)
            .addBiomeEntry(BiomeEntryType.REGISTRY_NAME, false, "terralith:savanna_slopes", 3)
            .addBiomeEntry(BiomeEntryType.REGISTRY_NAME, false, "byg:baobab_savanna", 4)
            .addBiomeEntry(BiomeEntryType.REGISTRY_NAME, false, "byg:araucaria_savanna", 5);

    public static final IafSpawnBiomeData BEACHES = new IafSpawnBiomeData()
            /*.addBiomeEntry(BiomeEntryType.BIOME_TAG, false, ConventionalBiomeTags.IN_OVERWORLD.location().toString(), 0)
            .addBiomeEntry(BiomeEntryType.BIOME_TAG, false, ConventionalBiomeTags.BEACH.location().toString(), 0)*/
            .addBiomeEntry(BiomeEntryType.BIOME_DICT, false, "overworld", 0)
            .addBiomeEntry(BiomeEntryType.BIOME_DICT, false, "beach", 0)
            .addBiomeEntry(BiomeEntryType.BIOME_DICT, false, "overworld", 1)
            .addBiomeEntry(BiomeEntryType.BIOME_CATEGORY, false, "beach", 1)
            .addBiomeEntry(BiomeEntryType.REGISTRY_NAME, false, "byg:dacite_shore", 2)
            .addBiomeEntry(BiomeEntryType.REGISTRY_NAME, false, "byg:basalt_barrera", 3);

    public static final IafSpawnBiomeData SWAMPS = new IafSpawnBiomeData()
            .addBiomeEntry(BiomeEntryType.BIOME_TAG, false, ConventionalBiomeTags.IN_OVERWORLD.location().toString(), 0)
            .addBiomeEntry(BiomeEntryType.BIOME_TAG, false, ConventionalBiomeTags.SWAMP.location().toString(), 0)
            .addBiomeEntry(BiomeEntryType.REGISTRY_NAME, false, "terralith:orchid_swamp", 1)
            .addBiomeEntry(BiomeEntryType.REGISTRY_NAME, false, "byg:crag_gardens", 2)
            .addBiomeEntry(BiomeEntryType.REGISTRY_NAME, false, "byg:cypress_swamplands", 3)
            .addBiomeEntry(BiomeEntryType.REGISTRY_NAME, false, "byg:bayou", 4);

    public static final IafSpawnBiomeData DESERT = new IafSpawnBiomeData()
            .addBiomeEntry(BiomeEntryType.BIOME_TAG, false, ConventionalBiomeTags.CLIMATE_DRY.location().toString(), 0)
            .addBiomeEntry(BiomeEntryType.BIOME_TAG, false, ConventionalBiomeTags.CLIMATE_HOT.location().toString(), 0)
            .addBiomeEntry(BiomeEntryType.BIOME_TAG, false, ConventionalBiomeTags.DESERT.location().toString(), 0)
            .addBiomeEntry(BiomeEntryType.BIOME_TAG, true, ConventionalBiomeTags.BADLANDS.location().toString(), 0)
            .addBiomeEntry(BiomeEntryType.REGISTRY_NAME, false, "terralith:desert_canyon", 1)
            .addBiomeEntry(BiomeEntryType.REGISTRY_NAME, false, "terralith:desert_oasis", 2)
            .addBiomeEntry(BiomeEntryType.REGISTRY_NAME, false, "terralith:desert_spires", 3)
            .addBiomeEntry(BiomeEntryType.REGISTRY_NAME, false, "terralith:ancient_sands", 4)
            .addBiomeEntry(BiomeEntryType.REGISTRY_NAME, false, "byg:mojave_desert", 5)
            .addBiomeEntry(BiomeEntryType.REGISTRY_NAME, false, "byg:atacama_desert", 6)
            .addBiomeEntry(BiomeEntryType.REGISTRY_NAME, false, "byg:windswept_valley", 7);

    public static final IafSpawnBiomeData OCEANS = new IafSpawnBiomeData()
            .addBiomeEntry(BiomeEntryType.BIOME_TAG, false, ConventionalBiomeTags.IN_OVERWORLD.location().toString(), 0)
            .addBiomeEntry(BiomeEntryType.BIOME_CATEGORY, false, "ocean", 0);

    public static final IafSpawnBiomeData PIXIES = new IafSpawnBiomeData()
            .addBiomeEntry(BiomeEntryType.BIOME_TAG, false, ConventionalBiomeTags.IN_OVERWORLD.location().toString(), 0)
            .addBiomeEntry(BiomeEntryType.BIOME_TAG, false, ConventionalBiomeTags.FLORAL.location().toString(), 0)
            .addBiomeEntry(BiomeEntryType.BIOME_TAG, false, ConventionalBiomeTags.IN_OVERWORLD.location().toString(), 1)
            .addBiomeEntry(BiomeEntryType.BIOME_TAG, false, ConventionalBiomeTags.VEGETATION_DENSE.location().toString(), 1)
            .addBiomeEntry(BiomeEntryType.BIOME_CATEGORY, false, "forest", 1)
            .addBiomeEntry(BiomeEntryType.BIOME_TAG, true, ConventionalBiomeTags.MOUNTAIN.location().toString(), 1)
            .addBiomeEntry(BiomeEntryType.BIOME_TAG, true, ConventionalBiomeTags.EXTREME_HILLS.location().toString(), 1)
            .addBiomeEntry(BiomeEntryType.BIOME_TAG, true, ConventionalBiomeTags.CLIMATE_WET.location().toString(), 1)
            .addBiomeEntry(BiomeEntryType.BIOME_TAG, true, ConventionalBiomeTags.CLIMATE_HOT.location().toString(), 1)
            .addBiomeEntry(BiomeEntryType.BIOME_TAG, true, ConventionalBiomeTags.CLIMATE_COLD.location().toString(), 1)
            .addBiomeEntry(BiomeEntryType.BIOME_TAG, true, ConventionalBiomeTags.CLIMATE_DRY.location().toString(), 1)
            .addBiomeEntry(BiomeEntryType.BIOME_CATEGORY, true, "taiga", 1)
            .addBiomeEntry(BiomeEntryType.REGISTRY_NAME, false, "terralith:siberian_taiga", 2)
            .addBiomeEntry(BiomeEntryType.REGISTRY_NAME, false, "terralith:skylands", 3)
            .addBiomeEntry(BiomeEntryType.REGISTRY_NAME, false, "terralith:mirage_isles", 4)
            .addBiomeEntry(BiomeEntryType.REGISTRY_NAME, false, "byg:amaranth_fields", 5)
            .addBiomeEntry(BiomeEntryType.REGISTRY_NAME, false, "byg:skyris_vale", 6)
            .addBiomeEntry(BiomeEntryType.REGISTRY_NAME, false, "byg:rainbow_beach", 7);

    public static final IafSpawnBiomeData JUNGLE = new IafSpawnBiomeData()
            .addBiomeEntry(BiomeEntryType.BIOME_TAG, false, ConventionalBiomeTags.IN_OVERWORLD.location().toString(), 0)
            .addBiomeEntry(BiomeEntryType.BIOME_CATEGORY, false, "jungle", 0)
            .addBiomeEntry(BiomeEntryType.REGISTRY_NAME, false, "terralith:rocky_jungle", 1)
            .addBiomeEntry(BiomeEntryType.REGISTRY_NAME, false, "terralith:jungle_mountains", 2)
            .addBiomeEntry(BiomeEntryType.REGISTRY_NAME, false, "terralith:tropical_jungle", 3)
            .addBiomeEntry(BiomeEntryType.REGISTRY_NAME, false, "byg:guiana_shield", 4)
            .addBiomeEntry(BiomeEntryType.REGISTRY_NAME, false, "byg:temperate_rainforest", 5)
            .addBiomeEntry(BiomeEntryType.REGISTRY_NAME, false, "byg:tropical_rainforest", 6)
            .addBiomeEntry(BiomeEntryType.REGISTRY_NAME, false, "byg:jacaranda_forest", 7);

    public static final IafSpawnBiomeData HILLS = new IafSpawnBiomeData()
            .addBiomeEntry(BiomeEntryType.BIOME_TAG, false, ConventionalBiomeTags.IN_OVERWORLD.location().toString(), 0)
            .addBiomeEntry(BiomeEntryType.BIOME_TAG, false, ConventionalBiomeTags.BADLANDS.location().toString(), 0)
            .addBiomeEntry(BiomeEntryType.BIOME_TAG, false, ConventionalBiomeTags.IN_OVERWORLD.location().toString(), 1)
            .addBiomeEntry(BiomeEntryType.BIOME_TAG, false, ConventionalBiomeTags.MOUNTAIN.location().toString(), 1)
            .addBiomeEntry(BiomeEntryType.BIOME_TAG, false, ConventionalBiomeTags.IN_OVERWORLD.location().toString(), 2)
            .addBiomeEntry(BiomeEntryType.BIOME_TAG, false, ConventionalBiomeTags.EXTREME_HILLS.location().toString(), 2)
            .addBiomeEntry(BiomeEntryType.REGISTRY_NAME, false, "terralith:rocky_mountains", 3);

    public static final IafSpawnBiomeData PLAINS = new IafSpawnBiomeData()
            .addBiomeEntry(BiomeEntryType.BIOME_TAG, false, ConventionalBiomeTags.IN_OVERWORLD.location().toString(), 0)
            .addBiomeEntry(BiomeEntryType.BIOME_TAG, false, ConventionalBiomeTags.PLAINS.location().toString(), 0)
            .addBiomeEntry(BiomeEntryType.REGISTRY_NAME, false, "terralith:steppe", 1)
            .addBiomeEntry(BiomeEntryType.REGISTRY_NAME, false, "byg:firecracker_shrubland", 2)
            .addBiomeEntry(BiomeEntryType.REGISTRY_NAME, false, "byg:autumnal_valley", 3);

    public static final IafSpawnBiomeData GRAVEYARD = new IafSpawnBiomeData()
            /*.addBiomeEntry(BiomeEntryType.BIOME_TAG, false, ConventionalBiomeTags.IN_OVERWORLD.location().toString(), 0)
            .addBiomeEntry(BiomeEntryType.BIOME_TAG, true, ConventionalBiomeTags.OCEAN.location().toString(), 0)
            .addBiomeEntry(BiomeEntryType.BIOME_TAG, true, ConventionalBiomeTags.AQUATIC.location().toString(), 0)
            .addBiomeEntry(BiomeEntryType.BIOME_TAG, true, ConventionalBiomeTags.BEACH.location().toString(), 0)
            .addBiomeEntry(BiomeEntryType.BIOME_CATEGORY, true, "ocean", 0)
            .addBiomeEntry(BiomeEntryType.BIOME_TAG, true, ConventionalBiomeTags.UNDERGROUND.location().toString(), 0)*/
            .addBiomeEntry(BiomeEntryType.BIOME_DICT, false, "overworld", 0)
            .addBiomeEntry(BiomeEntryType.BIOME_DICT, true, "ocean", 0)
            .addBiomeEntry(BiomeEntryType.BIOME_CATEGORY, true, "ocean", 0)
            .addBiomeEntry(BiomeEntryType.BIOME_CATEGORY, true, "river", 0)
            .addBiomeEntry(BiomeEntryType.BIOME_DICT, true, "beach", 0)
            .addBiomeEntry(BiomeEntryType.BIOME_CATEGORY, true, "beach", 0);

    public static final IafSpawnBiomeData HIPPOGRYPH_BLACK = new IafSpawnBiomeData()
            .addBiomeEntry(BiomeEntryType.BIOME_TAG, false, ConventionalBiomeTags.IN_OVERWORLD.location().toString(), 0)
            .addBiomeEntry(BiomeEntryType.REGISTRY_NAME, false, "minecraft:badlands", 0)
            .addBiomeEntry(BiomeEntryType.REGISTRY_NAME, false, "terralith:painted_mountains", 1);

    public static final IafSpawnBiomeData HIPPOGRYPH_DARK_BROWN = new IafSpawnBiomeData()
            .addBiomeEntry(BiomeEntryType.BIOME_TAG, false, ConventionalBiomeTags.IN_OVERWORLD.location().toString(), 0)
            .addBiomeEntry(BiomeEntryType.BIOME_TAG, false, ConventionalBiomeTags.FOREST.location().toString(), 0)
            .addBiomeEntry(BiomeEntryType.BIOME_TAG, false, ConventionalBiomeTags.MOUNTAIN.location().toString(), 0)
            .addBiomeEntry(BiomeEntryType.BIOME_TAG, false, ConventionalBiomeTags.SNOWY.location().toString(), 0)
            .addBiomeEntry(BiomeEntryType.BIOME_TAG, false, ConventionalBiomeTags.IN_OVERWORLD.location().toString(), 1)
            .addBiomeEntry(BiomeEntryType.BIOME_TAG, false, ConventionalBiomeTags.FOREST.location().toString(), 1)
            .addBiomeEntry(BiomeEntryType.BIOME_TAG, false, ConventionalBiomeTags.EXTREME_HILLS.location().toString(), 1)
            .addBiomeEntry(BiomeEntryType.BIOME_TAG, false, ConventionalBiomeTags.SNOWY.location().toString(), 1)
            .addBiomeEntry(BiomeEntryType.REGISTRY_NAME, false, "terralith:scarlet_mountains", 2);

    public static final IafSpawnBiomeData HIPPOGRYPH_WHITE = new IafSpawnBiomeData()
            .addBiomeEntry(BiomeEntryType.BIOME_TAG, false, ConventionalBiomeTags.IN_OVERWORLD.location().toString(), 0)
            .addBiomeEntry(BiomeEntryType.BIOME_TAG, false, ConventionalBiomeTags.VEGETATION_SPARSE.location().toString(), 0)
            .addBiomeEntry(BiomeEntryType.BIOME_TAG, false, ConventionalBiomeTags.SNOWY.location().toString(), 0)
            .addBiomeEntry(BiomeEntryType.BIOME_TAG, false, ConventionalBiomeTags.MOUNTAIN.location().toString(), 0)
            .addBiomeEntry(BiomeEntryType.BIOME_TAG, false, ConventionalBiomeTags.MOUNTAIN_PEAK.location().toString(), 0)
            .addBiomeEntry(BiomeEntryType.BIOME_TAG, false, ConventionalBiomeTags.IN_OVERWORLD.location().toString(), 1)
            .addBiomeEntry(BiomeEntryType.BIOME_TAG, false, ConventionalBiomeTags.VEGETATION_SPARSE.location().toString(), 1)
            .addBiomeEntry(BiomeEntryType.BIOME_TAG, false, ConventionalBiomeTags.SNOWY.location().toString(), 1)
            .addBiomeEntry(BiomeEntryType.BIOME_TAG, false, ConventionalBiomeTags.MOUNTAIN.location().toString(), 1)
            .addBiomeEntry(BiomeEntryType.BIOME_TAG, false, ConventionalBiomeTags.MOUNTAIN_SLOPE.location().toString(), 1)
            .addBiomeEntry(BiomeEntryType.REGISTRY_NAME, false, "terralith:rocky_mountains", 2);

    public static final IafSpawnBiomeData HIPPOGRYPH_GRAY = new IafSpawnBiomeData()
            .addBiomeEntry(BiomeEntryType.BIOME_TAG, false, ConventionalBiomeTags.IN_OVERWORLD.location().toString(), 0)
            .addBiomeEntry(BiomeEntryType.REGISTRY_NAME, false, "minecraft:windswept_forest", 0)
            .addBiomeEntry(BiomeEntryType.BIOME_TAG, false, ConventionalBiomeTags.IN_OVERWORLD.location().toString(), 1)
            .addBiomeEntry(BiomeEntryType.REGISTRY_NAME, false, "minecraft:windswept_hills", 1)
            .addBiomeEntry(BiomeEntryType.REGISTRY_NAME, false, "terralith:windswept_spires", 2);

    public static final IafSpawnBiomeData HIPPOGRYPH_CHESTNUT = new IafSpawnBiomeData()
            .addBiomeEntry(BiomeEntryType.BIOME_TAG, false, ConventionalBiomeTags.IN_OVERWORLD.location().toString(), 0)
            .addBiomeEntry(BiomeEntryType.BIOME_TAG, false, ConventionalBiomeTags.MOUNTAIN.location().toString(), 0)
            .addBiomeEntry(BiomeEntryType.BIOME_TAG, false, ConventionalBiomeTags.BADLANDS.location().toString(), 0)
            .addBiomeEntry(BiomeEntryType.BIOME_TAG, false, ConventionalBiomeTags.CLIMATE_DRY.location().toString(), 0)
            .addBiomeEntry(BiomeEntryType.REGISTRY_NAME, false, "terralith:savanna_badlands", 1);

    public static final IafSpawnBiomeData HIPPOGRYPH_CREAMY = new IafSpawnBiomeData()
            .addBiomeEntry(BiomeEntryType.BIOME_TAG, false, ConventionalBiomeTags.IN_OVERWORLD.location().toString(), 0)
            .addBiomeEntry(BiomeEntryType.REGISTRY_NAME, false, "minecraft:savanna_plateau", 0)
            .addBiomeEntry(BiomeEntryType.REGISTRY_NAME, false, "terralith:fractured_savanna", 1);

}

package com.iafabric.config;

import com.iafabric.IafMod;
import com.iafabric.config.biome.IafSpawnBiomeData;
import com.iafabric.config.biome.SpawnBiomeConfig;
import net.minecraft.core.Holder;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.biome.Biome;
import org.apache.commons.lang3.tuple.Pair;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

public class BiomeConfig {
    public static Pair<String, IafSpawnBiomeData> oreGenBiomes = Pair.of("iceandfire_fabric:ore_gen_biomes", DefaultBiomes.OVERWORLD);
    public static Pair<String, IafSpawnBiomeData> sapphireBiomes = Pair.of("iceandfire_fabric:sapphire_gen_biomes", DefaultBiomes.VERY_SNOWY);
    public static Pair<String, IafSpawnBiomeData> amethystBiomes = Pair.of("iceandfire_fabric:amethyst_gen_biomes", DefaultBiomes.SAVANNAS);
    public static Pair<String, IafSpawnBiomeData> fireLilyBiomes = Pair.of("iceandfire_fabric:fire_lily_biomes", DefaultBiomes.VERY_HOT);
    public static Pair<String, IafSpawnBiomeData> iceLilyBiomes = Pair.of("iceandfire_fabric:ice_lily_biomes", DefaultBiomes.VERY_SNOWY);
    public static Pair<String, IafSpawnBiomeData> lightningLilyBiomes = Pair.of("iceandfire_fabric:lightning_lily_biomes", DefaultBiomes.SAVANNAS);
    public static Pair<String, IafSpawnBiomeData> fireDragonBiomes = Pair.of("iceandfire_fabric:fire_dragon_biomes", DefaultBiomes.FIREDRAGON_ROOST);
    public static Pair<String, IafSpawnBiomeData> fireDragonCaveBiomes = Pair.of("iceandfire_fabric:fire_dragon_cave_biomes", DefaultBiomes.FIREDRAGON_CAVE);
    public static Pair<String, IafSpawnBiomeData> iceDragonBiomes = Pair.of("iceandfire_fabric:ice_dragon_biomes", DefaultBiomes.ICEDRAGON_ROOST);
    public static Pair<String, IafSpawnBiomeData> iceDragonCaveBiomes = Pair.of("iceandfire_fabric:ice_dragon_cave_biomes", DefaultBiomes.ICEDRAGON_CAVE);
    public static Pair<String, IafSpawnBiomeData> lightningDragonBiomes = Pair.of("iceandfire_fabric:lightning_dragon_biomes", DefaultBiomes.LIGHTNING_ROOST);
    public static Pair<String, IafSpawnBiomeData> lightningDragonCaveBiomes = Pair.of("iceandfire_fabric:lightning_dragon_cave_biomes", DefaultBiomes.LIGHTNING_CAVE);

    public static Pair<String, IafSpawnBiomeData> cyclopsCaveBiomes = Pair.of("iceandfire_fabric:cyclops_cave_biomes", DefaultBiomes.BEACHES);
    public static Pair<String, IafSpawnBiomeData> hippogryphBiomes = Pair.of("iceandfire_fabric:hippogryph_biomes", DefaultBiomes.HILLS);
    public static Pair<String, IafSpawnBiomeData> pixieBiomes = Pair.of("iceandfire_fabric:pixie_village_biomes", DefaultBiomes.PIXIES);
    public static Pair<String, IafSpawnBiomeData> hippocampusBiomes = Pair.of("iceandfire_fabric:hippocampus_biomes", DefaultBiomes.OCEANS);
    public static Pair<String, IafSpawnBiomeData> seaSerpentBiomes = Pair.of("iceandfire_fabric:sea_serpent_biomes", DefaultBiomes.OCEANS);
    public static Pair<String, IafSpawnBiomeData> sirenBiomes = Pair.of("iceandfire_fabric:siren_biomes", DefaultBiomes.OCEANS);
    public static Pair<String, IafSpawnBiomeData> amphithereBiomes = Pair.of("iceandfire_fabric:amphithere_biomes", DefaultBiomes.JUNGLE);
    public static Pair<String, IafSpawnBiomeData> desertMyrmexBiomes = Pair.of("iceandfire_fabric:desert_myrmex_biomes", DefaultBiomes.DESERT);
    public static Pair<String, IafSpawnBiomeData> jungleMyrmexBiomes = Pair.of("iceandfire_fabric:jungle_myrmex_biomes", DefaultBiomes.JUNGLE);
    public static Pair<String, IafSpawnBiomeData> snowyTrollBiomes = Pair.of("iceandfire_fabric:snowy_troll_biomes", DefaultBiomes.SNOWY);
    public static Pair<String, IafSpawnBiomeData> forestTrollBiomes = Pair.of("iceandfire_fabric:forest_troll_biomes", DefaultBiomes.WOODLAND);
    public static Pair<String, IafSpawnBiomeData> mountainTrollBiomes = Pair.of("iceandfire_fabric:mountain_troll_biomes", DefaultBiomes.VERY_HILLY);

    public static Pair<String, IafSpawnBiomeData> stymphalianBiomes = Pair.of("iceandfire_fabric:stymphalian_bird_biomes", DefaultBiomes.SWAMPS);
    public static Pair<String, IafSpawnBiomeData> hydraBiomes = Pair.of("iceandfire_fabric:hydra_cave_biomes", DefaultBiomes.SWAMPS);

    public static Pair<String, IafSpawnBiomeData> mausoleumBiomes = Pair.of("iceandfire_fabric:mausoleum_biomes", DefaultBiomes.MAUSOLEUM);
    public static Pair<String, IafSpawnBiomeData> graveyardBiomes = Pair.of("iceandfire_fabric:graveyard_biomes", DefaultBiomes.GRAVEYARD);
    public static Pair<String, IafSpawnBiomeData> gorgonTempleBiomes = Pair.of("iceandfire_fabric:gorgon_temple_biomes", DefaultBiomes.BEACHES);

    public static Pair<String, IafSpawnBiomeData> cockatriceBiomes = Pair.of("iceandfire_fabric:cockatrice_biomes", DefaultBiomes.SAVANNAS);
    public static Pair<String, IafSpawnBiomeData> deathwormBiomes = Pair.of("iceandfire_fabric:deathworm_biomes", DefaultBiomes.DESERT);
    public static Pair<String, IafSpawnBiomeData> wanderingCyclopsBiomes = Pair.of("iceandfire_fabric:wandering_cyclops_biomes", DefaultBiomes.PLAINS);
    public static Pair<String, IafSpawnBiomeData> lightningDragonSkeletonBiomes = Pair.of("iceandfire_fabric:lightning_dragon_skeleton_biomes", DefaultBiomes.SAVANNAS);
    public static Pair<String, IafSpawnBiomeData> fireDragonSkeletonBiomes = Pair.of("iceandfire_fabric:fire_dragon_skeleton_biomes", DefaultBiomes.DESERT);
    public static Pair<String, IafSpawnBiomeData> iceDragonSkeletonBiomes = Pair.of("iceandfire_fabric:ice_dragon_skeleton_biomes", DefaultBiomes.VERY_SNOWY);

    public static Pair<String, IafSpawnBiomeData> blackHippogryphBiomes = Pair.of("iceandfire_fabric:hippogryph_black_biomes", DefaultBiomes.HIPPOGRYPH_BLACK);
    public static Pair<String, IafSpawnBiomeData> brownHippogryphBiomes = Pair.of("iceandfire_fabric:hippogryph_brown_biomes", DefaultBiomes.VERY_HILLY);
    public static Pair<String, IafSpawnBiomeData> grayHippogryphBiomes = Pair.of("iceandfire_fabric:hippogryph_gray_biomes", DefaultBiomes.HIPPOGRYPH_GRAY);
    public static Pair<String, IafSpawnBiomeData> chestnutHippogryphBiomes = Pair.of("iceandfire_fabric:hippogryph_chestnut_biomes", DefaultBiomes.HIPPOGRYPH_CHESTNUT);
    public static Pair<String, IafSpawnBiomeData> creamyHippogryphBiomes = Pair.of("iceandfire_fabric:hippogryph_creamy_biomes", DefaultBiomes.HIPPOGRYPH_CREAMY);
    public static Pair<String, IafSpawnBiomeData> darkBrownHippogryphBiomes = Pair.of("iceandfire_fabric:hippogryph_dark_brown_biomes", DefaultBiomes.HIPPOGRYPH_DARK_BROWN);
    public static Pair<String, IafSpawnBiomeData> whiteHippogryphBiomes = Pair.of("iceandfire_fabric:hippogryph_white_biomes", DefaultBiomes.HIPPOGRYPH_WHITE);

    private static boolean init = false;
    private static final Map<String, IafSpawnBiomeData> biomeConfigValues = new HashMap<>();

    public static void init() {
        try {
            for (Field f : BiomeConfig.class.getFields()) {
                Object obj = f.get(null);
                if (obj instanceof Pair) {
                    String id = (String) ((Pair) obj).getLeft();
                    IafSpawnBiomeData data = (IafSpawnBiomeData) ((Pair) obj).getRight();
                    biomeConfigValues.put(id, SpawnBiomeConfig.create(Identifier.parse(id), data));
                }
            }
        }catch (Exception e){
            IafMod.LOGGER.warn("Encountered error building iceandfire biome config .json files");
            e.printStackTrace();
        }
        init = true;
    }

    public static boolean test(Pair<String, IafSpawnBiomeData> spawns, Holder<Biome> biome) {
        if (!init) {
            init();
        }
        return biomeConfigValues.get(spawns.getKey()).matches(biome);
    }
}

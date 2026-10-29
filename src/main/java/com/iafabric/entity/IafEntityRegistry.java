package com.iafabric.entity;

import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import com.iafabric.IafConfig;
import com.iafabric.IafMod;
import com.iafabric.config.BiomeConfig;
import net.fabricmc.fabric.api.biome.v1.BiomeModifications;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.SpawnPlacementTypes;
import net.minecraft.world.entity.SpawnPlacements;
import net.minecraft.world.level.levelgen.Heightmap;


public final class IafEntityRegistry {

    /** (each player's game places a dragon's parts itself, from the dragon: where the server has them is sent only seldom) */
    public static final EntityType<EntityDragonPart> DRAGON_MULTIPART = register("dragon_multipart", EntityType.Builder.<EntityDragonPart>of(EntityDragonPart::new, MobCategory.MISC).sized(0.5F, 0.5F).fireImmune().updateInterval(100));
    /** Effects drawn for a while (Vfx): never saved, nothing to summon. */
    public static final EntityType<EntityVfx> VFX = register("vfx", EntityType.Builder.<EntityVfx>of(EntityVfx::new, MobCategory.MISC).sized(0.25F, 0.25F).noSave().fireImmune().clientTrackingRange(10).updateInterval(40));
    public static final EntityType<EntitySlowPart> SLOW_MULTIPART = register("multipart", EntityType.Builder.<EntitySlowPart>of(EntitySlowPart::new, MobCategory.MISC).sized(0.5F, 0.5F).fireImmune());
    public static final EntityType<EntityHydraHead> HYDRA_MULTIPART = register("hydra_multipart", EntityType.Builder.<EntityHydraHead>of(EntityHydraHead::new, MobCategory.MISC).sized(0.5F, 0.5F).fireImmune());
    public static final EntityType<EntityCyclopsEye> CYCLOPS_MULTIPART = register("cylcops_multipart", EntityType.Builder.<EntityCyclopsEye>of(EntityCyclopsEye::new, MobCategory.MISC).sized(0.5F, 0.5F).fireImmune());
    public static final EntityType<EntityDragonEgg> DRAGON_EGG = register("dragon_egg", EntityType.Builder.of(EntityDragonEgg::new, MobCategory.MISC).sized(0.45F, 0.55F).fireImmune());
    public static final EntityType<EntityDragonArrow> DRAGON_ARROW = register("dragon_arrow", EntityType.Builder.<EntityDragonArrow>of(EntityDragonArrow::new, MobCategory.MISC).sized(0.5F, 0.5F));
    public static final EntityType<EntityDragonSkull> DRAGON_SKULL = register("dragon_skull", EntityType.Builder.of(EntityDragonSkull::new, MobCategory.MISC).sized(0.9F, 0.65F));
    public static final EntityType<EntityFireDragon> FIRE_DRAGON = register("fire_dragon", EntityType.Builder.<EntityFireDragon>of(EntityFireDragon::new, MobCategory.CREATURE).sized(0.78F, 1.2F).fireImmune().clientTrackingRange(10));
    public static final EntityType<EntityIceDragon> ICE_DRAGON = register("ice_dragon", EntityType.Builder.<EntityIceDragon>of(EntityIceDragon::new, MobCategory.CREATURE).sized(0.78F, 1.2F).clientTrackingRange(10));
    public static final EntityType<EntityLightningDragon> LIGHTNING_DRAGON = register("lightning_dragon", EntityType.Builder.<EntityLightningDragon>of(EntityLightningDragon::new, MobCategory.CREATURE).sized(0.78F, 1.2F).clientTrackingRange(10));
    public static final EntityType<EntityDragonFireCharge> FIRE_DRAGON_CHARGE = register("fire_dragon_charge", EntityType.Builder.<EntityDragonFireCharge>of(EntityDragonFireCharge::new, MobCategory.MISC).sized(0.9F, 0.9F));
    public static final EntityType<EntityDragonIceCharge> ICE_DRAGON_CHARGE = register("ice_dragon_charge", EntityType.Builder.<EntityDragonIceCharge>of(EntityDragonIceCharge::new, MobCategory.MISC).sized(0.9F, 0.9F));
    public static final EntityType<EntityDragonLightningCharge> LIGHTNING_DRAGON_CHARGE = register("lightning_dragon_charge", EntityType.Builder.<EntityDragonLightningCharge>of(EntityDragonLightningCharge::new, MobCategory.MISC).sized(0.9F, 0.9F));
    public static final EntityType<EntityHippogryphEgg> HIPPOGRYPH_EGG = register("hippogryph_egg", EntityType.Builder.<EntityHippogryphEgg>of(EntityHippogryphEgg::new, MobCategory.MISC).sized(0.5F, 0.5F));
    public static final EntityType<EntityHippogryph> HIPPOGRYPH = register("hippogryph", EntityType.Builder.of(EntityHippogryph::new, MobCategory.CREATURE).sized(1.7F, 1.6F));
    public static final EntityType<EntityStoneStatue> STONE_STATUE = register("stone_statue", EntityType.Builder.of(EntityStoneStatue::new, MobCategory.CREATURE).sized(0.5F, 0.5F));
    public static final EntityType<EntityGorgon> GORGON = register("gorgon", EntityType.Builder.of(EntityGorgon::new, MobCategory.CREATURE).sized(0.8F, 1.99F));
    public static final EntityType<EntityPixie> PIXIE = register("pixie", EntityType.Builder.of(EntityPixie::new, MobCategory.CREATURE).sized(0.4F, 0.8F));
    /** A player's exiled pixie (never saved with the land: she is made again by her player). */
    public static final EntityType<EntityExiledPixie> EXILED_PIXIE = register("exiled_pixie", EntityType.Builder.<EntityExiledPixie>of(EntityExiledPixie::new, MobCategory.MISC).sized(0.4F, 0.8F).noSave().noSummon().fireImmune().clientTrackingRange(8));
    public static final EntityType<EntityCyclops> CYCLOPS = register("cyclops", EntityType.Builder.of(EntityCyclops::new, MobCategory.CREATURE).sized(1.95F, 7.4F).clientTrackingRange(8));
    public static final EntityType<EntitySiren> SIREN = register("siren", EntityType.Builder.of(EntitySiren::new, MobCategory.CREATURE).sized(1.6F, 0.9F));
    public static final EntityType<EntityHippocampus> HIPPOCAMPUS = register("hippocampus", EntityType.Builder.of(EntityHippocampus::new, MobCategory.CREATURE).sized(1.95F, 0.95F));
    public static final EntityType<EntityDeathWorm> DEATH_WORM = register("deathworm", EntityType.Builder.of(EntityDeathWorm::new, MobCategory.CREATURE).sized(0.8F, 0.8F));
    public static final EntityType<EntityDeathWormEgg> DEATH_WORM_EGG = register("deathworm_egg", EntityType.Builder.<EntityDeathWormEgg>of(EntityDeathWormEgg::new, MobCategory.MISC).sized(0.5F, 0.5F));
    public static final EntityType<EntityCockatrice> COCKATRICE = register("cockatrice", EntityType.Builder.of(EntityCockatrice::new, MobCategory.CREATURE).sized(1.1F, 1F));
    public static final EntityType<EntityCockatriceEgg> COCKATRICE_EGG = register("cockatrice_egg", EntityType.Builder.<EntityCockatriceEgg>of(EntityCockatriceEgg::new, MobCategory.MISC).sized(0.5F, 0.5F));
    public static final EntityType<EntityStymphalianBird> STYMPHALIAN_BIRD = register("stymphalian_bird", EntityType.Builder.of(EntityStymphalianBird::new, MobCategory.CREATURE).sized(1.3F, 1.2F));
    public static final EntityType<EntityStymphalianFeather> STYMPHALIAN_FEATHER = register("stymphalian_feather", EntityType.Builder.<EntityStymphalianFeather>of(EntityStymphalianFeather::new, MobCategory.MISC).sized(0.5F, 0.5F));
    public static final EntityType<EntityStymphalianArrow> STYMPHALIAN_ARROW = register("stymphalian_arrow", EntityType.Builder.<EntityStymphalianArrow>of(EntityStymphalianArrow::new, MobCategory.MISC).sized(0.5F, 0.5F));
    public static final EntityType<EntityTroll> TROLL = register("troll", EntityType.Builder.of(EntityTroll::new, MobCategory.MONSTER).sized(1.2F, 3.5F));
    public static final EntityType<EntityMyrmexWorker> MYRMEX_WORKER = register("myrmex_worker", EntityType.Builder.of(EntityMyrmexWorker::new, MobCategory.CREATURE).sized(0.9F, 0.9F));
    public static final EntityType<EntityMyrmexSoldier> MYRMEX_SOLDIER = register("myrmex_soldier", EntityType.Builder.of(EntityMyrmexSoldier::new, MobCategory.CREATURE).sized(1.2F, 0.95F));
    public static final EntityType<EntityMyrmexSentinel> MYRMEX_SENTINEL = register("myrmex_sentinel", EntityType.Builder.of(EntityMyrmexSentinel::new, MobCategory.CREATURE).sized(1.3F, 1.95F));
    public static final EntityType<EntityMyrmexRoyal> MYRMEX_ROYAL = register("myrmex_royal", EntityType.Builder.of(EntityMyrmexRoyal::new, MobCategory.CREATURE).sized(1.9F, 1.86F));
    public static final EntityType<EntityMyrmexQueen> MYRMEX_QUEEN = register("myrmex_queen", EntityType.Builder.of(EntityMyrmexQueen::new, MobCategory.CREATURE).sized(2.9F, 1.86F));
    public static final EntityType<EntityMyrmexEgg> MYRMEX_EGG = register("myrmex_egg", EntityType.Builder.of(EntityMyrmexEgg::new, MobCategory.MISC).sized(0.45F, 0.55F));
    public static final EntityType<EntityAmphithere> AMPHITHERE = register("amphithere", EntityType.Builder.of(EntityAmphithere::new, MobCategory.CREATURE).sized(2.5F, 1.25F).clientTrackingRange(8));
    public static final EntityType<EntityAmphithereArrow> AMPHITHERE_ARROW = register("amphithere_arrow", EntityType.Builder.<EntityAmphithereArrow>of(EntityAmphithereArrow::new, MobCategory.MISC).sized(0.5F, 0.5F));
    public static final EntityType<EntitySeaSerpent> SEA_SERPENT = register("sea_serpent", EntityType.Builder.of(EntitySeaSerpent::new, MobCategory.CREATURE).sized(0.5F, 0.5F).clientTrackingRange(8));
    public static final EntityType<EntitySeaSerpentBubbles> SEA_SERPENT_BUBBLES = register("sea_serpent_bubbles", EntityType.Builder.<EntitySeaSerpentBubbles>of(EntitySeaSerpentBubbles::new, MobCategory.MISC).sized(0.9F, 0.9F));
    public static final EntityType<EntitySeaSerpentArrow> SEA_SERPENT_ARROW = register("sea_serpent_arrow", EntityType.Builder.<EntitySeaSerpentArrow>of(EntitySeaSerpentArrow::new, MobCategory.MISC).sized(0.5F, 0.5F));
    public static final EntityType<EntityChainTie> CHAIN_TIE = register("chain_tie", EntityType.Builder.<EntityChainTie>of(EntityChainTie::new, MobCategory.MISC).sized(0.8F, 0.9F));
    public static final EntityType<EntityPixieCharge> PIXIE_CHARGE = register("pixie_charge", EntityType.Builder.<EntityPixieCharge>of(EntityPixieCharge::new, MobCategory.MISC).sized(0.5F, 0.5F));
    public static final EntityType<EntityMyrmexSwarmer> MYRMEX_SWARMER = register("myrmex_swarmer", EntityType.Builder.of(EntityMyrmexSwarmer::new, MobCategory.CREATURE).sized(0.5F, 0.5F));
    public static final EntityType<EntityTideTrident> TIDE_TRIDENT = register("tide_trident", EntityType.Builder.<EntityTideTrident>of(EntityTideTrident::new, MobCategory.MISC).sized(0.85F, 0.5F));
    public static final EntityType<EntityMobSkull> MOB_SKULL = register("mob_skull", EntityType.Builder.of(EntityMobSkull::new, MobCategory.MISC).sized(0.85F, 0.85F));
    public static final EntityType<EntityDreadThrall> DREAD_THRALL = register("dread_thrall", EntityType.Builder.of(EntityDreadThrall::new, MobCategory.MONSTER).sized(0.6F, 1.8F));
    public static final EntityType<EntityDreadGhoul> DREAD_GHOUL = register("dread_ghoul", EntityType.Builder.of(EntityDreadGhoul::new, MobCategory.MONSTER).sized(0.6F, 1.8F));
    public static final EntityType<EntityDreadBeast> DREAD_BEAST = register("dread_beast", EntityType.Builder.of(EntityDreadBeast::new, MobCategory.MONSTER).sized(1.2F, 0.9F));
    public static final EntityType<EntityDreadScuttler> DREAD_SCUTTLER = register("dread_scuttler", EntityType.Builder.of(EntityDreadScuttler::new, MobCategory.MONSTER).sized(1.5F, 1.3F));
    public static final EntityType<EntityDreadLich> DREAD_LICH = register("dread_lich", EntityType.Builder.of(EntityDreadLich::new, MobCategory.MONSTER).sized(0.6F, 1.8F));
    public static final EntityType<EntityDreadLichSkull> DREAD_LICH_SKULL = register("dread_lich_skull", EntityType.Builder.<EntityDreadLichSkull>of(EntityDreadLichSkull::new, MobCategory.MISC).sized(0.5F, 0.5F));
    public static final EntityType<EntityDreadKnight> DREAD_KNIGHT = register("dread_knight", EntityType.Builder.of(EntityDreadKnight::new, MobCategory.MONSTER).sized(0.6F, 1.8F));
    public static final EntityType<EntityDreadQueen> DREAD_QUEEN = register("dread_queen", EntityType.Builder.of(EntityDreadQueen::new, MobCategory.MONSTER).sized(0.7F, 2.1F).fireImmune().clientTrackingRange(10));
    /** Cryona asleep on her throne and Black Frost prowling behind it in her citadel: her sign there (the two themselves wait in her realm). */
    public static final EntityType<EntityCryonaAsleep> CRYONA_ASLEEP = register("cryona_asleep", EntityType.Builder.<EntityCryonaAsleep>of(EntityCryonaAsleep::new, MobCategory.MISC).sized(0.7F, 2.1F).fireImmune().clientTrackingRange(10));
    public static final EntityType<EntityBlackFrostGuard> BLACK_FROST_GUARD = register("black_frost_guard", EntityType.Builder.<EntityBlackFrostGuard>of(EntityBlackFrostGuard::new, MobCategory.MISC).sized(0.78F, 1.2F).fireImmune().clientTrackingRange(10));
    public static final EntityType<EntityDreadHorse> DREAD_HORSE = register("dread_horse", EntityType.Builder.of(EntityDreadHorse::new, MobCategory.MONSTER).sized(1.3964844F, 1.6F));
    public static final EntityType<EntityHydra> HYDRA = register("hydra", EntityType.Builder.of(EntityHydra::new, MobCategory.CREATURE).sized(2.8F, 1.39F));
    public static final EntityType<EntityHydraBreath> HYDRA_BREATH = register("hydra_breath", EntityType.Builder.<EntityHydraBreath>of(EntityHydraBreath::new, MobCategory.MISC).sized(0.9F, 0.9F));
    public static final EntityType<EntityHydraArrow> HYDRA_ARROW = register("hydra_arrow", EntityType.Builder.<EntityHydraArrow>of(EntityHydraArrow::new, MobCategory.MISC).sized(0.5F, 0.5F));
    public static final EntityType<EntityGhost> GHOST = register("ghost", EntityType.Builder.of(EntityGhost::new, MobCategory.MONSTER).sized(0.8F, 1.9F).fireImmune());
    public static final EntityType<EntityGhostSword> GHOST_SWORD = register("ghost_sword", EntityType.Builder.<EntityGhostSword>of(EntityGhostSword::new, MobCategory.MISC).sized(0.5F, 0.5F));

    private IafEntityRegistry() {
    }

    private static <T extends Entity> EntityType<T> register(String name, EntityType.Builder<T> builder) {
        ResourceKey<EntityType<?>> key = ResourceKey.create(Registries.ENTITY_TYPE, IafMod.id(name));
        return Registry.register(BuiltInRegistries.ENTITY_TYPE, key, builder.build(key));
    }

    public static void init() {
        addSpawners();
        FabricDefaultAttributeRegistry.register(DRAGON_EGG, common(EntityDragonEgg.bakeAttributes()));
        FabricDefaultAttributeRegistry.register(DRAGON_SKULL, common(EntityDragonSkull.bakeAttributes()));
        FabricDefaultAttributeRegistry.register(FIRE_DRAGON, common(EntityFireDragon.bakeAttributes()));
        FabricDefaultAttributeRegistry.register(ICE_DRAGON, common(EntityIceDragon.bakeAttributes()));
        FabricDefaultAttributeRegistry.register(LIGHTNING_DRAGON, common(EntityLightningDragon.bakeAttributes()));
        FabricDefaultAttributeRegistry.register(HIPPOGRYPH, common(EntityHippogryph.bakeAttributes()));
        FabricDefaultAttributeRegistry.register(GORGON, common(EntityGorgon.bakeAttributes()));
        FabricDefaultAttributeRegistry.register(STONE_STATUE, common(EntityStoneStatue.bakeAttributes()));
        FabricDefaultAttributeRegistry.register(PIXIE, common(EntityPixie.bakeAttributes()));
        FabricDefaultAttributeRegistry.register(EXILED_PIXIE, common(EntityPixie.bakeAttributes()));
        FabricDefaultAttributeRegistry.register(CYCLOPS, common(EntityCyclops.bakeAttributes()));
        FabricDefaultAttributeRegistry.register(SIREN, common(EntitySiren.bakeAttributes()));
        FabricDefaultAttributeRegistry.register(HIPPOCAMPUS, common(EntityHippocampus.bakeAttributes()));
        FabricDefaultAttributeRegistry.register(DEATH_WORM, common(EntityDeathWorm.bakeAttributes()));
        FabricDefaultAttributeRegistry.register(COCKATRICE, common(EntityCockatrice.bakeAttributes()));
        FabricDefaultAttributeRegistry.register(STYMPHALIAN_BIRD, common(EntityStymphalianBird.bakeAttributes()));
        FabricDefaultAttributeRegistry.register(TROLL, common(EntityTroll.bakeAttributes()));
        FabricDefaultAttributeRegistry.register(MYRMEX_WORKER, common(EntityMyrmexWorker.bakeAttributes()));
        FabricDefaultAttributeRegistry.register(MYRMEX_SOLDIER, common(EntityMyrmexSoldier.bakeAttributes()));
        FabricDefaultAttributeRegistry.register(MYRMEX_SENTINEL, common(EntityMyrmexSentinel.bakeAttributes()));
        FabricDefaultAttributeRegistry.register(MYRMEX_ROYAL, common(EntityMyrmexRoyal.bakeAttributes()));
        FabricDefaultAttributeRegistry.register(MYRMEX_QUEEN, common(EntityMyrmexQueen.bakeAttributes()));
        FabricDefaultAttributeRegistry.register(MYRMEX_EGG, common(EntityMyrmexEgg.bakeAttributes()));
        FabricDefaultAttributeRegistry.register(MYRMEX_SWARMER, common(EntityMyrmexSwarmer.bakeAttributes()));
        FabricDefaultAttributeRegistry.register(AMPHITHERE, common(EntityAmphithere.bakeAttributes()));
        FabricDefaultAttributeRegistry.register(SEA_SERPENT, common(EntitySeaSerpent.bakeAttributes()));
        FabricDefaultAttributeRegistry.register(MOB_SKULL, common(EntityMobSkull.bakeAttributes()));
        FabricDefaultAttributeRegistry.register(DREAD_THRALL, common(EntityDreadThrall.bakeAttributes()));
        FabricDefaultAttributeRegistry.register(DREAD_LICH, common(EntityDreadLich.bakeAttributes()));
        FabricDefaultAttributeRegistry.register(DREAD_BEAST, common(EntityDreadBeast.bakeAttributes()));
        FabricDefaultAttributeRegistry.register(DREAD_HORSE, common(EntityDreadHorse.bakeAttributes()));
        FabricDefaultAttributeRegistry.register(DREAD_GHOUL, common(EntityDreadGhoul.bakeAttributes()));
        FabricDefaultAttributeRegistry.register(DREAD_KNIGHT, common(EntityDreadKnight.bakeAttributes()));
        FabricDefaultAttributeRegistry.register(DREAD_QUEEN, common(EntityDreadQueen.bakeAttributes()));
        FabricDefaultAttributeRegistry.register(CRYONA_ASLEEP, common(EntityCryonaAsleep.bakeAttributes()));
        FabricDefaultAttributeRegistry.register(BLACK_FROST_GUARD, common(EntityIceDragon.bakeAttributes()));
        FabricDefaultAttributeRegistry.register(DREAD_SCUTTLER, common(EntityDreadScuttler.bakeAttributes()));
        FabricDefaultAttributeRegistry.register(HYDRA, common(EntityHydra.bakeAttributes()));
        FabricDefaultAttributeRegistry.register(GHOST, common(EntityGhost.bakeAttributes()));
        SpawnPlacements.register(HIPPOGRYPH, SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, EntityHippogryph::checkMobSpawnRules);
        SpawnPlacements.register(TROLL, SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, EntityTroll::canTrollSpawnOn);
        SpawnPlacements.register(DREAD_LICH, SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, EntityDreadLich::canLichSpawnOn);
        SpawnPlacements.register(COCKATRICE, SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, EntityCockatrice::checkMobSpawnRules);
        SpawnPlacements.register(AMPHITHERE, SpawnPlacementTypes.NO_RESTRICTIONS, Heightmap.Types.MOTION_BLOCKING, EntityAmphithere::canAmphithereSpawnOn);
    }

    /** Natural spawns, chosen by the biome config. */
    public static void addSpawners() {
        if (IafConfig.spawnHippogryphs) {
            BiomeModifications.addSpawn(ctx -> BiomeConfig.test(BiomeConfig.hippogryphBiomes, ctx.getBiomeHolder()), MobCategory.CREATURE, HIPPOGRYPH, IafConfig.hippogryphSpawnRate, 1, 1);
        }
        if (IafConfig.spawnLiches) {
            BiomeModifications.addSpawn(ctx -> BiomeConfig.test(BiomeConfig.mausoleumBiomes, ctx.getBiomeHolder()), MobCategory.MONSTER, DREAD_LICH, IafConfig.lichSpawnRate, 1, 1);
        }
        if (IafConfig.spawnCockatrices) {
            BiomeModifications.addSpawn(ctx -> BiomeConfig.test(BiomeConfig.cockatriceBiomes, ctx.getBiomeHolder()), MobCategory.CREATURE, COCKATRICE, IafConfig.cockatriceSpawnRate, 1, 2);
        }
        if (IafConfig.spawnAmphitheres) {
            BiomeModifications.addSpawn(ctx -> BiomeConfig.test(BiomeConfig.amphithereBiomes, ctx.getBiomeHolder()), MobCategory.CREATURE, AMPHITHERE, IafConfig.amphithereSpawnRate, 1, 3);
        }
        if (IafConfig.spawnTrolls) {
            BiomeModifications.addSpawn(ctx -> BiomeConfig.test(BiomeConfig.forestTrollBiomes, ctx.getBiomeHolder())
                    || BiomeConfig.test(BiomeConfig.snowyTrollBiomes, ctx.getBiomeHolder())
                    || BiomeConfig.test(BiomeConfig.mountainTrollBiomes, ctx.getBiomeHolder()),
                MobCategory.MONSTER, TROLL, IafConfig.trollSpawnRate, 1, 3);
        }
    }

    /** Attributes every mob of the mod has besides its own (TemptGoal now reads a tempt range). */
    private static AttributeSupplier.Builder common(AttributeSupplier.Builder builder) {
        return builder.add(Attributes.TEMPT_RANGE, 10.0D);
    }
}

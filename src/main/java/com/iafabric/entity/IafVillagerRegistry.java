package com.iafabric.entity;

import com.google.common.collect.ImmutableSet;
import com.iafabric.IafMod;
import com.iafabric.block.IafBlockRegistry;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import net.fabricmc.fabric.api.object.builder.v1.world.poi.PoiHelper;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.ai.village.poi.PoiType;
import net.minecraft.world.entity.npc.villager.VillagerProfession;
import net.minecraft.world.item.trading.TradeSet;

/**
 * The scribe: a villager who works at the mod's lectern. Its trades are data
 * (data/iceandfire_fabric/trade_set/scribe/level_N.json and data/iceandfire_fabric/villager_trade/scribe/*.json).
 */
public final class IafVillagerRegistry {
    public static final ResourceKey<PoiType> SCRIBE_POI_KEY = ResourceKey.create(Registries.POINT_OF_INTEREST_TYPE, IafMod.id("scribe"));
    public static final PoiType SCRIBE_POI = PoiHelper.register(IafMod.id("scribe"), 1, 1, IafBlockRegistry.LECTERN);
    public static final ResourceKey<VillagerProfession> SCRIBE_KEY = ResourceKey.create(Registries.VILLAGER_PROFESSION, IafMod.id("scribe"));
    public static final VillagerProfession SCRIBE = Registry.register(BuiltInRegistries.VILLAGER_PROFESSION, SCRIBE_KEY,
        new VillagerProfession(Component.translatable("entity.minecraft.villager.iceandfire_fabric.scribe"), poi -> poi.is(SCRIBE_POI_KEY), poi -> poi.is(SCRIBE_POI_KEY),
            ImmutableSet.of(), ImmutableSet.of(), SoundEvents.VILLAGER_WORK_LIBRARIAN, tradeSets()));

    private IafVillagerRegistry() {
    }

    private static Int2ObjectMap<ResourceKey<TradeSet>> tradeSets() {
        Int2ObjectMap<ResourceKey<TradeSet>> map = new Int2ObjectOpenHashMap<>();
        for (int level = 1; level <= 5; level++) {
            map.put(level, ResourceKey.create(Registries.TRADE_SET, IafMod.id("scribe/level_" + level)));
        }
        return map;
    }

    /**
     * What a scribe always sells besides its trades drawn by chance: at novice the Frost Ward, at apprentice the map to
     * Cryona's citadel, at journeyman the long Frost Ward for ice dragon blood (data trade_set/scribe/sure_level_N).
     */
    public static void addSureTrades(net.minecraft.world.entity.npc.villager.Villager villager, net.minecraft.server.level.ServerLevel level) {
        var data = villager.getVillagerData();
        if (!data.profession().is(SCRIBE_KEY) || data.level() < 1 || data.level() > 3) {
            return;
        }
        villager.addOffersFromTradeSet(level, villager.getOffers(), ResourceKey.create(Registries.TRADE_SET, IafMod.id("scribe/sure_level_" + data.level())));
    }

    public static void init() {
    }
}

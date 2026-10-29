package com.iafabric.entity;

import com.google.common.collect.ImmutableSet;
import com.iafabric.IafMod;
import com.iafabric.block.IafBlockRegistry;
import com.iafabric.effect.IafEffects;
import net.fabricmc.fabric.api.object.builder.v1.trade.TradeOfferHelper;
import net.fabricmc.fabric.api.object.builder.v1.world.poi.PointOfInterestHelper;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.ai.village.poi.PoiType;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.minecraft.world.entity.npc.VillagerTrades;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.level.levelgen.structure.Structure;

import java.util.Optional;

/**
 * The scribe: a villager who works at the mod's lectern. Each level offers two trades drawn from its list, and the
 * first three levels also a sure one (addSureTrades, from VillagerScribeMixin).
 */
public final class IafVillagerRegistry {
	public static final ResourceKey<PoiType> SCRIBE_POI_KEY = ResourceKey.create(Registries.POINT_OF_INTEREST_TYPE, IafMod.id("scribe"));
	public static final PoiType SCRIBE_POI = PointOfInterestHelper.register(IafMod.id("scribe"), 1, 1, IafBlockRegistry.LECTERN);
	public static final ResourceKey<VillagerProfession> SCRIBE_KEY = ResourceKey.create(Registries.VILLAGER_PROFESSION, IafMod.id("scribe"));
	public static final VillagerProfession SCRIBE = Registry.register(BuiltInRegistries.VILLAGER_PROFESSION, SCRIBE_KEY,
		new VillagerProfession("scribe", poi -> poi.is(SCRIBE_POI_KEY), poi -> poi.is(SCRIBE_POI_KEY),
			ImmutableSet.of(), ImmutableSet.of(), SoundEvents.VILLAGER_WORK_LIBRARIAN));
	private static final TagKey<Structure> ON_THRONE_MAPS = TagKey.create(Registries.STRUCTURE, IafMod.id("on_throne_maps"));

	private IafVillagerRegistry() {
	}

	private static Item item(String name) {
		return BuiltInRegistries.ITEM.get(IafMod.id(name));
	}

	/** wants (count) for gives (count), as the scribe's data trades of the 26.x version. */
	private static VillagerTrades.ItemListing trade(Item wants, int wantsCount, Item gives, int givesCount, int maxUses, int xp, float discount) {
		return (trader, random) -> new MerchantOffer(new ItemStack(wants, wantsCount), new ItemStack(gives, givesCount), maxUses, xp, discount);
	}

	private static VillagerTrades.ItemListing trade(String wants, int wantsCount, Item gives, int givesCount, int maxUses, int xp, float discount) {
		return (trader, random) -> new MerchantOffer(new ItemStack(item(wants), wantsCount), new ItemStack(gives, givesCount), maxUses, xp, discount);
	}

	private static VillagerTrades.ItemListing trade(Item wants, int wantsCount, String gives, int givesCount, int maxUses, int xp, float discount) {
		return (trader, random) -> new MerchantOffer(new ItemStack(wants, wantsCount), new ItemStack(item(gives), givesCount), maxUses, xp, discount);
	}

	private static ItemStack potion(Potion potion) {
		return net.minecraft.world.item.alchemy.PotionUtils.setPotion(new ItemStack(Items.POTION), potion);
	}

	/**
	 * What a scribe always sells besides its trades drawn by chance: at novice the Frost Ward, at apprentice the map to
	 * Cryona's citadel, at journeyman the long Frost Ward for ice dragon blood.
	 */
	public static void addSureTrades(Villager villager) {
		var data = villager.getVillagerData();
		if (data.getProfession() != SCRIBE) {
			return;
		}
		switch (data.getLevel()) {
			case 1 -> villager.getOffers().add(new MerchantOffer(new ItemStack(Items.EMERALD, 2), potion(IafEffects.FROST_WARD_POTION), 16, 2, 0.05F));
			case 2 -> {
				MerchantOffer map = new VillagerTrades.TreasureMapForEmeralds(16, ON_THRONE_MAPS, "filled_map.iceandfire_fabric.throne",
					net.minecraft.world.level.saveddata.maps.MapDecoration.Type.RED_X, 3, 10).getOffer(villager, villager.getRandom());
				if (map != null) {
					villager.getOffers().add(map);
				}
			}
			case 3 -> villager.getOffers().add(new MerchantOffer(new ItemStack(Items.EMERALD, 3), new ItemStack(item("ice_dragon_blood")),
				potion(IafEffects.LONG_FROST_WARD_POTION), 8, 8, 0.05F));
			default -> {
			}
		}
	}

	public static void init() {
		TradeOfferHelper.registerVillagerOffers(SCRIBE, 1, factories -> {
			factories.add(trade("ash", 10, Items.EMERALD, 1, 8, 4, 0.05F));
			factories.add(trade(Items.BOOKSHELF, 3, Items.EMERALD, 1, 8, 3, 0.05F));
			factories.add(trade(Items.EMERALD, 1, "manuscript", 4, 25, 2, 0.05F));
			factories.add(trade(Items.PAPER, 15, Items.EMERALD, 2, 4, 4, 0.05F));
		});
		TradeOfferHelper.registerVillagerOffers(SCRIBE, 2, factories -> {
			factories.add(trade("amythest_gem", 1, Items.EMERALD, 3, 20, 3, 0.2F));
			factories.add(trade(Items.EMERALD, 2, "dragon_ice_spikes", 7, 2, 3, 0.05F));
			factories.add(trade(Items.EMERALD, 3, "frost_lily", 4, 3, 3, 0.05F));
			factories.add(trade(Items.EMERALD, 2, "myrmex_desert_resin", 1, 40, 2, 0.05F));
			factories.add(trade(Items.EMERALD, 2, "myrmex_jungle_resin", 1, 40, 2, 0.05F));
			factories.add(trade(Items.EMERALD, 2, "pixie_jar_empty", 1, 3, 4, 0.05F));
			factories.add(trade("fire_lily", 8, Items.EMERALD, 1, 3, 5, 0.05F));
			factories.add(trade("lightning_lily", 7, Items.EMERALD, 3, 2, 5, 0.05F));
			factories.add(trade("sapphire_gem", 1, Items.EMERALD, 2, 30, 3, 0.2F));
			factories.add(trade("silver_ingot", 5, Items.EMERALD, 1, 3, 5, 0.05F));
		});
		TradeOfferHelper.registerVillagerOffers(SCRIBE, 3, factories -> {
			factories.add(trade("chain", 2, Items.EMERALD, 3, 4, 2, 0.05F));
			factories.add(trade("dragonbone", 6, Items.EMERALD, 1, 7, 4, 0.05F));
			factories.add(trade(Items.EMERALD, 6, "fire_dragon_flesh", 2, 8, 3, 0.05F));
			factories.add(trade(Items.EMERALD, 7, "ice_dragon_flesh", 1, 8, 3, 0.05F));
			factories.add(trade(Items.EMERALD, 8, "lightning_dragon_flesh", 1, 8, 3, 0.05F));
			factories.add(trade(Items.EMERALD, 6, "pixie_dust", 2, 8, 3, 0.05F));
		});
		TradeOfferHelper.registerVillagerOffers(SCRIBE, 4, factories -> {
			factories.add(trade("dread_shard", 5, Items.EMERALD, 1, 10, 4, 0.05F));
			factories.add(trade(Items.EMERALD, 10, "dragonbone", 2, 20, 5, 0.05F));
			factories.add(trade(Items.EMERALD, 4, "shiny_scales", 1, 5, 2, 0.05F));
			factories.add(trade(Items.EMERALD, 8, "stymphalian_bird_feather", 12, 3, 6, 0.05F));
			factories.add(trade(Items.EMERALD, 4, "troll_tusk", 12, 7, 3, 0.05F));
		});
		TradeOfferHelper.registerVillagerOffers(SCRIBE, 5, factories -> {
			factories.add(trade("ectoplasm", 6, Items.EMERALD, 1, 7, 3, 0.05F));
			factories.add(trade(Items.EMERALD, 12, "hydra_fang", 1, 20, 3, 0.05F));
			factories.add(trade(Items.EMERALD, 15, "sea_serpent_fang", 3, 20, 3, 0.05F));
		});
	}
}

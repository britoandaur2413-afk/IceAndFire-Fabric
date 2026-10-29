package com.iafabric.client.render.item;

import com.iafabric.IafMod;
import com.iafabric.item.IafItemRegistry;
import com.iafabric.item.ItemDragonHorn;
import com.iafabric.item.ItemSummoningCrystal;
import net.fabricmc.fabric.api.client.model.loading.v1.ModelLoadingPlugin;
import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;

import java.util.List;

/** The item model properties the mod's item models use in their overrides (assets/iceandfire_fabric/models/item). */
public final class IafItemProperties {
	/** The tide trident as a flat picture (in the inventory, on the ground, in a frame): TideTridentModelMixin. */
	public static final ResourceLocation TIDE_TRIDENT_INVENTORY = IafMod.id("item/tide_trident_inventory");

	private IafItemProperties() {
	}

	public static void init() {
		// the kind of dragon in a dragon horn: 0 none, 0.25 fire, 0.5 ice, 0.75 lightning
		ItemProperties.register(IafItemRegistry.DRAGON_HORN, IafMod.id("horn_dragon"), (stack, level, owner, seed) -> ItemDragonHorn.getDragonType(stack) * 0.25F);
		// whether a summoning crystal holds a dragon
		for (Item crystal : List.of(IafItemRegistry.SUMMONING_CRYSTAL_FIRE, IafItemRegistry.SUMMONING_CRYSTAL_ICE, IafItemRegistry.SUMMONING_CRYSTAL_LIGHTNING)) {
			ItemProperties.register(crystal, IafMod.id("has_dragon"), (stack, level, owner, seed) -> ItemSummoningCrystal.hasDragon(stack) ? 1.0F : 0.0F);
		}
		// the dragonbone bow drawn as a bow is (the game gives these only to its own bow)
		ItemProperties.register(IafItemRegistry.DRAGON_BOW, new ResourceLocation("pull"), (stack, level, owner, seed) ->
			owner == null || owner.getUseItem() != stack ? 0.0F : (stack.getUseDuration() - owner.getUseItemRemainingTicks()) / 20.0F);
		ItemProperties.register(IafItemRegistry.DRAGON_BOW, new ResourceLocation("pulling"), (stack, level, owner, seed) ->
			owner != null && owner.isUsingItem() && owner.getUseItem() == stack ? 1.0F : 0.0F);
		ModelLoadingPlugin.register(context -> context.addModels(TIDE_TRIDENT_INVENTORY));
	}
}

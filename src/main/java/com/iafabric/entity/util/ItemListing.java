package com.iafabric.entity.util;

import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.trading.MerchantOffer;
import org.jetbrains.annotations.Nullable;

/** One possible trade of the mod's merchants (vanilla villagers' trades are data now). */
@FunctionalInterface
public interface ItemListing {
	@Nullable MerchantOffer getOffer(Entity trader, RandomSource rand);

	/** What the merchant wants, as the old code wrote it: an item stack with a count. */
	static ItemStack cost(ItemStack stack) {
		return new ItemStack(stack.getItem(), stack.getCount());
	}
}

package com.iafabric.compat;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.block.Block;

/** A colored block by its color (26.x's color collections, as Blocks.DYED_CANDLE.pick(color)). */
public final class Colors {
	private Colors() {
	}

	public static Block block(DyeColor color, String kind) {
		return BuiltInRegistries.BLOCK.get(ResourceLocation.withDefaultNamespace(color.getName() + "_" + kind));
	}

	public static Block candle(DyeColor color) {
		return block(color, "candle");
	}
}

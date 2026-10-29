package com.iafabric.test;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.block.Block;

/** What the tests share where 1.21.1 differs from 26.x. */
final class IafClick {
	private IafClick() {
	}

	/** A right click on it, as the server takes one: interactAt first, then interact if that did nothing. */
	static InteractionResult click(Entity target, Player player) {
		InteractionResult result = target.interactAt(player, target.position(), InteractionHand.MAIN_HAND);
		return result.consumesAction() ? result : player.interactOn(target, InteractionHand.MAIN_HAND);
	}

	/** The wool of this color (26.x's Blocks.WOOL.pick). */
	static Block wool(DyeColor color) {
		return BuiltInRegistries.BLOCK.get(ResourceLocation.withDefaultNamespace(color.getName() + "_wool"));
	}
}

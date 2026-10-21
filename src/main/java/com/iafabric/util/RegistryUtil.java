package com.iafabric.util;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

/** The registry name of a game object (what the old getRegistryName() returned). */
public final class RegistryUtil {
	private RegistryUtil() {
	}

	public static Identifier getName(Object o) {
		if (o instanceof Block block) {
			return BuiltInRegistries.BLOCK.getKey(block);
		}
		if (o instanceof BlockState state) {
			return BuiltInRegistries.BLOCK.getKey(state.getBlock());
		}
		if (o instanceof Item item) {
			return BuiltInRegistries.ITEM.getKey(item);
		}
		if (o instanceof ItemStack stack) {
			return BuiltInRegistries.ITEM.getKey(stack.getItem());
		}
		if (o instanceof EntityType<?> type) {
			return BuiltInRegistries.ENTITY_TYPE.getKey(type);
		}
		if (o instanceof Entity entity) {
			return BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType());
		}
		if (o instanceof BlockEntityType<?> type) {
			return BuiltInRegistries.BLOCK_ENTITY_TYPE.getKey(type);
		}
		throw new IllegalArgumentException("No registry name for " + o);
	}
}

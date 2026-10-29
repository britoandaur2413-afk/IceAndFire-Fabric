package com.iafabric.compat;

import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/** Every stack of a player's inventory (26.x's Inventory is iterable). */
public final class Inv {
	private Inv() {
	}

	public static List<ItemStack> all(Inventory inventory) {
		List<ItemStack> out = new ArrayList<>(inventory.getContainerSize());
		for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
			out.add(inventory.getItem(slot));
		}
		return out;
	}
}

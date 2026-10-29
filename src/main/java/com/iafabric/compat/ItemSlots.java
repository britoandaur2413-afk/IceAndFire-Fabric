package com.iafabric.compat;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

/** The equipment slot an item ticking in an inventory is in (26.x passes it to inventoryTick; 1.21.1 does not). */
public final class ItemSlots {
	private ItemSlots() {
	}

	public static @Nullable EquipmentSlot slotOf(Entity owner, ItemStack stack, boolean selected) {
		if (owner instanceof LivingEntity living) {
			if (selected && living.getMainHandItem() == stack) {
				return EquipmentSlot.MAINHAND;
			}
			for (EquipmentSlot slot : EquipmentSlot.values()) {
				if (slot != EquipmentSlot.MAINHAND && living.getItemBySlot(slot) == stack) {
					return slot;
				}
			}
		}
		return null;
	}
}

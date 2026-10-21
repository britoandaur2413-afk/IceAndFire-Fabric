package com.iafabric.util;

import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import org.jspecify.annotations.Nullable;

import java.util.function.Consumer;

/**
 * The free-form data the old items kept in their NBT tag, now in the custom_data component. Reads
 * return copies: write with {@link #update} or {@link #setTag}.
 */
public final class ItemNbt {
	private ItemNbt() {
	}

	public static boolean hasTag(ItemStack stack) {
		CustomData data = stack.get(DataComponents.CUSTOM_DATA);
		return data != null && !data.isEmpty();
	}

	/** A copy of the data, or null when the stack has none. */
	public static CompoundTag getTag(ItemStack stack) {
		CustomData data = stack.get(DataComponents.CUSTOM_DATA);
		return data == null ? null : data.copyTag();
	}

	/** A copy of the data, empty when the stack has none. */
	public static CompoundTag getOrEmpty(ItemStack stack) {
		CustomData data = stack.get(DataComponents.CUSTOM_DATA);
		return data == null ? new CompoundTag() : data.copyTag();
	}

	public static void setTag(ItemStack stack, @Nullable CompoundTag tag) {
		if (tag == null) {
			stack.remove(DataComponents.CUSTOM_DATA);
		} else {
			CustomData.set(DataComponents.CUSTOM_DATA, stack, tag);
		}
	}

	public static void update(ItemStack stack, Consumer<CompoundTag> change) {
		CustomData.update(DataComponents.CUSTOM_DATA, stack, change);
	}
}

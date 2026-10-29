package com.iafabric.util;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.function.Consumer;

/**
 * The free-form data the old items kept in their NBT tag (1.20.1 still has it; 1.21's custom_data component). Reads
 * return copies: write with {@link #update} or {@link #setTag}.
 */
public final class ItemNbt {
	private ItemNbt() {
	}

	public static boolean hasTag(ItemStack stack) {
		CompoundTag tag = stack.getTag();
		return tag != null && !tag.isEmpty();
	}

	/** A copy of the data, or null when the stack has none. */
	public static CompoundTag getTag(ItemStack stack) {
		CompoundTag tag = stack.getTag();
		return tag == null ? null : tag.copy();
	}

	/** A copy of the data, empty when the stack has none. */
	public static CompoundTag getOrEmpty(ItemStack stack) {
		CompoundTag tag = stack.getTag();
		return tag == null ? new CompoundTag() : tag.copy();
	}

	public static void setTag(ItemStack stack, @Nullable CompoundTag tag) {
		stack.setTag(tag == null || tag.isEmpty() ? null : tag.copy());
	}

	public static void update(ItemStack stack, Consumer<CompoundTag> change) {
		CompoundTag tag = getOrEmpty(stack);
		change.accept(tag);
		setTag(stack, tag);
	}
}

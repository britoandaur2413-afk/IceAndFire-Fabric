package com.iafabric.compat;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DynamicOps;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.RegistryOps;
import org.jetbrains.annotations.Nullable;

/**
 * The part of 26.x's ValueOutput the mod's saves use, written into a 1.21.1 CompoundTag: the save code
 * stays the same in every version of the port.
 */
public final class ValueOutput {
	private final CompoundTag tag;
	private final @Nullable HolderLookup.Provider registries;

	public ValueOutput(CompoundTag tag) {
		this(tag, null);
	}

	public ValueOutput(CompoundTag tag, @Nullable HolderLookup.Provider registries) {
		this.tag = tag;
		this.registries = registries;
	}

	public CompoundTag tag() {
		return tag;
	}

	public @Nullable HolderLookup.Provider registries() {
		return registries;
	}

	DynamicOps<Tag> ops() {
		return registries != null ? RegistryOps.create(NbtOps.INSTANCE, registries) : NbtOps.INSTANCE;
	}

	public void putInt(String key, int value) {
		tag.putInt(key, value);
	}

	public void putBoolean(String key, boolean value) {
		tag.putBoolean(key, value);
	}

	public void putFloat(String key, float value) {
		tag.putFloat(key, value);
	}

	public void putLong(String key, long value) {
		tag.putLong(key, value);
	}

	public void putDouble(String key, double value) {
		tag.putDouble(key, value);
	}

	public void putString(String key, String value) {
		tag.putString(key, value);
	}

	public void putByte(String key, byte value) {
		tag.putByte(key, value);
	}

	public void putShort(String key, short value) {
		tag.putShort(key, value);
	}

	public void putIntArray(String key, int[] value) {
		tag.putIntArray(key, value);
	}

	public void put(String key, Tag value) {
		tag.put(key, value);
	}

	public <T> void store(String key, Codec<T> codec, T value) {
		codec.encodeStart(ops(), value).result().ifPresent(encoded -> tag.put(key, encoded));
	}

	public <T> void storeNullable(String key, Codec<T> codec, @Nullable T value) {
		if (value != null) {
			store(key, codec, value);
		}
	}

	public ValueOutput child(String key) {
		CompoundTag child = new CompoundTag();
		tag.put(key, child);
		return new ValueOutput(child);
	}

	public void discard(String key) {
		tag.remove(key);
	}
}

package com.iafabric.compat;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DynamicOps;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.RegistryOps;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * The part of 26.x's ValueInput the mod's saves use, read from a 1.21.1 CompoundTag: missing keys give the
 * default, as in 26.x.
 */
public final class ValueInput {
	private final CompoundTag tag;
	private final @Nullable HolderLookup.Provider registries;

	public ValueInput(CompoundTag tag) {
		this(tag, null);
	}

	public ValueInput(CompoundTag tag, @Nullable HolderLookup.Provider registries) {
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

	public boolean contains(String key) {
		return tag.contains(key);
	}

	public int getIntOr(String key, int fallback) {
		return tag.contains(key, Tag.TAG_ANY_NUMERIC) ? tag.getInt(key) : fallback;
	}

	public boolean getBooleanOr(String key, boolean fallback) {
		return tag.contains(key, Tag.TAG_ANY_NUMERIC) ? tag.getBoolean(key) : fallback;
	}

	public float getFloatOr(String key, float fallback) {
		return tag.contains(key, Tag.TAG_ANY_NUMERIC) ? tag.getFloat(key) : fallback;
	}

	public long getLongOr(String key, long fallback) {
		return tag.contains(key, Tag.TAG_ANY_NUMERIC) ? tag.getLong(key) : fallback;
	}

	public double getDoubleOr(String key, double fallback) {
		return tag.contains(key, Tag.TAG_ANY_NUMERIC) ? tag.getDouble(key) : fallback;
	}

	public byte getByteOr(String key, byte fallback) {
		return tag.contains(key, Tag.TAG_ANY_NUMERIC) ? tag.getByte(key) : fallback;
	}

	public short getShortOr(String key, short fallback) {
		return tag.contains(key, Tag.TAG_ANY_NUMERIC) ? tag.getShort(key) : fallback;
	}

	public String getStringOr(String key, String fallback) {
		return tag.contains(key, Tag.TAG_STRING) ? tag.getString(key) : fallback;
	}

	public Optional<String> getString(String key) {
		return tag.contains(key, Tag.TAG_STRING) ? Optional.of(tag.getString(key)) : Optional.empty();
	}

	public Optional<Integer> getInt(String key) {
		return tag.contains(key, Tag.TAG_ANY_NUMERIC) ? Optional.of(tag.getInt(key)) : Optional.empty();
	}

	public <T> Optional<T> read(String key, Codec<T> codec) {
		Tag value = tag.get(key);
		return value == null ? Optional.empty() : codec.parse(ops(), value).result();
	}

	public <T> List<T> listOrEmpty(String key, Codec<T> codec) {
		List<T> out = new ArrayList<>();
		if (tag.get(key) instanceof ListTag list) {
			for (Tag element : list) {
				codec.parse(ops(), element).result().ifPresent(out::add);
			}
		}
		return out;
	}

	public Optional<ValueInput> child(String key) {
		return tag.contains(key, Tag.TAG_COMPOUND) ? Optional.of(new ValueInput(tag.getCompound(key), registries)) : Optional.empty();
	}

	public ValueInput childOrEmpty(String key) {
		return new ValueInput(tag.getCompound(key), registries);
	}
}

package com.iafabric.compat;

import com.mojang.serialization.Codec;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

/**
 * 26.x's CompoundTag getters with a fallback (and its codec read/store), added to 1.21.1's CompoundTag by
 * interface injection (fabric.mod.json, CompoundTagCompatMixin), so the mod's code reads the same in both.
 */
public interface CompoundTagCompat {
	private CompoundTag self() {
		return (CompoundTag) (Object) this;
	}

	default int iceandfire_fabric$getIntOr(String key, int fallback) {
		return self().contains(key, Tag.TAG_ANY_NUMERIC) ? self().getInt(key) : fallback;
	}

	default boolean iceandfire_fabric$getBooleanOr(String key, boolean fallback) {
		return self().contains(key, Tag.TAG_ANY_NUMERIC) ? self().getBoolean(key) : fallback;
	}

	default float iceandfire_fabric$getFloatOr(String key, float fallback) {
		return self().contains(key, Tag.TAG_ANY_NUMERIC) ? self().getFloat(key) : fallback;
	}

	default long iceandfire_fabric$getLongOr(String key, long fallback) {
		return self().contains(key, Tag.TAG_ANY_NUMERIC) ? self().getLong(key) : fallback;
	}

	default double iceandfire_fabric$getDoubleOr(String key, double fallback) {
		return self().contains(key, Tag.TAG_ANY_NUMERIC) ? self().getDouble(key) : fallback;
	}

	default byte iceandfire_fabric$getByteOr(String key, byte fallback) {
		return self().contains(key, Tag.TAG_ANY_NUMERIC) ? self().getByte(key) : fallback;
	}

	default short iceandfire_fabric$getShortOr(String key, short fallback) {
		return self().contains(key, Tag.TAG_ANY_NUMERIC) ? self().getShort(key) : fallback;
	}

	default String iceandfire_fabric$getStringOr(String key, String fallback) {
		return self().contains(key, Tag.TAG_STRING) ? self().getString(key) : fallback;
	}

	default ListTag iceandfire_fabric$getListOrEmpty(String key) {
		return self().get(key) instanceof ListTag list ? list : new ListTag();
	}

	default <T> Optional<T> iceandfire_fabric$read(String key, Codec<T> codec) {
		Tag value = self().get(key);
		return value == null ? Optional.empty() : codec.parse(NbtOps.INSTANCE, value).result();
	}

	default <T> void iceandfire_fabric$store(String key, Codec<T> codec, T value) {
		codec.encodeStart(NbtOps.INSTANCE, value).result().ifPresent(encoded -> self().put(key, encoded));
	}

	default <T> void iceandfire_fabric$storeNullable(String key, Codec<T> codec, @Nullable T value) {
		if (value != null) {
			iceandfire_fabric$store(key, codec, value);
		}
	}
}

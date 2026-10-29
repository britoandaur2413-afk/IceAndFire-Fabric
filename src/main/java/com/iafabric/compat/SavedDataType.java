package com.iafabric.compat;

import com.mojang.serialization.Codec;
import net.minecraft.nbt.NbtOps;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.saveddata.SavedData;
import org.jetbrains.annotations.Nullable;

import java.util.function.Supplier;

/**
 * 26.x's description of a kind of saved data (id, new data, codec), as 1.20.1 wants it:
 * storage.computeIfAbsent(TYPE.load(), TYPE.constructor(), TYPE.name()). The data itself saves through its save(CompoundTag).
 */
public record SavedDataType<T extends SavedData>(ResourceLocation id, Supplier<T> constructor, Codec<T> codec, @Nullable DataFixTypes fixes) {
	/** The file name under data/ (a namespace and path, no ':' in a file name). */
	public String name() {
		return id.getNamespace() + "_" + id.getPath().replace('/', '_');
	}

	/** Reads the data back from its file (new data if it cannot be read). */
	public java.util.function.Function<net.minecraft.nbt.CompoundTag, T> load() {
		return tag -> codec.parse(NbtOps.INSTANCE, tag).result().orElseGet(constructor);
	}
}

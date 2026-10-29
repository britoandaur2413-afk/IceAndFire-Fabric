package com.iafabric.compat;

import com.mojang.serialization.Codec;
import net.minecraft.nbt.NbtOps;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.saveddata.SavedData;
import org.jetbrains.annotations.Nullable;

import java.util.function.Supplier;

/**
 * 26.x's description of a kind of saved data (id, new data, codec), as a 1.21.1 SavedData.Factory and file name:
 * storage.computeIfAbsent(TYPE.factory(), TYPE.name()). The data itself saves through its save(CompoundTag).
 */
public record SavedDataType<T extends SavedData>(ResourceLocation id, Supplier<T> constructor, Codec<T> codec, @Nullable DataFixTypes fixes) {
	/** The file name under data/ (a namespace and path, no ':' in a file name). */
	public String name() {
		return id.getNamespace() + "_" + id.getPath().replace('/', '_');
	}

	public SavedData.Factory<T> factory() {
		return new SavedData.Factory<>(constructor, (tag, registries) -> codec.parse(NbtOps.INSTANCE, tag).result().orElseGet(constructor), null);
	}
}

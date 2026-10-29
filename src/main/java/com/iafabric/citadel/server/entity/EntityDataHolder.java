package com.iafabric.citadel.server.entity;

import net.minecraft.nbt.CompoundTag;

/** Implemented on every entity by {@link EntityMixin}: extra data the mod keeps on any entity. */
public interface EntityDataHolder {
	CompoundTag iaf$getData();

	void iaf$setData(CompoundTag tag);
}

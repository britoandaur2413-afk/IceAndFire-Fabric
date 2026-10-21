package com.iafabric.citadel.server.entity;

import com.iafabric.citadel.server.entity.EntityDataHolder;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Entity;

/** Extra NBT stored on any entity (replaces Citadel's entity data). Saved with the entity. */
public final class CitadelEntityData {
	private CitadelEntityData() {
	}

	public static CompoundTag getCitadelTag(Entity entity) {
		return ((EntityDataHolder) entity).iaf$getData();
	}

	public static CompoundTag getOrCreateCitadelTag(Entity entity) {
		return getCitadelTag(entity);
	}

	public static void setCitadelTag(Entity entity, CompoundTag tag) {
		((EntityDataHolder) entity).iaf$setData(tag);
	}
}

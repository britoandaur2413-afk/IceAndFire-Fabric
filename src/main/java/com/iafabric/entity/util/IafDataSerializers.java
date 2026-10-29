package com.iafabric.entity.util;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataSerializer;
import net.minecraft.network.syncher.EntityDataSerializers;

import java.util.Optional;
import java.util.UUID;

/** Synced entity data types later versions dropped; 1.20.1 still has them, so these are vanilla's own. */
public final class IafDataSerializers {
	public static final EntityDataSerializer<Optional<UUID>> OPTIONAL_UUID = EntityDataSerializers.OPTIONAL_UUID;

	public static final EntityDataSerializer<CompoundTag> COMPOUND_TAG = EntityDataSerializers.COMPOUND_TAG;

	private IafDataSerializers() {
	}

	public static void init() {
	}
}

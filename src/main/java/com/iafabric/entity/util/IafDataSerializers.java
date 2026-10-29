package com.iafabric.entity.util;

import com.iafabric.IafMod;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.core.UUIDUtil;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.syncher.EntityDataSerializer;

import java.util.Optional;
import java.util.UUID;

/** Synced entity data types vanilla no longer has. */
public final class IafDataSerializers {
	public static final EntityDataSerializer<Optional<UUID>> OPTIONAL_UUID =
		EntityDataSerializer.forValueType(ByteBufCodecs.optional(UUIDUtil.STREAM_CODEC));

	public static final EntityDataSerializer<CompoundTag> COMPOUND_TAG = EntityDataSerializer.forValueType(ByteBufCodecs.COMPOUND_TAG);

	private IafDataSerializers() {
	}

	public static void init() {
		EntityDataSerializers.registerSerializer(OPTIONAL_UUID);
		EntityDataSerializers.registerSerializer(COMPOUND_TAG);
	}
}

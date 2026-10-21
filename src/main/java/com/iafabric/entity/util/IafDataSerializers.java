package com.iafabric.entity.util;

import com.iafabric.IafMod;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricEntityDataRegistry;
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
		FabricEntityDataRegistry.register(IafMod.id("optional_uuid"), OPTIONAL_UUID);
		FabricEntityDataRegistry.register(IafMod.id("compound_tag"), COMPOUND_TAG);
	}
}

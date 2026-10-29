package com.iafabric.compat;

import net.minecraft.core.Holder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * An entity of a level by its UUID, on the server and on the client (26.x's Level.getEntity(UUID)); an attribute's
 * name as 26.x has it (1.21.1 names most of them generic.*, player.*).
 */
public final class Entities {
	private Entities() {
	}

	public static @Nullable Entity byUuid(Level level, UUID uuid) {
		return level.getEntities().get(uuid);
	}

	public static String attributeName(Attribute attribute) {
		return java.util.Optional.ofNullable(net.minecraft.core.registries.BuiltInRegistries.ATTRIBUTE.getKey(attribute)).map(k -> k.getPath().replaceFirst("^(generic|player|zombie|horse)\\.", "")).orElse("stat");
	}
}

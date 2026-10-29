package com.iafabric.compat;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

/**
 * Attribute modifiers named by an id as 1.21 has them; 1.20.1 knows them by a UUID and a name, so the UUID comes from
 * the id (always the same for the same id).
 */
public final class Modifiers {
	private Modifiers() {
	}

	public static UUID uuid(ResourceLocation id) {
		return UUID.nameUUIDFromBytes(id.toString().getBytes(StandardCharsets.UTF_8));
	}

	public static AttributeModifier of(ResourceLocation id, double amount, AttributeModifier.Operation operation) {
		return new AttributeModifier(uuid(id), id.toString(), amount, operation);
	}
}

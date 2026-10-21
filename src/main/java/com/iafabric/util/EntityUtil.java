package com.iafabric.util;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityReference;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.animal.equine.AbstractHorse;
import org.jspecify.annotations.Nullable;

import java.util.UUID;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.gamerules.GameRule;

/** Small entity helpers for things the old code read straight off the entity. */
public final class EntityUtil {
	private EntityUtil() {
	}

	public static UUID getOwnerUUID(Entity entity) {
		EntityReference<LivingEntity> ref = null;
		if (entity instanceof TamableAnimal tamable) {
			ref = tamable.getOwnerReference();
		} else if (entity instanceof AbstractHorse horse) {
			ref = horse.getOwnerReference();
		} else if (entity instanceof OwnableEntity ownable) {
			LivingEntity owner = ownable.getOwner();
			return owner == null ? null : owner.getUUID();
		}
		return ref == null ? null : ref.getUUID();
	}

	public static void setOwnerUUID(Entity entity, @Nullable UUID uuid) {
		EntityReference<LivingEntity> ref = uuid == null ? null : EntityReference.of(uuid);
		if (entity instanceof TamableAnimal tamable) {
			tamable.setOwnerReference(ref);
		}
	}

	/** A true/false game rule; the client does not know them and reads false. */
	public static boolean rule(Level level, GameRule<Boolean> rule) {
		return level instanceof ServerLevel server && server.getGameRules().get(rule);
	}
}

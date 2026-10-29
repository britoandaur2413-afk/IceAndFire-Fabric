package com.iafabric.util;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.animal.horse.AbstractHorse;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.GameRules;

/** Small entity helpers for things the old code read straight off the entity. */
public final class EntityUtil {
	private EntityUtil() {
	}

	public static UUID getOwnerUUID(Entity entity) {
		if (entity instanceof TamableAnimal tamable) {
			return tamable.getOwnerUUID();
		} else if (entity instanceof AbstractHorse horse) {
			return horse.getOwnerUUID();
		} else if (entity instanceof OwnableEntity ownable) {
			return ownable.getOwnerUUID();
		}
		return null;
	}

	public static void setOwnerUUID(Entity entity, @Nullable UUID uuid) {
		if (entity instanceof TamableAnimal tamable) {
			tamable.setOwnerUUID(uuid);
		}
	}

	/** A true/false game rule; the client does not know them and reads false. */
	public static boolean rule(Level level, GameRules.Key<GameRules.BooleanValue> rule) {
		return level instanceof ServerLevel server && server.getGameRules().getBoolean(rule);
	}
}

package com.iafabric.entity;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ThrowableItemProjectile;
import net.minecraft.world.level.Level;

/** A thrown item made the old way (position or thrower); its look is {@link #getDefaultItem()}. */
public abstract class IafThrowable extends ThrowableItemProjectile {
	protected IafThrowable(EntityType<? extends ThrowableItemProjectile> type, Level level) {
		super(type, level);
	}

	protected IafThrowable(EntityType<? extends ThrowableItemProjectile> type, double x, double y, double z, Level level) {
		super(type, level);
		this.setPos(x, y, z);
	}

	protected IafThrowable(EntityType<? extends ThrowableItemProjectile> type, LivingEntity thrower, Level level) {
		super(type, level);
		this.setPos(thrower.getX(), thrower.getEyeY() - 0.1F, thrower.getZ());
		this.setOwner(thrower);
	}
}

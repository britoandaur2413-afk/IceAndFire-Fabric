package com.iafabric.entity;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.level.Level;

/**
 * An arrow made the old way (position or shooter, no weapon stack); the item picked up comes from
 * {@link #getPickupItem()}. Keeps a copy of the base damage, which vanilla no longer exposes.
 */
public abstract class IafArrow extends AbstractArrow {
	private double iafBaseDamage = 2.0D;

	protected IafArrow(EntityType<? extends AbstractArrow> type, Level level) {
		super(type, level);
	}

	protected IafArrow(EntityType<? extends AbstractArrow> type, double x, double y, double z, Level level) {
		super(type, level);
		this.setPos(x, y, z);
	}

	protected IafArrow(EntityType<? extends AbstractArrow> type, LivingEntity shooter, Level level) {
		super(type, level);
		this.setPos(shooter.getX(), shooter.getEyeY() - 0.1F, shooter.getZ());
		this.setOwner(shooter);
	}

	@Override
	public void setBaseDamage(double baseDamage) {
		super.setBaseDamage(baseDamage);
		this.iafBaseDamage = baseDamage;
	}

	public double getBaseDamage() {
		return iafBaseDamage;
	}
}

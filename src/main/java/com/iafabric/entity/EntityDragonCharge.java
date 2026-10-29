package com.iafabric.entity;

import com.iafabric.IafConfig;
import com.iafabric.entity.util.IDragonProjectile;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.projectile.AbstractHurtingProjectile;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/** A ball of dragon breath (fire, ice or lightning) shot by a dragon. */
public abstract class EntityDragonCharge extends AbstractHurtingProjectile implements IDragonProjectile {
	private static final double POWER = 0.07D;

	/** On the player's game: its dragon's mouth from it, and when, as it was first drawn (it is drawn leaving the mouth). */
	public @Nullable Vec3 fromMouth;
	public float fromMouthAt;

	public EntityDragonCharge(EntityType<? extends AbstractHurtingProjectile> type, Level worldIn) {
		super(type, worldIn);
	}

	public EntityDragonCharge(EntityType<? extends AbstractHurtingProjectile> type, Level worldIn, double posX, double posY, double posZ,
		double accelX, double accelY, double accelZ) {
		super(type, posX, posY, posZ, accelX, accelY, accelZ, worldIn);
		this.aim(accelX, accelY, accelZ);
	}

	public EntityDragonCharge(EntityType<? extends AbstractHurtingProjectile> type, Level worldIn, EntityDragonBase shooter, double accelX,
		double accelY, double accelZ) {
		super(type, shooter, accelX, accelY, accelZ, worldIn);
		this.aim(accelX, accelY, accelZ);
	}

	/** Flies the way it is aimed, starting at POWER and gaining POWER a tick (as the 1.21 charges do). */
	private void aim(double x, double y, double z) {
		Vec3 dir = new Vec3(x, y, z);
		if (dir.lengthSqr() > 0) {
			dir = dir.normalize().scale(POWER);
			this.xPower = dir.x;
			this.yPower = dir.y;
			this.zPower = dir.z;
			this.setDeltaMovement(dir);
		}
	}

	@Override
	protected boolean canHitEntity(Entity hitMob) {
		Entity shooter = getOwner();
		return hitMob != this && super.canHitEntity(hitMob) && !(shooter == null || hitMob.isAlliedTo(shooter)) && !(hitMob instanceof EntityDragonPart);
	}

	@Override
	protected void onHit(@NotNull HitResult movingObject) {
		Entity shootingEntity = this.getOwner();
		if (this.level().isClientSide) {
			return;
		}
		if (movingObject.getType() == HitResult.Type.ENTITY) {
			Entity entity = ((EntityHitResult) movingObject).getEntity();
			if (entity instanceof IDragonProjectile) {
				return;
			}
			if (shootingEntity instanceof EntityDragonBase dragon) {
				if (dragon.isAlliedTo(entity) || dragon.is(entity) || dragon.isPart(entity)) {
					return;
				}
			}
			if (entity != shootingEntity && shootingEntity instanceof EntityDragonBase dragon) {
				if (entity instanceof TamableAnimal && dragon.isOwnedBy(dragon.getOwner())) {
					return;
				}
				dragon.randomizeAttacks();
				this.discard();
			}
			if (!entity.is(shootingEntity)) {
				if (shootingEntity instanceof EntityDragonBase dragon && entity instanceof TamableAnimal tamable && dragon.getOwner() == tamable.getOwner()) {
					return;
				}
				if (shootingEntity instanceof EntityDragonBase shootingDragon) {
					float damageAmount = getDamage() * shootingDragon.getDragonStage();
					Entity cause = shootingDragon.getRidingPlayer() != null ? shootingDragon.getRidingPlayer() : shootingDragon;
					DamageSource source = causeDamage(cause);
					entity.hurt(source, damageAmount);
					if (entity instanceof LivingEntity living && living.getHealth() == 0) {
						shootingDragon.randomizeAttacks();
					}
					if (this.level() instanceof ServerLevel) {
						this.doEnchantDamageEffects(shootingDragon, entity);
					}
				}
				this.discard();
			}
		}
		if (movingObject.getType() != HitResult.Type.MISS) {
			if (shootingEntity instanceof EntityDragonBase dragon && IafConfig.dragonGriefing != 2) {
				destroyArea(this.level(), BlockPos.containing(this.getX(), this.getY(), this.getZ()), dragon);
			}
			this.discard();
		}
	}

	public abstract DamageSource causeDamage(@Nullable Entity cause);

	public abstract void destroyArea(Level world, BlockPos center, EntityDragonBase destroyer);

	public abstract float getDamage();

	@Override
	public boolean isPickable() {
		return false;
	}

	@Override
	public boolean hurt(DamageSource source, float amount) {
		if (!(this.level() instanceof ServerLevel level)) {
			return super.hurt(source, amount);
		}
		return false;
	}

	@Override
	public float getPickRadius() {
		return 0F;
	}
}

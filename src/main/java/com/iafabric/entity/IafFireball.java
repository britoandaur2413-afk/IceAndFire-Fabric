package com.iafabric.entity;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.Level;
import com.iafabric.compat.ValueInput;
import com.iafabric.compat.ValueOutput;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

/**
 * A fireball that flies like the old (1.18) hurting projectiles: a fixed push per tick (xPower,
 * yPower, zPower) and drag. The mod's breath and magic shots were written for it.
 */
public abstract class IafFireball extends Projectile {
	public double xPower;
	public double yPower;
	public double zPower;

	protected IafFireball(EntityType<? extends IafFireball> type, Level level) {
		super(type, level);
	}

	protected IafFireball(EntityType<? extends IafFireball> type, double x, double y, double z, double accelX, double accelY, double accelZ, Level level) {
		this(type, level);
		this.moveTo(x, y, z, this.getYRot(), this.getXRot());
		this.reapplyPosition();
		setPower(accelX, accelY, accelZ);
	}

	protected IafFireball(EntityType<? extends IafFireball> type, LivingEntity shooter, double accelX, double accelY, double accelZ, Level level) {
		this(type, shooter.getX(), shooter.getY(), shooter.getZ(), accelX, accelY, accelZ, level);
		this.setOwner(shooter);
		this.setRot(shooter.getYRot(), shooter.getXRot());
	}

	private void setPower(double accelX, double accelY, double accelZ) {
		double d = Math.sqrt(accelX * accelX + accelY * accelY + accelZ * accelZ);
		if (d != 0.0D) {
			this.xPower = accelX / d * 0.1D;
			this.yPower = accelY / d * 0.1D;
			this.zPower = accelZ / d * 0.1D;
		}
	}

	@Override
	protected void defineSynchedData(SynchedEntityData.Builder builder) {
	}

	@Override
	public boolean shouldRenderAtSqrDistance(double distance) {
		double size = this.getBoundingBox().getSize() * 4.0D;
		if (Double.isNaN(size)) {
			size = 4.0D;
		}
		size *= 64.0D;
		return distance < size * size;
	}

	@Override
	public void tick() {
		Entity owner = this.getOwner();
		if (this.level().isClientSide || (owner == null || !owner.isRemoved()) && this.level().hasChunkAt(this.blockPosition())) {
			super.tick();
			if (this.shouldBurn()) {
				this.igniteForSeconds(1.0F);
			}
			HitResult hit = getHitResult();
			if (hit.getType() != HitResult.Type.MISS) {
				this.onHit(hit);
			}
			this.checkInsideBlocks();
			Vec3 motion = this.getDeltaMovement();
			double x = this.getX() + motion.x;
			double y = this.getY() + motion.y;
			double z = this.getZ() + motion.z;
			ProjectileUtil.rotateTowardsMovement(this, 0.2F);
			float inertia = this.getInertia();
			if (this.isInWater()) {
				for (int i = 0; i < 4; ++i) {
					this.level().addParticle(ParticleTypes.BUBBLE, x - motion.x * 0.25D, y - motion.y * 0.25D, z - motion.z * 0.25D, motion.x, motion.y, motion.z);
				}
				inertia = 0.8F;
			}
			this.setDeltaMovement(motion.add(this.xPower, this.yPower, this.zPower).scale(inertia));
			ParticleOptions trail = this.getTrailParticle();
			if (trail != null) {
				this.level().addParticle(trail, x, y + 0.5D, z, 0.0D, 0.0D, 0.0D);
			}
			this.setPos(x, y, z);
		} else {
			this.discard();
		}
	}

	/** What the projectile hits on its way this tick. */
	protected HitResult getHitResult() {
		return ProjectileUtil.getHitResultOnMoveVector(this, this::canHitEntity);
	}

	protected boolean shouldBurn() {
		return true;
	}

	protected ParticleOptions getTrailParticle() {
		return ParticleTypes.SMOKE;
	}

	protected float getInertia() {
		return 0.95F;
	}

	@Override
	public boolean hurt(DamageSource source, float amount) {
		if (!(this.level() instanceof ServerLevel level)) {
			return super.hurt(source, amount);
		}
		return false;
	}

	@Override
	public float getLightLevelDependentMagicValue() {
		return 1.0F;
	}

	@Override
	public void addAdditionalSaveData(CompoundTag outputTag) {
		ValueOutput output = new ValueOutput(outputTag, this.registryAccess());
		super.addAdditionalSaveData(outputTag);
		output.putDouble("PowerX", xPower);
		output.putDouble("PowerY", yPower);
		output.putDouble("PowerZ", zPower);
	}

	@Override
	public void readAdditionalSaveData(CompoundTag inputTag) {
		ValueInput input = new ValueInput(inputTag, this.registryAccess());
		super.readAdditionalSaveData(inputTag);
		xPower = input.getDoubleOr("PowerX", 0.0D);
		yPower = input.getDoubleOr("PowerY", 0.0D);
		zPower = input.getDoubleOr("PowerZ", 0.0D);
	}
}

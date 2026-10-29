package com.iafabric.entity;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.PushReaction;
import com.iafabric.compat.ValueInput;
import com.iafabric.compat.ValueOutput;

/**
 * An effect drawn in the world for a while (Vfx), never saved: a warning circle on the ground that fills up until the
 * blow falls, a warning lane along the way of a charge, a magic circle turning, a shock wave going out, a column of
 * light, a jet of frost. Only drawn; it touches nothing.
 */
public class EntityVfx extends Entity {
	public static final int CIRCLE = 0;
	public static final int LINE = 1;
	public static final int RUNE = 2;
	public static final int WAVE = 3;
	public static final int BEAM = 4;
	public static final int STREAM = 5;
	/** Magic circle looks. */
	public static final int FROST = 0;
	public static final int DREAD = 1;
	public static final int STORM = 2;
	public static final int FLAME = 3;
	public static final int NATURE = 4;

	private static final EntityDataAccessor<Integer> KIND = SynchedEntityData.defineId(EntityVfx.class, EntityDataSerializers.INT);
	private static final EntityDataAccessor<Integer> STYLE = SynchedEntityData.defineId(EntityVfx.class, EntityDataSerializers.INT);
	private static final EntityDataAccessor<Integer> COLOR = SynchedEntityData.defineId(EntityVfx.class, EntityDataSerializers.INT);
	private static final EntityDataAccessor<Float> SIZE = SynchedEntityData.defineId(EntityVfx.class, EntityDataSerializers.FLOAT);
	private static final EntityDataAccessor<Float> LENGTH = SynchedEntityData.defineId(EntityVfx.class, EntityDataSerializers.FLOAT);
	private static final EntityDataAccessor<Integer> CHARGE = SynchedEntityData.defineId(EntityVfx.class, EntityDataSerializers.INT);
	private static final EntityDataAccessor<Integer> LIFE = SynchedEntityData.defineId(EntityVfx.class, EntityDataSerializers.INT);
	private static final EntityDataAccessor<Float> SPIN = SynchedEntityData.defineId(EntityVfx.class, EntityDataSerializers.FLOAT);

	public EntityVfx(EntityType<? extends EntityVfx> type, Level level) {
		super(type, level);
		this.noPhysics = true;
		this.setNoGravity(true);
	}

	@Override
	protected void defineSynchedData() {
		this.entityData.define(KIND, CIRCLE);
		this.entityData.define(STYLE, FROST);
		this.entityData.define(COLOR, 0xFFFFFFFF);
		this.entityData.define(SIZE, 1F);
		this.entityData.define(LENGTH, 1F);
		this.entityData.define(CHARGE, 20);
		this.entityData.define(LIFE, 40);
		this.entityData.define(SPIN, 0F);
	}

	public EntityVfx setup(int kind, int style, int color, float size, float length, int charge, int life, float spin) {
		this.entityData.set(KIND, kind);
		this.entityData.set(STYLE, style);
		this.entityData.set(COLOR, color);
		this.entityData.set(SIZE, size);
		this.entityData.set(LENGTH, length);
		this.entityData.set(CHARGE, Math.max(1, charge));
		this.entityData.set(LIFE, Math.max(1, life));
		this.entityData.set(SPIN, spin);
		return this;
	}

	public int getKind() {
		return this.entityData.get(KIND);
	}

	public int getStyle() {
		return this.entityData.get(STYLE);
	}

	public int getColor() {
		return this.entityData.get(COLOR);
	}

	public float getSize() {
		return this.entityData.get(SIZE);
	}

	public float getLength() {
		return this.entityData.get(LENGTH);
	}

	public int getCharge() {
		return this.entityData.get(CHARGE);
	}

	public int getLife() {
		return this.entityData.get(LIFE);
	}

	public float getSpin() {
		return this.entityData.get(SPIN);
	}

	/** Lives longer (a magic circle kept while its caster still charges). */
	public void extend(int ticks) {
		this.entityData.set(LIFE, Math.max(getLife(), this.tickCount + ticks));
	}

	@Override
	public void tick() {
		super.tick();
		if (!this.level().isClientSide && this.tickCount >= getLife()) {
			this.discard();
		}
	}

	@Override
	public boolean isPickable() {
		return false;
	}

	@Override
	public boolean isAttackable() {
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
	public PushReaction getPistonPushReaction() {
		return PushReaction.IGNORE;
	}

	@Override
	public boolean shouldRenderAtSqrDistance(double distance) {
		return distance < 160 * 160;
	}

	@Override
	public void readAdditionalSaveData(CompoundTag inputTag) {
		ValueInput input = new ValueInput(inputTag, this.level().registryAccess());
	}

	@Override
	public void addAdditionalSaveData(CompoundTag outputTag) {
		ValueOutput output = new ValueOutput(outputTag, this.level().registryAccess());
	}
}

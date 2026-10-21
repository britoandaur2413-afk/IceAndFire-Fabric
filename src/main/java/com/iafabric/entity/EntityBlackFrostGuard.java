package com.iafabric.entity;

import com.iafabric.world.structure.DreadPieces;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.jspecify.annotations.Nullable;

/**
 * Black Frost guarding Cryona's throne in her citadel: her sign there with her (EntityCryonaAsleep), not Black Frost
 * itself (it waits in her realm). It sits upright on its bed of blue ice behind her throne, facing the way in, its head
 * high over her and turned to who comes in; its throne room is made for its size (CitadelPiece), so nothing of it
 * goes through a wall, and nothing of it is between the way in and her. It never fights there, nothing hurts it, and
 * it is no one's (no taming, riding or leading it). A player's blow on it is a blow on her (her call).
 */
public class EntityBlackFrostGuard extends EntityIceDragon {
	/** How far its head turns to who comes in. */
	private static final float LOOK = 45F;
	/** Its place (on its bed) and the way it faces (the way in). */
	private @Nullable Vec3 home;
	private float facing;

	public EntityBlackFrostGuard(EntityType<?> type, Level level) {
		super(type, level);
	}

	/** Black Frost by her throne, facing as given (not yet in the level). */
	public static @Nullable EntityBlackFrostGuard make(WorldGenLevel level, RandomSource random, BlockPos at, float yaw) {
		EntityBlackFrostGuard frost = DreadPieces.dreadDragon(IafEntityRegistry.BLACK_FROST_GUARD, level, random, at, 125, true);
		if (frost == null) {
			return null;
		}
		frost.place(at, yaw);
		return frost;
	}

	/** To its place, facing as given. */
	public void place(BlockPos at, float yaw) {
		this.home = Vec3.atBottomCenterOf(at);
		this.facing = yaw;
		this.snapTo(this.home.x, this.home.y, this.home.z, yaw, 0F);
		this.setYRot(yaw);
		this.yBodyRot = yaw;
		this.yHeadRot = yaw;
		this.fightLed = true;
	}

	/** Black Frost asleep at her throne in an older world (itself, lying still): her guard takes its place. */
	static void takeOver(ServerLevel level, EntityDragonBase old) {
		EntityBlackFrostGuard frost = make(level, level.getRandom(), old.blockPosition(), old.getYRot());
		if (frost != null) {
			level.addFreshEntity(frost);
		}
		old.discard();
	}

	@Override
	public void tick() {
		super.tick();
		if (!(this.level() instanceof ServerLevel level)) {
			return;
		}
		// (its sign's fight leads it: no mind of its own, yet no stiff body)
		this.setNoAi(false);
		this.fightLed = true;
		this.setInSittingPose(false);
		this.setFlying(false);
		this.setHovering(false);
		if (this.home == null) {
			this.home = this.position();
			this.facing = this.getYRot();
		}
		if (this.position().distanceToSqr(this.home) > 0.25) {
			this.teleportTo(this.home.x, this.home.y, this.home.z);
		}
		this.setDeltaMovement(Vec3.ZERO);
		this.setYRot(this.facing);
		this.yBodyRot = this.facing;
		// its head to who comes in
		float head = this.facing;
		Player watcher = level.getNearestPlayer(this.getX(), this.getY(), this.getZ(), 48, p -> !p.isSpectator());
		if (watcher != null) {
			float want = (float) Math.toDegrees(Math.atan2(-(watcher.getX() - this.getX()), watcher.getZ() - this.getZ()));
			head = this.facing + Mth.clamp(Mth.wrapDegrees(want - this.facing), -LOOK, LOOK);
		}
		this.yHeadRot = Mth.approachDegrees(this.yHeadRot, head, 4F);
	}

	/** Nothing hurts it: a player's blow is a blow on her (her call); an admin's /kill or the void takes it away. */
	@Override
	public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
		if (source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) {
			this.discard();
			return true;
		}
		if (source.getEntity() instanceof Player player && !player.isCreative() && !player.isSpectator()) {
			EntityCryonaAsleep queen = null;
			double best = EntityCryonaAsleep.HERS * EntityCryonaAsleep.HERS;
			for (EntityCryonaAsleep near : level.getEntitiesOfClass(EntityCryonaAsleep.class, this.getBoundingBox().inflate(EntityCryonaAsleep.HERS))) {
				double distance = near.distanceToSqr(this);
				if (distance < best) {
					best = distance;
					queen = near;
				}
			}
			if (queen != null) {
				queen.call(level, player);
			}
		}
		return false;
	}

	/** It sits upright behind her throne, its wings folded. */
	@Override
	public boolean isOrderedToSit() {
		return true;
	}

	/** (No sleep and no foe of its own.) */
	@Override
	public boolean isTimeToWake() {
		return true;
	}

	@Override
	public void updateCheckPlayer() {
	}

	@Override
	public void setTarget(@Nullable LivingEntity target) {
		super.setTarget(null);
	}

	/** (No food, saddle, horn or anything else reaches it.) */
	@Override
	public InteractionResult mobInteract(Player player, @NotNull InteractionHand hand) {
		return InteractionResult.SUCCESS;
	}

	@Override
	public boolean isPushable() {
		return false;
	}

	@Override
	public boolean canBeLeashed() {
		return false;
	}

	@Override
	public boolean canBeSeenAsEnemy() {
		return false;
	}

	@Override
	public boolean canBeAffected(MobEffectInstance effect) {
		return false;
	}

	@Override
	public boolean removeWhenFarAway(double distanceToClosestPlayer) {
		return false;
	}

	@Override
	public void addAdditionalSaveData(ValueOutput output) {
		super.addAdditionalSaveData(output);
		if (this.home != null) {
			output.putDouble("GuardX", this.home.x);
			output.putDouble("GuardY", this.home.y);
			output.putDouble("GuardZ", this.home.z);
			output.putFloat("GuardFacing", this.facing);
		}
	}

	@Override
	public void readAdditionalSaveData(ValueInput input) {
		super.readAdditionalSaveData(input);
		double x = input.getDoubleOr("GuardX", Double.NaN);
		if (!Double.isNaN(x)) {
			this.home = new Vec3(x, input.getDoubleOr("GuardY", 0), input.getDoubleOr("GuardZ", 0));
			this.facing = input.getFloatOr("GuardFacing", 0F);
		}
	}
}

package com.iafabric.entity;

import net.minecraft.nbt.CompoundTag;
import com.iafabric.misc.IafSoundRegistry;
import com.iafabric.pixie.ExiledPixies;
import com.iafabric.pixie.ExiledVariants;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import com.iafabric.compat.ValueInput;
import com.iafabric.compat.ValueOutput;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.EnumSet;

/**
 * An exiled pixie: the one that lived in the first tree a player cut, now theirs for the rest of their adventure.
 * A pixie like the others, faded half to white (one of twenty), that nothing hurts. She stays by her player's
 * shoulder, flies ahead to show the way and to what she wants them to see, and is made again by them wherever they go
 * (she is never saved with the land).
 */
public class EntityExiledPixie extends EntityPixie {
	private static final EntityDataAccessor<Integer> VARIANT = SynchedEntityData.defineId(EntityExiledPixie.class, EntityDataSerializers.INT);
	/** Still angry about her house (she circles her player, glowing, until they speak to her). */
	private static final EntityDataAccessor<Boolean> ANGRY = SynchedEntityData.defineId(EntityExiledPixie.class, EntityDataSerializers.BOOLEAN);

	/** Where she wants to be instead of by her player's shoulder (the way ahead, a hidden chest), and until when. */
	private @Nullable Vec3 focus;
	private long focusUntil;
	/** The focus only leads the way (she leaves it to speak to her player). */
	private boolean focusGuides;
	/** Until when she is speaking: before her player's eyes, a little to their left. */
	private long speakUntil;

	public EntityExiledPixie(EntityType<? extends EntityExiledPixie> type, Level level) {
		super(type, level);
		this.setInvulnerable(true);
	}

	@Override
	protected void defineSynchedData(SynchedEntityData.Builder builder) {
		super.defineSynchedData(builder);
		builder.define(VARIANT, 0);
		builder.define(ANGRY, false);
	}

	public int getVariant() {
		return ExiledVariants.of(this.entityData.get(VARIANT));
	}

	public void setVariant(int variant) {
		this.entityData.set(VARIANT, ExiledVariants.of(variant));
	}

	public boolean isAngry() {
		return this.entityData.get(ANGRY);
	}

	public void setAngry(boolean angry) {
		this.entityData.set(ANGRY, angry);
		this.setGlowingTag(angry);
	}

	/** She flies to a place for a while (null: back to her player's shoulder). */
	public void focus(@Nullable Vec3 at, int ticks) {
		this.focus = at;
		this.focusUntil = at == null ? 0 : this.level().getGameTime() + ticks;
		this.focusGuides = false;
	}

	/** She leads the way: a place ahead of her player, left whenever she speaks (a place she shows comes first). */
	public void guide(Vec3 at, int ticks) {
		if (this.focus == null || this.focusGuides || this.level().getGameTime() > this.focusUntil) {
			this.focus = at;
			this.focusUntil = this.level().getGameTime() + ticks;
			this.focusGuides = true;
		}
	}

	/** She comes before her player's eyes to speak, for a while. */
	public void speak(int ticks) {
		this.speakUntil = this.level().getGameTime() + ticks;
	}

	public boolean speaking() {
		return this.level().getGameTime() < this.speakUntil;
	}

	public @Nullable Vec3 focus() {
		if (this.focus != null && this.level().getGameTime() > this.focusUntil) {
			this.focus = null;
		}
		return this.focus;
	}

	@Override
	protected void registerGoals() {
		this.goalSelector.addGoal(0, new FloatGoal(this));
		this.goalSelector.addGoal(1, new Companion(this));
	}

	/** She passes through walls as she moves (a faded pixie, half a ghost): she never stays stuck behind her player in a cave. */
	@Override
	public void tick() {
		this.noPhysics = true;
		super.tick();
		this.noPhysics = false;
	}

	@Override
	protected float[] dustColor() {
		return ExiledVariants.dust(this.getVariant());
	}

	@Override
	protected boolean givesOwnerEffect() {
		// her gift is her own (ExiledPixies), not one color's effect
		return false;
	}

	@Override
	public int getColor() {
		return 0;
	}

	@Override
	public boolean hurt(DamageSource source, float amount) {
		if (!(this.level() instanceof ServerLevel level)) {
			return super.hurt(source, amount);
		}
		return false;
	}

	@Override
	public boolean isInvulnerableTo(DamageSource source) {
		return !source.is(net.minecraft.tags.DamageTypeTags.BYPASSES_INVULNERABILITY);
	}

	@Override
	public boolean isPushable() {
		return false;
	}

	@Override
	protected void doPush(@NotNull Entity entity) {
	}

	@Override
	public boolean canBeLeashed() {
		return false;
	}

	@Override
	public boolean removeWhenFarAway(double distance) {
		return false;
	}

	@Override
	public boolean isFood(ItemStack stack) {
		return false;
	}

	@Override
	public int getAmbientSoundInterval() {
		return 600;
	}

	@Override
	protected float getSoundVolume() {
		return 0.45F;
	}

	@Override
	protected @Nullable SoundEvent getAmbientSound() {
		return this.isAngry() ? IafSoundRegistry.PIXIE_TAUNT : IafSoundRegistry.PIXIE_IDLE;
	}

	/** A word with her (her player) opens her menu; anyone else she ignores. */
	@Override
	public InteractionResult mobInteract(@NotNull Player player, @NotNull InteractionHand hand) {
		if (!this.isOwnedBy(player)) {
			return InteractionResult.PASS;
		}
		if (player instanceof ServerPlayer server) {
			ExiledPixies.interact(server, this);
		}
		return InteractionResult.SUCCESS;
	}

	/** Only the one her player has out lives: an older copy, or one without a player, goes. */
	@Override
	protected void customServerAiStep() {
		ServerLevel level = (ServerLevel) this.level();
		super.customServerAiStep();
		if (!ExiledPixies.isCurrent(this)) {
			this.discard();
		}
	}

	@Override
	public void readAdditionalSaveData(CompoundTag inputTag) {
		ValueInput input = new ValueInput(inputTag, this.registryAccess());
		super.readAdditionalSaveData(inputTag);
		this.setVariant(input.getIntOr("Variant", 0));
	}

	@Override
	public void addAdditionalSaveData(CompoundTag outputTag) {
		ValueOutput output = new ValueOutput(outputTag, this.registryAccess());
		super.addAdditionalSaveData(outputTag);
		output.putInt("Variant", this.getVariant());
	}

	/**
	 * By her player's shoulder (a little to their left, out of their view ahead, at their eyes' height, bobbing), circling their head while
	 * she is angry, or where she wants them to look; far behind, she is at their side at once.
	 */
	static class Companion extends Goal {
		private final EntityExiledPixie pixie;

		Companion(EntityExiledPixie pixie) {
			this.pixie = pixie;
			this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
		}

		@Override
		public boolean canUse() {
			LivingEntity owner = this.pixie.getOwner();
			return owner != null && owner.isAlive() && !owner.isSpectator() && owner.level() == this.pixie.level();
		}

		@Override
		public boolean requiresUpdateEveryTick() {
			return true;
		}

		@Override
		public void tick() {
			LivingEntity owner = this.pixie.getOwner();
			if (owner == null) {
				return;
			}
			float t = this.pixie.tickCount;
			Vec3 eyes = owner.getEyePosition();
			Vec3 focus = this.pixie.focus();
			float yaw = owner.getYRot() * Mth.DEG_TO_RAD;
			Vec3 right = new Vec3(-Math.cos(yaw), 0, -Math.sin(yaw));
			Vec3 forward = new Vec3(-Math.sin(yaw), 0, Math.cos(yaw));
			boolean speaking = this.pixie.speaking() && (focus == null || this.pixie.focusGuides);
			Vec3 to;
			if (speaking) {
				// before their eyes, a little to their left and down, facing them
				focus = null;
				to = eyes.add(forward.scale(2.3)).add(right.scale(-0.75)).add(0, -0.25 + Math.sin(t * 0.12) * 0.06, 0);
			} else if (focus != null) {
				to = focus.add(0, Math.sin(t * 0.12) * 0.15, 0);
			} else if (this.pixie.isAngry()) {
				// angry: darting from side to side before their eyes
				to = eyes.add(forward.scale(1.6)).add(right.scale(Math.sin(t * 0.13) * 1.3)).add(0, 0.2 + Math.sin(t * 0.3) * 0.25, 0);
			} else {
				// over their left shoulder, a little behind: out of their view ahead, there when they look
				to = eyes.add(right.scale(-1.05)).add(forward.scale(-0.55)).add(0, 0.45 + Math.sin(t * 0.1) * 0.12, 0);
			}
			if (this.pixie.distanceToSqr(owner) > 24 * 24 && focus == null) {
				this.pixie.moveTo(to.x, to.y, to.z, owner.getYRot(), 0);
				this.pixie.setDeltaMovement(Vec3.ZERO);
				return;
			}
			double distance = this.pixie.position().distanceTo(to);
			double speed = Mth.clamp(0.2 + distance * 0.1, 0.2, 1.8);
			this.pixie.getMoveControl().setWantedPosition(to.x, to.y, to.z, speed);
			this.pixie.slowSpeed = distance < 3;
			if (speaking) {
				this.pixie.getLookControl().setLookAt(eyes.x, eyes.y, eyes.z, 30F, 30F);
				if (distance < 1.2) {
					// she turns to face them while she speaks
					float face = (float) Math.toDegrees(Math.atan2(-(eyes.x - this.pixie.getX()), eyes.z - this.pixie.getZ()));
					this.pixie.setYRot(face);
					this.pixie.yBodyRot = face;
				}
			} else if (focus != null && distance < 2.5) {
				this.pixie.getLookControl().setLookAt(focus.x, focus.y - 1, focus.z, 20F, 20F);
			} else {
				this.pixie.getLookControl().setLookAt(eyes.x, eyes.y, eyes.z, 20F, 20F);
			}
		}
	}
}

package com.iafabric.entity;

import com.iafabric.boss.ThroneFight;
import com.iafabric.citadel.animation.Animation;
import com.iafabric.citadel.animation.IAnimatedEntity;
import com.iafabric.item.IafItemRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

/**
 * Cryona asleep on her throne in her citadel: her sign in the world, not Cryona herself (she waits in her realm:
 * EntityDreadQueen, ThroneFight). Grey and still with her weapons, she never moves, never fights, and nothing hurts
 * her. A player who comes near, or strikes her, is called into her realm; if her call cannot be (no realm, nobody to
 * take), she only sleeps on. Black Frost prowls behind her throne the same way (EntityBlackFrostGuard); if it is gone,
 * she sets it again. In an older citadel her throne room is made anew first (CitadelPiece.renewThroneRoom).
 */
public class EntityCryonaAsleep extends Mob implements IAnimatedEntity, com.iafabric.entity.util.IBlacklistedFromStatues {
	/** How near a player is called (in her citadel: at the foot of her throne's dais). */
	public static final int CALL = 7;
	/** How far from her the Black Frost with her may be. */
	public static final int HERS = 48;
	/** How often she looks after her throne room and her guard. */
	private static final int LOOK = 40;
	/** Her throne (the citadel she rules), and where Black Frost prowls by her (none: no Black Frost is set again). */
	private @Nullable BlockPos throne;
	private @Nullable BlockPos bed;
	/** Her throne room: not looked at yet (-1), as it is now (DONE), its land not loaded (WAIT), being made anew (WORKING). */
	private int room = -1;
	/** A call that could not be is tried again after this. */
	private long quietUntil;
	private int animationTick;
	private Animation animation = NO_ANIMATION;

	public EntityCryonaAsleep(EntityType<? extends EntityCryonaAsleep> type, Level level) {
		super(type, level);
		this.setNoAi(true);
		this.setPersistenceRequired();
		this.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(IafItemRegistry.DREAD_QUEEN_SWORD));
		this.setItemSlot(EquipmentSlot.OFFHAND, new ItemStack(IafItemRegistry.DREAD_QUEEN_STAFF));
		this.setDropChance(EquipmentSlot.MAINHAND, 0F);
		this.setDropChance(EquipmentSlot.OFFHAND, 0F);
	}

	public static AttributeSupplier.Builder bakeAttributes() {
		return Mob.createMobAttributes()
			.add(Attributes.MAX_HEALTH, 100D)
			.add(Attributes.KNOCKBACK_RESISTANCE, 1D);
	}

	/** She asleep on her throne, facing as given (not yet in the level). */
	public static @Nullable EntityCryonaAsleep make(Level level, BlockPos throne, @Nullable BlockPos bed, double x, double y, double z, float yaw,
		EntitySpawnReason reason) {
		EntityCryonaAsleep queen = IafEntityRegistry.CRYONA_ASLEEP.create(level, reason);
		if (queen == null) {
			return null;
		}
		queen.snapTo(x, y, z, yaw, 0F);
		queen.yHeadRot = yaw;
		queen.yBodyRot = yaw;
		queen.throne = throne;
		queen.bed = bed;
		return queen;
	}

	/** Cryona asleep on her throne in an older world (she slept there herself then): her sign takes her place. */
	static void takeOver(ServerLevel level, EntityDreadQueen old) {
		BlockPos throne = old.getThrone() != null ? old.getThrone() : old.blockPosition();
		BlockPos bed = null;
		double best = HERS * HERS;
		for (EntityDragonBase dragon : level.getEntitiesOfClass(EntityDragonBase.class, old.getBoundingBox().inflate(HERS), EntityDragonBase::isBlackFrost)) {
			double distance = dragon.distanceToSqr(old);
			if (distance < best) {
				best = distance;
				bed = dragon.blockPosition();
			}
		}
		EntityCryonaAsleep queen = make(level, throne, bed, old.getX(), old.getY(), old.getZ(), old.getYRot(), EntitySpawnReason.LOAD);
		if (queen != null) {
			level.addFreshEntity(queen);
		}
		old.discard();
	}

	/** Her throne (where she sleeps, if none was given). */
	public BlockPos throne() {
		return this.throne != null ? this.throne : this.blockPosition();
	}

	public @Nullable BlockPos bed() {
		return this.bed;
	}

	/** To her new seat (her throne room made anew), Black Frost's place with her. */
	public void seat(BlockPos throne, @Nullable BlockPos bed) {
		this.throne = throne;
		this.bed = bed;
		this.snapTo(throne.getX() + 0.5, throne.getY() + 0.5, throne.getZ() + 0.5, this.getYRot(), 0F);
	}

	@Override
	public void aiStep() {
		super.aiStep();
		if (!(this.level() instanceof ServerLevel level)) {
			return;
		}
		if (this.room != com.iafabric.world.structure.CitadelPiece.DONE
			&& (this.room == com.iafabric.world.structure.CitadelPiece.WORKING || this.tickCount % LOOK == 0)) {
			// (an older citadel's: made anew a few rows a tick, nobody called meanwhile)
			this.room = com.iafabric.world.structure.CitadelPiece.renewThroneRoom(level, this);
			if (this.room != com.iafabric.world.structure.CitadelPiece.DONE) {
				return;
			}
		}
		if (this.tickCount % 10 != 0) {
			return;
		}
		if (this.tickCount % LOOK == 0) {
			this.guarded(level);
		}
		level.sendParticles(ParticleTypes.SNOWFLAKE, this.getX(), this.getY() + 2.2, this.getZ(), 3, 0.3, 0.2, 0.3, 0.01);
		if (level.getGameTime() < this.quietUntil) {
			return;
		}
		// (not for who went home from her duel a moment ago: only when they come again)
		BlockPos home = this.throne();
		Player near = level.getNearestPlayer(this.getX(), this.getY(), this.getZ(), CALL, p -> p instanceof Player player && !player.isCreative()
			&& !player.isSpectator() && !ThroneFight.declined(player, level.dimension(), home));
		if (near != null) {
			this.call(level, near);
		}
	}

	/**
	 * Black Frost prowls behind her throne: if it is gone, she sets it again at its place (not while another Black
	 * Frost is by her, as an older world's that is about to become her guard).
	 */
	private void guarded(ServerLevel level) {
		if (this.bed == null || this.tickCount < LOOK * 2 || !level.isLoaded(this.bed)
			|| !level.getEntitiesOfClass(EntityDragonBase.class, this.getBoundingBox().inflate(HERS), EntityDragonBase::isBlackFrost).isEmpty()) {
			return;
		}
		EntityBlackFrostGuard frost = EntityBlackFrostGuard.make(level, level.getRandom(), this.bed, this.getYRot());
		if (frost != null) {
			level.addFreshEntity(frost);
		}
	}

	private @Nullable EntityBlackFrostGuard guard(ServerLevel level) {
		EntityBlackFrostGuard frost = null;
		double best = HERS * HERS;
		for (EntityBlackFrostGuard dragon : level.getEntitiesOfClass(EntityBlackFrostGuard.class, this.getBoundingBox().inflate(HERS))) {
			double distance = dragon.distanceToSqr(this);
			if (distance < best) {
				best = distance;
				frost = dragon;
			}
		}
		return frost;
	}

	/** Her call: the players near (and who struck her) go to her realm; she and Black Frost by her go with them. */
	public void call(ServerLevel level, @Nullable LivingEntity foe) {
		if (this.isRemoved() || this.room == com.iafabric.world.structure.CitadelPiece.WORKING) {
			return;
		}
		EntityBlackFrostGuard frost = this.guard(level);
		if (!ThroneFight.begin(level, this, frost, foe)) {
			// (it cannot be now: she sleeps on, and calls again in a while)
			this.quietUntil = level.getGameTime() + 100;
		}
	}

	/** Nothing hurts her: a player's blow is a wish for her duel (her call); an admin's /kill or the void takes her away. */
	@Override
	public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
		if (source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) {
			this.discard();
			return true;
		}
		if (source.getEntity() instanceof Player player && !player.isCreative() && !player.isSpectator()) {
			this.call(level, player);
		}
		return false;
	}

	@Override
	protected void registerGoals() {
		// (none: she sleeps)
	}

	@Override
	public boolean isPushable() {
		return false;
	}

	@Override
	protected void doPush(Entity entity) {
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
	public boolean isPersistenceRequired() {
		return true;
	}

	@Override
	public void addAdditionalSaveData(ValueOutput output) {
		super.addAdditionalSaveData(output);
		if (this.throne != null) {
			output.putLong("Throne", this.throne.asLong());
		}
		if (this.bed != null) {
			output.putLong("Bed", this.bed.asLong());
		}
	}

	@Override
	public void readAdditionalSaveData(ValueInput input) {
		super.readAdditionalSaveData(input);
		long at = input.getLongOr("Throne", Long.MIN_VALUE);
		this.throne = at == Long.MIN_VALUE ? null : BlockPos.of(at);
		long bedAt = input.getLongOr("Bed", Long.MIN_VALUE);
		this.bed = bedAt == Long.MIN_VALUE ? null : BlockPos.of(bedAt);
		this.setNoAi(true);
	}

	@Override
	public int getAnimationTick() {
		return animationTick;
	}

	@Override
	public void setAnimationTick(int tick) {
		this.animationTick = tick;
	}

	@Override
	public Animation getAnimation() {
		return animation;
	}

	@Override
	public void setAnimation(Animation animation) {
		this.animation = animation;
	}

	@Override
	public Animation[] getAnimations() {
		return new Animation[]{};
	}

	/** Her sign is never turned to stone either. */
	@Override
	public boolean canBeTurnedToStone() {
		return false;
	}
}

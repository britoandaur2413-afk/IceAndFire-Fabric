package com.iafabric.entity;

import com.iafabric.IafConfig;
import com.iafabric.boss.CryonaFight;
import com.iafabric.boss.CryonaVoice;
import com.iafabric.boss.ThroneFight;
import com.iafabric.citadel.animation.Animation;
import com.iafabric.citadel.animation.IAnimatedEntity;
import com.iafabric.entity.ai.DreadAITargetNonDread;
import com.iafabric.entity.util.DragonUtils;
import com.iafabric.entity.util.IAnimalFear;
import com.iafabric.entity.util.IVillagerFear;
import com.iafabric.item.IafItemRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.BossEvent;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.jspecify.annotations.Nullable;

/**
 * Cryona, the Dread Queen, the last foe of the dread, in her realm (ThroneFight): she waits there as a fragment of
 * herself and fights from Black Frost's back: one boss, one bar (Black Frost's life), her blows forwarded to it. When
 * it falls she mourns it, then fights on alone for vengeance with her own life, and at her end she kneels, entrusts her
 * weapons to who beat her and fades. Her fight is CryonaFight; in her realm ThroneFight leads it. On her throne in her
 * citadel only her sign sleeps, a creature of its own that never fights (EntityCryonaAsleep).
 */
public class EntityDreadQueen extends EntityDreadMob implements IAnimatedEntity, IVillagerFear, IAnimalFear {

	public static Animation ANIMATION_SPAWN = Animation.create(40);
	/** Her staff raised for a spell. */
	public static Animation ANIMATION_CAST = Animation.create(24);
	/** A cut of her blade from the side (its blow at tick 9). */
	public static Animation ANIMATION_SLASH = Animation.create(16);
	/** A thrust of her blade (its blow at tick 9). */
	public static Animation ANIMATION_THRUST = Animation.create(18);
	/** Crouch, leap with her blade high, the blow down on landing (at tick 28). */
	public static Animation ANIMATION_SLAM = Animation.create(36);
	private static final EntityDataAccessor<Boolean> KNEELING = SynchedEntityData.defineId(EntityDreadQueen.class, EntityDataSerializers.BOOLEAN);
	private final ServerBossEvent bossInfo = new ServerBossEvent(java.util.UUID.randomUUID(), Component.translatable("iceandfire_fabric.boss.cryona"),
		BossEvent.BossBarColor.BLUE, BossEvent.BossBarOverlay.NOTCHED_10);
	/** Her ice shield's bar, under hers. */
	private final ServerBossEvent shieldInfo = new ServerBossEvent(java.util.UUID.randomUUID(), Component.translatable("iceandfire_fabric.boss.cryona_shield"),
		BossEvent.BossBarColor.WHITE, BossEvent.BossBarOverlay.PROGRESS);
	/** Her life: a boss's (her old config value no longer counts). */
	public static final double HEALTH = 3000;
	/** Her ice shield (CryonaFight: whole when she rises alone, mending when left alone). */
	private float shield;
	private long shieldHitAt = -100;
	private float shieldHitAmount;
	private int animationTick;
	private Animation currentAnimation;
	private boolean enraged;
	/** The part of her fight she is in (CryonaFight.MOUNTED...), kept with her. */
	private int phase;
	private int fightMarks;
	/** Where her throne is (the citadel she rules), if she has one. */
	private @Nullable BlockPos throne;
	/** Asleep on her throne in an older world (she slept there herself then): her sign takes her place. */
	private boolean oldSleeper;
	/** On the client: how far down she kneels (0 standing, 20 kneeling), and a tick ago. */
	public float kneelProgress;
	public float prevKneelProgress;
	/** Her bar shows her mourning now (CryonaFight.mourning). */
	private boolean mourningBar;

	public EntityDreadQueen(EntityType t, Level worldIn) {
		super(t, worldIn);
		this.xpReward = 500;
		this.bossInfo.setDarkenScreen(true);
		this.shieldInfo.setVisible(false);
		this.getAttribute(Attributes.MAX_HEALTH).setBaseValue(HEALTH);
		this.setHealth(this.getMaxHealth());
	}

	public float getShield() {
		return shield;
	}

	public void setShield(float shield) {
		this.shield = Math.max(0F, shield);
	}

	@Override
	protected void registerGoals() {
		this.goalSelector.addGoal(1, new FloatGoal(this));
		this.goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.0D, true));
		this.goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 1.0D));
		this.goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 8.0F));
		this.goalSelector.addGoal(7, new RandomLookAroundGoal(this));
		this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
		this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, 10, true, false, (entity, level) -> DragonUtils.canHostilesTarget(entity)));
		this.targetSelector.addGoal(3, new DreadAITargetNonDread(this, LivingEntity.class, false, (LivingEntity entity) -> entity instanceof LivingEntity && DragonUtils.canHostilesTarget(entity)));
	}

	public static AttributeSupplier.Builder bakeAttributes() {
		return Mob.createMobAttributes()
			.add(Attributes.MAX_HEALTH, IafConfig.dreadQueenMaxHealth)
			.add(Attributes.MOVEMENT_SPEED, 0.3D)
			.add(Attributes.ATTACK_DAMAGE, 12.0D)
			.add(Attributes.FOLLOW_RANGE, 64.0D)
			.add(Attributes.KNOCKBACK_RESISTANCE, 1.0D)
			.add(Attributes.ARMOR, 20.0D);
	}

	@Override
	protected void defineSynchedData(SynchedEntityData.Builder builder) {
		super.defineSynchedData(builder);
		builder.define(KNEELING, false);
	}

	/** Down on one knee (mourning, spent, at her end). */
	public boolean isKneeling() {
		return this.entityData.get(KNEELING);
	}

	public void setKneeling(boolean kneeling) {
		this.entityData.set(KNEELING, kneeling);
	}

	public boolean isEnraged() {
		return enraged;
	}

	public int getPhase() {
		return phase;
	}

	/** Her one-time calls made in her fight (CryonaFight), kept with her. */
	public int getFightMarks() {
		return fightMarks;
	}

	public void setFightMarks(int marks) {
		this.fightMarks = marks;
	}

	public void setPhase(int phase) {
		this.phase = phase;
	}

	public @Nullable BlockPos getThrone() {
		return throne;
	}

	public void setThrone(@Nullable BlockPos throne) {
		this.throne = throne;
	}

	@Override
	public void addAdditionalSaveData(ValueOutput compound) {
		super.addAdditionalSaveData(compound);
		compound.putBoolean("Enraged", this.enraged);
		compound.putInt("Phase", this.phase);
		compound.putInt("FightMarks", this.fightMarks);
		compound.putFloat("IceShield", this.shield);
		if (this.throne != null) {
			compound.putLong("Throne", this.throne.asLong());
		}
	}

	@Override
	public void readAdditionalSaveData(ValueInput compound) {
		super.readAdditionalSaveData(compound);
		this.oldSleeper = compound.getBooleanOr("Dormant", false);
		this.enraged = compound.getBooleanOr("Enraged", false);
		// (a scene cut by a save starts the part it led to)
		int saved = compound.getIntOr("Phase", 0);
		this.phase = saved == CryonaFight.FALLING ? CryonaFight.ALONE : saved == CryonaFight.DYING ? CryonaFight.ALONE : saved;
		long at = compound.getLongOr("Throne", Long.MIN_VALUE);
		this.throne = at == Long.MIN_VALUE ? null : BlockPos.of(at);
		this.shield = compound.getFloatOr("IceShield", 0F);
		this.fightMarks = compound.getIntOr("FightMarks", 0);
		if (this.getAttribute(Attributes.MAX_HEALTH).getBaseValue() != HEALTH) {
			// a Cryona of an older world: her life keeps its share
			float share = this.getHealth() / this.getMaxHealth();
			this.getAttribute(Attributes.MAX_HEALTH).setBaseValue(HEALTH);
			this.setHealth(this.getMaxHealth() * share);
		}
	}

	@Override
	public void tick() {
		super.tick();
		if (this.level().isClientSide()) {
			this.prevKneelProgress = this.kneelProgress;
			this.kneelProgress = Math.max(0F, Math.min(20F, this.kneelProgress + (this.isKneeling() ? 2F : -2.5F)));
		}
	}

	@Override
	public void aiStep() {
		super.aiStep();
		if (!(this.level() instanceof ServerLevel level)) {
			return;
		}
		if (this.oldSleeper) {
			this.oldSleeper = false;
			if (!ThroneFight.inRealm(this)) {
				EntityCryonaAsleep.takeOver(level, this);
				return;
			}
		}
		float mourning = CryonaFight.mourning(this);
		if (mourning >= 0F) {
			// her mourning: its bar is her fury growing (full, she rises), never her life going down
			if (!this.mourningBar) {
				this.mourningBar = true;
				this.bossInfo.setName(Component.translatable("iceandfire_fabric.boss.cryona_fury"));
				this.bossInfo.setColor(BossEvent.BossBarColor.RED);
			}
			this.bossInfo.setProgress(1F - mourning);
			this.bossInfo.setVisible(!com.iafabric.boss.ThroneFight.waiting(this));
			this.shieldInfo.setVisible(false);
			if (!ThroneFight.inRealm(this)) {
				CryonaFight.of(this).tick(level);
			}
			return;
		}
		if (this.mourningBar) {
			this.mourningBar = false;
			this.bossInfo.setName(Component.translatable("iceandfire_fabric.boss.cryona"));
			this.bossInfo.setColor(BossEvent.BossBarColor.BLUE);
		}
		this.bossInfo.setProgress(this.getHealth() / this.getMaxHealth());
		// one bar: Black Frost's while she rides it, hers once she fights alone (and her shield's under it)
		boolean shown = !this.isPassenger() && this.phase != CryonaFight.FALLING && !com.iafabric.boss.ThroneFight.waiting(this);
		this.bossInfo.setVisible(shown);
		this.shieldInfo.setProgress(Math.min(1F, this.shield / CryonaFight.SHIELD_MAX));
		this.shieldInfo.setVisible(shown && this.shield > 0 && this.phase != CryonaFight.DYING);
		if (!ThroneFight.inRealm(this)) {
			// (in her realm ThroneFight leads her fight)
			CryonaFight.of(this).tick(level);
		}
	}

	/** Alone, for vengeance: faster, never resting. */
	public void enrage(ServerLevel level) {
		enraged = true;
		this.addEffect(new MobEffectInstance(MobEffects.SPEED, MobEffectInstance.INFINITE_DURATION, 0, false, false));
		level.playSound(null, this.blockPosition(), SoundEvents.ENDER_DRAGON_GROWL, SoundSource.HOSTILE, 2.0F, 1.6F);
	}

	/** Three skulls of ice, spread, at her foe. */
	public void throwSkulls(ServerLevel level, LivingEntity target) {
		for (int i = -1; i <= 1; i++) {
			EntityDreadLichSkull skull = new EntityDreadLichSkull(IafEntityRegistry.DREAD_LICH_SKULL, level, this, enraged ? 9 : 7);
			skull.setPos(this.getX(), this.getEyeY(), this.getZ());
			double dx = target.getX() - this.getX();
			double dy = target.getY(0.5) - skull.getY();
			double dz = target.getZ() - this.getZ();
			Vec3 side = new Vec3(-dz, 0, dx).normalize().scale(i * 1.5);
			skull.shoot(dx + side.x, dy + Math.sqrt(dx * dx + dz * dz) * 0.1, dz + side.z, 1.3F, 2.0F);
			level.addFreshEntity(skull);
		}
		this.playSound(SoundEvents.EVOKER_CAST_SPELL, 1.5F, 0.6F);
	}

	/**
	 * Before the duel, in her realm, she is spoken to: what she asks, as at the altar (its card). (Still there, without
	 * a mind of her own; once the duel begins, a shield or a bow aimed at her is used as ever.)
	 */
	@Override
	public InteractionResult mobInteract(Player player, @NotNull InteractionHand hand) {
		if (!ThroneFight.inRealm(this) || this.isPassenger() || !this.isNoAi()) {
			return super.mobInteract(player, hand);
		}
		if (player instanceof ServerPlayer serverPlayer && ThroneFight.waiting(this)) {
			ThroneFight.openAltar(serverPlayer);
		}
		return InteractionResult.SUCCESS;
	}

	@Override
	public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
		if (this.phase == CryonaFight.FALLING || this.phase == CryonaFight.DYING || com.iafabric.boss.ThroneFight.waiting(this)) {
			// (mourning, at her end, before the duel is accepted: nothing touches her; a blow in her mourning shows it)
			if (this.phase == CryonaFight.FALLING) {
				CryonaFight.of(this).untouched(level, source);
			}
			return false;
		}
		if (this.getVehicle() instanceof EntityDragonBase frost && frost.isBlackFrost() && !frost.isModelDead()) {
			// on Black Frost's back she and it are one: the blow is its
			return frost.hurtServer(level, source, amount);
		}
		float taken = CryonaFight.damageTaken(this, amount);
		if (taken > 0) {
			CryonaFight.of(this).struck(level);
		}
		if (this.shield > 0 && taken > 0 && !com.iafabric.boss.BossLimits.unlimited(source)) {
			// her ice shield takes the blow first (a blow at once after another: only what is more)
			long now = level.getGameTime();
			float blow = taken;
			if (now - this.shieldHitAt < 10) {
				if (taken <= this.shieldHitAmount) {
					return false;
				}
				blow = taken - this.shieldHitAmount;
			}
			this.shieldHitAt = now;
			this.shieldHitAmount = taken;
			float absorbed = Math.min(this.shield, blow);
			this.shield -= absorbed;
			blow -= absorbed;
			level.sendParticles(new net.minecraft.core.particles.BlockParticleOption(ParticleTypes.BLOCK, net.minecraft.world.level.block.Blocks.ICE.defaultBlockState()),
				this.getX(), this.getY() + 1.2, this.getZ(), 10, 0.4, 0.6, 0.4, 0.1);
			// (its ring once in a while, not at each of many blows)
			com.iafabric.vfx.Vfx.once(level, this.position(), SoundEvents.GLASS_HIT, 1.1F, 0.8F, 8, 10);
			if (this.shield <= 0) {
				this.shield = 0;
				level.playSound(null, this.blockPosition(), com.iafabric.misc.IafSoundRegistry.CRYONA_SHIELD_BREAK, SoundSource.HOSTILE, 2.2F, 1F);
				level.sendParticles(new net.minecraft.core.particles.BlockParticleOption(ParticleTypes.BLOCK, net.minecraft.world.level.block.Blocks.BLUE_ICE.defaultBlockState()),
					this.getX(), this.getY() + 1.2, this.getZ(), 60, 0.8, 1, 0.8, 0.2);
				com.iafabric.boss.ThroneFight.tell(this, "iceandfire_fabric.cryona.shield_broken", net.minecraft.ChatFormatting.GOLD);
				com.iafabric.pixie.DuelTalk.moment(this, com.iafabric.pixie.DuelTalk.Moment.SHIELD);
			}
			if (blow <= 0) {
				return true;
			}
			taken = blow;
		}
		boolean hurt = super.hurtServer(level, source, taken);
		if (hurt && this.isAlive() && this.getHealth() <= 1F && this.phase != CryonaFight.GONE) {
			// her end: she kneels and entrusts her weapons, then fades (CryonaFight)
			CryonaFight.of(this).fall(level, source);
		}
		return hurt;
	}

	/** She loses at most a hundredth of her life in a second, whatever strikes her (BossLimits). */
	@Override
	protected void actuallyHurt(ServerLevel level, DamageSource source, float amount) {
		float before = this.getHealth();
		super.actuallyHurt(level, source, amount);
		if (!com.iafabric.boss.BossLimits.unlimited(source)) {
			float lost = before - this.getHealth();
			float allowed = com.iafabric.boss.BossLimits.allow(this, lost, level.getGameTime());
			if (allowed < lost) {
				this.setHealth(before - allowed);
				level.sendParticles(ParticleTypes.SNOWFLAKE, this.getX(), this.getY() + 1.2, this.getZ(), 6, 0.3, 0.5, 0.3, 0.02);
			}
		}
	}

	@Override
	public void setHealth(float health) {
		// her end is a scene of its own: a killing blow leaves her a breath of life for it
		if (health <= 0F && !this.level().isClientSide() && this.phase != CryonaFight.GONE && this.phase != CryonaFight.UNKNOWN) {
			health = 1F;
		}
		super.setHealth(health);
	}

	@Override
	public boolean doHurtTarget(ServerLevel level, Entity target) {
		// in her fight every blow of hers is shown first (CryonaFight); no plain blows
		if (CryonaFight.choreographed(this)) {
			return false;
		}
		return super.doHurtTarget(level, target);
	}

	@Override
	public void die(@NotNull DamageSource cause) {
		super.die(cause);
		this.bossInfo.removeAllPlayers();
		this.shieldInfo.removeAllPlayers();
		if (this.level() instanceof ServerLevel level) {
			for (EntityDreadMob minion : level.getEntitiesOfClass(EntityDreadMob.class, this.getBoundingBox().inflate(64), m -> this.getUUID().equals(m.getCommanderId()))) {
				minion.kill(level);
			}
		}
	}

	@Override
	protected void customServerAiStep(ServerLevel level) {
		super.customServerAiStep(level);
		this.bossInfo.setProgress(this.getHealth() / this.getMaxHealth());
	}

	@Override
	public void setCustomName(Component name) {
		super.setCustomName(name);
	}

	@Override
	public void startSeenByPlayer(@NotNull ServerPlayer player) {
		super.startSeenByPlayer(player);
		this.bossInfo.addPlayer(player);
		this.shieldInfo.addPlayer(player);
	}

	@Override
	public void stopSeenByPlayer(@NotNull ServerPlayer player) {
		super.stopSeenByPlayer(player);
		this.bossInfo.removePlayer(player);
		this.shieldInfo.removePlayer(player);
	}

	@Override
	@Nullable
	public SpawnGroupData finalizeSpawn(ServerLevelAccessor worldIn, DifficultyInstance difficultyIn, EntitySpawnReason reason, @Nullable SpawnGroupData spawnDataIn) {
		SpawnGroupData data = super.finalizeSpawn(worldIn, difficultyIn, reason, spawnDataIn);
		this.setAnimation(ANIMATION_SPAWN);
		this.populateDefaultEquipmentSlots(this.getRandom(), difficultyIn);
		return data;
	}

	@Override
	protected void populateDefaultEquipmentSlots(RandomSource random, DifficultyInstance difficulty) {
		super.populateDefaultEquipmentSlots(random, difficulty);
		this.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(IafItemRegistry.DREAD_QUEEN_SWORD));
		this.setItemSlot(EquipmentSlot.OFFHAND, new ItemStack(IafItemRegistry.DREAD_QUEEN_STAFF));
		// (she entrusts them herself at her end)
		this.setDropChance(EquipmentSlot.MAINHAND, 0.0F);
		this.setDropChance(EquipmentSlot.OFFHAND, 0.0F);
	}

	@Override
	public int getAnimationTick() {
		return animationTick;
	}

	@Override
	public void setAnimationTick(int tick) {
		animationTick = tick;
	}

	@Override
	public Animation getAnimation() {
		return currentAnimation;
	}

	@Override
	public void setAnimation(Animation animation) {
		currentAnimation = animation;
	}

	@Override
	public Animation[] getAnimations() {
		return new Animation[]{ANIMATION_SPAWN, ANIMATION_CAST, ANIMATION_SLASH, ANIMATION_THRUST, ANIMATION_SLAM};
	}

	@Override
	public boolean shouldAnimalsFear(Entity entity) {
		return true;
	}

	@Override
	public boolean shouldFear() {
		return true;
	}

	@Override
	public boolean isPersistenceRequired() {
		return true;
	}

	@Override
	public boolean removeWhenFarAway(double distanceToClosestPlayer) {
		return false;
	}
}

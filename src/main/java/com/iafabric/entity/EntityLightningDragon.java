package com.iafabric.entity;

import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.server.level.ServerLevel;
import com.iafabric.network.IafNetwork;
import com.iafabric.citadel.animation.Animation;
import com.iafabric.citadel.animation.IAnimatedEntity;
import com.iafabric.IafConfig;
import com.iafabric.IafMod;
import com.iafabric.entity.util.DragonUtils;
import com.iafabric.item.IafItemRegistry;
import com.iafabric.message.MessageDragonSyncFire;
import com.iafabric.misc.IafSoundRegistry;
import com.iafabric.misc.IafTagRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.pathfinder.PathType;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;

import net.minecraft.util.RandomSource;

public class EntityLightningDragon extends EntityDragonBase {

    public static final float[] growth_stage_1 = new float[]{1F, 3F};
    public static final float[] growth_stage_2 = new float[]{3F, 7F};
    public static final float[] growth_stage_3 = new float[]{7F, 12.5F};
    public static final float[] growth_stage_4 = new float[]{12.5F, 20F};
    public static final float[] growth_stage_5 = new float[]{20F, 30F};

    public static final Identifier FEMALE_LOOT = Identifier.fromNamespaceAndPath("iceandfire_fabric", "entities/dragon/lightning_dragon_female");
    public static final Identifier MALE_LOOT = Identifier.fromNamespaceAndPath("iceandfire_fabric", "entities/dragon/lightning_dragon_male");
    public static final Identifier SKELETON_LOOT = Identifier.fromNamespaceAndPath("iceandfire_fabric", "entities/dragon/lightning_dragon_skeleton");
    private static final EntityDataAccessor<Boolean> HAS_LIGHTNING_TARGET = SynchedEntityData.defineId(EntityLightningDragon.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Float> LIGHTNING_TARGET_X = SynchedEntityData.defineId(EntityLightningDragon.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> LIGHTNING_TARGET_Y = SynchedEntityData.defineId(EntityLightningDragon.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> LIGHTNING_TARGET_Z = SynchedEntityData.defineId(EntityLightningDragon.class, EntityDataSerializers.FLOAT);

    public EntityLightningDragon(Level worldIn) {
        this(IafEntityRegistry.LIGHTNING_DRAGON, worldIn);
    }

    public EntityLightningDragon(EntityType<?> t, Level worldIn) {
        super(t, worldIn, DragonType.LIGHTNING, 1, 1 + IafConfig.dragonAttackDamage, IafConfig.dragonHealth * 0.04, IafConfig.dragonHealth, 0.15F, 0.4F);
        this.setPathfindingMalus(PathType.FIRE_IN_NEIGHBOR, 0.0F);
        this.setPathfindingMalus(PathType.FIRE, 0.0F);
        this.setPathfindingMalus(PathType.LAVA, 8.0F);
        ANIMATION_SPEAK = Animation.create(20);
        ANIMATION_BITE = Animation.create(35);
        ANIMATION_SHAKEPREY = Animation.create(65);
        ANIMATION_TAILWHACK = Animation.create(40);
        ANIMATION_FIRECHARGE = Animation.create(30);
        ANIMATION_WINGBLAST = Animation.create(50);
        ANIMATION_ROAR = Animation.create(40);
        ANIMATION_EPIC_ROAR = Animation.create(60);
        this.growth_stages = new float[][]{growth_stage_1, growth_stage_2, growth_stage_3, growth_stage_4, growth_stage_5};
    }

    public void onStruckByLightning(LightningBolt lightningBolt) {
        this.heal(15F);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(HAS_LIGHTNING_TARGET, false);
        builder.define(LIGHTNING_TARGET_X, 0.0F);
        builder.define(LIGHTNING_TARGET_Y, 0.0F);
        builder.define(LIGHTNING_TARGET_Z, 0.0F);
    }

    @Override
    public int getStartMetaForType() {
        return 8;
    }

    @Override
    protected boolean shouldTarget(Entity entity) {
        if (entity instanceof EntityDragonBase && !this.isTame()) {
            return entity.getType() != this.getType() && this.getBbWidth() >= entity.getBbWidth() && !((EntityDragonBase) entity).isMobDead();
        }
        return entity instanceof Player || DragonUtils.isDragonTargetable(entity, IafTagRegistry.LIGHTNING_DRAGON_TARGETS) || !this.isTame() && DragonUtils.isVillager(entity);
    }

    @Override
    public boolean isTimeToWake() {
        return !this.level().isBrightOutside() || this.getCommand() == 2;
    }

    @Override
    protected void registerGoals() {
        super.registerGoals();
        this.goalSelector.addGoal(0, new FloatGoal(this));
    }

    @Override
    public String getVariantName(int variant) {
        switch (variant) {
            default:
                return "electric_";
            case 1:
                return "amythest_";
            case 2:
                return "copper_";
            case 3:
                return "black_";
        }
    }
    @Override
    public boolean isInvulnerableTo(ServerLevel level, DamageSource i) {
        if (i.is(net.minecraft.world.damagesource.DamageTypes.LIGHTNING_BOLT)) {
            this.heal(15F);
            this.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 20, 1));
            return true;
        }
        return super.isInvulnerableTo(level, i);
    }
    @Override
    public Item getVariantScale(int variant) {
        switch (variant) {
            default:
                return IafItemRegistry.DRAGONSCALES_ELECTRIC;
            case 1:
                return IafItemRegistry.DRAGONSCALES_AMYTHEST;
            case 2:
                return IafItemRegistry.DRAGONSCALES_COPPER;
            case 3:
                return IafItemRegistry.DRAGONSCALES_BLACK;
        }
    }

    @Override
    public Item getVariantEgg(int variant) {
        switch (variant) {
            default:
                return IafItemRegistry.DRAGONEGG_ELECTRIC;
            case 1:
                return IafItemRegistry.DRAGONEGG_AMYTHEST;
            case 2:
                return IafItemRegistry.DRAGONEGG_COPPER;
            case 3:
                return IafItemRegistry.DRAGONEGG_BLACK;
        }
    }

    public void setHasLightningTarget(boolean lightning_target) {
        this.entityData.set(HAS_LIGHTNING_TARGET, lightning_target);
    }

    public boolean hasLightningTarget() {
        return this.entityData.get(HAS_LIGHTNING_TARGET).booleanValue();
    }

    public void setLightningTargetVec(float x, float y, float z) {
        this.entityData.set(LIGHTNING_TARGET_X, x);
        this.entityData.set(LIGHTNING_TARGET_Y, y);
        this.entityData.set(LIGHTNING_TARGET_Z, z);
    }

    public float getLightningTargetX() {
        return this.entityData.get(LIGHTNING_TARGET_X);
    }

    public float getLightningTargetY() {
        return this.entityData.get(LIGHTNING_TARGET_Y);
    }

    public float getLightningTargetZ() {
        return this.entityData.get(LIGHTNING_TARGET_Z);
    }

    @Override
    public Item getSummoningCrystal() {
        return IafItemRegistry.SUMMONING_CRYSTAL_LIGHTNING;
    }


    @Override
    public boolean doHurtTarget(ServerLevel level, Entity entityIn) {
        this.getLookControl().setLookAt(entityIn, 30.0F, 30.0F);
        if (!this.isPlayingAttackAnimation()) {
            switch (groundAttack) {
                case BITE:
                    this.setAnimation(ANIMATION_BITE);
                    break;
                case TAIL_WHIP:
                    this.setAnimation(ANIMATION_TAILWHACK);
                    break;
                case SHAKE_PREY:
                    boolean flag = false;
                    if (RandomSource.create().nextInt(2) == 0 && isDirectPathBetweenPoints(this, this.position().add(0, this.getBbHeight() / 2, 0), entityIn.position().add(0, entityIn.getBbHeight() / 2, 0)) &&
                        entityIn.getBbWidth() < this.getBbWidth() * 0.5F && this.getControllingPassenger() == null && this.getDragonStage() > 1 && !(entityIn instanceof EntityDragonBase) && !DragonUtils.isAnimaniaMob(entityIn)) {
                        this.setAnimation(ANIMATION_SHAKEPREY);
                        flag = true;
                        entityIn.startRiding(this);
                    }
                    if (!flag) {
                        groundAttack = IafDragonAttacks.Ground.BITE;
                        this.setAnimation(ANIMATION_BITE);
                    }
                    break;
                case WING_BLAST:
                    this.setAnimation(ANIMATION_WINGBLAST);
                    break;
            }
        }
        return false;
    }

    @Override
    public void aiStep() {
        super.aiStep();
        LivingEntity attackTarget = this.getTarget();
        if (!level().isClientSide() && attackTarget != null) {
            if (this.getBoundingBox().inflate(2.5F + this.getRenderSize() * 0.33F, 2.5F + this.getRenderSize() * 0.33F, 2.5F + this.getRenderSize() * 0.33F).intersects(attackTarget.getBoundingBox())) {
                doHurtTarget((ServerLevel) this.level(), attackTarget);
            }
            if (this.groundAttack == IafDragonAttacks.Ground.FIRE && (usingGroundAttack || this.onGround())) {
                shootFireAtMob(attackTarget);
            }
            if (this.airAttack == IafDragonAttacks.Air.TACKLE && !usingGroundAttack && this.distanceToSqr(attackTarget) < 100) {
                double difX = attackTarget.getX() - this.getX();
                double difY = attackTarget.getY() + attackTarget.getBbHeight() - this.getY();
                double difZ = attackTarget.getZ() - this.getZ();
                this.setDeltaMovement(this.getDeltaMovement().add(difX * 0.1D, difY * 0.1D, difZ * 0.1D));
                if (this.getBoundingBox().inflate(1 + this.getRenderSize() * 0.5F, 1 + this.getRenderSize() * 0.5F, 1 + this.getRenderSize() * 0.5F).intersects(attackTarget.getBoundingBox())) {
                    if (this.level() instanceof ServerLevel serverLevel) {
                        doHurtTarget(serverLevel, attackTarget);
                    }
                    usingGroundAttack = true;
                    randomizeAttacks();
                    setFlying(false);
                    setHovering(false);
                }
            }
        }
        if(!isBreathingFire()){
            this.setHasLightningTarget(false);
        }
    }


    @Override
    protected void breathFireAtPos(BlockPos burningTarget) {
        if (this.isBreathingFire()) {
            if (this.isActuallyBreathingFire()) {
                setYRot(yBodyRot);
                if (this.fireTicks % 7 == 0) {
                    this.playSound(IafSoundRegistry.LIGHTNINGDRAGON_BREATH, 4, 1);
                }
                stimulateFire(burningTarget.getX() + 0.5F, burningTarget.getY() + 0.5F, burningTarget.getZ() + 0.5F, 1);
            }
        } else {
            this.setBreathingFire(true);
        }
    }

    @Override
    public void riderShootFire(Entity controller) {
        if (this.getRandom().nextInt(5) == 0 && !this.isBaby()) {
            if (this.getAnimation() != ANIMATION_FIRECHARGE) {
                this.setAnimation(ANIMATION_FIRECHARGE);
            } else if (this.getAnimationTick() == 20) {
                setYRot(yBodyRot);
                Vec3 headVec = this.getHeadPosition();
                this.playSound(IafSoundRegistry.LIGHTNINGDRAGON_BREATH_CRACKLE, 4, 1);
                double d2 = controller.getLookAngle().x;
                double d3 = controller.getLookAngle().y;
                double d4 = controller.getLookAngle().z;
                float inaccuracy = 1.0F;
                d2 = d2 + this.random.nextGaussian() * 0.007499999832361937D * inaccuracy;
                d3 = d3 + this.random.nextGaussian() * 0.007499999832361937D * inaccuracy;
                d4 = d4 + this.random.nextGaussian() * 0.007499999832361937D * inaccuracy;
                EntityDragonLightningCharge entitylargefireball = new EntityDragonLightningCharge(
                    IafEntityRegistry.LIGHTNING_DRAGON_CHARGE, level(), this, d2, d3, d4);
                float size = this.isBaby() ? 0.4F : this.shouldDropLoot() ? 1.3F : 0.8F;
                entitylargefireball.setPos(headVec.x, headVec.y, headVec.z);
                if (!level().isClientSide()) {
                    level().addFreshEntity(entitylargefireball);
                }
            }
        } else {
            if (this.isBreathingFire()) {
                if (this.isActuallyBreathingFire()) {
                    setYRot(yBodyRot);
                    if (this.fireTicks % 7 == 0) {
                        this.playSound(IafSoundRegistry.LIGHTNINGDRAGON_BREATH, 4, 1);
                    }
                    HitResult mop = rayTraceRider(controller, 10 * this.getDragonStage(), 1.0F);
                    if (mop != null) {
                        stimulateFire(mop.getLocation().x, mop.getLocation().y, mop.getLocation().z, 1);
                    }
                }
            } else {
                this.setBreathingFire(true);
            }
        }
    }

    @Override
    protected Item getBloodItem() {
        return IafItemRegistry.LIGHTNING_DRAGON_BLOOD;
    }

    @Override
    protected ItemLike getHeartItem() {
        return IafItemRegistry.LIGHTNING_DRAGON_HEART;
    }

    @Override
    public Identifier getDeadLootTable() {
        if (this.getDeathStage() >= (this.getAgeInDays() / 5) / 2) {
            return SKELETON_LOOT;
        } else {
            // (only a grown female's body may hold an egg)
            return laysEggs() ? FEMALE_LOOT : MALE_LOOT;
        }
    }

    private void shootFireAtMob(LivingEntity entity) {
        if (this.usingGroundAttack && this.groundAttack == IafDragonAttacks.Ground.FIRE || !this.usingGroundAttack && (this.airAttack == IafDragonAttacks.Air.SCORCH_STREAM || this.airAttack == IafDragonAttacks.Air.HOVER_BLAST)) {
            if (this.usingGroundAttack && this.getRandom().nextInt(5) == 0 || !this.usingGroundAttack && this.airAttack == IafDragonAttacks.Air.HOVER_BLAST) {
                if (this.getAnimation() != ANIMATION_FIRECHARGE) {
                    this.setAnimation(ANIMATION_FIRECHARGE);
                } else if (this.getAnimationTick() == 20) {
                    setYRot(yBodyRot);
                    Vec3 headVec = this.getHeadPosition();
                    double d2 = entity.getX() - headVec.x;
                    double d3 = entity.getY() - headVec.y;
                    double d4 = entity.getZ() - headVec.z;
                    float inaccuracy = 1.0F;
                    d2 = d2 + this.random.nextGaussian() * 0.007499999832361937D * inaccuracy;
                    d3 = d3 + this.random.nextGaussian() * 0.007499999832361937D * inaccuracy;
                    d4 = d4 + this.random.nextGaussian() * 0.007499999832361937D * inaccuracy;
                    this.playSound(IafSoundRegistry.LIGHTNINGDRAGON_BREATH, 4, 1);
                    EntityDragonLightningCharge entitylargefireball = new EntityDragonLightningCharge(
                        IafEntityRegistry.LIGHTNING_DRAGON_CHARGE, level(), this, d2, d3, d4);
                    float size = this.isBaby() ? 0.4F : this.shouldDropLoot() ? 1.3F : 0.8F;
                    entitylargefireball.setPos(headVec.x, headVec.y, headVec.z);
                    if (!level().isClientSide()) {
                        level().addFreshEntity(entitylargefireball);
                    }
                    if (!entity.isAlive() || entity == null) {
                        this.setBreathingFire(false);
                    }
                    this.randomizeAttacks();
                }
            } else {
                if (this.isBreathingFire()) {
                    if (this.isActuallyBreathingFire()) {
                        setYRot(yBodyRot);
                        if (this.tickCount % 5 == 0) {
                            this.playSound(IafSoundRegistry.LIGHTNINGDRAGON_BREATH, 4, 1);
                        }
                        stimulateFire(entity.getX(), entity.getY(), entity.getZ(), 1);
                        if (!entity.isAlive() || entity == null) {
                            this.setBreathingFire(false);
                            this.randomizeAttacks();
                        }
                    }
                } else {
                    this.setBreathingFire(true);
                }
            }
        }
        this.lookAt(entity, 360, 360);
    }

    @Override
    public void stimulateFire(double burnX, double burnY, double burnZ, int syncType) {
        if (syncType == 1 && !level().isClientSide()) {
            //sync with client
            IafNetwork.sendToAll(new MessageDragonSyncFire(this.getId(), burnX, burnY, burnZ, 0));
        }
        if (syncType == 2 && level().isClientSide()) {
            //sync with server
            IafNetwork.sendToServer(new MessageDragonSyncFire(this.getId(), burnX, burnY, burnZ, 0));
        }
        if (syncType == 3 && !level().isClientSide()) {
            //sync with client, fire bomb
            IafNetwork.sendToAll(new MessageDragonSyncFire(this.getId(), burnX, burnY, burnZ, 5));
        }
        if (syncType == 4 && level().isClientSide()) {
            //sync with server, fire bomb
            IafNetwork.sendToServer(new MessageDragonSyncFire(this.getId(), burnX, burnY, burnZ, 5));
        }
        if (syncType > 2 && syncType < 6) {
            if (this.getAnimation() != ANIMATION_FIRECHARGE) {
                this.setAnimation(ANIMATION_FIRECHARGE);
            } else if (this.getAnimationTick() == 20) {
                setYRot(yBodyRot);
                Vec3 headVec = this.getHeadPosition();
                double d2 = burnX - headVec.x;
                double d3 = burnY - headVec.y;
                double d4 = burnZ - headVec.z;
                float inaccuracy = 1.0F;
                d2 = d2 + this.random.nextGaussian() * 0.007499999832361937D * inaccuracy;
                d3 = d3 + this.random.nextGaussian() * 0.007499999832361937D * inaccuracy;
                d4 = d4 + this.random.nextGaussian() * 0.007499999832361937D * inaccuracy;
                this.playSound(IafSoundRegistry.LIGHTNINGDRAGON_BREATH_CRACKLE, 4, 1);
                EntityDragonLightningCharge entitylargefireball = new EntityDragonLightningCharge(
                    IafEntityRegistry.LIGHTNING_DRAGON_CHARGE, level(), this, d2, d3, d4);
                float size = this.isBaby() ? 0.4F : this.shouldDropLoot() ? 1.3F : 0.8F;
                entitylargefireball.setPos(headVec.x, headVec.y, headVec.z);
                if (!level().isClientSide()) {
                    level().addFreshEntity(entitylargefireball);
                }
            }
            return;
        }
        this.burnParticleX = burnX;
        this.burnParticleY = burnY;
        this.burnParticleZ = burnZ;
        if (level().isClientSide()) {
            // seen breathing: its puffs come from its mouth (DragonBreathFx), with the bolt
            this.breathSeenTick = this.tickCount;
        }
        Vec3 headPos = getHeadPosition();
        double d2 = burnX - headPos.x;
        double d3 = burnY - headPos.y;
        double d4 = burnZ - headPos.z;
        float particleScale = Mth.clamp(this.getRenderSize() * 0.08F, 0.55F, 3F);
        double distance = Math.max(2.5F * this.distanceToSqr(burnX, burnY, burnZ), 0);
        double conqueredDistance = burnProgress / 40D * distance;
        int increment = (int) Math.ceil(conqueredDistance / 100);
        for (int i = 0; i < conqueredDistance; i += increment) {
            double progressX = headPos.x + d2 * (i / (float) distance);
            double progressY = headPos.y + d3 * (i / (float) distance);
            double progressZ = headPos.z + d4 * (i / (float) distance);
            if (canPositionBeSeen(progressX, progressY, progressZ)) {
                setHasLightningTarget(true);
                setLightningTargetVec((float)burnX, (float)burnY, (float)burnZ);
            } else {
                if (!level().isClientSide()) {
                    HitResult result = this.level().clip(new ClipContext(
                        new Vec3(this.getX(), this.getY() + this.getEyeHeight(), this.getZ()),
                        new Vec3(progressX, progressY, progressZ), ClipContext.Block.COLLIDER,
                        ClipContext.Fluid.NONE, this));
                    BlockPos pos = BlockPos.containing(result.getLocation());
                    IafDragonDestructionManager.destroyAreaLightning(level(), pos, this);
                    setHasLightningTarget(true);
                    setLightningTargetVec((float) result.getLocation().x, (float) result.getLocation().y, (float) result.getLocation().z);
                }
            }
        }
        if (burnProgress >= 40D && canPositionBeSeen(burnX, burnY, burnZ)) {
            double spawnX = burnX + (random.nextFloat() * 3.0) - 1.5;
            double spawnY = burnY + (random.nextFloat() * 3.0) - 1.5;
            double spawnZ = burnZ + (random.nextFloat() * 3.0) - 1.5;
            setHasLightningTarget(true);
            setLightningTargetVec((float) spawnX, (float) spawnY, (float) spawnZ);
            if (!level().isClientSide()) {
                IafDragonDestructionManager.destroyAreaLightning(level(), BlockPos.containing(spawnX, spawnY, spawnZ), this);
            }
        }
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return this.isTeen() ? IafSoundRegistry.LIGHTNINGDRAGON_TEEN_IDLE : this.shouldDropLoot() ? IafSoundRegistry.LIGHTNINGDRAGON_ADULT_IDLE : IafSoundRegistry.LIGHTNINGDRAGON_CHILD_IDLE;
    }

    @Override
    protected SoundEvent getHurtSound(@NotNull DamageSource damageSourceIn) {
        return this.isTeen() ? IafSoundRegistry.LIGHTNINGDRAGON_TEEN_HURT : this.shouldDropLoot() ? IafSoundRegistry.LIGHTNINGDRAGON_ADULT_HURT : IafSoundRegistry.LIGHTNINGDRAGON_CHILD_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return this.isTeen() ? IafSoundRegistry.LIGHTNINGDRAGON_TEEN_DEATH : this.shouldDropLoot() ? IafSoundRegistry.LIGHTNINGDRAGON_ADULT_DEATH : IafSoundRegistry.LIGHTNINGDRAGON_CHILD_DEATH;
    }

    @Override
    public SoundEvent getRoarSound() {
        return this.isTeen() ? IafSoundRegistry.LIGHTNINGDRAGON_TEEN_ROAR : this.shouldDropLoot() ? IafSoundRegistry.LIGHTNINGDRAGON_ADULT_ROAR : IafSoundRegistry.LIGHTNINGDRAGON_CHILD_ROAR;
    }

    @Override
    public Animation[] getAnimations() {
        return new Animation[]{IAnimatedEntity.NO_ANIMATION, EntityDragonBase.ANIMATION_EAT, EntityDragonBase.ANIMATION_SPEAK, EntityDragonBase.ANIMATION_BITE, EntityDragonBase.ANIMATION_SHAKEPREY, EntityLightningDragon.ANIMATION_TAILWHACK, EntityLightningDragon.ANIMATION_FIRECHARGE, EntityLightningDragon.ANIMATION_WINGBLAST, EntityLightningDragon.ANIMATION_ROAR, EntityLightningDragon.ANIMATION_EPIC_ROAR};
    }

    @Override
    public boolean isFood(ItemStack stack) {
        return !stack.isEmpty() && stack.getItem() != null && stack.getItem() == IafItemRegistry.LIGHTNING_STEW;
    }

    @Override
    protected void spawnDeathParticles() {
        for (int k = 0; k < 3; ++k) {
            double d2 = this.random.nextGaussian() * 0.02D;
            double d0 = this.random.nextGaussian() * 0.02D;
            double d1 = this.random.nextGaussian() * 0.02D;
            if (level().isClientSide()) {
                this.level().addParticle(ParticleTypes.RAIN,
                    this.getX() + this.random.nextFloat() * this.getBbWidth() * 2.0F - this.getBbWidth(),
                    this.getY() + this.random.nextFloat() * this.getBbHeight(),
                    this.getZ() + this.random.nextFloat() * this.getBbWidth() * 2.0F - this.getBbWidth(), d2, d0, d1);
            }
        }
    }

    @Override
    protected void spawnBabyParticles() {
        IafMod.PROXY.dragonCough(this);
    }

    @Override
    protected ItemStack getSkull() {
        return new ItemStack(IafItemRegistry.DRAGON_SKULL_LIGHTNING);
    }


    @Override
    public Vec3 getHeadPosition() {
        //this.setDragonPitch(this.ticksExisted % 180 - 90);
        float sitProg = this.sitProgress * 0.005F;
        float deadProg = this.modelDeadProgress * -0.02F;
        float hoverProg = this.hoverProgress * 0.03F;
        float flyProg = Math.max(0, this.flyProgress * 0.01F);
        int tick = 0;
        if (this.getAnimationTick() < 10) {
            tick = this.getAnimationTick();
        } else if (this.getAnimationTick() > 50) {
            tick = 60 - this.getAnimationTick();
        } else {
            tick = 10;
        }
        float epicRoarProg = this.getAnimation() == ANIMATION_EPIC_ROAR && !this.isOrderedToSit() ? tick * 0.1F : 0;
        float sleepProg = this.sleepProgress * 0.025F;
        float pitchY = 0;
        float pitchAdjustment = 0;
        float pitchMinus = 0;
        float dragonPitch = -getDragonPitch();// -90 = down, 0 = straight, 90 = up
        if (this.isFlying() || this.isHovering()) {
            if (dragonPitch > 0) {
                pitchY = (dragonPitch / 90F) * 1.2F;
            } else {
                pitchY = (dragonPitch / 90F) * 3F;
            }
        }
        float flightXz = 1.0F + flyProg + hoverProg;
        float absPitch = Math.abs(dragonPitch) / 90F;//1 down/up, 0 straight
        float minXZ = dragonPitch > 20 ? (dragonPitch - 20) * 0.009F : 0;
        float xzMod = (0.58F - hoverProg * 0.45F + flyProg * 0.2F + absPitch * 0.3F - sitProg) * flightXz * getRenderSize();
        float xzModSine = xzMod * (Math.max(0.25F, Mth.cos((float) Math.toRadians(dragonPitch))) - minXZ);
        float headPosX = (float) (getX() + (xzModSine) * Mth.cos((float) ((yBodyRot + 90) * Math.PI / 180)));
        float headPosY = (float) (getY() + (0.7F + (sitProg * 5F) + hoverProg + deadProg + epicRoarProg + sleepProg + flyProg + pitchY) * getRenderSize() * 0.3F);
        float headPosZ = (float) (getZ() + (xzModSine) * Mth.sin((float) ((yBodyRot + 90) * Math.PI / 180)));
        return new Vec3(headPosX, headPosY, headPosZ);
    }
}

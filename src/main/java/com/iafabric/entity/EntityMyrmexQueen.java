package com.iafabric.entity;


import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import java.util.Optional;
import com.iafabric.entity.util.ItemListing;
import com.iafabric.compat.ValueInput;
import com.iafabric.compat.ValueOutput;
import com.iafabric.util.BlockMaterial;
import com.iafabric.citadel.animation.Animation;
import com.iafabric.IafConfig;
import com.iafabric.entity.ai.*;
import com.iafabric.entity.util.DragonUtils;
import com.iafabric.entity.util.MyrmexHive;
import com.iafabric.entity.util.MyrmexTrades;
import com.iafabric.world.gen.WorldGenMyrmexHive;
import com.google.common.base.Predicate;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.npc.VillagerTrades;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;

import org.jetbrains.annotations.Nullable;

public class EntityMyrmexQueen extends EntityMyrmexBase {

    public static final Animation ANIMATION_BITE = Animation.create(15);
    public static final Animation ANIMATION_STING = Animation.create(15);
    public static final Animation ANIMATION_EGG = Animation.create(20);
    public static final Animation ANIMATION_DIGNEST = Animation.create(45);
    public static final ResourceLocation DESERT_LOOT = new ResourceLocation("iceandfire_fabric", "entities/myrmex_queen_desert");
    public static final ResourceLocation JUNGLE_LOOT = new ResourceLocation("iceandfire_fabric", "entities/myrmex_queen_jungle");
    private static final ResourceLocation TEXTURE_DESERT = new ResourceLocation("iceandfire_fabric:textures/models/myrmex/myrmex_desert_queen.png");
    private static final ResourceLocation TEXTURE_JUNGLE = new ResourceLocation("iceandfire_fabric:textures/models/myrmex/myrmex_jungle_queen.png");
    private static final EntityDataAccessor<Boolean> HASMADEHOME = SynchedEntityData.defineId(EntityMyrmexQueen.class, EntityDataSerializers.BOOLEAN);
    private int eggTicks = 0;

    public EntityMyrmexQueen(EntityType<EntityMyrmexQueen> t, Level worldIn) {
        super(t, worldIn);
    }

    /** Drops from this mob's own loot table (variants have their own; Mob#getLootTable is final now). */
    @Override
    protected void dropFromLootTable(DamageSource source, boolean playerKilled) {
    	ServerLevel level = (ServerLevel) this.level();
        ResourceLocation table = this.getIafLootTable();
        if (table != null) {
            com.iafabric.compat.LootDrops.drop(this, source, playerKilled, table);
        }
    }

        @Nullable
    protected ResourceLocation getIafLootTable() {
        return isJungle() ? JUNGLE_LOOT : DESERT_LOOT;
    }

    @Override
    public int getExperienceReward() {
        return 20;
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(HASMADEHOME, Boolean.TRUE);
    }

    @Override
    protected ItemListing[] getLevel1Trades() {
        return isJungle() ? MyrmexTrades.JUNGLE_QUEEN.get(1) : MyrmexTrades.DESERT_QUEEN.get(1);
    }

    @Override
    protected ItemListing[] getLevel2Trades() {
        return isJungle() ? MyrmexTrades.JUNGLE_QUEEN.get(2) : MyrmexTrades.DESERT_QUEEN.get(2);
    }

    @Override
    public void setCustomName(Component name) {
        // loading a queen without a name sets none (null)
        if (name != null && this.getHive() != null) {
            if (!this.getHive().colonyName.equals(name.getString())) {
                this.getHive().colonyName = name.getString();
            }
        }
        super.setCustomName(name);
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tagTag) {
		ValueOutput tag = new ValueOutput(tagTag, this.level().registryAccess());
        super.addAdditionalSaveData(tagTag);
        tag.putInt("EggTicks", eggTicks);
        tag.putBoolean("MadeHome", this.hasMadeHome());

    }

    @Override
    public void readAdditionalSaveData(CompoundTag tagTag) {
		ValueInput tag = new ValueInput(tagTag, this.level().registryAccess());
        super.readAdditionalSaveData(tagTag);
        this.eggTicks = tag.getIntOr("EggTicks", 0);
        this.setMadeHome(tag.getBooleanOr("MadeHome", false));
    }

    public boolean hasMadeHome() {
        return this.entityData.get(HASMADEHOME).booleanValue();
    }

    public void setMadeHome(boolean madeHome) {
        this.entityData.set(HASMADEHOME, madeHome);
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (this.getAnimation() == ANIMATION_DIGNEST) {
            spawnGroundEffects(3);
        }
        if (this.getHive() != null) {
            this.getHive().tick(0, level());
        }

        if (hasMadeHome() && this.getGrowthStage() >= 2 && !this.canSeeSky()) {
            eggTicks++;
        } else if (this.canSeeSky()) {
            this.setAnimation(ANIMATION_DIGNEST);
            if (this.getAnimationTick() == 42) {
                int down = Math.max(15, this.blockPosition().getY() - 20 + this.getRandom().nextInt(10));
                BlockPos genPos = BlockPos.containing(this.getX(), down, this.getZ());
                {
                    WorldGenMyrmexHive hiveGen = new WorldGenMyrmexHive(true, this.isJungle(), null);
                    if (!level().isClientSide && level() instanceof ServerLevel) {
                        hiveGen.placeSmallGen((ServerLevel) level(), this.getRandom(), genPos);
                    }
                    this.setMadeHome(true);
                    this.moveTo(genPos.getX(), down, genPos.getZ(), 0, 0);
                    this.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, 30));
                    this.setHive(hiveGen.hive);
                    for (int i = 0; i < 3; i++) {
                        EntityMyrmexWorker worker = new EntityMyrmexWorker(IafEntityRegistry.MYRMEX_WORKER,
                            level());
                        worker.copyPosition(this);
                        worker.setHive(this.getHive());
                        worker.setJungleVariant(this.isJungle());
                        if (!level().isClientSide) {
                            level().addFreshEntity(worker);
                        }
                    }
                    return;
                }
            }
        }
        if (!level().isClientSide && eggTicks > IafConfig.myrmexPregnantTicks && this.getHive() == null || !level().isClientSide && this.getHive() != null && this.getHive().repopulate() && eggTicks > IafConfig.myrmexPregnantTicks) {
            float radius = -5.25F;
            float angle = (0.01745329251F * this.yBodyRot);
            double extraX = radius * Mth.sin((float) (Math.PI + angle));
            double extraZ = radius * Mth.cos(angle);
            BlockPos eggPos = BlockPos.containing(this.getX() + extraX, this.getY() + 0.75F, this.getZ() + extraZ);
            if (level().isEmptyBlock(eggPos)) {
                this.setAnimation(ANIMATION_EGG);
                if (this.getAnimationTick() == 10) {
                    EntityMyrmexEgg egg = new EntityMyrmexEgg(IafEntityRegistry.MYRMEX_EGG, this.level());
                    egg.setJungle(this.isJungle());
                    int caste = getRandomCaste(level(), this.getRandom(), getHive() == null || getHive().reproduces);
                    egg.setMyrmexCaste(caste);
                    egg.moveTo(this.getX() + extraX, this.getY() + 0.75F, this.getZ() + extraZ, 0, 0);
                    if (getHive() != null) {
                        egg.hiveUUID = this.getHive().hiveUUID;
                    }
                    if (!level().isClientSide) {
                        level().addFreshEntity(egg);
                    }
                    eggTicks = 0;
                }

            }


        }
        if (this.getAnimation() == ANIMATION_BITE && this.getTarget() != null && this.getAnimationTick() == 6) {
            this.playBiteSound();
            if (this.getAttackBounds().intersects(this.getTarget().getBoundingBox())) {
                this.getTarget().hurt(this.getTarget().damageSources().mobAttack(this), ((int) this.getAttribute(Attributes.ATTACK_DAMAGE).getValue()));
            }
        }
        if (this.getAnimation() == ANIMATION_STING && this.getAnimationTick() == 0) {
            this.playStingSound();
        }
        if (this.getAnimation() == ANIMATION_STING && this.getTarget() != null && this.getAnimationTick() == 6) {
            if (this.getAttackBounds().intersects(this.getTarget().getBoundingBox())) {
                LivingEntity attackTarget = this.getTarget();
                attackTarget.hurt(attackTarget.damageSources().mobAttack(this), ((int) this.getAttribute(Attributes.ATTACK_DAMAGE).getValue() * 2));
                attackTarget.addEffect(new MobEffectInstance(MobEffects.POISON, 200, 2));
                attackTarget.hurtMarked = true;
                float f = Mth.sqrt((float) (0.5 * 0.5 + 0.5 * 0.5));
                attackTarget.hurtMarked = true;
                attackTarget.setDeltaMovement(attackTarget.getDeltaMovement().multiply(0.5D, 1, 0.5D));
                attackTarget.setDeltaMovement(attackTarget.getDeltaMovement().add(-0.5 / f * 4, 1, -0.5 / f * 4));

                if (attackTarget.onGround()) {
                    attackTarget.setDeltaMovement(attackTarget.getDeltaMovement().add(0, 0.4, 0));
                }
            }
        }

    }

    @Override
    public boolean isInvulnerableTo(DamageSource source) {
    	ServerLevel level = this.level() instanceof ServerLevel serverLevel ? serverLevel : null;
        return super.isInvulnerableTo(source) || this.getAnimation() == ANIMATION_DIGNEST;
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(0, new MyrmexAITradePlayer(this));
        this.goalSelector.addGoal(0, new MyrmexAILookAtTradePlayer(this));
        this.goalSelector.addGoal(1, new MyrmexAIAttackMelee(this, 1.0D, true));
        this.goalSelector.addGoal(3, new MyrmexAIReEnterHive(this, 1.0D));
        this.goalSelector.addGoal(4, new MyrmexAIWanderHiveCenter(this, 1.0D));
        this.goalSelector.addGoal(5, new MyrmexQueenAIWander(this, 1D));
        this.goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 6.0F));
        this.goalSelector.addGoal(7, new RandomLookAroundGoal(this));
        this.targetSelector.addGoal(1, new MyrmexAIDefendHive(this));
        this.targetSelector.addGoal(2, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(3, new MyrmexAIAttackPlayers(this));
        this.targetSelector.addGoal(3, new NearestAttackableTargetGoal<LivingEntity>(this, LivingEntity.class, 10, true, true, entity -> entity != null && !EntityMyrmexBase.haveSameHive(EntityMyrmexQueen.this, entity) && DragonUtils.isAlive(entity) && !(entity instanceof Enemy)));

    }

    public void fall(float distance, float damageMultiplier) {
    }

    @Override
    public boolean isInHive() {
        if (getHive() != null) {
            for (BlockPos pos : getHive().getAllRooms()) {
                if (isCloseEnoughToTarget(MyrmexHive.getGroundedPos(getLevel(), pos), 300))
                    return true;
            }
        }
        return false;
    }

    @Override
    public boolean shouldMoveThroughHive() {
        return false;
    }

    public static AttributeSupplier.Builder bakeAttributes() {
        return Mob.createMobAttributes()
            //HEALTH
            .add(Attributes.MAX_HEALTH, 120D)
            //SPEED
            .add(Attributes.MOVEMENT_SPEED, 0.2D)
            //ATTACK
            .add(Attributes.ATTACK_DAMAGE, IafConfig.myrmexBaseAttackStrength * 3.5D)
            //FOLLOW RANGE
            .add(Attributes.FOLLOW_RANGE, 128.0D)
            //ARMOR
            .add(Attributes.ARMOR, 15.0D);
    }

    @Override
    public AttributeSupplier.Builder getConfigurableAttributes() {
        return bakeAttributes();
    }

    @Override
    public ResourceLocation getAdultTexture() {
        return isJungle() ? TEXTURE_JUNGLE : TEXTURE_DESERT;
    }

    @Override
    public float getModelScale() {
        return 1.75F;
    }

    @Override
    public int getCasteImportance() {
        return 3;
    }

    @Override
    public boolean shouldLeaveHive() {
        return false;
    }

    @Override
    public boolean shouldEnterHive() {
        return true;
    }

    @Override
    public boolean doHurtTarget(Entity entityIn) {
    	ServerLevel level = (ServerLevel) this.level();
        if (this.getGrowthStage() < 2) {
            return false;
        }
        if (this.getAnimation() != ANIMATION_STING && this.getAnimation() != ANIMATION_BITE) {
            this.setAnimation(this.getRandom().nextBoolean() ? ANIMATION_STING : ANIMATION_BITE);
            if (!this.level().isClientSide && this.getRandom().nextInt(3) == 0 && this.getItemInHand(InteractionHand.MAIN_HAND) != ItemStack.EMPTY) {
                this.spawnAtLocation(this.getItemInHand(InteractionHand.MAIN_HAND), 0);
                this.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
            }
            if (!this.getPassengers().isEmpty()) {
                for (Entity entity : this.getPassengers()) {
                    entity.stopRiding();
                }
            }
            return true;
        }
        return false;
    }

    @Override
    public boolean canMove() {
        return super.canMove() && this.hasMadeHome();
    }

    public void spawnGroundEffects(float size) {
        for (int i = 0; i < size * 3; i++) {
            for (int i1 = 0; i1 < 10; i1++) {
                double motionX = getRandom().nextGaussian() * 0.07D;
                double motionY = getRandom().nextGaussian() * 0.07D;
                double motionZ = getRandom().nextGaussian() * 0.07D;
                float radius = size * random.nextFloat();
                float angle = (0.01745329251F * this.yBodyRot) * 3.14F * random.nextFloat();
                double extraX = radius * Mth.sin((float) (Math.PI + angle));
                double extraY = 0.8F;
                double extraZ = radius * Mth.cos(angle);

                BlockState BlockState = this.level().getBlockState(new BlockPos(Mth.floor(this.getX() + extraX), Mth.floor(this.getY() + extraY) - 1, Mth.floor(this.getZ() + extraZ)));
                if (!BlockMaterial.AIR.is(BlockState)) {
                    if (level().isClientSide) {
                        level().addParticle(new BlockParticleOption(ParticleTypes.BLOCK, BlockState), true,this.getX() + extraX, this.getY() + extraY, this.getZ() + extraZ, motionX, motionY, motionZ);
                    }
                }
            }
        }
    }

    @Override
    public Animation[] getAnimations() {
        return new Animation[]{ANIMATION_PUPA_WIGGLE, ANIMATION_BITE, ANIMATION_STING, ANIMATION_EGG, ANIMATION_DIGNEST};
    }

    @Override
    public int getVillagerXp() {
        return 0;
    }

    @Override
    public boolean showProgressBar() {
        return false;
    }

    @Override
    public boolean isClientSide() {
        return false;
    }
}

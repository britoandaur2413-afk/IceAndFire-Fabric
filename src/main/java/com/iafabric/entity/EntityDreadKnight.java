package com.iafabric.entity;

import com.iafabric.IafMod;
import net.minecraft.resources.ResourceKey;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BannerPatternLayers;
import net.minecraft.core.component.DataComponents;
import com.iafabric.util.ItemNbt;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import com.iafabric.compat.ValueInput;
import com.iafabric.compat.ValueOutput;
import com.iafabric.citadel.animation.Animation;
import com.iafabric.citadel.animation.AnimationHandler;
import com.iafabric.citadel.animation.IAnimatedEntity;
import com.iafabric.entity.ai.DreadAIRideHorse;
import com.iafabric.entity.ai.DreadAITargetNonDread;
import com.iafabric.entity.util.DragonUtils;
import com.iafabric.entity.util.IAnimalFear;
import com.iafabric.entity.util.IDreadMob;
import com.iafabric.entity.util.IVillagerFear;
import com.iafabric.item.IafItemRegistry;
import com.iafabric.recipe.IafRecipeRegistry;
import com.google.common.base.Predicate;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BannerPattern;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;

import org.jetbrains.annotations.Nullable;

public class EntityDreadKnight extends EntityDreadMob implements IAnimatedEntity, IVillagerFear, IAnimalFear {

    private static final EntityDataAccessor<Integer> VARIANT = SynchedEntityData.defineId(EntityDreadKnight.class, EntityDataSerializers.INT);
    public static Animation ANIMATION_SPAWN = Animation.create(40);
    private int animationTick;
    private Animation currentAnimation;

    public EntityDreadKnight(EntityType type, Level worldIn) {
        super(type, worldIn);
    }

    /** A shield with the dread banner: cyan with the white dread pattern. */
    /** Client: a knight drawn on a dread dragon's back (DragonRiders), seated as a rider. */
    public boolean seatedOnDragon;

    @Override
    public boolean isPassenger() {
        return this.seatedOnDragon || super.isPassenger();
    }

    public ItemStack generateShield() {
        ItemStack shield = new ItemStack(Items.SHIELD, 1);
        shield.set(DataComponents.BASE_COLOR, DyeColor.CYAN);
        shield.set(DataComponents.BANNER_PATTERNS, new BannerPatternLayers.Builder()
            .addIfRegistered(this.registryAccess().lookupOrThrow(Registries.BANNER_PATTERN), ResourceKey.create(Registries.BANNER_PATTERN, IafMod.id("dread")), DyeColor.WHITE)
            .build());
        return shield;
    }

    public static AttributeSupplier.Builder bakeAttributes() {
        return Mob.createMobAttributes()
            //HEALTH
            .add(Attributes.MAX_HEALTH, 40.0D)
            //SPEED
            .add(Attributes.MOVEMENT_SPEED, 0.25D)
            //ATTACK
            .add(Attributes.ATTACK_DAMAGE, 2.0D)
            //FOLLOW RANGE
            .add(Attributes.FOLLOW_RANGE, 128.0D)
            //ARMOR
            .add(Attributes.ARMOR, 20.0D);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new DreadAIRideHorse(this));
        this.goalSelector.addGoal(1, new FloatGoal(this));
        this.goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.0D, true));
        this.goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 1.0D));
        this.goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 8.0F));
        this.goalSelector.addGoal(7, new RandomLookAroundGoal(this));
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this, IDreadMob.class));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, 10, true, false, entity -> DragonUtils.canHostilesTarget(entity)));
        this.targetSelector.addGoal(3, new DreadAITargetNonDread(this, LivingEntity.class, false, (LivingEntity entity) -> entity instanceof LivingEntity && DragonUtils.canHostilesTarget(entity)));
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(VARIANT, 0);
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (this.getAnimation() == ANIMATION_SPAWN && this.getAnimationTick() < 30) {
            BlockState belowBlock = level().getBlockState(this.blockPosition().below());
            if (belowBlock.getBlock() != Blocks.AIR) {
                for (int i = 0; i < 5; i++) {
                    this.level().addParticle(new BlockParticleOption(ParticleTypes.BLOCK, belowBlock), this.getX() + (double) (this.random.nextFloat() * this.getBbWidth() * 2.0F) - (double) this.getBbWidth(), this.getBoundingBox().minY, this.getZ() + (double) (this.random.nextFloat() * this.getBbWidth() * 2.0F) - (double) this.getBbWidth(), this.random.nextGaussian() * 0.02D, this.random.nextGaussian() * 0.02D, this.random.nextGaussian() * 0.02D);
                }
            }
            this.setDeltaMovement(0, this.getDeltaMovement().y, this.getDeltaMovement().z);
        }
        AnimationHandler.INSTANCE.updateAnimations(this);
    }

    @Override
    protected void populateDefaultEquipmentSlots(RandomSource random, DifficultyInstance difficulty) {
        super.populateDefaultEquipmentSlots(random, difficulty);
        this.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(IafItemRegistry.DREAD_KNIGHT_SWORD));
        if (random.nextBoolean()) {
            this.setItemSlot(EquipmentSlot.OFFHAND, this.generateShield());
        }
        setArmorVariant(random.nextInt(3));
    }

    @Override
    @Nullable
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor worldIn, DifficultyInstance difficultyIn, MobSpawnType reason, @Nullable SpawnGroupData spawnDataIn) {
        SpawnGroupData data = super.finalizeSpawn(worldIn, difficultyIn, reason, spawnDataIn);
        this.setAnimation(ANIMATION_SPAWN);
        this.populateDefaultEquipmentSlots(this.getRandom(), difficultyIn);
        return data;
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
    public void addAdditionalSaveData(CompoundTag compoundTag) {
		ValueOutput compound = new ValueOutput(compoundTag, this.registryAccess());
        super.addAdditionalSaveData(compoundTag);
        compound.putInt("ArmorVariant", getArmorVariant());
    }

    @Override
    public void readAdditionalSaveData(CompoundTag compoundTag) {
		ValueInput compound = new ValueInput(compoundTag, this.registryAccess());
        super.readAdditionalSaveData(compoundTag);
        setArmorVariant(compound.getIntOr("ArmorVariant", 0));
    }

    @Override
    public Animation getAnimation() {
        return currentAnimation;
    }

    @Override
    public void setAnimation(Animation animation) {
        currentAnimation = animation;
    }

    public int getArmorVariant() {
        return this.entityData.get(VARIANT).intValue();
    }

    public void setArmorVariant(int variant) {
        this.entityData.set(VARIANT, variant);
    }

    @Override
    public Animation[] getAnimations() {
        return new Animation[]{ANIMATION_SPAWN};
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
    @Nullable
    protected SoundEvent getAmbientSound() {
        return SoundEvents.STRAY_AMBIENT;
    }

    @Override
    @Nullable
    protected SoundEvent getHurtSound(@NotNull DamageSource source) {
        return SoundEvents.STRAY_HURT;
    }

    @Override
    @Nullable
    protected SoundEvent getDeathSound() {
        return SoundEvents.STRAY_DEATH;
    }

    protected void playStepSound(BlockPos pos, Block blockIn) {
        this.playSound(SoundEvents.STRAY_STEP, 0.15F, 1.0F);
    }

    @Override
    public float getVoicePitch() {
        return super.getVoicePitch() * 0.75F;
    }
}
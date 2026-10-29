package com.iafabric.entity;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import java.util.Optional;
import com.iafabric.entity.util.ItemListing;
import com.iafabric.util.ItemNbt;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.server.level.ServerLevel;
import com.iafabric.citadel.animation.Animation;
import com.iafabric.IafConfig;
import com.iafabric.IafMod;
import com.iafabric.entity.ai.*;
import com.iafabric.entity.util.DragonUtils;
import com.iafabric.entity.util.MyrmexTrades;
import com.iafabric.item.IafItemRegistry;
import com.iafabric.item.ItemMyrmexEgg;
import com.google.common.base.Predicate;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.npc.VillagerTrades;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;

import org.jetbrains.annotations.Nullable;

public class EntityMyrmexWorker extends EntityMyrmexBase {

    public static final Animation ANIMATION_BITE = Animation.create(15);
    public static final Animation ANIMATION_STING = Animation.create(15);
    public static final ResourceLocation DESERT_LOOT = ResourceLocation.fromNamespaceAndPath("iceandfire_fabric", "entities/myrmex_worker_desert");
    public static final ResourceLocation JUNGLE_LOOT = ResourceLocation.fromNamespaceAndPath("iceandfire_fabric", "entities/myrmex_worker_jungle");
    private static final ResourceLocation TEXTURE_DESERT = ResourceLocation.parse("iceandfire_fabric:textures/models/myrmex/myrmex_desert_worker.png");
    private static final ResourceLocation TEXTURE_JUNGLE = ResourceLocation.parse("iceandfire_fabric:textures/models/myrmex/myrmex_jungle_worker.png");
    public boolean keepSearching = true;

    public EntityMyrmexWorker(EntityType<EntityMyrmexWorker> t, Level worldIn) {
        super(t, worldIn);
    }

    public static AttributeSupplier.Builder bakeAttributes() {
        return Mob.createMobAttributes()
            //HEALTH
            .add(Attributes.MAX_HEALTH, 20)
            //SPEED
            .add(Attributes.MOVEMENT_SPEED, 0.3D)
            //ATTACK
            .add(Attributes.ATTACK_DAMAGE, IafConfig.myrmexBaseAttackStrength)
            //FOLLOW RANGE
            .add(Attributes.FOLLOW_RANGE, 32D)
            //ARMOR
            .add(Attributes.ARMOR, 4D);
    }

    /** Drops from this mob's own loot table (variants have their own; Mob#getLootTable is final now). */
    @Override
    protected void dropFromLootTable(DamageSource source, boolean playerKilled) {
    	ServerLevel level = (ServerLevel) this.level();
        ResourceLocation table = this.getIafLootTable();
        if (table != null) {
            com.iafabric.compat.LootDrops.drop(this, source, playerKilled, ResourceKey.create(Registries.LOOT_TABLE, table));
        }
    }

        @Nullable
    protected ResourceLocation getIafLootTable() {
        return isJungle() ? JUNGLE_LOOT : DESERT_LOOT;
    }

    @Override
    public void die(DamageSource cause) {
        if (!this.level().isClientSide && !this.getItemInHand(InteractionHand.MAIN_HAND).isEmpty()) {
            this.spawnAtLocation(this.getItemInHand(InteractionHand.MAIN_HAND), 0);
            this.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        }
        super.die(cause);
    }

    @Override
    protected int getBaseExperienceReward() {
        return 3;
    }

    @Override
    public boolean isSmallerThanBlock() {
        return true;
    }

    @Override
    public void aiStep() {
        super.aiStep();
        /*if (this.getAnimation() == ANIMATION_BITE && this.getAttackTarget() != null && this.getAnimationTick() == 6) {
            this.playBiteSound();
            if (this.getAttackBounds().intersects(this.getAttackTarget().getBoundingBox())) {
                this.getAttackTarget().attackEntityFrom(DamageSource.causeMobDamage(this), ((int) this.getAttribute(Attributes.ATTACK_DAMAGE).getValue()));
            }
        }
        if (this.getAnimation() == ANIMATION_STING && this.getAnimationTick() == 0) {
            this.playStingSound();
        }
        if (this.getAnimation() == ANIMATION_STING && this.getAttackTarget() != null && this.getAnimationTick() == 6) {
            if (this.getAttackBounds().intersects(this.getAttackTarget().getBoundingBox())) {
                this.getAttackTarget().attackEntityFrom(DamageSource.causeMobDamage(this), ((int) this.getAttribute(Attributes.ATTACK_DAMAGE).getValue() * 2));
                this.getAttackTarget().addPotionEffect(new EffectInstance(Effects.POISON, 60, 1));
            }
        }*/
        if (!this.getItemInHand(InteractionHand.MAIN_HAND).isEmpty()) {
            if (this.getItemInHand(InteractionHand.MAIN_HAND).getItem() instanceof ItemMyrmexEgg) {
                boolean isJungle = this.getItemInHand(InteractionHand.MAIN_HAND).getItem() == IafItemRegistry.MYRMEX_JUNGLE_EGG;
                CompoundTag tag = ItemNbt.getTag(this.getItemInHand(InteractionHand.MAIN_HAND));
                int metadata = 0;
                if (tag != null) {
                    metadata = tag.iceandfire_fabric$getIntOr("EggOrdinal", 0);
                }
                EntityMyrmexEgg egg = new EntityMyrmexEgg(IafEntityRegistry.MYRMEX_EGG, level());
                egg.copyPosition(this);
                egg.setJungle(isJungle);
                egg.setMyrmexCaste(metadata);
                if (!level().isClientSide) {
                    level().addFreshEntity(egg);
                }
                egg.startRiding(this);
                this.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
            }
        }
        if (!this.getPassengers().isEmpty()) {
            for (Entity entity : this.getPassengers()) {
                if (entity instanceof EntityMyrmexBase && ((EntityMyrmexBase) entity).getGrowthStage() >= 2) {
                    entity.stopRiding();
                }
            }
        }
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(0, new MyrmexAITradePlayer(this));
        this.goalSelector.addGoal(0, new MyrmexAILookAtTradePlayer(this));
        this.goalSelector.addGoal(1, new MyrmexAIAttackMelee(this, 1.0D, true));
        this.goalSelector.addGoal(2, new MyrmexAIStoreBabies(this, 1.0D));
        this.goalSelector.addGoal(3, new MyrmexAIStoreItems(this, 1.0D));
        this.goalSelector.addGoal(4, new MyrmexAIReEnterHive(this, 1.0D));
        this.goalSelector.addGoal(4, new MyrmexAILeaveHive(this, 1.0D));
        this.goalSelector.addGoal(6, new MyrmexAIForage(this, 2));
        this.goalSelector.addGoal(7, new MyrmexAIMoveThroughHive(this, 1.0D));
        this.goalSelector.addGoal(8, new MyrmexAIWander(this, 1D));
        this.goalSelector.addGoal(9, new LookAtPlayerGoal(this, Player.class, 6.0F));
        this.goalSelector.addGoal(10, new RandomLookAroundGoal(this));
        this.targetSelector.addGoal(1, new MyrmexAIDefendHive(this));
        this.targetSelector.addGoal(2, new MyrmexAIForageForItems(this));
        this.targetSelector.addGoal(3, new MyrmexAIPickupBabies(this));
        this.targetSelector.addGoal(4, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(4, new MyrmexAIAttackPlayers(this));
        this.targetSelector.addGoal(5, new NearestAttackableTargetGoal<LivingEntity>(this, LivingEntity.class, 10, true, true, entity -> EntityMyrmexWorker.this.getMainHandItem().isEmpty() && entity != null && !EntityMyrmexBase.haveSameHive(EntityMyrmexWorker.this, entity) && DragonUtils.isAlive(entity) && !(entity instanceof Enemy)));


    }

    @Override
    public boolean shouldWander() {
        return super.shouldWander() && this.canSeeSky();
    }

    @Override
    protected ItemListing[] getLevel1Trades() {
        return isJungle() ? MyrmexTrades.JUNGLE_WORKER.get(1) : MyrmexTrades.DESERT_WORKER.get(1);
    }

    @Override
    protected ItemListing[] getLevel2Trades() {
        return isJungle() ? MyrmexTrades.JUNGLE_WORKER.get(2) : MyrmexTrades.DESERT_WORKER.get(2);
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
        return 0.6F;
    }

    @Override
    public boolean shouldLeaveHive() {
        return !holdingSomething();
    }

    @Override
    public boolean shouldEnterHive() {
        return holdingSomething() || (!level().isDay() && !IafConfig.myrmexHiveIgnoreDaytime);
    }

    @Override
    public boolean shouldMoveThroughHive() {
        return !shouldLeaveHive() && !holdingSomething();
    }

    @Override
    public boolean doHurtTarget(Entity entityIn) {
    	ServerLevel level = (ServerLevel) this.level();
        if (this.getGrowthStage() < 2) {
            return false;
        }

        if (this.getAnimation() != ANIMATION_STING && this.getAnimation() != ANIMATION_BITE) {
            this.setAnimation(this.getRandom().nextBoolean() ? ANIMATION_STING : ANIMATION_BITE);
            float f = (float) this.getAttributeValue(Attributes.ATTACK_DAMAGE);
            this.setLastHurtMob(entityIn);
            boolean flag = entityIn.hurt(entityIn.damageSources().mobAttack(this), f);
            if (this.getAnimation() == ANIMATION_STING && flag) {
                this.playStingSound();
                if (entityIn instanceof LivingEntity) {
                    ((LivingEntity) entityIn).addEffect(new MobEffectInstance(MobEffects.POISON, 200, 2));
                    this.setTarget((LivingEntity) entityIn);
                }
            } else {
                this.playBiteSound();
            }
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


    public boolean holdingSomething() {
        return this.getHeldEntity() != null || !this.getItemInHand(InteractionHand.MAIN_HAND).isEmpty() || this.getTarget() != null;
    }

    public boolean holdingBaby() {
        return this.getHeldEntity() != null && (this.getHeldEntity() instanceof EntityMyrmexBase || this.getHeldEntity() instanceof EntityMyrmexEgg);
    }

    @Override
    public int getCasteImportance() {
        return 0;
    }

    @Override
    public Animation[] getAnimations() {
        return new Animation[]{ANIMATION_PUPA_WIGGLE, ANIMATION_BITE, ANIMATION_STING};
    }

    @Override
    public void positionRider(Entity passenger, Entity.MoveFunction moveFunction) {
        super.positionRider(passenger, moveFunction);
        if (this.hasPassenger(passenger)) {
            yBodyRot = getYRot();
            float radius = 1.05F;
            float angle = (0.01745329251F * this.yBodyRot);
            double extraX = radius * Mth.sin((float) (Math.PI + angle));
            double extraZ = radius * Mth.cos(angle);
            passenger.setPos(this.getX() + extraX, this.getY() + 0.25F, this.getZ() + extraZ);
        }
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
    	if (!(this.level() instanceof ServerLevel level)) {
    		return super.hurt(source, amount);
    	}
        if (amount >= 1.0D && !this.level().isClientSide && this.getRandom().nextInt(3) == 0 && this.getItemInHand(InteractionHand.MAIN_HAND) != ItemStack.EMPTY) {
            this.spawnAtLocation(this.getItemInHand(InteractionHand.MAIN_HAND), 0);
            this.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        }
        if (amount >= 1.0D && !this.getPassengers().isEmpty()) {
            for (Entity entity : this.getPassengers()) {
                entity.stopRiding();
            }
        }
        return super.hurt(source, amount);
    }

    public Entity getHeldEntity() {
        return this.getPassengers().isEmpty() ? null : this.getPassengers().get(0);
    }

    public void onPickupItem(ItemEntity itemEntity) {
        Item item = itemEntity.getItem().getItem();
        if (item == IafItemRegistry.MYRMEX_JUNGLE_RESIN && this.isJungle() || item == IafItemRegistry.MYRMEX_DESERT_RESIN && !this.isJungle()) {

            Player owner = null;
            try {
                if (itemEntity.getOwner() != null) {
                    owner = (itemEntity.getOwner() instanceof Player ownerPlayer ? ownerPlayer : null);
                }
            } catch (Exception e) {
                IafMod.LOGGER.warn("Myrmex picked up resin that wasn't thrown!");
            }
            if (owner != null && this.getHive() != null) {
                this.getHive().modifyPlayerReputation(owner.getUUID(), 5);
                this.playSound(SoundEvents.SLIME_SQUISH, 1, 1);
                if (!level().isClientSide) {
                    level().addFreshEntity(new ExperienceOrb(level(), owner.getX(), owner.getY(), owner.getZ(), 1 + random.nextInt(3)));
                }
            }
        }
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
        return this.getLevel().isClientSide;
    }
}

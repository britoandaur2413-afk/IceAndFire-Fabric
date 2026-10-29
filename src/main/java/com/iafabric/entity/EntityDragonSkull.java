package com.iafabric.entity;

import com.iafabric.util.ItemNbt;
import com.iafabric.compat.ValueInput;
import com.iafabric.compat.ValueOutput;
import com.iafabric.entity.util.IBlacklistedFromStatues;
import com.iafabric.entity.util.IDeadMob;
import com.iafabric.item.IafItemRegistry;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;

import org.jetbrains.annotations.Nullable;

public class EntityDragonSkull extends Animal implements IBlacklistedFromStatues, IDeadMob {

    private static final EntityDataAccessor<Integer> DRAGON_TYPE = SynchedEntityData.defineId(EntityDragonSkull.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DRAGON_AGE = SynchedEntityData.defineId(EntityDragonSkull.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DRAGON_STAGE = SynchedEntityData.defineId(EntityDragonSkull.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Float> DRAGON_DIRECTION = SynchedEntityData.defineId(EntityDragonSkull.class, EntityDataSerializers.FLOAT);

    public final float minSize = 0.3F;
    public final float maxSize = 8.58F;

    public EntityDragonSkull(EntityType type, Level worldIn) {
        super(type, worldIn);
        // setScale(this.getDragonAge());
    }

    @Override
    public boolean isFood(@NotNull ItemStack stack) {
        return false;
    }

    public static AttributeSupplier.Builder bakeAttributes() {
        return Mob.createMobAttributes()
            //HEALTH
            .add(Attributes.MAX_HEALTH, 10)
            //SPEED
            .add(Attributes.MOVEMENT_SPEED, 0D);
    }

    @Override
    public boolean isInvulnerableTo(DamageSource i) {
    	ServerLevel level = this.level() instanceof ServerLevel serverLevel ? serverLevel : null;
        return i.getEntity() != null && super.isInvulnerableTo(i);
    }

    @Override
    public boolean isNoAi() {
        return true;
    }

    public boolean isOnWall() {
        return this.level().isEmptyBlock(this.blockPosition().below());
    }

    public void onUpdate() {
        this.yBodyRotO = 0;
        this.yHeadRotO = 0;
        this.yBodyRot = 0;
        this.yHeadRot = 0;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DRAGON_TYPE, 0);
        builder.define(DRAGON_AGE, 0);
        builder.define(DRAGON_STAGE, 0);
        builder.define(DRAGON_DIRECTION, 0F);
    }

    public float getYaw() {
        return this.getEntityData().get(DRAGON_DIRECTION).floatValue();
    }

    public void setYaw(float var1) {
        this.getEntityData().set(DRAGON_DIRECTION, var1);
    }

    public int getDragonType() {
        return this.getEntityData().get(DRAGON_TYPE).intValue();
    }

    public void setDragonType(int var1) {
        this.getEntityData().set(DRAGON_TYPE, var1);
    }

    public int getStage() {
        return this.getEntityData().get(DRAGON_STAGE).intValue();
    }

    public void setStage(int var1) {
        this.getEntityData().set(DRAGON_STAGE, var1);
    }

    public int getDragonAge() {
        return this.getEntityData().get(DRAGON_AGE).intValue();
    }

    public void setDragonAge(int var1) {
        this.getEntityData().set(DRAGON_AGE, var1);
    }

    @Override
    public SoundEvent getHurtSound(@NotNull DamageSource damageSourceIn) {
        return null;
    }

    @Override
    public boolean hurt(DamageSource var1, float var2) {
    	if (!(this.level() instanceof ServerLevel level)) {
    		return super.hurt(var1, var2);
    	}
        this.turnIntoItem();
        return super.hurt(var1, var2);
    }

    public void turnIntoItem() {
        if (isRemoved())
            return;
        this.remove(RemovalReason.DISCARDED);
        ItemStack stack = new ItemStack(getDragonSkullItem());
        ItemNbt.setTag(stack, new CompoundTag());
        ItemNbt.update(stack, nbtTag -> nbtTag.putInt("Stage", this.getStage()));
        ItemNbt.update(stack, nbtTag -> nbtTag.putInt("DragonAge", this.getDragonAge()));
        if (!this.level().isClientSide)
            this.spawnAtLocation(stack, 0.0F);

    }

    public Item getDragonSkullItem() {
        switch (getDragonType()) {
            case 0:
                return IafItemRegistry.DRAGON_SKULL_FIRE;
            case 1:
                return IafItemRegistry.DRAGON_SKULL_ICE;
            case 2:
                return IafItemRegistry.DRAGON_SKULL_LIGHTNING;
            default:
                return IafItemRegistry.DRAGON_SKULL_FIRE;
        }
    }

    @Nullable
    @Override
    public AgeableMob getBreedOffspring(@NotNull ServerLevel serverWorld, @NotNull AgeableMob ageable) {
        return null;
    }

    @Override
    public InteractionResult mobInteract(Player player, @NotNull InteractionHand hand) {
        if (player.isShiftKeyDown()) {
            this.setYaw(player.getYRot());
        }
        return super.mobInteract(player, hand);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag compoundTag) {
		ValueInput compound = new ValueInput(compoundTag, this.registryAccess());
        this.setDragonType(compound.getIntOr("Type", 0));
        this.setStage(compound.getIntOr("Stage", 0));
        this.setDragonAge(compound.getIntOr("DragonAge", 0));
        this.setYaw(compound.getFloatOr("DragonYaw", 0.0F));
        super.readAdditionalSaveData(compoundTag);
    }

    @Override
    public void addAdditionalSaveData(CompoundTag compoundTag) {
		ValueOutput compound = new ValueOutput(compoundTag, this.registryAccess());
        compound.putInt("Type", this.getDragonType());
        compound.putInt("Stage", this.getStage());
        compound.putInt("DragonAge", this.getDragonAge());
        compound.putFloat("DragonYaw", this.getYaw());
        super.addAdditionalSaveData(compoundTag);
    }

    public float getDragonSize() {
        float step;
        step = (minSize - maxSize) / (125);

        if (this.getDragonAge() > 125) {
            return this.minSize + (step * 125);
        }

        return this.minSize + (step * this.getDragonAge());
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    protected void doPush(@NotNull Entity entity) {
    }

    @Override
    public boolean canBeTurnedToStone() {
        return false;
    }

    @Override
    public boolean isMobDead() {
        return true;
    }

    public int getDragonStage() {
        return Math.max(getStage(), 1);
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

package com.iafabric.entity;

import net.minecraft.world.entity.EquipmentSlot;
import com.iafabric.entity.util.IafDataSerializers;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.server.level.ServerLevel;
import com.iafabric.compat.ValueInput;
import com.iafabric.compat.ValueOutput;
import com.iafabric.entity.util.IBlacklistedFromStatues;
import com.iafabric.entity.util.IDeadMob;
import com.iafabric.enums.EnumDragonEgg;
import com.iafabric.item.IafItemRegistry;
import com.google.common.collect.ImmutableList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.players.OldUsersConverter;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;

import org.jetbrains.annotations.Nullable;
import java.util.Optional;
import java.util.UUID;

public class EntityDragonEgg extends LivingEntity implements IBlacklistedFromStatues, IDeadMob {

    protected static final EntityDataAccessor<java.util.Optional<UUID>> OWNER_UNIQUE_ID = SynchedEntityData.defineId(EntityDragonEgg.class, IafDataSerializers.OPTIONAL_UUID);
    private static final EntityDataAccessor<Integer> DRAGON_TYPE = SynchedEntityData.defineId(EntityDragonEgg.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DRAGON_AGE = SynchedEntityData.defineId(EntityDragonEgg.class, EntityDataSerializers.INT);

    public EntityDragonEgg(EntityType type, Level worldIn) {
        super(type, worldIn);
    }

    public static AttributeSupplier.Builder bakeAttributes() {
        return Mob.createMobAttributes()
            //HEALTH
            .add(Attributes.MAX_HEALTH, 10.0D)
            //SPEED
            .add(Attributes.MOVEMENT_SPEED, 0D);
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tagTag) {
		ValueOutput tag = new ValueOutput(tagTag, this.level().registryAccess());
        super.addAdditionalSaveData(tagTag);
        tag.putInt("Color", (byte) this.getEggType().ordinal());
        tag.putInt("DragonAge", this.getDragonAge());
        try {
            if (this.getOwnerId() == null) {
                tag.putString("OwnerUUID", "");
            } else {
                tag.putString("OwnerUUID", this.getOwnerId().toString());
            }
        } catch (Exception e) {

        }
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tagTag) {
		ValueInput tag = new ValueInput(tagTag, this.level().registryAccess());
        super.readAdditionalSaveData(tagTag);
        this.setEggType(EnumDragonEgg.values()[tag.getIntOr("Color", 0)]);
        this.setDragonAge(tag.getIntOr("DragonAge", 0));
        String s;

        if (tag.contains("OwnerUUID")) {
            s = tag.getStringOr("OwnerUUID", "");
        } else {
            String s1 = tag.getStringOr("Owner", "");
            UUID converedUUID = OldUsersConverter.convertMobOwnerIfNecessary(this.level().getServer(), s1);
            s = converedUUID == null ? s1 : converedUUID.toString();
        }
        if (!s.isEmpty()) {
            this.setOwnerId(UUID.fromString(s));
        }
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(DRAGON_TYPE, 0);
        this.entityData.define(DRAGON_AGE, 0);
        this.entityData.define(OWNER_UNIQUE_ID, Optional.empty());
    }

    @Nullable
    public UUID getOwnerId() {
        return this.entityData.get(OWNER_UNIQUE_ID).orElse(null);
    }

    public void setOwnerId(@Nullable UUID p_184754_1_) {
        this.entityData.set(OWNER_UNIQUE_ID, java.util.Optional.ofNullable(p_184754_1_));
    }

    public EnumDragonEgg getEggType() {
        return EnumDragonEgg.values()[this.getEntityData().get(DRAGON_TYPE).intValue()];
    }

    public void setEggType(EnumDragonEgg newtype) {
        this.getEntityData().set(DRAGON_TYPE, newtype.ordinal());
    }

    @Override
    public boolean isInvulnerableTo(DamageSource i) {
    	ServerLevel level = this.level() instanceof ServerLevel serverLevel ? serverLevel : null;
        return i.getEntity() != null && super.isInvulnerableTo(i);
    }

    public int getDragonAge() {
        return this.getEntityData().get(DRAGON_AGE).intValue();
    }

    public void setDragonAge(int i) {
        this.getEntityData().set(DRAGON_AGE, i);
    }

    @Override
    public void tick() {
        super.tick();
        if (!level().isClientSide) {
            this.setAirSupply(200);
            getEggType().dragonType.updateEggCondition(this);
        }
    }

    @Override
    public SoundEvent getHurtSound(@NotNull DamageSource damageSourceIn) {
        return null;
    }


    @Override
    public Iterable<ItemStack> getArmorSlots() {
        return net.minecraft.core.NonNullList.withSize(4, ItemStack.EMPTY);
    }

    @Override
    public ItemStack getItemBySlot(@NotNull EquipmentSlot slotIn) {
        return ItemStack.EMPTY;
    }

    @Override
    public void setItemSlot(@NotNull EquipmentSlot slotIn, @NotNull ItemStack stack) {

    }

    @Override
    public boolean hurt(DamageSource var1, float var2) {
    	if (!(this.level() instanceof ServerLevel level)) {
    		return super.hurt(var1, var2);
    	}
        // Fire hatches a fire egg (it sits in it) and drowning/suffocating mean nothing to an egg: only a hit breaks it loose.
        if (var1.is(DamageTypeTags.IS_FIRE) || var1.is(DamageTypeTags.IS_DROWNING) || var1.is(net.minecraft.world.damagesource.DamageTypes.IN_WALL)
            || var1.is(DamageTypeTags.IS_LIGHTNING) || var1.is(DamageTypeTags.IS_FREEZING) || isInvulnerableTo(var1)) {
            return false;
        }
        if (!level().isClientSide && !var1.is(DamageTypeTags.BYPASSES_INVULNERABILITY) && !isRemoved()) {
            this.spawnAtLocation(new ItemStack(this.getItem().getItem()), 1);
        }
        this.remove(RemovalReason.KILLED);
        return true;
    }

    private ItemStack getItem() {
        return switch (getEggType().ordinal()) {
            default -> new ItemStack(IafItemRegistry.DRAGONEGG_RED);
            case 1 -> new ItemStack(IafItemRegistry.DRAGONEGG_GREEN);
            case 2 -> new ItemStack(IafItemRegistry.DRAGONEGG_BRONZE);
            case 3 -> new ItemStack(IafItemRegistry.DRAGONEGG_GRAY);
            case 4 -> new ItemStack(IafItemRegistry.DRAGONEGG_BLUE);
            case 5 -> new ItemStack(IafItemRegistry.DRAGONEGG_WHITE);
            case 6 -> new ItemStack(IafItemRegistry.DRAGONEGG_SAPPHIRE);
            case 7 -> new ItemStack(IafItemRegistry.DRAGONEGG_SILVER);
            case 8 -> new ItemStack(IafItemRegistry.DRAGONEGG_ELECTRIC);
            case 9 -> new ItemStack(IafItemRegistry.DRAGONEGG_AMYTHEST);
            case 10 -> new ItemStack(IafItemRegistry.DRAGONEGG_COPPER);
            case 11 -> new ItemStack(IafItemRegistry.DRAGONEGG_BLACK);
        };
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    public HumanoidArm getMainArm() {
        return HumanoidArm.RIGHT;
    }

    @Override
    protected void doPush(@NotNull Entity entity) {
    }

    @Override
    public boolean canBeTurnedToStone() {
        return false;
    }

    public void onPlayerPlace(Player player) {
        this.setOwnerId(player.getUUID());
    }

    @Override
    public boolean isMobDead() {
        return true;
    }
}

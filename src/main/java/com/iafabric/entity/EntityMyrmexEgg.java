package com.iafabric.entity;

import com.iafabric.util.ItemNbt;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.core.UUIDUtil;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.server.level.ServerLevel;
import com.iafabric.compat.ValueInput;
import com.iafabric.compat.ValueOutput;
import com.iafabric.IafConfig;
import com.iafabric.entity.util.IBlacklistedFromStatues;
import com.iafabric.entity.util.IDeadMob;
import com.iafabric.entity.util.MyrmexHive;
import com.iafabric.item.IafItemRegistry;
import com.iafabric.misc.IafSoundRegistry;
import com.iafabric.world.MyrmexWorldData;
import com.iafabric.world.gen.WorldGenMyrmexHive;
import com.google.common.collect.ImmutableList;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

public class EntityMyrmexEgg extends LivingEntity implements IBlacklistedFromStatues, IDeadMob {

    private static final EntityDataAccessor<Boolean> MYRMEX_TYPE = SynchedEntityData.defineId(EntityMyrmexEgg.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Integer> MYRMEX_AGE = SynchedEntityData.defineId(EntityMyrmexEgg.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> MYRMEX_CASTE = SynchedEntityData.defineId(EntityMyrmexEgg.class, EntityDataSerializers.INT);
    public UUID hiveUUID;

    public EntityMyrmexEgg(EntityType t, Level worldIn) {
        super(t, worldIn);
    }

    public static AttributeSupplier.Builder bakeAttributes() {
        return Mob.createMobAttributes()
            //HEALTH
            .add(Attributes.MAX_HEALTH, 10.0D)
            //SPEED
            .add(Attributes.MOVEMENT_SPEED, 0.0D);
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tagTag) {
		ValueOutput tag = new ValueOutput(tagTag, this.registryAccess());
        super.addAdditionalSaveData(tagTag);
        tag.putBoolean("Jungle", this.isJungle());
        tag.putInt("MyrmexAge", this.getMyrmexAge());
        tag.putInt("MyrmexCaste", this.getMyrmexCaste());
        tag.store("HiveUUID", UUIDUtil.CODEC, hiveUUID == null ? hiveUUID = UUID.randomUUID() : hiveUUID);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tagTag) {
		ValueInput tag = new ValueInput(tagTag, this.registryAccess());
        super.readAdditionalSaveData(tagTag);
        this.setJungle(tag.getBooleanOr("Jungle", false));
        this.setMyrmexAge(tag.getIntOr("MyrmexAge", 0));
        this.setMyrmexCaste(tag.getIntOr("MyrmexCaste", 0));
        hiveUUID = tag.read("HiveUUID", UUIDUtil.CODEC).orElse(null);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(MYRMEX_TYPE, false);
        builder.define(MYRMEX_AGE, 0);
        builder.define(MYRMEX_CASTE, 0);
    }


    public boolean isJungle() {
        return this.getEntityData().get(MYRMEX_TYPE).booleanValue();
    }

    public void setJungle(boolean jungle) {
        this.getEntityData().set(MYRMEX_TYPE, jungle);
    }

    public int getMyrmexAge() {
        return this.getEntityData().get(MYRMEX_AGE).intValue();
    }

    public void setMyrmexAge(int i) {
        this.getEntityData().set(MYRMEX_AGE, i);
    }

    public int getMyrmexCaste() {
        return this.getEntityData().get(MYRMEX_CASTE).intValue();
    }

    public void setMyrmexCaste(int i) {
        this.getEntityData().set(MYRMEX_CASTE, i);
    }

    public boolean canSeeSky() {
        return level().canSeeSkyFromBelowWater(this.blockPosition());
    }

    @Override
    public void tick() {
        super.tick();
        if (!canSeeSky()) {
            this.setMyrmexAge(this.getMyrmexAge() + 1);
        }
        if (this.getMyrmexAge() > IafConfig.myrmexEggTicks) {
            this.remove(RemovalReason.DISCARDED);
            EntityMyrmexBase myrmex;
            switch (this.getMyrmexCaste()) {
                default:
                    myrmex = new EntityMyrmexWorker(IafEntityRegistry.MYRMEX_WORKER, level());
                    break;
                case 1:
                    myrmex = new EntityMyrmexSoldier(IafEntityRegistry.MYRMEX_SOLDIER, level());
                    break;
                case 2:
                    myrmex = new EntityMyrmexRoyal(IafEntityRegistry.MYRMEX_ROYAL, level());
                    break;
                case 3:
                    myrmex = new EntityMyrmexSentinel(IafEntityRegistry.MYRMEX_SENTINEL, level());
                    break;
                case 4:
                    myrmex = new EntityMyrmexQueen(IafEntityRegistry.MYRMEX_QUEEN, level());
                    break;
            }
            myrmex.setJungleVariant(this.isJungle());
            myrmex.setGrowthStage(0);
            myrmex.absMoveTo(this.getX(), this.getY(), this.getZ(), 0, 0);
            if (myrmex instanceof EntityMyrmexQueen) {
                MyrmexHive hive = new MyrmexHive(level(), this.blockPosition(), 100);
                Player player = level().getNearestPlayer(this, 30);
                if (player != null) {
                    hive.hasOwner = true;
                    hive.ownerUUID = player.getUUID();
                    if (!level().isClientSide) {
                        hive.modifyPlayerReputation(player.getUUID(), 100);
                    }
                }
                MyrmexWorldData.addHive(level(), hive);
                myrmex.setHive(hive);


            } else {
                if (MyrmexWorldData.get(level()) != null) {
                    MyrmexHive hive;
                    if (this.hiveUUID == null) {
                        hive = MyrmexWorldData.get(level()).getNearestHive(this.blockPosition(), 400);
                    } else {
                        hive = MyrmexWorldData.get(level()).getHiveFromUUID(hiveUUID);
                    }
                    if (!level().isClientSide && hive != null && Math.sqrt(this.distanceToSqr(hive.getCenter().getX(), hive.getCenter().getY(), hive.getCenter().getZ())) < 2000) {
                        myrmex.setHive(hive);
                    }
                }
            }

            if (!level().isClientSide) {
                level().addFreshEntity(myrmex);
            }
            this.level().playLocalSound(this.getX(), this.getY() + this.getEyeHeight(), this.getZ(), IafSoundRegistry.EGG_HATCH, this.getSoundSource(), 2.5F, 1.0F, false);
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
    public boolean hurt(DamageSource dmg, float var2) {
    	if (!(this.level() instanceof ServerLevel level)) {
    		return super.hurt(dmg, var2);
    	}
        if (dmg.is(DamageTypes.IN_WALL) || dmg.is(DamageTypes.FALL)) {
            return false;
        }
        if (!level().isClientSide && !dmg.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) {
            this.spawnAtLocation(this.getItem(), 0);
        }
        this.remove(RemovalReason.KILLED);
        return super.hurt(dmg, var2);
    }

    private ItemStack getItem() {
        ItemStack egg = new ItemStack(this.isJungle() ? IafItemRegistry.MYRMEX_JUNGLE_EGG : IafItemRegistry.MYRMEX_DESERT_EGG, 1);
        CompoundTag newTag = new CompoundTag();
        newTag.putInt("EggOrdinal", this.getMyrmexCaste());
        ItemNbt.setTag(egg, newTag);
        return egg;
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    public HumanoidArm getMainArm() {
        return null;
    }

    @Override
    protected void doPush(@NotNull Entity entity) {
    }

    @Override
    public boolean canBeTurnedToStone() {
        return false;
    }

    public void onPlayerPlace(Player player) {
    }

    @Override
    public boolean isMobDead() {
        return true;
    }

    public boolean isInNursery() {
        MyrmexHive hive = MyrmexWorldData.get(this.level()).getNearestHive(this.blockPosition(), 100);
        if (hive != null && hive.getRooms(WorldGenMyrmexHive.RoomType.NURSERY).isEmpty() && hive.getRandomRoom(WorldGenMyrmexHive.RoomType.NURSERY, this.getRandom(), this.blockPosition()) != null) {
            return false;
        }
        if (hive != null) {
            BlockPos nursery = hive.getRandomRoom(WorldGenMyrmexHive.RoomType.NURSERY, this.getRandom(), this.blockPosition());
            return this.distanceToSqr(nursery.getX(), nursery.getY(), nursery.getZ()) < 2025;
        }
        return false;
    }
}

package com.iafabric.entity;

import com.iafabric.entity.util.IafDataSerializers;
import com.iafabric.util.EntityNbt;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.server.level.ServerLevel;
import com.iafabric.compat.ValueInput;
import com.iafabric.compat.ValueOutput;
import com.iafabric.IafMod;
import com.iafabric.entity.util.IBlacklistedFromStatues;
import com.google.common.collect.ImmutableList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.core.registries.BuiltInRegistries;
import org.jetbrains.annotations.NotNull;

public class EntityStoneStatue extends LivingEntity implements IBlacklistedFromStatues {
    /** A statue is stone: no mob takes it for an enemy (the warden kept attacking them). */
    @Override
    public boolean canBeSeenAsEnemy() {
        return false;
    }


    private static final EntityDataAccessor<String> TRAPPED_ENTITY_TYPE = SynchedEntityData.defineId(EntityStoneStatue.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<CompoundTag> TRAPPED_ENTITY_DATA = SynchedEntityData.defineId(EntityStoneStatue.class, IafDataSerializers.COMPOUND_TAG);
    private static final EntityDataAccessor<Float> TRAPPED_ENTITY_WIDTH = SynchedEntityData.defineId(EntityStoneStatue.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> TRAPPED_ENTITY_HEIGHT = SynchedEntityData.defineId(EntityStoneStatue.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> TRAPPED_ENTITY_SCALE = SynchedEntityData.defineId(EntityStoneStatue.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Integer> CRACK_AMOUNT = SynchedEntityData.defineId(EntityStoneStatue.class, EntityDataSerializers.INT);
    private EntityDimensions stoneStatueSize = EntityDimensions.fixed(0.5F, 0.5F);

    public EntityStoneStatue(EntityType<? extends LivingEntity> t, Level worldIn) {
        super(t, worldIn);
    }

    public static AttributeSupplier.Builder bakeAttributes() {
        return Mob.createMobAttributes()
            //HEALTH
            .add(Attributes.MAX_HEALTH, 20)
            //SPEED
            .add(Attributes.MOVEMENT_SPEED, 0.0D)
            //ATTACK
            .add(Attributes.ATTACK_DAMAGE, 1.0D);
    }

    public static EntityStoneStatue buildStatueEntity(LivingEntity parent) {
        EntityStoneStatue statue = IafEntityRegistry.STONE_STATUE.create(parent.level());
        CompoundTag entityTag = new CompoundTag();
        try {
            if (!(parent instanceof Player)) {
                entityTag = EntityNbt.save(parent);
            }
        } catch (Exception e) {
            IafMod.LOGGER.debug("Encountered issue creating stone statue from {}", parent);
        }
        statue.setTrappedTag(entityTag);
        statue.setTrappedEntityTypeString(BuiltInRegistries.ENTITY_TYPE.getKey(parent.getType()).toString());
        statue.setTrappedEntityWidth(parent.getBbWidth());
        statue.setTrappedHeight(parent.getBbHeight());
        statue.setTrappedScale(parent.getScale() * parent.getScale());

        return statue;
    }

    @Override
    public void push(@NotNull Entity entityIn) {
    }

    @Override
    public void baseTick() {

    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(TRAPPED_ENTITY_TYPE, "minecraft:pig");
        this.entityData.define(TRAPPED_ENTITY_DATA, new CompoundTag());
        this.entityData.define(TRAPPED_ENTITY_WIDTH, 0.5F);
        this.entityData.define(TRAPPED_ENTITY_HEIGHT, 0.5F);
        this.entityData.define(TRAPPED_ENTITY_SCALE, 1F);
        this.entityData.define(CRACK_AMOUNT, 0);
    }

    public EntityType getTrappedEntityType() {
        String str = getTrappedEntityTypeString();
        return EntityNbt.type(str).orElse(EntityType.PIG);
    }

    public String getTrappedEntityTypeString() {
        return this.entityData.get(TRAPPED_ENTITY_TYPE);
    }

    public void setTrappedEntityTypeString(String string) {
        this.entityData.set(TRAPPED_ENTITY_TYPE, string);
    }

    public CompoundTag getTrappedTag() {
        return this.entityData.get(TRAPPED_ENTITY_DATA);
    }

    public void setTrappedTag(CompoundTag tag) {
        this.entityData.set(TRAPPED_ENTITY_DATA, tag);
    }

    public float getTrappedWidth() {
        return this.entityData.get(TRAPPED_ENTITY_WIDTH);
    }

    public void setTrappedEntityWidth(float size) {
        this.entityData.set(TRAPPED_ENTITY_WIDTH, size);
    }

    public float getTrappedHeight() {
        return this.entityData.get(TRAPPED_ENTITY_HEIGHT);
    }

    public void setTrappedHeight(float size) {
        this.entityData.set(TRAPPED_ENTITY_HEIGHT, size);
    }

    public float getTrappedScale() {
        return this.entityData.get(TRAPPED_ENTITY_SCALE);
    }

    public void setTrappedScale(float size) {
        this.entityData.set(TRAPPED_ENTITY_SCALE, size);
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tagTag) {
		ValueOutput tag = new ValueOutput(tagTag, this.level().registryAccess());
        super.addAdditionalSaveData(tagTag);
        tag.putInt("CrackAmount", this.getCrackAmount());
        tag.putFloat("StatueWidth", this.getTrappedWidth());
        tag.putFloat("StatueHeight", this.getTrappedHeight());
        tag.putFloat("StatueScale", this.getTrappedScale());
        tag.putString("StatueEntityType", this.getTrappedEntityTypeString());
        tag.store("StatueEntityTag", CompoundTag.CODEC, this.getTrappedTag());
    }

    /** This statue's own data (what it holds, its size and cracks), as stored in a statuette item. */
    public CompoundTag saveTrappedData() {
        CompoundTag output = new CompoundTag();
        this.addAdditionalSaveData(output);
        return output;
    }

    @Override
    public float getScale() {
        return getModelScale();
    }

    public float getModelScale() {
        return this.getTrappedScale();
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tagTag) {
		ValueInput tag = new ValueInput(tagTag, this.level().registryAccess());
        super.readAdditionalSaveData(tagTag);
        this.setCrackAmount(tag.getByteOr("CrackAmount", (byte) 0));
        this.setTrappedEntityWidth(tag.getFloatOr("StatueWidth", 0.0F));
        this.setTrappedHeight(tag.getFloatOr("StatueHeight", 0.0F));
        this.setTrappedScale(tag.getFloatOr("StatueScale", 0.0F));
        this.setTrappedEntityTypeString(tag.getStringOr("StatueEntityType", ""));
        tag.read("StatueEntityTag", CompoundTag.CODEC).ifPresent(this::setTrappedTag);
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
    	if (!(this.level() instanceof ServerLevel level)) {
    		return super.hurt(source, amount);
    	}
        return source == damageSources().fellOutOfWorld();
    }

    @Override
    public EntityDimensions getDimensions(Pose poseIn) {
        return stoneStatueSize;
    }

    @Override
    public void tick() {
        super.tick();
        this.setYRot(this.yBodyRot);
        this.yHeadRot = this.getYRot();
        if (Math.abs(this.getBbWidth() - getTrappedWidth()) > 0.01 || Math.abs(this.getBbHeight() - getTrappedHeight()) > 0.01) {
            double prevX = this.getX();
            double prevZ = this.getZ();
            this.stoneStatueSize = EntityDimensions.scalable(getTrappedWidth(), getTrappedHeight());
            refreshDimensions();
            this.setPos(prevX, this.getY(), prevZ);
        }
    }

    @Override
    public void kill() {
        this.remove(RemovalReason.KILLED);
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
    public HumanoidArm getMainArm() {
        return HumanoidArm.RIGHT;
    }

    public int getCrackAmount() {
        return this.entityData.get(CRACK_AMOUNT);
    }

    public void setCrackAmount(int crackAmount) {
        this.entityData.set(CRACK_AMOUNT, crackAmount);
    }

    @Override
    public boolean canBeTurnedToStone() {
        return false;
    }
}

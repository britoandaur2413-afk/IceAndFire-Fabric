package com.iafabric.entity;

import net.minecraft.world.phys.Vec3;
import com.iafabric.entity.util.IafDataSerializers;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.EntityDimensions;
import com.iafabric.compat.ValueInput;
import com.iafabric.compat.ValueOutput;
import com.iafabric.IafMod;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;

import org.jetbrains.annotations.Nullable;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public abstract class EntityMutlipartPart extends Entity {

    private static final EntityDataAccessor<Optional<UUID>> PARENT_UUID = SynchedEntityData.defineId(EntityMutlipartPart.class, IafDataSerializers.OPTIONAL_UUID);
    private static final EntityDataAccessor<Float> SCALE_WIDTH = SynchedEntityData.defineId(EntityMutlipartPart.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> SCALE_HEIGHT = SynchedEntityData.defineId(EntityMutlipartPart.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> PART_YAW = SynchedEntityData.defineId(EntityMutlipartPart.class, EntityDataSerializers.FLOAT);
    public EntityDimensions multipartSize;
    protected float radius;
    protected float angleYaw;
    protected float offsetY;
    protected float damageMultiplier;

    protected EntityMutlipartPart(EntityType<?> t, Level world) {
        super(t, world);
        multipartSize = t.getDimensions();
    }

    @Override
    public void readAdditionalSaveData(CompoundTag compoundTag) {
		ValueInput compound = new ValueInput(compoundTag, this.registryAccess());

    }

    @Override
    public void addAdditionalSaveData(CompoundTag compoundTag) {
		ValueOutput compound = new ValueOutput(compoundTag, this.registryAccess());

    }

    @Override
    protected void doWaterSplashEffect() {

    }

    public EntityMutlipartPart(EntityType<?> t, Entity parent, float radius, float angleYaw, float offsetY, float sizeX,
        float sizeY, float damageMultiplier) {
        super(t, parent.level());
        this.setParent(parent);
        this.setScaleX(sizeX);
        this.setScaleY(sizeY);
        this.radius = radius;
        this.angleYaw = (angleYaw + 90.0F) * ((float) Math.PI / 180.0F);
        this.offsetY = offsetY;

        this.damageMultiplier = damageMultiplier;
    }


    public static AttributeSupplier.Builder bakeAttributes() {
        return Mob.createMobAttributes()
            //HEALTH
            .add(Attributes.MAX_HEALTH, 2D)
            //SPEED
            .add(Attributes.MOVEMENT_SPEED, 0.1D);
    }

    @Override
    public EntityDimensions getDimensions(Pose poseIn) {
        return EntityDimensions.scalable(getScaleX(), getScaleY());
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(PARENT_UUID, Optional.empty());
        builder.define(SCALE_WIDTH, 0.5F);
        builder.define(SCALE_HEIGHT, 0.5F);
        builder.define(PART_YAW, 0F);
    }

    @Nullable
    public UUID getParentId() {
        return this.entityData.get(PARENT_UUID).orElse(null);
    }

    public void setParentId(@Nullable UUID uniqueId) {
        this.entityData.set(PARENT_UUID, Optional.ofNullable(uniqueId));
    }

    private float getScaleX() {
        return this.entityData.get(SCALE_WIDTH).floatValue();
    }

    private void setScaleX(float scale) {
        this.entityData.set(SCALE_WIDTH, scale);
    }

    private float getScaleY() {
        return this.entityData.get(SCALE_HEIGHT).floatValue();
    }

    private void setScaleY(float scale) {
        this.entityData.set(SCALE_HEIGHT, scale);
    }

    public float getPartYaw() {
        return this.entityData.get(PART_YAW).floatValue();
    }

    private void setPartYaw(float yaw) {
        this.entityData.set(PART_YAW, yaw % 360);
    }

    @Override
    public void tick() {
        wasTouchingWater = false;
        if (this.tickCount > 10) {
            Entity parent = getParent();
            refreshDimensions();
            if (parent != null && !level().isClientSide) {
                float renderYawOffset = yawOn(parent);
                if (this.placesItself()) {
                    // (placed on its own: around its bones)
                } else if (isSlowFollow()) {
                    this.setPos(parent.xo + this.radius * Mth.cos((float) (renderYawOffset * (Math.PI / 180.0F) + this.angleYaw)), parent.yo + this.offsetY, parent.zo + this.radius * Mth.sin((float) (renderYawOffset * (Math.PI / 180.0F) + this.angleYaw)));
                    double d0 = parent.getX() - this.getX();
                    double d1 = parent.getY() - this.getY();
                    double d2 = parent.getZ() - this.getZ();
                    float f2 = -((float) (Mth.atan2(d1, Mth.sqrt((float) (d0 * d0 + d2 * d2))) * (180F / (float) Math.PI)));
                    this.setXRot(this.limitAngle(this.getXRot(), f2, 5.0F));
                    this.markHurt();
                    this.setYRot(renderYawOffset);
                    this.setPartYaw(getYRot());
                    if (!this.level().isClientSide) {
                        this.collideWithNearbyEntities();
                    }
                } else {
                    this.setPos(parent.getX() + this.radius * Mth.cos((float) (renderYawOffset * (Math.PI / 180.0F) + this.angleYaw)), parent.getY() + this.offsetY, parent.getZ() + this.radius * Mth.sin((float) (renderYawOffset * (Math.PI / 180.0F) + this.angleYaw)));
                    this.markHurt();
                }
                if (!this.level().isClientSide) {
                    this.collideWithNearbyEntities();
                }
                if (parent.isRemoved() && !level().isClientSide) {
                    this.remove(RemovalReason.DISCARDED);
                }
            } else if (tickCount > 20 && !level().isClientSide) {
                remove(RemovalReason.DISCARDED);
            }
        }
        super.tick();
    }

    /** Whether it places itself on its body (not at a point turned with it). */
    protected boolean placesItself() {
        return false;
    }

    /** The turn it is placed by around its body: its body's. */
    protected float yawOn(Entity parent) {
        return parent instanceof LivingEntity living ? living.yBodyRot : parent.getYRot();
    }

    protected boolean isSlowFollow(){
        return false;
    }

    protected float limitAngle(float sourceAngle, float targetAngle, float maximumChange) {
        float f = Mth.wrapDegrees(targetAngle - sourceAngle);
        if (f > maximumChange) {
            f = maximumChange;
        }

        if (f < -maximumChange) {
            f = -maximumChange;
        }

        float f1 = sourceAngle + f;
        if (f1 < 0.0F) {
            f1 += 360.0F;
        } else if (f1 > 360.0F) {
            f1 -= 360.0F;
        }

        return f1;
    }


    @Override
    public void remove(@NotNull RemovalReason reason) {
        super.remove(RemovalReason.DISCARDED);
    }

    /**
     * Its body, on either side. (It was found only on the server: a player's blow on a head, a wing or a tail was
     * dropped by their own game, which had no body to send it to.)
     */
    public Entity getParent() {
        UUID id = getParentId();
        return id != null ? com.iafabric.compat.Entities.byUuid(level(), id) : null;
    }

    public void setParent(Entity entity) {
        this.setParentId(entity.getUUID());
    }

    @Override
    public boolean is(@NotNull Entity entity) {
        return this == entity || this.getParent() == entity;
    }

    @Override
    public boolean isPickable() {
        return true;
    }

    public void collideWithNearbyEntities() {
        List<Entity> entities = this.level().getEntities(this, this.getBoundingBox().expandTowards(0.20000000298023224D, 0.0D, 0.20000000298023224D));
        Entity parent = this.getParent();
        if (parent != null) {
            entities.stream().filter(entity -> entity != parent && !sharesRider(parent, entity) && !(entity instanceof EntityMutlipartPart) && entity.isPushable()).forEach(entity -> entity.push(parent));

        }
    }

    public static boolean sharesRider(Entity parent, Entity entityIn) {
        for (Entity entity : parent.getPassengers()) {
            if (entity.equals(entityIn)) {
                return true;
            }

            if (sharesRider(entity, entityIn)) {
                return true;
            }
        }

        return false;
    }

    @Override
    public InteractionResult interactAt(@NotNull Player player, Vec3 location, @NotNull InteractionHand hand) {
        // (the game sends the use of a part to the server itself, where it reaches the body once)
        Entity parent = getParent();
        return parent != null ? parent.interactAt(player, location, hand) : InteractionResult.PASS;
    }

    /**
     * The use itself reaches the body too: 1.21.1 asks a right click in two steps (interactAt, then this one when that
     * did nothing), where 26.x asks it once. A mob does its own (getting on, its menu, feeding) only in this one, so
     * without it a click on a dragon's body, head, wings or tail did nothing: only its own box let its owner on.
     */
    @Override
    public InteractionResult interact(@NotNull Player player, @NotNull InteractionHand hand) {
        Entity parent = getParent();
        return parent != null ? parent.interact(player, hand) : InteractionResult.PASS;
    }

    @Override
    public boolean hurt(DamageSource source, float damage) {
    	if (!(this.level() instanceof ServerLevel level)) {
    		return super.hurt(source, damage);
    	}
        Entity parent = getParent();
        // only blows of a player or a creature (an arrow, a breath) reach its body through it: never the place it
        // stands in (a wall, lava, a fall)
        if (parent == null || source.getEntity() == null && source.getDirectEntity() == null || this.isInvulnerableTo(source)) {
            return false;
        }
        return parent.hurt(source, damage * this.damageMultiplier);
    }

    public boolean isInvulnerableTo(DamageSource source) {
        return source.is(DamageTypes.FALL) || source.is(DamageTypes.DROWN) || source.is(DamageTypes.IN_WALL) || source.is(DamageTypes.FALLING_BLOCK) || source.is(DamageTypes.LAVA) || source.is(DamageTypeTags.IS_FIRE) || source.is(DamageTypes.CRAMMING) || source.is(DamageTypes.IN_FIRE) || super.isInvulnerableTo(source);
    }

    public boolean shouldNotExist() {
        Entity parent = getParent();
        return !parent.isAlive();
    }

    /** Whether it is in its level (added and not taken away). */
    private boolean inLevel;

    @Override
    public void setLevelCallback(net.minecraft.world.level.entity.EntityInLevelCallback callback) {
        super.setLevelCallback(callback);
        this.inLevel = callback != net.minecraft.world.level.entity.EntityInLevelCallback.NULL;
    }

    /**
     * Whether it needs no adding to its level: it is in it already, or it is gone. (It was isAlive here, true for a part
     * never added, so no part was ever in the world: the dragons had no hitbox on their neck, wings, head and tail.)
     */
    public boolean shouldContinuePersisting() {
        return this.inLevel || this.isRemoved();
    }

    /** Never saved with its chunk: its body makes it again. */
    @Override
    public boolean shouldBeSaved() {
        return false;
    }
}

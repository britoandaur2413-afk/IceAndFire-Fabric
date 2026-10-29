package com.iafabric.entity;

import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public class EntityDragonPart extends EntityMutlipartPart {
    /** Which of its body's parts it is (DragonBones: its head, a piece of its neck, a wing or the tail...); -1 for none. */
    private static final EntityDataAccessor<Byte> PART = SynchedEntityData.defineId(EntityDragonPart.class, EntityDataSerializers.BYTE);
    private EntityDragonBase dragon;
    /** Its box as placed on this side: its width (west to east), height and depth (north to south). */
    private float boxWidth = 0.5F, boxHeight = 0.5F, boxDepth = 0.5F;
    private boolean placed;

    public EntityDragonPart(EntityType<?> t, Level world) {
        super(t, world);
        this.noPhysics = true;
    }


    public EntityDragonPart(EntityType<?> type, EntityDragonBase dragon, float radius, float angleYaw, float offsetY,
        float sizeX, float sizeY, float damageMultiplier) {
        super(type, dragon, radius, angleYaw, offsetY, sizeX, sizeY, damageMultiplier);
        this.dragon = dragon;
        this.noPhysics = true;
    }

    public EntityDragonPart(EntityDragonBase parent, float radius, float angleYaw, float offsetY, float sizeX, float sizeY, float damageMultiplier) {
        super(IafEntityRegistry.DRAGON_MULTIPART, parent, radius, angleYaw, offsetY, sizeX, sizeY,
            damageMultiplier);
        this.dragon = parent;
        this.noPhysics = true;
    }

    /** One of its body's parts, around its bones as they are posed (DragonBones). */
    public EntityDragonPart(EntityDragonBase parent, int part) {
        this(parent, 0F, 0F, 0F, 0.5F, 0.5F, DragonBones.damage(part));
        this.entityData.set(PART, (byte) part);
        this.copyPosition(parent);
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(PART, (byte) -1);
    }

    public int part() {
        return this.entityData.get(PART);
    }

    /** The part of the rider drawn on its back (DragonRiders): in its seat, not one of its body's. */
    public static final int RIDER = -2;

    protected void setPart(int part) {
        this.entityData.set(PART, (byte) part);
    }

    /** Whether it places itself, on the server and on each player's game alike (one of its body's, or its rider's). */
    @Override
    protected boolean placesItself() {
        return this.part() >= 0 || this.part() == RIDER;
    }

    /**
     * Around its bones as its body is posed this tick (or in its rider's seat), on the server and on each player's game
     * alike: each works it out from the dragon as it has it (where it is drawn there, where it is struck here), so the
     * server does not send where it is (only seldom: DRAGON_MULTIPART).
     */
    @Override
    public void tick() {
        if (this.getParent() instanceof EntityDragonBase body) {
            if (this.part() == RIDER) {
                Vec3 seat = DragonRiders.seat(body);
                this.place(new Vec3(seat.x, seat.y + 0.1, seat.z), 0.9F, 1.5F, 0.9F);
            } else if (this.part() >= 0) {
                float[] boxes = DragonBones.boxes(body);
                if (boxes != null) {
                    int o = this.part() * 6;
                    this.place(body.position().add((boxes[o] + boxes[o + 3]) / 2.0F, boxes[o + 1], (boxes[o + 2] + boxes[o + 5]) / 2.0F),
                        Math.max(boxes[o + 3] - boxes[o], 0.05F), Math.max(boxes[o + 4] - boxes[o + 1], 0.05F), Math.max(boxes[o + 5] - boxes[o + 2], 0.05F));
                }
            }
        }
        super.tick();
    }

    private void place(Vec3 at, float width, float height, float depth) {
        // (drawn on from where it was placed last, never from where the server's word put it: its game leaves that)
        Vec3 was = this.placed ? this.position() : at;
        if (width != this.boxWidth || height != this.boxHeight || depth != this.boxDepth) {
            this.boxWidth = width;
            this.boxHeight = height;
            this.boxDepth = depth;
            this.refreshDimensions();
        }
        this.setPos(at);
        this.xo = this.xOld = was.x;
        this.yo = this.yOld = was.y;
        this.zo = this.zOld = was.z;
        this.placed = true;
    }

    @Override
    public EntityDimensions getDimensions(Pose poseIn) {
        return this.placesItself() ? EntityDimensions.scalable(Math.max(this.boxWidth, this.boxDepth), this.boxHeight) : super.getDimensions(poseIn);
    }

    /** Its box as wide and as deep as its bones are (not a square of the wider of the two). */
    @Override
    protected AABB makeBoundingBox() {
        if (!this.placesItself()) {
            return super.makeBoundingBox();
        }
        Vec3 pos = this.position();
        return new AABB(pos.x - this.boxWidth / 2, pos.y, pos.z - this.boxDepth / 2, pos.x + this.boxWidth / 2, pos.y + this.boxHeight, pos.z + this.boxDepth / 2);
    }

    /**
     * On a player's game it is placed from its dragon there each tick: where the server says it is comes later and from
     * where the dragon was before (it would shake its box off the dragon as it is drawn), so that is left once it is placed.
     */
    @Override
    public void lerpTo(double x, double y, double z, float yRot, float xRot, int steps, boolean teleport) {
        if (!this.placed) {
            super.lerpTo(x, y, z, yRot, xRot, steps, teleport);
        }
    }

    /** Black Frost is a wall: its parts push back who comes against its body; other dragons let things pass. */
    @Override
    public void collideWithNearbyEntities() {
        if (this.dragon != null && this.dragon.isBlackFrost() && !this.dragon.isModelDead()) {
            com.iafabric.boss.BossLimits.pushAway(this.dragon, this);
        }
    }

    @Override
    public boolean shouldNotExist() {
        return this.dragon != null && !this.dragon.isAlive() && !this.dragon.isModelDead();
    }
}

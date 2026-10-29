package com.iafabric.entity;

import net.minecraft.tags.DamageTypeTags;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public class EntityCyclopsEye extends EntityMutlipartPart {

    public EntityCyclopsEye(EntityType<?> t, Level world) {
        super(t, world);
    }


    public EntityCyclopsEye(LivingEntity parent, float radius, float angleYaw, float offsetY, float sizeX, float sizeY, float damageMultiplier) {
        super(IafEntityRegistry.CYCLOPS_MULTIPART, parent, radius, angleYaw, offsetY, sizeX, sizeY,
            damageMultiplier);
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float damage) {
        Entity parent = this.getParent();
        if (parent instanceof EntityCyclops cyclops && source.is(DamageTypeTags.IS_PROJECTILE) && atItsFace(cyclops, source)) {
            cyclops.onHitEye(source, damage);
            return true;
        } else {
            return parent != null && parent.hurtOrSimulate(source, damage);
        }
    }

    /** Its eye is on its head, which turns to where it looks: it stands on its face there. */
    @Override
    protected float yawOn(Entity parent) {
        return parent instanceof LivingEntity living ? living.yHeadRot : super.yawOn(parent);
    }

    /**
     * Whether the shot came at its face: from ahead of where it looks (within 60 degrees of it), as one in its eye
     * does. Through the back or the side of its head it strikes its head, not its eye.
     */
    public boolean atItsFace(EntityCyclops cyclops, DamageSource source) {
        Entity shot = source.getDirectEntity();
        Vec3 way;
        if (shot != null && shot.getDeltaMovement().lengthSqr() > 1.0E-4D) {
            way = shot.getDeltaMovement().normalize();
        } else if (source.getSourcePosition() != null) {
            way = this.getBoundingBox().getCenter().subtract(source.getSourcePosition()).normalize();
        } else {
            return false;
        }
        return -way.dot(cyclops.getViewVector(1.0F)) > 0.5D;
    }
}

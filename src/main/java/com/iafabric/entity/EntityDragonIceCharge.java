package com.iafabric.entity;

import com.iafabric.IafConfig;
import com.iafabric.IafMod;
import com.iafabric.enums.EnumParticles;
import com.iafabric.misc.IafDamageRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.projectile.AbstractHurtingProjectile;
import net.minecraft.world.level.Level;

import org.jetbrains.annotations.Nullable;

public class EntityDragonIceCharge extends EntityDragonCharge {

    public EntityDragonIceCharge(EntityType<? extends AbstractHurtingProjectile> type, Level worldIn) {
        super(type, worldIn);

    }


    public EntityDragonIceCharge(EntityType<? extends AbstractHurtingProjectile> type, Level worldIn, double posX,
                                 double posY, double posZ, double accelX, double accelY, double accelZ) {
        super(type, worldIn, posX, posY, posZ, accelX, accelY, accelZ);
    }

    public EntityDragonIceCharge(EntityType<? extends AbstractHurtingProjectile> type, Level worldIn,
                                 EntityDragonBase shooter, double accelX, double accelY, double accelZ) {
        super(type, worldIn, shooter, accelX, accelY, accelZ);
    }

    @Override
    public void tick() {
        if (this.level().isClientSide) {
            for (int i = 0; i < 10; ++i) {
                IafMod.PROXY.spawnParticle(EnumParticles.DragonIce, this.getX() + this.random.nextDouble() * 1 * (this.random.nextBoolean() ? -1 : 1), this.getY() + this.random.nextDouble() * 1 * (this.random.nextBoolean() ? -1 : 1), this.getZ() + this.random.nextDouble() * 1 * (this.random.nextBoolean() ? -1 : 1), 0.0D, 0.0D, 0.0D);
            }
        }
        super.tick();
    }

    @Override
    public DamageSource causeDamage(@Nullable Entity cause) {
        return IafDamageRegistry.causeDragonIceDamage(cause);
    }

    @Override
    public void destroyArea(Level world, BlockPos center, EntityDragonBase destroyer) {
        IafDragonDestructionManager.destroyAreaIceCharge(world, center, destroyer);
    }

    @Override
    public float getDamage() {
        return (float) IafConfig.dragonAttackDamageIce;
    }

    @Override
    protected boolean shouldBurn() {
        return false;
    }


}
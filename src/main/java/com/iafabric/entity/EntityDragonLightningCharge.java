package com.iafabric.entity;

import com.iafabric.IafConfig;
import com.iafabric.entity.util.IDragonProjectile;
import com.iafabric.misc.IafDamageRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.projectile.AbstractHurtingProjectile;
import net.minecraft.world.level.Level;

import org.jetbrains.annotations.Nullable;

public class EntityDragonLightningCharge extends EntityDragonCharge implements IDragonProjectile {


    public EntityDragonLightningCharge(EntityType<? extends AbstractHurtingProjectile> type, Level worldIn) {
        super(type, worldIn);
    }


    public EntityDragonLightningCharge(EntityType<? extends AbstractHurtingProjectile> type, Level worldIn, double posX,
                                       double posY, double posZ, double accelX, double accelY, double accelZ) {
        super(type, worldIn, posX, posY, posZ, accelX, accelY, accelZ);
    }

    public EntityDragonLightningCharge(EntityType<? extends AbstractHurtingProjectile> type, Level worldIn,
                                       EntityDragonBase shooter, double accelX, double accelY, double accelZ) {
        super(type, worldIn, shooter, accelX, accelY, accelZ);
    }

    @Override
    public DamageSource causeDamage(@Nullable Entity cause) {
        return IafDamageRegistry.causeDragonLightningDamage(cause);
    }

    @Override
    public void destroyArea(Level world, BlockPos center, EntityDragonBase destroyer) {
        IafDragonDestructionManager.destroyAreaLightningCharge(world, center, destroyer);
    }

    @Override
    public float getDamage() {
        return (float) IafConfig.dragonAttackDamageLightning;
    }


}
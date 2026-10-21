package com.iafabric.entity;

import com.iafabric.item.IafItemRegistry;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.protocol.Packet;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.throwableitemprojectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import org.jetbrains.annotations.NotNull;

public class EntityCockatriceEgg extends IafThrowable {

    public EntityCockatriceEgg(EntityType<? extends IafThrowable> type, Level worldIn) {
        super(type, worldIn);
    }

    public EntityCockatriceEgg(EntityType<? extends IafThrowable> type, Level worldIn, LivingEntity throwerIn) {
        super(type, throwerIn, worldIn);
    }

    public EntityCockatriceEgg(EntityType<? extends IafThrowable> type, double x, double y, double z,
                               Level worldIn) {
        super(type, x, y, z, worldIn);
    }

    @Override
    public void handleEntityEvent(byte id) {
        if (id == 3) {
            for (int i = 0; i < 8; ++i) {
                this.level().addParticle(new ItemParticleOption(ParticleTypes.ITEM, this.getItem().getItem()), this.getX(), this.getY(), this.getZ(), (this.random.nextFloat() - 0.5D) * 0.08D, (this.random.nextFloat() - 0.5D) * 0.08D, (this.random.nextFloat() - 0.5D) * 0.08D);
            }
        }

    }

    /**
     * Called when this EntityThrowable hits a block or entity.
     */
    @Override
    protected void onHit(HitResult result) {
        Entity thrower = getOwner();
        if (result.getType() == HitResult.Type.ENTITY) {
            ((EntityHitResult) result).getEntity().hurtOrSimulate(((EntityHitResult) result).getEntity().damageSources().thrown(this, thrower), 0.0F);
        }

        if (!this.level().isClientSide()) {
            if (this.random.nextInt(4) == 0) {
                int i = 1;

                if (this.random.nextInt(32) == 0) {
                    i = 4;
                }

                for (int j = 0; j < i; ++j) {
                    EntityCockatrice cockatrice = new EntityCockatrice(IafEntityRegistry.COCKATRICE, this.level());
                    cockatrice.setAge(-24000);
                    cockatrice.setHen(this.random.nextBoolean());
                    cockatrice.snapTo(this.getX(), this.getY(), this.getZ(), this.getYRot(), 0.0F);
                    if (thrower instanceof Player) {
                        cockatrice.tame((Player) thrower);
                    }
                    this.level().addFreshEntity(cockatrice);
                }
            }

            this.level().broadcastEntityEvent(this, (byte) 3);
            this.remove(RemovalReason.DISCARDED);
        }
    }

    @Override
    protected Item getDefaultItem() {
        return IafItemRegistry.ROTTEN_EGG;
    }
}

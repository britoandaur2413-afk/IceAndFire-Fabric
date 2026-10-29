package com.iafabric.entity;

import net.minecraft.world.item.ItemStack;
import com.iafabric.util.EntityUtil;
import com.iafabric.item.IafItemRegistry;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.Packet;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import org.jetbrains.annotations.NotNull;

public class EntityDeathWormEgg extends IafThrowable {

    private boolean giant;

    public EntityDeathWormEgg(EntityType<? extends IafThrowable> type, Level worldIn) {
        super(type, worldIn);
    }

    public EntityDeathWormEgg(EntityType<? extends IafThrowable> type, LivingEntity throwerIn, Level worldIn,
                              boolean giant) {
        super(type, throwerIn, worldIn);
        this.giant = giant;
        this.setItem(new net.minecraft.world.item.ItemStack(this.getDefaultItem()));
    }

    public EntityDeathWormEgg(EntityType<? extends IafThrowable> type, double x, double y, double z,
                              Level worldIn, boolean giant) {
        super(type, x, y, z, worldIn);
        this.giant = giant;
        this.setItem(new net.minecraft.world.item.ItemStack(this.getDefaultItem()));
    }


    @Override
    public void handleEntityEvent(byte id) {
        if (id == 3) {
            for (int i = 0; i < 8; ++i) {
                this.level().addParticle(new ItemParticleOption(ParticleTypes.ITEM, this.getItem()), this.getX(), this.getY(), this.getZ(), (this.random.nextFloat() - 0.5D) * 0.08D, (this.random.nextFloat() - 0.5D) * 0.08D, (this.random.nextFloat() - 0.5D) * 0.08D);
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
            ((EntityHitResult) result).getEntity().hurt(((EntityHitResult) result).getEntity().damageSources().thrown(this, thrower), 0.0F);
        }

        if (!this.level().isClientSide) {
            float wormSize = 0.25F + (float) (Math.random() * 0.35F);

            EntityDeathWorm deathworm = new EntityDeathWorm(IafEntityRegistry.DEATH_WORM, this.level());
            deathworm.setVariant(random.nextInt(3));
            deathworm.setTame(true);
            deathworm.setWormHome(blockPosition());
            deathworm.setWormAge(1);
            deathworm.setDeathWormScale(giant ? (wormSize * 4) : wormSize);
            deathworm.moveTo(this.getX(), this.getY(), this.getZ(), this.getYRot(), 0.0F);
            if (thrower instanceof Player) {
                EntityUtil.setOwnerUUID(deathworm, thrower.getUUID());
            }
            this.level().addFreshEntity(deathworm);

            this.level().broadcastEntityEvent(this, (byte) 3);
            this.remove(RemovalReason.DISCARDED);
        }
    }

    @Override
    protected Item getDefaultItem() {
        return giant ? IafItemRegistry.DEATHWORM_EGG_GIGANTIC : IafItemRegistry.DEATHWORM_EGG;
    }
}

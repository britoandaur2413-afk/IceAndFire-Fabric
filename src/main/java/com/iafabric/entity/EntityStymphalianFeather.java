package com.iafabric.entity;

import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerLevel;
import com.iafabric.IafConfig;
import com.iafabric.item.IafItemRegistry;
import net.minecraft.network.protocol.Packet;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import org.jetbrains.annotations.NotNull;

public class EntityStymphalianFeather extends IafArrow {

    public EntityStymphalianFeather(EntityType<? extends AbstractArrow> t, Level worldIn) {
        super(t, worldIn);
    }

    public EntityStymphalianFeather(EntityType<? extends AbstractArrow> t, Level worldIn, LivingEntity shooter) {
        super(t, shooter, worldIn);
        this.setBaseDamage(IafConfig.stymphalianBirdFeatherAttackStength);
    }


    @Override
    public void remove(@NotNull RemovalReason reason) {
        super.remove(reason);
        if (IafConfig.stymphalianBirdFeatherDropChance > 0) {
            if (!level().isClientSide && this.random.nextInt(IafConfig.stymphalianBirdFeatherDropChance) == 0) {
                this.spawnAtLocation(getPickupItem(), 0.1F);
            }
        }

    }

    @Override
    public void tick() {
        super.tick();
        if (this.tickCount > 100) {
            this.remove(RemovalReason.DISCARDED);
        }
    }

    @Override
    protected void onHitEntity(@NotNull EntityHitResult entityHit) {
        Entity shootingEntity = this.getOwner();
        if (shootingEntity instanceof EntityStymphalianBird && entityHit.getEntity() != null && entityHit.getEntity() instanceof EntityStymphalianBird) {
            return;
        } else {
            super.onHitEntity(entityHit);
            if (entityHit.getEntity() != null && entityHit.getEntity() instanceof EntityStymphalianBird) {
                LivingEntity LivingEntity = (LivingEntity) entityHit.getEntity();
                LivingEntity.setArrowCount(LivingEntity.getArrowCount() - 1);
                ItemStack itemstack1 = LivingEntity.isUsingItem() ? LivingEntity.getUseItem() : ItemStack.EMPTY;
                if ((itemstack1.getItem() instanceof net.minecraft.world.item.ShieldItem)) {
                    damageShield(LivingEntity, 1.0F);
                }
            }

        }
    }

    protected void damageShield(LivingEntity entity, float damage) {
        if (damage >= 3.0F && (entity.getUseItem().getItem() instanceof net.minecraft.world.item.ShieldItem)) {
            ItemStack copyBeforeUse = entity.getUseItem().copy();
            int i = 1 + Mth.floor(damage);
            InteractionHand Hand = entity.getUsedItemHand();
            copyBeforeUse.hurtAndBreak(i, entity, EquipmentSlot.MAINHAND);
            if (entity.getUseItem().isEmpty()) {
                if (entity instanceof Player) {
                }

                if (Hand == net.minecraft.world.InteractionHand.MAIN_HAND) {
                    entity.setItemSlot(EquipmentSlot.MAINHAND, ItemStack.EMPTY);
                } else {
                    entity.setItemSlot(EquipmentSlot.OFFHAND, ItemStack.EMPTY);
                }
                this.playSound(SoundEvents.SHIELD_BREAK, 0.8F, 0.8F + this.level().getRandom().nextFloat() * 0.4F);
            }
        }
    }

    @Override
    protected ItemStack getDefaultPickupItem() {
        return new ItemStack(IafItemRegistry.STYMPHALIAN_BIRD_FEATHER);
    }
}

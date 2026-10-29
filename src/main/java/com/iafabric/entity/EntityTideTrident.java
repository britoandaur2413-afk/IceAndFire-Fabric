package com.iafabric.entity;

import com.iafabric.item.IafItemRegistry;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ThrownTrident;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;

/** A thrown tide trident: hits harder than a trident and goes through two more mobs (more with Piercing). */
public class EntityTideTrident extends ThrownTrident {

    private static final int ADDITIONALPIERCING = 2;
    private int entitiesHit = 0;

    public EntityTideTrident(EntityType<? extends ThrownTrident> type, Level worldIn) {
        super(type, worldIn);
        this.tridentItem = new ItemStack(IafItemRegistry.TIDE_TRIDENT);
    }

    public EntityTideTrident(Level worldIn, LivingEntity thrower, ItemStack thrownStackIn) {
        this(IafEntityRegistry.TIDE_TRIDENT, worldIn);
        this.setPos(thrower.getX(), thrower.getEyeY() - 0.1F, thrower.getZ());
        this.setOwner(thrower);
        this.tridentItem = thrownStackIn.copy();
        this.entityData.set(ID_LOYALTY, (byte) EnchantmentHelper.getLoyalty(thrownStackIn));
        this.entityData.set(ID_FOIL, thrownStackIn.hasFoil());
    }

    @Override
    protected ItemStack getPickupItem() {
        return new ItemStack(IafItemRegistry.TIDE_TRIDENT);
    }

    @Override
    protected void onHitEntity(EntityHitResult result) {
        Entity entity = result.getEntity();
        float damage = 12.0F;
        Entity owner = this.getOwner();
        DamageSource source = this.damageSources().trident(this, owner == null ? this : owner);
        if (entity instanceof LivingEntity living) {
            damage += EnchantmentHelper.getDamageBonus(this.tridentItem, living.getMobType());
        }
        entitiesHit++;
        if (entitiesHit >= getMaxPiercing()) {
            this.dealtDamage = true;
        }
        if (entity.hurt(source, damage)) {
            if (entity instanceof LivingEntity living) {
                if (owner instanceof LivingEntity thrower) {
                    EnchantmentHelper.doPostHurtEffects(living, thrower);
                    EnchantmentHelper.doPostDamageEffects(thrower, living);
                }
                this.doPostHurtEffects(living);
            }
        }
        this.playSound(SoundEvents.TRIDENT_HIT, 1.0F, 1.0F);
    }

    private int getMaxPiercing() {
        return ADDITIONALPIERCING + getPierceLevel();
    }
}

package com.iafabric.item;

import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public class ItemDragonFlesh extends ItemGenericFood {

    int dragonType;

    public ItemDragonFlesh(int dragonType) {
        super(8, 0.8F, true, false, false);
        this.dragonType = dragonType;
    }

    static String getNameForType(int dragonType) {
        return switch (dragonType) {
            case 0 -> "fire_dragon_flesh";
            case 1 -> "ice_dragon_flesh";
            case 2 -> "lightning_dragon_flesh";
            default -> "fire_dragon_flesh";
        };
    }

    @Override
    public void onFoodEaten(ItemStack stack, Level worldIn, LivingEntity livingEntity) {
        if (!worldIn.isClientSide()) {
            if (dragonType == 0) {
                livingEntity.igniteForSeconds(5);
            } else if (dragonType == 1) {
                livingEntity.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 100, 2));
            } else {
                if (!livingEntity.level().isClientSide()) {
                    LightningBolt lightningboltentity = EntityTypes.LIGHTNING_BOLT.create(livingEntity.level(), EntitySpawnReason.TRIGGERED);
                    lightningboltentity.snapTo(livingEntity.position());
                    if (!livingEntity.level().isClientSide()) {
                        livingEntity.level().addFreshEntity(lightningboltentity);
                    }
                }
            }
        }
    }
}

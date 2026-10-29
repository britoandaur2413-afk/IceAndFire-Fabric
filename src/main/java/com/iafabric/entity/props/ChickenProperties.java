package com.iafabric.entity.props;

import net.minecraft.world.item.ItemStack;
import net.minecraft.server.level.ServerLevel;
import com.iafabric.citadel.server.entity.CitadelEntityData;
import com.iafabric.IafConfig;
import com.iafabric.item.IafItemRegistry;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.LivingEntity;

import net.minecraft.util.RandomSource;

public class ChickenProperties {
    private static final String CHICKEN_DATA = "TimeUntilNextEggIaf";
    private static final RandomSource rand = RandomSource.create();

    private static int createDefaultTime() {
        return rand.nextInt(6000) + 6000;
    }

    private static CompoundTag createDefaultData(CompoundTag nbt) {
        nbt.putInt(CHICKEN_DATA, createDefaultTime());
        return nbt;
    }

    public static int getTimeRemaining(LivingEntity entity) {
        CompoundTag nbt = CitadelEntityData.getOrCreateCitadelTag(entity);
        if (!nbt.contains(CHICKEN_DATA)) {
            createDefaultData(nbt);
            CitadelEntityData.setCitadelTag(entity, nbt);
        }
        return nbt.iceandfire_fabric$getIntOr(CHICKEN_DATA, 0);
    }

    public static void setTimeRemaining(LivingEntity entity, int time) {
        CompoundTag nbt = CitadelEntityData.getOrCreateCitadelTag(entity);
        nbt.putInt(CHICKEN_DATA, time);
        CitadelEntityData.setCitadelTag(entity, nbt);
        // No need to message clients since this can stay server only
    }

    public static void tickChicken(LivingEntity entity) {
        int timeUntilNextEgg = getTimeRemaining(entity);
        if (timeUntilNextEgg <= 0) {
            if (entity.getRandom().nextInt(IafConfig.cockatriceEggChance + 1) == 0 && entity.tickCount > 30) {
                entity.playSound(SoundEvents.CHICKEN_HURT, 2.0F,
                    (rand.nextFloat() - rand.nextFloat()) * 0.2F + 1.0F);
                entity.playSound(SoundEvents.CHICKEN_EGG, 1.0F,
                    (rand.nextFloat() - rand.nextFloat()) * 0.2F + 1.0F);
                if (entity.level() instanceof ServerLevel serverLevel) {
                    entity.spawnAtLocation(new ItemStack(IafItemRegistry.ROTTEN_EGG), 1);
                }
            }
            setTimeRemaining(entity, createDefaultTime());
        } else {
            setTimeRemaining(entity, timeUntilNextEgg - 1);
        }
    }
}

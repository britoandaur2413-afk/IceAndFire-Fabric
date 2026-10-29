package com.iafabric.api;

import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public class FoodUtils {

    public static int getFoodPoints(Entity entity) {
        int foodPoints = Math.round(entity.getBbWidth() * entity.getBbHeight() * 10);
        if (entity instanceof AgeableMob) {
            return foodPoints;
        }
        if (entity instanceof Player) {
            return 15;
        }
        return 0;
    }

    public static int getFoodPoints(ItemStack item, boolean meatOnly, boolean includeFish) {
        if (item != null && item != ItemStack.EMPTY && item.getItem() != null && item.getItem().getFoodProperties() != null) {
            int food = item.getItem().getFoodProperties().getNutrition() * 10;
            if (!meatOnly) {
                return food;
            } else if (isMeat(item)) {
                return food;
            } else if (includeFish && item.getItem() == Items.COD) {
                return food;
            }
        }
        return 0;
    }

    /** Meat (1.20.1 tells it by the food, not by a tag). */
    public static boolean isMeat(ItemStack stack) {
        return stack.getItem().getFoodProperties() != null && stack.getItem().getFoodProperties().isMeat();
    }

    public static boolean isSeeds(ItemStack stack) {
        // (1.20.1 has no seeds tag)
        return stack.is(net.minecraft.world.item.Items.WHEAT_SEEDS) || stack.is(net.minecraft.world.item.Items.MELON_SEEDS)
            || stack.is(net.minecraft.world.item.Items.PUMPKIN_SEEDS) || stack.is(net.minecraft.world.item.Items.BEETROOT_SEEDS)
            || stack.is(net.minecraft.world.item.Items.TORCHFLOWER_SEEDS) || stack.is(net.minecraft.world.item.Items.PITCHER_POD);
    }
}

package com.iafabric.item;

import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;

public class ItemGenericFood extends Item {
    private final int healAmount;
    private final float saturation;

    public ItemGenericFood(int amount, float saturation, boolean isWolfFood, boolean eatFast, boolean alwaysEdible) {
        super(withFood(IafItemRegistry.props(), amount, saturation, isWolfFood, eatFast, alwaysEdible, null));
        this.healAmount = amount;
        this.saturation = saturation;
    }

    public ItemGenericFood(int amount, float saturation, boolean isWolfFood, boolean eatFast, boolean alwaysEdible, int stackSize) {
        super(withFood(IafItemRegistry.props(), amount, saturation, isWolfFood, eatFast, alwaysEdible, null).stacksTo(stackSize));
        this.healAmount = amount;
        this.saturation = saturation;
    }

    /** The food component (wolves eat meat). */
    public static Item.Properties withFood(Item.Properties properties, int amount, float saturation, boolean meat, boolean eatFast, boolean alwaysEdible, MobEffectInstance potion) {
        FoodProperties.Builder builder = new FoodProperties.Builder().nutrition(amount).saturationMod(saturation);
        if (meat) {
            builder.meat();
        }
        if (alwaysEdible) {
            builder.alwaysEat();
        }
        if (eatFast) {
            builder.fast();
        }
        if (potion != null) {
            builder.effect(potion, 1.0F);
        }
        return properties.food(builder.build());
    }

    @Override
    public ItemStack finishUsingItem(@NotNull ItemStack stack, @NotNull Level worldIn, @NotNull LivingEntity livingEntity) {
        this.onFoodEaten(stack, worldIn, livingEntity);
        return super.finishUsingItem(stack, worldIn, livingEntity);
    }

    public void onFoodEaten(ItemStack stack, Level worldIn, LivingEntity livingEntity) {
    }
}

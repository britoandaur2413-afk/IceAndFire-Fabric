package com.iafabric.item;

import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.Consumable;
import net.minecraft.world.item.component.Consumables;
import net.minecraft.world.item.consume_effects.ApplyStatusEffectsConsumeEffect;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;

public class ItemGenericFood extends Item {
    private final int healAmount;
    private final float saturation;

    public ItemGenericFood(int amount, float saturation, boolean isWolfFood, boolean eatFast, boolean alwaysEdible) {
        super(withFood(IafItemRegistry.props(), amount, saturation, eatFast, alwaysEdible, null));
        this.healAmount = amount;
        this.saturation = saturation;
    }

    public ItemGenericFood(int amount, float saturation, boolean isWolfFood, boolean eatFast, boolean alwaysEdible, int stackSize) {
        super(withFood(IafItemRegistry.props(), amount, saturation, eatFast, alwaysEdible, null).stacksTo(stackSize));
        this.healAmount = amount;
        this.saturation = saturation;
    }

    /** Food and eating components (wolves' food is the item tag minecraft:wolf_food now). */
    public static Item.Properties withFood(Item.Properties properties, int amount, float saturation, boolean eatFast, boolean alwaysEdible, MobEffectInstance potion) {
        FoodProperties.Builder builder = new FoodProperties.Builder().nutrition(amount).saturationModifier(saturation);
        if (alwaysEdible) {
            builder.alwaysEdible();
        }
        Consumable.Builder consumable = Consumables.defaultFood();
        if (eatFast) {
            consumable.consumeSeconds(0.8F);
        }
        if (potion != null) {
            consumable.onConsume(new ApplyStatusEffectsConsumeEffect(potion, 1.0F));
        }
        return properties.food(builder.build(), consumable.build());
    }

    @Override
    public ItemStack finishUsingItem(@NotNull ItemStack stack, @NotNull Level worldIn, @NotNull LivingEntity livingEntity) {
        this.onFoodEaten(stack, worldIn, livingEntity);
        return super.finishUsingItem(stack, worldIn, livingEntity);
    }

    public void onFoodEaten(ItemStack stack, Level worldIn, LivingEntity livingEntity) {
    }
}

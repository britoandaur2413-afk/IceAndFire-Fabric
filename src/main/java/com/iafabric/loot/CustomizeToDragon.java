package com.iafabric.loot;

import net.minecraft.world.level.storage.loot.functions.LootItemFunctionType;
import java.util.List;
import com.iafabric.util.ItemNbt;
import com.iafabric.item.IafItemRegistry;
import com.iafabric.entity.DragonType;
import com.iafabric.entity.EntityDragonBase;
import com.iafabric.item.*;
import net.minecraft.core.Holder;
import java.util.Optional;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.functions.LootItemConditionalFunction;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import org.jetbrains.annotations.NotNull;

import net.minecraft.util.RandomSource;

public class CustomizeToDragon extends LootItemConditionalFunction {

    public CustomizeToDragon(LootItemCondition[] conditions) {
        super(conditions);
    }

    public static final LootItemFunctionType TYPE = new LootItemFunctionType(new Serializer());

    @Override
    public LootItemFunctionType getType() {
        return TYPE;
    }

    @Override
    protected ItemStack run(ItemStack stack, @NotNull LootContext context) {
        if (!stack.isEmpty() && context.getParamOrNull(LootContextParams.THIS_ENTITY) instanceof EntityDragonBase) {
            RandomSource random = RandomSource.create();
            EntityDragonBase dragon = (EntityDragonBase) context.getParamOrNull(LootContextParams.THIS_ENTITY);
            if (dragon == null) {
                return stack;
            }

            if (stack.getItem() == IafItemRegistry.DRAGON_BONE) {
                stack.setCount(1 + random.nextInt(1 + (dragon.getAgeInDays() / 25)));
                return stack;
            }
            if (stack.getItem() instanceof ItemDragonScales) {
                stack.setCount(dragon.getAgeInDays() / 25 + random.nextInt(1 + (dragon.getAgeInDays() / 5)));
                return new ItemStack(dragon.getVariantScale(dragon.getVariant()), stack.getCount());
            }
            else if (stack.getItem() instanceof ItemDragonEgg) {
                if (dragon.isOldEnoughToDropLoot()) {
                    return new ItemStack(dragon.getVariantEgg(dragon.getVariant()), stack.getCount());
                } else {
                    stack.setCount(1 + random.nextInt(1 + (dragon.getAgeInDays() / 5)));
                    return new ItemStack(dragon.getVariantScale(dragon.getVariant()), stack.getCount());
                }
            }
            else if (stack.getItem() instanceof ItemDragonFlesh) {
                stack.setCount(1 + random.nextInt(1 + (dragon.getAgeInDays() / 25)));
                return new ItemStack(stack.getItem(), stack.getCount());
            }
            else if (stack.getItem() instanceof ItemDragonSkull) {
                ItemStack stack1 = new ItemStack(dragon.dragonType == DragonType.FIRE ? IafItemRegistry.DRAGON_SKULL_FIRE : IafItemRegistry.DRAGON_SKULL_ICE, stack.getCount());
                ItemNbt.setTag(stack1, ItemNbt.getTag(stack));
                return stack1;
            }
            if (stack.getItem() == IafItemRegistry.FIRE_DRAGON_BLOOD || stack.getItem() == IafItemRegistry.ICE_DRAGON_BLOOD) {
                return new ItemStack(dragon.dragonType == DragonType.FIRE ? IafItemRegistry.FIRE_DRAGON_BLOOD : IafItemRegistry.ICE_DRAGON_BLOOD, stack.getCount());
            }
            else if (stack.getItem() == IafItemRegistry.FIRE_DRAGON_HEART || stack.getItem() == IafItemRegistry.ICE_DRAGON_HEART) {
                return new ItemStack(dragon.dragonType == DragonType.FIRE ? IafItemRegistry.FIRE_DRAGON_HEART : IafItemRegistry.ICE_DRAGON_HEART, stack.getCount());
            }
        }
        return stack;
    }

    public static class Serializer extends LootItemConditionalFunction.Serializer<CustomizeToDragon> {
        @Override
        public CustomizeToDragon deserialize(com.google.gson.JsonObject json, com.google.gson.JsonDeserializationContext context, LootItemCondition[] conditions) {
            return new CustomizeToDragon(conditions);
        }
    }
}

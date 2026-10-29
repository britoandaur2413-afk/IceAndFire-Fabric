package com.iafabric.loot;

import net.minecraft.world.level.storage.loot.functions.LootItemFunctionType;
import java.util.List;
import com.iafabric.entity.EntitySeaSerpent;
import com.iafabric.item.IafItemRegistry;
import com.iafabric.item.ItemSeaSerpentScales;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.Holder;
import java.util.Optional;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.functions.LootItemConditionalFunction;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import org.jetbrains.annotations.NotNull;

public class CustomizeToSeaSerpent extends LootItemConditionalFunction {

    public static final MapCodec<CustomizeToSeaSerpent> MAP_CODEC = RecordCodecBuilder.mapCodec(i -> commonFields(i).apply(i, CustomizeToSeaSerpent::new));

    public CustomizeToSeaSerpent(List<LootItemCondition> conditions) {
        super(conditions);
    }

    public static final LootItemFunctionType<CustomizeToSeaSerpent> TYPE = new LootItemFunctionType<>(MAP_CODEC);

    @Override
    public LootItemFunctionType<CustomizeToSeaSerpent> getType() {
        return TYPE;
    }

    @Override
    public ItemStack run(ItemStack stack, @NotNull LootContext context) {
        if (!stack.isEmpty() && context.getParamOrNull(LootContextParams.THIS_ENTITY) instanceof EntitySeaSerpent seaSerpent) {
            final int ancientModifier = seaSerpent.isAncient() ? 2 : 1;
            if (stack.getItem() instanceof ItemSeaSerpentScales) {
                stack.setCount(1 + seaSerpent.getRandom().nextInt(1 + (int) Math.ceil(seaSerpent.getSeaSerpentScale() * 3 * ancientModifier)));
                return new ItemStack(seaSerpent.getEnum().scale, stack.getCount());
            }
            if (stack.getItem() == IafItemRegistry.SERPENT_FANG) {
                stack.setCount(1 + seaSerpent.getRandom().nextInt(1 + (int) Math.ceil(seaSerpent.getSeaSerpentScale() * 2 * ancientModifier)));
                return stack;
            }
        }
        return stack;
    }
}

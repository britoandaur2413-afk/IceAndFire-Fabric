package com.iafabric.item;



import java.util.function.Consumer;
import com.iafabric.IafMod;
import com.iafabric.enums.EnumDragonEgg;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;

import org.jetbrains.annotations.Nullable;
import java.util.List;

public class ItemDragonScales extends Item {
    EnumDragonEgg type;

    public ItemDragonScales(EnumDragonEgg type) {
        super(IafItemRegistry.props());
        this.type = type;
    }

        /** The shared translation key (getDescriptionId is final now). */
    public String iafDescriptionId() {
        return "item.iceandfire_fabric.dragonscales";
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip, TooltipFlag flagIn) {
        tooltip.add(Component.translatable("dragon." + type.toString().toLowerCase()).withStyle(type.color));
    }

    @Override
    public Component getName(ItemStack stack) {
        return Component.translatable(this.iafDescriptionId());
    }
}

package com.iafabric.item;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;

import java.util.function.Consumer;

/** Carried anywhere in the inventory, the owner's dragons do not hurt them (PillagerDragons.allowDamage). */
public class ItemRiderSeal extends Item {
    public ItemRiderSeal() {
        super(IafItemRegistry.props().stacksTo(1));
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, TooltipDisplay display, Consumer<Component> tooltip, TooltipFlag flag) {
        tooltip.accept(Component.translatable("item.iceandfire_fabric.rider_seal.desc_0").withStyle(ChatFormatting.GRAY));
    }
}

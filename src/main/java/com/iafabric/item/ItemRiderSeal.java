package com.iafabric.item;

import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.function.Consumer;

/** Carried anywhere in the inventory, the owner's dragons do not hurt them (PillagerDragons.allowDamage). */
public class ItemRiderSeal extends Item {
    public ItemRiderSeal() {
        super(IafItemRegistry.props().stacksTo(1));
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("item.iceandfire_fabric.rider_seal.desc_0").withStyle(ChatFormatting.GRAY));
    }
}

package com.iafabric.item;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;

import java.util.function.Consumer;

/** Dragonsteel armor; its defense and durability come from the config (read before the items are made). */
public class ItemDragonsteelArmor extends IafArmorItem implements IProtectAgainstDragonItem {

    public ItemDragonsteelArmor(IafArmorMaterial material, int renderIndex, EquipmentSlot slot) {
        super(material, slot, IafItemRegistry.props());
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, TooltipDisplay display, Consumer<Component> tooltip, TooltipFlag flagIn) {
        tooltip.accept(Component.translatable("item.dragonscales_armor.desc").withStyle(ChatFormatting.GRAY));
    }
}

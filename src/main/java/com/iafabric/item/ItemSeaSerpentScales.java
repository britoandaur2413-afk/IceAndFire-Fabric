package com.iafabric.item;




import net.minecraft.world.item.Item;
import java.util.function.Consumer;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import org.jspecify.annotations.Nullable;
import java.util.List;

public class ItemSeaSerpentScales extends ItemGeneric {

    private final ChatFormatting color;
    private final String colorName;

    public ItemSeaSerpentScales(String colorName, ChatFormatting color) {
        super();
        this.color = color;
        this.colorName = colorName;
    }


    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, TooltipDisplay display, Consumer<Component> tooltip, TooltipFlag flagIn) {
        tooltip.accept(Component.translatable("sea_serpent." + colorName).withStyle(color));
    }
}

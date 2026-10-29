package com.iafabric.block;


import net.minecraft.world.level.material.MapColor;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import org.jetbrains.annotations.NotNull;

import org.jetbrains.annotations.Nullable;
import java.util.List;


public class BlockSeaSerpentScales extends Block implements IBlockTooltip {
    ChatFormatting color;
    String name;

    public BlockSeaSerpentScales(String name, ChatFormatting color) {
        super(
            IafBlockRegistry.props().mapColor(MapColor.STONE)
                .strength(30F, 500F)
                .sound(SoundType.STONE)
                .requiresCorrectToolForDrops()
        );

        this.color = color;
        this.name = name;
    }

        public void appendBlockTooltip(java.util.function.Consumer<Component> tooltip) {
        tooltip.accept(Component.translatable("sea_serpent." + name).withStyle(color));
    }
}

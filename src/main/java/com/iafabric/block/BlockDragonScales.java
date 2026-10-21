package com.iafabric.block;


import net.minecraft.world.level.material.MapColor;
import com.iafabric.enums.EnumDragonEgg;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import org.jetbrains.annotations.NotNull;

import org.jspecify.annotations.Nullable;
import java.util.List;

public class BlockDragonScales extends Block implements IDragonProof, IBlockTooltip {
    EnumDragonEgg type;

    public BlockDragonScales(EnumDragonEgg type) {
        super(
            IafBlockRegistry.props().mapColor(MapColor.STONE)
                .dynamicShape()
                .strength(30F, 500)
                .sound(SoundType.STONE)
                .requiresCorrectToolForDrops()
        );

        this.type = type;
    }


        public void appendBlockTooltip(java.util.function.Consumer<Component> tooltip) {
        tooltip.accept(Component.translatable("dragon." + type.toString().toLowerCase()).withStyle(type.color));
    }
}

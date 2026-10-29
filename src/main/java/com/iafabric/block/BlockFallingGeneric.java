package com.iafabric.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;


import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.FallingBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;

public class BlockFallingGeneric extends FallingBlock {
    public Item itemBlock;

    public BlockFallingGeneric(MapColor materialIn, float hardness, float resistance, SoundType sound) {
        super(
            IafBlockRegistry.props().mapColor(materialIn)
                .sound(sound)
                .strength(hardness, resistance)
        );
    }

    @SuppressWarnings("deprecation")
    public BlockFallingGeneric(MapColor materialIn, float hardness, float resistance, SoundType sound, boolean slippery) {
        super(
            IafBlockRegistry.props().mapColor(materialIn)
                .sound(sound)
                .strength(hardness, resistance)
                .friction(0.98F)
        );
    }


    public int getDustColor(BlockState blkst, net.minecraft.world.level.BlockGetter level, BlockPos pos) {
        return -8356741;
    }

    @Override
    protected MapCodec<? extends FallingBlock> codec() {
        return MapCodec.unit(this);
    }
}

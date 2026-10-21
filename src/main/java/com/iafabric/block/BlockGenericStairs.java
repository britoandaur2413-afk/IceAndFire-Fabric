package com.iafabric.block;

import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;

public class BlockGenericStairs extends StairBlock {

    public BlockGenericStairs(BlockState modelState) {
        super(
            modelState,
            IafBlockRegistry.props()
                .strength(20F)
        );
    }
}

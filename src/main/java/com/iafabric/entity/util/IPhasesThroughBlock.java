package com.iafabric.entity.util;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.LevelAccessor;

public interface IPhasesThroughBlock {

    boolean canPhaseThroughBlock(LevelAccessor world, BlockPos pos);
}

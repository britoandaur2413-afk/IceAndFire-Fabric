package com.iafabric.entity.tile;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;

import org.jetbrains.annotations.Nullable;

public class TileEntityDragonforgeBrick extends BlockEntity {

    public TileEntityDragonforgeBrick(BlockPos pos, BlockState state) {
        super(IafTileEntityRegistry.DRAGONFORGE_BRICK, pos, state);
    }


    public BlockEntity getConnectedTileEntity() {
        for (Direction facing : Direction.values()) {
            if (level.getBlockEntity(worldPosition.relative(facing)) instanceof TileEntityDragonforge) {
                return level.getBlockEntity(worldPosition.relative(facing));
            }
        }
        return null;
    }

}

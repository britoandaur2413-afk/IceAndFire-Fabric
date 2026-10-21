package com.iafabric.block;

import net.minecraft.core.particles.ParticleTypes;


import net.minecraft.world.level.material.MapColor;
import com.iafabric.IafMod;
import com.iafabric.enums.EnumParticles;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.WallTorchBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;

import net.minecraft.util.RandomSource;

public class BlockDreadTorchWall extends WallTorchBlock implements IDreadBlock {

    public BlockDreadTorchWall() {
        super(ParticleTypes.SMOKE,
            IafBlockRegistry.props().mapColor(MapColor.WOOD)
                .lightLevel((state) -> {
                    return 5;
                })
                .sound(SoundType.STONE)
                .noOcclusion()
                .dynamicShape()
                .noCollision()
                .overrideLootTable(IafBlockRegistry.DREAD_TORCH.getLootTable())
        );
    }

    @Override
    public void animateTick(BlockState stateIn, @NotNull Level worldIn, BlockPos pos, @NotNull RandomSource rand) {
        Direction direction = stateIn.getValue(FACING);
        double d0 = (double) pos.getX() + 0.5D;
        double d1 = (double) pos.getY() + 0.7D;
        double d2 = (double) pos.getZ() + 0.5D;
        double d3 = 0.22D;
        double d4 = 0.27D;
        Direction direction1 = direction.getOpposite();
        IafMod.PROXY.spawnParticle(EnumParticles.Dread_Torch, d0 + 0.27D * (double) direction1.getStepX(), d1 + 0.22D, d2 + 0.27D * (double) direction1.getStepZ(), 0.0D, 0.0D, 0.0D);
    }
}
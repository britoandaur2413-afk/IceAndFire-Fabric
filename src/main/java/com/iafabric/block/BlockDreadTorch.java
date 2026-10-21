package com.iafabric.block;

import net.minecraft.core.particles.ParticleTypes;


import net.minecraft.world.level.material.MapColor;
import com.iafabric.IafMod;
import com.iafabric.enums.EnumParticles;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.TorchBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;

import net.minecraft.util.RandomSource;

public class BlockDreadTorch extends TorchBlock implements IDreadBlock, IWallBlock {

    public BlockDreadTorch() {
        super(ParticleTypes.SMOKE,
            IafBlockRegistry.props().mapColor(MapColor.WOOD)
                .lightLevel((state) -> {
                    return 5;
                })
                .sound(SoundType.STONE)
                .noOcclusion()
                .dynamicShape()
                .noCollision()
        );
    }

    @Override
    public void animateTick(@NotNull BlockState stateIn, @NotNull Level worldIn, BlockPos pos, @NotNull RandomSource rand) {
        // Direction Direction = stateIn.get(FACING);
        double d0 = (double) pos.getX() + 0.5D;
        double d1 = (double) pos.getY() + 0.6D;
        double d2 = (double) pos.getZ() + 0.5D;
        double d3 = 0.22D;
        double d4 = 0.27D;
        IafMod.PROXY.spawnParticle(EnumParticles.Dread_Torch, d0, d1, d2, 0.0D, 0.0D, 0.0D);
  /*   if (Direction.getAxis().isHorizontal()) {
            Direction Direction1 = Direction.getOpposite();
            //worldIn.spawnParticle(ParticleTypes.SMOKE_NORMAL, d0 + 0.27D * (double)Direction1.getXOffset(), d1 + 0.22D, d2 + 0.27D * (double)Direction1.getZOffset(), 0.0D, 0.0D, 0.0D);
            IafMod.PROXY.spawnParticle("dread_torch", d0 + 0.27D * (double) Direction1.getXOffset(), d1 + 0.22D, d2 + 0.27D * (double) Direction1.getZOffset(), 0.0D, 0.0D, 0.0D);

        } else {
            //worldIn.spawnParticle(ParticleTypes.SMOKE_NORMAL, d0, d1, d2, 0.0D, 0.0D, 0.0D);
        }*/
    }

    @Override
    public Block wallBlock() {
        return IafBlockRegistry.DREAD_TORCH_WALL;
    }
}
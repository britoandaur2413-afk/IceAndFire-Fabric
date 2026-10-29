package com.iafabric.world.gen.processor;

import com.iafabric.world.IafProcessors;
import com.mojang.serialization.MapCodec;
import com.iafabric.block.IafBlockRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessor;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessorType;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import org.jetbrains.annotations.NotNull;

import org.jetbrains.annotations.Nullable;
import net.minecraft.util.RandomSource;

public class DreadPortalProcessor extends StructureProcessor {

    private final float integrity = 1.0F;
    /** Only placed from code (the exit portal): the codec is there for the processor's type. */
    public static final com.mojang.serialization.MapCodec<DreadPortalProcessor> CODEC = com.mojang.serialization.MapCodec.unit(() -> new DreadPortalProcessor(BlockPos.ZERO, null, null));

    public DreadPortalProcessor(BlockPos position, StructurePlaceSettings settings, Biome biome) {
    }

    public static BlockState getRandomCrackedBlock(@Nullable BlockState prev, RandomSource random) {
        float rand = random.nextFloat();
        if (rand < 0.3) {
            return IafBlockRegistry.DREAD_STONE_BRICKS.defaultBlockState();
        } else if (rand < 0.6) {
            return IafBlockRegistry.DREAD_STONE_BRICKS_CRACKED.defaultBlockState();
        } else {
            return IafBlockRegistry.DREAD_STONE_BRICKS_MOSSY.defaultBlockState();
        }
    }

    @Override
    public StructureTemplate.@Nullable StructureBlockInfo processBlock(LevelReader world, BlockPos pos, BlockPos p_230386_3_, StructureTemplate.StructureBlockInfo templateRelativePosInfo, StructureTemplate.StructureBlockInfo blockInfoIn, StructurePlaceSettings settings) {
        RandomSource random = settings.getRandom(blockInfoIn.pos());
        BlockPos at = blockInfoIn.pos();
        if (random.nextFloat() <= integrity) {
            if (blockInfoIn.state().getBlock() == Blocks.DIAMOND_BLOCK) {
                return new StructureTemplate.StructureBlockInfo(at, IafBlockRegistry.DREAD_PORTAL.defaultBlockState(), null);
            }
            if (blockInfoIn.state().getBlock() == IafBlockRegistry.DREAD_STONE_BRICKS) {
                BlockState state = getRandomCrackedBlock(null, random);
                return new StructureTemplate.StructureBlockInfo(at, state, null);
            }
            return blockInfoIn;
        }
        return blockInfoIn;

    }

    @Override
    protected StructureProcessorType<?> getType() {
        return IafProcessors.DREADPORTALPROCESSOR;
    }
}

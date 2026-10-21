package com.iafabric.world.gen;

import net.minecraft.world.level.levelgen.feature.Feature;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.chunk.ChunkGenerator;

/** What a feature is placed with (the old FeaturePlaceContext, which 26.x features no longer get). */
public record FeatureContext(WorldGenLevel level, ChunkGenerator chunkGenerator, RandomSource random, BlockPos origin) {
}

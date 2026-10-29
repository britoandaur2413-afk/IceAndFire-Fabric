package com.iafabric.world.gen;

import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

/**
 * A feature of the mod without configuration: placed with the level, generator, random and origin, as the 26.x
 * features are (worldgen/configured_feature/&lt;name&gt;.json gives it an empty config).
 */
public abstract class IafFeature extends Feature<NoneFeatureConfiguration> {
	protected IafFeature() {
		super(NoneFeatureConfiguration.CODEC);
	}

	@Override
	public final boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
		return place(context.level(), context.chunkGenerator(), context.random(), context.origin());
	}

	public abstract boolean place(WorldGenLevel level, ChunkGenerator generator, RandomSource random, BlockPos origin);
}

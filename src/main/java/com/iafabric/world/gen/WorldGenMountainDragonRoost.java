package com.iafabric.world.gen;

import com.iafabric.world.IafWorldData;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.feature.Feature;

/**
 * A dragon nest in the mountains and hills: the dragon of the land's climate (ice where it is cold, fire on the warm
 * peaks, lightning on the mild ones). The nests of the other lands need high ground too (DragonNests).
 */
public class WorldGenMountainDragonRoost implements Feature, TypedFeature {
	private final WorldGenFireDragonRoosts fire = new WorldGenFireDragonRoosts(null);
	private final WorldGenIceDragonRoosts ice = new WorldGenIceDragonRoosts(null);
	private final WorldGenLightningDragonRoosts lightning = new WorldGenLightningDragonRoosts(null);
	private final MapCodec<WorldGenMountainDragonRoost> featureCodec = MapCodec.unit(this);

	public WorldGenMountainDragonRoost(Object config) {
	}

	@Override
	public boolean place(WorldGenLevel level, ChunkGenerator generator, RandomSource random, BlockPos origin) {
		Biome biome = level.getBiome(origin).value();
		float temperature = biome.getBaseTemperature();
		if (temperature < 0.3F) {
			return ice.place(level, generator, random, origin);
		}
		if (temperature >= 0.95F) {
			return fire.place(level, generator, random, origin);
		}
		return lightning.place(level, generator, random, origin);
	}

	@Override
	public IafWorldData.FeatureType getFeatureType() {
		return IafWorldData.FeatureType.SURFACE;
	}

	@Override
	public MapCodec<WorldGenMountainDragonRoost> codec() {
		return featureCodec;
	}
}

package com.iafabric.world.feature;

import com.iafabric.world.gen.IafFeature;
import com.iafabric.world.gen.FeatureContext;
import net.minecraft.world.level.chunk.ChunkGenerator;
import com.mojang.serialization.MapCodec;
import com.iafabric.IafConfig;
import com.iafabric.entity.EntityDeathWorm;
import com.iafabric.entity.IafEntityRegistry;
import com.iafabric.world.IafWorldRegistry;
import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.Feature;

import net.minecraft.util.RandomSource;

public class SpawnDeathWorm extends IafFeature {

    public SpawnDeathWorm(Object configFactoryIn) {
    }

    @Override
    public boolean place(WorldGenLevel featureLevel, ChunkGenerator featureGenerator, RandomSource featureRandom, BlockPos featureOrigin) {
        FeatureContext context = new FeatureContext(featureLevel, featureGenerator, featureRandom, featureOrigin);
        WorldGenLevel worldIn = context.level();
        RandomSource rand = context.random();
        BlockPos position = context.origin();
        position = worldIn.getHeightmapPos(Heightmap.Types.WORLD_SURFACE_WG, position.offset(8, 0, 8));

        if (IafConfig.spawnDeathWorm && IafWorldRegistry.isFarEnoughFromSpawn(worldIn, position)) {
            if (rand.nextInt(IafConfig.deathWormSpawnRate + 1) == 0) {
                EntityDeathWorm deathWorm = IafEntityRegistry.DEATH_WORM.create(worldIn.getLevel());
                deathWorm.setPos(position.getX() + 0.5F, position.getY() + 1, position.getZ() + 0.5F);
                deathWorm.finalizeSpawn(worldIn, worldIn.getCurrentDifficultyAt(position), MobSpawnType.CHUNK_GENERATION, null);
                worldIn.addFreshEntity(deathWorm);
            }
        }

        return false;
    }

}

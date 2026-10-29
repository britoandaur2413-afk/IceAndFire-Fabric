package com.iafabric.world.feature;

import com.iafabric.world.gen.IafFeature;
import com.iafabric.world.gen.FeatureContext;
import net.minecraft.world.level.chunk.ChunkGenerator;
import com.mojang.serialization.MapCodec;
import net.minecraft.world.entity.MobSpawnType;
import com.iafabric.IafConfig;
import com.iafabric.entity.EntityHippocampus;
import com.iafabric.entity.IafEntityRegistry;
import com.iafabric.world.IafWorldRegistry;
import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.material.Fluids;

import net.minecraft.util.RandomSource;

public class SpawnHippocampus extends IafFeature {

    public SpawnHippocampus(Object configFactoryIn) {
    }

    @Override
    public boolean place(WorldGenLevel featureLevel, ChunkGenerator featureGenerator, RandomSource featureRandom, BlockPos featureOrigin) {
        FeatureContext context = new FeatureContext(featureLevel, featureGenerator, featureRandom, featureOrigin);
        WorldGenLevel worldIn = context.level();
        RandomSource rand = context.random();
        BlockPos position = context.origin();

        position = worldIn.getHeightmapPos(Heightmap.Types.WORLD_SURFACE_WG, position.offset(8, 0, 8));
        BlockPos oceanPos = worldIn.getHeightmapPos(Heightmap.Types.OCEAN_FLOOR_WG, position.offset(8, 0, 8));

        if (IafConfig.spawnHippocampus && rand.nextInt(IafConfig.hippocampusSpawnChance + 1) == 0) {
            for (int i = 0; i < rand.nextInt(5); i++) {
                BlockPos pos = oceanPos.offset(rand.nextInt(10) - 5, rand.nextInt(30), rand.nextInt(10) - 5);
                if (worldIn.getFluidState(pos).getType() == Fluids.WATER) {
                    EntityHippocampus campus = IafEntityRegistry.HIPPOCAMPUS.create(worldIn.getLevel());
                    campus.setVariant(rand.nextInt(6));
                    campus.moveTo(pos.getX() + 0.5F, pos.getY() + 0.5F, pos.getZ() + 0.5F, 0, 0);
                    worldIn.addFreshEntity(campus);
                }
            }
        }

        return false;
    }

}

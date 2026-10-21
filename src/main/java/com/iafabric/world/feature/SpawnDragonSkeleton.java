package com.iafabric.world.feature;

import net.minecraft.world.entity.EntitySpawnReason;
import com.iafabric.world.gen.FeatureContext;
import net.minecraft.world.level.chunk.ChunkGenerator;
import com.mojang.serialization.MapCodec;
import com.iafabric.IafConfig;
import com.iafabric.entity.EntityDragonBase;
import com.iafabric.world.IafWorldRegistry;
import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.Feature;

import net.minecraft.util.RandomSource;

public class SpawnDragonSkeleton implements Feature {

    protected EntityType<? extends EntityDragonBase> dragonType;

    public SpawnDragonSkeleton(EntityType<? extends EntityDragonBase> dt, Object configFactoryIn) {
        dragonType = dt;
    }

    @Override
    public boolean place(WorldGenLevel featureLevel, ChunkGenerator featureGenerator, RandomSource featureRandom, BlockPos featureOrigin) {
        FeatureContext context = new FeatureContext(featureLevel, featureGenerator, featureRandom, featureOrigin);
        WorldGenLevel worldIn = context.level();
        RandomSource rand = context.random();
        BlockPos position = context.origin();

        position = worldIn.getHeightmapPos(Heightmap.Types.WORLD_SURFACE_WG, position.offset(8, 0, 8));

        if (IafConfig.generateDragonSkeletons) {
            if (rand.nextInt(IafConfig.generateDragonSkeletonChance + 1) == 0) {
                EntityDragonBase dragon = dragonType.create(worldIn.getLevel(), EntitySpawnReason.TRIGGERED);
                dragon.setPos(position.getX() + 0.5F, position.getY() + 1, position.getZ() + 0.5F);
                int dragonage = 10 + rand.nextInt(100);
                dragon.growDragon(dragonage);
                dragon.modelDeadProgress = 20;
                dragon.setModelDead(true);
                dragon.setDeathStage((dragonage / 5) / 2);
                dragon.setYRot(rand.nextInt(360));
                worldIn.addFreshEntity(dragon);
                com.iafabric.world.FairyLandmarks.note(worldIn, com.iafabric.world.FairyLandmarks.Kind.SKELETON, position.above());
            }
        }

        return false;
    }

    private final MapCodec<SpawnDragonSkeleton> featureCodec = MapCodec.unit(this);

    @Override
    public MapCodec<SpawnDragonSkeleton> codec() {
        return featureCodec;
    }
}

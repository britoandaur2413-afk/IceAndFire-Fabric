package com.iafabric.world.feature;

import com.iafabric.world.gen.IafFeature;
import com.iafabric.world.gen.FeatureContext;
import net.minecraft.world.level.chunk.ChunkGenerator;
import com.mojang.serialization.MapCodec;
import net.minecraft.world.entity.EntityType;
import com.iafabric.IafConfig;
import com.iafabric.entity.EntityCyclops;
import com.iafabric.entity.IafEntityRegistry;
import com.iafabric.world.IafWorldRegistry;
import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.animal.Sheep;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.Feature;

import net.minecraft.util.RandomSource;

public class SpawnWanderingCyclops extends IafFeature {

    public SpawnWanderingCyclops(Object configFactoryIn) {
    }

    @Override
    public boolean place(WorldGenLevel featureLevel, ChunkGenerator featureGenerator, RandomSource featureRandom, BlockPos featureOrigin) {
        FeatureContext context = new FeatureContext(featureLevel, featureGenerator, featureRandom, featureOrigin);
        WorldGenLevel worldIn = context.level();
        RandomSource rand = context.random();
        BlockPos position = context.origin();

        position = worldIn.getHeightmapPos(Heightmap.Types.WORLD_SURFACE_WG, position.offset(8, 0, 8));

        if (IafConfig.generateWanderingCyclops && IafWorldRegistry.isFarEnoughFromSpawn(worldIn, position)) {
            if (rand.nextInt(IafConfig.spawnWanderingCyclopsChance + 1) == 0 && rand.nextInt(12) == 0) {
                EntityCyclops cyclops = IafEntityRegistry.CYCLOPS.create(worldIn.getLevel());
                cyclops.setPos(position.getX() + 0.5F, position.getY() + 1, position.getZ() + 0.5F);
                cyclops.finalizeSpawn(worldIn, worldIn.getCurrentDifficultyAt(position), MobSpawnType.SPAWNER, null, null);
                worldIn.addFreshEntity(cyclops);
                com.iafabric.world.FairyLandmarks.note(worldIn, com.iafabric.world.FairyLandmarks.Kind.CYCLOPS, position);
                for (int i = 0; i < 3 + rand.nextInt(3); i++) {
                    Sheep sheep = EntityType.SHEEP.create(worldIn.getLevel());
                    sheep.setPos(position.getX() + 0.5F, position.getY() + 1, position.getZ() + 0.5F);
                    sheep.setColor(Sheep.getRandomSheepColor(sheep.getRandom()));
                    worldIn.addFreshEntity(sheep);
                }
            }
        }

        return false;
    }

}

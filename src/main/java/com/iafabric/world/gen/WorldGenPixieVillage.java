package com.iafabric.world.gen;

import net.minecraft.world.level.chunk.ChunkGenerator;
import com.mojang.serialization.MapCodec;
import com.iafabric.IafConfig;
import com.iafabric.block.BlockPixieHouse;
import com.iafabric.block.IafBlockRegistry;
import com.iafabric.entity.EntityPixie;
import com.iafabric.entity.IafEntityRegistry;
import com.iafabric.world.IafWorldData;
import com.iafabric.world.IafWorldRegistry;
import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.Feature;

import net.minecraft.util.RandomSource;

public class WorldGenPixieVillage extends IafFeature implements TypedFeature {
    public WorldGenPixieVillage(Object configFactoryIn) {
    }

    @Override
    public boolean place(WorldGenLevel featureLevel, ChunkGenerator featureGenerator, RandomSource featureRandom, BlockPos featureOrigin) {
        FeatureContext context = new FeatureContext(featureLevel, featureGenerator, featureRandom, featureOrigin);
        WorldGenLevel worldIn = context.level();
        RandomSource rand = context.random();
        BlockPos position = context.origin();

        if (rand.nextInt(IafConfig.spawnPixiesChance) != 0 || !IafWorldRegistry.isFarEnoughFromSpawn(worldIn, position)) {
            return false;
        }

        int maxRoads = IafConfig.pixieVillageSize + rand.nextInt(5);
        BlockPos buildPosition = position;
        int placedRoads = 0;
        while(placedRoads < maxRoads){
            int roadLength = 10 + rand.nextInt(15);
            Direction buildingDirection = Direction.from2DDataValue(rand.nextInt(3));
            for(int i = 0; i < roadLength; i++) {
                BlockPos buildPosition2 = buildPosition.relative(buildingDirection, i);
                buildPosition2 = worldIn.getHeightmapPos(Heightmap.Types.WORLD_SURFACE_WG, buildPosition2).below();
                if (worldIn.getBlockState(buildPosition2).getFluidState().isEmpty()) {
                    worldIn.setBlock(buildPosition2, Blocks.DIRT_PATH.defaultBlockState(), 2);
                } else {
                    worldIn.setBlock(buildPosition2, Blocks.SPRUCE_PLANKS.defaultBlockState(), 2);
                }
                if (rand.nextInt(8) == 0) {
                    Direction houseDir = rand.nextBoolean() ? buildingDirection.getClockWise() : buildingDirection.getCounterClockWise();
                    BlockState houseState = IafBlockRegistry.PIXIE_HOUSE_OAK.defaultBlockState();
                    int houseColor = rand.nextInt(5);
                    houseState = switch (houseColor) {
                        case 0 -> IafBlockRegistry.PIXIE_HOUSE_MUSHROOM_RED.defaultBlockState().setValue(BlockPixieHouse.FACING, houseDir.getOpposite());
                        case 1 -> IafBlockRegistry.PIXIE_HOUSE_MUSHROOM_BROWN.defaultBlockState().setValue(BlockPixieHouse.FACING, houseDir.getOpposite());
                        case 2 -> IafBlockRegistry.PIXIE_HOUSE_OAK.defaultBlockState().setValue(BlockPixieHouse.FACING, houseDir.getOpposite());
                        case 3 -> IafBlockRegistry.PIXIE_HOUSE_BIRCH.defaultBlockState().setValue(BlockPixieHouse.FACING, houseDir.getOpposite());
                        case 4 -> IafBlockRegistry.PIXIE_HOUSE_SPRUCE.defaultBlockState().setValue(BlockPixieHouse.FACING, houseDir.getOpposite());
                        case 5 -> IafBlockRegistry.PIXIE_HOUSE_DARK_OAK.defaultBlockState().setValue(BlockPixieHouse.FACING, houseDir.getOpposite());
                        default -> houseState;
                    };
                    EntityPixie pixie = IafEntityRegistry.PIXIE.create(worldIn.getLevel());
                    pixie.finalizeSpawn(worldIn, worldIn.getCurrentDifficultyAt(buildPosition2.above()), MobSpawnType.SPAWNER, null, null);
                    pixie.setPos(buildPosition2.getX(), buildPosition2.getY() + 2, buildPosition2.getZ());
                    pixie.setPersistenceRequired();
                    worldIn.addFreshEntity(pixie);

                    worldIn.setBlock(buildPosition2.relative(houseDir).above(), houseState, 2);
                    if (!worldIn.getBlockState(buildPosition2.relative(houseDir)).canOcclude()) {
                        worldIn.setBlock(buildPosition2.relative(houseDir), Blocks.COARSE_DIRT.defaultBlockState(), 2);
                        worldIn.setBlock(buildPosition2.relative(houseDir).below(), Blocks.COARSE_DIRT.defaultBlockState(), 2);
                    }
                }
            }
            buildPosition = buildPosition.relative(buildingDirection, rand.nextInt(roadLength));
            placedRoads++;
        }
        com.iafabric.world.FairyLandmarks.note(worldIn, com.iafabric.world.FairyLandmarks.Kind.PIXIES, worldIn.getHeightmapPos(Heightmap.Types.WORLD_SURFACE_WG, position));

        return true;
    }

    @Override
    public IafWorldData.FeatureType getFeatureType() {
        return IafWorldData.FeatureType.SURFACE;
    }

}

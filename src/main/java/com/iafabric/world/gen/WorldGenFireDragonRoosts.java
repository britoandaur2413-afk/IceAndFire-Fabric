package com.iafabric.world.gen;


import net.minecraft.resources.ResourceKey;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.chunk.ChunkGenerator;
import com.mojang.serialization.MapCodec;
import net.minecraft.world.entity.MobSpawnType;
import com.iafabric.util.BlockMaterial;
import com.iafabric.IafConfig;
import com.iafabric.block.IafBlockRegistry;
import com.iafabric.entity.EntityDragonBase;
import com.iafabric.entity.IafEntityRegistry;
import com.iafabric.entity.util.HomePosition;
import com.iafabric.event.WorldGenUtils;
import com.iafabric.world.IafWorldData;
import com.iafabric.world.IafWorldRegistry;
import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.Feature;

import net.minecraft.util.RandomSource;

public class WorldGenFireDragonRoosts extends IafFeature implements TypedFeature {
    private static final Direction[] HORIZONTALS = new Direction[]{Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST};
    private static boolean isMale;
    public static ResourceLocation DRAGON_CHEST = new ResourceLocation("iceandfire_fabric", "chest/fire_dragon_roost");

    public WorldGenFireDragonRoosts(Object configFactoryIn) {
    }

    @Override
    public boolean place(WorldGenLevel featureLevel, ChunkGenerator featureGenerator, RandomSource featureRandom, BlockPos featureOrigin) {
        FeatureContext context = new FeatureContext(featureLevel, featureGenerator, featureRandom, featureOrigin);
        WorldGenLevel worldIn = context.level();
        RandomSource rand = context.random();
        BlockPos origin = context.origin();
        if (rand.nextInt(IafConfig.generateDragonRoostChance) != 0 || !IafWorldRegistry.isFarEnoughFromSpawn(worldIn, origin) || !IafWorldRegistry.isFarEnoughFromDangerousGen(worldIn, origin, "dragon_roost")) {
            return false;
        }
        // Young dragons nest on high ground only: the highest point of the chunk, well above the sea.
        BlockPos high = DragonNests.highGround(worldIn, origin);
        if (high == null) {
            return false;
        }
        final BlockPos position = high;

        isMale = RandomSource.create().nextBoolean();
        int radius = 12 + rand.nextInt(8);
        worldIn.setBlock(position, Blocks.AIR.defaultBlockState(), 2);
        DragonNests.spawnNest(worldIn, IafEntityRegistry.FIRE_DRAGON, rand, position, isMale, DragonNests.nestAge(rand));
        {
            int j = radius;
            int k = 2;
            int l = radius;
            float f = (j + k + l) * 0.333F + 0.5F;
            BlockPos.betweenClosedStream(position.offset(-j, k, -l), position.offset(j, 0, l)).map(BlockPos::immutable).forEach(blockPos -> {
                int yAdd = blockPos.getY() - position.getY();
                if (blockPos.distSqr(position) <= f * f && yAdd < 2 + rand.nextInt(k) && !worldIn.isEmptyBlock(blockPos.below())) {
                    if (worldIn.isEmptyBlock(blockPos.above()))
                        worldIn.setBlock(blockPos, IafBlockRegistry.CHARRED_GRASS.defaultBlockState(), 2);
                    else
                        worldIn.setBlock(blockPos, IafBlockRegistry.CHARRED_DIRT.defaultBlockState(), 2);
                }
            });
        }
        {
            int j = radius;
            int k = (radius / 5);
            int l = radius;
            float f = (j + k + l) * 0.333F + 0.5F;
            BlockPos.betweenClosedStream(position.offset(-j, -k, -l), position.offset(j, 1, l)).map(BlockPos::immutable).forEach(blockPos -> {
                if (blockPos.distSqr(position) < f * f) {
                    worldIn.setBlock(blockPos, rand.nextBoolean() ? IafBlockRegistry.CHARRED_GRAVEL.defaultBlockState() : IafBlockRegistry.CHARRED_DIRT.defaultBlockState(), 2);
                } else if (blockPos.distSqr(position) == f * f) {
                    worldIn.setBlock(blockPos, IafBlockRegistry.CHARRED_COBBLESTONE.defaultBlockState(), 2);
                }
            });
        }
        radius -= 2;
        {
            int j = radius;
            int k = 2;
            int l = radius;
            float f = (j + k + l) * 0.333F + 0.5F;
            BlockPos up = position.above(k - 1);
            BlockPos.betweenClosedStream(up.offset(-j, -k + 2, -l), up.offset(j, k, l)).map(BlockPos::immutable).forEach(blockPos -> {
                if (blockPos.distSqr(position) <= f * f) {
                    worldIn.setBlock(blockPos, Blocks.AIR.defaultBlockState(), 2);
                }
            });
        }
        radius += 15;
        {
            int j = radius;
            int k = (radius / 5);
            int l = radius;
            float f = (j + k + l) * 0.333F + 0.5F;
            BlockPos.betweenClosedStream(position.offset(-j, -k, -l), position.offset(j, k, l)).map(BlockPos::immutable).forEach(blockPos -> {
                if (blockPos.distSqr(position) <= f * f) {
                    double dist = blockPos.distSqr(position) / (f * f);
                    if (!worldIn.isEmptyBlock(blockPos) && rand.nextDouble() > dist * 0.5D) {
                        transformState(worldIn, blockPos, worldIn.getBlockState(blockPos));
                    }
                    if (dist > 0.5D && rand.nextInt(1000) == 0) {
                        BlockPos height = worldIn.getHeightmapPos(Heightmap.Types.WORLD_SURFACE_WG, blockPos);
                        new WorldGenRoostBoulder(IafBlockRegistry.CHARRED_COBBLESTONE, rand.nextInt(3), true).generate(worldIn, rand, height);
                    }
                    if (rand.nextInt(1000) == 0) {
                        BlockPos height = worldIn.getHeightmapPos(Heightmap.Types.WORLD_SURFACE_WG, blockPos);
                        new WorldGenRoostPile(IafBlockRegistry.ASH).generate(worldIn, rand, height);
                    }
                    if (dist < 0.3D && rand.nextInt(isMale ? 200 : 300) == 0) {
                        BlockPos height = WorldGenUtils.degradeSurface(worldIn, worldIn.getHeightmapPos(Heightmap.Types.WORLD_SURFACE_WG, blockPos)).above(1);
                        new WorldGenRoostGoldPile(IafBlockRegistry.GOLD_PILE).generate(worldIn, rand, height);
                    }
                    if (dist < 0.3D && rand.nextInt(isMale ? 500 : 700) == 0) {
                        BlockPos height = WorldGenUtils.degradeSurface(worldIn, worldIn.getHeightmapPos(Heightmap.Types.WORLD_SURFACE_WG, blockPos)).above();
                        worldIn.setBlock(height, Blocks.CHEST.defaultBlockState().setValue(ChestBlock.FACING, HORIZONTALS[RandomSource.create().nextInt(3)]), 2);
                        if (worldIn.getBlockState(height).getBlock() instanceof ChestBlock) {
                            BlockEntity tileentity1 = worldIn.getBlockEntity(height);
                            if (tileentity1 instanceof ChestBlockEntity) {
                                ((ChestBlockEntity) tileentity1).setLootTable(DRAGON_CHEST, RandomSource.create().nextLong());
                            }
                        }
                    }
                    if (rand.nextInt(5000) == 0) {
                        BlockPos height = worldIn.getHeightmapPos(Heightmap.Types.WORLD_SURFACE_WG, blockPos);
                        new WorldGenRoostArch(IafBlockRegistry.CHARRED_COBBLESTONE).generate(worldIn, rand, height);
                    }
                }
            });
        }

        return true;
    }

    private void transformState(LevelAccessor world, BlockPos blockpos, BlockState state) {
        float hardness = state.getDestroySpeed(world, blockpos);
        if (hardness != -1.0F) {
            if (state.getBlock() instanceof BaseEntityBlock) {
                return;
            }
            if (BlockMaterial.GRASS.is(state)) {
                world.setBlock(blockpos, IafBlockRegistry.CHARRED_GRASS.defaultBlockState(), 2);
            } else if (BlockMaterial.DIRT.is(state) && state.getBlock() == Blocks.DIRT) {
                world.setBlock(blockpos, IafBlockRegistry.CHARRED_DIRT.defaultBlockState(), 2);
            } else if (BlockMaterial.DIRT.is(state) && state.getBlock() == Blocks.GRAVEL) {
                world.setBlock(blockpos, IafBlockRegistry.CHARRED_GRAVEL.defaultBlockState(), 2);
            } else if (BlockMaterial.STONE.is(state) && (state.getBlock() == Blocks.COBBLESTONE || state.getBlock().getDescriptionId().contains("cobblestone"))) {
                world.setBlock(blockpos, IafBlockRegistry.CHARRED_COBBLESTONE.defaultBlockState(), 2);
            } else if (BlockMaterial.STONE.is(state) && state.getBlock() != IafBlockRegistry.CHARRED_COBBLESTONE) {
                world.setBlock(blockpos, IafBlockRegistry.CHARRED_STONE.defaultBlockState(), 2);
            } else if (state.getBlock() == Blocks.DIRT_PATH) {
                world.setBlock(blockpos, IafBlockRegistry.CHARRED_DIRT_PATH.defaultBlockState(), 2);
            } else if (BlockMaterial.WOOD.is(state)) {
                world.setBlock(blockpos, IafBlockRegistry.ASH.defaultBlockState(), 2);
            } else if (BlockMaterial.LEAVES.is(state) || BlockMaterial.PLANT.is(state)) {
                world.setBlock(blockpos, Blocks.AIR.defaultBlockState(), 2);
            }
        }
    }

    @Override
    public IafWorldData.FeatureType getFeatureType() {
        return IafWorldData.FeatureType.SURFACE;
    }

}

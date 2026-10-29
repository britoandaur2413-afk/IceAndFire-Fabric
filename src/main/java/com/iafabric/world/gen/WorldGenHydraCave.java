package com.iafabric.world.gen;

import net.minecraft.resources.ResourceKey;
import net.minecraft.core.registries.Registries;
import com.mojang.serialization.MapCodec;
import com.iafabric.IafConfig;
import com.iafabric.entity.EntityHydra;
import com.iafabric.entity.IafEntityRegistry;
import com.iafabric.world.IafWorldData;
import com.iafabric.world.IafWorldRegistry;
import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
//import net.minecraft.data.worldgen.Features;
import net.minecraft.data.worldgen.features.TreeFeatures;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.SkullBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.Feature;

import net.minecraft.util.RandomSource;
import java.util.stream.Collectors;

public class WorldGenHydraCave extends IafFeature implements TypedFeature {

    public static final ResourceLocation HYDRA_CHEST = ResourceLocation.fromNamespaceAndPath("iceandfire_fabric", "chest/hydra_cave");
    private static final Direction[] HORIZONTALS = new Direction[]{Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST};

    public WorldGenHydraCave(Object configFactoryIn) {
    }

    @Override
    public boolean place(WorldGenLevel featureLevel, ChunkGenerator featureGenerator, RandomSource featureRandom, BlockPos featureOrigin) {
        FeatureContext context = new FeatureContext(featureLevel, featureGenerator, featureRandom, featureOrigin);
        WorldGenLevel worldIn = context.level();
        RandomSource rand = context.random();
        BlockPos position = context.origin();
        ChunkGenerator generator = context.chunkGenerator();

        if (rand.nextInt(IafConfig.generateHydraChance) != 0 || !IafWorldRegistry.isFarEnoughFromSpawn(worldIn, position) || !IafWorldRegistry.isFarEnoughFromDangerousGen(worldIn, position, "hydra_cave")) {
            return false;
        }

        int i1 = 8;
        int dist = 6;
        if (worldIn.isEmptyBlock(position.offset(i1 - dist, -3, -i1 + dist)) || worldIn.isEmptyBlock(position.offset(i1 - dist, -3, i1 - dist)) || worldIn.isEmptyBlock(position.offset(-i1 + dist, -3, -i1 + dist)) || worldIn.isEmptyBlock(position.offset(-i1 + dist, -3, i1 - dist))) {
            return false;
        }
        build(worldIn, generator, rand, position);
        return true;
    }

    /**
     * The cave itself at a place: a mound of grass with its doorways, its hollow, chests, skulls, leaves and mushrooms
     * on its floor, the hydra in it; noted for the exiled pixie (now made as a structure: HydraCavePiece).
     */
    public static void build(WorldGenLevel worldIn, ChunkGenerator generator, RandomSource rand, BlockPos position) {
        int i1 = 8;
        int i2 = i1 - 2;
        {
            int ySize = rand.nextInt(2);
            int j = i1 + rand.nextInt(2);
            int k = 5 + ySize;
            int l = i1 + rand.nextInt(2);
            float f = (j + k + l) * 0.333F + 0.5F;


            for (BlockPos blockpos : BlockPos.betweenClosedStream(position.offset(-j, -k, -l), position.offset(j, k, l)).map(BlockPos::immutable).collect(Collectors.toSet())) {
                boolean doorwayX = blockpos.getX() >= position.getX() - 2 + rand.nextInt(2) && blockpos.getX() <= position.getX() + 2 + rand.nextInt(2);
                boolean doorwayZ = blockpos.getZ() >= position.getZ() - 2 + rand.nextInt(2) && blockpos.getZ() <= position.getZ() + 2 + rand.nextInt(2);
                boolean isNotInDoorway = !doorwayX && !doorwayZ && blockpos.getY() > position.getY() || blockpos.getY() > position.getY() + k - (1 + rand.nextInt(2));
                if (blockpos.distSqr(position) <= f * f) {
                    if (!(worldIn.getBlockState(position).getBlock() instanceof ChestBlock) && isNotInDoorway) {
                        worldIn.setBlock(blockpos, Blocks.GRASS_BLOCK.defaultBlockState(), 3);
                        if (worldIn.getBlockState(position.below()).getBlock() == Blocks.GRASS_BLOCK) {
                            worldIn.setBlock(blockpos.below(), Blocks.DIRT.defaultBlockState(), 3);
                        }
                        if (rand.nextInt(4) == 0) {
                            worldIn.setBlock(blockpos.above(), Blocks.SHORT_GRASS.defaultBlockState(), 2);
                        }
                        if (rand.nextInt(9) == 0) {
                            worldIn.registryAccess().registryOrThrow(Registries.CONFIGURED_FEATURE).getHolder(TreeFeatures.SWAMP_OAK).ifPresent(tree -> tree.value().place(worldIn, generator, rand, blockpos.above()));
                        }

                    }
                    if (blockpos.getY() == position.getY()) {
                        worldIn.setBlock(blockpos, Blocks.GRASS_BLOCK.defaultBlockState(), 3);
                    }
                    if (blockpos.getY() <= position.getY() - 1 && !worldIn.getBlockState(blockpos).canOcclude()) {
                        worldIn.setBlock(blockpos, Blocks.STONE.defaultBlockState(), 3);

                    }
                }
            }


        }
        {
            int ySize = rand.nextInt(2);
            int j = i2 + rand.nextInt(2);
            int k = 4 + ySize;
            int l = i2 + rand.nextInt(2);
            float f = (j + k + l) * 0.333F + 0.5F;
            for (BlockPos blockpos : BlockPos.betweenClosedStream(position.offset(-j, -k, -l), position.offset(j, k, l)).map(BlockPos::immutable).collect(Collectors.toSet())) {
                if (blockpos.distSqr(position) <= f * f && blockpos.getY() > position.getY()) {
                    if (!(worldIn.getBlockState(position).getBlock() instanceof ChestBlock)) {
                        worldIn.setBlock(blockpos, Blocks.AIR.defaultBlockState(), 3);

                    }
                }
            }
            for (BlockPos blockpos : BlockPos.betweenClosedStream(position.offset(-j, -k, -l), position.offset(j, k + 8, l)).map(BlockPos::immutable).collect(Collectors.toSet())) {
                if (blockpos.distSqr(position) <= f * f && blockpos.getY() == position.getY()) {
                    if (rand.nextInt(30) == 0 && isTouchingAir(worldIn, blockpos.above())) {
                        worldIn.setBlock(blockpos.above(1), Blocks.CHEST.defaultBlockState().setValue(ChestBlock.FACING, HORIZONTALS[RandomSource.create().nextInt(3)]), 2);
                        if (worldIn.getBlockState(blockpos.above(1)).getBlock() instanceof ChestBlock) {
                            BlockEntity tileentity1 = worldIn.getBlockEntity(blockpos.above(1));
                            if (tileentity1 instanceof ChestBlockEntity) {
                                ((ChestBlockEntity) tileentity1).setLootTable(ResourceKey.create(Registries.LOOT_TABLE, HYDRA_CHEST), rand.nextLong());
                            }
                        }
                        continue;
                    }
                    if (rand.nextInt(45) == 0 && isTouchingAir(worldIn, blockpos.above())) {
                        worldIn.setBlock(blockpos.above(), Blocks.SKELETON_SKULL.defaultBlockState().setValue(SkullBlock.ROTATION, rand.nextInt(15)), 2);
                        BlockEntity tileentity1 = worldIn.getBlockEntity(blockpos.above(1));
                        continue;
                    }
                    if (rand.nextInt(35) == 0 && isTouchingAir(worldIn, blockpos.above())) {
                        worldIn.setBlock(blockpos.above(), Blocks.OAK_LEAVES.defaultBlockState().setValue(LeavesBlock.PERSISTENT, true), 2);
                        for (Direction facing : Direction.values()) {
                            if (rand.nextFloat() < 0.3F && facing != Direction.DOWN) {
                                worldIn.setBlock(blockpos.above().relative(facing), Blocks.OAK_LEAVES.defaultBlockState(), 2);
                            }
                        }
                        continue;
                    }
                    if (rand.nextInt(15) == 0 && isTouchingAir(worldIn, blockpos.above())) {
                        worldIn.setBlock(blockpos.above(), Blocks.TALL_GRASS.defaultBlockState(), 2);
                        continue;
                    }
                    if (rand.nextInt(15) == 0 && isTouchingAir(worldIn, blockpos.above())) {
                        worldIn.setBlock(blockpos.above(), rand.nextBoolean() ? Blocks.BROWN_MUSHROOM.defaultBlockState() : Blocks.RED_MUSHROOM.defaultBlockState(), 2);
                        continue;
                    }
                }
            }
        }
        EntityHydra hydra = new EntityHydra(IafEntityRegistry.HYDRA, worldIn.getLevel());
        hydra.setVariant(rand.nextInt(3));
        hydra.restrictTo(position, 15);
        hydra.absMoveTo(position.getX() + 0.5, position.getY() + 1.5, position.getZ() + 0.5, rand.nextFloat() * 360, 0);
        worldIn.addFreshEntity(hydra);
        com.iafabric.world.FairyLandmarks.note(worldIn, com.iafabric.world.FairyLandmarks.Kind.HYDRA, position.above());
    }

    private static boolean isTouchingAir(LevelAccessor worldIn, BlockPos pos) {
        boolean isTouchingAir = true;
        for (Direction direction : HORIZONTALS) {
            if (!worldIn.isEmptyBlock(pos.relative(direction))) {
                isTouchingAir = false;
            }
        }
        return isTouchingAir;
    }

    @Override
    public IafWorldData.FeatureType getFeatureType() {
        return IafWorldData.FeatureType.SURFACE;
    }

}

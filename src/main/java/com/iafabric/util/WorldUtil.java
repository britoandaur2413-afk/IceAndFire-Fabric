package com.iafabric.util;

import com.iafabric.world.IafWorldRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.chunk.ChunkStatus;

/** World checks: loaded chunks and where things may generate. */
public class WorldUtil {
    /** Whether the block's chunk is fully loaded. */
    public static boolean isBlockLoaded(final LevelAccessor world, final BlockPos pos) {
        return isChunkLoaded(world, pos.getX() >> 4, pos.getZ() >> 4);
    }

    /** Whether a chunk is fully loaded (never loads it). */
    public static boolean isChunkLoaded(final LevelAccessor world, final int x, final int z) {
        return world.getChunk(x, z, ChunkStatus.FULL, false) != null;
    }

    /** Whether entities tick at this block. */
    public static boolean isEntityBlockLoaded(final LevelAccessor world, final BlockPos pos) {
        if (world instanceof ServerLevel server) {
            return server.isPositionEntityTicking(pos);
        }
        return isChunkLoaded(world, pos.getX() >> 4, pos.getZ() >> 4);
    }

    public static boolean isChunkLoaded(final LevelAccessor world, final ChunkPos pos) {
        return isChunkLoaded(world, pos.x, pos.z);
    }

    public static boolean canGenerate(int configChance, final WorldGenLevel level, final RandomSource random, final BlockPos origin, final String id, boolean checkFluid) {
        boolean canGenerate = random.nextInt(configChance) == 0 && IafWorldRegistry.isFarEnoughFromSpawn(level, origin) && IafWorldRegistry.isFarEnoughFromDangerousGen(level, origin, id);
        if (canGenerate && checkFluid) {
            if (!level.getFluidState(origin.below()).isEmpty()) {
                return false;
            }
        }
        return canGenerate;
    }
}

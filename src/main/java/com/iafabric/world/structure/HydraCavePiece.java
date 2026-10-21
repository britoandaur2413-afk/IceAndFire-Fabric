package com.iafabric.world.structure;

import com.iafabric.world.gen.WorldGenHydraCave;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;

/**
 * A hydra's cave: the mound of grass and its hollow, its chests and bones, the hydra in it (WorldGenHydraCave.build),
 * made whole as the chunk of its middle is made (as the cave was when it was a feature of the swamp).
 */
public class HydraCavePiece extends StructurePiece {
	private final BlockPos center;
	private final long seed;

	public HydraCavePiece(BlockPos center, long seed) {
		super(IafStructures.HYDRA_CAVE_PIECE, 0, new BoundingBox(center.getX() - 12, center.getY() - 8, center.getZ() - 12,
			center.getX() + 12, center.getY() + 14, center.getZ() + 12));
		this.center = center;
		this.seed = seed;
	}

	public HydraCavePiece(CompoundTag tag) {
		super(IafStructures.HYDRA_CAVE_PIECE, tag);
		this.center = BlockPos.of(tag.getLongOr("Center", 0L));
		this.seed = tag.getLongOr("Seed", 0L);
	}

	@Override
	protected void addAdditionalSaveData(StructurePieceSerializationContext context, CompoundTag tag) {
		tag.putLong("Center", this.center.asLong());
		tag.putLong("Seed", this.seed);
	}

	/** Its middle (on the floor of its hollow). */
	public BlockPos center() {
		return this.center;
	}

	@Override
	public void postProcess(WorldGenLevel level, StructureManager structures, ChunkGenerator generator, RandomSource random, BoundingBox chunkBox,
		ChunkPos chunkPos, BlockPos pivot) {
		if (chunkBox.isInside(this.center)) {
			WorldGenHydraCave.build(level, generator, RandomSource.create(this.seed), this.center);
		}
	}
}

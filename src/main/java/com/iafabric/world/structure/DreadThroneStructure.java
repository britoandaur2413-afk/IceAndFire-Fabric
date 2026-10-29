package com.iafabric.world.structure;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureType;

import java.util.Optional;

/**
 * Cryona's citadel (CitadelPiece): on wide dry land in the snow, its middle on its chunk's, at the height most of its
 * ground has (its footing fills the hollows). Very rare: the scribes' maps and /iceandfire find lead to it.
 */
public class DreadThroneStructure extends Structure {
	public static final MapCodec<DreadThroneStructure> CODEC = simpleCodec(DreadThroneStructure::new);

	public DreadThroneStructure(StructureSettings settings) {
		super(settings);
	}

	@Override
	protected Optional<GenerationStub> findGenerationPoint(GenerationContext context) {
		var generator = context.chunkGenerator();
		int facing = context.random().nextInt(4);
		int cx = context.chunkPos().getMiddleBlockX();
		int cz = context.chunkPos().getMiddleBlockZ();
		int[] heights = new int[17];
		heights[0] = generator.getFirstFreeHeight(cx, cz, Heightmap.Types.WORLD_SURFACE_WG, context.heightAccessor(), context.randomState());
		for (int i = 0; i < 16; i++) {
			double angle = Math.PI * 2 * (i % 8) / 8;
			int r = i < 8 ? 40 : 70;
			int x = cx + (int) (Math.sin(angle) * r);
			int z = cz + (int) (Math.cos(angle) * r);
			int h = generator.getFirstFreeHeight(x, z, Heightmap.Types.OCEAN_FLOOR_WG, context.heightAccessor(), context.randomState());
			int w = generator.getFirstFreeHeight(x, z, Heightmap.Types.WORLD_SURFACE_WG, context.heightAccessor(), context.randomState());
			if (i < 8 && (h <= generator.getSeaLevel() || w != h)) {
				// water under its walls (a pond out by its grounds' edge is filled by its footing)
				return Optional.empty();
			}
			heights[i + 1] = Math.max(h, generator.getSeaLevel());
		}
		int[] sorted = heights.clone();
		java.util.Arrays.sort(sorted);
		int y = sorted[sorted.length / 2];
		if (y <= generator.getSeaLevel() + 1) {
			return Optional.empty();
		}
		for (int i = 0; i < heights.length; i++) {
			// the land under its walls fairly even; out by the edge of its grounds a little less so
			if (Math.abs(heights[i] - y) > (i <= 8 ? 16 : 22)) {
				return Optional.empty();
			}
		}
		int[] gate = BuiltPiece.toWorld(cx, cz, (facing + 2) & 3, 0, CitadelPiece.MIDDLE);
		long seed = context.random().nextLong();
		return Optional.of(new GenerationStub(new BlockPos(cx, y, cz), builder -> builder.addPiece(new CitadelPiece(gate[0], y, gate[1], facing, seed))));
	}

	@Override
	public StructureType<?> type() {
		return IafStructures.DREAD_THRONE;
	}
}

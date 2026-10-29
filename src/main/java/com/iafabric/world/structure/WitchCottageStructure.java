package com.iafabric.world.structure;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureType;

import java.util.Optional;

/**
 * A witches' cottage (see WitchCottagePiece): on fairly flat dry land, the hut's middle at the corner of its chunk,
 * where the game's own search (and the exiled pixie's) says it is.
 */
public class WitchCottageStructure extends Structure {
	public static final MapCodec<WitchCottageStructure> CODEC = simpleCodec(WitchCottageStructure::new);

	public WitchCottageStructure(StructureSettings settings) {
		super(settings);
	}

	@Override
	protected Optional<GenerationStub> findGenerationPoint(GenerationContext context) {
		int facing = context.random().nextInt(4);
		int cx = context.chunkPos().getMinBlockX();
		int cz = context.chunkPos().getMinBlockZ();
		var generator = context.chunkGenerator();
		int y = generator.getFirstFreeHeight(cx, cz, Heightmap.Types.WORLD_SURFACE_WG, context.heightAccessor(), context.randomState());
		if (y <= generator.getSeaLevel() + 1) {
			return Optional.empty();
		}
		// fairly flat dry land under the hut and its clearing
		for (int i = 0; i < 8; i++) {
			double angle = Math.PI * i / 4;
			int x = cx + (int) (Math.sin(angle) * 8);
			int z = cz + (int) (Math.cos(angle) * 8);
			int h = generator.getFirstFreeHeight(x, z, Heightmap.Types.OCEAN_FLOOR_WG, context.heightAccessor(), context.randomState());
			int w = generator.getFirstFreeHeight(x, z, Heightmap.Types.WORLD_SURFACE_WG, context.heightAccessor(), context.randomState());
			if (Math.abs(h - y) > 3 || w != h) {
				return Optional.empty();
			}
		}
		int[] gate = BuiltPiece.toWorld(cx, cz, (facing + 2) & 3, 0, WitchCottagePiece.MIDDLE);
		long seed = context.random().nextLong();
		return Optional.of(new GenerationStub(new BlockPos(cx, y, cz), builder -> builder.addPiece(new WitchCottagePiece(gate[0], y, gate[1], facing, seed))));
	}

	@Override
	public StructureType<?> type() {
		return IafStructures.WITCH_COTTAGE;
	}
}

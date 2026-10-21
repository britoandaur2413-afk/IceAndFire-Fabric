package com.iafabric.world.structure;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureType;

import java.util.Optional;

/** A dread knight's spire (see DreadPieces.Spire): on fairly flat dry land in the cold, its tower on its chunk's middle. */
public class DreadSpireStructure extends Structure {
	public static final MapCodec<DreadSpireStructure> CODEC = simpleCodec(DreadSpireStructure::new);

	public DreadSpireStructure(StructureSettings settings) {
		super(settings);
	}

	@Override
	protected Optional<GenerationStub> findGenerationPoint(GenerationContext context) {
		var generator = context.chunkGenerator();
		int facing = context.random().nextInt(4);
		int cx = context.chunkPos().getMiddleBlockX();
		int cz = context.chunkPos().getMiddleBlockZ();
		int y = generator.getFirstFreeHeight(cx, cz, Heightmap.Types.WORLD_SURFACE_WG, context.heightAccessor(), context.randomState());
		if (y <= generator.getSeaLevel() + 1) {
			return Optional.empty();
		}
		for (int i = 0; i < 6; i++) {
			double angle = Math.PI * i / 3;
			int x = cx + (int) (Math.sin(angle) * 16);
			int z = cz + (int) (Math.cos(angle) * 16);
			int h = generator.getFirstFreeHeight(x, z, Heightmap.Types.OCEAN_FLOOR_WG, context.heightAccessor(), context.randomState());
			int w = generator.getFirstFreeHeight(x, z, Heightmap.Types.WORLD_SURFACE_WG, context.heightAccessor(), context.randomState());
			if (Math.abs(h - y) > 6 || h <= generator.getSeaLevel() || w != h) {
				return Optional.empty();
			}
		}
		int[] gate = BuiltPiece.toWorld(cx, cz, (facing + 2) & 3, 0, DreadPieces.Spire.MIDDLE);
		long seed = context.random().nextLong();
		return Optional.of(new GenerationStub(new BlockPos(cx, y, cz), builder -> builder.addPiece(new DreadPieces.Spire(gate[0], y, gate[1], facing, seed))));
	}

	@Override
	public StructureType<?> type() {
		return IafStructures.DREAD_SPIRE;
	}
}

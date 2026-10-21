package com.iafabric.world.structure;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureType;

import java.util.Optional;

/**
 * A pixie cavern (see PixieCavernPiece): a village of pixies about thirty blocks under the land, centred on its
 * chunk, with a tunnel that comes out on dry land. Where the land above is too thin, or the tunnel would come out in
 * water, there is none.
 */
public class PixieCavernStructure extends Structure {
	public static final MapCodec<PixieCavernStructure> CODEC = simpleCodec(PixieCavernStructure::new);
	private static final int DEPTH = 32;
	private static final int LONGEST = 140;

	public PixieCavernStructure(StructureSettings settings) {
		super(settings);
	}

	@Override
	protected Optional<GenerationStub> findGenerationPoint(GenerationContext context) {
		int cx = context.chunkPos().getMiddleBlockX();
		int cz = context.chunkPos().getMiddleBlockZ();
		var generator = context.chunkGenerator();
		int sea = generator.getSeaLevel();
		int surface = generator.getFirstOccupiedHeight(cx, cz, Heightmap.Types.OCEAN_FLOOR_WG, context.heightAccessor(), context.randomState());
		if (surface <= sea + 1) {
			return Optional.empty();
		}
		int facing = context.random().nextInt(4);
		// the land around must cover the cavern's roof
		int lowest = surface;
		for (int i = 0; i < 8; i++) {
			double angle = Math.PI * i / 4;
			int[] xz = BuiltPiece.toWorld(cx, cz, facing, (int) (Math.sin(angle) * 20), (int) (Math.cos(angle) * 20));
			lowest = Math.min(lowest, generator.getFirstOccupiedHeight(xz[0], xz[1], Heightmap.Types.OCEAN_FLOOR_WG, context.heightAccessor(), context.randomState()));
		}
		int floor = Math.min(surface - DEPTH, lowest - PixieCavernPiece.H - 6);
		if (floor - 8 < context.heightAccessor().getMinY() + 8 || surface - floor > DEPTH + 14) {
			return Optional.empty();
		}
		// the tunnel climbs until it comes out on the land: there it must be dry
		int mouth = -1;
		int end = -1;
		int top = floor;
		for (int v = PixieCavernPiece.TUNNEL_START; v <= LONGEST; v++) {
			int[] xz = BuiltPiece.toWorld(cx, cz, facing, (int) Math.round(PixieCavernPiece.tunnelMiddle(v)), v);
			int land = generator.getFirstOccupiedHeight(xz[0], xz[1], Heightmap.Types.OCEAN_FLOOR_WG, context.heightAccessor(), context.randomState());
			int tunnelFloor = PixieCavernPiece.tunnelFloor(floor, v);
			top = Math.max(top, land + 8);
			if (mouth < 0 && tunnelFloor + 7 >= land) {
				mouth = v;
			}
			if (tunnelFloor >= land) {
				int water = generator.getFirstOccupiedHeight(xz[0], xz[1], Heightmap.Types.WORLD_SURFACE_WG, context.heightAccessor(), context.randomState());
				if (water != land || land <= sea) {
					return Optional.empty();
				}
				end = v;
				break;
			}
		}
		if (end < 0) {
			return Optional.empty();
		}
		int kind = context.random().nextInt(3);
		long seed = context.random().nextLong();
		int m = mouth;
		int e = end;
		int t = top;
		return Optional.of(new GenerationStub(new BlockPos(cx, floor, cz),
			builder -> builder.addPiece(new PixieCavernPiece(cx, floor, cz, facing, kind, seed, m, e, t))));
	}

	@Override
	public StructureType<?> type() {
		return IafStructures.PIXIE_CAVERN;
	}
}

package com.iafabric.world.structure;

import com.iafabric.IafConfig;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.QuartPos;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureType;

import java.util.Optional;

/**
 * A hydra's cave in the swamp (see HydraCavePiece): on its ground or over its shallow water (as the cave always was:
 * the water under its mound turns to stone, an island), fairly flat, away from the world's spawn, its middle at the
 * corner of its chunk, where the game's own search (and the exiled pixie's, for her hydra quest) says it is.
 */
public class HydraCaveStructure extends Structure {
	public static final MapCodec<HydraCaveStructure> CODEC = simpleCodec(HydraCaveStructure::new);

	public HydraCaveStructure(StructureSettings settings) {
		super(settings);
	}

	@Override
	protected Optional<GenerationStub> findGenerationPoint(GenerationContext context) {
		if (!IafConfig.generateHydraCaves) {
			return Optional.empty();
		}
		int cx = context.chunkPos().getMinBlockX();
		int cz = context.chunkPos().getMinBlockZ();
		// (a hydra is no neighbour for a new player)
		double limit = IafConfig.dangerousWorldGenDistanceLimit;
		if ((double) cx * cx + (double) cz * cz < limit * limit) {
			return Optional.empty();
		}
		var generator = context.chunkGenerator();
		int top = generator.getFirstFreeHeight(cx, cz, Heightmap.Types.WORLD_SURFACE_WG, context.heightAccessor(), context.randomState());
		// (a swamp's, else nothing more to ask: most places asked are not)
		if (!context.validBiome().test(context.biomeResolver().getNoiseBiome(QuartPos.fromBlock(cx), QuartPos.fromBlock(top), QuartPos.fromBlock(cz)))) {
			return Optional.empty();
		}
		int floor = generator.getFirstFreeHeight(cx, cz, Heightmap.Types.OCEAN_FLOOR_WG, context.heightAccessor(), context.randomState());
		if (top - floor > 3) {
			return Optional.empty();
		}
		// fairly flat all round its mound, its water shallow
		for (int i = 0; i < 8; i++) {
			double angle = Math.PI * i / 4;
			int x = cx + (int) Math.round(Math.sin(angle) * 7);
			int z = cz + (int) Math.round(Math.cos(angle) * 7);
			int h = generator.getFirstFreeHeight(x, z, Heightmap.Types.OCEAN_FLOOR_WG, context.heightAccessor(), context.randomState());
			int w = generator.getFirstFreeHeight(x, z, Heightmap.Types.WORLD_SURFACE_WG, context.heightAccessor(), context.randomState());
			if (Math.abs(w - top) > 3 || w - h > 4) {
				return Optional.empty();
			}
		}
		BlockPos center = new BlockPos(cx, top, cz);
		long seed = context.random().nextLong();
		return Optional.of(new GenerationStub(center, builder -> builder.addPiece(new HydraCavePiece(center, seed))));
	}

	@Override
	public StructureType<?> type() {
		return IafStructures.HYDRA_CAVE;
	}
}

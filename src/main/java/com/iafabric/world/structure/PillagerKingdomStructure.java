package com.iafabric.world.structure;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.QuartPos;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureType;

import java.util.Optional;

/** A pillager kingdom (see PillagerKingdomPiece): on fairly flat dry land, centred on its chunk. */
public class PillagerKingdomStructure extends Structure {
	public static final MapCodec<PillagerKingdomStructure> CODEC = simpleCodec(PillagerKingdomStructure::new);

	public PillagerKingdomStructure(StructureSettings settings) {
		super(settings);
	}

	@Override
	protected Optional<GenerationStub> findGenerationPoint(GenerationContext context) {
		int facing = context.random().nextInt(4);
		int cx = context.chunkPos().getMiddleBlockX();
		int cz = context.chunkPos().getMiddleBlockZ();
		int[] gate = BuiltPiece.toWorld(cx, cz, (facing + 2) & 3, 0, PillagerKingdomPiece.LENGTH / 2);
		int gateY = context.chunkGenerator().getFirstFreeHeight(gate[0], gate[1], Heightmap.Types.WORLD_SURFACE_WG, context.heightAccessor(), context.randomState());
		if (gateY <= context.chunkGenerator().getSeaLevel() + 1) {
			return Optional.empty();
		}
		// fairly flat: the corners within six blocks of the gate's height, none of them under water
		for (int[] corner : new int[][]{{-32, 4}, {32, 4}, {-32, 66}, {32, 66}, {0, 35}, {-32, 35}, {32, 35}}) {
			int[] xz = BuiltPiece.toWorld(gate[0], gate[1], facing, corner[0], corner[1]);
			int y = context.chunkGenerator().getFirstFreeHeight(xz[0], xz[1], Heightmap.Types.OCEAN_FLOOR_WG, context.heightAccessor(), context.randomState());
			if (Math.abs(y - gateY) > 6 || y <= context.chunkGenerator().getSeaLevel()) {
				return Optional.empty();
			}
		}
		float temperature = context.biomeSource().getNoiseBiome(QuartPos.fromBlock(gate[0]), QuartPos.fromBlock(gateY), QuartPos.fromBlock(gate[1]), context.randomState().sampler())
			.value().getBaseTemperature();
		int kind = temperature < 0.3F ? 1 : temperature >= 0.95F ? 0 : 2;
		// two or three young dragons; one kingdom in four has an elite too
		int dragons = 2 + context.random().nextInt(2) + (context.random().nextInt(4) == 0 ? 8 : 0);
		long seed = context.random().nextLong();
		return Optional.of(new GenerationStub(new BlockPos(gate[0], gateY, gate[1]),
			builder -> builder.addPiece(new PillagerKingdomPiece(gate[0], gateY, gate[1], facing, kind, dragons, seed))));
	}

	@Override
	public StructureType<?> type() {
		return IafStructures.PILLAGER_KINGDOM;
	}
}

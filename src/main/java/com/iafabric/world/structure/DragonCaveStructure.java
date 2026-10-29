package com.iafabric.world.structure;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.QuartPos;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureType;

import java.util.Optional;

/**
 * A stage 4 dragons' den (see DragonLairPieces.Cave): its floor twenty-five to thirty-five blocks under dry land, centred on its chunk,
 * with a tunnel that comes out on dry land; the dragons of the land's climate.
 */
public class DragonCaveStructure extends Structure {
	public static final Codec<DragonCaveStructure> CODEC = simpleCodec(DragonCaveStructure::new);
	private static final int LONGEST = 110;

	public DragonCaveStructure(StructureSettings settings) {
		super(settings);
	}

	@Override
	protected Optional<GenerationStub> findGenerationPoint(GenerationContext context) {
		var generator = context.chunkGenerator();
		int sea = generator.getSeaLevel();
		int cx = context.chunkPos().getMiddleBlockX();
		int cz = context.chunkPos().getMiddleBlockZ();
		int surface = generator.getFirstOccupiedHeight(cx, cz, Heightmap.Types.OCEAN_FLOOR_WG, context.heightAccessor(), context.randomState());
		if (surface <= sea + 1) {
			return Optional.empty();
		}
		int facing = context.random().nextInt(4);
		int lowest = surface;
		for (int i = 0; i < 8; i++) {
			double angle = Math.PI * i / 4;
			int[] xz = BuiltPiece.toWorld(cx, cz, facing, (int) (Math.sin(angle) * DragonLairPieces.Cave.RX), (int) (Math.cos(angle) * DragonLairPieces.Cave.RX));
			lowest = Math.min(lowest, generator.getFirstOccupiedHeight(xz[0], xz[1], Heightmap.Types.OCEAN_FLOOR_WG, context.heightAccessor(), context.randomState()));
		}
		int floor = lowest - DragonLairPieces.Cave.RY - 8 - context.random().nextInt(10);
		if (floor - 8 < context.heightAccessor().getMinBuildHeight() + 10) {
			return Optional.empty();
		}
		int end = -1;
		int top = floor;
		for (int v = DragonLairPieces.Cave.TUNNEL_START; v <= LONGEST; v++) {
			int[] xz = BuiltPiece.toWorld(cx, cz, facing, (int) Math.round(DragonLairPieces.Cave.tunnelMiddle(v)), v);
			int land = generator.getFirstOccupiedHeight(xz[0], xz[1], Heightmap.Types.OCEAN_FLOOR_WG, context.heightAccessor(), context.randomState());
			top = Math.max(top, land + 8);
			if (DragonLairPieces.Cave.tunnelFloor(floor - 1, v) >= land) {
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
		float temperature = context.biomeSource().getNoiseBiome(QuartPos.fromBlock(cx), QuartPos.fromBlock(surface), QuartPos.fromBlock(cz), context.randomState().sampler())
			.value().getBaseTemperature();
		int kind = temperature < 0.3F ? 1 : temperature >= 0.95F ? 0 : 2;
		long seed = context.random().nextLong();
		int e = end;
		int t = top;
		return Optional.of(new GenerationStub(new BlockPos(cx, floor, cz),
			builder -> builder.addPiece(new DragonLairPieces.Cave(cx, floor, cz, facing, kind, seed, e, t))));
	}

	@Override
	public StructureType<?> type() {
		return IafStructures.DRAGON_CAVE;
	}
}

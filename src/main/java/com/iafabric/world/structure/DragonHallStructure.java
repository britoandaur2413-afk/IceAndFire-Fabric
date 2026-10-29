package com.iafabric.world.structure;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.QuartPos;
import net.minecraft.tags.BiomeTags;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureType;

import java.util.Optional;

/**
 * A stage 5 dragon's hall (see DragonHallPieces): an abandoned castle on fairly flat dry land, the forge hall twenty
 * blocks under it, and the arena deeper, under rock all the way. Rare (see its structure set). The building is
 * centred on the chunk that starts it, so all of it is within reach of the chunks that build it.
 */
public class DragonHallStructure extends Structure {
	public static final Codec<DragonHallStructure> CODEC = simpleCodec(DragonHallStructure::new);

	public DragonHallStructure(StructureSettings settings) {
		super(settings);
	}

	@Override
	protected Optional<GenerationStub> findGenerationPoint(GenerationContext context) {
		var generator = context.chunkGenerator();
		int sea = generator.getSeaLevel();
		int facing = context.random().nextInt(4);
		int cx = context.chunkPos().getMiddleBlockX();
		int cz = context.chunkPos().getMiddleBlockZ();
		int[] gate = BuiltPiece.toWorld(cx, cz, (facing + 2) & 3, 0, DragonHallPieces.MIDDLE);
		int gateY = generator.getFirstFreeHeight(gate[0], gate[1], Heightmap.Types.WORLD_SURFACE_WG, context.heightAccessor(), context.randomState());
		if (gateY <= sea + 2) {
			return Optional.empty();
		}
		// the castle on fairly flat dry land
		for (int[] p : new int[][]{{-26, 4}, {26, 4}, {-26, 56}, {26, 56}, {0, 30}}) {
			int[] xz = BuiltPiece.toWorld(gate[0], gate[1], facing, p[0], p[1]);
			int h = generator.getFirstFreeHeight(xz[0], xz[1], Heightmap.Types.OCEAN_FLOOR_WG, context.heightAccessor(), context.randomState());
			int w = generator.getFirstFreeHeight(xz[0], xz[1], Heightmap.Types.WORLD_SURFACE_WG, context.heightAccessor(), context.randomState());
			if (Math.abs(h - gateY) > 8 || h <= sea || w != h) {
				return Optional.empty();
			}
		}
		// rock over the forge hall and over the arena (the sea floor counts)
		int forgeCover = Integer.MAX_VALUE;
		for (int v = DragonHallPieces.FORGE_V0; v <= DragonHallPieces.FORGE_V1; v += 6) {
			for (int u = -12; u <= 12; u += 12) {
				int[] xz = BuiltPiece.toWorld(gate[0], gate[1], facing, u, v);
				forgeCover = Math.min(forgeCover, generator.getFirstOccupiedHeight(xz[0], xz[1], Heightmap.Types.OCEAN_FLOOR_WG, context.heightAccessor(), context.randomState()));
			}
		}
		if (forgeCover < gateY - DragonHallPieces.FORGE_DROP + 17) {
			return Optional.empty();
		}
		int lowest = Integer.MAX_VALUE;
		for (int v = DragonHallPieces.FORGE_V1; v <= DragonHallPieces.ARENA_V1 + 4; v += 8) {
			for (int u = -DragonHallPieces.ARENA_HALF; u <= DragonHallPieces.ARENA_HALF; u += 23) {
				int[] xz = BuiltPiece.toWorld(gate[0], gate[1], facing, u, v);
				lowest = Math.min(lowest, generator.getFirstOccupiedHeight(xz[0], xz[1], Heightmap.Types.OCEAN_FLOOR_WG, context.heightAccessor(), context.randomState()));
			}
		}
		// the vault under four blocks of rock at least
		int arenaFloor = Math.min(gateY - DragonHallPieces.MIN_DEPTH, lowest - DragonHallPieces.VAULT_TOP - 6);
		int depth = gateY - arenaFloor;
		// the bridge goes down no more than a block for each block ahead
		if (depth > DragonHallPieces.MAX_DEPTH || arenaFloor - 16 < context.heightAccessor().getMinBuildHeight() + 6) {
			return Optional.empty();
		}
		// the dragon of the land's climate (any in a mild land), and the castle in the land's stone
		var biome = context.biomeSource().getNoiseBiome(QuartPos.fromBlock(gate[0]), QuartPos.fromBlock(gateY), QuartPos.fromBlock(gate[1]), context.randomState().sampler());
		float temperature = biome.value().getBaseTemperature();
		int dragon = temperature < 0.3F ? 1 : temperature >= 0.95F ? 0 : context.random().nextInt(3);
		boolean hot = temperature >= 0.95F || biome.is(BiomeTags.IS_BADLANDS) || biome.is(BiomeTags.IS_SAVANNA);
		int style = temperature < 0.3F ? 1 : hot ? 2 : 0;
		int kind = dragon + 3 * style;
		long seed = context.random().nextLong();
		BlockPos at = new BlockPos(gate[0], gateY, gate[1]);
		return Optional.of(new GenerationStub(at, builder -> {
			builder.addPiece(new DragonHallPieces.Castle(gate[0], gateY, gate[1], facing, kind, depth, seed));
			builder.addPiece(new DragonHallPieces.Stair(gate[0], gateY, gate[1], facing, kind, depth, seed));
			builder.addPiece(new DragonHallPieces.Forge(gate[0], gateY, gate[1], facing, kind, depth, seed));
			builder.addPiece(new DragonHallPieces.Descent(gate[0], gateY, gate[1], facing, kind, depth, seed));
			builder.addPiece(new DragonHallPieces.Arena(gate[0], gateY, gate[1], facing, kind, depth, seed));
		}));
	}

	@Override
	public StructureType<?> type() {
		return IafStructures.DRAGON_HALL;
	}
}

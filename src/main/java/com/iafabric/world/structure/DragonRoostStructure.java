package com.iafabric.world.structure;

import com.iafabric.world.gen.DragonNests;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.QuartPos;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureType;

import java.util.Optional;

/**
 * A young dragon's roost (see DragonLairPieces.Roost): on the highest dry ground of its chunk, when that is high
 * enough (hills and mountains); the dragon of the land's climate.
 */
public class DragonRoostStructure extends Structure {
	public static final MapCodec<DragonRoostStructure> CODEC = simpleCodec(DragonRoostStructure::new);

	public DragonRoostStructure(StructureSettings settings) {
		super(settings);
	}

	@Override
	protected Optional<GenerationStub> findGenerationPoint(GenerationContext context) {
		var generator = context.chunkGenerator();
		int baseX = context.chunkPos().getMinBlockX();
		int baseZ = context.chunkPos().getMinBlockZ();
		int bestX = 0;
		int bestZ = 0;
		int bestY = Integer.MIN_VALUE;
		for (int x = 2; x < 16; x += 4) {
			for (int z = 2; z < 16; z += 4) {
				int y = generator.getFirstFreeHeight(baseX + x, baseZ + z, Heightmap.Types.WORLD_SURFACE_WG, context.heightAccessor(), context.randomState());
				if (y > bestY) {
					bestY = y;
					bestX = baseX + x;
					bestZ = baseZ + z;
				}
			}
		}
		int dry = generator.getFirstFreeHeight(bestX, bestZ, Heightmap.Types.OCEAN_FLOOR_WG, context.heightAccessor(), context.randomState());
		if (bestY < generator.getSeaLevel() + DragonNests.NEST_HEIGHT_ABOVE_SEA || dry != bestY) {
			return Optional.empty();
		}
		float temperature = context.biomeResolver().getNoiseBiome(QuartPos.fromBlock(bestX), QuartPos.fromBlock(bestY), QuartPos.fromBlock(bestZ))
			.value().getBaseTemperature();
		int kind = temperature < 0.3F ? 1 : temperature >= 0.95F ? 0 : 2;
		long seed = context.random().nextLong();
		int x = bestX;
		int y = bestY;
		int z = bestZ;
		return Optional.of(new GenerationStub(new BlockPos(x, y, z), builder -> builder.addPiece(new DragonLairPieces.Roost(x, y, z, kind, seed))));
	}

	@Override
	public StructureType<?> type() {
		return IafStructures.DRAGON_ROOST;
	}
}

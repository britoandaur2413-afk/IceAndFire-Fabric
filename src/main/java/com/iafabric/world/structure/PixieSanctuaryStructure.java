package com.iafabric.world.structure;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.QuartPos;
import net.minecraft.tags.BiomeTags;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureType;

import java.util.Optional;

/** A pixie sanctuary (see PixieSanctuaryPiece): on fairly flat dry land in the woods, its tree in the middle of its chunk. */
public class PixieSanctuaryStructure extends Structure {
	public static final Codec<PixieSanctuaryStructure> CODEC = simpleCodec(PixieSanctuaryStructure::new);

	public PixieSanctuaryStructure(StructureSettings settings) {
		super(settings);
	}

	@Override
	protected Optional<GenerationStub> findGenerationPoint(GenerationContext context) {
		int facing = context.random().nextInt(4);
		int cx = context.chunkPos().getMiddleBlockX();
		int cz = context.chunkPos().getMiddleBlockZ();
		var generator = context.chunkGenerator();
		int y = generator.getFirstFreeHeight(cx, cz, Heightmap.Types.WORLD_SURFACE_WG, context.heightAccessor(), context.randomState());
		if (y <= generator.getSeaLevel() + 1) {
			return Optional.empty();
		}
		// fairly flat dry land all around the tree
		for (int i = 0; i < 8; i++) {
			double angle = Math.PI * i / 4;
			int x = cx + (int) (Math.sin(angle) * 22);
			int z = cz + (int) (Math.cos(angle) * 22);
			int h = generator.getFirstFreeHeight(x, z, Heightmap.Types.OCEAN_FLOOR_WG, context.heightAccessor(), context.randomState());
			int w = generator.getFirstFreeHeight(x, z, Heightmap.Types.WORLD_SURFACE_WG, context.heightAccessor(), context.randomState());
			if (Math.abs(h - y) > 6 || h <= generator.getSeaLevel() || w != h) {
				return Optional.empty();
			}
		}
		int[] gate = BuiltPiece.toWorld(cx, cz, (facing + 2) & 3, 0, PixieSanctuaryPiece.CENTER_V);
		// the tree of the land: cherry in a cherry grove, birch among birches, an old oak in dark woods and taigas
		var biome = context.biomeSource().getNoiseBiome(QuartPos.fromBlock(cx), QuartPos.fromBlock(y), QuartPos.fromBlock(cz), context.randomState().sampler());
		int kind;
		if (biome.is(Biomes.CHERRY_GROVE)) {
			kind = 0;
		} else if (biome.is(Biomes.BIRCH_FOREST) || biome.is(Biomes.OLD_GROWTH_BIRCH_FOREST)) {
			kind = 2;
		} else if (biome.is(Biomes.DARK_FOREST) || biome.is(BiomeTags.IS_TAIGA)) {
			kind = 1;
		} else {
			float n = context.random().nextFloat();
			kind = n < 0.5F ? 0 : n < 0.8F ? 1 : 2;
		}
		long seed = context.random().nextLong();
		int k = kind;
		return Optional.of(new GenerationStub(new BlockPos(cx, y, cz), builder -> builder.addPiece(new PixieSanctuaryPiece(gate[0], y, gate[1], facing, k, seed))));
	}

	@Override
	public StructureType<?> type() {
		return IafStructures.PIXIE_SANCTUARY;
	}
}

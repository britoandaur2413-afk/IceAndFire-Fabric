package com.iafabric.world.structure;

import com.iafabric.IafMod;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceType;

/** The mod's buildings made in code (their places and rarity are data: worldgen/structure and structure_set). */
public final class IafStructures {
	public static final StructureType<DragonHallStructure> DRAGON_HALL = type("dragon_hall", DragonHallStructure.CODEC);
	public static final StructurePieceType KEEP_CASTLE = piece("dragon_keep_castle", (context, tag) -> new DragonHallPieces.Castle(tag));
	public static final StructurePieceType KEEP_STAIR = piece("dragon_keep_stair", (context, tag) -> new DragonHallPieces.Stair(tag));
	public static final StructurePieceType KEEP_FORGE = piece("dragon_keep_forge", (context, tag) -> new DragonHallPieces.Forge(tag));
	public static final StructurePieceType KEEP_DESCENT = piece("dragon_keep_descent", (context, tag) -> new DragonHallPieces.Descent(tag));
	public static final StructurePieceType KEEP_ARENA = piece("dragon_keep_arena", (context, tag) -> new DragonHallPieces.Arena(tag));
	/** The parts of the first dragon halls, kept so worlds made with them still load (they make nothing now). */
	public static final StructurePieceType HALL_CASTLE = legacy("dragon_hall_castle");
	public static final StructurePieceType HALL_RAMP = legacy("dragon_hall_ramp");
	public static final StructurePieceType HALL_HALL = legacy("dragon_hall_hall");
	public static final StructurePieceType HALL_CAVERN = legacy("dragon_hall_cavern");
	public static final StructureType<DragonRoostStructure> DRAGON_ROOST = type("dragon_roost", DragonRoostStructure.CODEC);
	public static final StructurePieceType ROOST_PIECE = piece("dragon_roost", (context, tag) -> new DragonLairPieces.Roost(tag));
	public static final StructureType<DragonCaveStructure> DRAGON_CAVE = type("dragon_cave", DragonCaveStructure.CODEC);
	public static final StructurePieceType CAVE_PIECE = piece("dragon_cave", (context, tag) -> new DragonLairPieces.Cave(tag));
	public static final StructureType<DreadThroneStructure> DREAD_THRONE = type("dread_throne", DreadThroneStructure.CODEC);
	/** The first thrones (an open arena), kept so worlds made with them still load as they were. */
	public static final StructurePieceType DREAD_THRONE_PIECE = piece("dread_throne", (context, tag) -> new DreadPieces.Throne(tag));
	public static final StructurePieceType DREAD_CITADEL_PIECE = piece("dread_citadel", (context, tag) -> new CitadelPiece(tag));
	public static final StructureType<DreadSpireStructure> DREAD_SPIRE = type("dread_spire", DreadSpireStructure.CODEC);
	public static final StructurePieceType DREAD_SPIRE_PIECE = piece("dread_spire", (context, tag) -> new DreadPieces.Spire(tag));
	public static final StructureType<PillagerKingdomStructure> PILLAGER_KINGDOM = type("pillager_kingdom", PillagerKingdomStructure.CODEC);
	public static final StructurePieceType KINGDOM = piece("pillager_kingdom", (context, tag) -> new PillagerKingdomPiece(tag));
	public static final StructureType<PixieSanctuaryStructure> PIXIE_SANCTUARY = type("pixie_sanctuary", PixieSanctuaryStructure.CODEC);
	public static final StructurePieceType SANCTUARY = piece("pixie_sanctuary", (context, tag) -> new PixieSanctuaryPiece(tag));
	public static final StructureType<PixieCavernStructure> PIXIE_CAVERN = type("pixie_cavern", PixieCavernStructure.CODEC);
	public static final StructurePieceType CAVERN = piece("pixie_cavern", (context, tag) -> new PixieCavernPiece(tag));
	public static final StructureType<WitchCottageStructure> WITCH_COTTAGE = type("witch_cottage", WitchCottageStructure.CODEC);
	public static final StructurePieceType WITCH_COTTAGE_PIECE = piece("witch_cottage", (context, tag) -> new WitchCottagePiece(tag));
	public static final StructureType<HydraCaveStructure> HYDRA_CAVE = type("hydra_cave", HydraCaveStructure.CODEC);
	public static final StructurePieceType HYDRA_CAVE_PIECE = piece("hydra_cave", (context, tag) -> new HydraCavePiece(tag));

	private IafStructures() {
	}

	private static <S extends net.minecraft.world.level.levelgen.structure.Structure> StructureType<S> type(String name, com.mojang.serialization.Codec<S> codec) {
		return Registry.register(BuiltInRegistries.STRUCTURE_TYPE, IafMod.id(name), () -> codec);
	}

	private static StructurePieceType legacy(String name) {
		StructurePieceType[] self = new StructurePieceType[1];
		self[0] = piece(name, (context, tag) -> new DragonHallPieces.Legacy(self[0], tag));
		return self[0];
	}

	private static StructurePieceType piece(String name, StructurePieceType type) {
		return Registry.register(BuiltInRegistries.STRUCTURE_PIECE, IafMod.id(name), type);
	}

	/** Loads the class (its fields register everything). */
	public static void init() {
	}
}

package com.iafabric.world.structure;

import com.iafabric.IafMod;
import com.iafabric.mixin.JigsawStructureAccessor;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.minecraft.world.level.levelgen.structure.placement.RandomSpreadStructurePlacement;
import net.minecraft.world.level.levelgen.structure.placement.StructurePlacement;
import net.minecraft.world.level.levelgen.structure.structures.JigsawStructure;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Room for the mod's buildings. Our buildings are made last in a chunk (they clear the trees and ease the land), so a
 * village, a trial chamber or a temple in their way was left cut in half. Now a structure of the game or of another mod
 * gives way (it is not made) where it would reach into one of our buildings that will be made there, and of our own
 * buildings the commoner gives way to the rarer one. It is decided where the game picks a structure's place, so
 * /locate and the explorer maps never lead to one that gave way; our buildings keep their frequency.
 */
public final class StructureRoom {
	/** How far a building of ours reaches from the middle of its start chunk at most (a pixie cavern's tunnel). */
	private static final int OUR_REACH = 170;
	/** Blocks kept clear round our buildings' parts. */
	private static final int MARGIN = 3;
	/** The most a jigsaw structure is thought to reach (they rarely go as far as their limit). */
	private static final int JIGSAW_REACH = 96;
	private static final int JIGSAW_HEIGHT = 40;
	private static final Map<Key, List<BoundingBox>> OURS = new ConcurrentHashMap<>();
	private static final AtomicBoolean WARNED = new AtomicBoolean();
	private static volatile Object setsFor;
	private static volatile List<Ours> sets = List.of();

	private record Key(long seed, Structure structure, int x, int z) {
	}

	/** One of our buildings' sets: the rarer (wider spacing) has the stronger claim to its place. */
	private record Ours(Structure structure, RandomSpreadStructurePlacement placement, int rank, String id) {
		boolean outranks(Ours other) {
			return rank != other.rank ? rank > other.rank : id.compareTo(other.id) > 0;
		}
	}

	private StructureRoom() {
	}

	/** Whether a structure about to be placed must give way to one of our buildings. */
	public static boolean givesWay(Structure self, Structure.GenerationContext context, Structure.GenerationStub stub) {
		try {
			return check(self, context, stub);
		} catch (RuntimeException e) {
			if (WARNED.compareAndSet(false, true)) {
				IafMod.LOGGER.warn("Could not check the room of a structure (it is made as it is)", e);
			}
			return false;
		}
	}

	public static boolean ours(Structure structure) {
		StructureType<?> type = structure.type();
		return type == IafStructures.DRAGON_HALL || type == IafStructures.DRAGON_ROOST || type == IafStructures.DRAGON_CAVE
			|| type == IafStructures.DREAD_THRONE || type == IafStructures.DREAD_SPIRE || type == IafStructures.PILLAGER_KINGDOM
			|| type == IafStructures.PIXIE_SANCTUARY || type == IafStructures.PIXIE_CAVERN;
	}

	private static boolean check(Structure self, Structure.GenerationContext context, Structure.GenerationStub stub) {
		StructureType<?> type = self.type();
		// never moved: the strongholds (the End's way), and what is everywhere and meant to be cut (mineshafts, buried chests)
		if (type == StructureType.STRONGHOLD || type == StructureType.MINESHAFT || type == StructureType.BURIED_TREASURE) {
			return false;
		}
		List<Ours> all = sets(context);
		if (all.isEmpty()) {
			return false;
		}
		Ours mine = null;
		if (ours(self)) {
			for (Ours o : all) {
				if (o.structure == self) {
					mine = o;
				}
			}
		}
		List<BoundingBox> room = room(self, stub, mine != null);
		if (room.isEmpty()) {
			return false;
		}
		BoundingBox whole = room.get(0);
		for (BoundingBox b : room) {
			whole = whole.encapsulate(b);
		}
		var possible = context.biomeSource().possibleBiomes();
		for (Ours o : all) {
			if (o == mine || mine != null && !o.outranks(mine)) {
				continue;
			}
			if (o.structure.biomes().stream().noneMatch(possible::contains)) {
				continue;
			}
			int spacing = o.placement.spacing();
			int cx0 = SectionPos.blockToSectionCoord(whole.minX() - OUR_REACH);
			int cx1 = SectionPos.blockToSectionCoord(whole.maxX() + OUR_REACH);
			int cz0 = SectionPos.blockToSectionCoord(whole.minZ() - OUR_REACH);
			int cz1 = SectionPos.blockToSectionCoord(whole.maxZ() + OUR_REACH);
			for (int gx = Math.floorDiv(cx0, spacing); gx <= Math.floorDiv(cx1, spacing); gx++) {
				for (int gz = Math.floorDiv(cz0, spacing); gz <= Math.floorDiv(cz1, spacing); gz++) {
					ChunkPos start = o.placement.getPotentialStructureChunk(context.seed(), gx * spacing, gz * spacing);
					if (start.x < cx0 || start.x > cx1 || start.z < cz0 || start.z > cz1
						|| !frequencyAllows(o.placement, start.x, start.z, context.seed())) {
						continue;
					}
					for (BoundingBox theirs : made(context, o.structure, start)) {
						BoundingBox kept = theirs.inflatedBy(MARGIN);
						for (BoundingBox b : room) {
							if (b.intersects(kept)) {
								return true;
							}
						}
					}
				}
			}
		}
		return false;
	}

	/** The parts of one of our buildings made at a start chunk (none if it is not made there). */
	private static List<BoundingBox> made(Structure.GenerationContext context, Structure structure, ChunkPos start) {
		Key key = new Key(context.seed(), structure, start.x, start.z);
		List<BoundingBox> boxes = OURS.get(key);
		if (boxes == null) {
			var at = new Structure.GenerationContext(context.registryAccess(), context.chunkGenerator(), context.biomeSource(),
				context.randomState(), context.structureTemplateManager(), context.seed(), start, context.heightAccessor(), structure.biomes()::contains);
			boxes = structure.findValidGenerationPoint(at).map(StructureRoom::parts).orElse(List.of());
			if (OURS.size() > 8192) {
				OURS.clear();
			}
			OURS.put(key, boxes);
		}
		return boxes;
	}

	private static List<BoundingBox> parts(Structure.GenerationStub stub) {
		List<BoundingBox> boxes = new ArrayList<>();
		for (StructurePiece piece : stub.getPiecesBuilder().build().pieces()) {
			boxes.add(piece.getBoundingBox());
		}
		return boxes;
	}

	/**
	 * The room a structure takes: our buildings' parts exactly (their makers are plain), any other's a box round its
	 * start as wide as it may reach (its pieces are not laid out here: a jigsaw's layout draws on the chunk's random).
	 */
	private static List<BoundingBox> room(Structure self, Structure.GenerationStub stub, boolean ours) {
		if (ours) {
			return parts(stub);
		}
		BlockPos p = stub.position();
		StructureType<?> type = self.type();
		int wide;
		int down;
		int up;
		if (self instanceof JigsawStructure jigsaw) {
			var reach = ((JigsawStructureAccessor) (Object) jigsaw).iceandfire_fabric$maxDistanceFromCenter();
			wide = Math.min(reach, JIGSAW_REACH);
			down = Math.min(reach, JIGSAW_HEIGHT);
			up = down;
		} else if (type == StructureType.WOODLAND_MANSION) {
			wide = 64;
			down = 8;
			up = 48;
		} else if (type == StructureType.OCEAN_MONUMENT) {
			wide = 32;
			down = 30;
			up = 30;
		} else if (type == StructureType.IGLOO) {
			// its cellar goes down a long ladder
			wide = 10;
			down = 40;
			up = 12;
		} else if (type == StructureType.DESERT_PYRAMID || type == StructureType.JUNGLE_TEMPLE || type == StructureType.SWAMP_HUT) {
			wide = 16;
			down = 20;
			up = 24;
		} else if (type == StructureType.SHIPWRECK || type == StructureType.OCEAN_RUIN) {
			wide = 24;
			down = 16;
			up = 24;
		} else if (type == StructureType.RUINED_PORTAL) {
			wide = 12;
			down = 12;
			up = 16;
		} else {
			wide = 48;
			down = 48;
			up = 48;
		}
		return List.of(new BoundingBox(p.getX() - wide, p.getY() - down, p.getZ() - wide, p.getX() + wide, p.getY() + up, p.getZ() + wide));
	}

	/** Our buildings' sets in this world (one building each, spread at random). */
	private static List<Ours> sets(Structure.GenerationContext context) {
		Object registries = context.registryAccess();
		if (setsFor == registries) {
			return sets;
		}
		List<Ours> found = new ArrayList<>();
		context.registryAccess().registryOrThrow(Registries.STRUCTURE_SET).holders().forEach(holder -> {
			var set = holder.value();
			if (set.structures().size() == 1 && set.placement() instanceof RandomSpreadStructurePlacement spread) {
				Structure structure = set.structures().get(0).structure().value();
				if (ours(structure)) {
					found.add(new Ours(structure, spread, spread.spacing(), holder.key().location().toString()));
				}
			}
		});
		sets = List.copyOf(found);
		setsFor = registries;
		return sets;
	}

	/** Forgets what was worked out (a new world). */
	public static void clear() {
		OURS.clear();
		setsFor = null;
	}

	/** Whether a placement's frequency lets it start at a chunk (1.21's applyAdditionalChunkRestrictions). */
	private static boolean frequencyAllows(StructurePlacement placement, int x, int z, long seed) {
		return placement.frequency() >= 1.0F || placement.frequencyReductionMethod().shouldGenerate(seed, placement.salt(), x, z, placement.frequency());
	}
}

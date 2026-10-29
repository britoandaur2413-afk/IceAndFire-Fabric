package com.iafabric.pixie;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.SectionPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureCheckResult;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.level.levelgen.structure.placement.RandomSpreadStructurePlacement;
import net.minecraft.world.level.levelgen.structure.placement.StructurePlacement;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The nearest of some structures, looked for a little at a time (a few places each tick) as the game's own search
 * does it all at once: ring after ring of the places where they may start, each asked whether one starts there (its
 * land is never generated: at most its structures' starts, as the locate command does).
 */
public final class FairyLocator {
	/** Places asked in a tick. */
	private static final int BUDGET = 2;

	private final ServerLevel level;
	private final BlockPos from;
	/** Places not to be found again (found empty before). */
	private final List<BlockPos> skip;
	private final List<Scan> scans = new ArrayList<>();
	private @Nullable BlockPos found;
	private boolean over;

	private final class Scan {
		final RandomSpreadStructurePlacement placement;
		final List<Holder<Structure>> structures;
		final int maxRing;
		int ring;
		int x;
		int z;
		boolean done;

		Scan(RandomSpreadStructurePlacement placement, List<Holder<Structure>> structures, int maxRing) {
			this.placement = placement;
			this.structures = structures;
			this.maxRing = maxRing;
			this.x = 0;
			this.z = 0;
		}

		/** The next place on its rings, or false when its rings are over. */
		boolean advance() {
			if (ring == 0) {
				ring = 1;
				x = -1;
				z = -1;
				return ring <= maxRing;
			}
			// along the edge of the ring: |x| or |z| is the ring
			do {
				z++;
				if (z > ring) {
					z = -ring;
					x++;
					if (x > ring) {
						ring++;
						if (ring > maxRing) {
							return false;
						}
						x = -ring;
						z = -ring;
						return true;
					}
				}
			} while (Math.abs(x) != ring && Math.abs(z) != ring);
			return true;
		}
	}

	/**
	 * A search from a place for the nearest of these structures, within about that many blocks (the structures of
	 * other worlds are not found: it is over at once).
	 */
	public FairyLocator(ServerLevel level, List<Holder<Structure>> wanted, BlockPos from, int within) {
		this(level, wanted, from, within, List.of());
	}

	public FairyLocator(ServerLevel level, List<Holder<Structure>> wanted, BlockPos from, int within, List<BlockPos> skip) {
		this.level = level;
		this.from = from;
		this.skip = skip;
		if (!level.getServer().getWorldData().worldGenOptions().generateStructures()) {
			this.over = true;
			return;
		}
		Map<RandomSpreadStructurePlacement, List<Holder<Structure>>> byPlacement = new LinkedHashMap<>();
		for (Holder<Structure> structure : wanted) {
			for (StructurePlacement placement : level.getChunkSource().getGeneratorState().getPlacementsForStructure(structure)) {
				if (placement instanceof RandomSpreadStructurePlacement spread) {
					byPlacement.computeIfAbsent(spread, p -> new ArrayList<>()).add(structure);
				}
			}
		}
		for (Map.Entry<RandomSpreadStructurePlacement, List<Holder<Structure>>> entry : byPlacement.entrySet()) {
			int spacing = Math.max(1, entry.getKey().spacing());
			this.scans.add(new Scan(entry.getKey(), entry.getValue(), Math.max(1, (within / 16 + spacing - 1) / spacing)));
		}
		this.over = this.scans.isEmpty();
	}

	public boolean over() {
		return this.over;
	}

	/** Where the nearest one is as the game's own search says it (its start chunk's corner, at height 0), or null (none found yet, or none at all). */
	public @Nullable BlockPos found() {
		return this.found;
	}

	/** Looks a little further; true when the search is over. */
	public boolean step() {
		if (this.over) {
			return true;
		}
		StructureManager manager = this.level.structureManager();
		long seed = this.level.getChunkSource().getGeneratorState().getLevelSeed();
		int originX = SectionPos.blockToSectionCoord(this.from.getX());
		int originZ = SectionPos.blockToSectionCoord(this.from.getZ());
		int budget = BUDGET;
		while (budget > 0) {
			Scan scan = null;
			for (Scan s : this.scans) {
				if (!s.done) {
					scan = s;
					break;
				}
			}
			if (scan == null) {
				this.over = true;
				return true;
			}
			int sectorX = originX + scan.placement.spacing() * scan.x;
			int sectorZ = originZ + scan.placement.spacing() * scan.z;
			ChunkPos chunk = scan.placement.getPotentialStructureChunk(seed, sectorX, sectorZ);
			BlockPos at = startAt(manager, scan, chunk);
			budget--;
			if (at != null && skipped(at)) {
				at = null;
			}
			if (at != null) {
				if (this.found == null || horizontal(at) < horizontal(this.found)) {
					this.found = at;
				}
				scan.done = true;
			} else if (!scan.advance()) {
				scan.done = true;
			}
		}
		return false;
	}

	private boolean skipped(BlockPos at) {
		for (BlockPos old : this.skip) {
			double dx = old.getX() - at.getX();
			double dz = old.getZ() - at.getZ();
			if (dx * dx + dz * dz < 96 * 96) {
				return true;
			}
		}
		return false;
	}

	private double horizontal(BlockPos at) {
		double dx = at.getX() - this.from.getX();
		double dz = at.getZ() - this.from.getZ();
		return dx * dx + dz * dz;
	}

	private @Nullable BlockPos startAt(StructureManager manager, Scan scan, ChunkPos chunk) {
		for (Holder<Structure> structure : scan.structures) {
			StructureCheckResult check = manager.checkStructurePresence(chunk, structure.value(), scan.placement, false);
			if (check == StructureCheckResult.START_NOT_PRESENT) {
				continue;
			}
			if (check == StructureCheckResult.START_PRESENT) {
				return scan.placement.getLocatePos(chunk);
			}
			ChunkAccess access = this.level.getChunk(chunk.x, chunk.z, ChunkStatus.STRUCTURE_STARTS);
			StructureStart start = manager.getStartForStructure(net.minecraft.core.SectionPos.bottomOf(access), structure.value(), access);
			if (start != null && start.isValid()) {
				return scan.placement.getLocatePos(start.getChunkPos());
			}
		}
		return null;
	}
}

package com.iafabric.world.structure;

import com.iafabric.block.IafBlockRegistry;
import com.iafabric.entity.EntityBlackFrostGuard;
import com.iafabric.entity.EntityCryonaAsleep;
import com.iafabric.entity.EntityDreadMob;
import com.iafabric.entity.IafEntityRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.LanternBlock;
import net.minecraft.world.level.block.SkullBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Half;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import org.jspecify.annotations.Nullable;

/**
 * Cryona's citadel in the snow: high walls of dark stone with round towers at their corners and middles, a gatehouse
 * with its portcullis raised, a courtyard of graves, braziers and obelisks of ice kept by the dead, and at its back the
 * keep: a great hall of pillars and hanging soul lanterns, then through a high arch the throne room under a tall
 * roof, a hall made for Black Frost (some fifty blocks from its jaws to its tail, near forty from wing to wing): Cryona
 * sleeps on her throne near its front and Black Frost sits upright behind her on its bed of blue ice, its head high
 * over her, its wings folded inside the room's walls (nothing between who comes in and her). While she reigns no stone
 * of it can be broken (boss/CastleWard): the way in is the gate.
 */
public class CitadelPiece extends BuiltPiece {
	/** Half its width, to the outer faces of its side walls. */
	static final int W = 44;
	/** From the gate to the outer face of its back wall. */
	static final int L = 132;
	/** Its middle (on the structure's start chunk), from the gate. */
	public static final int MIDDLE = 66;
	private static final int WALL = 3;
	private static final int WALL_H = 14;
	/** The keep: half its width, where it begins and ends. */
	private static final int KW = 28;
	private static final int KV0 = 52;
	private static final int KV1 = 129;
	private static final int HALL_H = 24;
	/** The arch between the great hall and the throne room (the room deep enough for Black Frost behind her throne). */
	private static final int ARCH_V = 76;
	private static final int ROOM_H = 30;
	/** Cryona's throne near the room's front (her seat a row further, a step up its dais). */
	public static final int THRONE_V = 89;
	/** Where her throne stood in older citadels (at the room's back, Black Frost asleep in its middle, in the way). */
	private static final int OLD_THRONE_V = 121;
	/**
	 * How far behind her seat Black Frost sits: its head over her throne (above the low crown of ice), its wings (some
	 * 44 wide, 22 deep) and its tail on its bed, all of it inside the room.
	 */
	public static final int GUARD_BEHIND = 20;
	/** Black Frost's bed of blue ice: its half width and half depth. */
	private static final double BED_U = 23.5;
	private static final double BED_V = 16;
	private static final int ROOF_V = (ARCH_V + KV1) / 2;
	private static final int BLEND = 10;
	private static final int DEPTH = 24;
	private static final int TOP = 52;
	/** Round towers: u, v, radius, height. */
	private static final int[][] TOWERS = {
		{-(W - 1), 1, 6, 24}, {W - 1, 1, 6, 24}, {-(W - 1), L - 1, 6, 26}, {W - 1, L - 1, 6, 26},
		{-(W - 1), 66, 5, 20}, {W - 1, 66, 5, 20}, {-(W - 1), 100, 5, 20}, {W - 1, 100, 5, 20}
	};
	private int ground;

	public CitadelPiece(int x, int y, int z, int facing, long seed) {
		super(IafStructures.DREAD_CITADEL_PIECE, x, y, z, facing, 0, 0, seed, -W - 6 - BLEND, W + 6 + BLEND, -10 - BLEND, L + 6 + BLEND,
			y - DEPTH, y + TOP);
	}

	public CitadelPiece(CompoundTag tag) {
		super(IafStructures.DREAD_CITADEL_PIECE, tag);
	}

	@Override
	protected int plantClearing() {
		return 10;
	}

	@Override
	protected boolean keptOpen(WorldGenLevel level, int u, int v, int y) {
		// the citadel itself (inside its outline) from its floor up; not the land eased round it, whose rule hangs on
		// the land
		if (Math.abs(u) > W + 6 || v < -10 || v > L + 6 || y < gateY) {
			return false;
		}
		BlockState state = at(level, u, v, y);
		return state != null && state.isAir();
	}

	@Override
	protected void column(WorldGenLevel level, int u, int v, int x, int z) {
		ground = DreadPieces.groundAt(level, x, z, gateY - DEPTH - 6);
	}

	/** Where Cryona sleeps on her throne. */
	public BlockPos throneSeat() {
		return world(0, THRONE_V + 1, gateY + 3);
	}

	/** Where Black Frost sits: straight behind her throne, on its bed of blue ice. */
	public BlockPos guard() {
		return world(0, THRONE_V + 1 + GUARD_BEHIND, gateY);
	}

	/** A citadel's keep being made anew, a few rows a tick, by the gate of the citadel. */
	private static final java.util.Map<BlockPos, Renewal> RENEWALS = new java.util.HashMap<>();
	/** How many rows of its keep are made anew a tick (no stop of the game for it). */
	private static final int ROWS_A_TICK = 2;
	/** What renewThroneRoom says: done (or not needed), wait (its land is not all loaded), working (call again next tick). */
	public static final int DONE = 0;
	public static final int WAIT = 1;
	public static final int WORKING = 2;

	/** A keep being made anew: the next row, and what the chest behind her old throne held. */
	private static final class Renewal {
		int v = KV0;
		final java.util.List<net.minecraft.world.item.ItemStack> held = new java.util.ArrayList<>();
		net.minecraft.resources.ResourceKey<net.minecraft.world.level.storage.loot.LootTable> loot;
		long seed;
	}

	/**
	 * An older citadel's keep (her throne at the back of a room too small for Black Frost, which lay asleep in its
	 * middle, in the way of who came in, its wings through its pillars) made as it is now: the throne room deep and
	 * wide enough for it (the great hall a little shorter), her throne near its front, Black Frost sitting behind it.
	 * A few rows a tick; the chest behind her throne moves along with what it holds, and a player left inside a new
	 * wall goes to the arch. Her sign goes to her new seat, Black Frost to its bed, and what is kept of her duel with
	 * them. DONE when done or not needed (not a citadel's, or one as it is now), WAIT while its land is not all loaded
	 * (looked at again later), WORKING while it is being made (called again next tick).
	 */
	public static int renewThroneRoom(net.minecraft.server.level.ServerLevel level, EntityCryonaAsleep queen) {
		var start = com.iafabric.boss.CastleWard.citadelAt(level, queen.throne());
		if (start == null) {
			return DONE;
		}
		CitadelPiece citadel = null;
		for (var piece : start.getPieces()) {
			if (piece instanceof CitadelPiece found) {
				citadel = found;
			}
		}
		if (citadel == null || queen.throne().equals(citadel.throneSeat())) {
			return DONE;
		}
		Renewal renewal = RENEWALS.get(citadel.gate());
		if (renewal == null) {
			if (!citadel.keepLoaded(level)) {
				return WAIT;
			}
			renewal = citadel.startRenewal(level);
			RENEWALS.put(citadel.gate(), renewal);
		}
		for (int i = 0; i < ROWS_A_TICK && renewal.v <= KV1; i++) {
			citadel.renewRow(level, renewal.v++);
		}
		if (renewal.v <= KV1) {
			return WORKING;
		}
		RENEWALS.remove(citadel.gate());
		citadel.finishRenewal(level, renewal);
		BlockPos from = queen.throne();
		queen.seat(citadel.throneSeat(), citadel.guard());
		for (EntityBlackFrostGuard frost : level.getEntitiesOfClass(EntityBlackFrostGuard.class, queen.getBoundingBox().inflate(EntityCryonaAsleep.HERS))) {
			BlockPos at = citadel.guard();
			frost.place(at, queen.getYRot());
		}
		com.iafabric.boss.ThroneFight.moved(level.getServer(), level.dimension(), from, citadel.throneSeat(), citadel.guard());
		com.iafabric.IafMod.LOGGER.info("An older citadel's keep made anew at {}: her throne {} -> {}", citadel.gate(), from, citadel.throneSeat());
		return DONE;
	}

	/** Whether all of its keep's land is loaded. */
	private boolean keepLoaded(net.minecraft.server.level.ServerLevel level) {
		for (int v = KV0; v <= KV1 + 7; v += 8) {
			for (int u = -KW; u <= KW + 7; u += 8) {
				if (!level.isLoaded(world(Math.min(u, KW), Math.min(v, KV1), gateY))) {
					return false;
				}
			}
		}
		return true;
	}

	/** What the chest behind her old throne holds is kept (emptied first: nothing of it falls out as it goes). */
	private Renewal startRenewal(net.minecraft.server.level.ServerLevel level) {
		Renewal renewal = new Renewal();
		BlockPos oldChest = world(-3, OLD_THRONE_V + 2, gateY + 3);
		if (level.getBlockEntity(oldChest) instanceof net.minecraft.world.level.block.entity.ChestBlockEntity chest) {
			renewal.loot = chest.getLootTable();
			renewal.seed = chest.getLootTableSeed();
			for (int i = 0; i < chest.getContainerSize(); i++) {
				renewal.held.add(chest.getItem(i).copy());
			}
			chest.setLootTable(null, 0L);
			chest.clearContent();
		}
		return renewal;
	}

	/** One row of its keep as it is now, block by block (only what differs). */
	private void renewRow(net.minecraft.server.level.ServerLevel level, int v) {
		int g = gateY;
		for (int u = -KW; u <= KW; u++) {
			for (int y = g - 1; y <= g + TOP; y++) {
				BlockState want = at(level, u, v, y);
				BlockPos pos = world(u, v, y);
				if (want != null && !level.getBlockState(pos).equals(want)) {
					level.setBlock(pos, want, net.minecraft.world.level.block.Block.UPDATE_CLIENTS);
				}
			}
		}
	}

	/** The chest behind her new throne with what the old one held; who was left inside a new wall, at the arch. */
	private void finishRenewal(net.minecraft.server.level.ServerLevel level, Renewal renewal) {
		int g = gateY;
		BlockPos chestAt = world(-3, THRONE_V + 2, g + 3);
		level.setBlock(chestAt, Blocks.CHEST.defaultBlockState().setValue(net.minecraft.world.level.block.ChestBlock.FACING, forwardDir().getOpposite()),
			net.minecraft.world.level.block.Block.UPDATE_CLIENTS);
		if (level.getBlockEntity(chestAt) instanceof net.minecraft.world.level.block.entity.ChestBlockEntity chest) {
			if (renewal.loot != null) {
				chest.setLootTable(renewal.loot, renewal.seed);
			}
			for (int i = 0; i < renewal.held.size() && i < chest.getContainerSize(); i++) {
				chest.setItem(i, renewal.held.get(i));
			}
		}
		var keep = new net.minecraft.world.phys.AABB(net.minecraft.world.phys.Vec3.atCenterOf(world(-KW, KV0, g - 1)),
			net.minecraft.world.phys.Vec3.atCenterOf(world(KW, KV1, g + TOP)));
		BlockPos arch = world(0, ARCH_V - 3, g);
		for (var player : level.getEntitiesOfClass(net.minecraft.world.entity.player.Player.class, keep)) {
			if (!level.noCollision(player)) {
				player.teleportTo(arch.getX() + 0.5, arch.getY(), arch.getZ() + 0.5);
			}
		}
	}

	private BlockState stone(int u, int v, int y) {
		float n = noise(u, v, y, 21);
		return (n < 0.55F ? Blocks.DEEPSLATE_BRICKS : n < 0.67F ? Blocks.CRACKED_DEEPSLATE_BRICKS : n < 0.85F ? Blocks.DEEPSLATE_TILES
			: Blocks.POLISHED_BLACKSTONE_BRICKS).defaultBlockState();
	}

	private BlockState foundation(int u, int v, int y) {
		return (noise(u, v, y, 22) < 0.6F ? Blocks.COBBLED_DEEPSLATE : Blocks.DEEPSLATE).defaultBlockState();
	}

	@Override
	protected @Nullable BlockState at(WorldGenLevel level, int u, int v, int y) {
		int g = gateY;
		int dy = y - g;
		int au = Math.abs(u);
		// how far outside the castle's outline: the land eased to its height there
		double ou = Math.max(0, au - (W + 6));
		double ov = Math.max(0, Math.max(-10 - v, v - (L + 6)));
		double out = Math.sqrt(ou * ou + ov * ov);
		if (out > BLEND) {
			return null;
		}
		if (out > 0) {
			double t = out / BLEND;
			t = t * t * (3 - 2 * t);
			int target = (int) Math.round((g - 1) * (1 - t) + ground * t);
			if (y == target) {
				return v < 0 && au <= 3 ? Blocks.PACKED_ICE.defaultBlockState() : Blocks.SNOW_BLOCK.defaultBlockState();
			}
			if (y < target) {
				return y > ground && y > g - 12 ? Blocks.PACKED_ICE.defaultBlockState() : null;
			}
			return y <= Math.max(ground, g + 3) ? Blocks.AIR.defaultBlockState() : null;
		}
		// under it all: its footing down to the land
		if (y < g - 1) {
			return y > ground ? foundation(u, v, y) : null;
		}
		for (int[] tower : TOWERS) {
			int du = u - tower[0];
			int dv = v - tower[1];
			if (du * du + dv * dv <= tower[2] * tower[2]) {
				return tower(u, v, y, dy, Math.sqrt(du * du + dv * dv), tower[2], tower[3]);
			}
		}
		if (v >= -6 && v <= 4 && au <= 10) {
			BlockState gate = gatehouse(u, v, y, dy, au);
			if (gate != null) {
				return gate;
			}
		}
		if (au > W || v < 0 || v > L) {
			// the frozen ground round its walls
			if (y == g - 1) {
				return v < 0 && au <= 3 ? Blocks.PACKED_ICE.defaultBlockState()
					: noise(u, v, y, 23) < 0.75F ? Blocks.SNOW_BLOCK.defaultBlockState() : Blocks.PACKED_ICE.defaultBlockState();
			}
			return dy <= TOP ? Blocks.AIR.defaultBlockState() : null;
		}
		if (au > W - WALL || v < WALL || v > L - WALL) {
			return outerWall(u, v, y, dy, au);
		}
		if (au <= KW && v >= KV0 && v <= KV1) {
			return keep(u, v, y, dy, au);
		}
		return bailey(u, v, y, dy, au);
	}

	/** A round tower: thick walls with barred slits, floors inside, crenels, a cone roof with a spike of ice. */
	private BlockState tower(int u, int v, int y, int dy, double r, int radius, int height) {
		if (dy == -1) {
			return Blocks.POLISHED_BLACKSTONE.defaultBlockState();
		}
		if (dy < height) {
			if (r > radius - 1.5) {
				if (dy % 7 == 4 && (Math.abs(u) > W - 2 || v < 3 || v > L - 3) && (r > radius - 0.8)) {
					return Blocks.IRON_BARS.defaultBlockState();
				}
				return dy % 7 == 6 ? Blocks.CHISELED_DEEPSLATE.defaultBlockState() : stone(u, v, y);
			}
			return dy > 0 && dy % 7 == 0 ? Blocks.POLISHED_BLACKSTONE.defaultBlockState() : Blocks.AIR.defaultBlockState();
		}
		if (dy == height) {
			return r > radius - 1.5 ? stone(u, v, y) : Blocks.POLISHED_BLACKSTONE.defaultBlockState();
		}
		if (dy == height + 1 && r > radius - 1) {
			return ((u + v) & 1) == 0 ? stone(u, v, y) : Blocks.AIR.defaultBlockState();
		}
		int up = dy - height - 2;
		if (up >= 0) {
			double cone = radius - 1.5 - up * 0.55;
			if (cone > 0.5 && r <= cone && r > cone - 1.3) {
				return Blocks.DEEPSLATE_TILES.defaultBlockState();
			}
			if (cone <= 0.5 && r < 0.75 && up < (radius - 1.5) / 0.55 + 4) {
				return Blocks.BLUE_ICE.defaultBlockState();
			}
		}
		return dy <= TOP ? Blocks.AIR.defaultBlockState() : null;
	}

	/** The gatehouse: two square towers and the arched way between them, its portcullis raised. */
	private @Nullable BlockState gatehouse(int u, int v, int y, int dy, int au) {
		boolean towers = au >= 4 && au <= 10;
		boolean passage = au <= 3 && v <= 2;
		if (!towers && !passage) {
			return null;
		}
		if (dy == -1) {
			return passage ? (au <= 1 ? Blocks.PACKED_ICE : IafBlockRegistry.DREAD_STONE_TILE).defaultBlockState() : Blocks.POLISHED_BLACKSTONE.defaultBlockState();
		}
		if (towers) {
			boolean shell = au == 4 || au == 10 || v == -6 || v == 4;
			if (dy < 20) {
				if (shell && dy % 6 == 3 && (v == -6 || au == 10)) {
					return Blocks.IRON_BARS.defaultBlockState();
				}
				return shell ? stone(u, v, y) : dy > 0 && dy % 7 == 0 ? Blocks.POLISHED_BLACKSTONE.defaultBlockState() : Blocks.AIR.defaultBlockState();
			}
			if (dy == 20) {
				return stone(u, v, y);
			}
			if (dy == 21) {
				return shell && ((u + v) & 1) == 0 ? stone(u, v, y) : Blocks.AIR.defaultBlockState();
			}
			if ((au == 7) && v == -1 && dy >= 22 && dy <= 24) {
				return Blocks.BLUE_ICE.defaultBlockState();
			}
			return dy <= TOP ? Blocks.AIR.defaultBlockState() : null;
		}
		int arch = au <= 1 ? 9 : au == 2 ? 8 : 7;
		if (dy < arch) {
			// the portcullis, raised: only its teeth show under the arch
			if (v == -3 && dy >= arch - 1) {
				return Blocks.IRON_BARS.defaultBlockState();
			}
			return Blocks.AIR.defaultBlockState();
		}
		if (dy < 20) {
			return dy == arch && v == -6 ? Blocks.CHISELED_DEEPSLATE.defaultBlockState() : stone(u, v, y);
		}
		if (dy == 20) {
			return stone(u, v, y);
		}
		if (dy == 21 && v == -6) {
			return ((u + v) & 1) == 0 ? stone(u, v, y) : Blocks.AIR.defaultBlockState();
		}
		return dy <= TOP ? Blocks.AIR.defaultBlockState() : null;
	}

	/** The curtain walls, a walk on their top behind crenels; the gate through the front one. */
	private BlockState outerWall(int u, int v, int y, int dy, int au) {
		if (dy == -1) {
			return Blocks.POLISHED_BLACKSTONE.defaultBlockState();
		}
		if (v < WALL && au <= 3) {
			int arch = au <= 1 ? 9 : au == 2 ? 8 : 7;
			if (dy < arch) {
				return Blocks.AIR.defaultBlockState();
			}
		}
		if (dy < WALL_H) {
			return dy % 6 == 5 ? Blocks.CHISELED_DEEPSLATE.defaultBlockState() : stone(u, v, y);
		}
		if (dy == WALL_H) {
			return Blocks.POLISHED_BLACKSTONE_BRICKS.defaultBlockState();
		}
		if (dy == WALL_H + 1) {
			boolean edge = au == W || v == 0 || v == L;
			if (edge) {
				return ((u + v) & 1) == 0 ? stone(u, v, y) : Blocks.AIR.defaultBlockState();
			}
			return Math.floorMod(u * 5 + v * 3, 17) == 0 && !edge ? IafBlockRegistry.DREAD_TORCH.defaultBlockState() : Blocks.AIR.defaultBlockState();
		}
		return dy <= TOP ? Blocks.AIR.defaultBlockState() : null;
	}

	/** The courtyard: a path to the keep between braziers, graves, obelisks of ice, the snow. */
	private BlockState bailey(int u, int v, int y, int dy, int au) {
		boolean path = au <= 3 && v < KV0;
		if (dy == -1) {
			if (path) {
				return (au == 3 ? IafBlockRegistry.DREAD_STONE_TILE : Blocks.PACKED_ICE).defaultBlockState();
			}
			float n = noise(u, v, y, 24);
			return (n < 0.7F ? Blocks.SNOW_BLOCK : n < 0.9F ? Blocks.PACKED_ICE : Blocks.POLISHED_DEEPSLATE).defaultBlockState();
		}
		if (path) {
			return dy <= TOP ? Blocks.AIR.defaultBlockState() : null;
		}
		// braziers along the path
		if (au == 5 && v >= 10 && v <= 48 && v % 9 == 1) {
			if (dy == 0) {
				return Blocks.POLISHED_BLACKSTONE.defaultBlockState();
			}
			if (dy == 1) {
				return Blocks.SOUL_CAMPFIRE.defaultBlockState().setValue(CampfireBlock.LIT, true);
			}
		}
		// obelisks of ice
		int[][] obelisks = {{24, 18}, {32, 38}, {36, 80}, {36, 118}, {16, 44}};
		for (int[] ob : obelisks) {
			int du = au - ob[0];
			int dv = v - ob[1];
			if (du >= 0 && du <= 1 && dv >= 0 && dv <= 1) {
				int h = 7 + (int) (noise(ob[0] * (u < 0 ? -1 : 1), ob[1], 0, 25) * 4);
				if (dy < h) {
					return (dy % 3 == 2 ? Blocks.BLUE_ICE : Blocks.PACKED_ICE).defaultBlockState();
				}
				if (dy == h && du == 0 && dv == 0) {
					return Blocks.SOUL_LANTERN.defaultBlockState();
				}
			}
		}
		// graves in rows, a skull on each stone
		if (v >= 8 && v <= 46 && au >= 10 && au <= 38 && Math.floorMod(u, 4) == 0 && Math.floorMod(v, 5) == 0 && noise(u, v, 0, 26) < 0.55F) {
			if (dy == 0) {
				return Blocks.MOSSY_STONE_BRICK_WALL.defaultBlockState();
			}
			if (dy == 1) {
				return Blocks.SKELETON_SKULL.defaultBlockState().setValue(SkullBlock.ROTATION, (int) (noise(u, v, y, 27) * 16));
			}
		}
		if (dy == 0) {
			float n = noise(u, v, y, 28);
			if (n < 0.04F) {
				return Blocks.DEAD_BUSH.defaultBlockState();
			}
			if (n < 0.055F) {
				return Blocks.COBWEB.defaultBlockState();
			}
		}
		return dy <= TOP ? Blocks.AIR.defaultBlockState() : null;
	}

	/** The keep: the great hall, the arch, the throne room under its tall roof. */
	private BlockState keep(int u, int v, int y, int dy, int au) {
		boolean room = v > ARCH_V + 2;
		int wallTop = room ? ROOM_H + 1 : HALL_H + 1;
		boolean shell = au > KW - 3 || v < KV0 + 3 || v > KV1 - 3;
		if (dy == -1) {
			return shell ? Blocks.POLISHED_BLACKSTONE.defaultBlockState() : floor(u, v, y, au, room);
		}
		if (shell) {
			// the door
			if (v < KV0 + 3 && au <= 4) {
				int arch = au <= 2 ? 11 : au == 3 ? 10 : 9;
				if (dy < arch) {
					return Blocks.AIR.defaultBlockState();
				}
				if (dy == arch && v == KV0) {
					return IafBlockRegistry.DREAD_STONE_BRICKS_CHISELED.defaultBlockState();
				}
			}
			// tall windows of ice on its sides
			if (au > KW - 3 && dy >= 6 && dy <= (room ? 20 : 15) && Math.floorMod(v - 4, 8) == 0 && v > KV0 + 4 && v < KV1 - 4) {
				return Blocks.ICE.defaultBlockState();
			}
			// the throne room's cold lights: rows set in its side walls between the windows, a panel behind the throne
			if (room && au == KW - 2 && (dy == 4 || dy == 10 || dy == 16) && Math.floorMod(v, 4) == 2 && v > ARCH_V + 3 && v < KV1 - 3) {
				return Blocks.SEA_LANTERN.defaultBlockState();
			}
			// behind Black Frost a wall of ice lit from within, against which it stands out
			if (room && v == KV1 - 2 && au <= 22 && dy >= 1 && dy <= 26) {
				return Math.floorMod(u, 4) == 0 && dy % 5 == 3 ? Blocks.SEA_LANTERN.defaultBlockState()
					: au <= 20 && dy <= 24 ? Blocks.PACKED_ICE.defaultBlockState() : Blocks.BLUE_ICE.defaultBlockState();
			}
			if (dy < wallTop) {
				return dy % 8 == 7 ? IafBlockRegistry.DREAD_STONE_BRICKS_CHISELED.defaultBlockState() : stone(u, v, y);
			}
			if (dy == wallTop) {
				boolean edge = au == KW || v == KV0 || v == KV1;
				return edge && ((u + v) & 1) == 0 ? stone(u, v, y) : edge ? Blocks.AIR.defaultBlockState() : Blocks.POLISHED_BLACKSTONE_BRICKS.defaultBlockState();
			}
			return roof(u, v, y, dy, au);
		}
		// the wall with the high arch between the hall and the throne room
		if (v >= ARCH_V && v <= ARCH_V + 2) {
			double arch = 9 + Math.sqrt(Math.max(0, 49 - u * u));
			if (au <= 7 && dy < arch) {
				return Blocks.AIR.defaultBlockState();
			}
			if (dy <= ROOM_H + 1) {
				return au <= 8 && dy <= arch + 1 ? Blocks.CHISELED_DEEPSLATE.defaultBlockState() : stone(u, v, y);
			}
			return roof(u, v, y, dy, au);
		}
		if (!room) {
			return hall(u, v, y, dy, au);
		}
		return throneRoom(u, v, y, dy, au);
	}

	private BlockState floor(int u, int v, int y, int au, boolean room) {
		if (!room) {
			if (au <= 2) {
				return Blocks.BLUE_ICE.defaultBlockState();
			}
			if (au == 3 && v % 4 == 0) {
				// lights along the way to the arch
				return Blocks.SEA_LANTERN.defaultBlockState();
			}
			if (v % 8 == 0) {
				return IafBlockRegistry.DREAD_STONE_TILE.defaultBlockState();
			}
			return (((u + v) & 1) == 0 ? Blocks.POLISHED_DEEPSLATE : Blocks.DEEPSLATE_TILES).defaultBlockState();
		}
		// Black Frost's bed of blue ice behind her throne, as wide as its wings, cold lights round its edge
		double e = bed(u, v);
		if (e <= 0.9) {
			return Blocks.BLUE_ICE.defaultBlockState();
		}
		if (e <= 1.0) {
			return ((u + v) & 3) == 0 ? Blocks.SEA_LANTERN.defaultBlockState() : IafBlockRegistry.DREAD_STONE_BRICKS_CHISELED.defaultBlockState();
		}
		// two rows of light along the aisle to the throne
		if (au == 3 && Math.floorMod(v, 3) == 0 && v < THRONE_V - 4) {
			return Blocks.SEA_LANTERN.defaultBlockState();
		}
		// rings round her seat
		int dv = v - (THRONE_V + 1);
		double r = Math.sqrt(u * u + dv * dv);
		return ((int) r) % 5 < 2 ? IafBlockRegistry.DREAD_STONE_TILE.defaultBlockState() : Blocks.PACKED_ICE.defaultBlockState();
	}

	/** The great hall: two rows of pillars with hearts of ice, soul lanterns hanging on chains, braziers by the walls. */
	private BlockState hall(int u, int v, int y, int dy, int au) {
		if (dy == HALL_H) {
			return Blocks.DEEPSLATE_TILES.defaultBlockState();
		}
		if (dy > HALL_H) {
			return roof(u, v, y, dy, au);
		}
		for (int pv = 60; pv <= ARCH_V - 8; pv += 8) {
			if (Math.abs(au - 13) <= 1 && Math.abs(v - pv) <= 1) {
				if (au == 13 && v == pv) {
					return Blocks.BLUE_ICE.defaultBlockState();
				}
				boolean face = au == 13 || v == pv;
				if (face && dy % 6 == 3) {
					// bands of cold light round them
					return Blocks.SEA_LANTERN.defaultBlockState();
				}
				return dy % 6 == 0 ? Blocks.CHISELED_DEEPSLATE.defaultBlockState() : Blocks.DEEPSLATE_BRICKS.defaultBlockState();
			}
			// soul lanterns on chains between them
			if (au == 6 && v == pv + 4) {
				if (dy >= 17) {
					return DreadPieces.chain();
				}
				if (dy == 16) {
					return Blocks.SOUL_LANTERN.defaultBlockState().setValue(LanternBlock.HANGING, true);
				}
			}
		}
		// braziers by the walls
		if (au == 22 && v >= 58 && v <= ARCH_V - 2 && (v - 58) % 8 == 0) {
			if (dy == 0) {
				return Blocks.POLISHED_BLACKSTONE.defaultBlockState();
			}
			if (dy == 1) {
				return Blocks.SOUL_CAMPFIRE.defaultBlockState().setValue(CampfireBlock.LIT, true);
			}
		}
		return Blocks.AIR.defaultBlockState();
	}

	/** How far out on Black Frost's bed a place is (1 at its edge). */
	private static double bed(int u, int v) {
		double bu = u / BED_U;
		double bv = (v - (THRONE_V + 1 + GUARD_BEHIND)) / BED_V;
		return Math.sqrt(bu * bu + bv * bv);
	}

	/**
	 * The throne room, a hall made for Black Frost: two pairs of pillars of ice by the way in, the throne on its dais near
	 * the front, Black Frost's bed of blue ice behind it under lanterns hung high (above its head), lamps along the walls
	 * (clear of its wings).
	 */
	private BlockState throneRoom(int u, int v, int y, int dy, int au) {
		if (dy == ROOM_H) {
			return Blocks.DEEPSLATE_TILES.defaultBlockState();
		}
		if (dy > ROOM_H) {
			return roof(u, v, y, dy, au);
		}
		// the bed's low rim (open toward her throne)
		double e = bed(u, v);
		if (dy == 0 && e > 1.0 && e <= 1.07 && v > THRONE_V + 4) {
			return IafBlockRegistry.DREAD_STONE_BRICKS_CHISELED.defaultBlockState();
		}
		// two pairs of great pillars of ice by the way in
		for (int pv : new int[]{ARCH_V + 5, THRONE_V - 2}) {
			if (Math.abs(au - 16) <= 1 && Math.abs(v - pv) <= 1) {
				if (au == 16 && v == pv) {
					return Blocks.BLUE_ICE.defaultBlockState();
				}
				if ((au == 16 || v == pv) && dy % 6 == 3) {
					return Blocks.SEA_LANTERN.defaultBlockState();
				}
				return dy % 6 == 0 ? Blocks.CHISELED_DEEPSLATE.defaultBlockState() : Blocks.PACKED_ICE.defaultBlockState();
			}
		}
		// soul lanterns hanging from on high, over Black Frost's bed (above its head) and the way in
		int bv = v - (THRONE_V + 1 + GUARD_BEHIND);
		if ((au == 10 && bv == 0 || u == 0 && (bv == 10 || v == ARCH_V + 8)) && dy >= 22) {
			return dy == 22 ? Blocks.SOUL_LANTERN.defaultBlockState().setValue(LanternBlock.HANGING, true) : DreadPieces.chain();
		}
		// cold lamps hanging at its height beside it (clear of its wings), so it is seen
		if (au == 24 && Math.abs(bv) <= 12 && Math.floorMod(bv, 6) == 0 && dy >= 14) {
			return dy == 14 ? Blocks.PEARLESCENT_FROGLIGHT.defaultBlockState() : DreadPieces.chain();
		}
		// lamps of cold light hanging low: by the throne, and along the walls (clear of its wings)
		boolean byThrone = au == 8 && v == THRONE_V - 1;
		boolean bySide = au == KW - 4 && Math.floorMod(v - ARCH_V, 8) == 4 && v < KV1 - 4;
		if ((byThrone || bySide) && dy >= 9) {
			return dy == 9 ? Blocks.PEARLESCENT_FROGLIGHT.defaultBlockState() : DreadPieces.chain();
		}
		BlockState throne = throne(u, v, dy);
		return throne != null ? throne : Blocks.AIR.defaultBlockState();
	}

	private @Nullable BlockState throne(int u, int v, int dy) {
		int au = Math.abs(u);
		int dv = v - THRONE_V;
		if (au > 6 || dv < -4 || dv > 4) {
			return null;
		}
		int step = dv <= -3 ? 1 : dv <= -2 ? 2 : 3;
		if (au > 5 && dv < -1) {
			return null;
		}
		if (dy < step) {
			// cold lights set in the dais' top beside the throne
			if (dy == step - 1 && au == 4 && (dv == -1 || dv == 1)) {
				return Blocks.SEA_LANTERN.defaultBlockState();
			}
			return IafBlockRegistry.DREAD_STONE_BRICKS.defaultBlockState();
		}
		if (dy == step && dv >= -1 && au <= 4) {
			if (u == 0 && dv == 1) {
				return IafBlockRegistry.DREAD_STONE_BRICKS_STAIRS.defaultBlockState().setValue(StairBlock.FACING, forwardDir())
					.setValue(StairBlock.HALF, Half.BOTTOM);
			}
			if (au == 1 && dv == 1) {
				return IafBlockRegistry.DREAD_STONE_BRICKS_CHISELED.defaultBlockState();
			}
			if (au == 3 && dv == 0) {
				return Blocks.SOUL_CAMPFIRE.defaultBlockState().setValue(CampfireBlock.LIT, true);
			}
		}
		// its high back, and a crown of ice behind it (low enough for Black Frost's head to be over it)
		if (dv == 2 && au <= 1 && dy > step && dy <= step + 5) {
			return au == 0 ? Blocks.BLUE_ICE.defaultBlockState() : IafBlockRegistry.DREAD_STONE_BRICKS.defaultBlockState();
		}
		if (dv == 3 && au <= 5 && dy > step && dy <= step + (5 - au)) {
			return au % 2 == 0 ? Blocks.PACKED_ICE.defaultBlockState() : Blocks.BLUE_ICE.defaultBlockState();
		}
		return null;
	}

	/** Over the keep: the hall's flat roof; over the throne room a tall roof of dark tiles with a crown of ice. */
	private BlockState roof(int u, int v, int y, int dy, int au) {
		if (v > ARCH_V) {
			int k = dy - ROOM_H - 2;
			if (k >= 0) {
				double hw = KW - 1.5 - k * 1.7;
				double hv = (KV1 - ARCH_V) / 2.0 - 1 - k * 1.05;
				int dv = Math.abs(v - ROOF_V);
				if (hw > 1.5 && hv > 1.5 && au <= hw && dv <= hv) {
					return au > hw - 2 || dv > hv - 2 ? Blocks.DEEPSLATE_TILES.defaultBlockState() : Blocks.AIR.defaultBlockState();
				}
				if (u == 0 && v == ROOF_V && dy <= TOP) {
					return Blocks.BLUE_ICE.defaultBlockState();
				}
			}
		}
		return dy <= TOP ? Blocks.AIR.defaultBlockState() : null;
	}

	private Direction forwardDir() {
		return switch (facing & 3) {
			case 0 -> Direction.SOUTH;
			case 1 -> Direction.WEST;
			case 2 -> Direction.NORTH;
			default -> Direction.EAST;
		};
	}

	/**
	 * Trees of the land around that grew over its courtyard (from chunks made before the ones of its walls): all of
	 * them inside its walls go, in the chunks this one may touch.
	 */
	private void clearCourtyard(WorldGenLevel level, BoundingBox chunkBox) {
		int x0 = chunkBox.minX() - 16;
		int x1 = chunkBox.maxX() + 16;
		int z0 = chunkBox.minZ() - 16;
		int z1 = chunkBox.maxZ() + 16;
		BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
		for (int x = x0; x <= x1; x++) {
			for (int z = z0; z <= z1; z++) {
				int[] uv = frame(x, z);
				int au = Math.abs(uv[0]);
				int v = uv[1];
				if (au > W - WALL || v < WALL || v > L - WALL || au <= KW && v >= KV0) {
					continue;
				}
				for (int y = gateY; y <= gateY + 22; y++) {
					if (plant(level.getBlockState(pos.set(x, y, z)))) {
						level.setBlock(pos, Blocks.AIR.defaultBlockState(), 2);
					}
				}
			}
		}
	}

	@Override
	protected void decorate(WorldGenLevel level, BoundingBox chunkBox, RandomSource random) {
		clearCourtyard(level, chunkBox);
		int g = gateY;
		// the chest behind the throne
		DreadPieces.chest(level, chunkBox, random, world(-3, THRONE_V + 2, g + 3), forwardDir().getOpposite(), "iceandfire_fabric:chest/mausoleum_chest");
		// Cryona asleep on her throne (her sign: she herself waits in her realm), Black Frost sitting behind it
		BlockPos seat = throneSeat();
		BlockPos guard = guard();
		if (chunkBox.isInside(seat)) {
			EntityCryonaAsleep queen = EntityCryonaAsleep.make(level.getLevel(), seat, guard, seat.getX() + 0.5, seat.getY() + 0.5, seat.getZ() + 0.5,
				forwardDir().getOpposite().toYRot(), EntitySpawnReason.STRUCTURE);
			if (queen != null) {
				level.addFreshEntity(queen);
			}
		}
		if (chunkBox.isInside(guard)) {
			EntityBlackFrostGuard frost = EntityBlackFrostGuard.make(level, random, guard, forwardDir().getOpposite().toYRot());
			if (frost != null) {
				level.addFreshEntity(frost);
			}
		}
		// the dead keep the courtyard, knights the keep's door and its hall, a lich before the arch
		int[][] dead = {{-14, 14}, {14, 22}, {-20, 34}, {22, 40}, {-34, 70}, {34, 96}};
		for (int i = 0; i < dead.length; i++) {
			spawn(level, chunkBox, i % 2 == 0 ? IafEntityRegistry.DREAD_THRALL : IafEntityRegistry.DREAD_GHOUL, world(dead[i][0], dead[i][1], g));
		}
		spawn(level, chunkBox, IafEntityRegistry.DREAD_KNIGHT, world(-6, KV0 - 3, g));
		spawn(level, chunkBox, IafEntityRegistry.DREAD_KNIGHT, world(6, KV0 - 3, g));
		spawn(level, chunkBox, IafEntityRegistry.DREAD_KNIGHT, world(-8, 72, g));
		spawn(level, chunkBox, IafEntityRegistry.DREAD_KNIGHT, world(8, 80, g));
		spawn(level, chunkBox, IafEntityRegistry.DREAD_LICH, world(0, ARCH_V - 4, g));
	}

	private void spawn(WorldGenLevel level, BoundingBox chunkBox, EntityType<? extends EntityDreadMob> type, BlockPos pos) {
		if (!chunkBox.isInside(pos)) {
			return;
		}
		EntityDreadMob mob = type.create(level.getLevel(), EntitySpawnReason.STRUCTURE);
		if (mob != null) {
			mob.snapTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, level.getRandom().nextFloat() * 360, 0);
			mob.finalizeSpawn(level, level.getCurrentDifficultyAt(pos), EntitySpawnReason.STRUCTURE, null);
			mob.setPersistenceRequired();
			level.addFreshEntity(mob);
		}
	}
}

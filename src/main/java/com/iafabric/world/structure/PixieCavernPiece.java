package com.iafabric.world.structure;

import com.iafabric.block.BlockPixieHouse;
import com.iafabric.block.IafBlockRegistry;
import com.iafabric.entity.EntityPixie;
import com.iafabric.entity.IafEntityRegistry;
import com.iafabric.entity.tile.TileEntityPixieHouse;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CaveVines;
import net.minecraft.world.level.block.PinkPetalsBlock;
import net.minecraft.world.level.block.LanternBlock;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.MultifaceBlock;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;
import org.jetbrains.annotations.Nullable;

/**
 * A pixie cavern: a hidden village of pixies in a great hollow under the land. A wide winding tunnel (lanterns, a
 * little path) climbs from the land down to it; in it, pixie houses stand around a plaza with the pixie altar under a
 * hanging lantern, joined by small paths with lamp posts, a stream with bridges, big mushrooms (or crystal spires, or
 * azalea trees: three kinds of cavern), light from the roof, flowers and pixies about. Its rock is moss and cobble
 * all around, so no spring of lava (or water) opens into it.
 * Its gate is the middle of the plaza at the floor; the tunnel goes along v and comes out at {@link #end}.
 */
public class PixieCavernPiece extends BuiltPiece {
	static final int R = 24;
	static final int H = 18;
	static final int TUNNEL_START = 17;
	private static final int HOUSES = 9;
	private static final int RING = 14;
	private static final int GIANTS = 4;
	/** Where the tunnel's roof meets the land and where its floor does (along v). */
	private final int mouth;
	private final int end;
	private final int[][] houses = new int[HOUSES][];
	private final int[][] giants = new int[GIANTS][];
	/** The land's height at the column being made. */
	private int surface;
	/** Whether the column being made is near the cavern or its tunnel. */
	private boolean near;

	public PixieCavernPiece(int x, int y, int z, int facing, int kind, long seed, int mouth, int end, int top) {
		super(IafStructures.CAVERN, x, y, z, facing, kind, 0, seed, -R - 5, R + 5, -R - 5, end + 3, y - 5, Math.max(y + H + 2, top));
		this.mouth = mouth;
		this.end = end;
		layout();
	}

	public PixieCavernPiece(CompoundTag tag) {
		super(IafStructures.CAVERN, tag);
		this.mouth = tag.iceandfire_fabric$getIntOr("Mouth", 60);
		this.end = tag.iceandfire_fabric$getIntOr("End", 70);
		layout();
	}

	@Override
	protected void addAdditionalSaveData(StructurePieceSerializationContext context, CompoundTag tag) {
		super.addAdditionalSaveData(context, tag);
		tag.putInt("Mouth", mouth);
		tag.putInt("End", end);
	}

	/** The houses on a ring around the plaza (a gap where the tunnel comes in) and the big growths between them. */
	private void layout() {
		double turn = noise(0, 0, 0, 50) * Math.PI * 2;
		for (int i = 0; i < HOUSES; i++) {
			double angle = turn + Math.PI * 2 * i / HOUSES;
			double r = RING + (noise(i, 0, 0, 51) - 0.5) * 2;
			int u = (int) Math.round(Math.sin(angle) * r);
			int v = (int) Math.round(Math.cos(angle) * r);
			if (v > 8 && Math.abs(u) < 7) {
				// the tunnel comes in here: the house steps aside
				u += u < 0 ? -6 : 6;
			}
			houses[i] = new int[]{u, v};
		}
		for (int i = 0; i < GIANTS; i++) {
			double angle = turn + Math.PI * 2 * (i + 0.5) / GIANTS + 0.4;
			int u = (int) Math.round(Math.sin(angle) * 18.5);
			int v = (int) Math.round(Math.cos(angle) * 18.5);
			if (v > 10 && Math.abs(u) < 9) {
				u += u < 0 ? -8 : 8;
			}
			giants[i] = new int[]{u, v, 6 + (int) (noise(i, 0, 0, 52) * 3)};
		}
	}

	/** Where along v its tunnel comes out on the land. */
	public int end() {
		return end;
	}

	// ---- the shape -------------------------------------------------------------------------------------------

	/** The tunnel's middle across (it winds) at a place along it. */
	/** Its way in: the foot of the arch where the tunnel comes out on the land (where one stands). */
	public BlockPos wayIn() {
		int at = end - 1;
		return world((int) Math.round(tunnelMiddle(at)), at, tunnelFloor(gateY, at) + 1);
	}

	static double tunnelMiddle(int v) {
		double t = v - TUNNEL_START;
		return t <= 0 ? 0 : 5 * Math.sin(t / 13.0) * Math.min(1, t / 10.0);
	}

	/** The tunnel's floor at a place along it: one block up for every two ahead. */
	static int tunnelFloor(int floor, int v) {
		return floor + Math.max(0, v - TUNNEL_START) / 2;
	}

	private double radius(int u, int v) {
		return R - 1.5 + smooth(u, v, 9, 1) * 3.5;
	}

	private boolean nearStream(int u, int v) {
		return Math.abs(u - stream(v)) <= 3.5 && v > -R && v < R - 6;
	}

	/** The stream's middle across: it runs along one side of the cavern. */
	private double stream(int v) {
		double side = noise(0, 0, 0, 53) < 0.5 ? -1 : 1;
		return side * (9.5 + 2.5 * Math.sin(v / 5.0));
	}

	private boolean isStream(int u, int v) {
		return Math.abs(u - stream(v)) <= 1.0 && v > -R + 4 && v < R - 8 && u * u + v * v < (R - 5) * (R - 5);
	}

	private int floorAt(int u, int v) {
		double d2 = u * u + v * v;
		if (d2 <= 8 * 8 || nearStream(u, v)) {
			return gateY;
		}
		for (int[] h : houses) {
			if (Math.abs(u - h[0]) <= 2 && Math.abs(v - h[1]) <= 2) {
				return gateY;
			}
		}
		return gateY + (int) Math.round((smooth(u, v, 7, 2) - 0.5) * 2.6);
	}

	private boolean inCavern(int u, int v, int y) {
		if (y < gateY || y > gateY + H) {
			return false;
		}
		int d2 = u * u + v * v;
		if (d2 > (R + 3) * (R + 3)) {
			return false;
		}
		double rb = radius(u, v);
		double dy = (y - (gateY - 2)) / (double) H;
		return d2 / (rb * rb) + dy * dy < 1 && y > floorAt(u, v);
	}

	private boolean inTunnel(int u, int v, int y) {
		if (v < TUNNEL_START - 6 || v > end) {
			return false;
		}
		double du = u - tunnelMiddle(v);
		double w = 4.0 + smooth(v, 0, 6, 3) * 1.6;
		if (Math.abs(du) > w) {
			return false;
		}
		int floor = tunnelFloor(gateY, v);
		if (y <= floor) {
			return false;
		}
		double dy = (y - floor + 1.5) / 8.5;
		return du * du / (w * w) + dy * dy < 1;
	}

	private boolean open(int u, int v, int y) {
		return inCavern(u, v, y) || inTunnel(u, v, y);
	}

	/** Something open within two blocks: the rock there is the cavern's own (moss and cobble, no springs). */
	private boolean nearOpen(int u, int v, int y) {
		for (int d = 1; d <= 2; d++) {
			if (open(u + d, v, y) || open(u - d, v, y) || open(u, v + d, y) || open(u, v - d, y) || open(u, v, y + d) || open(u, v, y - d)) {
				return true;
			}
		}
		return open(u + 1, v + 1, y) || open(u - 1, v - 1, y) || open(u + 1, v - 1, y) || open(u - 1, v + 1, y);
	}

	@Override
	protected void column(WorldGenLevel level, int u, int v, int x, int z) {
		surface = level.getHeight(Heightmap.Types.WORLD_SURFACE_WG, x, z) - 1;
		boolean cavern = u * u + v * v <= (R + 6) * (R + 6);
		boolean tunnel = v >= TUNNEL_START - 8 && v <= end + 2 && Math.abs(u - tunnelMiddle(Math.max(v, TUNNEL_START))) <= 9;
		near = cavern || tunnel;
	}

	// ---- the blocks ------------------------------------------------------------------------------------------

	@Override
	protected @Nullable BlockState at(WorldGenLevel level, int u, int v, int y) {
		if (!near || y < gateY - 4) {
			return null;
		}
		BlockState arch = arch(u, v, y);
		if (arch != null) {
			return arch;
		}
		if (open(u, v, y)) {
			return inside(u, v, y);
		}
		if (y > surface) {
			// above the land at the tunnel's mouth: nothing is built in the air
			return null;
		}
		if (open(u, v, y + 1)) {
			return floor(u, v, y);
		}
		if (open(u, v, y - 1)) {
			return roof(u, v, y);
		}
		if (nearOpen(u, v, y)) {
			return open(u, v, y + 2) || open(u, v, y + 3) ? Blocks.ROOTED_DIRT.defaultBlockState() : rock(u, v, y);
		}
		return null;
	}

	/** Where the tunnel comes out: an arch of birch and flowering azalea with a lantern, to be seen from afar. */
	private @Nullable BlockState arch(int u, int v, int y) {
		int at = end - 1;
		if (v != at) {
			return null;
		}
		int mid = (int) Math.round(tunnelMiddle(at));
		int floor = tunnelFloor(gateY, at);
		int du = u - mid;
		int dy = y - floor;
		if (Math.abs(du) == 5 && dy >= 1 && dy <= 5) {
			return Blocks.BIRCH_LOG.defaultBlockState();
		}
		if (Math.abs(du) <= 6 && dy == 6) {
			return Blocks.FLOWERING_AZALEA_LEAVES.defaultBlockState().setValue(LeavesBlock.PERSISTENT, true);
		}
		if (Math.abs(du) <= 3 && dy == 7) {
			return Blocks.FLOWERING_AZALEA_LEAVES.defaultBlockState().setValue(LeavesBlock.PERSISTENT, true);
		}
		if (du == 0 && dy == 5) {
			return Blocks.LANTERN.defaultBlockState().setValue(LanternBlock.HANGING, true);
		}
		return null;
	}

	private BlockState rock(int u, int v, int y) {
		float n = noise(u, v, y, 60);
		if (kind == 1) {
			return n < 0.5F ? Blocks.SMOOTH_BASALT.defaultBlockState() : n < 0.8F ? Blocks.MOSSY_COBBLESTONE.defaultBlockState() : Blocks.AMETHYST_BLOCK.defaultBlockState();
		}
		return n < 0.45F ? Blocks.MOSSY_COBBLESTONE.defaultBlockState() : n < 0.8F ? Blocks.MOSS_BLOCK.defaultBlockState() : Blocks.COBBLESTONE.defaultBlockState();
	}

	/** The roof: moss with lights in it (shroomlight, froglights or glow berries by the kind of cavern). */
	private BlockState roof(int u, int v, int y) {
		float n = noise(u, v, y, 61);
		if (n < 0.09F) {
			return switch (kind) {
				case 1 -> Blocks.PEARLESCENT_FROGLIGHT.defaultBlockState();
				case 2 -> Blocks.VERDANT_FROGLIGHT.defaultBlockState();
				default -> Blocks.SHROOMLIGHT.defaultBlockState();
			};
		}
		if (kind == 1 && n < 0.3F) {
			return Blocks.AMETHYST_BLOCK.defaultBlockState();
		}
		return n < 0.75F ? Blocks.MOSS_BLOCK.defaultBlockState() : Blocks.MOSSY_COBBLESTONE.defaultBlockState();
	}

	/** The floor's top: the plaza, paths, the stream, house yards, moss. */
	private BlockState floor(int u, int v, int y) {
		int d2 = u * u + v * v;
		if (inTunnel(u, v, y + 1) && !inCavern(u, v, y + 1)) {
			double du = Math.abs(u - tunnelMiddle(v));
			if (du < 0.8) {
				return Blocks.DIRT_PATH.defaultBlockState();
			}
			return noise(u, v, y, 62) < 0.7F ? Blocks.MOSS_BLOCK.defaultBlockState() : Blocks.MOSSY_COBBLESTONE.defaultBlockState();
		}
		if (d2 <= 7 * 7) {
			// the plaza: a ring of calcite, mossy bricks inside, amethyst at the four ways
			double d = Math.sqrt(d2);
			if (d > 6 && d <= 7) {
				return Blocks.CALCITE.defaultBlockState();
			}
			if ((u == 0 || v == 0) && Math.abs(u + v) == 5) {
				return Blocks.AMETHYST_BLOCK.defaultBlockState();
			}
			return (u + v & 1) == 0 ? Blocks.MOSSY_STONE_BRICKS.defaultBlockState() : Blocks.STONE_BRICKS.defaultBlockState();
		}
		if (isStream(u, v)) {
			return bridge(u, v) ? Blocks.BIRCH_PLANKS.defaultBlockState() : Blocks.WATER.defaultBlockState();
		}
		if (isPath(u, v)) {
			return noise(u, v, y, 63) < 0.25F ? Blocks.MOSSY_STONE_BRICKS.defaultBlockState() : Blocks.DIRT_PATH.defaultBlockState();
		}
		for (int[] h : houses) {
			if (Math.abs(u - h[0]) <= 1 && Math.abs(v - h[1]) <= 1) {
				return kind == 0 ? Blocks.MYCELIUM.defaultBlockState() : Blocks.PODZOL.defaultBlockState();
			}
		}
		float n = noise(u, v, y, 64);
		if (kind == 0 && n < 0.18F) {
			return Blocks.MYCELIUM.defaultBlockState();
		}
		return n < 0.93F ? Blocks.MOSS_BLOCK.defaultBlockState() : Blocks.CLAY.defaultBlockState();
	}

	/** The little paths: from each house to the plaza, and a ring joining the houses. */
	private boolean isPath(int u, int v) {
		double d = Math.sqrt(u * u + v * v);
		if (d < 6.5) {
			return false;
		}
		if (Math.abs(d - RING) < 0.55) {
			for (int[] h : houses) {
				if (Math.abs(u - h[0]) <= 1 && Math.abs(v - h[1]) <= 1) {
					return false;
				}
			}
			return true;
		}
		for (int[] h : houses) {
			double len = Math.sqrt(h[0] * h[0] + h[1] * h[1]);
			// the distance from the line plaza-house, between them
			double along = (u * h[0] + v * h[1]) / len;
			if (along > 6 && along < len - 1.5) {
				double across = Math.abs(u * h[1] - v * h[0]) / len;
				if (across < 0.55) {
					return true;
				}
			}
		}
		// the way from the tunnel to the plaza
		return v > 6 && Math.abs(u - tunnelMiddle(v)) < 0.6;
	}

	private boolean bridge(int u, int v) {
		for (int dv = -1; dv <= 1; dv++) {
			for (int du = -1; du <= 1; du++) {
				if (isPath(u + du, v + dv) && !isStream(u + du, v + dv)) {
					return Math.abs(du) + Math.abs(dv) <= 1 && pathCrosses(u, v);
				}
			}
		}
		return false;
	}

	/** Whether a path goes through this water (it is on a path's line). */
	private boolean pathCrosses(int u, int v) {
		double d = Math.sqrt(u * u + v * v);
		if (Math.abs(d - RING) < 1.2) {
			return true;
		}
		for (int[] h : houses) {
			double len = Math.sqrt(h[0] * h[0] + h[1] * h[1]);
			double along = (u * h[0] + v * h[1]) / len;
			if (along > 6 && along < len - 1.5 && Math.abs(u * h[1] - v * h[0]) / len < 1.2) {
				return true;
			}
		}
		return false;
	}

	/** Inside: air with what grows on the floor, what hangs from the roof, the plaza's altar and lantern, the big growths. */
	private BlockState inside(int u, int v, int y) {
		BlockState giant = giant(u, v, y);
		if (giant != null) {
			return giant;
		}
		boolean ground = !open(u, v, y - 1);
		boolean top = !open(u, v, y + 1);
		boolean tunnel = inTunnel(u, v, y) && !inCavern(u, v, y);
		int d2 = u * u + v * v;
		// the plaza: the altar on a dais, a lantern hanging above it on a long chain
		if (!tunnel && d2 <= 2) {
			if (y == gateY + 1 && Math.abs(u) <= 1 && Math.abs(v) <= 1) {
				return Blocks.MOSSY_STONE_BRICKS.defaultBlockState();
			}
			if (u == 0 && v == 0) {
				if (y == gateY + 2) {
					return IafBlockRegistry.PIXIE_ALTAR.defaultBlockState();
				}
				if (y == gateY + 8) {
					return Blocks.LANTERN.defaultBlockState().setValue(LanternBlock.HANGING, true);
				}
				if (y > gateY + 8) {
					return Blocks.CHAIN.defaultBlockState().setValue(RotatedPillarBlock.AXIS, Direction.Axis.Y);
				}
			}
		}
		if (ground) {
			return ground(u, v, y, tunnel);
		}
		if (lampPost(u, v, tunnel) && y == floorBelow(u, v, y) + 2) {
			return Blocks.LANTERN.defaultBlockState();
		}
		// what hangs from the roof only where the roof is built: toward its mouth the tunnel is open to the sky (the
		// roof would be above the land), and vines, lichen and lanterns hung there from nothing
		boolean roofed = y + 1 <= surface;
		boolean roofedTwoUp = y + 2 <= surface;
		// in the tunnel, every twelve blocks a lantern hangs on a chain from the roof
		boolean hangingLantern = tunnel && v % 12 == 0 && u == (int) Math.round(tunnelMiddle(v));
		if (hangingLantern && top) {
			return roofed ? Blocks.CHAIN.defaultBlockState().setValue(RotatedPillarBlock.AXIS, Direction.Axis.Y) : Blocks.CAVE_AIR.defaultBlockState();
		}
		if (hangingLantern && !open(u, v, y + 2)) {
			return roofedTwoUp ? Blocks.LANTERN.defaultBlockState().setValue(LanternBlock.HANGING, true) : Blocks.CAVE_AIR.defaultBlockState();
		}
		if (top) {
			return roofed ? hanging(u, v, y) : Blocks.CAVE_AIR.defaultBlockState();
		}
		if (roofedTwoUp && !open(u, v, y + 2) && noise(u, v, y + 1, 65) < 0.06F) {
			// the tip of a vine two long
			return Blocks.CAVE_VINES.defaultBlockState().setValue(CaveVines.BERRIES, noise(u, v, y, 66) < 0.6F);
		}
		return Blocks.CAVE_AIR.defaultBlockState();
	}

	private int floorBelow(int u, int v, int y) {
		int f = y;
		while (f > gateY - 3 && open(u, v, f)) {
			f--;
		}
		return f;
	}

	/** Little lamp posts: along the paths in the cavern, and every twelve blocks in the tunnel. */
	private boolean lampPost(int u, int v, boolean tunnel) {
		if (tunnel) {
			int side = (v / 12 & 1) == 0 ? 1 : -1;
			return v % 12 == 6 && u == (int) Math.round(tunnelMiddle(v) + side * 3);
		}
		for (int[] h : houses) {
			double len = Math.sqrt(h[0] * h[0] + h[1] * h[1]);
			int pu = (int) Math.round(h[0] * 9.5 / len + h[1] / len);
			int pv = (int) Math.round(h[1] * 9.5 / len - h[0] / len);
			if (u == pu && v == pv) {
				return true;
			}
		}
		return false;
	}

	private BlockState ground(int u, int v, int y, boolean tunnel) {
		if (lampPost(u, v, tunnel)) {
			return Blocks.BIRCH_FENCE.defaultBlockState();
		}
		int below = y - 1;
		if (!tunnel && isStream(u, v) && !bridge(u, v)) {
			return noise(u, v, y, 67) < 0.2F ? Blocks.LILY_PAD.defaultBlockState() : Blocks.CAVE_AIR.defaultBlockState();
		}
		if (!tunnel && (u * u + v * v <= 7 * 7 || isPath(u, v))) {
			return Blocks.CAVE_AIR.defaultBlockState();
		}
		for (int[] h : houses) {
			if (Math.abs(u - h[0]) <= 1 && Math.abs(v - h[1]) <= 1) {
				return Blocks.CAVE_AIR.defaultBlockState();
			}
			if (Math.abs(u - h[0]) <= 2 && Math.abs(v - h[1]) <= 2 && noise(u, v, y, 68) < 0.5F) {
				return flower(u, v, y);
			}
		}
		if (tunnel && Math.abs(u - tunnelMiddle(v)) < 1.5) {
			return Blocks.CAVE_AIR.defaultBlockState();
		}
		float n = noise(u, v, below, 69);
		if (n < 0.2F) {
			return Blocks.MOSS_CARPET.defaultBlockState();
		}
		if (n < 0.26F) {
			return kind == 1 ? Blocks.AMETHYST_CLUSTER.defaultBlockState() : Blocks.FLOWERING_AZALEA.defaultBlockState();
		}
		if (n < 0.30F) {
			return Blocks.FLOWERING_AZALEA.defaultBlockState();
		}
		if (n < 0.34F) {
			return Blocks.AZALEA.defaultBlockState();
		}
		if (n < 0.42F) {
			return flower(u, v, y);
		}
		if (n < 0.50F) {
			return kind == 0 ? (n < 0.46F ? Blocks.RED_MUSHROOM : Blocks.BROWN_MUSHROOM).defaultBlockState() : Blocks.FERN.defaultBlockState();
		}
		return Blocks.CAVE_AIR.defaultBlockState();
	}

	private BlockState flower(int u, int v, int y) {
		float n = noise(u, v, y, 70);
		if (n < 0.4F) {
			return Blocks.PINK_PETALS.defaultBlockState().setValue(PinkPetalsBlock.AMOUNT, 1 + (int) (noise(u, v, y, 71) * 4))
				.setValue(PinkPetalsBlock.FACING, Direction.from2DDataValue((int) (noise(u, v, y, 72) * 4)));
		}
		if (n < 0.7F) {
			return Blocks.PINK_PETALS.defaultBlockState().setValue(PinkPetalsBlock.AMOUNT, 1 + (int) (noise(u, v, y, 71) * 4))
				.setValue(PinkPetalsBlock.FACING, Direction.from2DDataValue((int) (noise(u, v, y, 72) * 4)));
		}
		Block[] flowers = {Blocks.ALLIUM, Blocks.AZURE_BLUET, Blocks.LILY_OF_THE_VALLEY, Blocks.BLUE_ORCHID, Blocks.OXEYE_DAISY};
		return flowers[(int) (noise(u, v, y, 73) * flowers.length)].defaultBlockState();
	}

	/** Under the roof: glow lichen, spore blossoms, glow berries. */
	private BlockState hanging(int u, int v, int y) {
		if (noise(u, v, y, 65) < 0.06F) {
			// a vine two long (its tip is under it)
			return Blocks.CAVE_VINES_PLANT.defaultBlockState().setValue(CaveVines.BERRIES, false);
		}
		float n = noise(u, v, y, 74);
		if (n < 0.012F) {
			return Blocks.SPORE_BLOSSOM.defaultBlockState();
		}
		if (n < 0.1F) {
			return Blocks.GLOW_LICHEN.defaultBlockState().setValue(MultifaceBlock.getFaceProperty(Direction.UP), true);
		}
		if (n < 0.14F) {
			return Blocks.CAVE_VINES.defaultBlockState().setValue(CaveVines.BERRIES, noise(u, v, y, 66) < 0.6F);
		}
		if (kind == 2 && n < 0.18F) {
			return Blocks.HANGING_ROOTS.defaultBlockState();
		}
		return Blocks.CAVE_AIR.defaultBlockState();
	}

	/** The big growths: giant mushrooms, crystal spires or azalea trees. */
	private @Nullable BlockState giant(int u, int v, int y) {
		for (int[] g : giants) {
			int du = u - g[0];
			int dv = v - g[1];
			if (Math.abs(du) > 4 || Math.abs(dv) > 4) {
				continue;
			}
			int base = floorAt(g[0], g[1]);
			int h = g[2];
			int dy = y - base;
			if (dy <= 0) {
				continue;
			}
			int r2 = du * du + dv * dv;
			switch (kind) {
				case 1 -> {
					// a spire of amethyst, wide at the foot
					int width = dy <= 1 ? 2 : dy <= h / 2 ? 1 : 0;
					if (Math.abs(du) <= width && Math.abs(dv) <= width && dy <= h && r2 <= width * width + 1) {
						return dy == h ? Blocks.PEARLESCENT_FROGLIGHT.defaultBlockState() : Blocks.AMETHYST_BLOCK.defaultBlockState();
					}
					if (du == 0 && dv == 0 && dy == h + 1) {
						return Blocks.AMETHYST_CLUSTER.defaultBlockState();
					}
				}
				case 2 -> {
					// an azalea tree
					if (du == 0 && dv == 0 && dy <= h - 1) {
						return Blocks.OAK_LOG.defaultBlockState();
					}
					int cy = h;
					int ddy = dy - cy;
					if (r2 + ddy * ddy * 2 <= 10 && ddy >= -2 && ddy <= 1) {
						BlockState leaves = (noise(u, v, y, 75) < 0.4F ? Blocks.FLOWERING_AZALEA_LEAVES : Blocks.AZALEA_LEAVES).defaultBlockState();
						return leaves.setValue(LeavesBlock.PERSISTENT, true);
					}
					if (ddy == -3 && r2 <= 6 && r2 > 0 && noise(u, v, y, 76) < 0.5F) {
						return Blocks.HANGING_ROOTS.defaultBlockState();
					}
				}
				default -> {
					// a giant mushroom: red with a round cap, or brown with a flat one
					boolean red = (g[0] + g[1] & 1) == 0;
					if (du == 0 && dv == 0 && dy < h) {
						return Blocks.MUSHROOM_STEM.defaultBlockState();
					}
					if (red) {
						if (dy >= h - 1 && dy <= h + 1 && r2 <= (dy == h + 1 ? 2 : 6) && (dy == h + 1 || r2 >= (dy == h ? 0 : 4))) {
							return Blocks.RED_MUSHROOM_BLOCK.defaultBlockState();
						}
						if (dy == h - 1 && du == 0 && dv == 0) {
							return Blocks.SHROOMLIGHT.defaultBlockState();
						}
					} else {
						if (dy == h && r2 <= 12) {
							return Blocks.BROWN_MUSHROOM_BLOCK.defaultBlockState();
						}
						if (dy == h - 1 && r2 == 2) {
							return Blocks.SHROOMLIGHT.defaultBlockState();
						}
					}
				}
			}
		}
		return null;
	}

	@Override
	protected void decorate(WorldGenLevel level, BoundingBox chunkBox, RandomSource random) {
		Block[] blocks = switch (kind) {
			case 1 -> new Block[]{IafBlockRegistry.PIXIE_HOUSE_BIRCH, IafBlockRegistry.PIXIE_HOUSE_OAK};
			case 2 -> new Block[]{IafBlockRegistry.PIXIE_HOUSE_OAK, IafBlockRegistry.PIXIE_HOUSE_SPRUCE, IafBlockRegistry.PIXIE_HOUSE_DARK_OAK};
			default -> new Block[]{IafBlockRegistry.PIXIE_HOUSE_MUSHROOM_RED, IafBlockRegistry.PIXIE_HOUSE_MUSHROOM_BROWN};
		};
		for (int i = 0; i < HOUSES; i++) {
			int[] h = houses[i];
			BlockPos pos = world(h[0], h[1], gateY + 1);
			if (!chunkBox.isInside(pos)) {
				continue;
			}
			Block block = blocks[i % blocks.length];
			BlockPos middle = world(0, 0, pos.getY());
			Direction toPlaza = Direction.getNearest(middle.getX() - pos.getX(), 0, middle.getZ() - pos.getZ());
			level.setBlock(pos, block.defaultBlockState().setValue(BlockPixieHouse.FACING, toPlaza), 2);
			if (level.getBlockEntity(pos) instanceof TileEntityPixieHouse house) {
				house.houseType = TileEntityPixieHouse.getHouseTypeFromBlock(block);
				house.hasPixie = true;
				house.pixieType = Mth.floor(noise(h[0], h[1], 0, 77) * 5) % 5;
			}
		}
		for (int i = 0; i < 6; i++) {
			double angle = Math.PI * 2 * i / 6;
			BlockPos pos = world((int) Math.round(Math.sin(angle) * 4), (int) Math.round(Math.cos(angle) * 4), gateY + 3 + i % 3);
			if (chunkBox.isInside(pos)) {
				EntityPixie pixie = IafEntityRegistry.PIXIE.create(level.getLevel());
				if (pixie != null) {
					pixie.moveTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, random.nextFloat() * 360, 0);
					pixie.setColor(random.nextInt(5));
					pixie.setPersistenceRequired();
					level.addFreshEntity(pixie);
				}
			}
		}
	}
}

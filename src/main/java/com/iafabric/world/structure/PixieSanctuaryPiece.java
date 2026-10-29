package com.iafabric.world.structure;

import com.iafabric.block.BlockPixieHouse;
import com.iafabric.block.IafBlockRegistry;
import com.iafabric.entity.EntityPixie;
import com.iafabric.entity.IafEntityRegistry;
import com.iafabric.entity.tile.TileEntityPixieHouse;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.PinkPetalsBlock;
import net.minecraft.world.level.block.LanternBlock;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.SlabType;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import org.jetbrains.annotations.Nullable;

/**
 * A pixie sanctuary: a village of pixies in the woods around a giant tree. The tree has great roots, a hollow at its
 * foot where the pixie altar is, a ramp winding up its trunk to a platform with pixie houses, branches with lanterns
 * and a wide crown (cherry, an old oak with azalea, or birch: by the land). Around it, pixie houses with flower
 * gardens on a ring, joined by little paths with lamp posts (a ring around the tree, a ring between the houses, a
 * path to each house, the way in), a pond, flowers and pixies about.
 * Its gate is where the way in starts; the tree stands {@link #CENTER_V} ahead.
 */
public class PixieSanctuaryPiece extends BuiltPiece {
	static final int R = 26;
	static final int CENTER_V = R + 2;
	private static final int BLEND = 9;
	private static final int TRUNK = 22;
	private static final int PLATFORM = 11;
	private static final int HOUSES = 8;
	private static final int HOUSE_RING = 19;
	private static final int[][] PLATFORM_HOUSES = {{60}, {180}, {300}};
	private final int[][] houses = new int[HOUSES][];
	private final double[][] branches = new double[7][];
	private final int[] pond = new int[2];
	/** The land's height at the column being made (without trees). */
	private int ground;

	public PixieSanctuaryPiece(int gateX, int gateY, int gateZ, int facing, int kind, long seed) {
		super(IafStructures.SANCTUARY, gateX, gateY, gateZ, facing, kind, 0, seed, -R - BLEND, R + BLEND, -BLEND + 2, CENTER_V + R + BLEND,
			gateY - 8, gateY + TRUNK + 14);
		layout();
	}

	public PixieSanctuaryPiece(CompoundTag tag) {
		super(IafStructures.SANCTUARY, tag);
		layout();
	}

	private void layout() {
		double turn = noise(0, 0, 0, 30) * Math.PI * 2;
		for (int i = 0; i < HOUSES; i++) {
			double angle = turn + Math.PI * 2 * i / HOUSES;
			// no house on the way in
			if (Math.abs(Mth.wrapDegrees(Math.toDegrees(angle) - 180)) < 25) {
				angle += Math.toRadians(30);
			}
			int r = HOUSE_RING + (int) ((noise(i, 0, 0, 31) - 0.5) * 3);
			houses[i] = new int[]{(int) Math.round(Math.sin(angle) * r), CENTER_V + (int) Math.round(Math.cos(angle) * r)};
		}
		for (int i = 0; i < branches.length; i++) {
			double angle = turn + Math.PI * 2 * i / branches.length + (noise(i, 1, 0, 32) - 0.5) * 0.6;
			double start = 13 + noise(i, 2, 0, 33) * 6;
			double length = 7 + noise(i, 3, 0, 34) * 4;
			branches[i] = new double[]{angle, start, length};
		}
		double pondAngle = turn + Math.PI * 2 * 0.5 / HOUSES;
		pond[0] = (int) Math.round(Math.sin(pondAngle) * 14);
		pond[1] = CENTER_V + (int) Math.round(Math.cos(pondAngle) * 14);
	}

	// ---- the tree's wood and leaves by kind (0 cherry, 1 old oak, 2 birch) ----------------------------------------

	private BlockState wood() {
		return switch (kind) {
			case 0 -> Blocks.CHERRY_WOOD.defaultBlockState();
			case 2 -> Blocks.BIRCH_WOOD.defaultBlockState();
			default -> Blocks.DARK_OAK_WOOD.defaultBlockState();
		};
	}

	private BlockState log(Direction.Axis axis) {
		Block block = switch (kind) {
			case 0 -> Blocks.CHERRY_LOG;
			case 2 -> Blocks.BIRCH_LOG;
			default -> Blocks.DARK_OAK_LOG;
		};
		return block.defaultBlockState().setValue(RotatedPillarBlock.AXIS, axis);
	}

	private BlockState leaves(int u, int v, int y) {
		float n = noise(u, v, y, 35);
		Block block = switch (kind) {
			case 0 -> n < 0.85F ? Blocks.CHERRY_LEAVES : Blocks.FLOWERING_AZALEA_LEAVES;
			case 2 -> n < 0.6F ? Blocks.BIRCH_LEAVES : Blocks.FLOWERING_AZALEA_LEAVES;
			default -> n < 0.45F ? Blocks.OAK_LEAVES : n < 0.8F ? Blocks.AZALEA_LEAVES : Blocks.FLOWERING_AZALEA_LEAVES;
		};
		return block.defaultBlockState().setValue(LeavesBlock.PERSISTENT, true);
	}

	private BlockState planks() {
		return switch (kind) {
			case 0 -> Blocks.CHERRY_PLANKS.defaultBlockState();
			case 2 -> Blocks.BIRCH_PLANKS.defaultBlockState();
			default -> Blocks.SPRUCE_PLANKS.defaultBlockState();
		};
	}

	private Block slab() {
		return switch (kind) {
			case 0 -> Blocks.CHERRY_SLAB;
			case 2 -> Blocks.BIRCH_SLAB;
			default -> Blocks.SPRUCE_SLAB;
		};
	}

	private Block fence() {
		return switch (kind) {
			case 0 -> Blocks.CHERRY_FENCE;
			case 2 -> Blocks.BIRCH_FENCE;
			default -> Blocks.SPRUCE_FENCE;
		};
	}

	// ---- the tree ----------------------------------------------------------------------------------------------

	private static double trunkRadius(int dy) {
		if (dy <= 2) {
			return 3.7 - dy * 0.2;
		}
		return 3.2 - (dy - 2) * (1.5 / (TRUNK - 2));
	}

	/** The trunk's edge here: round with bumps. */
	private double trunkEdge(int du, int dv, int dy) {
		double angle = Math.atan2(du, dv);
		return trunkRadius(dy) + (smooth(angle * 3 + 10, dy, 3, 36) - 0.5) * 0.9;
	}

	private @Nullable BlockState tree(int du, int dv, int y) {
		int dy = y - gateY;
		double r = Math.sqrt(du * du + dv * dv);
		if (dy >= 0 && dy <= TRUNK && r <= trunkEdge(du, dv, dy)) {
			// the hollow at its foot, its door toward the way in, the altar inside
			if (dy < 5 && r <= trunkRadius(dy) - 1.4) {
				if (du == 0 && dv == 0 && dy == 0) {
					return IafBlockRegistry.PIXIE_ALTAR.defaultBlockState();
				}
				if (du == 0 && dv == 0 && dy == 4) {
					return Blocks.LANTERN.defaultBlockState().setValue(LanternBlock.HANGING, true);
				}
				return Blocks.AIR.defaultBlockState();
			}
			if (dy < 4 && dv < 0 && Math.abs(du) <= 1) {
				return Blocks.AIR.defaultBlockState();
			}
			if (dy == 2 && r > trunkRadius(dy) - 2.2 && (du == 2 || du == -2) && dv == 1) {
				return Blocks.SHROOMLIGHT.defaultBlockState();
			}
			return wood();
		}
		// roots: six, rising from the ground by the trunk and going down into it further out (none on the ramp or the way in)
		if (dy >= -2 && dy <= 2 && r > 2.5 && r < 12 && ramp(du, dv, y) == null && !(Math.abs(du) <= 2 && dv < 0)) {
			double angle = Math.atan2(du, dv);
			for (int k = 0; k < 6; k++) {
				double rootAngle = noise(0, 0, 0, 30) * Math.PI * 2 + k * Math.PI / 3 + 0.3;
				double delta = Math.abs(Mth.wrapDegrees(Math.toDegrees(angle - rootAngle)));
				double across = r * Math.sin(Math.toRadians(Math.min(delta, 90)));
				double length = 8 + noise(k, 0, 0, 37) * 3;
				if (delta < 90 && r < length && across < 1.1 - r / 20) {
					double top = 2 - (r - 3) * 0.45;
					if (dy <= top && dy >= top - 1.6) {
						return wood();
					}
				}
			}
		}
		// branches, with leaves at their ends and a lantern under some
		for (int i = 0; i < branches.length; i++) {
			double[] b = branches[i];
			double sx = Math.sin(b[0]);
			double sz = Math.cos(b[0]);
			double along = du * sx + dv * sz;
			if (along < 1 || along > b[2] + trunkRadius((int) b[1])) {
				continue;
			}
			double across = Math.abs(du * sz - dv * sx);
			double by = b[1] + along * 0.45;
			if (across < 1.0 && Math.abs(dy - by) < 0.9) {
				return log(Math.abs(sx) > Math.abs(sz) ? Direction.Axis.X : Direction.Axis.Z);
			}
		}
		for (int i = 0; i < branches.length; i++) {
			double[] b = branches[i];
			double reach = b[2] + trunkRadius((int) b[1]);
			double ex = Math.sin(b[0]) * reach;
			double ez = Math.cos(b[0]) * reach;
			double ey = b[1] + reach * 0.45;
			double dx = du - ex;
			double dz = dv - ez;
			double dyy = dy - ey;
			double d2 = dx * dx + dz * dz + dyy * dyy * 1.8;
			if (d2 < 14 + smooth(du + 40, dv + dy * 3, 2, 38) * 6) {
				return noise(du, dv, y, 39) < 0.03F ? Blocks.PEARLESCENT_FROGLIGHT.defaultBlockState() : leaves(du, dv, y);
			}
			if (du == (int) Math.round(ex) && dv == (int) Math.round(ez) && dyy < -2 && dyy > -6 && i % 2 == 0) {
				return dyy > -5 ? Blocks.CHAIN.defaultBlockState().setValue(RotatedPillarBlock.AXIS, Direction.Axis.Y)
					: Blocks.LANTERN.defaultBlockState().setValue(LanternBlock.HANGING, true);
			}
		}
		// the crown
		double cy = TRUNK + 2;
		double ch = dy - cy;
		double crown = (r * r) / (12.5 * 12.5) + (ch * ch) / (6.5 * 6.5);
		if (crown < 0.75 + smooth(du * 1.3 + 7, dv * 1.3 + dy, 3, 40) * 0.5 && dy > TRUNK - 5) {
			return noise(du, dv, y, 41) < 0.02F ? Blocks.PEARLESCENT_FROGLIGHT.defaultBlockState() : leaves(du, dv, y);
		}
		return null;
	}

	/** How far round the ramp is here (0 by the door, 1 at the platform): it turns once. */
	private static double rampTurn(int du, int dv) {
		double angle = Math.atan2(du, -dv) - Math.toRadians(35);
		return Math.floorMod((int) Math.round(Math.toDegrees(angle) * 10), 3600) / 3600.0;
	}

	/** The ramp around the trunk: half steps of slabs, one turn up to the platform. */
	private @Nullable BlockState ramp(int du, int dv, int y) {
		double r = Math.sqrt(du * du + dv * dv);
		if (r < 3.9 || r > 5.9) {
			return null;
		}
		double h = rampTurn(du, dv) * PLATFORM;
		int floor = gateY + (int) Math.floor(h);
		if (y == floor) {
			return slab().defaultBlockState().setValue(SlabBlock.TYPE, h - Math.floor(h) < 0.5 ? SlabType.BOTTOM : SlabType.TOP);
		}
		if (y > floor && y <= floor + 3) {
			return Blocks.AIR.defaultBlockState();
		}
		return null;
	}

	/** The platform around the trunk, with a railing (open where the ramp comes up). */
	private @Nullable BlockState platform(int du, int dv, int y) {
		double r = Math.sqrt(du * du + dv * dv);
		int dy = y - gateY;
		double edge = 7.6 + smooth(Math.atan2(du, dv) * 4 + 20, 0, 2, 53) * 1.4;
		if (r > edge + 0.4 || r < trunkRadius(PLATFORM) - 0.5) {
			return null;
		}
		// the stairwell: open over the last part of the ramp
		if (r >= 3.9 && r <= 5.9 && rampTurn(du, dv) > 0.68 && dy >= PLATFORM - 2 && dy <= PLATFORM) {
			return null;
		}
		if (dy == PLATFORM) {
			return planks();
		}
		// beams under it, from the trunk out, and a ring of slabs at its edge below
		if (dy == PLATFORM - 1 && (Math.abs(du) <= 0 || Math.abs(dv) <= 0 || Math.abs(Math.abs(du) - Math.abs(dv)) == 0 && r < edge - 1)) {
			return log(Math.abs(du) >= Math.abs(dv) ? Direction.Axis.X : Direction.Axis.Z);
		}
		if (dy == PLATFORM - 1 && r > edge - 0.8) {
			return slab().defaultBlockState().setValue(SlabBlock.TYPE, SlabType.TOP);
		}
		double angle = Math.toDegrees(Math.atan2(du, -dv)) - 35;
		boolean rampTop = Mth.wrapDegrees(angle) > -40 && Mth.wrapDegrees(angle) < 5;
		if (dy == PLATFORM + 1 && r > edge - 0.6 && !rampTop) {
			return fence().defaultBlockState();
		}
		if (dy == PLATFORM + 2 && r > edge - 0.6 && !rampTop && (du == 0 || dv == 0)) {
			return Blocks.LANTERN.defaultBlockState();
		}
		if (dy > PLATFORM && dy <= PLATFORM + 4) {
			return Blocks.AIR.defaultBlockState();
		}
		return null;
	}

	// ---- the village -----------------------------------------------------------------------------------------

	private boolean isPath(int u, int v) {
		int du = u;
		int dv = v - CENTER_V;
		double d = Math.sqrt(du * du + dv * dv);
		// the way in, to the tree's door
		if (Math.abs(u) <= 1 && v < CENTER_V - 3) {
			return true;
		}
		if (Math.abs(d - 10) < 0.6 || Math.abs(d - HOUSE_RING) < 0.55) {
			return true;
		}
		for (int[] h : houses) {
			int hu = h[0];
			int hv = h[1] - CENTER_V;
			double len = Math.sqrt(hu * hu + hv * hv);
			double along = (du * hu + dv * hv) / len;
			if (along > 10 && along < len - 1.5 && Math.abs(du * hv - dv * hu) / len < 0.55) {
				return true;
			}
		}
		return false;
	}

	private boolean isPond(int u, int v) {
		int du = u - pond[0];
		int dv = v - pond[1];
		return du * du + dv * dv <= 7 + smooth(u, v, 2, 42) * 4;
	}

	private boolean nearHouse(int u, int v, int reach) {
		for (int[] h : houses) {
			if (Math.abs(u - h[0]) <= reach && Math.abs(v - h[1]) <= reach) {
				return true;
			}
		}
		return false;
	}

	private boolean lampPost(int u, int v) {
		for (int[] h : houses) {
			int hu = h[0];
			int hv = h[1] - CENTER_V;
			double len = Math.sqrt(hu * hu + hv * hv);
			int pu = (int) Math.round(hu * 14.5 / len + hv / len);
			int pv = CENTER_V + (int) Math.round(hv * 14.5 / len - hu / len);
			if (u == pu && v == pv) {
				return true;
			}
		}
		return (u == 2 || u == -2) && v > 2 && v < CENTER_V - 6 && v % 6 == 0;
	}

	@Override
	protected void column(WorldGenLevel level, int u, int v, int x, int z) {
		// the land under any tree there
		int top = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z) - 1;
		BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos(x, top, z);
		while (top > gateY - 12 && (level.getBlockState(pos).is(BlockTags.LOGS) || level.getBlockState(pos).is(BlockTags.LEAVES)
			|| level.getBlockState(pos).canBeReplaced())) {
			pos.setY(--top);
		}
		ground = top;
	}

	@Override
	protected @Nullable BlockState at(WorldGenLevel level, int u, int v, int y) {
		int g = gateY;
		int du = u;
		int dv = v - CENTER_V;
		double d = Math.sqrt(du * du + dv * dv);
		if (d > R + BLEND) {
			return null;
		}
		double rim = R - 3 + smooth(Math.atan2(du, dv) * 5 + 30, 0, 2, 54) * 3;
		if (d > rim) {
			// the edge, wavy: the land eased to the sanctuary's height, the trees there left alone
			double t = Math.min(1, (d - rim) / (R + BLEND - rim));
			t = t * t * (3 - 2 * t);
			int target = (int) Math.round((g - 1) * (1 - t) + ground * t);
			if (y == target) {
				return Blocks.GRASS_BLOCK.defaultBlockState();
			}
			if (y < target) {
				return y > ground ? Blocks.DIRT.defaultBlockState() : null;
			}
			return y <= ground ? Blocks.AIR.defaultBlockState() : null;
		}
		BlockState tree = tree(du, dv, y);
		if (tree != null) {
			return tree;
		}
		BlockState platform = platform(du, dv, y);
		if (platform != null) {
			return platform;
		}
		BlockState ramp = ramp(du, dv, y);
		if (ramp != null) {
			return ramp;
		}
		if (y < g - 1) {
			return y > g - 7 && level.getBlockState(world(u, v, y)).isAir() ? Blocks.DIRT.defaultBlockState() : null;
		}
		if (y == g - 1) {
			if (isPond(u, v)) {
				return Blocks.WATER.defaultBlockState();
			}
			if (isPath(u, v)) {
				return noise(u, v, y, 43) < 0.3F ? Blocks.MOSSY_STONE_BRICKS.defaultBlockState() : Blocks.DIRT_PATH.defaultBlockState();
			}
			if (nearHouse(u, v, 1)) {
				return Blocks.MOSS_BLOCK.defaultBlockState();
			}
			return noise(u, v, y, 44) < 0.25F ? Blocks.MOSS_BLOCK.defaultBlockState() : Blocks.GRASS_BLOCK.defaultBlockState();
		}
		if (y == g) {
			if (isPond(u, v)) {
				return noise(u, v, y, 45) < 0.25F ? Blocks.LILY_PAD.defaultBlockState() : Blocks.AIR.defaultBlockState();
			}
			if (lampPost(u, v)) {
				return fence().defaultBlockState();
			}
			if (isPath(u, v) || nearHouse(u, v, 0)) {
				return Blocks.AIR.defaultBlockState();
			}
			if (nearHouse(u, v, 2)) {
				return flower(u, v, y, 0.7F);
			}
			return flower(u, v, y, d < 7 ? 0.0F : 0.32F);
		}
		if (y == g + 1 && lampPost(u, v)) {
			return Blocks.LANTERN.defaultBlockState();
		}
		return y <= g + TRUNK + 10 ? Blocks.AIR.defaultBlockState() : null;
	}

	private BlockState flower(int u, int v, int y, float chance) {
		float n = noise(u, v, y, 46);
		if (n >= chance) {
			return noise(u, v, y, 47) < 0.25F ? Blocks.GRASS.defaultBlockState() : Blocks.AIR.defaultBlockState();
		}
		float m = noise(u, v, y, 48);
		if (m < 0.35F) {
			return (kind == 0 ? Blocks.PINK_PETALS : Blocks.PINK_PETALS).defaultBlockState().setValue(PinkPetalsBlock.AMOUNT, 1 + (int) (noise(u, v, y, 49) * 4))
				.setValue(PinkPetalsBlock.FACING, Direction.from2DDataValue((int) (noise(u, v, y, 50) * 4)));
		}
		if (m < 0.45F) {
			return Blocks.FLOWERING_AZALEA.defaultBlockState();
		}
		if (m < 0.52F) {
			return Blocks.FLOWERING_AZALEA.defaultBlockState();
		}
		Block[] flowers = {Blocks.ALLIUM, Blocks.PINK_TULIP, Blocks.AZURE_BLUET, Blocks.OXEYE_DAISY, Blocks.CORNFLOWER, Blocks.LILY_OF_THE_VALLEY, Blocks.BLUE_ORCHID};
		return flowers[(int) (noise(u, v, y, 51) * flowers.length)].defaultBlockState();
	}

	@Override
	protected void decorate(WorldGenLevel level, BoundingBox chunkBox, RandomSource random) {
		Block[] blocks = {IafBlockRegistry.PIXIE_HOUSE_MUSHROOM_RED, IafBlockRegistry.PIXIE_HOUSE_BIRCH, IafBlockRegistry.PIXIE_HOUSE_MUSHROOM_BROWN,
			IafBlockRegistry.PIXIE_HOUSE_OAK, IafBlockRegistry.PIXIE_HOUSE_SPRUCE, IafBlockRegistry.PIXIE_HOUSE_DARK_OAK};
		BlockPos tree = world(0, CENTER_V, gateY);
		for (int i = 0; i < HOUSES; i++) {
			house(level, chunkBox, world(houses[i][0], houses[i][1], gateY), blocks[i % blocks.length], tree, i);
		}
		for (int i = 0; i < PLATFORM_HOUSES.length; i++) {
			// round the platform, away from where the ramp comes up
			double angle = Math.toRadians(360 * (0.12 + 0.2 * i) + 35);
			BlockPos pos = world((int) Math.round(Math.sin(angle) * 6.5), CENTER_V - (int) Math.round(Math.cos(angle) * 6.5), gateY + PLATFORM + 1);
			house(level, chunkBox, pos, blocks[(i + 3) % blocks.length], tree.atY(pos.getY()), HOUSES + i);
		}
		// pixies about the tree
		for (int i = 0; i < 6; i++) {
			double angle = Math.PI * 2 * i / 6;
			BlockPos pos = world((int) Math.round(Math.sin(angle) * 9), CENTER_V + (int) Math.round(Math.cos(angle) * 9), gateY + 2 + (i % 2) * 10);
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

	private void house(WorldGenLevel level, BoundingBox chunkBox, BlockPos pos, Block block, BlockPos faceTo, int index) {
		if (!chunkBox.isInside(pos)) {
			return;
		}
		Direction toward = Direction.getNearest(faceTo.getX() - pos.getX(), 0, faceTo.getZ() - pos.getZ());
		level.setBlock(pos, block.defaultBlockState().setValue(BlockPixieHouse.FACING, toward), 2);
		if (level.getBlockEntity(pos) instanceof TileEntityPixieHouse house) {
			house.houseType = TileEntityPixieHouse.getHouseTypeFromBlock(block);
			house.hasPixie = true;
			house.pixieType = Mth.floor(noise(index, 0, 0, 52) * 5) % 5;
		}
	}
}

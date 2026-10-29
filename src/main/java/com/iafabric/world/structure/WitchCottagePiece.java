package com.iafabric.world.structure;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.BarrelBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CakeBlock;
import net.minecraft.world.level.block.CarvedPumpkinBlock;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.LanternBlock;
import net.minecraft.world.level.block.LayeredCauldronBlock;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import org.jetbrains.annotations.Nullable;

/**
 * A witches' cottage: a small crooked hut of spruce and dark oak on a stone footing, one step up from a clearing in the
 * woods, under a steep roof that ends in a soul lantern, its chimney smoking. Inside, a cauldron, a brewing stand,
 * shelves, a bitten cake on a barrel and a chest full of cakes (the witches are out). Outside, pumpkins and mushrooms.
 * Its gate is at the foot of its steps; the hut's middle is {@link #MIDDLE} ahead, at the corner of its chunk.
 */
public class WitchCottagePiece extends BuiltPiece {
	public static final int MIDDLE = 5;
	/** The hut's walls: across, and from the front wall to the back one. */
	private static final int HALF = 3;
	private static final int FRONT = 2;
	private static final int BACK = 8;
	/** The clearing about it (an ellipse this wide each way about a point a little before the hut's middle). */
	private static final double YARD = 8.5;
	public static final String LOOT = "iceandfire_fabric:chest/witch_cottage";
	/** The land's height at the column being made (without plants). */
	private int ground;

	public WitchCottagePiece(int gateX, int gateY, int gateZ, int facing, long seed) {
		super(IafStructures.WITCH_COTTAGE_PIECE, gateX, gateY, gateZ, facing, 0, 0, seed, -9, 9, -5, 13, gateY - 7, gateY + 11);
	}

	public WitchCottagePiece(CompoundTag tag) {
		super(IafStructures.WITCH_COTTAGE_PIECE, tag);
	}

	/** The world's way of the hut's forward (from the gate in) and of its across. */
	private Direction forward() {
		return switch (facing & 3) {
			case 0 -> Direction.SOUTH;
			case 1 -> Direction.WEST;
			case 2 -> Direction.NORTH;
			default -> Direction.EAST;
		};
	}

	private Direction across() {
		return forward().getCounterClockWise();
	}

	@Override
	protected int plantClearing() {
		return 6;
	}

	@Override
	protected boolean keptOpen(WorldGenLevel level, int u, int v, int y) {
		return Math.abs(u) < HALF && v > FRONT && v < BACK && y > gateY && y < gateY + 4;
	}

	@Override
	protected void column(WorldGenLevel level, int u, int v, int x, int z) {
		int top = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z) - 1;
		BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos(x, top, z);
		while (top > gateY - 8 && (level.getBlockState(pos).is(BlockTags.LOGS) || level.getBlockState(pos).is(BlockTags.LEAVES)
			|| level.getBlockState(pos).canBeReplaced())) {
			pos.setY(--top);
		}
		ground = top;
	}

	private boolean hut(int u, int v) {
		return Math.abs(u) <= HALF && v >= FRONT && v <= BACK;
	}

	private boolean porch(int u, int v) {
		return Math.abs(u) <= 2 && v >= 0 && v < FRONT;
	}

	private boolean steps(int u, int v) {
		return Math.abs(u) <= 1 && v == -1;
	}

	private boolean chimney(int u, int v) {
		return u == HALF + 1 && v == BACK - 1;
	}

	@Override
	protected @Nullable BlockState at(WorldGenLevel level, int u, int v, int y) {
		int g = gateY;
		double du = u / YARD;
		double dv = (v - MIDDLE + 1) / YARD;
		double r = Math.sqrt(du * du + dv * dv);
		BlockState roof = roof(u, v, y);
		if (roof != null) {
			return roof;
		}
		if (chimney(u, v)) {
			if (y > ground && y <= g + 7) {
				return noise(u, v, y, 3) < 0.35F ? Blocks.MOSSY_COBBLESTONE.defaultBlockState() : Blocks.COBBLESTONE.defaultBlockState();
			}
			return y == g + 8 ? Blocks.CAMPFIRE.defaultBlockState() : null;
		}
		if (hut(u, v) || porch(u, v)) {
			return hut(u, v) ? body(u, v, y) : porchAt(u, v, y);
		}
		if (steps(u, v) && y == g) {
			return Blocks.SPRUCE_STAIRS.defaultBlockState().setValue(StairBlock.FACING, forward());
		}
		if (r > 1) {
			return null;
		}
		// the clearing: the land eased to the hut's height at its edge, its plants and trees taken away
		double rim = 0.75;
		int target = g - 1;
		if (r > rim) {
			double t = (r - rim) / (1 - rim);
			t = t * t * (3 - 2 * t);
			target = (int) Math.round((g - 1) * (1 - t) + ground * t);
		}
		if (y == target) {
			BlockState here = level.getBlockState(world(u, v, y));
			if (u == 0 && v < -1 && v >= -4) {
				return Blocks.DIRT_PATH.defaultBlockState();
			}
			if (garden(u, v) == 2) {
				return Blocks.PODZOL.defaultBlockState();
			}
			return here.is(BlockTags.DIRT) && !here.is(Blocks.DIRT) ? null : Blocks.GRASS_BLOCK.defaultBlockState();
		}
		if (y < target) {
			return y > ground ? Blocks.DIRT.defaultBlockState() : null;
		}
		if (y == target + 1 && r <= rim) {
			return garden(u, v) > 0 ? gardenAt(u, v) : wild(u, v, y);
		}
		return y <= g + 6 || y <= ground ? Blocks.AIR.defaultBlockState() : null;
	}

	/** Its stone footing down to the land, its floor, its walls (logs at the corners, a beam on top), door and windows, and inside. */
	private BlockState body(int u, int v, int y) {
		int g = gateY;
		boolean corner = Math.abs(u) == HALF && (v == FRONT || v == BACK);
		boolean wall = Math.abs(u) == HALF || v == FRONT || v == BACK;
		if (y < g) {
			if (y <= ground - 1) {
				return null;
			}
			if (corner) {
				return log(Direction.Axis.Y);
			}
			return noise(u, v, y, 1) < 0.4F ? Blocks.MOSSY_COBBLESTONE.defaultBlockState() : Blocks.COBBLESTONE.defaultBlockState();
		}
		if (y == g) {
			return wall ? Blocks.DARK_OAK_PLANKS.defaultBlockState() : Blocks.SPRUCE_PLANKS.defaultBlockState();
		}
		if (y > g + 3) {
			return Blocks.AIR.defaultBlockState();
		}
		if (corner) {
			return log(Direction.Axis.Y);
		}
		if (wall) {
			if (y == g + 3) {
				return log(Math.abs(u) == HALF ? forward().getAxis() : across().getAxis());
			}
			if (v == FRONT && u == 0) {
				return Blocks.DARK_OAK_DOOR.defaultBlockState().setValue(DoorBlock.FACING, forward())
					.setValue(DoorBlock.HALF, y == g + 1 ? DoubleBlockHalf.LOWER : DoubleBlockHalf.UPPER);
			}
			if (y == g + 2 && (Math.abs(u) == HALF && v == MIDDLE || v == BACK && u == 0)) {
				return Blocks.PURPLE_STAINED_GLASS.defaultBlockState();
			}
			return noise(u, v, y, 2) < 0.25F ? Blocks.DARK_OAK_PLANKS.defaultBlockState() : Blocks.SPRUCE_PLANKS.defaultBlockState();
		}
		return inside(u, v, y);
	}

	/** The room: the cauldron, the brewing stand and the shelves on one side; the chest, the barrel with a bitten cake on the other. */
	private BlockState inside(int u, int v, int y) {
		int dy = y - gateY;
		if (dy == 1) {
			if (u == -2 && v == 7) {
				return Blocks.WATER_CAULDRON.defaultBlockState().setValue(LayeredCauldronBlock.LEVEL, 3);
			}
			if (u == -2 && v == 6) {
				return Blocks.BREWING_STAND.defaultBlockState();
			}
			if (u == -2 && v == 3) {
				return Blocks.POTTED_RED_MUSHROOM.defaultBlockState();
			}
			if (u == 2 && v == 6) {
				return Blocks.BARREL.defaultBlockState().setValue(BarrelBlock.FACING, Direction.UP);
			}
			if (u == 2 && v == 3) {
				return Blocks.POTTED_DEAD_BUSH.defaultBlockState();
			}
			if (Math.abs(u) <= 1 && v >= 4 && v <= 6) {
				return Blocks.PURPLE_CARPET.defaultBlockState();
			}
		}
		if (u == -2 && (v == 4 || v == 5) && dy <= 2) {
			return Blocks.BOOKSHELF.defaultBlockState();
		}
		if (dy == 2 && u == 2 && v == 6) {
			return Blocks.CAKE.defaultBlockState().setValue(CakeBlock.BITES, 2);
		}
		if (dy == 3) {
			if (u == 0 && v == MIDDLE) {
				return Blocks.LANTERN.defaultBlockState().setValue(LanternBlock.HANGING, true);
			}
			if (u == -2 && v == 7 || u == 2 && v == 3) {
				return Blocks.COBWEB.defaultBlockState();
			}
		}
		return Blocks.AIR.defaultBlockState();
	}

	/** Its porch: a dark floor on the footing, a post with a lantern at each front corner. */
	private BlockState porchAt(int u, int v, int y) {
		int g = gateY;
		if (y < g) {
			return y <= ground - 1 ? null : Blocks.COBBLESTONE.defaultBlockState();
		}
		if (y == g) {
			return Blocks.DARK_OAK_PLANKS.defaultBlockState();
		}
		if (Math.abs(u) == 2 && v == 0) {
			if (y == g + 1) {
				return Blocks.DARK_OAK_FENCE.defaultBlockState();
			}
			if (y == g + 2) {
				return Blocks.LANTERN.defaultBlockState();
			}
		}
		return y <= g + 3 ? Blocks.AIR.defaultBlockState() : null;
	}

	/**
	 * Its roof, a witch's hat: steep, five layers (stairs at the edges), its brim of dark oak out over the walls, its
	 * crown of purple wool, ending in a post with a soul lantern.
	 */
	private @Nullable BlockState roof(int u, int v, int y) {
		int layer = y - gateY - 4;
		if (layer < 0) {
			return null;
		}
		if (layer == 5 && u == 0 && v == MIDDLE) {
			return Blocks.DARK_OAK_FENCE.defaultBlockState();
		}
		if (layer == 6 && u == 0 && v == MIDDLE) {
			return Blocks.SOUL_LANTERN.defaultBlockState();
		}
		if (layer > 4) {
			return null;
		}
		int half = HALF + 1 - layer;
		int front = FRONT - 1 + layer;
		int back = BACK + 1 - layer;
		if (Math.abs(u) > half || v < front || v > back || chimney(u, v)) {
			return null;
		}
		boolean side = Math.abs(u) == half;
		boolean end = v == front || v == back;
		boolean crown = layer >= 2;
		if (side == end) {
			// a corner, or within
			return (crown ? Blocks.PURPLE_WOOL : Blocks.DARK_OAK_PLANKS).defaultBlockState();
		}
		Direction up = side ? (u < 0 ? across() : across().getOpposite()) : (v == front ? forward() : forward().getOpposite());
		return (crown ? Blocks.PURPUR_STAIRS : Blocks.DARK_OAK_STAIRS).defaultBlockState().setValue(StairBlock.FACING, up);
	}

	/** Its garden by the hut: pumpkins on one side (1), mushrooms on podzol on the other (2); 0 none. */
	private static int garden(int u, int v) {
		if (u >= 5 && u <= 6 && v >= 2 && v <= 5) {
			return (u + v) % 2 == 0 ? 1 : 0;
		}
		if (u <= -5 && u >= -6 && v >= 3 && v <= 6) {
			return (u + v) % 2 == 0 ? 2 : 0;
		}
		return 0;
	}

	private BlockState gardenAt(int u, int v) {
		if (garden(u, v) == 2) {
			return (Math.floorMod(v, 2) == 0 ? Blocks.RED_MUSHROOM : Blocks.BROWN_MUSHROOM).defaultBlockState();
		}
		if (u == 6 && v == 3) {
			return Blocks.JACK_O_LANTERN.defaultBlockState().setValue(CarvedPumpkinBlock.FACING, forward().getOpposite());
		}
		return Blocks.PUMPKIN.defaultBlockState();
	}

	/** The clearing's own plants: a few grass tufts, ferns and alliums. */
	private BlockState wild(int u, int v, int y) {
		float n = noise(u, v, y, 4);
		if (n < 0.08F) {
			return Blocks.SHORT_GRASS.defaultBlockState();
		}
		if (n < 0.11F) {
			return Blocks.FERN.defaultBlockState();
		}
		if (n < 0.13F) {
			return Blocks.ALLIUM.defaultBlockState();
		}
		return Blocks.AIR.defaultBlockState();
	}

	private BlockState log(Direction.Axis axis) {
		return Blocks.DARK_OAK_LOG.defaultBlockState().setValue(RotatedPillarBlock.AXIS, axis);
	}

	@Override
	protected void decorate(WorldGenLevel level, BoundingBox chunkBox, RandomSource random) {
		// their chest, full of cakes, its front to the door
		DreadPieces.chest(level, chunkBox, random, world(2, BACK - 1, gateY + 1), forward().getOpposite(), LOOT);
	}

	/** Where the chest full of cakes is. */
	public BlockPos chest() {
		return world(2, BACK - 1, gateY + 1);
	}
}

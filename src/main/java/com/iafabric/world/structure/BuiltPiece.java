package com.iafabric.world.structure;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceType;
import it.unimi.dsi.fastutil.booleans.BooleanArrayList;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * A part of a building made by a rule for each block instead of a saved template: {@link #at} says what goes at a
 * place given in the building's own frame (u across, v forward from its gate, y up), and only the blocks of the
 * chunk being made are set, so a building can be as big as needed. The same place always gets the same block, in
 * whatever order the chunks are made.
 */
public abstract class BuiltPiece extends StructurePiece {
	/** The building's gate (its frame's origin) and the way it faces (0 south, 1 west, 2 north, 3 east). */
	protected final int gateX;
	protected final int gateY;
	protected final int gateZ;
	protected final int facing;
	/** What the building is for: the dragon kind of a dragon hall, for example. */
	protected final int kind;
	/** A size of the building chosen for its place: how deep a dragon hall goes, for example. */
	protected final int param;
	protected final long seed;

	protected BuiltPiece(StructurePieceType type, int gateX, int gateY, int gateZ, int facing, int kind, int param, long seed,
		int uMin, int uMax, int vMin, int vMax, int yMin, int yMax) {
		super(type, 0, box(gateX, gateZ, facing, uMin, uMax, vMin, vMax, yMin, yMax));
		this.gateX = gateX;
		this.gateY = gateY;
		this.gateZ = gateZ;
		this.facing = facing;
		this.kind = kind;
		this.param = param;
		this.seed = seed;
	}

	protected BuiltPiece(StructurePieceType type, CompoundTag tag) {
		super(type, tag);
		this.gateX = tag.getIntOr("GateX", 0);
		this.gateY = tag.getIntOr("GateY", 0);
		this.gateZ = tag.getIntOr("GateZ", 0);
		this.facing = tag.getIntOr("Facing", 0);
		this.kind = tag.getIntOr("Kind", 0);
		this.param = tag.getIntOr("Param", 0);
		this.seed = tag.getLongOr("Seed", 0L);
	}

	@Override
	protected void addAdditionalSaveData(StructurePieceSerializationContext context, CompoundTag tag) {
		tag.putInt("GateX", gateX);
		tag.putInt("GateY", gateY);
		tag.putInt("GateZ", gateZ);
		tag.putInt("Facing", facing);
		tag.putInt("Kind", kind);
		tag.putInt("Param", param);
		tag.putLong("Seed", seed);
	}

	private static BoundingBox box(int gateX, int gateZ, int facing, int uMin, int uMax, int vMin, int vMax, int yMin, int yMax) {
		int[] a = toWorld(gateX, gateZ, facing, uMin, vMin);
		int[] b = toWorld(gateX, gateZ, facing, uMax, vMax);
		return new BoundingBox(Math.min(a[0], b[0]), yMin, Math.min(a[1], b[1]), Math.max(a[0], b[0]), yMax, Math.max(a[1], b[1]));
	}

	/** The building's gate (its frame's origin). */
	public BlockPos gate() {
		return new BlockPos(gateX, gateY, gateZ);
	}

	/** The way the building faces (0 south, 1 west, 2 north, 3 east). */
	public int facing() {
		return facing;
	}

	public int kind() {
		return kind;
	}

	public int param() {
		return param;
	}

	/** A place of the building in the world. */
	public BlockPos at(int u, int v, int y) {
		return world(u, v, y);
	}

	/** World x, z of a place in the frame of a building with its gate at (gateX, gateZ). */
	public static int[] toWorld(int gateX, int gateZ, int facing, int u, int v) {
		return switch (facing & 3) {
			case 0 -> new int[]{gateX + u, gateZ + v};
			case 1 -> new int[]{gateX - v, gateZ + u};
			case 2 -> new int[]{gateX - u, gateZ - v};
			default -> new int[]{gateX + v, gateZ - u};
		};
	}

	protected BlockPos world(int u, int v, int y) {
		int[] xz = toWorld(gateX, gateZ, facing, u, v);
		return new BlockPos(xz[0], y, xz[1]);
	}

	/** A world column's place in the building's frame (u, v). */
	protected int[] frame(int x, int z) {
		return new int[]{uOf(x, z), vOf(x, z)};
	}

	private int uOf(int x, int z) {
		return switch (facing & 3) {
			case 0 -> x - gateX;
			case 1 -> z - gateZ;
			case 2 -> gateX - x;
			default -> gateZ - z;
		};
	}

	private int vOf(int x, int z) {
		return switch (facing & 3) {
			case 0 -> z - gateZ;
			case 1 -> gateX - x;
			case 2 -> gateZ - z;
			default -> x - gateX;
		};
	}

	/** A number in [0, 1) that is always the same for the same place (and salt) of this building. */
	protected float noise(int u, int v, int y, int salt) {
		long h = seed ^ (u * 73428767L) ^ (v * 912931L) ^ (y * 4243613L) ^ (salt * 2654435761L);
		h ^= (h >>> 33);
		h *= 0xff51afd7ed558ccdL;
		h ^= (h >>> 33);
		h *= 0xc4ceb9fe1a85ec53L;
		h ^= (h >>> 33);
		return (h >>> 40) / (float) (1L << 24);
	}

	/**
	 * A smooth number in [0, 1) over the building's ground: it changes little from one block to the next (about
	 * once every {@code scale} blocks), for natural shapes (a cave's wall, a path's bends).
	 */
	protected float smooth(double u, double v, double scale, int salt) {
		double x = u / scale;
		double z = v / scale;
		int x0 = (int) Math.floor(x);
		int z0 = (int) Math.floor(z);
		double fx = x - x0;
		double fz = z - z0;
		fx = fx * fx * (3 - 2 * fx);
		fz = fz * fz * (3 - 2 * fz);
		float a = noise(x0, z0, 0, salt);
		float b = noise(x0 + 1, z0, 0, salt);
		float c = noise(x0, z0 + 1, 0, salt);
		float d = noise(x0 + 1, z0 + 1, 0, salt);
		double top = a + (b - a) * fx;
		double bottom = c + (d - c) * fx;
		return (float) (top + (bottom - top) * fz);
	}

	/** Called for each column of the chunk before its blocks (a piece may look at the land there once). */
	protected void column(WorldGenLevel level, int u, int v, int x, int z) {
	}

	/** What goes at a place of the building, or null to leave the land as it is. */
	protected abstract @Nullable BlockState at(WorldGenLevel level, int u, int v, int y);

	/** Chests, creatures and anything else at fixed places, set once the chunk's blocks are in. */
	protected void decorate(WorldGenLevel level, BoundingBox chunkBox, RandomSource random) {
	}

	@Override
	public void postProcess(WorldGenLevel level, StructureManager structures, ChunkGenerator generator, RandomSource random, BoundingBox chunkBox,
		ChunkPos chunkPos, BlockPos pivot) {
		BoundingBox box = this.boundingBox;
		int minX = Math.max(box.minX(), chunkBox.minX());
		int maxX = Math.min(box.maxX(), chunkBox.maxX());
		int minZ = Math.max(box.minZ(), chunkBox.minZ());
		int maxZ = Math.min(box.maxZ(), chunkBox.maxZ());
		int minY = Math.max(box.minY(), level.getMinY() + 1);
		int maxY = Math.min(box.maxY(), level.getMaxY() - 1);
		if (minX > maxX || minZ > maxZ) {
			return;
		}
		BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
		for (int x = minX; x <= maxX; x++) {
			for (int z = minZ; z <= maxZ; z++) {
				int u = uOf(x, z);
				int v = vOf(x, z);
				column(level, u, v, x, z);
				for (int y = minY; y <= maxY; y++) {
					BlockState state = at(level, u, v, y);
					if (state != null) {
						pos.set(x, y, z);
						level.setBlock(pos, state, 2);
					}
				}
			}
		}
		decorate(level, chunkBox, random);
		clearFloatingPlants(level, chunkBox);
		clearHanging(level, chunkBox);
	}

	/**
	 * Takes away the small things left hanging where this piece cut the land or a wall away: grass, flowers, bushes,
	 * snow, a lantern or a torch. Whatever could not stay where it is (it would drop at the first touch) with nothing
	 * firm under it; what the piece set on its own floors stays.
	 */
	private void clearHanging(WorldGenLevel level, BoundingBox chunkBox) {
		BoundingBox box = this.boundingBox;
		int minX = Math.max(box.minX(), chunkBox.minX());
		int maxX = Math.min(box.maxX(), chunkBox.maxX());
		int minZ = Math.max(box.minZ(), chunkBox.minZ());
		int maxZ = Math.min(box.maxZ(), chunkBox.maxZ());
		int minY = Math.max(box.minY(), level.getMinY() + 1);
		int maxY = Math.min(box.maxY() + 2, level.getMaxY() - 1);
		BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
		BlockPos.MutableBlockPos below = new BlockPos.MutableBlockPos();
		for (int x = minX; x <= maxX; x++) {
			for (int z = minZ; z <= maxZ; z++) {
				// what hangs (vines, lichen, roots, a hanging lantern) from the top down, so a whole vine goes with its top
				for (int y = maxY; y >= minY; y--) {
					BlockState state = level.getBlockState(pos.set(x, y, z));
					if (hangs(state) && !state.canSurvive(level, pos)) {
						level.setBlock(pos, state.getFluidState().createLegacyBlock(), 2);
					}
				}
				// what stands from the bottom up, so the top of a tall plant goes with its foot
				for (int y = minY; y <= maxY; y++) {
					BlockState state = level.getBlockState(pos.set(x, y, z));
					if (state.isAir() || hangs(state) || state.canSurvive(level, pos)) {
						continue;
					}
					below.set(x, y - 1, z);
					if (level.getBlockState(below).isFaceSturdy(level, below, Direction.UP)) {
						continue;
					}
					level.setBlock(pos, state.getFluidState().createLegacyBlock(), 2);
				}
			}
		}
	}

	/** What holds on from above or from a side (a firm floor under it does not keep it). */
	private static boolean hangs(BlockState state) {
		return state.is(BlockTags.CAVE_VINES) || state.is(Blocks.WEEPING_VINES) || state.is(Blocks.WEEPING_VINES_PLANT) || state.is(Blocks.HANGING_ROOTS)
			|| state.is(Blocks.SPORE_BLOSSOM) || state.is(Blocks.PALE_HANGING_MOSS) || state.is(Blocks.VINE) || state.is(Blocks.GLOW_LICHEN)
			|| state.is(BlockTags.CEILING_HANGING_SIGNS) || state.is(BlockTags.WALL_SIGNS) || state.is(Blocks.LADDER)
			|| state.hasProperty(BlockStateProperties.HANGING) && state.getValue(BlockStateProperties.HANGING);
	}

	/**
	 * How far around its box a piece looks for the trees and giant mushrooms it cut and left hanging in the air (0 to
	 * not look). Trees grow before the piece is made: a nest's bowl or a fortress's wall cuts their trunks.
	 */
	protected int plantClearing() {
		return 0;
	}

	/**
	 * Whether this piece keeps a place open whatever the land was (inside its walls, its rooms): any bit of a tree
	 * found there was cut by it, or grew into it from a chunk made later, and is taken away even if it leans on a wall.
	 * Only where the piece's rule does not hang on the land's height (not its apron).
	 */
	protected boolean keptOpen(WorldGenLevel level, int u, int v, int y) {
		return false;
	}

	/** Whether the building keeps this place open: no bit of a tree may be there (see {@link #keptOpen}). */
	public boolean openAt(WorldGenLevel level, BlockPos pos) {
		return boundingBox.isInside(pos) && keptOpen(level, uOf(pos.getX(), pos.getZ()), vOf(pos.getX(), pos.getZ()), pos.getY());
	}

	/** What a tree or a giant mushroom is made of (not the snow or the moss on the ground). */
	protected static boolean treePart(BlockState state) {
		return state.is(BlockTags.LOGS) || state.is(BlockTags.LEAVES) || state.is(Blocks.MUSHROOM_STEM) || state.is(Blocks.RED_MUSHROOM_BLOCK)
			|| state.is(Blocks.BROWN_MUSHROOM_BLOCK) || state.is(Blocks.VINE) || state.is(Blocks.COCOA) || state.is(Blocks.BEE_NEST)
			|| state.is(Blocks.PALE_HANGING_MOSS) || state.is(Blocks.CREAKING_HEART);
	}

	/** What a tree or a giant mushroom is made of, or what grows on it; not leaves a builder set (they never fall). */
	protected static boolean plant(BlockState state) {
		if (state.hasProperty(LeavesBlock.PERSISTENT) && state.getValue(LeavesBlock.PERSISTENT)) {
			return false;
		}
		return state.is(BlockTags.LOGS) || state.is(BlockTags.LEAVES) || state.is(Blocks.MUSHROOM_STEM) || state.is(Blocks.RED_MUSHROOM_BLOCK)
			|| state.is(Blocks.BROWN_MUSHROOM_BLOCK) || state.is(Blocks.VINE) || state.is(Blocks.COCOA) || state.is(Blocks.BEE_NEST)
			|| state.is(Blocks.SNOW) || state.is(Blocks.PALE_HANGING_MOSS) || state.is(Blocks.PALE_MOSS_CARPET) || state.is(Blocks.CREAKING_HEART);
	}

	/** The wood of a tree or a giant mushroom: what holds the rest of it up. */
	private static boolean wood(BlockState state) {
		return state.is(BlockTags.LOGS) || state.is(Blocks.MUSHROOM_STEM) || state.is(Blocks.CREAKING_HEART);
	}

	/**
	 * Takes away what is left of the trees and giant mushrooms this piece cut: every trunk or branch that touches no
	 * ground or wall any more, with its leaves. It looks over the chunk and the ones around it (all a chunk being made
	 * may touch), so a tree cut in one chunk is cleared in the next one too.
	 */
	private void clearFloatingPlants(WorldGenLevel level, BoundingBox chunkBox) {
		int margin = plantClearing();
		if (margin <= 0) {
			return;
		}
		BoundingBox box = this.boundingBox;
		int x0 = Math.max(chunkBox.minX() - 15, box.minX() - margin);
		int x1 = Math.min(chunkBox.maxX() + 15, box.maxX() + margin);
		int z0 = Math.max(chunkBox.minZ() - 15, box.minZ() - margin);
		int z1 = Math.min(chunkBox.maxZ() + 15, box.maxZ() + margin);
		int y0 = Math.max(box.minY(), level.getMinY() + 1);
		int y1 = Math.min(box.maxY() + 24, level.getMaxY() - 1);
		if (x0 > x1 || z0 > z1 || y0 > y1) {
			return;
		}
		List<BlockPos> loose = floatingPlants(level, x0, y0, z0, x1, y1, z1, (state, pos) -> {
			if (treePart(state) && box.isInside(pos) && keptOpen(level, uOf(pos.getX(), pos.getZ()), vOf(pos.getX(), pos.getZ()), pos.getY())) {
				// inside its walls: no tree, leaning on a wall or not
				level.setBlock(pos, Blocks.AIR.defaultBlockState(), 2);
				return true;
			}
			return false;
		});
		for (BlockPos pos : loose) {
			level.setBlock(pos, Blocks.AIR.defaultBlockState(), 2);
		}
	}

	/** Takes a bit of a tree away at once while an area is looked over (true: it is gone). */
	@FunctionalInterface
	public interface PlantCut {
		boolean cut(BlockState state, BlockPos pos);
	}

	private static final byte WOOD = 1;
	/** Leaves the game keeps only near wood (they know how far it is). */
	private static final byte LEAF = 2;
	/** What grows on a tree or holds on by itself: vines, cocoa, snow, a mushroom's cap, moss. */
	private static final byte CLING = 3;
	private static final int[][] SIDES = {{1, 0, 0}, {-1, 0, 0}, {0, 1, 0}, {0, -1, 0}, {0, 0, 1}, {0, 0, -1}};

	/**
	 * The bits of trees and giant mushrooms in an area that nothing holds up any more. Only wood holds a tree: a trunk
	 * or a branch (its blocks touching, corners too) is held when it touches firm ground or a wall; a leaf belongs to the
	 * nearest wood within six steps through the leaves, as the game's own leaves do, and is held when that is; what clings
	 * on (a vine, cocoa, snow, a mushroom's cap) goes with what it grows on, or holds on to something firm by itself. Leaves
	 * hold nothing up: in a thick forest every crown touches the next, and a crown cut from its trunk would hang on its
	 * neighbours. What goes on past the area is left as it is. {@code cut} may take bits away at once as they are found.
	 */
	public static List<BlockPos> floatingPlants(LevelReader level, int x0, int y0, int z0, int x1, int y1, int z1, @Nullable PlantCut cut) {
		int sx = x1 - x0 + 1;
		int sy = y1 - y0 + 1;
		int sz = z1 - z0 + 1;
		byte[] cells = new byte[sx * sy * sz];
		BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
		List<BlockPos> loose = new ArrayList<>();
		boolean any = false;
		for (int x = x0; x <= x1; x++) {
			for (int z = z0; z <= z1; z++) {
				// all the way up: while the land is being made, the height a chunk knows of its blocks may be from before
				// the trees of the chunks around it grew into it (their crowns were missed, and their trunks taken away)
				for (int y = y0; y <= y1; y++) {
					BlockState state = level.getBlockState(pos.set(x, y, z));
					if (!plant(state) || cut != null && cut.cut(state, pos)) {
						continue;
					}
					cells[((x - x0) * sz + (z - z0)) * sy + (y - y0)] = wood(state) ? WOOD : state.hasProperty(LeavesBlock.DISTANCE) ? LEAF : CLING;
					any = true;
				}
			}
		}
		if (!any) {
			return loose;
		}
		int n = cells.length;
		int[] queue = new int[n];
		// the wood, piece by piece
		int[] piece = new int[n];
		BooleanArrayList held = new BooleanArrayList();
		held.add(false);
		for (int start = 0; start < n; start++) {
			if (cells[start] != WOOD || piece[start] != 0) {
				continue;
			}
			int id = held.size();
			boolean firm = false;
			int head = 0;
			int tail = 0;
			queue[tail++] = start;
			piece[start] = id;
			while (head < tail) {
				int i = queue[head++];
				int y = i % sy;
				int z = (i / sy) % sz;
				int x = i / (sy * sz);
				if (x == 0 || x == sx - 1 || z == 0 || z == sz - 1 || y == 0 || y == sy - 1) {
					// it goes on past where this area looks: left as it is
					firm = true;
				}
				for (int dx = -1; dx <= 1; dx++) {
					for (int dz = -1; dz <= 1; dz++) {
						for (int dy = -1; dy <= 1; dy++) {
							int nx = x + dx;
							int ny = y + dy;
							int nz = z + dz;
							if ((dx | dy | dz) == 0 || nx < 0 || nx >= sx || ny < 0 || ny >= sy || nz < 0 || nz >= sz) {
								continue;
							}
							int j = (nx * sz + nz) * sy + ny;
							if (cells[j] == WOOD && piece[j] == 0) {
								piece[j] = id;
								queue[tail++] = j;
							} else if (!firm && cells[j] == 0 && Math.abs(dx) + Math.abs(dy) + Math.abs(dz) == 1
								&& firm(level, pos.set(x0 + nx, y0 + ny, z0 + nz))) {
								firm = true;
							}
						}
					}
				}
			}
			held.add(firm);
		}
		// the leaves: from the wood held (and the area's edge) and from the wood left hanging at once, step by step through
		// the leaves, so each is reached first from the nearest (from what is held when as near to both)
		byte[] from = new byte[n];
		byte[] steps = new byte[n];
		byte[] source = new byte[n];
		for (int i = 0; i < n; i++) {
			if (cells[i] == WOOD) {
				source[i] = (byte) (held.getBoolean(piece[i]) ? 1 : 2);
			} else if (cells[i] != 0) {
				int y = i % sy;
				int z = (i / sy) % sz;
				int x = i / (sy * sz);
				if (x == 0 || x == sx - 1 || z == 0 || z == sz - 1 || y == 0 || y == sy - 1) {
					source[i] = 1;
				} else if (cells[i] == CLING) {
					// a vine on a rock, snow on the ground: held where it is, but it holds nothing up
					for (int[] side : SIDES) {
						int j = ((x + side[0]) * sz + z + side[2]) * sy + y + side[1];
						if (cells[j] == 0 && firm(level, pos.set(x0 + x + side[0], y0 + y + side[1], z0 + z + side[2]))) {
							from[i] = 1;
							break;
						}
					}
				}
			}
		}
		int tail = 0;
		for (byte kind = 1; kind <= 2; kind++) {
			for (int i = 0; i < n; i++) {
				if (source[i] == kind) {
					from[i] = kind;
					queue[tail++] = i;
				}
			}
		}
		int head = 0;
		while (head < tail) {
			int i = queue[head++];
			int y = i % sy;
			int z = (i / sy) % sz;
			int x = i / (sy * sz);
			for (int[] side : SIDES) {
				int nx = x + side[0];
				int ny = y + side[1];
				int nz = z + side[2];
				if (nx < 0 || nx >= sx || ny < 0 || ny >= sy || nz < 0 || nz >= sz) {
					continue;
				}
				int j = (nx * sz + nz) * sy + ny;
				// what clings on is as far from the wood as what it grows on
				int far = cells[j] == CLING ? steps[i] : steps[i] + 1;
				if (cells[j] >= LEAF && from[j] == 0 && far <= 6) {
					from[j] = from[i];
					steps[j] = (byte) far;
					queue[tail++] = j;
				}
			}
		}
		for (int i = 0; i < n; i++) {
			if (cells[i] != 0 && from[i] != 1) {
				int y = i % sy;
				int z = (i / sy) % sz;
				int x = i / (sy * sz);
				loose.add(new BlockPos(x0 + x, y0 + y, z0 + z));
			}
		}
		return loose;
	}

	/** Ground, a wall, anything a tree can stand on or lean against (not air, water, grass or another tree). */
	private static boolean firm(LevelReader level, BlockPos pos) {
		BlockState state = level.getBlockState(pos);
		return !state.isAir() && !state.canBeReplaced() && state.getFluidState().isEmpty();
	}
}

package com.iafabric.world.structure;

import com.iafabric.block.BlockGoldPile;
import com.iafabric.block.IafBlockRegistry;
import com.iafabric.entity.EntityDragonBase;
import com.iafabric.entity.IafEntityRegistry;
import com.iafabric.entity.util.HomePosition;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.CandleBlock;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.LanternBlock;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.SkullBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.WallBannerBlock;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraft.world.level.block.state.properties.Half;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceType;
import org.jspecify.annotations.Nullable;

/**
 * The parts of a stage 5 dragon's hall, a dwarf hold taken by a dragon (in the manner of the Lonely Mountain and the
 * mines under the mountains): on the land, an abandoned castle in the stone of its land (stone bricks, ice and spruce
 * in cold lands, sandstone in hot ones), its walls and towers broken, a courtyard with rubble and a great keep whose
 * roof has fallen in. In the keep a wide stair goes down to the forge hall: channels of lava (ice or water by the
 * dragon), pillars, forges, and passages to side rooms (a store, a library, a prison, barracks). Beyond it a bridge
 * runs down over a deep chasm to the arena: a huge walled hall with galleries and braziers, and at its end, on a dais,
 * the dragon's hoard, a hill of gold with chests in it, where the dragon sleeps.
 * Kind: the dragon (kind % 3: 0 fire, 1 ice, 2 lightning) and the land (kind / 3: 0 temperate, 1 cold, 2 hot).
 * Param: how deep the arena's floor is under the gate.
 */
public final class DragonHallPieces {
	public static final int CASTLE_HALF = 30;
	public static final int CASTLE_END = 60;
	public static final int KEEP_HALF = 16;
	public static final int KEEP_V0 = 33;
	public static final int KEEP_V1 = 59;
	public static final int STAIR_V0 = 37;
	public static final int STAIR_OPEN_V1 = 52;
	public static final int FORGE_DROP = 21;
	public static final int FORGE_V0 = 62;
	public static final int FORGE_V1 = 100;
	public static final int ARENA_V0 = 150;
	public static final int ARENA_V1 = 238;
	public static final int ARENA_HALF = 46;
	public static final int HOARD_V = 210;
	/** The arena's vault: its height at the walls and in the middle. */
	public static final int VAULT_SIDE = 24;
	public static final int VAULT_TOP = 42;
	/** The chasm's half width and its roof over the bridge. */
	public static final int CHASM_HALF = 24;
	public static final int CHASM_ROOF = 22;
	public static final int MIDDLE = 114;
	public static final int MIN_DEPTH = 58;
	public static final int MAX_DEPTH = 70;
	private static final int[] ROOMS_V = {74, 90};

	private DragonHallPieces() {
	}

	/** What the parts share: the frame, the depths, the stone of the land and of the dragon. */
	abstract static class HallPiece extends BuiltPiece {
		HallPiece(StructurePieceType type, int x, int y, int z, int facing, int kind, int depth, long seed,
			int uMin, int uMax, int vMin, int vMax, int yMin, int yMax) {
			super(type, x, y, z, facing, kind, depth, seed, uMin, uMax, vMin, vMax, yMin, yMax);
		}

		HallPiece(StructurePieceType type, CompoundTag tag) {
			super(type, tag);
		}

		int dragon() {
			return kind % 3;
		}

		int style() {
			return kind / 3;
		}

		int forgeFloor() {
			return gateY - FORGE_DROP;
		}

		int arenaFloor() {
			return gateY - param;
		}

		/** The stair's step at a place along it: four blocks down for every five ahead, to the forge's floor. */
		int stairFloor(int v) {
			return gateY - 1 - Math.min(FORGE_DROP - 1, (int) Math.round(Math.max(0, v - STAIR_V0) * 0.8));
		}

		/** The bridge's floor over the chasm, from the forge's floor down to the arena's. */
		int bridgeFloor(int v) {
			double t = Math.max(0, Math.min(1, (v - FORGE_V1) / (double) (ARENA_V0 - FORGE_V1)));
			return forgeFloor() - (int) Math.round(t * (forgeFloor() - arenaFloor()));
		}

		/** The bottom of the chasm (lava, ice or rock). */
		int chasmBottom() {
			return arenaFloor() - 14;
		}

		Direction forward() {
			return switch (facing & 3) {
				case 0 -> Direction.SOUTH;
				case 1 -> Direction.WEST;
				case 2 -> Direction.NORTH;
				default -> Direction.EAST;
			};
		}

		Direction right() {
			return forward().getCounterClockWise();
		}

		// ---- the castle's stone (by the land) ----

		BlockState castleStone(int u, int v, int y) {
			float n = noise(u, v, y, 1);
			return switch (style()) {
				case 2 -> (n < 0.55F ? Blocks.CUT_SANDSTONE : n < 0.8F ? Blocks.SANDSTONE : n < 0.92F ? Blocks.SMOOTH_SANDSTONE : Blocks.CHISELED_SANDSTONE).defaultBlockState();
				case 1 -> (n < 0.55F ? Blocks.STONE_BRICKS : n < 0.8F ? Blocks.CRACKED_STONE_BRICKS : n < 0.9F ? Blocks.PACKED_ICE : Blocks.ANDESITE).defaultBlockState();
				default -> (n < 0.5F ? Blocks.STONE_BRICKS : n < 0.72F ? Blocks.MOSSY_STONE_BRICKS : n < 0.9F ? Blocks.CRACKED_STONE_BRICKS : Blocks.ANDESITE).defaultBlockState();
			};
		}

		BlockState castleFloor(int u, int v, int y) {
			float n = noise(u, v, y, 2);
			return switch (style()) {
				case 2 -> (n < 0.5F ? Blocks.SMOOTH_SANDSTONE : n < 0.8F ? Blocks.SAND : Blocks.CUT_SANDSTONE).defaultBlockState();
				case 1 -> (n < 0.4F ? Blocks.STONE_BRICKS : n < 0.7F ? Blocks.GRAVEL : n < 0.85F ? Blocks.PACKED_ICE : Blocks.COBBLESTONE).defaultBlockState();
				default -> (n < 0.35F ? Blocks.STONE_BRICKS : n < 0.55F ? Blocks.COBBLESTONE : n < 0.75F ? Blocks.GRAVEL : n < 0.9F ? Blocks.MOSSY_COBBLESTONE : Blocks.COARSE_DIRT).defaultBlockState();
			};
		}

		BlockState castleWood() {
			return (style() == 1 ? Blocks.SPRUCE_LOG : style() == 2 ? Blocks.STRIPPED_ACACIA_LOG : Blocks.DARK_OAK_LOG).defaultBlockState();
		}

		Block castleStairs() {
			return style() == 2 ? Blocks.SANDSTONE_STAIRS : Blocks.STONE_BRICK_STAIRS;
		}

		Block roofStairs() {
			return style() == 1 ? Blocks.SPRUCE_STAIRS : style() == 2 ? Blocks.RED_SANDSTONE_STAIRS : Blocks.DARK_OAK_STAIRS;
		}

		BlockState castleWall() {
			return (style() == 2 ? Blocks.SANDSTONE_WALL : Blocks.STONE_BRICK_WALL).defaultBlockState();
		}

		// ---- the depths' stone (by the dragon) ----

		BlockState deepWall(int u, int v, int y) {
			float n = noise(u, v, y, 3);
			return switch (dragon()) {
				case 1 -> (n < 0.55F ? Blocks.STONE_BRICKS : n < 0.75F ? Blocks.CRACKED_STONE_BRICKS : n < 0.9F ? Blocks.PACKED_ICE : Blocks.BLUE_ICE).defaultBlockState();
				case 2 -> (n < 0.55F ? Blocks.DEEPSLATE_BRICKS : n < 0.8F ? Blocks.CRACKED_DEEPSLATE_BRICKS : Blocks.DEEPSLATE_TILES).defaultBlockState();
				default -> (n < 0.5F ? Blocks.POLISHED_BLACKSTONE_BRICKS : n < 0.75F ? Blocks.CRACKED_POLISHED_BLACKSTONE_BRICKS : n < 0.93F ? Blocks.BLACKSTONE : Blocks.GILDED_BLACKSTONE).defaultBlockState();
			};
		}

		BlockState deepFloor(int u, int v, int y) {
			boolean check = (Math.floorMod(u, 4) < 2) == (Math.floorMod(v, 4) < 2);
			return switch (dragon()) {
				case 1 -> (check ? Blocks.POLISHED_DIORITE : Blocks.PACKED_ICE).defaultBlockState();
				case 2 -> (check ? Blocks.POLISHED_DEEPSLATE : Blocks.DEEPSLATE_TILES).defaultBlockState();
				default -> check ? Blocks.POLISHED_BLACKSTONE.defaultBlockState() : Blocks.POLISHED_BASALT.defaultBlockState();
			};
		}

		BlockState deepPillar() {
			return switch (dragon()) {
				case 1 -> Blocks.QUARTZ_PILLAR.defaultBlockState();
				case 2 -> Blocks.DEEPSLATE.defaultBlockState().setValue(RotatedPillarBlock.AXIS, Direction.Axis.Y);
				default -> Blocks.POLISHED_BASALT.defaultBlockState().setValue(RotatedPillarBlock.AXIS, Direction.Axis.Y);
			};
		}

		BlockState deepTrim() {
			return switch (dragon()) {
				case 1 -> Blocks.CHISELED_STONE_BRICKS.defaultBlockState();
				case 2 -> Blocks.CHISELED_DEEPSLATE.defaultBlockState();
				default -> Blocks.CHISELED_POLISHED_BLACKSTONE.defaultBlockState();
			};
		}

		BlockState deepRail() {
			return switch (dragon()) {
				case 1 -> Blocks.STONE_BRICK_WALL.defaultBlockState();
				case 2 -> Blocks.DEEPSLATE_BRICK_WALL.defaultBlockState();
				default -> Blocks.POLISHED_BLACKSTONE_BRICK_WALL.defaultBlockState();
			};
		}

		/** What runs in the forge's channels: lava, ice, water. */
		BlockState channel() {
			return switch (dragon()) {
				case 1 -> Blocks.BLUE_ICE.defaultBlockState();
				case 2 -> Blocks.WATER.defaultBlockState();
				default -> Blocks.LAVA.defaultBlockState();
			};
		}

		BlockState lantern(boolean hanging) {
			return (dragon() == 1 ? Blocks.SOUL_LANTERN : Blocks.LANTERN).defaultBlockState().setValue(LanternBlock.HANGING, hanging);
		}

		BlockState brazier() {
			return (dragon() == 1 ? Blocks.SOUL_CAMPFIRE : Blocks.CAMPFIRE).defaultBlockState().setValue(CampfireBlock.LIT, true);
		}

		BlockState chain() {
			return Blocks.IRON_CHAIN.defaultBlockState().setValue(RotatedPillarBlock.AXIS, Direction.Axis.Y);
		}

		Block treasure() {
			return switch (dragon()) {
				case 1 -> IafBlockRegistry.SILVER_BLOCK;
				case 2 -> Blocks.COPPER_BLOCK.waxed().unaffected();
				default -> Blocks.GOLD_BLOCK;
			};
		}

		BlockState pile(int layers) {
			Block pile = switch (dragon()) {
				case 1 -> IafBlockRegistry.SILVER_PILE;
				case 2 -> IafBlockRegistry.COPPER_PILE;
				default -> IafBlockRegistry.GOLD_PILE;
			};
			return pile.defaultBlockState().setValue(BlockGoldPile.LAYERS, Math.max(1, Math.min(7, layers)));
		}

		String dragonName() {
			return switch (dragon()) {
				case 1 -> "ice";
				case 2 -> "lightning";
				default -> "fire";
			};
		}

		void chest(WorldGenLevel level, BoundingBox chunkBox, RandomSource random, BlockPos pos, Direction facing, String loot) {
			if (!chunkBox.isInside(pos)) {
				return;
			}
			level.setBlock(pos, Blocks.CHEST.defaultBlockState().setValue(ChestBlock.FACING, facing), 2);
			if (level.getBlockEntity(pos) instanceof ChestBlockEntity chest) {
				chest.setLootTable(ResourceKey.create(Registries.LOOT_TABLE, Identifier.parse(loot)), random.nextLong());
			}
		}
	}

	/** The abandoned castle on the land: walls, towers, the gatehouse, the courtyard, the keep with the stairwell. */
	public static class Castle extends HallPiece {
		private static final int BLEND = 7;
		private int ground;

		public Castle(int x, int y, int z, int facing, int kind, int depth, long seed) {
			super(IafStructures.KEEP_CASTLE, x, y, z, facing, kind, depth, seed, -CASTLE_HALF - 6 - BLEND, CASTLE_HALF + 6 + BLEND, -6 - BLEND,
				CASTLE_END + 6 + BLEND, y - 8, y + 30);
		}

		public Castle(CompoundTag tag) {
			super(IafStructures.KEEP_CASTLE, tag);
		}

		@Override
		protected int plantClearing() {
			return 8;
		}

		@Override
		protected boolean keptOpen(WorldGenLevel level, int u, int v, int y) {
			// the castle itself (walls, towers, courtyard, keep) from its floor up; not the apron round it, whose rule
			// hangs on the land
			if (y < gateY) {
				return false;
			}
			int au = Math.abs(u);
			boolean tower = au >= 4 && au <= 10 && v >= -3 && v <= 3;
			for (int[] t : new int[][]{{-CASTLE_HALF, 0}, {CASTLE_HALF, 0}, {-CASTLE_HALF, CASTLE_END}, {CASTLE_HALF, CASTLE_END}}) {
				int du = u - t[0];
				int dv = v - t[1];
				tower |= du * du + dv * dv <= 31;
			}
			if (!tower && (au > CASTLE_HALF || v < 0 || v > CASTLE_END)) {
				return false;
			}
			BlockState state = at(level, u, v, y);
			return state != null && state.isAir();
		}

		@Override
		protected void column(WorldGenLevel level, int u, int v, int x, int z) {
			int top = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z) - 1;
			BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos(x, top, z);
			while (top > gateY - 16 && (level.getBlockState(pos).is(BlockTags.LOGS) || level.getBlockState(pos).canBeReplaced())) {
				pos.setY(--top);
			}
			ground = top;
		}

		@Override
		protected @Nullable BlockState at(WorldGenLevel level, int u, int v, int y) {
			int au = Math.abs(u);
			// round corner towers and the gatehouse's two square towers first: they stand out of the walls
			for (int[] t : new int[][]{{-CASTLE_HALF, 0}, {CASTLE_HALF, 0}, {-CASTLE_HALF, CASTLE_END}, {CASTLE_HALF, CASTLE_END}}) {
				int du = u - t[0];
				int dv = v - t[1];
				if (du * du + dv * dv <= 31) {
					return roundTower(du, dv, y, u, v);
				}
			}
			if (au >= 4 && au <= 10 && v >= -3 && v <= 3) {
				return gateTower(au - 7, v, y, u);
			}
			if (au > CASTLE_HALF || v < 0 || v > CASTLE_END) {
				return apron(u, v, y);
			}
			if (au >= CASTLE_HALF - 1 || v <= 1 || v >= CASTLE_END - 1) {
				return curtain(u, v, y, au);
			}
			if (au <= KEEP_HALF + 2 && v >= KEEP_V0 && v <= KEEP_V1) {
				return keep(u, v, y, au);
			}
			if (au >= 19 && au <= 28 && v >= 6 && v <= 28) {
				return ruinedHall(u, v, y, au);
			}
			return courtyard(u, v, y, au);
		}

		/** Outside the walls: a ring of rubble, then the land eased to the castle's height. */
		private @Nullable BlockState apron(int u, int v, int y) {
			int g = gateY;
			int out = Math.max(Math.max(Math.abs(u) - CASTLE_HALF, -v), v - CASTLE_END);
			if (out > BLEND + 3) {
				return null;
			}
			int target;
			if (out <= 3) {
				target = g - 1;
			} else {
				double t = (out - 3) / (double) (BLEND + 1);
				t = t * t * (3 - 2 * t);
				target = (int) Math.round((g - 1) * (1 - t) + ground * t);
			}
			if (y == target) {
				return out <= 3 && noise(u, v, y, 4) < 0.3F ? castleFloor(u, v, y) : surface();
			}
			if (y < target) {
				return y > ground && y > g - 10 ? (style() == 2 ? Blocks.SANDSTONE : Blocks.DIRT).defaultBlockState() : null;
			}
			if (y == target + 1 && out <= 3 && smooth(u, v, 3, 5) > 0.72F) {
				return castleStone(u, v, y);
			}
			return y <= Math.max(ground, g + 2) ? Blocks.AIR.defaultBlockState() : null;
		}

		private BlockState surface() {
			return (style() == 2 ? Blocks.SAND : Blocks.GRASS_BLOCK).defaultBlockState();
		}

		/** How high a broken wall still stands here (a breach now and then). */
		private int wallTop(int u, int v) {
			int top = gateY + 6 + (int) (smooth(u * 0.7 + v * 0.7, u - v, 5, 6) * 6);
			if (smooth(u + v * 1.3, v - u * 0.4, 8, 7) < 0.16F) {
				top = gateY + 1 + (int) (noise(u, v, 0, 8) * 2);
			}
			return top;
		}

		private @Nullable BlockState roundTower(int du, int dv, int y, int u, int v) {
			int g = gateY;
			double r = Math.sqrt(du * du + dv * dv);
			if (y < g - 6) {
				return null;
			}
			if (y < g) {
				return castleStone(u, v, y);
			}
			int top = g + 13 + (int) (smooth(du + u, dv + v, 3, 9) * 5);
			if (r < 3.4) {
				if (y == g + 5 || y == g + 10) {
					return noise(u, v, y, 10) < 0.25F ? Blocks.AIR.defaultBlockState() : castleWood();
				}
				return y <= g + 22 ? Blocks.AIR.defaultBlockState() : null;
			}
			if (y > top) {
				return y <= g + 22 ? Blocks.AIR.defaultBlockState() : null;
			}
			boolean slit = (y - g) % 5 == 3 && (du == 0 || dv == 0);
			if (slit || y > g + 2 && noise(u, v, y, 11) < 0.04F) {
				return Blocks.AIR.defaultBlockState();
			}
			return castleStone(u, v, y);
		}

		private @Nullable BlockState gateTower(int du, int v, int y, int u) {
			int g = gateY;
			if (y < g - 6) {
				return null;
			}
			if (y < g) {
				return castleStone(u, v, y);
			}
			boolean edge = Math.abs(du) == 3 || Math.abs(v) == 3;
			int top = g + 12 + (int) (smooth(u, v, 3, 12) * 3);
			if (!edge) {
				if (y == g + 6) {
					return castleWood();
				}
				return y <= g + 20 ? Blocks.AIR.defaultBlockState() : null;
			}
			if (y > top) {
				if (y == top + 1 && (u + v & 1) == 0 && top >= g + 13) {
					return castleWall();
				}
				return y <= g + 20 ? Blocks.AIR.defaultBlockState() : null;
			}
			if ((y - g) % 5 == 3 && (du == 0 || v == 0)) {
				return Blocks.AIR.defaultBlockState();
			}
			return castleStone(u, v, y);
		}

		/** The curtain walls, two thick and broken, with the gate (its portcullis raised) in the front. */
		private @Nullable BlockState curtain(int u, int v, int y, int au) {
			int g = gateY;
			if (y < g - 6) {
				return null;
			}
			if (y < g) {
				return castleStone(u, v, y);
			}
			if (v <= 1 && au <= 3) {
				int arch = au <= 1 ? g + 6 : au == 2 ? g + 5 : g + 4;
				if (y <= arch) {
					return Blocks.AIR.defaultBlockState();
				}
				if (y == arch + 1 && v == 1) {
					return Blocks.IRON_BARS.defaultBlockState();
				}
				return y <= g + 10 ? castleStone(u, v, y) : y <= g + 16 ? Blocks.AIR.defaultBlockState() : null;
			}
			int top = wallTop(u, v);
			if (y > top) {
				if (y == top + 1 && top >= g + 9 && (u + v & 1) == 0 && (au == CASTLE_HALF || v == 0 || v == CASTLE_END)) {
					return castleWall();
				}
				return y <= g + 16 ? Blocks.AIR.defaultBlockState() : null;
			}
			if (y > g + 2 && noise(u, v, y, 13) < 0.035F) {
				return Blocks.AIR.defaultBlockState();
			}
			return castleStone(u, v, y);
		}

		/** The courtyard: rubble, a well, two broken knight statues by the way to the keep, weeds. */
		private @Nullable BlockState courtyard(int u, int v, int y, int au) {
			int g = gateY;
			if (y < g - 6) {
				return null;
			}
			if (y < g - 1) {
				return (style() == 2 ? Blocks.SANDSTONE : Blocks.DIRT).defaultBlockState();
			}
			if (y == g - 1) {
				return au <= 2 && v < KEEP_V0 ? castleStone(u, v, y) : castleFloor(u, v, y);
			}
			int dy = y - g;
			// the well
			int wu = u + 10;
			int wv = v - 18;
			if (Math.abs(wu) <= 2 && Math.abs(wv) <= 2) {
				boolean ring = Math.abs(wu) == 2 || Math.abs(wv) == 2;
				if (dy == 0) {
					return ring ? castleStone(u, v, y) : Blocks.AIR.defaultBlockState();
				}
				if (Math.abs(wu) == 2 && Math.abs(wv) == 2 && dy <= 3) {
					return castleWood();
				}
				if (dy == 4 && wu == 0) {
					return castleWood().setValue(RotatedPillarBlock.AXIS, Direction.Axis.Z);
				}
			}
			// two knights of stone by the way, one broken
			for (int side : new int[]{-1, 1}) {
				if (u == side * 5 && v == 27) {
					if (dy == 0) {
						return Blocks.CHISELED_STONE_BRICKS.defaultBlockState();
					}
					if (dy == 1 && side < 0) {
						return castleStone(u, v, y);
					}
					if (dy == (side < 0 ? 2 : 1)) {
						return Blocks.SKELETON_SKULL.defaultBlockState().setValue(SkullBlock.ROTATION, side < 0 ? 4 : 12);
					}
				}
			}
			// rubble heaps
			float heap = smooth(u, v, 3, 14);
			if (au > 3 && heap > 0.7F && dy <= (heap > 0.82F ? 1 : 0)) {
				return castleStone(u, v, y);
			}
			if (dy == 0 && au > 3) {
				float n = noise(u, v, y, 15);
				if (n < 0.04F) {
					return Blocks.BONE_BLOCK.defaultBlockState();
				}
				if (n < 0.18F) {
					return switch (style()) {
						case 2 -> Blocks.DEAD_BUSH.defaultBlockState();
						case 1 -> Blocks.AIR.defaultBlockState();
						default -> n < 0.12F ? Blocks.SHORT_GRASS.defaultBlockState() : Blocks.FERN.defaultBlockState();
					};
				}
			}
			return y <= g + 18 ? Blocks.AIR.defaultBlockState() : null;
		}

		/** Ruined halls along the side walls: low broken walls, no roof, rubble and hay inside. */
		private @Nullable BlockState ruinedHall(int u, int v, int y, int au) {
			int g = gateY;
			if (y < g - 1) {
				return y < g - 6 ? null : castleStone(u, v, y);
			}
			if (y == g - 1) {
				return castleFloor(u, v, y);
			}
			boolean wall = au == 19 || au == 28 || v == 6 || v == 28;
			int top = g + 1 + (int) (smooth(u, v, 3, 16) * 5);
			if (wall) {
				if (au == 19 && Math.abs(v - 17) <= 1 && y <= g + 2) {
					return Blocks.AIR.defaultBlockState();
				}
				return y <= top ? castleStone(u, v, y) : y <= g + 16 ? Blocks.AIR.defaultBlockState() : null;
			}
			if (y == g) {
				float n = noise(u, v, y, 17);
				if (n < 0.08F) {
					return Blocks.HAY_BLOCK.defaultBlockState();
				}
				if (n < 0.2F) {
					return castleStone(u, v, y);
				}
			}
			return y <= g + 16 ? Blocks.AIR.defaultBlockState() : null;
		}

		/** The keep: a great hall, its roof fallen in, pillars, the stairwell going down, the throne at the back. */
		private @Nullable BlockState keep(int u, int v, int y, int au) {
			int g = gateY;
			if (y < g - 6) {
				return null;
			}
			boolean stairwell = au <= 4 && v >= STAIR_V0 && v <= STAIR_OPEN_V1;
			if (y < g) {
				if (stairwell) {
					return null;
				}
				return y == g - 1 ? keepFloor(u, v, y) : castleStone(u, v, y);
			}
			int dy = y - g;
			// buttresses outside the side walls
			if (au > KEEP_HALF) {
				boolean buttress = (v - KEEP_V0) % 6 == 4 && dy <= 13 - (au - KEEP_HALF - 1) * 4;
				return buttress ? castleStone(u, v, y) : y <= g + 26 ? Blocks.AIR.defaultBlockState() : null;
			}
			boolean side = au >= KEEP_HALF - 1;
			boolean front = v <= KEEP_V0 + 1;
			boolean back = v >= KEEP_V1 - 1;
			int gable = g + 16 + (int) Math.floor((KEEP_HALF - au) * 0.55);
			// the roof: a gable along the hall, mostly fallen in; rafters remain
			if (dy > 16 && !front && !back) {
				if (y == gable) {
					if (smooth(u, v, 5, 18) > 0.47F) {
						return roofStairs().defaultBlockState().setValue(StairBlock.FACING, u < 0 ? right() : right().getOpposite())
							.setValue(StairBlock.HALF, Half.BOTTOM);
					}
					return (v - KEEP_V0) % 4 == 2 ? castleWood().setValue(RotatedPillarBlock.AXIS, Direction.Axis.X) : Blocks.AIR.defaultBlockState();
				}
				return y <= g + 26 ? Blocks.AIR.defaultBlockState() : null;
			}
			if (side || front || back) {
				int top = front || back ? gable : g + 16;
				if (y > top) {
					return y <= g + 26 ? Blocks.AIR.defaultBlockState() : null;
				}
				// the great door
				if (front && au <= 2 && y <= (au <= 1 ? g + 7 : g + 6)) {
					return Blocks.AIR.defaultBlockState();
				}
				// the rose window over the door
				if (front) {
					double rr = Math.sqrt(u * u + (dy - 11) * (dy - 11));
					if (rr <= 3.2) {
						if (v == KEEP_V0 + 1) {
							return Blocks.AIR.defaultBlockState();
						}
						return u == 0 || dy == 11 ? castleStone(u, v, y) : glass();
					}
				}
				// tall windows in the side walls, some panes broken
				if (side && (v - KEEP_V0) % 6 == 1 && dy >= 5 && dy <= 12) {
					return au == KEEP_HALF && noise(u, v, y, 19) < 0.6F ? glass() : Blocks.AIR.defaultBlockState();
				}
				if (dy > 3 && noise(u, v, y, 20) < 0.03F) {
					return Blocks.AIR.defaultBlockState();
				}
				return castleStone(u, v, y);
			}
			// inside: the stairwell with a balustrade round it (open toward the door)
			if (stairwell) {
				if (dy == 0 && (au == 4 || v == STAIR_OPEN_V1) && v > STAIR_V0) {
					return castleWall();
				}
				return y <= g + 26 ? Blocks.AIR.defaultBlockState() : null;
			}
			// the throne on its dais
			if (v >= KEEP_V1 - 4 && au <= 3) {
				if (dy == 0) {
					return Blocks.POLISHED_ANDESITE.defaultBlockState();
				}
				if (dy == 1 && u == 0 && v == KEEP_V1 - 3) {
					return castleStairs().defaultBlockState().setValue(StairBlock.FACING, forward()).setValue(StairBlock.HALF, Half.BOTTOM);
				}
				if (dy == 1 && au == 1 && v == KEEP_V1 - 3) {
					return castleWall();
				}
			}
			// pillars, some broken
			if (au == 9 && (v - KEEP_V0) % 6 == 4) {
				int top = g + 15 - (noise(u, v, 0, 21) < 0.35F ? 4 + (int) (noise(u, v, 1, 21) * 8) : 0);
				if (y <= top) {
					return dy == 0 || y == top ? Blocks.CHISELED_STONE_BRICKS.defaultBlockState() : castleStone(u, v, y);
				}
			}
			// chandeliers
			if (au == 6 && (v == 44 || v == 52)) {
				if (dy == 11) {
					return Blocks.LANTERN.defaultBlockState().setValue(LanternBlock.HANGING, true);
				}
				if (dy > 11 && dy <= 15) {
					return chain();
				}
			}
			// rubble from the roof, and what is left of a red runner
			if (dy == 0 && smooth(u, v, 3, 22) > 0.74F && au > 4) {
				return castleStone(u, v, y);
			}
			if (dy == 0 && au <= 1 && v < STAIR_V0 && noise(u, v, y, 23) < 0.7F) {
				return Blocks.CARPET.red().defaultBlockState();
			}
			return y <= g + 26 ? Blocks.AIR.defaultBlockState() : null;
		}

		private BlockState keepFloor(int u, int v, int y) {
			return (u + v & 1) == 0 ? Blocks.POLISHED_ANDESITE.defaultBlockState() : castleFloor(u, v, y);
		}

		private BlockState glass() {
			return (switch (dragon()) {
				case 1 -> Blocks.STAINED_GLASS_PANE.lightBlue();
				case 2 -> Blocks.STAINED_GLASS_PANE.purple();
				default -> Blocks.STAINED_GLASS_PANE.orange();
			}).defaultBlockState();
		}

		@Override
		protected void decorate(WorldGenLevel level, BoundingBox chunkBox, RandomSource random) {
			int g = gateY;
			Direction back = forward().getOpposite();
			for (int u : new int[]{-3, 3}) {
				BlockPos pos = world(u, KEEP_V1 - 2, g + 4);
				if (chunkBox.isInside(pos)) {
					level.setBlock(pos, (dragon() == 1 ? Blocks.WALL_BANNER.lightBlue() : dragon() == 2 ? Blocks.WALL_BANNER.purple() : Blocks.WALL_BANNER.red())
						.defaultBlockState().setValue(WallBannerBlock.FACING, back), 2);
				}
			}
			chest(level, chunkBox, random, world(-3, KEEP_V1 - 4, g + 1), back, "minecraft:chests/simple_dungeon");
		}
	}

	/** The wide stair from the keep down to the forge hall. */
	public static class Stair extends HallPiece {
		public Stair(int x, int y, int z, int facing, int kind, int depth, long seed) {
			super(IafStructures.KEEP_STAIR, x, y, z, facing, kind, depth, seed, -6, 6, STAIR_V0 - 1, FORGE_V0 + 1, y - FORGE_DROP - 3, y + 1);
		}

		public Stair(CompoundTag tag) {
			super(IafStructures.KEEP_STAIR, tag);
		}

		@Override
		protected @Nullable BlockState at(WorldGenLevel level, int u, int v, int y) {
			int au = Math.abs(u);
			if (au > 5 || v < STAIR_V0 || v > FORGE_V0) {
				return null;
			}
			int f = stairFloor(v);
			boolean open = v <= STAIR_OPEN_V1;
			int top = open ? gateY - 1 : f + 8;
			if (y < f - 1 || y > top) {
				return null;
			}
			boolean deep = v > STAIR_OPEN_V1 + 3;
			if (au >= 4) {
				if (au == 4 && v % 6 == 0 && y == f + 3 && !open) {
					return deepTrim();
				}
				return deep ? deepWall(u, v, y) : castleStone(u, v, y);
			}
			if (y <= f) {
				return deep ? deepFloor(u, v, y) : castleStone(u, v, y);
			}
			if (!open && y >= f + 7) {
				return au == 3 && y == f + 7 ? deepTrim() : deep ? deepWall(u, v, y) : castleStone(u, v, y);
			}
			if (!open && au == 2 && v % 8 == 4) {
				if (y == f + 5) {
					return lantern(true);
				}
				if (y == f + 6) {
					return chain();
				}
			}
			return Blocks.CAVE_AIR.defaultBlockState();
		}
	}

	/** The forge hall: channels of lava (or ice, or water), pillars, forges, and side rooms through passages. */
	public static class Forge extends HallPiece {
		public Forge(int x, int y, int z, int facing, int kind, int depth, long seed) {
			super(IafStructures.KEEP_FORGE, x, y, z, facing, kind, depth, seed, -26, 26, FORGE_V0 - 3, FORGE_V1 + 3, y - FORGE_DROP - 3, y - FORGE_DROP + 15);
		}

		public Forge(CompoundTag tag) {
			super(IafStructures.KEEP_FORGE, tag);
		}

		private int ceiling(int u) {
			return forgeFloor() + 12 - Math.max(0, Math.abs(u) - 6);
		}

		/** The kind of a side room (0 store, 1 library, 2 prison, 3 barracks), by the building's seed. */
		private int room(int side, int index) {
			return (int) Math.floorMod((long) (noise(side, index, 0, 30) * 1000) + side * 2L + index, 4L);
		}

		private boolean open(int u, int v, int y) {
			int f = forgeFloor();
			int au = Math.abs(u);
			if (y <= f) {
				return false;
			}
			if (au <= 11 && v >= FORGE_V0 && v <= FORGE_V1) {
				return y <= ceiling(u);
			}
			// the way out to the bridge
			if (au <= 3 && v > FORGE_V1 && v <= FORGE_V1 + 3) {
				return y <= f + 8;
			}
			for (int rv : ROOMS_V) {
				if (au >= 12 && au <= 14 && Math.abs(v - rv) <= 1) {
					return y <= f + 3;
				}
				if (au >= 15 && au <= 23 && Math.abs(v - rv) <= 4) {
					return y <= f + 5;
				}
			}
			return false;
		}

		@Override
		protected @Nullable BlockState at(WorldGenLevel level, int u, int v, int y) {
			int f = forgeFloor();
			if (y < f - 2 || y > f + 15) {
				return null;
			}
			// where the stair comes in, the stair is the one to build
			if (Math.abs(u) <= 5 && v < FORGE_V0) {
				return null;
			}
			if (open(u, v, y)) {
				return inside(u, v, y);
			}
			if (open(u, v, y + 1)) {
				return floor(u, v, y);
			}
			for (int d = 1; d <= 2; d++) {
				if (open(u + d, v, y) || open(u - d, v, y) || open(u, v + d, y) || open(u, v - d, y) || open(u, v, y - d) || open(u, v, y + d)) {
					return deepWall(u, v, y);
				}
			}
			return null;
		}

		/** The ways across the channels: to the side rooms and in the middle. */
		private static boolean crossing(int v) {
			for (int rv : ROOMS_V) {
				if (Math.abs(v - rv) <= 1) {
					return true;
				}
			}
			return Math.abs(v - 82) <= 1;
		}

		private BlockState floor(int u, int v, int y) {
			int au = Math.abs(u);
			boolean hall = au <= 11 && v >= FORGE_V0 && v <= FORGE_V1;
			if (hall && (au == 4 || au == 5) && v > FORGE_V0 + 1 && v < FORGE_V1 - 1 && !crossing(v)) {
				return channel();
			}
			if (hall && au <= 1) {
				return v % 2 == 0 ? deepTrim() : deepFloor(u, v, y);
			}
			return deepFloor(u, v, y);
		}

		private BlockState inside(int u, int v, int y) {
			int f = forgeFloor();
			int au = Math.abs(u);
			int dy = y - f;
			boolean hall = au <= 11 && v >= FORGE_V0 && v <= FORGE_V1;
			if (!hall) {
				if (au >= 15 && au <= 23) {
					return roomInside(u, v, y, au, dy);
				}
				return Blocks.CAVE_AIR.defaultBlockState();
			}
			// railings along the channels
			if (au == 3 && dy == 1 && v > FORGE_V0 + 1 && v < FORGE_V1 - 1 && !crossing(v)) {
				return Blocks.IRON_BARS.defaultBlockState();
			}
			// pillars with capitals
			if (au == 7 && (v - FORGE_V0) % 6 == 4) {
				return dy == 9 || dy == 1 ? deepTrim() : deepPillar();
			}
			// lanterns on chains over the aisle
			if (au == 2 && (v - FORGE_V0) % 8 == 2) {
				if (dy == 8) {
					return lantern(true);
				}
				if (dy > 8) {
					return chain();
				}
			}
			// forges against the walls, between the pillars
			if (au >= 9 && dy <= 2) {
				BlockState forge = forge(au, Math.floorMod(v - FORGE_V0, 12), dy);
				if (forge != null) {
					return forge;
				}
			}
			// braziers by the pillars
			if (au == 8 && (v - FORGE_V0) % 6 == 4 && dy == 1) {
				return brazier();
			}
			return Blocks.CAVE_AIR.defaultBlockState();
		}

		/** A forge station along a wall: furnaces, a cauldron of lava (or snow), an anvil, a grindstone, a table. */
		private @Nullable BlockState forge(int au, int at, int dy) {
			if (dy == 1) {
				if (au == 11 && (at == 6 || at == 8)) {
					return Blocks.BLAST_FURNACE.defaultBlockState();
				}
				if (au == 11 && at == 7) {
					return dragon() == 1 ? Blocks.POWDER_SNOW_CAULDRON.defaultBlockState() : Blocks.LAVA_CAULDRON.defaultBlockState();
				}
				if (au == 9 && at == 7) {
					return Blocks.ANVIL.defaultBlockState();
				}
				if (au == 10 && at == 10) {
					return Blocks.GRINDSTONE.defaultBlockState();
				}
				if (au == 11 && at == 10) {
					return Blocks.SMITHING_TABLE.defaultBlockState();
				}
				if (au == 11 && (at == 1 || at == 2)) {
					return Blocks.BARREL.defaultBlockState();
				}
			}
			if (dy == 2) {
				if (au == 11 && at == 7) {
					return Blocks.IRON_BARS.defaultBlockState();
				}
				if (au == 11 && at == 1) {
					return Blocks.BARREL.defaultBlockState();
				}
			}
			return null;
		}

		/** A side room: what it holds by its kind. */
		private BlockState roomInside(int u, int v, int y, int au, int dy) {
			int side = u < 0 ? 0 : 1;
			int index = Math.abs(v - ROOMS_V[0]) <= 4 ? 0 : 1;
			int dv = v - ROOMS_V[index];
			if (au == 19 && dv == 0) {
				if (dy == 4) {
					return lantern(true);
				}
				if (dy == 5) {
					return chain();
				}
			}
			switch (room(side, index)) {
				case 1 -> {
					// a library: shelves along the walls, a lectern, candles
					if ((au == 23 || Math.abs(dv) == 4 && au > 16) && dy <= 3) {
						return Blocks.BOOKSHELF.defaultBlockState();
					}
					if (dy == 1 && au == 19 && dv == 2) {
						return Blocks.LECTERN.defaultBlockState();
					}
					if (dy == 1 && au == 21 && Math.abs(dv) == 2) {
						return Blocks.CANDLE.defaultBlockState().setValue(CandleBlock.CANDLES, 3).setValue(CandleBlock.LIT, true);
					}
				}
				case 2 -> {
					// a prison: cells of iron bars, bones and skulls
					if (au == 19 && dy <= 3 && dv != 0) {
						return Blocks.IRON_BARS.defaultBlockState();
					}
					if (au >= 20 && dv == 0 && dy <= 3) {
						return Blocks.IRON_BARS.defaultBlockState();
					}
					if (dy == 1 && au == 22 && Math.abs(dv) == 3) {
						return Blocks.SKELETON_SKULL.defaultBlockState();
					}
					if (dy == 1 && au == 21 && Math.abs(dv) == 2) {
						return Blocks.BONE_BLOCK.defaultBlockState();
					}
				}
				case 3 -> {
					// barracks: beds (set when decorating) and barrels
					if (dy == 1 && au == 23 && Math.abs(dv) <= 1) {
						return Blocks.BARREL.defaultBlockState();
					}
				}
				default -> {
					// a store: barrels and hay
					if ((au == 23 || Math.abs(dv) == 4) && dy <= 2 && noise(u, v, y, 31) < 0.6F) {
						return noise(u, v, y, 32) < 0.7F ? Blocks.BARREL.defaultBlockState() : Blocks.HAY_BLOCK.defaultBlockState();
					}
				}
			}
			return Blocks.CAVE_AIR.defaultBlockState();
		}

		@Override
		protected void decorate(WorldGenLevel level, BoundingBox chunkBox, RandomSource random) {
			int f = forgeFloor();
			for (int side = 0; side < 2; side++) {
				int s = side == 0 ? -1 : 1;
				Direction toHall = s < 0 ? right() : right().getOpposite();
				for (int index = 0; index < ROOMS_V.length; index++) {
					int rv = ROOMS_V[index];
					int type = room(side, index);
					String loot = switch (type) {
						case 1 -> "minecraft:chests/stronghold_library";
						case 2, 3 -> "minecraft:chests/simple_dungeon";
						default -> "minecraft:chests/abandoned_mineshaft";
					};
					chest(level, chunkBox, random, world(s * 21, rv - 3, f + 1), toHall, loot);
					if (type == 3) {
						for (int k = 0; k < 3; k++) {
							BlockPos foot = world(s * (17 + k * 2), rv + 2, f + 1);
							BlockPos head = foot.relative(forward());
							if (chunkBox.isInside(foot) && chunkBox.isInside(head)) {
								BlockState bed = Blocks.BED.red().defaultBlockState().setValue(BedBlock.FACING, forward());
								level.setBlock(foot, bed.setValue(BedBlock.PART, BedPart.FOOT), 2);
								level.setBlock(head, bed.setValue(BedBlock.PART, BedPart.HEAD), 2);
							}
						}
					}
				}
			}
		}
	}

	/** The great descent: a bridge going down over a deep chasm (lava, ice or rock at its bottom), pillars, falls. */
	public static class Descent extends HallPiece {
		public Descent(int x, int y, int z, int facing, int kind, int depth, long seed) {
			super(IafStructures.KEEP_DESCENT, x, y, z, facing, kind, depth, seed, -CHASM_HALF - 2, CHASM_HALF + 2, FORGE_V1 + 1, ARENA_V0 - 1, y - depth - 16,
				y - FORGE_DROP + CHASM_ROOF + 3);
		}

		public Descent(CompoundTag tag) {
			super(IafStructures.KEEP_DESCENT, tag);
		}

		private int ceiling(int v) {
			return bridgeFloor(v) + CHASM_ROOF;
		}

		/** Where the falls and the crystal columns come down the side walls. */
		private static boolean fallAt(int v) {
			return v == 114 || v == 128 || v == 142;
		}

		@Override
		protected @Nullable BlockState at(WorldGenLevel level, int u, int v, int y) {
			int au = Math.abs(u);
			int b = bridgeFloor(v);
			int bottom = chasmBottom();
			int top = ceiling(v);
			if (y < bottom - 1 || y > top + 2 || v <= FORGE_V1 || v >= ARENA_V0) {
				return null;
			}
			if (au >= CHASM_HALF || y > top || y == bottom - 1) {
				// the walls and the vault, lava falls from the side walls (for fire)
				if (au == CHASM_HALF && fallAt(v) && y == top - 1 && dragon() == 0) {
					return Blocks.LAVA.defaultBlockState();
				}
				return au > CHASM_HALF + 1 && y <= top ? null : deepWall(u, v, y);
			}
			// frozen falls (ice) or crystal columns (lightning) on the side walls
			if (au == CHASM_HALF - 1 && fallAt(v) && dragon() != 0) {
				return dragon() == 1 ? (noise(u, v, y, 40) < 0.5F ? Blocks.BLUE_ICE : Blocks.PACKED_ICE).defaultBlockState() : Blocks.AMETHYST_BLOCK.defaultBlockState();
			}
			// the bridge, three thick, with a low wall and lamp posts
			if (au <= 2 && y <= b && y >= b - 2) {
				return y == b ? deepFloor(u, v, y) : deepWall(u, v, y);
			}
			if (au == 3 && y == b + 1) {
				return deepRail();
			}
			if (au == 3 && y == b + 2 && v % 6 == 0) {
				return lantern(false);
			}
			// pillars rising from the chasm, two rows a side
			if ((Math.abs(au - 10) <= 1 || Math.abs(au - 18) <= 1) && Math.floorMod(v - 108, 12) <= 2 && v >= 106 && v <= 144) {
				return y >= top - 1 ? deepTrim() : deepPillar();
			}
			// lanterns hanging over the chasm
			if ((au == 7 || au == 14) && v % 9 == 0) {
				if (y == top - 4) {
					return lantern(true);
				}
				if (y > top - 4) {
					return chain();
				}
			}
			if (y == bottom) {
				return switch (dragon()) {
					case 1 -> (noise(u, v, y, 42) < 0.08F ? Blocks.SEA_LANTERN : noise(u, v, y, 42) < 0.6F ? Blocks.ICE : Blocks.PACKED_ICE).defaultBlockState();
					case 2 -> (noise(u, v, y, 42) < 0.08F ? Blocks.OCHRE_FROGLIGHT : noise(u, v, y, 42) < 0.7F ? Blocks.SMOOTH_BASALT : Blocks.AMETHYST_BLOCK).defaultBlockState();
					default -> Blocks.LAVA.defaultBlockState();
				};
			}
			if (y == bottom + 1 && dragon() == 2 && noise(u, v, y, 41) < 0.08F) {
				return Blocks.AMETHYST_CLUSTER.defaultBlockState();
			}
			return Blocks.CAVE_AIR.defaultBlockState();
		}

		@Override
		protected void decorate(WorldGenLevel level, BoundingBox chunkBox, RandomSource random) {
			// the lava falls from the side walls flow down into the chasm
			if (dragon() == 0) {
				for (int side : new int[]{-CHASM_HALF, CHASM_HALF}) {
					for (int v : new int[]{114, 128, 142}) {
						BlockPos fall = world(side, v, ceiling(v) - 1);
						if (chunkBox.isInside(fall)) {
							level.scheduleTick(fall, net.minecraft.world.level.material.Fluids.LAVA, 1);
						}
					}
				}
			}
		}
	}

	/** The arena: a huge walled hall with galleries, buttresses and braziers; the hoard on a dais at its end. */
	public static class Arena extends HallPiece {
		public Arena(int x, int y, int z, int facing, int kind, int depth, long seed) {
			super(IafStructures.KEEP_ARENA, x, y, z, facing, kind, depth, seed, -ARENA_HALF - 4, ARENA_HALF + 4, ARENA_V0 - 2, ARENA_V1 + 3,
				y - depth - 16, y - depth + VAULT_TOP + 4);
		}

		public Arena(CompoundTag tag) {
			super(IafStructures.KEEP_ARENA, tag);
		}

		private int vault(int u) {
			double t = Math.min(1, Math.abs(u) / (double) (ARENA_HALF + 1));
			return arenaFloor() + VAULT_SIDE + (int) Math.floor((VAULT_TOP - VAULT_SIDE) * Math.sqrt(1 - t * t));
		}

		/** The hoard's top at a place (Integer.MIN_VALUE where there is none). */
		int hoardTop(int u, int v) {
			int dv = v - HOARD_V;
			double r = Math.sqrt(u * u + dv * dv);
			if (r > 17) {
				return Integer.MIN_VALUE;
			}
			double h = 12 * (1 - (r / 17) * (r / 17)) + (smooth(u, v, 3, 50) - 0.5) * 2;
			return arenaFloor() + 3 + (int) Math.round(Math.max(0, h));
		}

		/** The lights in the vault: glowstone (fire), sea lanterns (ice), froglights (lightning). */
		private BlockState vaultLight() {
			return switch (dragon()) {
				case 1 -> Blocks.SEA_LANTERN.defaultBlockState();
				case 2 -> Blocks.OCHRE_FROGLIGHT.defaultBlockState();
				default -> Blocks.GLOWSTONE.defaultBlockState();
			};
		}

		private static int dais(double r) {
			return r <= 18 ? 3 : r <= 21.5 ? 2 : r <= 25 ? 1 : 0;
		}

		@Override
		protected @Nullable BlockState at(WorldGenLevel level, int u, int v, int y) {
			int a = arenaFloor();
			int au = Math.abs(u);
			if (y < a - 14) {
				return null;
			}
			// the front wall, down to the chasm's bottom, with the gate
			if (v <= ARENA_V0) {
				if (au > ARENA_HALF + 3 || y < a - 15 || y > vault(u) + 2) {
					return null;
				}
				if (au <= 3 && y > a && y <= a + (au <= 1 ? 8 : au == 2 ? 7 : 6)) {
					return Blocks.CAVE_AIR.defaultBlockState();
				}
				if (au <= 2 && y == a) {
					return deepFloor(u, v, y);
				}
				return au <= 3 && y == a + 9 ? deepTrim() : deepWall(u, v, y);
			}
			if (y < a - 3) {
				return null;
			}
			int top = vault(u);
			if (au > ARENA_HALF || v >= ARENA_V1 || y > top) {
				if (y > top + 2 || au > ARENA_HALF + 3 || v > ARENA_V1 + 2) {
					return null;
				}
				// lights set in the vault
				if (y == top + 1 && au <= ARENA_HALF && v < ARENA_V1 && Math.floorMod(u, 5) == 2 && Math.floorMod(v, 5) == 0) {
					return vaultLight();
				}
				// the back wall: lava falls into a trough (for fire)
				if (v == ARENA_V1 && au % 12 == 6 && y == a + 20 && dragon() == 0) {
					return Blocks.LAVA.defaultBlockState();
				}
				return deepWall(u, v, y);
			}
			if (y < a) {
				return deepWall(u, v, y);
			}
			int dv = v - HOARD_V;
			double r = Math.sqrt(u * u + dv * dv);
			// the dais and the hoard on it
			int steps = dais(r);
			if (steps > 0 && y <= a + steps) {
				return y == a + steps ? (steps == 3 ? Blocks.CHISELED_STONE_BRICKS.defaultBlockState() : deepTrim()) : deepWall(u, v, y);
			}
			int hoard = hoardTop(u, v);
			if (y > a + 3 && y <= hoard) {
				float n = noise(u, v, y, 51);
				if (y == hoard && n < 0.03F) {
					return Blocks.EMERALD_BLOCK.defaultBlockState();
				}
				if (y == hoard && n < 0.05F) {
					return Blocks.DIAMOND_BLOCK.defaultBlockState();
				}
				return treasure().defaultBlockState();
			}
			if (hoard != Integer.MIN_VALUE && y == hoard + 1) {
				return pile(2 + (int) (noise(u, v, y, 52) * 6));
			}
			if (y == a) {
				return floor(u, v, y, r);
			}
			int dy = y - a;
			// a brazier's pedestal (coins never take its place: the brazier was left floating over them)
			boolean pedestal = Math.abs(r - 30) < 0.5 && Math.floorMod(u + v * 3, 7) == 0;
			// coins spilled round the dais
			if (dy == 1 && !pedestal && r > 25 && r < 34 && noise(u, v, y, 53) < 0.18F) {
				return pile(1 + (int) (noise(u, v, y, 54) * 3));
			}
			// the galleries along the sides, a stair up from each front corner
			int gallery = ARENA_HALF - 2;
			if (au >= gallery) {
				int ramp = a + Math.min(16, Math.max(0, v - (ARENA_V0 + 3)));
				if (v <= ARENA_V0 + 19 && y <= ramp) {
					return deepWall(u, v, y);
				}
				if (y == a + 16) {
					return deepFloor(u, v, y);
				}
				if (y == a + 15) {
					return deepTrim();
				}
				if (y == a + 17 && au == gallery && v % 8 == 1 && v > ARENA_V0 + 19) {
					return brazier();
				}
			}
			if (au == gallery - 1 && v > ARENA_V0 + 19) {
				if (y == a + 16) {
					return deepFloor(u, v, y);
				}
				if (y == a + 17) {
					return deepRail();
				}
			}
			// buttresses on the walls
			if (au >= ARENA_HALF - 1 && (v - ARENA_V0) % 8 == 5) {
				return deepPillar();
			}
			// broken pillars for cover, and for a dragon to fly round
			for (int[] p : new int[][]{{-28, 172}, {28, 172}, {-34, 194}, {34, 194}, {-14, 183}, {14, 183}, {-30, 224}, {30, 224}}) {
				if (Math.abs(u - p[0]) <= 2 && Math.abs(v - p[1]) <= 2) {
					int h = 6 + (int) (noise(p[0], p[1], 0, 55) * 12);
					if (dy <= h) {
						return dy == h ? deepTrim() : deepPillar();
					}
				}
			}
			// chandeliers
			for (int[] c : new int[][]{{-17, 169}, {17, 169}, {-20, 188}, {20, 188}, {0, 161}, {-31, 213}, {31, 213}, {-22, 230}, {22, 230}}) {
				if (u == c[0] && v == c[1]) {
					if (dy == 20) {
						return lantern(true);
					}
					if (dy > 20) {
						return chain();
					}
				}
			}
			// braziers on pedestals round the dais
			if (pedestal && dy <= 2) {
				return dy == 2 ? brazier() : deepTrim();
			}
			// lanterns under the galleries
			if (au == gallery && y == a + 14 && v % 8 == 5 && v > ARENA_V0 + 19) {
				return lantern(true);
			}
			// the trough at the back, behind bars
			if (v >= ARENA_V1 - 2 && dy == 1 && v == ARENA_V1 - 2) {
				return Blocks.IRON_BARS.defaultBlockState();
			}
			if (dy == 1 && r > 20 && noise(u, v, y, 56) < 0.01F) {
				return Blocks.BONE_BLOCK.defaultBlockState();
			}
			if (dy == 1 && r > 20 && noise(u, v, y, 57) < 0.004F) {
				return Blocks.SKELETON_SKULL.defaultBlockState().setValue(SkullBlock.ROTATION, (int) (noise(u, v, y, 58) * 16));
			}
			return Blocks.CAVE_AIR.defaultBlockState();
		}

		private BlockState floor(int u, int v, int y, double r) {
			if (v >= ARENA_V1 - 2) {
				// the trough
				return switch (dragon()) {
					case 1 -> Blocks.BLUE_ICE.defaultBlockState();
					case 2 -> Blocks.WATER.defaultBlockState();
					default -> Blocks.LAVA.defaultBlockState();
				};
			}
			// rings round the dais, rays from it, the dragon's stone between
			if (r > 27 && r < 28.2 || r > 36 && r < 37) {
				return deepTrim();
			}
			if (r < 40 && (u == 0 || v == HOARD_V)) {
				return deepTrim();
			}
			if (dragon() == 0 && smooth(u, v, 4, 59) > 0.78F) {
				return Blocks.MAGMA_BLOCK.defaultBlockState();
			}
			if (dragon() == 1 && smooth(u, v, 4, 59) > 0.74F) {
				return Blocks.BLUE_ICE.defaultBlockState();
			}
			if (dragon() == 2 && smooth(u, v, 4, 59) > 0.78F) {
				return Blocks.AMETHYST_BLOCK.defaultBlockState();
			}
			return deepFloor(u, v, y);
		}

		@Override
		protected void decorate(WorldGenLevel level, BoundingBox chunkBox, RandomSource random) {
			// chests in the hoard, half buried
			for (int i = 0; i < 7; i++) {
				double angle = Math.PI * 2 * i / 7 + 0.4;
				double reach = 5 + (i % 3) * 3.5;
				int u = (int) Math.round(Math.sin(angle) * reach);
				int v = HOARD_V + (int) Math.round(Math.cos(angle) * reach);
				BlockPos pos = world(u, v, hoardTop(u, v));
				chest(level, chunkBox, random, pos, Direction.from2DDataValue(i % 4),
					"iceandfire_fabric:chest/" + dragonName() + "_dragon_" + (i % 2 == 0 ? "male" : "female") + "_cave");
				if (chunkBox.isInside(pos.above())) {
					level.setBlock(pos.above(), Blocks.CAVE_AIR.defaultBlockState(), 2);
				}
			}
			// the lava falls at the back flow down into their trough
			if (dragon() == 0) {
				for (int u = -ARENA_HALF + 2; u <= ARENA_HALF - 2; u++) {
					if (Math.floorMod(Math.abs(u), 12) == 6) {
						BlockPos fall = world(u, ARENA_V1, arenaFloor() + 20);
						if (chunkBox.isInside(fall)) {
							level.scheduleTick(fall, net.minecraft.world.level.material.Fluids.LAVA, 1);
						}
					}
				}
			}
			// the dragon asleep on its hoard
			BlockPos summit = world(0, HOARD_V, hoardTop(0, HOARD_V) + 2);
			if (chunkBox.isInside(summit)) {
				EntityType<? extends EntityDragonBase> type = switch (dragon()) {
					case 1 -> IafEntityRegistry.ICE_DRAGON;
					case 2 -> IafEntityRegistry.LIGHTNING_DRAGON;
					default -> IafEntityRegistry.FIRE_DRAGON;
				};
				EntityDragonBase dragon = type.create(level.getLevel(), EntitySpawnReason.STRUCTURE);
				if (dragon != null) {
					dragon.setAgeInDays(0);
					dragon.growDragon(100 + random.nextInt(25));
					dragon.setHealth(dragon.getMaxHealth());
					dragon.setVariant(random.nextInt(4));
					dragon.setGender(true);
					dragon.absSnapTo(summit.getX() + 0.5, summit.getY(), summit.getZ() + 0.5, forward().getOpposite().toYRot(), 0);
					dragon.setInSittingPose(true);
					dragon.setPersistenceRequired();
					dragon.homePos = new HomePosition(summit, level.getLevel());
					dragon.hasHomePosition = true;
					dragon.setHunger(50);
					level.addFreshEntity(dragon);
				}
			}
		}
	}

	/** A part of the old dragon halls (saved in worlds made before): it makes nothing now. */
	public static class Legacy extends BuiltPiece {
		public Legacy(StructurePieceType type, CompoundTag tag) {
			super(type, tag);
		}

		@Override
		protected @Nullable BlockState at(WorldGenLevel level, int u, int v, int y) {
			return null;
		}
	}
}

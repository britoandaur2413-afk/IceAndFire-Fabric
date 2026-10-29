package com.iafabric.world.structure;

import com.iafabric.block.BlockGoldPile;
import com.iafabric.block.IafBlockRegistry;
import com.iafabric.entity.EntityDragonBase;
import com.iafabric.entity.IafEntityRegistry;
import com.iafabric.world.gen.DragonNests;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceType;
import org.jetbrains.annotations.Nullable;

/**
 * Where the wild dragons live. A roost: a young dragon's nest on high ground, a bowl scorched (or frozen, or struck
 * by lightning) into the hilltop with a rim of rocks and bones, a mound of gold (silver, copper) and chests in its
 * middle, the land around burnt bare. A cave: a stage 4 dragons' den under the land, its rock burnt, frozen or
 * cracked by their breath, ores in its walls, a hoard with chests, two or three dragons (the third sometimes an
 * elder), and a wide tunnel winding up to the land. Their rock keeps lava and water out.
 * Kind: the dragons' kind (0 fire, 1 ice, 2 lightning).
 */
public final class DragonLairPieces {
	private DragonLairPieces() {
	}

	abstract static class LairPiece extends BuiltPiece {
		LairPiece(StructurePieceType type, int x, int y, int z, int facing, int kind, int param, long seed,
			int uMin, int uMax, int vMin, int vMax, int yMin, int yMax) {
			super(type, x, y, z, facing, kind, param, seed, uMin, uMax, vMin, vMax, yMin, yMax);
		}

		LairPiece(StructurePieceType type, CompoundTag tag) {
			super(type, tag);
		}

		BlockState stone(int u, int v, int y) {
			float n = noise(u, v, y, 1);
			return (switch (kind) {
				case 1 -> n < 0.6F ? IafBlockRegistry.FROZEN_STONE : n < 0.9F ? IafBlockRegistry.FROZEN_COBBLESTONE : Blocks.PACKED_ICE;
				case 2 -> n < 0.6F ? IafBlockRegistry.CRACKLED_STONE : IafBlockRegistry.CRACKLED_COBBLESTONE;
				default -> n < 0.6F ? IafBlockRegistry.CHARRED_STONE : n < 0.9F ? IafBlockRegistry.CHARRED_COBBLESTONE : Blocks.BASALT;
			}).defaultBlockState();
		}

		BlockState ground(int u, int v, int y) {
			float n = noise(u, v, y, 2);
			return (switch (kind) {
				case 1 -> n < 0.5F ? IafBlockRegistry.FROZEN_DIRT : n < 0.8F ? IafBlockRegistry.FROZEN_GRAVEL : Blocks.SNOW_BLOCK;
				case 2 -> n < 0.5F ? IafBlockRegistry.CRACKLED_DIRT : IafBlockRegistry.CRACKLED_GRAVEL;
				default -> n < 0.5F ? IafBlockRegistry.CHARRED_DIRT : n < 0.8F ? IafBlockRegistry.CHARRED_GRAVEL : IafBlockRegistry.ASH;
			}).defaultBlockState();
		}

		BlockState scorched(int u, int v, int y) {
			return (switch (kind) {
				case 1 -> IafBlockRegistry.FROZEN_GRASS;
				case 2 -> IafBlockRegistry.CRACKLED_GRASS;
				default -> IafBlockRegistry.CHARRED_GRASS;
			}).defaultBlockState();
		}

		Block treasure() {
			return switch (kind) {
				case 1 -> IafBlockRegistry.SILVER_BLOCK;
				case 2 -> Blocks.WAXED_COPPER_BLOCK;
				default -> Blocks.GOLD_BLOCK;
			};
		}

		BlockState pile(int layers) {
			Block pile = switch (kind) {
				case 1 -> IafBlockRegistry.SILVER_PILE;
				case 2 -> IafBlockRegistry.COPPER_PILE;
				default -> IafBlockRegistry.GOLD_PILE;
			};
			return pile.defaultBlockState().setValue(BlockGoldPile.LAYERS, Math.max(1, Math.min(7, layers)));
		}

		EntityType<? extends EntityDragonBase> dragonType() {
			return switch (kind) {
				case 1 -> IafEntityRegistry.ICE_DRAGON;
				case 2 -> IafEntityRegistry.LIGHTNING_DRAGON;
				default -> IafEntityRegistry.FIRE_DRAGON;
			};
		}

		String name() {
			return switch (kind) {
				case 1 -> "ice";
				case 2 -> "lightning";
				default -> "fire";
			};
		}

		void chest(WorldGenLevel level, BoundingBox chunkBox, RandomSource random, BlockPos pos, String loot) {
			if (!chunkBox.isInside(pos)) {
				return;
			}
			level.setBlock(pos, Blocks.CHEST.defaultBlockState().setValue(ChestBlock.FACING, Direction.from2DDataValue(random.nextInt(4))), 2);
			if (level.getBlockEntity(pos) instanceof ChestBlockEntity chest) {
				chest.setLootTable(ResourceKey.create(Registries.LOOT_TABLE, ResourceLocation.parse(loot)), random.nextLong());
			}
			if (chunkBox.isInside(pos.above())) {
				level.setBlock(pos.above(), Blocks.AIR.defaultBlockState(), 2);
			}
		}
	}

	/** A young dragon's roost on a hilltop: its gate is the middle of the nest, on the ground. */
	public static class Roost extends LairPiece {
		static final int R = 12;
		private static final int SCORCH = 8;
		private int ground;

		public Roost(int x, int y, int z, int kind, long seed) {
			super(IafStructures.ROOST_PIECE, x, y, z, 0, kind, 0, seed, -R - SCORCH, R + SCORCH, -R - SCORCH, R + SCORCH, y - 10, y + 16);
		}

		public Roost(CompoundTag tag) {
			super(IafStructures.ROOST_PIECE, tag);
		}

		@Override
		protected int plantClearing() {
			return 8;
		}

		@Override
		protected void column(WorldGenLevel level, int u, int v, int x, int z) {
			int top = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z) - 1;
			BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos(x, top, z);
			while (top > gateY - 20 && (level.getBlockState(pos).is(BlockTags.LOGS) || level.getBlockState(pos).canBeReplaced())) {
				pos.setY(--top);
			}
			ground = top;
		}

		private double edge(int u, int v) {
			return R - 1.5 + smooth(Math.atan2(u, v) * 3 + 10, 0, 2, 3) * 3;
		}

		/** The nest's floor at a place: a shallow bowl. */
		private int floor(double r, double edge) {
			return gateY - 2 + (int) Math.round(2 * (r / edge) * (r / edge));
		}

		@Override
		protected @Nullable BlockState at(WorldGenLevel level, int u, int v, int y) {
			double r = Math.sqrt(u * u + v * v);
			double edge = edge(u, v);
			if (r > edge + SCORCH) {
				return null;
			}
			if (r > edge) {
				// the land around, burnt (frozen, struck) bare: its top scorched, the trees' leaves gone
				double reach = 1 - (r - edge) / SCORCH;
				if (y == ground && noise(u, v, y, 4) < reach) {
					BlockState here = level.getBlockState(world(u, v, y));
					return here.is(BlockTags.DIRT) ? scorched(u, v, y) : here.is(BlockTags.BASE_STONE_OVERWORLD) ? stone(u, v, y) : null;
				}
				if (y > ground && kind != 1 && level.getBlockState(world(u, v, y)).is(BlockTags.LEAVES)) {
					return Blocks.AIR.defaultBlockState();
				}
				return null;
			}
			int f = floor(r, edge);
			if (y < f - 3) {
				return y >= ground - 8 && y > gateY - 10 && level.getBlockState(world(u, v, y)).isAir() ? stone(u, v, y) : null;
			}
			if (y <= f) {
				if (y < f) {
					return stone(u, v, y);
				}
				if (r <= 2.5) {
					return treasure().defaultBlockState();
				}
				return noise(u, v, y, 5) < 0.06F ? IafBlockRegistry.DRAGON_BONE_BLOCK.defaultBlockState() : ground(u, v, y);
			}
			int dy = y - f;
			// a rim of rocks round the bowl, and bones standing up in it
			if (r > edge - 1.6) {
				int rim = 1 + (int) (smooth(u, v, 2, 6) * 3);
				if (dy <= rim) {
					return stone(u, v, y);
				}
			}
			for (int k = 0; k < 6; k++) {
				double a = Math.PI * 2 * k / 6 + noise(k, 0, 0, 7);
				int bu = (int) Math.round(Math.sin(a) * (edge - 3));
				int bv = (int) Math.round(Math.cos(a) * (edge - 3));
				if (u == bu && v == bv && dy <= 2 + (k % 3)) {
					return IafBlockRegistry.DRAGON_BONE_BLOCK.defaultBlockState();
				}
			}
			// the mound of treasure in the middle
			if (r <= 4.5) {
				int mound = (int) Math.round(2.5 * (1 - r / 4.5));
				if (dy <= mound) {
					return dy == mound ? pile(3 + (int) (noise(u, v, y, 8) * 4)) : treasure().defaultBlockState();
				}
			}
			if (dy == 1 && r < edge - 2 && noise(u, v, y, 9) < 0.12F) {
				return pile(1 + (int) (noise(u, v, y, 10) * 2));
			}
			return y <= gateY + 14 ? Blocks.AIR.defaultBlockState() : null;
		}

		@Override
		protected void decorate(WorldGenLevel level, BoundingBox chunkBox, RandomSource random) {
			double edge = edge(3, 0);
			int f = floor(3, edge);
			chest(level, chunkBox, random, world(3, 0, f + 1), "iceandfire_fabric:chest/" + name() + "_dragon_roost");
			chest(level, chunkBox, random, world(-2, -3, floor(3.6, edge(-2, -3)) + 1), "iceandfire_fabric:chest/" + name() + "_dragon_roost");
			BlockPos home = world(0, 0, gateY);
			BlockPos at = world(0, 6, floor(6, edge(0, 6)) + 1);
			if (chunkBox.isInside(at)) {
				DragonNests.spawn(level, dragonType(), random, at, random.nextBoolean(), DragonNests.nestAge(random), home, true);
				if (random.nextInt(3) == 0) {
					BlockPos beside = world(-5, 4, floor(6.4, edge(-5, 4)) + 1);
					DragonNests.spawn(level, dragonType(), random, beside, random.nextBoolean(), 5 + random.nextInt(15), home, false);
				}
			}
		}
	}

	/** A stage 4 dragons' den under the land: its gate is the middle of the cave's floor; its tunnel goes along v. */
	public static class Cave extends LairPiece {
		/** One or two eggs on the floor beside the female (where there is room, in this chunk). */
		private void eggs(WorldGenLevel level, BoundingBox chunkBox, RandomSource random, com.iafabric.entity.EntityDragonBase mother, int[] place) {
			int count = 1 + random.nextInt(2);
			int[][] beside = {{3, 1}, {2, -2}, {-2, 2}};
			for (int[] offset : beside) {
				if (count <= 0) {
					break;
				}
				BlockPos at = world(place[0] + offset[0], place[1] + offset[1], gateY);
				for (int dy = 0; dy <= 2 && chunkBox.isInside(at); dy++) {
					BlockPos up = at.above(dy);
					if (level.getBlockState(up).isAir() && level.getBlockState(up.above()).isAir() && !level.getBlockState(up.below()).isAir()) {
						com.iafabric.entity.EntityDragonEgg egg = mother.createEgg(mother);
						egg.moveTo(up.getX() + 0.5, up.getY(), up.getZ() + 0.5, random.nextFloat() * 360, 0);
						level.addFreshEntity(egg);
						count--;
						break;
					}
				}
			}
		}

		static final int RX = 30;
		static final int RY = 16;
		static final int TUNNEL_START = 26;
		private final int end;
		private int surface;
		private boolean near;

		public Cave(int x, int y, int z, int facing, int kind, long seed, int end, int top) {
			super(IafStructures.CAVE_PIECE, x, y, z, facing, kind, 0, seed, -RX - 5, RX + 5, -RX - 5, end + 3, y - 5, Math.max(y + RY + 3, top));
			this.end = end;
		}

		public Cave(CompoundTag tag) {
			super(IafStructures.CAVE_PIECE, tag);
			this.end = tag.iceandfire_fabric$getIntOr("End", 60);
		}

		@Override
		protected void addAdditionalSaveData(StructurePieceSerializationContext context, CompoundTag tag) {
			super.addAdditionalSaveData(context, tag);
			tag.putInt("End", end);
		}

		static double tunnelMiddle(int v) {
			double t = v - TUNNEL_START;
			return t <= 0 ? 0 : 6 * Math.sin(t / 13.0) * Math.min(1, t / 8.0);
		}

		static int tunnelFloor(int floor, int v) {
			return floor + Math.max(0, v - TUNNEL_START) / 2;
		}

		private boolean inCave(int u, int v, int y) {
			if (y < gateY || y > gateY + RY + 1) {
				return false;
			}
			double rb = RX - 2 + smooth(u, v, 10, 11) * 5;
			double dy = (y - (gateY - 1)) / (double) RY;
			return (u * u + v * v) / (rb * rb) + dy * dy < 1;
		}

		private boolean inTunnel(int u, int v, int y) {
			if (v < TUNNEL_START - 6 || v > end) {
				return false;
			}
			double du = u - tunnelMiddle(v);
			double w = 6 + smooth(v, 0, 6, 12) * 2;
			if (Math.abs(du) > w) {
				return false;
			}
			int floor = tunnelFloor(gateY - 1, v);
			if (y <= floor) {
				return false;
			}
			double dy = (y - floor + 1.5) / 12.5;
			return du * du / (w * w) + dy * dy < 1;
		}

		private boolean open(int u, int v, int y) {
			return inCave(u, v, y) || inTunnel(u, v, y);
		}

		@Override
		protected void column(WorldGenLevel level, int u, int v, int x, int z) {
			surface = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z) - 1;
			near = u * u + v * v <= (RX + 8) * (RX + 8) || v >= TUNNEL_START - 8 && v <= end + 2 && Math.abs(u - tunnelMiddle(Math.max(v, TUNNEL_START))) <= 11;
		}

		@Override
		protected @Nullable BlockState at(WorldGenLevel level, int u, int v, int y) {
			if (!near || y < gateY - 4) {
				return null;
			}
			if (open(u, v, y)) {
				return inside(u, v, y);
			}
			if (y > surface) {
				return null;
			}
			if (open(u, v, y + 1)) {
				return ground(u, v, y);
			}
			if (open(u, v, y - 1) && noise(u, v, y, 22) < 0.04F) {
				// glowing stones in the roof
				return (kind == 1 ? Blocks.SEA_LANTERN : kind == 2 ? Blocks.OCHRE_FROGLIGHT : Blocks.SHROOMLIGHT).defaultBlockState();
			}
			for (int d = 1; d <= 2; d++) {
				if (open(u + d, v, y) || open(u - d, v, y) || open(u, v + d, y) || open(u, v - d, y) || open(u, v, y + d) || open(u, v, y - d)) {
					return d == 1 && noise(u, v, y, 13) < 0.07F ? ore(u, v, y) : stone(u, v, y);
				}
			}
			return null;
		}

		private BlockState ore(int u, int v, int y) {
			boolean rich = noise(u, v, y, 14) < 0.35F;
			return (switch (kind) {
				case 1 -> rich ? IafBlockRegistry.SAPPHIRE_ORE : IafBlockRegistry.SILVER_ORE;
				case 2 -> rich ? Blocks.AMETHYST_BLOCK : Blocks.COPPER_ORE;
				default -> rich ? Blocks.EMERALD_ORE : Blocks.GOLD_ORE;
			}).defaultBlockState();
		}

		private BlockState inside(int u, int v, int y) {
			boolean ground = !open(u, v, y - 1);
			double r = Math.sqrt(u * u + v * v);
			if (inCave(u, v, y) && r <= 8) {
				// the hoard
				int top = gateY + (int) Math.round(4 * (1 - r / 8));
				if (y < top) {
					return treasure().defaultBlockState();
				}
				if (y == top) {
					return pile(2 + (int) (noise(u, v, y, 15) * 5));
				}
			}
			if (ground && noise(u, v, y, 16) < 0.08F && r > 7) {
				return noise(u, v, y, 17) < 0.6F ? pile(1 + (int) (noise(u, v, y, 18) * 2)) : Blocks.BONE_BLOCK.defaultBlockState();
			}
			if (ground && kind == 0 && r > 10 && r < 22 && smooth(u, v, 3, 19) > 0.8F) {
				return Blocks.MAGMA_BLOCK.defaultBlockState();
			}
			return Blocks.CAVE_AIR.defaultBlockState();
		}

		@Override
		protected void decorate(WorldGenLevel level, BoundingBox chunkBox, RandomSource random) {
			for (int i = 0; i < 4; i++) {
				double a = Math.PI * 2 * i / 4 + 0.6;
				int u = (int) Math.round(Math.sin(a) * 4);
				int v = (int) Math.round(Math.cos(a) * 4);
				int top = gateY + (int) Math.round(4 * (1 - Math.sqrt(u * u + v * v) / 8));
				chest(level, chunkBox, random, world(u, v, top), "iceandfire_fabric:chest/" + name() + "_dragon_" + (i % 2 == 0 ? "male" : "female") + "_cave");
			}
			// a breeding pair (a female of stage 4 or 5 by her eggs, and a male); one time in three a third, an elder
			int members = 2 + (int) (noise(0, 0, 0, 20) * 2);
			boolean elder = members == 3 && noise(0, 0, 0, 21) < 0.34F;
			int[][] places = {{-14, 3}, {12, -8}, {0, 15}};
			BlockPos home = world(0, 0, gateY + 3);
			for (int i = 0; i < members; i++) {
				BlockPos at = world(places[i][0], places[i][1], gateY);
				if (chunkBox.isInside(at)) {
					int age = i == 2 && elder ? 100 + random.nextInt(25) : i == 0 ? 80 + random.nextInt(35) : 75 + random.nextInt(25);
					boolean male = i == 1 || i == 2 && random.nextBoolean();
					com.iafabric.entity.EntityDragonBase dragon = DragonNests.spawn(level, dragonType(), random, at, male, age, home, random.nextInt(3) != 0);
					if (i == 0 && dragon != null) {
						eggs(level, chunkBox, random, dragon, places[0]);
						com.iafabric.world.FairyLandmarks.note(level, com.iafabric.world.FairyLandmarks.Kind.NEST, at);
					}
				}
			}
		}
	}
}

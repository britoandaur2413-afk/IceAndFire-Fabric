package com.iafabric.world.structure;

import com.iafabric.block.IafBlockRegistry;
import com.iafabric.entity.DreadDragons;
import com.iafabric.entity.EntityDragonBase;
import com.iafabric.entity.EntityBlackFrostGuard;
import com.iafabric.entity.EntityCryonaAsleep;
import com.iafabric.entity.EntityDreadMob;
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
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.LanternBlock;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.SkullBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Half;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import org.jspecify.annotations.Nullable;

/**
 * The buildings of the dread. The Throne (the first one, now CitadelPiece): Cryona's frozen arena, a ring of unbreakable dread stone open to the sky,
 * pillars of ice, her throne on a dais at the far side (she sleeps on it) and Black Frost asleep on a bed of blue ice
 * in the middle. The Spire: a dread knight's black tower in the cold lands, his undead dragon perched on its top and
 * another on a nest of bones below, among graves and the dead.
 */
public final class DreadPieces {
	private DreadPieces() {
	}

	/** The land under any tree at a column. */
	static int groundAt(WorldGenLevel level, int x, int z, int floor) {
		int top = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z) - 1;
		BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos(x, top, z);
		while (top > floor && (level.getBlockState(pos).is(BlockTags.LOGS) || level.getBlockState(pos).canBeReplaced())) {
			pos.setY(--top);
		}
		return top;
	}

	static BlockState chain() {
		return Blocks.IRON_CHAIN.defaultBlockState().setValue(RotatedPillarBlock.AXIS, Direction.Axis.Y);
	}

	static void chest(WorldGenLevel level, BoundingBox chunkBox, RandomSource random, BlockPos pos, Direction facing, String loot) {
		if (!chunkBox.isInside(pos)) {
			return;
		}
		level.setBlock(pos, Blocks.CHEST.defaultBlockState().setValue(ChestBlock.FACING, facing), 2);
		if (level.getBlockEntity(pos) instanceof ChestBlockEntity chest) {
			chest.setLootTable(ResourceKey.create(Registries.LOOT_TABLE, Identifier.parse(loot)), random.nextLong());
		}
	}

	/** A dragon of the dread: an undead ice dragon (Black Frost when asked), resting where it is set. */
	public static EntityDragonBase dreadDragon(WorldGenLevel level, RandomSource random, BlockPos pos, int age, boolean blackFrost) {
		return dreadDragon(IafEntityRegistry.ICE_DRAGON, level, random, pos, age, blackFrost);
	}

	/** The same, of a kind of its own (Black Frost by Cryona's throne in her citadel). */
	public static <T extends EntityDragonBase> T dreadDragon(EntityType<T> type, WorldGenLevel level, RandomSource random, BlockPos pos, int age, boolean blackFrost) {
		T dragon = type.create(level.getLevel(), EntitySpawnReason.STRUCTURE);
		if (dragon == null) {
			return null;
		}
		dragon.setAgeInDays(0);
		dragon.growDragon(age);
		dragon.setAgingDisabled(true);
		dragon.setGender(true);
		dragon.dreadMount = true;
		dragon.setDreadLook(blackFrost ? 2 : 1);
		if (blackFrost) {
			dragon.setCustomName(DreadDragons.blackFrostName());
			DreadDragons.blackFrostStats(dragon);
		}
		dragon.setHealth(dragon.getMaxHealth());
		dragon.absSnapTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, random.nextFloat() * 360, 0);
		dragon.setPersistenceRequired();
		dragon.homePos = new HomePosition(pos, level.getLevel());
		dragon.hasHomePosition = true;
		dragon.setHunger(100);
		return dragon;
	}

	/** Cryona's frozen arena (its gate at v = 0, its middle {@link #MIDDLE} ahead). */
	public static class Throne extends BuiltPiece {
		public static final int R = 30;
		public static final int MIDDLE = R + 8;
		private static final int BLEND = 7;
		private int ground;

		public Throne(int x, int y, int z, int facing, long seed) {
			super(IafStructures.DREAD_THRONE_PIECE, x, y, z, facing, 0, 0, seed, -R - 12 - BLEND, R + 12 + BLEND, -4 - BLEND, MIDDLE + R + 12 + BLEND,
				y - 8, y + 26);
		}

		public Throne(CompoundTag tag) {
			super(IafStructures.DREAD_THRONE_PIECE, tag);
		}

		@Override
		protected int plantClearing() {
			return 8;
		}

		@Override
		protected void column(WorldGenLevel level, int u, int v, int x, int z) {
			ground = groundAt(level, x, z, gateY - 16);
		}

		/** The throne's place: across the arena from the gate. */
		public static int throneV() {
			return MIDDLE + R - 6;
		}

		/** Where Cryona sleeps on her throne. */
		public BlockPos throneSeat() {
			return world(0, throneV() + 1, gateY + 3);
		}

		@Override
		protected @Nullable BlockState at(WorldGenLevel level, int u, int v, int y) {
			int g = gateY;
			int dv = v - MIDDLE;
			double r = Math.sqrt(u * u + dv * dv);
			if (r > R + 12 + BLEND) {
				return null;
			}
			if (r > R + 12) {
				// the land eased to the arena's height
				double t = (r - R - 12) / BLEND;
				t = t * t * (3 - 2 * t);
				int target = (int) Math.round((g - 1) * (1 - t) + ground * t);
				if (y == target) {
					return Blocks.SNOW_BLOCK.defaultBlockState();
				}
				if (y < target) {
					return y > ground && y > g - 10 ? Blocks.PACKED_ICE.defaultBlockState() : null;
				}
				return y <= Math.max(ground, g + 2) ? Blocks.AIR.defaultBlockState() : null;
			}
			if (y < g - 8) {
				return null;
			}
			// the ring of dread stone: unbreakable, with battlements and the gate
			if (r > R && r <= R + 3) {
				boolean gate = Math.abs(u) <= 3 && dv < 0;
				if (y < g) {
					return IafBlockRegistry.DREAD_STONE_BRICKS.defaultBlockState();
				}
				if (gate) {
					int arch = Math.abs(u) <= 1 ? g + 8 : Math.abs(u) == 2 ? g + 7 : g + 6;
					if (y <= arch) {
						return Blocks.AIR.defaultBlockState();
					}
					if (y == arch + 1 && r < R + 1) {
						return Blocks.IRON_BARS.defaultBlockState();
					}
				}
				if (y <= g + 12) {
					float n = noise(u, v, y, 1);
					return (n < 0.12F ? IafBlockRegistry.DREAD_STONE_BRICKS_CRACKED : n < 0.22F ? IafBlockRegistry.DREAD_STONE_BRICKS_MOSSY
						: (y - g) % 6 == 5 ? IafBlockRegistry.DREAD_STONE_BRICKS_CHISELED : IafBlockRegistry.DREAD_STONE_BRICKS).defaultBlockState();
				}
				if (y == g + 13) {
					return (u + v & 1) == 0 && r > R + 1.5 ? IafBlockRegistry.DREAD_STONE_BRICKS.defaultBlockState()
						: Math.floorMod(u * 7 + v * 3, 11) == 0 ? IafBlockRegistry.DREAD_TORCH.defaultBlockState() : Blocks.AIR.defaultBlockState();
				}
				return y <= g + 20 ? Blocks.AIR.defaultBlockState() : null;
			}
			// outside the ring: frozen ground, obelisks of ice, graves
			if (r > R + 3) {
				if (y < g - 1) {
					return y > g - 6 ? Blocks.PACKED_ICE.defaultBlockState() : null;
				}
				if (y == g - 1) {
					return noise(u, v, y, 2) < 0.7F ? Blocks.SNOW_BLOCK.defaultBlockState() : Blocks.PACKED_ICE.defaultBlockState();
				}
				double angle = Math.atan2(u, dv);
				int spike = (int) Math.round(angle / (Math.PI / 6));
				double sa = spike * Math.PI / 6;
				double su = Math.sin(sa) * (R + 8);
				double sv = Math.cos(sa) * (R + 8);
				double ds = Math.sqrt((u - su) * (u - su) + (dv - sv) * (dv - sv));
				int h = 6 + (int) (noise(spike, 0, 0, 3) * 8);
				if (ds < 1.6 - (y - g) * 0.08 && y - g <= h && Math.abs(spike) != 6) {
					return (y - g) % 3 == 0 ? Blocks.BLUE_ICE.defaultBlockState() : Blocks.PACKED_ICE.defaultBlockState();
				}
				return y <= g + 20 ? Blocks.AIR.defaultBlockState() : null;
			}
			// the arena's floor: rings of dread tiles and ice, the bed of blue ice in the middle
			if (y < g - 1) {
				return IafBlockRegistry.DREAD_STONE.defaultBlockState();
			}
			if (y == g - 1) {
				if (r <= 6) {
					return Blocks.BLUE_ICE.defaultBlockState();
				}
				return ((int) r) % 6 < 2 ? IafBlockRegistry.DREAD_STONE_TILE.defaultBlockState() : Blocks.PACKED_ICE.defaultBlockState();
			}
			int dy = y - g;
			// the bed: raised blue ice
			if (r <= 6 && dy == 0 && r > 4.5) {
				return IafBlockRegistry.DREAD_STONE_BRICKS_CHISELED.defaultBlockState();
			}
			// pillars of ice round the arena, soul lanterns hanging from them
			for (int i = 0; i < 8; i++) {
				double a = Math.PI * 2 * (i + 0.5) / 8;
				int pu = (int) Math.round(Math.sin(a) * (R - 5));
				int pv = (int) Math.round(Math.cos(a) * (R - 5));
				if (Math.abs(u - pu) <= 1 && Math.abs(dv - pv) <= 1) {
					if (dy <= 9) {
						return u == pu && dv == pv ? Blocks.BLUE_ICE.defaultBlockState() : Blocks.PACKED_ICE.defaultBlockState();
					}
					if (dy == 10 && u == pu && dv == pv) {
						return Blocks.SOUL_CAMPFIRE.defaultBlockState().setValue(CampfireBlock.LIT, true);
					}
				}
			}
			// the throne on its dais
			BlockState throne = throne(u, v, y, dy);
			if (throne != null) {
				return throne;
			}
			return y <= g + 24 ? Blocks.AIR.defaultBlockState() : null;
		}

		private @Nullable BlockState throne(int u, int v, int y, int dy) {
			int tv = throneV();
			int du = Math.abs(u);
			int dv = v - tv;
			if (du > 5 || dv < -4 || dv > 4) {
				return null;
			}
			// three steps up to the dais
			int step = dv <= -3 ? 1 : dv <= -2 ? 2 : 3;
			if (dy < step) {
				return IafBlockRegistry.DREAD_STONE_BRICKS.defaultBlockState();
			}
			if (dy == step && dv >= -1 && du <= 4) {
				if (u == 0 && dv == 1) {
					return IafBlockRegistry.DREAD_STONE_BRICKS_STAIRS.defaultBlockState().setValue(StairBlock.FACING, forwardDir())
						.setValue(StairBlock.HALF, Half.BOTTOM);
				}
				if (du == 1 && dv == 1) {
					return IafBlockRegistry.DREAD_STONE_BRICKS_CHISELED.defaultBlockState();
				}
				if (du == 3 && dv == 0) {
					return Blocks.SOUL_CAMPFIRE.defaultBlockState().setValue(CampfireBlock.LIT, true);
				}
			}
			// the throne's high back and a crown of ice behind it
			if (dv == 2 && du <= 1 && dy > step && dy <= step + 4) {
				return du == 0 ? Blocks.BLUE_ICE.defaultBlockState() : IafBlockRegistry.DREAD_STONE_BRICKS.defaultBlockState();
			}
			if (dv == 3 && du <= 3 && dy > step && dy <= step + 3 + (3 - du) * 2) {
				return du % 2 == 0 ? Blocks.PACKED_ICE.defaultBlockState() : Blocks.BLUE_ICE.defaultBlockState();
			}
			return null;
		}

		private Direction forwardDir() {
			return switch (facing & 3) {
				case 0 -> Direction.SOUTH;
				case 1 -> Direction.WEST;
				case 2 -> Direction.NORTH;
				default -> Direction.EAST;
			};
		}

		@Override
		protected void decorate(WorldGenLevel level, BoundingBox chunkBox, RandomSource random) {
			int g = gateY;
			// the chest behind the throne
			chest(level, chunkBox, random, world(-2, throneV() + 2, g + 3), forwardDir().getOpposite(), "iceandfire_fabric:chest/mausoleum_chest");
			// Cryona asleep on her throne (her sign: she herself waits in her realm)
			BlockPos seat = world(0, throneV() + 1, g + 3);
			BlockPos bed = world(0, MIDDLE, g);
			if (chunkBox.isInside(seat)) {
				EntityCryonaAsleep queen = EntityCryonaAsleep.make(level.getLevel(), seat, bed, seat.getX() + 0.5, seat.getY() + 0.5, seat.getZ() + 0.5,
					forwardDir().getOpposite().toYRot(), EntitySpawnReason.STRUCTURE);
				if (queen != null) {
					level.addFreshEntity(queen);
				}
			}
			// Black Frost by her (it prowls behind her: Black Frost itself waits in her realm too)
			if (chunkBox.isInside(bed)) {
				EntityBlackFrostGuard frost = EntityBlackFrostGuard.make(level, random, bed, random.nextFloat() * 360F);
				if (frost != null) {
					level.addFreshEntity(frost);
				}
			}
		}
	}

	/** A dread knight's spire: a black tower with an undead dragon perched on top, another on a nest of bones below. */
	public static class Spire extends BuiltPiece {
		public static final int MIDDLE = 22;
		private static final int YARD = 19;
		private static final int HEIGHT = 24;
		private static final int BLEND = 6;
		private int ground;

		public Spire(int x, int y, int z, int facing, long seed) {
			super(IafStructures.DREAD_SPIRE_PIECE, x, y, z, facing, 0, 0, seed, -YARD - BLEND - 2, YARD + BLEND + 2, -BLEND, MIDDLE + YARD + BLEND + 2,
				y - 8, y + HEIGHT + 10);
		}

		public Spire(CompoundTag tag) {
			super(IafStructures.DREAD_SPIRE_PIECE, tag);
		}

		@Override
		protected int plantClearing() {
			return 8;
		}

		@Override
		protected void column(WorldGenLevel level, int u, int v, int x, int z) {
			ground = groundAt(level, x, z, gateY - 16);
		}

		private BlockState dark(int u, int v, int y) {
			float n = noise(u, v, y, 10);
			return (n < 0.5F ? Blocks.DEEPSLATE_BRICKS : n < 0.75F ? Blocks.CRACKED_DEEPSLATE_BRICKS : n < 0.92F ? Blocks.POLISHED_BLACKSTONE_BRICKS
				: Blocks.DEEPSLATE_TILES).defaultBlockState();
		}

		/** The tower's radius at a height: wide at the foot, narrow at the top, the perch wide again. */
		private static double radius(int dy) {
			if (dy >= HEIGHT - 1) {
				return 6.5;
			}
			return 5.2 - dy * 0.11;
		}

		/** The way toward the gate (-v) in the world. */
		private Direction toGate() {
			return switch (facing & 3) {
				case 0 -> Direction.NORTH;
				case 1 -> Direction.EAST;
				case 2 -> Direction.SOUTH;
				default -> Direction.WEST;
			};
		}

		/** Where the second dragon's nest of bones is. */
		static final int[] NEST = {11, MIDDLE + 9};

		@Override
		protected @Nullable BlockState at(WorldGenLevel level, int u, int v, int y) {
			int g = gateY;
			int dv = v - MIDDLE;
			double r = Math.sqrt(u * u + dv * dv);
			if (r > YARD + BLEND) {
				return null;
			}
			if (r > YARD) {
				double t = (r - YARD) / BLEND;
				t = t * t * (3 - 2 * t);
				int target = (int) Math.round((g - 1) * (1 - t) + ground * t);
				if (y == target) {
					return Blocks.PODZOL.defaultBlockState();
				}
				if (y < target) {
					return y > ground && y > g - 10 ? Blocks.DIRT.defaultBlockState() : null;
				}
				return y <= Math.max(ground, g + 2) ? Blocks.AIR.defaultBlockState() : null;
			}
			if (y < g - 6) {
				return null;
			}
			int dy = y - g;
			// the tower
			double rr = radius(Math.max(0, dy));
			if (dy >= -1 && dy <= HEIGHT + 1 && r <= rr) {
				if (dy == HEIGHT - 1) {
					// the perch's floor, bones round its edge
					return r > rr - 1.2 ? Blocks.BONE_BLOCK.defaultBlockState() : Blocks.POLISHED_BLACKSTONE.defaultBlockState();
				}
				if (dy == HEIGHT && r > rr - 0.8) {
					return (u + dv & 1) == 0 ? Blocks.POLISHED_BLACKSTONE_BRICK_WALL.defaultBlockState() : Blocks.AIR.defaultBlockState();
				}
				if (dy >= HEIGHT) {
					return Blocks.AIR.defaultBlockState();
				}
				// the door, on the side of the way in
				if (u == 0 && dv <= -3 && dy >= 0 && dy <= 2) {
					return dy == 0 ? Blocks.POLISHED_BLACKSTONE.defaultBlockState() : Blocks.AIR.defaultBlockState();
				}
				// a column up the middle with a ladder on it
				if (u == 0 && dv == 0) {
					return dark(u, v, y);
				}
				if (u == 0 && dv == -1 && dy > 0) {
					return Blocks.LADDER.defaultBlockState().setValue(net.minecraft.world.level.block.LadderBlock.FACING, toGate());
				}
				boolean wall = r > rr - 1.3;
				if (wall) {
					// slits of soul light
					if ((dy % 6 == 3) && (u == 0 || dv == 0)) {
						return Blocks.SOUL_LANTERN.defaultBlockState();
					}
					return dy % 6 == 0 ? IafBlockRegistry.DREAD_STONE_BRICKS_CHISELED.defaultBlockState() : dark(u, v, y);
				}
				// inside: floors every six (a hole by the ladder)
				if (dy % 6 == 0 && !(u == 0 && dv == -1 && dy > 0)) {
					return Blocks.POLISHED_BLACKSTONE.defaultBlockState();
				}
				return Blocks.AIR.defaultBlockState();
			}
			// the door into the tower
			if (u == 0 && dv < 0 && dv >= -6 && dy >= 0 && dy <= 2 && r <= radius(0) + 0.5) {
				return Blocks.AIR.defaultBlockState();
			}
			// the yard: dead earth, graves, a wall of broken stone round it
			if (y < g - 1) {
				return Blocks.DIRT.defaultBlockState();
			}
			if (y == g - 1) {
				float n = noise(u, v, y, 11);
				return (n < 0.4F ? Blocks.PODZOL : n < 0.7F ? Blocks.COARSE_DIRT : n < 0.85F ? Blocks.SOUL_SOIL : Blocks.GRAVEL).defaultBlockState();
			}
			if (r > YARD - 1.5) {
				int top = g + 1 + (int) (smooth(u, v, 3, 12) * 4);
				if (Math.abs(u) <= 2 && dv < 0) {
					return y <= g + 6 ? Blocks.AIR.defaultBlockState() : null;
				}
				return y <= top ? dark(u, v, y) : y <= g + 8 ? Blocks.AIR.defaultBlockState() : null;
			}
			// the nest of bones
			int nu = u - NEST[0];
			int nv = v - NEST[1];
			double nr = Math.sqrt(nu * nu + nv * nv);
			if (nr <= 5) {
				if (dy == 0 && nr > 3.8) {
					return Blocks.BONE_BLOCK.defaultBlockState();
				}
				if (dy == 1 && nr > 4.2 && noise(u, v, y, 13) < 0.4F) {
					return Blocks.BONE_BLOCK.defaultBlockState();
				}
				return y <= g + 10 ? Blocks.AIR.defaultBlockState() : null;
			}
			// graves: a stone with a skull, in rows
			if (dy <= 1 && Math.floorMod(u, 4) == 0 && Math.floorMod(v, 5) == 0 && r > 8 && noise(u, v, 0, 14) < 0.6F) {
				if (dy == 0) {
					return Blocks.MOSSY_STONE_BRICK_WALL.defaultBlockState();
				}
				return Blocks.SKELETON_SKULL.defaultBlockState().setValue(SkullBlock.ROTATION, (int) (noise(u, v, y, 15) * 16));
			}
			if (dy == 0 && noise(u, v, y, 16) < 0.08F) {
				return noise(u, v, y, 17) < 0.5F ? Blocks.DEAD_BUSH.defaultBlockState() : Blocks.COBWEB.defaultBlockState();
			}
			if (dy == 0 && Math.floorMod(u * 5 + v * 3, 23) == 0) {
				return IafBlockRegistry.DREAD_TORCH.defaultBlockState();
			}
			return y <= g + 10 ? Blocks.AIR.defaultBlockState() : null;
		}

		@Override
		protected void decorate(WorldGenLevel level, BoundingBox chunkBox, RandomSource random) {
			int g = gateY;
			// the perched dragon with its knight, and the one on the nest below
			BlockPos perch = world(0, MIDDLE, g + HEIGHT);
			if (chunkBox.isInside(perch)) {
				ridden(level, random, perch, 80 + random.nextInt(10));
			}
			BlockPos nest = world(NEST[0], NEST[1], g + 1);
			if (chunkBox.isInside(nest)) {
				ridden(level, random, nest, 55 + random.nextInt(10));
			}
			// the dead about the yard and a lich at the tower's door
			int[][] dead = {{-8, MIDDLE - 6}, {8, MIDDLE - 8}, {-12, MIDDLE + 6}, {-4, MIDDLE + 12}};
			for (int i = 0; i < dead.length; i++) {
				spawn(level, chunkBox, i % 2 == 0 ? IafEntityRegistry.DREAD_THRALL : IafEntityRegistry.DREAD_GHOUL, world(dead[i][0], dead[i][1], g));
			}
			spawn(level, chunkBox, IafEntityRegistry.DREAD_LICH, world(0, MIDDLE - 8, g));
			// two knights on foot keep the yard (and ride a dragon whose knight has fallen)
			spawn(level, chunkBox, IafEntityRegistry.DREAD_KNIGHT, world(-6, MIDDLE + 2, g));
			spawn(level, chunkBox, IafEntityRegistry.DREAD_KNIGHT, world(6, MIDDLE - 3, g));
			chest(level, chunkBox, random, world(-1, MIDDLE, g + 1), Direction.SOUTH, "iceandfire_fabric:chest/graveyard");
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

		private void ridden(WorldGenLevel level, RandomSource random, BlockPos pos, int age) {
			EntityDragonBase dragon = dreadDragon(level, random, pos, age, false);
			if (dragon == null) {
				return;
			}
			dragon.setCommand(1);
			// its knight is drawn on its back, with its own hitbox and health (DragonRiders)
			com.iafabric.entity.DragonRiders.mount(dragon, com.iafabric.entity.DragonRiders.KNIGHT);
			level.addFreshEntity(dragon);
		}
	}
}

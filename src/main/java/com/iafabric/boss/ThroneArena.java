package com.iafabric.boss;

import com.iafabric.block.IafBlockRegistry;
import com.iafabric.vfx.Vfx;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Cryona's arena in her realm: a disc of ice and dread stone with rings and rays set in it (lit by sea lanterns), her
 * throne on a dais at the north end between two spires, a ring of great pillars crowned with soul fire, low obelisks
 * to hide behind from a charge, round the edge a forest of ice spikes, and at the way in a path of light to the altar
 * where the duel is chosen; round the middle, Black Frost's perches marked in the ice. Made when a fight begins, in
 * the realm's flat ground (top at FLOOR - 1).
 */
final class ThroneArena {
	/**
	 * Black Frost's perches (x, z from the middle): open ice where it stands to fight, far from the obelisks and the
	 * pillars and seen whole from afar; it flies from one to another high over the obelisks. The first is where it
	 * waits (the dread circle in the middle); none is by the way in.
	 */
	static final int[][] PERCHES = {{0, -4}, {38, 22}, {-38, 22}, {-38, -22}, {0, -44}, {38, -22}};

	private ThroneArena() {
	}

	static void build(ServerLevel realm, BlockPos center) {
		RandomSource random = RandomSource.create(center.asLong());
		int g = ThroneFight.FLOOR - 1;
		// rings and rays in the floor
		for (int[] ring : new int[][]{{14, 0}, {36, 1}, {60, 2}, {84, 3}, {106, 4}}) {
			int r = ring[0];
			int steps = (int) (Math.PI * 2 * r * 1.6);
			for (int k = 0; k < steps; k++) {
				double a = Math.PI * 2 * k / steps;
				BlockPos pos = center.offset((int) Math.round(Math.cos(a) * r), 0, (int) Math.round(Math.sin(a) * r)).atY(g);
				BlockState state = k % 24 == 0 ? Blocks.SEA_LANTERN.defaultBlockState()
					: ring[1] % 2 == 0 ? IafBlockRegistry.DREAD_STONE_TILE.defaultBlockState() : Blocks.BLUE_ICE.defaultBlockState();
				realm.setBlock(pos, state, 2);
			}
		}
		for (int ray = 0; ray < 8; ray++) {
			double a = Math.PI * 2 * ray / 8 + Math.PI / 8;
			for (int r = 15; r < 106; r++) {
				BlockPos pos = center.offset((int) Math.round(Math.cos(a) * r), 0, (int) Math.round(Math.sin(a) * r)).atY(g);
				realm.setBlock(pos, r % 10 == 5 ? Blocks.SEA_LANTERN.defaultBlockState() : Blocks.PACKED_ICE.defaultBlockState(), 2);
			}
		}
		// the middle: a dread circle in the stone
		for (int dx = -4; dx <= 4; dx++) {
			for (int dz = -4; dz <= 4; dz++) {
				if (dx * dx + dz * dz <= 16) {
					realm.setBlock(center.offset(dx, 0, dz).atY(g), (dx + dz) % 2 == 0 ? IafBlockRegistry.DREAD_STONE_BRICKS_CHISELED.defaultBlockState()
						: IafBlockRegistry.DREAD_STONE_BRICKS.defaultBlockState(), 2);
				}
			}
		}
		throne(realm, center.offset(0, 0, -80), g);
		path(realm, center, g);
		perches(realm, center);
		// the great pillars
		for (int k = 0; k < 16; k++) {
			double a = Math.PI * 2 * k / 16;
			BlockPos base = center.offset((int) Math.round(Math.cos(a) * 92), 0, (int) Math.round(Math.sin(a) * 92)).atY(g + 1);
			int height = 16 + random.nextInt(12);
			boolean broken = random.nextInt(4) == 0;
			pillar(realm, base, broken ? height / 2 : height, random, !broken);
		}
		// low obelisks to hide behind, in two rings
		for (int k = 0; k < 14; k++) {
			double a = k < 6 ? Math.PI * 2 * k / 6 : Math.PI * 2 * (k - 6) / 8 + Math.PI / 8;
			int r = k < 6 ? 32 : 66;
			BlockPos base = center.offset((int) Math.round(Math.cos(a) * r), 0, (int) Math.round(Math.sin(a) * r)).atY(g + 1);
			int height = 6 + random.nextInt(4);
			for (int y = 0; y < height; y++) {
				for (int dx = 0; dx <= 1; dx++) {
					for (int dz = 0; dz <= 1; dz++) {
						realm.setBlock(base.offset(dx, y, dz), y == height - 1 ? Blocks.SEA_LANTERN.defaultBlockState() : ThroneFight.ice(random), 2);
					}
				}
			}
		}
		// the forest of ice spikes round the edge
		for (int k = 0; k < 160; k++) {
			double a = Math.PI * 2 * k / 160 + random.nextDouble() * 0.02;
			int r = ThroneFight.RADIUS + 3 + random.nextInt(6);
			BlockPos base = center.offset((int) Math.round(Math.cos(a) * r), 0, (int) Math.round(Math.sin(a) * r)).atY(g + 1);
			int height = 10 + random.nextInt(22);
			int width = random.nextInt(3) == 0 ? 2 : 1;
			for (int y = 0; y < height; y++) {
				int w = y > height * 0.7 ? 1 : width;
				for (int dx = 0; dx < w; dx++) {
					for (int dz = 0; dz < w; dz++) {
						realm.setBlock(base.offset(dx, y, dz), ThroneFight.ice(random), 2);
					}
				}
			}
		}
	}

	/**
	 * The way in: a path of light from where the players come in to the altar (where the duel is chosen), lamps of
	 * cold light along it, the altar on a ring of dread stone.
	 */
	private static void path(ServerLevel realm, BlockPos center, int g) {
		for (int dz = ThroneFight.ALTAR_AT + 2; dz <= ThroneFight.COME_IN + 3; dz++) {
			for (int dx = -1; dx <= 1; dx++) {
				BlockState state = dx != 0 ? Blocks.PACKED_ICE.defaultBlockState() : dz % 2 == 0 ? Blocks.SEA_LANTERN.defaultBlockState() : Blocks.BLUE_ICE.defaultBlockState();
				realm.setBlock(center.offset(dx, 0, dz).atY(g), state, 2);
			}
			if (dz % 4 == 1) {
				for (int side : new int[]{-2, 2}) {
					realm.setBlock(center.offset(side, 0, dz).atY(g), Blocks.SEA_LANTERN.defaultBlockState(), 2);
				}
			}
		}
		BlockPos altar = center.offset(0, 0, ThroneFight.ALTAR_AT);
		for (int dx = -2; dx <= 2; dx++) {
			for (int dz = -2; dz <= 2; dz++) {
				if (dx * dx + dz * dz <= 5) {
					realm.setBlock(altar.offset(dx, 0, dz).atY(g), (Math.abs(dx) + Math.abs(dz)) % 2 == 0
						? IafBlockRegistry.DREAD_STONE_BRICKS_CHISELED.defaultBlockState() : IafBlockRegistry.DREAD_STONE_TILE.defaultBlockState(), 2);
				}
			}
		}
		realm.setBlock(altar.atY(g + 1), IafBlockRegistry.THRONE_ALTAR.defaultBlockState(), 2);
	}

	/**
	 * The duel's altar on its ring of dread stone: there before a duel and while one waits for its players; gone while
	 * one is fought (it breaks into shards of ice and light as the duel begins, and stands again when it waits).
	 */
	static void altar(ServerLevel realm, BlockPos center, boolean there) {
		BlockPos at = center.offset(0, 0, ThroneFight.ALTAR_AT).atY(ThroneFight.FLOOR);
		boolean standing = realm.getBlockState(at).is(IafBlockRegistry.THRONE_ALTAR);
		double x = at.getX() + 0.5;
		double y = at.getY() + 0.5;
		double z = at.getZ() + 0.5;
		if (there && !standing) {
			realm.setBlock(at, IafBlockRegistry.THRONE_ALTAR.defaultBlockState(), 2);
			realm.sendParticles(ParticleTypes.END_ROD, x, y, z, 24, 0.3, 0.6, 0.3, 0.05);
		} else if (!there && standing) {
			realm.setBlock(at, Blocks.AIR.defaultBlockState(), 2);
			realm.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.BLUE_ICE.defaultBlockState()), x, y, z, 60, 0.4, 0.5, 0.4, 0.2);
			realm.sendParticles(ParticleTypes.END_ROD, x, y, z, 30, 0.3, 0.6, 0.3, 0.1);
			Vfx.shatter(realm, new net.minecraft.world.phys.Vec3(x, y, z), 1.3F, 0.7F);
		}
	}

	/**
	 * Black Frost's perches marked in the ice (an arena of an older duel gets them when it goes on): a ring of dread
	 * stone with eight cold lights, a star of blue ice in the middle. Flat with the floor: nothing to trip on.
	 */
	static void perches(ServerLevel realm, BlockPos center) {
		int g = ThroneFight.FLOOR - 1;
		for (int i = 1; i < PERCHES.length; i++) {
			BlockPos at = center.offset(PERCHES[i][0], 0, PERCHES[i][1]);
			for (int dx = -7; dx <= 7; dx++) {
				for (int dz = -7; dz <= 7; dz++) {
					int d2 = dx * dx + dz * dz;
					if (d2 > 44) {
						continue;
					}
					BlockState state;
					if (d2 >= 30) {
						boolean light = dx == 0 || dz == 0 || Math.abs(dx) == Math.abs(dz);
						state = light ? Blocks.SEA_LANTERN.defaultBlockState() : IafBlockRegistry.DREAD_STONE_TILE.defaultBlockState();
					} else if (d2 <= 2) {
						state = IafBlockRegistry.DREAD_STONE_BRICKS_CHISELED.defaultBlockState();
					} else if ((dx == 0 || dz == 0) && d2 <= 16 || Math.abs(dx) == Math.abs(dz) && d2 <= 8) {
						state = Blocks.BLUE_ICE.defaultBlockState();
					} else {
						state = Blocks.PACKED_ICE.defaultBlockState();
					}
					realm.setBlock(at.offset(dx, 0, dz).atY(g), state, 2);
				}
			}
		}
	}

	private static void pillar(ServerLevel realm, BlockPos base, int height, RandomSource random, boolean fire) {
		for (int y = 0; y < height; y++) {
			for (int dx = -1; dx <= 1; dx++) {
				for (int dz = -1; dz <= 1; dz++) {
					boolean corner = Math.abs(dx) == 1 && Math.abs(dz) == 1;
					BlockState state = y == 0 || y == height - 1 ? IafBlockRegistry.DREAD_STONE_BRICKS.defaultBlockState()
						: corner ? IafBlockRegistry.DREAD_STONE_BRICKS.defaultBlockState() : ThroneFight.ice(random);
					realm.setBlock(base.offset(dx, y, dz), state, 2);
				}
			}
		}
		if (fire) {
			realm.setBlock(base.above(height), Blocks.SOUL_CAMPFIRE.defaultBlockState().setValue(CampfireBlock.LIT, true), 2);
		}
	}

	/** Her throne on a three-step dais, two spires of ice behind it. */
	private static void throne(ServerLevel realm, BlockPos at, int g) {
		for (int step = 0; step < 3; step++) {
			int half = 7 - step * 2;
			for (int dx = -half; dx <= half; dx++) {
				for (int dz = -half; dz <= half; dz++) {
					realm.setBlock(at.offset(dx, 0, dz).atY(g + 1 + step), step == 2 && (dx + dz) % 2 == 0
						? IafBlockRegistry.DREAD_STONE_BRICKS_CHISELED.defaultBlockState() : IafBlockRegistry.DREAD_STONE_BRICKS.defaultBlockState(), 2);
				}
			}
		}
		int top = g + 4;
		BlockState seat = IafBlockRegistry.DREAD_STONE_BRICKS_STAIRS.defaultBlockState().setValue(StairBlock.FACING, Direction.NORTH);
		realm.setBlock(at.atY(top), seat, 2);
		for (int y = 0; y < 4; y++) {
			realm.setBlock(at.offset(0, 0, -1).atY(top + y), IafBlockRegistry.DREAD_STONE_BRICKS.defaultBlockState(), 2);
		}
		realm.setBlock(at.offset(0, 0, -1).atY(top + 4), Blocks.BLUE_ICE.defaultBlockState(), 2);
		for (int side : new int[]{-1, 1}) {
			realm.setBlock(at.offset(side, 0, 0).atY(top), IafBlockRegistry.DREAD_STONE_BRICKS_SLAB.defaultBlockState(), 2);
			BlockPos spire = at.offset(side * 6, 0, -4);
			for (int y = 0; y < 18; y++) {
				int w = y < 10 ? 1 : 0;
				for (int dx = -w; dx <= w; dx++) {
					for (int dz = -w; dz <= w; dz++) {
						realm.setBlock(spire.offset(dx, 0, dz).atY(top - 3 + y), y % 6 == 5 ? Blocks.SEA_LANTERN.defaultBlockState()
							: Blocks.PACKED_ICE.defaultBlockState(), 2);
					}
				}
			}
			realm.setBlock(at.offset(side * 4, 0, 3).atY(top - 1), Blocks.SOUL_CAMPFIRE.defaultBlockState().setValue(CampfireBlock.LIT, true), 2);
		}
	}
}

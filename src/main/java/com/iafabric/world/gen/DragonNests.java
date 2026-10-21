package com.iafabric.world.gen;

import com.iafabric.entity.EntityDragonBase;
import com.iafabric.entity.util.HomePosition;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.levelgen.Heightmap;
import org.jspecify.annotations.Nullable;

/**
 * Where dragons live: the young ones (stage 3 and below) nest on high ground, the stage 4 ones live in caves in
 * groups of two or three (the third sometimes a stage 5 elder).
 */
public final class DragonNests {
	/** A nest is this many blocks above sea level or higher. */
	public static final int NEST_HEIGHT_ABOVE_SEA = 22;

	private DragonNests() {
	}

	/**
	 * The highest ground of the chunk around {@code origin} (looked for within the chunk the feature may write),
	 * or null when it is not high enough for a nest or is under water.
	 */
	public static @Nullable BlockPos highGround(WorldGenLevel level, BlockPos origin) {
		int baseX = (origin.getX() >> 4 << 4);
		int baseZ = (origin.getZ() >> 4 << 4);
		BlockPos best = null;
		for (int x = 0; x < 16; x += 3) {
			for (int z = 0; z < 16; z += 3) {
				BlockPos top = level.getHeightmapPos(Heightmap.Types.WORLD_SURFACE_WG, new BlockPos(baseX + x, 0, baseZ + z));
				if (best == null || top.getY() > best.getY()) {
					best = top;
				}
			}
		}
		if (best == null || best.getY() < level.getSeaLevel() + NEST_HEIGHT_ABOVE_SEA
			|| !level.getFluidState(best.below()).isEmpty()) {
			return null;
		}
		return best;
	}

	/** Age (days) of a nest's dragon: stage 3 most of the time, stage 2 otherwise. */
	public static int nestAge(RandomSource rand) {
		return rand.nextInt(10) < 6 ? 50 + rand.nextInt(25) : 25 + rand.nextInt(25);
	}

	/**
	 * The dragons of a nest at {@code center}: one young dragon of {@code age} days and, one time in three, a
	 * hatchling beside it.
	 */
	public static void spawnNest(WorldGenLevel level, EntityType<? extends EntityDragonBase> type, RandomSource rand, BlockPos center, boolean male, int age) {
		int y = level.getHeightmapPos(Heightmap.Types.WORLD_SURFACE_WG, center).getY();
		spawn(level, type, rand, new BlockPos(center.getX(), y, center.getZ()), male, age, center, false);
		if (rand.nextInt(3) == 0) {
			BlockPos beside = center.offset(rand.nextBoolean() ? 3 : -3, 0, rand.nextBoolean() ? 3 : -3);
			int by = level.getHeightmapPos(Heightmap.Types.WORLD_SURFACE_WG, beside).getY();
			spawn(level, type, rand, new BlockPos(beside.getX(), by, beside.getZ()), rand.nextBoolean(), 5 + rand.nextInt(15), center, false);
		}
	}

	/** One dragon living at {@code home}, asleep or not, standing at {@code at}. */
	public static EntityDragonBase spawn(WorldGenLevel level, EntityType<? extends EntityDragonBase> type, RandomSource rand, BlockPos at, boolean male, int age, BlockPos home, boolean asleep) {
		EntityDragonBase dragon = type.create(level.getLevel(), EntitySpawnReason.STRUCTURE);
		dragon.setGender(male);
		dragon.setPersistenceRequired();
		dragon.setAgeInDays(0);
		dragon.growDragon(age);
		dragon.setAgingDisabled(true);
		dragon.setHealth(dragon.getMaxHealth());
		dragon.setVariant(rand.nextInt(4));
		dragon.absSnapTo(at.getX() + 0.5, at.getY() + 0.5, at.getZ() + 0.5, rand.nextFloat() * 360, 0);
		dragon.homePos = new HomePosition(home, level.getLevel());
		dragon.hasHomePosition = true;
		dragon.setHunger(50);
		if (asleep) {
			dragon.setInSittingPose(true);
		}
		level.addFreshEntity(dragon);
		return dragon;
	}
}

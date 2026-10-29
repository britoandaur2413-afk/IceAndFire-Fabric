package com.iafabric.pixie;

import com.iafabric.block.BlockExiledLantern;
import com.iafabric.block.IafBlockRegistry;
import com.iafabric.misc.IafSoundRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CandleBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * An exiled pixie's ring: her lantern (a crystal of her color on a mossy stone) in a ring of lit candles of her
 * color, which she makes where a quest leads, on open dry ground by the place (natural ground first: never on a roof
 * nor over what was built there; only air and plants are replaced; about as high as the place's own ground, not at
 * the foot of a cliff or down a ravine by it; never shut in, as in a pen). Reaching it is reaching the place. Lit while the
 * quest is under way, with a pillar of light over it that its player sees from afar; reached and done, it stays as a
 * quiet mark (grey crystal, candles out).
 */
public final class FairyCheckpoints {
	/** Her candles' color, by her variant (the nearest to her dress). */
	private static final DyeColor[] CANDLES = {DyeColor.PINK, DyeColor.LIME, DyeColor.PURPLE, DyeColor.YELLOW, DyeColor.LIGHT_BLUE,
		DyeColor.MAGENTA, DyeColor.ORANGE, DyeColor.LIGHT_GRAY, DyeColor.LIGHT_BLUE, DyeColor.WHITE, DyeColor.PURPLE, DyeColor.LIME,
		DyeColor.ORANGE, DyeColor.PINK, DyeColor.YELLOW, DyeColor.LIME, DyeColor.GREEN, DyeColor.LIGHT_BLUE, DyeColor.ORANGE, DyeColor.CYAN};
	/** Where the ring's candles stand about the lantern (a circle of two blocks and a bit). */
	private static final int[][] RING = {{2, 1}, {1, 2}, {-1, 2}, {-2, 1}, {-2, -1}, {-1, -2}, {1, -2}, {2, -1}};

	private FairyCheckpoints() {
	}

	public static Block candle(int variant) {
		return com.iafabric.compat.Colors.candle(CANDLES[ExiledVariants.of(variant)]);
	}

	/**
	 * Her ring near a place, as near to it as there is open dry ground within a reach (in loaded land); the lantern's
	 * place, or null if there is no ground for it.
	 */
	public static @Nullable BlockPos build(ServerLevel level, BlockPos near, int variant, int reach) {
		return build(level, near, variant, reach, false);
	}

	/**
	 * As above; at an exact place (a skeleton, a flock, a den under the land) on a floor near its own height, not on
	 * the land's top over it.
	 */
	public static @Nullable BlockPos build(ServerLevel level, BlockPos near, int variant, int reach, boolean exact) {
		return build(level, near, variant, reach, exact, false);
	}

	/**
	 * As above; on the place's own floor (a cave's plaza: built, not natural ground), as near to it as there is room,
	 * not on natural ground further off (another cave beside it).
	 */
	public static @Nullable BlockPos build(ServerLevel level, BlockPos near, int variant, int reach, boolean exact, boolean ownFloor) {
		BlockPos spot = ownFloor ? null : spot(level, near, reach, true, exact);
		if (spot == null) {
			spot = spot(level, near, reach, false, exact);
		}
		if (spot == null) {
			return null;
		}
		level.setBlock(spot, IafBlockRegistry.EXILED_LANTERN.defaultBlockState().setValue(BlockExiledLantern.VARIANT, ExiledVariants.of(variant))
			.setValue(BlockExiledLantern.LIT, true), 3);
		Block candle = candle(variant);
		for (int i = 0; i < RING.length; i++) {
			BlockPos at = candleSpot(level, spot.offset(RING[i][0], 0, RING[i][1]));
			if (at != null) {
				level.setBlock(at, candle.defaultBlockState().setValue(CandleBlock.CANDLES, 1 + i % 3).setValue(CandleBlock.LIT, true), 3);
			}
		}
		return spot;
	}

	/**
	 * The nearest column to a place with open dry ground, fairly flat about it, the lantern's room free; on the land's
	 * top, not more than a few blocks above or below the place's own ground, and not shut in.
	 */
	private static @Nullable BlockPos spot(ServerLevel level, BlockPos near, int reach, boolean natural, boolean exact) {
		Integer ground = exact ? null : ground(level, near);
		for (int r = 0; r <= reach; r++) {
			List<BlockPos> found = new ArrayList<>();
			for (int dx = -r; dx <= r; dx++) {
				for (int dz = -r; dz <= r; dz++) {
					if (Math.max(Math.abs(dx), Math.abs(dz)) != r) {
						continue;
					}
					BlockPos top = exact ? floor(level, near.getX() + dx, near.getY(), near.getZ() + dz) : top(level, near.getX() + dx, near.getZ() + dz);
					if (top == null || !open(level, top, natural) || ground != null && Math.abs(top.getY() - ground) > 6) {
						continue;
					}
					int flat = 0;
					for (int[] offset : RING) {
						BlockPos ring = exact ? floor(level, top.getX() + offset[0], top.getY(), top.getZ() + offset[1])
							: top(level, top.getX() + offset[0], top.getZ() + offset[1]);
						if (ring != null && Math.abs(ring.getY() - top.getY()) <= 1) {
							flat++;
						}
					}
					if (flat >= 6) {
						found.add(top);
					}
				}
			}
			found.sort(Comparator.comparingDouble(at -> (at.getX() - near.getX()) * (double) (at.getX() - near.getX())
				+ (at.getZ() - near.getZ()) * (double) (at.getZ() - near.getZ())));
			for (BlockPos at : found) {
				if (exact || !shutIn(level, at)) {
					return at;
				}
			}
		}
		return null;
	}

	/** The place's own ground: the lowest top of the land just about it (under a roof, a bell, a tree there), or null if not loaded. */
	private static @Nullable Integer ground(ServerLevel level, BlockPos near) {
		Integer lowest = null;
		for (int dx = -2; dx <= 2; dx += 2) {
			for (int dz = -2; dz <= 2; dz += 2) {
				BlockPos top = top(level, near.getX() + dx, near.getZ() + dz);
				if (top != null && (lowest == null || top.getY() < lowest)) {
					lowest = top.getY();
				}
			}
		}
		return lowest;
	}

	/**
	 * Whether a place is shut in (a pen, a walled yard): walking from it over the land's top (a block up or down a
	 * step, never over a fence, a wall or a gate), there is no way out to ten blocks off.
	 */
	private static boolean shutIn(ServerLevel level, BlockPos from) {
		Set<Long> seen = new HashSet<>();
		ArrayDeque<BlockPos> queue = new ArrayDeque<>();
		queue.add(from);
		seen.add(BlockPos.asLong(from.getX(), 0, from.getZ()));
		while (!queue.isEmpty() && seen.size() < 400) {
			BlockPos at = queue.poll();
			int ox = at.getX() - from.getX();
			int oz = at.getZ() - from.getZ();
			if (ox * ox + oz * oz >= 100) {
				return false;
			}
			for (Direction side : Direction.Plane.HORIZONTAL) {
				int x = at.getX() + side.getStepX();
				int z = at.getZ() + side.getStepZ();
				if (!seen.add(BlockPos.asLong(x, 0, z))) {
					continue;
				}
				BlockPos next = top(level, x, z);
				if (next == null) {
					// land not loaded: it may well go on
					return false;
				}
				BlockState under = level.getBlockState(next.below());
				if (Math.abs(next.getY() - at.getY()) <= 1 && !under.is(BlockTags.FENCES) && !under.is(BlockTags.WALLS) && !under.is(BlockTags.FENCE_GATES)) {
					queue.add(next);
				}
			}
		}
		return queue.isEmpty();
	}

	/** The free place over a floor nearest a height in a column (a cave's too; its land loaded), or null. */
	private static @Nullable BlockPos floor(ServerLevel level, int x, int y, int z) {
		if (!level.hasChunk(x >> 4, z >> 4)) {
			return null;
		}
		for (int dy : new int[]{0, 1, -1, 2, -2, 3, -3, 4, -4}) {
			BlockPos at = new BlockPos(x, y + dy, z);
			BlockState here = level.getBlockState(at);
			if (here.canBeReplaced() && here.getFluidState().isEmpty() && level.getBlockState(at.above()).canBeReplaced()
				&& level.getBlockState(at.below()).isFaceSturdy(level, at.below(), Direction.UP)) {
				return at;
			}
		}
		return null;
	}

	/** The first free place over the ground in a column (its land loaded), or null. */
	private static @Nullable BlockPos top(ServerLevel level, int x, int z) {
		if (!level.hasChunk(x >> 4, z >> 4)) {
			return null;
		}
		return new BlockPos(x, level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z), z);
	}

	/** Dry sturdy ground under, room for the lantern (air or plants), and, first, ground the land made (no roof, no floor). */
	private static boolean open(ServerLevel level, BlockPos top, boolean natural) {
		BlockState ground = level.getBlockState(top.below());
		BlockState here = level.getBlockState(top);
		if (!ground.isFaceSturdy(level, top.below(), Direction.UP) || !ground.getFluidState().isEmpty() || !here.getFluidState().isEmpty()
			|| !here.canBeReplaced() || !level.getBlockState(top.above()).canBeReplaced()) {
			return false;
		}
		return !natural || ground.is(BlockTags.DIRT) || ground.is(BlockTags.SAND) || ground.is(BlockTags.BASE_STONE_OVERWORLD)
			|| ground.is(Blocks.SNOW_BLOCK) || ground.is(Blocks.GRAVEL) || ground.is(BlockTags.TERRACOTTA) || ground.is(Blocks.PACKED_ICE);
	}

	/** Where a candle can stand at a place of the ring (a block up or down at most). */
	private static @Nullable BlockPos candleSpot(ServerLevel level, BlockPos at) {
		for (int dy : new int[]{0, 1, -1}) {
			BlockPos pos = at.above(dy);
			BlockState state = level.getBlockState(pos);
			if (state.canBeReplaced() && state.getFluidState().isEmpty() && !state.is(IafBlockRegistry.EXILED_LANTERN)
				&& Block.canSupportCenter(level, pos.below(), Direction.UP)) {
				return pos;
			}
		}
		return null;
	}

	/** Her lantern is still where it was made. */
	public static boolean present(ServerLevel level, BlockPos lantern) {
		return level.getBlockState(lantern).is(IafBlockRegistry.EXILED_LANTERN);
	}

	public static boolean loaded(ServerLevel level, BlockPos at) {
		return level.hasChunk(at.getX() >> 4, at.getZ() >> 4);
	}

	/** The ring lit (its quest under way) or quiet (its place reached and done): lantern and candles. */
	public static void light(ServerLevel level, BlockPos lantern, boolean lit) {
		BlockState state = level.getBlockState(lantern);
		if (!state.is(IafBlockRegistry.EXILED_LANTERN)) {
			return;
		}
		level.setBlock(lantern, state.setValue(BlockExiledLantern.LIT, lit), 3);
		Block candle = candle(state.getValue(BlockExiledLantern.VARIANT));
		for (int[] offset : RING) {
			for (int dy = -1; dy <= 1; dy++) {
				BlockPos at = lantern.offset(offset[0], dy, offset[1]);
				BlockState c = level.getBlockState(at);
				if (c.is(candle)) {
					level.setBlock(at, c.setValue(CandleBlock.LIT, lit), 3);
				}
			}
		}
	}

	/** The ring taken away (its quest given up or gone): lantern and its candles. */
	public static void remove(ServerLevel level, BlockPos lantern) {
		BlockState state = level.getBlockState(lantern);
		if (!state.is(IafBlockRegistry.EXILED_LANTERN)) {
			return;
		}
		Block candle = candle(state.getValue(BlockExiledLantern.VARIANT));
		for (int[] offset : RING) {
			for (int dy = -1; dy <= 1; dy++) {
				BlockPos at = lantern.offset(offset[0], dy, offset[1]);
				if (level.getBlockState(at).is(candle)) {
					level.setBlock(at, Blocks.AIR.defaultBlockState(), 3);
				}
			}
		}
		level.setBlock(lantern, Blocks.AIR.defaultBlockState(), 3);
		level.sendParticles(ParticleTypes.END_ROD, lantern.getX() + 0.5, lantern.getY() + 0.6, lantern.getZ() + 0.5, 12, 0.3, 0.3, 0.3, 0.03);
	}

	/** Its place reached: a burst of light and her dust, a chime. */
	public static void reached(ServerLevel level, BlockPos lantern, int variant) {
		double x = lantern.getX() + 0.5;
		double y = lantern.getY() + 0.8;
		double z = lantern.getZ() + 0.5;
		level.sendParticles(ParticleTypes.END_ROD, x, y, z, 40, 0.6, 0.8, 0.6, 0.08);
		level.sendParticles(new DustParticleOptions(net.minecraft.world.phys.Vec3.fromRGB24(ExiledVariants.DRESS[ExiledVariants.of(variant)]).toVector3f(), 1.4F), x, y, z, 50, 1.6, 0.6, 1.6, 0.0);
		level.playSound(null, lantern, SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.BLOCKS, 1.4F, 1.1F);
		level.playSound(null, lantern, IafSoundRegistry.PIXIE_TAUNT, SoundSource.NEUTRAL, 0.8F, 1.3F);
	}

	/** Its pillar of light, for its player (seen from far: forced particles), from the lantern up into the sky. */
	public static void pillar(ServerLevel level, ServerPlayer player, BlockPos lantern, int variant) {
		DustParticleOptions dust = new DustParticleOptions(net.minecraft.world.phys.Vec3.fromRGB24(ExiledVariants.DRESS[ExiledVariants.of(variant)]).toVector3f(), 2.0F);
		double x = lantern.getX() + 0.5;
		double z = lantern.getZ() + 0.5;
		for (int dy = 1; dy <= 28; dy += 2) {
			level.sendParticles(player, ParticleTypes.END_ROD, true,x, lantern.getY() + dy, z, 1, 0.04, 0.5, 0.04, 0.0);
			if (dy % 4 == 1) {
				level.sendParticles(player, dust, true,x, lantern.getY() + dy, z, 1, 0.12, 0.6, 0.12, 0.0);
			}
		}
	}
}

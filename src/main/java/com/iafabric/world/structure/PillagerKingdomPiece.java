package com.iafabric.world.structure;

import com.iafabric.entity.DragonRiders;
import com.iafabric.entity.EntityDragonBase;
import com.iafabric.entity.IafEntityRegistry;
import com.iafabric.entity.PillagerDragons;
import com.iafabric.entity.util.HomePosition;
import com.iafabric.item.IafItemRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.raid.Raid;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.LadderBlock;
import net.minecraft.world.level.block.LanternBlock;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.WallBannerBlock;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraft.world.level.block.state.properties.Half;
import net.minecraft.world.level.block.state.properties.SlabType;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import org.jspecify.annotations.Nullable;

/**
 * A pillager kingdom: a walled war camp of the illagers, built in the stone and wood of its land (sandstone and dark
 * oak in hot lands, stone bricks and spruce in cold ones, cobble and dark oak elsewhere). A gatehouse with banners,
 * walls with a walkway, towers with pointed roofs; inside, a road with lamp posts to the keep (a great hall with the
 * throne, banners, chests and the evoker), barracks with beds, a smithy, tents by the gate, a store, training targets,
 * caged allays, and the dragons' roosts: stone rings with a nest of hay where the kingdom's young dragons rest, each
 * with a pillager on its back. Now and then the kingdom has an elite: an older ice dragon in netherite on a roost of
 * ice, ridden by the warlord.
 * Kind: the dragons' kind and the land (0 fire / hot, 1 ice / cold, 2 lightning / temperate). Param: how many young
 * dragons (2 or 3), plus 8 with an elite.
 */
public class PillagerKingdomPiece extends BuiltPiece {
	public static final int HALF = 34;
	public static final int LENGTH = 70;
	private static final int BLEND = 6;
	private static final int[][] ROOSTS = {{-21, 17}, {21, 17}, {-21, 41}, {21, 41}};
	private static final int ROOST_R = 7;
	private static final int[][] TOWERS = {{-HALF, 0}, {HALF, 0}, {-HALF, LENGTH}, {HALF, LENGTH}, {-HALF, LENGTH / 2}, {HALF, LENGTH / 2}};
	private static final int KEEP_U = 12;
	private static final int KEEP_V0 = 49;
	private static final int KEEP_V1 = 66;
	private int ground;

	public PillagerKingdomPiece(int gateX, int gateY, int gateZ, int facing, int kind, int dragons, long seed) {
		super(IafStructures.KINGDOM, gateX, gateY, gateZ, facing, kind, dragons, seed, -HALF - BLEND - 3, HALF + BLEND + 3, -BLEND - 3, LENGTH + BLEND + 3,
			gateY - 8, gateY + 24);
	}

	public PillagerKingdomPiece(CompoundTag tag) {
		super(IafStructures.KINGDOM, tag);
	}

	@Override
	protected int plantClearing() {
		return 8;
	}

	@Override
	protected boolean keptOpen(WorldGenLevel level, int u, int v, int y) {
		// the kingdom itself (walls, towers, yards) from its ground up; not the apron round it, whose rule hangs on
		// the land
		if (y < gateY) {
			return false;
		}
		int au = Math.abs(u);
		boolean tower = au >= 3 && au <= 9 && v >= -2 && v <= 4;
		for (int[] t : TOWERS) {
			tower |= Math.abs(u - t[0]) <= 3 && Math.abs(v - t[1]) <= 3;
		}
		if (!tower && (au > HALF || v < 0 || v > LENGTH)) {
			return false;
		}
		BlockState state = at(level, u, v, y);
		return state != null && state.isAir();
	}

	// ---- the land's stone and wood ---------------------------------------------------------------------------

	private BlockState base(int u, int v, int y) {
		float n = noise(u, v, y, 21);
		return switch (kind) {
			case 0 -> (n < 0.6F ? Blocks.CUT_SANDSTONE : n < 0.85F ? Blocks.SANDSTONE : Blocks.SMOOTH_SANDSTONE).defaultBlockState();
			case 1 -> (n < 0.6F ? Blocks.STONE_BRICKS : n < 0.85F ? Blocks.CRACKED_STONE_BRICKS : Blocks.MOSSY_STONE_BRICKS).defaultBlockState();
			default -> (n < 0.7F ? Blocks.COBBLESTONE : Blocks.MOSSY_COBBLESTONE).defaultBlockState();
		};
	}

	private BlockState log() {
		return (kind == 1 ? Blocks.SPRUCE_LOG : Blocks.DARK_OAK_LOG).defaultBlockState();
	}

	private BlockState planks() {
		return (kind == 1 ? Blocks.SPRUCE_PLANKS : Blocks.DARK_OAK_PLANKS).defaultBlockState();
	}

	private BlockState lightPlanks() {
		return (kind == 0 ? Blocks.ACACIA_PLANKS : Blocks.BIRCH_PLANKS).defaultBlockState();
	}

	private Block roofStairs() {
		return kind == 0 ? Blocks.RED_SANDSTONE_STAIRS : Blocks.DARK_OAK_STAIRS;
	}

	private Block roofSlab() {
		return kind == 0 ? Blocks.RED_SANDSTONE_SLAB : Blocks.DARK_OAK_SLAB;
	}

	private BlockState roofFill() {
		return (kind == 0 ? Blocks.CUT_RED_SANDSTONE : Blocks.DARK_OAK_PLANKS).defaultBlockState();
	}

	private Block fence() {
		return kind == 1 ? Blocks.SPRUCE_FENCE : Blocks.DARK_OAK_FENCE;
	}

	private BlockState wallTop() {
		return (kind == 0 ? Blocks.SANDSTONE_WALL : kind == 1 ? Blocks.STONE_BRICK_WALL : Blocks.COBBLESTONE_WALL).defaultBlockState();
	}

	/** The way the building faces, as a world direction (its +v), and its +u. */
	private Direction forward() {
		return switch (facing & 3) {
			case 0 -> Direction.SOUTH;
			case 1 -> Direction.WEST;
			case 2 -> Direction.NORTH;
			default -> Direction.EAST;
		};
	}

	private Direction right() {
		return forward().getCounterClockWise();
	}

	private static BlockState stairs(Block block, Direction facing) {
		return block.defaultBlockState().setValue(StairBlock.FACING, facing).setValue(StairBlock.HALF, Half.BOTTOM);
	}

	/**
	 * A gable roof over a rectangle (an eave of one all round): its ridge along u or along v, rising one a block.
	 * Null where it is not the roof.
	 */
	private @Nullable BlockState roof(int u, int v, int y, int u0, int u1, int v0, int v1, int baseY, boolean ridgeAlongU) {
		if (u < u0 - 1 || u > u1 + 1 || v < v0 - 1 || v > v1 + 1) {
			return null;
		}
		double across = ridgeAlongU ? v - (v0 + v1) / 2.0 : u - (u0 + u1) / 2.0;
		double half = (ridgeAlongU ? v1 - v0 : u1 - u0) / 2.0 + 1;
		int surface = baseY + (int) Math.floor(half - Math.abs(across));
		if (y == surface) {
			if (Math.abs(across) < 0.6) {
				return roofSlab().defaultBlockState().setValue(SlabBlock.TYPE, SlabType.BOTTOM);
			}
			Direction toRidge = ridgeAlongU ? (across < 0 ? forward() : forward().getOpposite()) : (across < 0 ? right() : right().getOpposite());
			return stairs(roofStairs(), toRidge);
		}
		boolean inside = u >= u0 && u <= u1 && v >= v0 && v <= v1;
		if (y > baseY && y < surface && inside) {
			boolean gable = ridgeAlongU ? u == u0 || u == u1 : v == v0 || v == v1;
			return gable ? planks() : Blocks.AIR.defaultBlockState();
		}
		return null;
	}

	// ---- the plan ----------------------------------------------------------------------------------------------

	@Override
	protected void column(WorldGenLevel level, int u, int v, int x, int z) {
		int top = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z) - 1;
		BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos(x, top, z);
		while (top > gateY - 14 && (level.getBlockState(pos).is(BlockTags.LOGS) || level.getBlockState(pos).canBeReplaced())) {
			pos.setY(--top);
		}
		ground = top;
	}

	@Override
	protected @Nullable BlockState at(WorldGenLevel level, int u, int v, int y) {
		int au = Math.abs(u);
		for (int[] t : TOWERS) {
			if (Math.abs(u - t[0]) <= 3 && Math.abs(v - t[1]) <= 3) {
				return tower(u - t[0], v - t[1], y, gateY + 12, u, v);
			}
		}
		// the gatehouse: two towers beside the gate
		if (au >= 3 && au <= 9 && v >= -2 && v <= 4) {
			return tower(au - 6, v - 1, y, gateY + 10, u, v);
		}
		if (au > HALF || v < 0 || v > LENGTH) {
			return apron(u, v, y);
		}
		if (au >= HALF - 1 || v <= 1 || v >= LENGTH - 1) {
			return wall(u, v, y, au);
		}
		if (au <= KEEP_U + 1 && v >= KEEP_V0 - 1 && v <= KEEP_V1 + 1) {
			return keep(u, v, y, au);
		}
		if (u <= -15 && u >= -31 && v >= 50 && v <= 66) {
			return barracks(u + 23, v - 58, y);
		}
		if (u >= 15 && u <= 31 && v >= 51 && v <= 65) {
			return smithy(u - 23, v - 58, y);
		}
		for (int i = 0; i < ROOSTS.length; i++) {
			int du = u - ROOSTS[i][0];
			int dv = v - ROOSTS[i][1];
			if (du * du + dv * dv <= ROOST_R * ROOST_R + ROOST_R) {
				return roost(du, dv, y, i == 3 && (param & 8) != 0, ROOSTS[i][0] < 0, u, v);
			}
		}
		if (u <= -10 && u >= -31 && v >= 3 && v <= 9) {
			return tents(u, v, y);
		}
		if (u >= 10 && u <= 31 && v >= 3 && v <= 9) {
			return store(u - 20, v - 6, y);
		}
		return yard(u, v, y, au);
	}

	/** Outside the walls: a ring of gravel, then the land eased to the kingdom's height. */
	private @Nullable BlockState apron(int u, int v, int y) {
		int g = gateY;
		int out = Math.max(Math.max(Math.abs(u) - HALF, -v), v - LENGTH);
		if (out > BLEND + 3) {
			return null;
		}
		int target;
		if (out <= 2) {
			target = g - 1;
		} else {
			double t = (out - 2) / (double) (BLEND + 1);
			t = t * t * (3 - 2 * t);
			target = (int) Math.round((g - 1) * (1 - t) + ground * t);
		}
		if (y == target) {
			if (out <= 2) {
				return noise(u, v, y, 24) < 0.6F ? Blocks.GRAVEL.defaultBlockState() : Blocks.COARSE_DIRT.defaultBlockState();
			}
			return (kind == 0 ? Blocks.SAND : Blocks.GRASS_BLOCK).defaultBlockState();
		}
		if (y < target) {
			return y > ground && y > g - 9 ? (kind == 0 ? Blocks.SANDSTONE : Blocks.DIRT).defaultBlockState() : null;
		}
		return y <= Math.max(ground, g + 2) ? Blocks.AIR.defaultBlockState() : null;
	}

	/** A tower: stone below, wood above with slits, a floor at the walkway, battlements and a pointed roof. */
	private @Nullable BlockState tower(int du, int dv, int y, int top, int u, int v) {
		int g = gateY;
		if (y < g - 6) {
			return null;
		}
		int r = Math.max(Math.abs(du), Math.abs(dv));
		if (r > 3) {
			return y <= g + 18 && y >= g ? Blocks.AIR.defaultBlockState() : y < g ? base(u, v, y) : null;
		}
		if (y < g) {
			return base(u, v, y);
		}
		boolean wall = r == 3;
		boolean corner = Math.abs(du) == 3 && Math.abs(dv) == 3;
		if (y <= top) {
			if (corner) {
				return log();
			}
			if (wall) {
				boolean slit = (y - g) % 4 == 2 && (du == 0 || dv == 0);
				return slit ? Blocks.AIR.defaultBlockState() : y < g + 3 ? base(u, v, y) : planks();
			}
			if (y == top || y == g + 5) {
				return du == 2 && dv == 2 ? Blocks.AIR.defaultBlockState() : planks();
			}
			if (du == 2 && dv == 2 && y > g) {
				// a ladder up from the ground to the top
				return Blocks.LADDER.defaultBlockState().setValue(LadderBlock.FACING, right().getOpposite());
			}
			return Blocks.AIR.defaultBlockState();
		}
		// battlements, posts, then a pointed roof
		int above = y - top;
		if (above == 1) {
			return wall && (du + dv & 1) == 0 ? wallTop() : Blocks.AIR.defaultBlockState();
		}
		if (above == 2 && corner) {
			return log();
		}
		if (above == 2 && du == 0 && dv == 0) {
			return Blocks.LANTERN.defaultBlockState().setValue(LanternBlock.HANGING, true);
		}
		int layer = above - 3;
		if (layer >= 0 && layer <= 3) {
			if (r == 3 - layer) {
				return roofFill();
			}
			return r < 3 - layer ? Blocks.AIR.defaultBlockState() : null;
		}
		if (layer == 4 && r == 0) {
			return fence().defaultBlockState();
		}
		return above <= 8 ? Blocks.AIR.defaultBlockState() : null;
	}

	/** The walls: a stone foot, logs and planks above, battlements, a walkway inside, the gate with its bars. */
	private @Nullable BlockState wall(int u, int v, int y, int au) {
		int g = gateY;
		if (y < g - 5) {
			return null;
		}
		boolean outer = au == HALF || v == 0 || v == LENGTH;
		if (y < g) {
			return base(u, v, y);
		}
		if (v <= 1 && au <= 2) {
			// the gate
			if (v == 0 && y == g + 4) {
				return Blocks.IRON_BARS.defaultBlockState();
			}
			if (y == g + 5) {
				return planks();
			}
			return y <= g + 9 ? Blocks.AIR.defaultBlockState() : null;
		}
		if (outer) {
			if (y <= g + 1) {
				return base(u, v, y);
			}
			if (y <= g + 6) {
				return (u + v) % 6 == 0 ? log() : planks();
			}
			if (y == g + 7) {
				return (u + v & 1) == 0 ? wallTop() : Blocks.AIR.defaultBlockState();
			}
			return y <= g + 9 ? Blocks.AIR.defaultBlockState() : null;
		}
		// the walkway inside, on posts
		if (y == g + 5) {
			return roofSlab().defaultBlockState().setValue(SlabBlock.TYPE, SlabType.TOP);
		}
		if (y < g + 5 && (u + v) % 8 == 0) {
			return log();
		}
		if (y == g && (u + v) % 8 == 4 && noise(u, v, y, 27) < 0.3F) {
			return Blocks.BARREL.defaultBlockState();
		}
		return y <= g + 9 ? Blocks.AIR.defaultBlockState() : null;
	}

	/** The keep: a great hall of stone and wood, its door on the road, the throne at the back. */
	private @Nullable BlockState keep(int u, int v, int y, int au) {
		int g = gateY;
		if (y < g - 5) {
			return null;
		}
		BlockState roof = roof(u, v, y, -KEEP_U, KEEP_U, KEEP_V0, KEEP_V1, g + 10, true);
		if (roof != null) {
			return roof;
		}
		if (au > KEEP_U || v < KEEP_V0 || v > KEEP_V1) {
			// a step of stone around it, stairs up to the door
			if (y < g) {
				return base(u, v, y);
			}
			if (y == g && v == KEEP_V0 - 1 && au <= 3) {
				return stairs(kind == 0 ? Blocks.SANDSTONE_STAIRS : Blocks.STONE_BRICK_STAIRS, forward());
			}
			return y <= g + 12 ? Blocks.AIR.defaultBlockState() : null;
		}
		boolean wall = au == KEEP_U || v == KEEP_V0 || v == KEEP_V1;
		if (y < g) {
			return y == g - 1 && !wall ? planks() : base(u, v, y);
		}
		if (y > g + 10) {
			return Blocks.AIR.defaultBlockState();
		}
		if (wall) {
			boolean column = au == KEEP_U ? (v - KEEP_V0) % 4 == 0 || v == KEEP_V1 : au % 4 == 0;
			if (column) {
				return log();
			}
			if (y <= g + 1) {
				return base(u, v, y);
			}
			if (v == KEEP_V0 && au <= 1 && y <= g + 3) {
				return Blocks.AIR.defaultBlockState();
			}
			boolean bay = au == KEEP_U ? (v - KEEP_V0) % 4 == 2 : au % 4 == 2;
			if (bay && (y == g + 3 || y == g + 4 || y == g + 7 || y == g + 8)) {
				return Blocks.GLASS_PANE.defaultBlockState();
			}
			if (y == g + 5) {
				return log().setValue(RotatedPillarBlock.AXIS, au == KEEP_U ? Direction.Axis.Z : Direction.Axis.X);
			}
			return y < g + 5 ? planks() : lightPlanks();
		}
		// inside: the throne on a dais at the back, a red runner to it, columns, lanterns on chains, beams
		if (v >= KEEP_V1 - 3 && au <= 4) {
			if (y == g) {
				return Blocks.POLISHED_BLACKSTONE.defaultBlockState();
			}
			if (y == g + 1 && u == 0 && v == KEEP_V1 - 2) {
				return stairs(Blocks.DARK_OAK_STAIRS, forward());
			}
			return Blocks.AIR.defaultBlockState();
		}
		if (au == 6 && (v - KEEP_V0) % 5 == 3 && y <= g + 9) {
			return log();
		}
		if (y == g && au <= 1) {
			return Blocks.CARPET.red().defaultBlockState();
		}
		if (au == 3 && (v - KEEP_V0) % 5 == 3) {
			if (y == g + 7) {
				return Blocks.LANTERN.defaultBlockState().setValue(LanternBlock.HANGING, true);
			}
			if (y > g + 7) {
				return Blocks.IRON_CHAIN.defaultBlockState();
			}
		}
		if (y == g + 10) {
			return au % 4 == 0 ? log().setValue(RotatedPillarBlock.AXIS, Direction.Axis.X) : Blocks.AIR.defaultBlockState();
		}
		return Blocks.AIR.defaultBlockState();
	}

	/** Barracks: a long house with beds along its walls, its door toward the road. */
	private @Nullable BlockState barracks(int du, int dv, int y) {
		int g = gateY;
		if (y < g - 5) {
			return null;
		}
		BlockState roof = roof(du, dv, y, -7, 7, -7, 7, g + 5, false);
		if (roof != null) {
			return roof;
		}
		int adu = Math.abs(du);
		int adv = Math.abs(dv);
		if (adu > 7 || adv > 7) {
			return y < g ? base(du, dv, y) : y <= g + 13 ? Blocks.AIR.defaultBlockState() : null;
		}
		boolean wall = adu == 7 || adv == 7;
		if (y < g) {
			return y == g - 1 && !wall ? planks() : base(du, dv, y);
		}
		if (y > g + 5) {
			return Blocks.AIR.defaultBlockState();
		}
		if (wall) {
			if (adu == 7 && adv == 7 || adv == 7 && adu % 4 == 3 || adu == 7 && adv % 4 == 3) {
				return log();
			}
			if (du == 7 && adv <= 1 && y <= g + 2) {
				return Blocks.AIR.defaultBlockState();
			}
			if (y == g + 2 && (adv == 7 ? adu % 4 == 1 : adv % 4 == 1)) {
				return Blocks.GLASS_PANE.defaultBlockState();
			}
			if (y == g + 5) {
				return log().setValue(RotatedPillarBlock.AXIS, adu == 7 ? Direction.Axis.Z : Direction.Axis.X);
			}
			return y == g ? base(du, dv, y) : planks();
		}
		if (y == g + 4 && du == 0 && dv % 4 == 0) {
			return Blocks.LANTERN.defaultBlockState().setValue(LanternBlock.HANGING, true);
		}
		return Blocks.AIR.defaultBlockState();
	}

	/** A smithy open on its sides: a roof on posts, the forge (lava behind bars), anvil, grindstone, smithing table. */
	private @Nullable BlockState smithy(int du, int dv, int y) {
		int g = gateY;
		if (y < g - 5) {
			return null;
		}
		BlockState roof = roof(du, dv, y, -6, 6, -5, 5, g + 5, false);
		if (roof != null) {
			return roof;
		}
		int adu = Math.abs(du);
		int adv = Math.abs(dv);
		if (y < g) {
			return y == g - 1 ? Blocks.POLISHED_ANDESITE.defaultBlockState() : base(du, dv, y);
		}
		if (adu > 6 || adv > 5) {
			return y <= g + 13 ? Blocks.AIR.defaultBlockState() : null;
		}
		if (adu == 6 && (adv == 5 || dv == 0) && y <= g + 5) {
			return log();
		}
		if (y == g) {
			if (du == -4 && (dv == -3 || dv == -1)) {
				return Blocks.BLAST_FURNACE.defaultBlockState();
			}
			if (du == -4 && dv == -2) {
				return Blocks.LAVA_CAULDRON.defaultBlockState();
			}
			if (du == 0 && dv == 0) {
				return Blocks.ANVIL.defaultBlockState();
			}
			if (du == 2 && dv == -3) {
				return Blocks.SMITHING_TABLE.defaultBlockState();
			}
			if (du == 3 && dv == -3) {
				return Blocks.GRINDSTONE.defaultBlockState();
			}
		}
		if (y == g + 1 && du == -4 && dv == -2) {
			return Blocks.IRON_BARS.defaultBlockState();
		}
		if (y == g + 4 && du == 0 && dv == 0) {
			return Blocks.LANTERN.defaultBlockState().setValue(LanternBlock.HANGING, true);
		}
		return Blocks.AIR.defaultBlockState();
	}

	/** A dragon's roost: a ring of stone with a low wall, posts with lanterns, a nest of hay (the elite's of ice). */
	private @Nullable BlockState roost(int du, int dv, int y, boolean elite, boolean leftSide, int u, int v) {
		int g = gateY;
		if (y < g - 5) {
			return null;
		}
		double r = Math.sqrt(du * du + dv * dv);
		if (y < g) {
			return base(u, v, y);
		}
		// the opening toward the road
		boolean gap = Math.abs(dv) <= 1 && (leftSide ? du > 0 : du < 0);
		if (y == g) {
			if (r <= 3.5) {
				return elite ? (noise(u, v, y, 25) < 0.5F ? Blocks.PACKED_ICE : Blocks.BLUE_ICE).defaultBlockState() : Blocks.HAY_BLOCK.defaultBlockState();
			}
			if (r > ROOST_R - 0.5) {
				return (kind == 0 ? Blocks.CHISELED_SANDSTONE : Blocks.CHISELED_STONE_BRICKS).defaultBlockState();
			}
			return (du + dv & 1) == 0 ? Blocks.POLISHED_ANDESITE.defaultBlockState() : base(u, v, y);
		}
		boolean post = Math.abs(du) == Math.abs(dv) && r > ROOST_R - 2.2;
		if (post && y <= g + 6) {
			return elite && y == g + 5 ? Blocks.BLUE_ICE.defaultBlockState() : log();
		}
		if (post && y == g + 7) {
			return Blocks.LANTERN.defaultBlockState();
		}
		if (y == g + 1 && r > ROOST_R - 0.5 && !gap) {
			return wallTop();
		}
		if (y == g + 1 && r > 4.5 && r < 5.5 && noise(u, v, y, 26) < 0.12F) {
			return Blocks.BONE_BLOCK.defaultBlockState();
		}
		return y <= g + 14 ? Blocks.AIR.defaultBlockState() : null;
	}

	/** Tents by the gate, of wool, each with a campfire before it. */
	private @Nullable BlockState tents(int u, int v, int y) {
		int g = gateY;
		if (y < g - 5) {
			return null;
		}
		if (y < g) {
			return y == g - 1 ? Blocks.COARSE_DIRT.defaultBlockState() : base(u, v, y);
		}
		int tent = Math.floorDiv(-u - 10, 7);
		int du = -u - 10 - tent * 7 - 3;
		int dv = v - 6;
		if (Math.abs(dv) <= 2 && Math.abs(du) <= 2) {
			int h = 2 - Math.abs(du);
			BlockState wool = (tent % 3 == 0 ? Blocks.WOOL.gray() : tent % 3 == 1 ? Blocks.WOOL.brown() : Blocks.WOOL.white()).defaultBlockState();
			if (y - g == h || y - g < h && dv == 2) {
				return wool;
			}
			return y <= g + 8 ? Blocks.AIR.defaultBlockState() : null;
		}
		if (y == g && du == 0 && dv == -3) {
			return Blocks.CAMPFIRE.defaultBlockState().setValue(CampfireBlock.LIT, true);
		}
		return y <= g + 8 ? Blocks.AIR.defaultBlockState() : null;
	}

	/** The store: a shed with barrels, hay and a chest. */
	private @Nullable BlockState store(int du, int dv, int y) {
		int g = gateY;
		if (y < g - 5) {
			return null;
		}
		BlockState roof = roof(du, dv, y, -9, 9, -2, 2, g + 3, true);
		if (roof != null) {
			return roof;
		}
		if (y < g) {
			return y == g - 1 ? planks() : base(du, dv, y);
		}
		if ((Math.abs(du) == 9 || du == 0) && Math.abs(dv) == 2 && y <= g + 3) {
			return log();
		}
		if (y == g && dv == 2 && Math.abs(du) < 9) {
			return Math.abs(du) % 3 == 0 ? Blocks.HAY_BLOCK.defaultBlockState() : Blocks.BARREL.defaultBlockState();
		}
		if (y == g + 1 && dv == 2 && Math.abs(du) % 4 == 1) {
			return Blocks.BARREL.defaultBlockState();
		}
		return y <= g + 8 ? Blocks.AIR.defaultBlockState() : null;
	}

	/** The yard: levelled, a road from the gate to the keep with lamp posts, targets, cages, campfires. */
	private @Nullable BlockState yard(int u, int v, int y, int au) {
		int g = gateY;
		if (y < g - 5) {
			return null;
		}
		if (y < g - 1) {
			return (kind == 0 ? Blocks.SANDSTONE : Blocks.DIRT).defaultBlockState();
		}
		boolean road = au <= 2 && v < KEEP_V0 - 1;
		if (y == g - 1) {
			if (road) {
				return au == 2 ? base(u, v, y) : kind == 0 ? Blocks.SMOOTH_SANDSTONE.defaultBlockState() : Blocks.DIRT_PATH.defaultBlockState();
			}
			float n = noise(u, v, y, 22);
			return switch (kind) {
				case 0 -> (n < 0.7F ? Blocks.SAND : Blocks.COARSE_DIRT).defaultBlockState();
				case 1 -> (n < 0.5F ? Blocks.GRAVEL : n < 0.8F ? Blocks.COARSE_DIRT : Blocks.PODZOL).defaultBlockState();
				default -> (n < 0.45F ? Blocks.GRAVEL : n < 0.75F ? Blocks.COARSE_DIRT : Blocks.GRASS_BLOCK).defaultBlockState();
			};
		}
		// lamp posts along the road
		if (au == 4 && v % 6 == 3 && v < KEEP_V0 - 2) {
			if (y == g || y == g + 1) {
				return fence().defaultBlockState();
			}
			if (y == g + 2) {
				return Blocks.LANTERN.defaultBlockState();
			}
		}
		// targets for practice
		if (au == 9 && v >= 24 && v <= 34 && v % 5 == 4) {
			if (y == g) {
				return Blocks.HAY_BLOCK.defaultBlockState();
			}
			if (y == g + 1) {
				return Blocks.TARGET.defaultBlockState();
			}
		}
		// cages with allays
		if (au >= 8 && au <= 10 && v >= 44 && v <= 46) {
			boolean edge = au != 9 || v != 45;
			if (y >= g && y <= g + 2) {
				return edge ? Blocks.IRON_BARS.defaultBlockState() : Blocks.AIR.defaultBlockState();
			}
			if (y == g + 3) {
				return planks();
			}
		}
		if (y == g && au == 12 && v == 30) {
			return Blocks.CAMPFIRE.defaultBlockState().setValue(CampfireBlock.LIT, true);
		}
		return y <= g + 18 ? Blocks.AIR.defaultBlockState() : null;
	}

	// ---- who lives here ----------------------------------------------------------------------------------------

	@Override
	protected void decorate(WorldGenLevel level, BoundingBox chunkBox, RandomSource random) {
		int g = gateY;
		Direction back = forward().getOpposite();
		// banners on the gatehouse, by the keep's door and behind the throne
		banner(level, chunkBox, world(-6, -3, g + 6), back, true);
		banner(level, chunkBox, world(6, -3, g + 6), back, true);
		banner(level, chunkBox, world(-3, KEEP_V0 - 1, g + 4), back, true);
		banner(level, chunkBox, world(3, KEEP_V0 - 1, g + 4), back, true);
		banner(level, chunkBox, world(-3, KEEP_V1 - 1, g + 4), back, false);
		banner(level, chunkBox, world(3, KEEP_V1 - 1, g + 4), back, false);
		chest(level, chunkBox, random, world(-6, KEEP_V1 - 2, g), back, "chests/pillager_outpost");
		chest(level, chunkBox, random, world(6, KEEP_V1 - 2, g), back, "chests/pillager_outpost");
		chest(level, chunkBox, random, world(-20, 64, g), back, "chests/pillager_outpost");
		chest(level, chunkBox, random, world(20, 56, g), forward(), "chests/village/village_weaponsmith");
		chest(level, chunkBox, random, world(20, 7, g), back, "chests/village/village_plains_house");
		// beds in the barracks, two rows of three
		for (int i = 0; i < 6; i++) {
			int du = -4 + (i % 3) * 3;
			int dv = i < 3 ? -5 : 5;
			BlockPos foot = world(-23 + du, 58 + dv + (i < 3 ? 1 : -1), g);
			Direction head = i < 3 ? back : forward();
			BlockPos headPos = foot.relative(head);
			if (chunkBox.isInside(foot) && chunkBox.isInside(headPos)) {
				BlockState bed = Blocks.BED.gray().defaultBlockState().setValue(BedBlock.FACING, head);
				level.setBlock(foot, bed.setValue(BedBlock.PART, BedPart.FOOT), 2);
				level.setBlock(headPos, bed.setValue(BedBlock.PART, BedPart.HEAD), 2);
			}
		}
		// the kingdom's people: on the walls, in the yard, at the gate and the door, the evoker by the throne
		int[][] pillagers = {{-HALF + 1, 30}, {HALF - 1, 30}, {-12, 1}, {12, 1}, {-8, 20}, {8, 20}, {-6, 36}, {6, 36}, {-24, 12}, {16, 54}, {-12, 58}};
		for (int[] p : pillagers) {
			boolean onWall = Math.abs(p[0]) >= HALF - 1 || p[1] <= 1;
			spawn(level, chunkBox, EntityTypes.PILLAGER, p[0], p[1], onWall ? g + 6 : g);
		}
		for (int[] p : new int[][]{{-3, 4}, {3, 4}, {-3, KEEP_V0 - 3}, {3, KEEP_V0 - 3}}) {
			spawn(level, chunkBox, EntityTypes.VINDICATOR, p[0], p[1], g);
		}
		spawn(level, chunkBox, EntityTypes.EVOKER, 0, KEEP_V1 - 5, g);
		for (int[] cage : new int[][]{{-9, 45}, {9, 45}}) {
			spawn(level, chunkBox, EntityTypes.ALLAY, cage[0], cage[1], g);
		}
		int young = Math.max(2, Math.min(3, param & 7));
		for (int i = 0; i < young; i++) {
			BlockPos pos = world(ROOSTS[i][0], ROOSTS[i][1], g + 1);
			if (chunkBox.isInside(pos)) {
				spawnDragon(level, random, pos, false);
			}
		}
		if ((param & 8) != 0) {
			BlockPos pos = world(ROOSTS[3][0], ROOSTS[3][1], g + 1);
			if (chunkBox.isInside(pos)) {
				spawnDragon(level, random, pos, true);
			}
		}
	}

	private void banner(WorldGenLevel level, BoundingBox chunkBox, BlockPos pos, Direction facing, boolean black) {
		if (chunkBox.isInside(pos)) {
			level.setBlock(pos, (black ? Blocks.WALL_BANNER.black() : Blocks.WALL_BANNER.gray()).defaultBlockState().setValue(WallBannerBlock.FACING, facing), 2);
		}
	}

	private void chest(WorldGenLevel level, BoundingBox chunkBox, RandomSource random, BlockPos pos, Direction facing, String loot) {
		if (!chunkBox.isInside(pos)) {
			return;
		}
		level.setBlock(pos, Blocks.CHEST.defaultBlockState().setValue(ChestBlock.FACING, facing), 2);
		if (level.getBlockEntity(pos) instanceof ChestBlockEntity chest) {
			chest.setLootTable(ResourceKey.create(Registries.LOOT_TABLE, Identifier.withDefaultNamespace(loot)), random.nextLong());
		}
	}

	private void spawn(WorldGenLevel level, BoundingBox chunkBox, EntityType<? extends Mob> type, int u, int v, int y) {
		BlockPos pos = world(u, v, y);
		if (!chunkBox.isInside(pos)) {
			return;
		}
		Mob mob = type.create(level.getLevel(), EntitySpawnReason.STRUCTURE);
		if (mob == null) {
			return;
		}
		mob.snapTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, level.getRandom().nextFloat() * 360, 0);
		mob.finalizeSpawn(level, level.getCurrentDifficultyAt(pos), EntitySpawnReason.STRUCTURE, null);
		mob.setPersistenceRequired();
		level.addFreshEntityWithPassengers(mob);
	}

	/**
	 * A kingdom dragon on its roost: young (first ridden), kept at that age, in iron, with a pillager on its back;
	 * or the elite: an older ice dragon in netherite, ridden by the warlord.
	 */
	private void spawnDragon(WorldGenLevel level, RandomSource random, BlockPos pos, boolean elite) {
		EntityType<? extends EntityDragonBase> type = elite ? IafEntityRegistry.ICE_DRAGON : switch (kind) {
			case 1 -> IafEntityRegistry.ICE_DRAGON;
			case 2 -> IafEntityRegistry.LIGHTNING_DRAGON;
			default -> IafEntityRegistry.FIRE_DRAGON;
		};
		EntityDragonBase dragon = type.create(level.getLevel(), EntitySpawnReason.STRUCTURE);
		if (dragon == null) {
			return;
		}
		dragon.setAgeInDays(0);
		dragon.growDragon(elite ? 82 + random.nextInt(8) : 52 + random.nextInt(10));
		dragon.setAgingDisabled(true);
		dragon.setHealth(dragon.getMaxHealth());
		dragon.setVariant(random.nextInt(4));
		dragon.setGender(random.nextBoolean());
		dragon.absSnapTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, random.nextFloat() * 360, 0);
		dragon.pillagerMount = true;
		dragon.eliteMount = elite;
		dragon.setCommand(1);
		dragon.setPersistenceRequired();
		dragon.homePos = new HomePosition(pos, level.getLevel());
		dragon.hasHomePosition = true;
		dragon.setHunger(100);
		Item[] armor = elite
			? new Item[]{IafItemRegistry.DRAGONARMOR_NETHERITE_0, IafItemRegistry.DRAGONARMOR_NETHERITE_1, IafItemRegistry.DRAGONARMOR_NETHERITE_2, IafItemRegistry.DRAGONARMOR_NETHERITE_3}
			: new Item[]{IafItemRegistry.DRAGONARMOR_IRON_0, IafItemRegistry.DRAGONARMOR_IRON_1, IafItemRegistry.DRAGONARMOR_IRON_2, IafItemRegistry.DRAGONARMOR_IRON_3};
		EquipmentSlot[] slots = {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};
		for (int i = 0; i < 4; i++) {
			dragon.setItemSlot(slots[i], new ItemStack(armor[i]));
		}
		// its rider is drawn on its back, with its own hitbox and health (DragonRiders)
		DragonRiders.mount(dragon, elite ? DragonRiders.WARLORD : DragonRiders.PILLAGER);
		level.addFreshEntity(dragon);
	}

	/** The warlord: a pillager captain, sturdy (more health, armor, resistance), with the illagers' banner. */
	public static void warlord(WorldGenLevel level, Mob rider) {
		rider.addTag(PillagerDragons.WARLORD);
		rider.setCustomName(Component.translatable("entity.iceandfire_fabric.pillager_warlord"));
		var health = rider.getAttribute(Attributes.MAX_HEALTH);
		if (health != null) {
			health.setBaseValue(80);
		}
		var armor = rider.getAttribute(Attributes.ARMOR);
		if (armor != null) {
			armor.setBaseValue(10);
		}
		var knockback = rider.getAttribute(Attributes.KNOCKBACK_RESISTANCE);
		if (knockback != null) {
			knockback.setBaseValue(1);
		}
		rider.setHealth(rider.getMaxHealth());
		rider.addEffect(new MobEffectInstance(MobEffects.RESISTANCE, MobEffectInstance.INFINITE_DURATION, 0, false, false));
		rider.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, MobEffectInstance.INFINITE_DURATION, 0, false, false));
		rider.setItemSlot(EquipmentSlot.HEAD, Raid.getOminousBannerInstance(level.registryAccess().lookupOrThrow(Registries.BANNER_PATTERN)));
		rider.setDropChance(EquipmentSlot.HEAD, 1F);
		ItemStack crossbow = new ItemStack(Items.CROSSBOW);
		crossbow.enchant(level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.MULTISHOT), 1);
		crossbow.enchant(level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.QUICK_CHARGE), 2);
		rider.setItemSlot(EquipmentSlot.MAINHAND, crossbow);
	}
}

package com.iafabric.util;

import net.minecraft.tags.BlockTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.level.block.BaseFireBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.NetherPortalBlock;
import net.minecraft.world.level.block.state.BlockState;

import java.util.function.Predicate;

/**
 * The old block "materials" the original code tested blocks against, rebuilt from tags and block
 * types now that blocks no longer carry a material.
 */
public enum BlockMaterial {
	AIR(BlockState::isAir),
	STONE(s -> s.is(BlockTags.BASE_STONE_OVERWORLD) || s.is(BlockTags.BASE_STONE_NETHER) || s.is(BlockTags.STONE_ORE_REPLACEABLES)
		|| s.is(Blocks.COBBLESTONE) || s.is(Blocks.MOSSY_COBBLESTONE) || s.is(BlockTags.STONE_BRICKS) || s.is(BlockTags.MINEABLE_WITH_PICKAXE)
		&& !s.is(BlockTags.ICE) && !Tests.METAL.test(s)),
	DIRT(s -> s.is(BlockTags.DIRT) && !s.is(Blocks.GRASS_BLOCK) || s.is(Blocks.FARMLAND) || s.is(Blocks.DIRT_PATH) || s.is(Blocks.GRAVEL)),
	GRASS(s -> s.is(Blocks.GRASS_BLOCK) || s.is(Blocks.MYCELIUM) || s.is(Blocks.PODZOL)),
	SAND(s -> s.is(BlockTags.SAND) || s.is(Blocks.SOUL_SOIL) || s.is(Blocks.SNOW_BLOCK) && false),
	CLAY(s -> s.is(Blocks.CLAY)),
	WOOD(s -> s.is(BlockTags.LOGS) || s.is(BlockTags.PLANKS) || s.is(BlockTags.WOODEN_SLABS) || s.is(BlockTags.WOODEN_STAIRS)
		|| s.is(BlockTags.WOODEN_FENCES) || s.is(BlockTags.WOODEN_DOORS) || s.is(BlockTags.WOODEN_TRAPDOORS) || s.is(BlockTags.MINEABLE_WITH_AXE)
		&& !s.is(BlockTags.LEAVES)),
	LEAVES(s -> s.is(BlockTags.LEAVES)),
	PLANT(s -> s.is(BlockTags.FLOWERS) || s.is(BlockTags.SAPLINGS) || s.is(BlockTags.CROPS) || s.is(Blocks.SUGAR_CANE) || s.is(Blocks.CACTUS)
		|| s.is(Blocks.BAMBOO) || s.is(Blocks.VINE) || s.is(Blocks.LILY_PAD) || s.is(BlockTags.CAVE_VINES)),
	REPLACEABLE_PLANT(s -> s.is(BlockTags.REPLACEABLE_BY_TREES) && !s.is(BlockTags.LEAVES) && !s.liquid()),
	WATER(s -> s.getFluidState().is(FluidTags.WATER) && s.getBlock() instanceof LiquidBlock || s.is(Blocks.WATER)),
	LAVA(s -> s.getFluidState().is(FluidTags.LAVA)),
	ICE(s -> s.is(Blocks.ICE) || s.is(Blocks.FROSTED_ICE)),
	ICE_SOLID(s -> s.is(Blocks.PACKED_ICE) || s.is(Blocks.BLUE_ICE)),
	SNOW(s -> s.is(Blocks.SNOW)),
	TOP_SNOW(s -> s.is(Blocks.SNOW)),
	FIRE(s -> s.getBlock() instanceof BaseFireBlock),
	GLASS(s -> s.getBlock() instanceof net.minecraft.world.level.block.AbstractGlassBlock || s.is(BlockTags.IMPERMEABLE)),
	PORTAL(s -> s.getBlock() instanceof NetherPortalBlock || s.is(Blocks.END_PORTAL) || s.is(Blocks.END_GATEWAY)),
	METAL(s -> Tests.METAL.test(s));

	/** In a holder so the constants above can use it. */
	private static final class Tests {
		static final Predicate<BlockState> METAL = s -> s.is(Blocks.IRON_BLOCK) || s.is(Blocks.GOLD_BLOCK) || s.is(Blocks.DIAMOND_BLOCK)
		|| s.is(Blocks.EMERALD_BLOCK) || s.is(Blocks.NETHERITE_BLOCK) || s.is(Blocks.IRON_BARS) || s.is(Blocks.ANVIL) || s.is(Blocks.CAULDRON)
		|| s.is(Blocks.HOPPER) || s.is(Blocks.LANTERN) || s.is(Blocks.CHAIN);
	}

	private final Predicate<BlockState> test;

	BlockMaterial(Predicate<BlockState> test) {
		this.test = test;
	}

	public boolean is(BlockState state) {
		return test.test(state);
	}
}

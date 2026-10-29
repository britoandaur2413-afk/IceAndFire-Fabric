package com.iafabric.block;

import net.minecraft.world.ItemInteractionResult;
import com.iafabric.boss.ThroneFight;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * The altar of the Frozen Throne, at the end of the path of light in Cryona's realm: used (or walked up to), it shows
 * what the duel holds and lets the player offer ice dragon blood to begin it, or go home (ThroneFight). It cannot be
 * broken and is in no creative tab.
 */
public class BlockThroneAltar extends Block implements INoTab {
	private static final VoxelShape SHAPE = Shapes.or(Block.box(0, 0, 0, 16, 4, 16), Block.box(3, 4, 3, 13, 12, 13), Block.box(1, 12, 1, 15, 16, 15));

	public BlockThroneAltar() {
		super(IafBlockRegistry.props().mapColor(MapColor.ICE).strength(-1.0F, 3600000.0F).noLootTable().sound(SoundType.GLASS).lightLevel(state -> 12));
	}

	@Override
	protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return SHAPE;
	}

	@Override
	protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
		if (player instanceof ServerPlayer serverPlayer) {
			ThroneFight.openAltar(serverPlayer);
		}
		return InteractionResult.SUCCESS;
	}

	@Override
	protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
		if (player instanceof ServerPlayer serverPlayer) {
			ThroneFight.openAltar(serverPlayer);
		}
		return ItemInteractionResult.SUCCESS;
	}

	@Override
	public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
		if (random.nextInt(2) == 0) {
			level.addParticle(ParticleTypes.SNOWFLAKE, pos.getX() + 0.2 + random.nextDouble() * 0.6, pos.getY() + 1.05, pos.getZ() + 0.2 + random.nextDouble() * 0.6,
				0, 0.04, 0);
		}
	}

	@Override
	public boolean shouldBeInTab() {
		return false;
	}
}

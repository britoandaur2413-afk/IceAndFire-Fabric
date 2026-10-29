package com.iafabric.block;

import com.iafabric.item.IafItemRegistry;
import com.iafabric.pixie.PixieBonding;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.phys.BlockHitResult;

/**
 * A pixie altar (in the pixie sanctuaries): pixie dust offered on it makes the bonded pixie the player has out grow
 * (one dust at a time, the whole handful when sneaking).
 */
public class BlockPixieAltar extends Block {
    public BlockPixieAltar() {
        super(IafBlockRegistry.props().mapColor(MapColor.COLOR_PINK).strength(2.0F, 6.0F).sound(SoundType.AMETHYST).lightLevel(state -> 10));
    }

    @Override
	public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
		ItemStack stack = player.getItemInHand(hand);
        if (!stack.is(IafItemRegistry.PIXIE_DUST)) {
            return InteractionResult.PASS;
        }
        if (player instanceof ServerPlayer serverPlayer) {
            int color = PixieBonding.get(serverPlayer).active;
            if (color < 0 || PixieBonding.activePixie(serverPlayer) == null) {
                serverPlayer.displayClientMessage(Component.translatable("iceandfire_fabric.pixie_bond.altar_no_pixie").withStyle(ChatFormatting.LIGHT_PURPLE), true);
                return InteractionResult.SUCCESS;
            }
            int needed = PixieBonding.dustToMax(PixieBonding.get(serverPlayer).get(color));
            if (needed <= 0) {
                // at its last level it takes no more
                serverPlayer.displayClientMessage(Component.translatable("iceandfire_fabric.pixie_bond.max").withStyle(ChatFormatting.GOLD), true);
                return InteractionResult.SUCCESS;
            }
            int dust = Math.min(needed, player.isShiftKeyDown() ? stack.getCount() : 1);
            if (!player.isCreative()) {
                stack.shrink(dust);
            }
            PixieBonding.addXp(serverPlayer, color, dust * PixieBonding.DUST_XP);
            serverPlayer.displayClientMessage(Component.translatable("iceandfire_fabric.pixie_bond.altar_xp", dust * PixieBonding.DUST_XP).withStyle(ChatFormatting.LIGHT_PURPLE), true);
            level.playSound(null, pos, SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.BLOCKS, 1.0F, 1.2F);
            ((net.minecraft.server.level.ServerLevel) level).sendParticles(ParticleTypes.END_ROD, pos.getX() + 0.5, pos.getY() + 1.1, pos.getZ() + 0.5, 12, 0.25, 0.3, 0.25, 0.03);
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (random.nextInt(3) == 0) {
            level.addParticle(ParticleTypes.END_ROD, pos.getX() + 0.3 + random.nextDouble() * 0.4, pos.getY() + 1.05, pos.getZ() + 0.3 + random.nextDouble() * 0.4, 0, 0.02, 0);
        }
    }
}

package com.iafabric.mixin;

import com.iafabric.boss.CastleWard;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.context.BlockPlaceContext;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Nothing can be built in Cryona's citadel while it holds (no stairs over its walls, no pillar to its windows), nor in
 * her realm at all.
 */
@Mixin(BlockItem.class)
public class BlockItemWardMixin {
	@Inject(method = "place", at = @At("HEAD"), cancellable = true)
	private void iceandfire_fabric$citadelHolds(BlockPlaceContext context, CallbackInfoReturnable<InteractionResult> cir) {
		Player player = context.getPlayer();
		if (player != null && !player.isCreative() && CastleWard.warded(context.getLevel(), context.getClickedPos())) {
			CastleWard.refuse(player, context.getLevel());
			cir.setReturnValue(InteractionResult.FAIL);
		}
	}
}

package com.iafabric.mixin;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Input;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * The mounts of the mod read their rider's forward and sideways input (zza, xxa) on the server, as in 1.18.
 * In 26.x the server only keeps the keys the client sends: while riding, they are turned back into that input.
 */
@Mixin(ServerPlayer.class)
public abstract class ServerPlayerMixin {
	@Inject(method = "setLastClientInput", at = @At("TAIL"))
	private void iceandfire_fabric$riderInput(Input input, CallbackInfo ci) {
		ServerPlayer self = (ServerPlayer) (Object) this;
		if (self.isPassenger()) {
			self.zza = input.forward() == input.backward() ? 0 : input.forward() ? 1 : -1;
			self.xxa = input.left() == input.right() ? 0 : input.left() ? 1 : -1;
		} else if (self.zza != 0 || self.xxa != 0) {
			self.zza = 0;
			self.xxa = 0;
		}
	}
}

package com.iafabric.mixin.client;

import com.iafabric.client.gui.MemoryOverlay;
import net.minecraft.client.MouseHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** In a memory of their exiled pixie the player's eyes are on her: the mouse does not turn them (MemoryOverlay). */
@Mixin(MouseHandler.class)
public class MemoryLookMixin {
	@Inject(method = "turnPlayer", at = @At("HEAD"), cancellable = true)
	private void iceandfire_fabric$eyesOnHer(CallbackInfo ci) {
		if (MemoryOverlay.active()) {
			ci.cancel();
		}
	}
}

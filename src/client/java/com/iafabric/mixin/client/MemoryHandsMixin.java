package com.iafabric.mixin.client;

import com.iafabric.client.gui.MemoryOverlay;
import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** A memory of their exiled pixie is seen through the player's eyes alone: their hands and what they hold are not in it. */
@Mixin(GameRenderer.class)
public class MemoryHandsMixin {
	@Inject(method = "renderItemInHand", at = @At("HEAD"), cancellable = true)
	private void iceandfire_fabric$noHandsInMemory(CallbackInfo ci) {
		if (MemoryOverlay.active()) {
			ci.cancel();
		}
	}
}

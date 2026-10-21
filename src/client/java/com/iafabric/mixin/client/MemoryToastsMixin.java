package com.iafabric.mixin.client;

import com.iafabric.client.gui.MemoryOverlay;
import net.minecraft.client.gui.components.toasts.ToastManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** No notice slides in over a memory of their exiled pixie: the ones that come wait till it is over (MemoryOverlay). */
@Mixin(ToastManager.class)
public class MemoryToastsMixin {
	@Inject(method = "update", at = @At("HEAD"), cancellable = true)
	private void iceandfire_fabric$waitInMemory(CallbackInfo ci) {
		if (MemoryOverlay.active()) {
			ci.cancel();
		}
	}

	@Inject(method = "extractRenderState", at = @At("HEAD"), cancellable = true)
	private void iceandfire_fabric$hiddenInMemory(CallbackInfo ci) {
		if (MemoryOverlay.active()) {
			ci.cancel();
		}
	}
}

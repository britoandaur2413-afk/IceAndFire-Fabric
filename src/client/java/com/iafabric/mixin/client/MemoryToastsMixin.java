package com.iafabric.mixin.client;

import com.iafabric.client.gui.MemoryOverlay;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.toasts.ToastComponent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** No notice slides in over a memory of their exiled pixie: the ones that come wait till it is over (MemoryOverlay). */
@Mixin(ToastComponent.class)
public class MemoryToastsMixin {
	@Inject(method = "render", at = @At("HEAD"), cancellable = true)
	private void iceandfire_fabric$waitInMemory(GuiGraphics graphics, CallbackInfo ci) {
		if (MemoryOverlay.active()) {
			ci.cancel();
		}
	}
}

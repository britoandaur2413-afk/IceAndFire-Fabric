package com.iafabric.mixin.client;

import com.iafabric.client.gui.IafHud;
import com.iafabric.client.gui.MemoryOverlay;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** While a memory frames the screen, the game's HUD is gone: only the mod's (its bars and subtitles) is drawn. */
@Mixin(Gui.class)
public class GuiHudMixin {
	@Inject(method = "render", at = @At("HEAD"), cancellable = true)
	private void iceandfire_fabric$framed(GuiGraphics g, float delta, CallbackInfo ci) {
		if (MemoryOverlay.hidesHud()) {
			IafHud.render(g, delta);
			ci.cancel();
		}
	}
}

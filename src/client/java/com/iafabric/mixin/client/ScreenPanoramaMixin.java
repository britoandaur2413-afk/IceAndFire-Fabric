package com.iafabric.mixin.client;

import com.iafabric.IafConfig;
import com.iafabric.client.gui.IafMainMenu;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** The title screen's panorama gives way to Ice and Fire's bestiary on its table (IafMainMenu). */
@Mixin(Screen.class)
public class ScreenPanoramaMixin {
	@Shadow
	public int width;
	@Shadow
	public int height;

	@Inject(method = "extractPanorama", at = @At("HEAD"), cancellable = true)
	private void iceandfire_fabric$bestiary(GuiGraphicsExtractor graphics, float a, CallbackInfo ci) {
		if ((Object) this instanceof TitleScreen && IafConfig.customMainMenu) {
			IafMainMenu.draw(graphics, this.width, this.height);
			ci.cancel();
		}
	}
}

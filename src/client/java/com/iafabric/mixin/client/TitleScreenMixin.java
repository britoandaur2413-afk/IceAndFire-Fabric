package com.iafabric.mixin.client;

import com.iafabric.IafConfig;
import com.iafabric.IafMod;
import com.iafabric.client.gui.IafMainMenu;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.SplashRenderer;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Ice and Fire's own title screen (IafMainMenu): one of its splash texts, and its name above the game's version. */
@Mixin(TitleScreen.class)
public abstract class TitleScreenMixin extends Screen {
	@Shadow
	private @Nullable SplashRenderer splash;
	@Unique
	private boolean iceandfire_fabric$splashChosen;

	protected TitleScreenMixin(Component title) {
		super(title);
	}

	@Inject(method = "init", at = @At("TAIL"))
	private void iceandfire_fabric$splash(CallbackInfo ci) {
		if (IafConfig.customMainMenu && !this.iceandfire_fabric$splashChosen) {
			this.iceandfire_fabric$splashChosen = true;
			String line = IafMainMenu.splash();
			if (line != null) {
				this.splash = new SplashRenderer(Component.literal(line));
			}
		}
	}

	@Inject(method = "extractRenderState", at = @At("TAIL"))
	private void iceandfire_fabric$name(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a, CallbackInfo ci) {
		if (IafConfig.customMainMenu) {
			graphics.text(this.font, "Ice and Fire " + IafMod.VERSION, 2, this.height - 20, 0xFFFFDD55);
		}
	}
}

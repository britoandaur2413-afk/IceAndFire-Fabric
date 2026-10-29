package com.iafabric.mixin.client;

import com.iafabric.IafConfig;
import com.iafabric.IafMod;
import com.iafabric.client.gui.IafMainMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.SplashRenderer;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.renderer.PanoramaRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
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
				this.splash = new SplashRenderer(line);
			}
		}
	}

	/** Its own background instead of the game's panorama... */
	@Redirect(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/PanoramaRenderer;render(FF)V"))
	private void iceandfire_fabric$bestiary(PanoramaRenderer panorama, float partialTick, float alpha, GuiGraphics graphics, int mouseX, int mouseY, float a) {
		if (IafConfig.customMainMenu) {
			IafMainMenu.draw(graphics, this.width, this.height);
		} else {
			panorama.render(partialTick, alpha);
		}
	}

	/** ...and without the shade the game lays over the panorama. */
	@Redirect(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiGraphics;blit(Lnet/minecraft/resources/ResourceLocation;IIIIFFIIII)V", ordinal = 0))
	private void iceandfire_fabric$noShade(GuiGraphics graphics, ResourceLocation texture, int x, int y, int width, int height, float u, float v,
		int uWidth, int vHeight, int textureWidth, int textureHeight) {
		if (!IafConfig.customMainMenu) {
			graphics.blit(texture, x, y, width, height, u, v, uWidth, vHeight, textureWidth, textureHeight);
		}
	}

	@Inject(method = "render", at = @At("TAIL"))
	private void iceandfire_fabric$name(GuiGraphics graphics, int mouseX, int mouseY, float a, CallbackInfo ci) {
		if (IafConfig.customMainMenu) {
			graphics.drawString(this.font, "Ice and Fire " + IafMod.VERSION, 2, this.height - 20, 0xFFFFDD55);
		}
	}
}

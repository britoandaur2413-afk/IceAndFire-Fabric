package com.iafabric.mixin.client;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.Gui;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * The texts across the middle of the screen (the line over the hotbar, a title and its subtitle) narrowed when they
 * are wider than the screen (a longer language, a big GUI scale): never cut at its edges.
 */
@Mixin(Gui.class)
public abstract class HudFitMixin {
	@Unique
	private static final String BACKDROP = "Lnet/minecraft/client/gui/Gui;drawBackdrop(Lnet/minecraft/client/gui/GuiGraphics;Lnet/minecraft/client/gui/Font;III)V";

	@Shadow
	private @Nullable Component overlayMessageString;
	@Shadow
	private @Nullable Component title;
	@Shadow
	private @Nullable Component subtitle;

	@Shadow
	public abstract Font getFont();

	@Inject(method = "render", at = @At(value = "INVOKE", target = BACKDROP, ordinal = 0))
	private void iceandfire_fabric$fitOverlay(GuiGraphics graphics, float delta, CallbackInfo ci) {
		this.iceandfire_fabric$fit(graphics, this.overlayMessageString, 1F);
	}

	@Inject(method = "render", at = @At(value = "INVOKE", target = BACKDROP, ordinal = 1))
	private void iceandfire_fabric$fitTitle(GuiGraphics graphics, float delta, CallbackInfo ci) {
		this.iceandfire_fabric$fit(graphics, this.title, 4F);
	}

	@Inject(method = "render", at = @At(value = "INVOKE", target = BACKDROP, ordinal = 2))
	private void iceandfire_fabric$fitSubtitle(GuiGraphics graphics, float delta, CallbackInfo ci) {
		this.iceandfire_fabric$fit(graphics, this.subtitle, 2F);
	}

	/** Narrows what is drawn next (its pose is its own, popped after it) when the text at this scale is wider than the screen. */
	@Unique
	private void iceandfire_fabric$fit(GuiGraphics graphics, @Nullable Component text, float scale) {
		if (text == null) {
			return;
		}
		int width = this.getFont().width(text);
		float room = (graphics.guiWidth() - 8) / scale;
		if (width > room) {
			graphics.pose().scale(room / width, 1F, 1.0F);
		}
	}
}

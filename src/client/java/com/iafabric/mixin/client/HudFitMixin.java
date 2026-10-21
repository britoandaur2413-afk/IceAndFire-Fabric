package com.iafabric.mixin.client;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.Hud;
import net.minecraft.network.chat.Component;
import org.jspecify.annotations.Nullable;
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
@Mixin(Hud.class)
public abstract class HudFitMixin {
	@Unique
	private static final String BACKDROP_TEXT = "Lnet/minecraft/client/gui/GuiGraphicsExtractor;textWithBackdrop(Lnet/minecraft/client/gui/Font;Lnet/minecraft/network/chat/Component;IIII)V";

	@Shadow
	private @Nullable Component overlayMessageString;
	@Shadow
	private @Nullable Component title;
	@Shadow
	private @Nullable Component subtitle;

	@Shadow
	public abstract Font getFont();

	@Inject(method = "extractOverlayMessage", at = @At(value = "INVOKE", target = BACKDROP_TEXT))
	private void iceandfire_fabric$fitOverlay(GuiGraphicsExtractor graphics, DeltaTracker delta, CallbackInfo ci) {
		this.iceandfire_fabric$fit(graphics, this.overlayMessageString, 1F);
	}

	@Inject(method = "extractTitle", at = @At(value = "INVOKE", target = BACKDROP_TEXT, ordinal = 0))
	private void iceandfire_fabric$fitTitle(GuiGraphicsExtractor graphics, DeltaTracker delta, CallbackInfo ci) {
		this.iceandfire_fabric$fit(graphics, this.title, 4F);
	}

	@Inject(method = "extractTitle", at = @At(value = "INVOKE", target = BACKDROP_TEXT, ordinal = 1))
	private void iceandfire_fabric$fitSubtitle(GuiGraphicsExtractor graphics, DeltaTracker delta, CallbackInfo ci) {
		this.iceandfire_fabric$fit(graphics, this.subtitle, 2F);
	}

	/** Narrows what is drawn next (its pose is its own, popped after it) when the text at this scale is wider than the screen. */
	@Unique
	private void iceandfire_fabric$fit(GuiGraphicsExtractor graphics, @Nullable Component text, float scale) {
		if (text == null) {
			return;
		}
		int width = this.getFont().width(text);
		float room = (graphics.guiWidth() - 8) / scale;
		if (width > room) {
			graphics.pose().scale(room / width, 1F);
		}
	}
}

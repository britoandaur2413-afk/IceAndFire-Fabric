package com.iafabric.client.gui;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FastColor;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.entity.LivingEntity;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;

/** The drawing calls the 1.18 screens used, on the 26.x GUI extractor. */
public final class IafGui {
	private IafGui() {
	}

	/** A part of a 256x256 texture. */
	public static void blit(GuiGraphics g, ResourceLocation texture, int x, int y, int u, int v, int width, int height) {
		g.blit(texture, x, y, u, v, width, height, 256, 256);
	}

	/** A part of a texture of any size, with an alpha. */
	public static void blit(GuiGraphics g, ResourceLocation texture, int x, int y, float u, float v, int width, int height, int texWidth, int texHeight, float alpha) {
		blitTinted(g, white(alpha), texture, x, y, width, height, u, v, width, height, texWidth, texHeight);
	}

	/** White with an alpha (0 to 1). */
	public static int white(float alpha) {
		return FastColor.ARGB32.colorFromFloat(alpha, 1.0F, 1.0F, 1.0F);
	}

	/** A region of a texture drawn at a size, tinted by an ARGB color (its alpha too). */
	public static void blitTinted(GuiGraphics g, int argb, ResourceLocation texture, int x, int y, int width, int height, float u, float v,
		int regionWidth, int regionHeight, int texWidth, int texHeight) {
		com.mojang.blaze3d.systems.RenderSystem.enableBlend();
		g.setColor(FastColor.ARGB32.red(argb) / 255.0F, FastColor.ARGB32.green(argb) / 255.0F, FastColor.ARGB32.blue(argb) / 255.0F,
			FastColor.ARGB32.alpha(argb) / 255.0F);
		g.blit(texture, x, y, width, height, u, v, regionWidth, regionHeight, texWidth, texHeight);
		g.setColor(1.0F, 1.0F, 1.0F, 1.0F);
		com.mojang.blaze3d.systems.RenderSystem.disableBlend();
	}

	/** Text without shadow; a color without alpha is made opaque. */
	public static void text(GuiGraphics g, Font font, String text, int x, int y, int color) {
		g.drawString(font, text, x, y, (color & 0xFF000000) == 0 ? FastColor.ARGB32.opaque(color) : color, false);
	}

	public static void centered(GuiGraphics g, Font font, String text, int centerX, int y, int color) {
		text(g, font, text, centerX - font.width(text) / 2, y, color);
	}

	/** For the tests: while not null, each text fit had to narrow, and the most it was (its room over its width). */
	public static @Nullable Map<String, Float> narrowed;

	/**
	 * Text in a space of a given width: a line longer than that (a longer translation) is narrowed to fit it,
	 * keeping its height, so it never runs over what is next to it.
	 */
	public static void fit(GuiGraphics g, Font font, String text, int x, int y, int maxWidth, int color) {
		int width = font.width(text);
		if (width <= maxWidth || width == 0) {
			text(g, font, text, x, y, color);
			return;
		}
		if (narrowed != null) {
			narrowed.merge(text, (float) maxWidth / width, Math::min);
		}
		g.pose().pushPose();
		g.pose().translate(x, y, 0.0F);
		g.pose().scale((float) maxWidth / width, 1.0F, 1.0F);
		text(g, font, text, 0, 0, color);
		g.pose().popPose();
	}

	public static void fitCentered(GuiGraphics g, Font font, String text, int centerX, int y, int maxWidth, int color) {
		fit(g, font, text, centerX - Math.min(maxWidth, font.width(text)) / 2, y, maxWidth, color);
	}

	/** The least a paragraph is shrunk to be all in its box, and the most its letters are then narrowed. */
	private static final float MIN_SCALE = 0.65F;
	private static final float MIN_SQUEEZE = 0.6F;

	/** How a paragraph goes in its box: its lines, its letters' height and their narrowing, and whether it is all there. */
	public record Layout(List<FormattedCharSequence> lines, float scale, float squeeze, boolean whole) {
	}

	/**
	 * A paragraph in a box as wide as width and as tall as maxLines lines at this scale. A longer text (a longer
	 * translation) is drawn smaller, then narrower, just enough to be all in the same box; one line is only narrowed.
	 */
	public static Layout layout(Font font, Component text, int width, float scale, int maxLines) {
		if (maxLines <= 1) {
			int w = font.width(text);
			float squeeze = w * scale <= width || w == 0 ? 1F : width / (w * scale);
			return new Layout(List.of(text.getVisualOrderText()), scale, squeeze, squeeze >= MIN_SQUEEZE);
		}
		float height = maxLines * 10 * scale + 0.01F;
		float s = scale;
		List<FormattedCharSequence> lines = font.split(text, (int) (width / s));
		while (lines.size() * 10 * s > height && s > MIN_SCALE) {
			s = Math.max(MIN_SCALE, s * 0.94F);
			lines = font.split(text, (int) (width / s));
		}
		float q = 1F;
		while (lines.size() * 10 * s > height && q > MIN_SQUEEZE) {
			q = Math.max(MIN_SQUEEZE, q * 0.94F);
			lines = font.split(text, (int) (width / (s * q)));
		}
		int room = Math.max(1, (int) Math.floor(height / (10 * s)));
		boolean whole = lines.size() <= room;
		return new Layout(whole ? lines : lines.subList(0, room), s, q, whole);
	}

	/** A paragraph drawn in its box (see layout), from its left or centered on x, with or without shadow; the height it took. */
	public static int paragraph(GuiGraphics g, Font font, Component text, int x, int y, int width, int color, float scale, int maxLines,
		boolean centered, boolean shadow) {
		Layout layout = layout(font, text, width, scale, maxLines);
		g.pose().pushPose();
		g.pose().translate(x, y, 0.0F);
		g.pose().scale(layout.scale() * layout.squeeze(), layout.scale(), 1.0F);
		for (int i = 0; i < layout.lines().size(); i++) {
			FormattedCharSequence line = layout.lines().get(i);
			g.drawString(font, line, centered ? -font.width(line) / 2 : 0, i * 10, color, shadow);
		}
		g.pose().popPose();
		return Math.round(layout.lines().size() * 10 * layout.scale());
	}

	/** A mob drawn inside a box (it is clipped to it), turned toward the mouse. */
	public static void entityInBox(GuiGraphics g, LivingEntity entity, int x1, int y1, int x2, int y2, int scale, float mouseX, float mouseY) {
		entityInBox(g, entity, x1, y1, x2, y2, scale, 0.0625F, mouseX, mouseY);
	}

	/** Likewise, the point this far (blocks) over the middle of its box in the box's middle. */
	public static void entityInBox(GuiGraphics g, LivingEntity entity, int x1, int y1, int x2, int y2, int scale, float lift, float mouseX, float mouseY) {
		InventoryScreen.renderEntityInInventoryFollowsMouse(g, x1, y1, x2, y2, scale, lift, mouseX, mouseY, entity);
	}

	/** A mob standing with its feet at (x, y), turned toward the mouse, like 1.18 renderEntityInInventory. */
	public static void entity(GuiGraphics g, LivingEntity entity, int x, int y, int scale, float mouseX, float mouseY) {
		int half = Math.max(20, scale * 2);
		InventoryScreen.renderEntityInInventoryFollowsMouse(g, x - half, y - half * 2, x + half, y, scale, 0.0625F, mouseX, mouseY, entity);
	}
}

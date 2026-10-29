package com.iafabric.client.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.Map;

/** The drawing calls the 1.18 screens used, on the 26.x GUI extractor. */
public final class IafGui {
	private IafGui() {
	}

	/** A part of a 256x256 texture. */
	public static void blit(GuiGraphicsExtractor g, Identifier texture, int x, int y, int u, int v, int width, int height) {
		g.blit(RenderPipelines.GUI_TEXTURED, texture, x, y, u, v, width, height, 256, 256);
	}

	/** A part of a texture of any size, with an alpha. */
	public static void blit(GuiGraphicsExtractor g, Identifier texture, int x, int y, float u, float v, int width, int height, int texWidth, int texHeight, float alpha) {
		g.blit(RenderPipelines.GUI_TEXTURED, texture, x, y, u, v, width, height, texWidth, texHeight, ARGB.white(alpha));
	}

	/** Text without shadow; a color without alpha is made opaque. */
	public static void text(GuiGraphicsExtractor g, Font font, String text, int x, int y, int color) {
		g.text(font, text, x, y, (color & 0xFF000000) == 0 ? ARGB.opaque(color) : color, false);
	}

	public static void centered(GuiGraphicsExtractor g, Font font, String text, int centerX, int y, int color) {
		text(g, font, text, centerX - font.width(text) / 2, y, color);
	}

	/** For the tests: while not null, each text fit had to narrow, and the most it was (its room over its width). */
	public static @Nullable Map<String, Float> narrowed;

	/**
	 * Text in a space of a given width: a line longer than that (a longer translation) is narrowed to fit it,
	 * keeping its height, so it never runs over what is next to it.
	 */
	public static void fit(GuiGraphicsExtractor g, Font font, String text, int x, int y, int maxWidth, int color) {
		int width = font.width(text);
		if (width <= maxWidth || width == 0) {
			text(g, font, text, x, y, color);
			return;
		}
		if (narrowed != null) {
			narrowed.merge(text, (float) maxWidth / width, Math::min);
		}
		g.pose().pushMatrix();
		g.pose().translate(x, y);
		g.pose().scale((float) maxWidth / width, 1.0F);
		text(g, font, text, 0, 0, color);
		g.pose().popMatrix();
	}

	public static void fitCentered(GuiGraphicsExtractor g, Font font, String text, int centerX, int y, int maxWidth, int color) {
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
	public static int paragraph(GuiGraphicsExtractor g, Font font, Component text, int x, int y, int width, int color, float scale, int maxLines,
		boolean centered, boolean shadow) {
		Layout layout = layout(font, text, width, scale, maxLines);
		g.pose().pushMatrix();
		g.pose().translate(x, y);
		g.pose().scale(layout.scale() * layout.squeeze(), layout.scale());
		for (int i = 0; i < layout.lines().size(); i++) {
			FormattedCharSequence line = layout.lines().get(i);
			g.text(font, line, centered ? -font.width(line) / 2 : 0, i * 10, color, shadow);
		}
		g.pose().popMatrix();
		return Math.round(layout.lines().size() * 10 * layout.scale());
	}

	/** A mob drawn inside a box (it is clipped to it), turned toward the mouse. */
	public static void entityInBox(GuiGraphicsExtractor g, LivingEntity entity, int x1, int y1, int x2, int y2, int scale, float mouseX, float mouseY) {
		entityInBox(g, entity, x1, y1, x2, y2, scale, 0.0625F, mouseX, mouseY);
	}

	/** Likewise, the point this far (blocks) over the middle of its box in the box's middle. */
	public static void entityInBox(GuiGraphicsExtractor g, LivingEntity entity, int x1, int y1, int x2, int y2, int scale, float lift, float mouseX, float mouseY) {
		InventoryScreen.extractEntityInInventoryFollowsMouse(g, x1, y1, x2, y2, scale, lift, mouseX, mouseY, entity);
	}

	/** A mob standing with its feet at (x, y), turned toward the mouse, like 1.18 renderEntityInInventory. */
	public static void entity(GuiGraphicsExtractor g, LivingEntity entity, int x, int y, int scale, float mouseX, float mouseY) {
		int half = Math.max(20, scale * 2);
		InventoryScreen.extractEntityInInventoryFollowsMouse(g, x - half, y - half * 2, x + half, y, scale, 0.0625F, mouseX, mouseY, entity);
	}

	/** The ids of what is only drawn, from the top down (the world's count up from 1). */
	private static int drawnIds = Integer.MAX_VALUE;

	/**
	 * A mob made only to be drawn in a screen (null without a world). 26.x gives no id to an entity the client makes itself,
	 * and other mods read the id of whatever is drawn (Forgeworks Charms' Hunter Vision crashed the game on the exiled
	 * pixie's picture), so it gets one no entity of the world has.
	 */
	public static <T extends Entity> @Nullable T drawnOnly(EntityType<T> type) {
		ClientLevel level = Minecraft.getInstance().level;
		T entity = level == null ? null : type.create(level, EntitySpawnReason.LOAD);
		if (entity != null) {
			entity.setId(drawnIds--);
		}
		return entity;
	}
}

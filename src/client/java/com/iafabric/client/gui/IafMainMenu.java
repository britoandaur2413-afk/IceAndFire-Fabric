package com.iafabric.client.gui;

import com.iafabric.IafMod;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FastColor;
import net.minecraft.Util;
import org.jetbrains.annotations.Nullable;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Ice and Fire's own title screen, as the original mod had it: its bestiary open on a table, a page turning every
 * two seconds, and on the new pages a drawing or two of its creatures in faded ink, fading in and out; its own
 * splash texts and its name under the game's version. IafConfig.customMainMenu turns it off (the game's panorama).
 */
public final class IafMainMenu {
	private static final ResourceLocation TABLE = IafMod.id("textures/gui/main_menu/table.png");
	private static final ResourceLocation BOOK = IafMod.id("textures/gui/main_menu/bestiary_menu.png");
	private static final ResourceLocation[] PAGES = new ResourceLocation[6];
	/** drawing_1 to drawing_22 and the size of each texture (drawing_0 is not one of them, as in the original). */
	private static final ResourceLocation[] DRAWINGS = new ResourceLocation[22];
	private static final int[] DRAWING_SIZE = new int[22];
	/** A page every 40 ticks: 6 frames of its turn (2 ticks each), then the drawings fade in, stay and fade out. */
	private static final int CYCLE = 40;
	private static final int TURN = 12;
	private static final int FADE = 10;
	private static final int FADE_OUT = 30;
	/** The drawings' ink at its darkest (the original drew them half seen). */
	private static final float INK = 0.5F;

	static {
		for (int i = 0; i < PAGES.length; i++) {
			PAGES[i] = IafMod.id("textures/gui/main_menu/page_" + (i + 1) + ".png");
		}
		for (int i = 0; i < DRAWINGS.length; i++) {
			int n = i + 1;
			DRAWINGS[i] = IafMod.id("textures/gui/main_menu/drawing_" + n + ".png");
			DRAWING_SIZE[i] = n <= 3 || n >= 21 ? 256 : 128;
		}
	}

	private record Picture(int image, int x, int y) {
	}

	private static long cycle = -1;
	private static Picture[] pictures = new Picture[0];
	private static @Nullable List<String> splashes;

	private IafMainMenu() {
	}

	/** The title screen's background: the table, the book, the page turning or its drawings. */
	public static void draw(GuiGraphics g, int width, int height) {
		double ticks = Util.getMillis() / 50.0;
		long n = (long) Math.floor(ticks / CYCLE);
		double at = ticks - n * CYCLE;
		if (n != cycle) {
			cycle = n;
			newPictures();
		}
		g.blit(TABLE, 0, 0, width, height, 0F, 0F, 64, 32, 64, 32);
		int bookWidth = Math.max(1, width - 100);
		g.blit(BOOK, 50, 0, bookWidth, height, 0F, 0F, 512, 512, 512, 512);
		if (at < TURN) {
			int frame = Math.min(PAGES.length - 1, (int) (at / 2));
			g.blit(PAGES[frame], 50, 0, bookWidth, height, 0F, 0F, 512, 512, 512, 512);
			return;
		}
		float fade = at < TURN + FADE ? (float) ((at - TURN) / FADE) : at <= FADE_OUT ? 1F : (float) Math.max(0, 1 - (at - FADE_OUT) / FADE);
		int middleX = width / 2;
		int middleY = height / 5;
		float widthScale = width / 427F;
		float heightScale = height / 427F;
		int size = (int) (Math.min(widthScale, heightScale) * 192);
		for (Picture picture : pictures) {
			int texture = DRAWING_SIZE[picture.image()];
			IafGui.blitTinted(g, IafGui.white(INK * fade + 0.01F), DRAWINGS[picture.image()], (int) (picture.x() * widthScale) + middleX,
				(int) (picture.y() * heightScale) + middleY, size, size, 0F, 0F, texture, texture, texture, texture);
		}
	}

	/** One or two drawings for the new pages, one on each side of the book. */
	private static void newPictures() {
		ThreadLocalRandom random = ThreadLocalRandom.current();
		pictures = new Picture[1 + random.nextInt(2)];
		boolean left = random.nextBoolean();
		for (int i = 0; i < pictures.length; i++) {
			left = !left;
			int x = left ? -15 - random.nextInt(20) - 128 : 30 + random.nextInt(20);
			pictures[i] = new Picture(random.nextInt(DRAWINGS.length - 1), x, random.nextInt(25));
		}
	}

	/** One of the original mod's splash texts (assets/iceandfire_fabric/splashes.txt), or null if it cannot be read. */
	public static @Nullable String splash() {
		if (splashes == null) {
			try (BufferedReader reader = Minecraft.getInstance().getResourceManager().openAsReader(IafMod.id("splashes.txt"))) {
				splashes = reader.lines().map(String::trim).filter(line -> !line.isEmpty()).toList();
			} catch (IOException e) {
				IafMod.LOGGER.warn("Ice and Fire could not read its splash texts", e);
				splashes = List.of();
			}
		}
		return splashes.isEmpty() ? null : splashes.get(ThreadLocalRandom.current().nextInt(splashes.size()));
	}
}

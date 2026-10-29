package com.iafabric.client.gui;

import com.iafabric.message.MessageExiledMemory;
import com.iafabric.pixie.FairyVision;
import net.minecraft.ChatFormatting;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.Nullable;

/**
 * A memory of the player's exiled pixie, seen as a film: black bars slide in at the top and the bottom of the screen
 * and nothing else is on it (no hotbar, life, crosshair nor chat: her words are its subtitles, in the lower bar), the
 * world through the player's own eyes (first person, no hands), and they can neither move, look away, open their pack,
 * throw nor use anything till it is over. In her glade its edges are hazy, as a memory is; a card in the middle names
 * her gift.
 */
public final class MemoryOverlay {
	/** How long the bars take to slide in or out. */
	private static final int SLIDE = 20;
	/** How long her gift's card is shown (fading in and out). */
	private static final int CARD = 120;
	private static boolean active;
	private static long ticks;
	private static long changed = -1000;
	private static @Nullable CameraType before;
	private static @Nullable Component card;
	private static long cardAt;
	/** The world they are seeing (another, the words said in the one before are gone). */
	private static @Nullable Object world;

	private MemoryOverlay() {
	}

	/** Whether a memory plays (the player held, the screen framed). */
	public static boolean active() {
		return active;
	}

	/** Whether the screen is framed still (a memory plays, or its bars are sliding out): the rest of the HUD waits. */
	public static boolean showing() {
		return active || ticks - changed < SLIDE;
	}

	public static void handle(MessageExiledMemory message) {
		switch (message.what) {
			case MessageExiledMemory.ON -> begin();
			case MessageExiledMemory.CARD -> {
				card = message.text.copy().withStyle(ChatFormatting.BOLD);
				cardAt = ticks;
			}
			default -> end();
		}
	}

	private static void begin() {
		Minecraft mc = Minecraft.getInstance();
		if (!active) {
			active = true;
			changed = ticks;
			before = mc.options.getCameraType();
		}
		card = null;
		// through their own eyes, nothing in use nor any pack open
		mc.options.setCameraType(CameraType.FIRST_PERSON);
		if (mc.player != null) {
			if (mc.player.isUsingItem() && mc.gameMode != null) {
				mc.gameMode.releaseUsingItem(mc.player);
			}
			if (mc.screen instanceof AbstractContainerScreen<?>) {
				mc.player.closeContainer();
			}
		}
	}

	private static void end() {
		if (active) {
			active = false;
			changed = ticks;
			if (before != null) {
				Minecraft.getInstance().options.setCameraType(before);
			}
		}
		before = null;
		card = null;
		// (her last words in it stay in the memory)
		ExiledPixieHud.clear();
	}

	/** Out of the game: nothing of a memory is left. */
	public static void clear() {
		if (active && before != null) {
			Minecraft.getInstance().options.setCameraType(before);
		}
		active = false;
		changed = -1000;
		before = null;
		card = null;
	}

	public static void tick() {
		ticks++;
		Minecraft mc = Minecraft.getInstance();
		if (active && mc.player != null && mc.screen instanceof AbstractContainerScreen<?>) {
			mc.player.closeContainer();
		}
		if (active && mc.level != world && world != null) {
			ExiledPixieHud.clear();
		}
		world = mc.level;
	}

	/** Whether the game's HUD (the hotbar, life, crosshair, chat...) is gone: while the screen is framed. */
	public static boolean hidesHud() {
		return showing();
	}

	/** How tall each bar is (an eighth of the screen, about). */
	static int bar(int height) {
		return Math.max(18, Math.round(height * 0.12F));
	}

	private static float ease(float x) {
		x = Mth.clamp(x, 0F, 1F);
		return x * x * (3 - 2 * x);
	}

	/**
	 * The haze at the edges of a memory, in her glade: pale ovals one over another, the middle clear (under the
	 * player's eyes, which close over it).
	 */
	public static void haze(GuiGraphics g, float delta) {
		Minecraft mc = Minecraft.getInstance();
		if (!active || mc.level == null || mc.level.dimension() != FairyVision.GLADE) {
			return;
		}
		int w = g.guiWidth();
		int h = g.guiHeight();
		double cx = w / 2.0;
		double cy = h / 2.0;
		int step = Math.max(1, h / 270);
		for (int y = 0; y < h; y += step) {
			for (int k = 0; k < 6; k++) {
				double scale = 0.72 + 0.075 * k;
				double a = w * 0.62 * scale;
				double b = h * 0.64 * scale;
				double dy = (y + step / 2.0 - cy) / b;
				int color = 0x15F6ECFF;
				if (Math.abs(dy) >= 1) {
					g.fill(0, y, w, y + step, color);
					continue;
				}
				int half = (int) (a * Math.sqrt(1 - dy * dy));
				if (cx - half > 0) {
					g.fill(0, y, (int) cx - half, y + step, color);
					g.fill((int) cx + half, y, w, y + step, color);
				}
			}
		}
	}

	/** Over everything: the bars, the card of her gift, her words as subtitles. */
	public static void render(GuiGraphics g, float delta) {
		if (!showing()) {
			return;
		}
		Minecraft mc = Minecraft.getInstance();
		int w = g.guiWidth();
		int h = g.guiHeight();
		float partial = delta;
		float since = Mth.clamp((ticks - changed + partial) / SLIDE, 0F, 1F);
		int bar = bar(h);
		int cut = Math.round(bar * ease(active ? since : 1F - since));
		if (cut > 0) {
			g.fill(0, 0, w, cut, 0xFF000000);
			g.fill(0, h - cut, w, h, 0xFF000000);
		}
		if (!active) {
			return;
		}
		if (card != null) {
			card(g, mc.font, card, w, h, bar, partial);
		}
		ExiledPixieHud.subtitle(g, mc.font, w, h, bar, partial);
	}

	/** Her gift's name, large in the upper middle (smaller if the screen is narrow), fading in and out. */
	private static void card(GuiGraphics g, Font font, Component text, int w, int h, int bar, float partial) {
		float age = ticks - cardAt + partial;
		float alpha = Mth.clamp(Math.min(age / 15F, (CARD - age) / 20F), 0F, 1F);
		if (alpha <= 0.02F) {
			return;
		}
		int textW = Math.max(1, font.width(text));
		float scale = Math.min(1.5F, (w - 32) / (float) textW);
		int y = bar + Math.round((h - 2 * bar) * 0.2F);
		int band = Math.round(9 * scale) + 12;
		g.fill(0, y - band / 2, w, y + band / 2, (int) (alpha * 0x78) << 24);
		g.pose().pushPose();
		g.pose().translate(w / 2F, y, 0.0F);
		g.pose().scale(scale, scale, 1.0F);
		g.drawString(font, text, -textW / 2, -4, 0x9FEFFF | (int) (alpha * 255) << 24, true);
		g.pose().popPose();
	}
}

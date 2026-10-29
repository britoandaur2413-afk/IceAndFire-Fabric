package com.iafabric.client.gui;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.util.Mth;

/**
 * The player's eyes in a scene of their exiled pixie, over everything on the screen: told to close them, the lids come
 * down and all goes black; told to open them, a slit of light opens in the middle, widens, the heavy lids blink once
 * and the world comes clear, the dark at the edges going last.
 */
public final class EyesOverlay {
	private static final int NONE = 0;
	private static final int CLOSING = 1;
	private static final int CLOSED = 2;
	private static final int OPENING = 3;
	private static int mode = NONE;
	private static long ticks;
	private static long started;
	private static int length = 1;

	private EyesOverlay() {
	}

	public static void start(boolean open, int duration) {
		mode = open ? OPENING : CLOSING;
		started = ticks;
		length = Math.max(1, duration);
	}

	/** Whether they see nothing now (closing done, not opening yet). */
	public static boolean closed() {
		return mode == CLOSED;
	}

	public static void clear() {
		mode = NONE;
	}

	/** For the scenes: how far they are through opening their eyes (0 to 1), or -1 when they are not opening them. */
	public static float opening() {
		return mode == OPENING ? Mth.clamp((ticks - started) / (float) length, 0F, 1F) : -1F;
	}

	public static void tick() {
		ticks++;
		if (mode == CLOSING && ticks - started >= length) {
			mode = CLOSED;
		} else if (mode == OPENING && ticks - started >= length) {
			mode = NONE;
		}
	}

	private static float ease(float x) {
		x = Mth.clamp(x, 0F, 1F);
		return x * x * (3 - 2 * x);
	}

	/** How open the eyes are (0 shut, 1 wide) a part of the way through opening them: a slit, a heavy blink, then open. */
	private static float opening(float t) {
		if (t < 0.4F) {
			return 0.42F * ease(t / 0.4F);
		}
		if (t < 0.55F) {
			return 0.42F - 0.3F * (float) Math.sin((t - 0.4F) / 0.15F * Math.PI);
		}
		return 0.42F + 0.58F * ease((t - 0.55F) / 0.45F);
	}

	public static void render(GuiGraphics g, DeltaTracker delta) {
		if (mode == NONE) {
			return;
		}
		int w = g.guiWidth();
		int h = g.guiHeight();
		if (mode == CLOSED) {
			g.fill(0, 0, w, h, 0xFF000000);
			return;
		}
		float t = Mth.clamp((ticks - started + delta.getGameTimeDeltaPartialTick(true)) / length, 0F, 1F);
		float open = mode == CLOSING ? 1F - ease(t) : opening(t);
		lids(g, w, h, open);
		// the dark of eyes just opened (or closing) over what is seen
		int dark = (int) ((1F - open) * 170);
		if (dark > 0) {
			g.fill(0, 0, w, h, dark << 24);
		}
	}

	/** The lids: black but for an eye-shaped opening in the middle, as wide as the eyes are open, its edge soft. */
	private static void lids(GuiGraphics g, int w, int h, float open) {
		if (open <= 0.001F) {
			g.fill(0, 0, w, h, 0xFF000000);
			return;
		}
		double cx = w / 2.0;
		double cy = h / 2.0;
		double a = w * (0.55 + 0.5 * open);
		double b = h * 0.75 * open;
		int[] feather = {0xC8000000, 0x8C000000, 0x50000000, 0x24000000};
		int band = Math.max(2, h / 90);
		for (int y = 0; y < h; y++) {
			double dy = (y + 0.5 - cy) / b;
			if (Math.abs(dy) >= 1) {
				g.fill(0, y, w, y + 1, 0xFF000000);
				continue;
			}
			int half = (int) (a * Math.sqrt(1 - dy * dy));
			int left = (int) cx - half;
			int right = (int) cx + half;
			if (left > 0) {
				g.fill(0, y, left, y + 1, 0xFF000000);
			}
			if (right < w) {
				g.fill(right, y, w, y + 1, 0xFF000000);
			}
			for (int i = 0; i < feather.length; i++) {
				int l = left + i * band;
				int r = right - (i + 1) * band;
				if (l + band > r) {
					break;
				}
				g.fill(Math.max(0, l), y, Math.max(0, l + band), y + 1, feather[i]);
				g.fill(Math.min(w, r), y, Math.min(w, r + band), y + 1, feather[i]);
			}
		}
	}
}

package com.iafabric.client.texture;

import com.mojang.blaze3d.platform.NativeImage;

/** ARGB pixels of an image (26.x reads and writes ARGB; 1.21.1's NativeImage keeps ABGR). */
public final class Pixels {
	private Pixels() {
	}

	private static int swap(int color) {
		return (color & 0xFF00FF00) | ((color >> 16) & 0xFF) | ((color & 0xFF) << 16);
	}

	public static int argb(NativeImage image, int x, int y) {
		return swap(image.getPixelRGBA(x, y));
	}

	public static void setArgb(NativeImage image, int x, int y, int argb) {
		image.setPixelRGBA(x, y, swap(argb));
	}
}

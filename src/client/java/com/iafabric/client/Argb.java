package com.iafabric.client;

/** ARGB colors as later versions make them (1.20.1's FastColor has no float maker), and their parts as floats. */
public final class Argb {
	private Argb() {
	}

	public static int of(float alpha, float red, float green, float blue) {
		return ((int) (alpha * 255F) & 255) << 24 | ((int) (red * 255F) & 255) << 16 | ((int) (green * 255F) & 255) << 8 | (int) (blue * 255F) & 255;
	}

	public static float alpha(int color) {
		return (color >>> 24) / 255F;
	}

	public static float red(int color) {
		return (color >> 16 & 255) / 255F;
	}

	public static float green(int color) {
		return (color >> 8 & 255) / 255F;
	}

	public static float blue(int color) {
		return (color & 255) / 255F;
	}
}

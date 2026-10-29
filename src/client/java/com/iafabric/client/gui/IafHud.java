package com.iafabric.client.gui;

import net.minecraft.client.gui.GuiGraphics;

/**
 * The mod's HUD, in order: the pixie spell, her checkpoint's pin, her lines, a memory's haze, the player's eyes in her
 * scenes, and a memory's bars and subtitles over everything.
 */
public final class IafHud {
	private IafHud() {
	}

	public static void render(GuiGraphics g, float delta) {
		PixieSpellHud.render(g, delta);
		FairyWaypoint.hud(g, delta);
		ExiledPixieHud.render(g, delta);
		MemoryOverlay.haze(g, delta);
		EyesOverlay.render(g, delta);
		MemoryOverlay.render(g, delta);
	}
}

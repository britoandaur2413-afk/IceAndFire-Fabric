package com.iafabric.client.gui;

import com.iafabric.client.IafKeybinds;
import com.iafabric.message.MessagePixieBondSync;
import com.iafabric.pixie.PixieAbility;
import com.iafabric.pixie.PixieBonds;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;

/**
 * Beside the hotbar, while a pixie is out with a spell chosen: the spell's picture, darkened from the top while it is
 * not ready (a shade that shrinks as the wait ends), greyed when the pixie is not of the level for it, and the key
 * that calls it.
 */
public final class PixieSpellHud {
	private PixieSpellHud() {
	}

	public static void render(GuiGraphicsExtractor g, DeltaTracker delta) {
		Minecraft mc = Minecraft.getInstance();
		if (mc.player == null || mc.level == null || mc.player.isSpectator() || MemoryOverlay.showing()) {
			return;
		}
		PixieBonds bonds = MessagePixieBondSync.client;
		if (bonds.active < 0) {
			return;
		}
		PixieBonds.Bond bond = bonds.get(bonds.active);
		PixieAbility spell = bond.selected();
		if (spell == null || spell.color != bonds.active || !bond.knows(spell)) {
			return;
		}
		int x = g.guiWidth() / 2 - 91 - 52;
		int y = g.guiHeight() - 22;
		g.fill(x, y, x + 20, y + 20, 0x90101018);
		g.outline(x, y, 20, 20, 0xFF606070);
		boolean able = bond.level >= spell.minLevel;
		IafGui.blit(g, GuiPixieMenu.icon(spell), x + 2, y + 2, 0F, 0F, 16, 16, 16, 16, able ? 1F : 0.4F);
		long wait = bond.ready[spell.ordinal()] - mc.level.getGameTime();
		if (wait > 0) {
			int shade = (int) Math.ceil(16 * Math.min(1.0, wait / (double) spell.cooldown));
			g.fill(x + 2, y + 2, x + 18, y + 2 + shade, 0xA0000000);
			String seconds = String.valueOf((wait + 19) / 20);
			IafGui.text(g, mc.font, seconds, x + 10 - mc.font.width(seconds) / 2, y + 6, 0xFFFFFFFF);
		}
		String key = IafKeybinds.pixieMagic.getTranslatedKeyMessage().getString();
		IafGui.text(g, mc.font, key, x + 19 - mc.font.width(key), y + 12, wait > 0 || !able ? 0xFF909090 : 0xFFFFFF80);
	}
}

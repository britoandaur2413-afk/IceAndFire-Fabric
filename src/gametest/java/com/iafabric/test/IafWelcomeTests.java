package com.iafabric.test;

import com.iafabric.gamita.GamitaWelcomeScreen;
import com.iafabric.test.ct.ClientGameTestContext;
import com.iafabric.test.ct.FabricClientGameTest;
import net.minecraft.client.gui.screens.TitleScreen;

/**
 * GAMITA_MODELS' welcome: it comes once before the title screen, in English and Spanish, at a small and a big GUI
 * scale, with the cursor on a button; "Satisfied!" goes on to the title screen.
 */
public class IafWelcomeTests implements FabricClientGameTest {
	@Override
	public void runTest(ClientGameTestContext context) {
		if (IafTestOnly.skip("welcome")) {
			return;
		}
		context.waitFor(mc -> mc.screen instanceof GamitaWelcomeScreen, 200);
		context.waitTicks(10);
		for (String lang : new String[]{"en_us", "es_es"}) {
			context.runOnClient(mc -> {
				mc.getLanguageManager().setSelected(lang);
				mc.options.languageCode = lang;
				mc.reloadResourcePacks();
			});
			context.waitFor(mc -> mc.getOverlay() == null, 600);
			for (int scale : new int[]{2, 4}) {
				context.runOnClient(mc -> {
					mc.options.guiScale().set(scale);
					mc.resizeDisplay();
				});
				context.waitTicks(25);
				context.takeScreenshot("welcome_" + lang + "_scale" + scale);
			}
		}
		// an arrow: the next one named
		context.runOnClient(mc -> {
			if (mc.screen.children().size() > 3) {
				((net.minecraft.client.gui.components.AbstractWidget) mc.screen.children().get(1)).onClick(0, 0);
			}
		});
		context.waitTicks(5);
		// the cursor on Patreon: it grows and glows
		context.runOnClient(mc -> {
			var screen = mc.screen;
			var patreon = screen.children().get(screen.children().size() - 1);
			var w = (net.minecraft.client.gui.components.AbstractWidget) patreon;
			w.setFocused(true);
		});
		context.waitTicks(15);
		context.takeScreenshot("welcome_patreon_hover");
		context.runOnClient(mc -> {
			((net.minecraft.client.gui.components.AbstractWidget) mc.screen.children().get(mc.screen.children().size() - 3)).onClick(0, 0);
			mc.options.guiScale().set(0);
			mc.resizeDisplay();
		});
		context.waitForScreen(TitleScreen.class);
		context.runOnClient(mc -> {
			mc.getLanguageManager().setSelected("en_us");
			mc.options.languageCode = "en_us";
			mc.reloadResourcePacks();
		});
		context.waitFor(mc -> mc.getOverlay() == null, 600);
	}
}

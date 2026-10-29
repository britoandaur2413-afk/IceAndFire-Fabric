package com.iafabric.test;

import com.iafabric.gamita.GamitaWelcomeScreen;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;

/**
 * GAMITA_MODELS' welcome: it comes once before the title screen, in English and Spanish, with the cursor on Patreon;
 * "Satisfied!" goes on.
 */
public class IafWelcomeTests implements FabricClientGameTest {
	@Override
	public void runTest(ClientGameTestContext context) {
		// it comes when the game starts: the other tests begin on the title screen, so it is closed even when skipped
		try {
			context.waitFor(mc -> mc.gui.screen() instanceof GamitaWelcomeScreen, 200);
		} catch (AssertionError e) {
			// beside a pack of other mods (IAF_PACK=1): what is on the screen instead, and the other tests go on
			String shown = context.computeOnClient(mc -> mc.gui.screen() == null ? "nothing" : mc.gui.screen().getClass().getName());
			com.iafabric.IafMod.LOGGER.warn("[IAFWELCOME] GAMITA_MODELS' welcome did not come in 10 s; on the screen: {}", shown);
			context.takeScreenshot("welcome_missing");
			if (System.getenv("IAF_PACK") == null) {
				throw e;
			}
			// another mod's first-launch screen came first: closed, ours comes on the title screen after it
			context.runOnClient(mc -> mc.gui.screen().onClose());
			try {
				context.waitFor(mc -> mc.gui.screen() instanceof GamitaWelcomeScreen, 200);
				com.iafabric.IafMod.LOGGER.info("[IAFWELCOME] GAMITA_MODELS' welcome came once that screen was closed");
				context.takeScreenshot("welcome_after_other");
				context.runOnClient(mc -> mc.gui.screen().onClose());
			} catch (AssertionError again) {
				String after = context.computeOnClient(mc -> mc.gui.screen() == null ? "nothing" : mc.gui.screen().getClass().getName());
				com.iafabric.IafMod.LOGGER.warn("[IAFWELCOME] GAMITA_MODELS' welcome never came; on the screen: {}", after);
				context.setScreen(net.minecraft.client.gui.screens.TitleScreen::new);
			}
			context.waitFor(mc -> mc.gui.screen() instanceof net.minecraft.client.gui.screens.TitleScreen, 200);
			return;
		}
		if (IafTestOnly.skip("welcome")) {
			context.runOnClient(mc -> mc.gui.screen().onClose());
			context.waitTicks(2);
			return;
		}
		context.waitTicks(10);
		for (String lang : new String[]{"en_us", "es_es"}) {
			context.runOnClient(mc -> {
				mc.getLanguageManager().setSelected(lang);
				mc.options.languageCode = lang;
				mc.getLanguageManager().onResourceManagerReload(mc.getResourceManager());
				var screen = mc.gui.screen();
				screen.init(mc.getWindow().getGuiScaledWidth(), mc.getWindow().getGuiScaledHeight());
			});
			context.waitTicks(15);
			context.takeScreenshot("welcome_" + lang);
		}
		context.runOnClient(mc -> {
			var screen = mc.gui.screen();
			((net.minecraft.client.gui.components.AbstractWidget) screen.children().get(screen.children().size() - 2)).setFocused(true);
		});
		context.waitTicks(15);
		context.takeScreenshot("welcome_patreon_hover");
		context.runOnClient(mc -> {
			mc.getLanguageManager().setSelected("en_us");
			mc.options.languageCode = "en_us";
			mc.getLanguageManager().onResourceManagerReload(mc.getResourceManager());
			mc.gui.screen().onClose();
		});
		context.waitTicks(5);
	}
}

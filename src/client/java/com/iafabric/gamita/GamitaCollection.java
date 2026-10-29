package com.iafabric.gamita;

import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.TitleScreen;

import java.util.Comparator;
import java.util.List;

/**
 * GAMITA_MODELS' welcome, shared by all its mods. Each of its mods carries this same code (in its own package) and
 * says so in its fabric.mod.json: {@code "custom": {"gamita_models": {"welcome": 1}}}. When the game starts, every one
 * of them looks at the mods there that say so and only one shows the screen (the one with the newest welcome, then
 * the first by id), once per game, before the title screen: ten of them, still one screen.
 */
public final class GamitaCollection {
	/** This copy's welcome; a newer one (more mods, new texts) is shown instead of an older one. */
	public static final int WELCOME = 2;
	public static final String PATREON = "https://patreon.com/DragonLorePackforMinecraft";
	public static final String DISCORD = "https://discord.gg/t6TssmqKv6";

	private static boolean shown;

	private GamitaCollection() {
	}

	public static void init(String myModId) {
		ScreenEvents.AFTER_INIT.register((client, screen, w, h) -> {
			if (!shown && screen instanceof TitleScreen && myModId.equals(leader())) {
				shown = true;
				// (after this screen has opened, not inside its opening)
				client.tell(() -> client.setScreen(new GamitaWelcomeScreen(screen, myModId, mods())));
			}
		});
	}

	/** GAMITA_MODELS' mods in this game (those that say so), by name. */
	public static List<ModContainer> mods() {
		return FabricLoader.getInstance().getAllMods().stream()
			.filter(m -> m.getMetadata().containsCustomValue("gamita_models"))
			.sorted(Comparator.comparing(m -> m.getMetadata().getName().toLowerCase()))
			.toList();
	}

	private static int welcome(ModContainer mod) {
		try {
			var value = mod.getMetadata().getCustomValue("gamita_models").getAsObject().get("welcome");
			return value == null ? 0 : value.getAsNumber().intValue();
		} catch (RuntimeException e) {
			return 0;
		}
	}

	/** The one of them that shows it. */
	private static String leader() {
		return mods().stream()
			.max(Comparator.<ModContainer>comparingInt(GamitaCollection::welcome)
				.thenComparing(m -> m.getMetadata().getId(), Comparator.reverseOrder()))
			.map(m -> m.getMetadata().getId())
			.orElse("");
	}

	static void open(String url) {
		net.minecraft.Util.getPlatform().openUri(url);
	}

	static Minecraft mc() {
		return Minecraft.getInstance();
	}
}

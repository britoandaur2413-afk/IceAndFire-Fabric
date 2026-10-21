package com.iafabric.test;

import com.iafabric.IafConfig;
import com.iafabric.IafMod;
import com.iafabric.client.gui.ExiledPixieHud;
import com.iafabric.client.gui.GuiBossCard;
import com.iafabric.client.gui.GuiExiledPixie;
import com.iafabric.client.gui.GuiPixieBonds;
import com.iafabric.client.gui.IafGui;
import com.iafabric.message.MessageBossCard;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.gui.screens.worldselection.WorldCreationUiState;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Ice and Fire's own title screen: the bestiary on its table, a page turning, drawings on the new pages, one of its
 * splash texts; with IafConfig.customMainMenu off, the game's own panorama again. And its texts in each of its
 * languages, all in their boxes (smaller or narrower when longer, never cut).
 */
public class IafMenuTests implements FabricClientGameTest {
	/** The mod's languages (each has all its texts: IafGameTests.translations). */
	public static final List<String> LANGUAGES = List.of("en_us", "es_es", "es_ar", "es_cl", "es_ec", "es_mx", "de_de", "fr_fr", "ja_jp", "ko_kr",
		"pl_pl", "pt_br", "ru_ru", "uk_ua", "vi_vn", "zh_cn");
	/** The lines shown over the hotbar (measured bold, as the duel's are). */
	private static final List<String> OVERLAY = List.of("iceandfire_fabric.cryona.debris", "iceandfire_fabric.cryona.lament", "iceandfire_fabric.cryona.mourning_hint",
		"iceandfire_fabric.cryona.shield_broken", "iceandfire_fabric.cryona.shield_mends", "iceandfire_fabric.cryona.spent", "iceandfire_fabric.cryona.vengeance", "iceandfire_fabric.cryona.zero",
		"iceandfire_fabric.frost.breath", "iceandfire_fabric.frost.dive", "iceandfire_fabric.frost.grab", "iceandfire_fabric.frost.melee", "iceandfire_fabric.frost.prison",
		"iceandfire_fabric.frost.pylon_broken", "iceandfire_fabric.frost.pylons", "iceandfire_fabric.frost.stampede", "iceandfire_fabric.frost.super_breath", "iceandfire_fabric.frost.grabbed",
		"iceandfire_fabric.frost.prisoned", "iceandfire_fabric.frost.pylon_crack", "iceandfire_fabric.boss.frost_wound", "iceandfire_fabric.boss.totem_saved", "iceandfire_fabric.castle.sealed",
		"iceandfire_fabric.realm.warded", "iceandfire_fabric.throne.altar_quiet", "iceandfire_fabric.throne.edge", "iceandfire_fabric.throne.need_blood", "iceandfire_fabric.throne.subtitle",
		"iceandfire_fabric.pixie_magic.level", "iceandfire_fabric.pixie_magic.no_pixie", "iceandfire_fabric.pixie_magic.no_target", "iceandfire_fabric.pixie_magic.none",
		"iceandfire_fabric.pixie_magic.rebirth_on", "iceandfire_fabric.pixie_magic.reborn", "iceandfire_fabric.pixie_magic.wait", "exiled.iceandfire_fabric.ability.resting",
		"exiled.iceandfire_fabric.ability.wait", "exiled.iceandfire_fabric.not_met", "iceandfire_fabric.pixie_bond.altar_no_pixie", "iceandfire_fabric.pixie_bond.altar_xp",
		"iceandfire_fabric.pixie_bond.fed", "iceandfire_fabric.pixie_bond.knows", "iceandfire_fabric.pixie_bond.max", "iceandfire_fabric.pixie_bond.not_learnt",
		"iceandfire_fabric.pixie_bond.other_color", "item.iceandfire_fabric.gorgon_head.no_eyes", "myrmex.message.null_hive", "myrmex.message.staff_already_set",
		"myrmex.message.staff_set_named", "myrmex.message.staff_set_unnamed", "dragon.command.new_home", "dragon.command.remove_home",
		"hippogryph.command.new_home", "hippogryph.command.remove_home", "cockatrice.command.new_home", "cockatrice.command.remove_home",
		"amphithere.command.new_home");
	/** A 1280x720 screen at its usual GUI scale (3): her box is 170 wide there, its text 129. */
	private static final int SCREEN = 426;
	private static final int SPEECH_TEXT = 170 - 41;
	/** Narrower than this, a line across the screen is hard to read; smaller than this, a menu's text. */
	private static final float LEAST_SQUEEZE = 0.6F;
	private static final float LEAST_LETTERS = 0.7F;
	private static void log(String message) {
		IafMod.LOGGER.info("[IAFMENU] {}", message);
	}

	@Override
	public void runTest(ClientGameTestContext context) {
		if (System.getenv("IAF_BATTLE") != null || IafTestOnly.skip("menu")) {
			return;
		}
		context.setScreen(TitleScreen::new);
		context.waitForScreen(TitleScreen.class);
		context.waitTicks(5);
		String splash = context.computeOnClient(mc -> {
			try {
				var field = TitleScreen.class.getDeclaredField("splash");
				field.setAccessible(true);
				Object renderer = field.get(mc.gui.screen());
				if (renderer == null) {
					return null;
				}
				var text = renderer.getClass().getDeclaredField("splash");
				text.setAccessible(true);
				return ((net.minecraft.network.chat.Component) text.get(renderer)).getString();
			} catch (ReflectiveOperationException e) {
				throw new RuntimeException(e);
			}
		});
		boolean ours = context.computeOnClient(mc -> {
			try (var reader = mc.getResourceManager().openAsReader(IafMod.id("splashes.txt"))) {
				return reader.lines().map(String::trim).anyMatch(line -> line.equals(splash));
			} catch (java.io.IOException e) {
				return false;
			}
		});
		log("the title screen's splash: \"" + splash + "\", one of Ice and Fire's: " + ours);
		if (!ours) {
			throw new AssertionError("the title screen's splash is not one of Ice and Fire's: " + splash);
		}
		fit(context);
		// (the pages turn by the clock: a shot while one turns, then one with the drawings in full ink)
		phase(context, 4, 10);
		context.takeScreenshot("menu_page_turning");
		phase(context, 23, 29);
		context.takeScreenshot("menu_drawings");
		IafConfig.customMainMenu = false;
		try {
			context.setScreen(TitleScreen::new);
			context.waitTicks(10);
			context.takeScreenshot("menu_vanilla");
		} finally {
			IafConfig.customMainMenu = true;
		}
		labels(context);
		context.setScreen(TitleScreen::new);
		context.waitTicks(2);
	}

	/**
	 * In a world where her quests are all done (`/iceandfire fairy complete`), the mod's screens drawn in each language:
	 * the exiled pixie's three tabs, the bonds, the duel's card (both tabs). No line of one row (a button, a tab, a
	 * power's name) may be narrowed past LEAST_SQUEEZE (IafGui.narrowed keeps how much each one was).
	 */
	private static void labels(ClientGameTestContext context) {
		String before = context.computeOnClient(mc -> mc.options.languageCode);
		List<String> problems = new ArrayList<>();
		try (TestSingleplayerContext world = context.worldBuilder().adjustSettings(settings -> {
			settings.setGameMode(WorldCreationUiState.SelectedGameMode.SURVIVAL);
			settings.setAllowCommands(true);
		}).create()) {
			world.getServer().runCommand("gamerule spawn_mobs false");
			world.getServer().runCommand("time set noon");
			context.waitTicks(20);
			world.getServer().runCommand("execute as @p run iceandfire fairy complete");
			context.waitTicks(40);
			try {
				for (String code : LANGUAGES) {
					context.runOnClient(mc -> {
						language(mc, code);
						IafGui.narrowed = new HashMap<>();
					});
					context.setScreen(GuiExiledPixie::new);
					context.waitTicks(2);
					for (int tab = 0; tab < 3; tab++) {
						int shown = tab;
						context.runOnClient(mc -> ((GuiExiledPixie) mc.gui.screen()).show(shown, shown == 1 ? 0 : -1));
						context.waitTicks(2);
					}
					context.setScreen(GuiPixieBonds::new);
					context.waitTicks(2);
					context.setScreen(() -> new GuiBossCard(new MessageBossCard(-1, -1, 10000, 10000, 3000, 3000, 1000, 1000, 2, false, false)));
					context.waitTicks(2);
					context.runOnClient(mc -> ((GuiBossCard) mc.gui.screen()).show(1));
					context.waitTicks(2);
					Map<String, Float> narrowed = context.computeOnClient(mc -> {
						Map<String, Float> drawn = IafGui.narrowed;
						IafGui.narrowed = null;
						return drawn;
					});
					List<String> narrowest = narrowed.entrySet().stream().sorted(Map.Entry.comparingByValue())
						.map(entry -> Math.round(entry.getValue() * 100) + "% " + entry.getKey()).limit(3).toList();
					log(code + ": " + narrowed.size() + " labels narrowed to fit" + (narrowest.isEmpty() ? "" : ", the most: " + String.join(" | ", narrowest)));
					narrowed.forEach((text, squeeze) -> {
						if (squeeze < LEAST_SQUEEZE) {
							problems.add(code + ": " + Math.round(squeeze * 100) + "% " + text);
						}
					});
				}
			} finally {
				context.runOnClient(mc -> {
					IafGui.narrowed = null;
					mc.gui.setScreen(null);
					language(mc, before);
				});
			}
		}
		if (!problems.isEmpty()) {
			throw new AssertionError(problems.size() + " labels are narrowed too much: " + String.join(" | ", problems));
		}
	}

	/** In each of its languages, the mod's texts all go in their boxes (smaller or narrower when longer, never cut). */
	private static void fit(ClientGameTestContext context) {
		String before = context.computeOnClient(mc -> mc.options.languageCode);
		List<String> problems = new ArrayList<>();
		try {
			for (String code : LANGUAGES) {
				context.runOnClient(mc -> language(mc, code));
				List<String> unfit = context.computeOnClient(IafMenuTests::unfit);
				log(code + ": " + (unfit.isEmpty() ? "all its texts go in their boxes" : unfit.size() + " do not: " + String.join(" | ", unfit)));
				unfit.forEach(text -> problems.add(code + ": " + text));
			}
		} finally {
			context.runOnClient(mc -> language(mc, before));
		}
		if (!problems.isEmpty()) {
			throw new AssertionError(problems.size() + " texts do not go in their boxes: " + String.join(" | ", problems));
		}
	}

	/** The game in this language (its texts reloaded). */
	public static void language(Minecraft mc, String code) {
		mc.getLanguageManager().setSelected(code);
		mc.options.languageCode = code;
		mc.getLanguageManager().onResourceManagerReload(mc.getResourceManager());
	}

	/**
	 * The texts that do not all go in their boxes in the game's language now: the exiled pixie's menu, the boss card's
	 * strip, the bonds' cards, her lines in her box (and the vision's as subtitles), the lines over the hotbar and the
	 * duel's title. Her lines that need smaller letters are logged.
	 */
	private static List<String> unfit(Minecraft mc) {
		List<String> out = new ArrayList<>();
		GuiExiledPixie.unfit(LEAST_LETTERS).forEach(text -> out.add("pixie menu: " + text));
		GuiBossCard.unfit(LEAST_LETTERS).forEach(text -> out.add("boss card: " + text));
		if (!GuiPixieBonds.needFits(LEAST_LETTERS)) {
			out.add("bonds: " + Component.translatable("gui.iceandfire_fabric.pixie_bonds.need_pixie").getString());
		}
		int smaller = 0;
		float least = 1F;
		String smallest = "";
		for (String key : keys(mc)) {
			if (!key.startsWith("exiled.iceandfire_fabric.") || key.endsWith(".desc")) {
				continue;
			}
			Component text = Component.translatable(key, "Alex", "12", "Alex");
			IafGui.Layout layout = ExiledPixieHud.speechLayout(mc.font, text, SPEECH_TEXT);
			if (!layout.whole() || layout.scale() * layout.squeeze() < LEAST_LETTERS) {
				out.add("her box: " + text.getString());
			} else if (layout.scale() * layout.squeeze() < 1F) {
				smaller++;
				if (layout.scale() * layout.squeeze() < least) {
					least = layout.scale() * layout.squeeze();
					smallest = key;
				}
			}
			if (key.startsWith("exiled.iceandfire_fabric.vision.") && !ExiledPixieHud.subtitleFits(mc.font, text, SCREEN)) {
				out.add("subtitle: " + text.getString());
			}
		}
		log(mc.options.languageCode + ": " + smaller + " of her lines take smaller letters in her box" + (smaller > 0 ? ", the least "
			+ Math.round(least * 100) + "% (" + smallest + ")" : ""));
		for (String key : OVERLAY) {
			Component text = Component.translatable(key, "Alex", "12", "Alex", "3").withStyle(ChatFormatting.BOLD);
			if (mc.font.width(text) * LEAST_SQUEEZE > SCREEN - 8) {
				out.add("over the hotbar: " + text.getString());
			}
		}
		for (String key : List.of("iceandfire_fabric.throne.title")) {
			Component text = Component.translatable(key);
			if (mc.font.width(text) * 4 * LEAST_SQUEEZE > SCREEN - 8) {
				out.add("title: " + text.getString());
			}
		}
		return out;
	}

	/** The mod's text keys (its English file has them all). */
	private static List<String> keys(Minecraft mc) {
		try (var reader = mc.getResourceManager().openAsReader(IafMod.id("lang/en_us.json"))) {
			return new ArrayList<>(com.google.gson.JsonParser.parseReader(reader).getAsJsonObject().keySet());
		} catch (java.io.IOException e) {
			throw new RuntimeException(e);
		}
	}

	/** Waits until the menu's two-second page is between these ticks of it. */
	private static void phase(ClientGameTestContext context, int from, int to) {
		for (int i = 0; i < 400; i++) {
			double at = (Util.getMillis() / 50.0) % 40;
			if (at >= from && at < to) {
				return;
			}
			context.waitTick();
		}
		throw new AssertionError("the menu's page never came between " + from + " and " + to);
	}
}

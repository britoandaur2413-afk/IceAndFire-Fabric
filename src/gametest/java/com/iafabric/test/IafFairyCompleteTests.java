package com.iafabric.test;

import com.iafabric.IafMod;
import com.iafabric.client.gui.GuiExiledPixie;
import com.iafabric.pixie.ExiledQuest;
import com.mojang.blaze3d.platform.InputConstants;
import com.iafabric.test.ct.FabricClientGameTest;
import com.iafabric.test.ct.ClientGameTestContext;
import com.iafabric.test.ct.TestSingleplayerContext;
import net.minecraft.client.gui.screens.worldselection.WorldCreationUiState;
import net.minecraft.server.level.ServerPlayer;

import java.util.Set;

/**
 * The test command (IAF_ONLY=complete): a player who never met her types /iceandfire fairy complete in the chat, and her
 * pixie and the strength pixie are out with them at once; the pixies' menu shows the strength pixie at level 5 with
 * its spells, hers every quest done but the last (its moment to come).
 */
public class IafFairyCompleteTests implements FabricClientGameTest {
	private static final int GROUND = -60;

	private static void log(String message) {
		IafMod.LOGGER.info("[IAFCOMPLETE] {}", message);
	}

	@Override
	public void runTest(ClientGameTestContext context) {
		if (System.getenv("IAF_BATTLE") != null || IafTestOnly.skip("complete")) {
			return;
		}
		try (TestSingleplayerContext world = context.worldBuilder().adjustSettings(settings -> {
			settings.setGameMode(WorldCreationUiState.SelectedGameMode.SURVIVAL);
			settings.setAllowCommands(true);
		}).create()) {
			world.getServer().runCommand("gamerule doDaylightCycle false");
			world.getServer().runCommand("gamerule doMobSpawning false");
			world.getServer().runCommand("time set noon");
			world.getServer().runCommand("weather clear");
			context.runOnClient(mc -> {
				if (mc.options.hideGui) {
					mc.options.hideGui = !mc.options.hideGui;
				}
				mc.getLanguageManager().setSelected("es_es");
				mc.options.languageCode = "es_es";
				mc.getLanguageManager().onResourceManagerReload(mc.getResourceManager());
			});
			world.getServer().runOnServer(server -> {
				ServerPlayer player = server.getPlayerList().getPlayers().get(0);
				player.teleportTo(server.overworld(), 0.5, GROUND, 0.5, Set.of(), 0, 10);
			});
			context.waitTicks(30);
			// typed in the chat, as a player would
			context.getInput().pressKey(InputConstants.getKey("key.keyboard.t"));
			context.waitTicks(3);
			context.getInput().typeChars("/iceandfire fairy complete");
			context.waitTicks(2);
			context.getInput().pressKey(InputConstants.getKey("key.keyboard.enter"));
			context.waitTicks(50);
			String state = world.getServer().computeOnServer(server -> {
				ServerPlayer player = server.getPlayerList().getPlayers().get(0);
				var data = com.iafabric.pixie.ExiledPixies.get(player);
				var bonds = com.iafabric.pixie.PixieBonding.get(player);
				return "her stage " + data.stage + ", " + ExiledQuest.left(data) + " quests left, level " + data.level + ", her pixie out "
					+ (com.iafabric.pixie.ExiledPixies.living(player) != null) + "; strength pixie level " + bonds.get(0).level + ", out "
					+ (com.iafabric.pixie.PixieBonding.activePixie(player) != null);
			});
			log("after /iceandfire fairy complete typed in the chat: " + state);
			if (!state.contains("0 quests left") || !state.contains("her pixie out true") || !state.endsWith("out true")) {
				throw new AssertionError("the command did not leave her quests done and both pixies out: " + state);
			}
			context.takeScreenshot("fairy_complete");
			context.getInput().pressKey(InputConstants.getKey("key.keyboard.h"));
			context.waitTicks(10);
			context.takeScreenshot("fairy_complete_bonds");
			context.runOnClient(mc -> mc.setScreen(null));
			context.waitTicks(3);
			context.getInput().pressKey(InputConstants.getKey("key.keyboard.j"));
			context.waitTicks(10);
			context.runOnClient(mc -> {
				if (mc.screen instanceof GuiExiledPixie menu) {
					menu.show(1, ExiledQuest.HYDRA.ordinal());
				}
			});
			context.waitTicks(3);
			context.takeScreenshot("fairy_complete_quests");
			context.runOnClient(mc -> {
				mc.setScreen(null);
				mc.getLanguageManager().setSelected("en_us");
				mc.options.languageCode = "en_us";
				mc.getLanguageManager().onResourceManagerReload(mc.getResourceManager());
			});
			context.waitTicks(3);
		}
	}
}

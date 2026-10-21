package com.iafabric.test;

import com.iafabric.IafMod;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.client.gui.screens.worldselection.WorldCreationUiState;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;

/**
 * Battles with the Mod Tester mod (in run/clientGameTest/mods): only when the environment variable
 * IAF_BATTLE holds a modtest command ("suite iceandfire_fabric 20", "fight iceandfire_fabric:ice_dragon plains 2 diamond tough 40").
 * A normal world, the player watching as a spectator next to each fight, a picture of each one, until the
 * tester writes its report (run/clientGameTest/modtester/reports/latest.md).
 */
public class IafBattleTests implements FabricClientGameTest {
	private static void log(String message) {
		IafMod.LOGGER.info("[IAFBATTLE] {}", message);
	}

	@Override
	public void runTest(ClientGameTestContext context) {
		String battle = System.getenv("IAF_BATTLE");
		if (battle == null || battle.isBlank()) {
			return;
		}
		try (TestSingleplayerContext world = context.worldBuilder().adjustSettings(settings -> {
			settings.setGameMode(WorldCreationUiState.SelectedGameMode.CREATIVE);
			// A normal world (the test default is flat): the tester looks for plains, deserts, seas...
			for (WorldCreationUiState.WorldTypeEntry entry : settings.getNormalPresetList()) {
				if (entry.preset() != null && entry.preset().is(net.minecraft.world.level.levelgen.presets.WorldPresets.NORMAL)) {
					settings.setWorldType(entry);
				}
			}
		}).create()) {
			world.getServer().runCommand("gamerule spawn_mobs false");
			world.getServer().runCommand("gamerule advance_time false");
			world.getServer().runCommand("time set day");
			world.getServer().runCommand("gamemode spectator @a");
			context.runOnClient(mc -> {
				if (!mc.gui.hud.isHidden()) {
					mc.gui.hud.toggle();
				}
			});
			context.waitTicks(40);
			Path done = context.computeOnClient(mc -> mc.gameDirectory.toPath()).resolve("modtester").resolve("reports").resolve("latest.done");
			log("running modtest " + battle);
			world.getServer().runCommand("modtest " + battle);
			context.waitTicks(60);
			if (!Files.exists(done) && !Files.exists(done.resolveSibling("latest.running"))) {
				log("the tester did not start (is the Mod Tester jar built? is the command right?)");
				return;
			}
			int shot = 0;
			String lastTag = "";
			for (int waited = 0; waited < 20 * 60 * 90 && !Files.exists(done); waited += 100) {
				// Watch the fight: the spectator moves next to the creature the tester summoned.
				String[] found = new String[1];
				double[] at = new double[3];
				world.getServer().runOnServer(server -> {
					for (Entity entity : server.overworld().getAllEntities()) {
						for (String tag : entity.entityTags()) {
							if (tag.startsWith("modtester_") && entity.isAlive()) {
								found[0] = tag + " " + entity.getType().toShortString();
								at[0] = entity.getX();
								at[1] = entity.getY() + entity.getBbHeight() * 0.5;
								at[2] = entity.getZ();
							}
						}
					}
				});
				if (found[0] != null) {
					Vec3 eye = new Vec3(at[0] - 12, at[1] + 6, at[2] - 12);
					world.getServer().runCommand(String.format(Locale.ROOT, "tp @a[gamemode=spectator] %.1f %.1f %.1f facing %.1f %.1f %.1f", eye.x, eye.y, eye.z, at[0], at[1], at[2]));
					if (!found[0].split(" ")[0].equals(lastTag)) {
						lastTag = found[0].split(" ")[0];
						String name = found[0].split(" ")[1].replace(':', '_');
						// One picture as it arrives (before the bots reach it), one during the fight.
						context.waitTicks(3);
						context.takeScreenshot("battle_" + shot + "a_" + name);
						context.waitTicks(50);
						context.takeScreenshot("battle_" + (shot++) + "b_" + name);
						continue;
					}
				}
				context.waitTicks(100);
			}
			log("battle done: " + Files.exists(done));
			// The big features made while the bots travelled: a picture inside each (are they whole?).
			for (var type : com.iafabric.world.IafWorldData.FeatureType.values()) {
				for (var entry : com.iafabric.world.IafWorldData.lastGenerated(type)) {
					var pos = entry.getSecond();
					boolean under = type == com.iafabric.world.IafWorldData.FeatureType.UNDERGROUND;
					// The feature starts at its chunk and finds its own height: look for the creature living there.
					double[] at = {pos.getX() + 8, pos.getY(), pos.getZ() + 8};
					world.getServer().runCommand(String.format(Locale.ROOT, "tp @a[gamemode=spectator] %d %d %d", pos.getX() + 8, 120, pos.getZ() + 8));
					context.waitTicks(60);
					world.getServer().runOnServer(server -> {
						var level = server.overworld();
						var box = new net.minecraft.world.phys.AABB(pos.getX() - 40, level.getMinY(), pos.getZ() - 40, pos.getX() + 56, level.getMaxY(), pos.getZ() + 56);
						for (var mob : level.getEntitiesOfClass(net.minecraft.world.entity.Mob.class, box, m -> m.getType().toShortString().contains("dragon")
							|| m.getType().toShortString().contains("hydra") || m.getType().toShortString().contains("cyclops") || m.getType().toShortString().contains("siren")
							|| m.getType().toShortString().contains("pixie") || m.getType().toShortString().contains("myrmex"))) {
							at[0] = mob.getX();
							at[1] = mob.getY() + mob.getBbHeight() * 0.5;
							at[2] = mob.getZ();
							log(entry.getFirst() + ": " + mob.getType().toShortString() + " at " + mob.blockPosition());
							break;
						}
					});
					log("generated " + entry.getFirst() + " near " + pos);
					world.getServer().runCommand(String.format(Locale.ROOT, "tp @a[gamemode=spectator] %.1f %.1f %.1f facing %.1f %.1f %.1f",
						at[0] - (under ? 8 : 25), at[1] + (under ? 4 : 20), at[2] - (under ? 8 : 25), at[0], at[1], at[2]));
					context.waitTicks(100);
					context.takeScreenshot("worldgen_" + entry.getFirst());
				}
			}
			world.getServer().runOnServer(server -> log("chunks still waiting for feature blocks: "
				+ com.iafabric.world.PendingWorldgen.waitingChunks(server.overworld())));
		}
	}
}

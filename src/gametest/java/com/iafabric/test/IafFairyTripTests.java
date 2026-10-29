package com.iafabric.test;

import com.iafabric.IafMod;
import com.iafabric.client.gui.ExiledPixieHud;
import com.iafabric.message.MessageExiledAction;
import com.iafabric.pixie.ExiledPixieData;
import com.iafabric.pixie.ExiledPixies;
import com.iafabric.pixie.ExiledQuest;
import com.iafabric.pixie.ExiledTalk;
import com.iafabric.test.ct.FabricClientGameTest;
import com.iafabric.test.ct.ClientGameTestContext;
import com.iafabric.test.ct.TestSingleplayerContext;
import net.minecraft.client.gui.screens.worldselection.WorldCreationUiState;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.presets.WorldPresets;

import java.util.Set;

/**
 * Her first quests in a real world (IAF_ONLY=trip): the nearest village there really is, found by her and led to, an
 * anecdote of villagers on the way, the village reached and a word about its villagers; then the nearest witches'
 * cottage the world made, a joke about witches on the way, their chest shown, full of cakes, one taken; then her
 * friends, from near a pixies' cave (its way in first, then its plaza among the pixies) and from near the great
 * tree's sanctuary.
 */
public class IafFairyTripTests implements FabricClientGameTest {
	private static void log(String message) {
		IafMod.LOGGER.info("[IAFTRIP] {}", message);
	}

	@Override
	public void runTest(ClientGameTestContext context) {
		if (System.getenv("IAF_BATTLE") != null || IafTestOnly.skip("trip")) {
			return;
		}
		try (TestSingleplayerContext world = context.worldBuilder().adjustSettings(settings -> {
			settings.setGameMode(WorldCreationUiState.SelectedGameMode.CREATIVE);
			settings.setGenerateStructures(true);
			String seed = System.getenv("IAF_SEED");
			if (seed != null && !seed.isBlank()) {
				settings.setSeed(seed);
			}
			for (WorldCreationUiState.WorldTypeEntry entry : settings.getNormalPresetList()) {
				if (entry.preset() != null && entry.preset().is(WorldPresets.NORMAL)) {
					settings.setWorldType(entry);
				}
			}
		}).create()) {
			world.getServer().runOnServer(server -> log("world seed " + server.overworld().getSeed()));
			world.getServer().runCommand("gamerule doDaylightCycle false");
			world.getServer().runCommand("gamerule doMobSpawning false");
			world.getServer().runCommand("time set noon");
			world.getServer().runCommand("weather clear");
			context.runOnClient(mc -> {
				// (a scene before may have hidden it)
				if (mc.options.hideGui) {
					mc.options.hideGui = !mc.options.hideGui;
				}
				mc.getLanguageManager().setSelected("es_es");
				mc.options.languageCode = "es_es";
				mc.getLanguageManager().onResourceManagerReload(mc.getResourceManager());
			});
			context.waitTicks(20);
			world.getServer().runOnServer(server -> {
				ServerPlayer player = server.getPlayerList().getPlayers().get(0);
				ExiledPixies.meet(player, player.blockPosition().above(2));
				ExiledPixies.act(player, MessageExiledAction.ADOPT, 0);
				ExiledPixieData data = ExiledPixies.get(player);
				data.talk = 2;
				player.setAttached(ExiledPixies.DATA, data);
			});
			context.waitTicks(220);
			context.runOnClient(mc -> ExiledPixieHud.clear());
			world.getServer().runOnServer(server -> {
				ServerPlayer player = server.getPlayerList().getPlayers().get(0);
				ExiledPixieData data = ExiledPixies.get(player);
				data.nextOffer = 0;
				player.setAttached(ExiledPixies.DATA, data);
			});
			context.waitTicks(40);
			context.takeScreenshot("trip_offer");
			ExiledPixieData offered = data(world);
			log("her first offer: " + offered.offer());
			if (offered.offer() != ExiledQuest.VILLAGE) {
				throw new AssertionError("her first offer is not the village: " + offered.offer());
			}
			trip(context, world, ExiledQuest.VILLAGE, "village");
			// the cake: offered next
			world.getServer().runOnServer(server -> {
				ServerPlayer player = server.getPlayerList().getPlayers().get(0);
				ExiledPixieData data = ExiledPixies.get(player);
				data.nextOffer = 0;
				player.setAttached(ExiledPixies.DATA, data);
			});
			context.runOnClient(mc -> ExiledPixieHud.clear());
			context.waitTicks(40);
			context.takeScreenshot("trip_cake_offer");
			ExiledPixieData cake = data(world);
			log("her offer after the village: " + cake.offer());
			if (cake.offer() != ExiledQuest.CAKE) {
				throw new AssertionError("after the village she does not offer the cake: " + cake.offer());
			}
			trip(context, world, ExiledQuest.CAKE, "cottage");
			friends(context, world, "pixie_cavern", "cave");
			friends(context, world, "pixie_sanctuary", "tree");
		}
	}

	/** Her friends' quest taken up from a little way off a place of pixies: led there, her ring among them, her words. */
	private static void friends(ClientGameTestContext context, TestSingleplayerContext world, String structure, String name) {
		int[] at = IafWorldTests.locate(context, world, "iceandfire_fabric:" + structure);
		if (at == null) {
			throw new AssertionError("no " + structure + " found by /locate");
		}
		BlockPos place = new BlockPos(at[0], 0, at[1]);
		go(world, at[0] + 60.5, at[1] + 0.5, place, 4, 10);
		context.waitTicks(40);
		context.runOnClient(mc -> ExiledPixieHud.clear());
		world.getServer().runOnServer(server -> {
			ServerPlayer player = server.getPlayerList().getPlayers().get(0);
			ExiledPixieData data = ExiledPixies.get(player);
			data.done |= ExiledQuest.VILLAGE.bit() | ExiledQuest.CAKE.bit();
			data.done &= ~ExiledQuest.FRIENDS.bit();
			data.quest = -1;
			data.offered = -1;
			data.target = null;
			data.via = null;
			data.checkpoint = null;
			data.progress = 0;
			player.setAttached(ExiledPixies.DATA, data);
			ExiledPixies.act(player, MessageExiledAction.ACCEPT, ExiledQuest.FRIENDS.ordinal());
		});
		ExiledPixieData found = null;
		for (int i = 0; i < 60 && (found == null || found.target == null); i++) {
			context.waitTicks(10);
			found = data(world);
		}
		BlockPos target = found.target;
		BlockPos via = found.via;
		log("friends (" + name + "): her place " + (target == null ? "not found" : target.toShortString()) + ", the way in "
			+ (via == null ? "none" : via.toShortString()));
		if (target == null) {
			throw new AssertionError("she found no place of her friends from near the " + structure);
		}
		if (structure.equals("pixie_cavern") != (via != null)) {
			throw new AssertionError("the way in is not what the place asks for: " + structure + ", way in " + via);
		}
		context.waitTicks(20);
		context.takeScreenshot("trip_friends_" + name + "_way");
		if (via != null) {
			// at the arch of its way in: she says it is down there; then down at its plaza
			world.getServer().runOnServer(server -> {
				ServerPlayer player = server.getPlayerList().getPlayers().get(0);
				player.getAbilities().flying = false;
				player.onUpdateAbilities();
				float yaw = (float) Math.toDegrees(Math.atan2(-(target.getX() + 0.5 - via.getX() - 0.5), target.getZ() + 0.5 - via.getZ() - 0.5));
				player.teleportTo(server.overworld(), via.getX() + 0.5, via.getY(), via.getZ() + 0.5, Set.of(), yaw, 10);
			});
			context.waitTicks(40);
			context.takeScreenshot("trip_friends_" + name + "_door");
			ExiledPixieData inside = data(world);
			log("friends (" + name + "): at its way in, the way in left " + (inside.via == null));
			if (inside.via != null) {
				throw new AssertionError("reaching the cave's way in did not lead on to its plaza");
			}
			world.getServer().runOnServer(server -> {
				ServerPlayer player = server.getPlayerList().getPlayers().get(0);
				player.getAbilities().flying = true;
				player.onUpdateAbilities();
				player.teleportTo(server.overworld(), target.getX() + 8.5, target.getY() + 2, target.getZ() + 0.5, Set.of(), 90, 15);
			});
		} else {
			go(world, target.getX() + 0.5 + 26, target.getZ() + 0.5, target, 3, 12);
		}
		BlockPos ring = null;
		for (int i = 0; i < 30 && ring == null; i++) {
			context.waitTicks(10);
			ring = data(world).checkpoint;
		}
		int[] ground = {0};
		BlockPos lantern = ring;
		if (ring != null) {
			world.getServer().runOnServer(server -> ground[0] = server.overworld().getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, lantern.getX(), lantern.getZ()));
		}
		log("friends (" + name + "): her ring " + (ring == null ? "none" : ring.toShortString() + " (the land's top there " + ground[0] + ")"));
		if (ring == null) {
			throw new AssertionError("she made no ring at her friends' place (" + name + ")");
		}
		if (via == null && ring.getY() < ground[0] - 2) {
			throw new AssertionError("her ring at the sanctuary is under the land");
		}
		double rx = ring.getX() - target.getX();
		double rz = ring.getZ() - target.getZ();
		if (via != null && (rx * rx + rz * rz > 9 * 9 || Math.abs(ring.getY() - target.getY()) > 4)) {
			throw new AssertionError("her ring in the pixies' cave is not on its plaza: " + ring.toShortString() + ", the plaza at " + target.toShortString());
		}
		context.runOnClient(mc -> ExiledPixieHud.clear());
		world.getServer().runOnServer(server -> {
			ServerPlayer player = server.getPlayerList().getPlayers().get(0);
			player.getAbilities().flying = false;
			player.onUpdateAbilities();
			player.teleportTo(server.overworld(), lantern.getX() + 0.5, lantern.getY(), lantern.getZ() + 1.4, Set.of(), 180, 5);
		});
		context.waitTicks(30);
		context.takeScreenshot("trip_friends_" + name + "_sad");
		context.waitTicks(105);
		context.takeScreenshot("trip_friends_" + name + "_look");
		context.waitTicks(110);
		context.takeScreenshot("trip_friends_" + name + "_cake");
		int[] pixies = {0};
		world.getServer().runOnServer(server -> pixies[0] = server.overworld().getEntitiesOfClass(com.iafabric.entity.EntityPixie.class,
			new net.minecraft.world.phys.AABB(lantern).inflate(32), p -> !(p instanceof com.iafabric.entity.EntityExiledPixie)).size());
		ExiledPixieData there = data(world);
		log("friends (" + name + "): in her ring " + ((there.progress & 1) != 0) + ", pixies about " + pixies[0]);
		if ((there.progress & 1) == 0 || pixies[0] == 0) {
			throw new AssertionError("her friends' place was not reached, or there are no pixies about (" + name + ")");
		}
	}

	private static ExiledPixieData data(TestSingleplayerContext world) {
		ExiledPixieData[] data = new ExiledPixieData[1];
		world.getServer().runOnServer(server -> data[0] = ExiledPixies.get(server.getPlayerList().getPlayers().get(0)));
		return data[0];
	}

	/** Teleports the player (flying) to a column's ground, so far up, facing a place; its land made first. */
	private static void go(TestSingleplayerContext world, double x, double z, BlockPos facing, int up, float pitch) {
		world.getServer().runOnServer(server -> {
			ServerPlayer player = server.getPlayerList().getPlayers().get(0);
			ServerLevel level = server.overworld();
			level.getChunk((int) Math.floor(x) >> 4, (int) Math.floor(z) >> 4);
			int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, (int) Math.floor(x), (int) Math.floor(z));
			float yaw = (float) Math.toDegrees(Math.atan2(-(facing.getX() + 0.5 - x), facing.getZ() + 0.5 - z));
			player.getAbilities().flying = up > 0;
			player.onUpdateAbilities();
			player.teleportTo(level, x, y + up, z, Set.of(), yaw, pitch);
		});
	}

	/**
	 * A quest from her word to its place: accepted, its place found, on the way (her checkpoint, an anecdote or a
	 * joke), near (her ring), there; the cottage's chest opened and a cake taken.
	 */
	private static void trip(ClientGameTestContext context, TestSingleplayerContext world, ExiledQuest quest, String name) {
		world.getServer().runOnServer(server -> ExiledPixies.act(server.getPlayerList().getPlayers().get(0), MessageExiledAction.ACCEPT, quest.ordinal()));
		BlockPos target = null;
		for (int i = 0; i < 60 && target == null; i++) {
			context.waitTicks(10);
			target = data(world).target;
		}
		BlockPos[] from = new BlockPos[1];
		world.getServer().runOnServer(server -> from[0] = server.getPlayerList().getPlayers().get(0).blockPosition());
		log(name + ": its place " + (target == null ? "not found" : target.toShortString() + ", " + (int) Math.sqrt(target.distSqr(from[0].atY(0)))
			+ " blocks away"));
		if (target == null) {
			throw new AssertionError("she found no " + name);
		}
		// on the way, some way off: her checkpoint, and an anecdote or a joke
		double dx = target.getX() + 0.5 - from[0].getX();
		double dz = target.getZ() + 0.5 - from[0].getZ();
		double d = Math.max(1, Math.sqrt(dx * dx + dz * dz));
		BlockPos aim = target;
		// (a village's roads and houses reach far, and standing on them is being there: its way and her ring are seen from
		// above it)
		int above = quest == ExiledQuest.VILLAGE ? 40 : 0;
		go(world, target.getX() + 0.5 - dx / d * 90, target.getZ() + 0.5 - dz / d * 90, aim, 6 + above, above > 0 ? 30 : 4);
		context.waitTicks(80);
		context.runOnClient(mc -> ExiledPixieHud.clear());
		world.getServer().runOnServer(server -> {
			ServerPlayer player = server.getPlayerList().getPlayers().get(0);
			long later = player.level().getGameTime() + 100000;
			ExiledTalk.trip(player, ExiledPixies.get(player), later);
			ExiledTalk.trip(player, ExiledPixies.get(player), later + 3000);
		});
		context.waitTicks(40);
		context.takeScreenshot("trip_" + name + "_way");
		// near: her ring
		go(world, target.getX() + 0.5 - dx / d * 26, target.getZ() + 0.5 - dz / d * 26, aim, 3 + above, above > 0 ? 50 : 12);
		BlockPos ring = null;
		for (int i = 0; i < 30 && ring == null; i++) {
			context.waitTicks(10);
			ring = data(world).checkpoint;
		}
		log(name + ": her ring " + (ring == null ? "none" : ring.toShortString() + ", " + (int) Math.sqrt(ring.distSqr(target.atY(ring.getY())))
			+ " blocks from its place"));
		if (ring == null) {
			throw new AssertionError("she made no ring at the " + name);
		}
		context.takeScreenshot("trip_" + name + "_ring");
		BlockPos lantern = ring;
		world.getServer().runOnServer(server -> {
			ServerPlayer player = server.getPlayerList().getPlayers().get(0);
			player.getAbilities().flying = false;
			player.onUpdateAbilities();
			float yaw = (float) Math.toDegrees(Math.atan2(-(aim.getX() + 0.5 - lantern.getX() - 0.5), aim.getZ() + 0.5 - lantern.getZ() - 0.5));
			player.teleportTo(server.overworld(), lantern.getX() + 0.5, lantern.getY(), lantern.getZ() + 1.4, Set.of(), yaw, 8);
		});
		context.waitTicks(50);
		context.takeScreenshot("trip_" + name + "_there");
		if (quest == ExiledQuest.VILLAGE) {
			ExiledPixieData done = data(world);
			log("the village reached: done " + done.isDone(ExiledQuest.VILLAGE));
			if (!done.isDone(ExiledQuest.VILLAGE)) {
				throw new AssertionError("reaching her ring in the village did not finish the quest");
			}
			// a word about its villagers
			context.runOnClient(mc -> ExiledPixieHud.clear());
			boolean[] spoke = {false};
			for (int i = 0; i < 8 && !spoke[0]; i++) {
				int k = i;
				world.getServer().runOnServer(server -> {
					ServerPlayer player = server.getPlayerList().getPlayers().get(0);
					log("near her player: " + ExiledTalk.near(player));
					spoke[0] = ExiledTalk.sight(player, ExiledPixies.get(player), player.level().getGameTime() + 100000L * (k + 1), true);
				});
			}
			context.waitTicks(40);
			context.takeScreenshot("trip_village_talk");
			return;
		}
		// the witches' chest: shown, opened, a cake taken
		BlockPos shown = context.computeOnClient(mc -> com.iafabric.message.MessageExiledSync.extra.contains("Hint") ? BlockPos.of(com.iafabric.message.MessageExiledSync.extra.getLong("Hint")) : null);
		log("at the cottage: the chest she shows " + (shown == null ? "none" : shown.toShortString()));
		if (shown == null) {
			throw new AssertionError("she did not show the witches' chest");
		}
		context.runOnClient(mc -> {
			if (!mc.options.hideGui) {
				mc.options.hideGui = !mc.options.hideGui;
			}
			mc.options.setCameraType(net.minecraft.client.CameraType.THIRD_PERSON_BACK);
		});
		context.waitTicks(10);
		context.takeScreenshot("trip_cottage_view");
		context.runOnClient(mc -> {
			mc.options.hideGui = !mc.options.hideGui;
			mc.options.setCameraType(net.minecraft.client.CameraType.FIRST_PERSON);
		});
		world.getServer().runOnServer(server -> {
			ServerPlayer player = server.getPlayerList().getPlayers().get(0);
			if (server.overworld().getBlockEntity(shown) instanceof ChestBlockEntity box) {
				player.openMenu(box);
			}
		});
		context.waitTicks(20);
		context.takeScreenshot("trip_cottage_chest");
		int[] cakes = {0};
		world.getServer().runOnServer(server -> {
			ServerPlayer player = server.getPlayerList().getPlayers().get(0);
			player.closeContainer();
			if (server.overworld().getBlockEntity(shown) instanceof ChestBlockEntity box) {
				for (int i = 0; i < box.getContainerSize(); i++) {
					if (box.getItem(i).is(Items.CAKE)) {
						cakes[0]++;
					}
				}
				for (int i = 0; i < box.getContainerSize(); i++) {
					if (box.getItem(i).is(Items.CAKE)) {
						player.getInventory().add(box.removeItem(i, 1));
						break;
					}
				}
			}
		});
		context.waitTicks(40);
		context.takeScreenshot("trip_cake_done");
		ExiledPixieData done = data(world);
		log("the witches' chest: " + cakes[0] + " cakes; one taken, the quest done " + done.isDone(ExiledQuest.CAKE));
		if (cakes[0] < 5 || !done.isDone(ExiledQuest.CAKE)) {
			throw new AssertionError("the witches' chest had too few cakes, or a cake from it did not finish the quest");
		}
	}
}

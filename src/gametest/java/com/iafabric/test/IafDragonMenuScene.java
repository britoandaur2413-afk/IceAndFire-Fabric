package com.iafabric.test;

import com.iafabric.IafMod;
import com.iafabric.entity.EntityDragonBase;
import com.iafabric.entity.IafEntityRegistry;
import com.iafabric.test.ct.ClientGameTestContext;
import com.iafabric.test.ct.FabricClientGameTest;
import com.iafabric.test.ct.TestSingleplayerContext;
import net.minecraft.client.CameraType;
import net.minecraft.client.gui.screens.worldselection.WorldCreationUiState;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.List;

/**
 * IAF_ONLY=dragonmenu (from the 26.3 port, f463d60 and 078debe): its owner riding a grown dragon opens its menu, in first
 * and third person; it draws the game's copy of the dragon (the server's copy, whose rider is the server's player, crashed
 * the game: "renderer is null"). Then the dragon full grown: its menu stays open for its rider, in first person the
 * spyglass is used from its back and the spikes by the seat are not in the view; on foot by its side its menu stays
 * open, and closes once the player walks away.
 */
public class IafDragonMenuScene implements FabricClientGameTest {
	private static final int GROUND = -60;

	private static void log(String message) {
		IafMod.LOGGER.info("[IAFDRAGONMENU] {}", message);
	}

	private static ServerPlayer player(MinecraftServer server) {
		return server.getPlayerList().getPlayers().get(0);
	}

	@Override
	public void runTest(ClientGameTestContext context) {
		if (IafTestOnly.skip("dragonmenu")) {
			return;
		}
		List<String> problems = new ArrayList<>();
		try (TestSingleplayerContext world = context.worldBuilder().adjustSettings(settings -> {
			settings.setGameMode(WorldCreationUiState.SelectedGameMode.SURVIVAL);
			settings.setAllowCheats(true);
		}).create()) {
			world.getServer().runCommand("gamerule doDaylightCycle false");
			world.getServer().runCommand("gamerule doMobSpawning false");
			world.getServer().runCommand("time set noon");
			context.runOnClient(mc -> mc.options.hideGui = true);
			menuRiding(context, world, problems);
			context.runOnClient(mc -> {
				mc.options.hideGui = false;
				mc.options.setCameraType(CameraType.FIRST_PERSON);
			});
		}
		if (!problems.isEmpty()) {
			throw new AssertionError(String.join(" | ", problems));
		}
	}

	/** What is on the screen: the dragon's menu (and which copy of the dragon it draws), or what else. */
	private static String dragonMenu(ClientGameTestContext context) {
		return context.computeOnClient(mc -> {
			if (!(mc.screen instanceof com.iafabric.client.gui.GuiDragon)) {
				return "no menu: " + mc.screen;
			}
			Entity drawn = IafMod.PROXY.getReferencedMob();
			return "its menu, drawing " + (drawn == null ? "nothing" : drawn.level().isClientSide() ? "the game's dragon" : "the server's dragon")
				+ (mc.player.isPassenger() ? ", the player riding" : ", the player off");
		});
	}

	private static void openDragonMenu(TestSingleplayerContext world, int id) {
		world.getServer().runOnServer(server -> {
			if (server.overworld().getEntity(id) instanceof EntityDragonBase dragon) {
				dragon.openInventory(player(server));
			}
		});
	}

	private static void menuRiding(ClientGameTestContext context, TestSingleplayerContext world, List<String> problems) {
		world.getServer().runCommand("tp @p 60.5 " + GROUND + " 30.5 0 0");
		context.waitTicks(5);
		int[] id = {0};
		world.getServer().runOnServer(server -> {
			ServerLevel level = server.overworld();
			ServerPlayer player = player(server);
			EntityDragonBase dragon = IafEntityRegistry.LIGHTNING_DRAGON.create(level);
			dragon.setAgeInDays(74);
			dragon.moveTo(60.5, GROUND, 33.5, 0, 0);
			dragon.tame(player);
			level.addFreshEntity(dragon);
			player.startRiding(dragon, true);
			id[0] = dragon.getId();
		});
		context.waitTicks(30);
		for (CameraType camera : new CameraType[]{CameraType.FIRST_PERSON, CameraType.THIRD_PERSON_BACK}) {
			context.runOnClient(mc -> mc.options.setCameraType(camera));
			openDragonMenu(world, id[0]);
			context.waitTicks(20);
			String seen = dragonMenu(context);
			log("the dragon's menu while riding, " + camera + ": " + seen);
			context.takeScreenshot("dragonmenu_riding_" + camera.name().toLowerCase(java.util.Locale.ROOT));
			if (!seen.contains("the game's dragon")) {
				problems.add("riding, the dragon's menu in " + camera + ": " + seen);
			}
			context.runOnClient(mc -> mc.player.closeContainer());
			context.waitTicks(5);
		}
		// full grown, still ridden: its menu
		world.getServer().runOnServer(server -> {
			if (server.overworld().getEntity(id[0]) instanceof EntityDragonBase dragon) {
				dragon.setAgeInDays(125);
			}
		});
		context.waitTicks(40);
		context.runOnClient(mc -> mc.options.setCameraType(CameraType.FIRST_PERSON));
		openDragonMenu(world, id[0]);
		context.waitTicks(20);
		String grown = dragonMenu(context);
		double over = world.getServer().computeOnServer(server -> player(server).distanceTo(server.overworld().getEntity(id[0])));
		log(String.format(java.util.Locale.ROOT, "the full grown dragon's menu, its rider %.1f blocks from its middle: %s", over, grown));
		if (!grown.contains("its menu")) {
			problems.add("riding a full grown dragon, its menu did not stay open (" + grown + ")");
		}
		context.runOnClient(mc -> mc.player.closeContainer());
		context.waitTicks(5);
		// the spyglass from its back, in first person
		world.getServer().runOnServer(server -> player(server).setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.SPYGLASS)));
		context.runOnClient(mc -> {
			mc.player.setYRot(0F);
			mc.player.setXRot(10F);
		});
		context.waitTicks(10);
		context.takeScreenshot("dragonmenu_riding_grown_first_person");
		context.runOnClient(mc -> mc.player.setXRot(-6F));
		context.getInput().holdKey(options -> options.keyUse);
		context.waitTicks(15);
		boolean scoping = context.computeOnClient(mc -> mc.player.isScoping());
		context.takeScreenshot("dragonmenu_riding_grown_spyglass");
		context.getInput().releaseKey(options -> options.keyUse);
		log("the spyglass on a full grown dragon's back, in first person: " + (scoping ? "looking through it" : "not used"));
		if (!scoping) {
			problems.add("the spyglass was not used from a full grown dragon's back");
		}
		world.getServer().runOnServer(server -> player(server).setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY));
		context.runOnClient(mc -> mc.options.setCameraType(CameraType.THIRD_PERSON_BACK));
		context.waitTicks(5);
		context.takeScreenshot("dragonmenu_riding_grown_third_person");
		context.runOnClient(mc -> mc.options.setCameraType(CameraType.FIRST_PERSON));
		// on foot by its side: open while near its body (farther than 8 blocks from its middle), closed once away
		double[] side = world.getServer().computeOnServer(server -> {
			ServerPlayer player = player(server);
			player.stopRiding();
			var box = server.overworld().getEntity(id[0]).getBoundingBox();
			player.teleportTo(box.maxX + 6, GROUND, (box.minZ + box.maxZ) / 2);
			return new double[]{box.maxX + 6, (box.minZ + box.maxZ) / 2, box.getXsize()};
		});
		context.waitTicks(10);
		openDragonMenu(world, id[0]);
		context.waitTicks(20);
		String byItsSide = dragonMenu(context);
		double fromMiddle = world.getServer().computeOnServer(server -> player(server).distanceTo(server.overworld().getEntity(id[0])));
		log(String.format(java.util.Locale.ROOT, "on foot 6 blocks from its body (%.1f wide), %.1f from its middle: %s", side[2], fromMiddle, byItsSide));
		if (!byItsSide.contains("its menu")) {
			problems.add("on foot by a full grown dragon's side, its menu did not stay open (" + byItsSide + ")");
		}
		world.getServer().runOnServer(server -> player(server).teleportTo(side[0] + 20, GROUND, side[1]));
		context.waitTicks(20);
		String away = dragonMenu(context);
		log("20 blocks farther: " + away);
		if (away.contains("its menu")) {
			problems.add("the dragon's menu stayed open with the player 20 blocks away");
			context.runOnClient(mc -> mc.player.closeContainer());
		}
		world.getServer().runOnServer(server -> {
			if (server.overworld().getEntity(id[0]) instanceof EntityDragonBase dragon) {
				dragon.discard();
			}
		});
		context.waitTicks(5);
	}
}

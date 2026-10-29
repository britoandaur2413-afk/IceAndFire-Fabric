package com.iafabric.test;

import com.iafabric.IafMod;
import com.iafabric.entity.tile.TileEntityJar;
import com.iafabric.test.ct.FabricClientGameTest;
import com.iafabric.test.ct.ClientGameTestContext;
import com.iafabric.test.ct.TestSingleplayerContext;
import net.minecraft.client.gui.screens.worldselection.WorldCreationUiState;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;

/** The pixies as a player meets them: a jar with a pixie put down shows its pixie. */
public class IafPixieTests implements FabricClientGameTest {
	private static final int GROUND = -60;

	private static void log(String message) {
		IafMod.LOGGER.info("[IAFPIXIE] {}", message);
	}

	@Override
	public void runTest(ClientGameTestContext context) {
		if (System.getenv("IAF_BATTLE") != null || IafTestOnly.skip("pixie")) {
			return;
		}
		try (TestSingleplayerContext world = context.worldBuilder().adjustSettings(settings ->
			settings.setGameMode(WorldCreationUiState.SelectedGameMode.SURVIVAL)).create()) {
			world.getServer().runCommand("gamerule doDaylightCycle false");
			world.getServer().runCommand("gamerule doMobSpawning false");
			world.getServer().runCommand("time set noon");
			world.getServer().runCommand("tp @p 0.5 " + GROUND + " 0.5 0 50");
			context.waitTicks(20);
			jarPlaced(context, world);
			bondsMenu(context, world);
		}
	}

	/**
	 * The pixie bonds menu as a player uses it: H opens it, a tamed pink pixie near gives the pink card its "Bond"
	 * button, a click bonds it; pictures before and after, and in Spanish.
	 */
	private static void bondsMenu(ClientGameTestContext context, TestSingleplayerContext world) {
		world.getServer().runOnServer(server -> {
			var player = server.getPlayerList().getPlayers().get(0);
			var level = server.overworld();
			com.iafabric.entity.EntityPixie pixie = com.iafabric.entity.IafEntityRegistry.PIXIE.create(level);
			pixie.moveTo(player.getX() + 1.5, player.getY() + 1, player.getZ() + 1.5, 0, 0);
			pixie.setColor(0);
			pixie.tame(player);
			level.addFreshEntity(pixie);
		});
		context.waitTicks(20);
		context.getInput().pressKey(com.mojang.blaze3d.platform.InputConstants.getKey("key.keyboard.h"));
		context.waitTicks(10);
		boolean open = context.computeOnClient(mc -> mc.screen instanceof com.iafabric.client.gui.GuiPixieBonds);
		log("H opens the pixie bonds menu: " + open);
		if (!open) {
			throw new AssertionError("H did not open the pixie bonds menu");
		}
		context.takeScreenshot("pixie_bonds_menu");
		// a real click of the left mouse button (in 26.x it is button 1) where the menu shows "Bond"
		double[] at = context.computeOnClient(mc -> {
			String bond = net.minecraft.network.chat.Component.translatable("gui.iceandfire_fabric.pixie_bonds.bond").getString();
			for (var child : mc.screen.children()) {
				if (child instanceof net.minecraft.client.gui.components.Button button && button.getMessage().getString().equals(bond)) {
					double scale = (double) mc.getWindow().getScreenWidth() / mc.getWindow().getGuiScaledWidth();
					return new double[]{(button.getX() + button.getWidth() / 2.0) * scale, (button.getY() + button.getHeight() / 2.0) * scale};
				}
			}
			return null;
		});
		if (at == null) {
			throw new AssertionError("no Bond button with a tamed pink pixie near");
		}
		context.getInput().setCursorPos(at[0], at[1]);
		context.waitTicks(2);
		context.getInput().pressMouse(com.mojang.blaze3d.platform.InputConstants.MOUSE_BUTTON_LEFT);
		context.waitTicks(20);
		int[] active = {-1};
		world.getServer().runOnServer(server -> active[0] = com.iafabric.pixie.PixieBonding.get(server.getPlayerList().getPlayers().get(0)).active);
		log("clicked Bond: the pixie out is color " + active[0]);
		if (active[0] != 0) {
			throw new AssertionError("the click did not bond the pink pixie");
		}
		context.takeScreenshot("pixie_bonds_menu_bonded");
		// the pixie grows and learns: level 3, rage and the pillar of fire, two orbs worn
		world.getServer().runOnServer(server -> {
			var player = server.getPlayerList().getPlayers().get(0);
			var slot = new net.minecraft.world.SimpleContainer(1);
			slot.setItem(0, new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.BLAZE_POWDER));
			com.iafabric.pixie.PixieBonding.feed(player, 0, slot);
			slot.setItem(0, new net.minecraft.world.item.ItemStack(com.iafabric.item.IafItemRegistry.FIRE_DRAGON_BLOOD));
			com.iafabric.pixie.PixieBonding.feed(player, 0, slot);
			com.iafabric.pixie.PixieBonding.addXp(player, 0, 5000);
			var bond = com.iafabric.pixie.PixieBonding.get(player).get(0);
			bond.orbs[com.iafabric.pixie.OrbKind.PHYSICAL.ordinal()] = new net.minecraft.world.item.ItemStack(com.iafabric.item.IafItemRegistry.PIXIE_ORB_PHYSICAL);
			bond.orbs[com.iafabric.pixie.OrbKind.FIRE.ordinal()] = new net.minecraft.world.item.ItemStack(com.iafabric.item.IafItemRegistry.PIXIE_ORB_FIRE);
			com.iafabric.pixie.PixieBonding.sync(player);
			player.getInventory().add(new net.minecraft.world.item.ItemStack(com.iafabric.item.IafItemRegistry.PIXIE_DUST, 12));
			player.getInventory().add(new net.minecraft.world.item.ItemStack(com.iafabric.item.IafItemRegistry.AMBROSIA));
			player.getInventory().add(new net.minecraft.world.item.ItemStack(com.iafabric.item.IafItemRegistry.PIXIE_ORB_WATER));
		});
		context.waitTicks(10);
		// a real click on "Open" on the pink pixie's card opens its own menu
		double[] openAt = context.computeOnClient(mc -> {
			String label = net.minecraft.network.chat.Component.translatable("gui.iceandfire_fabric.pixie_bonds.open").getString();
			for (var child : mc.screen.children()) {
				if (child instanceof net.minecraft.client.gui.components.Button button && button.getMessage().getString().equals(label)) {
					double scale = (double) mc.getWindow().getScreenWidth() / mc.getWindow().getGuiScaledWidth();
					return new double[]{(button.getX() + button.getWidth() / 2.0) * scale, (button.getY() + button.getHeight() / 2.0) * scale};
				}
			}
			return null;
		});
		if (openAt == null) {
			throw new AssertionError("no Open button on the bonded pixie's card");
		}
		context.getInput().setCursorPos(openAt[0], openAt[1]);
		context.waitTicks(2);
		context.getInput().pressMouse(com.mojang.blaze3d.platform.InputConstants.MOUSE_BUTTON_LEFT);
		context.waitTicks(20);
		boolean menu = context.computeOnClient(mc -> mc.screen instanceof com.iafabric.client.gui.GuiPixieMenu);
		log("Open on the card opens the pixie's menu: " + menu);
		if (!menu) {
			throw new AssertionError("the pixie's own menu did not open");
		}
		// the mouse on its first spell (rage): its tooltip; then on the pillar of fire (from level 5); then on what it gives
		double[][] spots = context.computeOnClient(mc -> {
			var screen = mc.screen;
			int left = (screen.width - 230) / 2;
			int top = (screen.height - 226) / 2;
			double scale = (double) mc.getWindow().getScreenWidth() / mc.getWindow().getGuiScaledWidth();
			return new double[][]{{(left + 34 + 9) * scale, (top + 110 + 9) * scale}, {(left + 34 + 40 + 9) * scale, (top + 110 + 9) * scale},
				{(left + 150) * scale, (top + 64) * scale}, {(left + 8 + 9) * scale, (top + 110 + 9) * scale}};
		});
		double[] fire = spots[0];
		context.getInput().setCursorPos(fire[0], fire[1]);
		context.waitTicks(5);
		context.takeScreenshot("pixie_menu");
		context.getInput().setCursorPos(spots[1][0], spots[1][1]);
		context.waitTicks(5);
		context.takeScreenshot("pixie_menu_locked");
		context.getInput().setCursorPos(spots[2][0], spots[2][1]);
		context.waitTicks(5);
		context.takeScreenshot("pixie_menu_gift");
		context.getInput().setCursorPos(spots[3][0], spots[3][1]);
		context.waitTicks(5);
		context.takeScreenshot("pixie_menu_help");
		context.runOnClient(mc -> {
			mc.getLanguageManager().setSelected("es_es");
			mc.options.languageCode = "es_es";
			mc.getLanguageManager().onResourceManagerReload(mc.getResourceManager());
			mc.setScreen(null);
			com.iafabric.network.IafNetwork.sendToServer(new com.iafabric.message.MessagePixieBond(com.iafabric.message.MessagePixieBond.MENU, 0));
		});
		context.waitTicks(20);
		context.getInput().setCursorPos(fire[0], fire[1]);
		context.waitTicks(5);
		context.takeScreenshot("pixie_menu_es");
		context.runOnClient(mc -> mc.setScreen(new com.iafabric.client.gui.GuiPixieBonds()));
		context.waitTicks(5);
		context.runOnClient(mc -> {
			mc.getLanguageManager().setSelected("es_es");
			mc.options.languageCode = "es_es";
			mc.getLanguageManager().onResourceManagerReload(mc.getResourceManager());
			mc.setScreen(new com.iafabric.client.gui.GuiPixieBonds());
		});
		context.waitTicks(5);
		context.takeScreenshot("pixie_bonds_menu_es");
		context.runOnClient(mc -> {
			mc.getLanguageManager().setSelected("en_us");
			mc.options.languageCode = "en_us";
			mc.getLanguageManager().onResourceManagerReload(mc.getResourceManager());
			mc.setScreen(null);
		});
		context.waitTicks(5);
	}

	/** A jar with a pink pixie put down with a right click: the client sees the pixie inside. */
	private static void jarPlaced(ClientGameTestContext context, TestSingleplayerContext world) {
		world.getServer().runOnServer(server -> server.getPlayerList().getPlayers().get(0).setItemInHand(InteractionHand.MAIN_HAND,
			new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath(IafMod.MOD_ID, "pixie_jar_0")))));
		context.waitTicks(5);
		context.getInput().lookAt(new BlockPos(0, GROUND - 1, 2));
		context.waitTicks(2);
		context.getInput().pressKey(options -> options.keyUse);
		context.waitTicks(20);
		BlockPos jar = new BlockPos(0, GROUND, 2);
		boolean[] client = {false};
		context.runOnClient(mc -> client[0] = mc.level.getBlockEntity(jar) instanceof TileEntityJar tile && tile.hasPixie);
		boolean[] server = {false};
		world.getServer().runOnServer(s -> server[0] = s.overworld().getBlockEntity(jar) instanceof TileEntityJar tile && tile.hasPixie);
		log("jar put down: server has the pixie " + server[0] + ", client sees it " + client[0]);
		context.takeScreenshot("pixie_jar_placed");
		if (!server[0] || !client[0]) {
			throw new AssertionError("the pixie in the jar is not there: server " + server[0] + ", client " + client[0]);
		}
	}
}

package com.iafabric.test;

import com.iafabric.item.IafItemRegistry;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.client.CameraType;
import net.minecraft.client.gui.screens.worldselection.WorldCreationUiState;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * IAF_ONLY=armorstand: the mod's dragonsteel on two armor stands, one without arms and one with them, beside the game's
 * diamond armor on two more. A chestplate on a stand without arms keeps its shoulders, as the game's own does: the
 * armor hides where its wearer is hidden only on a player (a first-person swing), never on a stand.
 */
public class IafArmorStandScene implements FabricClientGameTest {
	private static final int GROUND = -60;

	@Override
	public void runTest(ClientGameTestContext context) {
		if (IafTestOnly.skip("armorstand")) {
			return;
		}
		try (TestSingleplayerContext world = context.worldBuilder().adjustSettings(settings ->
			settings.setGameMode(WorldCreationUiState.SelectedGameMode.CREATIVE)).create()) {
			world.getServer().runCommand("gamerule advance_time false");
			world.getServer().runCommand("gamerule spawn_mobs false");
			world.getServer().runCommand("time set noon");
			world.getServer().runCommand("tp @p 0 " + GROUND + " -0.5 0 10");
			world.getServer().runOnServer(server -> {
				ServerLevel level = server.overworld();
				Item[] dragonsteel = {IafItemRegistry.DRAGONSTEEL_FIRE_HELMET, IafItemRegistry.DRAGONSTEEL_FIRE_CHESTPLATE,
					IafItemRegistry.DRAGONSTEEL_FIRE_LEGGINGS, IafItemRegistry.DRAGONSTEEL_FIRE_BOOTS};
				Item[] diamond = {Items.DIAMOND_HELMET, Items.DIAMOND_CHESTPLATE, Items.DIAMOND_LEGGINGS, Items.DIAMOND_BOOTS};
				stand(level, -2.25, dragonsteel, false);
				stand(level, -0.75, dragonsteel, true);
				stand(level, 0.75, diamond, false);
				stand(level, 2.25, diamond, true);
			});
			context.runOnClient(mc -> {
				mc.options.setCameraType(CameraType.FIRST_PERSON);
				if (!mc.gui.hud.isHidden()) {
					mc.gui.hud.toggle();
				}
			});
			context.waitTicks(40);
			context.takeScreenshot("armorstand_front");
			// the mod's pair up close
			world.getServer().runCommand("tp @p -1.5 " + GROUND + " 1 0 15");
			context.waitTicks(10);
			context.takeScreenshot("armorstand_dragonsteel");
			context.runOnClient(mc -> {
				if (mc.gui.hud.isHidden()) {
					mc.gui.hud.toggle();
				}
			});
		}
	}

	private static void stand(ServerLevel level, double x, Item[] armor, boolean arms) {
		ArmorStand stand = new ArmorStand(level, x, GROUND, 3);
		stand.setYRot(180);
		stand.setYBodyRot(180);
		stand.setYHeadRot(180);
		stand.setShowArms(arms);
		EquipmentSlot[] slots = {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};
		for (int i = 0; i < slots.length; i++) {
			stand.setItemSlot(slots[i], new ItemStack(armor[i]));
		}
		level.addFreshEntity(stand);
	}
}

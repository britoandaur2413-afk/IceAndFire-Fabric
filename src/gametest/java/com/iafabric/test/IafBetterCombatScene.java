package com.iafabric.test;

import com.iafabric.IafMod;
import com.iafabric.entity.EntityGhostSword;
import com.iafabric.item.IafItemRegistry;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.CameraType;
import net.minecraft.client.gui.screens.worldselection.WorldCreationUiState;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.AABB;

/**
 * IAF_ONLY=bettercombat, with Better Combat (and its player animation library) in the run's mods folder: a player in
 * the mod's dragonsteel armor swings a sword in first person (Better Combat draws the player's arms with the armor on
 * them: the helmet and chest piece must not be drawn round the camera), then swings the ghost sword at the sky (its
 * blade must fly, as on a swing at nothing without Better Combat).
 */
public class IafBetterCombatScene implements FabricClientGameTest {
	private static final int GROUND = -60;

	private static void log(String message) {
		IafMod.LOGGER.info("[IAFCOMBAT] {}", message);
	}

	@Override
	public void runTest(ClientGameTestContext context) {
		if (!FabricLoader.getInstance().isModLoaded("bettercombat") || IafTestOnly.skip("bettercombat")) {
			return;
		}
		try (TestSingleplayerContext world = context.worldBuilder().adjustSettings(settings ->
			settings.setGameMode(WorldCreationUiState.SelectedGameMode.SURVIVAL)).create()) {
			world.getServer().runCommand("gamerule advance_time false");
			world.getServer().runCommand("gamerule spawn_mobs false");
			world.getServer().runCommand("time set noon");
			world.getServer().runCommand("tp @p 0 " + GROUND + " 0 0 0");
			world.getServer().runOnServer(server -> {
				var player = server.getPlayerList().getPlayers().get(0);
				player.setItemSlot(EquipmentSlot.HEAD, new ItemStack(IafItemRegistry.DRAGONSTEEL_FIRE_HELMET));
				player.setItemSlot(EquipmentSlot.CHEST, new ItemStack(IafItemRegistry.DRAGONSTEEL_FIRE_CHESTPLATE));
				player.setItemSlot(EquipmentSlot.LEGS, new ItemStack(IafItemRegistry.DRAGONSTEEL_FIRE_LEGGINGS));
				player.setItemSlot(EquipmentSlot.FEET, new ItemStack(IafItemRegistry.DRAGONSTEEL_FIRE_BOOTS));
				player.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.DIAMOND_SWORD));
			});
			context.waitTicks(40);
			context.runOnClient(mc -> {
				mc.options.setCameraType(CameraType.FIRST_PERSON);
				mc.gui.hud.getChat().clearMessages(false);
			});
			context.waitTicks(5);
			context.takeScreenshot("bettercombat_before");
			// a swing in first person: pictures along its animation
			context.getInput().pressKey(options -> options.keyAttack);
			for (int tick = 1; tick <= 10; tick++) {
				context.waitTick();
				if (tick % 2 == 0) {
					context.takeScreenshot("bettercombat_swing_" + tick);
				}
			}
			// the armor from behind, as others see it (all of it)
			context.runOnClient(mc -> mc.options.setCameraType(CameraType.THIRD_PERSON_BACK));
			context.waitTicks(25);
			context.getInput().pressKey(options -> options.keyAttack);
			context.waitTicks(4);
			context.takeScreenshot("bettercombat_swing_third_person");
			context.runOnClient(mc -> mc.options.setCameraType(CameraType.FIRST_PERSON));

			// the ghost sword swung at the sky (nothing to hit): its blade flies
			world.getServer().runOnServer(server -> server.getPlayerList().getPlayers().get(0)
				.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(IafItemRegistry.GHOST_SWORD)));
			context.runOnClient(mc -> {
				mc.player.setXRot(-60F);
				mc.player.xRotO = -60F;
			});
			context.waitTicks(30);
			int[] blades = {0, 0};
			for (int swing = 0; swing < 2; swing++) {
				context.getInput().pressKey(options -> options.keyAttack);
				context.waitTicks(30);
			}
			world.getServer().runOnServer(server -> {
				var player = server.getPlayerList().getPlayers().get(0);
				blades[0] = server.overworld().getEntitiesOfClass(EntityGhostSword.class, new AABB(player.blockPosition()).inflate(80)).size();
				blades[1] = player.getCooldowns().isOnCooldown(player.getMainHandItem()) ? 1 : 0;
			});
			log("ghost sword swung twice at the sky under Better Combat: " + blades[0] + " blades flying");
			context.takeScreenshot("bettercombat_ghost_sword");
			if (blades[0] == 0) {
				throw new AssertionError("the ghost sword threw no blade on Better Combat's swings at nothing");
			}
		}
	}
}

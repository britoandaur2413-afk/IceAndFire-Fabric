package com.iafabric.test;

import com.iafabric.IafMod;
import com.iafabric.client.IafKeybinds;
import com.iafabric.entity.EntityDragonBase;
import com.iafabric.entity.IafEntityRegistry;
import com.iafabric.item.IafItemRegistry;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.worldselection.WorldCreationUiState;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;

import java.util.Locale;

/**
 * Riding a dragon as a player would, with the keys: right click to get on, space held to take off, forward
 * to fly, R for the breath, shift in the air (comes down, does not drop the rider), shift on the ground to
 * get off. The rider must stay on the dragon's back. Pictures from third person; the positions in the log.
 */
public class IafRidingTests implements FabricClientGameTest {
	private static final int GROUND = -60;

	private static void log(String message) {
		IafMod.LOGGER.info("[IAFRIDE] {}", message);
	}

	@Override
	public void runTest(ClientGameTestContext context) {
		if (System.getenv("IAF_BATTLE") != null || IafTestOnly.skip("riding")) {
			return;
		}
		try (TestSingleplayerContext world = context.worldBuilder().adjustSettings(settings ->
			settings.setGameMode(WorldCreationUiState.SelectedGameMode.SURVIVAL)).create()) {
			// The keys are R, G, X and F7 (26.x numbers keys its own way: the 1.18 numbers were other keys), and a
			// key an earlier build saved wrong goes back to its default.
			context.runOnClient(mc -> {
				var expected = java.util.Map.of(IafKeybinds.dragonFireAttack, "key.keyboard.r", IafKeybinds.dragonStrike, "key.keyboard.g",
					IafKeybinds.dragonDown, "key.keyboard.x", IafKeybinds.dragonChangeView, "key.keyboard.f7");
				expected.forEach((key, name) -> {
					if (!key.getDefaultKey().getName().equals(name) || !key.saveString().equals(name)) {
						throw new AssertionError(key.getName() + " is on " + key.saveString() + " (default " + key.getDefaultKey().getName() + "), not " + name);
					}
				});
				IafKeybinds.dragonStrike.setKey(com.mojang.blaze3d.platform.InputConstants.getKey("key.keyboard.scroll.lock"));
				IafKeybinds.fixOldWrongKeys();
				if (!IafKeybinds.dragonStrike.saveString().equals("key.keyboard.g")) {
					throw new AssertionError("a key saved wrong was not put back: " + IafKeybinds.dragonStrike.saveString());
				}
				log("keys: R breath, G bite, X down, F7 view; a key saved wrong goes back to its default");
			});
			world.getServer().runCommand("gamerule advance_time false");
			world.getServer().runCommand("gamerule spawn_mobs false");
			world.getServer().runCommand("time set noon");
			world.getServer().runCommand("tp @p 0 " + GROUND + " 0 0 0");
			context.waitTicks(20);
			int[] dragonId = {-1};
			world.getServer().runOnServer(server -> {
				var level = server.overworld();
				var player = server.getPlayerList().getPlayers().get(0);
				EntityDragonBase dragon = IafEntityRegistry.FIRE_DRAGON.create(level, EntitySpawnReason.COMMAND);
				dragon.snapTo(0, GROUND, 4, 180, 0);
				level.addFreshEntity(dragon);
				dragon.growDragon(80);
				dragon.setHealth(dragon.getMaxHealth());
				dragon.tame(player);
				dragon.setCommand(0);
				// Dragon armor on all four places, to see it on the dragon.
				dragon.setItemSlot(EquipmentSlot.HEAD, new ItemStack(IafItemRegistry.DRAGONARMOR_IRON_0));
				dragon.setItemSlot(EquipmentSlot.CHEST, new ItemStack(IafItemRegistry.DRAGONARMOR_IRON_1));
				dragon.setItemSlot(EquipmentSlot.LEGS, new ItemStack(IafItemRegistry.DRAGONARMOR_IRON_2));
				dragon.setItemSlot(EquipmentSlot.FEET, new ItemStack(IafItemRegistry.DRAGONARMOR_IRON_3));
				dragonId[0] = dragon.getId();
			});
			context.waitTicks(40);
			// from behind the player, who faces the dragon
			context.runOnClient(mc -> mc.options.setCameraType(net.minecraft.client.CameraType.THIRD_PERSON_BACK));
			context.waitTicks(10);
			context.takeScreenshot("ride_armor");
			// Armor put on later through the dragon's menu, as a player does.
			int[] second = {-1};
			world.getServer().runOnServer(server -> {
				var level = server.overworld();
				var player = server.getPlayerList().getPlayers().get(0);
				EntityDragonBase dragon = IafEntityRegistry.ICE_DRAGON.create(level, EntitySpawnReason.COMMAND);
				dragon.snapTo(-8, GROUND, 4, 180, 0);
				level.addFreshEntity(dragon);
				dragon.growDragon(80);
				dragon.tame(player);
				dragon.setCommand(1);
				second[0] = dragon.getId();
			});
			context.waitTicks(30);
			world.getServer().runOnServer(server -> {
				var player = server.getPlayerList().getPlayers().get(0);
				var dragon = (EntityDragonBase) server.overworld().getEntity(second[0]);
				dragon.openInventory(player);
				var menu = player.containerMenu;
				menu.getSlot(1).set(new ItemStack(IafItemRegistry.DRAGONARMOR_GOLD_0));
				menu.getSlot(2).set(new ItemStack(IafItemRegistry.DRAGONARMOR_GOLD_1));
				menu.getSlot(3).set(new ItemStack(IafItemRegistry.DRAGONARMOR_GOLD_2));
				menu.getSlot(4).set(new ItemStack(IafItemRegistry.DRAGONARMOR_GOLD_3));
				menu.broadcastChanges();
				player.closeContainer();
			});
			context.waitTicks(20);
			context.runOnClient(mc -> log("client sees the gold armor on the second dragon: "
				+ ((EntityDragonBase) mc.level.getEntity(second[0])).getItemBySlot(EquipmentSlot.HEAD)));
			world.getServer().runCommand("tp @p -8 " + (GROUND + 3) + " 22 180 10");
			context.waitTicks(20);
			context.takeScreenshot("ride_armor_from_menu");
			world.getServer().runCommand("tp @p 0 " + GROUND + " 0 0 0");
			context.runOnClient(mc -> mc.options.setCameraType(net.minecraft.client.CameraType.FIRST_PERSON));
			context.waitTicks(10);

			// Right click with an empty hand, like a player.
			world.getServer().runOnServer(server -> {
				var player = server.getPlayerList().getPlayers().get(0);
				var dragon = server.overworld().getEntity(dragonId[0]);
				dragon.interact(player, InteractionHand.MAIN_HAND, dragon.position());
			});
			context.waitTicks(20);
			report(context, world, "mounted");
			context.takeScreenshot("ride_mounted");
			// from behind and from its side: the rider on its back
			context.runOnClient(mc -> mc.options.setCameraType(net.minecraft.client.CameraType.THIRD_PERSON_BACK));
			context.waitTicks(10);
			context.takeScreenshot("ride_mounted_behind");
			context.runOnClient(mc -> mc.options.setCameraType(net.minecraft.client.CameraType.FIRST_PERSON));
			sideShots(context, world, dragonId[0], "ride_side_ground", 2);

			context.getInput().holdKey(options -> options.keyJump);
			context.waitTicks(50);
			context.getInput().releaseKey(options -> options.keyJump);
			report(context, world, "after space");
			sideShots(context, world, dragonId[0], "ride_side_hover", 4);
			context.getInput().holdKey(options -> options.keyUp);
			context.waitTicks(40);
			report(context, world, "flying forward");
			context.takeScreenshot("ride_flying");
			context.runOnClient(mc -> mc.options.setCameraType(net.minecraft.client.CameraType.THIRD_PERSON_BACK));
			for (int i = 0; i < 3; i++) {
				// along its beat: the rider stays on its back
				context.waitTicks(7);
				context.takeScreenshot("ride_flying_behind_" + i);
			}
			// turning in flight its back rolls: the rider in its seat leans with it (seen from close behind it)
			net.minecraft.world.entity.decoration.ArmorStand[] behind = {null};
			context.runOnClient(mc -> {
				behind[0] = new net.minecraft.world.entity.decoration.ArmorStand(mc.level, mc.player.getX(), mc.player.getY(), mc.player.getZ());
				mc.options.setCameraType(net.minecraft.client.CameraType.FIRST_PERSON);
			});
			for (int i = 0; i < 16; i++) {
				context.runOnClient(mc -> {
					mc.player.setYRot(mc.player.getYRot() + 6F);
					mc.player.setYHeadRot(mc.player.getYRot());
					if (mc.player.getVehicle() instanceof EntityDragonBase d) {
						float yaw = d.yBodyRot * net.minecraft.util.Mth.DEG_TO_RAD;
						var at = mc.player.position().add(0, 1, 0);
						// 6 blocks behind and 2 over the rider, looking at it
						double x = at.x + net.minecraft.util.Mth.sin(yaw) * 6, z = at.z - net.minecraft.util.Mth.cos(yaw) * 6, y = at.y + 2;
						float look = (float) Math.toDegrees(Math.atan2(-(at.x - x), at.z - z));
						behind[0].snapTo(x, y, z, look, 18F);
						behind[0].setYHeadRot(look);
						behind[0].setOldPosAndRot();
						mc.setCameraEntity(behind[0]);
					}
				});
				context.waitTick();
				if (i == 5 || i == 10 || i == 15) {
					context.takeScreenshot("ride_turn_close_" + i);
				}
			}
			context.runOnClient(mc -> {
				mc.setCameraEntity(mc.player);
				mc.options.setCameraType(net.minecraft.client.CameraType.THIRD_PERSON_BACK);
			});
			sideShots(context, world, dragonId[0], "ride_side_fly", 2);
			// up and down: the rider tilts with its back
			context.getInput().holdKey(options -> options.keyJump);
			context.waitTicks(10);
			sideShots(context, world, dragonId[0], "ride_side_climb", 2);
			context.getInput().releaseKey(options -> options.keyJump);
			context.getInput().holdKey(com.mojang.blaze3d.platform.InputConstants.getKey("key.keyboard.x"));
			context.waitTicks(10);
			sideShots(context, world, dragonId[0], "ride_side_dive", 2);
			context.getInput().releaseKey(com.mojang.blaze3d.platform.InputConstants.getKey("key.keyboard.x"));
			context.runOnClient(mc -> mc.options.setCameraType(net.minecraft.client.CameraType.FIRST_PERSON));
			context.getInput().holdKey(com.mojang.blaze3d.platform.InputConstants.getKey("key.keyboard.r"));
			context.waitTicks(20);
			report(context, world, "breathing (R)");
			context.takeScreenshot("ride_breath");
			// its breath a while on, from its back and from behind it: from its mouth
			context.waitTicks(15);
			context.takeScreenshot("ride_breath_more");
			context.runOnClient(mc -> mc.options.setCameraType(net.minecraft.client.CameraType.THIRD_PERSON_BACK));
			context.waitTicks(8);
			context.takeScreenshot("ride_breath_back");
			context.runOnClient(mc -> mc.options.setCameraType(net.minecraft.client.CameraType.FIRST_PERSON));
			context.getInput().releaseKey(com.mojang.blaze3d.platform.InputConstants.getKey("key.keyboard.r"));
			context.getInput().releaseKey(options -> options.keyUp);

			// G bites, X flies down (the real keys, as a player presses them).
			context.getInput().holdKey(com.mojang.blaze3d.platform.InputConstants.getKey("key.keyboard.g"));
			context.waitTicks(4);
			world.getServer().runOnServer(server -> {
				var dragon = (EntityDragonBase) server.overworld().getEntity(dragonId[0]);
				log("G held: dragon attacking " + dragon.isAttacking() + ", striking " + dragon.isStriking());
			});
			context.getInput().releaseKey(com.mojang.blaze3d.platform.InputConstants.getKey("key.keyboard.g"));
			double[] heightBefore = {0};
			world.getServer().runOnServer(server -> heightBefore[0] = server.overworld().getEntity(dragonId[0]).getY());
			context.getInput().holdKey(com.mojang.blaze3d.platform.InputConstants.getKey("key.keyboard.x"));
			context.waitTicks(20);
			world.getServer().runOnServer(server -> {
				var dragon = (EntityDragonBase) server.overworld().getEntity(dragonId[0]);
				log(String.format(Locale.ROOT, "X held: going down %s, height %.1f -> %.1f", dragon.isGoingDown(), heightBefore[0], dragon.getY()));
			});
			context.getInput().releaseKey(com.mojang.blaze3d.platform.InputConstants.getKey("key.keyboard.x"));

			// Shift in the air: the dragon comes down and keeps the rider.
			context.getInput().holdKey(options -> options.keyShift);
			context.waitTicks(15);
			report(context, world, "shift in the air");
			context.waitTicks(100);
			report(context, world, "after coming down");
			// shift still held as it lands: the rider stays on it, and it stays on the ground
			world.getServer().runOnServer(server -> {
				var player = server.getPlayerList().getPlayers().get(0);
				var dragon = (EntityDragonBase) server.overworld().getEntity(dragonId[0]);
				if (player.getVehicle() != dragon) {
					throw new AssertionError("landing with shift held dropped the rider off");
				}
				if (!dragon.onGround() || dragon.isFlying() || dragon.isHovering()) {
					throw new AssertionError("it did not stay on the ground: onGround " + dragon.onGround() + " flying " + dragon.isFlying() + " hovering " + dragon.isHovering());
				}
			});
			context.getInput().releaseKey(options -> options.keyShift);
			context.waitTicks(10);
			world.getServer().runOnServer(server -> {
				if (server.getPlayerList().getPlayers().get(0).getVehicle() == null) {
					throw new AssertionError("letting go of shift dropped the rider off");
				}
			});
			context.getInput().holdKeyFor(options -> options.keyShift, 10);
			context.waitTicks(20);
			report(context, world, "shift on the ground");
			// pressed again: off; it stays there, sitting
			world.getServer().runOnServer(server -> {
				var player = server.getPlayerList().getPlayers().get(0);
				var dragon = (EntityDragonBase) server.overworld().getEntity(dragonId[0]);
				if (player.getVehicle() != null) {
					throw new AssertionError("shift pressed again on the ground did not let the rider off");
				}
				if (dragon.getCommand() != 1 || !dragon.isOrderedToSit()) {
					throw new AssertionError("it did not stay sitting: command " + dragon.getCommand() + " sitting " + dragon.isOrderedToSit());
				}
			});
			// seen from its side, a few blocks off: sitting where it landed
			context.waitTicks(30);
			context.runOnClient(mc -> {
				if (mc.level.getEntity(dragonId[0]) instanceof EntityDragonBase dragon) {
					var eye = new net.minecraft.world.entity.decoration.ArmorStand(mc.level, dragon.getX(), dragon.getY(), dragon.getZ());
					float yaw = dragon.yBodyRot * net.minecraft.util.Mth.DEG_TO_RAD;
					double x = dragon.getX() + Math.cos(yaw) * 16, z = dragon.getZ() + Math.sin(yaw) * 16, y = dragon.getY() + 5;
					float look = (float) Math.toDegrees(Math.atan2(-(dragon.getX() - x), dragon.getZ() - z));
					eye.snapTo(x, y, z, look, 12F);
					eye.setYHeadRot(look);
					eye.setOldPosAndRot();
					mc.setCameraEntity(eye);
				}
			});
			context.waitTicks(3);
			context.takeScreenshot("ride_off_sitting");
			context.runOnClient(mc -> mc.setCameraEntity(mc.player));

			// A fire forge ahead: R while looking at it sends the breath into its aperture, and it starts cooking.
			BlockPos coreAt = new BlockPos(0, GROUND + 1, 14);
			BlockPos aperture = coreAt.north();
			world.getServer().runOnServer(server -> {
				var level = server.overworld();
				var player = server.getPlayerList().getPlayers().get(0);
				buildFireForge(level, coreAt);
				var dragon = (EntityDragonBase) level.getEntity(dragonId[0]);
				dragon.snapTo(0.5, GROUND, 0.5, 0, 0);
				dragon.getNavigation().stop();
				player.snapTo(0.5, GROUND, 0.5, 0, 0);
				dragon.interact(player, InteractionHand.MAIN_HAND, dragon.position());
			});
			context.waitTicks(30);
			world.getServer().runOnServer(server -> {
				var core = (com.iafabric.entity.tile.TileEntityDragonforge) server.overworld().getBlockEntity(coreAt);
				var input = (com.iafabric.entity.tile.TileEntityDragonforgeInput) server.overworld().getBlockEntity(aperture);
				log("forge built: assembled " + core.assembled() + ", can smelt " + core.canSmelt() + ", aperture ready " + input.isAssembled());
			});
			context.getInput().lookAt(aperture);
			// the breath takes a second to start and two more to reach the aperture
			context.getInput().holdKey(com.mojang.blaze3d.platform.InputConstants.getKey("key.keyboard.r"));
			context.waitTicks(80);
			context.takeScreenshot("ride_breath_forge");
			context.waitTicks(40);
			context.getInput().releaseKey(com.mojang.blaze3d.platform.InputConstants.getKey("key.keyboard.r"));
			world.getServer().runOnServer(server -> {
				var dragon = (EntityDragonBase) server.overworld().getEntity(dragonId[0]);
				var core = (com.iafabric.entity.tile.TileEntityDragonforge) server.overworld().getBlockEntity(coreAt);
				log("breath at the forge: dragon target " + dragon.burningTarget + " (aperture " + aperture + "), forge cooking " + core.cookTime
					+ ", rider " + server.getPlayerList().getPlayers().get(0).getVehicle());
			});
			world.getServer().runOnServer(server -> server.getPlayerList().getPlayers().get(0).stopRiding());
			context.waitTicks(10);

			// A lightning dragon ridden in the air: R makes it breathe (its lightning or a charge).
			int[] lightningId = {-1};
			world.getServer().runOnServer(server -> {
				var level = server.overworld();
				var player = server.getPlayerList().getPlayers().get(0);
				EntityDragonBase dragon = IafEntityRegistry.LIGHTNING_DRAGON.create(level, EntitySpawnReason.COMMAND);
				dragon.snapTo(30.5, GROUND, 0.5, 0, 0);
				level.addFreshEntity(dragon);
				dragon.growDragon(80);
				dragon.tame(player);
				dragon.setCommand(0);
				player.snapTo(30.5, GROUND, 0.5, 0, 0);
				dragon.interact(player, InteractionHand.MAIN_HAND, dragon.position());
				lightningId[0] = dragon.getId();
			});
			context.waitTicks(10);
			context.getInput().holdKey(options -> options.keyJump);
			context.waitTicks(40);
			context.getInput().releaseKey(options -> options.keyJump);
			context.getInput().holdKey(com.mojang.blaze3d.platform.InputConstants.getKey("key.keyboard.r"));
			boolean[] breathed = {false};
			for (int i = 0; i < 6 && !breathed[0]; i++) {
				context.waitTicks(10);
				world.getServer().runOnServer(server -> {
					var dragon = (EntityDragonBase) server.overworld().getEntity(lightningId[0]);
					breathed[0] |= dragon.isBreathingFire() || dragon.getAnimation() == EntityDragonBase.ANIMATION_FIRECHARGE;
				});
			}
			context.takeScreenshot("ride_lightning_breath");
			context.getInput().releaseKey(com.mojang.blaze3d.platform.InputConstants.getKey("key.keyboard.r"));
			log("lightning dragon ridden, R held: breathed " + breathed[0]);
			if (!breathed[0]) {
				throw new AssertionError("the ridden lightning dragon did not breathe with R");
			}
			world.getServer().runOnServer(server -> server.getPlayerList().getPlayers().get(0).stopRiding());
			context.waitTicks(10);

			// The ghost sword: a swing at nothing throws its blade.
			world.getServer().runOnServer(server -> server.getPlayerList().getPlayers().get(0).setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(IafItemRegistry.GHOST_SWORD)));
			context.waitTicks(5);
			context.getInput().lookAt(0, -30);
			context.runOnClient(mc -> log("before the swing: hit " + (mc.hitResult == null ? "none" : mc.hitResult.getType()) + ", holding " + mc.player.getMainHandItem()));
			context.getInput().pressKey(options -> options.keyAttack);
			context.waitTicks(3);
			context.runOnClient(mc -> log("after the swing: cooldown " + mc.player.getCooldowns().isOnCooldown(mc.player.getMainHandItem())));
			int[] blades = {0};
			world.getServer().runOnServer(server -> blades[0] = server.overworld().getEntitiesOfClass(com.iafabric.entity.EntityGhostSword.class,
				server.getPlayerList().getPlayers().get(0).getBoundingBox().inflate(20)).size());
			log("ghost sword blades thrown: " + blades[0]);
			context.takeScreenshot("ghost_sword");
		}
	}

	/** A fire forge as a player builds it: 3x3x3 of forge bricks, dragon bone blocks at the corners above and below, the core in the middle, the aperture on its north side; iron and fire blood in it. */
	/**
	 * Pictures of the rider on its dragon from the dragon's side, close (a camera of the player's game only, kept
	 * beside the rider every tick, whichever way the dragon flies), a few ticks apart along its beat: the rider in its
	 * seat, tilting with the dragon's back. The camera goes back to the player after.
	 */
	private static void sideShots(ClientGameTestContext context, TestSingleplayerContext world, int dragonId, String name, int count) {
		net.minecraft.world.entity.decoration.ArmorStand[] eye = {null};
		net.minecraft.client.CameraType[] was = {null};
		context.runOnClient(mc -> {
			eye[0] = new net.minecraft.world.entity.decoration.ArmorStand(mc.level, mc.player.getX(), mc.player.getY(), mc.player.getZ());
			// (seen through the stand's own eyes: a camera behind it would stand back by the dragon's size)
			was[0] = mc.options.getCameraType();
			mc.options.setCameraType(net.minecraft.client.CameraType.FIRST_PERSON);
			if (!mc.gui.hud.isHidden()) {
				mc.gui.hud.toggle();
			}
		});
		for (int i = 0; i < count * 6; i++) {
			context.runOnClient(mc -> {
				var dragon = mc.player.getVehicle();
				if (dragon instanceof EntityDragonBase d) {
					besideIt(d, mc.player, eye[0]);
					mc.setCameraEntity(eye[0]);
				}
			});
			context.waitTick();
			if (i % 6 == 5) {
				context.takeScreenshot(name + "_" + i / 6);
			}
		}
		context.runOnClient(mc -> {
			mc.setCameraEntity(mc.player);
			mc.options.setCameraType(was[0]);
			if (mc.gui.hud.isHidden()) {
				mc.gui.hud.toggle();
			}
		});
	}

	/** The camera 7 blocks to the dragon's side (as it is turned now), at its rider's height, looking at the rider. */
	private static void besideIt(EntityDragonBase dragon, net.minecraft.world.entity.Entity rider, net.minecraft.world.entity.Entity eye) {
		float yaw = dragon.yBodyRot * net.minecraft.util.Mth.DEG_TO_RAD;
		var at = rider.position().add(0, 1, 0);
		double x = at.x + Math.cos(yaw) * 7, z = at.z + Math.sin(yaw) * 7, y = at.y + 0.5;
		double dx = at.x - x, dz = at.z - z;
		float look = (float) Math.toDegrees(Math.atan2(-dx, dz));
		eye.snapTo(x, y, z, look, 4F);
		eye.setYHeadRot(look);
		eye.setOldPosAndRot();
	}

	private static void buildFireForge(net.minecraft.server.level.ServerLevel level, BlockPos core) {
		for (int dy = -1; dy <= 1; dy++) {
			for (int dx = -1; dx <= 1; dx++) {
				for (int dz = -1; dz <= 1; dz++) {
					boolean corner = dx != 0 && dz != 0;
					var block = corner && dy != 0 ? com.iafabric.block.IafBlockRegistry.DRAGON_BONE_BLOCK : com.iafabric.block.IafBlockRegistry.DRAGONFORGE_FIRE_BRICK;
					if (dy == 0 && dx == 0 && dz == 0) {
						block = com.iafabric.block.IafBlockRegistry.DRAGONFORGE_FIRE_CORE_DISABLED;
					} else if (dy == 0 && dx == 0 && dz == -1) {
						block = com.iafabric.block.IafBlockRegistry.DRAGONFORGE_FIRE_INPUT;
					}
					level.setBlockAndUpdate(core.offset(dx, dy, dz), block.defaultBlockState());
				}
			}
		}
		var forge = (com.iafabric.entity.tile.TileEntityDragonforge) level.getBlockEntity(core);
		forge.setItem(0, new ItemStack(net.minecraft.world.item.Items.IRON_INGOT, 4));
		forge.setItem(1, new ItemStack(IafItemRegistry.FIRE_DRAGON_BLOOD, 4));
	}

	private static void report(ClientGameTestContext context, TestSingleplayerContext world, String when) {
		String client = context.computeOnClient(mc -> describe(mc));
		String[] server = new String[1];
		world.getServer().runOnServer(s -> {
			var player = s.getPlayerList().getPlayers().get(0);
			server[0] = String.format(Locale.ROOT, "server: player %s riding %s", fmt(player.position()), player.getVehicle() == null ? "nothing" : dragonState(player.getVehicle()));
		});
		log(when + " | " + client + " | " + server[0]);
	}

	private static String describe(Minecraft mc) {
		var vehicle = mc.player.getVehicle();
		return String.format(Locale.ROOT, "client: player %s riding %s", fmt(mc.player.position()), vehicle == null ? "nothing" : dragonState(vehicle));
	}

	private static String dragonState(net.minecraft.world.entity.Entity vehicle) {
		if (vehicle instanceof EntityDragonBase dragon) {
			return String.format(Locale.ROOT, "dragon at %s flying=%s hovering=%s onGround=%s breathing=%s", fmt(dragon.position()), dragon.isFlying(), dragon.isHovering(),
				dragon.onGround(), dragon.isBreathingFire());
		}
		return vehicle.getType().toShortString();
	}

	private static String fmt(net.minecraft.world.phys.Vec3 v) {
		return String.format(Locale.ROOT, "(%.1f %.1f %.1f)", v.x, v.y, v.z);
	}
}

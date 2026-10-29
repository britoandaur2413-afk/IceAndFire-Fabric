package com.iafabric.test;

import com.iafabric.IafMod;
import com.iafabric.entity.DragonSaddle;
import com.iafabric.entity.DragonSeatPose;
import com.iafabric.entity.EntityDragonBase;
import com.iafabric.entity.IafEntityRegistry;
import com.iafabric.item.IafItemRegistry;
import com.iafabric.test.ct.ClientGameTestContext;
import com.iafabric.test.ct.FabricClientGameTest;
import com.iafabric.test.ct.TestSingleplayerContext;
import net.minecraft.client.gui.screens.worldselection.WorldCreationUiState;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

import java.util.Locale;

/**
 * IAF_ONLY=saddle: the double saddle as a player sees it. On grown fire, ice and lightning dragons of three sizes (from
 * its side, ahead, behind and above), with a rider in each seat; the player riding a fire dragon with a friend behind
 * (on the ground, hovering, flying, breathing fire), the friend off with the player; its place in the dragon's menu;
 * its icon.
 */
public class IafSaddleScene implements FabricClientGameTest {
	private static final int GROUND = -60;
	/** The dragons kept on the ground for their pictures (a tamed dragon wandering takes off now and then). */
	private static final java.util.Set<Integer> KEPT_DOWN = java.util.concurrent.ConcurrentHashMap.newKeySet();

	static {
		net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents.START_SERVER_TICK.register(server -> {
			for (int id : KEPT_DOWN) {
				if (server.overworld().getEntity(id) instanceof EntityDragonBase dragon) {
					dragon.setFlying(false);
					dragon.setHovering(false);
					dragon.flyTicks = 0;
					dragon.hoverTicks = 0;
				}
			}
		});
	}

	private static void log(String message) {
		IafMod.LOGGER.info("[IAFSADDLE] {}", message);
	}

	@Override
	public void runTest(ClientGameTestContext context) {
		if (System.getenv("IAF_BATTLE") != null || IafTestOnly.skip("saddle")) {
			return;
		}
		try (TestSingleplayerContext world = context.worldBuilder().adjustSettings(settings ->
			settings.setGameMode(WorldCreationUiState.SelectedGameMode.SURVIVAL)).create()) {
			world.getServer().runCommand("gamerule doDaylightCycle false");
			world.getServer().runCommand("gamerule doMobSpawning false");
			world.getServer().runCommand("time set noon");
			world.getServer().runCommand("tp @p 0 " + GROUND + " 0 0 0");
			context.waitTicks(20);

			// three saddled dragons standing still, each with a rider in each seat: fire (stage 4), ice, lightning (its biggest)
			int[] shown = new int[3];
			world.getServer().runOnServer(server -> {
				var level = server.overworld();
				var player = server.getPlayerList().getPlayers().get(0);
				EntityType<?>[] types = {IafEntityRegistry.FIRE_DRAGON, IafEntityRegistry.ICE_DRAGON, IafEntityRegistry.LIGHTNING_DRAGON};
				int[] days = {80, 100, 125};
				for (int i = 0; i < 3; i++) {
					EntityDragonBase dragon = (EntityDragonBase) types[i].create(level);
					dragon.moveTo(-40 + i * 40, GROUND, 40, 90, 0);
					dragon.yBodyRot = 90;
					dragon.setYHeadRot(90);
					dragon.setAgeInDays(0);
					dragon.growDragon(days[i]);
					dragon.setAgingDisabled(true);
					// standing still, wings folded: no mind of its own (its model is not stiffened, as with no AI)
					dragon.fightLed = true;
					dragon.tame(player);
					dragon.dragonInventory.setItem(DragonSaddle.SLOT, new ItemStack(IafItemRegistry.DOUBLE_DRAGON_SADDLE));
					level.addFreshEntity(dragon);
					shown[i] = dragon.getId();
					KEPT_DOWN.add(dragon.getId());
				}
			});
			context.waitTicks(20);
			world.getServer().runOnServer(server -> {
				for (int id : shown) {
					var dragon = (EntityDragonBase) server.overworld().getEntity(id);
					log(String.format(Locale.ROOT, "%s: %.1f over the ground, on it %s, flying %s, hovering %s, wings out %.0f/%.0f",
						dragon.getType().toShortString(), dragon.getY() - GROUND, dragon.onGround(), dragon.isFlying(), dragon.isHovering(), dragon.hoverProgress, dragon.flyProgress));
				}
			});
			String[] names = {"fire", "ice", "lightning"};
			for (int i = 0; i < 3; i++) {
				views(context, shown[i], "saddle_" + names[i] + "_empty", i == 0);
			}
			// a rider in each seat (two players of the server: one owns it, the other is the friend)
			world.getServer().runOnServer(server -> {
				var level = server.overworld();
				for (int i = 0; i < 3; i++) {
					var dragon = (EntityDragonBase) level.getEntity(shown[i]);
					ServerPlayer owner = player(server, "Rider" + i, dragon.position());
					ServerPlayer friend = player(server, "Friend" + i, dragon.position());
					dragon.tame(owner);
					owner.startRiding(dragon, true);
					dragon.mobInteract(friend, InteractionHand.MAIN_HAND);
					log(String.format(Locale.ROOT, "%s dragon (size %.1f): owner on %s, friend on %s in the seat behind %s, seats %.2f blocks apart",
						names[i], dragon.getRenderSize() / 3F, owner.getVehicle() == dragon, friend.getVehicle() == dragon, DragonSaddle.inBackSeat(dragon, friend),
						DragonSeatPose.world(dragon).distanceTo(DragonSaddle.backSeat(dragon))));
				}
			});
			context.waitTicks(20);
			for (int i = 0; i < 3; i++) {
				views(context, shown[i], "saddle_" + names[i] + "_riders", i == 0);
			}

			// the player rides a fire dragon of their own, a friend gets on behind
			int[] ridden = {-1};
			int[] friendId = {-1};
			world.getServer().runOnServer(server -> {
				var level = server.overworld();
				var player = server.getPlayerList().getPlayers().get(0);
				EntityDragonBase dragon = IafEntityRegistry.FIRE_DRAGON.create(level);
				dragon.moveTo(0.5, GROUND, 6.5, 180, 0);
				dragon.setAgeInDays(0);
				dragon.growDragon(90);
				dragon.setHealth(dragon.getMaxHealth());
				dragon.tame(player);
				dragon.setCommand(0);
				level.addFreshEntity(dragon);
				ridden[0] = dragon.getId();
				// put on with a right click, as a player does
				player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(IafItemRegistry.DOUBLE_DRAGON_SADDLE));
				dragon.interact(player, InteractionHand.MAIN_HAND);
				log("saddle put on by a right click: " + dragon.hasDoubleSaddle() + ", left in hand " + player.getMainHandItem().getCount());
				friendId[0] = player(server, "Amigo", new Vec3(2.5, GROUND, 4.5)).getId();
			});
			context.waitTicks(20);
			world.getServer().runOnServer(server -> {
				var level = server.overworld();
				var player = server.getPlayerList().getPlayers().get(0);
				var dragon = (EntityDragonBase) level.getEntity(ridden[0]);
				var friend = (ServerPlayer) level.getEntity(friendId[0]);
				dragon.interact(friend, InteractionHand.MAIN_HAND);
				log("a friend's click before the owner is on: on it " + (friend.getVehicle() == dragon));
				// the player before its head (its head part, as the server places it: well ahead of the dragon's own box), to
				// get on with a right click of the mouse on it
				KEPT_DOWN.add(dragon.getId());
				var box = dragon.getParts().get(com.iafabric.entity.DragonBones.HEAD).getBoundingBox();
				var c = box.getCenter();
				var away = new Vec3(c.x - dragon.getX(), 0, c.z - dragon.getZ()).normalize();
				double off = Math.max(box.getXsize(), box.getZsize()) / 2 + 1.2;
				player.teleportTo(c.x + away.x * off, GROUND, c.z + away.z * off);
			});
			context.waitTicks(10);
			// aimed at its head part as the player's game has it, then a right click
			String[] aimed = {"nothing"};
			context.runOnClient(mc -> {
				var dragon = (EntityDragonBase) mc.level.getEntity(ridden[0]);
				var head = mc.level.getEntitiesOfClass(com.iafabric.entity.EntityDragonPart.class, dragon.getBoundingBox().inflate(30),
					p -> p.part() == com.iafabric.entity.DragonBones.HEAD && dragon.getUUID().equals(p.getParentId())).stream().findFirst().orElseThrow();
				var to = head.getBoundingBox().getCenter().subtract(mc.player.getEyePosition());
				float yaw = (float) Math.toDegrees(Math.atan2(-to.x, to.z));
				float pitch = (float) -Math.toDegrees(Math.atan2(to.y, Math.sqrt(to.x * to.x + to.z * to.z)));
				mc.player.setYRot(yaw);
				mc.player.setXRot(pitch);
				mc.player.yRotO = yaw;
				mc.player.xRotO = pitch;
				mc.player.setYHeadRot(yaw);
			});
			context.waitTicks(3);
			context.runOnClient(mc -> {
				if (mc.hitResult instanceof net.minecraft.world.phys.EntityHitResult hit) {
					aimed[0] = hit.getEntity() instanceof com.iafabric.entity.EntityDragonPart part ? "its part " + part.part() : hit.getEntity().getType().toShortString();
				}
			});
			context.getInput().pressMouse(org.lwjgl.glfw.GLFW.GLFW_MOUSE_BUTTON_RIGHT);
			context.waitTicks(10);
			world.getServer().runOnServer(server -> {
				var level = server.overworld();
				var player = server.getPlayerList().getPlayers().get(0);
				var dragon = (EntityDragonBase) level.getEntity(ridden[0]);
				var friend = (ServerPlayer) level.getEntity(friendId[0]);
				KEPT_DOWN.remove(dragon.getId());
				boolean on = player.getVehicle() == dragon;
				log("the player's right click with the mouse on " + aimed[0] + ": on it " + on);
				if (!aimed[0].startsWith("its part")) {
					throw new AssertionError("the right click was not on one of its parts but on " + aimed[0]);
				}
				if (!on) {
					throw new AssertionError("a right click on " + aimed[0] + " did not let its owner on");
				}
				dragon.interact(friend, InteractionHand.MAIN_HAND);
				log("owner on " + (player.getVehicle() == dragon) + ", friend on " + (friend.getVehicle() == dragon) + " behind " + DragonSaddle.inBackSeat(dragon, friend));
			});
			context.waitTicks(20);
			context.runOnClient(mc -> {
				var dragon = (EntityDragonBase) mc.level.getEntity(ridden[0]);
				var friend = mc.level.getEntity(friendId[0]);
				log("the player's game: friend " + (friend == null ? "unseen" : "on " + (friend.getVehicle() == dragon) + " behind " + DragonSaddle.inBackSeat(dragon, friend))
					+ ", saddle " + DragonSaddle.worn(dragon));
				mc.options.setCameraType(net.minecraft.client.CameraType.THIRD_PERSON_BACK);
				// (the other players' joins and advancements)
				mc.gui.getChat().clearMessages(false);
			});
			context.waitTicks(10);
			context.takeScreenshot("saddle_ride_behind");
			views(context, ridden[0], "saddle_ride_ground", false);
			context.runOnClient(mc -> mc.options.setCameraType(net.minecraft.client.CameraType.FIRST_PERSON));
			context.getInput().holdKey(options -> options.keyJump);
			context.waitTicks(50);
			context.getInput().releaseKey(options -> options.keyJump);
			views(context, ridden[0], "saddle_ride_hover", false);
			context.getInput().holdKey(options -> options.keyUp);
			context.waitTicks(40);
			views(context, ridden[0], "saddle_ride_fly", false);
			context.getInput().releaseKey(options -> options.keyUp);
			// its breath from its mouth, the player and the friend on its back (from its side and from behind the player)
			context.getInput().holdKey(com.mojang.blaze3d.platform.InputConstants.getKey("key.keyboard.r"));
			context.waitTicks(25);
			views(context, ridden[0], "saddle_ride_breath", false);
			context.runOnClient(mc -> mc.options.setCameraType(net.minecraft.client.CameraType.THIRD_PERSON_BACK));
			context.waitTicks(5);
			context.takeScreenshot("saddle_ride_breath_third");
			context.getInput().releaseKey(com.mojang.blaze3d.platform.InputConstants.getKey("key.keyboard.r"));
			context.runOnClient(mc -> mc.options.setCameraType(net.minecraft.client.CameraType.FIRST_PERSON));
			// down with shift (it lands and stays), shift again on the ground: the player off, the friend too
			context.getInput().holdKey(options -> options.keyShift);
			context.waitTicks(120);
			context.getInput().releaseKey(options -> options.keyShift);
			context.waitTicks(10);
			context.getInput().holdKeyFor(options -> options.keyShift, 10);
			context.waitTicks(20);
			world.getServer().runOnServer(server -> {
				var level = server.overworld();
				var player = server.getPlayerList().getPlayers().get(0);
				var dragon = (EntityDragonBase) level.getEntity(ridden[0]);
				var friend = (ServerPlayer) level.getEntity(friendId[0]);
				log(String.format(Locale.ROOT, "landed: owner on %s, friend on %s (seat behind %d), friend at %.1f over the ground",
					player.getVehicle() == dragon, friend.getVehicle() == dragon, dragon.getBackRider(), friend.getY() - GROUND));
			});

			// its place in the dragon's menu (its owner's sneak and right click): the saddle there; the dragon whole in its
			// window at three sizes (the player's own fire dragon of 90 days, the ice one of 100, the lightning one of 125)
			int[] menus = {ridden[0], shown[1], shown[2]};
			for (int i = 0; i < 3; i++) {
				final int id = menus[i];
				world.getServer().runOnServer(server -> {
					var player = server.getPlayerList().getPlayers().get(0);
					var dragon = (EntityDragonBase) server.overworld().getEntity(id);
					player.teleportTo(dragon.getX() + 5, dragon.getY(), dragon.getZ());
					dragon.openInventory(player);
				});
				context.runOnClient(mc -> IafMod.PROXY.setReferencedMob(mc.level.getEntity(id)));
				context.waitTicks(10);
				context.takeScreenshot("saddle_menu_" + names[i]);
				context.runOnClient(mc -> mc.player.closeContainer());
				context.waitTicks(5);
			}
			// and its icon in the hotbar
			world.getServer().runOnServer(server -> {
				var player = server.getPlayerList().getPlayers().get(0);
				player.getInventory().setItem(0, new ItemStack(IafItemRegistry.DOUBLE_DRAGON_SADDLE));
				player.getInventory().selected = 0;
			});
			context.runOnClient(mc -> mc.options.setCameraType(net.minecraft.client.CameraType.FIRST_PERSON));
			context.waitTicks(10);
			context.takeScreenshot("saddle_icon");
		}
	}

	/** Another player on the server (as a friend who joined), where the dragon is. */
	private static ServerPlayer player(MinecraftServer server, String name, Vec3 at) {
		var level = server.overworld();
		var player = new ServerPlayer(server, level, new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(), name));
		var connection = new net.minecraft.network.Connection(net.minecraft.network.protocol.PacketFlow.SERVERBOUND);
		new io.netty.channel.embedded.EmbeddedChannel(connection);
		server.getPlayerList().placeNewPlayer(connection, player);
		player.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);
		player.moveTo(at.x, at.y, at.z, 0F, 0F);
		return player;
	}

	/**
	 * Pictures of a dragon's back from a camera held on it as it moves: its side, ahead of it to its side, behind and
	 * over it, and (all) from above. Looking at the middle between its seats.
	 */
	private static void views(ClientGameTestContext context, int dragonId, String name, boolean all) {
		String[] views = all ? new String[]{"side", "close", "front", "behind", "above"} : new String[]{"side", "close", "behind"};
		ArmorStand[] eye = {null};
		net.minecraft.client.CameraType[] was = {null};
		context.runOnClient(mc -> {
			eye[0] = new ArmorStand(mc.level, mc.player.getX(), mc.player.getY(), mc.player.getZ());
			was[0] = mc.options.getCameraType();
			mc.options.setCameraType(net.minecraft.client.CameraType.FIRST_PERSON);
			mc.options.hideGui = true;
		});
		for (String view : views) {
			for (int i = 0; i < 4; i++) {
				context.runOnClient(mc -> {
					if (mc.level.getEntity(dragonId) instanceof EntityDragonBase dragon) {
						aim(dragon, eye[0], view);
						mc.setCameraEntity(eye[0]);
					}
				});
				context.waitTick();
			}
			context.takeScreenshot(name + "_" + view);
		}
		context.runOnClient(mc -> {
			mc.setCameraEntity(mc.player);
			mc.options.setCameraType(was[0]);
			mc.options.hideGui = false;
		});
	}

	private static void aim(EntityDragonBase dragon, ArmorStand eye, String view) {
		float yaw = dragon.yBodyRot * Mth.DEG_TO_RAD;
		Vec3 forward = new Vec3(-Mth.sin(yaw), 0, Mth.cos(yaw));
		Vec3 right = new Vec3(-Mth.cos(yaw), 0, -Mth.sin(yaw));
		double size = dragon.getRenderSize() / 3.0;
		Vec3 at = DragonSeatPose.world(dragon).add(DragonSaddle.backSeat(dragon)).scale(0.5);
		Vec3 from = switch (view) {
			case "close" -> at.add(right.scale(size * 0.6 + 2)).add(forward.scale(-(size * 0.3))).add(0, size * 0.6 + 1.5, 0);
			case "front" -> at.add(forward.scale(size * 1.3 + 3)).add(right.scale(size * 1.3 + 3)).add(0, size * 0.9, 0);
			case "behind" -> at.add(forward.scale(-(size * 1.2 + 3))).add(0, size * 0.8 + 1.5, 0);
			case "above" -> at.add(forward.scale(-0.5)).add(0, size * 1.6 + 4, 0);
			default -> at.add(right.scale(size * 1.7 + 5)).add(0, size * 0.9 + 1.5, 0);
		};
		Vec3 d = at.subtract(from);
		float look = (float) Math.toDegrees(Math.atan2(-d.x, d.z));
		float pitch = (float) -Math.toDegrees(Math.atan2(d.y, Math.sqrt(d.x * d.x + d.z * d.z)));
		eye.moveTo(from.x, from.y - eye.getEyeHeight(), from.z, look, pitch);
		eye.setYHeadRot(look);
		eye.setOldPosAndRot();
	}
}

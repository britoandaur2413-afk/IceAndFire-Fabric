package com.iafabric.test;

import com.iafabric.IafMod;
import com.iafabric.entity.DragonSaddle;
import com.iafabric.entity.DragonSeatPose;
import com.iafabric.entity.EntityDragonBase;
import com.iafabric.entity.IafEntityRegistry;
import com.iafabric.item.IafItemRegistry;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.client.gui.screens.worldselection.WorldCreationUiState;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;

import java.util.Locale;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * IAF_ONLY=promo: pictures for a version's notes, in a world of the game's own land at noon. An ice dragon hovering with
 * two players on its double saddle (from ahead and its side, from its side, from behind), then the three breaths (fire,
 * frost, lightning), each dragon hovering and breathing ahead of it, seen from its side a little ahead.
 */
public class IafPromoScene implements FabricClientGameTest {
	/** The dragons held hovering where they are, wings spread (no mind of their own, no gravity, never landing). */
	private static final Set<Integer> HOVERING = ConcurrentHashMap.newKeySet();

	static {
		net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents.START_SERVER_TICK.register(server -> {
			for (int id : HOVERING) {
				if (server.overworld().getEntity(id) instanceof EntityDragonBase dragon) {
					dragon.setNoGravity(true);
					dragon.setHovering(true);
					dragon.setFlying(false);
					dragon.flyTicks = 0;
					dragon.hoverTicks = 0;
					dragon.setDeltaMovement(Vec3.ZERO);
				}
			}
		});
	}

	private static void log(String message) {
		IafMod.LOGGER.info("[IAFPROMO] {}", message);
	}

	@Override
	public void runTest(ClientGameTestContext context) {
		if (System.getenv("IAF_BATTLE") != null || IafTestOnly.skip("promo")) {
			return;
		}
		try (TestSingleplayerContext world = context.worldBuilder().adjustSettings(settings -> {
			settings.setGameMode(WorldCreationUiState.SelectedGameMode.CREATIVE);
			// the game's own land (the test default is flat)
			for (WorldCreationUiState.WorldTypeEntry entry : settings.getNormalPresetList()) {
				if (entry.preset() != null && entry.preset().is(net.minecraft.world.level.levelgen.presets.WorldPresets.NORMAL)) {
					settings.setWorldType(entry);
				}
			}
		}).create()) {
			world.getServer().runCommand("gamerule spawn_mobs false");
			world.getServer().runCommand("gamerule advance_time false");
			world.getServer().runCommand("gamerule advance_weather false");
			world.getServer().runCommand("time set noon");
			world.getServer().runCommand("weather clear");
			context.runOnClient(mc -> {
				mc.options.renderDistance().set(16);
				if (!mc.gui.hud.isHidden()) {
					mc.gui.hud.toggle();
				}
			});
			// over the land by the world's spawn, high enough to hover clear of its trees
			Vec3[] base = {null};
			world.getServer().runOnServer(server -> {
				var level = server.overworld();
				var spawn = level.getRespawnData().pos();
				int ground = level.getHeight(Heightmap.Types.MOTION_BLOCKING, spawn.getX(), spawn.getZ());
				base[0] = new Vec3(spawn.getX() + 0.5, Math.max(ground, level.getSeaLevel()) + 22, spawn.getZ() + 0.5);
				log("spawn " + spawn + ", ground " + ground + ", dragons at " + base[0]);
			});
			// (the land about it made and drawn before any picture)
			fly(context, world, base[0].add(30, 6, 30), 0, 0, 120);

			// an ice dragon with two players on its double saddle: its owner in front, a friend behind
			int[] saddled = {-1};
			ServerPlayer[] riders = new ServerPlayer[2];
			world.getServer().runOnServer(server -> {
				EntityDragonBase dragon = dragon(server, IafEntityRegistry.ICE_DRAGON, 95, base[0], 210F);
				dragon.dragonInventory.setItem(DragonSaddle.SLOT, new ItemStack(IafItemRegistry.DOUBLE_DRAGON_SADDLE));
				riders[0] = player(server, "Rider", base[0]);
				riders[1] = player(server, "Friend", base[0]);
				dragon.tame(riders[0]);
				riders[0].startRiding(dragon, true, true);
				dragon.mobInteract(riders[1], InteractionHand.MAIN_HAND);
				saddled[0] = dragon.getId();
				log("ice dragon: owner on " + (riders[0].getVehicle() == dragon) + ", friend behind " + DragonSaddle.inBackSeat(dragon, riders[1]));
			});
			context.waitTicks(40);
			// over the riders' shoulders toward its head, then from its side and from ahead to its side, close
			shoot(context, world, saddled[0], "promo_saddle_ride", -0.55, 0.35, 0.3, 20);
			shoot(context, world, saddled[0], "promo_saddle_side", 0.15, 1.2, 0.55, 20);
			shoot(context, world, saddled[0], "promo_saddle_front", 1.0, 0.9, 0.3, 20);
			world.getServer().runOnServer(server -> {
				var dragon = server.overworld().getEntity(saddled[0]);
				// (the two players leave the game: they would hang where they sat)
				for (ServerPlayer rider : riders) {
					rider.stopRiding();
					server.getPlayerList().remove(rider);
				}
				HOVERING.remove(saddled[0]);
				dragon.discard();
			});

			// the three breaths, each grown dragon hovering and breathing ahead of it
			String[] kinds = {"fire", "ice", "lightning"};
			EntityType<?>[] types = {IafEntityRegistry.FIRE_DRAGON, IafEntityRegistry.ICE_DRAGON, IafEntityRegistry.LIGHTNING_DRAGON};
			for (int i = 0; i < 3; i++) {
				final EntityType<?> type = types[i];
				int[] id = {-1};
				double[] target = new double[3];
				world.getServer().runOnServer(server -> {
					EntityDragonBase dragon = dragon(server, type, 100, base[0], 250F);
					id[0] = dragon.getId();
					// ahead of it, a little lower than its head: in the air, nothing there to burn
					float yaw = 250F * Mth.DEG_TO_RAD;
					double size = dragon.getRenderSize() / 3.0;
					Vec3 at = base[0].add(-Mth.sin(yaw) * size * 3.2, size * 0.4, Mth.cos(yaw) * size * 3.2);
					target[0] = at.x;
					target[1] = at.y;
					target[2] = at.z;
				});
				context.waitTicks(30);
				for (int tick = 0; tick < 24; tick++) {
					breathe(world, id[0], target);
					context.waitTick();
				}
				shootBreathing(context, world, id[0], target, "promo_breath_" + kinds[i]);
				world.getServer().runOnServer(server -> {
					HOVERING.remove(id[0]);
					server.overworld().getEntity(id[0]).discard();
				});
				context.waitTicks(10);
			}
		}
	}

	/** A grown dragon hovering where it is put, turned to a side, standing still (no mind of its own). */
	private static EntityDragonBase dragon(MinecraftServer server, EntityType<?> type, int days, Vec3 at, float yaw) {
		var level = server.overworld();
		EntityDragonBase dragon = (EntityDragonBase) type.create(level, EntitySpawnReason.COMMAND);
		dragon.snapTo(at.x, at.y, at.z, yaw, 0);
		dragon.yBodyRot = yaw;
		dragon.setYHeadRot(yaw);
		dragon.setAgeInDays(0);
		dragon.growDragon(days);
		dragon.setAgingDisabled(true);
		dragon.setHealth(dragon.getMaxHealth());
		dragon.fightLed = true;
		level.addFreshEntity(dragon);
		HOVERING.add(dragon.getId());
		return dragon;
	}

	/** Another player on the server (as a friend who joined), where the dragon is. */
	private static ServerPlayer player(MinecraftServer server, String name, Vec3 at) {
		var level = server.overworld();
		var cookie = net.minecraft.server.network.CommonListenerCookie.createInitial(new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(), name), false);
		var player = new ServerPlayer(server, level, cookie.gameProfile(), cookie.clientInformation());
		var connection = new net.minecraft.network.Connection(net.minecraft.network.protocol.PacketFlow.SERVERBOUND);
		new io.netty.channel.embedded.EmbeddedChannel(connection);
		server.getPlayerList().placeNewPlayer(connection, player, cookie);
		player.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);
		player.snapTo(at.x, at.y, at.z, 0F, 0F);
		return player;
	}

	/** The player flying at a place, looking a way (the picture is through their eyes, the hud hidden). */
	private static void fly(ClientGameTestContext context, TestSingleplayerContext world, Vec3 at, float yaw, float pitch, int wait) {
		world.getServer().runOnServer(server -> {
			var player = server.getPlayerList().getPlayers().get(0);
			player.getAbilities().mayfly = true;
			player.getAbilities().flying = true;
			player.onUpdateAbilities();
			player.teleportTo(server.overworld(), at.x, at.y, at.z, Set.of(), yaw, pitch, false);
		});
		context.runOnClient(mc -> mc.player.getAbilities().flying = true);
		context.waitTicks(wait);
	}

	/**
	 * A picture of a dragon's saddle and riders from a place by it, in its sizes: ahead of it, to its right and over
	 * the middle between its seats, looking at that middle.
	 */
	private static void shoot(ClientGameTestContext context, TestSingleplayerContext world, int id, String name, double ahead, double right, double over, int wait) {
		Vec3[] from = {null}, to = {null};
		world.getServer().runOnServer(server -> {
			var dragon = (EntityDragonBase) server.overworld().getEntity(id);
			float yaw = dragon.yBodyRot * Mth.DEG_TO_RAD;
			Vec3 forward = new Vec3(-Mth.sin(yaw), 0, Mth.cos(yaw)), side = new Vec3(-Mth.cos(yaw), 0, -Mth.sin(yaw));
			double size = dragon.getRenderSize() / 3.0;
			Vec3 seats = DragonSeatPose.world(dragon).add(DragonSaddle.backSeat(dragon)).scale(0.5).add(0, 0.6, 0);
			from[0] = seats.add(forward.scale(size * ahead + Math.signum(ahead) * 2)).add(side.scale(size * right + 1.5)).add(0, size * over + 1, 0);
			// (from behind, looking past the riders along its neck; else at the riders)
			to[0] = ahead < 0 ? seats.add(forward.scale(size * 0.9)).add(0, size * 0.1, 0) : seats;
		});
		look(context, world, from[0], to[0], wait);
		context.takeScreenshot(name);
	}

	/** A picture of a dragon breathing: from its side a little ahead of it, looking between its mouth and where it breathes. */
	private static void shootBreathing(ClientGameTestContext context, TestSingleplayerContext world, int id, double[] target, String name) {
		Vec3[] from = {null}, to = {null};
		world.getServer().runOnServer(server -> {
			var dragon = (EntityDragonBase) server.overworld().getEntity(id);
			float yaw = dragon.yBodyRot * Mth.DEG_TO_RAD;
			Vec3 forward = new Vec3(-Mth.sin(yaw), 0, Mth.cos(yaw)), side = new Vec3(-Mth.cos(yaw), 0, -Mth.sin(yaw));
			double size = dragon.getRenderSize() / 3.0;
			Vec3 head = dragon.position().add(forward.scale(size * 1.2)).add(0, size * 0.9, 0);
			to[0] = head.add(new Vec3(target[0], target[1], target[2])).scale(0.5);
			// (a little ahead of its raised wing: its face, its breath and its body)
			from[0] = to[0].add(side.scale(size * 2.4 + 6)).add(forward.scale(size * 0.3)).add(0, size * 0.3, 0);
		});
		look(context, world, from[0], to[0], 2);
		// (breathing on while the camera settles: its stream full when the picture is taken)
		for (int tick = 0; tick < 20; tick++) {
			breathe(world, id, target);
			context.waitTick();
		}
		context.takeScreenshot(name);
	}

	private static void look(ClientGameTestContext context, TestSingleplayerContext world, Vec3 from, Vec3 to, int wait) {
		Vec3 d = to.subtract(from);
		float yaw = (float) Math.toDegrees(Math.atan2(-d.x, d.z));
		float pitch = (float) -Math.toDegrees(Math.atan2(d.y, Math.sqrt(d.x * d.x + d.z * d.z)));
		// (the player's eyes there: their feet an eye's height lower)
		fly(context, world, from.subtract(0, 1.62, 0), yaw, pitch, wait);
		log(String.format(Locale.ROOT, "camera at (%.1f %.1f %.1f) looking %.0f/%.0f", from.x, from.y, from.z, yaw, pitch));
	}

	/** It breathes at its point, this tick (as the riding test's breath: the server says so to the players' games). */
	private static void breathe(TestSingleplayerContext world, int id, double[] target) {
		world.getServer().runOnServer(server -> {
			if (server.overworld().getEntity(id) instanceof EntityDragonBase dragon) {
				dragon.setBreathingFire(true);
				dragon.stimulateFire(target[0], target[1], target[2], 1);
			}
		});
	}
}

package com.iafabric.test;

import com.iafabric.IafMod;
import com.iafabric.entity.EntityDragonBase;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.client.gui.screens.worldselection.WorldCreationUiState;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.phys.Vec3;

import java.util.Locale;

/**
 * IAF_ONLY=tridragon: the three-headed dragon's model (TriDragonPreview) as a player sees it, grown (125 days, a quarter
 * bigger than a dragon of its age): standing, from ahead, its sides, behind and above, its three heads close; then
 * hovering. Only when asked by name.
 */
public class IafTriDragonScene implements FabricClientGameTest {
	private static final int GROUND = -60;
	/** Kept on the ground (0) or in the air (1) for its pictures (a dragon takes off or lands on its own). */
	private static final java.util.Map<Integer, Integer> HELD = new java.util.concurrent.ConcurrentHashMap<>();

	static {
		net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents.START_SERVER_TICK.register(server -> {
			HELD.forEach((id, air) -> {
				if (server.overworld().getEntity(id) instanceof EntityDragonBase dragon) {
					dragon.setFlying(false);
					dragon.setHovering(air == 1);
					dragon.flyTicks = 0;
					dragon.hoverTicks = 0;
				}
			});
		});
	}

	private static void log(String message) {
		IafMod.LOGGER.info("[IAFTRI] {}", message);
	}

	@Override
	public void runTest(ClientGameTestContext context) {
		if (System.getenv("IAF_BATTLE") != null || IafTestOnly.skip("tridragon") || System.getenv().getOrDefault("IAF_ONLY", "").isBlank()) {
			return;
		}
		try (TestSingleplayerContext world = context.worldBuilder().adjustSettings(settings ->
			settings.setGameMode(WorldCreationUiState.SelectedGameMode.CREATIVE)).create()) {
			world.getServer().runCommand("gamerule advance_time false");
			world.getServer().runCommand("gamerule spawn_mobs false");
			world.getServer().runCommand("time set noon");
			world.getServer().runCommand("weather clear");
			world.getServer().runCommand("tp @p 0 " + GROUND + " 0 0 0");
			context.waitTicks(20);
			int[] id = {-1};
			world.getServer().runOnServer(server -> {
				var level = server.overworld();
				var player = server.getPlayerList().getPlayers().get(0);
				EntityDragonBase dragon = TriDragonPreview.TYPE.create(level, EntitySpawnReason.COMMAND);
				dragon.snapTo(0.5, GROUND, 24.5, 180, 0);
				dragon.yBodyRot = 180;
				dragon.setYHeadRot(180);
				dragon.setAgeInDays(0);
				dragon.growDragon(125);
				dragon.setAgingDisabled(true);
				dragon.fightLed = true;
				dragon.tame(player);
				level.addFreshEntity(dragon);
				id[0] = dragon.getId();
				HELD.put(dragon.getId(), 0);
			});
			context.waitTicks(40);
			context.runOnClient(mc -> {
				var dragon = (EntityDragonBase) mc.level.getEntity(id[0]);
				log(String.format(Locale.ROOT, "the three-headed dragon: size %.1f (x %.2f drawn), stage %d", dragon.getRenderSize() / 3F,
					TriDragonPreviewClient.BIGGER, dragon.getDragonStage()));
			});
			views(context, id[0], "tridragon_ground", new String[]{"front", "front_right", "right", "back", "left", "above", "heads"});
			// in the air: its wings spread
			world.getServer().runOnServer(server -> {
				var dragon = (EntityDragonBase) server.overworld().getEntity(id[0]);
				HELD.put(id[0], 1);
				dragon.setPos(dragon.getX(), GROUND + 12, dragon.getZ());
				dragon.setNoGravity(true);
			});
			context.waitTicks(60);
			views(context, id[0], "tridragon_hover", new String[]{"front", "front_right", "right", "above"});
		}
	}

	/** Pictures from a camera held on it as it moves (the hud hidden). */
	private static void views(ClientGameTestContext context, int id, String name, String[] views) {
		ArmorStand[] eye = {null};
		context.runOnClient(mc -> {
			eye[0] = new ArmorStand(mc.level, mc.player.getX(), mc.player.getY(), mc.player.getZ());
			mc.options.setCameraType(net.minecraft.client.CameraType.FIRST_PERSON);
			if (!mc.gui.hud.isHidden()) {
				mc.gui.hud.toggle();
			}
		});
		for (String view : views) {
			for (int i = 0; i < 4; i++) {
				context.runOnClient(mc -> {
					if (mc.level.getEntity(id) instanceof EntityDragonBase dragon) {
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
			if (mc.gui.hud.isHidden()) {
				mc.gui.hud.toggle();
			}
		});
	}

	private static void aim(EntityDragonBase dragon, ArmorStand eye, String view) {
		float yaw = dragon.yBodyRot * Mth.DEG_TO_RAD;
		Vec3 forward = new Vec3(-Mth.sin(yaw), 0, Mth.cos(yaw));
		Vec3 right = new Vec3(-Mth.cos(yaw), 0, -Mth.sin(yaw));
		double size = dragon.getRenderSize() / 3.0 * TriDragonPreviewClient.BIGGER;
		// (its middle neck stands high: the middle of the picture a little over its back)
		Vec3 at = dragon.position().add(0, size * (dragon.isHovering() || dragon.isFlying() ? 1.9 : 1.6), 0).add(forward.scale(size * 0.4));
		double far = size * 4.2 + 8;
		Vec3 from = switch (view) {
			case "front_right" -> at.add(forward.scale(far * 0.75)).add(right.scale(far * 0.75)).add(0, size * 0.5, 0);
			case "right" -> at.add(right.scale(far)).add(0, size * 0.3, 0);
			case "left" -> at.add(right.scale(-far)).add(0, size * 0.3, 0);
			case "back" -> at.add(forward.scale(-far * 0.8)).add(right.scale(far * 0.5)).add(0, size * 0.6, 0);
			case "above" -> at.add(forward.scale(far * 0.3)).add(0, far * 1.1, 0);
			case "heads" -> {
				at = dragon.position().add(0, size * 1.5, 0).add(forward.scale(size * 1.0));
				yield at.add(forward.scale(size * 1.3 + 3)).add(right.scale(size * 0.4)).add(0, size * 0.2, 0);
			}
			default -> at.add(forward.scale(far)).add(0, size * 0.4, 0);
		};
		Vec3 d = at.subtract(from);
		float look = (float) Math.toDegrees(Math.atan2(-d.x, d.z));
		float pitch = (float) -Math.toDegrees(Math.atan2(d.y, Math.sqrt(d.x * d.x + d.z * d.z)));
		eye.snapTo(from.x, from.y - eye.getEyeHeight(), from.z, look, pitch);
		eye.setYHeadRot(look);
		eye.setOldPosAndRot();
	}
}

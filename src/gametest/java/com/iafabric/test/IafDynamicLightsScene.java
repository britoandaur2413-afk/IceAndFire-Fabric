package com.iafabric.test;

import com.iafabric.IafMod;
import com.iafabric.entity.EntitySeaSerpent;
import com.iafabric.entity.IafEntityRegistry;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.CameraType;
import net.minecraft.client.gui.screens.worldselection.WorldCreationUiState;
import net.minecraft.core.BlockPos;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

/**
 * IAF_ONLY=dynamiclights, with LambDynamicLights in the run's mods folder (a player: sea serpents crashed the game with
 * it): sea serpents of every size swim in a pool by a player holding a torch, one burns on the shore, they fight the
 * player, and the world is left and opened again with them in it. Nothing may move to NaN: a leap straight up and
 * a splash on a creature right under its middle divided by zero (NaN speed, then a NaN place: what crashed the lights).
 */
public class IafDynamicLightsScene implements FabricClientGameTest {
	private static final int GROUND = -60;

	private static void log(String message) {
		IafMod.LOGGER.info("[IAFLIGHTS] {}", message);
	}

	@Override
	public void runTest(ClientGameTestContext context) {
		if (IafTestOnly.skip("dynamiclights")) {
			return;
		}
		List<String> problems = new ArrayList<>();
		log("LambDynamicLights loaded: " + FabricLoader.getInstance().isModLoaded("lambdynlights"));
		try (TestSingleplayerContext world = context.worldBuilder().adjustSettings(settings -> {
			settings.setGameMode(WorldCreationUiState.SelectedGameMode.SURVIVAL);
			settings.setAllowCommands(true);
		}).create()) {
			world.getServer().runCommand("difficulty normal");
			world.getServer().runCommand("gamerule advance_time false");
			world.getServer().runCommand("gamerule spawn_mobs false");
			world.getServer().runCommand("time set midnight");
			world.getServer().runCommand("fill -24 " + (GROUND - 3) + " 4 24 " + (GROUND - 1) + " 40 minecraft:water");
			world.getServer().runCommand("tp @p 0 " + GROUND + " 0 0 20");
			world.getServer().runOnServer(server -> {
				var player = server.getPlayerList().getPlayers().get(0);
				player.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.TORCH));
				player.addEffect(new MobEffectInstance(MobEffects.RESISTANCE, 20000, 4));
				player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 20000, 4));
				var level = server.overworld();
				float[] scales = {0.5F, 1.5F, 3F, 5F};
				for (int i = 0; i < scales.length; i++) {
					EntitySeaSerpent serpent = IafEntityRegistry.SEA_SERPENT.create(level, EntitySpawnReason.COMMAND);
					serpent.setVariant(i);
					scale(serpent, scales[i]);
					serpent.setAncient(i == 3);
					serpent.snapTo(-12 + i * 8, GROUND - 3, 12 + i * 6, 0, 0);
					level.addFreshEntity(serpent);
					serpent.setTarget(player);
				}
				EntitySeaSerpent burning = IafEntityRegistry.SEA_SERPENT.create(level, EntitySpawnReason.COMMAND);
				scale(burning, 1F);
				burning.snapTo(4, GROUND, -4, 0, 0);
				level.addFreshEntity(burning);
				burning.igniteForSeconds(30);
			});
			context.runOnClient(mc -> mc.options.setCameraType(CameraType.FIRST_PERSON));
			// what made NaN: a leap straight up (pitch 90) and a splash on a creature right under its middle
			world.getServer().runOnServer(server -> {
				var level = server.overworld();
				EntitySeaSerpent leaper = IafEntityRegistry.SEA_SERPENT.create(level, EntitySpawnReason.COMMAND);
				scale(leaper, 2F);
				leaper.snapTo(-4, GROUND - 2, 30, 0, 0);
				level.addFreshEntity(leaper);
				leaper.setXRot(90F);
				leaper.moveJumping();
				Vec3 motion = leaper.getDeltaMovement();
				log("leap at pitch 90: speed " + motion);
				if (!Double.isFinite(motion.x) || !Double.isFinite(motion.z)) {
					problems.add("a leap at pitch 90 made a NaN speed " + motion);
				}
				var cow = EntityTypes.COW.create(level, EntitySpawnReason.COMMAND);
				cow.snapTo(leaper.getX(), leaper.getY(), leaper.getZ(), 0, 0);
				level.addFreshEntity(cow);
				try {
					var splash = EntitySeaSerpent.class.getDeclaredMethod("doSplashDamage");
					splash.setAccessible(true);
					splash.invoke(leaper);
				} catch (ReflectiveOperationException e) {
					throw new RuntimeException(e);
				}
				log("cow splashed from right under its middle: speed " + cow.getDeltaMovement());
				if (!Double.isFinite(cow.getDeltaMovement().x)) {
					problems.add("the splash threw a creature under its middle at NaN");
				}
			});
			// a serpent already gone NaN (as one in a world played before this version): it is put back, nothing crashes
			world.getServer().runOnServer(server -> {
				var level = server.overworld();
				EntitySeaSerpent lost = IafEntityRegistry.SEA_SERPENT.create(level, EntitySpawnReason.COMMAND);
				scale(lost, 2F);
				lost.snapTo(8, GROUND - 2, 30, 0, 0);
				level.addFreshEntity(lost);
				lost.setDeltaMovement(Double.NaN, Double.NaN, Double.NaN);
			});
			context.waitTicks(40);
			world.getServer().runOnServer(server -> {
				for (EntitySeaSerpent serpent : server.overworld().getEntitiesOfClass(EntitySeaSerpent.class, new AABB(BlockPos.ZERO).inflate(100))) {
					log("serpent " + serpent.getId() + " at " + serpent.position());
				}
			});
			for (int second = 1; second <= 20; second++) {
				context.waitTicks(20);
				world.getServer().runOnServer(server -> {
					for (var entity : server.overworld().getAllEntities()) {
						if (entity != null && (!Double.isFinite(entity.getX() + entity.getY() + entity.getZ())
							|| !Double.isFinite(entity.getDeltaMovement().lengthSqr()))) {
							problems.add(entity.getType() + " at NaN: " + entity.position() + " speed " + entity.getDeltaMovement());
						}
					}
				});
				if (second % 5 == 0) {
					context.takeScreenshot("dynamiclights_" + second);
				}
			}
			// what crashed a player's game: a serpent that grows while a light is near (the torch): its own copies of its
			// tail on the player's game, never in the level, were removed, and LambDynamicLights asked for their id
			for (int grow = 0; grow < 3; grow++) {
				float size = 1.5F + grow;
				world.getServer().runOnServer(server -> {
					for (EntitySeaSerpent serpent : server.overworld().getEntitiesOfClass(EntitySeaSerpent.class, new AABB(BlockPos.ZERO).inflate(100))) {
						scale(serpent, size);
					}
				});
				context.waitTicks(20);
			}
			log("sea serpents grew three times near the torch");
			// the other bodies with parts, near the torch too: a hydra that grows heads, a death worm that grows, and
			// a dragon and a cyclops that go away (their parts go with them)
			world.getServer().runOnServer(server -> {
				var level = server.overworld();
				var hydra = IafEntityRegistry.HYDRA.create(level, EntitySpawnReason.COMMAND);
				hydra.snapTo(-6, GROUND, -6, 0, 0);
				level.addFreshEntity(hydra);
				var worm = IafEntityRegistry.DEATH_WORM.create(level, EntitySpawnReason.COMMAND);
				worm.snapTo(6, GROUND, -6, 0, 0);
				level.addFreshEntity(worm);
				var dragon = IafEntityRegistry.FIRE_DRAGON.create(level, EntitySpawnReason.COMMAND);
				dragon.setAgeInDays(60);
				dragon.snapTo(0, GROUND, -14, 0, 0);
				level.addFreshEntity(dragon);
				var cyclops = IafEntityRegistry.CYCLOPS.create(level, EntitySpawnReason.COMMAND);
				cyclops.snapTo(-10, GROUND, -2, 0, 0);
				level.addFreshEntity(cyclops);
			});
			context.waitTicks(40);
			for (int grow = 0; grow < 3; grow++) {
				int step = grow;
				world.getServer().runOnServer(server -> {
					var level = server.overworld();
					AABB around = new AABB(BlockPos.ZERO).inflate(100);
					for (var hydra : level.getEntitiesOfClass(com.iafabric.entity.EntityHydra.class, around)) {
						hydra.setHeadCount(2 + step * 3);
					}
					for (var worm : level.getEntitiesOfClass(com.iafabric.entity.EntityDeathWorm.class, around)) {
						worm.setWormAge(5 + step * 5);
						worm.setDeathWormScale(1F + step);
					}
				});
				context.waitTicks(20);
			}
			world.getServer().runOnServer(server -> {
				AABB around = new AABB(BlockPos.ZERO).inflate(100);
				server.overworld().getEntitiesOfClass(com.iafabric.entity.EntityDragonBase.class, around).forEach(e -> e.discard());
				server.overworld().getEntitiesOfClass(com.iafabric.entity.EntityCyclops.class, around).forEach(e -> e.discard());
			});
			context.waitTicks(40);
			log("hydra, death worm, dragon and cyclops changed near the torch");
			int[] seen = {0};
			context.runOnClient(mc -> seen[0] = mc.level.getEntitiesOfClass(EntitySeaSerpent.class,
				new AABB(BlockPos.ZERO).inflate(100)).size());
			log("sea serpents on the client after 20 s: " + seen[0]);
			// a sea serpent's own segments as the player's torch passes by them
			world.getServer().runCommand("tp @p 0 " + (GROUND - 1) + " 20 0 0");
			context.waitTicks(100);
			context.takeScreenshot("dynamiclights_among_them");
			TestWorldSaveHolder.save = world.getWorldSave();
		}
		try (TestSingleplayerContext again = TestWorldSaveHolder.save.open()) {
			context.waitTicks(100);
			int[] seen = {0};
			context.runOnClient(mc -> seen[0] = mc.level.getEntitiesOfClass(EntitySeaSerpent.class,
				new AABB(BlockPos.ZERO).inflate(100)).size());
			log("sea serpents after the world was opened again: " + seen[0]);
			context.takeScreenshot("dynamiclights_reopened");
			if (seen[0] == 0) {
				problems.add("no sea serpents after the world was opened again");
			}
		}
		if (!problems.isEmpty()) {
			throw new AssertionError(String.join("; ", new java.util.LinkedHashSet<>(problems)));
		}
	}

	private static void scale(EntitySeaSerpent serpent, float scale) {
		try {
			var method = EntitySeaSerpent.class.getDeclaredMethod("setSeaSerpentScale", float.class);
			method.setAccessible(true);
			method.invoke(serpent, scale);
		} catch (ReflectiveOperationException e) {
			throw new RuntimeException(e);
		}
	}

	private static final class TestWorldSaveHolder {
		static net.fabricmc.fabric.api.client.gametest.v1.world.TestWorldSave save;
	}
}

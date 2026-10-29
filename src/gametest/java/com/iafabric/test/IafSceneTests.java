package com.iafabric.test;

import com.iafabric.IafMod;
import com.iafabric.entity.DragonRiders;
import com.iafabric.entity.EntityDragonBase;
import com.iafabric.entity.IafEntityRegistry;
import com.iafabric.item.IafItemRegistry;
import com.iafabric.test.ct.FabricClientGameTest;
import com.iafabric.test.ct.ClientGameTestContext;
import com.iafabric.test.ct.TestSingleplayerContext;
import net.minecraft.client.gui.screens.worldselection.WorldCreationUiState;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;

import java.util.Locale;

/**
 * Pictures of what the third round added, on a flat world: the riders drawn on the dragons of the pillagers and the
 * dread (standing, sitting, hit, killed and replaced); the effects of the fights; Cryona's fight in her realm; the pixies' spells. IAF_ONLY=riders, vfx, boss, magic.
 */
public class IafSceneTests implements FabricClientGameTest {
	private static final int GROUND = -60;

	private static void log(String message) {
		IafMod.LOGGER.info("[IAFSCENE] {}", message);
	}

	@Override
	public void runTest(ClientGameTestContext context) {
		if (System.getenv("IAF_BATTLE") != null) {
			return;
		}
		boolean riders = !IafTestOnly.skip("riders");
		boolean vfx = !IafTestOnly.skip("vfx");
		boolean boss = !IafTestOnly.skip("boss");
		boolean magic = !IafTestOnly.skip("magic");
		boolean hitbox = !IafTestOnly.skip("hitbox");
		// (only when asked by name: Black Frost measured against a ruler, for the rooms made for it)
		boolean frostSize = System.getenv("IAF_ONLY") != null && !IafTestOnly.skip("frostsize");
		// (only when asked by name: the dragons' parts around their bones, their boxes shown, in each pose)
		boolean dragonBoxes = System.getenv("IAF_ONLY") != null && !IafTestOnly.skip("dragonbox");
		// (only when asked by name: the three dragons breathing, from their mouths as drawn)
		boolean breath = !IafTestOnly.skip("breath") && !(System.getProperty("iceandfire_fabric.test.only", "") + System.getenv().getOrDefault("IAF_ONLY", "")).isBlank();
		if (!riders && !vfx && !boss && !magic && !hitbox && !frostSize && !dragonBoxes && !breath) {
			return;
		}
		try (TestSingleplayerContext world = context.worldBuilder().adjustSettings(settings ->
			settings.setGameMode(WorldCreationUiState.SelectedGameMode.CREATIVE)).create()) {
			world.getServer().runCommand("gamerule doDaylightCycle false");
			world.getServer().runCommand("gamerule doMobSpawning false");
			world.getServer().runCommand("time set noon");
			world.getServer().runCommand("weather clear");
			context.runOnClient(mc -> {
				if (!mc.options.hideGui) {
					mc.options.hideGui = !mc.options.hideGui;
				}
			});
			if (riders) {
				riders(context, world);
			}
			if (vfx) {
				effects(context, world);
			}
			if (boss) {
				boss(context, world);
			}
			if (magic) {
				magic(context, world);
			}
			if (hitbox) {
				hitboxes(context, world);
			}
			if (frostSize) {
				frostSize(context, world);
			}
			if (dragonBoxes) {
				dragonBoxes(context, world);
			}
			if (breath) {
				breath(context, world);
			}
		}
	}

	/**
	 * Black Frost (as in her realm and by her throne) against rulers on the ground (a block a mark: red each fifth,
	 * black each tenth) and columns of them beside it: standing, walking, asleep and sitting, from above and its side.
	 */
	private static void frostSize(ClientGameTestContext context, TestSingleplayerContext world) {
		world.getServer().runCommand("time set midnight");
		world.getServer().runCommand("effect give @p night_vision 3000 0 true");
		world.getServer().runOnServer(server -> {
			var level = server.overworld();
			for (int i = -45; i <= 45; i++) {
				var mark = IafClick.wool(i % 10 == 0 ? net.minecraft.world.item.DyeColor.BLACK
					: i % 5 == 0 ? net.minecraft.world.item.DyeColor.RED : (i & 1) == 0 ? net.minecraft.world.item.DyeColor.WHITE
					: net.minecraft.world.item.DyeColor.LIGHT_GRAY).defaultBlockState();
				level.setBlock(new BlockPos(i, GROUND - 1, 0), mark, 3);
				level.setBlock(new BlockPos(0, GROUND - 1, i), mark, 3);
				if (i >= 0 && i <= 30) {
					// beside it, as far from the side camera as it is: its height
					level.setBlock(new BlockPos(0, GROUND + i, -40), mark, 3);
					level.setBlock(new BlockPos(0, GROUND + i, 30), mark, 3);
				}
			}
		});
		int[] which = {0};
		for (String pose : new String[]{"sit"}) {
			world.getServer().runOnServer(server -> {
				var level = server.overworld();
				for (var old : level.getEntitiesOfClass(EntityDragonBase.class, new net.minecraft.world.phys.AABB(0, GROUND, 0, 1, GROUND + 1, 1).inflate(80))) {
					old.discard();
				}
				EntityDragonBase frost = pose.equals("stand") || pose.equals("walk") || pose.equals("sit")
					? com.iafabric.entity.EntityBlackFrostGuard.make(level, level.getRandom(), new BlockPos(0, GROUND, 0), 0F)
					: com.iafabric.world.structure.DreadPieces.dreadDragon(level, level.getRandom(), new BlockPos(0, GROUND, 0), 125, true);
				frost.moveTo(0.5, GROUND, 0.5, 0F, 0F);
				frost.setYRot(0F);
				frost.yBodyRot = 0F;
				frost.yHeadRot = 0F;
				if (pose.equals("sleep")) {
					frost.setNoAi(true);
					frost.setInSittingPose(true);
				}

				level.addFreshEntity(frost);
			});
			if (pose.equals("walk")) {
				// along x through the middle (an arc so wide it is straight there)
				var prowl = new com.iafabric.boss.Prowl(20, 160);
				for (int i = 0; i < 60; i++) {
					world.getServer().runOnServer(server -> {
						for (var frost : server.overworld().getEntitiesOfClass(com.iafabric.entity.EntityBlackFrostGuard.class, new net.minecraft.world.phys.AABB(0, GROUND, 0, 1, GROUND + 1, 1).inflate(60))) {
							prowl.step(frost, new net.minecraft.world.phys.Vec3(0.5, GROUND, -400.5), 180F, 401, GROUND, null);
						}
					});
					context.waitTick();
				}
			} else {
				context.waitTicks(60);
			}
			world.getServer().runOnServer(server -> {
				for (var frost : server.overworld().getEntitiesOfClass(EntityDragonBase.class, new net.minecraft.world.phys.AABB(0, GROUND, 0, 1, GROUND + 1, 1).inflate(80))) {
					log("Black Frost " + pose + ": at " + Math.round(frost.getX() * 10) / 10.0 + " " + Math.round(frost.getZ() * 10) / 10.0 + ", asleep " + frost.isSleeping()
						+ ", sitting " + frost.isOrderedToSit() + ", sleep drawn " + frost.sleepProgress + ", sit drawn " + frost.sitProgress);
				}
			});
			camera(context, world, 0.5, GROUND + 75, 0.5, 0, 90, 20);
			context.takeScreenshot("frost_size_top_" + pose);
			camera(context, world, 70.5, GROUND + 9, 0.5, 90, 0, 10);
			context.takeScreenshot("frost_size_side_" + pose);
			which[0]++;
		}
	}

	/** Each part's box of these dragons outlined in white dust (its edges, a dot every so far) on the player's game. */
	private static void outline(ClientGameTestContext context, int[] ids, double step) {
		context.runOnClient(mc -> {
			var dust = new net.minecraft.core.particles.DustParticleOptions(new org.joml.Vector3f(1F, 1F, 1F), 0.6F);
			for (int id : ids) {
				if (!(mc.level.getEntity(id) instanceof EntityDragonBase dragon)) {
					continue;
				}
				for (var part : mc.level.getEntitiesOfClass(com.iafabric.entity.EntityDragonPart.class, dragon.getBoundingBox().inflate(60),
					p -> dragon.getUUID().equals(p.getParentId()) && p.part() >= 0)) {
					var b = part.getBoundingBox();
					double[] xs = {b.minX, b.maxX}, ys = {b.minY, b.maxY}, zs = {b.minZ, b.maxZ};
					for (double y : ys) {
						for (double z : zs) {
							for (double x = b.minX; x <= b.maxX; x += step) {
								mc.level.addAlwaysVisibleParticle(dust, true, x, y, z, 0, 0, 0);
							}
						}
					}
					for (double x : xs) {
						for (double z : zs) {
							for (double y = b.minY; y <= b.maxY; y += step) {
								mc.level.addAlwaysVisibleParticle(dust, true, x, y, z, 0, 0, 0);
							}
						}
					}
					for (double x : xs) {
						for (double y : ys) {
							for (double z = b.minZ; z <= b.maxZ; z += step) {
								mc.level.addAlwaysVisibleParticle(dust, true, x, y, z, 0, 0, 0);
							}
						}
					}
				}
			}
		});
	}

	/**
	 * The three dragons' parts with their boxes shown (F3+B), grown ones: standing, sitting, hovering and flying as their
	 * wings beat, all three from their side, then each from its side and from ahead. Their parts hold to their bones as
	 * they are drawn (DragonBones), on the ground and in the air.
	 */
	private static void dragonBoxes(ClientGameTestContext context, TestSingleplayerContext world) {
		String[] kinds = {"fire", "ice", "lightning"};
		int[] ids = new int[3];
		int[] distances = new int[2];
		context.runOnClient(mc -> {
			distances[0] = mc.options.renderDistance().get();
			distances[1] = mc.options.simulationDistance().get();
			mc.options.renderDistance().set(10);
			mc.options.simulationDistance().set(10);
		});
		world.getServer().runOnServer(server -> {
			var level = server.overworld();
			var types = new net.minecraft.world.entity.EntityType<?>[]{IafEntityRegistry.FIRE_DRAGON, IafEntityRegistry.ICE_DRAGON, IafEntityRegistry.LIGHTNING_DRAGON};
			for (int i = 0; i < 3; i++) {
				EntityDragonBase dragon = (EntityDragonBase) types[i].create(level);
				dragon.moveTo(30.5, GROUND, i * 60 + 0.5, 90, 0);
				level.addFreshEntity(dragon);
				dragon.setAgeInDays(0);
				dragon.growDragon(85);
				dragon.setAgingDisabled(true);
				dragon.setNoAi(true);
				dragon.setHealth(dragon.getMaxHealth());
				dragon.setYRot(90);
				dragon.yBodyRot = 90;
				dragon.yHeadRot = 90;
				ids[i] = dragon.getId();
			}
		});
		// (the boxes are drawn with the hud shown, as F3+B shows them)
		context.runOnClient(mc -> {
			mc.options.hideGui = false;
			mc.getEntityRenderDispatcher().setRenderHitBoxes(true);
		});
		String[] poses = {"standing", "sitting", "hovering", "flying"};
		// (each in its pose the whole while, every tick before the dragons' own: they would land or take off on their own)
		heldIds = ids;
		if (!holding) {
			holding = true;
			net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents.START_SERVER_TICK.register(server -> {
				String pose = heldPose;
				if (pose == null) {
					return;
				}
				for (int id : heldIds) {
					if (server.overworld().getEntity(id) instanceof EntityDragonBase dragon) {
						boolean air = pose.equals("hovering") || pose.equals("flying");
						dragon.setCommand(pose.equals("sitting") ? 1 : 0);
						dragon.setNoGravity(air);
						if (air && dragon.getY() < GROUND + 10) {
							dragon.setPos(dragon.getX(), GROUND + 10, dragon.getZ());
						}
						dragon.setHovering(pose.equals("hovering"));
						dragon.setFlying(pose.equals("flying"));
						// (it does not want to land, nor to go from hovering to flying: it has only just taken off)
						dragon.flyTicks = 0;
						dragon.hoverTicks = 0;
						dragon.setDeltaMovement(net.minecraft.world.phys.Vec3.ZERO);
					}
				}
			});
		}
		for (String pose : poses) {
			heldPose = pose;
			context.waitTicks(50);
			double up = pose.equals("hovering") || pose.equals("flying") ? 10 : 0;
			camera(context, world, -24, GROUND + 34 + up, -46, -38, 24, 2);
			context.waitTicks(10);
			context.takeScreenshot("dragonbox_" + pose + "_all");
			for (int i = 0; i < 3; i++) {
				double z = i * 60 + 0.5;
				int[] one = {ids[i]};
				// from its right side (in the air from farther: its wings spread toward the camera)
				double away = up > 0 ? 34 : 24;
				camera(context, world, 30.5, GROUND + 8 + up, z - away, 0, up > 0 ? 6 : 12, 2);
				context.waitTicks(4);
				outline(context, one, 0.3);
				context.waitTicks(1);
				context.takeScreenshot("dragonbox_" + pose + "_" + kinds[i] + "_side");
				camera(context, world, up > 0 ? 0.5 : 6.5, GROUND + 6 + up, z, -90, 10, 2);
				context.waitTicks(4);
				outline(context, one, 0.3);
				context.waitTicks(1);
				context.takeScreenshot("dragonbox_" + pose + "_" + kinds[i] + "_front");
				// what the player's game has (the camera beside it: one far off has no parts sent there): its parts, around its
				// bones as it is drawn there
				String kind = kinds[i];
				context.runOnClient(mc -> {
					if (!(mc.level.getEntity(one[0]) instanceof EntityDragonBase dragon)) {
						throw new AssertionError("the " + kind + " dragon is not on the player's game " + pose);
					}
					var parts = mc.level.getEntitiesOfClass(com.iafabric.entity.EntityDragonPart.class, dragon.getBoundingBox().inflate(60),
						p -> dragon.getUUID().equals(p.getParentId()) && p.part() >= 0);
					var snout = com.iafabric.entity.DragonBones.middle(dragon, "HeadFront");
					var head = parts.stream().filter(p -> p.part() == com.iafabric.entity.DragonBones.HEAD).findFirst();
					boolean held = snout != null && head.isPresent()
						&& head.get().getBoundingBox().inflate(0.05).contains(dragon.position().add(snout.x(), snout.y(), snout.z()));
					log("dragon boxes " + kind + " " + pose + ": " + parts.size() + " parts on the player's game, its snout in its head's part " + held
						+ " (sitting " + dragon.sitProgress + ", hovering " + dragon.hoverProgress + ", flying " + dragon.flyProgress + ")");
					if (parts.size() != com.iafabric.entity.DragonBones.PARTS || !held) {
						throw new AssertionError("the " + kind + " dragon's parts are not around it on the player's game " + pose);
					}
				});
				if (up > 0) {
					// from above: its wings spread
					camera(context, world, 30.5, GROUND + up + 34, z, 90, 90, 2);
					context.waitTicks(4);
					outline(context, one, 0.4);
					context.waitTicks(1);
					context.takeScreenshot("dragonbox_" + pose + "_" + kinds[i] + "_top");
				}
			}
		}
		context.runOnClient(mc -> {
			mc.getEntityRenderDispatcher().setRenderHitBoxes(false);
			mc.options.hideGui = true;
			mc.options.renderDistance().set(distances[0]);
			mc.options.simulationDistance().set(distances[1]);
		});
		heldPose = null;
		world.getServer().runOnServer(server -> {
			for (int id : ids) {
				var dragon = server.overworld().getEntity(id);
				if (dragon != null) {
					dragon.discard();
				}
			}
		});
	}

	/** The pose the dragons of the parts' scene are held in, and which (each server tick: see dragonBoxes). */
	private static volatile String heldPose;
	private static volatile int[] heldIds = new int[0];
	private static boolean holding;

	/**
	 * A big dragon's body as the player's game sees it (hit boxes shown): its head, neck, wings and tail are there to
	 * be struck, where the dragon is drawn.
	 */
	private static void hitboxes(ClientGameTestContext context, TestSingleplayerContext world) {
		java.util.UUID[] made = new java.util.UUID[1];
		world.getServer().runOnServer(server -> {
			var level = server.overworld();
			EntityDragonBase dragon = IafEntityRegistry.ICE_DRAGON.create(level);
			dragon.moveTo(0.5, GROUND, 30.5, 0F, 0F);
			dragon.setAgeInDays(0);
			dragon.growDragon(100);
			dragon.setNoAi(true);
			level.addFreshEntity(dragon);
			made[0] = dragon.getUUID();
		});
		camera(context, world, 14.5, GROUND + 9, 18.5, 45, 28, 80);
		int[] seen = new int[2];
		context.runOnClient(mc -> {
			mc.getEntityRenderDispatcher().setRenderHitBoxes(true);
			var box = new net.minecraft.world.phys.AABB(0, GROUND, 30, 1, GROUND + 1, 31).inflate(40);
			seen[0] = mc.level.getEntitiesOfClass(com.iafabric.entity.EntityDragonPart.class, box).size();
			seen[1] = mc.level.getEntitiesOfClass(EntityDragonBase.class, box).size();
			for (var part : mc.level.getEntitiesOfClass(com.iafabric.entity.EntityDragonPart.class, box)) {
				log(String.format(java.util.Locale.ROOT, "client part at %.1f %.1f %.1f size %.2f x %.2f", part.getX(), part.getY(), part.getZ(),
					part.getBbWidth(), part.getBbHeight()));
			}
		});
		int[] server = new int[1];
		world.getServer().runOnServer(s -> server[0] = s.overworld().getEntitiesOfClass(com.iafabric.entity.EntityDragonPart.class,
			new net.minecraft.world.phys.AABB(0, GROUND, 30, 1, GROUND + 1, 31).inflate(40)).size());
		log("hitboxes: the client sees " + seen[0] + " body parts of " + seen[1] + " dragon(s); the server has " + server[0]);
		if (seen[0] < 10) {
			throw new AssertionError("the player's game sees " + seen[0] + " body parts of the dragon");
		}
		// (the boxes show from the next frames on, with the hud: hidden, the game draws none)
		boolean hidden = context.computeOnClient(mc -> mc.options.hideGui);
		context.runOnClient(mc -> {
			if (mc.options.hideGui) {
				mc.options.hideGui = !mc.options.hideGui;
			}
		});
		context.waitTicks(5);
		log("hitboxes shown: " + context.computeOnClient(mc -> mc.getEntityRenderDispatcher().shouldRenderHitBoxes())
			+ ", reduced debug info " + context.computeOnClient(mc -> mc.showOnlyReducedInfo()) + ", hud was hidden " + hidden);
		context.takeScreenshot("hitbox_dragon");
		if (hidden) {
			context.runOnClient(mc -> mc.options.hideGui = !mc.options.hideGui);
		}
		// what the game's hitboxes ask of each entity (EntityHitboxDebugRenderer through LevelExtractor.isEntityVisible): is it
		// seen? (the parts' renderer said no to all, so no part ever had its box)
		int seenParts = context.computeOnClient(mc -> {
			var camera = mc.player.getEyePosition();
			var frustum = new net.minecraft.client.renderer.culling.Frustum(new org.joml.Matrix4f(), new org.joml.Matrix4f().ortho(-120, 120, -120, 120, -120, 120));
			frustum.prepare(camera.x, camera.y, camera.z);
			int count = 0;
			for (var part : mc.level.getEntitiesOfClass(com.iafabric.entity.EntityDragonPart.class, new net.minecraft.world.phys.AABB(0, GROUND, 30, 1, GROUND + 1, 31).inflate(40))) {
				if (mc.getEntityRenderDispatcher().shouldRender(part, frustum, camera.x, camera.y, camera.z)) {
					count++;
				}
			}
			return count;
		});
		log("hitboxes: " + seenParts + " of the dragon's parts are seen by the game (their boxes show with F3+B)");
		if (seenParts < 10) {
			throw new AssertionError("only " + seenParts + " of the dragon's parts are seen: their boxes do not show");
		}
		// a blow on a part from the player's own game reaches the dragon (it was dropped there: no body to send it to)
		var around = new net.minecraft.world.phys.AABB(0, GROUND, 30, 1, GROUND + 1, 31).inflate(40);
		float[] life = new float[2];
		world.getServer().runOnServer(s -> {
			var player = s.getPlayerList().getPlayers().get(0);
			player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.DIAMOND_SWORD));
			life[0] = ((EntityDragonBase) s.overworld().getEntity(made[0])).getHealth();
		});
		context.waitTicks(30);
		String[] struck = new String[1];
		int[] partId = {-1};
		context.runOnClient(mc -> {
			var dragon = com.iafabric.compat.Entities.byUuid(mc.level, made[0]);
			// the part of its own farthest from its body: its head or the tip of its tail
			var part = mc.level.getEntitiesOfClass(com.iafabric.entity.EntityDragonPart.class, around, p -> made[0].equals(p.getParentId())).stream()
				.max(java.util.Comparator.comparingDouble(p -> p.distanceToSqr(dragon))).orElseThrow();
			struck[0] = String.format(java.util.Locale.ROOT, "%.2f x %.2f, %.1f blocks from its middle", part.getBbWidth(), part.getBbHeight(), part.distanceTo(dragon));
			partId[0] = part.getId();
		});
		// (the player right over it: the server takes a blow only within reach)
		world.getServer().runOnServer(s -> {
			var part = s.overworld().getEntity(partId[0]);
			var player = s.getPlayerList().getPlayers().get(0);
			player.teleportTo(s.overworld(), part.getX(), part.getBoundingBox().maxY + 0.3, part.getZ(), java.util.Set.of(), 0, 90);
		});
		context.waitTicks(5);
		context.runOnClient(mc -> mc.gameMode.attack(mc.player, mc.level.getEntity(partId[0])));
		context.waitTicks(10);
		world.getServer().runOnServer(s -> life[1] = ((EntityDragonBase) s.overworld().getEntity(made[0])).getHealth());
		log(String.format(java.util.Locale.ROOT, "hitboxes: a blow from the player's game on a part (%s): its life %.1f -> %.1f", struck[0], life[0], life[1]));
		if (life[1] >= life[0]) {
			throw new AssertionError("a blow on a part from the player's game did not reach the dragon");
		}
		// in the air its wings spread three and a half times its size out: its parts hold the tips of its fingers there
		for (int i = 0; i < 50; i++) {
			world.getServer().runOnServer(s -> {
				var dragon = (EntityDragonBase) s.overworld().getEntity(made[0]);
				dragon.setNoGravity(true);
				dragon.setHovering(true);
			});
			context.waitTicks(1);
		}
		double[] spread = new double[3];
		world.getServer().runOnServer(s -> {
			var dragon = (EntityDragonBase) s.overworld().getEntity(made[0]);
			double k = dragon.getRenderSize() / 3;
			var parts = s.overworld().getEntitiesOfClass(com.iafabric.entity.EntityDragonPart.class, around, p -> dragon.getUUID().equals(p.getParentId()));
			double far = 0;
			for (var part : parts) {
				far = Math.max(far, Math.max(Math.abs(part.getBoundingBox().minX - dragon.getX()), Math.abs(part.getBoundingBox().maxX - dragon.getX())));
				far = Math.max(far, Math.max(Math.abs(part.getBoundingBox().minZ - dragon.getZ()), Math.abs(part.getBoundingBox().maxZ - dragon.getZ())));
			}
			spread[0] = far / k;
			spread[1] = dragon.hoverProgress;
			// the tips of its fingers (the ice dragon's: fingerR1.2 and so on), each in one of its parts
			for (String finger : new String[]{"fingerR1.2", "fingerR2.2", "fingerR3.2", "fingerR4.2", "fingerL1.2", "fingerL2.2", "fingerL3.2", "fingerL4.2"}) {
				var tip = com.iafabric.entity.DragonBones.point(dragon, finger, 0, 14.5F, 0);
				if (tip != null) {
					var at = dragon.position().add(tip.x(), tip.y(), tip.z());
					if (parts.stream().noneMatch(p -> p.getBoundingBox().inflate(0.02).contains(at))) {
						spread[2]++;
					}
				}
			}
		});
		log(String.format(java.util.Locale.ROOT, "hitboxes in the air: its parts reach %.2f of its size out, hovering %.0f/20; finger tips out of its parts: %.0f",
			spread[0], spread[1], spread[2]));
		if (spread[2] > 0 || spread[0] < 2.5) {
			throw new AssertionError("in the air the dragon's parts do not hold its wings: " + spread[2] + " finger tips out, reach " + spread[0]);
		}
		context.takeScreenshot("hitbox_dragon_air");
		context.runOnClient(mc -> mc.getEntityRenderDispatcher().setRenderHitBoxes(false));
	}

	/** Spells of each pixie color at a row of zombies, and the spell beside the hotbar with its wait. */
	private static void magic(ClientGameTestContext context, TestSingleplayerContext world) {
		world.getServer().runCommand("gamemode survival @p");
		world.getServer().runCommand("effect give @p resistance 3000 4 true");
		camera(context, world, 0.5, GROUND, -80.5, 0, 10, 10);
		String[][] casts = {{"0", "PILLAR", "magic_pink_pillar"}, {"1", "GRAVITY", "magic_purple_gravity"}, {"1", "STARFALL", "magic_purple_starfall"},
			{"2", "SPEAR", "magic_blue_spear"}, {"3", "ROOTS", "magic_green_roots"}, {"4", "STORM", "magic_yellow_storm"},
			{"2", "BLIZZARD", "magic_blue_blizzard"}};
		for (String[] cast : casts) {
			int color = Integer.parseInt(cast[0]);
			world.getServer().runOnServer(server -> {
				var level = server.overworld();
				var player = server.getPlayerList().getPlayers().get(0);
				var bonds = com.iafabric.pixie.PixieBonding.get(player);
				if (!bonds.get(color).bonded) {
					var pixie = IafEntityRegistry.PIXIE.create(level);
					pixie.moveTo(player.getX(), player.getY() + 1, player.getZ(), 0, 0);
					pixie.setColor(color);
					level.addFreshEntity(pixie);
					pixie.tame(player);
					com.iafabric.pixie.PixieBonding.bond(player, color);
					com.iafabric.pixie.PixieBonding.addXp(player, color, 16000);
				}
				var bond = bonds.get(color);
				for (var spell : com.iafabric.pixie.PixieAbility.spellsOf(color)) {
					bond.known |= spell.bit();
				}
				if (bonds.active != color) {
					com.iafabric.pixie.PixieBonding.summon(player, color);
				}
				// zombies in a row before the player, still
				for (var zombie : level.getEntitiesOfClass(net.minecraft.world.entity.monster.Zombie.class, player.getBoundingBox().inflate(40))) {
					zombie.discard();
				}
				for (int i = -2; i <= 2; i++) {
					var zombie = net.minecraft.world.entity.EntityType.ZOMBIE.create(level);
					zombie.moveTo(player.getX() + i * 2.5, player.getY(), player.getZ() + 9 + Math.abs(i), 180, 0);
					zombie.setNoAi(true);
					level.addFreshEntity(zombie);
				}
				com.iafabric.pixie.PixieBonding.selectSpell(player, color, com.iafabric.pixie.PixieAbility.valueOf(cast[1]).ordinal());
				player.setLastHurtMob(level.getEntitiesOfClass(net.minecraft.world.entity.monster.Zombie.class, player.getBoundingBox().inflate(12)).get(0));
				player.setXRot(20F);
				player.setYRot(0F);
				bond.ready[com.iafabric.pixie.PixieAbility.valueOf(cast[1]).ordinal()] = 0;
				com.iafabric.pixie.PixieMagic.cast(player);
				// the pixie goes to rest (it would fly before the camera, and its dust with it); the spell goes on
				com.iafabric.pixie.PixieBonding.rest(player, false);
			});
			context.waitTicks(cast[1].equals("SPEAR") ? 14 : 20);
			camera(context, world, 12.5, GROUND + 12, -98.5, 23, 22, 3);
			context.takeScreenshot(cast[2]);
			camera(context, world, 0.5, GROUND, -80.5, 0, 10, 60);
		}
		// the spell beside the hotbar, waiting (the yellow pixie out again, its storm just cast)
		world.getServer().runOnServer(server -> {
			var player = server.getPlayerList().getPlayers().get(0);
			com.iafabric.pixie.PixieBonding.summon(player, 4);
			com.iafabric.pixie.PixieBonding.selectSpell(player, 4, com.iafabric.pixie.PixieAbility.STORM.ordinal());
			var bond = com.iafabric.pixie.PixieBonding.get(player).get(4);
			bond.ready[com.iafabric.pixie.PixieAbility.STORM.ordinal()] = server.overworld().getGameTime() + 500;
			com.iafabric.pixie.PixieBonding.sync(player);
		});
		camera(context, world, 0.5, GROUND, -110.5, 0, 10, 20);
		context.runOnClient(mc -> {
			if (mc.options.hideGui) {
				mc.options.hideGui = !mc.options.hideGui;
			}
		});
		context.waitTicks(5);
		context.takeScreenshot("magic_hud");
		context.runOnClient(mc -> {
			if (!mc.options.hideGui) {
				mc.options.hideGui = !mc.options.hideGui;
			}
		});
		world.getServer().runCommand("gamemode creative @p");
	}

	/**
	 * Cryona's fight: she sleeps on a throne with Black Frost; a player comes near, she wakes and takes the player
	 * into her realm. On Black Frost (one bar): each of their powers in turn. Black Frost falls: she mourns it, its body
	 * goes to her as mist, she rises for vengeance; alone, her blows; at her end her weapons go to the player and she
	 * fades; the way home.
	 */
	private static void boss(ClientGameTestContext context, TestSingleplayerContext world) {
		world.getServer().runCommand("gamemode survival @p");
		world.getServer().runCommand("effect give @p resistance 3000 4 true");
		world.getServer().runCommand("effect give @p regeneration 3000 2 true");
		world.getServer().runCommand("effect give @p saturation 3000 1 true");
		// their blows take a share of life through any potion and the longest kill: the scene's player is spared them (still their foe)
		world.getServer().runOnServer(server -> server.getPlayerList().getPlayers().get(0).setInvulnerable(true));
		camera(context, world, 0, GROUND, 120, 180, 0, 20);
		world.getServer().runOnServer(server -> {
			var level = server.overworld();
			// her sign: she asleep on her throne, Black Frost sitting behind it as in her citadel (the two themselves wait in her realm)
			var frost = com.iafabric.entity.EntityBlackFrostGuard.make(level, level.getRandom(), new BlockPos(0, GROUND, 158 + com.iafabric.world.structure.CitadelPiece.GUARD_BEHIND), 180F);
			level.addFreshEntity(frost);
			var queen = com.iafabric.entity.EntityCryonaAsleep.make(level, new BlockPos(0, GROUND, 158), new BlockPos(0, GROUND, 158 + com.iafabric.world.structure.CitadelPiece.GUARD_BEHIND),
				0.5, GROUND, 158.5, 180F, MobSpawnType.COMMAND);
			level.addFreshEntity(queen);
			log("realm there: " + (server.getLevel(com.iafabric.boss.ThroneFight.REALM) != null));
		});
		context.waitTicks(20);
		// her sign, from beyond her call: she asleep on her throne, Black Frost sitting upright behind her, its head over her
		camera(context, world, 0.5, GROUND, 130.5, 0, -4, 40);
		context.takeScreenshot("boss_sign_guard");
		double[] sits = new double[3];
		world.getServer().runOnServer(server -> {
			for (var frost : server.overworld().getEntitiesOfClass(com.iafabric.entity.EntityBlackFrostGuard.class, new net.minecraft.world.phys.AABB(0, GROUND, 158, 1, GROUND + 1, 159).inflate(48))) {
				sits[0] = frost.getZ() - 158.5;
				sits[1] = frost.sitProgress;
				sits[2] = net.minecraft.util.Mth.wrapDegrees(frost.yBodyRot - 180F);
			}
		});
		log("Black Frost by her throne: " + Math.round(sits[0]) + " behind her, sitting drawn " + sits[1] + " of 20, turned " + Math.round(sits[2]) + " from her way");
		if (sits[0] < com.iafabric.world.structure.CitadelPiece.GUARD_BEHIND - 1 || sits[1] < 15 || Math.abs(sits[2]) > 5) {
			throw new AssertionError("Black Frost does not sit behind her throne: " + sits[0] + " behind, sitting " + sits[1] + ", turned " + sits[2]);
		}
		camera(context, world, 0.5, GROUND, 152, 0, 5, 0);
		context.takeScreenshot("boss_throne_wakes");
		context.waitTicks(140);
		BlockPos[] arena = {null};
		world.getServer().runOnServer(server -> {
			var player = server.getPlayerList().getPlayers().get(0);
			arena[0] = com.iafabric.boss.ThroneFight.arenaOf(player);
			log("player in " + player.level().dimension().location() + ", arena " + arena[0] + ", fight state " + com.iafabric.boss.ThroneFight.stateOf(player));
		});
		if (arena[0] == null) {
			throw new AssertionError("the player was not taken into Cryona's realm");
		}
		BlockPos c = arena[0];
		context.takeScreenshot("boss_arrival");
		// nothing happens as they come in: the two only speak, untouchable; a path of light leads to the altar
		context.runOnClient(mc -> {
			if (mc.options.hideGui) {
				mc.options.hideGui = !mc.options.hideGui;
			}
		});
		camera(context, world, c.getX() + 0.5, c.getY() + 1, c.getZ() + 30.5, 180, 6, 160);
		context.takeScreenshot("boss_path");
		context.runOnClient(mc -> {
			if (!mc.options.hideGui) {
				mc.options.hideGui = !mc.options.hideGui;
			}
		});
		expectDuel(world, c, 0, "the duel began by itself before the altar");
		// the fragment of Cryona before the altar, Black Frost prowling behind her (nothing between them and her)
		camera(context, world, c.getX() + 0.5, c.getY() + 1, c.getZ() + 24.5, 180, 4, 60);
		context.takeScreenshot("boss_fragment");
		context.waitTicks(100);
		context.takeScreenshot("boss_fragment_prowl");
		String[] room = {""};
		world.getServer().runOnServer(server -> {
			var realm = server.getLevel(com.iafabric.boss.ThroneFight.REALM);
			for (var queen : realm.getEntitiesOfClass(com.iafabric.entity.EntityDreadQueen.class, new net.minecraft.world.phys.AABB(c).inflate(140, 80, 140))) {
				room[0] += "Cryona at z " + Math.round(queen.getZ() - c.getZ()) + (queen.isPassenger() ? " riding" : " standing");
			}
			for (var frost : realm.getEntitiesOfClass(EntityDragonBase.class, new net.minecraft.world.phys.AABB(c).inflate(140, 80, 140), EntityDragonBase::isBlackFrost)) {
				room[0] += ", Black Frost at z " + Math.round(frost.getZ() - c.getZ());
			}
		});
		log("the throne room before the duel: " + room[0]);
		if (!room[0].contains("standing") || !room[0].contains("Black Frost")) {
			throw new AssertionError("the fragment is not standing before them with Black Frost behind: " + room[0]);
		}
		leaveAndComeBack(context, world, c);
		// spoken to, the fragment shows what she asks (the altar's card)
		camera(context, world, c.getX() + 0.5, c.getY(), c.getZ() + 10.5, 180, 8, 20);
		context.runOnClient(mc -> {
			for (var entity : mc.level.entitiesForRendering()) {
				if (entity instanceof com.iafabric.entity.EntityDreadQueen queen) {
					if (!mc.gameMode.interactAt(mc.player, queen, new net.minecraft.world.phys.EntityHitResult(queen), net.minecraft.world.InteractionHand.MAIN_HAND).consumesAction()) {
						mc.gameMode.interact(mc.player, queen, net.minecraft.world.InteractionHand.MAIN_HAND);
					}
					break;
				}
			}
		});
		context.waitTicks(10);
		boolean spoken = context.computeOnClient(mc -> mc.screen instanceof com.iafabric.client.gui.GuiBossCard);
		log("the fragment spoken to: her card is open " + spoken);
		if (!spoken) {
			throw new AssertionError("spoken to, the fragment did not show what she asks");
		}
		context.runOnClient(mc -> mc.setScreen(null));
		context.waitTicks(5);
		realmWard(world);
		// walked up to, the altar shows its card: the two bosses facing the player, their life, their powers, the
		// rules, what it asks
		camera(context, world, c.getX() + 0.5, c.getY(), c.getZ() + 19.5, 180, 25, 20);
		boolean card = context.computeOnClient(mc -> mc.screen instanceof com.iafabric.client.gui.GuiBossCard);
		log("the altar's card is open: " + card);
		if (!card) {
			throw new AssertionError("walking up to the altar did not open its card");
		}
		context.takeScreenshot("boss_card_frost");
		// the mouse on a power: told in full in the card's strip (nothing floats over the card)
		double[] point = context.computeOnClient(mc -> {
			double[] gui = ((com.iafabric.client.gui.GuiBossCard) mc.screen).screenOf(144 + 50, 92 + 8);
			double scale = mc.getWindow().getGuiScale();
			return new double[]{gui[0] * scale, gui[1] * scale};
		});
		context.getInput().setCursorPos(point[0], point[1]);
		context.waitTicks(2);
		context.takeScreenshot("boss_card_info");
		// the same in Spanish (it must read well in both)
		context.runOnClient(mc -> {
			mc.getLanguageManager().setSelected("es_es");
			mc.options.languageCode = "es_es";
			mc.getLanguageManager().onResourceManagerReload(mc.getResourceManager());
		});
		context.waitTicks(3);
		context.takeScreenshot("boss_card_info_es");
		// and in two of its longer languages (the strip makes its letters smaller, all of it there)
		for (String code : java.util.List.of("de_de", "ru_ru")) {
			context.runOnClient(mc -> IafMenuTests.language(mc, code));
			context.waitTicks(3);
			context.takeScreenshot("boss_card_info_" + code.substring(0, 2));
		}
		context.runOnClient(mc -> IafMenuTests.language(mc, "es_es"));
		context.waitTicks(3);
		context.getInput().setCursorPos(2, 2);
		context.runOnClient(mc -> ((com.iafabric.client.gui.GuiBossCard) mc.screen).show(1));
		context.waitTicks(2);
		context.takeScreenshot("boss_card_queen_es");
		context.runOnClient(mc -> {
			((com.iafabric.client.gui.GuiBossCard) mc.screen).show(0);
			mc.getLanguageManager().setSelected("en_us");
			mc.options.languageCode = "en_us";
			mc.getLanguageManager().onResourceManagerReload(mc.getResourceManager());
		});
		context.waitTicks(3);
		context.runOnClient(mc -> ((com.iafabric.client.gui.GuiBossCard) mc.screen).show(1));
		context.waitTicks(2);
		context.takeScreenshot("boss_card_queen");
		context.runOnClient(mc -> mc.setScreen(null));
		// without ice dragon blood it does not begin; with it, it does
		world.getServer().runCommand("execute as @p run iceandfire duel accept");
		context.waitTicks(5);
		expectDuel(world, c, 0, "the duel began without ice dragon blood");
		world.getServer().runCommand("give @p iceandfire_fabric:ice_dragon_blood 2");
		world.getServer().runCommand("execute as @p run iceandfire duel accept");
		context.waitTicks(20);
		expectDuel(world, c, 1, "the duel did not begin with ice dragon blood offered");
		// the arena from high up
		camera(context, world, c.getX() + 0.5, c.getY() + 60, c.getZ() + 72, 180, 42, 30);
		context.takeScreenshot("boss_arena");
		// one boss: Cryona on Black Frost, one bar (the hud shown a moment)
		still(world, c, 16.5, -12.5);
		camera(context, world, c.getX() + 12.5, c.getY() + 7, c.getZ() + 2.5, 105, 14, 10);
		context.runOnClient(mc -> {
			if (mc.options.hideGui) {
				mc.options.hideGui = !mc.options.hideGui;
			}
		});
		context.waitTicks(5);
		context.takeScreenshot("boss_one_bar");
		boolean[] bar = {false};
		world.getServer().runOnServer(server -> {
			var realm = server.getLevel(com.iafabric.boss.ThroneFight.REALM);
			var player = server.getPlayerList().getPlayers().get(0);
			for (var frost : realm.getEntitiesOfClass(EntityDragonBase.class, new net.minecraft.world.phys.AABB(c).inflate(140, 80, 140), EntityDragonBase::isBlackFrost)) {
				bar[0] |= com.iafabric.entity.DreadDragons.barShown(frost, player);
			}
		});
		log("the duel's one bar shown: " + bar[0]);
		if (!bar[0]) {
			throw new AssertionError("no boss bar in the duel (Cryona on Black Frost)");
		}
		context.runOnClient(mc -> {
			if (!mc.options.hideGui) {
				mc.options.hideGui = !mc.options.hideGui;
			}
		});
		// each power in turn, seen from beside the player
		String[][] powers = {{"queen", "spikes", "20", "boss_spike_circles", "22", "boss_spikes_burst"},
			{"queen", "ice", "38", "boss_ice_lifted", "50", "boss_ice_thrown"},
			{"frost", "charge", "22", "boss_charge_lane", "22", "boss_charge"},
			{"frost", "dive", "34", "boss_dive_circle", "22", "boss_dive_impact"},
			{"frost", "hail", "18", "boss_hail", "0", ""},
			{"frost", "prison", "40", "boss_prison_circle", "24", "boss_prison_frozen"},
			{"frost", "breath", "26", "boss_breath_lane", "16", "boss_breath_jet"},
			{"queen", "zero", "50", "boss_absolute_zero", "75", "boss_spent"}};
		for (String[] power : powers) {
			still(world, c, 16.5, -12.5);
			world.getServer().runOnServer(server -> {
				var realm = server.getLevel(com.iafabric.boss.ThroneFight.REALM);
				for (var queen : realm.getEntitiesOfClass(com.iafabric.entity.EntityDreadQueen.class, new net.minecraft.world.phys.AABB(c).inflate(140, 80, 140))) {
					if (power[0].equals("queen")) {
						com.iafabric.boss.CryonaFight.of(queen).cast(realm, power[1]);
					}
				}
				for (var frost : realm.getEntitiesOfClass(EntityDragonBase.class, new net.minecraft.world.phys.AABB(c).inflate(140, 80, 140), EntityDragonBase::isBlackFrost)) {
					if (power[0].equals("frost")) {
						com.iafabric.boss.BlackFrostFight.of(frost).cast(realm, power[1]);
					}
				}
			});
			camera(context, world, c.getX() + 26.5, c.getY() + 20, c.getZ() + 30.5, 140, 32, Integer.parseInt(power[2]));
			context.takeScreenshot(power[3]);
			if (System.getProperty("iceandfire_fabric.test.only", "").contains("burst")) {
				// (asked by name: tick after tick, to see what flickers)
				for (int i = 0; i < 8; i++) {
					context.waitTicks(1);
					context.takeScreenshot(power[3] + "_tick" + i);
				}
			}
			if (!power[5].isEmpty()) {
				context.waitTicks(Integer.parseInt(power[4]));
				context.takeScreenshot(power[5]);
			}
			context.waitTicks(power[1].equals("zero") ? 150 : power[1].equals("prison") ? 90 : 30);
		}
		// from afar: it flies off from who comes close, takes who it reaches in its claws, slows who nears it, spits ice
		frostAtRange(context, world, c);
		// a fall: the duel waits as it is, the player's things lie on the ice (never to go); back at her throne, the same
		// arena, the two as they were, and the altar again
		fallAndComeBack(context, world, c);
		// Black Frost falls: she mourns it, its body goes to her as mist, she rises for vengeance
		still(world, c, 16.5, -12.5);
		world.getServer().runOnServer(server -> {
			var realm = server.getLevel(com.iafabric.boss.ThroneFight.REALM);
			var player = server.getPlayerList().getPlayers().get(0);
			for (var frost : realm.getEntitiesOfClass(EntityDragonBase.class, new net.minecraft.world.phys.AABB(c).inflate(140, 80, 140), EntityDragonBase::isBlackFrost)) {
				frost.setNoAi(false);
				// (no blow takes more than a hundredth of its life a second: an admin's /kill, for the scene)
				frost.hurt(realm.damageSources().genericKill(), 100000F);
			}
		});
		camera(context, world, c.getX() + 17.5, c.getY() + 6, c.getZ() + 6.5, 118, 16, 30);
		context.takeScreenshot("boss_frost_falls");
		context.waitTicks(40);
		context.takeScreenshot("boss_frost_mist");
		context.waitTicks(45);
		// (her mourning lasts the pixie's memories, the cryonafight scene's: hurried here to her rising)
		world.getServer().runOnServer(server -> {
			for (var queen : server.getLevel(com.iafabric.boss.ThroneFight.REALM).getEntitiesOfClass(com.iafabric.entity.EntityDreadQueen.class,
				new net.minecraft.world.phys.AABB(c).inflate(140, 80, 140))) {
				com.iafabric.boss.CryonaFight.of(queen).hurry(30);
			}
		});
		context.waitTicks(31);
		context.takeScreenshot("boss_vengeance");
		context.waitTicks(30);
		// alone: her life and her ice shield (the hud a moment)
		context.runOnClient(mc -> {
			if (mc.options.hideGui) {
				mc.options.hideGui = !mc.options.hideGui;
			}
		});
		camera(context, world, c.getX() + 15.5, c.getY() + 9, c.getZ() + 4.5, 90, 26, 6);
		context.takeScreenshot("boss_alone_shield");
		context.runOnClient(mc -> {
			if (!mc.options.hideGui) {
				mc.options.hideGui = !mc.options.hideGui;
			}
		});
		// alone: her blows (the player before her, she still but for the blow)
		String[][] blows = {{"combo", "8", "boss_combo_lane", "10", "boss_combo_cut"},
			{"leap", "14", "boss_leap_circle", "16", "boss_leap_impact"},
			{"wave", "12", "boss_wave_lanes", "14", "boss_wave_spikes"},
			{"nova", "16", "boss_nova", "16", "boss_nova_burst"},
			{"retreat", "6", "boss_retreat_cut", "10", "boss_retreat_leap"},
			{"beam", "30", "boss_beam_lane", "40", "boss_beam_fire"},
			{"skyfall", "20", "boss_skyfall_circles", "14", "boss_skyfall_ice"}};
		for (String[] blow : blows) {
			world.getServer().runOnServer(server -> {
				var realm = server.getLevel(com.iafabric.boss.ThroneFight.REALM);
				var player = server.getPlayerList().getPlayers().get(0);
				player.teleportTo(c.getX() + 0.5, c.getY(), c.getZ() + (blow[0].equals("nova") || blow[0].equals("combo") || blow[0].equals("retreat") ? 4.5 : 10.5));
				for (var queen : realm.getEntitiesOfClass(com.iafabric.entity.EntityDreadQueen.class, new net.minecraft.world.phys.AABB(c).inflate(140, 80, 140))) {
					queen.teleportTo(c.getX() + 0.5, c.getY(), c.getZ() - 1.5);
					queen.setYRot(0F);
					var fight = com.iafabric.boss.CryonaFight.of(queen);
					fight.rest(200);
					fight.cast(realm, blow[0]);
				}
			});
			camera(context, world, c.getX() + 15.5, c.getY() + 9, c.getZ() + 4.5, 90, 26, Integer.parseInt(blow[1]));
			context.takeScreenshot(blow[2]);
			context.waitTicks(Integer.parseInt(blow[3]));
			context.takeScreenshot(blow[4]);
			context.waitTicks(40);
		}
		// her end: she kneels, her weapons float to the player, she fades
		world.getServer().runOnServer(server -> {
			var realm = server.getLevel(com.iafabric.boss.ThroneFight.REALM);
			var player = server.getPlayerList().getPlayers().get(0);
			player.teleportTo(c.getX() + 0.5, c.getY(), c.getZ() + 6.5);
			for (var queen : realm.getEntitiesOfClass(com.iafabric.entity.EntityDreadQueen.class, new net.minecraft.world.phys.AABB(c).inflate(140, 80, 140))) {
				queen.teleportTo(c.getX() + 0.5, c.getY(), c.getZ() - 1.5);
				// (past her ice shield and her limit: an admin's /kill, for the scene; it leaves her a breath of life for her end)
				queen.hurt(realm.damageSources().genericKill(), 100000F);
			}
		});
		camera(context, world, c.getX() + 2.5, c.getY() + 1.4, c.getZ() + 2.5, 148, 12, 30);
		context.takeScreenshot("boss_end_kneel");
		context.waitTicks(55);
		context.takeScreenshot("boss_end_weapons");
		context.waitTicks(55);
		context.takeScreenshot("boss_end_fade");
		context.waitTicks(80);
		camera(context, world, c.getX() + 0.5, c.getY() + 8, c.getZ() + 20.5, 180, 15, 10);
		context.takeScreenshot("boss_victory");
		world.getServer().runOnServer(server -> {
			var player = server.getPlayerList().getPlayers().get(0);
			player.setInvulnerable(false);
			boolean sword = player.getInventory().contains(new net.minecraft.world.item.ItemStack(com.iafabric.item.IafItemRegistry.DREAD_QUEEN_SWORD));
			player.getAbilities().flying = false;
			player.onUpdateAbilities();
			log("after her end the player " + (sword ? "has" : "has not") + " her sword");
		});
		camera(context, world, c.getX() + 0.5, c.getY(), c.getZ() + 0.5, 180, 0, 30);
		boolean[] home = {false};
		world.getServer().runOnServer(server -> {
			var player = server.getPlayerList().getPlayers().get(0);
			home[0] = player.level().dimension() == net.minecraft.world.level.Level.OVERWORLD;
			log("after the victory the player is in " + player.level().dimension().location() + " at " + player.blockPosition());
		});
		if (!home[0]) {
			throw new AssertionError("the player did not go home after the victory");
		}
		world.getServer().runCommand("gamemode creative @p");
	}

	/** While true, every blow a player takes is logged (the grab's scene: what took how much of their life). */
	private static final boolean[] LOG_BLOWS = {false};

	static {
		net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents.AFTER_DAMAGE.register((entity, source, base, taken, blocked) -> {
			if (LOG_BLOWS[0] && entity instanceof net.minecraft.world.entity.player.Player) {
				log(String.format(java.util.Locale.ROOT, "blow on the player: %s %.2f (life %.2f)", source.getMsgId(), taken, entity.getHealth()));
			}
		});
	}

	/**
	 * Black Frost on foot: it runs at the player and strikes (a circle where each blow lands), then a stampede: a lane
	 * from edge to edge of the arena through the player, and its rush down it.
	 */
	private static void frostOnFoot(ClientGameTestContext context, TestSingleplayerContext world, BlockPos c) {
		// (the player only watches: its bite reaches them at its jaws' length)
		world.getServer().runOnServer(server -> {
			var player = server.getPlayerList().getPlayers().get(0);
			player.setInvulnerable(true);
			player.setHealth(player.getMaxHealth());
		});
		// Cryona on its back, seen from aside: her hips on its back
		still(world, c, 22.5, -3.5);
		int side = sideEye(world, c, 6);
		context.waitTicks(3);
		context.runOnClient(mc -> {
			var stand = mc.level.getEntity(side);
			if (stand != null) {
				mc.setCameraEntity(stand);
			}
		});
		context.waitTicks(10);
		context.takeScreenshot("boss_rider_side");
		context.runOnClient(mc -> mc.setCameraEntity(mc.player));
		world.getServer().runOnServer(server -> {
			if (server.getLevel(com.iafabric.boss.ThroneFight.REALM).getEntity(side) instanceof net.minecraft.world.entity.Entity stand) {
				stand.discard();
			}
		});
		still(world, c, 22.5, -3.5);
		double[] gap = new double[2];
		world.getServer().runOnServer(server -> {
			var frost = frostAt(server, c);
			var player = server.getPlayerList().getPlayers().get(0);
			player.getAbilities().flying = false;
			player.onUpdateAbilities();
			gap[0] = Math.sqrt(frost.position().subtract(player.position()).horizontalDistanceSqr());
			com.iafabric.boss.BlackFrostFight.of(frost).cast(server.getLevel(com.iafabric.boss.ThroneFight.REALM), "melee");
		});
		context.runOnClient(mc -> mc.player.getAbilities().flying = false);
		context.waitTicks(44);
		float[] view = new float[5];
		world.getServer().runOnServer(server -> {
			var frost = frostAt(server, c);
			var player = server.getPlayerList().getPlayers().get(0);
			gap[1] = Math.sqrt(frost.position().subtract(player.position()).horizontalDistanceSqr());
			float[] way = look(player.getEyePosition(), frost.position().add(0, frost.getBbHeight() * 0.6, 0));
			view[0] = (float) player.getX();
			view[1] = (float) player.getY();
			view[2] = (float) player.getZ();
			view[3] = way[0];
			view[4] = way[1];
		});
		log(String.format(java.util.Locale.ROOT, "on foot: Black Frost ran from %.1f to %.1f blocks of the player", gap[0], gap[1]));
		if (gap[1] > gap[0] - 5) {
			throw new AssertionError("Black Frost did not run at the player: " + gap[0] + " -> " + gap[1]);
		}
		context.runOnClient(mc -> {
			mc.player.setYRot(view[3]);
			mc.player.setXRot(view[4]);
		});
		context.takeScreenshot("boss_frost_runs");
		boolean[] struck = {false};
		for (int i = 0; i < 60 && !struck[0]; i++) {
			context.waitTicks(1);
			world.getServer().runOnServer(server -> struck[0] = com.iafabric.boss.BlackFrostFight.of(frostAt(server, c)).striking());
		}
		log("on foot: a blow of its own " + (struck[0] ? "shown" : "not shown"));
		if (!struck[0]) {
			throw new AssertionError("Black Frost on foot never struck");
		}
		context.runOnClient(mc -> mc.options.setCameraType(net.minecraft.client.CameraType.THIRD_PERSON_BACK));
		context.waitTicks(6);
		context.takeScreenshot("boss_frost_strike");
		context.runOnClient(mc -> mc.options.setCameraType(net.minecraft.client.CameraType.FIRST_PERSON));
		// the stampede: from its first perch, a lane through the player to the arena's edge
		still(world, c, 30.5, -3.5);
		world.getServer().runOnServer(server -> com.iafabric.boss.BlackFrostFight.of(frostAt(server, c)).cast(server.getLevel(com.iafabric.boss.ThroneFight.REALM), "stampede"));
		camera(context, world, c.getX() + 22.5, c.getY() + 14, c.getZ() + 44.5, 145, 22, 16);
		String[] rush = new String[1];
		world.getServer().runOnServer(server -> rush[0] = com.iafabric.boss.BlackFrostFight.of(frostAt(server, c)).greatBlow());
		log("stampede: " + rush[0]);
		if (!"charge".equals(rush[0])) {
			throw new AssertionError("no stampede rush began: " + rush[0]);
		}
		context.takeScreenshot("boss_stampede_lane");
		context.waitTicks(30);
		context.takeScreenshot("boss_stampede_rush");
		context.waitTicks(80);
	}

	/** Black Frost of a realm's arena. */
	private static EntityDragonBase frostAt(net.minecraft.server.MinecraftServer server, BlockPos c) {
		var realm = server.getLevel(com.iafabric.boss.ThroneFight.REALM);
		return realm.getEntitiesOfClass(EntityDragonBase.class, new net.minecraft.world.phys.AABB(c).inflate(140, 80, 140), EntityDragonBase::isBlackFrost).get(0);
	}

	/**
	 * An eye beside Black Frost on its first perch, looking at the place before it where a player close to it stands
	 * (an invisible stand; the camera follows it): to see its claws take them, from the side.
	 */
	private static int sideEye(TestSingleplayerContext world, BlockPos c, double reach) {
		int[] id = new int[1];
		world.getServer().runOnServer(server -> {
			var realm = server.getLevel(com.iafabric.boss.ThroneFight.REALM);
			var frost = frostAt(server, c);
			var stand = net.minecraft.world.entity.EntityType.ARMOR_STAND.create(realm);
			net.minecraft.world.phys.Vec3 eye = new net.minecraft.world.phys.Vec3(frost.getX() + 21, c.getY() + 6, frost.getZ() + reach + 4);
			float[] way = look(eye, new net.minecraft.world.phys.Vec3(frost.getX(), c.getY() + 7, frost.getZ() + reach));
			stand.setInvisible(true);
			stand.setNoGravity(true);
			stand.setSilent(true);
			stand.setInvulnerable(true);
			stand.moveTo(eye.x, eye.y - stand.getEyeHeight(), eye.z, way[0], way[1]);
			stand.setYHeadRot(way[0]);
			stand.setYBodyRot(way[0]);
			realm.addFreshEntity(stand);
			id[0] = stand.getId();
		});
		return id[0];
	}

	/** The way (yaw, pitch) from an eye to a place. */
	private static float[] look(net.minecraft.world.phys.Vec3 eye, net.minecraft.world.phys.Vec3 at) {
		var d = at.subtract(eye);
		float yaw = (float) Math.toDegrees(Math.atan2(-d.x, d.z));
		float pitch = (float) -Math.toDegrees(Math.atan2(d.y, Math.sqrt(d.x * d.x + d.z * d.z)));
		return new float[]{yaw, pitch};
	}

	/**
	 * Black Frost fights from afar: who comes near it (close, out of its claws' reach) sees it fly off to a perch far
	 * from them, where it stands whole in sight; who it reaches sees a circle close round their feet and is lifted in its
	 * claws and thrown (no life lost to the height, a share on the ice); the cold round it slows and frosts who nears
	 * it; from its perch it spits shards of ice that fall on circles.
	 */
	private static void frostAtRange(ClientGameTestContext context, TestSingleplayerContext world, BlockPos c) {
		// on its first perch (the middle), the player close to it but out of its claws' reach: it flies away
		still(world, c, 16.5, -3.5);
		double[] reach = new double[1];
		world.getServer().runOnServer(server -> {
			var frost = frostAt(server, c);
			var fight = com.iafabric.boss.BlackFrostFight.of(frost);
			reach[0] = fight.reach();
			var player = server.getPlayerList().getPlayers().get(0);
			player.teleportTo(frost.getX(), c.getY(), frost.getZ() + reach[0] + 2.5);
			fight.ready(false);
			log(String.format(java.util.Locale.ROOT, "Black Frost: %.1f wide, its claws reach %.1f, its cold %.1f", frost.getBbWidth(), reach[0], reach[0] + 8));
		});
		context.waitTicks(4);
		String[] blow = new String[2];
		world.getServer().runOnServer(server -> {
			var fight = com.iafabric.boss.BlackFrostFight.of(frostAt(server, c));
			blow[0] = fight.greatBlow();
			blow[1] = fight.stance();
		});
		log("a player close to it: Black Frost " + blow[0] + ", fighting " + blow[1]);
		if (!"flight".equals(blow[0]) && !"melee".equals(blow[1])) {
			throw new AssertionError("Black Frost neither flew off nor came down to fight a player close to it: " + blow[0] + ", " + blow[1]);
		}
		// its flight to a far perch, sweeping round the arena
		still(world, c, 16.5, -3.5);
		world.getServer().runOnServer(server -> com.iafabric.boss.BlackFrostFight.of(frostAt(server, c)).cast(server.getLevel(com.iafabric.boss.ThroneFight.REALM), "fly"));
		context.waitTicks(30);
		float[] view = new float[5];
		world.getServer().runOnServer(server -> {
			var frost = frostAt(server, c);
			var player = server.getPlayerList().getPlayers().get(0);
			float[] way = look(player.getEyePosition(), frost.position().add(0, frost.getBbHeight() * 0.5, 0));
			view[0] = (float) player.getX();
			view[1] = (float) player.getY();
			view[2] = (float) player.getZ();
			view[3] = way[0];
			view[4] = way[1];
		});
		camera(context, world, view[0], view[1], view[2], view[3], view[4], 1);
		context.takeScreenshot("boss_frost_flight");
		int[] perch = {-1};
		double[] far = new double[1];
		for (int i = 0; i < 8 && perch[0] < 0; i++) {
			context.waitTicks(20);
			world.getServer().runOnServer(server -> {
				var frost = frostAt(server, c);
				var fight = com.iafabric.boss.BlackFrostFight.of(frost);
				perch[0] = fight.greatBlow() == null ? fight.perch() : -1;
				far[0] = Math.sqrt(frost.position().subtract(server.getPlayerList().getPlayers().get(0).position()).horizontalDistanceSqr());
			});
		}
		String[] perched = new String[1];
		world.getServer().runOnServer(server -> perched[0] = com.iafabric.boss.BlackFrostFight.of(frostAt(server, c)).stance());
		log(String.format(java.util.Locale.ROOT, "Black Frost landed on perch %d, %.1f blocks from the player, fighting %s", perch[0], far[0], perched[0]));
		if (perch[0] <= 0 || far[0] < 25 || !"ranged".equals(perched[0])) {
			throw new AssertionError("Black Frost did not take a far perch to fight from: perch " + perch[0] + ", " + far[0] + " blocks away, " + perched[0]);
		}
		// seen from there, whole, turned to the player
		world.getServer().runOnServer(server -> {
			var frost = frostAt(server, c);
			com.iafabric.boss.BlackFrostFight.of(frost).rest(200);
			var player = server.getPlayerList().getPlayers().get(0);
			float[] way = look(player.getEyePosition(), frost.position().add(0, frost.getBbHeight() * 0.4, 0));
			view[3] = way[0];
			view[4] = way[1];
		});
		camera(context, world, view[0], view[1], view[2], view[3], view[4], 30);
		context.takeScreenshot("boss_frost_perched");
		// its head turned to the player; its body drawn as it turns (its rider sits on its back as it is drawn)
		float[] look = new float[2];
		world.getServer().runOnServer(server -> {
			var frost = frostAt(server, c);
			var player = server.getPlayerList().getPlayers().get(0);
			float want = look(frost.position(), player.position())[0];
			look[0] = Math.abs(net.minecraft.util.Mth.wrapDegrees(frost.yHeadRot - want));
		});
		look[1] = context.computeOnClient(mc -> {
			for (var dragon : mc.level.getEntitiesOfClass(EntityDragonBase.class, mc.player.getBoundingBox().inflate(150), EntityDragonBase::isBlackFrost)) {
				return Math.abs(net.minecraft.util.Mth.wrapDegrees(dragon.yBodyRot - dragon.getYRot()));
			}
			return 999F;
		});
		log(String.format(java.util.Locale.ROOT, "Black Frost's head %.1f degrees off the player; its body as drawn %.1f degrees off its turn", look[0], look[1]));
		if (look[0] > 20 || look[1] > 3) {
			throw new AssertionError("Black Frost's head does not follow the player or its body is drawn turned away: " + look[0] + ", " + look[1]);
		}
		frostOnFoot(context, world, c);
		// its claws: the player at its feet, on the ice (no flight), able to fall; their life counted
		still(world, c, 16.5, -3.5);
		float[] life = new float[3];
		double[] at = new double[4];
		world.getServer().runOnServer(server -> {
			var frost = frostAt(server, c);
			var player = server.getPlayerList().getPlayers().get(0);
			player.setInvulnerable(false);
			player.removeAllEffects();
			player.getAbilities().flying = false;
			player.getAbilities().mayfly = false;
			player.onUpdateAbilities();
			player.setHealth(player.getMaxHealth());
			// (hungry enough not to heal by itself while it is measured)
			player.getFoodData().setFoodLevel(12);
			player.getFoodData().setSaturation(0F);
			double z = frost.getZ() + reach[0] - 2.5;
			float[] way = look(new net.minecraft.world.phys.Vec3(frost.getX(), c.getY() + 1.6, z), frost.position().add(0, frost.getBbHeight() * 0.6, 0));
			player.teleportTo(server.getLevel(com.iafabric.boss.ThroneFight.REALM), frost.getX(), c.getY(), z, java.util.Set.of(), way[0], way[1]);
			life[0] = player.getHealth();
			at[0] = player.getY();
			at[3] = player.getY();
			com.iafabric.boss.BlackFrostFight.of(frost).ready(true);
		});
		int eye = sideEye(world, c, reach[0]);
		LOG_BLOWS[0] = true;
		context.runOnClient(mc -> mc.player.getAbilities().flying = false);
		// (the eye is in the player's game a moment later)
		context.waitTicks(3);
		boolean watching = context.computeOnClient(mc -> {
			var stand = mc.level.getEntity(eye);
			if (stand != null) {
				mc.setCameraEntity(stand);
			}
			return stand != null && mc.getCameraEntity() == stand;
		});
		log("the grab seen from beside: " + watching);
		context.waitTicks(11);
		context.takeScreenshot("boss_grab_circle");
		String[] held = new String[1];
		for (int i = 0; i < 40; i++) {
			context.waitTicks(1);
			world.getServer().runOnServer(server -> {
				var player = server.getPlayerList().getPlayers().get(0);
				held[0] = com.iafabric.boss.BlackFrostFight.of(frostAt(server, c)).greatBlow();
				at[3] = Math.max(at[3], player.getY());
			});
			if ("holding".equals(held[0]) && at[3] > at[0] + 9) {
				break;
			}
		}
		context.takeScreenshot("boss_grab_lifted");
		log(String.format(java.util.Locale.ROOT, "its claws: %s, the player lifted %.1f blocks", held[0], at[3] - at[0]));
		// (back behind the player before the throw: a player whose game looks through another's eyes does not move)
		context.runOnClient(mc -> {
			mc.setCameraEntity(mc.player);
			mc.options.setCameraType(net.minecraft.client.CameraType.THIRD_PERSON_BACK);
		});
		for (int i = 0; i < 30 && "holding".equals(held[0]); i++) {
			context.waitTicks(1);
			world.getServer().runOnServer(server -> held[0] = com.iafabric.boss.BlackFrostFight.of(frostAt(server, c)).greatBlow());
		}
		context.waitTicks(5);
		context.takeScreenshot("boss_grab_thrown");
		if (at[3] - at[0] < 8) {
			throw new AssertionError("Black Frost's claws did not lift the player: " + held[0] + ", up " + (at[3] - at[0]));
		}
		// thrown, and down on the ice
		boolean[] landed = {false};
		for (int i = 0; i < 60 && !landed[0]; i++) {
			context.waitTicks(2);
			world.getServer().runOnServer(server -> {
				var player = server.getPlayerList().getPlayers().get(0);
				var frost = frostAt(server, c);
				landed[0] = player.onGround() && player.getY() < at[0] + 3 && !"holding".equals(com.iafabric.boss.BlackFrostFight.of(frost).greatBlow());
				if (landed[0]) {
					log(String.format(java.util.Locale.ROOT, "thrown: down at %.1f %.1f %.1f", player.getX() - c.getX(), player.getY(), player.getZ() - c.getZ()));
				}
				at[1] = Math.sqrt(player.position().subtract(new net.minecraft.world.phys.Vec3(frost.getX(), player.getY(), frost.getZ())).horizontalDistanceSqr());
				life[1] = player.getHealth();
			});
		}
		context.waitTicks(4);
		world.getServer().runOnServer(server -> life[2] = server.getPlayerList().getPlayers().get(0).getHealth());
		LOG_BLOWS[0] = false;
		context.runOnClient(mc -> {
			mc.setCameraEntity(mc.player);
			mc.options.setCameraType(net.minecraft.client.CameraType.FIRST_PERSON);
		});
		world.getServer().runOnServer(server -> {
			if (server.getLevel(com.iafabric.boss.ThroneFight.REALM).getEntity(eye) instanceof net.minecraft.world.entity.Entity stand) {
				stand.discard();
			}
		});
		log(String.format(java.util.Locale.ROOT, "thrown: landed %s, %.1f blocks from it, life %.1f -> %.1f", landed[0], at[1], life[0], life[2]));
		world.getServer().runCommand("effect give @p resistance 3000 4 true");
		world.getServer().runCommand("effect give @p regeneration 3000 2 true");
		world.getServer().runOnServer(server -> {
			var player = server.getPlayerList().getPlayers().get(0);
			player.setInvulnerable(true);
			player.setHealth(player.getMaxHealth());
			player.getFoodData().setFoodLevel(20);
		});
		// (the landing takes a share of their life, the height none: 20 life, 2.4 when caught, 4 on the ice)
		if (!landed[0] || life[2] < life[0] - 8 || life[2] > life[0] - 5) {
			throw new AssertionError("the throw was not as it should be: landed " + landed[0] + ", life " + life[0] + " -> " + life[2]);
		}
		// the cold round it: the player near it (out of its reach) is slowed and their sight frosts
		still(world, c, 16.5, -3.5);
		boolean[] cold = new boolean[2];
		world.getServer().runOnServer(server -> {
			var frost = frostAt(server, c);
			var player = server.getPlayerList().getPlayers().get(0);
			player.teleportTo(frost.getX() + 3, c.getY(), frost.getZ() + reach[0] + 6);
			com.iafabric.boss.BlackFrostFight.of(frost).rest(60);
		});
		context.waitTicks(16);
		world.getServer().runOnServer(server -> {
			var player = server.getPlayerList().getPlayers().get(0);
			cold[0] = player.hasEffect(net.minecraft.world.effect.MobEffects.MOVEMENT_SLOWDOWN);
			cold[1] = player.getTicksFrozen() > 30;
			var frost = frostAt(server, c);
			float[] way = look(player.getEyePosition(), frost.position().add(0, frost.getBbHeight() * 0.6, 0));
			view[0] = (float) player.getX();
			view[1] = (float) player.getY();
			view[2] = (float) player.getZ();
			view[3] = way[0];
			view[4] = way[1];
		});
		log("its cold: the player is slowed " + cold[0] + ", their sight frosts " + cold[1]);
		if (!cold[0] || !cold[1]) {
			throw new AssertionError("the cold round Black Frost did not slow and frost the player");
		}
		context.runOnClient(mc -> {
			if (mc.options.hideGui) {
				mc.options.hideGui = !mc.options.hideGui;
			}
		});
		camera(context, world, view[0], view[1], view[2], view[3], view[4], 2);
		context.takeScreenshot("boss_frost_aura");
		context.runOnClient(mc -> {
			if (!mc.options.hideGui) {
				mc.options.hideGui = !mc.options.hideGui;
			}
		});
		// its volley, from afar (seen from behind the player it falls on)
		still(world, c, 30.5, -3.5);
		camera(context, world, c.getX() + 0.5, c.getY(), c.getZ() + 30.5, 180, 4, 2);
		context.runOnClient(mc -> mc.options.setCameraType(net.minecraft.client.CameraType.THIRD_PERSON_BACK));
		world.getServer().runOnServer(server -> com.iafabric.boss.BlackFrostFight.of(frostAt(server, c)).cast(server.getLevel(com.iafabric.boss.ThroneFight.REALM), "volley"));
		context.waitTicks(14);
		context.takeScreenshot("boss_volley");
		context.waitTicks(10);
		context.takeScreenshot("boss_volley_falls");
		context.runOnClient(mc -> mc.options.setCameraType(net.minecraft.client.CameraType.FIRST_PERSON));
		context.waitTicks(30);
	}

	/** The player south of the middle, Black Frost (Cryona on its back) north of it, both still but for their powers. */
	/** Nothing can be put nor broken in her realm: a block, lava from a bucket, the ice under the player's feet. */
	private static void realmWard(TestSingleplayerContext world) {
		String[] result = new String[1];
		world.getServer().runOnServer(server -> {
			var realm = server.getLevel(com.iafabric.boss.ThroneFight.REALM);
			var player = server.getPlayerList().getPlayers().get(0);
			BlockPos floor = player.blockPosition().below();
			while (realm.getBlockState(floor).isAir() && floor.getY() > realm.getMinBuildHeight()) {
				floor = floor.below();
			}
			var hand = net.minecraft.world.InteractionHand.MAIN_HAND;
			var hit = new net.minecraft.world.phys.BlockHitResult(net.minecraft.world.phys.Vec3.atCenterOf(floor).add(0, 0.5, 0), net.minecraft.core.Direction.UP, floor, false);
			player.setItemInHand(hand, new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.COBBLESTONE, 4));
			player.gameMode.useItemOn(player, realm, player.getMainHandItem(), hand, hit);
			boolean cobble = realm.getBlockState(floor.above()).is(net.minecraft.world.level.block.Blocks.COBBLESTONE);
			player.setItemInHand(hand, new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.LAVA_BUCKET));
			player.setXRot(90F);
			player.gameMode.useItem(player, realm, player.getMainHandItem(), hand);
			int lava = 0;
			for (BlockPos pos : BlockPos.betweenClosed(floor.offset(-3, 0, -3), floor.offset(3, 4, 3))) {
				if (realm.getFluidState(pos).is(net.minecraft.tags.FluidTags.LAVA)) {
					lava++;
				}
			}
			var before = realm.getBlockState(floor);
			player.setItemInHand(hand, net.minecraft.world.item.ItemStack.EMPTY);
			boolean broken = player.gameMode.destroyBlock(floor) || !realm.getBlockState(floor).equals(before);
			player.setXRot(0F);
			result[0] = "cobblestone " + cobble + ", lava " + lava + ", floor broken " + broken;
		});
		log("realm ward: " + result[0]);
		if (!result[0].equals("cobblestone false, lava 0, floor broken false")) {
			throw new AssertionError("something was put or broken in her realm: " + result[0]);
		}
	}

	/**
	 * Home by the light before the duel: not called back on the spot (she does not wake for them there); gone away from
	 * her throne and come again, called to the same arena. Home again, striking her on her throne: taken at once.
	 */
	private static void leaveAndComeBack(ClientGameTestContext context, TestSingleplayerContext world, BlockPos c) {
		camera(context, world, c.getX() + 0.5, c.getY(), c.getZ() + 41.5, 0, 0, 30);
		context.waitTicks(140);
		String[] where = {""};
		world.getServer().runOnServer(server -> {
			var player = server.getPlayerList().getPlayers().get(0);
			where[0] = player.level().dimension().location() + (com.iafabric.boss.ThroneFight.arenaOf(player) == null ? "" : " (in a duel)");
		});
		context.takeScreenshot("boss_left_home");
		log("home by the light, a while after: " + where[0]);
		if (!where[0].equals("minecraft:overworld")) {
			throw new AssertionError("going home by the light, the player was called back: " + where[0]);
		}
		// away from her throne, then back to it: called again, to the same arena
		camera(context, world, 0.5, GROUND, 80, 0, 0, 40);
		camera(context, world, 0.5, GROUND, 152, 0, 5, 20);
		BlockPos[] back = {null};
		for (int i = 0; i < 25 && back[0] == null; i++) {
			context.waitTicks(20);
			world.getServer().runOnServer(server -> back[0] = com.iafabric.boss.ThroneFight.arenaOf(server.getPlayerList().getPlayers().get(0)));
		}
		log("gone away and come again: called to " + back[0]);
		if (back[0] == null || !back[0].equals(c)) {
			throw new AssertionError("gone away and come again, the player was not called to the same arena: " + back[0]);
		}
		context.waitTicks(180);
		// home once more by the light, and they strike her on her throne: who strikes her wants the duel (taken at once)
		camera(context, world, c.getX() + 0.5, c.getY(), c.getZ() + 41.5, 0, 0, 40);
		// (she is back on her throne within two seconds: the game looks for thrones to fill every 40 ticks)
		for (int i = 0; i < 6; i++) {
			boolean[] onThrone = {false};
			world.getServer().runOnServer(server -> onThrone[0] = !server.overworld().getEntitiesOfClass(com.iafabric.entity.EntityCryonaAsleep.class,
				server.getPlayerList().getPlayers().get(0).getBoundingBox().inflate(60)).isEmpty());
			if (onThrone[0]) {
				break;
			}
			context.waitTicks(10);
		}
		boolean[] struck = {false};
		world.getServer().runOnServer(server -> {
			var player = server.getPlayerList().getPlayers().get(0);
			for (var queen : server.overworld().getEntitiesOfClass(com.iafabric.entity.EntityCryonaAsleep.class, player.getBoundingBox().inflate(60))) {
				queen.hurt(player.damageSources().playerAttack(player), 1F);
				struck[0] = true;
				break;
			}
		});
		BlockPos[] taken = {null};
		for (int i = 0; i < 25 && taken[0] == null; i++) {
			context.waitTicks(20);
			world.getServer().runOnServer(server -> taken[0] = com.iafabric.boss.ThroneFight.arenaOf(server.getPlayerList().getPlayers().get(0)));
		}
		log("home again, then striking her on her throne (" + struck[0] + "): called to " + taken[0]);
		if (!struck[0] || taken[0] == null || !taken[0].equals(c)) {
			throw new AssertionError("striking her after going home, the player was not taken to the same arena: " + struck[0] + " " + taken[0]);
		}
		context.waitTicks(180);
	}

	/** The state of the duel of an arena must be this (0 not begun, 1 fighting, 4 waiting for its players). */
	private static void expectDuel(TestSingleplayerContext world, BlockPos c, int state, String failure) {
		int[] now = new int[1];
		world.getServer().runOnServer(server -> now[0] = com.iafabric.boss.ThroneFight.stateAt(c));
		log("duel state " + now[0] + " (expected " + state + ")");
		if (now[0] != state) {
			throw new AssertionError(failure + ": state " + now[0]);
		}
	}

	/**
	 * The player falls in the duel: it waits (the two as they were, Black Frost's life kept), the things they had lie
	 * on the ice and never go. Back at her throne she wakes and takes them to the same arena, and the altar begins it
	 * again with blood.
	 */
	private static void fallAndComeBack(ClientGameTestContext context, TestSingleplayerContext world, BlockPos c) {
		still(world, c, 16.5, -12.5);
		float[] frostLife = new float[1];
		world.getServer().runOnServer(server -> {
			var realm = server.getLevel(com.iafabric.boss.ThroneFight.REALM);
			var player = server.getPlayerList().getPlayers().get(0);
			player.getInventory().add(new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.DIAMOND_SWORD));
			player.getInventory().add(new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.SNOWBALL, 16));
			for (var frost : realm.getEntitiesOfClass(EntityDragonBase.class, new net.minecraft.world.phys.AABB(c).inflate(140, 80, 140), EntityDragonBase::isBlackFrost)) {
				frost.setHealth(frost.getMaxHealth() * 0.7F);
				frostLife[0] = frost.getHealth();
			}
			player.hurt(realm.damageSources().genericKill(), Float.MAX_VALUE);
		});
		context.waitTicks(10);
		int[] kept = new int[2];
		world.getServer().runOnServer(server -> {
			var realm = server.getLevel(com.iafabric.boss.ThroneFight.REALM);
			for (var item : realm.getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class, new net.minecraft.world.phys.AABB(c).inflate(140, 80, 140))) {
				kept[0]++;
				if (item.getAge() == -32768) {
					kept[1]++;
				}
			}
		});
		log("after the fall: " + kept[0] + " things on the ice, " + kept[1] + " never to go");
		if (kept[0] == 0 || kept[0] != kept[1]) {
			throw new AssertionError("the fallen player's things are not kept on the ice: " + kept[0] + " lying, " + kept[1] + " kept");
		}
		context.runOnClient(mc -> {
			if (mc.player != null) {
				mc.player.respawn();
			}
		});
		context.waitTicks(40);
		context.runOnClient(mc -> mc.setScreen(null));
		expectDuel(world, c, 4, "the duel does not wait after the fall");
		world.getServer().runCommand("gamemode survival @p");
		world.getServer().runCommand("effect give @p resistance 3000 4 true");
		world.getServer().runCommand("effect give @p regeneration 3000 2 true");
		world.getServer().runOnServer(server -> server.getPlayerList().getPlayers().get(0).setInvulnerable(true));
		// back at her throne: she sleeps there again, wakes, and takes the player to the same arena
		camera(context, world, 0.5, GROUND, 152, 0, 5, 60);
		BlockPos[] back = {null};
		for (int i = 0; i < 25 && back[0] == null; i++) {
			context.waitTicks(20);
			world.getServer().runOnServer(server -> back[0] = com.iafabric.boss.ThroneFight.arenaOf(server.getPlayerList().getPlayers().get(0)));
		}
		if (back[0] == null || !back[0].equals(c)) {
			throw new AssertionError("back at her throne the player was not taken to the same arena: " + back[0]);
		}
		context.waitTicks(40);
		float[] life = new float[1];
		boolean[] resumed = new boolean[1];
		world.getServer().runOnServer(server -> {
			var realm = server.getLevel(com.iafabric.boss.ThroneFight.REALM);
			resumed[0] = com.iafabric.boss.ThroneFight.resumedAt(c);
			for (var frost : realm.getEntitiesOfClass(EntityDragonBase.class, new net.minecraft.world.phys.AABB(c).inflate(140, 80, 140), EntityDragonBase::isBlackFrost)) {
				life[0] = frost.getHealth();
			}
		});
		log("back: the duel goes on " + resumed[0] + ", Black Frost's life " + life[0] + " (it was " + frostLife[0] + ")");
		if (!resumed[0] || Math.abs(life[0] - frostLife[0]) > frostLife[0] * 0.02F) {
			throw new AssertionError("the duel did not go on as it was: resumed " + resumed[0] + ", Black Frost's life " + life[0] + " / " + frostLife[0]);
		}
		camera(context, world, c.getX() + 0.5, c.getY() + 1, c.getZ() + 30.5, 180, 6, 60);
		context.takeScreenshot("boss_again");
		world.getServer().runCommand("give @p iceandfire_fabric:ice_dragon_blood 1");
		world.getServer().runCommand("execute as @p run iceandfire duel accept");
		context.waitTicks(20);
		expectDuel(world, c, 1, "the duel did not go on with blood offered again");
	}

	private static void still(TestSingleplayerContext world, BlockPos c, double playerZ, double frostZ) {
		world.getServer().runOnServer(server -> {
			var realm = server.getLevel(com.iafabric.boss.ThroneFight.REALM);
			var player = server.getPlayerList().getPlayers().get(0);
			player.teleportTo(c.getX() + 0.5, c.getY(), c.getZ() + playerZ);
			for (var frost : realm.getEntitiesOfClass(EntityDragonBase.class, new net.minecraft.world.phys.AABB(c).inflate(140, 80, 140), EntityDragonBase::isBlackFrost)) {
				// (no flight nor grab left halfway)
				com.iafabric.boss.BlackFrostFight.of(frost).stop();
				com.iafabric.boss.BlackFrostFight.of(frost).rest(400);
				frost.setNoAi(true);
				frost.setFlying(false);
				frost.setHovering(false);
				frost.teleportTo(c.getX() + 0.5, c.getY(), c.getZ() + frostZ);
				frost.setYRot(0F);
				frost.yBodyRot = 0F;
				frost.yHeadRot = 0F;
			}
			for (var queen : realm.getEntitiesOfClass(com.iafabric.entity.EntityDreadQueen.class, new net.minecraft.world.phys.AABB(c).inflate(140, 80, 140))) {
				com.iafabric.boss.CryonaFight.of(queen).rest(400);
			}
		});
	}

	/** The effects: warning circles and lanes filling, magic circles, waves, a column of light, ice spikes. */
	private static void effects(ClientGameTestContext context, TestSingleplayerContext world) {
		world.getServer().runOnServer(server -> {
			var level = server.overworld();
			double z = 60;
			com.iafabric.vfx.Vfx.circle(level, new net.minecraft.world.phys.Vec3(-8, GROUND, z), 4F, com.iafabric.vfx.Vfx.DANGER, 200);
			com.iafabric.vfx.Vfx.circle(level, new net.minecraft.world.phys.Vec3(0, GROUND, z + 6), 3F, com.iafabric.vfx.Vfx.ICE, 60);
			com.iafabric.vfx.Vfx.lane(level, new net.minecraft.world.phys.Vec3(8, GROUND, z - 4), 0F, 16F, 3F, com.iafabric.vfx.Vfx.DANGER, 200);
			com.iafabric.vfx.Vfx.rune(level, new net.minecraft.world.phys.Vec3(-6, GROUND, z + 16), 5F, com.iafabric.entity.EntityVfx.FROST, com.iafabric.vfx.Vfx.ICE, 400, 2F, 0F);
			com.iafabric.vfx.Vfx.rune(level, new net.minecraft.world.phys.Vec3(6, GROUND + 4, z + 16), 4F, com.iafabric.entity.EntityVfx.DREAD, com.iafabric.vfx.Vfx.DREAD, 400, -3F, 0F);
			com.iafabric.vfx.Vfx.rune(level, new net.minecraft.world.phys.Vec3(-14, GROUND, z + 6), 3F, com.iafabric.entity.EntityVfx.STORM, com.iafabric.vfx.Vfx.STORM, 400, 4F, 0F);
			com.iafabric.vfx.Vfx.rune(level, new net.minecraft.world.phys.Vec3(16, GROUND, z + 6), 3F, com.iafabric.entity.EntityVfx.FLAME, com.iafabric.vfx.Vfx.FIRE, 400, 4F, 0F);
			com.iafabric.vfx.Vfx.beam(level, new net.minecraft.world.phys.Vec3(6, GROUND + 4, z + 16), 1.2F, 18F, com.iafabric.vfx.Vfx.DREAD, 400);
			for (int i = 0; i < 9; i++) {
				double a = Math.PI * 2 * i / 9;
				com.iafabric.vfx.Vfx.spike(level, new net.minecraft.world.phys.Vec3(-6 + Math.cos(a) * 3.5, GROUND, z + 16 + Math.sin(a) * 3.5),
					(i % 3 == 0 ? net.minecraft.world.level.block.Blocks.BLUE_ICE : net.minecraft.world.level.block.Blocks.PACKED_ICE).defaultBlockState(),
					0.7F, 2.5F + (i % 3), (float) Math.cos(a) * 15F, (float) -Math.sin(a) * 15F, 6, 400);
			}
			com.iafabric.vfx.Vfx.wave(level, new net.minecraft.world.phys.Vec3(0, GROUND, z + 6), 9F, com.iafabric.vfx.Vfx.ICE, 400);
		});
		camera(context, world, 0, GROUND + 14, 34, 0, 32, 60);
		context.takeScreenshot("vfx_overview");
		camera(context, world, -6, GROUND + 6, 66, 0, 30, 10);
		context.takeScreenshot("vfx_frost_circle_spikes");
		// the old dragons' great breath: each calls its element on a zombie (fire rain, ice spikes, a storm)
		String[] kinds = {"fire", "ice", "lightning"};
		for (int k = 0; k < kinds.length; k++) {
			int x = -40 + k * 40;
			int kind = k;
			world.getServer().runOnServer(server -> {
				var level = server.overworld();
				var type = kind == 0 ? IafEntityRegistry.FIRE_DRAGON : kind == 1 ? IafEntityRegistry.ICE_DRAGON : IafEntityRegistry.LIGHTNING_DRAGON;
				EntityDragonBase dragon = type.create(level);
				dragon.moveTo(x, GROUND, 140, 180, 0);
				level.addFreshEntity(dragon);
				dragon.setAgeInDays(0);
				dragon.growDragon(85);
				dragon.setNoAi(true);
				var zombie = net.minecraft.world.entity.EntityType.ZOMBIE.create(level);
				zombie.moveTo(x, GROUND, 124, 0, 0);
				zombie.setNoAi(true);
				zombie.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.DAMAGE_RESISTANCE, 2000, 4));
				zombie.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.FIRE_RESISTANCE, 2000, 0));
				level.addFreshEntity(zombie);
				dragon.setTarget(zombie);
				dragon.specialCooldown = 0;
			});
			camera(context, world, x + 12.5, GROUND + 10, 106.5, 30, 28, 22);
			context.takeScreenshot("vfx_dragon_" + kinds[k] + "_warning");
			context.waitTicks(14);
			context.takeScreenshot("vfx_dragon_" + kinds[k] + "_strike");
		}
	}

	/**
	 * The three dragons breathing at a point ahead of them, as their fights make them: the stream leaves each one's mouth
	 * as drawn (fire and frost puffs, the lightning's bolt), from their side, close to their heads and from ahead; the
	 * fire dragon's charge leaving its mouth; the fire dragon in the air drifting and turning its head as it breathes.
	 */
	private static void breath(ClientGameTestContext context, TestSingleplayerContext world) {
		String[] kinds = {"fire", "ice", "lightning"};
		int[] ids = new int[3];
		// (far enough to see the three of them, and for them to live, while it is shown)
		int[] distances = new int[2];
		context.runOnClient(mc -> {
			distances[0] = mc.options.renderDistance().get();
			distances[1] = mc.options.simulationDistance().get();
			mc.options.renderDistance().set(10);
			mc.options.simulationDistance().set(10);
		});
		double[][] targets = new double[3][];
		world.getServer().runOnServer(server -> {
			var level = server.overworld();
			var types = new net.minecraft.world.entity.EntityType<?>[]{IafEntityRegistry.FIRE_DRAGON, IafEntityRegistry.ICE_DRAGON, IafEntityRegistry.LIGHTNING_DRAGON};
			for (int i = 0; i < 3; i++) {
				EntityDragonBase dragon = (EntityDragonBase) types[i].create(level);
				double z = i * 30 + 0.5;
				dragon.moveTo(30.5, GROUND, z, 90, 0);
				level.addFreshEntity(dragon);
				dragon.setAgeInDays(0);
				dragon.growDragon(85);
				dragon.setAgingDisabled(true);
				dragon.setNoAi(true);
				dragon.setHealth(dragon.getMaxHealth());
				dragon.setYRot(90);
				dragon.yBodyRot = 90;
				dragon.yHeadRot = 90;
				ids[i] = dragon.getId();
				// ahead of it (west), a little lower than its head, in the air (nothing there to burn)
				targets[i] = new double[]{4.5, GROUND + 3, z};
			}
		});
		// the three of them in view (they are drawn: their mouths are where they are drawn)
		camera(context, world, 0, GROUND + 20, -40, -27, 13, 60);
		for (int tick = 0; tick < 30; tick++) {
			breathe(world, ids, targets);
			context.waitTick();
		}
		context.takeScreenshot("breath_all");
		// its mouth as drawn: ahead of it, opening toward where it breathes
		context.runOnClient(mc -> {
			for (int i = 0; i < 3; i++) {
				if (mc.level.getEntity(ids[i]) instanceof EntityDragonBase dragon) {
					var mouth = com.iafabric.client.particle.DragonBreathFx.mouth(dragon);
					if (mouth == null || dragon.mouthDir == null) {
						throw new AssertionError("the " + kinds[i] + " dragon's mouth was not drawn");
					}
					var ahead = mouth.subtract(dragon.position());
					var toTarget = new net.minecraft.world.phys.Vec3(targets[i][0], targets[i][1], targets[i][2]).subtract(mouth).normalize();
					var open = com.iafabric.client.particle.DragonBreathFx.mouthWay(dragon);
					double angle = Math.toDegrees(Math.acos(Math.max(-1, Math.min(1, open.dot(toTarget)))));
					log("breath " + kinds[i] + ": mouth " + rounded(ahead) + " from its feet (old head point " + rounded(dragon.getHeadPosition().subtract(dragon.position()))
						+ "), opening " + Math.round(angle) + " degrees from its target; particles " + mc.particleEngine.countParticles());
					if (ahead.x > -1) {
						throw new AssertionError("the " + kinds[i] + " dragon's mouth is not ahead of it (west): " + rounded(ahead));
					}
				}
			}
		});
		for (int i = 0; i < 3; i++) {
			double z = i * 30 + 0.5;
			int which = i;
			// from its right side (north of it, looking south), a little above: the stream leaves its mouth westward
			breatheWhile(context, world, ids, targets, 6);
			camera(context, world, 20, GROUND + 9, z - 22, 0, 15, 2);
			breatheWhile(context, world, ids, targets, 8);
			context.takeScreenshot("breath_" + kinds[i] + "_side");
			// close to its head
			camera(context, world, 24, GROUND + 7, z - 9, 20, 22, 2);
			breatheWhile(context, world, ids, targets, 8);
			context.takeScreenshot("breath_" + kinds[i] + "_mouth");
			// from ahead of it and a little aside: into its mouth
			camera(context, world, 6, GROUND + 7, z - 12, -51, 12, 2);
			breatheWhile(context, world, ids, targets, 8);
			context.takeScreenshot("breath_" + kinds[i] + "_front");
			context.runOnClient(mc -> {
				if (mc.level.getEntity(ids[which]) instanceof EntityDragonBase dragon) {
					log("breath " + kinds[which] + " now: breathing " + dragon.isBreathingFire() + " (" + dragon.fireBreathProgress + "), seen "
						+ (dragon.tickCount - dragon.breathSeenTick) + " ticks ago, mouth drawn " + (dragon.tickCount - dragon.mouthTick) + " ticks ago"
						+ (dragon instanceof com.iafabric.entity.EntityLightningDragon lightning ? ", bolt " + lightning.hasLightningTarget() : "")
						+ "; particles " + mc.particleEngine.countParticles());
				} else {
					log("breath " + kinds[which] + ": not on the player's game");
				}
			});
		}
		// the fire dragon's charge (its blast): drawn leaving its mouth, on its way within a few ticks
		camera(context, world, 20, GROUND + 9, -21.5, 0, 15, 10);
		for (int tick = 0; tick < 30; tick++) {
			world.getServer().runOnServer(server -> {
				if (server.overworld().getEntity(ids[0]) instanceof EntityDragonBase dragon) {
					dragon.setBreathingFire(false);
					dragon.stimulateFire(targets[0][0], targets[0][1], targets[0][2], 3);
				}
			});
			context.waitTick();
			if (tick == 21 || tick == 22 || tick == 24 || tick == 27) {
				context.takeScreenshot("breath_fire_charge_" + tick);
				context.runOnClient(mc -> {
					for (var charge : mc.level.getEntitiesOfClass(com.iafabric.entity.EntityDragonCharge.class, mc.player.getBoundingBox().inflate(60))) {
						log("charge " + charge.tickCount + " ticks old: drawn from its dragon's mouth " + (charge.fromMouth == null ? "not yet" : rounded(charge.fromMouth)) + " off where its server shot it");
					}
				});
			}
		}
		// the fire dragon in the air, drifting sideways and turning its head as it breathes
		world.getServer().runOnServer(server -> {
			if (server.overworld().getEntity(ids[0]) instanceof EntityDragonBase dragon) {
				dragon.setNoGravity(true);
				dragon.setHovering(true);
				dragon.setPos(dragon.getX(), GROUND + 8, dragon.getZ() - 12);
			}
			for (int i = 1; i < 3; i++) {
				var other = server.overworld().getEntity(ids[i]);
				if (other != null) {
					other.discard();
				}
			}
		});
		// from above (north up), then from ahead and aside
		for (int pass = 0; pass < 2; pass++) {
			world.getServer().runOnServer(server -> {
				if (server.overworld().getEntity(ids[0]) instanceof EntityDragonBase dragon) {
					dragon.setPos(30.5, GROUND + 8, -11.5);
				}
			});
			if (pass == 0) {
				camera(context, world, 16, GROUND + 42, 0.5, 180, 90, 20);
			} else {
				camera(context, world, 2, GROUND + 14, -22, -55, 12, 20);
			}
			for (int tick = 0; tick < 48; tick++) {
				int at = tick;
				world.getServer().runOnServer(server -> {
					if (server.overworld().getEntity(ids[0]) instanceof EntityDragonBase dragon) {
						dragon.setPos(dragon.getX(), dragon.getY(), dragon.getZ() + 0.5);
						dragon.yHeadRot = 90 + 35 * (float) Math.sin(at * 0.25);
						dragon.setBreathingFire(true);
						dragon.stimulateFire(4.5, GROUND + 6, dragon.getZ() - 14 * Math.sin(at * 0.25), 1);
					}
				});
				context.waitTick();
				if (tick >= 18 && tick % 6 == 0) {
					context.takeScreenshot("breath_fire_moving_" + (pass == 0 ? "top_" : "side_") + (tick - 18) / 6);
				}
			}
		}
		world.getServer().runOnServer(server -> {
			var dragon = server.overworld().getEntity(ids[0]);
			if (dragon != null) {
				dragon.discard();
			}
		});
		context.runOnClient(mc -> {
			mc.options.renderDistance().set(distances[0]);
			mc.options.simulationDistance().set(distances[1]);
		});
	}

	/** Each of them breathes at its point, this tick (as their fights do: the server says so to the players' games). */
	private static void breathe(TestSingleplayerContext world, int[] ids, double[][] targets) {
		world.getServer().runOnServer(server -> {
			for (int i = 0; i < ids.length; i++) {
				if (server.overworld().getEntity(ids[i]) instanceof EntityDragonBase dragon) {
					dragon.setBreathingFire(true);
					dragon.stimulateFire(targets[i][0], targets[i][1], targets[i][2], 1);
				}
			}
		});
	}

	private static void breatheWhile(ClientGameTestContext context, TestSingleplayerContext world, int[] ids, double[][] targets, int ticks) {
		for (int tick = 0; tick < ticks; tick++) {
			breathe(world, ids, targets);
			context.waitTick();
		}
	}

	private static String rounded(net.minecraft.world.phys.Vec3 v) {
		return String.format(java.util.Locale.ROOT, "%.2f %.2f %.2f", v.x, v.y, v.z);
	}

	private static void camera(ClientGameTestContext context, TestSingleplayerContext world, double x, double y, double z, float yaw, float pitch, int wait) {
		world.getServer().runOnServer(server -> {
			var player = server.getPlayerList().getPlayers().get(0);
			player.getAbilities().mayfly = true;
			player.getAbilities().flying = true;
			player.onUpdateAbilities();
		});
		// (in the player's own world: a command from the server would take them back to the overworld)
		world.getServer().runOnServer(server -> {
			var player = server.getPlayerList().getPlayers().get(0);
			player.teleportTo((net.minecraft.server.level.ServerLevel) player.level(), x, y, z, java.util.Set.of(), yaw, pitch);
		});
		context.runOnClient(mc -> mc.player.getAbilities().flying = true);
		context.waitTicks(wait);
	}

	/** A kingdom dragon with its pillager, the elite with the warlord, a dread dragon with its knight. */
	private static void riders(ClientGameTestContext context, TestSingleplayerContext world) {
		int[] ids = new int[3];
		world.getServer().runOnServer(server -> {
			var level = server.overworld();
			EntityDragonBase young = IafEntityRegistry.FIRE_DRAGON.create(level);
			young.moveTo(-16, GROUND, 14, 90, 0);
			level.addFreshEntity(young);
			young.setAgeInDays(0);
			young.growDragon(55);
			young.setAgingDisabled(true);
			young.pillagerMount = true;
			for (int i = 0; i < 4; i++) {
				young.setItemSlot(new EquipmentSlot[]{EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET}[i],
					new ItemStack(new net.minecraft.world.item.Item[]{IafItemRegistry.DRAGONARMOR_IRON_0, IafItemRegistry.DRAGONARMOR_IRON_1,
						IafItemRegistry.DRAGONARMOR_IRON_2, IafItemRegistry.DRAGONARMOR_IRON_3}[i]));
			}
			DragonRiders.mount(young, DragonRiders.PILLAGER);
			EntityDragonBase elite = IafEntityRegistry.ICE_DRAGON.create(level);
			elite.moveTo(4, GROUND, 18, 90, 0);
			level.addFreshEntity(elite);
			elite.setAgeInDays(0);
			elite.growDragon(85);
			elite.setAgingDisabled(true);
			elite.pillagerMount = true;
			elite.eliteMount = true;
			for (int i = 0; i < 4; i++) {
				elite.setItemSlot(new EquipmentSlot[]{EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET}[i],
					new ItemStack(new net.minecraft.world.item.Item[]{IafItemRegistry.DRAGONARMOR_NETHERITE_0, IafItemRegistry.DRAGONARMOR_NETHERITE_1,
						IafItemRegistry.DRAGONARMOR_NETHERITE_2, IafItemRegistry.DRAGONARMOR_NETHERITE_3}[i]));
			}
			DragonRiders.mount(elite, DragonRiders.WARLORD);
			EntityDragonBase dread = com.iafabric.world.structure.DreadPieces.dreadDragon(level, level.getRandom(), new BlockPos(26, GROUND, 16), 80, false);
			dread.moveTo(26, GROUND, 16, 90, 0);
			level.addFreshEntity(dread);
			DragonRiders.mount(dread, DragonRiders.KNIGHT);
			for (EntityDragonBase dragon : new EntityDragonBase[]{young, elite, dread}) {
				dragon.setNoAi(true);
				dragon.setHealth(dragon.getMaxHealth());
				dragon.setYRot(90);
				dragon.yBodyRot = 90;
				dragon.yHeadRot = 90;
			}
			ids[0] = young.getId();
			ids[1] = elite.getId();
			ids[2] = dread.getId();
		});
		context.waitTicks(40);
		// from their left side, a little above: the riders on their backs
		camera(context, world, 4, GROUND + 9, -12, 0, 22, 40);
		context.takeScreenshot("riders_standing");
		camera(context, world, -16, GROUND + 5, 2, 0, 15, 20);
		context.takeScreenshot("riders_pillager_close");
		camera(context, world, 4, GROUND + 9, 4, 0, 18, 20);
		context.takeScreenshot("riders_warlord_close");
		camera(context, world, 26, GROUND + 7, 4, 0, 18, 20);
		context.takeScreenshot("riders_knight_close");
		// sitting
		world.getServer().runOnServer(server -> {
			for (int id : ids) {
				if (server.overworld().getEntity(id) instanceof EntityDragonBase dragon) {
					dragon.setInSittingPose(true);
					dragon.setOrderedToSit(true);
				}
			}
		});
		context.waitTicks(40);
		camera(context, world, 4, GROUND + 9, -12, 0, 22, 20);
		context.takeScreenshot("riders_sitting");
		// a hit on the knight shows red; the pillager is killed: the dragon goes to a pillager on foot, who rides it
		world.getServer().runOnServer(server -> {
			var level = server.overworld();
			var player = server.getPlayerList().getPlayers().get(0);
			if (level.getEntity(ids[2]) instanceof EntityDragonBase dread) {
				dread.setNoAi(false);
				DragonRiders.hurtRider(dread, level, level.damageSources().playerAttack(player), 4F);
			}
		});
		context.waitTicks(2);
		camera(context, world, 26, GROUND + 7, 4, 0, 18, 0);
		context.takeScreenshot("riders_knight_hit");
		world.getServer().runOnServer(server -> {
			var level = server.overworld();
			var player = server.getPlayerList().getPlayers().get(0);
			if (level.getEntity(ids[0]) instanceof EntityDragonBase young) {
				young.setNoAi(false);
				young.setOrderedToSit(false);
				young.setCommand(0);
				young.setInSittingPose(false);
				DragonRiders.hurtRider(young, level, level.damageSources().playerAttack(player), 100F);
				log("pillager killed: rider now " + young.getVirtualRider());
				var walker = net.minecraft.world.entity.EntityType.PILLAGER.create(level);
				walker.moveTo(-16, GROUND, 30, 0, 0);
				level.addFreshEntity(walker);
			}
		});
		context.waitTicks(10);
		camera(context, world, -16, GROUND + 8, -2, 0, 20, 0);
		context.takeScreenshot("riders_pillager_down");
		int[] got = {0};
		for (int i = 0; i < 30 && got[0] == 0; i++) {
			context.waitTicks(10);
			world.getServer().runOnServer(server -> {
				if (server.overworld().getEntity(ids[0]) instanceof EntityDragonBase young && young.getVirtualRider() == DragonRiders.PILLAGER) {
					got[0] = 1;
				}
			});
		}
		world.getServer().runOnServer(server -> {
			if (server.overworld().getEntity(ids[0]) instanceof EntityDragonBase young) {
				log(String.format(Locale.ROOT, "after the fall: rider %d candidate %s berserk %s at %.1f %.1f", young.getVirtualRider(),
					young.riderCandidate, young.berserk, young.getX(), young.getZ()));
				young.setNoAi(true);
			}
		});
		camera(context, world, -16, GROUND + 8, -2, 0, 20, 20);
		context.takeScreenshot("riders_pillager_back");
		if (got[0] == 0) {
			throw new AssertionError("the kingdom dragon got no new rider");
		}
	}
}

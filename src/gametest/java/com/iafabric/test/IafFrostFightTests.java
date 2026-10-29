package com.iafabric.test;

import com.iafabric.IafMod;
import com.iafabric.boss.BlackFrostFight;
import com.iafabric.boss.ThroneFight;
import com.iafabric.entity.EntityDragonBase;
import com.iafabric.entity.EntityDreadQueen;
import com.iafabric.test.ct.FabricClientGameTest;
import com.iafabric.test.ct.ClientGameTestContext;
import com.iafabric.test.ct.TestSingleplayerContext;
import net.minecraft.client.gui.screens.worldselection.WorldCreationUiState;
import net.minecraft.client.sounds.SoundEventListener;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Black Frost's duel up close (IAF_ONLY=frostfight; IAF_FROST=sounds,neck,breath,charge,hail,altar for some of it):
 * what the player hears (no sound over and over: ice breaking is one crash, bites flinch once a half second), its head
 * on the player close by (it bows only a little: no neck bent into the ice), its breath (its jet tilted down the lane;
 * without the exiled pixie's gift it melts and cauterizes, with it a cold blow their armor lessens), its charge (long,
 * shards of ice rising along its way), its hail (rings of circles, none over another), the altar (gone in the duel,
 * back when it waits).
 */
public class IafFrostFightTests implements FabricClientGameTest {
	private static final int GROUND = -60;

	private static void log(String message) {
		IafMod.LOGGER.info("[IAFFROST] {}", message);
	}

	@Override
	public void runTest(ClientGameTestContext context) {
		if (System.getenv("IAF_BATTLE") != null || IafTestOnly.skip("frostfight")) {
			return;
		}
		try (TestSingleplayerContext world = context.worldBuilder().adjustSettings(settings -> {
			settings.setGameMode(WorldCreationUiState.SelectedGameMode.SURVIVAL);
			settings.setAllowCheats(true);
		}).create()) {
			world.getServer().runCommand("gamerule doDaylightCycle false");
			world.getServer().runCommand("gamerule doMobSpawning false");
			world.getServer().runCommand("time set noon");
			world.getServer().runCommand("weather clear");
			context.runOnClient(mc -> {
				if (!mc.options.hideGui) {
					mc.options.hideGui = !mc.options.hideGui;
				}
			});
			BlockPos c = duel(context, world);
			if (step("free")) {
				free(context, world, c);
			}
			if (step("stomp")) {
				stomp(context, world, c);
			}
			if (step("dive")) {
				dive(context, world, c);
			}
			if (step("claws")) {
				claws(context, world, c);
			}
			if (step("sounds")) {
				sounds(context, world, c);
			}
			if (step("neck")) {
				neck(context, world, c);
			}
			if (step("closeup")) {
				closeup(context, world, c);
			}
			if (step("breath")) {
				breath(context, world, c);
			}
			if (step("charge")) {
				charge(context, world, c);
			}
			if (step("hail")) {
				hail(context, world, c);
			}
			if (step("altar")) {
				altar(context, world, c);
			}
		}
	}

	/** Whether to run a part of the scene (IAF_FROST=neck,breath... runs only those; all by default). */
	private static boolean step(String name) {
		String only = System.getenv("IAF_FROST");
		return only == null || only.isBlank() || java.util.Arrays.asList(only.split(",")).contains(name);
	}

	/** Her sign by the player, her call, the duel accepted with blood: the arena's middle. */
	private static BlockPos duel(ClientGameTestContext context, TestSingleplayerContext world) {
		world.getServer().runOnServer(server -> {
			ServerPlayer player = server.getPlayerList().getPlayers().get(0);
			player.setInvulnerable(true);
			var level = server.overworld();
			// (at the foot of her dais: her call reaches them there)
			player.teleportTo(level, 0.5, GROUND, 152.5, Set.of(), 0, 5);
			var frost = com.iafabric.entity.EntityBlackFrostGuard.make(level, level.getRandom(),
				new BlockPos(0, GROUND, 158 + com.iafabric.world.structure.CitadelPiece.GUARD_BEHIND), 180F);
			level.addFreshEntity(frost);
			var queen = com.iafabric.entity.EntityCryonaAsleep.make(level, new BlockPos(0, GROUND, 158),
				new BlockPos(0, GROUND, 158 + com.iafabric.world.structure.CitadelPiece.GUARD_BEHIND), 0.5, GROUND, 158.5, 180F, MobSpawnType.COMMAND);
			level.addFreshEntity(queen);
		});
		BlockPos[] arena = {null};
		for (int i = 0; i < 30 && arena[0] == null; i++) {
			context.waitTicks(10);
			world.getServer().runOnServer(server -> arena[0] = ThroneFight.arenaOf(server.getPlayerList().getPlayers().get(0)));
		}
		if (arena[0] == null) {
			throw new AssertionError("the player was not taken into Cryona's realm");
		}
		context.waitTicks(120);
		world.getServer().runCommand("give @p iceandfire_fabric:ice_dragon_blood 2");
		world.getServer().runCommand("execute as @p run iceandfire duel accept");
		context.waitTicks(20);
		int[] state = new int[1];
		world.getServer().runOnServer(server -> state[0] = ThroneFight.stateAt(arena[0]));
		log("the duel at " + arena[0].toShortString() + ", state " + state[0]);
		if (state[0] != 1) {
			throw new AssertionError("the duel did not begin: state " + state[0]);
		}
		return arena[0];
	}

	/**
	 * Its neck as the player's game draws it, a minute of the duel left to itself (no rest, no blow cast): the player
	 * goes round it near and far, now and then under its neck and at its forelegs, striking it when close. Each tick:
	 * its head's turn from its body and its tilt as drawn, the swing its breath gives its neck; never bent for long.
	 */
	private static void free(ClientGameTestContext context, TestSingleplayerContext world, BlockPos c) {
		world.getServer().runOnServer(server -> {
			ServerPlayer player = server.getPlayerList().getPlayers().get(0);
			player.setInvulnerable(true);
			player.getAbilities().flying = false;
			player.getAbilities().mayfly = false;
			player.onUpdateAbilities();
			player.teleportTo(c.getX() + 0.5, c.getY(), c.getZ() + 20.5);
		});
		int id = world.getServer().computeOnServer(server -> frostAt(server, c).getId());
		float worstTurn = 0;
		float worstTilt = 0;
		float worstSwing = 0;
		int bent = 0;
		int streak = 0;
		int longest = 0;
		int shots = 0;
		float[] worstNeck = {-999, 999};
		int[] stompShots = {0};
		StringBuilder timeline = new StringBuilder();
		for (int i = 0; i < 1200; i++) {
			context.waitTicks(1);
			int n = i;
			// the player round it: a slow circle that comes in close (under its neck, at its forelegs) and goes out again
			world.getServer().runOnServer(server -> {
				ServerPlayer player = server.getPlayerList().getPlayers().get(0);
				EntityDragonBase frost = frostAt(server, c);
				com.iafabric.boss.BossCold.cure(player);
				player.setHealth(player.getMaxHealth());
				if (n % 4 == 0) {
					double a = n * 0.012;
					double r = 14 + 12 * Math.sin(n * 0.009);
					Vec3 at = frost.position().add(Math.cos(a) * r, 0, Math.sin(a) * r);
					Vec3 ground = com.iafabric.vfx.Vfx.ground((ServerLevel) frost.level(), at.add(0, 3, 0));
					double yaw = Math.toRadians(frost.getYRot());
					Vec3 neck = frost.position().add(-Math.sin(yaw) * 9, 5, Math.cos(yaw) * 9);
					float[] way = look(ground.add(0, 1.62, 0), neck);
					player.teleportTo((ServerLevel) player.level(), ground.x, ground.y, ground.z, Set.of(), way[0], way[1]);
				}
				if (n % 10 == 0 && player.distanceTo(frost) < 10) {
					player.attack(frost);
					player.resetAttackStrengthTicker();
				}
			});
			String[] state = {""};
			float[] drawn = context.computeOnClient(mc -> {
				if (mc.level == null || !(mc.level.getEntity(id) instanceof EntityDragonBase frost)) {
					return new float[]{0, 0, 0, 0, 0, 0};
				}
				float neck = 0;
				float body = 0;
				if (mc.getEntityRenderDispatcher().getRenderer(frost) instanceof com.iafabric.client.render.entity.RenderDragonBase render) {
					var model = render.getModel();
					for (String part : new String[]{"Neck1", "Neck2", "Neck3", "Head"}) {
						var cube = model.getCube(part);
						if (cube != null) {
							neck += (float) Math.toDegrees(cube.rotateAngleX - cube.defaultRotationX);
						}
					}
					var upper = model.getCube("BodyUpper");
					body = upper == null ? 0 : (float) Math.toDegrees(upper.rotateAngleX - upper.defaultRotationX);
				}
				var anim = frost.getAnimation();
				String name = anim == EntityDragonBase.ANIMATION_BITE ? "bite" : anim == EntityDragonBase.ANIMATION_WINGBLAST ? "wingblast"
					: anim == EntityDragonBase.ANIMATION_TAILWHACK ? "tailwhack" : anim == EntityDragonBase.ANIMATION_ROAR ? "roar"
					: anim == EntityDragonBase.ANIMATION_EPIC_ROAR ? "epic_roar" : anim == EntityDragonBase.ANIMATION_FIRECHARGE ? "firecharge"
					: anim == EntityDragonBase.ANIMATION_SPEAK ? "speak" : anim == EntityDragonBase.ANIMATION_SHAKEPREY ? "shakeprey"
					: anim == EntityDragonBase.ANIMATION_EAT ? "eat" : "none";
				state[0] = String.format(Locale.ROOT, "anim %s %d, breath %.1f, hover %.1f, fly %.1f, sit %.1f, sleep %.1f, dive %.1f, tackle %.1f, dead %.1f, pitch %.0f, body %.0f",
					name, frost.getAnimationTick(), frost.fireBreathProgress, frost.hoverProgress, frost.flyProgress, frost.sitProgress, frost.sleepProgress,
					frost.diveProgress, frost.tackleProgress, frost.modelDeadProgress, frost.getDragonPitch(), body);
				return new float[]{net.minecraft.util.Mth.wrapDegrees(frost.yHeadRot - frost.yBodyRot), frost.getXRot(),
					frost.turn_buffer == null ? 0 : frost.turn_buffer.swing(), frost.isBreathingFire() ? 1 : 0, frost.fireBreathProgress, neck};
			});
			String blow = world.getServer().computeOnServer(server -> {
				EntityDragonBase frost = frostAt(server, c);
				String great = BlackFrostFight.of(frost).greatBlow();
				return (great == null ? BlackFrostFight.of(frost).stance() : great) + String.format(Locale.ROOT, ", on the server turned %.0f, its yaw %.0f",
					net.minecraft.util.Mth.wrapDegrees(frost.yHeadRot - frost.yBodyRot), frost.getYRot());
			});
			float turn = Math.abs(drawn[0]);
			float tilt = Math.abs(drawn[1]);
			float swing = drawn[3] > 0 ? Math.abs(drawn[2]) : 0;
			float neck = drawn[5];
			worstTurn = Math.max(worstTurn, turn);
			worstTilt = Math.max(worstTilt, tilt);
			worstSwing = Math.max(worstSwing, swing);
			worstNeck[0] = Math.max(worstNeck[0], neck);
			worstNeck[1] = Math.min(worstNeck[1], neck);
			boolean isBent = turn > 12 || tilt > 10 || swing > 12 || Math.abs(neck) > 40;
			if (isBent) {
				bent++;
				streak++;
				longest = Math.max(longest, streak);
				if (streak == 6 && shots < 6) {
					shots++;
					context.takeScreenshot("frost_free_bent_" + n);
				}
			} else {
				streak = 0;
			}
			if (i % 10 == 0 || isBent && streak <= 3) {
				timeline.append(String.format(Locale.ROOT, "%n  %4d: neck bent %.0f, turned %.0f, tilted %.0f, breath swing %.0f%s (%s; %s)", n, drawn[5], drawn[0], drawn[1],
					drawn[2], drawn[3] > 0 ? " BREATHING " + drawn[4] : "", blow, state[0]));
			}
			if (i % 300 == 150) {
				context.takeScreenshot("frost_free_" + n);
			}
			if (state[0].startsWith("anim wingblast 1") && stompShots[0] < 3) {
				// its stomp (its wing beat), halfway: its neck as drawn then
				stompShots[0]++;
				context.takeScreenshot("frost_free_stomp_" + n);
			}
		}
		log(String.format(Locale.ROOT, "a minute left to itself: its neck as drawn bent %.0f to %.0f from its pose, its head at most %.0f off its body, tilted at most %.0f, "
			+ "its breath's swing at most %.0f; bent %d ticks, %d at a stretch at most", worstNeck[1], worstNeck[0], worstTurn, worstTilt, worstSwing, bent, longest) + timeline);
		if (longest > 10 || worstNeck[0] > 15) {
			throw new AssertionError("its neck bent down " + worstNeck[0] + " degrees, or stayed bent " + longest + " ticks at a stretch (turn " + worstTurn + ", tilt "
				+ worstTilt + ", swing " + worstSwing + ")");
		}
	}

	/** Its dive: the player well inside its circle, near its edge, loses as much as at its heart; outside it, nothing. */
	private static void dive(ClientGameTestContext context, TestSingleplayerContext world, BlockPos c) {
		for (double off : new double[]{9.5, 13.5}) {
			world.getServer().runOnServer(server -> {
				ServerPlayer player = server.getPlayerList().getPlayers().get(0);
				EntityDragonBase frost = frostAt(server, c);
				BlackFrostFight fight = BlackFrostFight.of(frost);
				fight.stop();
				calm(server, c);
				frost.teleportTo(c.getX() + 0.5, c.getY(), c.getZ() - 20.5);
				player.setInvulnerable(false);
				player.getAbilities().flying = false;
				player.getAbilities().mayfly = false;
				player.onUpdateAbilities();
				com.iafabric.boss.BossCold.cure(player);
				player.setHealth(player.getMaxHealth());
				player.teleportTo(c.getX() + 0.5, c.getY(), c.getZ() + 20.5);
				fight.ready(false);
				fight.cast(server.getLevel(ThroneFight.REALM), "dive");
			});
			context.waitTicks(6);
			// (to that distance from the heart of its circle, and still there when it falls)
			world.getServer().runOnServer(server -> server.getPlayerList().getPlayers().get(0).teleportTo(c.getX() + 0.5 + off, c.getY(), c.getZ() + 20.5));
			context.waitTicks(10);
			if (off < 10) {
				camera(context, world, c.getX() + 0.5 + off, c.getY() + 18, c.getZ() + 34.5, 150, 40, 1);
				context.takeScreenshot("frost_dive_circle");
				world.getServer().runOnServer(server -> {
					ServerPlayer player = server.getPlayerList().getPlayers().get(0);
					player.getAbilities().flying = false;
					player.getAbilities().mayfly = false;
					player.onUpdateAbilities();
					player.teleportTo(c.getX() + 0.5 + off, c.getY(), c.getZ() + 20.5);
				});
				context.runOnClient(mc -> mc.player.getAbilities().flying = false);
			}
			float[] life = new float[2];
			world.getServer().runOnServer(server -> life[0] = server.getPlayerList().getPlayers().get(0).getHealth());
			for (int i = 0; i < 80; i++) {
				context.waitTicks(1);
				String great = world.getServer().computeOnServer(server -> BlackFrostFight.of(frostAt(server, c)).greatBlow());
				if (great == null) {
					break;
				}
			}
			context.waitTicks(4);
			world.getServer().runOnServer(server -> {
				ServerPlayer player = server.getPlayerList().getPlayers().get(0);
				life[1] = player.getHealth();
				com.iafabric.boss.BossCold.cure(player);
				player.setHealth(player.getMaxHealth());
				player.setInvulnerable(true);
			});
			float lost = (life[0] - life[1]) / 20F;
			log(String.format(Locale.ROOT, "its dive, the player %.1f blocks from its heart (its circle 11): lost %.0f%% of their life", off, lost * 100));
			if (off < 11 && lost < 0.6F || off > 11 && lost > 0.05F) {
				throw new AssertionError("its dive did not strike all its circle alike: " + off + " blocks off, lost " + lost);
			}
			context.waitTicks(30);
		}
	}

	/** Its claws for who strikes it with their own hand close by: taken, thrown far, hurt. */
	private static void claws(ClientGameTestContext context, TestSingleplayerContext world, BlockPos c) {
		float[] life = new float[2];
		world.getServer().runOnServer(server -> {
			ServerPlayer player = server.getPlayerList().getPlayers().get(0);
			EntityDragonBase frost = frostAt(server, c);
			BlackFrostFight fight = BlackFrostFight.of(frost);
			fight.stop();
			calm(server, c);
			fight.rest(400);
			frost.teleportTo(c.getX() + 0.5, c.getY(), c.getZ() + 0.5);
			player.setInvulnerable(false);
			player.getAbilities().flying = false;
			player.getAbilities().mayfly = false;
			player.onUpdateAbilities();
			com.iafabric.boss.BossCold.cure(player);
			player.setHealth(player.getMaxHealth());
			player.teleportTo(c.getX() + 6.5, c.getY(), c.getZ() + 3.5);
			fight.ready(true);
			fight.rest(400);
			life[0] = player.getHealth();
		});
		int id = world.getServer().computeOnServer(server -> frostAt(server, c).getId());
		context.waitTicks(2);
		String before = world.getServer().computeOnServer(server -> {
			ServerPlayer player = server.getPlayerList().getPlayers().get(0);
			log("the player's weakness (from its roar): " + player.hasEffect(net.minecraft.world.effect.MobEffects.WEAKNESS));
			return BlackFrostFight.of(frostAt(server, c)).greatBlow();
		});
		if (before != null) {
			throw new AssertionError("it began a great blow before it was struck: " + before);
		}
		// (struck as a player does, through their game: any blow by hand counts, even one that does it no harm)
		context.runOnClient(mc -> {
			net.minecraft.world.entity.Entity entity = mc.level.getEntity(id);
			if (entity != null) {
				mc.gameMode.attack(mc.player, entity);
			}
		});
		String[] grab = {null};
		for (int i = 0; i < 6 && grab[0] == null; i++) {
			context.waitTicks(1);
			grab[0] = world.getServer().computeOnServer(server -> BlackFrostFight.of(frostAt(server, c)).greatBlow());
		}
		log("struck close by with the player's own hand: " + grab[0]);
		if (!"grab".equals(grab[0])) {
			throw new AssertionError("its claws did not close on the player who struck it close by: " + grab[0]);
		}
		context.waitTicks(30);
		context.runOnClient(mc -> mc.options.setCameraType(net.minecraft.client.CameraType.THIRD_PERSON_BACK));
		context.waitTicks(2);
		context.takeScreenshot("frost_claws_held");
		context.runOnClient(mc -> mc.options.setCameraType(net.minecraft.client.CameraType.FIRST_PERSON));
		Vec3[] from = new Vec3[1];
		world.getServer().runOnServer(server -> from[0] = server.getPlayerList().getPlayers().get(0).position());
		for (int i = 0; i < 160; i++) {
			context.waitTicks(1);
			boolean down = world.getServer().computeOnServer(server -> {
				ServerPlayer player = server.getPlayerList().getPlayers().get(0);
				return player.onGround() && BlackFrostFight.of(frostAt(server, c)).greatBlow() == null;
			});
			if (down && i > 40) {
				break;
			}
		}
		context.waitTicks(4);
		double[] thrown = new double[1];
		world.getServer().runOnServer(server -> {
			ServerPlayer player = server.getPlayerList().getPlayers().get(0);
			life[1] = player.getHealth();
			thrown[0] = Math.sqrt(player.position().subtract(from[0]).horizontalDistanceSqr());
			com.iafabric.boss.BossCold.cure(player);
			player.setHealth(player.getMaxHealth());
			player.setInvulnerable(true);
		});
		float lost = (life[0] - life[1]) / 20F;
		log(String.format(Locale.ROOT, "in its claws: thrown %.0f blocks, lost %.0f%% of their life", thrown[0], lost * 100));
		if (thrown[0] < 12 || lost < 0.25F) {
			throw new AssertionError("its claws did not throw the player far, nor hurt them well: " + thrown[0] + " blocks, lost " + lost);
		}
		context.waitTicks(20);
	}

	/** Its stomp (its wing beat) seen from aside at its height: its neck stays up, never hanging at its chest. */
	private static void stomp(ClientGameTestContext context, TestSingleplayerContext world, BlockPos c) {
		int id = world.getServer().computeOnServer(server -> {
			ServerPlayer player = server.getPlayerList().getPlayers().get(0);
			EntityDragonBase frost = frostAt(server, c);
			BlackFrostFight fight = BlackFrostFight.of(frost);
			fight.stop();
			calm(server, c);
			frost.teleportTo(c.getX() + 0.5, c.getY(), c.getZ() + 0.5);
			frost.setYRot(0F);
			frost.yBodyRot = 0F;
			frost.yHeadRot = 0F;
			player.setInvulnerable(true);
			player.getAbilities().flying = false;
			player.getAbilities().mayfly = false;
			player.onUpdateAbilities();
			player.teleportTo(c.getX() + 0.5, c.getY(), c.getZ() + 7.5);
			fight.ready(false);
			fight.cast(server.getLevel(ThroneFight.REALM), "stomp");
			return frost.getId();
		});
		for (int k = 0; k < 2; k++) {
			int tick = k == 0 ? 8 : 16;
			for (int i = 0; i < 60; i++) {
				context.waitTicks(1);
				int now = context.computeOnClient(mc -> mc.level.getEntity(id) instanceof EntityDragonBase frost
					&& frost.getAnimation() == EntityDragonBase.ANIMATION_WINGBLAST ? frost.getAnimationTick() : -1);
				if (now >= tick) {
					break;
				}
			}
			world.getServer().runOnServer(server -> {
				ServerPlayer player = server.getPlayerList().getPlayers().get(0);
				player.setGameMode(net.minecraft.world.level.GameType.SPECTATOR);
				Vec3 eye = new Vec3(c.getX() + 26.5, c.getY() + 5, c.getZ() + 9.5);
				float[] way = look(eye, new Vec3(c.getX() + 0.5, c.getY() + 4, c.getZ() + 9.5));
				player.teleportTo((ServerLevel) player.level(), eye.x, eye.y - 1.62, eye.z, Set.of(), way[0], way[1]);
			});
			context.waitTicks(1);
			context.takeScreenshot("frost_stomp_side_" + tick);
			world.getServer().runOnServer(server -> {
				ServerPlayer player = server.getPlayerList().getPlayers().get(0);
				player.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);
				player.teleportTo(c.getX() + 0.5, c.getY(), c.getZ() + 7.5);
			});
		}
		context.waitTicks(30);
	}

	/** What the player hears in the duel as it goes (twenty seconds): no sound over and over. */
	private static void sounds(ClientGameTestContext context, TestSingleplayerContext world, BlockPos c) {
		Map<String, Integer> heard = new HashMap<>();
		Map<String, int[]> perSecond = new HashMap<>();
		long[] tick = {0};
		SoundEventListener listener = (sound, events) -> {
			String id = sound.getLocation().toString();
			heard.merge(id, 1, Integer::sum);
			int[] seconds = perSecond.computeIfAbsent(id, k -> new int[40]);
			seconds[(int) Math.min(39, tick[0] / 20)]++;
		};
		context.runOnClient(mc -> mc.getSoundManager().addListener(listener));
		world.getServer().runOnServer(server -> {
			ServerPlayer player = server.getPlayerList().getPlayers().get(0);
			player.getAbilities().flying = false;
			player.getAbilities().mayfly = false;
			player.onUpdateAbilities();
			player.teleportTo(c.getX() + 0.5, c.getY(), c.getZ() + 26.5);
		});
		String[] powers = {"hail", "frost_breath", "charge", "hail"};
		for (int i = 0; i < 600; i++) {
			context.waitTicks(1);
			tick[0]++;
			if (i >= 400 && i % 4 == 0) {
				// the last ten seconds: the player at its side in its cold, striking it as fast as they can
				int n = i;
				world.getServer().runOnServer(server -> {
					ServerPlayer player = server.getPlayerList().getPlayers().get(0);
					EntityDragonBase frost = frostAt(server, c);
					if (n == 400) {
						BlackFrostFight fight = BlackFrostFight.of(frost);
						fight.stop();
						fight.rest(400);
						calm(server, c);
						player.teleportTo(frost.getX() + 9, frost.getY(), frost.getZ() + 2);
					}
					player.attack(frost);
					player.resetAttackStrengthTicker();
				});
			}
			if (i < 400 && i % 100 == 20) {
				String power = powers[i / 100];
				world.getServer().runOnServer(server -> {
					EntityDragonBase frost = frostAt(server, c);
					BlackFrostFight fight = BlackFrostFight.of(frost);
					// (a great blow of its own under way would keep it from this one)
					fight.stop();
					fight.ready(false);
					fight.cast(server.getLevel(ThroneFight.REALM), power);
					log("cast " + power + ": " + fight.greatBlow() + ", its foes " + ThroneFight.foesNear(frost).size());
				});
			}
			if (i % 20 == 0) {
				// (the player lives through it: their wound mended, their life back)
				world.getServer().runOnServer(server -> {
					ServerPlayer player = server.getPlayerList().getPlayers().get(0);
					com.iafabric.boss.BossCold.cure(player);
					player.setHealth(player.getMaxHealth());
				});
			}
			if (i == 200) {
				context.takeScreenshot("frost_fight_going");
			}
		}
		context.runOnClient(mc -> mc.getSoundManager().removeListener(listener));
		List<Map.Entry<String, Integer>> sorted = new ArrayList<>(heard.entrySet());
		sorted.sort((a, b) -> b.getValue() - a.getValue());
		StringBuilder out = new StringBuilder();
		for (Map.Entry<String, Integer> entry : sorted) {
			int max = 0;
			for (int n : perSecond.get(entry.getKey())) {
				max = Math.max(max, n);
			}
			out.append(String.format(Locale.ROOT, "%n  %s x%d (at most %d in a second)", entry.getKey(), entry.getValue(), max));
		}
		log("sounds heard in thirty seconds of the duel (the last ten striking it at its side):" + out);
		if (!heard.containsKey("iceandfire_fabric:black_frost_breath") || heard.containsKey("iceandfire_fabric:icedragon_breath")) {
			throw new AssertionError("its frost breath did not sound as its own (black_frost_breath " + heard.get("iceandfire_fabric:black_frost_breath")
				+ ", the old icedragon_breath " + heard.get("iceandfire_fabric:icedragon_breath") + ")");
		}
		for (String din : new String[]{"minecraft:block.glass.break", "minecraft:entity.player.hurt_freeze", "iceandfire_fabric:icedragon_adult_hurt",
			"minecraft:block.powder_snow.break"}) {
			int max = 0;
			for (int n : perSecond.getOrDefault(din, new int[1])) {
				max = Math.max(max, n);
			}
			if (max > 4) {
				throw new AssertionError(din + " heard " + max + " times in a second: a din");
			}
		}
	}

	/**
	 * Its head on the player close by: before its jaws, at its side, at its flank, under its neck; seen through the
	 * player's eyes, then from aside (the player a spectator a moment: its head stays as it was).
	 */
	private static void neck(ClientGameTestContext context, TestSingleplayerContext world, BlockPos c) {
		// first as it stands looking ahead (no foe about: the player a spectator by its side)
		world.getServer().runOnServer(server -> {
			ServerPlayer player = server.getPlayerList().getPlayers().get(0);
			EntityDragonBase frost = frostAt(server, c);
			BlackFrostFight fight = BlackFrostFight.of(frost);
			fight.stop();
			fight.rest(400);
			frost.setFlying(false);
			frost.setHovering(false);
			frost.teleportTo(c.getX() + 0.5, c.getY(), c.getZ() + 0.5);
			frost.setYRot(0F);
			frost.yBodyRot = 0F;
			frost.yHeadRot = 0F;
			frost.setXRot(0F);
			player.setGameMode(net.minecraft.world.level.GameType.SPECTATOR);
			Vec3 eye = new Vec3(c.getX() + 34.5, c.getY() + 8, c.getZ() + 12.5);
			float[] way = look(eye, frost.position().add(0, 6, 8));
			player.teleportTo((ServerLevel) player.level(), eye.x, eye.y - 1.62, eye.z, Set.of(), way[0], way[1]);
		});
		context.waitTicks(20);
		world.getServer().runOnServer(server -> {
			EntityDragonBase frost = frostAt(server, c);
			log(String.format(Locale.ROOT, "standing, no foe about: tilted %.0f, its body pitched %.0f", frost.getXRot(), frost.getDragonPitch()));
		});
		context.takeScreenshot("frost_neck_neutral_side");
		double[][] spots = {{0, 26}, {9, 16}, {-6, 20}, {14, 4}, {0, 12}};
		String[] names = {"front", "aside", "aside_left", "flank", "under"};
		for (int i = 0; i < spots.length; i++) {
			double[] spot = spots[i];
			String name = names[i];
			world.getServer().runOnServer(server -> {
				ServerPlayer player = server.getPlayerList().getPlayers().get(0);
				EntityDragonBase frost = frostAt(server, c);
				BlackFrostFight fight = BlackFrostFight.of(frost);
				fight.stop();
				fight.rest(400);
				frost.setFlying(false);
				frost.setHovering(false);
				frost.teleportTo(c.getX() + 0.5, c.getY(), c.getZ() + 0.5);
				frost.setYRot(0F);
				frost.yBodyRot = 0F;
				frost.yHeadRot = 0F;
				player.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);
				player.getAbilities().flying = false;
				player.onUpdateAbilities();
				Vec3 at = new Vec3(c.getX() + 0.5 + spot[0], c.getY(), c.getZ() + 0.5 + spot[1]);
				float[] way = look(at.add(0, 1.62, 0), frost.getHeadPosition());
				player.teleportTo((ServerLevel) player.level(), at.x, at.y, at.z, Set.of(), way[0], way[1]);
				for (EntityDreadQueen queen : server.getLevel(ThroneFight.REALM).getEntitiesOfClass(EntityDreadQueen.class, new AABB(c).inflate(140, 80, 140))) {
					com.iafabric.boss.CryonaFight.of(queen).rest(400);
				}
			});
			context.waitTicks(30);
			float[] head = new float[3];
			world.getServer().runOnServer(server -> {
				ServerPlayer player = server.getPlayerList().getPlayers().get(0);
				EntityDragonBase frost = frostAt(server, c);
				head[0] = net.minecraft.util.Mth.wrapDegrees(frost.yHeadRot - frost.yBodyRot);
				head[1] = frost.getXRot();
				head[2] = net.minecraft.util.Mth.wrapDegrees(frost.getYRot());
				log(String.format(Locale.ROOT, "  its state: sleeping %s (%.1f), sitting %s (%.1f), noAi %s, led %s, size %.1f x %.1f, head at %s from it, animation %s, rider %s",
					frost.isSleeping(), frost.sleepProgress, frost.isOrderedToSit(), frost.sitProgress, frost.isNoAi(), frost.fightLed, frost.getBbWidth(), frost.getBbHeight(),
					frost.getHeadPosition().subtract(frost.position()), frost.getAnimation() == null ? "none" : frost.getAnimation().toString(), frost.getFirstPassenger()));
				float[] way = look(player.getEyePosition(), frost.getHeadPosition());
				player.teleportTo((ServerLevel) player.level(), player.getX(), player.getY(), player.getZ(), Set.of(), way[0], way[1]);
			});
			log(String.format(Locale.ROOT, "the player %s of it: its head turned %.0f from its body, tilted %.0f, its body at %.0f", name, head[0], head[1], head[2]));
			if (head[1] > 8.5F || head[1] < -14.5F) {
				throw new AssertionError("Black Frost's head bows " + head[1] + " degrees toward the player " + name + ": its neck bends into the ice");
			}
			context.waitTicks(2);
			context.takeScreenshot("frost_neck_" + name);
			world.getServer().runOnServer(server -> {
				ServerPlayer player = server.getPlayerList().getPlayers().get(0);
				EntityDragonBase frost = frostAt(server, c);
				player.setGameMode(net.minecraft.world.level.GameType.SPECTATOR);
				Vec3 eye = new Vec3(c.getX() + 34.5, c.getY() + 8, c.getZ() + 12.5);
				float[] way = look(eye, frost.position().add(0, 6, 8));
				player.teleportTo((ServerLevel) player.level(), eye.x, eye.y - 1.62, eye.z, Set.of(), way[0], way[1]);
			});
			context.waitTicks(4);
			context.takeScreenshot("frost_neck_" + name + "_side");
		}
		world.getServer().runOnServer(server -> server.getPlayerList().getPlayers().get(0).setGameMode(net.minecraft.world.level.GameType.SURVIVAL));
	}

	/**
	 * Close by its forelegs while it fights on foot (as a player who runs at it): through the player's eyes every few
	 * ticks, which of its moves it is in, its head's tilt.
	 */
	private static void closeup(ClientGameTestContext context, TestSingleplayerContext world, BlockPos c) {
		world.getServer().runOnServer(server -> {
			ServerPlayer player = server.getPlayerList().getPlayers().get(0);
			EntityDragonBase frost = frostAt(server, c);
			BlackFrostFight fight = BlackFrostFight.of(frost);
			fight.stop();
			calm(server, c);
			frost.setFlying(false);
			frost.setHovering(false);
			frost.teleportTo(c.getX() + 0.5, c.getY(), c.getZ() + 0.5);
			frost.setYRot(0F);
			frost.yBodyRot = 0F;
			frost.yHeadRot = 0F;
			player.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);
			player.setInvulnerable(true);
			player.getAbilities().flying = false;
			player.getAbilities().mayfly = false;
			player.onUpdateAbilities();
			Vec3 at = new Vec3(c.getX() + 3.5, c.getY(), c.getZ() + 9.5);
			float[] way = look(at.add(0, 1.62, 0), frost.position().add(0, 9, 14));
			player.teleportTo((ServerLevel) player.level(), at.x, at.y, at.z, Set.of(), way[0], way[1]);
			fight.cast(server.getLevel(ThroneFight.REALM), "melee");
			fight.ready(false);
		});
		for (int i = 0; i < 24; i++) {
			context.waitTicks(5);
			String[] state = {""};
			world.getServer().runOnServer(server -> {
				ServerPlayer player = server.getPlayerList().getPlayers().get(0);
				EntityDragonBase frost = frostAt(server, c);
				var anim = frost.getAnimation();
				String name = anim == EntityDragonBase.ANIMATION_BITE ? "bite" : anim == EntityDragonBase.ANIMATION_WINGBLAST ? "wingblast"
					: anim == EntityDragonBase.ANIMATION_TAILWHACK ? "tailwhack" : anim == EntityDragonBase.ANIMATION_ROAR ? "roar"
					: anim == EntityDragonBase.ANIMATION_EPIC_ROAR ? "epic_roar" : anim == EntityDragonBase.ANIMATION_FIRECHARGE ? "firecharge"
					: anim == EntityDragonBase.ANIMATION_SPEAK ? "speak" : anim == EntityDragonBase.ANIMATION_SHAKEPREY ? "shakeprey" : "none";
				// (the player looks up at its neck, as one at its forelegs does)
				double r = Math.toRadians(frost.getYRot());
				Vec3 neck = frost.position().add(-Math.sin(r) * 17, 4, Math.cos(r) * 17);
				float[] way = look(player.getEyePosition(), neck);
				player.teleportTo((ServerLevel) player.level(), player.getX(), player.getY(), player.getZ(), Set.of(), way[0], way[1]);
				state[0] = String.format(Locale.ROOT, "%s %d, head tilted %.0f, turned %.0f, body at %.0f, %.1f from the player, %s, %s", name, frost.getAnimationTick(),
					frost.getXRot(), net.minecraft.util.Mth.wrapDegrees(frost.yHeadRot - frost.yBodyRot), net.minecraft.util.Mth.wrapDegrees(frost.getYRot()),
					Math.sqrt(frost.position().subtract(player.position()).horizontalDistanceSqr()), BlackFrostFight.of(frost).stance(),
					BlackFrostFight.of(frost).striking() ? "striking" : "");
			});
			log("close by its forelegs, " + (i * 5 + 5) + " ticks: " + state[0]);
			context.takeScreenshot("frost_close_" + (i * 5 + 5));
		}
		// its blows seen from aside, halfway through each (the player a spectator a moment, by its flank)
		for (int k = 0; k < 3; k++) {
			boolean[] striking = {false};
			for (int i = 0; i < 80 && !striking[0]; i++) {
				context.waitTicks(1);
				world.getServer().runOnServer(server -> striking[0] = BlackFrostFight.of(frostAt(server, c)).striking());
			}
			context.waitTicks(10);
			Vec3[] back = new Vec3[1];
			world.getServer().runOnServer(server -> {
				ServerPlayer player = server.getPlayerList().getPlayers().get(0);
				EntityDragonBase frost = frostAt(server, c);
				back[0] = player.position();
				player.setGameMode(net.minecraft.world.level.GameType.SPECTATOR);
				double r = Math.toRadians(frost.getYRot());
				Vec3 forward = new Vec3(-Math.sin(r), 0, Math.cos(r));
				Vec3 side = new Vec3(-forward.z, 0, forward.x);
				Vec3 eye = frost.position().add(side.scale(30)).add(forward.scale(8)).add(0, 7, 0);
				float[] way = look(eye, frost.position().add(forward.scale(9)).add(0, 4, 0));
				player.teleportTo((ServerLevel) player.level(), eye.x, eye.y - 1.62, eye.z, Set.of(), way[0], way[1]);
			});
			context.waitTicks(2);
			context.takeScreenshot("frost_close_blow_side_" + k);
			world.getServer().runOnServer(server -> {
				ServerPlayer player = server.getPlayerList().getPlayers().get(0);
				player.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);
				player.teleportTo(back[0].x, back[0].y, back[0].z);
			});
			context.waitTicks(20);
		}
		world.getServer().runOnServer(server -> {
			ServerPlayer player = server.getPlayerList().getPlayers().get(0);
			player.getAbilities().mayfly = true;
			player.onUpdateAbilities();
		});
	}

	/** The way (yaw, pitch) from an eye to a place. */
	private static float[] look(Vec3 eye, Vec3 at) {
		Vec3 d = at.subtract(eye);
		return new float[]{(float) Math.toDegrees(Math.atan2(-d.x, d.z)), (float) -Math.toDegrees(Math.atan2(d.y, Math.sqrt(d.x * d.x + d.z * d.z)))};
	}

	/**
	 * Its breath on a player in its lane, in diamond armor: its super breath (near its end) without the pixie's gift
	 * (it melts them: a share of their life at each bite, cauterized) and with it (a cold blow their armor lessens, no
	 * wound); its jet tilted down the lane to the ground. Then its breath any time, from afar.
	 */
	private static void breath(ClientGameTestContext context, TestSingleplayerContext world, BlockPos c) {
		for (boolean gift : new boolean[]{false, true}) {
			float[] before = new float[1];
			world.getServer().runOnServer(server -> {
				ServerPlayer player = server.getPlayerList().getPlayers().get(0);
				EntityDragonBase frost = frostAt(server, c);
				BlackFrostFight fight = BlackFrostFight.of(frost);
				fight.stop();
				fight.rest(400);
				calm(server, c);
				frost.setHealth(frost.getMaxHealth() * 0.25F);
				frost.teleportTo(c.getX() + 0.5, c.getY(), c.getZ() + 0.5);
				frost.setYRot(0F);
				frost.yBodyRot = 0F;
				frost.yHeadRot = 0F;
				player.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);
				player.setInvulnerable(false);
				player.getAbilities().flying = false;
				player.onUpdateAbilities();
				player.teleportTo((ServerLevel) player.level(), c.getX() + 0.5, c.getY(), c.getZ() + 36.5, Set.of(), 180, 10);
				com.iafabric.boss.BossCold.cure(player);
				player.removeAllEffects();
				player.setHealth(player.getMaxHealth());
				for (var slot : new net.minecraft.world.entity.EquipmentSlot[]{net.minecraft.world.entity.EquipmentSlot.HEAD,
					net.minecraft.world.entity.EquipmentSlot.CHEST, net.minecraft.world.entity.EquipmentSlot.LEGS, net.minecraft.world.entity.EquipmentSlot.FEET}) {
					var item = switch (slot) {
						case HEAD -> net.minecraft.world.item.Items.DIAMOND_HELMET;
						case CHEST -> net.minecraft.world.item.Items.DIAMOND_CHESTPLATE;
						case LEGS -> net.minecraft.world.item.Items.DIAMOND_LEGGINGS;
						default -> net.minecraft.world.item.Items.DIAMOND_BOOTS;
					};
					player.setItemSlot(slot, new net.minecraft.world.item.ItemStack(item));
				}
				var data = com.iafabric.pixie.ExiledPixies.get(player);
				data.gifts = gift ? data.gifts | com.iafabric.pixie.ExiledPixieData.GIFT_ICE : data.gifts & ~com.iafabric.pixie.ExiledPixieData.GIFT_ICE;
				player.setAttached(com.iafabric.pixie.ExiledPixies.DATA, data);
				before[0] = player.getHealth();
				fight.ready(false);
				fight.cast(server.getLevel(ThroneFight.REALM), "super_breath");
			});
			context.waitTicks(30);
			context.takeScreenshot("frost_breath_lane" + (gift ? "_gift" : ""));
			StringBuilder timeline = new StringBuilder();
			float[] tilt = {Float.NaN};
			boolean[] wounded = new boolean[1];
			boolean[] frosted = new boolean[1];
			float[] least = {before[0]};
			for (int t = 0; t < 40; t += 4) {
				context.waitTicks(4);
				float[] now = new float[2];
				world.getServer().runOnServer(server -> {
					ServerPlayer player = server.getPlayerList().getPlayers().get(0);
					now[0] = player.getHealth();
					now[1] = com.iafabric.boss.BossCold.wounded(player) ? 1 : 0;
					wounded[0] |= now[1] > 0;
					frosted[0] |= player.getTicksFrozen() >= player.getTicksRequiredToFreeze();
					least[0] = Math.min(least[0], now[0]);
					for (var vfx : server.getLevel(ThroneFight.REALM).getEntitiesOfClass(com.iafabric.entity.EntityVfx.class, new AABB(c).inflate(60, 30, 60),
						e -> e.getKind() == com.iafabric.entity.EntityVfx.STREAM)) {
						tilt[0] = vfx.getXRot();
					}
					if (player.getHealth() < player.getMaxHealth() * 0.3F) {
						// (out of it, before it kills them: what it took so far tells enough)
						player.teleportTo(c.getX() + 30.5, c.getY(), c.getZ() + 30.5);
					}
				});
				timeline.append(String.format(Locale.ROOT, " %.1f%s", now[0], now[1] > 0 ? "*" : ""));
				if (t == 8) {
					context.takeScreenshot("frost_breath_jet" + (gift ? "_gift" : ""));
				}
			}
			float lost = before[0] - least[0];
			log((gift ? "with" : "without") + " the pixie's gift, in diamond armor, its super breath (its jet tilted " + tilt[0] + " degrees down): life " + before[0]
				+ " ->" + timeline + " (* cauterized), lost " + lost);
			if (!(tilt[0] > 3F)) {
				throw new AssertionError("its breath's jet is not tilted down the lane: " + tilt[0]);
			}
			if (!gift && (lost < before[0] * 0.3F || !wounded[0])) {
				throw new AssertionError("without the pixie's gift its breath did not melt the player: lost " + lost + ", cauterized " + wounded[0]);
			}
			log("its breath left the player frozen as the game shows it: " + frosted[0]);
			if (!frosted[0]) {
				throw new AssertionError("its breath did not freeze the player as the game shows it");
			}
			if (gift && (lost < before[0] * 0.2F || wounded[0])) {
				throw new AssertionError("with the pixie's gift its breath did not hurt hard as a cold blow: lost " + lost + ", cauterized " + wounded[0]);
			}
			context.waitTicks(40);
		}
		// its breath any time, from afar (its life high): the lane and the jet
		world.getServer().runOnServer(server -> {
			ServerPlayer player = server.getPlayerList().getPlayers().get(0);
			com.iafabric.boss.BossCold.cure(player);
			player.setHealth(player.getMaxHealth());
			player.setInvulnerable(true);
			EntityDragonBase frost = frostAt(server, c);
			frost.setHealth(frost.getMaxHealth() * 0.8F);
			BlackFrostFight fight = BlackFrostFight.of(frost);
			fight.stop();
			calm(server, c);
			frost.teleportTo(c.getX() + 0.5, c.getY(), c.getZ() + 0.5);
			player.teleportTo((ServerLevel) player.level(), c.getX() + 6.5, c.getY(), c.getZ() + 38.5, Set.of(), 170, 8);
			fight.ready(false);
			fight.cast(server.getLevel(ThroneFight.REALM), "frost_breath");
		});
		context.waitTicks(24);
		context.takeScreenshot("frost_breath_any_lane");
		context.waitTicks(12);
		context.takeScreenshot("frost_breath_any_jet");
		// the same jet from aside
		world.getServer().runOnServer(server -> {
			ServerPlayer player = server.getPlayerList().getPlayers().get(0);
			player.setGameMode(net.minecraft.world.level.GameType.SPECTATOR);
			Vec3 eye = new Vec3(c.getX() + 34.5, c.getY() + 9, c.getZ() + 26.5);
			float[] way = look(eye, new Vec3(c.getX() + 3.5, c.getY() + 3, c.getZ() + 26.5));
			player.teleportTo((ServerLevel) player.level(), eye.x, eye.y - 1.62, eye.z, Set.of(), way[0], way[1]);
			for (var vfx : server.getLevel(ThroneFight.REALM).getEntitiesOfClass(com.iafabric.entity.EntityVfx.class, new AABB(c).inflate(60, 30, 60),
				e -> e.getKind() == com.iafabric.entity.EntityVfx.STREAM)) {
				log("a jet at " + vfx.position() + ", turned " + vfx.getYRot() + ", tilted " + vfx.getXRot() + ", " + vfx.getLength() + " long, " + vfx.getSize() + " wide");
			}
		});
		context.waitTicks(3);
		context.takeScreenshot("frost_breath_any_side");
		world.getServer().runOnServer(server -> server.getPlayerList().getPlayers().get(0).setGameMode(net.minecraft.world.level.GameType.SURVIVAL));
		context.waitTicks(40);
	}

	/** Its charge: the lane, the rush. */
	private static void charge(ClientGameTestContext context, TestSingleplayerContext world, BlockPos c) {
		world.getServer().runOnServer(server -> {
			ServerPlayer player = server.getPlayerList().getPlayers().get(0);
			EntityDragonBase frost = frostAt(server, c);
			BlackFrostFight fight = BlackFrostFight.of(frost);
			fight.stop();
			calm(server, c);
			frost.teleportTo(c.getX() + 0.5, c.getY(), c.getZ() - 20.5);
			player.teleportTo(c.getX() + 0.5, c.getY(), c.getZ() + 12.5);
			fight.ready(false);
			fight.cast(server.getLevel(ThroneFight.REALM), "charge");
		});
		camera(context, world, c.getX() + 24.5, c.getY() + 12, c.getZ() + 2.5, 90, 22, 20);
		context.takeScreenshot("frost_charge_lane");
		context.waitTicks(25);
		context.takeScreenshot("frost_charge_rush");
		int[] shards = new int[1];
		world.getServer().runOnServer(server -> shards[0] = server.getLevel(ThroneFight.REALM).getEntitiesOfClass(net.minecraft.world.entity.Display.BlockDisplay.class,
			new AABB(c).inflate(20, 10, 80)).size());
		context.waitTicks(10);
		context.takeScreenshot("frost_charge_after");
		log("its charge: " + shards[0] + " shards of ice along its way");
		if (shards[0] < 6) {
			throw new AssertionError("its charge left no shards of ice along its way: " + shards[0]);
		}
		context.waitTicks(40);
	}

	/** Its hail: circles round the player where ice falls. */
	private static void hail(ClientGameTestContext context, TestSingleplayerContext world, BlockPos c) {
		world.getServer().runOnServer(server -> {
			ServerPlayer player = server.getPlayerList().getPlayers().get(0);
			EntityDragonBase frost = frostAt(server, c);
			BlackFrostFight fight = BlackFrostFight.of(frost);
			fight.stop();
			calm(server, c);
			frost.teleportTo(c.getX() + 0.5, c.getY(), c.getZ() - 20.5);
			player.getAbilities().flying = false;
			player.onUpdateAbilities();
			player.teleportTo(c.getX() + 0.5, c.getY(), c.getZ() + 20.5);
			fight.ready(false);
			fight.cast(server.getLevel(ThroneFight.REALM), "hail");
		});
		context.waitTicks(10);
		world.getServer().runOnServer(server -> {
			ServerPlayer player = server.getPlayerList().getPlayers().get(0);
			player.getAbilities().mayfly = true;
			player.getAbilities().flying = true;
			player.onUpdateAbilities();
			player.teleportTo((ServerLevel) player.level(), c.getX() + 0.5, c.getY() + 22, c.getZ() + 28.5, Set.of(), 180, 62);
		});
		context.runOnClient(mc -> mc.player.getAbilities().flying = true);
		context.waitTicks(6);
		context.takeScreenshot("frost_hail_circles");
		String[] rings = {""};
		world.getServer().runOnServer(server -> {
			List<Vec3> circles = new ArrayList<>();
			float size = 0;
			for (var vfx : server.getLevel(ThroneFight.REALM).getEntitiesOfClass(com.iafabric.entity.EntityVfx.class, new AABB(c.offset(0, 0, 20)).inflate(12, 6, 12),
				e -> e.getKind() == com.iafabric.entity.EntityVfx.CIRCLE && Math.abs(e.getSize() - 2.6F) < 0.01F)) {
				circles.add(vfx.position());
				size = vfx.getSize();
			}
			double closest = Double.MAX_VALUE;
			for (int i = 0; i < circles.size(); i++) {
				for (int j = i + 1; j < circles.size(); j++) {
					closest = Math.min(closest, Math.sqrt(circles.get(i).subtract(circles.get(j)).horizontalDistanceSqr()));
				}
			}
			rings[0] = circles.size() + " circles of " + size + ", the closest two " + String.format(Locale.ROOT, "%.2f", closest) + " apart";
			if (circles.size() < 8 || closest < size * 2 - 0.05) {
				rings[0] += " (BAD)";
			}
		});
		log("its hail's first wave: " + rings[0]);
		if (rings[0].endsWith("(BAD)")) {
			throw new AssertionError("its hail's circles are too few or one over another: " + rings[0]);
		}
		context.waitTicks(30);
		context.takeScreenshot("frost_hail_second_wave");
		context.waitTicks(30);
	}

	/** The altar: gone while the duel goes on; when it waits (its player fell), there again. */
	private static void altar(ClientGameTestContext context, TestSingleplayerContext world, BlockPos c) {
		camera(context, world, c.getX() + 6.5, c.getY() + 4, c.getZ() + 24.5, 150, 25, 10);
		context.takeScreenshot("frost_altar_in_duel");
		boolean[] there = new boolean[1];
		world.getServer().runOnServer(server -> there[0] = server.getLevel(ThroneFight.REALM).getBlockState(c.offset(0, 0, 17))
			.is(com.iafabric.block.IafBlockRegistry.THRONE_ALTAR));
		log("the altar while the duel goes on: " + (there[0] ? "there" : "gone"));
		if (there[0]) {
			throw new AssertionError("the altar stands in the duel");
		}
		world.getServer().runOnServer(server -> {
			ServerPlayer player = server.getPlayerList().getPlayers().get(0);
			player.setInvulnerable(false);
			player.hurt(player.damageSources().genericKill(), Float.MAX_VALUE);
		});
		context.waitTicks(20);
		int[] state = new int[1];
		world.getServer().runOnServer(server -> {
			state[0] = ThroneFight.stateAt(c);
			there[0] = server.getLevel(ThroneFight.REALM).getBlockState(c.offset(0, 0, 17)).is(com.iafabric.block.IafBlockRegistry.THRONE_ALTAR);
		});
		log("the player fell: the duel " + (state[0] == 4 ? "waits" : "state " + state[0]) + ", the altar " + (there[0] ? "there again" : "gone"));
		if (state[0] != 4 || !there[0]) {
			throw new AssertionError("the duel waits without its altar: state " + state[0] + ", altar " + there[0]);
		}
		context.runOnClient(mc -> {
			if (mc.player != null) {
				mc.player.respawn();
			}
		});
		context.waitTicks(30);
		context.runOnClient(mc -> mc.setScreen(null));
	}

	/** Cryona on its back rests a while (her spells would cross what is looked at). */
	private static void calm(MinecraftServer server, BlockPos c) {
		for (EntityDreadQueen queen : server.getLevel(ThroneFight.REALM).getEntitiesOfClass(EntityDreadQueen.class, new AABB(c).inflate(140, 80, 140))) {
			com.iafabric.boss.CryonaFight.of(queen).rest(600);
		}
	}

	private static EntityDragonBase frostAt(MinecraftServer server, BlockPos c) {
		var realm = server.getLevel(ThroneFight.REALM);
		return realm.getEntitiesOfClass(EntityDragonBase.class, new AABB(c).inflate(140, 80, 140), EntityDragonBase::isBlackFrost).get(0);
	}

	private static void camera(ClientGameTestContext context, TestSingleplayerContext world, double x, double y, double z, float yaw, float pitch, int wait) {
		world.getServer().runOnServer(server -> {
			ServerPlayer player = server.getPlayerList().getPlayers().get(0);
			player.getAbilities().mayfly = true;
			player.getAbilities().flying = true;
			player.onUpdateAbilities();
			player.teleportTo((ServerLevel) player.level(), x, y, z, Set.of(), yaw, pitch);
		});
		context.runOnClient(mc -> mc.player.getAbilities().flying = true);
		context.waitTicks(wait);
	}
}

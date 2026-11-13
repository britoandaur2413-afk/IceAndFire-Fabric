package com.iafabric.test;

import com.iafabric.IafMod;
import com.iafabric.boss.BlackFrostFight;
import com.iafabric.boss.CryonaFight;
import com.iafabric.boss.Soundtracks;
import com.iafabric.boss.ThroneFight;
import com.iafabric.client.sound.Soundtrack;
import com.iafabric.entity.EntityDragonBase;
import com.iafabric.entity.EntityDreadMob;
import com.iafabric.entity.EntityDreadQueen;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.client.gui.screens.worldselection.WorldCreationUiState;
import net.minecraft.client.sounds.SoundEventListener;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Cryona's duel as the player lives it (IAF_ONLY=cryonafight; IAF_CRYONA=music,pylons,mourning,beams,ring,rays,sounds,fall
 * for some of it): its music (the duel's, the pixie's memories while she mourns, none once the player falls; never the
 * game's own over it), Black Frost's pylons (from three quarters of its life: struck three times, its life drops), her
 * mourning (she kneels, nothing touches her, the dead rise, rays of ice fall, thunder far off, her sobs; then she rises),
 * her beams in all directions, her ring of ice with its warm gaps, her rays in her vengeance, and no sound over and over.
 */
public class IafCryonaFightTests implements FabricClientGameTest {
	private static final int GROUND = -60;
	/** What the player's game played (its game time, the sound), the whole scene long. */
	private static final List<Object[]> HEARD = java.util.Collections.synchronizedList(new ArrayList<>());

	private static void log(String message) {
		IafMod.LOGGER.info("[IAFCRYONA] {}", message);
	}

	@Override
	public void runTest(ClientGameTestContext context) {
		if (System.getenv("IAF_BATTLE") != null || IafTestOnly.skip("cryonafight")) {
			return;
		}
		try (TestSingleplayerContext world = context.worldBuilder().adjustSettings(settings -> {
			settings.setGameMode(WorldCreationUiState.SelectedGameMode.SURVIVAL);
			settings.setAllowCommands(true);
		}).create()) {
			world.getServer().runCommand("gamerule advance_time false");
			world.getServer().runCommand("gamerule spawn_mobs false");
			world.getServer().runCommand("time set noon");
			world.getServer().runCommand("weather clear");
			context.runOnClient(mc -> {
				if (!mc.gui.hud.isHidden()) {
					mc.gui.hud.toggle();
				}
			});
			SoundEventListener listener = (sound, events, range) -> {
				long time = net.minecraft.client.Minecraft.getInstance().level == null ? 0 : net.minecraft.client.Minecraft.getInstance().level.getGameTime();
				HEARD.add(new Object[]{time, sound.getIdentifier().toString()});
			};
			context.runOnClient(mc -> mc.getSoundManager().addListener(listener));
			// (the music heard for real: the test game has its music turned off)
			double music = context.computeOnClient(mc -> mc.options.getSoundSourceOptionInstance(net.minecraft.sounds.SoundSource.MUSIC).get());
			context.runOnClient(mc -> {
				mc.options.getSoundSourceOptionInstance(net.minecraft.sounds.SoundSource.MUSIC).set(1.0);
				mc.getMusicManager().stopPlaying();
			});
			try {
				scene(context, world);
			} finally {
				context.runOnClient(mc -> {
					mc.getSoundManager().removeListener(listener);
					mc.options.getSoundSourceOptionInstance(net.minecraft.sounds.SoundSource.MUSIC).set(music);
				});
			}
		}
	}

	private void scene(ClientGameTestContext context, TestSingleplayerContext world) {
		{
			BlockPos c = duel(context, world);
			int from = heard(context).size();
			int said = pixie(context, world);
			if (step("music")) {
				music(context);
			}
			if (step("pylons")) {
				pylons(context, world, c);
			}
			if (step("mourning")) {
				mourning(context, world, c);
			}
			if (step("beams")) {
				beams(context, world, c);
			}
			if (step("ring")) {
				ring(context, world, c);
			}
			if (step("rays")) {
				rays(context, world, c);
			}
			if (step("sounds")) {
				sounds(context, world, c);
			}
			// (the game's own music may come back once the player fell and is home: looked for till then)
			List<Object[]> all = heard(context);
			pixieSaid(context, said);
			if (step("fall")) {
				fall(context, world, c);
			}
			List<String> vanilla = new ArrayList<>();
			for (Object[] h : all.subList(Math.min(from, all.size()), all.size())) {
				if (((String) h[1]).startsWith("minecraft:music")) {
					vanilla.add((String) h[1]);
				}
			}
			log("the game's own music heard in the duel: " + vanilla);
			if (!vanilla.isEmpty()) {
				throw new AssertionError("the game's own music played over the duel's: " + vanilla);
			}
		}
	}

	/** What the player's game played so far (a copy, taken on its own thread). */
	private static List<Object[]> heard(ClientGameTestContext context) {
		return context.computeOnClient(mc -> {
			synchronized (HEARD) {
				return new ArrayList<>(HEARD);
			}
		});
	}

	/** Whether to run a part of the scene (IAF_CRYONA=music,rays... runs only those; all by default). */
	private static boolean step(String name) {
		String only = System.getenv("IAF_CRYONA");
		return only == null || only.isBlank() || java.util.Arrays.asList(only.split(",")).contains(name);
	}

	/** Her sign by the player, her call, the duel accepted with blood: the arena's middle. */
	private static BlockPos duel(ClientGameTestContext context, TestSingleplayerContext world) {
		world.getServer().runOnServer(server -> {
			ServerPlayer player = server.getPlayerList().getPlayers().get(0);
			player.setPermanentlyInvulnerable(true);
			var level = server.overworld();
			player.teleportTo(level, 0.5, GROUND, 152.5, Set.of(), 0, 5, false);
			var frost = com.iafabric.entity.EntityBlackFrostGuard.make(level, level.getRandom(),
				new BlockPos(0, GROUND, 158 + com.iafabric.world.structure.CitadelPiece.GUARD_BEHIND), 180F);
			level.addFreshEntity(frost);
			var queen = com.iafabric.entity.EntityCryonaAsleep.make(level, new BlockPos(0, GROUND, 158),
				new BlockPos(0, GROUND, 158 + com.iafabric.world.structure.CitadelPiece.GUARD_BEHIND), 0.5, GROUND, 158.5, 180F, EntitySpawnReason.COMMAND);
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
		int before = context.computeOnClient(mc -> Soundtrack.playing());
		if (before != Soundtracks.NONE) {
			throw new AssertionError("a soundtrack plays before the duel begins: " + before);
		}
		world.getServer().runCommand("give @p iceandfire_fabric:ice_dragon_blood 2");
		world.getServer().runCommand("execute as @p run iceandfire duel accept");
		context.waitTicks(20);
		int state = world.getServer().computeOnServer(server -> ThroneFight.stateAt(arena[0]));
		log("the duel at " + arena[0].toShortString() + ", state " + state);
		if (state != 1) {
			throw new AssertionError("the duel did not begin: state " + state);
		}
		return arena[0];
	}

	/** Her exiled pixie with the player in the duel (her quests all done: she has nothing else to tell). */
	private static int pixie(ClientGameTestContext context, TestSingleplayerContext world) {
		world.getServer().runOnServer(server -> com.iafabric.pixie.ExiledPixies.completeAll(server.getPlayerList().getPlayers().get(0)));
		// (what the command itself makes her say, said)
		context.waitTicks(140);
		boolean there = world.getServer().computeOnServer(server -> com.iafabric.pixie.ExiledPixies.living(server.getPlayerList().getPlayers().get(0)) != null);
		log("her exiled pixie with the player in the duel: " + there);
		if (!there) {
			throw new AssertionError("the exiled pixie is not with the player in the duel");
		}
		return context.computeOnClient(mc -> com.iafabric.client.gui.ExiledPixieHud.heard());
	}

	/** What she said in the duel (her lines since from, of all she said): only of the fight. */
	private static void pixieSaid(ClientGameTestContext context, int from) {
		List<String> keys = context.computeOnClient(mc -> {
			List<String> out = new ArrayList<>();
			List<net.minecraft.network.chat.Component> said = com.iafabric.client.gui.ExiledPixieHud.recent();
			int fresh = Math.min(said.size(), com.iafabric.client.gui.ExiledPixieHud.heard() - from);
			for (net.minecraft.network.chat.Component line : said.subList(said.size() - fresh, said.size())) {
				out.add(line.getContents() instanceof net.minecraft.network.chat.contents.TranslatableContents translatable ? translatable.getKey() : line.getString());
			}
			return out;
		});
		log("her pixie said in the duel: " + keys);
		long off = keys.stream().filter(key -> !key.startsWith("exiled.iceandfire_fabric.duel.")).count();
		if (off > 0 || keys.size() < 3) {
			throw new AssertionError("her pixie spoke of other things in the duel, or hardly of it: " + keys);
		}
	}

	/** The duel's music as it begins (its fade in), the game's own music held off. */
	private static void music(ClientGameTestContext context) {
		int track = Soundtracks.NONE;
		for (int i = 0; i < 60 && track != Soundtracks.DUEL; i++) {
			context.waitTicks(1);
			track = context.computeOnClient(mc -> Soundtrack.audible() ? Soundtrack.playing() : Soundtracks.NONE);
		}
		log("the duel begun: its soundtrack " + track + " (1 the duel's), heard");
		if (track != Soundtracks.DUEL) {
			throw new AssertionError("the duel's music does not play: " + track);
		}
	}

	/** Its pylons at three quarters of its life: their lights to the sky; three blows break one, and its life drops. */
	private static void pylons(ClientGameTestContext context, TestSingleplayerContext world, BlockPos c) {
		List<Vec3> spots = new ArrayList<>();
		world.getServer().runOnServer(server -> {
			EntityDragonBase frost = frostAt(server, c);
			BlackFrostFight fight = BlackFrostFight.of(frost);
			fight.stop();
			fight.rest(900);
			calm(server, c);
			frost.setHealth(frost.getMaxHealth() * 0.7F);
			frost.teleportTo(c.getX() + 0.5, c.getY(), c.getZ() + 0.5);
			spots.addAll(fight.pylonsNow(server.getLevel(ThroneFight.REALM)));
		});
		log("its pylons at 70% of its life: " + spots);
		if (spots.size() < 2) {
			throw new AssertionError("its pylons did not rise: " + spots);
		}
		context.waitTicks(20);
		Vec3 first = spots.get(0);
		Vec3 in = new Vec3(c.getX() + 0.5 - first.x, 0, c.getZ() + 0.5 - first.z).normalize();
		Vec3 side = new Vec3(-in.z, 0, in.x);
		Vec3 high = first.add(in.scale(14)).add(side.scale(6)).add(0, 6, 0);
		float[] way = look(high, first.add(0, 4, 0));
		camera(context, world, high.x, high.y - 1.62, high.z, way[0], way[1], 10);
		context.takeScreenshot("cryona_pylons");
		Vec3 wide = new Vec3(c.getX() + 0.5, c.getY() + 40, c.getZ() + 0.5).subtract(in.scale(30));
		way = look(wide, new Vec3(c.getX() + 0.5, c.getY(), c.getZ() + 0.5));
		camera(context, world, wide.x, wide.y - 1.62, wide.z, way[0], way[1], 6);
		context.takeScreenshot("cryona_pylons_all");
		// by the first, three blows
		float[] health = new float[2];
		int[] box = {-1};
		world.getServer().runOnServer(server -> {
			ServerPlayer player = server.getPlayerList().getPlayers().get(0);
			EntityDragonBase frost = frostAt(server, c);
			Entity pylon = BlackFrostFight.of(frost).pylonNear(first);
			box[0] = pylon == null ? -1 : pylon.getId();
			Vec3 at = first.add(in.scale(7)).add(side.scale(2));
			float[] look = look(at.add(0, 1.62, 0), first.add(0, 3, 0));
			player.getAbilities().flying = false;
			player.getAbilities().mayfly = false;
			player.onUpdateAbilities();
			player.teleportTo((ServerLevel) player.level(), at.x, at.y, at.z, Set.of(), look[0], look[1], false);
			health[0] = frost.getHealth();
		});
		context.waitTicks(15);
		context.takeScreenshot("cryona_pylon_close");
		world.getServer().runOnServer(server -> {
			ServerPlayer player = server.getPlayerList().getPlayers().get(0);
			Vec3 at = first.add(in.scale(2.4));
			float[] look = look(at.add(0, 1.62, 0), first.add(0, 2.5, 0));
			player.teleportTo((ServerLevel) player.level(), at.x, at.y, at.z, Set.of(), look[0], look[1], false);
		});
		context.waitTicks(5);
		for (int k = 0; k < 3; k++) {
			context.runOnClient(mc -> {
				Entity entity = mc.level.getEntity(box[0]);
				if (entity != null) {
					mc.gameMode.attack(mc.player, entity);
				}
			});
			context.waitTicks(8);
			if (k == 1) {
				context.takeScreenshot("cryona_pylon_cracked");
			}
		}
		context.waitTicks(2);
		context.takeScreenshot("cryona_pylon_breaking");
		context.waitTicks(14);
		int[] left = new int[1];
		world.getServer().runOnServer(server -> {
			EntityDragonBase frost = frostAt(server, c);
			health[1] = frost.getHealth();
			left[0] = server.getLevel(ThroneFight.REALM).getEntitiesOfClass(net.minecraft.world.entity.Interaction.class, new AABB(c).inflate(140, 40, 140),
				e -> e.entityTags().contains(BlackFrostFight.PYLON_TAG)).size();
		});
		float max = world.getServer().computeOnServer(server -> frostAt(server, c).getMaxHealth());
		log(String.format(Locale.ROOT, "three blows on a pylon: its life %.0f -> %.0f (%.1f%% of it), pylons left %d", health[0], health[1],
			(health[0] - health[1]) / max * 100, left[0]));
		if (health[0] - health[1] < max * 0.045F || left[0] != spots.size() - 1) {
			throw new AssertionError("a pylon broken did not weaken Black Frost: " + health[0] + " -> " + health[1] + ", pylons left " + left[0]);
		}
	}

	/**
	 * Black Frost falls: the duel's music fades and the pixie's memories begin; Cryona kneels, nothing touches her; the
	 * dead rise round the player, rays of ice fall, thunder far off; her sobs; then (hurried) she rises, her shield whole,
	 * her dead gone, the duel's music again.
	 */
	/** Her fury's bar as the player's game shows it: how full, and 1 if red (null if there is none). */
	private static float[] furyBar(net.minecraft.client.Minecraft mc) {
		try {
			java.lang.reflect.Field field = net.minecraft.client.gui.components.BossHealthOverlay.class.getDeclaredField("events");
			field.setAccessible(true);
			for (Object bar : ((Map<?, ?>) field.get(mc.gui.hud.getBossOverlay())).values()) {
				net.minecraft.client.gui.components.LerpingBossEvent event = (net.minecraft.client.gui.components.LerpingBossEvent) bar;
				if (event.getName().getContents() instanceof net.minecraft.network.chat.contents.TranslatableContents text
					&& text.getKey().equals("iceandfire_fabric.boss.cryona_fury")) {
					return new float[]{event.getProgress(), event.getColor() == net.minecraft.world.BossEvent.BossBarColor.RED ? 1 : 0};
				}
			}
		} catch (ReflectiveOperationException e) {
			throw new RuntimeException(e);
		}
		return null;
	}

	private static void mourning(ClientGameTestContext context, TestSingleplayerContext world, BlockPos c) {
		long since = context.computeOnClient(mc -> mc.level.getGameTime());
		world.getServer().runOnServer(server -> {
			ServerPlayer player = server.getPlayerList().getPlayers().get(0);
			player.setPermanentlyInvulnerable(true);
			player.getAbilities().flying = false;
			player.getAbilities().mayfly = false;
			player.onUpdateAbilities();
			player.teleportTo(c.getX() + 0.5, c.getY(), c.getZ() + 24.5);
			EntityDragonBase frost = frostAt(server, c);
			BlackFrostFight.of(frost).stop();
			frost.hurtServer(server.getLevel(ThroneFight.REALM), server.getLevel(ThroneFight.REALM).damageSources().genericKill(), Float.MAX_VALUE);
		});
		context.waitTicks(15);
		int phase = world.getServer().computeOnServer(server -> queenAt(server, c).getPhase());
		log("Black Frost fallen: Cryona's phase " + phase + " (2 mourning)");
		if (phase != CryonaFight.FALLING) {
			throw new AssertionError("Cryona does not mourn Black Frost: phase " + phase);
		}
		int track = Soundtracks.NONE;
		for (int i = 0; i < 160 && track != Soundtracks.MEMORIES; i++) {
			context.waitTicks(1);
			track = context.computeOnClient(mc -> Soundtrack.audible() ? Soundtrack.playing() : Soundtracks.NONE);
		}
		int scene = world.getServer().computeOnServer(server -> CryonaFight.of(queenAt(server, c)).scene());
		log("her mourning at " + scene + ": the soundtrack " + track + " (2 the pixie's memories)");
		if (track != Soundtracks.MEMORIES) {
			throw new AssertionError("the pixie's memories do not play as she mourns: " + track);
		}
		// she kneels, nothing touches her
		waitScene(context, world, c, 130);
		boolean[] hurt = new boolean[1];
		world.getServer().runOnServer(server -> {
			ServerPlayer player = server.getPlayerList().getPlayers().get(0);
			EntityDreadQueen queen = queenAt(server, c);
			float before = queen.getHealth();
			hurt[0] = queen.hurtServer((ServerLevel) queen.level(), queen.damageSources().playerAttack(player), 50F) || queen.getHealth() < before;
			double r = Math.toRadians(queen.yBodyRot + 40F);
			Vec3 eye = queen.position().add(-Math.sin(r) * 5.5, 1.9, Math.cos(r) * 5.5);
			float[] way = look(eye, queen.position().add(0, 1, 0));
			player.getAbilities().mayfly = true;
			player.getAbilities().flying = true;
			player.onUpdateAbilities();
			player.teleportTo((ServerLevel) player.level(), eye.x, eye.y - 1.62, eye.z, Set.of(), way[0], way[1], false);
		});
		context.runOnClient(mc -> mc.player.getAbilities().flying = true);
		if (hurt[0]) {
			throw new AssertionError("Cryona was hurt as she mourns");
		}
		float[] fury = context.computeOnClient(IafCryonaFightTests::furyBar);
		context.waitTicks(8);
		context.takeScreenshot("cryona_mourning_kneel");
		// the dead rise round the player
		waitScene(context, world, c, 262);
		world.getServer().runOnServer(server -> {
			ServerPlayer player = server.getPlayerList().getPlayers().get(0);
			EntityDreadQueen queen = queenAt(server, c);
			Vec3 eye = player.position().add(0, 14, 9);
			float[] way = look(eye, player.position());
			player.teleportTo((ServerLevel) player.level(), eye.x, eye.y - 1.62, eye.z, Set.of(), way[0], way[1], false);
		});
		context.waitTicks(4);
		int dead = world.getServer().computeOnServer(server -> mine(server, c).size());
		log("her mourning at 266: " + dead + " of the dead risen");
		context.takeScreenshot("cryona_mourning_dead");
		if (dead < 5) {
			throw new AssertionError("no horde of the dead rose in her mourning: " + dead);
		}
		float[] later = context.computeOnClient(IafCryonaFightTests::furyBar);
		log(String.format(Locale.ROOT, "her bar as she mourns (the player's game): %s, red %s, %.2f at 130 -> %.2f at 266",
			fury == null ? "none" : "her fury", fury != null && fury[1] > 0, fury == null ? -1 : fury[0], later == null ? -1 : later[0]));
		if (fury == null || later == null || fury[1] <= 0 || later[0] <= fury[0]) {
			throw new AssertionError("her bar as she mourns is not her fury, red and filling");
		}
		// her rays of ice (round where the player is when they come: seen from aside on the ice)
		waitScene(context, world, c, 366);
		world.getServer().runOnServer(server -> {
			ServerPlayer player = server.getPlayerList().getPlayers().get(0);
			Vec3 ground = new Vec3(player.getX(), c.getY(), player.getZ());
			Vec3 eye = ground.add(15, 3.5, 9);
			float[] way = look(eye, ground.add(0, 5, 0));
			player.teleportTo((ServerLevel) player.level(), eye.x, eye.y - 1.62, eye.z, Set.of(), way[0], way[1], false);
		});
		waitScene(context, world, c, 384);
		context.takeScreenshot("cryona_mourning_rays_circles");
		waitScene(context, world, c, 396);
		context.takeScreenshot("cryona_mourning_rays_fall");
		waitScene(context, world, c, 406);
		context.takeScreenshot("cryona_mourning_rays_ring");
		int[] columns = new int[1];
		world.getServer().runOnServer(server -> columns[0] = server.getLevel(ThroneFight.REALM).getEntitiesOfClass(com.iafabric.entity.EntityVfx.class,
			new AABB(c).inflate(140, 40, 140), e -> e.getKind() == com.iafabric.entity.EntityVfx.BEAM && e.getSize() > 1F).size());
		log("her rays falling: " + columns[0] + " columns of ice light");
		if (columns[0] < 2) {
			throw new AssertionError("her rays of ice did not fall: " + columns[0]);
		}
		// thunder far off, now and then (looked for till her second sob)
		double[] bolt = {-1};
		for (int i = 0; i < 400 && bolt[0] < 0; i++) {
			context.waitTicks(1);
			world.getServer().runOnServer(server -> {
				ServerPlayer player = server.getPlayerList().getPlayers().get(0);
				for (var lightning : server.getLevel(ThroneFight.REALM).getEntitiesOfClass(net.minecraft.world.entity.LightningBolt.class, new AABB(c).inflate(160, 60, 160))) {
					bolt[0] = Math.sqrt(lightning.position().subtract(player.position()).horizontalDistanceSqr());
					float[] way = look(player.getEyePosition(), lightning.position().add(0, 12, 0));
					player.teleportTo((ServerLevel) player.level(), player.getX(), player.getY(), player.getZ(), Set.of(), way[0], way[1], false);
				}
			});
		}
		if (bolt[0] >= 0) {
			context.waitTicks(1);
			context.takeScreenshot("cryona_mourning_thunder");
		}
		log("thunder far off in her mourning: " + (bolt[0] < 0 ? "none" : String.format(Locale.ROOT, "%.0f blocks from the player", bolt[0])));
		if (bolt[0] >= 0 && bolt[0] < 25) {
			throw new AssertionError("thunder fell near the player: " + bolt[0]);
		}
		waitScene(context, world, c, 720);
		List<String> voice = new ArrayList<>();
		for (Object[] h : heard(context)) {
			if ((long) h[0] >= since && ((String) h[1]).startsWith("iceandfire_fabric:cryona_")) {
				voice.add((String) h[1]);
			}
		}
		log("her voice as she mourns: " + voice);
		if (!voice.contains("iceandfire_fabric:cryona_lament") || !voice.contains("iceandfire_fabric:cryona_sob1") || !voice.contains("iceandfire_fabric:cryona_sob2")) {
			throw new AssertionError("her lament and sobs were not heard: " + voice);
		}
		// hurried to her rising
		world.getServer().runOnServer(server -> CryonaFight.of(queenAt(server, c)).hurry(60));
		context.waitTicks(40);
		world.getServer().runOnServer(server -> {
			ServerPlayer player = server.getPlayerList().getPlayers().get(0);
			EntityDreadQueen queen = queenAt(server, c);
			double r = Math.toRadians(queen.yBodyRot);
			Vec3 eye = queen.position().add(-Math.sin(r) * 9, 2.5, Math.cos(r) * 9);
			float[] way = look(eye, queen.position().add(0, 1.2, 0));
			player.teleportTo((ServerLevel) player.level(), eye.x, eye.y - 1.62, eye.z, Set.of(), way[0], way[1], false);
		});
		context.waitTicks(25);
		context.takeScreenshot("cryona_mourning_rises");
		track = Soundtracks.NONE;
		for (int i = 0; i < 120 && track != Soundtracks.DUEL; i++) {
			context.waitTicks(1);
			track = context.computeOnClient(mc -> Soundtrack.audible() ? Soundtrack.playing() : Soundtracks.NONE);
		}
		Object[] after = world.getServer().computeOnServer(server -> {
			EntityDreadQueen queen = queenAt(server, c);
			return new Object[]{queen.getPhase(), queen.getShield(), mine(server, c).size()};
		});
		log("she rose: phase " + after[0] + " (3 alone), her shield " + after[1] + ", her dead left " + after[2] + ", the soundtrack " + track);
		if ((int) after[0] != CryonaFight.ALONE || (float) after[1] < CryonaFight.SHIELD_MAX || (int) after[2] != 0 || track != Soundtracks.DUEL) {
			throw new AssertionError("her rising went wrong: phase " + after[0] + ", shield " + after[1] + ", dead " + after[2] + ", soundtrack " + track);
		}
		context.takeScreenshot("cryona_risen");
	}

	/** Her beams in all directions: the six lanes, the first beams, the six between them. */
	private static void beams(ClientGameTestContext context, TestSingleplayerContext world, BlockPos c) {
		alone(context, world, c);
		setUp(world, c, "beams");
		context.waitTicks(16);
		overHer(context, world, c);
		context.takeScreenshot("cryona_beams_lanes");
		context.waitTicks(17);
		context.takeScreenshot("cryona_beams_first");
		context.waitTicks(20);
		context.takeScreenshot("cryona_beams_second");
		calmHer(world, c);
		context.waitTicks(30);
	}

	/** Her ring of ice: the warm lights in its gaps, then the ring running out. */
	private static void ring(ClientGameTestContext context, TestSingleplayerContext world, BlockPos c) {
		alone(context, world, c);
		setUp(world, c, "ring");
		context.waitTicks(10);
		overHer(context, world, c);
		context.takeScreenshot("cryona_ring_gaps");
		context.waitTicks(22);
		context.takeScreenshot("cryona_ring_running");
		calmHer(world, c);
		context.waitTicks(30);
	}

	/** Her rays in her vengeance: the circles and the thin lights on them, then the rays. */
	private static void rays(ClientGameTestContext context, TestSingleplayerContext world, BlockPos c) {
		alone(context, world, c);
		setUp(world, c, "rays");
		world.getServer().runOnServer(server -> CryonaFight.of(queenAt(server, c)).rest(300));
		context.waitTicks(4);
		world.getServer().runOnServer(server -> {
			ServerPlayer player = server.getPlayerList().getPlayers().get(0);
			Vec3 eye = player.position().add(0, 9, 14);
			float[] way = look(eye, player.position());
			player.getAbilities().mayfly = true;
			player.getAbilities().flying = true;
			player.onUpdateAbilities();
			player.teleportTo((ServerLevel) player.level(), eye.x, eye.y - 1.62, eye.z, Set.of(), way[0], way[1], false);
		});
		context.runOnClient(mc -> mc.player.getAbilities().flying = true);
		context.waitTicks(16);
		context.takeScreenshot("cryona_rays_circles");
		context.waitTicks(16);
		context.takeScreenshot("cryona_rays_fall");
		calmHer(world, c);
		context.waitTicks(30);
	}

	/**
	 * Twenty seconds of her vengeance, the player striking her as fast as they can at her side while she casts: no sound
	 * over and over (her shield's ring, ice breaking, her spells, her voice).
	 */
	private static void sounds(ClientGameTestContext context, TestSingleplayerContext world, BlockPos c) {
		alone(context, world, c);
		long since = context.computeOnClient(mc -> mc.level.getGameTime());
		world.getServer().runOnServer(server -> {
			ServerPlayer player = server.getPlayerList().getPlayers().get(0);
			EntityDreadQueen queen = queenAt(server, c);
			CryonaFight.of(queen).stop();
			queen.setShield(CryonaFight.SHIELD_MAX);
			player.getAbilities().flying = false;
			player.getAbilities().mayfly = false;
			player.onUpdateAbilities();
			player.setPermanentlyInvulnerable(false);
			player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.DIAMOND_SWORD));
			player.teleportTo(queen.getX() + 2.5, queen.getY(), queen.getZ());
		});
		String[] powers = {"spikes", "combo", "beams", "ring", "rays"};
		for (int i = 0; i < 400; i++) {
			context.waitTicks(1);
			int n = i;
			world.getServer().runOnServer(server -> {
				ServerPlayer player = server.getPlayerList().getPlayers().get(0);
				EntityDreadQueen queen = queenAt(server, c);
				com.iafabric.boss.BossCold.cure(player);
				player.setHealth(player.getMaxHealth());
				if (n % 4 == 0 && player.distanceTo(queen) < 5) {
					player.attack(queen);
					player.resetAttackStrengthTicker();
				} else if (n % 20 == 0) {
					player.teleportTo(queen.getX() + 2.5, queen.getY(), queen.getZ());
				}
				if (n % 80 == 10) {
					CryonaFight fight = CryonaFight.of(queen);
					fight.stop();
					fight.cast((ServerLevel) queen.level(), powers[n / 80]);
				}
			});
		}
		Map<String, int[]> perSecond = new HashMap<>();
		Map<String, Integer> total = new HashMap<>();
		for (Object[] h : heard(context)) {
			long t = (long) h[0] - since;
			if (t < 0) {
				continue;
			}
			String id = (String) h[1];
			total.merge(id, 1, Integer::sum);
			perSecond.computeIfAbsent(id, k -> new int[30])[(int) Math.min(29, t / 20)]++;
		}
		List<Map.Entry<String, Integer>> sorted = new ArrayList<>(total.entrySet());
		sorted.sort((a, b) -> b.getValue() - a.getValue());
		StringBuilder out = new StringBuilder();
		Map<String, Integer> most = new HashMap<>();
		for (Map.Entry<String, Integer> entry : sorted) {
			int max = 0;
			for (int count : perSecond.get(entry.getKey())) {
				max = Math.max(max, count);
			}
			most.put(entry.getKey(), max);
			out.append(String.format(Locale.ROOT, "%n  %s x%d (at most %d in a second)", entry.getKey(), entry.getValue(), max));
		}
		log("sounds heard in twenty seconds of her vengeance (struck at her side as fast as can be):" + out);
		Map<String, Integer> limits = Map.of("minecraft:block.glass.hit", 3, "minecraft:block.glass.break", 4, "minecraft:entity.evoker.prepare_attack", 1,
			"iceandfire_fabric:cryona_ice_ray", 2, "iceandfire_fabric:cryona_shield_up", 1, "minecraft:entity.player.hurt", 4);
		for (Map.Entry<String, Integer> limit : limits.entrySet()) {
			if (most.getOrDefault(limit.getKey(), 0) > limit.getValue()) {
				throw new AssertionError(limit.getKey() + " heard " + most.get(limit.getKey()) + " times in a second: a din");
			}
		}
		for (Map.Entry<String, Integer> entry : most.entrySet()) {
			if (entry.getKey().startsWith("iceandfire_fabric:cryona_") && !entry.getKey().equals("iceandfire_fabric:cryona_ice_ray") && entry.getValue() > 1) {
				throw new AssertionError("her voice " + entry.getKey() + " over and over: " + entry.getValue() + " in a second");
			}
		}
		world.getServer().runOnServer(server -> server.getPlayerList().getPlayers().get(0).setPermanentlyInvulnerable(true));
		calmHer(world, c);
		context.waitTicks(20);
	}

	/** The player falls in the duel: its music is gone at once (fading), and stays gone after they come back to life. */
	private static void fall(ClientGameTestContext context, TestSingleplayerContext world, BlockPos c) {
		world.getServer().runOnServer(server -> {
			ServerPlayer player = server.getPlayerList().getPlayers().get(0);
			player.setPermanentlyInvulnerable(false);
			player.hurtServer((ServerLevel) player.level(), player.damageSources().genericKill(), Float.MAX_VALUE);
		});
		for (int i = 0; i < 6; i++) {
			context.waitTicks(5);
			log("the player fell, " + (i * 5 + 5) + " ticks: " + context.computeOnClient(mc -> Soundtrack.describe()));
		}
		boolean playing = context.computeOnClient(mc -> Soundtrack.playing() != Soundtracks.NONE || Soundtrack.active());
		int told = world.getServer().computeOnServer(server -> Soundtracks.told(server.getPlayerList().getPlayers().get(0)));
		log("the player fell: a soundtrack still " + (playing ? "plays" : "silent") + ", told " + told);
		if (playing || told != Soundtracks.NONE) {
			throw new AssertionError("the duel's music goes on after the player fell");
		}
		context.runOnClient(mc -> {
			if (mc.player != null) {
				mc.player.respawn();
			}
		});
		context.waitTicks(60);
		context.runOnClient(mc -> mc.gui.setScreen(null));
		playing = context.computeOnClient(mc -> Soundtrack.playing() != Soundtracks.NONE || Soundtrack.active());
		log("back to life at home: a soundtrack " + (playing ? "plays" : "silent"));
		if (playing) {
			throw new AssertionError("the duel's music plays again after the player came back to life");
		}
	}

	// ---------------------------------------------------------------------------------------------------------------

	/** Cryona alone (Black Frost fallen, her mourning hurried to its end) if she is not yet. */
	private static void alone(ClientGameTestContext context, TestSingleplayerContext world, BlockPos c) {
		int phase = world.getServer().computeOnServer(server -> queenAt(server, c).getPhase());
		if (phase == CryonaFight.ALONE) {
			return;
		}
		world.getServer().runOnServer(server -> {
			for (EntityDragonBase frost : server.getLevel(ThroneFight.REALM).getEntitiesOfClass(EntityDragonBase.class, new AABB(c).inflate(140, 80, 140),
				d -> d.isBlackFrost() && !d.isModelDead())) {
				frost.hurtServer(server.getLevel(ThroneFight.REALM), server.getLevel(ThroneFight.REALM).damageSources().genericKill(), Float.MAX_VALUE);
			}
		});
		for (int i = 0; i < 600 && phase != CryonaFight.ALONE; i++) {
			context.waitTicks(1);
			phase = world.getServer().computeOnServer(server -> {
				EntityDreadQueen queen = queenAt(server, c);
				// (once its body is gone to her as mist)
				if (CryonaFight.of(queen).scene() > 110) {
					CryonaFight.of(queen).hurry(4);
				}
				return queen.getPhase();
			});
		}
		if (phase != CryonaFight.ALONE) {
			throw new AssertionError("Cryona did not come to fight alone: phase " + phase);
		}
		context.waitTicks(20);
	}

	/** She stands in the middle, the player on the ice before her, and she casts a power. */
	private static void setUp(TestSingleplayerContext world, BlockPos c, String power) {
		world.getServer().runOnServer(server -> {
			ServerPlayer player = server.getPlayerList().getPlayers().get(0);
			EntityDreadQueen queen = queenAt(server, c);
			CryonaFight fight = CryonaFight.of(queen);
			fight.stop();
			queen.teleportTo(c.getX() + 0.5, c.getY(), c.getZ() + 0.5);
			player.setPermanentlyInvulnerable(true);
			player.getAbilities().flying = false;
			player.getAbilities().mayfly = false;
			player.onUpdateAbilities();
			player.teleportTo((ServerLevel) player.level(), c.getX() + 0.5, c.getY(), c.getZ() + 9.5, Set.of(), 180, 10, false);
			fight.cast((ServerLevel) queen.level(), power);
		});
	}

	/** The player high over her, looking down on all round her. */
	private static void overHer(ClientGameTestContext context, TestSingleplayerContext world, BlockPos c) {
		world.getServer().runOnServer(server -> {
			ServerPlayer player = server.getPlayerList().getPlayers().get(0);
			Vec3 eye = new Vec3(c.getX() + 0.5, c.getY() + 30, c.getZ() + 18.5);
			float[] way = look(eye, new Vec3(c.getX() + 0.5, c.getY(), c.getZ() + 0.5));
			player.getAbilities().mayfly = true;
			player.getAbilities().flying = true;
			player.onUpdateAbilities();
			player.teleportTo((ServerLevel) player.level(), eye.x, eye.y - 1.62, eye.z, Set.of(), way[0], way[1], false);
		});
		context.runOnClient(mc -> mc.player.getAbilities().flying = true);
		context.waitTicks(2);
	}

	/** She rests a while (the next part of the scene in peace). */
	private static void calmHer(TestSingleplayerContext world, BlockPos c) {
		world.getServer().runOnServer(server -> {
			CryonaFight fight = CryonaFight.of(queenAt(server, c));
			fight.stop();
			fight.rest(400);
		});
	}

	/** Waits till her mourning is that far. */
	private static void waitScene(ClientGameTestContext context, TestSingleplayerContext world, BlockPos c, int scene) {
		for (int i = 0; i < 2400; i++) {
			int now = world.getServer().computeOnServer(server -> CryonaFight.of(queenAt(server, c)).scene());
			if (now >= scene) {
				return;
			}
			context.waitTicks(1);
		}
		throw new AssertionError("her mourning did not come to " + scene);
	}

	/** The dead she raised. */
	private static List<EntityDreadMob> mine(MinecraftServer server, BlockPos c) {
		EntityDreadQueen queen = queenAt(server, c);
		return server.getLevel(ThroneFight.REALM).getEntitiesOfClass(EntityDreadMob.class, new AABB(c).inflate(140, 80, 140),
			m -> !(m instanceof EntityDreadQueen) && queen.getUUID().equals(m.getCommanderId()));
	}

	/** Cryona on its back rests a while (her spells would cross what is looked at). */
	private static void calm(MinecraftServer server, BlockPos c) {
		for (EntityDreadQueen queen : server.getLevel(ThroneFight.REALM).getEntitiesOfClass(EntityDreadQueen.class, new AABB(c).inflate(140, 80, 140))) {
			CryonaFight.of(queen).rest(900);
		}
	}

	private static EntityDragonBase frostAt(MinecraftServer server, BlockPos c) {
		return server.getLevel(ThroneFight.REALM).getEntitiesOfClass(EntityDragonBase.class, new AABB(c).inflate(140, 80, 140), EntityDragonBase::isBlackFrost).get(0);
	}

	private static EntityDreadQueen queenAt(MinecraftServer server, BlockPos c) {
		return server.getLevel(ThroneFight.REALM).getEntitiesOfClass(EntityDreadQueen.class, new AABB(c).inflate(140, 80, 140)).get(0);
	}

	/** The way (yaw, pitch) from an eye to a place. */
	private static float[] look(Vec3 eye, Vec3 at) {
		Vec3 d = at.subtract(eye);
		return new float[]{(float) Math.toDegrees(Math.atan2(-d.x, d.z)), (float) -Math.toDegrees(Math.atan2(d.y, Math.sqrt(d.x * d.x + d.z * d.z)))};
	}

	private static void camera(ClientGameTestContext context, TestSingleplayerContext world, double x, double y, double z, float yaw, float pitch, int wait) {
		world.getServer().runOnServer(server -> {
			ServerPlayer player = server.getPlayerList().getPlayers().get(0);
			player.getAbilities().mayfly = true;
			player.getAbilities().flying = true;
			player.onUpdateAbilities();
			player.teleportTo((ServerLevel) player.level(), x, y, z, Set.of(), yaw, pitch, false);
		});
		context.runOnClient(mc -> mc.player.getAbilities().flying = true);
		context.waitTicks(wait);
	}
}

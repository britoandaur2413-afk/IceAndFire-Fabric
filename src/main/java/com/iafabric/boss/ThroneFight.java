package com.iafabric.boss;

import net.minecraft.core.HolderLookup;
import com.iafabric.IafMod;
import com.iafabric.entity.EntityBlackFrostGuard;
import com.iafabric.entity.EntityCryonaAsleep;
import com.iafabric.entity.EntityDragonBase;
import com.iafabric.entity.EntityDreadMob;
import com.iafabric.entity.EntityDreadQueen;
import com.iafabric.entity.IafEntityRegistry;
import com.iafabric.entity.util.HomePosition;
import com.iafabric.item.IafItemRegistry;
import com.iafabric.vfx.Vfx;
import com.iafabric.world.structure.DreadPieces;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.UUIDUtil;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.saveddata.SavedData;
import com.iafabric.compat.SavedDataType;
import net.minecraft.world.phys.AABB;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * The fight with Cryona and Black Frost happens in her own realm, the Frozen Throne: her sign asleep on her throne
 * (EntityCryonaAsleep, Black Frost prowling behind her throne: they never fight) calls the players who come near, or
 * strike it, into her realm, onto an immense arena of ice ringed by spikes (nothing to fall from, nowhere to
 * flee: the cold drives back whoever goes too far), with room for Black Frost to fly. Only those players and the two
 * of them. Each fight has an arena of its own, far from the others.
 * <p>
 * Nothing happens as they come in: the fragment of Cryona stands before them past the altar and speaks, Black Frost
 * prowls behind her, round her and back (nothing between them and her), and nothing touches the two. A path of light
 * leads to the altar; there (walked up to, or used) the player sees what the duel holds and chooses: offer ice dragon
 * blood to begin it (she leaps onto Black Frost), or go home. Who goes home is not called back by her throne until they
 * have gone away from it and come again. If all its players fall or leave, the duel waits: the two stay as they are,
 * the players' things stay where they fell (they never vanish), and whoever comes back to her throne returns to that
 * same arena (with blood again to go on). Both beaten: a way home opens (with what she left).
 */
public final class ThroneFight {
	public static final ResourceKey<Level> REALM = ResourceKey.create(Registries.DIMENSION, IafMod.id("frozen_throne"));
	/** The height players stand at in the realm (the top of its flat ground is one lower). */
	public static final int FLOOR = 64;
	/** The arena's edge, from its middle. */
	public static final int RADIUS = 120;
	/** How far apart two arenas are. */
	private static final int SPACING = 3000;
	/** How near the throne a player is taken along. */
	private static final int TAKEN = 40;
	/** Where the players come in, the altar at the end of the path of light, the light home, Black Frost (along z). */
	static final int COME_IN = 30;
	static final int ALTAR_AT = 17;
	private static final int HOME_AT = 41;
	private static final int BOSS_AT = -4;
	/** Where the fragment of Cryona stands before the duel (along z), facing the way in, past the altar. */
	private static final int FRAGMENT_AT = 7;
	/** How far from her Black Frost prowls behind her (Prowl): its wings (some 19 blocks from its back) stay behind her. */
	private static final double PROWL_RADIUS = 23;
	/** How near the altar a player stands to see its choice. */
	private static final double ALTAR_REACH = 2.6;
	/** The chunks kept loaded round an arena while its fight lasts. */
	private static final int ARENA_TICKET = 10;
	/** How long the marks of the choice last (they go when the duel begins or waits). */
	private static final int MARKS = 24000;
	private static final Map<Integer, Fight> FIGHTS = new HashMap<>();
	/** Who went home before a duel began: her throne does not call them back till they go away and come again. */
	private static final Map<UUID, Declined> DECLINED = new HashMap<>();

	private record Declined(ResourceKey<Level> home, BlockPos throne) {
	}

	private ThroneFight() {
	}

	/** A fight going on, or waiting for its players. */
	public static final class Fight {
		final int id;
		final ResourceKey<Level> home;
		/** Her throne (where her sign sleeps; moved along when an older citadel's throne room is made anew). */
		BlockPos throne;
		/** Where Black Frost prowls by her throne (none: only she is set there again). */
		@Nullable BlockPos bed;
		/** The citadel she rules (the chunk of its start), freed when she is beaten. */
		@Nullable Long citadel;
		final Map<UUID, Vec3> returns = new HashMap<>();
		final Set<UUID> players = new HashSet<>();
		@Nullable UUID queen;
		@Nullable UUID frost;
		long started;
		/** 0 coming in (before the duel begins), 1 fighting, 2 won (the way home open), 3 over, 4 waiting for its players. */
		int state;
		long wonAt;
		Entity way;
		/** Before the duel begins (and while it waits): she and Black Frost only speak, nothing touches them. */
		boolean waiting;
		/** From her call until the players stand in the arena (they are not there yet: none of them is gone). */
		boolean entering;
		/** It waited for its players and goes on now: the two as they were. */
		boolean resumed;
		final List<Entity> marks = new ArrayList<>();
		/** Who stands at the altar (its choice shows once each time they walk up to it). */
		final Set<UUID> atAltar = new HashSet<>();
		/** Who was called to it while it goes on and is on their way in. */
		final Set<UUID> arriving = new HashSet<>();
		/** Black Frost's prowl behind her before the duel. */
		final Prowl prowl = new Prowl(40, 140);

		Fight(int id, ResourceKey<Level> home, BlockPos throne, @Nullable BlockPos bed) {
			this.id = id;
			this.home = home;
			this.throne = throne;
			this.bed = bed;
		}

		public BlockPos center() {
			return new BlockPos(id * SPACING, FLOOR, 0);
		}

		BlockPos altar() {
			return center().offset(0, 0, ALTAR_AT);
		}
	}

	public static void register() {
		registerDuelCommand();
		// Black Frost's pylons are struck, and Black Frost itself (its claws for who strikes it close by); on the server,
		// the player's game sends the blow as for anything
		net.fabricmc.fabric.api.event.player.AttackEntityCallback.EVENT.register((player, level, hand, entity, hit) ->
			level.isClientSide ? net.minecraft.world.InteractionResult.PASS : BlackFrostFight.attacked(player, entity));
		// who Black Frost threw (or let go) loses no life to the height: its own blow on landing is the share it takes
		net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents.ALLOW_DAMAGE.register((entity, source, amount) ->
			!(source.is(net.minecraft.tags.DamageTypeTags.IS_FALL) && BlackFrostFight.fallsSafely(entity)));
		ServerTickEvents.END_WORLD_TICK.register(level -> {
			if (level.dimension() == REALM) {
				tick(level);
			} else if (level.getGameTime() % 20 == 0) {
				joinNear(level);
			}
			Restores.tick(level);
		});
		ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
			ServerPlayer player = handler.getPlayer();
			if (player.level().dimension() == REALM && fightOf(player) == null) {
				// back from a fight that waits or ended while they were away
				Vfx.later(player.serverLevel(), 2, () -> sendHome(player, null));
			}
		});
		ServerLifecycleEvents.SERVER_STOPPING.register(server -> {
			for (Fight fight : List.copyOf(FIGHTS.values())) {
				if (fight.state <= 1 || fight.state == 4) {
					// it waits for them, as it was, until someone comes back to her throne
					Restores.keep(server, fight);
					Restores.add(server, fight.home, fight.throne, fight.bed);
				}
			}
			FIGHTS.clear();
		});
	}

	public static boolean inRealm(Entity entity) {
		return entity.level().dimension() == REALM;
	}

	public static @Nullable Fight fightOf(Player player) {
		for (Fight fight : FIGHTS.values()) {
			if (fight.state != 4 && fight.players.contains(player.getUUID())) {
				return fight;
			}
		}
		return null;
	}

	/** The fight an entity of the realm belongs to (by where it is). */
	public static @Nullable Fight fightAt(Entity entity) {
		if (!inRealm(entity)) {
			return null;
		}
		int id = (int) Math.round(entity.getX() / SPACING);
		return FIGHTS.get(id);
	}

	/** The middle of the arena an entity of the realm is in. */
	public static Vec3 centerOf(Entity entity) {
		int id = (int) Math.round(entity.getX() / SPACING);
		return Vec3.atBottomCenterOf(new BlockPos(id * SPACING, FLOOR, 0));
	}

	/**
	 * Her call from her throne (EntityCryonaAsleep): the players near go to her realm (after a moment, a dread circle
	 * under their feet), where Cryona and Black Frost themselves wait; she and Black Frost asleep on the throne go as
	 * well (they are set again when the duel waits or is lost). If her duel waits for its players, they go back to it.
	 * False when it cannot be (no realm, nobody to take): nothing happens then, she sleeps on.
	 */
	public static boolean begin(ServerLevel level, EntityCryonaAsleep sign, @Nullable EntityBlackFrostGuard frostSign, @Nullable LivingEntity foe) {
		ServerLevel realm = level.getServer().getLevel(REALM);
		if (realm == null || level.dimension() == REALM) {
			IafMod.LOGGER.warn("Cryona's realm is not there ({}): she sleeps on", REALM.location());
			return false;
		}
		BlockPos throne = sign.throne();
		List<ServerPlayer> taken = new ArrayList<>();
		for (ServerPlayer player : level.players()) {
			if (player.isAlive() && !player.isSpectator() && !player.isCreative() && player.distanceToSqr(sign) < TAKEN * TAKEN && fightOf(player) == null
				&& !declined(player, level.dimension(), throne)) {
				taken.add(player);
			}
		}
		if (foe instanceof ServerPlayer player && !taken.contains(player) && fightOf(player) == null) {
			// (who strikes her wants the duel, even if they went home from it a moment ago)
			DECLINED.remove(player.getUUID());
			taken.add(player);
		}
		if (taken.isEmpty()) {
			IafMod.LOGGER.debug("Cryona's call with no player to take");
			return false;
		}
		Fight fight = waitingFor(level.getServer(), level.dimension(), throne);
		boolean resume = fight != null;
		if (!resume) {
			fight = new Fight(Restores.nextId(level.getServer()), level.dimension(), throne, frostSign != null ? frostSign.blockPosition() : sign.bed());
			var start = CastleWard.citadelAt(level, fight.throne);
			fight.citadel = start == null ? null : start.getChunkPos().toLong();
		}
		fight.state = 0;
		fight.waiting = true;
		fight.entering = true;
		fight.resumed = resume;
		fight.started = realm.getGameTime();
		fight.players.clear();
		fight.returns.clear();
		fight.atAltar.clear();
		fight.arriving.clear();
		FIGHTS.put(fight.id, fight);
		// the call: a dread circle under each of them, a column of light, then they are gone
		level.playSound(null, sign.blockPosition(), SoundEvents.WITHER_SPAWN, SoundSource.HOSTILE, 2F, 0.6F);
		for (ServerPlayer player : taken) {
			call(level, fight, player);
		}
		// (the two asleep on her throne are only her sign: in her realm the two themselves fight, or wait as they were)
		sign.discard();
		if (frostSign != null) {
			frostSign.discard();
		}
		// the arena stays loaded while the fight lasts (the bosses fly far from the players)
		realm.getChunkSource().addRegionTicket(net.minecraft.server.level.TicketType.DRAGON, new net.minecraft.world.level.ChunkPos(fight.center()), ARENA_TICKET, net.minecraft.util.Unit.INSTANCE);
		if (!resume) {
			ThroneArena.build(realm, fight.center());
		} else {
			// (an arena of an older duel: Black Frost's perches, its altar)
			ThroneArena.perches(realm, fight.center());
			ThroneArena.altar(realm, fight.center(), true);
		}
		Fight called = fight;
		Vfx.later(level, 40, () -> enter(level, realm, called));
		IafMod.LOGGER.info("Cryona's fight {} {}: {} player(s) taken from {}", fight.id, resume ? "goes on" : "begins", taken.size(), fight.throne);
		return true;
	}

	/** A player is called to her realm: a dread circle under their feet, a column of light. */
	private static void call(ServerLevel level, Fight fight, ServerPlayer player) {
		fight.players.add(player.getUUID());
		fight.returns.put(player.getUUID(), player.position());
		Vfx.rune(level, Vfx.ground(level, player.position()), 2.5F, com.iafabric.entity.EntityVfx.DREAD, Vfx.DREAD, 50, 4F, 0F);
		Vfx.beam(level, player.position(), 0.8F, 12F, Vfx.DREAD, 50);
		player.sendSystemMessage(Component.translatable("iceandfire_fabric.throne.drawn").withStyle(ChatFormatting.LIGHT_PURPLE, ChatFormatting.BOLD));
	}

	/** The players come into the arena, Cryona and Black Frost before them. */
	private static void enter(ServerLevel from, ServerLevel realm, Fight fight) {
		int i = 0;
		for (UUID id : List.copyOf(fight.players)) {
			ServerPlayer player = from.getServer().getPlayerList().getPlayer(id);
			if (player == null || !player.isAlive()) {
				fight.players.remove(id);
				continue;
			}
			arrive(realm, fight, player, i++, fight.players.size());
		}
		if (fight.players.isEmpty()) {
			fight.entering = false;
			pause(realm, fight, List.of());
			return;
		}
		placeBosses(realm, fight, 0);
	}

	/** One player stands at the arena's way in, facing the throne. */
	private static void arrive(ServerLevel realm, Fight fight, ServerPlayer player, int i, int count) {
		BlockPos center = fight.center();
		double x = center.getX() + (i - (count - 1) / 2.0) * 2.5;
		double z = center.getZ() + COME_IN;
		player.teleportTo(realm, x + 0.5, FLOOR, z + 0.5, Set.of(), 180F, 0F);
		Vfx.rune(realm, new Vec3(x + 0.5, FLOOR, z + 0.5), 2.5F, com.iafabric.entity.EntityVfx.DREAD, Vfx.DREAD, 40, -4F, 0F);
	}

	/**
	 * Cryona and Black Frost before the players: the two who waited, as they were (found once their land is loaded),
	 * or a new pair. They only speak; then the way to the altar shows.
	 */
	private static void placeBosses(ServerLevel realm, Fight fight, int attempt) {
		if (fight.state != 0) {
			return;
		}
		BlockPos center = fight.center();
		BlockPos at = center.offset(0, 0, FRAGMENT_AT);
		Entity queenNow = fight.queen == null ? null : realm.getEntity(fight.queen);
		Entity frostNow = fight.frost == null ? null : realm.getEntity(fight.frost);
		if (fight.resumed) {
			// (by what they are, in the arena, if not by who they were)
			AABB arena = new AABB(Vec3.atCenterOf(center), Vec3.atCenterOf(center)).inflate(RADIUS + 20, 80, RADIUS + 20);
			if (!(queenNow instanceof EntityDreadQueen)) {
				for (EntityDreadQueen queen : realm.getEntitiesOfClass(EntityDreadQueen.class, arena, EntityDreadQueen::isAlive)) {
					queenNow = queen;
					fight.queen = queen.getUUID();
					break;
				}
			}
			if (!(frostNow instanceof EntityDragonBase)) {
				for (EntityDragonBase dragon : realm.getEntitiesOfClass(EntityDragonBase.class, arena, d -> d.isBlackFrost() && d.isAlive())) {
					frostNow = dragon;
					fight.frost = dragon.getUUID();
					break;
				}
			}
			IafMod.LOGGER.info("Cryona's fight {} goes on: Cryona {}, Black Frost {} (try {})", fight.id, queenNow != null ? "found" : "not loaded yet",
				frostNow != null ? "found" : "not loaded yet", attempt);
		}
		if (fight.resumed && fight.queen != null && !(queenNow instanceof EntityDreadQueen) && attempt < 15) {
			// (their land was just loaded: they come a moment later)
			Vfx.later(realm, 10, () -> placeBosses(realm, fight, attempt + 1));
			return;
		}
		if (queenNow instanceof EntityDreadQueen queen && queen.isAlive()) {
			EntityDragonBase frost = frostNow instanceof EntityDragonBase dragon && dragon.isAlive() && !dragon.isModelDead() ? dragon : null;
			still(realm, fight, queen, frost);
		} else {
			if (fight.resumed) {
				IafMod.LOGGER.warn("Cryona's fight {}: the two who waited were not found; a new pair", fight.id);
				fight.resumed = false;
			}
			// the fragment of Cryona before them, Black Frost behind her
			Vec3 prowl = prowlAt(fight);
			EntityDragonBase frost = DreadPieces.dreadDragon(realm, realm.getRandom(), BlockPos.containing(prowl), 125, true);
			EntityDreadQueen queen = IafEntityRegistry.DREAD_QUEEN.create(realm);
			if (frost == null || queen == null) {
				lose(realm, fight);
				return;
			}
			frost.moveTo(prowl.x, FLOOR, prowl.z, 0F, 0F);
			frost.homePos = new HomePosition(center, realm);
			frost.hasHomePosition = true;
			realm.addFreshEntity(frost);
			queen.moveTo(at.getX() + 0.5, at.getY(), at.getZ() + 0.5, 0F, 0F);
			queen.finalizeSpawn(realm, realm.getCurrentDifficultyAt(at), MobSpawnType.TRIGGERED, null);
			queen.setNoAi(true);
			queen.setThrone(fight.throne);
			realm.addFreshEntity(queen);
			fight.queen = queen.getUUID();
			fight.frost = frost.getUUID();
			prowl(fight, frost, null);
		}
		fight.waiting = true;
		fight.entering = false;
		Vec3 boss = Vec3.atBottomCenterOf(at);
		Vfx.rune(realm, Vfx.ground(realm, boss), 14F, com.iafabric.entity.EntityVfx.DREAD, Vfx.DREAD, 70, 1.5F, 0F);
		Vfx.beam(realm, boss, 3F, 40F, Vfx.DREAD, 70);
		realm.playSound(null, at, SoundEvents.ENDER_DRAGON_GROWL, SoundSource.HOSTILE, 3F, 0.6F);
		// she only speaks: a fragment of the queen she was, who will see if they are worthy of her weapons
		Vfx.later(realm, 20, () -> {
			if (fight.queen != null && realm.getEntity(fight.queen) instanceof EntityDreadQueen cryona) {
				CryonaVoice.say(realm, cryona, CryonaVoice.Line.ENTRY, true);
			}
		});
		if (fight.resumed) {
			Vfx.later(realm, 50, () -> say(realm, fight, "iceandfire_fabric.throne.again", ChatFormatting.AQUA));
			Vfx.later(realm, 90, () -> ask(realm, fight));
		} else {
			Vfx.later(realm, 60, () -> say(realm, fight, "iceandfire_fabric.throne.fragment1", ChatFormatting.AQUA));
			Vfx.later(realm, 110, () -> say(realm, fight, "iceandfire_fabric.throne.fragment2", ChatFormatting.AQUA));
			Vfx.later(realm, 150, () -> ask(realm, fight));
		}
	}

	/**
	 * The two as the duel waits and nothing touches them: the fragment of Cryona standing past the altar, facing the
	 * way in (off Black Frost's back), and Black Frost prowling behind her. No blow of theirs is left halfway; their
	 * life is as it was.
	 */
	private static void still(ServerLevel realm, Fight fight, EntityDreadQueen queen, @Nullable EntityDragonBase frost) {
		BlockPos at = fight.center().offset(0, 0, FRAGMENT_AT);
		if (queen.isPassenger()) {
			queen.stopRiding();
		}
		if (frost != null) {
			BlackFrostFight.of(frost).stop();
			frost.setTarget(null);
			Vec3 prowl = prowlAt(fight);
			frost.teleportTo(prowl.x, FLOOR, prowl.z);
			prowl(fight, frost, null);
		}
		CryonaFight.of(queen).stop();
		queen.setNoAi(true);
		queen.setTarget(null);
		queen.teleportTo(at.getX() + 0.5, at.getY(), at.getZ() + 0.5);
		queen.setYRot(0F);
		queen.yHeadRot = 0F;
		queen.yBodyRot = 0F;
	}

	/** Where Black Frost is on its prowl behind her now (she faces the way in). */
	private static Vec3 prowlAt(Fight fight) {
		return fight.prowl.at(Vec3.atBottomCenterOf(fight.center().offset(0, 0, FRAGMENT_AT)), 0F, PROWL_RADIUS, FLOOR);
	}

	/** Black Frost's prowl behind her before the duel (Prowl), its head on the player. */
	private static void prowl(Fight fight, EntityDragonBase frost, @Nullable ServerPlayer player) {
		fight.prowl.step(frost, Vec3.atBottomCenterOf(fight.center().offset(0, 0, FRAGMENT_AT)), 0F, PROWL_RADIUS, FLOOR, player);
	}

	/** The way to choose: the path of light to the altar, the altar marked, the light home behind them. */
	private static void ask(ServerLevel realm, Fight fight) {
		if (!fight.waiting || fight.state != 0) {
			return;
		}
		BlockPos center = fight.center();
		Vec3 altar = Vec3.atBottomCenterOf(fight.altar());
		Vec3 home = Vfx.ground(realm, Vec3.atBottomCenterOf(center.offset(0, 0, HOME_AT)));
		fight.marks.add(Vfx.rune(realm, Vfx.ground(realm, altar), 2.6F, com.iafabric.entity.EntityVfx.DREAD, Vfx.DREAD, MARKS, 1.5F, 0F));
		// (two beams at its sides, framing her past it: none between the player and her)
		fight.marks.add(Vfx.beam(realm, altar.add(-2.6, 0, 0), 0.5F, 9F, Vfx.DREAD, MARKS));
		fight.marks.add(Vfx.beam(realm, altar.add(2.6, 0, 0), 0.5F, 9F, Vfx.DREAD, MARKS));
		fight.marks.add(Vfx.rune(realm, home, 1.8F, com.iafabric.entity.EntityVfx.FROST, Vfx.ICE, MARKS, -2F, 0F));
		fight.marks.add(Vfx.beam(realm, home, 0.8F, 14F, Vfx.ICE, MARKS));
		for (ServerPlayer player : playersOf(realm, fight)) {
			player.sendSystemMessage(Component.translatable("iceandfire_fabric.throne.choose").withStyle(ChatFormatting.GRAY));
			player.connection.send(new net.minecraft.network.protocol.game.ClientboundSetTitlesAnimationPacket(10, 70, 20));
			player.connection.send(new net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket(
				Component.translatable("iceandfire_fabric.throne.title").withStyle(ChatFormatting.LIGHT_PURPLE)));
			player.connection.send(new net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket(
				Component.translatable("iceandfire_fabric.throne.subtitle").withStyle(ChatFormatting.GRAY)));
		}
	}

	/** Before the duel: the two still, its choice shown to who walks up to the altar; who steps into the light goes home. */
	private static void waitingTick(ServerLevel realm, Fight fight) {
		ServerPlayer first = firstPlayer(realm, fight);
		if (fight.frost != null && realm.getEntity(fight.frost) instanceof EntityDragonBase frost && !frost.isModelDead()) {
			prowl(fight, frost, first);
		}
		if (fight.queen != null && realm.getEntity(fight.queen) instanceof EntityDreadQueen queen) {
			queen.setNoAi(true);
			if (first != null) {
				// she watches who comes
				float want = (float) Math.toDegrees(Math.atan2(-(first.getX() - queen.getX()), first.getZ() - queen.getZ()));
				queen.yHeadRot = Mth.approachDegrees(queen.yHeadRot, want, 8F);
				queen.yBodyRot = Mth.approachDegrees(queen.yBodyRot, want, 3F);
			}
		}
		if (fight.entering) {
			return;
		}
		Vec3 altar = Vec3.atBottomCenterOf(fight.altar());
		Vec3 home = Vec3.atBottomCenterOf(fight.center().offset(0, 0, HOME_AT));
		for (ServerPlayer player : playersOf(realm, fight)) {
			boolean near = player.position().subtract(altar).horizontalDistanceSqr() < ALTAR_REACH * ALTAR_REACH && Math.abs(player.getY() - altar.y) < 3;
			if (near && fight.atAltar.add(player.getUUID())) {
				openAltar(player);
			} else if (!near) {
				fight.atAltar.remove(player.getUUID());
			}
			if (player.position().subtract(home).horizontalDistanceSqr() < 1.8 * 1.8 && Math.abs(player.getY() - home.y) < 3) {
				leave(realm, fight, player);
			}
		}
		if (fight.state == 0 && realm.getGameTime() % 600 == 300) {
			// a reminder of the way, now and then
			for (ServerPlayer player : playersOf(realm, fight)) {
				player.displayClientMessage(Component.translatable("iceandfire_fabric.throne.subtitle").withStyle(ChatFormatting.LIGHT_PURPLE), true);
			}
		}
	}

	/**
	 * The altar's choice for a player of a duel that has not begun: its card (GuiBossCard) with the two bosses, their
	 * life and powers, the duel's rules, what it asks (ice dragon blood), and the buttons to begin it, go home, or think
	 * it over (/iceandfire duel accept, leave).
	 */
	public static void openAltar(ServerPlayer player) {
		Fight fight = fightOf(player);
		if (fight == null || !fight.waiting || fight.state != 0 || !(player.level() instanceof ServerLevel realm) || realm.dimension() != REALM) {
			player.displayClientMessage(Component.translatable("iceandfire_fabric.throne.altar_quiet").withStyle(ChatFormatting.GRAY), true);
			return;
		}
		Entity frostNow = fight.frost == null ? null : realm.getEntity(fight.frost);
		Entity queenNow = fight.queen == null ? null : realm.getEntity(fight.queen);
		EntityDragonBase frost = frostNow instanceof EntityDragonBase dragon && dragon.isAlive() && !dragon.isModelDead() ? dragon : null;
		EntityDreadQueen queen = queenNow instanceof EntityDreadQueen cryona && cryona.isAlive() ? cryona : null;
		com.iafabric.network.IafNetwork.sendToPlayer(new com.iafabric.message.MessageBossCard(
			frost == null ? -1 : frost.getId(), queen == null ? -1 : queen.getId(),
			frost == null ? 0F : frost.getHealth(), frost == null ? 0F : frost.getMaxHealth(),
			queen == null ? 0F : queen.getHealth(), queen == null ? 0F : queen.getMaxHealth(),
			queen == null ? 0F : queen.getShield(), CryonaFight.SHIELD_MAX,
			bloodOf(player), player.isCreative(), fight.resumed), player);
		realm.playSound(null, player.blockPosition(), SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.PLAYERS, 1F, 0.8F);
	}

	private static int bloodOf(ServerPlayer player) {
		int count = 0;
		for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
			ItemStack stack = player.getInventory().getItem(i);
			if (stack.is(IafItemRegistry.ICE_DRAGON_BLOOD)) {
				count += stack.getCount();
			}
		}
		return count;
	}

	/** The altar takes one ice dragon blood (none from who plays in creative); false if they have none. */
	private static boolean offerBlood(ServerPlayer player) {
		if (player.isCreative()) {
			return true;
		}
		for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
			ItemStack stack = player.getInventory().getItem(i);
			if (stack.is(IafItemRegistry.ICE_DRAGON_BLOOD)) {
				stack.shrink(1);
				return true;
			}
		}
		return false;
	}

	/** The duel is accepted at the altar (one ice dragon blood offered): the fragment rises, the fight begins or goes on. */
	public static void accept(ServerLevel realm, Fight fight, ServerPlayer by) {
		if (!fight.waiting || fight.state != 0 || fight.entering) {
			return;
		}
		if (!offerBlood(by)) {
			by.sendSystemMessage(Component.translatable("iceandfire_fabric.throne.need_blood").withStyle(ChatFormatting.RED));
			by.displayClientMessage(Component.translatable("iceandfire_fabric.throne.need_blood").withStyle(ChatFormatting.RED), true);
			realm.playSound(null, by.blockPosition(), SoundEvents.VAULT_INSERT_ITEM_FAIL, SoundSource.PLAYERS, 1F, 1F);
			return;
		}
		fight.waiting = false;
		fight.state = 1;
		// (no altar in the fight: it breaks as the duel begins, and stands again when it waits)
		ThroneArena.altar(realm, fight.center(), false);
		for (Entity mark : fight.marks) {
			Vfx.remove(mark);
		}
		fight.marks.clear();
		fight.atAltar.clear();
		Vfx.beam(realm, Vec3.atBottomCenterOf(fight.altar()).add(0, 1, 0), 1F, 12F, Vfx.ICE, 30);
		realm.playSound(null, fight.altar(), SoundEvents.TRIAL_SPAWNER_OMINOUS_ACTIVATE, SoundSource.HOSTILE, 2F, 0.8F);
		say(realm, fight, fight.resumed ? "iceandfire_fabric.throne.resumed" : "iceandfire_fabric.throne.intro", ChatFormatting.AQUA);
		for (ServerPlayer player : playersOf(realm, fight)) {
			com.iafabric.pixie.DuelTalk.say(player, com.iafabric.pixie.DuelTalk.Moment.BEGIN);
		}
		for (ServerPlayer player : playersOf(realm, fight)) {
			player.sendSystemMessage(Component.translatable("iceandfire_fabric.throne.blood_given", by.getDisplayName()).withStyle(ChatFormatting.LIGHT_PURPLE));
		}
		// all at the line where they came in, ready
		BlockPos center = fight.center();
		int i = 0;
		List<ServerPlayer> players = playersOf(realm, fight);
		for (ServerPlayer player : players) {
			double x = center.getX() + (i - (players.size() - 1) / 2.0) * 2.5;
			player.teleportTo(realm, x + 0.5, FLOOR, center.getZ() + COME_IN + 0.5, Set.of(), 180F, 0F);
			i++;
		}
		ServerPlayer first = firstPlayer(realm, fight);
		if (fight.frost != null && realm.getEntity(fight.frost) instanceof EntityDragonBase blackFrost && !blackFrost.isModelDead()
			&& fight.queen != null && realm.getEntity(fight.queen) instanceof EntityDreadQueen rider && rider.getPhase() != CryonaFight.ALONE
			&& !rider.isPassenger()) {
			// she leaps onto Black Frost
			Vfx.beam(realm, rider.position(), 1.2F, 10F, Vfx.DREAD, 20);
			rider.startRiding(blackFrost, true);
			realm.playSound(null, blackFrost.blockPosition(), SoundEvents.ENDER_DRAGON_FLAP, SoundSource.HOSTILE, 2F, 0.7F);
		}
		if (fight.frost != null && realm.getEntity(fight.frost) instanceof EntityDragonBase blackFrost && !blackFrost.isModelDead()) {
			blackFrost.fightLed = false;
			blackFrost.setNoAi(false);
			blackFrost.setCommand(0);
			blackFrost.setInSittingPose(false);
			if (first != null) {
				blackFrost.setTarget(first);
			}
			blackFrost.roar();
		}
		if (fight.queen != null && realm.getEntity(fight.queen) instanceof EntityDreadQueen cryona) {
			cryona.setNoAi(false);
			if (first != null) {
				cryona.setTarget(first);
			}
			CryonaVoice.say(realm, cryona, CryonaVoice.Line.WAKE, true);
		}
		Vfx.wave(realm, Vec3.atBottomCenterOf(center.offset(0, 0, FRAGMENT_AT)), 24F, Vfx.DREAD, 20);
		IafMod.LOGGER.info("Cryona's fight {} {}: the duel accepted by {}", fight.id, fight.resumed ? "goes on" : "starts", by.getName().getString());
	}

	/** A player declines and goes home (if nobody is left, the duel waits for them as it is). */
	public static void leave(ServerLevel realm, Fight fight, ServerPlayer player) {
		if (!fight.waiting) {
			return;
		}
		fight.players.remove(player.getUUID());
		fight.atAltar.remove(player.getUUID());
		player.sendSystemMessage(Component.translatable("iceandfire_fabric.throne.left").withStyle(ChatFormatting.GRAY));
		// (her throne does not take them back at once: only when they go away and come again)
		DECLINED.put(player.getUUID(), new Declined(fight.home, fight.throne));
		sendHome(player, fight);
		if (fight.players.isEmpty()) {
			pause(realm, fight, List.of());
		}
	}

	/**
	 * Whether a player went home from this throne's duel before it began and has not gone away since (her throne does
	 * not call them back, nor does she wake for them); once they are far from it, they are as anyone again.
	 */
	public static boolean declined(net.minecraft.world.entity.player.Player player, ResourceKey<Level> home, BlockPos throne) {
		Declined declined = DECLINED.get(player.getUUID());
		if (declined == null) {
			return false;
		}
		if (declined.home() != player.level().dimension()
			|| player.distanceToSqr(Vec3.atCenterOf(declined.throne())) > (TAKEN + 16) * (TAKEN + 16)) {
			DECLINED.remove(player.getUUID());
			return false;
		}
		return declined.home() == home && declined.throne().distSqr(throne) < 16 * 16;
	}

	/** Whether a boss of the realm waits for its duel to begin (or go on): nothing touches it then. */
	public static boolean waiting(Entity boss) {
		if (!inRealm(boss)) {
			return false;
		}
		Fight fight = fightAt(boss);
		return fight != null && fight.waiting;
	}

	/** The duel command of the altar's buttons: /iceandfire duel accept, /iceandfire duel leave, /iceandfire duel altar (its choice again). */
	public static void registerDuelCommand() {
		net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
			dispatcher.register(net.minecraft.commands.Commands.literal(com.iafabric.command.IafCommands.COMMAND)
				.then(net.minecraft.commands.Commands.literal("duel")
					.then(net.minecraft.commands.Commands.literal("accept").executes(context -> duel(context.getSource(), 1)))
					.then(net.minecraft.commands.Commands.literal("leave").executes(context -> duel(context.getSource(), 2)))
					.then(net.minecraft.commands.Commands.literal("altar").executes(context -> duel(context.getSource(), 3))))));
	}

	private static int duel(net.minecraft.commands.CommandSourceStack source, int what) {
		if (!(source.getEntity() instanceof ServerPlayer player) || !(player.level() instanceof ServerLevel realm)) {
			return 0;
		}
		Fight fight = fightOf(player);
		if (fight == null || !fight.waiting || fight.state != 0) {
			source.sendFailure(Component.translatable("iceandfire_fabric.throne.no_duel"));
			return 0;
		}
		switch (what) {
			case 1 -> accept(realm, fight, player);
			case 2 -> leave(realm, fight, player);
			default -> openAltar(player);
		}
		return 1;
	}

	private static @Nullable ServerPlayer firstPlayer(ServerLevel realm, Fight fight) {
		for (UUID id : fight.players) {
			if (realm.getEntity(id) instanceof ServerPlayer player && player.isAlive()) {
				return player;
			}
		}
		return null;
	}

	/** The players of a fight still in its arena. */
	public static List<ServerPlayer> playersOf(ServerLevel realm, Fight fight) {
		List<ServerPlayer> list = new ArrayList<>();
		for (UUID id : fight.players) {
			if (realm.getEntity(id) instanceof ServerPlayer player && player.isAlive()) {
				list.add(player);
			}
		}
		return list;
	}

	/** The players fighting a boss: those of its arena in the realm, those near it anywhere else. */
	public static List<ServerPlayer> foesNear(Entity boss) {
		Fight fight = fightAt(boss);
		if (!(boss.level() instanceof ServerLevel realm)) {
			return List.of();
		}
		if (fight == null) {
			return realm.getPlayers(player -> player.isAlive() && !player.isCreative() && !player.isSpectator() && player.distanceToSqr(boss) < 48 * 48);
		}
		if (fight.waiting) {
			return List.of();
		}
		List<ServerPlayer> list = new ArrayList<>();
		for (ServerPlayer player : playersOf(realm, fight)) {
			if (!player.isCreative() && !player.isSpectator()) {
				list.add(player);
			}
		}
		return list;
	}

	private static void tick(ServerLevel realm) {
		for (Iterator<Fight> it = FIGHTS.values().iterator(); it.hasNext(); ) {
			Fight fight = it.next();
			if (fight.state == 3) {
				it.remove();
				continue;
			}
			if (fight.state == 4 || fight.entering) {
				// waiting for its players (or they are on their way in)
				continue;
			}
			Vec3 center = Vec3.atBottomCenterOf(fight.center());
			if (realm.getGameTime() % 20 == 0) {
				keepThings(realm, center);
			}
			// who left (died, went away, logged out) is out of the fight
			List<UUID> gone = new ArrayList<>();
			fight.players.removeIf(id -> {
				boolean out = !fight.arriving.contains(id) && (!(realm.getEntity(id) instanceof ServerPlayer player) || !player.isAlive());
				if (out) {
					gone.add(id);
					ServerPlayer was = realm.getServer().getPlayerList().getPlayer(id);
					if (was != null) {
						Soundtracks.forget(was);
					}
				}
				return out;
			});
			if (fight.players.isEmpty() && fight.state <= 1) {
				// her words over the last of them; the duel waits for them as it is
				keepThings(realm, center);
				pause(realm, fight, gone);
				continue;
			}
			for (ServerPlayer player : playersOf(realm, fight)) {
				double dx = player.getX() - center.x;
				double dz = player.getZ() - center.z;
				if (dx * dx + dz * dz > RADIUS * RADIUS || player.getY() < FLOOR - 8) {
					// the cold drives them back
					double angle = Math.atan2(dz, dx);
					player.teleportTo(realm, center.x + Math.cos(angle) * (RADIUS - 6), FLOOR + 1, center.z + Math.sin(angle) * (RADIUS - 6), Set.of(),
						player.getYRot(), player.getXRot());
					player.displayClientMessage(Component.translatable("iceandfire_fabric.throne.edge").withStyle(ChatFormatting.AQUA), true);
				}
			}
			Entity queen = fight.queen == null ? null : realm.getEntity(fight.queen);
			Entity frost = fight.frost == null ? null : realm.getEntity(fight.frost);
			keepIn(realm, queen, center);
			keepIn(realm, frost, center);
			music(realm, fight, queen);
			if (fight.waiting) {
				waitingTick(realm, fight);
				continue;
			}
			if (fight.state == 1) {
				if (queen instanceof EntityDreadQueen cryona && cryona.isAlive()) {
					CryonaFight.of(cryona).tick(realm);
				}
				if (frost instanceof EntityDragonBase dragon && dragon.isAlive() && !dragon.isModelDead()) {
					BlackFrostFight.of(dragon).tickRealm(realm, center);
				} else if (frost instanceof EntityDragonBase dragon && dragon.isModelDead()) {
					// (who it held or threw when it fell floats down)
					BlackFrostFight.of(dragon).release();
				}
			}
			boolean queenDown = queen == null || !queen.isAlive();
			boolean frostDown = frost == null || !frost.isAlive() || frost instanceof EntityDragonBase dragon && dragon.isModelDead();
			// (beaten even before the fight began, in their coming: a win all the same)
			if (fight.state <= 1 && fight.queen != null && queenDown && frostDown && realm.getGameTime() - fight.started > 100) {
				win(realm, fight);
			}
			if (fight.state == 2) {
				waitingHome(realm, fight, center);
			}
		}
	}

	/**
	 * What its players hear (Soundtracks): the duel's music while it goes on, the exiled pixie's memories while Cryona
	 * mourns, nothing before it begins, at her end and once it is won.
	 */
	private static void music(ServerLevel realm, Fight fight, @Nullable Entity queen) {
		int track = Soundtracks.NONE;
		if (fight.state == 1 && !fight.waiting && queen instanceof EntityDreadQueen cryona && cryona.isAlive()) {
			track = CryonaFight.trackOf(cryona);
		}
		for (ServerPlayer player : playersOf(realm, fight)) {
			Soundtracks.hear(player, track, REALM);
		}
	}

	/** What lies on the arena (the things of who fell) never goes: it waits for them, untouchable. */
	private static void keepThings(ServerLevel realm, Vec3 center) {
		for (ItemEntity item : realm.getEntitiesOfClass(ItemEntity.class, new AABB(center, center).inflate(RADIUS + 20, 60, RADIUS + 20))) {
			item.setUnlimitedLifetime();
			if (!item.isInvulnerable()) {
				item.setInvulnerable(true);
			}
		}
	}

	/**
	 * All its players fell or left: the duel waits. The two stand still where it began, untouchable, their life as it
	 * is; the dead she called are gone; her sign sleeps on her throne again, and whoever comes back to it returns here.
	 */
	private static void pause(ServerLevel realm, Fight fight, List<UUID> gone) {
		fight.state = 4;
		fight.waiting = true;
		fight.entering = false;
		fight.atAltar.clear();
		for (Entity mark : fight.marks) {
			Vfx.remove(mark);
		}
		fight.marks.clear();
		Entity queenNow = fight.queen == null ? null : realm.getEntity(fight.queen);
		Entity frostNow = fight.frost == null ? null : realm.getEntity(fight.frost);
		for (UUID id : gone) {
			ServerPlayer player = realm.getServer().getPlayerList().getPlayer(id);
			if (player != null && queenNow != null) {
				CryonaVoice.sayTo(player, queenNow.position(), CryonaVoice.Line.LOST);
			}
		}
		if (queenNow instanceof EntityDreadQueen queen && queen.isAlive()) {
			EntityDragonBase frost = frostNow instanceof EntityDragonBase dragon && dragon.isAlive() && !dragon.isModelDead() ? dragon : null;
			still(realm, fight, queen, frost);
		} else if (fight.queen != null && queenNow == null) {
			// (not loaded: they keep what they were saved as)
			IafMod.LOGGER.debug("Cryona's fight {} waits with its two unloaded", fight.id);
		}
		Vec3 center = Vec3.atBottomCenterOf(fight.center());
		// (she is one of the dread too: only the dead she called go)
		for (EntityDreadMob minion : realm.getEntitiesOfClass(EntityDreadMob.class, new AABB(center, center).inflate(RADIUS + 30, 90, RADIUS + 30),
			mob -> !(mob instanceof EntityDreadQueen))) {
			minion.discard();
		}
		for (UUID id : gone) {
			ServerPlayer player = realm.getServer().getPlayerList().getPlayer(id);
			if (player != null) {
				player.sendSystemMessage(Component.translatable("iceandfire_fabric.throne.paused").withStyle(ChatFormatting.GOLD));
			}
		}
		Restores.keep(realm.getServer(), fight);
		Restores.add(realm.getServer(), fight.home, fight.throne, fight.bed);
		ThroneArena.altar(realm, fight.center(), true);
		release(realm, fight);
		IafMod.LOGGER.info("Cryona's fight {} waits for its players", fight.id);
	}

	/** The duel of a throne that waits for its players (in memory, or saved with the world), or null. */
	private static @Nullable Fight waitingFor(MinecraftServer server, ResourceKey<Level> home, BlockPos throne) {
		for (Fight fight : FIGHTS.values()) {
			if (fight.state == 4 && fight.home == home && fight.throne.equals(throne)) {
				return fight;
			}
		}
		return Restores.kept(server, home, throne);
	}

	/**
	 * Who comes to the throne of a duel going on (back from a fall, or late) is called to it too: to its way in,
	 * before the altar if it has not begun.
	 */
	private static void joinNear(ServerLevel level) {
		ServerLevel realm = level.getServer().getLevel(REALM);
		if (realm == null) {
			return;
		}
		for (Fight fight : List.copyOf(FIGHTS.values())) {
			if (fight.state > 1 || fight.entering || fight.home != level.dimension()) {
				continue;
			}
			for (ServerPlayer player : level.players()) {
				if (!player.isAlive() || player.isSpectator() || player.isCreative() || fightOf(player) != null
					|| player.distanceToSqr(Vec3.atCenterOf(fight.throne)) > TAKEN * TAKEN || declined(player, level.dimension(), fight.throne)) {
					continue;
				}
				call(level, fight, player);
				fight.arriving.add(player.getUUID());
				Vfx.later(level, 40, () -> {
					fight.arriving.remove(player.getUUID());
					if (player.isAlive() && fight.players.contains(player.getUUID()) && fight.state <= 1) {
						arrive(realm, fight, player, 0, 1);
						if (fight.waiting) {
							player.sendSystemMessage(Component.translatable("iceandfire_fabric.throne.choose").withStyle(ChatFormatting.GRAY));
						}
					}
				});
			}
		}
	}

	/** A boss that flew or was thrown far is brought back over the middle. */
	private static void keepIn(ServerLevel realm, @Nullable Entity boss, Vec3 center) {
		if (boss == null || !boss.isAlive() || boss.isPassenger()) {
			return;
		}
		double dx = boss.getX() - center.x;
		double dz = boss.getZ() - center.z;
		if (dx * dx + dz * dz > (RADIUS + 12) * (RADIUS + 12) || boss.getY() > FLOOR + 70 || boss.getY() < FLOOR - 10) {
			boss.teleportTo(center.x, FLOOR + 12, center.z);
		}
	}

	/** Both beaten: the realm grows quiet and a way home opens in the middle (taken by stepping in, or after a minute). */
	private static void win(ServerLevel realm, Fight fight) {
		fight.state = 2;
		fight.wonAt = realm.getGameTime();
		Restores.forget(realm.getServer(), fight.id);
		Vec3 center = Vec3.atBottomCenterOf(fight.center());
		fight.way = Vfx.beam(realm, center, 1.8F, 30F, Vfx.ICE, 1300);
		Vfx.rune(realm, center, 4F, com.iafabric.entity.EntityVfx.FROST, Vfx.ICE, 1300, 1.2F, 0F);
		realm.playSound(null, BlockPos.containing(center), SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, SoundSource.PLAYERS, 1.5F, 1F);
		say(realm, fight, "iceandfire_fabric.throne.won", ChatFormatting.GOLD);
		for (ServerPlayer player : playersOf(realm, fight)) {
			com.iafabric.pixie.DuelTalk.say(player, com.iafabric.pixie.DuelTalk.Moment.WON);
		}
		if (fight.citadel != null) {
			CastleWard.freeStart(realm.getServer(), fight.citadel);
		}
		for (EntityDreadMob minion : realm.getEntitiesOfClass(EntityDreadMob.class, new AABB(center, center).inflate(RADIUS + 20, 60, RADIUS + 20),
			mob -> !(mob instanceof EntityDreadQueen))) {
			minion.kill();
		}
		IafMod.LOGGER.info("Cryona's fight {} won", fight.id);
	}

	private static void waitingHome(ServerLevel realm, Fight fight, Vec3 center) {
		boolean late = realm.getGameTime() - fight.wonAt > 1200;
		for (ServerPlayer player : playersOf(realm, fight)) {
			double dx = player.getX() - center.x;
			double dz = player.getZ() - center.z;
			if (late || dx * dx + dz * dz < 2.5 * 2.5) {
				fight.players.remove(player.getUUID());
				sendHome(player, fight);
			}
		}
		if (fight.players.isEmpty()) {
			Vfx.remove(fight.way);
			fight.state = 3;
			release(realm, fight);
		}
	}

	/** The arena's chunks may go now. */
	private static void release(ServerLevel realm, Fight fight) {
		realm.getChunkSource().removeRegionTicket(net.minecraft.server.level.TicketType.DRAGON, new net.minecraft.world.level.ChunkPos(fight.center()), ARENA_TICKET, net.minecraft.util.Unit.INSTANCE);
	}

	/** It cannot go on (its two could not be made): over; she sleeps on her throne again, Black Frost at her feet. */
	private static void lose(ServerLevel realm, Fight fight) {
		fight.state = 3;
		Vec3 center = Vec3.atBottomCenterOf(fight.center());
		for (Entity entity : realm.getEntitiesOfClass(Entity.class, new AABB(center, center).inflate(RADIUS + 30, 90, RADIUS + 30),
			e -> e instanceof EntityDreadQueen || e instanceof EntityDreadMob || e instanceof EntityDragonBase dragon && dragon.dreadMount)) {
			entity.discard();
		}
		Restores.forget(realm.getServer(), fight.id);
		Restores.add(realm.getServer(), fight.home, fight.throne, fight.bed);
		release(realm, fight);
		IafMod.LOGGER.info("Cryona's fight {} lost: she sleeps again at {}", fight.id, fight.throne);
	}

	/** Back where they were before her call (or to their spawn). */
	private static void sendHome(ServerPlayer player, @Nullable Fight fight) {
		Soundtracks.forget(player);
		MinecraftServer server = player.level().getServer();
		Vec3 back = fight == null ? null : fight.returns.get(player.getUUID());
		ServerLevel home = fight == null ? null : server.getLevel(fight.home);
		if (back != null && home != null) {
			player.teleportTo(home, back.x, back.y, back.z, Set.of(), player.getYRot(), player.getXRot());
		} else {
			player.changeDimension(player.findRespawnPositionAndUseSpawnBlock(false, net.minecraft.world.level.portal.DimensionTransition.DO_NOTHING));
		}
	}

	private static void say(ServerLevel realm, Fight fight, String key, ChatFormatting color) {
		for (ServerPlayer player : playersOf(realm, fight)) {
			player.sendSystemMessage(Component.translatable(key).withStyle(color, ChatFormatting.BOLD));
		}
	}

	/** The overlay line to the players of the fight a boss is in. */
	public static void tell(Entity boss, String key, ChatFormatting color) {
		Fight fight = fightAt(boss);
		if (!(boss.level() instanceof ServerLevel realm)) {
			return;
		}
		for (ServerPlayer player : fight == null ? realm.getPlayers(p -> p.distanceToSqr(boss) < 64 * 64) : playersOf(realm, fight)) {
			player.displayClientMessage(Component.translatable(key).withStyle(color, ChatFormatting.BOLD), true);
		}
	}

	/**
	 * Saved with the world: the thrones to set again (she and Black Frost asleep, her sign), done when their place is
	 * loaded; the duels that wait for their players (where their arena is and who waits in it); the count of the fights
	 * (each has its own arena).
	 */
	static final class Restores extends SavedData {
		private static final SavedDataType<Restores> TYPE = new SavedDataType<>(IafMod.stored("iafabric_throne_fights"), Restores::new,
			CompoundTag.CODEC.xmap(Restores::new, data -> data.save(new CompoundTag())), DataFixTypes.SAVED_DATA_COMMAND_STORAGE);
		private final List<CompoundTag> waiting = new ArrayList<>();
		private final List<CompoundTag> kept = new ArrayList<>();
		private int next = 1;

		private Restores() {
		}

		private Restores(CompoundTag tag) {
			this.next = tag.iceandfire_fabric$getIntOr("Next", 1);
			for (Tag t : tag.iceandfire_fabric$getListOrEmpty("Waiting")) {
				if (t instanceof CompoundTag c) {
					waiting.add(c);
				}
			}
			for (Tag t : tag.iceandfire_fabric$getListOrEmpty("Kept")) {
				if (t instanceof CompoundTag c) {
					kept.add(c);
				}
			}
		}

		private CompoundTag save(CompoundTag tag) {
			tag.putInt("Next", next);
			ListTag list = new ListTag();
			list.addAll(waiting);
			tag.put("Waiting", list);
			ListTag duels = new ListTag();
			duels.addAll(kept);
			tag.put("Kept", duels);
			return tag;
		}

		private static Restores get(MinecraftServer server) {
			return server.overworld().getDataStorage().computeIfAbsent(TYPE.factory(), TYPE.name());
		}

		static int nextId(MinecraftServer server) {
			Restores data = get(server);
			int id = data.next++;
			data.setDirty();
			return id;
		}

		static void add(MinecraftServer server, ResourceKey<Level> home, BlockPos throne, @Nullable BlockPos bed) {
			Restores data = get(server);
			for (CompoundTag tag : data.waiting) {
				if (tag.iceandfire_fabric$getLongOr("Throne", 0L) == throne.asLong() && home.location().toString().equals(tag.iceandfire_fabric$getStringOr("Level", ""))) {
					return;
				}
			}
			CompoundTag tag = new CompoundTag();
			tag.putString("Level", home.location().toString());
			tag.putLong("Throne", throne.asLong());
			if (bed != null) {
				tag.putLong("Bed", bed.asLong());
			}
			data.waiting.add(tag);
			data.setDirty();
		}

		/** A duel that waits for its players, as it is. */
		static void keep(MinecraftServer server, Fight fight) {
			Restores data = get(server);
			data.kept.removeIf(tag -> tag.iceandfire_fabric$getIntOr("Id", -1) == fight.id);
			CompoundTag tag = new CompoundTag();
			tag.putInt("Id", fight.id);
			tag.putString("Level", fight.home.location().toString());
			tag.putLong("Throne", fight.throne.asLong());
			if (fight.bed != null) {
				tag.putLong("Bed", fight.bed.asLong());
			}
			if (fight.citadel != null) {
				tag.putLong("Citadel", fight.citadel);
			}
			if (fight.queen != null) {
				tag.putIntArray("Queen", UUIDUtil.uuidToIntArray(fight.queen));
			}
			if (fight.frost != null) {
				tag.putIntArray("Frost", UUIDUtil.uuidToIntArray(fight.frost));
			}
			data.kept.add(tag);
			data.setDirty();
		}

		static void moved(MinecraftServer server, ResourceKey<Level> home, BlockPos from, BlockPos to, @Nullable BlockPos bed) {
			Restores data = get(server);
			boolean changed = false;
			for (List<CompoundTag> list : List.of(data.waiting, data.kept)) {
				for (CompoundTag tag : list) {
					if (tag.iceandfire_fabric$getLongOr("Throne", 0L) == from.asLong() && home.location().toString().equals(tag.iceandfire_fabric$getStringOr("Level", ""))) {
						tag.putLong("Throne", to.asLong());
						if (bed != null) {
							tag.putLong("Bed", bed.asLong());
						} else {
							tag.remove("Bed");
						}
						changed = true;
					}
				}
			}
			if (changed) {
				data.setDirty();
			}
		}

		static void forget(MinecraftServer server, int id) {
			Restores data = get(server);
			if (data.kept.removeIf(tag -> tag.iceandfire_fabric$getIntOr("Id", -1) == id)) {
				data.setDirty();
			}
		}

		/** The saved duel of a throne that waits for its players, as a fight again (or null). */
		static @Nullable Fight kept(MinecraftServer server, ResourceKey<Level> home, BlockPos throne) {
			for (CompoundTag tag : get(server).kept) {
				if (tag.iceandfire_fabric$getLongOr("Throne", 0L) != throne.asLong() || !home.location().toString().equals(tag.iceandfire_fabric$getStringOr("Level", ""))) {
					continue;
				}
				Fight fight = new Fight(tag.iceandfire_fabric$getIntOr("Id", 0), home, throne, (tag.contains("Bed") ? BlockPos.of(tag.getLong("Bed")) : null));
				fight.citadel = (tag.contains("Citadel") ? (Long) tag.getLong("Citadel") : null);
				fight.queen = (tag.hasUUID("Queen") ? tag.getUUID("Queen") : null);
				fight.frost = (tag.hasUUID("Frost") ? tag.getUUID("Frost") : null);
				fight.state = 4;
				fight.waiting = true;
				return fight;
			}
			return null;
		}

		static void tick(ServerLevel level) {
			if (level.getGameTime() % 40 != 0 || level.dimension() == REALM) {
				return;
			}
			Restores data = get(level.getServer());
			if (data.waiting.isEmpty()) {
				return;
			}
			for (Iterator<CompoundTag> it = data.waiting.iterator(); it.hasNext(); ) {
				CompoundTag tag = it.next();
				if (!level.dimension().location().toString().equals(tag.iceandfire_fabric$getStringOr("Level", ""))) {
					continue;
				}
				BlockPos throne = BlockPos.of(tag.iceandfire_fabric$getLongOr("Throne", 0L));
				BlockPos bed = (tag.contains("Bed") ? BlockPos.of(tag.getLong("Bed")) : null);
				if (!level.isLoaded(throne) || bed != null && !level.isLoaded(bed)) {
					continue;
				}
				asleepAgain(level, throne, bed);
				it.remove();
				data.setDirty();
			}
		}
	
		@Override
		public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
			return this.save(tag);
		}
	}

	/** She sleeps on her throne again, Black Frost prowling behind it (as the throne was made): her sign there. */
	static void asleepAgain(ServerLevel level, BlockPos throne, @Nullable BlockPos bed) {
		if (!level.getEntitiesOfClass(EntityCryonaAsleep.class, new AABB(throne).inflate(16)).isEmpty()) {
			return;
		}
		if (bed != null && level.getEntitiesOfClass(EntityBlackFrostGuard.class, new AABB(throne).inflate(EntityCryonaAsleep.HERS)).isEmpty()) {
			EntityBlackFrostGuard frost = EntityBlackFrostGuard.make(level, level.getRandom(), bed, 0F);
			if (frost != null) {
				level.addFreshEntity(frost);
			}
		}
		EntityCryonaAsleep queen = EntityCryonaAsleep.make(level, throne, bed, throne.getX() + 0.5, throne.getY(), throne.getZ() + 0.5, 180F,
			MobSpawnType.TRIGGERED);
		if (queen != null) {
			level.addFreshEntity(queen);
		}
	}

	/**
	 * Her throne moved (an older citadel's throne room made anew): what is kept of her duel by its throne (a duel that
	 * waits, her setting again, who went home from it) goes with it.
	 */
	public static void moved(MinecraftServer server, ResourceKey<Level> home, BlockPos from, BlockPos to, @Nullable BlockPos bed) {
		for (Fight fight : FIGHTS.values()) {
			if (fight.home == home && fight.throne.equals(from)) {
				fight.throne = to;
				fight.bed = bed;
			}
		}
		DECLINED.replaceAll((id, declined) -> declined.home() == home && declined.throne().equals(from) ? new Declined(home, to) : declined);
		Restores.moved(server, home, from, to, bed);
	}

	/** For the gametests: the fight a player is in, its state, and its arena's middle. */
	public static int stateOf(Player player) {
		Fight fight = fightOf(player);
		return fight == null ? -1 : fight.state;
	}

	public static @Nullable BlockPos arenaOf(Player player) {
		Fight fight = fightOf(player);
		return fight == null ? null : fight.center();
	}

	/** For the scenes: the state of the duel of an arena (-1 none), and whether it went on from a wait. */
	public static int stateAt(BlockPos arena) {
		Fight fight = FIGHTS.get((int) Math.round(arena.getX() / (double) SPACING));
		return fight == null ? -1 : fight.state;
	}

	public static boolean resumedAt(BlockPos arena) {
		Fight fight = FIGHTS.get((int) Math.round(arena.getX() / (double) SPACING));
		return fight != null && fight.resumed;
	}

	/** A random spot within a radius of a place (for the powers). */
	static Vec3 spot(RandomSource random, Vec3 center, double radius) {
		double angle = random.nextDouble() * Math.PI * 2;
		double distance = Math.sqrt(random.nextDouble()) * radius;
		return center.add(Math.cos(angle) * distance, 0, Math.sin(angle) * distance);
	}

	static BlockState ice(RandomSource random) {
		return (random.nextInt(4) == 0 ? Blocks.BLUE_ICE : Blocks.PACKED_ICE).defaultBlockState();
	}
}

package com.iafabric.boss;

import com.iafabric.entity.EntityDragonBase;
import com.iafabric.entity.EntityDreadMob;
import com.iafabric.entity.EntityDreadQueen;
import com.iafabric.entity.EntityVfx;
import com.iafabric.entity.IafEntityRegistry;
import com.iafabric.item.IafItemRegistry;
import com.iafabric.vfx.Vfx;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Display;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.WeakHashMap;

/**
 * Cryona's fight, one boss in two parts, every blow shown before it falls.
 * <p>
 * On Black Frost (its life is the fight's bar, her blows go to it): from its back she bursts spikes of ice from circles
 * under her foes, lifts blocks of ice and throws them along lanes drawn on the ground, now and then raises a few of the
 * dead for a while and, at range, throws skulls of ice. When Black Frost is down to half, Absolute Zero: it carries her
 * over the middle of the arena, she calls the cold down on all of it (only who stands in a warm light is spared); then
 * it lands, stunned, and both are open.
 * <p>
 * When Black Frost falls she kneels by it and mourns; its body goes to her as mist and is gone (what is left of it
 * stays on the ice). Then, as long as the exiled pixie's memories play, her grief wakes the dead round her foes and calls
 * rays of ice down from the sky, thunder rolling far off now and then; nothing touches her, and her sobs turn to anger,
 * then fury. She rises for vengeance with her own bar, and fights like a knight of the old ice: three cuts (each shows
 * its lane a moment before), a leap that falls on a circle, spikes running out along three lanes, the frost burst round
 * her, a step through the cold behind her foe, her beams in all directions at once, a ring of ice running out (but for
 * three warm gaps), the rays of ice; at half her life and at a fifth Absolute Zero again, after which she kneels, spent
 * (double damage). After her great blows she stops to breathe: the time to strike. Her blows go through golden hearts.
 * <p>
 * At her end she kneels, says her last words, her sword and staff rise and float to who beat her, and she fades.
 */
public final class CryonaFight {
	public static final int UNKNOWN = 0;
	public static final int MOUNTED = 1;
	public static final int FALLING = 2;
	public static final int ALONE = 3;
	public static final int DYING = 4;
	public static final int GONE = 5;
	private static final Map<EntityDreadQueen, CryonaFight> FIGHTS = new WeakHashMap<>();
	private static final int ZERO_CHANNEL = 110;
	private static final int SPENT = 140;
	/**
	 * Her mourning: the mist of Black Frost's body (to MIST_END); the memories from when the duel's music has faded out
	 * (MEMORIES_AT) as long as they play; she rises at MOURNING and fights again at RISEN.
	 */
	private static final int MIST_END = 100;
	private static final int MEMORIES_AT = Soundtracks.FADE_OUT;
	private static final int MOURNING = MEMORIES_AT + Soundtracks.MEMORIES_TICKS + 10;
	private static final int RISEN = MOURNING + 22;
	/** In her mourning: the dead rise in waves; her rays begin; her voice (sobs, anger, fury). */
	private static final int DEAD_FIRST = 200;
	private static final int DEAD_EVERY = 200;
	private static final int RAYS_FIRST = 360;
	private static final int SOB1_AT = 300;
	private static final int SOB2_AT = 700;
	private static final int ANGER_AT = 1250;
	private static final int FURY_AT = 1800;
	/** A ray of ice from the sky: its circle, how long it fills, its share of who it strikes' life (mourning, vengeance). */
	private static final float RAY_RADIUS = 3.2F;
	private static final int RAY_FILL = 34;
	private static final float RAY_MOURNING = 0.4F;
	private static final float RAY_VENGEANCE = 0.3F;
	private static final int DEATH_SCENE = 160;
	/** How long the dead she raises stay. */
	private static final int MINION_LIFE = 600;
	private static final int MAX_MINIONS = 3;
	/** When she is spent the dead rise round her in numbers. */
	private static final int SPENT_MINIONS = 5;
	private static final int MAX_SPENT_MINIONS = 8;
	/** Her ice shield: its most, how long untouched before it mends, how fast it mends (a tick), a mend after a call. */
	public static final float SHIELD_MAX = 1000F;
	private static final int SHIELD_REST = 160;
	private static final float SHIELD_MEND = 5F;
	private static final float SHIELD_AFTER_CALL = 400F;
	private final EntityDreadQueen queen;
	private @Nullable Vec3 home;
	private int busy = 40;
	private boolean recovering;
	private int spikes = 90;
	private int debris = 220;
	private int dead = 400;
	private int skulls = 260;
	private int nova = 120;
	private int blink = 200;
	private int combo = 20;
	private int leap = 100;
	private int wave = 70;
	private int retreat = 60;
	private int beam = 300;
	private int skyfall = 120;
	private int rays = 140;
	private int beams = 240;
	private int ring = 180;
	/** When she last spoke as she cast (never one line after another). */
	private long lastLine = -1000;
	/** Her shield mends now (its sound once, as it begins). */
	private boolean mending;
	/** In her mourning: when her rays fall next; when thunder rolls far off. */
	private int nextRays = RAYS_FIRST;
	private final int[] thunder = new int[3];
	/** The blows she took lately (game times), and the last. */
	private final java.util.ArrayDeque<Long> hits = new java.util.ArrayDeque<>();
	private long lastHit = -10000;
	/** She called the dead: the end of her next great blow mends her shield. */
	private boolean called;
	private boolean zeroMounted;
	private boolean zeroHalf;
	private boolean zeroFifth;
	private boolean deadHigh;
	private boolean deadLow;
	private int channel;
	private int spent;
	private int scene;
	private @Nullable UUID frostId;
	private @Nullable UUID heir;
	private @Nullable Vec3 corpseAt;
	private Display.@Nullable ItemDisplay sword;
	private Display.@Nullable ItemDisplay staff;
	private @Nullable Move move;
	private final Set<String> told = new HashSet<>();
	private final List<Shard> shards = new ArrayList<>();

	private CryonaFight(EntityDreadQueen queen) {
		this.queen = queen;
		// her calls made once in a fight (kept with her: a duel that waits goes on as it was)
		int marks = queen.getFightMarks();
		zeroMounted = (marks & 1) != 0;
		zeroHalf = (marks & 2) != 0;
		zeroFifth = (marks & 4) != 0;
		deadHigh = (marks & 8) != 0;
		deadLow = (marks & 16) != 0;
	}

	private void saveMarks() {
		queen.setFightMarks((zeroMounted ? 1 : 0) | (zeroHalf ? 2 : 0) | (zeroFifth ? 4 : 0) | (deadHigh ? 8 : 0) | (deadLow ? 16 : 0));
	}

	/** Her duel waits (all its players fell or left): no blow is left halfway; what she called once stays called. */
	public void stop() {
		move = null;
		channel = 0;
		spent = 0;
		recovering = false;
		busy = Math.max(busy, 60);
		clearShards();
		queen.getNavigation().stop();
		queen.setDeltaMovement(Vec3.ZERO);
		if (queen.getPhase() == FALLING) {
			// (her mourning goes on from the memories' beginning when they come back: their music starts again with it)
			scene = Math.min(scene, MEMORIES_AT);
			nextRays = RAYS_FIRST;
		}
	}

	public static CryonaFight of(EntityDreadQueen queen) {
		return FIGHTS.computeIfAbsent(queen, CryonaFight::new);
	}

	/** Her blows are all shown first in her fight: no plain blows (EntityDreadQueen.doHurtTarget). */
	public static boolean choreographed(EntityDreadQueen queen) {
		int phase = queen.getPhase();
		return phase >= MOUNTED && phase <= DYING;
	}

	/** What a blow does to her: a quarter while she calls Absolute Zero, double while she is spent, more while she breathes. */
	public static float damageTaken(EntityDreadQueen queen, float amount) {
		CryonaFight fight = FIGHTS.get(queen);
		if (fight == null) {
			return amount;
		}
		return fight.channel > 0 ? amount * 0.25F : fight.spent > 0 ? amount * 2F : fight.recovering ? amount * 1.3F : amount;
	}

	/** Whether she is calling Absolute Zero (Black Frost carrying her is shielded too). */
	static boolean channeling(EntityDreadQueen queen) {
		CryonaFight fight = FIGHTS.get(queen);
		return fight != null && fight.channel > 0;
	}

	/** Her powers wait (a scene's still moment, Black Frost's great blows). */
	public void rest(int ticks) {
		busy = Math.max(busy, ticks);
	}

	/**
	 * A blow reached her (EntityDreadQueen.hurtServer): the more she is struck, the sooner she comes back from being
	 * spent, and the less her ice shield mends.
	 */
	public void struck(ServerLevel level) {
		long now = level.getGameTime();
		lastHit = now;
		mending = false;
		hits.addLast(now);
		while (!hits.isEmpty() && now - hits.peekFirst() > 200) {
			hits.pollFirst();
		}
		if (spent > 20) {
			spent = Math.max(20, spent - 4);
		}
	}

	/** How much her shield mends now: less the more she was struck in the last ten seconds. */
	private float mendShare(ServerLevel level) {
		long now = level.getGameTime();
		while (!hits.isEmpty() && now - hits.peekFirst() > 200) {
			hits.pollFirst();
		}
		return Math.max(0.2F, 1F - hits.size() / 20F);
	}

	/** Her shield, untouched a while, mends (alone, in her fight). */
	private void mendShield(ServerLevel level) {
		if (queen.getShield() >= SHIELD_MAX || level.getGameTime() - lastHit < SHIELD_REST) {
			return;
		}
		if (!mending) {
			mending = true;
			if (queen.getShield() < SHIELD_MAX * 0.6F) {
				// it rises again round her
				level.playSound(null, queen.blockPosition(), com.iafabric.misc.IafSoundRegistry.CRYONA_SHIELD_UP, SoundSource.HOSTILE, 1.8F, 1F);
				Vfx.wave(level, Vfx.ground(level, queen.position()), 2.5F, Vfx.ICE, 10);
			}
		}
		queen.setShield(Math.min(SHIELD_MAX, queen.getShield() + SHIELD_MEND * mendShare(level)));
		if (queen.tickCount % 10 == 0) {
			level.sendParticles(ParticleTypes.SNOWFLAKE, queen.getX(), queen.getY() + 1.2, queen.getZ(), 6, 0.5, 0.8, 0.5, 0.01);
		}
	}

	/** One of her great blows is done: after a call of the dead it mends her shield. */
	private void charged(ServerLevel level) {
		if (!called || queen.getPhase() != ALONE) {
			return;
		}
		called = false;
		float mend = SHIELD_AFTER_CALL * mendShare(level);
		queen.setShield(Math.min(SHIELD_MAX, queen.getShield() + mend));
		level.playSound(null, queen.blockPosition(), com.iafabric.misc.IafSoundRegistry.CRYONA_SHIELD_UP, SoundSource.HOSTILE, 2F, 1F);
		Vfx.wave(level, Vfx.ground(level, queen.position()), 3F, Vfx.ICE, 10);
		ThroneFight.tell(queen, "iceandfire_fabric.cryona.shield_mends", ChatFormatting.AQUA);
	}

	public boolean spent() {
		return spent > 0;
	}

	/** A blow of hers that takes several ticks (her cuts, her leap, her lanes of spikes). */
	private abstract class Move {
		int t;

		/** True when it is over. */
		abstract boolean tick(ServerLevel level);
	}

	/** A block of ice she lifted: rising, turning about her, then thrown at a place. */
	private static final class Shard {
		Display.BlockDisplay display;
		double angle;
		double radius;
		double height;
		int age;
		Vec3 to;
		int launchAt = -1;
		Vec3 from;
	}

	// ---------------------------------------------------------------------------------------------------------------
	// each tick

	public void tick(ServerLevel level) {
		if (!queen.isAlive()) {
			return;
		}
		if (home == null) {
			home = queen.position();
		}
		tickShards(level);
		int phase = queen.getPhase();
		if (phase == UNKNOWN) {
			phase = begin();
		}
		if (phase == FALLING) {
			tickFalling(level);
			return;
		}
		if (phase == DYING) {
			tickDying(level);
			return;
		}
		if (phase == GONE) {
			return;
		}
		if (move != null) {
			if (move.tick(level)) {
				move = null;
				pin(false);
			}
			return;
		}
		EntityDragonBase frost = blackFrost(level);
		if (phase == MOUNTED) {
			if (frost == null || frost.isModelDead() || !frost.isAlive()) {
				startFalling(level, frost);
				return;
			}
			if (!queen.isPassenger() && !frost.isVehicle() && queen.distanceTo(frost) < 48) {
				// she keeps to its back
				queen.startRiding(frost, true);
			}
		}
		if (channel > 0) {
			channel--;
			if (queen.tickCount % 4 == 0) {
				level.sendParticles(ParticleTypes.SNOWFLAKE, queen.getX(), queen.getY() + 1, queen.getZ(), 12, 1.5, 1.5, 1.5, 0.1);
			}
			return;
		}
		if (spent > 0) {
			spent--;
			if (queen.tickCount % 5 == 0) {
				level.sendParticles(ParticleTypes.CRIT, queen.getX(), queen.getY() + 2.3, queen.getZ(), 6, 0.4, 0.1, 0.4, 0.05);
			}
			if (spent == 0) {
				recover(level);
			}
			return;
		}
		if (phase == ALONE) {
			mendShield(level);
		}
		cooldowns(phase == ALONE && queen.getHealth() < queen.getMaxHealth() * 0.35F ? 2 : 1);
		if (busy > 0) {
			busy--;
			if (recovering && queen.tickCount % 6 == 0) {
				// she catches her breath: the time to strike
				level.sendParticles(ParticleTypes.CLOUD, queen.getX(), queen.getEyeY(), queen.getZ(), 2, 0.15, 0.05, 0.15, 0.01);
			}
			if (busy == 0) {
				recovering = false;
			}
			return;
		}
		List<ServerPlayer> foes = ThroneFight.foesNear(queen);
		if (foes.isEmpty()) {
			return;
		}
		ServerPlayer nearest = nearestTo(foes);
		if (queen.getTarget() == null || !queen.getTarget().isAlive()) {
			queen.setTarget(nearest);
		}
		if (phase == MOUNTED) {
			mounted(level, foes, nearest, frost);
		} else {
			alone(level, foes, nearest);
		}
	}

	/** Her first tick in a fight: on Black Frost or alone. */
	private int begin() {
		int phase = queen.getVehicle() instanceof EntityDragonBase dragon && dragon.isBlackFrost() ? MOUNTED : ALONE;
		if (phase == MOUNTED) {
			frostId = queen.getVehicle().getUUID();
		} else {
			// alone from the start: her shield whole
			queen.setShield(SHIELD_MAX);
		}
		queen.setPhase(phase);
		return phase;
	}

	private void cooldowns(int speed) {
		spikes -= speed;
		debris -= speed;
		dead -= speed;
		skulls -= speed;
		nova -= speed;
		blink -= speed;
		combo -= speed;
		leap -= speed;
		wave -= speed;
		retreat -= speed;
		beam -= speed;
		skyfall -= speed;
		rays -= speed;
		beams -= speed;
		ring -= speed;
	}

	private @Nullable EntityDragonBase blackFrost(ServerLevel level) {
		if (queen.getVehicle() instanceof EntityDragonBase dragon && dragon.isBlackFrost()) {
			frostId = dragon.getUUID();
			return dragon;
		}
		if (frostId == null) {
			ThroneFight.Fight fight = ThroneFight.fightAt(queen);
			frostId = fight == null ? null : fight.frost;
		}
		return frostId != null && level.getEntity(frostId) instanceof EntityDragonBase dragon ? dragon : null;
	}

	private ServerPlayer nearestTo(List<ServerPlayer> foes) {
		ServerPlayer nearest = foes.get(0);
		for (ServerPlayer foe : foes) {
			if (foe.distanceToSqr(queen) < nearest.distanceToSqr(queen)) {
				nearest = foe;
			}
		}
		return nearest;
	}

	/** The middle of her fight: her arena's in her realm, where she woke elsewhere. */
	private Vec3 center() {
		if (ThroneFight.inRealm(queen) && ThroneFight.fightAt(queen) != null) {
			return ThroneFight.centerOf(queen);
		}
		return home == null ? queen.position() : home;
	}

	// ---------------------------------------------------------------------------------------------------------------
	// on Black Frost

	private void mounted(ServerLevel level, List<ServerPlayer> foes, ServerPlayer nearest, EntityDragonBase frost) {
		RandomSource random = queen.getRandom();
		if (!zeroMounted && frost.getHealth() < frost.getMaxHealth() * 0.5F) {
			zeroMounted = true;
			saveMarks();
			absoluteZero(level, random, frost, CryonaVoice.Line.ZERO);
			return;
		}
		if (BlackFrostFight.of(frost).inGreatBlow()) {
			// (never two great blows at once)
			return;
		}
		if (spikes <= 0) {
			spikes = 200 + random.nextInt(60);
			spikeCircles(level, foes, random);
			BlackFrostFight.of(frost).rest(30);
		} else if (debris <= 0) {
			debris = 320 + random.nextInt(80);
			liftIce(level, foes, random);
		} else if (dead <= 0) {
			dead = 800 + random.nextInt(200);
			raiseDead(level, foes, random, 2);
		} else if (skulls <= 0 && nearest.distanceTo(queen) > 10 && queen.hasLineOfSight(nearest)) {
			skulls = 240 + random.nextInt(60);
			queen.throwSkulls(level, nearest);
			busy = 20;
		}
	}

	// ---------------------------------------------------------------------------------------------------------------
	// alone, for vengeance

	private void alone(ServerLevel level, List<ServerPlayer> foes, ServerPlayer nearest) {
		RandomSource random = queen.getRandom();
		float health = queen.getHealth() / queen.getMaxHealth();
		if (!zeroHalf && health < 0.5F || !zeroFifth && health < 0.2F) {
			boolean last = health < 0.2F;
			zeroHalf |= health < 0.5F;
			zeroFifth |= last;
			saveMarks();
			// (at the last, her strength fails her, and she will not yield)
			absoluteZero(level, random, null, last ? CryonaVoice.Line.FALTER : CryonaVoice.Line.ZERO);
			return;
		}
		if (!deadHigh && health < 0.7F || !deadLow && health < 0.35F) {
			deadHigh |= health < 0.7F;
			deadLow |= health < 0.35F;
			saveMarks();
			raiseDead(level, foes, random, 2);
			return;
		}
		double d = nearest.distanceTo(queen);
		if (rays <= 0) {
			// the rays of her grief, in her vengeance too
			rays = 280 + random.nextInt(60);
			cast(level, 22, CryonaVoice.Line.CAST3);
			hint("iceandfire_fabric.cryona.rays");
			rays(level, foes, random, health < 0.5F ? 3 : 2, RAY_VENGEANCE);
			return;
		}
		if (health < 0.5F && skyfall <= 0) {
			// from half her life on: ice from the sky
			skyfall = 260 + random.nextInt(60);
			skyfall(level, foes, random);
			return;
		}
		if (beams <= 0 && d < 14) {
			beams = 340 + random.nextInt(80);
			move = new Beams(level, nearest);
			pin(true);
			return;
		}
		if (ring <= 0 && d < 10) {
			ring = 300 + random.nextInt(80);
			move = new Ring(level, nearest);
			pin(true);
			return;
		}
		if (spikes <= 0) {
			spikes = 240 + random.nextInt(60);
			spikeCircles(level, foes, random);
			return;
		}
		if (beam <= 0 && d >= 8 && d < 30) {
			beam = 360 + random.nextInt(80);
			move = new Beam(level, nearest);
			pin(true);
			return;
		}
		if (d < 6 && retreat <= 0 && random.nextInt(3) == 0) {
			retreat = 120 + random.nextInt(40);
			move = new Retreat(level, nearest);
			pin(true);
			return;
		}
		if (d < 5.5) {
			if (combo <= 0) {
				combo = 60 + random.nextInt(30);
				move = new Combo(level, nearest, false);
			} else if (nova <= 0) {
				nova = 180;
				frostNova(level);
			}
		} else if (d < 14) {
			if (leap <= 0 && random.nextBoolean()) {
				leap = 160 + random.nextInt(40);
				move = new Leap(level, nearest);
			} else if (wave <= 0) {
				wave = 140 + random.nextInt(40);
				move = new Wave(level, nearest);
			} else if (combo <= 0 && d < 9) {
				combo = 60 + random.nextInt(30);
				move = new Combo(level, nearest, true);
			}
		} else {
			if (wave <= 0) {
				wave = 140 + random.nextInt(40);
				move = new Wave(level, nearest);
			} else if (leap <= 0 && d < 24) {
				leap = 160 + random.nextInt(40);
				move = new Leap(level, nearest);
			} else if (blink <= 0) {
				blink = 240;
				stepThroughCold(level, nearest);
			} else if (skulls <= 0 && queen.hasLineOfSight(nearest)) {
				skulls = 260;
				queen.throwSkulls(level, nearest);
				busy = 20;
			}
		}
		if (move != null) {
			pin(true);
		}
	}

	/**
	 * Casts one of her powers now, by name (scenes and tests): spikes, ice, nova, dead, zero, combo, leap, wave, retreat,
	 * beam, skyfall, rays, beams, ring.
	 */
	public void cast(ServerLevel level, String power) {
		List<ServerPlayer> foes = ThroneFight.foesNear(queen);
		if (foes.isEmpty()) {
			return;
		}
		RandomSource random = queen.getRandom();
		ServerPlayer nearest = nearestTo(foes);
		if (queen.getPhase() == UNKNOWN) {
			begin();
		}
		switch (power) {
			case "spikes" -> spikeCircles(level, foes, random);
			case "ice" -> liftIce(level, foes, random);
			case "nova" -> frostNova(level);
			case "dead" -> raiseDead(level, foes, random, 2);
			case "zero" -> absoluteZero(level, random, queen.getVehicle() instanceof EntityDragonBase d && d.isBlackFrost() ? d : null, CryonaVoice.Line.ZERO);
			case "combo" -> {
				move = new Combo(level, nearest, false);
				pin(true);
			}
			case "leap" -> {
				move = new Leap(level, nearest);
				pin(true);
			}
			case "wave" -> {
				move = new Wave(level, nearest);
				pin(true);
			}
			case "retreat" -> {
				move = new Retreat(level, nearest);
				pin(true);
			}
			case "beam" -> {
				move = new Beam(level, nearest);
				pin(true);
			}
			case "skyfall" -> skyfall(level, foes, random);
			case "rays" -> rays(level, foes, random, 3, queen.getPhase() == FALLING ? RAY_MOURNING : RAY_VENGEANCE);
			case "beams" -> {
				move = new Beams(level, nearest);
				pin(true);
			}
			case "ring" -> {
				move = new Ring(level, nearest);
				pin(true);
			}
			default -> {
			}
		}
	}

	// ---------------------------------------------------------------------------------------------------------------
	// helpers

	private void pin(boolean pinned) {
		if (pinned) {
			queen.getNavigation().stop();
			queen.setDeltaMovement(Vec3.ZERO);
		}
		queen.setNoAi(pinned);
	}

	private void face(float yaw) {
		queen.setYRot(yaw);
		queen.yBodyRot = yaw;
		queen.yHeadRot = yaw;
	}

	private static float yawOf(Vec3 way) {
		return (float) Math.toDegrees(Math.atan2(-way.x, way.z));
	}

	private static Vec3 dirOf(float yaw) {
		double r = Math.toRadians(yaw);
		return new Vec3(-Math.sin(r), 0, Math.cos(r));
	}

	/** The ice under a foe (one leaping, flying or thrown is marked on the floor below them). */
	private Vec3 floorUnder(ServerLevel level, LivingEntity foe) {
		return Vfx.ground(level, new Vec3(foe.getX(), Math.min(foe.getY(), queen.getY() + 3), foe.getZ()));
	}

	private static boolean foe(LivingEntity entity) {
		return entity instanceof ServerPlayer player && player.isAlive() && !player.isCreative() && !player.isSpectator();
	}

	private DamageSource magic(ServerLevel level) {
		return level.damageSources().indirectMagic(queen, queen);
	}

	/** Her staff raised (and her words, now and then): a spell begins. */
	private void cast(ServerLevel level, int ticks, CryonaVoice.@Nullable Line line) {
		busy = Math.max(busy, ticks);
		queen.setAnimation(EntityDreadQueen.ANIMATION_CAST);
		Vfx.once(level, queen.position(), SoundEvents.EVOKER_PREPARE_ATTACK, 1.4F, 0.7F, 20, 16);
		if (line != null) {
			speak(level, line, 3);
		}
	}

	/** Her words as she casts: one time in so many, and never two lines within eight seconds. */
	private void speak(ServerLevel level, CryonaVoice.Line line, int oneIn) {
		long now = level.getGameTime();
		if (now - lastLine < 160 || queen.getRandom().nextInt(oneIn) != 0) {
			return;
		}
		if (CryonaVoice.say(level, queen, line, false)) {
			lastLine = now;
		}
	}

	/** One of her blows on a foe: a share of their life, through any armor and through golden hearts (BossLimits.pierce). */
	private void hit(ServerLevel level, LivingEntity entity, float share) {
		BossLimits.pierce(level, entity, queen, share);
	}

	/** A hint shown the first time a blow comes (how to get out of it). */
	private void hint(String key) {
		if (told.add(key)) {
			ThroneFight.tell(queen, key, ChatFormatting.AQUA);
		}
	}

	/** She stops after a great blow to breathe: the time to strike (she takes a little more). */
	private void breathe(int ticks) {
		busy = Math.max(busy, ticks);
		recovering = true;
	}

	/** The foes in a lane on the ground (from a place along a way, so long, so wide). */
	private List<LivingEntity> inLane(ServerLevel level, Vec3 from, float yaw, float length, float width) {
		Vec3 dir = dirOf(yaw);
		Vec3 side = new Vec3(-dir.z, 0, dir.x);
		List<LivingEntity> hit = new ArrayList<>();
		AABB box = new AABB(from, from.add(dir.scale(length))).inflate(width, 3, width);
		for (LivingEntity entity : level.getEntitiesOfClass(LivingEntity.class, box, CryonaFight::foe)) {
			Vec3 rel = entity.position().subtract(from);
			double along = rel.dot(dir);
			double across = Math.abs(rel.dot(side));
			if (along >= -0.5 && along <= length + 0.5 && across <= width / 2 + entity.getBbWidth() / 2 && Math.abs(rel.y) < 3) {
				hit.add(entity);
			}
		}
		return hit;
	}

	// ---------------------------------------------------------------------------------------------------------------
	// her blade

	/** Three cuts: each lane shows a moment before the blade falls along it; the third, a thrust, goes further. */
	private final class Combo extends Move {
		private final ServerPlayer target;
		private int strike;
		private int hitAt;
		private Vec3 from;
		private float yaw;
		private float length;
		private float width;
		private boolean struck;

		Combo(ServerLevel level, ServerPlayer target, boolean dash) {
			this.target = target;
			hint("iceandfire_fabric.cryona.combo");
			if (dash) {
				// she closes in first, in a gust of snow
				Vec3 way = target.position().subtract(queen.position()).multiply(1, 0, 1);
				double go = Math.max(0, way.length() - 3);
				Vec3 to = Vfx.ground(level, queen.position().add(way.normalize().scale(go)));
				level.sendParticles(ParticleTypes.SNOWFLAKE, queen.getX(), queen.getY() + 1, queen.getZ(), 20, 0.4, 0.6, 0.4, 0.05);
				if (level.noCollision(queen, queen.getBoundingBox().move(to.subtract(queen.position())))) {
					queen.teleportTo(to.x, to.y, to.z);
				}
			}
			next(level, 14);
		}

		private void next(ServerLevel level, int windup) {
			Vec3 at = queen.position();
			yaw = yawOf(target.position().subtract(at).multiply(1, 0, 1));
			face(yaw);
			length = strike == 2 ? 7.5F : 5F;
			width = strike == 2 ? 2.4F : 3.6F;
			from = Vfx.ground(level, at);
			Vfx.lane(level, from, yaw, length, width, Vfx.DANGER, windup);
			hitAt = t + windup;
		}

		@Override
		boolean tick(ServerLevel level) {
			t++;
			face(yaw);
			if (t == hitAt - 9) {
				queen.setAnimation(strike == 2 ? EntityDreadQueen.ANIMATION_THRUST : EntityDreadQueen.ANIMATION_SLASH);
				level.playSound(null, queen.blockPosition(), SoundEvents.TRIDENT_RIPTIDE_1, SoundSource.HOSTILE, 0.8F, 1.6F);
			}
			if (t < hitAt) {
				return false;
			}
			// she steps into the cut
			Vec3 dir = dirOf(yaw);
			Vec3 to = queen.position().add(dir.scale(strike == 2 ? 2.2 : 1.1));
			if (level.noCollision(queen, queen.getBoundingBox().move(to.subtract(queen.position())))) {
				queen.teleportTo(to.x, queen.getY(), to.z);
			}
			level.playSound(null, queen.blockPosition(), SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.HOSTILE, 1.4F, strike == 2 ? 0.6F : 0.8F);
			Vec3 mid = from.add(dir.scale(length / 2));
			level.sendParticles(ParticleTypes.SWEEP_ATTACK, mid.x, mid.y + 1, mid.z, 3, length / 4, 0.2, length / 4, 0);
			level.sendParticles(ParticleTypes.SNOWFLAKE, mid.x, mid.y + 1, mid.z, 16, length / 3, 0.3, length / 3, 0.05);
			for (LivingEntity entity : inLane(level, from, yaw, length, width)) {
				hit(level, entity, strike == 2 ? 0.24F : 0.16F);
				BossLimits.shove(entity, dir.x * (strike == 2 ? 1.2 : 0.5), 0.3, dir.z * (strike == 2 ? 1.2 : 0.5));
				struck = true;
			}
			strike++;
			if (strike > 2) {
				breathe(22);
				charged(level);
				if (struck) {
					speak(level, CryonaVoice.Line.TAUNT, 4);
				}
				return true;
			}
			next(level, 9);
			return false;
		}
	}

	/** She crouches and leaps high, her blade over her head, and falls on a circle that fills under her flight. */
	private final class Leap extends Move {
		private static final int CROUCH = 12;
		private static final int FLIGHT = 16;
		private static final float LEAP_RADIUS = 7F;
		private final Vec3 start;
		private final Vec3 land;

		Leap(ServerLevel level, ServerPlayer target) {
			start = queen.position();
			land = Vfx.ground(level, target.position());
			hint("iceandfire_fabric.cryona.leap");
			Vfx.circle(level, land, LEAP_RADIUS, Vfx.DANGER, CROUCH + FLIGHT);
			Vfx.rune(level, land, LEAP_RADIUS + 0.5F, EntityVfx.FROST, Vfx.ICE, CROUCH + FLIGHT + 8, 3F, 0F);
			queen.setAnimation(EntityDreadQueen.ANIMATION_SLAM);
			face(yawOf(land.subtract(start)));
			level.playSound(null, queen.blockPosition(), SoundEvents.WARDEN_ROAR, SoundSource.HOSTILE, 0.6F, 1.8F);
			speak(level, CryonaVoice.Line.CAST3, 2);
		}

		@Override
		boolean tick(ServerLevel level) {
			t++;
			if (t <= CROUCH) {
				if (t % 3 == 0) {
					level.sendParticles(ParticleTypes.CLOUD, queen.getX(), queen.getY() + 0.1, queen.getZ(), 6, 0.4, 0.05, 0.4, 0.02);
				}
				return false;
			}
			if (t < CROUCH + FLIGHT) {
				double f = (t - CROUCH) / (double) FLIGHT;
				Vec3 p = start.lerp(land, f).add(0, Math.sin(Math.PI * f) * 7, 0);
				queen.teleportTo(p.x, p.y, p.z);
				level.sendParticles(ParticleTypes.SNOWFLAKE, p.x, p.y + 1, p.z, 4, 0.2, 0.3, 0.2, 0.02);
				return false;
			}
			// she falls on it
			queen.teleportTo(land.x, land.y, land.z);
			RandomSource random = queen.getRandom();
			Vfx.wave(level, land, LEAP_RADIUS + 5F, Vfx.ICE, 14);
			level.playSound(null, BlockPos.containing(land), SoundEvents.ANVIL_LAND, SoundSource.HOSTILE, 1.6F, 0.5F);
			Vfx.shatter(level, land, 2F, 0.5F);
			level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.PACKED_ICE.defaultBlockState()), land.x, land.y + 0.3, land.z, 50, 1.6, 0.3, 1.6, 0.15);
			for (int i = 0; i < 14; i++) {
				double a = Math.PI * 2 * i / 14 + random.nextDouble() * 0.3;
				double r = 4.6 + random.nextDouble() * 2.2;
				Vfx.spike(level, land.add(Math.cos(a) * r, 0, Math.sin(a) * r), ThroneFight.ice(random), 0.7F, 1.8F + random.nextFloat() * 1.8F,
					(float) Math.sin(a) * 22F, (float) -Math.cos(a) * 22F, 3, 50);
			}
			float reach = LEAP_RADIUS + 0.2F;
			for (LivingEntity entity : level.getEntitiesOfClass(LivingEntity.class, new AABB(land, land).inflate(reach, 3, reach), CryonaFight::foe)) {
				double d = Math.sqrt(entity.position().subtract(land).horizontalDistanceSqr());
				if (d > reach) {
					continue;
				}
				Vec3 away = entity.position().subtract(land).multiply(1, 0, 1).normalize();
				hit(level, entity, (float) Math.max(0.16, 0.32 - d * 0.022));
				BossCold.slow(entity, 30, 0, queen);
				BossLimits.shove(entity, away.x * 1.3, 0.55, away.z * 1.3);
			}
			// her blade is stuck in the ice a moment
			breathe(28);
			charged(level);
			return true;
		}
	}

	/** Her staff to the ground: spikes run out from her along three lanes, one after the other. */
	private final class Wave extends Move {
		private static final int WINDUP = 16;
		private static final int STEPS = 12;
		private final Vec3 from;
		private final float[] yaws = new float[3];
		private final List<Set<UUID>> hit = new ArrayList<>();

		Wave(ServerLevel level, ServerPlayer target) {
			from = Vfx.ground(level, queen.position());
			float base = yawOf(target.position().subtract(from).multiply(1, 0, 1));
			face(base);
			hint("iceandfire_fabric.cryona.wave");
			for (int k = 0; k < 3; k++) {
				yaws[k] = base + (k - 1) * 24F;
				Vfx.lane(level, from, yaws[k], 19F, 2.4F, Vfx.ICE, WINDUP);
				hit.add(new HashSet<>());
			}
			cast(level, 0, CryonaVoice.Line.CAST1);
		}

		@Override
		boolean tick(ServerLevel level) {
			t++;
			if (t < WINDUP || (t - WINDUP) % 2 != 0) {
				return false;
			}
			int step = (t - WINDUP) / 2;
			if (step >= STEPS) {
				breathe(14);
				charged(level);
				return true;
			}
			RandomSource random = queen.getRandom();
			double d = 2 + step * 1.45;
			for (int k = 0; k < 3; k++) {
				Vec3 dir = dirOf(yaws[k]);
				Vec3 at = from.add(dir.scale(d));
				for (int i = 0; i < 2; i++) {
					Vec3 spot = at.add((random.nextDouble() - 0.5) * 1.2, 0, (random.nextDouble() - 0.5) * 1.2);
					Vfx.spike(level, spot, ThroneFight.ice(random), 0.6F + random.nextFloat() * 0.3F, 1.6F + random.nextFloat() * 1.6F,
						(float) (dir.z * 15), (float) (-dir.x * 15), 3, 36);
				}
				for (LivingEntity entity : level.getEntitiesOfClass(LivingEntity.class, new AABB(at, at).inflate(1.7, 2.5, 1.7), CryonaFight::foe)) {
					if (hit.get(k).add(entity.getUUID())) {
						hit(level, entity, 0.2F);
						BossCold.freeze(level, entity, 12);
						BossLimits.shove(entity, 0, 0.45, 0);
					}
				}
			}
			if (step % 2 == 0) {
				Vfx.shatter(level, from.add(dirOf(yaws[1]).scale(d)), 1.1F, 0.7F + step * 0.03F);
			}
			return false;
		}
	}

	/** A quick cut (its lane a moment before), then she leaps back out of reach in a gust of snow. */
	private final class Retreat extends Move {
		private static final int WINDUP = 9;
		private static final int BACK = 7;
		private final float yaw;
		private final Vec3 from;
		private Vec3 start;
		private Vec3 land;

		Retreat(ServerLevel level, ServerPlayer target) {
			from = Vfx.ground(level, queen.position());
			yaw = yawOf(target.position().subtract(from).multiply(1, 0, 1));
			face(yaw);
			Vfx.lane(level, from, yaw, 4.5F, 3.4F, Vfx.DANGER, WINDUP);
			queen.setAnimation(EntityDreadQueen.ANIMATION_SLASH);
			level.playSound(null, queen.blockPosition(), SoundEvents.TRIDENT_RIPTIDE_1, SoundSource.HOSTILE, 0.8F, 1.7F);
		}

		@Override
		boolean tick(ServerLevel level) {
			t++;
			face(yaw);
			if (t == WINDUP) {
				level.playSound(null, queen.blockPosition(), SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.HOSTILE, 1.4F, 0.9F);
				Vec3 mid = from.add(dirOf(yaw).scale(2.2));
				level.sendParticles(ParticleTypes.SWEEP_ATTACK, mid.x, mid.y + 1, mid.z, 2, 0.8, 0.2, 0.8, 0);
				for (LivingEntity entity : inLane(level, from, yaw, 4.5F, 3.4F)) {
					hit(level, entity, 0.16F);
				}
				// where she lands, back out of reach
				start = queen.position();
				Vec3 back = start.subtract(dirOf(yaw).scale(BACK));
				land = Vfx.ground(level, back);
				if (!level.noCollision(queen, queen.getBoundingBox().move(land.subtract(start)))) {
					land = start;
				}
				return false;
			}
			if (t > WINDUP && t <= WINDUP + 6 && start != null) {
				double f = (t - WINDUP) / 6.0;
				Vec3 p = start.lerp(land, f).add(0, Math.sin(Math.PI * f) * 1.6, 0);
				queen.teleportTo(p.x, p.y, p.z);
				level.sendParticles(ParticleTypes.SNOWFLAKE, p.x, p.y + 1, p.z, 6, 0.3, 0.4, 0.3, 0.03);
				return false;
			}
			if (t > WINDUP + 6) {
				breathe(10);
				charged(level);
				return true;
			}
			return false;
		}
	}

	/**
	 * She lifts her staff: a long lane fills slowly from her to her foe; then the old cold runs down it in a beam.
	 * Death to all in it (a totem of undying saves): out of the lane, to its sides, is safe.
	 */
	private final class Beam extends Move {
		private static final int CHARGE = 60;
		private static final int BLOW = 14;
		private static final float LENGTH = 34F;
		private static final float WIDTH = 5F;
		private final Vec3 from;
		private final float yaw;
		private final Set<UUID> struck = new HashSet<>();

		Beam(ServerLevel level, ServerPlayer target) {
			from = Vfx.ground(level, queen.position());
			yaw = yawOf(target.position().subtract(from).multiply(1, 0, 1));
			face(yaw);
			hint("iceandfire_fabric.cryona.beam");
			Vfx.lane(level, from, yaw, LENGTH, WIDTH, Vfx.DANGER, CHARGE);
			Vfx.rune(level, from, 3F, EntityVfx.FROST, Vfx.ICE, CHARGE + 6, 6F, 0F);
			queen.setAnimation(EntityDreadQueen.ANIMATION_CAST);
			level.playSound(null, queen.blockPosition(), SoundEvents.WARDEN_SONIC_CHARGE, SoundSource.HOSTILE, 2F, 0.8F);
			speak(level, CryonaVoice.Line.CAST2, 1);
		}

		@Override
		boolean tick(ServerLevel level) {
			t++;
			face(yaw);
			Vec3 staff = queen.position().add(0, 1.7, 0).add(dirOf(yaw).scale(0.8));
			if (t < CHARGE) {
				if (t % 12 == 0) {
					queen.setAnimation(EntityDreadQueen.ANIMATION_CAST);
				}
				if (t % 2 == 0) {
					RandomSource random = queen.getRandom();
					Vec3 out = staff.add((random.nextDouble() - 0.5) * 4, random.nextDouble() * 2, (random.nextDouble() - 0.5) * 4);
					Vec3 in = staff.subtract(out).normalize();
					level.sendParticles(ParticleTypes.END_ROD, out.x, out.y, out.z, 0, in.x, in.y, in.z, 0.2);
				}
				return false;
			}
			if (t == CHARGE) {
				float pitch = (float) Math.toDegrees(Math.atan2(staff.y - from.y - 0.6, LENGTH));
				Vfx.stream(level, staff, yaw, pitch, LENGTH, WIDTH, 0xF0E8F8FF, BLOW + 4);
				level.playSound(null, queen.blockPosition(), SoundEvents.WARDEN_SONIC_BOOM, SoundSource.HOSTILE, 3F, 0.7F);
			}
			for (LivingEntity entity : inLane(level, from, yaw, LENGTH, WIDTH)) {
				if (struck.add(entity.getUUID())) {
					BossLimits.doom(level, entity, queen);
				}
			}
			if (t % 3 == 0) {
				Vec3 at = from.add(dirOf(yaw).scale(queen.getRandom().nextDouble() * LENGTH));
				level.sendParticles(ParticleTypes.SNOWFLAKE, at.x, at.y + 0.8, at.z, 10, WIDTH / 4, 0.4, WIDTH / 4, 0.05);
			}
			if (t >= CHARGE + BLOW) {
				breathe(24);
				charged(level);
				return true;
			}
			return false;
		}
	}

	/**
	 * Her beams in all directions at once: six lanes from her (one through her nearest foe) fill, and the old cold runs
	 * down all of them; then six more between them, sooner. Away from her, between two lanes, is safe; then where the
	 * first ones ran.
	 */
	private final class Beams extends Move {
		private static final int FIRST = 34;
		private static final int SECOND = 20;
		private static final int BLOW = 10;
		private static final int COUNT = 6;
		private static final float LENGTH = 30F;
		private static final float WIDTH = 3.4F;
		private final Vec3 from;
		private final float base;
		private final Set<UUID> struck = new HashSet<>();

		Beams(ServerLevel level, ServerPlayer target) {
			from = Vfx.ground(level, queen.position());
			base = yawOf(target.position().subtract(from).multiply(1, 0, 1));
			face(base);
			hint("iceandfire_fabric.cryona.beams");
			com.iafabric.pixie.DuelTalk.moment(queen, com.iafabric.pixie.DuelTalk.Moment.BEAMS);
			lanes(level, base, FIRST);
			Vfx.rune(level, from, 3.5F, EntityVfx.FROST, Vfx.ICE, FIRST + SECOND + BLOW + 6, 8F, 0F);
			queen.setAnimation(EntityDreadQueen.ANIMATION_CAST);
			level.playSound(null, queen.blockPosition(), SoundEvents.WARDEN_SONIC_CHARGE, SoundSource.HOSTILE, 2F, 1.1F);
			speak(level, CryonaVoice.Line.CAST2, 2);
		}

		private void lanes(ServerLevel level, float start, int charge) {
			for (int k = 0; k < COUNT; k++) {
				Vfx.lane(level, from, start + k * 360F / COUNT, LENGTH, WIDTH, Vfx.DANGER, charge);
			}
		}

		private void blow(ServerLevel level, float start) {
			Vec3 staff = queen.position().add(0, 1.7, 0);
			float pitch = (float) Math.toDegrees(Math.atan2(staff.y - from.y - 0.6, LENGTH));
			for (int k = 0; k < COUNT; k++) {
				float yaw = start + k * 360F / COUNT;
				Vfx.stream(level, staff.add(dirOf(yaw).scale(0.6)), yaw, pitch, LENGTH, WIDTH, 0xF0E8F8FF, BLOW + 4);
			}
			queen.setAnimation(EntityDreadQueen.ANIMATION_CAST);
			level.playSound(null, queen.blockPosition(), SoundEvents.WARDEN_SONIC_BOOM, SoundSource.HOSTILE, 2.5F, 0.9F);
			struck.clear();
		}

		private void strike(ServerLevel level, float start) {
			for (int k = 0; k < COUNT; k++) {
				for (LivingEntity entity : inLane(level, from, start + k * 360F / COUNT, LENGTH, WIDTH)) {
					if (struck.add(entity.getUUID())) {
						hit(level, entity, 0.35F);
						BossCold.slow(entity, 40, 1, queen);
					}
				}
			}
		}

		@Override
		boolean tick(ServerLevel level) {
			t++;
			face(base);
			float second = base + 180F / COUNT;
			if (t < FIRST && t % 2 == 0) {
				// the cold gathers in her staff
				RandomSource random = queen.getRandom();
				Vec3 staff = queen.position().add(0, 1.7, 0);
				Vec3 out = staff.add((random.nextDouble() - 0.5) * 5, random.nextDouble() * 2, (random.nextDouble() - 0.5) * 5);
				Vec3 in = staff.subtract(out).normalize();
				level.sendParticles(ParticleTypes.END_ROD, out.x, out.y, out.z, 0, in.x, in.y, in.z, 0.2);
			}
			if (t == FIRST) {
				blow(level, base);
				lanes(level, second, SECOND);
			}
			if (t >= FIRST && t < FIRST + BLOW) {
				strike(level, base);
			}
			if (t == FIRST + SECOND) {
				blow(level, second);
			}
			if (t >= FIRST + SECOND && t < FIRST + SECOND + BLOW) {
				strike(level, second);
			}
			if (t >= FIRST + SECOND + BLOW) {
				breathe(26);
				charged(level);
				return true;
			}
			return false;
		}
	}

	/**
	 * Her staff on the ice: a ring of ice runs out from her to the edge of her reach, but for three gaps that warm lights
	 * show (out through one is safe, or over the ring with a leap as it comes).
	 */
	private final class Ring extends Move {
		private static final int WINDUP = 24;
		private static final int STEPS = 13;
		private static final float GAP = 24F;
		private static final double REACH = 22;
		private final Vec3 from;
		private final float[] gaps = new float[3];
		private final Set<UUID> struck = new HashSet<>();

		Ring(ServerLevel level, ServerPlayer target) {
			from = Vfx.ground(level, queen.position());
			// (no gap straight at her foe: they must move to one)
			float start = yawOf(target.position().subtract(from).multiply(1, 0, 1)) + 60F;
			hint("iceandfire_fabric.cryona.ring");
			com.iafabric.pixie.DuelTalk.moment(queen, com.iafabric.pixie.DuelTalk.Moment.RING);
			for (int k = 0; k < 3; k++) {
				gaps[k] = start + k * 120F;
				Vec3 dir = dirOf(gaps[k]);
				// a path of warm lights out through it
				for (double d : new double[]{6, 13, 20}) {
					Vec3 light = Vfx.ground(level, from.add(dir.scale(d)).add(0, 2, 0));
					Vfx.rune(level, light, 1.8F, EntityVfx.FLAME, Vfx.FIRE, WINDUP + STEPS * 2 + 8, 3F, 0F);
					Vfx.beam(level, light, 0.6F, 6F, Vfx.FIRE, WINDUP + STEPS * 2 + 8);
				}
			}
			Vfx.rune(level, from, 3.5F, EntityVfx.FROST, Vfx.ICE, WINDUP + 6, 6F, 0F);
			queen.setAnimation(EntityDreadQueen.ANIMATION_SLAM);
			level.playSound(null, queen.blockPosition(), SoundEvents.EVOKER_PREPARE_ATTACK, SoundSource.HOSTILE, 1.6F, 0.6F);
		}

		private boolean inGap(float yaw) {
			for (float gap : gaps) {
				if (Math.abs(Mth.wrapDegrees(yaw - gap)) <= GAP) {
					return true;
				}
			}
			return false;
		}

		@Override
		boolean tick(ServerLevel level) {
			t++;
			if (t < WINDUP) {
				return false;
			}
			if (t == WINDUP) {
				Vfx.wave(level, from, (float) REACH, Vfx.ICE, STEPS * 2);
				Vfx.shatter(level, from, 1.6F, 0.6F);
				level.playSound(null, queen.blockPosition(), SoundEvents.ANVIL_LAND, SoundSource.HOSTILE, 1.4F, 0.5F);
			}
			if ((t - WINDUP) % 2 != 0) {
				return false;
			}
			int step = (t - WINDUP) / 2;
			if (step > STEPS) {
				breathe(18);
				charged(level);
				return true;
			}
			double r = 2 + step * (REACH - 2) / STEPS;
			RandomSource random = queen.getRandom();
			for (int i = 0; i < 6; i++) {
				float yaw = random.nextFloat() * 360F;
				if (inGap(yaw)) {
					continue;
				}
				Vec3 dir = dirOf(yaw);
				Vfx.spike(level, Vfx.ground(level, from.add(dir.scale(r)).add(0, 2, 0)), ThroneFight.ice(random), 0.6F, 1.2F + random.nextFloat() * 1.2F,
					(float) (dir.z * 20), (float) (-dir.x * 20), 2, 26);
			}
			for (LivingEntity entity : level.getEntitiesOfClass(LivingEntity.class, new AABB(from, from).inflate(r + 1.5, 4, r + 1.5), CryonaFight::foe)) {
				Vec3 rel = entity.position().subtract(from).multiply(1, 0, 1);
				double d = rel.length();
				// (over it with a leap, or out through a gap: safe)
				if (Math.abs(d - r) > 1.3 || entity.getY() - from.y > 1.0 || inGap(yawOf(rel)) || !struck.add(entity.getUUID())) {
					continue;
				}
				hit(level, entity, 0.22F);
				BossCold.freeze(level, entity, 12);
				BossLimits.shove(entity, rel.x / Math.max(0.1, d) * 0.6, 0.45, rel.z / Math.max(0.1, d) * 0.6);
			}
			return false;
		}
	}

	/**
	 * Rays of ice from the sky round each foe (her grief's, and in her vengeance): a circle at their feet and a ring of
	 * circles round it, never one over another (between them is safe); a thin light falls on each as it fills, then the
	 * ray: a great share of who it strikes' life, through any armor and through golden hearts.
	 */
	private void rays(ServerLevel level, List<ServerPlayer> foes, RandomSource random, int around, float share) {
		for (int f = 0; f < Math.min(3, foes.size()); f++) {
			Vec3 middle = floorUnder(level, foes.get(f));
			ray(level, middle, RAY_FILL, share);
			// (the ring all at once, a moment after the one at their feet: two crashes, not a din of them)
			for (Vec3 at : Vfx.ring(middle, around, 8.2, random.nextDouble() * 360)) {
				ray(level, Vfx.ground(level, at.add(0, 3, 0)), RAY_FILL + 10, share);
			}
		}
	}

	/** One ray of ice: its circle filling, a thin light from the sky on it, then the ray. */
	private void ray(ServerLevel level, Vec3 at, int fill, float share) {
		Vfx.circle(level, at, RAY_RADIUS, Vfx.DANGER, fill);
		Vfx.beam(level, at, 0.12F, 60F, 0x80D8F4FF, fill);
		Vfx.later(level, fill, () -> {
			if (!queen.isAlive() || queen.getPhase() == DYING || queen.getPhase() == GONE) {
				return;
			}
			RandomSource random = queen.getRandom();
			// a column of ice light from the sky, its white heart, the ice bursting where it strikes
			Vfx.beam(level, at, RAY_RADIUS * 0.75F, 70F, 0xE0BFEFFF, 12);
			Vfx.beam(level, at, RAY_RADIUS * 0.3F, 70F, 0xFFFFFFFF, 8);
			Vfx.wave(level, at, RAY_RADIUS + 2F, Vfx.ICE, 12);
			level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.PACKED_ICE.defaultBlockState()), at.x, at.y + 0.4, at.z, 30,
				RAY_RADIUS / 2, 0.3, RAY_RADIUS / 2, 0.15);
			level.sendParticles(ParticleTypes.END_ROD, at.x, at.y + 2, at.z, 16, 0.5, 2, 0.5, 0.05);
			level.sendParticles(ParticleTypes.SNOWFLAKE, at.x, at.y + 1, at.z, 24, RAY_RADIUS / 2, 0.8, RAY_RADIUS / 2, 0.08);
			for (int i = 0; i < 5; i++) {
				double a = Math.PI * 2 * i / 5 + random.nextDouble() * 0.4;
				Vfx.spike(level, at.add(Math.cos(a) * (RAY_RADIUS - 0.6), 0, Math.sin(a) * (RAY_RADIUS - 0.6)), ThroneFight.ice(random), 0.6F,
					1.4F + random.nextFloat() * 1.4F, (float) Math.sin(a) * 25F, (float) -Math.cos(a) * 25F, 2, 40);
			}
			Vfx.once(level, at, com.iafabric.misc.IafSoundRegistry.CRYONA_ICE_RAY, 2F, 0.9F + random.nextFloat() * 0.2F, 8, 20);
			float reach = RAY_RADIUS + 0.3F;
			for (LivingEntity entity : level.getEntitiesOfClass(LivingEntity.class, new AABB(at, at).inflate(reach, 4, reach), CryonaFight::foe)) {
				if (entity.position().subtract(at).horizontalDistanceSqr() <= reach * reach) {
					hit(level, entity, share);
					BossCold.slow(entity, 40, 1, queen);
				}
			}
		});
	}

	/**
	 * From half her life on: icicles fall from the sky round each foe, never one circle over another: one at their
	 * feet first, then a ring round them, one after another round it (where one fell is safe to step into).
	 */
	private void skyfall(ServerLevel level, List<ServerPlayer> foes, RandomSource random) {
		cast(level, 24, CryonaVoice.Line.CAST3);
		hint("iceandfire_fabric.cryona.skyfall");
		for (int f = 0; f < Math.min(3, foes.size()); f++) {
			Vec3 middle = floorUnder(level, foes.get(f));
			List<Vec3> ring = Vfx.ring(middle, 7, 7.0, random.nextDouble() * 360);
			icicle(level, middle, 30, random);
			for (int i = 0; i < ring.size(); i++) {
				icicle(level, Vfx.ground(level, ring.get(i).add(0, 2, 0)), 36 + i * 3, random);
			}
		}
	}

	/** An icicle falling from the sky on a circle that fills first (that many ticks). */
	private void icicle(ServerLevel level, Vec3 at, int delay, RandomSource random) {
		{
			Vfx.circle(level, at, 2.8F, Vfx.ICE, delay);
			Display.BlockDisplay icicle = Vfx.shape(level, at.add(0, 24, 0), Blocks.PACKED_ICE.defaultBlockState(), 0.9F, 2.8F, 180F, 0F, true);
			Vfx.later(level, delay - 7, () -> {
				if (icicle != null && !icicle.isRemoved()) {
					Vfx.move(icicle, at.add(0, 2.8, 0), 7);
				}
			});
			Vfx.later(level, delay, () -> {
				Vfx.remove(icicle);
				level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.PACKED_ICE.defaultBlockState()), at.x, at.y + 0.5, at.z, 24, 0.7, 0.5, 0.7, 0.1);
				Vfx.shatter(level, at, 1.2F, 0.6F + random.nextFloat() * 0.2F);
				for (LivingEntity entity : level.getEntitiesOfClass(LivingEntity.class, new AABB(at, at).inflate(3, 3, 3), CryonaFight::foe)) {
					if (entity.position().subtract(at).horizontalDistanceSqr() <= 3 * 3) {
						hit(level, entity, 0.25F);
					}
				}
			});
		}
	}

	// ---------------------------------------------------------------------------------------------------------------
	// her spells

	/**
	 * Circles of frost under her foes and in a ring round them (never one over another: between them, or out through a
	 * gap, is safe): spikes of ice burst from them.
	 */
	private void spikeCircles(ServerLevel level, List<ServerPlayer> foes, RandomSource random) {
		cast(level, 25, CryonaVoice.Line.CAST1);
		List<Vec3> spots = new ArrayList<>();
		for (int f = 0; f < Math.min(2, foes.size()); f++) {
			Vec3 middle = floorUnder(level, foes.get(f));
			spots.add(middle);
			spots.addAll(Vfx.ring(middle, 5, 9.0, random.nextDouble() * 360));
		}
		for (Vec3 at : spots) {
			Vec3 ground = Vfx.ground(level, at);
			Vfx.rune(level, ground, 4.2F, EntityVfx.FROST, Vfx.ICE, 80, 3F, 0F);
			Vfx.circle(level, ground, 3.8F, Vfx.ICE, 36);
			Vfx.later(level, 36, () -> {
				for (int i = 0; i < 6; i++) {
					double angle = random.nextDouble() * Math.PI * 2;
					double distance = Math.sqrt(random.nextDouble()) * 3.7;
					Vec3 spot = ground.add(Math.cos(angle) * distance, 0, Math.sin(angle) * distance);
					float lean = (float) (distance * 8);
					Vfx.spike(level, spot, ThroneFight.ice(random), 0.55F + random.nextFloat() * 0.4F, 2.2F + random.nextFloat() * 2.8F,
						(float) Math.sin(angle) * lean, (float) -Math.cos(angle) * lean, 4, 60);
				}
				Vfx.shatter(level, ground, 1.6F, 0.6F);
				level.sendParticles(ParticleTypes.SNOWFLAKE, ground.x, ground.y + 1, ground.z, 30, 1.5, 0.8, 1.5, 0.06);
				for (LivingEntity entity : level.getEntitiesOfClass(LivingEntity.class, new AABB(ground, ground).inflate(3.9, 3, 3.9), CryonaFight::foe)) {
					if (entity.position().subtract(ground).horizontalDistanceSqr() <= 3.9 * 3.9) {
						hit(level, entity, 0.2F);
						BossCold.freeze(level, entity, 20);
						BossCold.slow(entity, 40, 0, queen);
					}
				}
			});
		}
	}

	/** She lifts blocks of ice from the floor, turns them about her, shows where each will go, and throws them. */
	private void liftIce(ServerLevel level, List<ServerPlayer> foes, RandomSource random) {
		cast(level, 90, CryonaVoice.Line.CAST2);
		ThroneFight.tell(queen, "iceandfire_fabric.cryona.debris", ChatFormatting.AQUA);
		int count = Math.min(10, 6 + foes.size() * 2);
		for (int i = 0; i < count; i++) {
			Shard shard = new Shard();
			shard.angle = Math.PI * 2 * i / count;
			shard.radius = 4.5 + random.nextDouble() * 2;
			shard.height = 3 + random.nextDouble() * 3;
			Vec3 start = Vfx.ground(level, queen.position().add(Math.cos(shard.angle) * shard.radius, 0, Math.sin(shard.angle) * shard.radius));
			float size = 1.1F + random.nextFloat() * 0.8F;
			shard.display = Vfx.shape(level, start, ThroneFight.ice(random), size, size, random.nextFloat() * 50, random.nextFloat() * 50, true);
			if (shard.display == null) {
				continue;
			}
			level.sendParticles(ParticleTypes.CLOUD, start.x, start.y + 0.3, start.z, 6, 0.4, 0.1, 0.4, 0.02);
			ServerPlayer target = foes.get(i % foes.size());
			int launch = 55 + i * 5;
			shard.launchAt = launch;
			// the line it will fly along shows a while before
			Vfx.later(level, launch - 25, () -> {
				if (shard.display == null || shard.display.isRemoved() || !target.isAlive()) {
					return;
				}
				shard.to = Vfx.ground(level, ThroneFight.spot(random, target.position(), 1.5));
				Vec3 from = orbitSpot(shard);
				Vec3 way = shard.to.subtract(from);
				Vfx.lane(level, new Vec3(from.x, shard.to.y, from.z), yawOf(way), (float) way.horizontalDistance(), 1.8F, Vfx.DANGER, 25);
			});
			shards.add(shard);
		}
	}

	private Vec3 orbitSpot(Shard shard) {
		return queen.position().add(Math.cos(shard.angle) * shard.radius, shard.height, Math.sin(shard.angle) * shard.radius);
	}

	private void tickShards(ServerLevel level) {
		for (java.util.Iterator<Shard> it = shards.iterator(); it.hasNext(); ) {
			Shard shard = it.next();
			if (shard.display == null || shard.display.isRemoved()) {
				it.remove();
				continue;
			}
			shard.age++;
			if (shard.from != null) {
				// flying: it lands after eight ticks
				if (shard.age - shard.launchAt >= 8) {
					shatter(level, shard);
					it.remove();
				}
				continue;
			}
			if (shard.launchAt >= 0 && shard.age >= shard.launchAt && shard.to != null) {
				shard.from = shard.display.position();
				Vfx.move(shard.display, shard.to, 8);
				level.playSound(null, shard.from.x, shard.from.y, shard.from.z, SoundEvents.SNOWBALL_THROW, SoundSource.HOSTILE, 1.5F, 0.5F);
				continue;
			}
			if (shard.age > 400 || !queen.isAlive() || queen.getPhase() >= FALLING && queen.getPhase() != ALONE) {
				Vfx.remove(shard.display);
				it.remove();
				continue;
			}
			// rising, then turning about her
			shard.angle += 0.09;
			if (shard.age % 2 == 0) {
				Vfx.move(shard.display, orbitSpot(shard), shard.age < 20 ? 6 : 2);
			}
		}
	}

	private void shatter(ServerLevel level, Shard shard) {
		Vec3 at = shard.to;
		Vfx.remove(shard.display);
		level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.PACKED_ICE.defaultBlockState()), at.x, at.y + 0.5, at.z, 30, 0.8, 0.5, 0.8, 0.1);
		Vfx.shatter(level, at, 1.5F, 0.8F);
		Vfx.wave(level, at, 3F, Vfx.ICE, 10);
		for (LivingEntity entity : level.getEntitiesOfClass(LivingEntity.class, new AABB(at, at).inflate(2.4, 3, 2.4), CryonaFight::foe)) {
			Vec3 away = entity.position().subtract(at).multiply(1, 0, 1).normalize();
			hit(level, entity, 0.16F);
			BossLimits.shove(entity, away.x * 0.8, 0.35, away.z * 0.8);
		}
	}

	/** A burst of frost round her: its circle fills first, then all in it are thrown back and frozen. */
	private void frostNova(ServerLevel level) {
		cast(level, 40, null);
		Vec3 at = Vfx.ground(level, queen.position());
		Vfx.circle(level, at, 11F, Vfx.DANGER, 30);
		Vfx.rune(level, at, 11.5F, EntityVfx.FROST, Vfx.ICE, 40, -5F, 0F);
		hint("iceandfire_fabric.cryona.nova");
		Vfx.later(level, 30, () -> {
			if (!queen.isAlive() || queen.getPhase() != ALONE && queen.getPhase() != MOUNTED) {
				return;
			}
			Vec3 now = queen.position();
			Vfx.wave(level, Vfx.ground(level, now), 14F, Vfx.ICE, 14);
			Vfx.shatter(level, queen.position(), 2F, 0.5F);
			for (LivingEntity entity : level.getEntitiesOfClass(LivingEntity.class, new AABB(at, at).inflate(11.5, 4, 11.5), CryonaFight::foe)) {
				if (entity.position().subtract(at).horizontalDistanceSqr() > 11.3 * 11.3) {
					continue;
				}
				Vec3 away = entity.position().subtract(now).multiply(1, 0, 1).normalize();
				hit(level, entity, 0.24F);
				BossCold.slow(entity, 40, 0, queen);
				BossCold.freeze(level, entity, 15);
				BossLimits.shove(entity, away.x * 1.5, 0.5, away.z * 1.5);
			}
		});
	}

	/** Dread circles near her foes: from each one of the dead climbs out, for a while (at most three of hers at once). */
	private void raiseDead(ServerLevel level, List<ServerPlayer> foes, RandomSource random, int count) {
		raiseDead(level, foes, random, count, MAX_MINIONS);
		if (queen.getPhase() == ALONE) {
			called = true;
		}
	}

	private void raiseDead(ServerLevel level, List<ServerPlayer> foes, RandomSource random, int count, int most) {
		int alive = level.getEntitiesOfClass(EntityDreadMob.class, queen.getBoundingBox().inflate(120, 60, 120), m -> queen.getUUID().equals(m.getCommanderId())).size();
		int raise = Math.min(count, most - alive);
		if (raise <= 0) {
			return;
		}
		if (spent <= 0) {
			cast(level, 30, null);
		}
		for (int i = 0; i < raise; i++) {
			ServerPlayer foe = foes.get(i % foes.size());
			minion(level, Vfx.ground(level, ThroneFight.spot(random, foe.position(), 6)), foe, random, MINION_LIFE);
		}
	}

	/** One of the dead climbing out of a dread circle (shown first), after a foe, for a while. */
	private void minion(ServerLevel level, Vec3 at, ServerPlayer foe, RandomSource random, int life) {
		Vfx.rune(level, at, 1.8F, EntityVfx.DREAD, Vfx.DREAD, 50, 6F, 0F);
		Vfx.beam(level, at, 0.5F, 4F, Vfx.DREAD, 40);
		Vfx.later(level, 30, () -> {
			EntityDreadMob minion = switch (random.nextInt(3)) {
				case 0 -> IafEntityRegistry.DREAD_GHOUL.create(level);
				case 1 -> IafEntityRegistry.DREAD_KNIGHT.create(level);
				default -> IafEntityRegistry.DREAD_THRALL.create(level);
			};
			if (minion == null || !queen.isAlive() || queen.getPhase() == DYING || queen.getPhase() == GONE) {
				return;
			}
			minion.moveTo(at.x, at.y, at.z, random.nextFloat() * 360, 0);
			minion.finalizeSpawn(level, level.getCurrentDifficultyAt(minion.blockPosition()), MobSpawnType.MOB_SUMMONED, null, null);
			minion.setCommanderId(queen.getUUID());
			minion.summonFor(life);
			minion.setTarget(foe);
			level.addFreshEntity(minion);
			level.sendParticles(ParticleTypes.SOUL, at.x, at.y + 0.5, at.z, 14, 0.4, 0.5, 0.4, 0.03);
		});
	}

	/** Her dead (those she raised) back to dust. */
	private void dismissDead(ServerLevel level) {
		for (EntityDreadMob minion : level.getEntitiesOfClass(EntityDreadMob.class, queen.getBoundingBox().inflate(120, 60, 120), m -> queen.getUUID().equals(m.getCommanderId()))) {
			level.sendParticles(ParticleTypes.SOUL, minion.getX(), minion.getY() + 1, minion.getZ(), 8, 0.3, 0.5, 0.3, 0.02);
			minion.discard();
		}
	}

	/** She steps through the cold behind her foe (a dread circle shows where, a moment before), and cuts at once. */
	private void stepThroughCold(ServerLevel level, ServerPlayer foe) {
		Vec3 behind = Vfx.ground(level, foe.position().subtract(foe.getLookAngle().multiply(1, 0, 1).normalize().scale(3)));
		cast(level, 22, null);
		Vfx.rune(level, behind, 1.6F, EntityVfx.DREAD, Vfx.DREAD, 30, 8F, 0F);
		level.sendParticles(ParticleTypes.SNOWFLAKE, queen.getX(), queen.getY() + 1, queen.getZ(), 30, 0.4, 0.8, 0.4, 0.05);
		Vfx.later(level, 15, () -> {
			if (queen.isAlive() && !queen.isPassenger() && queen.getPhase() == ALONE && move == null) {
				queen.teleportTo(behind.x, behind.y, behind.z);
				level.playSound(null, queen.blockPosition(), SoundEvents.ENDERMAN_TELEPORT, SoundSource.HOSTILE, 1.5F, 0.5F);
				level.sendParticles(ParticleTypes.SNOWFLAKE, behind.x, behind.y + 1, behind.z, 30, 0.4, 0.8, 0.4, 0.05);
				if (foe.isAlive()) {
					busy = 0;
					move = new Combo(level, foe, false);
					pin(true);
				}
			}
		});
	}

	/**
	 * Absolute Zero: over the middle of the arena (carried there by Black Frost, or rising alone), shielded (a quarter
	 * of each blow), a great dread circle under her; warm lights glow here and there. When it falls, only who stands in
	 * a warm light is spared. Then she is spent for seven seconds (double damage); Black Frost lands, stunned.
	 */
	private void absoluteZero(ServerLevel level, RandomSource random, @Nullable EntityDragonBase frost, CryonaVoice.Line line) {
		Vec3 center = center();
		boolean realm = ThroneFight.inRealm(queen);
		channel = ZERO_CHANNEL;
		move = null;
		// the cold gathers in her: everyone is out of the ice, free to run for a warm light
		for (ServerPlayer foe : ThroneFight.foesNear(queen)) {
			BossCold.thaw(foe);
		}
		if (frost != null) {
			BlackFrostFight.of(frost).hover(level, center.add(0, 11, 0), ZERO_CHANNEL);
		} else {
			pin(true);
			queen.setNoGravity(true);
			queen.teleportTo(center.x, center.y + 7, center.z);
		}
		Vfx.rune(level, center, realm ? 24F : 16F, EntityVfx.DREAD, Vfx.DREAD, ZERO_CHANNEL + 10, 1F, 0F);
		Vfx.rune(level, center, 6F, EntityVfx.FROST, Vfx.ICE, ZERO_CHANNEL + 10, -3F, frost != null ? 11F : 7F);
		Vfx.beam(level, center, 2.5F, 50F, Vfx.DREAD, ZERO_CHANNEL + 10);
		List<Vec3> warm = new ArrayList<>();
		double start = random.nextDouble() * Math.PI * 2;
		for (int i = 0; i < 3; i++) {
			double angle = start + Math.PI * 2 * i / 3;
			double distance = realm ? 30 + random.nextDouble() * 18 : 10 + random.nextDouble() * 6;
			Vec3 at = Vfx.ground(level, center.add(Math.cos(angle) * distance, 0, Math.sin(angle) * distance));
			warm.add(at);
			Vfx.rune(level, at, 4.2F, EntityVfx.FLAME, Vfx.FIRE, ZERO_CHANNEL + 10, 3F, 0F);
			Vfx.beam(level, at, 0.7F, 10F, Vfx.FIRE, ZERO_CHANNEL + 10);
		}
		level.playSound(null, BlockPos.containing(center), SoundEvents.WITHER_SPAWN, SoundSource.HOSTILE, 3F, 1.3F);
		ThroneFight.tell(queen, "iceandfire_fabric.cryona.zero", ChatFormatting.RED);
		com.iafabric.pixie.DuelTalk.moment(queen, com.iafabric.pixie.DuelTalk.Moment.ZERO);
		CryonaVoice.say(level, queen, line, true);
		queen.setAnimation(EntityDreadQueen.ANIMATION_CAST);
		Vfx.later(level, ZERO_CHANNEL, () -> {
			if (!queen.isAlive() || queen.getPhase() != MOUNTED && queen.getPhase() != ALONE) {
				return;
			}
			Vfx.wave(level, center, realm ? 70F : 30F, Vfx.ICE, 30);
			Vfx.shatter(level, center, 4F, 0.4F);
			for (ServerPlayer foe : ThroneFight.foesNear(queen)) {
				boolean safe = false;
				for (Vec3 at : warm) {
					safe |= foe.position().subtract(at).horizontalDistanceSqr() <= 4.4 * 4.4;
				}
				if (safe) {
					continue;
				}
				// the longest call of all: nine tenths of their life out of the warm lights (BossLimits.doom), its ice through any
				// ward; the exiled pixie's gift (the resistance to absolute ice) spares them half of it
				boolean gift = com.iafabric.pixie.FairyVision.iceGift(foe);
				if (BossCold.bitable(foe)) {
					// without it, straight to the heart, and it cauterizes
					BossCold.bite(level, foe, queen, foe.getMaxHealth() * 0.9F);
				} else {
					BossLimits.doom(level, foe, queen, gift ? 0.5F : 0.9F);
				}
				BossCold.prison(level, foe, 40);
				BossCold.slow(foe, 60, 1, queen);
				level.sendParticles(ParticleTypes.SNOWFLAKE, foe.getX(), foe.getY() + 1, foe.getZ(), 40, 0.5, 1, 0.5, 0.1);
			}
			spent = SPENT;
			ThroneFight.tell(queen, "iceandfire_fabric.cryona.spent", ChatFormatting.GOLD);
			com.iafabric.pixie.DuelTalk.moment(queen, com.iafabric.pixie.DuelTalk.Moment.SPENT);
			// spent, she calls the dead round her to keep her
			List<ServerPlayer> near = ThroneFight.foesNear(queen);
			if (!near.isEmpty()) {
				raiseDead(level, near, random, SPENT_MINIONS, MAX_SPENT_MINIONS);
				called = true;
			}
			EntityDragonBase carrier = frost != null && frost.isAlive() && !frost.isModelDead() ? frost : null;
			if (carrier != null) {
				// Black Frost comes down, stunned, under her
				BlackFrostFight.of(carrier).land(level, center, SPENT);
			} else {
				// she falls to the floor and kneels
				queen.setNoGravity(false);
				Vec3 floor = Vfx.ground(level, center);
				queen.teleportTo(floor.x, floor.y, floor.z);
				queen.setKneeling(true);
				Vfx.rune(level, floor, 3F, EntityVfx.DREAD, 0x80B07CFF, SPENT, 0.5F, 0F);
			}
		});
	}

	/** Spent no more: she rises. */
	private void recover(ServerLevel level) {
		queen.setKneeling(false);
		queen.setNoGravity(false);
		pin(false);
		level.playSound(null, queen.blockPosition(), SoundEvents.ENDER_DRAGON_GROWL, SoundSource.HOSTILE, 2F, 1.5F);
		busy = 30;
	}

	// ---------------------------------------------------------------------------------------------------------------
	// Black Frost falls

	/** Black Frost is down: she kneels by it and mourns; its body goes to her as mist. */
	private void startFalling(ServerLevel level, @Nullable EntityDragonBase frost) {
		queen.setPhase(FALLING);
		scene = 0;
		move = null;
		channel = 0;
		spent = 0;
		busy = 0;
		recovering = false;
		clearShards();
		if (queen.isPassenger()) {
			queen.stopRiding();
		}
		Vec3 spot = Vfx.ground(level, queen.position());
		if (frost != null) {
			corpseAt = frost.position();
			// before its head, on the ground (its mist crosses the air to her)
			Vec3 head = frost.getHeadPosition();
			Vec3 way = head.subtract(frost.position()).multiply(1, 0, 1);
			double reach = Math.min(14, frost.getBbWidth() / 2 + 8);
			spot = Vfx.ground(level, frost.position().add(way.lengthSqr() < 0.01 ? new Vec3(0, 0, reach) : way.normalize().scale(reach)));
		}
		pin(true);
		queen.setNoGravity(false);
		queen.teleportTo(spot.x, spot.y, spot.z);
		queen.setKneeling(true);
		if (frost != null) {
			face(yawOf(frost.position().subtract(spot)));
		}
		dismissDead(level);
		RandomSource random = queen.getRandom();
		nextRays = RAYS_FIRST;
		thunder[0] = 520 + random.nextInt(200);
		thunder[1] = 1150 + random.nextInt(250);
		thunder[2] = 1750 + random.nextInt(250);
		CryonaVoice.say(level, queen, CryonaVoice.Line.LAMENT, true);
		ThroneFight.tell(queen, "iceandfire_fabric.cryona.lament", ChatFormatting.DARK_AQUA);
		com.iafabric.pixie.DuelTalk.moment(queen, com.iafabric.pixie.DuelTalk.Moment.FALLS);
		level.playSound(null, queen.blockPosition(), SoundEvents.WITHER_DEATH, SoundSource.HOSTILE, 0.6F, 1.6F);
	}

	private void tickFalling(ServerLevel level) {
		scene++;
		Entity corpse = frostId == null ? null : level.getEntity(frostId);
		if (corpse != null && scene == 4) {
			// a stream of mist from its body to her
			Vec3 from = corpse.getBoundingBox().getCenter();
			Vec3 to = queen.position().add(0, 1.2, 0);
			Vec3 way = to.subtract(from);
			float pitch = (float) Math.toDegrees(Math.atan2(-way.y, way.horizontalDistance()));
			Vfx.stream(level, from, yawOf(way), pitch, (float) way.length(), 4F, 0xC0D8F4FF, 96);
		}
		if (corpse != null && scene <= 100) {
			mist(level, corpse);
			if (scene > 40 && scene % 2 == 0) {
				// it sinks a little into the ice as it goes
				corpse.teleportTo(corpse.getX(), corpse.getY() - 0.03, corpse.getZ());
			}
			face(yawOf(corpse.position().subtract(queen.position())));
		}
		if (scene == 60) {
			level.playSound(null, queen.blockPosition(), SoundEvents.WARDEN_HEARTBEAT, SoundSource.HOSTILE, 2F, 0.7F);
			Vfx.rune(level, Vfx.ground(level, queen.position()), 3.5F, EntityVfx.DREAD, Vfx.DREAD, 70, 4F, 0F);
		}
		if (scene == MIST_END) {
			if (corpse != null) {
				Vec3 at = corpse.position();
				level.sendParticles(ParticleTypes.CLOUD, at.x, at.y + 2, at.z, 60, corpse.getBbWidth() / 2, 1.5, corpse.getBbWidth() / 2, 0.05);
				level.sendParticles(ParticleTypes.SNOWFLAKE, at.x, at.y + 2, at.z, 80, corpse.getBbWidth() / 2, 1.5, corpse.getBbWidth() / 2, 0.1);
				Vfx.shatter(level, at, 3F, 0.4F);
				remains(level, corpse);
				corpse.discard();
			}
		}
		pin(true);
		mourn(level);
		if (scene == MOURNING - 24) {
			queen.setAnimation(EntityDreadQueen.ANIMATION_SPAWN);
		}
		if (scene == MOURNING - 20) {
			// she rises for vengeance
			queen.setKneeling(false);
		}
		if (scene == MOURNING) {
			CryonaVoice.say(level, queen, CryonaVoice.Line.VENGEANCE, true);
			ThroneFight.tell(queen, "iceandfire_fabric.cryona.vengeance", ChatFormatting.RED);
			com.iafabric.pixie.DuelTalk.moment(queen, com.iafabric.pixie.DuelTalk.Moment.RISES);
			Vec3 at = Vfx.ground(level, queen.position());
			Vfx.wave(level, at, 14F, Vfx.DREAD, 18);
			Vfx.rune(level, at, 6F, EntityVfx.DREAD, Vfx.DREAD, 40, 6F, 0F);
			level.playSound(null, queen.blockPosition(), SoundEvents.ENDER_DRAGON_GROWL, SoundSource.HOSTILE, 3F, 1.4F);
			// her shield of ice whole again round her; her dead back to dust
			queen.setShield(SHIELD_MAX);
			level.playSound(null, queen.blockPosition(), com.iafabric.misc.IafSoundRegistry.CRYONA_SHIELD_UP, SoundSource.HOSTILE, 2.4F, 1F);
			dismissDead(level);
			for (LivingEntity entity : level.getEntitiesOfClass(LivingEntity.class, queen.getBoundingBox().inflate(10, 4, 10), CryonaFight::foe)) {
				Vec3 away = entity.position().subtract(queen.position()).multiply(1, 0, 1).normalize();
				hit(level, entity, 0.1F);
				BossLimits.shove(entity, away.x * 1.6, 0.5, away.z * 1.6);
			}
		}
		if (scene >= RISEN) {
			queen.setPhase(ALONE);
			queen.setShield(SHIELD_MAX);
			pin(false);
			queen.enrage(level);
			busy = 30;
			combo = 0;
			// (her great blows a while after, not all at once)
			rays = 120;
			beams = 260;
			ring = 200;
			skyfall = Math.max(skyfall, 200);
		}
	}

	/**
	 * Her mourning while the pixie's memories play: she kneels where Black Frost fell, nothing touches her; her grief
	 * wakes the dead round her foes in waves and calls rays of ice down on them (more as her anger grows); far thunder
	 * now and then; her sobs turn to anger, then fury.
	 */
	private void mourn(ServerLevel level) {
		if (scene < MIST_END || scene >= MOURNING) {
			return;
		}
		RandomSource random = queen.getRandom();
		if (scene == MEMORIES_AT + 40) {
			ThroneFight.tell(queen, "iceandfire_fabric.cryona.mourning_hint", ChatFormatting.DARK_AQUA);
		}
		if (scene % 8 == 0) {
			// her grief round her, then the dread's cold as her anger grows
			level.sendParticles(ParticleTypes.SNOWFLAKE, queen.getX(), queen.getY() + 1.2, queen.getZ(), 5, 0.6, 0.6, 0.6, 0.01);
			if (scene % 24 == 0) {
				level.sendParticles(ParticleTypes.SOUL, queen.getX(), queen.getY() + 0.6, queen.getZ(), 2, 0.5, 0.3, 0.5, 0.02);
			}
			if (scene > ANGER_AT) {
				level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, queen.getX(), queen.getY() + 0.2, queen.getZ(), 3 + (scene - ANGER_AT) / 250, 0.9, 0.1, 0.9, 0.01);
			}
		}
		switch (scene) {
			case SOB1_AT -> CryonaVoice.say(level, queen, CryonaVoice.Line.SOB1, true);
			case SOB2_AT -> CryonaVoice.say(level, queen, CryonaVoice.Line.SOB2, true);
			case ANGER_AT -> CryonaVoice.say(level, queen, CryonaVoice.Line.ANGER, true);
			case FURY_AT -> CryonaVoice.say(level, queen, CryonaVoice.Line.FURY, true);
			default -> {
			}
		}
		List<ServerPlayer> foes = ThroneFight.foesNear(queen);
		if (foes.isEmpty()) {
			return;
		}
		if (scene >= DEAD_FIRST && scene < MOURNING - 120 && (scene - DEAD_FIRST) % DEAD_EVERY == 0) {
			mourningDead(level, foes, random);
			com.iafabric.pixie.DuelTalk.moment(queen, com.iafabric.pixie.DuelTalk.Moment.DEAD);
		}
		if (scene >= nextRays && scene < MOURNING - 50) {
			float fury = Mth.clamp((scene - RAYS_FIRST) / (float) (MOURNING - RAYS_FIRST), 0F, 1F);
			if (scene == RAYS_FIRST) {
				hint("iceandfire_fabric.cryona.rays");
				com.iafabric.pixie.DuelTalk.moment(queen, com.iafabric.pixie.DuelTalk.Moment.RAYS);
			}
			rays(level, foes, random, fury < 0.35F ? 2 : fury < 0.7F ? 3 : 4, RAY_MOURNING);
			nextRays = scene + (int) Mth.lerp(fury, 150F, 80F) + random.nextInt(20);
		}
		for (int at : thunder) {
			if (scene == at) {
				thunder(level, foes, random);
			}
		}
	}

	/** A blow at her as she mourns: her grief guards her, and shows it (the first time, in words too). */
	public void untouched(ServerLevel level, DamageSource source) {
		if (!(source.getEntity() instanceof ServerPlayer)) {
			return;
		}
		level.sendParticles(ParticleTypes.END_ROD, queen.getX(), queen.getY() + 1.2, queen.getZ(), 10, 0.6, 0.8, 0.6, 0.05);
		level.sendParticles(ParticleTypes.SNOWFLAKE, queen.getX(), queen.getY() + 1.2, queen.getZ(), 12, 0.7, 0.8, 0.7, 0.05);
		Vfx.once(level, queen.position(), SoundEvents.AMETHYST_BLOCK_RESONATE, 1.2F, 1.4F, 20, 12);
		hint("iceandfire_fabric.cryona.untouchable");
	}

	/** In her mourning the dead answer her grief: a horde round her foes, wave after wave (so many of hers at once at most). */
	private void mourningDead(ServerLevel level, List<ServerPlayer> foes, RandomSource random) {
		int alive = level.getEntitiesOfClass(EntityDreadMob.class, queen.getBoundingBox().inflate(120, 60, 120), m -> queen.getUUID().equals(m.getCommanderId())).size();
		// a horde: five more each time for one player (two more for each other one), up to twelve of hers (four more for each)
		int raise = Math.min(5 + 2 * (foes.size() - 1), 12 + 4 * (foes.size() - 1) - alive);
		for (int i = 0; i < raise; i++) {
			ServerPlayer foe = foes.get(i % foes.size());
			double a = random.nextDouble() * Math.PI * 2;
			double r = 5 + random.nextDouble() * 7;
			Vec3 at = Vfx.ground(level, floorUnder(level, foe).add(Math.cos(a) * r, 2, Math.sin(a) * r));
			minion(level, at, foe, random, MOURNING - scene + 60);
		}
	}

	/** Thunder far off over the ice, now and then (only its light and its roll: nothing is struck, nothing burns). */
	private void thunder(ServerLevel level, List<ServerPlayer> foes, RandomSource random) {
		Vec3 middle = center();
		for (int attempt = 0; attempt < 10; attempt++) {
			double a = random.nextDouble() * Math.PI * 2;
			double r = 34 + random.nextDouble() * 34;
			Vec3 at = middle.add(Math.cos(a) * r, 0, Math.sin(a) * r);
			boolean far = true;
			for (ServerPlayer foe : foes) {
				far &= foe.position().subtract(at).horizontalDistanceSqr() > 28 * 28;
			}
			if (!far) {
				continue;
			}
			net.minecraft.world.entity.LightningBolt bolt = net.minecraft.world.entity.EntityType.LIGHTNING_BOLT.create(level);
			if (bolt == null) {
				return;
			}
			Vec3 ground = Vfx.ground(level, at.add(0, 4, 0));
			bolt.moveTo(ground.x, ground.y, ground.z, 0F, 0F);
			bolt.setVisualOnly(true);
			level.addFreshEntity(bolt);
			return;
		}
	}

	/** Its body flows to her as mist. */
	private void mist(ServerLevel level, Entity corpse) {
		RandomSource random = queen.getRandom();
		Vec3 to = queen.position().add(0, 1.2, 0);
		AABB box = corpse.getBoundingBox();
		for (int i = 0; i < 12; i++) {
			Vec3 from = new Vec3(Mth.lerp(random.nextDouble(), box.minX, box.maxX), Mth.lerp(random.nextDouble(), box.minY, box.maxY),
				Mth.lerp(random.nextDouble(), box.minZ, box.maxZ));
			Vec3 way = to.subtract(from);
			double speed = way.length() / 16.0;
			Vec3 dir = way.normalize();
			level.sendParticles(i % 3 == 0 ? ParticleTypes.END_ROD : i % 3 == 1 ? ParticleTypes.SOUL_FIRE_FLAME : ParticleTypes.SNOWFLAKE, from.x, from.y, from.z, 0,
				dir.x, dir.y, dir.z, speed);
		}
		level.sendParticles(ParticleTypes.SOUL, box.getCenter().x, box.maxY - 0.5, box.getCenter().z, 2, box.getXsize() / 3, 0.3, box.getZsize() / 3, 0.02);
	}

	/** What is left of Black Frost on the ice: its blood (for the long Frost Ward), its heart, bones and scales. */
	private void remains(ServerLevel level, Entity corpse) {
		RandomSource random = queen.getRandom();
		// where it lay (before it sank), a little up
		Vec3 at = (corpseAt != null ? corpseAt : corpse.position()).add(0, 1.2, 0);
		List<ItemStack> left = new ArrayList<>();
		left.add(new ItemStack(IafItemRegistry.ICE_DRAGON_BLOOD, 4 + random.nextInt(3)));
		left.add(new ItemStack(IafItemRegistry.ICE_DRAGON_HEART));
		left.add(new ItemStack(IafItemRegistry.DRAGON_BONE, 6 + random.nextInt(5)));
		if (corpse instanceof EntityDragonBase dragon) {
			left.add(new ItemStack(dragon.getVariantScale(dragon.getVariant()), 8 + random.nextInt(5)));
		}
		for (ItemStack stack : left) {
			net.minecraft.world.entity.item.ItemEntity item = new net.minecraft.world.entity.item.ItemEntity(level, at.x, at.y, at.z, stack);
			item.setDeltaMovement((random.nextDouble() - 0.5) * 0.3, 0.3, (random.nextDouble() - 0.5) * 0.3);
			item.setUnlimitedLifetime();
			level.addFreshEntity(item);
		}
	}

	// ---------------------------------------------------------------------------------------------------------------
	// her end

	/** Her end (a killing blow leaves her a breath of life for it): she kneels, says her last words, entrusts her weapons. */
	public void fall(ServerLevel level, DamageSource source) {
		if (queen.getPhase() == DYING || queen.getPhase() == GONE) {
			return;
		}
		queen.setPhase(DYING);
		scene = 0;
		move = null;
		channel = 0;
		spent = 0;
		busy = 0;
		recovering = false;
		clearShards();
		if (queen.isPassenger()) {
			queen.stopRiding();
		}
		pin(true);
		queen.setNoGravity(false);
		Vec3 floor = Vfx.ground(level, queen.position());
		queen.teleportTo(floor.x, floor.y, floor.z);
		queen.setKneeling(true);
		Player by = source.getEntity() instanceof Player player ? player : null;
		if (by == null) {
			List<ServerPlayer> foes = ThroneFight.foesNear(queen);
			by = foes.isEmpty() ? level.getNearestPlayer(queen, 64) : nearestTo(foes);
		}
		heir = by == null ? null : by.getUUID();
		if (by != null) {
			// she kneels before who beat her
			face(yawOf(by.position().subtract(queen.position()).multiply(1, 0, 1)));
		}
		for (EntityDreadMob minion : level.getEntitiesOfClass(EntityDreadMob.class, queen.getBoundingBox().inflate(120, 60, 120), m -> queen.getUUID().equals(m.getCommanderId()))) {
			level.sendParticles(ParticleTypes.SOUL, minion.getX(), minion.getY() + 1, minion.getZ(), 8, 0.3, 0.5, 0.3, 0.02);
			minion.discard();
		}
		CryonaVoice.say(level, queen, CryonaVoice.Line.DEATH, true);
		level.playSound(null, queen.blockPosition(), SoundEvents.BEACON_DEACTIVATE, SoundSource.HOSTILE, 2F, 0.6F);
	}

	private void tickDying(ServerLevel level) {
		scene++;
		RandomSource random = queen.getRandom();
		Player to = heir == null ? null : level.getPlayerByUUID(heir);
		if (scene % 3 == 0) {
			int n = 2 + scene / 30;
			level.sendParticles(ParticleTypes.SNOWFLAKE, queen.getX(), queen.getY() + 1.2, queen.getZ(), n, 0.35, 0.7, 0.35, 0.02);
		}
		if (scene == 60) {
			// her sword and her staff rise from her hands
			Vec3 body = queen.position();
			Vec3 side = dirOf(queen.yBodyRot + 90F);
			sword = Vfx.item(level, body.add(side.scale(-0.5)).add(0, 1.1, 0), queen.getItemBySlot(EquipmentSlot.MAINHAND).isEmpty()
				? new ItemStack(IafItemRegistry.DREAD_QUEEN_SWORD) : queen.getItemBySlot(EquipmentSlot.MAINHAND).copy(), 1.3F);
			staff = Vfx.item(level, body.add(side.scale(0.5)).add(0, 1.1, 0), queen.getItemBySlot(EquipmentSlot.OFFHAND).isEmpty()
				? new ItemStack(IafItemRegistry.DREAD_QUEEN_STAFF) : queen.getItemBySlot(EquipmentSlot.OFFHAND).copy(), 1.3F);
			queen.setItemSlot(EquipmentSlot.MAINHAND, ItemStack.EMPTY);
			queen.setItemSlot(EquipmentSlot.OFFHAND, ItemStack.EMPTY);
			level.playSound(null, queen.blockPosition(), SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.HOSTILE, 2F, 0.8F);
		}
		if (scene > 60 && scene < 118 && scene % 2 == 0) {
			// they float to who beat her
			float f = (scene - 60) / 58F;
			Vec3 target = to != null ? to.position().add(0, 1.4, 0) : queen.position().add(0, 2.5, 0);
			Vec3 lift = queen.position().add(0, 2.8, 0);
			for (int k = 0; k < 2; k++) {
				Display.ItemDisplay display = k == 0 ? sword : staff;
				if (display == null || display.isRemoved()) {
					continue;
				}
				Vec3 from = display.position();
				Vec3 goal = f < 0.3F ? lift.add(dirOf(queen.yBodyRot + 90F).scale(k == 0 ? -0.6 : 0.6))
					: lift.lerp(target, (f - 0.3) / 0.7).add(dirOf(queen.yBodyRot + 90F).scale(k == 0 ? -0.5 : 0.5));
				Vfx.move(display, from.lerp(goal, 0.5), 2);
				level.sendParticles(ParticleTypes.END_ROD, from.x, from.y, from.z, 1, 0.05, 0.05, 0.05, 0.01);
			}
		}
		if (scene == 118) {
			// in their hands
			Vfx.remove(sword);
			Vfx.remove(staff);
			sword = null;
			staff = null;
			ItemStack[] weapons = {new ItemStack(IafItemRegistry.DREAD_QUEEN_SWORD), new ItemStack(IafItemRegistry.DREAD_QUEEN_STAFF)};
			for (ItemStack weapon : weapons) {
				if (to == null || !to.isAlive() || !to.getInventory().add(weapon)) {
					Vec3 at = to != null ? to.position() : queen.position();
					net.minecraft.world.entity.item.ItemEntity item = new net.minecraft.world.entity.item.ItemEntity(level, at.x, at.y + 0.5, at.z, weapon);
					item.setUnlimitedLifetime();
					level.addFreshEntity(item);
				}
			}
			if (to instanceof ServerPlayer player) {
				player.sendSystemMessage(net.minecraft.network.chat.Component.translatable("iceandfire_fabric.cryona.weapons").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD));
				level.playSound(null, player.blockPosition(), SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 1F, 0.8F);
			}
		}
		if (scene >= 120 && scene % 2 == 0) {
			// she fades into the cold
			level.sendParticles(ParticleTypes.END_ROD, queen.getX(), queen.getY() + 1, queen.getZ(), 3, 0.3, 0.8, 0.3, 0.02);
			level.sendParticles(ParticleTypes.CLOUD, queen.getX(), queen.getY() + 0.8, queen.getZ(), 2, 0.3, 0.6, 0.3, 0.01);
		}
		if (scene >= DEATH_SCENE) {
			level.sendParticles(ParticleTypes.SNOWFLAKE, queen.getX(), queen.getY() + 1, queen.getZ(), 80, 0.5, 1, 0.5, 0.08);
			level.playSound(null, queen.blockPosition(), SoundEvents.ILLUSIONER_MIRROR_MOVE, SoundSource.HOSTILE, 2F, 0.6F);
			queen.setPhase(GONE);
			queen.setInvisible(true);
			pin(false);
			if (!ThroneFight.inRealm(queen) && queen.getThrone() != null) {
				CastleWard.free(level, queen.getThrone());
			}
			Entity killer = to;
			queen.hurt(killer instanceof Player player ? level.damageSources().playerAttack(player) : level.damageSources().magic(), Float.MAX_VALUE);
			if (queen.isAlive()) {
				queen.kill();
			}
		}
	}

	/** Her mourning's bar: what is left of it (1 to 0) while the memories play; -1 when she does not mourn. */
	public static float mourning(EntityDreadQueen queen) {
		CryonaFight fight = FIGHTS.get(queen);
		if (fight == null || queen.getPhase() != FALLING || fight.scene < MEMORIES_AT) {
			return -1F;
		}
		return Mth.clamp(1F - (fight.scene - MEMORIES_AT) / (float) (MOURNING - MEMORIES_AT), 0F, 1F);
	}

	/** The soundtrack of her fight now (Soundtracks): the duel's; the pixie's memories while she mourns; none at her end. */
	public static int trackOf(EntityDreadQueen queen) {
		return switch (queen.getPhase()) {
			case FALLING -> Soundtracks.MEMORIES;
			case DYING, GONE -> Soundtracks.NONE;
			default -> Soundtracks.DUEL;
		};
	}

	/** For the tests: her mourning goes on to that many ticks before she rises. */
	public void hurry(int left) {
		if (queen.getPhase() == FALLING) {
			scene = Math.max(scene, MOURNING - left);
		}
	}

	/** For the tests: how far her mourning is. */
	public int scene() {
		return scene;
	}

	private void clearShards() {
		for (Shard shard : shards) {
			Vfx.remove(shard.display);
		}
		shards.clear();
	}
}

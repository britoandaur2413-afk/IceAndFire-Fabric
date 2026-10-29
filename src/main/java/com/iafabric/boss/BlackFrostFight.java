package com.iafabric.boss;

import com.iafabric.effect.IafEffects;
import com.iafabric.entity.EntityDragonBase;
import com.iafabric.entity.EntityDreadQueen;
import com.iafabric.entity.EntityVfx;
import com.iafabric.vfx.Vfx;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Display;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Interaction;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
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
import java.util.WeakHashMap;

/**
 * Black Frost's powers, each shown before it comes: a charge (a lane drawn on the ground from it through its foe and
 * far past them; it crouches, then rushes down it, ice and snow thrown up at its feet and shards of ice rising along
 * its way, throwing aside who is in the way; into a pillar or an obelisk and it is stunned), a dive (it climbs high
 * over its foe, a circle marks where it will land; it falls like a stone, ice bursts round it and it is stunned a
 * while), hail (rings of circles round its foe where blocks of ice will fall, in two waves, never one circle over
 * another) and a glacial roar (its circle fills, then all near are thrown back and slowed). Its breath, from afar and
 * up close: a lane shows before it, then a jet of frost sweeps down it to the ground; without the exiled pixie's gift
 * it melts who it catches, with the gift it is a cold blow their armor lessens (BossCold.breathOf). Rarely, the Glacial
 * Prison: a great deep blue circle fills slowly round its foe; who is still in it when it is full is frozen solid for
 * a while, Frost Ward or not. Near its end (under a third of its life) its breath grows: the super breath, longer and
 * wider, blowing longer. Stunned, it takes half again; while Cryona on its back calls Absolute Zero it is shielded.
 * Its head follows its foe, but its long neck only bows a little: never into the ice.
 * <p>
 * In its realm its fight alone leads it (no mind of its own between blows, yet its head follows its foe and its body
 * breathes), and it changes its way of fighting every twenty seconds or so, as a living thing would:
 * <ul>
 * <li>from afar, on one of its perches round the arena (marked in the ice, open ground where it is seen whole): volleys
 * of ice (each shard falls where a circle shows), hail, the prison; the cold round it slows who comes near and a gust
 * drives them back now and then; who reaches it is roared away, taken in its claws (a circle closes round their feet;
 * still in it, they are lifted and thrown far: the ice takes a share of their life, the height nothing), or sees it
 * come down to fight them, or (now and then) fly off;</li>
 * <li>close, on foot: it runs at its nearest foe and strikes, each blow shown by a circle as it winds up (a bite before
 * it, its tail round its hindquarters, a stomp at its forefeet), and rests a moment after each (the time to strike
 * it); far off, it leaps at them (its dive); after a while, or after a share of its life, it flies away to a perch,
 * sweeping round the arena;</li>
 * <li>a stampede: three rushes from edge to edge of the arena, each down a lane drawn through its foe (into a pillar
 * or an obelisk and it is stunned), then it stands winded and fights on foot.</li>
 * </ul>
 * Its dive, a crash, a grab dodged, a stampede's end and the end of Absolute Zero leave it stunned: the time to strike.
 */
public final class BlackFrostFight {
	private static final Map<EntityDragonBase, BlackFrostFight> FIGHTS = new WeakHashMap<>();
	/** Who falls with no life lost to the height (thrown or let go by it), until a game time. */
	private static final Map<UUID, Long> SAFE_FALL = new HashMap<>();
	private static final int WIND_UP = 35;
	private static final int RUSH = 12;
	/** The Glacial Prison: its circle, how long it fills, how long who stays in it is frozen. */
	private static final float PRISON_RADIUS = 8F;
	private static final int PRISON_FILL = 60;
	private static final int PRISON_HOLD = 80;
	private static final int PRISON_COLOR = 0xFF3A6BFF;
	/** The super breath: its wind-up, how long it blows, its lane (longer in its realm, where it keeps far). */
	private static final int BREATH_WIND_UP = 22;
	/** The heart of its dive's circle: nine tenths of the life of who is there (BossLimits.doom). */
	/** The share of their life its dive takes from all in its circle (golden hearts do not take it: BossLimits.doom). */
	private static final float DIVE_SHARE = 0.75F;
	/** Where its dive falls (all in it are struck). */
	private static final float DIVE_RADIUS = 11F;
	private static final int BREATH_BLOW = 34;
	private static final float BREATH_LENGTH = 36F;
	private static final float BREATH_LENGTH_REALM = 56F;
	private static final float BREATH_WIDTH = 11F;
	/** Where the blast comes in each breath's sound (ticks from its start): the great breath's, the frost breath's. */
	private static final int GREAT_BREATH_BLAST = 9;
	private static final int BREATH_BLAST = 20;
	/** Its great breath's rhythm (ticks between two), and near its end. */
	private static final int GREAT_BREATH_EVERY = 560;
	private static final int GREAT_BREATH_DESPERATE = 380;
	/** Its breath any time (not the super one): wind-up, blowing, lane width and longest lane. */
	private static final int FROST_BREATH_WIND_UP = 28;
	private static final int FROST_BREATH_BLOW = 22;
	private static final float FROST_BREATH_WIDTH = 6.5F;
	private static final float FROST_BREATH_LENGTH = 40F;
	/** Its charge: blocks a tick, and how far past its foe it rushes. */
	private static final double CHARGE_SPEED = 2.1;
	private static final double CHARGE_PAST = 30;
	/** Its hail: each circle's size. */
	private static final float HAIL_RADIUS = 2.6F;
	/** How far its head bows as it breathes down its lane (degrees): a few, its neck is long. */
	private static final float HEAD_DOWN = 8F;
	/** Its claws: the circle round its foe's feet, its wind-up, the lift and the hold before the throw. */
	private static final float GRAB_RADIUS = 4.5F;
	private static final int GRAB_WIND = 20;
	/** How soon its claws are ready again (ticks, and a few more at random). */
	private static final int GRAB_AGAIN = 140;
	private static final int GRAB_LIFT = 26;
	private static final int GRAB_HOLD = 10;
	private static final double GRAB_HEIGHT = 13;
	/** Its flight from perch to perch: blocks a tick. */
	private static final double FLY_SPEED = 0.8;
	/** Its flight to the middle for Absolute Zero (Cryona's call waits for it). */
	private static final int HOVER_FLY = 30;
	/** Its volley: shards of ice spat at its foe, the ticks each takes to fall. */
	private static final int SHARDS = 3;
	private static final int SHARD_FLIGHT = 16;
	private static final float SHARD_RADIUS = 3F;
	/**
	 * Its pylons (from three quarters of its life down): crystals of ice rising round the arena, a light over each to
	 * the sky; three blows break one, and its light runs to Black Frost: a twentieth of its life. They fade after a while;
	 * new ones rise a while after the last is gone.
	 */
	private static final float PYLONS_FROM = 0.75F;
	private static final int PYLONS_EVERY = 900;
	private static final int PYLON_LIFE = 800;
	private static final int PYLON_HITS = 3;
	private static final float PYLON_SHARE = 0.05F;
	private static final float PYLON_HEIGHT = 6F;
	private static final float PYLON_WIDTH = 1.8F;
	public static final String PYLON_TAG = "iceandfire_fabric_pylon";
	/** Not known yet which perch it stands on. */
	private static final int UNKNOWN_PERCH = -2;
	/** Its ways of fighting in its realm, each a while: from a perch afar, close on foot, a stampede of rushes. */
	private static final int RANGED = 0;
	private static final int MELEE = 1;
	private static final int STAMPEDE = 2;
	/** On foot: its run (blocks a tick), and its blows. */
	private static final double RUN_SPEED = 0.32;
	private static final int BITE = 0;
	private static final int TAIL = 1;
	private static final int STOMP = 2;
	/** A stampede's rush: blocks a tick, and how far from the middle it runs (the arena's edge, short of its spikes). */
	private static final double RUSH_SPEED = 1.4;
	private static final double RUSH_EDGE = 100;
	private final EntityDragonBase dragon;
	private int busy = 60;
	private int charge = 100;
	private int dive = 260;
	private int hail = 150;
	private int roar = 200;
	private int prison = 900;
	private int breath = 360;
	/** The breaths it took (a sound waiting for one never plays for another). */
	private int breaths;
	private final List<Pylon> pylons = new ArrayList<>();
	private int pylonTimer = 40;
	/** When it last roared (its own time). */
	private int lastRoar = -1000;
	private static final int ROAR_EVERY = 120;
	/** Pylons of an older fight (the world saved with them standing) are swept away once. */
	private boolean pylonsSwept;
	/** Its breath any time (the super one has its own wait, breath). */
	private int frostBreath = 160;
	private boolean greatBreath;
	private float breathWidth = BREATH_WIDTH;
	private int breathWind = BREATH_WIND_UP;
	private int breathBlow = BREATH_BLOW;
	private int stunned;
	private int chargeTick = -1;
	private Vec3 chargeFrom;
	private Vec3 chargeTo;
	private final Set<UUID> hit = new HashSet<>();
	private int diveTick = -1;
	private Vec3 diveFrom;
	private Vec3 diveSpot;
	private int prisonTick = -1;
	private Vec3 prisonAt;
	private int breathTick = -1;
	private Vec3 breathFrom;
	private float breathYaw;
	private float breathLength = BREATH_LENGTH;
	private int hoverTicks;
	private int hoverTotal;
	private Vec3 hoverFrom;
	private Vec3 hoverAt;
	/** Led in its realm (ThroneFight): all it does is its own here, its own mind never takes over between. */
	private boolean steered;
	private @Nullable Vec3 arena;
	private int perch = UNKNOWN_PERCH;
	private int perchTime;
	/** How long before it flies off again from who came too close (a while to strike it, after it landed). */
	private int away = 60;
	private int volley = 50;
	private int grab = 120;
	private int flyTick = -1;
	private int flyTicks;
	private int flyPerch;
	private Vec3 flyFrom;
	private Vec3 flyTo;
	private double flyPeak;
	private int grabTick = -1;
	private Vec3 grabAt;
	private @Nullable UUID held;
	private Vec3 liftFrom;
	/** Who it threw, and the ticks since: their fall takes no life from the height, their landing a share. */
	private final Map<UUID, Integer> thrown = new HashMap<>();
	/** Ticks to the next gust of its cold. */
	private int gust;
	private int stance = RANGED;
	private int stanceLeft = 300;
	/** Its life when this way of fighting began (on foot, it flies off after a share of it). */
	private float stanceLife;
	/** How it fights when it is up again after a stun (-1: as it was). */
	private int nextStance = -1;
	private int strikeTick = -1;
	/** Stomps one after another (a foe kept too close before it), and its steps back from them. */
	private int stomps;
	private int backTicks;
	private int strikeKind;
	private int strikeWind;
	private Vec3 strikeAt;
	private float strikeRadius;
	/** A stampede: rushes left, and whether the charge under way is one. */
	private int rushes;
	private boolean stampeding;
	private int chargeWind = WIND_UP;
	private int rushTicks = RUSH;
	/** The bend of its flight (it sweeps round the arena from perch to perch). */
	private Vec3 flyControl;

	private BlackFrostFight(EntityDragonBase dragon) {
		this.dragon = dragon;
	}

	/** One of its pylons: its crystal, the shard of light on it, its light to the sky, its circle, what is struck. */
	private static final class Pylon {
		Vec3 at;
		Display.@Nullable BlockDisplay crystal;
		Display.@Nullable BlockDisplay tip;
		@Nullable EntityVfx beam;
		@Nullable EntityVfx rune;
		@Nullable Interaction box;
		final List<Display.BlockDisplay> shards = new ArrayList<>();
		int hits;
		int age;
	}

	/** Its duel waits (all its players fell or left): no blow is left halfway. */
	public void stop() {
		chargeTick = -1;
		diveTick = -1;
		prisonTick = -1;
		cutBreath();
		hoverTicks = 0;
		stunned = 0;
		flyTick = -1;
		strikeTick = -1;
		backTicks = 0;
		stomps = 0;
		rushes = 0;
		stampeding = false;
		stance = RANGED;
		stanceLeft = 300;
		nextStance = -1;
		release();
		if (dragon.level() instanceof ServerLevel level) {
			clearPylons(level);
			sweepPylons(level);
		}
		perch = UNKNOWN_PERCH;
		busy = Math.max(busy, 60);
		hit.clear();
		dragon.fightLed = false;
		dragon.setHovering(false);
		dragon.setFlying(false);
		dragon.getNavigation().stop();
		dragon.setDeltaMovement(Vec3.ZERO);
	}

	public static BlackFrostFight of(EntityDragonBase dragon) {
		return FIGHTS.computeIfAbsent(dragon, BlackFrostFight::new);
	}

	/** What a blow does to it: half again while it is stunned, a quarter while Cryona on its back calls Absolute Zero. */
	public static float damageTaken(EntityDragonBase dragon, float amount) {
		if (dragon.getFirstPassenger() instanceof EntityDreadQueen queen && CryonaFight.channeling(queen)) {
			return amount * 0.25F;
		}
		BlackFrostFight fight = FIGHTS.get(dragon);
		return fight != null && fight.stunned > 0 ? amount * 1.5F : amount;
	}

	/** Near its end (under a third of its life): its breath grows. */
	public static boolean desperate(EntityDragonBase dragon) {
		return dragon.getHealth() < dragon.getMaxHealth() * 0.3F;
	}

	private static boolean foe(LivingEntity entity) {
		return entity instanceof ServerPlayer player && player.isAlive() && !player.isCreative() && !player.isSpectator();
	}

	/** Whether it is in one of its great blows or on its way (Cryona waits for it). */
	public boolean inGreatBlow() {
		return chargeTick >= 0 || diveTick >= 0 || prisonTick >= 0 || breathTick >= 0 || hoverTicks > 0 || flyTick >= 0 || grabTick >= 0;
	}

	/** Its powers where Cryona woke outside her realm (from DreadDragons). */
	public void tickOutside(ServerLevel level) {
		steered = false;
		tick(level);
	}

	/** Its powers outside its realm: between them its own mind leads it. */
	void tick(ServerLevel level) {
		RandomSource random = dragon.getRandom();
		if (hoverTicks > 0) {
			tickHover(level);
			return;
		}
		if (flyTick >= 0) {
			tickFlight(level);
			return;
		}
		if (grabTick >= 0) {
			tickGrab(level);
			return;
		}
		if (chargeTick >= 0) {
			tickCharge(level);
			return;
		}
		if (diveTick >= 0) {
			tickDive(level);
			return;
		}
		if (prisonTick >= 0) {
			tickPrison(level);
			return;
		}
		if (breathTick >= 0) {
			tickBreath(level);
			return;
		}
		if (stunned > 0) {
			tickStunned(level);
			return;
		}
		if (busy > 0) {
			busy--;
			return;
		}
		List<ServerPlayer> foes = ThroneFight.foesNear(dragon);
		if (foes.isEmpty()) {
			return;
		}
		ServerPlayer nearest = nearest(foes);
		if (dragon.getTarget() == null || !dragon.getTarget().isAlive()) {
			dragon.setTarget(nearest);
		}
		charge--;
		dive--;
		hail--;
		roar--;
		prison--;
		breath--;
		frostBreath--;
		if (breath <= 0 && nearest.distanceToSqr(dragon) < 50 * 50) {
			breath = (desperate(dragon) ? GREAT_BREATH_DESPERATE : GREAT_BREATH_EVERY) + random.nextInt(100);
			startBreath(level, nearest, true);
		} else if (frostBreath <= 0 && nearest.distanceToSqr(dragon) < 34 * 34) {
			frostBreath = 260 + random.nextInt(120);
			startBreath(level, nearest, false);
		} else if (prison <= 0 && foes.size() > 0) {
			prison = 1500 + random.nextInt(600);
			startPrison(level, foes.get(random.nextInt(foes.size())));
		} else if (dive <= 0 && dragon.getHealth() < dragon.getMaxHealth() * 0.9F) {
			dive = 300 + random.nextInt(100);
			startDive(level, nearest);
		} else if (charge <= 0 && nearest.distanceToSqr(dragon) > 8 * 8) {
			charge = 220 + random.nextInt(80);
			startCharge(level, nearest);
		} else if (hail <= 0) {
			hail = 170 + random.nextInt(60);
			hail(level, foes, random);
		} else if (roar <= 0 && nearest.distanceToSqr(dragon) < 14 * 14) {
			roar = 240;
			glacialRoar(level);
		}
	}

	/**
	 * Its fight in its realm (from ThroneFight, the arena's middle given): from afar, on foot, in a stampede, as it
	 * chooses; its head always on its nearest foe.
	 */
	void tickRealm(ServerLevel level, Vec3 center) {
		steered = true;
		arena = center;
		lead();
		if (breathTick < 0 && dragon.isBreathingFire()) {
			// a breath of its own mind from before the duel led it (it went on drawn): here only the duel's breaths are
			dragon.setBreathingFire(false);
		}
		tickThrown(level);
		if (perch == UNKNOWN_PERCH) {
			perch = perchAt(dragon.position());
		}
		List<ServerPlayer> foes = ThroneFight.foesNear(dragon);
		ServerPlayer nearest = foes.isEmpty() ? null : nearest(foes);
		tickPylons(level, foes);
		act(level, foes, nearest);
		if (nearest != null && breathTick < 0 && flyTick < 0 && hoverTicks <= 0) {
			lookAt(nearest);
		}
	}

	/** Its fight leads it: no mind of its own (no foe, no step, no bite of its own), but no stiff NoAi mob either. */
	private void lead() {
		dragon.fightLed = true;
		dragon.setTarget(null);
		if (dragon.isNoAi() && !dragon.isModelDead()) {
			dragon.setNoAi(false);
		}
	}

	private void act(ServerLevel level, List<ServerPlayer> foes, @Nullable ServerPlayer nearest) {
		if (hoverTicks > 0) {
			tickHover(level);
			return;
		}
		if (flyTick >= 0) {
			tickFlight(level);
			return;
		}
		if (grabTick >= 0) {
			tickGrab(level);
			return;
		}
		if (chargeTick >= 0) {
			tickCharge(level);
			return;
		}
		if (diveTick >= 0) {
			tickDive(level);
			return;
		}
		if (prisonTick >= 0) {
			tickPrison(level);
			return;
		}
		if (breathTick >= 0) {
			tickBreath(level);
			return;
		}
		if (strikeTick >= 0) {
			tickStrike(level);
			return;
		}
		if (stunned > 0) {
			tickStunned(level);
			return;
		}
		if (nearest == null) {
			return;
		}
		if (backTicks > 0) {
			stepBack(level, nearest);
			return;
		}
		dive--;
		hail--;
		roar--;
		prison--;
		breath--;
		frostBreath--;
		grab--;
		volley--;
		away--;
		stanceLeft--;
		if (breath <= 0 && busy <= 0 && strikeTick < 0 && stance != STAMPEDE && flat(nearest) < 52) {
			// its great breath, on a rhythm of its own (sooner near its end): it rears and lets it go at once
			breath = (desperate(dragon) ? GREAT_BREATH_DESPERATE : GREAT_BREATH_EVERY) + dragon.getRandom().nextInt(100);
			startBreath(level, nearest, true);
			return;
		}
		switch (stance) {
			case MELEE -> melee(level, foes, nearest);
			case STAMPEDE -> stampede(level, nearest);
			default -> ranged(level, foes, nearest);
		}
	}

	/**
	 * Its head stays with its body, level: its whole body turns to its foe instead. (Its long neck turned and bowed to a
	 * player close by, under its neck or at its forelegs, twisted it aside and bent its jaws down into the ice.)
	 */
	private void lookAt(LivingEntity foe) {
		dragon.yHeadRot = Mth.approachDegrees(dragon.yHeadRot, dragon.yBodyRot, 12F);
		dragon.setXRot(Mth.approach(dragon.getXRot(), 0F, 3F));
	}

	/** How far before it its jaws reach on the ice (a bite falls there, never under its own neck). */
	private double jawsReach() {
		return dragon.getRenderSize() * 0.55;
	}

	/**
	 * Where its jaws are: on the ice, before it at its size (the game's own head position counts a flight's lift a while
	 * after it landed, which sent its breath from over its head); in the air, the game's own.
	 */
	private Vec3 jaws() {
		if (dragon.isFlying() || dragon.isHovering()) {
			return dragon.getHeadPosition();
		}
		float size = dragon.getRenderSize();
		return dragon.position().add(forward().scale(size * 0.6)).add(0, size * 0.22, 0);
	}

	/** Its roar (long and loud): not again within six seconds, never one over another. */
	private void roar() {
		if (dragon.tickCount - lastRoar >= ROAR_EVERY || dragon.tickCount < lastRoar) {
			lastRoar = dragon.tickCount;
			dragon.roar();
		}
	}

	/** Where it faces, along the ground. */
	private Vec3 forward() {
		double r = Math.toRadians(dragon.getYRot());
		return new Vec3(-Math.sin(r), 0, Math.cos(r));
	}

	/** A new way of fighting, for a while. */
	private void beginStance(ServerLevel level, int next) {
		stance = next;
		stanceLife = dragon.getHealth();
		RandomSource random = dragon.getRandom();
		switch (next) {
			case MELEE -> {
				stanceLeft = 320 + random.nextInt(120);
				perch = -1;
				busy = Math.max(busy, 16);
				// (down to fight on foot it comes at them first; its breaths wait a while)
				frostBreath = Math.max(frostBreath, 80);
				breath = Math.max(breath, 80);
				roar();
				ThroneFight.tell(dragon, "iceandfire_fabric.frost.melee", ChatFormatting.GOLD);
			}
			case STAMPEDE -> {
				rushes = 3;
				stanceLeft = 600;
				busy = Math.max(busy, 10);
			}
			default -> stanceLeft = 400 + random.nextInt(160);
		}
	}

	/** From afar, on its perch: volleys, hail, the prison; the cold round it; who reaches it is met. */
	private void ranged(ServerLevel level, List<ServerPlayer> foes, ServerPlayer nearest) {
		turnTo(nearest.position(), 6F);
		if (perch >= 0) {
			aura(level, foes);
		}
		if (busy > 0) {
			busy--;
			return;
		}
		RandomSource random = dragon.getRandom();
		double d = flat(nearest);
		double reach = reach();
		if (perch < 0) {
			// off its perches: to one at a good distance from them
			flyToPerch(level, foes, false);
		} else if (d < reach && grab <= 0) {
			grab = GRAB_AGAIN + random.nextInt(40);
			startGrab(level, nearest);
		} else if (d < reach + 6 && roar <= 0) {
			roar = 260;
			glacialRoar(level);
		} else if (d < reach + 6) {
			// who stands at its feet: now and then it takes to the air, else it comes down to fight them there
			if (away <= 0 && random.nextInt(3) == 0) {
				flyToPerch(level, foes, true);
				away = 500;
			} else {
				beginStance(level, MELEE);
			}
		} else if (stanceLeft <= 0) {
			beginStance(level, random.nextInt(100) < 55 ? MELEE : STAMPEDE);
		} else if (prison <= 0) {
			prison = 1500 + random.nextInt(600);
			startPrison(level, foes.get(random.nextInt(foes.size())));
		} else if (dive <= 0 && dragon.getHealth() < dragon.getMaxHealth() * 0.9F && d > 20) {
			// a leap at them: it fights on foot where it lands
			dive = 320 + random.nextInt(100);
			nextStance = MELEE;
			startDive(level, nearest);
		} else if (frostBreath <= 0 && d < 52) {
			frostBreath = 240 + random.nextInt(120);
			startBreath(level, nearest, false);
		} else if (volley <= 0) {
			volley = 110 + random.nextInt(50);
			volley(level, nearest);
		} else if (hail <= 0) {
			hail = 190 + random.nextInt(60);
			hail(level, foes, random);
		}
	}

	/**
	 * Close, on foot: it runs at its nearest foe and strikes (a bite before it, its tail round its hindquarters and
	 * sides, a stomp at its forefeet, now and then its claws), resting a moment after each blow. Far off, it leaps at
	 * them; after a while or a share of its life, it flies off to a perch.
	 */
	private void melee(ServerLevel level, List<ServerPlayer> foes, ServerPlayer nearest) {
		if (busy > 0) {
			busy--;
			turnTo(nearest.position(), 5F);
			return;
		}
		RandomSource random = dragon.getRandom();
		if (stanceLeft <= 0 || stanceLife - dragon.getHealth() > dragon.getMaxHealth() * 0.04F) {
			// enough of it on foot: up and away, sweeping round the arena to a perch far from them
			flyToPerch(level, foes, true);
			return;
		}
		double d = flat(nearest);
		Vec3 forward = forward();
		Vec3 rel = nearest.position().subtract(dragon.position()).multiply(1, 0, 1);
		double front = rel.dot(forward);
		double jaws = jawsReach();
		if (frostBreath <= 0 && front > jaws - 6 && d < 34) {
			// its breath before it, close (out of the lane, to its side, is safe)
			frostBreath = 220 + random.nextInt(100);
			startBreath(level, nearest, false);
			return;
		}
		if (d > 40 && dive <= 0) {
			dive = 320 + random.nextInt(100);
			nextStance = MELEE;
			startDive(level, nearest);
			return;
		}
		if (d > jaws + 4) {
			run(level, nearest.position());
			return;
		}
		// (each blow where its body truly reaches: its jaws far before it, its forefeet under its chest, its tail behind)
		float off = Math.abs(Mth.wrapDegrees((float) Math.toDegrees(Math.atan2(-rel.x, rel.z)) - dragon.getYRot()));
		if (front < -2 && d < dragon.getBbWidth() * 1.2 + 4) {
			stomps = 0;
			startStrike(level, TAIL, nearest);
		} else if (off > 40) {
			// beside it: it turns its whole body to them first
			turnTo(nearest.position(), 9F);
		} else if (front >= jaws - 6) {
			stomps = 0;
			startStrike(level, BITE, nearest);
		} else if (grab <= 0) {
			// a player close by its chest: its claws first, whenever they are ready
			grab = GRAB_AGAIN + random.nextInt(40);
			startGrab(level, nearest);
		} else if (stomps >= 1 && !blocked(level, forward().scale(-RUN_SPEED))) {
			// a stomp was not enough: it steps back to bring its jaws to bear
			stomps = 0;
			backTicks = 16;
		} else {
			stomps++;
			startStrike(level, STOMP, nearest);
		}
	}

	/** It steps back a few strides, still facing its foe (then its jaws reach them). */
	private void stepBack(ServerLevel level, LivingEntity foe) {
		backTicks--;
		turnTo(foe.position(), 6F);
		Vec3 step = forward().scale(-RUN_SPEED * 0.8);
		if (blocked(level, step)) {
			backTicks = 0;
			return;
		}
		Vec3 next = dragon.position().add(step);
		Vec3 ground = Vfx.ground(level, next.add(0, 1.5, 0));
		dragon.teleportTo(next.x, ground.y, next.z);
		dragon.setDeltaMovement(Vec3.ZERO);
		if (dragon.tickCount % 7 == 0) {
			level.playSound(null, dragon.blockPosition(), SoundEvents.RAVAGER_STEP, SoundSource.HOSTILE, 2F, 0.55F);
		}
		if (backTicks == 0) {
			busy = Math.max(busy, 4);
		}
	}

	/** It runs on the ice toward a place, turning as it goes, round an obelisk or a pillar in its way. */
	private void run(ServerLevel level, Vec3 target) {
		turnTo(target, 8F);
		Vec3 step = forward().scale(RUN_SPEED);
		if (blocked(level, step)) {
			boolean free = false;
			for (float turn : new float[]{30F, -30F, 60F, -60F, 90F, -90F}) {
				double r = Math.toRadians(dragon.getYRot() + turn);
				Vec3 other = new Vec3(-Math.sin(r), 0, Math.cos(r)).scale(RUN_SPEED);
				if (!blocked(level, other)) {
					face(dragon.getYRot() + turn * 0.5F);
					step = other;
					free = true;
					break;
				}
			}
			if (!free) {
				return;
			}
		}
		Vec3 next = dragon.position().add(step);
		Vec3 ground = Vfx.ground(level, next.add(0, 1.5, 0));
		dragon.teleportTo(next.x, ground.y, next.z);
		// (no speed of its own left on it: the game would move it that far again)
		dragon.setDeltaMovement(Vec3.ZERO);
		if (dragon.tickCount % 7 == 0) {
			level.playSound(null, dragon.blockPosition(), SoundEvents.RAVAGER_STEP, SoundSource.HOSTILE, 2F, 0.55F);
			level.sendParticles(ParticleTypes.SNOWFLAKE, dragon.getX(), dragon.getY() + 0.2, dragon.getZ(), 10, dragon.getBbWidth() / 2, 0.1,
				dragon.getBbWidth() / 2, 0.05);
		}
	}

	private boolean blocked(ServerLevel level, Vec3 step) {
		return !level.noCollision(dragon, dragon.getBoundingBox().move(step).deflate(0.6, 0, 0.6).move(0, 0.6, 0));
	}

	/**
	 * A blow on foot, shown first: an orange circle where it lands (it hurts), its move begun so it lands as the circle
	 * fills: the bite before its head, its tail round its hindquarters and sides, the stomp at its forefeet.
	 */
	private void startStrike(ServerLevel level, int kind, LivingEntity foe) {
		strikeKind = kind;
		strikeTick = 0;
		double w = dragon.getBbWidth();
		Vec3 at;
		switch (kind) {
			case BITE -> {
				// at its jaws' reach before it
				face(foe.position());
				strikeWind = 18;
				strikeRadius = 3.8F;
				at = dragon.position().add(forward().scale(jawsReach()));
			}
			case TAIL -> {
				strikeWind = 24;
				strikeRadius = (float) (w * 0.6 + 4.5);
				at = dragon.position().subtract(forward().scale(w * 0.55));
			}
			default -> {
				// its forefeet, under its chest
				strikeWind = 20;
				strikeRadius = 5.5F;
				at = dragon.position().add(forward().scale(w * 0.3 + 3.5));
			}
		}
		strikeAt = Vfx.ground(level, at.add(0, 2, 0));
		Vfx.circle(level, strikeAt, strikeRadius, Vfx.FIRE, strikeWind);
		Vfx.once(level, dragon.position(), kind == TAIL ? SoundEvents.RAVAGER_STUNNED : SoundEvents.RAVAGER_ROAR, 1.4F, kind == BITE ? 1.1F : 0.7F, 40, 40);
	}

	private void tickStrike(ServerLevel level) {
		strikeTick++;
		int lead = strikeKind == BITE ? 16 : strikeKind == TAIL ? 22 : 15;
		if (strikeTick == Math.max(1, strikeWind - lead)) {
			dragon.setAnimation(strikeKind == BITE ? EntityDragonBase.ANIMATION_BITE
				: strikeKind == TAIL ? EntityDragonBase.ANIMATION_TAILWHACK : EntityDragonBase.ANIMATION_WINGBLAST);
			dragon.setAnimationTick(0);
		}
		if (strikeTick < strikeWind) {
			return;
		}
		strikeTick = -1;
		RandomSource random = dragon.getRandom();
		// (a moment to breathe after each blow: the time to strike it)
		busy = 16 + random.nextInt(12);
		float reach = strikeRadius + 0.3F;
		for (LivingEntity entity : level.getEntitiesOfClass(LivingEntity.class, new AABB(strikeAt, strikeAt).inflate(reach, 4, reach), BlackFrostFight::foe)) {
			if (entity.position().subtract(strikeAt).horizontalDistanceSqr() > reach * reach) {
				continue;
			}
			Vec3 away = awayFrom(entity);
			if (strikeKind == BITE) {
				BossLimits.strike(level, entity, dragon, 0.15F);
				BossLimits.shove(entity, away.x * 0.8, 0.3, away.z * 0.8);
			} else if (strikeKind == TAIL) {
				BossLimits.strike(level, entity, dragon, 0.12F);
				BossLimits.shove(entity, away.x * 1.4, 0.45, away.z * 1.4);
			} else {
				BossLimits.strike(level, entity, dragon, 0.12F);
				BossCold.slow(entity, 40, 0, dragon);
				BossLimits.shove(entity, away.x * 0.6, 0.55, away.z * 0.6);
			}
		}
		switch (strikeKind) {
			case BITE -> level.playSound(null, BlockPos.containing(strikeAt), SoundEvents.RAVAGER_ATTACK, SoundSource.HOSTILE, 2F, 0.6F);
			case TAIL -> {
				level.playSound(null, BlockPos.containing(strikeAt), SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.HOSTILE, 2.5F, 0.5F);
				Vfx.wave(level, strikeAt, strikeRadius, Vfx.ICE, 10);
			}
			default -> {
				level.playSound(null, BlockPos.containing(strikeAt), SoundEvents.GENERIC_EXPLODE, SoundSource.HOSTILE, 1.5F, 0.8F);
				Vfx.wave(level, strikeAt, strikeRadius + 1, Vfx.ICE, 12);
				level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.PACKED_ICE.defaultBlockState()), strikeAt.x, strikeAt.y + 0.3,
					strikeAt.z, 40, strikeRadius / 2, 0.2, strikeRadius / 2, 0.2);
			}
		}
	}

	/**
	 * A stampede: three rushes from edge to edge of the arena, each down a lane drawn through its foe; then it stands
	 * winded (the time to strike) and fights on foot.
	 */
	private void stampede(ServerLevel level, ServerPlayer nearest) {
		if (busy > 0) {
			busy--;
			turnTo(nearest.position(), 8F);
			return;
		}
		if (rushes > 0) {
			startRush(level, nearest, rushes == 3);
			rushes--;
			return;
		}
		stampeding = false;
		nextStance = MELEE;
		stun(level, 60, "iceandfire_fabric.frost.winded");
	}

	private void startRush(ServerLevel level, ServerPlayer foe, boolean first) {
		hold();
		Vec3 from = Vfx.ground(level, dragon.position().add(0, 2, 0));
		Vec3 way = foe.position().subtract(from).multiply(1, 0, 1);
		Vec3 dir = way.lengthSqr() < 1 ? forward() : way.normalize();
		double length = edgeAlong(from, dir);
		chargeFrom = from;
		chargeTo = Vfx.ground(level, from.add(dir.scale(length)).add(0, 2, 0));
		chargeWind = first ? 32 : 22;
		rushTicks = Math.max(8, (int) (length / RUSH_SPEED));
		face(chargeTo);
		float yaw = (float) Math.toDegrees(Math.atan2(-dir.x, dir.z));
		Vfx.lane(level, from, yaw, (float) length, Math.max(3.5F, dragon.getBbWidth() * 0.9F), Vfx.DANGER, chargeWind);
		roar();
		Vfx.once(level, dragon.position(), SoundEvents.RAVAGER_ROAR, 2F, 0.6F, 40, 40);
		riderWaits(chargeWind + rushTicks + 10);
		chargeTick = 0;
		hit.clear();
		stampeding = true;
		if (first) {
			ThroneFight.tell(dragon, "iceandfire_fabric.frost.stampede", ChatFormatting.RED);
			com.iafabric.pixie.DuelTalk.moment(dragon, com.iafabric.pixie.DuelTalk.Moment.STAMPEDE);
		}
	}

	/** How far it can run from a place along a way before the arena's edge (where its rushes end). */
	private double edgeAlong(Vec3 from, Vec3 dir) {
		if (arena == null) {
			return 46;
		}
		Vec3 rel = from.subtract(arena).multiply(1, 0, 1);
		double b = dir.dot(rel);
		double c = rel.lengthSqr() - RUSH_EDGE * RUSH_EDGE;
		double t = -b + Math.sqrt(Math.max(0, b * b - c));
		return Mth.clamp(t, 20, 170);
	}

	private void tickStunned(ServerLevel level) {
		stunned--;
		if (dragon.tickCount % 4 == 0) {
			Vec3 head = dragon.getHeadPosition();
			level.sendParticles(ParticleTypes.CRIT, head.x, head.y + 1, head.z, 6, 0.8, 0.3, 0.8, 0.05);
		}
		if (stunned == 0) {
			free();
			if (steered) {
				// up again: on a perch it stays, elsewhere it flies to one, or it fights on foot as it chose
				perch = perchAt(dragon.position());
				busy = Math.max(busy, 10);
				if (nextStance >= 0) {
					beginStance(level, nextStance);
					nextStance = -1;
				}
			}
		}
	}

	/**
	 * Does one of its powers now, by name (scenes and tests): charge, dive, hail, roar, prison, breath (super_breath,
	 * frost_breath), grab, volley, fly, bite, tail, stomp, melee, stampede.
	 */
	public void cast(ServerLevel level, String power) {
		List<ServerPlayer> foes = ThroneFight.foesNear(dragon);
		if (foes.isEmpty() || inGreatBlow()) {
			return;
		}
		ServerPlayer nearest = nearest(foes);
		switch (power) {
			case "charge" -> startCharge(level, nearest);
			case "dive" -> startDive(level, nearest);
			case "hail" -> hail(level, foes, dragon.getRandom());
			case "roar" -> glacialRoar(level);
			case "prison" -> startPrison(level, nearest);
			case "breath" -> startBreath(level, nearest, desperate(dragon));
			case "super_breath" -> startBreath(level, nearest, true);
			case "frost_breath" -> startBreath(level, nearest, false);
			case "grab" -> startGrab(level, nearest);
			case "volley" -> volley(level, nearest);
			case "fly" -> flyToPerch(level, foes, true);
			case "bite" -> startStrike(level, BITE, nearest);
			case "tail" -> startStrike(level, TAIL, nearest);
			case "stomp" -> startStrike(level, STOMP, nearest);
			case "melee" -> {
				busy = 0;
				beginStance(level, MELEE);
			}
			case "stampede" -> {
				busy = 0;
				beginStance(level, STAMPEDE);
			}
			default -> {
			}
		}
	}

	/**
	 * A player struck it with their own hand, close by: its claws close on them as soon as they are ready (not while it
	 * is stunned, nor in the middle of another blow).
	 */
	public void struckBy(ServerLevel level, ServerPlayer player) {
		if (!steered || grab > 0 || inGreatBlow() || stunned > 0 || strikeTick >= 0 || backTicks > 0 || flat(player) > 16
			|| ThroneFight.waiting(dragon)) {
			return;
		}
		grab = GRAB_AGAIN + dragon.getRandom().nextInt(40);
		startGrab(level, player);
	}

	/** Its powers wait (a scene's still moment, Cryona's spells). */
	public void rest(int ticks) {
		busy = Math.max(busy, ticks);
	}

	/** For the scenes: it acts at once; its claws are ready or not, its retreat is, its roar is not. */
	public void ready(boolean claws) {
		grab = claws ? 0 : 400;
		away = 0;
		roar = 400;
		busy = 0;
	}

	/** Cryona on its back waits for its great blow. */
	private void riderWaits(int ticks) {
		if (dragon.getFirstPassenger() instanceof EntityDreadQueen queen) {
			CryonaFight.of(queen).rest(ticks);
		}
	}

	/** After a blow: outside its realm its own mind leads it again (in its realm it never does). */
	private void free() {
		if (!steered) {
			dragon.setNoAi(false);
		}
	}

	private ServerPlayer nearest(List<ServerPlayer> foes) {
		ServerPlayer nearest = foes.get(0);
		for (ServerPlayer foe : foes) {
			if (foe.distanceToSqr(dragon) < nearest.distanceToSqr(dragon)) {
				nearest = foe;
			}
		}
		return nearest;
	}

	/** How far something is from its middle, along the ground. */
	private double flat(LivingEntity entity) {
		return Math.sqrt(entity.position().subtract(dragon.position()).horizontalDistanceSqr());
	}

	/** How close a foe may come before its claws reach them (from its middle). */
	public double reach() {
		return dragon.getBbWidth() * 0.5 + 3.5;
	}

	/** How far the cold round it reaches (from its middle). */
	private double auraRadius() {
		return reach() + 8;
	}

	// ---------------------------------------------------------------------------------------------------------------
	// in its realm: its perches, its flight, the cold round it, its claws, its volley

	/** A perch of its arena, on the ice. */
	private Vec3 perchSpot(int index) {
		int[] offset = ThroneArena.PERCHES[index];
		Vec3 center = arena == null ? dragon.position() : arena;
		return new Vec3(center.x + offset[0], ThroneFight.FLOOR, center.z + offset[1]);
	}

	/** The perch it stands on (near its middle), or -1. */
	private int perchAt(Vec3 at) {
		if (arena == null) {
			return -1;
		}
		for (int i = 0; i < ThroneArena.PERCHES.length; i++) {
			if (perchSpot(i).subtract(at).horizontalDistanceSqr() < 5 * 5) {
				return i;
			}
		}
		return -1;
	}

	/**
	 * The perch to fly to: fleeing, the farthest from its foes (within the arena's reach); else one at a good distance
	 * from them (about forty blocks: near enough to fight, far enough to be seen whole). Never the one it stands on.
	 */
	private int pickPerch(List<ServerPlayer> foes, boolean far) {
		if (arena == null) {
			return -1;
		}
		RandomSource random = dragon.getRandom();
		int best = -1;
		double bestScore = -Double.MAX_VALUE;
		for (int i = 0; i < ThroneArena.PERCHES.length; i++) {
			Vec3 spot = perchSpot(i);
			if (i == perch || spot.subtract(dragon.position()).horizontalDistanceSqr() < 8 * 8) {
				continue;
			}
			double near = Double.MAX_VALUE;
			for (ServerPlayer foe : foes) {
				near = Math.min(near, Math.sqrt(foe.position().subtract(spot).horizontalDistanceSqr()));
			}
			if (foes.isEmpty()) {
				near = 40;
			}
			double score = far ? near : -Math.abs(near - 40) - (near < 20 ? 40 : 0);
			score += random.nextDouble() * 6;
			if (score > bestScore) {
				bestScore = score;
				best = i;
			}
		}
		return best;
	}

	private void flyToPerch(ServerLevel level, List<ServerPlayer> foes, boolean far) {
		int to = pickPerch(foes, far);
		if (to < 0) {
			busy = 40;
			return;
		}
		away = 120;
		fly(level, to);
	}

	/** It takes off and flies over the arena to a perch, high over the obelisks; a circle shows where it lands. */
	private void fly(ServerLevel level, int index) {
		hold();
		flyFrom = dragon.position();
		flyTo = Vfx.ground(level, perchSpot(index).add(0, 2, 0));
		Vec3 way = flyTo.subtract(flyFrom).multiply(1, 0, 1);
		double distance = way.length();
		// not a hop but a sweep round the arena, bending away from its middle
		Vec3 mid = flyFrom.add(flyTo).scale(0.5);
		Vec3 side = distance < 1 ? new Vec3(1, 0, 0) : new Vec3(-way.z, 0, way.x).normalize();
		if (arena != null && side.dot(mid.subtract(arena)) < 0) {
			side = side.scale(-1);
		}
		double bend = Mth.clamp(distance * 0.45, 10, 28);
		flyControl = mid.add(side.scale(bend));
		flyTicks = Mth.clamp((int) ((distance + bend * 0.8) / FLY_SPEED), 40, 140);
		flyPeak = Mth.clamp(12 + distance * 0.15, 14, 24);
		flyTick = 0;
		flyPerch = index;
		perch = -1;
		riderWaits(flyTicks + 10);
		Vfx.circle(level, flyTo, dragon.getBbWidth() * 0.6F + 1F, Vfx.ICE, flyTicks);
		level.playSound(null, dragon.blockPosition(), SoundEvents.ENDER_DRAGON_FLAP, SoundSource.HOSTILE, 4F, 0.7F);
		level.sendParticles(ParticleTypes.SNOWFLAKE, flyFrom.x, flyFrom.y + 0.5, flyFrom.z, 60, dragon.getBbWidth() / 2, 0.4, dragon.getBbWidth() / 2, 0.25);
	}

	private void tickFlight(ServerLevel level) {
		flyTick++;
		double t = Math.min(1.0, flyTick / (double) flyTicks);
		double s = t * t * (3 - 2 * t);
		double lift = flyPeak * Math.pow(Math.sin(Math.PI * t), 0.7);
		Vec3 control = flyControl == null ? flyFrom.add(flyTo).scale(0.5) : flyControl;
		double u = 1 - s;
		double x = u * u * flyFrom.x + 2 * u * s * control.x + s * s * flyTo.x;
		double z = u * u * flyFrom.z + 2 * u * s * control.z + s * s * flyTo.z;
		Vec3 at = new Vec3(x, Mth.lerp(s, flyFrom.y, flyTo.y) + lift, z);
		// (along its bend)
		Vec3 ahead = new Vec3(2 * u * (control.x - flyFrom.x) + 2 * s * (flyTo.x - control.x), 0, 2 * u * (control.z - flyFrom.z) + 2 * s * (flyTo.z - control.z));
		Vec3 step = at.subtract(dragon.position());
		dragon.teleportTo(at.x, at.y, at.z);
		dragon.setDeltaMovement(Vec3.ZERO);
		boolean landing = t > 0.82;
		dragon.setFlying(!landing);
		dragon.setHovering(landing);
		double ground = Math.sqrt(step.horizontalDistanceSqr());
		dragon.setDragonPitch((float) Mth.clamp(Math.toDegrees(Math.atan2(-step.y, Math.max(0.05, ground))), -30, 30));
		if (!landing) {
			turnTo(at.add(ahead), 10F);
		} else {
			// it lands turned to its foes
			List<ServerPlayer> foes = ThroneFight.foesNear(dragon);
			turnTo(foes.isEmpty() ? flyTo : nearest(foes).position(), 9F);
		}
		if (flyTick % 3 == 0) {
			level.sendParticles(ParticleTypes.SNOWFLAKE, dragon.getX(), dragon.getY() + 0.5, dragon.getZ(), 12, dragon.getBbWidth() / 2, 0.4, dragon.getBbWidth() / 2, 0.05);
		}
		if (flyTick % 14 == 0) {
			level.playSound(null, dragon.blockPosition(), SoundEvents.ENDER_DRAGON_FLAP, SoundSource.HOSTILE, 3F, 0.75F);
		}
		if (t < 1) {
			return;
		}
		flyTick = -1;
		dragon.teleportTo(flyTo.x, flyTo.y, flyTo.z);
		dragon.setFlying(false);
		dragon.setHovering(false);
		dragon.setDragonPitch(0F);
		perch = flyPerch;
		perchTime = 0;
		busy = Math.max(busy, 20);
		if (steered) {
			beginStance(level, RANGED);
		}
		Vfx.wave(level, flyTo, 9F, Vfx.ICE, 12);
		level.playSound(null, dragon.blockPosition(), SoundEvents.GENERIC_EXPLODE, SoundSource.HOSTILE, 1.5F, 0.6F);
		level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.PACKED_ICE.defaultBlockState()), flyTo.x, flyTo.y + 0.3, flyTo.z, 50,
			dragon.getBbWidth() / 2, 0.2, dragon.getBbWidth() / 2, 0.2);
		// who stands where it lands is put aside (no blow)
		double room = dragon.getBbWidth() * 0.6 + 2;
		for (LivingEntity entity : level.getEntitiesOfClass(LivingEntity.class, dragon.getBoundingBox().inflate(3, 2, 3), BlackFrostFight::foe)) {
			if (flat(entity) < room) {
				Vec3 away = awayFrom(entity);
				BossLimits.shove(entity, away.x * 1.1, 0.4, away.z * 1.1);
			}
		}
	}

	/** The way from its middle to something, along the ground. */
	private Vec3 awayFrom(LivingEntity entity) {
		Vec3 away = entity.position().subtract(dragon.position()).multiply(1, 0, 1);
		return away.lengthSqr() < 1.0E-4 ? new Vec3(1, 0, 0) : away.normalize();
	}

	/**
	 * The cold round it while it stands: who comes near is slowed (the nearer the more) and feels the frost on their
	 * sight, and every two seconds a gust of ice (a ring going out on the ice) drives them back, harder the nearer they
	 * are (half with the Frost Ward); snow blows out from it along the ice, its edge shows now and then. Not while it is
	 * stunned (nor in the air).
	 */
	private void aura(ServerLevel level, List<ServerPlayer> foes) {
		if (dragon.tickCount % 4 != 0) {
			return;
		}
		double radius = auraRadius();
		gust -= 4;
		boolean blow = false;
		for (ServerPlayer foe : foes) {
			double d = flat(foe);
			if (d > radius || foe.getY() > dragon.getY() + 10) {
				continue;
			}
			double k = 1 - d / radius;
			BossCold.slow(foe, 12, k > 0.5 ? 1 : 0, dragon);
			foe.setTicksFrozen(Math.max(foe.getTicksFrozen(), (int) (40 + 70 * k)));
			if (gust <= 0 && !carries(foe)) {
				blow = true;
				Vec3 away = awayFrom(foe);
				double push = (0.35 + 0.45 * k) * (IafEffects.warded(foe) ? 0.5 : 1);
				BossLimits.shove(foe, away.x * push, 0.18, away.z * push);
			}
		}
		if (blow) {
			gust = 40;
			Vec3 at = Vfx.ground(level, dragon.position());
			Vfx.wave(level, at, (float) radius, Vfx.ICE, 12);
			level.playSound(null, dragon.blockPosition(), SoundEvents.POWDER_SNOW_BREAK, SoundSource.HOSTILE, 3F, 0.5F);
		}
		RandomSource random = dragon.getRandom();
		for (int i = 0; i < 10; i++) {
			double a = random.nextDouble() * Math.PI * 2;
			double r = dragon.getBbWidth() * 0.5 + random.nextDouble() * 2;
			level.sendParticles(ParticleTypes.SNOWFLAKE, dragon.getX() + Math.cos(a) * r, dragon.getY() + 0.3, dragon.getZ() + Math.sin(a) * r, 0,
				Math.cos(a), 0.02, Math.sin(a), 0.5);
		}
		if (dragon.tickCount % 20 == 0) {
			for (int i = 0; i < 40; i++) {
				double a = Math.PI * 2 * i / 40;
				level.sendParticles(ParticleTypes.SNOWFLAKE, dragon.getX() + Math.cos(a) * radius, dragon.getY() + 0.25, dragon.getZ() + Math.sin(a) * radius, 1,
					0.05, 0.05, 0.05, 0);
			}
		}
	}

	/** Its claws: who came too close sees a circle close round their feet; still in it when it shuts, they are taken. */
	private void startGrab(ServerLevel level, ServerPlayer foe) {
		hold();
		riderWaits(GRAB_WIND + GRAB_LIFT + GRAB_HOLD + 20);
		grabAt = Vfx.ground(level, foe.position());
		grabTick = 0;
		held = null;
		face(grabAt);
		roar();
		Vfx.circle(level, grabAt, GRAB_RADIUS, Vfx.FIRE, GRAB_WIND);
		Vfx.rune(level, grabAt, GRAB_RADIUS + 0.4F, EntityVfx.FROST, Vfx.FIRE, GRAB_WIND + 6, 5F, 0F);
		Vfx.once(level, dragon.position(), SoundEvents.RAVAGER_ROAR, 2F, 0.8F, 40, 40);
		ThroneFight.tell(dragon, "iceandfire_fabric.frost.grab", ChatFormatting.GOLD);
		com.iafabric.pixie.DuelTalk.moment(dragon, com.iafabric.pixie.DuelTalk.Moment.GRAB);
	}

	private void tickGrab(ServerLevel level) {
		grabTick++;
		if (grabTick < GRAB_WIND) {
			face(grabAt);
			if (grabTick % 4 == 0) {
				RandomSource random = dragon.getRandom();
				for (int i = 0; i < 6; i++) {
					double a = random.nextDouble() * Math.PI * 2;
					level.sendParticles(ParticleTypes.CRIT, grabAt.x + Math.cos(a) * GRAB_RADIUS, grabAt.y + 0.3, grabAt.z + Math.sin(a) * GRAB_RADIUS, 1, 0, 0.1, 0, 0.02);
				}
			}
			return;
		}
		if (grabTick == GRAB_WIND) {
			ServerPlayer caught = null;
			double best = GRAB_RADIUS + 0.4;
			for (ServerPlayer foe : ThroneFight.foesNear(dragon)) {
				double d = Math.sqrt(foe.position().subtract(grabAt).horizontalDistanceSqr());
				if (d <= best && Math.abs(foe.getY() - grabAt.y) < 3) {
					caught = foe;
					best = d;
				}
			}
			dragon.setAnimation(EntityDragonBase.ANIMATION_WINGBLAST);
			dragon.setAnimationTick(0);
			perch = -1;
			if (caught == null) {
				// its claws close on the ice: off balance, open a while
				grabTick = -1;
				level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.PACKED_ICE.defaultBlockState()), grabAt.x, grabAt.y + 0.3, grabAt.z, 40, 1.2, 0.3, 1.2, 0.2);
				level.playSound(null, BlockPos.containing(grabAt), SoundEvents.ZOMBIE_ATTACK_IRON_DOOR, SoundSource.HOSTILE, 2F, 0.5F);
				stun(level, 50, "iceandfire_fabric.frost.grab_missed");
				return;
			}
			held = caught.getUUID();
			liftFrom = dragon.position();
			BossLimits.strike(level, caught, dragon, 0.12F);
			caught.displayClientMessage(Component.translatable("iceandfire_fabric.frost.grabbed").withStyle(ChatFormatting.RED, ChatFormatting.BOLD), true);
			level.playSound(null, caught.blockPosition(), SoundEvents.PLAYER_HURT_FREEZE, SoundSource.PLAYERS, 1.5F, 0.8F);
			level.playSound(null, dragon.blockPosition(), SoundEvents.ENDER_DRAGON_FLAP, SoundSource.HOSTILE, 4F, 0.6F);
		}
		ServerPlayer player = held != null && level.getEntity(held) instanceof ServerPlayer p && p.isAlive() && !p.isSpectator() ? p : null;
		if (player == null) {
			// they are gone (fell, left): it flies on
			grabTick = -1;
			held = null;
			busy = 10;
			return;
		}
		int t = grabTick - GRAB_WIND;
		double f = Math.min(1.0, t / (double) GRAB_LIFT);
		Vec3 at = liftFrom.add(0, GRAB_HEIGHT * f * f * (3 - 2 * f), 0);
		dragon.teleportTo(at.x, at.y, at.z);
		dragon.setDeltaMovement(Vec3.ZERO);
		dragon.setFlying(false);
		dragon.setHovering(true);
		// in its claws, under its chest (never under the ice as it rises)
		Vec3 claws = claws();
		claws = new Vec3(claws.x, Math.max(claws.y, grabAt.y + 0.1), claws.z);
		player.teleportTo(level, claws.x, claws.y, claws.z, Set.of(), player.getYRot(), player.getXRot());
		player.setDeltaMovement(Vec3.ZERO);
		player.resetFallDistance();
		if (t % 4 == 0) {
			level.sendParticles(ParticleTypes.SNOWFLAKE, claws.x, claws.y + 1, claws.z, 8, 0.4, 0.6, 0.4, 0.05);
		}
		if (t < GRAB_LIFT + GRAB_HOLD) {
			return;
		}
		// and thrown far, into the arena
		Vec3 way = throwWay(player);
		player.setDeltaMovement(way.x * 2.0, 0.6, way.z * 2.0);
		player.hurtMarked = true;
		thrown.put(player.getUUID(), 0);
		fallsSafelyFor(player, 160);
		level.playSound(null, dragon.blockPosition(), SoundEvents.ENDER_DRAGON_GROWL, SoundSource.HOSTILE, 3F, 1.1F);
		grabTick = -1;
		held = null;
		flyToPerch(level, ThroneFight.foesNear(dragon), true);
	}

	/** Where its claws hold who it took: under its chest. */
	private Vec3 claws() {
		double r = Math.toRadians(dragon.getYRot());
		Vec3 forward = new Vec3(-Math.sin(r), 0, Math.cos(r));
		// (under the front of its chest, hanging below its feet: seen from aside)
		return dragon.position().add(forward.scale(dragon.getBbWidth() * 0.45)).add(0, -2.2, 0);
	}

	/** The way it throws who it held: before it, and toward the arena's middle (never out of it). */
	private Vec3 throwWay(ServerPlayer player) {
		double r = Math.toRadians(dragon.getYRot());
		Vec3 way = new Vec3(-Math.sin(r), 0, Math.cos(r));
		if (arena != null) {
			Vec3 in = arena.subtract(player.position()).multiply(1, 0, 1);
			if (in.lengthSqr() > 1) {
				way = way.add(in.normalize().scale(in.length() > 50 ? 1.5 : 0.6));
			}
		}
		way = way.multiply(1, 0, 1);
		return way.lengthSqr() < 1.0E-4 ? new Vec3(1, 0, 0) : way.normalize();
	}

	/** Whether it holds a player in its claws or threw them (still in the air): nothing else pushes them then. */
	public boolean carries(net.minecraft.world.entity.player.Player player) {
		return player.getUUID().equals(held) || thrown.containsKey(player.getUUID());
	}

	/** Whether its body lets a player be (no push off it): its claws are closing, or it holds or threw them. */
	public boolean leavesBe(net.minecraft.world.entity.player.Player player) {
		return grabTick >= 0 || carries(player);
	}

	/** Who it holds goes free (its duel waits, Absolute Zero): they float down. */
	private void letGo() {
		if (held != null && dragon.level() instanceof ServerLevel level && level.getEntity(held) instanceof ServerPlayer player) {
			player.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, 100));
			fallsSafelyFor(player, 200);
		}
		held = null;
	}

	/** A player falls with no life lost to the height for a while (thrown, let go). */
	private static void fallsSafelyFor(LivingEntity entity, int ticks) {
		long now = entity.level().getGameTime();
		SAFE_FALL.put(entity.getUUID(), now + ticks);
		if (SAFE_FALL.size() > 64) {
			SAFE_FALL.values().removeIf(until -> until < now);
		}
	}

	/**
	 * Whether a fall takes nothing from this one now: Black Frost threw them or let them go a moment ago (from the
	 * damage hook in ThroneFight; the landing on the ice is its own blow).
	 */
	public static boolean fallsSafely(LivingEntity entity) {
		Long until = SAFE_FALL.get(entity.getUUID());
		return until != null && entity.level().getGameTime() <= until;
	}

	/**
	 * Its duel waits, or it fell: who it holds and who it threw (still in the air) float down, never hurt by the
	 * height (from ThroneFight once it is down).
	 */
	public void release() {
		letGo();
		grabTick = -1;
		if (!pylons.isEmpty() && dragon.level() instanceof ServerLevel level) {
			clearPylons(level);
		}
		if (dragon.level() instanceof ServerLevel level) {
			for (UUID id : thrown.keySet()) {
				if (level.getEntity(id) instanceof ServerPlayer player && !player.onGround()) {
					player.resetFallDistance();
					player.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, 60));
					fallsSafelyFor(player, 160);
				}
			}
		}
		thrown.clear();
	}

	/** Who it threw: no life is lost to the height; landing on the ice takes a share of their life and slows them. */
	private void tickThrown(ServerLevel level) {
		for (Iterator<Map.Entry<UUID, Integer>> it = thrown.entrySet().iterator(); it.hasNext(); ) {
			Map.Entry<UUID, Integer> entry = it.next();
			int t = entry.getValue() + 1;
			entry.setValue(t);
			if (!(level.getEntity(entry.getKey()) instanceof ServerPlayer player) || !player.isAlive() || t > 140) {
				it.remove();
				continue;
			}
			player.resetFallDistance();
			if (t > 5 && (player.onGround() || player.isInWater())) {
				it.remove();
				// (the height takes nothing a moment more: the game may count the fall a tick after this)
				fallsSafelyFor(player, 10);
				Vec3 at = player.position();
				BossLimits.strike(level, player, dragon, 0.2F);
				BossCold.slow(player, 40, 0, dragon);
				Vfx.wave(level, at, 3.5F, Vfx.ICE, 10);
				level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.PACKED_ICE.defaultBlockState()), at.x, at.y + 0.2, at.z, 30, 0.6, 0.2, 0.6, 0.15);
				Vfx.shatter(level, at, 1.2F, 0.6F);
			}
		}
	}

	/** Its volley: shards of ice spat at its foe one after another; each falls where a circle shows on the ice. */
	private void volley(ServerLevel level, ServerPlayer foe) {
		busy = 24 + SHARDS * 10;
		dragon.setAnimation(EntityDragonBase.ANIMATION_FIRECHARGE);
		dragon.setAnimationTick(0);
		UUID id = foe.getUUID();
		for (int i = 0; i < SHARDS; i++) {
			Vfx.later(level, 6 + i * 10, () -> spit(level, id));
		}
	}

	private void spit(ServerLevel level, UUID id) {
		if (!dragon.isAlive() || dragon.isModelDead() || stunned > 0 || flyTick >= 0 || grabTick >= 0 || hoverTicks > 0
			|| !(level.getEntity(id) instanceof ServerPlayer foe) || !foe.isAlive()) {
			return;
		}
		RandomSource random = dragon.getRandom();
		Vec3 target = Vfx.ground(level, foe.position());
		Vec3 mouth = jaws();
		face(target);
		Vfx.circle(level, target, SHARD_RADIUS, Vfx.ICE, SHARD_FLIGHT);
		Display.BlockDisplay shard = Vfx.shape(level, mouth, ThroneFight.ice(random), 0.9F, 1.6F, random.nextFloat() * 40 - 20, random.nextFloat() * 40 - 20, true);
		// up from its mouth and down on the circle
		Vec3 apex = mouth.add(target).scale(0.5).add(0, 5 + Math.sqrt(mouth.subtract(target).horizontalDistanceSqr()) * 0.12, 0);
		if (shard != null) {
			Vfx.later(level, 1, () -> Vfx.move(shard, apex, SHARD_FLIGHT / 2));
			Vfx.later(level, 1 + SHARD_FLIGHT / 2, () -> Vfx.move(shard, target, SHARD_FLIGHT / 2));
		}
		level.sendParticles(ParticleTypes.SNOWFLAKE, mouth.x, mouth.y, mouth.z, 16, 0.4, 0.4, 0.4, 0.15);
		level.playSound(null, BlockPos.containing(mouth), SoundEvents.SNOW_GOLEM_SHOOT, SoundSource.HOSTILE, 3F, 0.5F);
		Vfx.later(level, SHARD_FLIGHT + 1, () -> {
			Vfx.remove(shard);
			level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.PACKED_ICE.defaultBlockState()), target.x, target.y + 0.4, target.z, 24, 0.6, 0.3, 0.6, 0.12);
			Vfx.shatter(level, target, 1.1F, 0.8F);
			Vfx.spike(level, target, ThroneFight.ice(random), 0.7F, 1.4F + random.nextFloat(), random.nextFloat() * 30 - 15, random.nextFloat() * 30 - 15, 2, 40);
			for (LivingEntity entity : level.getEntitiesOfClass(LivingEntity.class, new AABB(target, target).inflate(SHARD_RADIUS + 0.3, 3, SHARD_RADIUS + 0.3), BlackFrostFight::foe)) {
				if (entity.position().subtract(target).horizontalDistanceSqr() <= (SHARD_RADIUS + 0.3) * (SHARD_RADIUS + 0.3)) {
					BossLimits.strike(level, entity, dragon, 0.08F);
					BossCold.slow(entity, 30, 0, dragon);
				}
			}
		});
	}

	/** It turns toward a place, a few degrees a tick. */
	private void turnTo(Vec3 at, float step) {
		double dx = at.x - dragon.getX();
		double dz = at.z - dragon.getZ();
		if (dx * dx + dz * dz < 0.01) {
			return;
		}
		float want = (float) Math.toDegrees(Math.atan2(-dx, dz));
		face(Mth.approachDegrees(dragon.getYRot(), want, step));
	}

	// ---------------------------------------------------------------------------------------------------------------
	// its pylons

	/** Its pylons as they stand (each tick in its realm): new ones from three quarters of its life down. */
	private void tickPylons(ServerLevel level, List<ServerPlayer> foes) {
		if (!pylonsSwept) {
			pylonsSwept = true;
			sweepPylons(level);
		}
		for (Iterator<Pylon> it = pylons.iterator(); it.hasNext(); ) {
			Pylon pylon = it.next();
			pylon.age++;
			if (pylon.age >= PYLON_LIFE || pylon.box == null || pylon.box.isRemoved()) {
				// its light fades: it sinks back into the ice
				it.remove();
				fade(level, pylon);
				continue;
			}
			if (pylon.age % 6 == 0) {
				level.sendParticles(ParticleTypes.END_ROD, pylon.at.x, pylon.at.y + PYLON_HEIGHT + 0.6, pylon.at.z, 2, 0.25, 0.4, 0.25, 0.02);
				level.sendParticles(ParticleTypes.SNOWFLAKE, pylon.at.x, pylon.at.y + 1, pylon.at.z, 3, 0.8, 1.2, 0.8, 0.01);
			}
		}
		if (arena == null || foes.isEmpty() || !dragon.isAlive() || dragon.isModelDead() || dragon.getHealth() > dragon.getMaxHealth() * PYLONS_FROM) {
			return;
		}
		if (pylons.isEmpty() && --pylonTimer <= 0) {
			pylonTimer = PYLONS_EVERY + dragon.getRandom().nextInt(200);
			raisePylons(level);
		}
	}

	/** Three pylons round the arena, far from it, on open ice. */
	private void raisePylons(ServerLevel level) {
		RandomSource random = dragon.getRandom();
		double start = random.nextDouble() * Math.PI * 2;
		for (int i = 0; i < 3; i++) {
			for (int attempt = 0; attempt < 16; attempt++) {
				double a = start + Math.PI * 2 * i / 3 + (random.nextDouble() - 0.5) * 0.7;
				double r = 24 + random.nextDouble() * 24;
				Vec3 spot = Vfx.ground(level, arena.add(Math.cos(a) * r, 2, Math.sin(a) * r));
				if (spot.subtract(dragon.position()).horizontalDistanceSqr() < 22 * 22 || Math.abs(spot.y - arena.y) > 3
					|| !level.noCollision(new AABB(spot.x - 1.2, spot.y + 0.1, spot.z - 1.2, spot.x + 1.2, spot.y + PYLON_HEIGHT + 1, spot.z + 1.2))) {
					continue;
				}
				Pylon pylon = pylon(level, spot);
				if (pylon != null) {
					pylons.add(pylon);
				}
				break;
			}
		}
		if (!pylons.isEmpty()) {
			level.playSound(null, BlockPos.containing(arena), SoundEvents.BEACON_ACTIVATE, SoundSource.HOSTILE, 4F, 0.6F);
			ThroneFight.tell(dragon, "iceandfire_fabric.frost.pylons", ChatFormatting.AQUA);
			com.iafabric.pixie.DuelTalk.moment(dragon, com.iafabric.pixie.DuelTalk.Moment.PYLONS);
		}
	}

	private @Nullable Pylon pylon(ServerLevel level, Vec3 at) {
		Interaction box = EntityType.INTERACTION.create(level);
		if (box == null) {
			return null;
		}
		Pylon pylon = new Pylon();
		pylon.at = at;
		box.moveTo(at.x, at.y, at.z, 0F, 0F);
		((com.iafabric.mixin.InteractionInvoker) box).iceandfire_fabric$setWidth(PYLON_WIDTH + 0.6F);
		((com.iafabric.mixin.InteractionInvoker) box).iceandfire_fabric$setHeight(PYLON_HEIGHT + 0.6F);
		box.addTag(PYLON_TAG);
		level.addFreshEntity(box);
		pylon.box = box;
		// a crystal of blue ice rising out of the ice, a shard of light turning on its top, its light to the sky
		Display.BlockDisplay crystal = Vfx.shape(level, at.subtract(0, 0.2, 0), Blocks.BLUE_ICE.defaultBlockState(), PYLON_WIDTH, 0.05F, 0F, 0F, true);
		if (crystal != null) {
			crystal.addTag(PYLON_TAG);
			Vfx.later(level, 1, () -> Vfx.reshape(crystal, Vfx.transform(PYLON_WIDTH, PYLON_HEIGHT, 0F, 0F, 45F), 16));
		}
		pylon.crystal = crystal;
		// shards of ice round its foot, leaning out
		RandomSource random = dragon.getRandom();
		for (int i = 0; i < 5; i++) {
			double a = Math.PI * 2 * i / 5 + random.nextDouble() * 0.5;
			Display.BlockDisplay shard = Vfx.shape(level, at.add(Math.cos(a) * 1.3, -0.2, Math.sin(a) * 1.3), ThroneFight.ice(random), 0.6F, 0.05F, 0F, 0F, true);
			if (shard != null) {
				shard.addTag(PYLON_TAG);
				float h = 1.4F + random.nextFloat() * 1.2F;
				Vfx.later(level, 1, () -> Vfx.reshape(shard, Vfx.transform(0.6F, h, (float) Math.sin(a) * 25F, (float) -Math.cos(a) * 25F, 0F), 12));
				pylon.shards.add(shard);
			}
		}
		Vfx.later(level, 16, () -> {
			// (on its top once it stands)
			if (pylons.contains(pylon)) {
				pylon.tip = Vfx.shape(level, at.add(0, PYLON_HEIGHT * (1F - pylon.hits * 0.18F) + 0.3, 0), Blocks.SEA_LANTERN.defaultBlockState(), 0.8F, 0.8F,
					35F, 35F, true);
				if (pylon.tip != null) {
					pylon.tip.addTag(PYLON_TAG);
				}
			}
		});
		pylon.beam = Vfx.beam(level, at, 0.85F, 110F, Vfx.ICE, PYLON_LIFE + 10);
		pylon.rune = Vfx.rune(level, at, 2.8F, EntityVfx.FROST, Vfx.ICE, PYLON_LIFE + 10, 2F, 0F);
		level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.BLUE_ICE.defaultBlockState()), at.x, at.y + 0.5, at.z, 40, 0.8, 0.4, 0.8, 0.2);
		return pylon;
	}

	/**
	 * A player strikes something (AttackEntityCallback, on the server): one of the pylons cracks, and breaks at the third
	 * blow. A pylon of no fight (left from an older one) just goes.
	 */
	/**
	 * A player's blow by hand (ThroneFight's attack hook): a pylon cracks; Black Frost (its body or any part of it)
	 * sends its claws for them, even when the blow does it no harm (the weakness of its roar, a bare fist).
	 */
	public static net.minecraft.world.InteractionResult attacked(net.minecraft.world.entity.player.Player player, Entity target) {
		Entity body = target instanceof com.iafabric.entity.EntityMutlipartPart part ? part.getParent() : target;
		if (body instanceof EntityDragonBase frost && frost.isBlackFrost() && player instanceof ServerPlayer foe
			&& foe.level() instanceof ServerLevel level) {
			BlackFrostFight fight = FIGHTS.get(frost);
			if (fight != null) {
				fight.struckBy(level, foe);
			}
			return net.minecraft.world.InteractionResult.PASS;
		}
		return strikePylon(player, target);
	}

	public static net.minecraft.world.InteractionResult strikePylon(net.minecraft.world.entity.player.Player player, Entity target) {
		if (!(target instanceof Interaction) || !target.getTags().contains(PYLON_TAG) || !(player.level() instanceof ServerLevel level)) {
			return net.minecraft.world.InteractionResult.PASS;
		}
		for (BlackFrostFight fight : List.copyOf(FIGHTS.values())) {
			for (Pylon pylon : fight.pylons) {
				if (pylon.box == target) {
					fight.crack(level, pylon, player);
					return net.minecraft.world.InteractionResult.SUCCESS;
				}
			}
		}
		target.discard();
		return net.minecraft.world.InteractionResult.SUCCESS;
	}

	private void crack(ServerLevel level, Pylon pylon, net.minecraft.world.entity.player.Player by) {
		pylon.hits++;
		Vec3 mid = pylon.at.add(0, PYLON_HEIGHT * 0.5, 0);
		level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.BLUE_ICE.defaultBlockState()), mid.x, mid.y, mid.z, 24, 0.5, 0.8, 0.5, 0.15);
		if (pylon.hits < PYLON_HITS) {
			level.playSound(null, BlockPos.containing(mid), SoundEvents.AMETHYST_BLOCK_HIT, SoundSource.BLOCKS, 2F, 0.6F + pylon.hits * 0.2F);
			// it cracks and sinks a little
			float height = PYLON_HEIGHT * (1F - pylon.hits * 0.18F);
			if (pylon.crystal != null && !pylon.crystal.isRemoved()) {
				Vfx.reshape(pylon.crystal, Vfx.transform(PYLON_WIDTH - pylon.hits * 0.2F, height, 0F, 0F, 45F + pylon.hits * 10F), 4);
			}
			if (pylon.tip != null && !pylon.tip.isRemoved()) {
				Vfx.move(pylon.tip, pylon.at.add(0, height + 0.3, 0), 4);
			}
			if (by instanceof ServerPlayer player) {
				player.displayClientMessage(Component.translatable("iceandfire_fabric.frost.pylon_crack", pylon.hits, PYLON_HITS).withStyle(ChatFormatting.AQUA), true);
			}
			return;
		}
		pylons.remove(pylon);
		removePylon(pylon);
		Vfx.shatter(level, mid, 1.6F, 0.7F);
		level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.BLUE_ICE.defaultBlockState()), mid.x, mid.y, mid.z, 70, 0.8, 1.4, 0.8, 0.3);
		level.playSound(null, BlockPos.containing(mid), SoundEvents.BEACON_DEACTIVATE, SoundSource.HOSTILE, 3F, 0.8F);
		// its light runs to Black Frost
		Vec3 from = pylon.at.add(0, PYLON_HEIGHT, 0);
		Vec3 way = dragon.getBoundingBox().getCenter().subtract(from);
		float yaw = (float) Math.toDegrees(Math.atan2(-way.x, way.z));
		float pitch = (float) Math.toDegrees(Math.atan2(-way.y, Math.max(0.1, way.horizontalDistance())));
		Vfx.stream(level, from, yaw, pitch, (float) way.length(), 1.4F, 0xF0BFEFFF, 14);
		Vfx.later(level, 8, () -> weaken(level, by));
	}

	/** A pylon's light reaches it: a twentieth of its life (beyond its limit of a hundredth a second; never its last). */
	private void weaken(ServerLevel level, net.minecraft.world.entity.player.Player by) {
		if (!dragon.isAlive() || dragon.isModelDead()) {
			return;
		}
		dragon.setHealth(Math.max(1F, dragon.getHealth() - dragon.getMaxHealth() * PYLON_SHARE));
		// (it flinches, with its own cry)
		level.broadcastDamageEvent(dragon, level.damageSources().playerAttack(by));
		Vfx.wave(level, Vfx.ground(level, dragon.position()), dragon.getBbWidth() + 4F, Vfx.ICE, 14);
		AABB body = dragon.getBoundingBox();
		level.sendParticles(ParticleTypes.END_ROD, body.getCenter().x, body.getCenter().y, body.getCenter().z, 40, body.getXsize() / 3, body.getYsize() / 3,
			body.getZsize() / 3, 0.1);
		ThroneFight.tell(dragon, "iceandfire_fabric.frost.pylon_broken", ChatFormatting.GOLD);
		com.iafabric.pixie.DuelTalk.moment(dragon, com.iafabric.pixie.DuelTalk.Moment.PYLON_BROKEN);
	}

	private void fade(ServerLevel level, Pylon pylon) {
		removePylon(pylon);
		level.sendParticles(ParticleTypes.CLOUD, pylon.at.x, pylon.at.y + 1.5, pylon.at.z, 20, 0.6, 1, 0.6, 0.02);
		level.sendParticles(ParticleTypes.SNOWFLAKE, pylon.at.x, pylon.at.y + 1.5, pylon.at.z, 20, 0.6, 1.2, 0.6, 0.02);
	}

	private static void removePylon(Pylon pylon) {
		for (Display.BlockDisplay shard : pylon.shards) {
			Vfx.remove(shard);
		}
		Vfx.remove(pylon.box);
		Vfx.remove(pylon.crystal);
		Vfx.remove(pylon.tip);
		Vfx.remove(pylon.beam);
		Vfx.remove(pylon.rune);
	}

	private void clearPylons(ServerLevel level) {
		for (Pylon pylon : pylons) {
			fade(level, pylon);
		}
		pylons.clear();
		pylonTimer = Math.max(pylonTimer, 200);
	}

	/** Any pylon left in its arena that is no longer one of its own (an older fight's) goes. */
	private void sweepPylons(ServerLevel level) {
		Vec3 middle = arena != null ? arena : dragon.position();
		for (Entity entity : level.getEntitiesOfClass(Entity.class, new AABB(middle, middle).inflate(ThroneFight.RADIUS + 20, 40, ThroneFight.RADIUS + 20),
			e -> e.getTags().contains(PYLON_TAG))) {
			boolean ours = false;
			for (Pylon pylon : pylons) {
				ours |= entity == pylon.box || entity == pylon.crystal || entity == pylon.tip || pylon.shards.contains(entity);
			}
			if (!ours) {
				entity.discard();
			}
		}
	}

	/** For the tests: its pylons now (raised at once if none stand), and where each stands. */
	public List<Vec3> pylonsNow(ServerLevel level) {
		if (pylons.isEmpty() && arena != null) {
			raisePylons(level);
		}
		List<Vec3> list = new ArrayList<>();
		for (Pylon pylon : pylons) {
			list.add(pylon.at);
		}
		return list;
	}

	/** For the tests: the entity a pylon is struck through (the nearest to a place). */
	public @Nullable Entity pylonNear(Vec3 at) {
		Pylon best = null;
		for (Pylon pylon : pylons) {
			if (best == null || pylon.at.distanceToSqr(at) < best.at.distanceToSqr(at)) {
				best = pylon;
			}
		}
		return best == null ? null : best.box;
	}

	// ---------------------------------------------------------------------------------------------------------------

	/** It carries Cryona over a place a while (Absolute Zero): it flies there, then stays still in the air. */
	void hover(ServerLevel level, Vec3 at, int ticks) {
		chargeTick = -1;
		diveTick = -1;
		prisonTick = -1;
		flyTick = -1;
		letGo();
		grabTick = -1;
		cutBreath();
		hold();
		hoverFrom = dragon.position();
		hoverAt = at;
		hoverTicks = ticks;
		hoverTotal = ticks;
		perch = -1;
		roar();
	}

	private void tickHover(ServerLevel level) {
		// carrying Cryona over the middle while she calls the cold
		hoverTicks--;
		double f = Math.min(1.0, (hoverTotal - hoverTicks) / (double) HOVER_FLY);
		double s = f * f * (3 - 2 * f);
		Vec3 at = hoverFrom.add(hoverAt.subtract(hoverFrom).scale(s)).add(0, 3 * Math.sin(Math.PI * f), 0);
		dragon.teleportTo(at.x, at.y, at.z);
		dragon.setDeltaMovement(Vec3.ZERO);
		dragon.setFlying(f < 1);
		dragon.setHovering(f >= 1);
		if (f < 1) {
			turnTo(hoverAt, 10F);
		}
		if (dragon.tickCount % 6 == 0) {
			level.sendParticles(ParticleTypes.SNOWFLAKE, dragon.getX(), dragon.getY(), dragon.getZ(), 16, 3, 0.5, 3, 0.1);
		}
	}

	/** It comes down on a place and stays there stunned (after Absolute Zero). */
	void land(ServerLevel level, Vec3 at, int ticks) {
		hoverTicks = 0;
		Vec3 floor = Vfx.ground(level, at);
		dragon.setHovering(false);
		dragon.setFlying(false);
		dragon.teleportTo(floor.x, floor.y, floor.z);
		Vfx.wave(level, floor, 10F, Vfx.ICE, 14);
		level.playSound(null, dragon.blockPosition(), SoundEvents.GENERIC_EXPLODE, SoundSource.HOSTILE, 2F, 0.7F);
		if (steered) {
			nextStance = MELEE;
		}
		stun(level, ticks, "iceandfire_fabric.frost.stunned");
	}

	private void hold() {
		if (!steered) {
			dragon.setNoAi(true);
		}
		dragon.setFlying(false);
		dragon.setHovering(false);
		dragon.setDeltaMovement(Vec3.ZERO);
		dragon.getNavigation().stop();
	}

	private void face(Vec3 at) {
		double dx = at.x - dragon.getX();
		double dz = at.z - dragon.getZ();
		face((float) Math.toDegrees(Math.atan2(-dx, dz)));
	}

	private void face(float yaw) {
		dragon.setYRot(yaw);
		dragon.yBodyRot = yaw;
		dragon.yHeadRot = yaw;
	}

	/** It turns on its foe and crouches; a lane through its foe and far past them shows the way it will rush. */
	private void startCharge(ServerLevel level, ServerPlayer foe) {
		hold();
		Vec3 from = Vfx.ground(level, dragon.position());
		Vec3 way = foe.position().subtract(from).multiply(1, 0, 1);
		Vec3 dir = way.lengthSqr() < 1 ? forward() : way.normalize();
		// (through them and on, to the arena's edge at most)
		double length = Math.max(24, Math.min(way.length() + CHARGE_PAST, arena != null ? edgeAlong(from, dir) : 70));
		chargeFrom = new Vec3(from.x, from.y, from.z);
		chargeTo = Vfx.ground(level, from.add(dir.scale(length)));
		chargeWind = WIND_UP;
		rushTicks = Math.max(RUSH, (int) (length / CHARGE_SPEED));
		riderWaits(WIND_UP + rushTicks + 10);
		stampeding = false;
		face(chargeTo);
		float yaw = (float) Math.toDegrees(Math.atan2(-dir.x, dir.z));
		Vfx.lane(level, from, yaw, (float) length, Math.max(3.5F, dragon.getBbWidth() * 0.9F), Vfx.DANGER, WIND_UP);
		roar();
		Vfx.once(level, dragon.position(), SoundEvents.RAVAGER_ROAR, 2F, 0.6F, 40, 40);
		chargeTick = 0;
		hit.clear();
		// it comes down to the ground first if it was up
		dragon.teleportTo(chargeFrom.x, chargeFrom.y, chargeFrom.z);
	}

	private void tickCharge(ServerLevel level) {
		chargeTick++;
		face(chargeTo);
		if (chargeTick < chargeWind) {
			if (chargeTick % 3 == 0) {
				level.sendParticles(ParticleTypes.CLOUD, dragon.getX(), dragon.getY() + 0.2, dragon.getZ(), 8, dragon.getBbWidth() / 2, 0.1, dragon.getBbWidth() / 2, 0.02);
			}
			return;
		}
		int t = chargeTick - chargeWind;
		double f = Math.min(1.0, (t + 1) / (double) rushTicks);
		Vec3 next = chargeFrom.add(chargeTo.subtract(chargeFrom).scale(f));
		if (t == 0) {
			// off it goes: a burst of ice where it stood
			Vfx.wave(level, chargeFrom, dragon.getBbWidth() + 4F, Vfx.ICE, 12);
			level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.PACKED_ICE.defaultBlockState()), chargeFrom.x, chargeFrom.y + 0.4,
				chargeFrom.z, 60, dragon.getBbWidth() / 2, 0.3, dragon.getBbWidth() / 2, 0.4);
			level.playSound(null, BlockPos.containing(chargeFrom), SoundEvents.ENDER_DRAGON_FLAP, SoundSource.HOSTILE, 3F, 0.5F);
		}
		// a pillar or an obelisk in the way: it crashes into it and is stunned
		if (!level.noCollision(dragon, dragon.getBoundingBox().move(next.subtract(dragon.position())).deflate(0.6, 0, 0.6).move(0, 0.6, 0))) {
			chargeTick = -1;
			perch = -1;
			if (stampeding) {
				// (a stampede ends in the stone: stunned, then on foot)
				rushes = 0;
				stampeding = false;
				nextStance = MELEE;
			}
			level.playSound(null, dragon.blockPosition(), SoundEvents.ZOMBIE_ATTACK_IRON_DOOR, SoundSource.HOSTILE, 2.5F, 0.5F);
			level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.PACKED_ICE.defaultBlockState()),
				dragon.getX(), dragon.getY() + 2, dragon.getZ(), 60, 2, 1.5, 2, 0.2);
			stun(level, 90, "iceandfire_fabric.frost.crashed");
			return;
		}
		dragon.teleportTo(next.x, next.y, next.z);
		Vec3 dir = chargeTo.subtract(chargeFrom).normalize();
		rushTrail(level, dir, t);
		for (LivingEntity entity : level.getEntitiesOfClass(LivingEntity.class, dragon.getBoundingBox().inflate(1.5, 1, 1.5), BlackFrostFight::foe)) {
			if (hit.add(entity.getUUID())) {
				Vec3 side = new Vec3(-dir.z, 0, dir.x);
				if (side.dot(entity.position().subtract(dragon.position())) < 0) {
					side = side.scale(-1);
				}
				BossLimits.strike(level, entity, dragon, 0.25F);
				BossLimits.shove(entity, side.x * 1.6 + dir.x * 0.6, 0.6, side.z * 1.6 + dir.z * 0.6);
			}
		}
		if (f >= 1) {
			chargeTick = -1;
			// (a stampede turns for its next rush; in its realm, off its perch)
			busy = stampeding ? 12 : 20;
			perch = -1;
			free();
			// it skids to a stop: ice thrown up before it
			Vfx.wave(level, chargeTo, dragon.getBbWidth() + 3F, Vfx.ICE, 12);
			Vec3 front = chargeTo.add(dir.scale(dragon.getBbWidth() * 0.6));
			level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.PACKED_ICE.defaultBlockState()), front.x, front.y + 0.5, front.z, 70,
				dragon.getBbWidth() / 2, 0.6, dragon.getBbWidth() / 2, 0.45);
			level.sendParticles(ParticleTypes.CLOUD, front.x, front.y + 1, front.z, 30, dragon.getBbWidth() / 2, 0.8, dragon.getBbWidth() / 2, 0.1);
			Vfx.shatter(level, front, 1.2F, 0.5F);
		}
	}

	/**
	 * A rush's might on the ice: ice and snow thrown up at its feet, frost blowing out behind it, shards of ice rising
	 * along its way on both sides, leaning out (they melt away a while after).
	 */
	private void rushTrail(ServerLevel level, Vec3 dir, int t) {
		RandomSource random = dragon.getRandom();
		double w = dragon.getBbWidth();
		Vec3 side = new Vec3(-dir.z, 0, dir.x);
		Vec3 at = dragon.position();
		level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.PACKED_ICE.defaultBlockState()), at.x, at.y + 0.3, at.z, 24, w * 0.6, 0.3, w * 0.6, 0.35);
		level.sendParticles(ParticleTypes.SNOWFLAKE, at.x, at.y + 1, at.z, 20, w * 0.6, 0.8, w * 0.6, 0.15);
		Vec3 back = at.subtract(dir.scale(w));
		level.sendParticles(ParticleTypes.CLOUD, back.x, back.y + 0.8, back.z, 10, w * 0.4, 0.5, w * 0.4, 0.08);
		if (t % 2 == 0) {
			for (int s : new int[]{-1, 1}) {
				Vec3 spot = at.add(side.scale(s * (w * 0.7 + 1 + random.nextDouble())));
				float lean = 20F + random.nextFloat() * 15F;
				Vfx.spike(level, Vfx.ground(level, spot.add(0, 2, 0)), ThroneFight.ice(random), 0.7F + random.nextFloat() * 0.4F, 1.8F + random.nextFloat() * 2.2F,
					(float) side.z * s * lean, (float) -side.x * s * lean, 3, 60);
			}
		}
	}

	/** It climbs high over its foe; a circle marks where it will land. */
	private void startDive(ServerLevel level, ServerPlayer foe) {
		hold();
		riderWaits(70);
		diveSpot = Vfx.ground(level, foe.position());
		diveFrom = dragon.position();
		diveTick = 0;
		perch = -1;
		roar();
		// where it falls (orange: it hurts) and its heart (red: nine tenths of their life)
		// (red: all of it takes three quarters of their life)
		Vfx.circle(level, diveSpot, DIVE_RADIUS, 0xFFFF1A1A, 50);
		Vfx.rune(level, diveSpot, DIVE_RADIUS + 0.5F, EntityVfx.FROST, Vfx.ICE, 60, 2F, 0F);
		ThroneFight.tell(dragon, "iceandfire_fabric.frost.dive", ChatFormatting.RED);
		com.iafabric.pixie.DuelTalk.moment(dragon, com.iafabric.pixie.DuelTalk.Moment.DIVE);
	}

	private void tickDive(ServerLevel level) {
		diveTick++;
		Vec3 high = diveSpot.add(0, 22, 0);
		if (diveTick <= 20) {
			// up
			double f = diveTick / 20.0;
			Vec3 at = diveFrom.add(high.subtract(diveFrom).scale(f));
			dragon.teleportTo(at.x, at.y, at.z);
			dragon.setFlying(true);
			face(diveSpot);
			return;
		}
		if (diveTick < 46) {
			// waiting over the circle, beating its wings
			dragon.setFlying(false);
			dragon.setHovering(true);
			if (diveTick % 5 == 0) {
				level.sendParticles(ParticleTypes.SNOWFLAKE, dragon.getX(), dragon.getY(), dragon.getZ(), 20, 3, 0.5, 3, 0.1);
			}
			return;
		}
		double f = Math.min(1.0, (diveTick - 45) / 4.0);
		Vec3 at = high.add(diveSpot.subtract(high).scale(f));
		dragon.teleportTo(at.x, at.y, at.z);
		if (f < 1) {
			return;
		}
		diveTick = -1;
		dragon.setFlying(false);
		dragon.setHovering(false);
		RandomSource random = dragon.getRandom();
		Vfx.wave(level, diveSpot, DIVE_RADIUS + 5F, Vfx.ICE, 16);
		level.playSound(null, dragon.blockPosition(), SoundEvents.GENERIC_EXPLODE, SoundSource.HOSTILE, 3F, 0.6F);
		level.sendParticles(ParticleTypes.EXPLOSION_EMITTER, diveSpot.x, diveSpot.y + 1, diveSpot.z, 1, 0, 0, 0, 0);
		for (int i = 0; i < 16; i++) {
			double angle = Math.PI * 2 * i / 16;
			double distance = 6 + random.nextDouble() * 4.5;
			Vec3 spot = diveSpot.add(Math.cos(angle) * distance, 0, Math.sin(angle) * distance);
			Vfx.spike(level, spot, ThroneFight.ice(random), 0.8F, 2.5F + random.nextFloat() * 2.5F, (float) Math.sin(angle) * 25F,
				(float) -Math.cos(angle) * 25F, 3, 70);
		}
		float reach = DIVE_RADIUS + 0.3F;
		for (LivingEntity entity : level.getEntitiesOfClass(LivingEntity.class, new AABB(diveSpot, diveSpot).inflate(reach, 4, reach), BlackFrostFight::foe)) {
			double d = Math.sqrt(entity.position().subtract(diveSpot).horizontalDistanceSqr());
			if (d > reach) {
				continue;
			}
			// all of its circle alike: where it was drawn, it strikes
			Vec3 away = d < 0.1 ? new Vec3(1, 0, 0) : entity.position().subtract(diveSpot).multiply(1, 0, 1).normalize();
			BossLimits.doom(level, entity, dragon, DIVE_SHARE);
			BossLimits.shove(entity, away.x * 1.4, 0.6, away.z * 1.4);
		}
		stun(level, 100, "iceandfire_fabric.frost.stunned");
	}

	private void stun(ServerLevel level, int ticks, String key) {
		stunned = ticks;
		hold();
		ThroneFight.tell(dragon, key, ChatFormatting.GOLD);
		com.iafabric.pixie.DuelTalk.moment(dragon, com.iafabric.pixie.DuelTalk.Moment.STUNNED);
		Vfx.rune(level, Vfx.ground(level, dragon.position()), dragon.getBbWidth() * 0.7F, EntityVfx.FROST, 0x80FFFFFF, ticks, 0.5F, 0F);
	}

	/**
	 * Its hail round each foe, in two waves, never one circle over another: first a ring of circles round them and one
	 * at their feet (between the two, or out through a gap, is safe); then, where they went, a narrower ring and one at
	 * their feet again, sooner. A block of ice falls on each circle as it fills.
	 */
	private void hail(ServerLevel level, List<ServerPlayer> foes, RandomSource random) {
		busy = 50;
		roar();
		int count = 0;
		for (ServerPlayer foe : foes) {
			if (count++ >= 3) {
				break;
			}
			UUID id = foe.getUUID();
			hailWave(level, floorUnder(level, foe), 7, 6.6, 30, random);
			Vfx.later(level, 28, () -> {
				if (dragon.isAlive() && !dragon.isModelDead() && level.getEntity(id) instanceof ServerPlayer target && target.isAlive()) {
					hailWave(level, floorUnder(level, target), 5, 6.0, 20, random);
				}
			});
		}
	}

	/** The ice under a foe (one leaping or thrown is marked on the floor below them). */
	private Vec3 floorUnder(ServerLevel level, LivingEntity foe) {
		return Vfx.ground(level, new Vec3(foe.getX(), Math.min(foe.getY(), dragon.getY() + 3), foe.getZ()));
	}

	/** One wave of its hail: a ring of circles round a place (turned at random) and one on it, filling in that many ticks. */
	private void hailWave(ServerLevel level, Vec3 middle, int count, double distance, int fill, RandomSource random) {
		List<Vec3> spots = new ArrayList<>(Vfx.ring(middle, count, distance, random.nextDouble() * 360));
		spots.add(middle);
		for (Vec3 spot : spots) {
			Vec3 at = Vfx.ground(level, spot.add(0, 2, 0));
			Vfx.circle(level, at, HAIL_RADIUS, Vfx.ICE, fill);
			var block = Vfx.shape(level, at.add(0, 20, 0), ThroneFight.ice(random), 1.3F, 1.3F, random.nextFloat() * 45, random.nextFloat() * 45, true);
			Vfx.later(level, fill - 8, () -> {
				if (block != null) {
					Vfx.move(block, at, 8);
				}
			});
			Vfx.later(level, fill, () -> {
				Vfx.remove(block);
				level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.PACKED_ICE.defaultBlockState()), at.x, at.y + 0.5, at.z, 20, 0.6, 0.4, 0.6, 0.1);
				Vfx.shatter(level, at, 1F, 0.8F + random.nextFloat() * 0.3F);
				float reach = HAIL_RADIUS + 0.2F;
				for (LivingEntity entity : level.getEntitiesOfClass(LivingEntity.class, new AABB(at, at).inflate(reach, 3, reach), BlackFrostFight::foe)) {
					if (entity.position().subtract(at).horizontalDistanceSqr() <= reach * reach) {
						BossLimits.strike(level, entity, dragon, 0.1F);
						BossCold.slow(entity, 30, 0, dragon);
					}
				}
			});
		}
	}

	/** It roars; a circle of frost fills round it, then all in it are thrown back and slowed. */
	private void glacialRoar(ServerLevel level) {
		busy = 40;
		hold();
		roar();
		Vec3 at = Vfx.ground(level, dragon.position());
		Vfx.circle(level, at, 16F, Vfx.ICE, 25);
		Vfx.rune(level, at, 16.5F, EntityVfx.FROST, Vfx.ICE, 32, -6F, 0F);
		Vfx.later(level, 25, () -> {
			if (!dragon.isAlive() || dragon.isModelDead()) {
				return;
			}
			Vfx.wave(level, at, 19F, Vfx.ICE, 14);
			level.playSound(null, dragon.blockPosition(), SoundEvents.ENDER_DRAGON_GROWL, SoundSource.HOSTILE, 3F, 1.2F);
			for (LivingEntity entity : level.getEntitiesOfClass(LivingEntity.class, new AABB(at, at).inflate(16, 5, 16), BlackFrostFight::foe)) {
				if (entity.position().subtract(at).horizontalDistanceSqr() > 16.3 * 16.3) {
					continue;
				}
				Vec3 away = entity.position().subtract(at).multiply(1, 0, 1).normalize();
				BossLimits.strike(level, entity, dragon, 0.08F);
				BossCold.slow(entity, 50, 0, dragon);
				BossLimits.shove(entity, away.x * 1.8, 0.5, away.z * 1.8);
			}
			if (stunned <= 0 && hoverTicks <= 0) {
				free();
			}
		});
	}

	/**
	 * The Glacial Prison (rare): it lands, rears and draws the cold to its foe; a great deep blue circle fills slowly
	 * round them. Who is still in it when it is full is frozen solid for four seconds, Frost Ward or not (and the two
	 * of them leave the frozen alone a while).
	 */
	private void startPrison(ServerLevel level, ServerPlayer foe) {
		hold();
		riderWaits(PRISON_FILL + 20);
		prisonAt = Vfx.ground(level, foe.position());
		prisonTick = 0;
		face(prisonAt);
		roar();
		Vfx.circle(level, prisonAt, PRISON_RADIUS, PRISON_COLOR, PRISON_FILL);
		Vfx.rune(level, prisonAt, PRISON_RADIUS + 0.5F, EntityVfx.FROST, PRISON_COLOR, PRISON_FILL + 10, 1.2F, 0F);
		Vfx.beam(level, prisonAt, 0.6F, 14F, Vfx.ICE, PRISON_FILL);
		level.playSound(null, dragon.blockPosition(), SoundEvents.WARDEN_SONIC_CHARGE, SoundSource.HOSTILE, 2F, 0.6F);
		ThroneFight.tell(dragon, "iceandfire_fabric.frost.prison", ChatFormatting.RED);
		com.iafabric.pixie.DuelTalk.moment(dragon, com.iafabric.pixie.DuelTalk.Moment.PRISON);
	}

	private void tickPrison(ServerLevel level) {
		prisonTick++;
		face(prisonAt);
		if (prisonTick < PRISON_FILL) {
			if (prisonTick % 3 == 0) {
				// the cold gathers on the circle
				RandomSource random = dragon.getRandom();
				for (int i = 0; i < 4; i++) {
					double a = random.nextDouble() * Math.PI * 2;
					Vec3 from = prisonAt.add(Math.cos(a) * PRISON_RADIUS, 0.2, Math.sin(a) * PRISON_RADIUS);
					Vec3 dir = prisonAt.subtract(from).normalize();
					level.sendParticles(ParticleTypes.SNOWFLAKE, from.x, from.y, from.z, 0, dir.x, 0.05, dir.z, 0.25);
				}
			}
			return;
		}
		prisonTick = -1;
		busy = 40;
		free();
		Vfx.wave(level, prisonAt, PRISON_RADIUS + 2, PRISON_COLOR, 12);
		level.playSound(null, prisonAt.x, prisonAt.y, prisonAt.z, SoundEvents.GLASS_PLACE, SoundSource.HOSTILE, 3F, 0.5F);
		boolean caught = false;
		for (LivingEntity entity : level.getEntitiesOfClass(LivingEntity.class, new AABB(prisonAt, prisonAt).inflate(PRISON_RADIUS, 4, PRISON_RADIUS), BlackFrostFight::foe)) {
			if (entity.position().subtract(prisonAt).horizontalDistanceSqr() > PRISON_RADIUS * PRISON_RADIUS) {
				continue;
			}
			caught = true;
			BossLimits.strike(level, entity, dragon, 0.1F);
			BossCold.prison(level, entity, PRISON_HOLD);
			// a shell of ice round them
			Display.BlockDisplay shell = Vfx.shape(level, entity.position().subtract(0, 0.1, 0), Blocks.ICE.defaultBlockState(),
				entity.getBbWidth() + 0.7F, entity.getBbHeight() + 0.4F, 0F, 0F, true);
			Vfx.later(level, PRISON_HOLD, () -> {
				Vfx.remove(shell);
				level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.ICE.defaultBlockState()), entity.getX(), entity.getY() + 1, entity.getZ(), 30, 0.4, 0.8, 0.4, 0.1);
			});
			if (entity instanceof ServerPlayer player) {
				player.displayClientMessage(Component.translatable("iceandfire_fabric.frost.prisoned").withStyle(ChatFormatting.AQUA, ChatFormatting.BOLD), true);
			}
		}
		if (caught) {
			// they leave the frozen alone a while
			busy = PRISON_HOLD - 10;
			riderWaits(PRISON_HOLD - 10);
		}
	}

	/**
	 * Its breath: it rears and draws a deep breath while a lane shows on the ground before it (as far as its foe and on),
	 * then a jet of frost sweeps down from its jaws to the ground at the lane's end: out of the lane, to its sides or
	 * behind it, is safe. The super breath (under a third of its life) is longer, wider and blows longer.
	 */
	private void startBreath(ServerLevel level, ServerPlayer foe, boolean great) {
		hold();
		greatBreath = great;
		breathWind = great ? BREATH_WIND_UP : FROST_BREATH_WIND_UP;
		breathBlow = great ? BREATH_BLOW : FROST_BREATH_BLOW;
		breathWidth = great ? BREATH_WIDTH : FROST_BREATH_WIDTH;
		riderWaits(breathWind + breathBlow + 10);
		breathTick = 0;
		// (turned to its foe first: its head, far before it, moves as it turns; its lane starts under its head as it is then)
		face(foe.position());
		for (int i = 0; i < 2; i++) {
			Vec3 head = jaws();
			breathFrom = Vfx.ground(level, new Vec3(head.x, dragon.getY(), head.z));
			breathYaw = (float) Math.toDegrees(Math.atan2(-(foe.getX() - breathFrom.x), foe.getZ() - breathFrom.z));
			face(breathYaw);
		}
		double reach = Math.sqrt(foe.position().subtract(breathFrom).horizontalDistanceSqr()) + 10;
		float most = great ? (steered ? BREATH_LENGTH_REALM : BREATH_LENGTH) : FROST_BREATH_LENGTH;
		breathLength = (float) Mth.clamp(reach, 18, most);
		Vfx.lane(level, breathFrom, breathYaw, breathLength, breathWidth, Vfx.DANGER, breathWind);
		roar();
		// its breath's own sound, as long as it lasts: the charge as it draws the air in, its blast the moment the jet comes
		// out (each sound's blast is so far into it: it starts that much before the wind-up's end), its echo after
		int count = ++breaths;
		Vfx.later(level, Math.max(1, breathWind - (great ? GREAT_BREATH_BLAST : BREATH_BLAST)), () -> {
			if (count == breaths && breathTick >= 0) {
				level.playSound(null, dragon.blockPosition(), great ? com.iafabric.misc.IafSoundRegistry.BLACK_FROST_GREAT_BREATH
					: com.iafabric.misc.IafSoundRegistry.BLACK_FROST_BREATH, SoundSource.HOSTILE, 4F, 1F);
			}
		});
		ThroneFight.tell(dragon, great ? "iceandfire_fabric.frost.super_breath" : "iceandfire_fabric.frost.breath", great ? ChatFormatting.RED : ChatFormatting.GOLD);
		com.iafabric.pixie.DuelTalk.moment(dragon, great ? com.iafabric.pixie.DuelTalk.Moment.GREAT_BREATH : com.iafabric.pixie.DuelTalk.Moment.BREATH);
	}

	private void tickBreath(ServerLevel level) {
		breathTick++;
		face(breathYaw);
		Vec3 head = jaws();
		double r = Math.toRadians(breathYaw);
		Vec3 dir = new Vec3(-Math.sin(r), 0, Math.cos(r));
		if (breathTick < breathWind) {
			if (breathTick % 2 == 0) {
				// it draws the air in
				RandomSource random = dragon.getRandom();
				Vec3 from = head.add((random.nextDouble() - 0.5) * 8, random.nextDouble() * 3, (random.nextDouble() - 0.5) * 8).add(dir.scale(5));
				Vec3 way = head.subtract(from).normalize();
				level.sendParticles(ParticleTypes.CLOUD, from.x, from.y, from.z, 0, way.x, way.y, way.z, 0.4);
			}
			return;
		}
		if (breathTick == breathWind) {
			Vec3 mouth = head.add(dir.scale(1.5));
			// from its jaws down to the ground at the lane's end: the jet sweeps the lane, over who stands in it
			Vec3 end = breathFrom.add(dir.scale(breathLength));
			Vec3 way = end.subtract(mouth);
			double run = Math.max(1, Math.sqrt(way.horizontalDistanceSqr()));
			double drop = mouth.y - end.y - 0.3;
			float pitch = (float) Math.toDegrees(Math.atan2(drop, run));
			float yaw = (float) Math.toDegrees(Math.atan2(-way.x, way.z));
			Vfx.stream(level, mouth, yaw, pitch, (float) Math.sqrt(run * run + drop * drop) + 1, breathWidth, Vfx.ICE, breathBlow + 4);
			dragon.setBreathingFire(true);
		}
		int t = breathTick - breathWind;
		dragon.setXRot(Mth.approach(dragon.getXRot(), HEAD_DOWN, 2F));
		{
			// frost billowing from its jaws down to the lane's end, all along its jet (seen from aside as from before it)
			Vec3 mouth = head.add(dir.scale(1.5));
			Vec3 path = breathFrom.add(dir.scale(breathLength)).subtract(mouth);
			Vec3 way = path.normalize();
			RandomSource random = dragon.getRandom();
			for (int i = 0; i < 6; i++) {
				Vec3 spread = way.add((random.nextDouble() - 0.5) * 0.25, (random.nextDouble() - 0.5) * 0.15, (random.nextDouble() - 0.5) * 0.25);
				level.sendParticles(ParticleTypes.SNOWFLAKE, mouth.x, mouth.y, mouth.z, 0, spread.x, spread.y, spread.z, 1.4);
			}
			for (int i = 0; i < 24; i++) {
				double f = Math.sqrt(random.nextDouble());
				double spread = (0.6 + breathWidth * 0.5 * f) * Math.sqrt(random.nextDouble());
				double a = random.nextDouble() * Math.PI * 2;
				Vec3 at = mouth.add(path.scale(f)).add(Math.cos(a) * spread, Math.sin(a) * spread * 0.6, Math.sin(a) * spread);
				level.sendParticles(i % 3 == 0 ? ParticleTypes.SNOWFLAKE : i % 3 == 1 ? ParticleTypes.CLOUD : ParticleTypes.CLOUD, at.x,
					Math.max(at.y, breathFrom.y + 0.2), at.z, 0, way.x, way.y, way.z, 0.3);
			}
		}
		if (t % 4 == 0) {
			RandomSource random = dragon.getRandom();
			Vec3 side = new Vec3(-dir.z, 0, dir.x);
			// frost over the whole lane
			for (int i = 0; i < 8; i++) {
				Vec3 at = breathFrom.add(dir.scale(random.nextDouble() * breathLength)).add(side.scale((random.nextDouble() - 0.5) * breathWidth));
				level.sendParticles(ParticleTypes.SNOWFLAKE, at.x, at.y + 0.6, at.z, 5, 0.5, 0.4, 0.5, 0.05);
				level.sendParticles(ParticleTypes.CLOUD, at.x, at.y + 0.4, at.z, 1, 0.6, 0.2, 0.6, 0.02);
			}
			if (t % 8 == 0) {
				Vec3 edge = breathFrom.add(dir.scale(4 + random.nextDouble() * (breathLength - 6)));
				Vec3 out = side.scale((random.nextBoolean() ? 1 : -1) * breathWidth / 2);
				Vfx.spike(level, edge.add(out), ThroneFight.ice(random), 0.6F, 1.5F + random.nextFloat() * 1.5F, 0F, 0F, 3, 50);
			}
			AABB box = new AABB(breathFrom, breathFrom.add(dir.scale(breathLength))).inflate(breathWidth, 4, breathWidth);
			for (LivingEntity entity : level.getEntitiesOfClass(LivingEntity.class, box, BlackFrostFight::foe)) {
				Vec3 rel = entity.position().subtract(breathFrom);
				double along = rel.dot(dir);
				if (along < -1 || along > breathLength + 1 || Math.abs(rel.dot(side)) > breathWidth / 2 + 0.4) {
					continue;
				}
				// without the pixie's gift it melts them; with it, a cold blow their armor lessens
				BossCold.breathOf(level, entity, dragon, greatBreath);
				BossCold.slow(entity, 30, 1, dragon);
			}
		}
		if (t >= breathBlow) {
			endBreath();
			busy = 40;
			free();
		}
	}

	private void endBreath() {
		if (breathTick >= 0) {
			breathTick = -1;
			dragon.setBreathingFire(false);
		}
	}

	/** A breath cut short (its duel waits, Absolute Zero): its sound goes silent with it. */
	private void cutBreath() {
		if (breathTick >= 0 && dragon.level() instanceof ServerLevel level) {
			net.minecraft.resources.ResourceLocation sound = (greatBreath ? com.iafabric.misc.IafSoundRegistry.BLACK_FROST_GREAT_BREATH
				: com.iafabric.misc.IafSoundRegistry.BLACK_FROST_BREATH).getLocation();
			for (ServerPlayer player : level.getPlayers(p -> p.distanceToSqr(dragon) < 96 * 96)) {
				player.connection.send(new net.minecraft.network.protocol.game.ClientboundStopSoundPacket(sound, SoundSource.HOSTILE));
			}
		}
		endBreath();
	}

	/** For the tests: whether a great blow of its is under way, and which. */
	public @Nullable String greatBlow() {
		return chargeTick >= 0 ? "charge" : diveTick >= 0 ? "dive" : prisonTick >= 0 ? "prison" : breathTick >= 0 ? "breath" : hoverTicks > 0 ? "hover"
			: flyTick >= 0 ? "flight" : grabTick >= 0 ? (held != null ? "holding" : "grab") : null;
	}

	/** For the tests: the perch it stands on (-1 none), and whether it is stunned. */
	public int perch() {
		return perch;
	}

	public boolean stunned() {
		return stunned > 0;
	}

	/** For the tests: its way of fighting now (ranged, melee, stampede), and whether a blow on foot is coming. */
	public String stance() {
		return stance == MELEE ? "melee" : stance == STAMPEDE ? "stampede" : "ranged";
	}

	public boolean striking() {
		return strikeTick >= 0;
	}
}

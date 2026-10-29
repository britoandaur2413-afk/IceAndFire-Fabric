package com.iafabric.entity;

import com.iafabric.world.structure.PillagerKingdomPiece;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.illager.Pillager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.entity.projectile.arrow.Arrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * The riders of the pillagers' and the dread's dragons. The rider is drawn on the dragon's back (it is not an entity
 * of its own) with its own hitbox and health: a pillager, the warlord (on a kingdom's elite) or a dread knight. Hit
 * enough, it falls dead from the dragon (a real one, with its drops). Then the dragon lands and goes to the nearest
 * of its riders on foot (a pillager, a dread knight; it glows so it can be stopped), who climbs on: the rider is back.
 * With nobody near to ride it, the dragon fights on with its breath. While ridden, the rider fights (a pillager shoots,
 * the warlord uses the elite's frost, a knight strikes what comes near) and the dragon bites: its breath only now and
 * then, when its rider bids it.
 */
public final class DragonRiders {
	public static final int NONE = 0;
	public static final int PILLAGER = 1;
	public static final int WARLORD = 2;
	public static final int KNIGHT = 3;
	public static final byte EVENT_RIDER_HURT = 81;
	/** How far a dragon looks for a new rider. */
	private static final int SEARCH = 32;
	/** How long it tries to reach one before it gives up (ticks), and how long it then fights alone. */
	private static final int GIVE_UP = 400;
	private static final int ALONE = 600;

	private DragonRiders() {
	}

	/** A dragon that carries a rider of its own: a kingdom's or the dread's (not Black Frost, the queen rides it). */
	public static boolean hasRiders(EntityDragonBase dragon) {
		return (dragon.pillagerMount || dragon.dreadMount) && !dragon.isBlackFrost() && !dragon.isTame();
	}

	public static float maxHealth(int kind) {
		return switch (kind) {
			case WARLORD -> 80F;
			case KNIGHT -> 40F;
			default -> 24F;
		};
	}

	/** Gives it a fresh rider (structures). */
	public static void mount(EntityDragonBase dragon, int kind) {
		dragon.setVirtualRider(kind);
		dragon.riderHealth = maxHealth(kind);
	}

	/** Where its rider's feet are (the hitbox stands there): over a player's seat, more for a bigger back. */
	public static Vec3 seat(EntityDragonBase dragon) {
		// (its feet: the seat on the dragon's back bone, where the player's game draws it, less its seat over its feet)
		return DragonSeatPose.world(dragon).subtract(0, DragonSeatPose.RIDER_SEAT, 0);
	}

	/** Every tick on the server for a dragon of the pillagers or the dread (from DragonSmarts). */
	public static void tick(EntityDragonBase dragon, ServerLevel level) {
		if (!hasRiders(dragon) || dragon.isNoAi() || dragon.fightLed) {
			return;
		}
		adoptPassengers(dragon);
		if (dragon.getVirtualRider() != NONE) {
			if (dragon.riderCandidate != null) {
				dragon.riderCandidate = null;
			}
			dragon.berserk = false;
			keepPart(dragon, level);
			fight(dragon, level);
			return;
		}
		dropPart(dragon);
		seek(dragon, level);
	}

	/** Riders of old (real passengers) become its rider. */
	private static void adoptPassengers(EntityDragonBase dragon) {
		for (Entity passenger : java.util.List.copyOf(dragon.getPassengers())) {
			if (passenger instanceof EntityDreadQueen || !(passenger instanceof LivingEntity living) || !dragon.carriesRider(passenger)) {
				continue;
			}
			if (dragon.getVirtualRider() == NONE) {
				dragon.setVirtualRider(kindOf(living));
				dragon.riderHealth = Math.max(4F, living.getHealth());
				passenger.stopRiding();
				passenger.discard();
			} else {
				passenger.stopRiding();
			}
		}
	}

	private static int kindOf(Entity rider) {
		if (rider instanceof EntityDreadMob) {
			return KNIGHT;
		}
		return PillagerDragons.isWarlord(rider) ? WARLORD : PILLAGER;
	}

	static void keepPart(EntityDragonBase dragon, ServerLevel level) {
		if (dragon.riderPart == null || dragon.riderPart.isRemoved()) {
			EntityDragonRiderPart part = new EntityDragonRiderPart(dragon);
			dragon.riderPart = part;
			level.addFreshEntity(part);
		}
	}

	private static void dropPart(EntityDragonBase dragon) {
		if (dragon.riderPart != null) {
			dragon.riderPart.remove(Entity.RemovalReason.DISCARDED);
			dragon.riderPart = null;
		}
	}

	/** A hit on its rider's hitbox (EntityDragonRiderPart). */
	public static boolean hurtRider(EntityDragonBase dragon, ServerLevel level, DamageSource source, float amount) {
		int kind = dragon.getVirtualRider();
		if (kind == NONE) {
			return dragon.hurtServer(level, source, amount);
		}
		Entity attacker = source.getEntity();
		if (attacker == dragon || attacker != null && dragon.isAlliedTo(attacker) || source.is(DamageTypeTags.IS_FALL)
			|| source.is(DamageTypeTags.IS_DROWNING) || dragon.isModelDead()) {
			return false;
		}
		float taken = amount * (kind == WARLORD ? 0.6F : kind == KNIGHT ? 0.75F : 1F);
		dragon.riderHealth -= taken;
		level.broadcastEntityEvent(dragon, EVENT_RIDER_HURT);
		Vec3 at = seat(dragon);
		level.playSound(null, at.x, at.y + 1, at.z, kind == KNIGHT ? SoundEvents.WITHER_SKELETON_HURT : SoundEvents.PILLAGER_HURT, SoundSource.HOSTILE, 1F, 1F);
		if (attacker instanceof LivingEntity living && !(attacker instanceof Player player && (player.isCreative() || player.isSpectator()))) {
			dragon.setLastHurtByMob(living);
		}
		if (dragon.riderHealth <= 0) {
			com.iafabric.IafMod.LOGGER.debug("rider of {} killed by {} ({})", dragon, source.getMsgId(), attacker);
			riderFalls(dragon, level, source);
		}
		return true;
	}

	/** Its rider is killed: it falls dead from the dragon, a real one (its drops, its experience to who killed it). */
	private static void riderFalls(EntityDragonBase dragon, ServerLevel level, DamageSource source) {
		int kind = dragon.getVirtualRider();
		Vec3 at = seat(dragon);
		dragon.setVirtualRider(NONE);
		dragon.riderHealth = 0;
		dropPart(dragon);
		Mob body = spawnRider(level, kind, at);
		if (body != null) {
			body.hurtServer(level, source, 1000F);
			if (body.isAlive()) {
				body.kill(level);
			}
		}
		level.sendParticles(ParticleTypes.POOF, at.x, at.y + 1, at.z, 12, 0.3, 0.5, 0.3, 0.02);
		announce(level, dragon, "iceandfire_fabric.rider.down");
		dragon.randomizeAttacks();
	}

	/** The dragon dies: its rider jumps off, alive. */
	static void onDragonDeath(EntityDragonBase dragon, ServerLevel level) {
		int kind = dragon.getVirtualRider();
		if (kind == NONE) {
			return;
		}
		Vec3 at = seat(dragon);
		dragon.setVirtualRider(NONE);
		dropPart(dragon);
		Mob rider = spawnRider(level, kind, at);
		if (rider != null) {
			rider.setHealth(Math.max(2F, Math.min(rider.getMaxHealth(), dragon.riderHealth)));
			rider.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, 160, 0));
		}
	}

	/** A real rider of that kind where the drawn one was. */
	private static Mob spawnRider(ServerLevel level, int kind, Vec3 at) {
		Mob mob = kind == KNIGHT ? IafEntityRegistry.DREAD_KNIGHT.create(level, EntitySpawnReason.MOB_SUMMONED)
			: EntityTypes.PILLAGER.create(level, EntitySpawnReason.MOB_SUMMONED);
		if (mob == null) {
			return null;
		}
		mob.snapTo(at.x, at.y, at.z, level.getRandom().nextFloat() * 360, 0);
		mob.finalizeSpawn(level, level.getCurrentDifficultyAt(mob.blockPosition()), EntitySpawnReason.MOB_SUMMONED, null);
		if (kind == WARLORD) {
			PillagerKingdomPiece.warlord(level, mob);
		}
		mob.setPersistenceRequired();
		level.addFreshEntity(mob);
		return mob;
	}

	/**
	 * No rider: it lands and goes to the nearest one who may ride it, who climbs on when it is there. With none near
	 * (or none it can reach), it fights alone, with its breath.
	 */
	private static void seek(EntityDragonBase dragon, ServerLevel level) {
		long now = level.getGameTime();
		Mob candidate = null;
		if (dragon.riderCandidate != null) {
			if (level.getEntity(dragon.riderCandidate) instanceof Mob mob && mayRide(dragon, mob)) {
				candidate = mob;
			} else {
				dragon.riderCandidate = null;
			}
		}
		if (candidate == null && now >= dragon.riderGiveUpUntil && (now + dragon.getId()) % 20 == 0) {
			candidate = nearest(dragon, level);
			if (candidate != null) {
				dragon.riderCandidate = candidate.getUUID();
				dragon.riderSearchSince = now;
				dragon.riderBestDistance = Double.MAX_VALUE;
				dragon.riderProgressAt = now;
				dragon.walkingToRider = false;
				dragon.berserk = false;
				dragon.setTarget(null);
			}
		}
		if (candidate == null) {
			if (!dragon.berserk && (now + dragon.getId()) % 20 == 0) {
				dragon.berserk = true;
				dragon.randomizeAttacks();
				announce(level, dragon, "iceandfire_fabric.rider.wild");
			}
			return;
		}
		if (now - dragon.riderSearchSince > GIVE_UP) {
			// it cannot get there: it fights alone for a while
			dragon.riderCandidate = null;
			dragon.riderGiveUpUntil = now + ALONE;
			return;
		}
		if (dragon.getTarget() != null) {
			dragon.setTarget(null);
		}
		if (dragon.isFlying() || dragon.isHovering()) {
			dragon.setFlying(false);
			dragon.setHovering(false);
		}
		if (dragon.isOrderedToSit()) {
			dragon.setCommand(0);
		}
		if (dragon.isSleeping()) {
			dragon.setInSittingPose(false);
		}
		if (now % 20 == 0) {
			candidate.addEffect(new MobEffectInstance(MobEffects.GLOWING, 30, 0, false, false));
		}
		// called to ride, it stops fighting and runs to the dragon
		if (candidate.getTarget() != null) {
			candidate.setTarget(null);
		}
		double dx = candidate.getX() - dragon.getX();
		double dz = candidate.getZ() - dragon.getZ();
		double distance = Math.sqrt(dx * dx + dz * dz);
		double reach = dragon.getBbWidth() / 2 + 4;
		if (distance <= reach && Math.abs(candidate.getY() - dragon.getY()) < 4 + dragon.getBbHeight() / 2) {
			// the rider leaps on
			climbOn(dragon, candidate, level);
			return;
		}
		if (distance < dragon.riderBestDistance - 1) {
			dragon.riderBestDistance = distance;
			dragon.riderProgressAt = now;
		}
		if (now - dragon.riderProgressAt > 60) {
			if (distance < 8 + dragon.getBbWidth() / 2 || distance < 12 + dragon.getBbWidth() / 2 && (candidate.hasLineOfSight(dragon) || dragon.hasLineOfSight(candidate))) {
				// it cannot come nearer (a ledge, a wall between): the rider runs and leaps the rest
				climbOn(dragon, candidate, level);
				return;
			}
			// the path leads nowhere: straight to it
			dragon.walkingToRider = true;
		}
		if ((now + dragon.getId()) % 10 == 0) {
			if (!dragon.walkingToRider) {
				dragon.walkingToRider = !dragon.getNavigation().moveTo(candidate, 1.1);
			}
			candidate.getNavigation().moveTo(dragon, 1.2);
		}
		if (dragon.walkingToRider || dragon.getNavigation().isDone()) {
			// no way found by the pathfinder (a big dragon in a narrow place): straight to it
			dragon.getNavigation().stop();
			dragon.getMoveControl().setWantedPosition(candidate.getX(), candidate.getY(), candidate.getZ(), 1.1);
		}
	}

	/** Who may ride it: a pillager for a kingdom's dragon (the warlord only on the elite), a dread knight for the dread's. */
	private static boolean mayRide(EntityDragonBase dragon, Mob mob) {
		if (!mob.isAlive() || mob.isPassenger() || mob.isVehicle() || mob.isNoAi() || mob.distanceToSqr(dragon) > (SEARCH + 8) * (SEARCH + 8)) {
			return false;
		}
		if (dragon.pillagerMount) {
			return mob instanceof Pillager && (!PillagerDragons.isWarlord(mob) || dragon.eliteMount);
		}
		return mob instanceof EntityDreadKnight;
	}

	private static Mob nearest(EntityDragonBase dragon, ServerLevel level) {
		Mob best = null;
		double bestDistance = Double.MAX_VALUE;
		for (Mob mob : level.getEntitiesOfClass(Mob.class, dragon.getBoundingBox().inflate(SEARCH), m -> mayRide(dragon, m))) {
			double d = mob.distanceToSqr(dragon);
			if (d < bestDistance) {
				bestDistance = d;
				best = mob;
			}
		}
		return best;
	}

	private static void climbOn(EntityDragonBase dragon, Mob rider, ServerLevel level) {
		int kind = kindOf(rider);
		dragon.setVirtualRider(kind);
		dragon.riderHealth = Math.max(4F, rider.getHealth());
		dragon.riderCandidate = null;
		dragon.berserk = false;
		level.sendParticles(ParticleTypes.CLOUD, rider.getX(), rider.getY() + 1, rider.getZ(), 10, 0.3, 0.5, 0.3, 0.02);
		Vec3 at = seat(dragon);
		level.playSound(null, at.x, at.y, at.z, SoundEvents.ARMOR_EQUIP_IRON.value(), SoundSource.HOSTILE, 1.2F, 0.8F);
		rider.discard();
		dragon.roar();
		dragon.randomizeAttacks();
		keepPart(dragon, level);
	}

	/** Its rider fights: a pillager shoots, the warlord shoots three at once and uses the frost, a knight strikes. */
	private static void fight(EntityDragonBase dragon, ServerLevel level) {
		LivingEntity target = dragon.getTarget();
		if (target == null || !target.isAlive() || dragon.isSleeping()) {
			return;
		}
		int kind = dragon.getVirtualRider();
		long now = level.getGameTime() + dragon.getId();
		Vec3 eye = seat(dragon).add(0, 1.1, 0);
		Vec3 aim = new Vec3(target.getX(), target.getY(0.6), target.getZ());
		double distance = eye.distanceTo(aim);
		if (kind == KNIGHT) {
			if (distance < 4.5 && now % 25 == 0) {
				target.hurtServer(level, level.damageSources().mobAttack(dragon), 7F);
				level.playSound(null, eye.x, eye.y, eye.z, SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.HOSTILE, 1F, 0.8F);
				level.sendParticles(ParticleTypes.SWEEP_ATTACK, target.getX(), target.getY(0.6), target.getZ(), 1, 0, 0, 0, 0);
			}
			return;
		}
		int every = kind == WARLORD ? 30 : 45;
		if (distance < 30 && now % every == 0 && sees(level, dragon, eye, aim)) {
			shoot(dragon, level, eye, aim, kind == WARLORD ? 3 : 1);
		}
	}

	private static boolean sees(ServerLevel level, EntityDragonBase dragon, Vec3 from, Vec3 to) {
		return level.clip(new ClipContext(from, to, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, dragon)).getType() == HitResult.Type.MISS;
	}

	private static void shoot(EntityDragonBase dragon, ServerLevel level, Vec3 eye, Vec3 aim, int count) {
		Vec3 way = aim.subtract(eye);
		double flat = Math.sqrt(way.x * way.x + way.z * way.z);
		// out of its own hitbox first
		Vec3 from = eye.add(way.normalize().scale(1.2));
		for (int i = 0; i < count; i++) {
			double turn = Math.toRadians(i == 0 ? 0 : i == 1 ? 9 : -9);
			double x = way.x * Math.cos(turn) - way.z * Math.sin(turn);
			double z = way.x * Math.sin(turn) + way.z * Math.cos(turn);
			Arrow arrow = new Arrow(level, from.x, from.y, from.z, new ItemStack(Items.ARROW), new ItemStack(Items.CROSSBOW));
			arrow.setOwner(dragon);
			arrow.pickup = AbstractArrow.Pickup.DISALLOWED;
			arrow.shoot(x, way.y + flat * 0.12, z, 2.8F, 3F);
			level.addFreshEntity(arrow);
		}
		level.playSound(null, eye.x, eye.y, eye.z, SoundEvents.CROSSBOW_SHOOT, SoundSource.HOSTILE, 1F, 1F);
	}

	/**
	 * Its attacks as its rider leads them: ridden, it bites and strikes (its breath only now and then, when the
	 * warlord or a knight bids it); alone with no one to ride it, it uses its breath.
	 */
	public static void shapeAttacks(EntityDragonBase dragon) {
		if (!hasRiders(dragon) || dragon.groundAttack == null || dragon.airAttack == null) {
			return;
		}
		int kind = dragon.getVirtualRider();
		if (kind != NONE) {
			boolean bidden = kind == KNIGHT && dragon.getRandom().nextInt(3) == 0 || kind == WARLORD && dragon.getRandom().nextInt(4) == 0;
			if (!bidden) {
				if (dragon.groundAttack == IafDragonAttacks.Ground.FIRE) {
					dragon.groundAttack = IafDragonAttacks.Ground.BITE;
				}
				dragon.airAttack = IafDragonAttacks.Air.TACKLE;
			}
		} else if (dragon.berserk) {
			dragon.groundAttack = dragon.getRandom().nextInt(4) == 0 ? IafDragonAttacks.Ground.WING_BLAST : IafDragonAttacks.Ground.FIRE;
			dragon.airAttack = dragon.getRandom().nextBoolean() ? IafDragonAttacks.Air.SCORCH_STREAM : IafDragonAttacks.Air.HOVER_BLAST;
		}
	}

	private static void announce(ServerLevel level, Entity at, String key) {
		for (ServerPlayer player : level.players()) {
			if (player.distanceToSqr(at) < 40 * 40) {
				player.sendOverlayMessage(Component.translatable(key).withStyle(ChatFormatting.GOLD));
			}
		}
	}

	/** Whether this one is the warlord riding an elite (the frost is his). */
	public static boolean warlordRides(EntityDragonBase dragon) {
		return dragon.eliteMount && dragon.getVirtualRider() == WARLORD;
	}

}

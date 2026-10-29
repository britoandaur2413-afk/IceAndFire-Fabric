package com.iafabric.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.UUID;

/**
 * How a wild dragon fights where it lives, so a dragon in a cave or a hall does not end up pushing against the
 * rock: a player who hurt it stays its target, under a low roof it fights on its feet (in a hall as high as a dragon's
 * it flies, to places in the hall), it climbs a ledge with a beat of its wings, gets out of blocks it was pushed into,
 * goes up and over (or backs off) when its way is blocked, and a flight that makes no way is given another way or
 * ends on the ground.
 */
public final class DragonSmarts {
	/** How long a player who hurt a dragon stays its target (ticks). */
	private static final int REVENGE_TICKS = 1200;

	private DragonSmarts() {
	}

	/** Rock or a building above its head (leaves do not count): a cave, a hall. */
	public static boolean isUnderRoof(EntityDragonBase dragon) {
		int top = dragon.level().getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, dragon.getBlockX(), dragon.getBlockZ());
		return top > dragon.getBoundingBox().maxY + 1;
	}

	/**
	 * Room to fly: out in the open, or under a roof as high and wide as a dragon's hall (its whole body well above
	 * it and round it, not just a cave's dome).
	 */
	public static boolean roomToFly(EntityDragonBase dragon) {
		if (!isUnderRoof(dragon)) {
			return true;
		}
		AABB box = dragon.getBoundingBox();
		double up = dragon.getBbHeight() * 1.5 + 6;
		return dragon.level().noCollision(dragon, box.move(0, up, 0)) && dragon.level().noCollision(dragon, box.move(0, up / 2, 0).inflate(6, 0, 6));
	}

	/** Air blocks above a place, up to max (how high a dragon may hover there under a roof). */
	public static int headroom(net.minecraft.world.level.Level level, BlockPos from, int max) {
		BlockPos.MutableBlockPos pos = from.mutable();
		for (int up = 1; up <= max; up++) {
			pos.setY(from.getY() + up);
			if (!level.getBlockState(pos).getCollisionShape(level, pos).isEmpty()) {
				return up - 1;
			}
		}
		return max;
	}

	/** A place to fly to inside a hall or a cave: where the whole dragon fits, in its sight (null when there is none). */
	public static BlockPos caveFlightTarget(EntityDragonBase dragon) {
		var random = dragon.getRandom();
		AABB box = dragon.getBoundingBox();
		for (int i = 0; i < 24; i++) {
			double angle = random.nextDouble() * Math.PI * 2;
			double distance = 8 + random.nextDouble() * 22;
			double dy = random.nextDouble() * 12 - 4;
			Vec3 way = new Vec3(Math.cos(angle) * distance, dy, Math.sin(angle) * distance);
			Vec3 to = dragon.position().add(way);
			if (dragon.level().noCollision(dragon, box.move(way).inflate(1)) && !dragon.isTargetBlocked(to.add(0, dragon.getBbHeight() / 2, 0))) {
				return BlockPos.containing(to);
			}
		}
		return null;
	}

	/** Every tick on the server, for a dragon nobody rides. */
	public static void tick(EntityDragonBase dragon) {
		if (dragon.isTame() || dragon.isModelDead() || dragon.getControllingPassenger() != null || !(dragon.level() instanceof ServerLevel level)) {
			return;
		}
		keepAttacker(dragon, level);
		com.iafabric.event.DragonSkies.tickDragon(dragon, level);
		PillagerDragons.tick(dragon, level);
		DreadDragons.tick(dragon, level);
		DragonRiders.tick(dragon, level);
		DragonSpecials.tick(dragon, level);
		if (dragon.tickCount % 20 == 0) {
			unstick(dragon);
		}
		climb(dragon);
		walkStuck(dragon);
		flightStuck(dragon);
	}

	/**
	 * On its feet, going somewhere, and not getting there for three seconds (a wall of a fortress, a corner of a
	 * cave): where there is room above it beats its wings up and over (a short flight to its foe or its way), under a
	 * roof it backs off a few blocks and tries again from there.
	 */
	private static void walkStuck(EntityDragonBase dragon) {
		LivingEntity target = dragon.getTarget();
		boolean going = target != null && dragon.distanceToSqr(target) > Math.pow(dragon.getBbWidth() + 3, 2) || !dragon.getNavigation().isDone();
		if (!dragon.onGround() || dragon.isFlying() || dragon.isHovering() || !going || dragon.isOrderedToSit() || dragon.isSleeping() || !dragon.canMove()) {
			dragon.walkStuckTicks = 0;
			dragon.walkStuckFrom = null;
			return;
		}
		if (dragon.walkStuckFrom == null || dragon.position().distanceToSqr(dragon.walkStuckFrom) > 2.25) {
			dragon.walkStuckFrom = dragon.position();
			dragon.walkStuckTicks = 0;
			return;
		}
		if (++dragon.walkStuckTicks < 60) {
			return;
		}
		dragon.walkStuckTicks = 0;
		dragon.walkStuckFrom = null;
		Vec3 to = target != null ? target.position() : dragon.getNavigation().getTargetPos() != null ? Vec3.atBottomCenterOf(dragon.getNavigation().getTargetPos()) : null;
		if (!isUnderRoof(dragon) && dragon.level().noCollision(dragon, dragon.getBoundingBox().move(0, 5, 0)) && !dragon.isBaby() && PillagerDragons.mayTakeOff(dragon)) {
			// up and over
			dragon.setHovering(true);
			dragon.setDeltaMovement(dragon.getDeltaMovement().add(0, 0.6, 0));
			dragon.hurtMarked = true;
			if (to != null) {
				dragon.flightManager.setFlightTarget(to.add(0, dragon.getBbHeight() + 2, 0));
			}
			dragon.playSound(SoundEvents.ENDER_DRAGON_FLAP, 1.5F, 0.7F);
			return;
		}
		// back off: a free place a few blocks away from where it pushed, then on again
		Vec3 away = to != null ? dragon.position().subtract(to).multiply(1, 0, 1).normalize() : Vec3.directionFromRotation(0, dragon.getYRot() + 180);
		for (int tries = 0; tries < 6; tries++) {
			double angle = Math.toRadians((dragon.getRandom().nextFloat() - 0.5F) * 120F);
			double x = away.x * Math.cos(angle) - away.z * Math.sin(angle);
			double z = away.x * Math.sin(angle) + away.z * Math.cos(angle);
			Vec3 spot = dragon.position().add(x * 7, 0, z * 7);
			if (dragon.getNavigation().moveTo(spot.x, spot.y, spot.z, 1.0)) {
				return;
			}
		}
	}

	/**
	 * Flying into a hill, a tree or a wall and making no way: after a second and a half it is given another way (up
	 * when there is room, else back the way it came); after three seconds while fighting, or six without a fight, it
	 * lands and goes on on its feet.
	 */
	private static void flightStuck(EntityDragonBase dragon) {
		if (!dragon.isFlying() && !dragon.isHovering() || dragon.isModelDead()) {
			dragon.flightStuckTicks = 0;
			return;
		}
		Vec3 motion = dragon.getDeltaMovement();
		boolean blocked = dragon.horizontalCollision || dragon.verticalCollision && !dragon.onGround() && motion.y >= 0
			|| dragon.isFlying() && !dragon.isHovering() && motion.horizontalDistanceSqr() < 0.0016;
		if (!blocked) {
			dragon.flightStuckTicks = Math.max(0, dragon.flightStuckTicks - 2);
			return;
		}
		dragon.flightStuckTicks++;
		if (dragon.flightStuckTicks == 30 || dragon.flightStuckTicks == 80) {
			AABB box = dragon.getBoundingBox();
			Vec3 way;
			if (dragon.level().noCollision(dragon, box.move(0, 6, 0))) {
				way = dragon.position().add(0, dragon.getBbHeight() + 8, 0);
			} else {
				way = dragon.position().subtract(Vec3.directionFromRotation(0, dragon.getYRot()).scale(14)).add(0, 3, 0);
			}
			dragon.flightManager.setFlightTarget(way);
			dragon.setDeltaMovement(motion.x * -0.5, 0.35, motion.z * -0.5);
			dragon.hurtMarked = true;
		}
		if (dragon.getTarget() != null && dragon.flightStuckTicks > 60 || dragon.flightStuckTicks > 120) {
			dragon.setFlying(false);
			dragon.setHovering(false);
			dragon.flightStuckTicks = 0;
		}
	}

	/** The player who hurt it is its target while the fight lasts, before any other creature it would chase. */
	private static void keepAttacker(EntityDragonBase dragon, ServerLevel level) {
		if (dragon.getLastHurtByMob() instanceof Player hurt && !hurt.isCreative() && !hurt.isSpectator()) {
			dragon.revengePlayer = hurt.getUUID();
			dragon.revengeUntil = level.getGameTime() + REVENGE_TICKS;
		}
		UUID id = dragon.revengePlayer;
		if (id == null) {
			return;
		}
		Player player = level.getPlayerByUUID(id);
		if (player == null || !player.isAlive() || player.isCreative() || player.isSpectator() || level.getGameTime() > dragon.revengeUntil
			|| dragon.distanceToSqr(player) > 64 * 64) {
			dragon.revengePlayer = null;
			return;
		}
		if (dragon.getTarget() != player) {
			dragon.setTarget(player);
		}
	}

	/**
	 * A ledge in its way: a beat of the wings throws it up and forward (as high as the ledge, up to six blocks), a
	 * short flight instead of taking off.
	 */
	private static void climb(EntityDragonBase dragon) {
		if (dragon.hopCooldown > 0) {
			dragon.hopCooldown--;
			return;
		}
		if (!dragon.onGround() || !dragon.horizontalCollision || dragon.isFlying() || dragon.isHovering() || dragon.isSleeping()
			|| !dragon.canMove() || dragon.isBaby()) {
			return;
		}
		LivingEntity target = dragon.getTarget();
		boolean going = target != null || !dragon.getNavigation().isDone();
		if (!going) {
			return;
		}
		Vec3 ahead = target != null ? target.position().subtract(dragon.position()) : Vec3.directionFromRotation(0, dragon.getYRot());
		Vec3 flat = new Vec3(ahead.x, 0, ahead.z).normalize();
		int ledge = ledgeHeight(dragon, flat);
		if (ledge <= 0) {
			return;
		}
		// v^2 = 2 g h (gravity 0.08 a tick, a little more for the drag)
		double up = Math.sqrt(2 * 0.08 * (ledge + 1)) * 1.15;
		dragon.setDeltaMovement(flat.x * 0.45, up, flat.z * 0.45);
		dragon.hurtMarked = true;
		dragon.hopCooldown = 30;
		dragon.playSound(SoundEvents.ENDER_DRAGON_FLAP, 1.5F, 0.6F + dragon.getRandom().nextFloat() * 0.2F);
	}

	/** How many blocks higher the ground in front is (0 when there is no ledge, or it is more than six). */
	private static int ledgeHeight(EntityDragonBase dragon, Vec3 flat) {
		double reach = dragon.getBbWidth() / 2 + 1.0;
		BlockPos front = BlockPos.containing(dragon.getX() + flat.x * reach, dragon.getY(), dragon.getZ() + flat.z * reach);
		for (int h = 1; h <= 6; h++) {
			BlockPos above = front.above(h);
			if (dragon.level().getBlockState(above).getCollisionShape(dragon.level(), above).isEmpty()
				&& dragon.level().getBlockState(above.above()).getCollisionShape(dragon.level(), above.above()).isEmpty()) {
				return h;
			}
		}
		return 0;
	}

	/**
	 * Inside blocks for three seconds (a big dragon in a narrow place, pushed by a fight): it moves to the nearest
	 * free place, up to four blocks away, instead of pushing against the rock.
	 */
	private static void unstick(EntityDragonBase dragon) {
		AABB box = dragon.getBoundingBox().deflate(0.2);
		if (dragon.level().noCollision(dragon, box)) {
			dragon.stuckInBlocksTicks = 0;
			return;
		}
		dragon.stuckInBlocksTicks += 20;
		if (dragon.stuckInBlocksTicks < 60) {
			return;
		}
		for (int dy = 0; dy <= 4; dy++) {
			for (int r = 0; r <= 4; r++) {
				for (int dx = -r; dx <= r; dx++) {
					for (int dz = -r; dz <= r; dz++) {
						if (Math.max(Math.abs(dx), Math.abs(dz)) != r) {
							continue;
						}
						if (dragon.level().noCollision(dragon, box.move(dx, dy, dz))) {
							dragon.setPos(dragon.getX() + dx, dragon.getY() + dy, dragon.getZ() + dz);
							dragon.stuckInBlocksTicks = 0;
							return;
						}
					}
				}
			}
		}
	}
}

package com.iafabric.pixie;

import com.iafabric.entity.EntityDragonBase;
import com.iafabric.entity.EntityPixie;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

/**
 * The bonded pixie out with its player helps by itself, slowly, in the one way of its color: a punch of light (pink),
 * a lift into the air (purple), a frost bolt (blue), mending its player (green), a spark that jumps on (yellow). It
 * goes for what its player hits, what hurts its player or a monster after its player. What it kills counts as its
 * player's (the pixie grows with it). Its spells are its player's to call (PixieMagic).
 */
public final class PixieCombat {
	/** How far the pixie reaches. */
	static final double REACH = 20;
	/** How long what its player hit or what hurt its player stays the foe (ticks). */
	private static final int MEMORY = 200;

	private PixieCombat() {
	}

	/** Every five ticks for a player with a pixie out. */
	static void tick(ServerPlayer player, EntityPixie pixie, PixieBonds.Bond bond) {
		ServerLevel level = (ServerLevel) player.level();
		long now = level.getGameTime();
		if (pixie.level() != level) {
			return;
		}
		PixieAbility help = PixieAbility.supportOf(pixie.getColor());
		if (bond.ready[help.ordinal()] > now) {
			return;
		}
		if (help == PixieAbility.MEND) {
			if (player.getHealth() < player.getMaxHealth() && pixie.distanceTo(player) <= REACH) {
				mend(level, player, pixie, bond);
				bond.ready[help.ordinal()] = now + help.cooldown;
			}
			return;
		}
		LivingEntity foe = foe(level, player, pixie);
		if (foe == null) {
			return;
		}
		pixie.getLookControl().setLookAt(foe, 30F, 30F);
		help(level, player, pixie, bond, help, foe);
		bond.ready[help.ordinal()] = now + help.cooldown;
	}

	/** What the pixie fights: what its player hit, what hurt its player, or a monster after its player. */
	public static LivingEntity foe(ServerLevel level, ServerPlayer player, EntityPixie pixie) {
		LivingEntity hit = player.getLastHurtMob();
		if (hit != null && player.tickCount - player.getLastHurtMobTimestamp() < MEMORY && fair(player, pixie, hit)) {
			return hit;
		}
		LivingEntity by = player.getLastHurtByMob();
		if (by != null && player.tickCount - player.getLastHurtByMobTimestamp() < MEMORY && fair(player, pixie, by)) {
			return by;
		}
		LivingEntity nearest = null;
		for (Mob mob : level.getEntitiesOfClass(Mob.class, player.getBoundingBox().inflate(14), m -> m.getTarget() == player)) {
			if (fair(player, pixie, mob) && (nearest == null || mob.distanceToSqr(player) < nearest.distanceToSqr(player))) {
				nearest = mob;
			}
		}
		return nearest;
	}

	/** Alive, near, seen, and not the player's own (a pet, a bonded pixie). */
	static boolean fair(ServerPlayer player, EntityPixie pixie, LivingEntity entity) {
		// canBeSeenAsEnemy: alive and not a dead dragon's body (that one stays "alive" until it is harvested)
		if (!entity.canBeSeenAsEnemy() || entity == player || entity == pixie || entity.distanceToSqr(pixie) > REACH * REACH) {
			return false;
		}
		if (entity instanceof OwnableEntity owned && player.getUUID().equals(owned.getOwnerUUID())) {
			return false;
		}
		if (entity instanceof Player other && (other.isCreative() || other.isSpectator())) {
			return false;
		}
		return pixie.hasLineOfSight(entity);
	}

	/** A foe for the spells that strike all round: a monster, one after the player, or the one the player fights. */
	static boolean hostile(ServerPlayer player, EntityPixie pixie, LivingEntity entity) {
		if (entity == player || entity == pixie || !entity.canBeSeenAsEnemy() || entity instanceof Player) {
			return false;
		}
		if (entity instanceof OwnableEntity owned && player.getUUID().equals(owned.getOwnerUUID())) {
			return false;
		}
		return entity instanceof Enemy || entity instanceof Mob mob && mob.getTarget() == player || entity == player.getLastHurtMob()
			|| entity == player.getLastHurtByMob();
	}

	/** Not bosses or great beasts: they are too heavy to lift. */
	static boolean liftable(LivingEntity foe) {
		return foe.getMaxHealth() <= 100 && !(foe instanceof EntityDragonBase) && !foe.hasEffect(MobEffects.LEVITATION);
	}

	private static void help(ServerLevel level, ServerPlayer player, EntityPixie pixie, PixieBonds.Bond bond, PixieAbility help, LivingEntity foe) {
		DamageSource source = level.damageSources().indirectMagic(pixie, player);
		int lv = bond.level;
		switch (help) {
			case STRIKE -> {
				beam(level, pixie, foe, new DustParticleOptions(net.minecraft.world.phys.Vec3.fromRGB24(color(pixie)).toVector3f(), 1.3F), ParticleTypes.CRIT);
				sound(level, pixie, SoundEvents.PLAYER_ATTACK_STRONG, 1.6F);
				foe.hurt(source, 2F + lv * 0.8F);
				Vec3 away = foe.position().subtract(pixie.position()).multiply(1, 0, 1).normalize();
				foe.push(away.x * 0.5, 0.15, away.z * 0.5);
				foe.hurtMarked = true;
			}
			case LIFT -> {
				beam(level, pixie, foe, ParticleTypes.END_ROD, new DustParticleOptions(net.minecraft.world.phys.Vec3.fromRGB24(color(pixie)).toVector3f(), 1.0F));
				sound(level, pixie, SoundEvents.EVOKER_CAST_SPELL, 1.8F);
				if (liftable(foe)) {
					foe.addEffect(new MobEffectInstance(MobEffects.LEVITATION, 20 + lv * 4, 1), player);
					foe.hurt(source, 1F);
				} else {
					foe.hurt(source, 2F + lv * 0.5F);
				}
			}
			case FROST -> {
				beam(level, pixie, foe, ParticleTypes.SNOWFLAKE, ParticleTypes.ITEM_SNOWBALL);
				sound(level, pixie, SoundEvents.GLASS_BREAK, 1.8F);
				foe.hurt(source, 1.5F + lv * 0.6F);
				foe.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, 0), player);
				foe.setTicksFrozen(Math.max(foe.getTicksFrozen(), foe.getTicksRequiredToFreeze() + 30));
			}
			case SPARK -> {
				beam(level, pixie, foe, ParticleTypes.ELECTRIC_SPARK, ParticleTypes.ELECTRIC_SPARK);
				sound(level, pixie, SoundEvents.LIGHTNING_BOLT_IMPACT, 1.7F);
				foe.hurt(source, 2F + lv * 0.6F);
				for (LivingEntity next : level.getEntitiesOfClass(LivingEntity.class, foe.getBoundingBox().inflate(5),
					e -> e != foe && hostile(player, pixie, e) && fair(player, pixie, e))) {
					beam(level, foe, next, ParticleTypes.ELECTRIC_SPARK, ParticleTypes.ELECTRIC_SPARK);
					next.hurt(source, (2F + lv * 0.6F) * 0.6F);
					break;
				}
			}
			default -> {
			}
		}
	}

	private static void mend(ServerLevel level, ServerPlayer player, EntityPixie pixie, PixieBonds.Bond bond) {
		beam(level, pixie, player, ParticleTypes.HAPPY_VILLAGER, new DustParticleOptions(net.minecraft.world.phys.Vec3.fromRGB24(color(pixie)).toVector3f(), 1.0F));
		player.heal(1F + bond.level * 0.5F);
		level.sendParticles(ParticleTypes.HEART, player.getX(), player.getY() + 1.2, player.getZ(), 2, 0.4, 0.3, 0.4, 0.02);
		sound(level, pixie, SoundEvents.ALLAY_ITEM_GIVEN, 1.3F);
	}

	static int color(EntityPixie pixie) {
		float[] rgb = EntityPixie.PARTICLE_RGB[pixie.getColor()];
		return (int) (rgb[0] * 255) << 16 | (int) (rgb[1] * 255) << 8 | (int) (rgb[2] * 255);
	}

	static void sound(ServerLevel level, EntityPixie pixie, SoundEvent sound, float pitch) {
		level.playSound(null, pixie.getX(), pixie.getY(), pixie.getZ(), sound, SoundSource.NEUTRAL, 0.6F, pitch);
	}

	/** A line of light from one to the other, so a player sees what the pixie does. */
	static void beam(ServerLevel level, LivingEntity from, LivingEntity to, ParticleOptions main, ParticleOptions spark) {
		Vec3 a = from.position().add(0, from.getBbHeight() * 0.5, 0);
		Vec3 b = to.position().add(0, to.getBbHeight() * 0.5, 0);
		double length = a.distanceTo(b);
		int steps = Math.max(2, (int) (length * 3));
		for (int i = 0; i <= steps; i++) {
			Vec3 p = a.lerp(b, i / (double) steps);
			level.sendParticles(i % 3 == 0 ? spark : main, p.x, p.y, p.z, 1, 0.02, 0.02, 0.02, 0.0);
		}
		level.sendParticles(spark, b.x, b.y, b.z, 8, 0.25, 0.25, 0.25, 0.02);
	}
}

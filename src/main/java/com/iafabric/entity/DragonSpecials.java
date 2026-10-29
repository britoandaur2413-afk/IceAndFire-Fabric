package com.iafabric.entity;

import com.iafabric.entity.util.DragonUtils;
import com.iafabric.misc.IafDamageRegistry;
import com.iafabric.vfx.Vfx;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

/**
 * The great breath of the old dragons (stage 4 and 5, wild): now and then, in a fight, besides their breath, each
 * kind calls down its element where its foe stands, shown first on the ground so it can be dodged: fire falls from
 * above into warning circles (a rain of fire), ice spikes burst from a magic circle of frost (and freeze who is
 * caught), lightning strikes into warning circles (a storm). Then a wait before the next.
 */
public final class DragonSpecials {
	/** Warning time (ticks) before it falls. */
	private static final int WARN = 30;

	private DragonSpecials() {
	}

	/** Every tick on the server for a wild dragon nobody rides (from DragonSmarts). */
	public static void tick(EntityDragonBase dragon, ServerLevel level) {
		if (dragon.getDragonStage() < 4 || dragon.isTame() || dragon.isBlackFrost() || dragon.isSleeping() || dragon.isModelDead()) {
			return;
		}
		if (DragonRiders.hasRiders(dragon) && dragon.getVirtualRider() != DragonRiders.NONE) {
			// led by its rider (a kingdom's, the dread's): it does not call these
			return;
		}
		if (dragon.specialCooldown > 0) {
			dragon.specialCooldown--;
			return;
		}
		LivingEntity target = dragon.getTarget();
		if (target == null || !target.isAlive() || dragon.distanceToSqr(target) > 32 * 32 || !dragon.hasLineOfSight(target)
			|| target instanceof Player player && (player.isCreative() || player.isSpectator())) {
			dragon.specialCooldown = 20;
			return;
		}
		RandomSource random = dragon.getRandom();
		dragon.specialCooldown = 220 + random.nextInt(140);
		dragon.roar();
		if (dragon.dragonType == DragonType.ICE) {
			spikes(dragon, level, target);
		} else if (dragon.dragonType == DragonType.LIGHTNING) {
			storm(dragon, level, target);
		} else {
			fireRain(dragon, level, target);
		}
	}

	/** Where the strikes go: one where the foe stands, the others round it. */
	private static List<Vec3> spots(RandomSource random, Vec3 at, int count, double spread) {
		List<Vec3> spots = new ArrayList<>();
		spots.add(at);
		for (int i = 1; i < count; i++) {
			double angle = Math.PI * 2 * i / (count - 1) + random.nextDouble() * 0.6;
			double distance = spread * (0.5 + random.nextDouble() * 0.5);
			spots.add(at.add(Math.cos(angle) * distance, 0, Math.sin(angle) * distance));
		}
		return spots;
	}

	private static float power(EntityDragonBase dragon) {
		return 4F + dragon.getDragonStage() * 2.5F;
	}

	/** Hurts who is in a circle (not the dragon's side) and does more to them. */
	private static void strike(EntityDragonBase dragon, ServerLevel level, Vec3 at, double radius, DamageSource source, float damage,
		java.util.function.Consumer<LivingEntity> more) {
		AABB box = new AABB(at, at).inflate(radius, 3, radius);
		for (LivingEntity entity : level.getEntitiesOfClass(LivingEntity.class, box, e -> e.isAlive() && e != dragon && !DragonUtils.onSameTeam(dragon, e))) {
			double dx = entity.getX() - at.x;
			double dz = entity.getZ() - at.z;
			if (dx * dx + dz * dz <= radius * radius) {
				entity.hurt(source, damage);
				more.accept(entity);
			}
		}
	}

	/** Fire falls into the circles: a burning rock from the sky, a blast, fire on who is caught. */
	private static void fireRain(EntityDragonBase dragon, ServerLevel level, LivingEntity target) {
		RandomSource random = dragon.getRandom();
		Vfx.rune(level, Vfx.ground(level, dragon.position()), dragon.getBbWidth() * 0.9F, com.iafabric.entity.EntityVfx.FLAME, Vfx.FIRE, WARN + 10, 3F, 0F);
		for (Vec3 spot : spots(random, target.position(), 5, 7)) {
			Vec3 ground = Vfx.ground(level, spot);
			Vfx.circle(level, ground, 3F, Vfx.FIRE, WARN);
			// the burning rock drops from high up and lands as the circle fills
			var rock = Vfx.shape(level, ground.add(0, 22, 0), Blocks.MAGMA_BLOCK.defaultBlockState(), 1.4F, 1.4F, random.nextFloat() * 40, random.nextFloat() * 40, true);
			if (rock != null) {
				Vfx.later(level, WARN - 10, () -> Vfx.move(rock, ground, 10));
			}
			Vfx.later(level, WARN, () -> {
				Vfx.remove(rock);
				level.sendParticles(ParticleTypes.EXPLOSION, ground.x, ground.y + 0.5, ground.z, 2, 0.5, 0.2, 0.5, 0);
				level.sendParticles(ParticleTypes.FLAME, ground.x, ground.y + 0.3, ground.z, 40, 1.6, 0.4, 1.6, 0.05);
				level.sendParticles(ParticleTypes.LAVA, ground.x, ground.y + 0.3, ground.z, 10, 1.2, 0.2, 1.2, 0);
				level.playSound(null, ground.x, ground.y, ground.z, SoundEvents.GENERIC_EXPLODE.value(), SoundSource.HOSTILE, 1.4F, 0.8F + random.nextFloat() * 0.3F);
				Vfx.wave(level, ground, 4F, Vfx.FIRE, 12);
				strike(dragon, level, ground, 3.2, IafDamageRegistry.causeDragonFireDamage(dragon), power(dragon), e -> e.igniteForSeconds(5));
			});
		}
	}

	/** Ice spikes burst from a frost circle round the foe; who is caught is frozen. */
	private static void spikes(EntityDragonBase dragon, ServerLevel level, LivingEntity target) {
		RandomSource random = dragon.getRandom();
		Vec3 center = Vfx.ground(level, target.position());
		float radius = 4.5F;
		Vfx.rune(level, center, radius + 0.5F, com.iafabric.entity.EntityVfx.FROST, Vfx.ICE, WARN + 40, 2.5F, 0F);
		Vfx.circle(level, center, radius, Vfx.ICE, WARN);
		level.playSound(null, center.x, center.y, center.z, SoundEvents.EVOKER_PREPARE_ATTACK, SoundSource.HOSTILE, 1.5F, 1.4F);
		Vfx.later(level, WARN, () -> {
			for (int i = 0; i < 14; i++) {
				double angle = random.nextDouble() * Math.PI * 2;
				double distance = Math.sqrt(random.nextDouble()) * radius;
				Vec3 at = center.add(Math.cos(angle) * distance, 0, Math.sin(angle) * distance);
				float lean = (float) (distance / radius * 25);
				Vfx.spike(level, at, (random.nextInt(3) == 0 ? Blocks.BLUE_ICE : Blocks.PACKED_ICE).defaultBlockState(), 0.6F + random.nextFloat() * 0.4F,
					2F + random.nextFloat() * 2.5F, (float) Math.sin(angle) * lean, (float) -Math.cos(angle) * lean, 4, 70);
			}
			level.sendParticles(ParticleTypes.SNOWFLAKE, center.x, center.y + 1, center.z, 60, radius / 2, 1, radius / 2, 0.08);
			level.playSound(null, center.x, center.y, center.z, SoundEvents.GLASS_BREAK, SoundSource.HOSTILE, 2F, 0.6F);
			Vfx.wave(level, center, radius + 2, Vfx.ICE, 14);
			strike(dragon, level, center, radius, IafDamageRegistry.causeDragonIceDamage(dragon), power(dragon), e -> com.iafabric.boss.BossCold.freeze(level, e, 30));
		});
	}

	/** Lightning strikes into the circles round the foe and throws who is caught. */
	private static void storm(EntityDragonBase dragon, ServerLevel level, LivingEntity target) {
		RandomSource random = dragon.getRandom();
		Vfx.rune(level, Vfx.ground(level, dragon.position()), dragon.getBbWidth() * 0.9F, com.iafabric.entity.EntityVfx.STORM, Vfx.STORM, WARN + 10, -4F, 0F);
		List<Vec3> spots = spots(random, target.position(), 5, 7);
		for (int i = 0; i < spots.size(); i++) {
			Vec3 ground = Vfx.ground(level, spots.get(i));
			int delay = WARN + i * 4;
			Vfx.circle(level, ground, 2.6F, Vfx.STORM, delay);
			Vfx.later(level, delay, () -> {
				LightningBolt bolt = EntityType.LIGHTNING_BOLT.create(level);
				if (bolt != null) {
					bolt.moveTo(ground.x, ground.y, ground.z);
					bolt.setVisualOnly(true);
					level.addFreshEntity(bolt);
				}
				level.sendParticles(ParticleTypes.ELECTRIC_SPARK, ground.x, ground.y + 0.5, ground.z, 30, 1.2, 0.6, 1.2, 0.2);
				strike(dragon, level, ground, 2.8, IafDamageRegistry.causeDragonLightningDamage(dragon), power(dragon), e -> {
					Vec3 away = e.position().subtract(ground).multiply(1, 0, 1).normalize();
					e.push(away.x * 0.9, 0.4, away.z * 0.9);
					e.hurtMarked = true;
				});
			});
		}
	}
}

package com.iafabric.entity;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.raid.Raider;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * The frost of a kingdom's elite dragon, used by its rider, the warlord, each in its turn a few seconds apart: a
 * blizzard on the foe (for five seconds it slows and freezes all but illagers there), an ice prison around the foe
 * (frosted ice that melts after four seconds) and, when the foe is close, a frost roar that throws back and slows all
 * near the dragon. Each is announced to the players near, so they see it coming.
 */
public final class PillagerFrost {
	private static final Map<UUID, long[]> READY = new HashMap<>();
	private static final List<Blizzard> BLIZZARDS = new ArrayList<>();
	private static final List<Prison> PRISONS = new ArrayList<>();

	private PillagerFrost() {
	}

	public static void register() {
		ServerTickEvents.END_WORLD_TICK.register(PillagerFrost::tickLevel);
	}

	/** While the elite fights (from PillagerDragons). */
	static void tick(EntityDragonBase dragon, LivingEntity target, ServerLevel level) {
		long now = level.getGameTime();
		long[] ready = READY.computeIfAbsent(dragon.getUUID(), k -> new long[]{now + 60, now + 140, now + 100, 0});
		if (now < ready[3] || !target.isAlive()) {
			return;
		}
		double distance = dragon.distanceTo(target);
		if (distance > 32 || !dragon.hasLineOfSight(target)) {
			return;
		}
		if (distance < 10 && ready[2] <= now) {
			roar(dragon, level);
			ready[2] = now + 200;
		} else if (ready[1] <= now) {
			prison(dragon, target, level);
			ready[1] = now + 320;
		} else if (ready[0] <= now) {
			blizzard(dragon, target, level);
			ready[0] = now + 260;
		} else {
			return;
		}
		ready[3] = now + 60;
		Vec3 seat = DragonRiders.seat(dragon);
		level.playSound(null, seat.x, seat.y + 1, seat.z, SoundEvents.PILLAGER_CELEBRATE, SoundSource.HOSTILE, 2F, 0.8F);
	}

	private static boolean spared(LivingEntity entity, EntityDragonBase dragon) {
		return entity == dragon || entity instanceof Raider || dragon.alliedWith(entity) || !entity.isAlive()
			|| entity instanceof net.minecraft.world.entity.player.Player player && (player.isCreative() || player.isSpectator());
	}

	private static void announce(ServerLevel level, LivingEntity at, String key) {
		for (ServerPlayer player : level.players()) {
			if (player.distanceToSqr(at) < 48 * 48) {
				player.displayClientMessage(Component.translatable(key).withStyle(ChatFormatting.AQUA), true);
			}
		}
	}

	private static void blizzard(EntityDragonBase dragon, LivingEntity target, ServerLevel level) {
		BLIZZARDS.add(new Blizzard(level, target.position(), level.getGameTime() + 100, dragon));
		level.playSound(null, target.getX(), target.getY(), target.getZ(), SoundEvents.EVOKER_PREPARE_ATTACK, SoundSource.HOSTILE, 2F, 0.6F);
		announce(level, target, "iceandfire_fabric.warlord.blizzard");
	}

	private static void prison(EntityDragonBase dragon, LivingEntity target, ServerLevel level) {
		BlockPos feet = target.blockPosition();
		List<BlockPos> placed = new ArrayList<>();
		for (int dy = 0; dy <= 2; dy++) {
			for (int dx = -1; dx <= 1; dx++) {
				for (int dz = -1; dz <= 1; dz++) {
					boolean wall = dx != 0 || dz != 0;
					if (!wall && dy < 2) {
						continue;
					}
					BlockPos pos = feet.offset(dx, dy, dz);
					if (level.getBlockState(pos).isAir()) {
						level.setBlock(pos, Blocks.FROSTED_ICE.defaultBlockState(), 3);
						placed.add(pos);
					}
				}
			}
		}
		PRISONS.add(new Prison(level, placed, level.getGameTime() + 80));
		target.hurt(level.damageSources().freeze(), 3F);
		target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 80, 3), dragon);
		level.sendParticles(ParticleTypes.SNOWFLAKE, target.getX(), target.getY() + 1, target.getZ(), 40, 0.8, 1, 0.8, 0.05);
		level.playSound(null, target.getX(), target.getY(), target.getZ(), SoundEvents.GLASS_PLACE, SoundSource.HOSTILE, 2F, 0.5F);
		announce(level, target, "iceandfire_fabric.warlord.prison");
	}

	private static void roar(EntityDragonBase dragon, ServerLevel level) {
		dragon.roar();
		level.playSound(null, dragon.getX(), dragon.getY(), dragon.getZ(), SoundEvents.ENDER_DRAGON_GROWL, SoundSource.HOSTILE, 2F, 1.4F);
		for (int i = 0; i < 36; i++) {
			double angle = Math.PI * 2 * i / 36;
			level.sendParticles(ParticleTypes.SNOWFLAKE, dragon.getX() + Math.cos(angle) * 4, dragon.getY() + 1, dragon.getZ() + Math.sin(angle) * 4, 3,
				Math.cos(angle) * 0.6, 0.1, Math.sin(angle) * 0.6, 0.2);
		}
		for (LivingEntity entity : level.getEntitiesOfClass(LivingEntity.class, dragon.getBoundingBox().inflate(10), e -> !spared(e, dragon))) {
			Vec3 away = entity.position().subtract(dragon.position()).multiply(1, 0, 1).normalize();
			entity.push(away.x * 1.5, 0.45, away.z * 1.5);
			entity.hurtMarked = true;
			entity.hurt(level.damageSources().freeze(), 4F);
			entity.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 2), dragon);
			entity.setTicksFrozen(Math.max(entity.getTicksFrozen(), entity.getTicksRequiredToFreeze() + 40));
		}
		announce(level, dragon, "iceandfire_fabric.warlord.roar");
	}

	private static void tickLevel(ServerLevel level) {
		long now = level.getGameTime();
		for (Iterator<Blizzard> it = BLIZZARDS.iterator(); it.hasNext(); ) {
			Blizzard b = it.next();
			if (b.level != level) {
				continue;
			}
			if (now > b.until) {
				it.remove();
				continue;
			}
			level.sendParticles(ParticleTypes.SNOWFLAKE, b.at.x, b.at.y + 2.5, b.at.z, 14, 4, 1.5, 4, 0.02);
			level.sendParticles(ParticleTypes.WHITE_ASH, b.at.x, b.at.y + 2, b.at.z, 10, 4, 1.5, 4, 0.0);
			if (now % 10 == 0) {
				for (LivingEntity entity : level.getEntitiesOfClass(LivingEntity.class, new AABB(b.at, b.at).inflate(5, 3, 5), e -> !spared(e, b.dragon))) {
					entity.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, 1));
					entity.setTicksFrozen(Math.min(entity.getTicksFrozen() + 30, entity.getTicksRequiredToFreeze() + 100));
					entity.hurt(level.damageSources().freeze(), 1.5F);
				}
			}
		}
		for (Iterator<Prison> it = PRISONS.iterator(); it.hasNext(); ) {
			Prison p = it.next();
			if (p.level != level || now < p.until) {
				continue;
			}
			for (BlockPos pos : p.blocks) {
				if (level.getBlockState(pos).is(Blocks.FROSTED_ICE)) {
					level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
					level.sendParticles(ParticleTypes.ITEM_SNOWBALL, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 3, 0.3, 0.3, 0.3, 0.02);
				}
			}
			it.remove();
		}
		if (now % 1200 == 0) {
			READY.keySet().removeIf(id -> level.getEntity(id) == null);
		}
	}

	private record Blizzard(ServerLevel level, Vec3 at, long until, EntityDragonBase dragon) {
	}

	private record Prison(ServerLevel level, List<BlockPos> blocks, long until) {
	}
}

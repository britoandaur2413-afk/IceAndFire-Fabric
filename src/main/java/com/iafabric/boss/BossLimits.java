package com.iafabric.boss;

import com.iafabric.IafMod;
import com.iafabric.entity.EntityDragonBase;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * What makes Cryona and Black Frost true bosses. Nothing kills them at a stroke: in any second (20 ticks) they lose at
 * most a hundredth of their life, whatever strikes them (only /kill and the void go through), so each lasts at least
 * a hundred seconds. Their blows, all shown before they fall, take a share of the life of who they strike through any
 * armor, enchantment, potion or shield (the cold of the Frozen Throne); the longest to call take nine tenths of it,
 * always (they kill who was hurt already; a totem of undying saves its bearer). Black Frost does not give way to any blow or push: it is a wall, and pushes back who
 * comes against it.
 */
public final class BossLimits {
	public static final ResourceKey<DamageType> FROST_DOOM = ResourceKey.create(Registries.DAMAGE_TYPE, IafMod.id("frost_doom"));
	/** The share of its life a boss may lose in a window, and the window (ticks). */
	public static final float SHARE = 0.01F;
	public static final int WINDOW = 20;
	/** The share of its life the longest calls take when not dodged. */
	public static final float HEAVY = 0.9F;
	private static final Map<UUID, Deque<long[]>> LOSSES = new HashMap<>();

	private BossLimits() {
	}

	/** Whether a blow is beyond the limit (an admin's /kill, the void). */
	public static boolean unlimited(DamageSource source) {
		return source.is(DamageTypes.GENERIC_KILL) || source.is(DamageTypes.FELL_OUT_OF_WORLD);
	}

	/**
	 * How much of a loss of life a boss may take now (and counts it): what is left of a hundredth of its life in the
	 * last second.
	 */
	public static float allow(LivingEntity boss, float loss, long now) {
		if (loss <= 0) {
			return loss;
		}
		Deque<long[]> losses = LOSSES.computeIfAbsent(boss.getUUID(), id -> new ArrayDeque<>());
		while (!losses.isEmpty() && now - losses.peekFirst()[0] >= WINDOW) {
			losses.pollFirst();
		}
		float taken = 0;
		for (long[] l : losses) {
			taken += Float.intBitsToFloat((int) l[1]);
		}
		float allowed = Math.max(0F, Math.min(loss, boss.getMaxHealth() * SHARE - taken));
		if (allowed > 0) {
			losses.addLast(new long[]{now, Float.floatToIntBits(allowed)});
		}
		if (LOSSES.size() > 64) {
			LOSSES.entrySet().removeIf(e -> e.getValue().isEmpty());
		}
		return allowed;
	}

	/** The cold of the Frozen Throne, from a boss. */
	public static DamageSource doomSource(ServerLevel level, Entity boss) {
		return new DamageSource(level.registryAccess().lookupOrThrow(Registries.DAMAGE_TYPE).getOrThrow(FROST_DOOM), boss, boss);
	}

	/**
	 * A blow that takes a share of who it strikes' life, through any armor, enchantment, potion or shield; Black
	 * Frost's goes straight to the heart of who lacks the pixie's gift (BossCold.bite).
	 */
	public static boolean strike(ServerLevel level, LivingEntity target, Entity boss, float share) {
		float damage = Math.max(1F, target.getMaxHealth() * share);
		if (BossCold.bites(target, boss)) {
			BossCold.bite(level, (net.minecraft.server.level.ServerPlayer) target, boss, damage);
			return true;
		}
		return target.hurt(doomSource(level, boss), damage);
	}

	/**
	 * As strike, and golden hearts do not take it either (Cryona's blows): it goes to the life under them, which stay
	 * as they were.
	 */
	public static boolean pierce(ServerLevel level, LivingEntity target, Entity boss, float share) {
		float absorption = target.getAbsorptionAmount();
		if (absorption <= 0F) {
			return strike(level, target, boss, share);
		}
		target.setAbsorptionAmount(0F);
		boolean hit = strike(level, target, boss, share);
		if (target.isAlive()) {
			target.setAbsorptionAmount(Math.max(target.getAbsorptionAmount(), absorption));
		}
		return hit;
	}

	/**
	 * The end of a long call not dodged: nine tenths of its life, always. Through any armor, enchantment, potion or
	 * shield, a moment after another blow too, and its golden hearts do not take it (they stay as they were). It kills
	 * only who was hurt already; a totem of undying in hand saves them.
	 */
	public static boolean doom(ServerLevel level, LivingEntity target, Entity boss) {
		return doom(level, target, boss, HEAVY);
	}

	/** As above, a share of their life (the exiled pixie's gift lightens Absolute Zero). */
	public static boolean doom(ServerLevel level, LivingEntity target, Entity boss, float share) {
		if (BossCold.bites(target, boss)) {
			BossCold.bite(level, (net.minecraft.server.level.ServerPlayer) target, boss, target.getMaxHealth() * share);
			return true;
		}
		float absorption = target.getAbsorptionAmount();
		float before = target.getHealth();
		float damage = target.getMaxHealth() * share;
		target.setAbsorptionAmount(0F);
		target.invulnerableTime = 0;
		// (and no wait after another blow: it lands whole, always)
		target.invulnerableTime = 0;
		boolean hit = target.hurt(doomSource(level, boss), damage);
		if (target.isAlive()) {
			target.setAbsorptionAmount(Math.max(target.getAbsorptionAmount(), absorption));
			if (hit && before <= damage && target instanceof ServerPlayer player) {
				player.displayClientMessage(net.minecraft.network.chat.Component.translatable("iceandfire_fabric.boss.totem_saved")
					.withStyle(net.minecraft.ChatFormatting.GOLD, net.minecraft.ChatFormatting.BOLD), true);
			}
		}
		return hit;
	}

	/** Black Frost pushes back who comes against its body (players, creatures; not its own, not its rider). */
	public static void pushAway(EntityDragonBase dragon, Entity part) {
		for (Entity entity : part.level().getEntities(part, part.getBoundingBox().inflate(0.3))) {
			if (entity == dragon || entity.isPassenger() && entity.getVehicle() == dragon || entity instanceof com.iafabric.entity.EntityMutlipartPart
				|| entity instanceof Player player && (player.isSpectator() || player.isCreative() && player.getAbilities().flying) || !entity.isPushable() && !(entity instanceof Player)) {
				continue;
			}
			Vec3 away = entity.position().subtract(part.position()).multiply(1, 0, 1);
			if (away.lengthSqr() < 0.0001) {
				away = entity.position().subtract(dragon.position()).multiply(1, 0, 1);
			}
			away = away.lengthSqr() < 0.0001 ? new Vec3(1, 0, 0) : away.normalize();
			if (entity instanceof Player player) {
				// (a player its claws close on, hold or threw goes its way; one against it slides off, down, never up)
				if (!BlackFrostFight.of(dragon).leavesBe(player)) {
					shove(entity, away.x * 0.35, -0.08, away.z * 0.35);
				}
			} else {
				shove(entity, away.x * 0.35, 0.02, away.z * 0.35);
			}
		}
	}

	/**
	 * Throws something along a way. A player's game gets the throw as it is: the server keeps no true speed for a
	 * player (only a fading copy of the last push), so a push added to it would pile up on the ones before; theirs is
	 * set. A creature's speed is the server's own: the throw is added to it. Either way it is sent (26.3 sends a speed
	 * to the one it belongs to only with syncVelocity; needsSync tells only the others).
	 */
	public static void shove(Entity entity, double x, double y, double z) {
		if (entity instanceof Player) {
			entity.setDeltaMovement(x, y, z);
		} else {
			entity.push(x, y, z);
		}
		entity.hurtMarked = true;
	}

	/** Whether this boss gives way to nothing (no knockback, not pushed). */
	public static boolean immovable(@Nullable Entity entity) {
		return entity instanceof EntityDragonBase dragon && dragon.isBlackFrost() && !dragon.isModelDead();
	}
}

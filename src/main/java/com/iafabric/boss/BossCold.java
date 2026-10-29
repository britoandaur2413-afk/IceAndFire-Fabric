package com.iafabric.boss;

import com.iafabric.effect.IafEffects;
import com.iafabric.entity.EntityDragonBase;
import com.iafabric.entity.props.FrozenProperties;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

/**
 * The cold of the bosses and the old ice dragons on a player, kept fair: a freeze is short, and after one the player
 * is free of ice for eight seconds (never ice upon ice), so the next warning on the ground can always be dodged. Who
 * drank a Frost Ward is not frozen nor slowed by it at all, and ice breath hurts them less; only Black Frost's Glacial
 * Prison and Cryona's Absolute Zero freeze through the ward. Black Frost's breath always freezes whoever it catches
 * (a moment free after each, to step out of it). Who holds their exiled pixie's gift, the resistance to absolute ice,
 * is held half as long by every freeze of theirs, recovers faster, and is only chilled by the breath's first touch.
 * Without that gift, Black Frost's ice (and Cryona's Absolute Zero) bites straight to the heart: no armor, enchantment,
 * potion, ward, shield, golden heart nor anything another mod adds holds it (only a totem of undying saves them from
 * the bite that would kill them), and it cauterizes: no life comes back to them for twenty seconds after it (no food,
 * potion, apple or anything else heals them). Its breath above all: it melts who lacks the gift; who holds it takes a
 * cold blow their armor and potions lessen, and is only chilled.
 */
public final class BossCold {
	/** Ticks free of ice after a freeze ends. */
	private static final int FREE = 160;
	/** The longest Black Frost's breath freezes (its fight is fair). */
	private static final int BLACK_FROST_BREATH = 30;
	/** After its breath froze them, a moment free of it (two seconds; four with the pixie's gift). */
	private static final int BREATH_FREE = 40;
	private static final int GIFT_BREATH_FREE = 80;
	/** With the pixie's gift, the breath's first touch only chills: caught again within this, it freezes. */
	private static final int CHILL_WINDOW = 30;
	private static final Map<UUID, Long> FREE_UNTIL = new HashMap<>();
	private static final Map<UUID, Long> CHILLED = new HashMap<>();
	/** How long its ice cauterizes (ticks after its last bite), and a breath's bite (a share of their life, once a half second). */
	public static final int WOUND_TICKS = 400;
	public static final float BREATH_BITE = 0.15F;
	private static final int BREATH_BITE_EVERY = 10;
	/** Who bears a wound of its ice (no life comes back to them), until when; and each one's last bite of the breath. */
	private static final Map<UUID, Long> WOUND = new HashMap<>();
	private static final Map<UUID, Long> BITTEN = new HashMap<>();
	/** Who their totem saves this moment (its heart back is the one life that comes back to the cauterized). */
	private static @Nullable UUID saving;
	/** When each one last flinched at a bite (its hurt sound and red flash once a half second, not at every bite). */
	private static final Map<UUID, Long> FLINCHED = new HashMap<>();
	private static final int FLINCH_EVERY = 10;
	/**
	 * Its breath's blow on who holds the pixie's gift, each time it catches them: a share of their life (armor does not
	 * hold this cold; enchantments, potions and a shield do), the super breath's more. All its lane alike.
	 */
	private static final float GIFT_BREATH = 0.08F;
	private static final float GIFT_GREAT_BREATH = 0.12F;
	public static final net.minecraft.resources.ResourceKey<net.minecraft.world.damagesource.DamageType> FROST_BREATH =
		net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.DAMAGE_TYPE, com.iafabric.IafMod.id("frost_breath"));
	/** How long past frozen its breath leaves who it caught frozen as the game shows it (frost on their sight, shaking). */
	private static final int FROSTED = 100;
	/** Its breath's bite on who lacks it: a share of their life each time it catches them (every few ticks). */
	private static final float BREATH_MELT = 0.10F;
	private static final float GREAT_BREATH_MELT = 0.14F;

	/** A freeze's length on them: half with their pixie's gift. */
	private static int held(LivingEntity entity, int ticks) {
		return com.iafabric.pixie.FairyVision.iceGift(entity) ? Math.max(1, ticks / 2) : ticks;
	}

	private BossCold() {
	}

	public static void register() {
		net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents.END_SERVER_TICK.register(server -> {
			if (server.getTickCount() % 20 != 0 || WOUND.isEmpty()) {
				return;
			}
			long now = server.overworld().getGameTime();
			for (Iterator<Map.Entry<UUID, Long>> it = WOUND.entrySet().iterator(); it.hasNext(); ) {
				Map.Entry<UUID, Long> entry = it.next();
				ServerPlayer player = server.getPlayerList().getPlayer(entry.getKey());
				if (player == null || now >= entry.getValue() || !player.isAlive()) {
					it.remove();
					continue;
				}
				if (!player.hasEffect(IafEffects.FROST_WOUND)) {
					// (milk takes the effect, not the wound: it shows again)
					player.addEffect(new MobEffectInstance(IafEffects.FROST_WOUND, (int) (entry.getValue() - now), 0, false, true, true));
				}
			}
		});
		net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents.AFTER_RESPAWN.register((old, player, alive) -> {
			WOUND.remove(player.getUUID());
			BITTEN.remove(player.getUUID());
			FLINCHED.remove(player.getUUID());
		});
	}

	/** For tests and game masters: no wound of its ice left on them (life comes back to them again). */
	public static void cure(LivingEntity entity) {
		WOUND.remove(entity.getUUID());
		BITTEN.remove(entity.getUUID());
		FREE_UNTIL.remove(entity.getUUID());
		CHILLED.remove(entity.getUUID());
		entity.removeEffect(IafEffects.FROST_WOUND);
	}

	/**
	 * Black Frost's breath on who stands in its jet (once every few ticks while they do). Without the pixie's gift it
	 * melts them: straight to the heart (a tenth of their life each time, more from its super breath), cauterized, held
	 * in its ice. With the gift it is a cold blow like any other (armor, enchantments and potions lessen it; the Frost
	 * Ward too) and it only chills them, holding them a moment if it catches them again.
	 */
	public static void breathOf(ServerLevel level, LivingEntity entity, EntityDragonBase dragon, boolean great) {
		if (!IafEffects.warded(entity)) {
			// frozen as the game shows it: frost on their sight, their body shaking
			entity.setTicksFrozen(Math.max(entity.getTicksFrozen(), entity.getTicksRequiredToFreeze() + FROSTED));
		}
		if (bites(entity, dragon)) {
			bite(level, (ServerPlayer) entity, dragon, entity.getMaxHealth() * (great ? GREAT_BREATH_MELT : BREATH_MELT));
			blackFrostBreath(level, entity, dragon, BLACK_FROST_BREATH / 2);
			return;
		}
		float amount = entity.getMaxHealth() * (great ? GIFT_GREAT_BREATH : GIFT_BREATH);
		if (IafEffects.warded(entity)) {
			amount *= IafEffects.WARDED_BREATH;
			warded(level, entity);
		}
		entity.hurt(new DamageSource(level.registryAccess().lookupOrThrow(net.minecraft.core.registries.Registries.DAMAGE_TYPE)
			.getOrThrow(FROST_BREATH), dragon, dragon), amount);
		if (!IafEffects.warded(entity)) {
			blackFrostBreath(level, entity, dragon, BLACK_FROST_BREATH / 2);
		}
	}

	/** Whether no life may come back to it now: a wound of Black Frost's ice (on the server only). */
	public static boolean wounded(LivingEntity entity) {
		if (WOUND.isEmpty() || entity.level() == null || entity.level().isClientSide) {
			return false;
		}
		Long until = WOUND.get(entity.getUUID());
		return until != null && entity.level().getServer() != null && entity.level().getServer().overworld().getGameTime() < until;
	}

	/** Whether no life may come back to it now (cauterized, and not its totem saving it this moment). */
	public static boolean noHealing(LivingEntity entity) {
		return wounded(entity) && !entity.getUUID().equals(saving);
	}

	/** Whether Black Frost's ice bites a player straight to the heart: one in its fight (not in creative), without the pixie's gift. */
	public static boolean bites(LivingEntity target, @Nullable Entity boss) {
		return boss instanceof EntityDragonBase dragon && dragon.isBlackFrost() && bitable(target);
	}

	/**
	 * A player the ice of the Frozen Throne bites straight to the heart: not in creative, nor made untouchable by a game
	 * master, without the pixie's gift.
	 */
	public static boolean bitable(LivingEntity target) {
		return target instanceof ServerPlayer player && !player.isCreative() && !player.isSpectator() && player.isAlive()
			&& !player.isInvulnerable() && !com.iafabric.pixie.FairyVision.iceGift(player);
	}

	/**
	 * The ice of the Frozen Throne on who lacks the pixie's gift: straight to their hearts (no armor, enchantment,
	 * potion, ward, shield, golden heart nor any mod's guard holds it), and it cauterizes them. A totem of undying in
	 * hand saves them from the bite that would kill them, as from any death (the cauterization stays). Who falls to it
	 * falls for real: what they carried waits for them on the arena (ThroneFight), a lesson.
	 */
	public static void bite(ServerLevel level, ServerPlayer player, Entity boss, float damage) {
		long now = level.getServer().overworld().getGameTime();
		boolean fresh = !wounded(player);
		WOUND.put(player.getUUID(), now + WOUND_TICKS);
		player.addEffect(new MobEffectInstance(IafEffects.FROST_WOUND, WOUND_TICKS, 0, false, true, true));
		if (fresh) {
			player.displayClientMessage(Component.translatable("iceandfire_fabric.boss.frost_wound").withStyle(ChatFormatting.AQUA, ChatFormatting.BOLD), true);
		}
		DamageSource source = BossLimits.doomSource(level, boss);
		float health = player.getHealth() - damage;
		level.sendParticles(ParticleTypes.SNOWFLAKE, player.getX(), player.getY() + 1, player.getZ(), 12, 0.3, 0.5, 0.3, 0.02);
		if (health > 0F) {
			player.setHealth(health);
			// (they flinch at it once a half second: a jet that bites often is not a din of hurt sounds)
			Long flinched = FLINCHED.get(player.getUUID());
			if (flinched == null || now - flinched >= FLINCH_EVERY || now < flinched) {
				FLINCHED.put(player.getUUID(), now);
				level.broadcastDamageEvent(player, source);
			}
			return;
		}
		player.setHealth(0F);
		boolean saved;
		saving = player.getUUID();
		try {
			saved = ((com.iafabric.mixin.LivingEntityTotemInvoker) player).iceandfire_fabric$checkTotemDeathProtection(source);
		} finally {
			saving = null;
		}
		if (saved) {
			// saved by their totem (its own: back to a heart, its effects, its flash)
			return;
		}
		player.die(source);
	}

	/** Its breath's bite, once a half second while it catches them. */
	private static void breathBite(ServerLevel level, ServerPlayer player, EntityDragonBase dragon) {
		long now = level.getGameTime();
		Long last = BITTEN.get(player.getUUID());
		if (last != null && now - last < BREATH_BITE_EVERY && now >= last) {
			return;
		}
		BITTEN.put(player.getUUID(), now);
		float share = BlackFrostFight.desperate(dragon) ? BREATH_BITE * 1.5F : BREATH_BITE;
		bite(level, player, dragon, player.getMaxHealth() * share);
	}

	/** Freezes for a while, unless it was frozen a moment ago or the Frost Ward holds the cold off. */
	public static void freeze(ServerLevel level, LivingEntity entity, int ticks) {
		if (IafEffects.warded(entity)) {
			warded(level, entity);
			return;
		}
		long now = level.getGameTime();
		Long free = FREE_UNTIL.get(entity.getUUID());
		if (free != null && now < free) {
			return;
		}
		ticks = held(entity, ticks);
		FrozenProperties.setFrozenFor(entity, ticks);
		freeAfter(level, entity, ticks);
	}

	/** Black Frost's Glacial Prison: frozen solid, ward or no ward; then free of ice as after any freeze. */
	public static void prison(ServerLevel level, LivingEntity entity, int ticks) {
		ticks = held(entity, ticks);
		FrozenProperties.setFrozenFor(entity, ticks);
		freeAfter(level, entity, ticks);
	}

	private static void freeAfter(ServerLevel level, LivingEntity entity, int ticks) {
		long now = level.getGameTime();
		FREE_UNTIL.put(entity.getUUID(), now + ticks + FREE);
		if (FREE_UNTIL.size() > 256) {
			FREE_UNTIL.values().removeIf(until -> until < now);
		}
	}

	/** Slowed by the cold (not with the ward). */
	public static void slow(LivingEntity entity, int ticks, int amplifier, Entity source) {
		if (!IafEffects.warded(entity)) {
			entity.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, ticks, amplifier), source);
		}
	}

	/** Out of the ice and the slowness at once (before Absolute Zero: everyone may run for a warm light). */
	public static void thaw(LivingEntity entity) {
		FrozenProperties.thaw(entity);
		entity.removeEffect(MobEffects.MOVEMENT_SLOWDOWN);
		entity.setTicksFrozen(0);
	}

	/**
	 * The freeze of an ice dragon's breath or ice charge: none with the ward, short from Black Frost (and never ice upon
	 * ice), as long as it always was from the other ice dragons.
	 */
	public static void breathFreeze(LivingEntity entity, EntityDragonBase dragon, int ticks) {
		if (!(entity.level() instanceof ServerLevel level)) {
			return;
		}
		if (empowered(dragon)) {
			// Cryona's weapons in its rider's (or owner's) hand: its ice freezes solid, ward or no ward
			if (!FrozenProperties.isFrozen(entity)) {
				prison(level, entity, 60);
				net.minecraft.world.entity.Display.BlockDisplay shell = com.iafabric.vfx.Vfx.shape(level, entity.position().subtract(0, 0.1, 0),
					net.minecraft.world.level.block.Blocks.ICE.defaultBlockState(), entity.getBbWidth() + 0.6F, entity.getBbHeight() + 0.3F, 0F, 0F, true);
				com.iafabric.vfx.Vfx.later(level, 60, () -> com.iafabric.vfx.Vfx.remove(shell));
			}
			return;
		}
		if (bites(entity, dragon)) {
			// without the pixie's gift its breath bites through any ward
			breathBite(level, (ServerPlayer) entity, dragon);
		}
		if (IafEffects.warded(entity)) {
			warded(level, entity);
			return;
		}
		if (dragon.isBlackFrost()) {
			blackFrostBreath(level, entity, dragon, Math.min(ticks, BLACK_FROST_BREATH));
			return;
		}
		FrozenProperties.setFrozenFor(entity, ticks);
	}

	/**
	 * Black Frost's breath: it always freezes whoever it catches (a moment free after, to step out of it). With the
	 * pixie's gift its first touch only chills; caught again soon, they freeze for half as long and are free longer.
	 */
	private static void blackFrostBreath(ServerLevel level, LivingEntity entity, EntityDragonBase dragon, int ticks) {
		long now = level.getGameTime();
		UUID id = entity.getUUID();
		Long free = FREE_UNTIL.get(id);
		if (free != null && now < free) {
			return;
		}
		boolean gift = com.iafabric.pixie.FairyVision.iceGift(entity);
		if (gift) {
			Long chilled = CHILLED.get(id);
			if (chilled == null || now - chilled > CHILL_WINDOW) {
				CHILLED.put(id, now);
				entity.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 30, 0), dragon);
				level.sendParticles(ParticleTypes.SNOWFLAKE, entity.getX(), entity.getY() + 1, entity.getZ(), 8, 0.3, 0.5, 0.3, 0.02);
				return;
			}
			CHILLED.remove(id);
		}
		int held = gift ? Math.max(1, ticks / 2) : ticks;
		FrozenProperties.setFrozenFor(entity, held);
		FREE_UNTIL.put(id, now + held + (gift ? GIFT_BREATH_FREE : BREATH_FREE));
	}

	/** An ice dragon whose rider (or owner) holds one of Cryona's weapons: its ice freezes solid. */
	public static boolean empowered(EntityDragonBase dragon) {
		if (!(dragon instanceof com.iafabric.entity.EntityIceDragon) || dragon.isBlackFrost()) {
			return false;
		}
		if (com.iafabric.item.ItemDreadQueenStaff.holdsCryonasWeapon(dragon.getRidingPlayer())) {
			return true;
		}
		return dragon.isTame() && dragon.getOwner() instanceof LivingEntity owner && com.iafabric.item.ItemDreadQueenStaff.holdsCryonasWeapon(owner);
	}

	/** An ice breath's blow: less with the ward; Black Frost's stronger when it is near its end (its super breath). */
	public static float breathDamage(LivingEntity entity, EntityDragonBase dragon, float amount) {
		if (dragon.isBlackFrost() && BlackFrostFight.desperate(dragon)) {
			amount *= 1.5F;
		}
		return IafEffects.warded(entity) ? amount * IafEffects.WARDED_BREATH : amount;
	}

	/** The ward shines a moment where the cold meets it. */
	private static void warded(ServerLevel level, LivingEntity entity) {
		if (entity.tickCount % 4 == 0) {
			level.sendParticles(ParticleTypes.GLOW, entity.getX(), entity.getY() + entity.getBbHeight() * 0.6, entity.getZ(), 3,
				entity.getBbWidth() * 0.4, entity.getBbHeight() * 0.3, entity.getBbWidth() * 0.4, 0.01);
		}
	}
}

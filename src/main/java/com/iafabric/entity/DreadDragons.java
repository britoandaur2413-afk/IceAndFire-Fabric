package com.iafabric.entity;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.BossEvent;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * The dragons of the dread: undead dragons ridden by dread knights in their spires (DragonRiders: the knight has its
 * own hitbox and health; killed, the dragon goes to another knight), and Black Frost, the Dread Queen's own. They
 * never fight the dread nor break blocks, rest (sitting, or asleep for Black Frost) while nothing comes, and stand up
 * when a player comes near. Black Frost has a boss bar.
 */
public final class DreadDragons {
	private static final int ALERT = 24;
	private static final int CALM = 120;
	private static final Map<UUID, ServerBossEvent> BARS = new HashMap<>();

	private DreadDragons() {
	}

	public static void register() {
		net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents.END_LEVEL_TICK.register(level -> {
			if (level.getGameTime() % 20 == 0) {
				cleanBars(level);
			}
		});
	}

	/** Every tick on the server for a dread dragon (from DragonSmarts). */
	public static void tick(EntityDragonBase dragon, ServerLevel level) {
		if (dragon.isBlackFrost() && dragon.dreadMount && !dragon.isModelDead() && (dragon.tickCount + dragon.getId()) % 40 == 0
			&& Math.abs(dragon.getMaxHealth() - BLACK_FROST_HEALTH) > 1) {
			// a Black Frost of an older world (its age gave it its own life and armor again)
			blackFrostStats(dragon);
		}
		if (dragon.isBlackFrost() && dragon.getFirstPassenger() instanceof EntityDreadQueen
			&& !com.iafabric.boss.ThroneFight.inRealm(dragon) && !dragon.isModelDead()) {
			// Cryona woke outside her realm: its powers here (in the realm ThroneFight leads them; held, it still acts)
			com.iafabric.boss.BlackFrostFight.of(dragon).tickOutside(level);
		}
		if (!dragon.dreadMount || dragon.isNoAi() || dragon.fightLed) {
			// (Black Frost lies still while its queen sleeps)
			return;
		}
		LivingEntity target = dragon.getTarget();
		if (target instanceof com.iafabric.entity.util.IDreadMob || target != null && dragon.alliedWith(target)) {
			dragon.setTarget(null);
			target = null;
		}
		Entity rider = dragon.getFirstPassenger();
		if (rider instanceof Mob mob && mob.getTarget() != null && mob.getTarget().isAlive() && mob.getTarget() != target && !(mob.getTarget() instanceof EntityDreadMob)) {
			dragon.setTarget(mob.getTarget());
			target = mob.getTarget();
		}
		if (dragon.isBlackFrost()) {
			// asleep while its queen sleeps on her throne (awake, it fights on even without her)
			if (!(rider instanceof EntityDreadQueen) && dragon.isSleeping()) {
				return;
			}
		}
		if (dragon.riderCandidate != null) {
			// it goes for a new knight (DragonRiders): no sitting down on the way
			dragon.calmTicks = 0;
			return;
		}
		if (target == null && dragon.tickCount % 10 == 0) {
			Player near = level.getNearestPlayer(dragon, ALERT);
			if (near != null && !near.isCreative() && !near.isSpectator() && dragon.hasLineOfSight(near)) {
				dragon.setTarget(near);
				target = near;
			}
		}
		if (target == null) {
			if (++dragon.calmTicks >= CALM && dragon.onGround() && !dragon.isOrderedToSit() && !dragon.isBlackFrost()) {
				dragon.setCommand(1);
			}
		} else {
			dragon.calmTicks = 0;
			if (dragon.isOrderedToSit()) {
				dragon.setCommand(0);
				dragon.roar();
			}
			if (dragon.isSleeping()) {
				dragon.setInSittingPose(false);
			}
		}
	}

	/** Black Frost's boss bar, seen by the players near it (every tick, from its server logic, whoever leads it). */
	public static void bar(EntityDragonBase dragon, ServerLevel level) {
		ServerBossEvent bar = BARS.computeIfAbsent(dragon.getUUID(), id -> {
			ServerBossEvent event = new ServerBossEvent(UUID.randomUUID(), dragon.getDisplayName(), BossEvent.BossBarColor.PURPLE, BossEvent.BossBarOverlay.NOTCHED_10);
			event.setDarkenScreen(true);
			return event;
		});
		bar.setProgress(Math.max(0F, dragon.getHealth() / dragon.getMaxHealth()));
		// Cryona on its back: one boss, hers is the name
		boolean ridden = dragon.getFirstPassenger() instanceof EntityDreadQueen;
		bar.setName(ridden ? Component.translatable("iceandfire_fabric.boss.cryona_mounted") : dragon.getDisplayName());
		bar.setColor(ridden ? BossEvent.BossBarColor.BLUE : BossEvent.BossBarColor.PURPLE);
		if (dragon.tickCount % 10 == 0) {
			// (no bar before the duel is accepted: there is no fight yet)
			boolean shown = !dragon.isModelDead() && !dragon.isSleeping() && !com.iafabric.boss.ThroneFight.waiting(dragon);
			for (ServerPlayer player : level.players()) {
				boolean near = shown && player.distanceToSqr(dragon) < 80 * 80;
				if (near && !bar.getPlayers().contains(player)) {
					bar.addPlayer(player);
				} else if (!near && bar.getPlayers().contains(player)) {
					bar.removePlayer(player);
				}
			}
		}
	}

	/** Whether a player sees Black Frost's bar now. */
	public static boolean barShown(EntityDragonBase dragon, ServerPlayer player) {
		ServerBossEvent bar = BARS.get(dragon.getUUID());
		return bar != null && bar.isVisible() && bar.getPlayers().contains(player);
	}

	/** Bars of dragons gone or dead are taken away (from the server tick, once a second). */
	public static void cleanBars(ServerLevel level) {
		for (Iterator<Map.Entry<UUID, ServerBossEvent>> it = BARS.entrySet().iterator(); it.hasNext(); ) {
			Map.Entry<UUID, ServerBossEvent> entry = it.next();
			Entity entity = level.getEntity(entry.getKey());
			if (entity != null && (!entity.isAlive() || entity instanceof EntityDragonBase dragon && dragon.isModelDead())) {
				entry.getValue().removeAllPlayers();
				it.remove();
			} else if (entity == null && level == level.getServer().overworld() && level.getServer().getAllLevels().iterator().hasNext()) {
				// not in this world: it may be in another; its bar stays until it is found gone
				boolean anywhere = false;
				for (ServerLevel other : level.getServer().getAllLevels()) {
					anywhere |= other.getEntity(entry.getKey()) != null;
				}
				if (!anywhere) {
					entry.getValue().removeAllPlayers();
					it.remove();
				}
			}
		}
	}

	/** The undead dragons near a place (for the dread spires and the queen). */
	public static List<EntityDragonBase> near(ServerLevel level, Entity at, double range) {
		return level.getEntitiesOfClass(EntityDragonBase.class, at.getBoundingBox().inflate(range), d -> d.dreadMount && d.isAlive());
	}

	/** Black Frost's life (a boss's: ten thousand) and armor (low: its life is its guard). */
	public static final double BLACK_FROST_HEALTH = 10000;
	public static final double BLACK_FROST_ARMOR = 8;

	/**
	 * Gives Black Frost its life and armor (a Black Frost of an older world too: its life keeps its share). Its own armor
	 * comes from its age and its equipment, the modifier brings it to {@link #BLACK_FROST_ARMOR}.
	 */
	public static void blackFrostStats(EntityDragonBase dragon) {
		var health = dragon.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH);
		var armor = dragon.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.ARMOR);
		if (health == null || armor == null) {
			return;
		}
		float share = dragon.getMaxHealth() > 0 ? dragon.getHealth() / dragon.getMaxHealth() : 1F;
		var healthId = com.iafabric.IafMod.id("black_frost_health");
		var armorId = com.iafabric.IafMod.id("black_frost_armor");
		health.removeModifier(healthId);
		armor.removeModifier(armorId);
		health.addPermanentModifier(new net.minecraft.world.entity.ai.attributes.AttributeModifier(healthId, BLACK_FROST_HEALTH - health.getValue(),
			net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADD_VALUE));
		armor.addPermanentModifier(new net.minecraft.world.entity.ai.attributes.AttributeModifier(armorId, BLACK_FROST_ARMOR - armor.getValue(),
			net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADD_VALUE));
		var resistance = dragon.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.KNOCKBACK_RESISTANCE);
		if (resistance != null) {
			resistance.setBaseValue(1);
		}
		dragon.setHealth(Math.max(1F, dragon.getMaxHealth() * share));
	}

	/** The name Black Frost goes by. */
	public static Component blackFrostName() {
		return Component.translatable("entity.iceandfire_fabric.black_frost");
	}
}

package com.iafabric.event;

import com.iafabric.IafConfig;
import com.iafabric.entity.EntityDragonBase;
import com.iafabric.entity.EntityPixie;
import com.iafabric.entity.IafEntityRegistry;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Dragons of the open sky. Now and then a young dragon (stage 2 or 3) flies over a player: it leaves everyone alone
 * unless attacked. A player who kills two of them in a short time is warned by an oracle pixie, and then a wave of
 * strong dragons comes for them.
 */
public final class DragonSkies {
	/** Ticks between the oracle pixie's warning and the wave (also changed by the tests). */
	public static int waveDelay = 600;
	/** Ticks between the second kill and the oracle pixie. */
	public static int oracleDelay = 100;
	/** How far from the player the wave's dragons appear (closer where the land there is not loaded). */
	public static int waveDistance = 45;
	/** Two kills within this many ticks call a wave. */
	private static final int KILL_WINDOW = 20 * 60 * 20;
	private static final int SKY_DRAGON_LIFE = 20 * 60 * 4;
	private static final int WAVE_LIFE = 20 * 60 * 5;

	private static final Map<UUID, Deque<Long>> KILLS = new HashMap<>();
	/** Per player: when the oracle pixie comes and when the wave comes (game time), 0 when nothing is coming. */
	private static final Map<UUID, long[]> COMING = new HashMap<>();

	private DragonSkies() {
	}

	public static void register() {
		ServerTickEvents.END_WORLD_TICK.register(DragonSkies::tick);
		ServerLivingEntityEvents.AFTER_DEATH.register((entity, source) -> {
			if (entity instanceof EntityDragonBase dragon && dragon.skyWanderer && source.getEntity() instanceof ServerPlayer player) {
				onSkyDragonKilled(player);
			}
		});
	}

	private static void tick(ServerLevel level) {
		if (level.dimension() != Level.OVERWORLD) {
			return;
		}
		long now = level.getGameTime();
		for (ServerPlayer player : level.players()) {
			long[] coming = COMING.get(player.getUUID());
			if (coming != null) {
				if (coming[0] != 0 && now >= coming[0]) {
					coming[0] = 0;
					sendOracle(level, player);
				}
				if (coming[1] != 0 && now >= coming[1]) {
					coming[1] = 0;
					sendWave(level, player);
					COMING.remove(player.getUUID());
				}
			}
			if (IafConfig.skyDragons && !player.isCreative() && !player.isSpectator() && (now + player.getId()) % 1200 == 0
				&& level.getRandom().nextInt(Math.max(1, IafConfig.skyDragonMinutes)) == 0 && skyDragonsNear(level, player) == 0) {
				spawnSkyDragon(level, player);
			}
		}
	}

	private static int skyDragonsNear(ServerLevel level, ServerPlayer player) {
		return level.getEntitiesOfClass(EntityDragonBase.class, player.getBoundingBox().inflate(200), d -> d.skyWanderer || d.waveDragon).size();
	}

	/** The dragon of the climate where the player is. */
	private static EntityType<? extends EntityDragonBase> kindAt(ServerLevel level, BlockPos pos) {
		float temperature = level.getBiome(pos).value().getBaseTemperature();
		return temperature < 0.3F ? IafEntityRegistry.ICE_DRAGON : temperature >= 0.95F ? IafEntityRegistry.FIRE_DRAGON : IafEntityRegistry.LIGHTNING_DRAGON;
	}

	/** A young dragon flying high, 60 to 90 blocks from the player. */
	public static EntityDragonBase spawnSkyDragon(ServerLevel level, ServerPlayer player) {
		return spawnSkyDragon(level, player, 60 + level.getRandom().nextInt(30));
	}

	public static EntityDragonBase spawnSkyDragon(ServerLevel level, ServerPlayer player, double distance) {
		float angle = level.getRandom().nextFloat() * Mth.TWO_PI;
		int x = Mth.floor(player.getX() + Mth.cos(angle) * distance);
		int z = Mth.floor(player.getZ() + Mth.sin(angle) * distance);
		int ground = level.getHeight(Heightmap.Types.MOTION_BLOCKING, x, z);
		double y = Math.max(ground + 40, player.getY() + 30);
		EntityDragonBase dragon = kindAt(level, player.blockPosition()).create(level);
		dragon.growDragon(30 + level.getRandom().nextInt(40));
		dragon.setHealth(dragon.getMaxHealth());
		dragon.setVariant(level.getRandom().nextInt(4));
		dragon.setGender(level.getRandom().nextBoolean());
		dragon.absMoveTo(x + 0.5, y, z + 0.5, level.getRandom().nextFloat() * 360, 0);
		dragon.skyWanderer = true;
		dragon.despawnAt = level.getGameTime() + SKY_DRAGON_LIFE;
		dragon.setHovering(false);
		dragon.setFlying(true);
		level.addFreshEntity(dragon);
		return dragon;
	}

	/** Two sky dragons killed in a short time: the oracle pixie comes, then the wave. */
	public static void onSkyDragonKilled(ServerPlayer player) {
		long now = player.level().getGameTime();
		Deque<Long> kills = KILLS.computeIfAbsent(player.getUUID(), id -> new ArrayDeque<>());
		kills.addLast(now);
		while (!kills.isEmpty() && now - kills.peekFirst() > KILL_WINDOW) {
			kills.removeFirst();
		}
		if (kills.size() >= Math.max(1, IafConfig.dragonWaveKills) && !COMING.containsKey(player.getUUID())) {
			kills.clear();
			COMING.put(player.getUUID(), new long[]{now + oracleDelay, now + oracleDelay + waveDelay});
		}
	}

	/** An oracle pixie appears beside the player, warns them, and is gone. */
	public static EntityPixie sendOracle(ServerLevel level, ServerPlayer player) {
		Vec3 look = player.getViewVector(1.0F);
		Vec3 at = player.position().add(look.x * 2.5, 1.6, look.z * 2.5);
		EntityPixie pixie = IafEntityRegistry.PIXIE.create(level);
		pixie.absMoveTo(at.x, at.y, at.z, player.getYRot() + 180, 0);
		pixie.setColor(2);
		pixie.oracleTicks = 0;
		pixie.setCustomName(Component.translatable("entity.iceandfire_fabric.pixie.oracle").withStyle(ChatFormatting.AQUA));
		pixie.setCustomNameVisible(true);
		pixie.setGlowingTag(true);
		pixie.setInvulnerable(true);
		level.addFreshEntity(pixie);
		player.sendSystemMessage(Component.translatable("iceandfire_fabric.oracle.warning", pixie.getDisplayName()).withStyle(ChatFormatting.LIGHT_PURPLE));
		return pixie;
	}

	/** Three strong dragons come for the player from the sky: they do not stop until the player is gone. */
	public static void sendWave(ServerLevel level, ServerPlayer player) {
		EntityType<? extends EntityDragonBase> type = kindAt(level, player.blockPosition());
		for (int i = 0; i < 3; i++) {
			float angle = level.getRandom().nextFloat() * Mth.TWO_PI;
			double distance = waveDistance;
			int x = Mth.floor(player.getX() + Mth.cos(angle) * distance);
			int z = Mth.floor(player.getZ() + Mth.sin(angle) * distance);
			while (distance > 4 && !level.isLoaded(new BlockPos(x, 0, z))) {
				distance -= 8;
				x = Mth.floor(player.getX() + Mth.cos(angle) * distance);
				z = Mth.floor(player.getZ() + Mth.sin(angle) * distance);
			}
			int ground = level.getHeight(Heightmap.Types.MOTION_BLOCKING, x, z);
			EntityDragonBase dragon = type.create(level);
			dragon.growDragon(80 + level.getRandom().nextInt(15));
			dragon.setHealth(dragon.getMaxHealth());
			dragon.setVariant(level.getRandom().nextInt(4));
			dragon.setGender(level.getRandom().nextBoolean());
			dragon.absMoveTo(x + 0.5, Math.max(ground + 25, player.getY() + 20), z + 0.5, 0, 0);
			dragon.waveDragon = true;
			dragon.despawnAt = level.getGameTime() + WAVE_LIFE;
			dragon.revengePlayer = player.getUUID();
			dragon.revengeUntil = level.getGameTime() + WAVE_LIFE;
			dragon.setFlying(true);
			level.addFreshEntity(dragon);
			dragon.setTarget(player);
		}
		player.sendSystemMessage(Component.translatable("iceandfire_fabric.oracle.wave").withStyle(ChatFormatting.RED));
	}

	/**
	 * A sky dragon only fights who hurt it; sky and wave dragons leave when their time is over and no player is
	 * near. Called each tick for wild dragons (DragonSmarts).
	 */
	public static void tickDragon(EntityDragonBase dragon, ServerLevel level) {
		if (dragon.skyWanderer && dragon.revengePlayer == null) {
			LivingEntity target = dragon.getTarget();
			if (target != null && target != dragon.getLastHurtByMob()) {
				dragon.setTarget(null);
			}
		}
		if ((dragon.skyWanderer || dragon.waveDragon) && dragon.despawnAt > 0 && level.getGameTime() > dragon.despawnAt
			&& level.getEntitiesOfClass(ServerPlayer.class, new AABB(dragon.blockPosition()).inflate(96)).isEmpty()) {
			dragon.discard();
		}
	}
}

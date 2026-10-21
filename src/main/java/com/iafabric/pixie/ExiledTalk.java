package com.iafabric.pixie;

import com.iafabric.IafMod;
import com.iafabric.entity.EntityDragonBase;
import com.iafabric.entity.EntityDreadQueen;
import com.iafabric.entity.EntityExiledPixie;
import com.iafabric.message.MessageExiledSpeech;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.player.Player;
import org.jspecify.annotations.Nullable;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * What an exiled pixie says about what is around her player. The creatures near catch her eye (the nearer, the more
 * of them, in sight and just come near, the likelier; her player's own less; a kind she spoke of lately waits its
 * turn): six lines for each of those met most often, three for the rest, her anecdotes for villagers and her jokes
 * for witches. On the way to a village she tells of the villagers she knew, on the way to the witches' cottage she
 * jokes about witches; at her friends' sanctuary she finds them gone; when her player bonds another pixie she has a
 * word about pixies nowadays. Each kind of line goes round before one of it is said again (how many: ExiledLines;
 * their texts: the lang files).
 */
public final class ExiledTalk {
	/** How near a creature catches her eye (blocks). */
	private static final double NOTICE = 24;
	/** How long a kind waits after she spoke of it: five minutes. */
	private static final long KIND_WAIT = 6000;

	/** Each player's last word about each kind (game time). */
	private static final Map<UUID, Map<String, Long>> SPOKEN = new HashMap<>();
	/** The kinds near each player at the last look (one just come near catches her eye more). */
	private static final Map<UUID, Set<String>> SEEN = new HashMap<>();
	/** When each player's next anecdote or joke on the way is due (and on the way to what). */
	private static final Map<UUID, Trip> TRIP = new HashMap<>();

	private record Trip(ExiledQuest quest, long due) {
	}

	private ExiledTalk() {
	}

	static void forget(UUID id) {
		SPOKEN.remove(id);
		SEEN.remove(id);
		TRIP.remove(id);
	}

	/** The kind of lines a creature calls for, or null (none of hers: a player, herself, those she has her own words for). */
	public static @Nullable String kind(LivingEntity entity) {
		if (entity instanceof Player || entity instanceof EntityExiledPixie || entity instanceof EntityDreadQueen
			|| entity instanceof com.iafabric.entity.EntityCryonaAsleep) {
			return null;
		}
		if (entity instanceof EntityDragonBase dragon) {
			if (dragon.isModelDead()) {
				return "dragon_skeleton";
			}
			if (dragon.isBlackFrost()) {
				return null;
			}
			if (dragon.getDreadLook() == 1) {
				return "bone_dragon";
			}
		}
		Identifier id = BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType());
		String name = id.getPath();
		if (id.getNamespace().equals(IafMod.MOD_ID)) {
			if (name.startsWith("myrmex")) {
				return "myrmex";
			}
			if (name.startsWith("dread_")) {
				return "dread";
			}
			return ExiledLines.OURS.contains(name) ? name : null;
		}
		return id.getNamespace().equals("minecraft") && ExiledLines.SEES.containsKey(name) && !ExiledLines.OURS.contains(name) ? name : null;
	}

	/** The kinds near the player, each with how much it catches her eye (nearer, in sight, wild, more of them: more). */
	public static Map<String, Double> near(ServerPlayer player) {
		Map<String, Double> kinds = new HashMap<>();
		for (LivingEntity entity : player.level().getEntitiesOfClass(LivingEntity.class, player.getBoundingBox().inflate(NOTICE, 12, NOTICE),
			e -> e.isAlive() && !e.isInvisible())) {
			String kind = kind(entity);
			double distance = entity.distanceTo(player);
			if (kind == null || distance > NOTICE) {
				continue;
			}
			double weight = 1 - distance / NOTICE;
			weight *= weight;
			if (!player.hasLineOfSight(entity)) {
				weight *= 0.35;
			}
			if (entity instanceof OwnableEntity own && own.getOwner() == player) {
				weight *= 0.3;
			}
			kinds.merge(kind, weight, Double::sum);
		}
		kinds.replaceAll((kind, weight) -> Math.min(weight, 1.5));
		return kinds;
	}

	/**
	 * A word about a creature near, if one catches her eye now (likelier when her remark is due anyway); false if
	 * none did.
	 */
	public static boolean sight(ServerPlayer player, ExiledPixieData data, long now, boolean due) {
		UUID id = player.getUUID();
		Map<String, Double> kinds = near(player);
		Set<String> before = SEEN.put(id, new HashSet<>(kinds.keySet()));
		Map<String, Long> spoken = SPOKEN.computeIfAbsent(id, k -> new HashMap<>());
		Map<String, Double> calls = new HashMap<>();
		double total = 0;
		for (Map.Entry<String, Double> entry : kinds.entrySet()) {
			if (now - spoken.getOrDefault(entry.getKey(), -KIND_WAIT) < KIND_WAIT) {
				continue;
			}
			double weight = entry.getValue();
			if (before != null && !before.contains(entry.getKey())) {
				// it just came near
				weight *= 2;
			}
			calls.put(entry.getKey(), weight);
			total += weight;
		}
		if (total <= 0) {
			return false;
		}
		RandomSource random = player.getRandom();
		double chance = due ? 0.7 : Math.min(0.6, total * 0.35) * switch (data.talk) {
			case 0 -> 0.2;
			case 2 -> 1.0;
			default -> 0.6;
		};
		if (random.nextDouble() >= chance) {
			return false;
		}
		double pick = random.nextDouble() * total;
		String chosen = null;
		for (Map.Entry<String, Double> entry : calls.entrySet()) {
			chosen = entry.getKey();
			pick -= entry.getValue();
			if (pick <= 0) {
				break;
			}
		}
		spoken.put(chosen, now);
		ExiledPixies.say(player, line(player, data, "sees." + chosen, ExiledLines.SEES.get(chosen)), MessageExiledSpeech.TALK);
		return true;
	}

	/** On the way to a village or to the witches' cottage: now and then an anecdote of villagers, or a joke about witches. */
	public static boolean trip(ServerPlayer player, ExiledPixieData data, long now) {
		ExiledQuest quest = data.current();
		String kind = quest == ExiledQuest.VILLAGE ? "villager" : quest == ExiledQuest.CAKE ? "witch" : null;
		UUID id = player.getUUID();
		if (kind == null || data.target == null || (data.progress & 1) != 0 || !data.targetLevel.equals(player.level().dimension().identifier().toString())) {
			TRIP.remove(id);
			return false;
		}
		BlockPos goal = data.goal();
		double dx = goal.getX() + 0.5 - player.getX();
		double dz = goal.getZ() + 0.5 - player.getZ();
		if (dx * dx + dz * dz < 24 * 24) {
			return false;
		}
		Trip trip = TRIP.get(id);
		if (trip == null || trip.quest() != quest) {
			// the first one soon after setting out
			TRIP.put(id, new Trip(quest, now + 300 + player.getRandom().nextInt(300)));
			return false;
		}
		if (now < trip.due()) {
			return false;
		}
		TRIP.put(id, new Trip(quest, now + tripWait(data, player.getRandom())));
		ExiledPixies.say(player, line(player, data, "sees." + kind, ExiledLines.SEES.get(kind)), MessageExiledSpeech.TALK);
		return true;
	}

	/** How long until her next one on the way, as much as the player wants her to talk. */
	private static long tripWait(ExiledPixieData data, RandomSource random) {
		return switch (data.talk) {
			case 0 -> 3000 + random.nextInt(1200);
			case 2 -> 800 + random.nextInt(400);
			default -> 1300 + random.nextInt(600);
		};
	}

	/** At her friends' sanctuary: they are gone, the young pixies look at her oddly, and a cake makes a friend of any of them. */
	static void friends(ServerPlayer player, ExiledPixieData data) {
		ExiledPixies.say(player, line(player, data, "friends.sad", ExiledLines.FRIENDS_SAD), MessageExiledSpeech.QUEST);
		ExiledPixies.sayLater(player, 110, line(player, data, "friends.look", ExiledLines.FRIENDS_LOOK), MessageExiledSpeech.TALK);
		ExiledPixies.sayLater(player, 220, line(player, data, "friends.cake", ExiledLines.FRIENDS_CAKE), MessageExiledSpeech.QUEST);
	}

	/** Her player bonded another pixie: a word (and a wink) about pixies nowadays. */
	public static void bonded(ServerPlayer player) {
		ExiledPixieData data = ExiledPixies.get(player);
		if (data.stage == ExiledPixieData.FRIEND && data.out && ExiledPixies.living(player) != null) {
			ExiledPixies.sayLater(player, 50, line(player, data, "bond", ExiledLines.BOND), MessageExiledSpeech.TALK);
		}
	}

	/** One of a kind's lines (exiled.iceandfire_fabric.KIND.N) not said since they all last went round. */
	public static Component line(ServerPlayer player, ExiledPixieData data, String kind, int count) {
		long all = count >= 64 ? -1L : (1L << count) - 1;
		long told = data.told.getOrDefault(kind, 0L) & all;
		if (told == all) {
			told = 0;
		}
		int n = player.getRandom().nextInt(count - Long.bitCount(told));
		int line = 0;
		for (int i = 0; i < count; i++) {
			if ((told & (1L << i)) == 0 && n-- == 0) {
				line = i;
				break;
			}
		}
		data.told.put(kind, told | (1L << line));
		ExiledPixies.save(player, data);
		return Component.translatable("exiled.iceandfire_fabric." + kind + "." + line);
	}
}

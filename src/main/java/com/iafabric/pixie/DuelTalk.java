package com.iafabric.pixie;

import com.iafabric.boss.ThroneFight;
import com.iafabric.message.MessageExiledSpeech;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

/**
 * What the exiled pixie says in Cryona's duel: only about the fight as it goes (her other remarks wait till it is over),
 * when its moments come: a blow to get out of, an opening to strike, its turns. Never two lines close together, nor the
 * same moment again soon; as little as the player wants her to talk (the urgent ones always).
 */
public final class DuelTalk {
	/** Ticks before the same moment is spoken of again. */
	private static final int AGAIN = 1200;
	private static final Map<UUID, Long> LAST = new HashMap<>();
	private static final Map<UUID, Map<Moment, Long>> SAID = new HashMap<>();

	/** A moment of the duel she speaks of: its lines (how many), whether it is a danger, whether it is urgent. */
	public enum Moment {
		BEGIN(1, false, false),
		BREATH(2, true, false),
		GREAT_BREATH(1, true, false),
		DIVE(1, true, false),
		STAMPEDE(1, true, false),
		PRISON(1, true, false),
		GRAB(1, true, false),
		STUNNED(2, false, false),
		PYLONS(1, false, false),
		PYLON_BROKEN(1, false, false),
		ZERO(1, true, true),
		SPENT(1, false, false),
		FALLS(1, false, true),
		DEAD(1, true, false),
		RAYS(1, true, false),
		RISES(1, false, true),
		SHIELD(1, false, false),
		BEAMS(1, true, false),
		RING(1, true, false),
		HURT(1, true, true),
		WON(1, false, true);

		final int lines;
		final boolean danger;
		final boolean urgent;

		Moment(int lines, boolean danger, boolean urgent) {
			this.lines = lines;
			this.danger = danger;
			this.urgent = urgent;
		}

		String key() {
			return name().toLowerCase(Locale.ROOT);
		}
	}

	private DuelTalk() {
	}

	/** A moment of the fight a boss is in: told to its players whose pixie is with them. */
	public static void moment(Entity boss, Moment moment) {
		List<ServerPlayer> players = ThroneFight.foesNear(boss);
		for (ServerPlayer player : players) {
			say(player, moment);
		}
	}

	/** A moment of the duel for one player (if their pixie is out with them, and it is time for a word). */
	public static void say(ServerPlayer player, Moment moment) {
		ExiledPixieData data = ExiledPixies.get(player);
		if (data.stage != ExiledPixieData.FRIEND || !data.out || FairyVision.inMemory(player) || data.talk == 0 && !moment.urgent
			|| ExiledPixies.living(player) == null) {
			return;
		}
		long now = ((ServerLevel) player.level()).getGameTime();
		UUID id = player.getUUID();
		Map<Moment, Long> said = SAID.computeIfAbsent(id, key -> new HashMap<>());
		if (now - before(said.get(moment), now) < AGAIN || !moment.urgent && now - before(LAST.get(id), now) < gap(data)) {
			return;
		}
		said.put(moment, now);
		LAST.put(id, now);
		int line = moment.lines > 1 ? player.getRandom().nextInt(moment.lines) : 0;
		ExiledPixies.say(player, Component.translatable("exiled.iceandfire_fabric.duel." + moment.key() + (moment.lines > 1 ? "." + line : "")),
			moment.danger ? MessageExiledSpeech.ALARM : MessageExiledSpeech.TALK);
	}

	/** When a line was said, if it was in this world's past (a time ahead of now was another world's: never said here). */
	private static long before(Long at, long now) {
		return at == null || at > now ? -99999L : at;
	}

	/** Ticks between two of her lines in the duel: as much as the player wants her to talk. */
	private static long gap(ExiledPixieData data) {
		return data.talk >= 2 ? 120 : 180;
	}

	/** Out of the game: forgotten. */
	static void forget(UUID id) {
		LAST.remove(id);
		SAID.remove(id);
	}
}

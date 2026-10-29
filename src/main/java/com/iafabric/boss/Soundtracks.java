package com.iafabric.boss;

import com.iafabric.message.MessageMusic;
import com.iafabric.network.IafNetwork;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import org.jspecify.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * The mod's soundtracks, told to each player (their game plays them, Soundtrack on the client): Cryona's duel (it
 * loops while the duel lasts), and the exiled pixie's memories (her glade's vision; Cryona's mourning after Black Frost
 * falls, as long as it lasts). One at a time: a new one starts once the one before has faded out. Each player is told
 * only when theirs changes.
 */
public final class Soundtracks {
	public static final int NONE = 0;
	public static final int DUEL = 1;
	public static final int MEMORIES = 2;
	/** How long the memories' track lasts (ticks), and how long the duel's takes to fade out before the next begins. */
	public static final int MEMORIES_TICKS = 2079;
	public static final int FADE_OUT = 80;
	/** What each player was last told to hear. */
	private static final Map<UUID, Integer> HEARS = new HashMap<>();

	private Soundtracks() {
	}

	public static void register() {
		// (a player who comes back to the game, or back to life, is told again)
		net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents.LEAVE.register(player -> HEARS.remove(player.getUUID()));
		net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents.AFTER_RESPAWN.register((old, player, alive) -> HEARS.remove(player.getUUID()));
		net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents.SERVER_STOPPED.register(server -> HEARS.clear());
	}

	/** What a player hears now, in that world only (null: wherever they are); told only when it changes. */
	public static void hear(ServerPlayer player, int track, @Nullable ResourceKey<Level> world) {
		Integer was = HEARS.get(player.getUUID());
		if (track == NONE) {
			forget(player);
			return;
		}
		if (was != null && was == track) {
			return;
		}
		HEARS.put(player.getUUID(), track);
		IafNetwork.sendToPlayer(new MessageMusic(track, world == null ? "" : world.identifier().toString()), player);
	}

	/** Their track (if any) fades out. */
	public static void forget(ServerPlayer player) {
		if (HEARS.remove(player.getUUID()) != null) {
			IafNetwork.sendToPlayer(new MessageMusic(NONE, ""), player);
		}
	}

	/** For the tests: what a player was told to hear. */
	public static int told(ServerPlayer player) {
		return HEARS.getOrDefault(player.getUUID(), NONE);
	}
}

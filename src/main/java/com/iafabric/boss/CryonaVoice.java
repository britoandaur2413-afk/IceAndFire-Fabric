package com.iafabric.boss;

import com.iafabric.misc.IafSoundRegistry;
import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSoundPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Cryona's voice: an exile forgotten by the world, she speaks a tongue nobody knows any more. A line for each moment of
 * her fight, from the gate of her citadel to her last words. Each is heard by the players near her (or of her fight)
 * wherever they stand, coming from her side, and the words of the longer ones are written to them as she says them.
 * She never talks over herself.
 */
public final class CryonaVoice {
	/** Her lines: the sound, how long it lasts (ticks) and whether its words are written. */
	public enum Line {
		ENTRY(IafSoundRegistry.CRYONA_ENTRY, 96, true),
		WAKE(IafSoundRegistry.CRYONA_WAKE, 92, true),
		CAST1(IafSoundRegistry.CRYONA_CAST1, 36, false),
		CAST2(IafSoundRegistry.CRYONA_CAST2, 30, false),
		CAST3(IafSoundRegistry.CRYONA_CAST3, 46, false),
		ZERO(IafSoundRegistry.CRYONA_ZERO, 54, true),
		LAMENT(IafSoundRegistry.CRYONA_LAMENT, 106, true),
		VENGEANCE(IafSoundRegistry.CRYONA_VENGEANCE, 54, true),
		FALTER(IafSoundRegistry.CRYONA_FALTER, 64, true),
		DEATH(IafSoundRegistry.CRYONA_DEATH, 164, true),
		LOST(IafSoundRegistry.CRYONA_LOST, 52, true),
		TAUNT(IafSoundRegistry.CRYONA_TAUNT, 22, false),
		SOB1(IafSoundRegistry.CRYONA_SOB1, 80, true),
		SOB2(IafSoundRegistry.CRYONA_SOB2, 100, true),
		ANGER(IafSoundRegistry.CRYONA_ANGER, 82, true),
		FURY(IafSoundRegistry.CRYONA_FURY, 76, true);

		final SoundEvent sound;
		final int ticks;
		final boolean written;

		Line(SoundEvent sound, int ticks, boolean written) {
			this.sound = sound;
			this.ticks = ticks;
			this.written = written;
		}

		/** Her words for this line (the same in every language: nobody knows hers). */
		public String key() {
			return "iceandfire_fabric.cryona.line." + name().toLowerCase(java.util.Locale.ROOT);
		}
	}

	/** How far from her a player hears her (outside her realm). */
	private static final int HEARD = 64;
	private static final Map<UUID, Long> QUIET_AT = new HashMap<>();

	private CryonaVoice() {
	}

	/** Whether she is saying something now. */
	public static boolean talking(ServerLevel level, Entity speaker) {
		Long quiet = QUIET_AT.get(speaker.getUUID());
		return quiet != null && level.getGameTime() < quiet;
	}

	/** She says a line (unless she is saying another one; a line that must be said cuts in). */
	public static boolean say(ServerLevel level, Entity speaker, Line line, boolean cutIn) {
		if (!cutIn && talking(level, speaker)) {
			return false;
		}
		QUIET_AT.put(speaker.getUUID(), level.getGameTime() + line.ticks);
		if (QUIET_AT.size() > 64) {
			QUIET_AT.values().removeIf(until -> until < level.getGameTime());
		}
		for (ServerPlayer player : hearers(level, speaker)) {
			sound(player, speaker.position(), line);
			if (line.written) {
				player.sendSystemMessage(Component.translatable("iceandfire_fabric.cryona.says",
					Component.translatable(line.key()).withStyle(ChatFormatting.ITALIC)).withStyle(ChatFormatting.DARK_AQUA));
			}
		}
		return true;
	}

	/** A line heard by one player only, from a place (her citadel's gate: her voice from deep inside). */
	public static void sayTo(ServerPlayer player, Vec3 from, Line line) {
		sound(player, from, line);
		if (line.written) {
			player.sendSystemMessage(Component.translatable("iceandfire_fabric.cryona.says",
				Component.translatable(line.key()).withStyle(ChatFormatting.ITALIC)).withStyle(ChatFormatting.DARK_AQUA));
		}
	}

	/** Played a few blocks from the player on her side: always heard well, from where she is. */
	private static void sound(ServerPlayer player, Vec3 from, Line line) {
		Vec3 ear = player.getEyePosition();
		Vec3 way = from.subtract(ear);
		Vec3 at = way.lengthSqr() < 16 ? from : ear.add(way.normalize().scale(4));
		player.connection.send(new ClientboundSoundPacket(BuiltInRegistries.SOUND_EVENT.wrapAsHolder(line.sound), SoundSource.HOSTILE, at.x, at.y, at.z,
			1.6F, 1F, player.getRandom().nextLong()));
	}

	private static List<ServerPlayer> hearers(ServerLevel level, Entity speaker) {
		if (ThroneFight.inRealm(speaker)) {
			ThroneFight.Fight fight = ThroneFight.fightAt(speaker);
			if (fight != null) {
				return ThroneFight.playersOf(level, fight);
			}
		}
		return level.getPlayers(player -> player.distanceToSqr(speaker) < HEARD * HEARD);
	}
}

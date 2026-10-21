package com.iafabric.client.sound;

import com.iafabric.boss.Soundtracks;
import com.iafabric.message.MessageMusic;
import com.iafabric.misc.IafSoundRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import org.jspecify.annotations.Nullable;

/**
 * The mod's soundtracks as the player hears them: one at a time, never over another one nor over the game's own music
 * (which waits while one is wanted, MusicManagerMixin). A new one starts only once the one before has faded out; a
 * track that does not loop is not started again when it ends (one the game cut short, changing the player's world,
 * is). It ends (fading) when the player falls or is no longer in the world it belongs to, and at once when they leave
 * the game.
 */
public final class Soundtrack {
	/** Ticks to fade in; to fade out the memories' track; to fade out when the player falls or leaves its world. */
	private static final int FADE_IN = 30;
	private static final int MEMORIES_OUT = 40;
	private static final int QUICK = 20;
	/** Ticks a track is given to begin before its sound is asked whether it still plays. */
	private static final int STARTING = 20;
	private static @Nullable Track playing;
	private static int wanted = Soundtracks.NONE;
	private static @Nullable Identifier where;
	/** The track asked for that played to its end (not looping): not again till another is asked for. */
	private static int ended = Soundtracks.NONE;

	private Soundtrack() {
	}

	public static void handle(MessageMusic message) {
		where = message.world.isEmpty() ? null : Identifier.tryParse(message.world);
		if (message.track != wanted) {
			ended = Soundtracks.NONE;
		}
		wanted = message.track;
	}

	/** Whether one of its tracks plays, fades or is wanted: the game's own music waits. */
	public static boolean active() {
		return playing != null || wanted != Soundtracks.NONE;
	}

	/** For the tests: the track playing (not fading out), or none. */
	public static int playing() {
		return playing == null || playing.leaving ? Soundtracks.NONE : playing.track;
	}

	/** For the tests: whether the sound engine truly plays the track (not fading out). */
	public static boolean audible() {
		return playing != null && !playing.leaving && Minecraft.getInstance().getSoundManager().isActive(playing);
	}

	/** For the tests: its state, in words. */
	public static String describe() {
		Track now = playing;
		Minecraft mc = Minecraft.getInstance();
		return "wanted " + wanted + ", ended " + ended + ", where " + where + (now == null ? ", nothing playing" : ", playing " + now.track + " (age " + now.age
			+ ", leaving " + now.leaving + ", volume " + now.getVolume() + ", stopped " + now.isStopped() + ", active " + mc.getSoundManager().isActive(now) + ")")
			+ ", player alive " + (mc.player != null && mc.player.isAlive()) + ", paused " + mc.isPaused();
	}

	/** Out of the game: silence at once. */
	public static void clear() {
		if (playing != null) {
			Minecraft.getInstance().getSoundManager().stop(playing);
		}
		playing = null;
		wanted = Soundtracks.NONE;
		where = null;
		ended = Soundtracks.NONE;
	}

	public static void tick() {
		Minecraft mc = Minecraft.getInstance();
		int want = wanted;
		Track now = playing;
		boolean out = mc.level == null || mc.player == null;
		// fallen, or no longer in the world its track belongs to: its time is over, at once
		boolean dead = !out && !mc.player.isAlive();
		Identifier here = out ? null : mc.level.dimension().identifier();
		boolean away = !out && (where != null && !here.equals(where) || now != null && now.world != null && !here.equals(now.world));
		if (out) {
			want = Soundtracks.NONE;
		} else if (want != Soundtracks.NONE && (dead || where != null && !here.equals(where))) {
			want = Soundtracks.NONE;
			wanted = Soundtracks.NONE;
		}
		boolean quick = dead || away;
		if (now != null) {
			// (counted here: a track the sound engine never took, its music turned off, is never ticked by it)
			now.age++;
			boolean gone = now.isStopped() || now.age > STARTING && !mc.getSoundManager().isActive(now);
			if (gone) {
				// (played to its end: not again; cut short, as the game silences everything when its world changes: again)
				if (!now.leaving && !now.looping() && now.age >= now.length - 60) {
					ended = now.track;
				}
				playing = null;
				now = null;
			} else if (now.track != want && (!now.leaving || quick)) {
				now.leave(quick ? QUICK : now.track == Soundtracks.MEMORIES ? MEMORIES_OUT : Soundtracks.FADE_OUT);
			} else if (now.track == want && now.leaving) {
				// (wanted again while it faded: it comes back)
				now.leaving = false;
			}
		}
		boolean heard = mc.options.getSoundSourceVolume(SoundSource.MASTER) > 0F && mc.options.getSoundSourceVolume(SoundSource.MUSIC) > 0F;
		if (now == null && want != Soundtracks.NONE && want != ended && heard) {
			SoundEvent sound = want == Soundtracks.DUEL ? IafSoundRegistry.MUSIC_CRYONA_DUEL : IafSoundRegistry.MUSIC_FAIRY_MEMORIES;
			// (the game's own music stops for it)
			mc.getMusicManager().stopPlaying();
			playing = new Track(want, sound, want == Soundtracks.DUEL);
			playing.world = where;
			playing.length = want == Soundtracks.MEMORIES ? Soundtracks.MEMORIES_TICKS : 0;
			mc.getSoundManager().play(playing);
		}
	}

	/** A track: fading in, playing (looping or not), fading out when it must give way. */
	private static final class Track extends AbstractTickableSoundInstance {
		final int track;
		/** The world it belongs to (none: wherever the player is); how long it lasts (ticks, a track that does not loop). */
		@Nullable Identifier world;
		int length;
		int age;
		boolean leaving;
		private float step;

		Track(int track, SoundEvent sound, boolean loop) {
			super(sound, SoundSource.MUSIC, SoundInstance.createUnseededRandom());
			this.track = track;
			this.looping = loop;
			this.delay = 0;
			this.relative = true;
			this.attenuation = SoundInstance.Attenuation.NONE;
			this.volume = 0.01F;
		}

		boolean looping() {
			return this.looping;
		}

		/** It fades out in so many ticks (or sooner, if it already was). */
		void leave(int ticks) {
			float faster = Math.max(0.01F, this.volume) / Math.max(1, ticks);
			step = leaving ? Math.max(step, faster) : faster;
			leaving = true;
		}

		@Override
		public void tick() {
			if (leaving) {
				this.volume -= step;
				if (this.volume <= 0.005F) {
					this.volume = 0F;
					this.stop();
				}
			} else if (this.volume < 1F) {
				this.volume = Math.min(1F, this.volume + 1F / FADE_IN);
			}
		}
	}
}

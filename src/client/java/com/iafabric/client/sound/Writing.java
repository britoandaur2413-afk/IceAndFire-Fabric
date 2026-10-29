package com.iafabric.client.sound;

import com.iafabric.misc.IafSoundRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.sounds.SoundSource;
import org.jetbrains.annotations.Nullable;

/**
 * A quill writing by magic, softly, while the words of a memory's subtitles are being written out (ExiledPixieHud):
 * the player hears that there is something to read. One at a time; it fades in and out with the writing.
 */
public final class Writing {
	private static final float VOLUME = 0.45F;
	private static @Nullable Quill quill;

	private Writing() {
	}

	/** Each tick: whether words are being written now. */
	public static void tick(boolean writing) {
		Minecraft mc = Minecraft.getInstance();
		if (quill != null && (quill.isStopped() || quill.age > 10 && !mc.getSoundManager().isActive(quill))) {
			quill = null;
		}
		if (quill != null) {
			quill.age++;
			quill.writing = writing;
		} else if (writing && mc.options.getSoundSourceVolume(SoundSource.MASTER) > 0F && mc.options.getSoundSourceVolume(SoundSource.MASTER) > 0F) {
			quill = new Quill();
			mc.getSoundManager().play(quill);
		}
	}

	/** Out of the memory: silence at once. */
	public static void clear() {
		if (quill != null) {
			Minecraft.getInstance().getSoundManager().stop(quill);
		}
		quill = null;
	}

	/** The quill's sound: looping while the words come, fading out once they are written. */
	private static final class Quill extends AbstractTickableSoundInstance {
		int age;
		boolean writing = true;

		Quill() {
			super(IafSoundRegistry.MEMORY_WRITING, SoundSource.MASTER, SoundInstance.createUnseededRandom());
			this.looping = true;
			this.delay = 0;
			this.relative = true;
			this.attenuation = SoundInstance.Attenuation.NONE;
			this.volume = 0.05F;
		}

		@Override
		public void tick() {
			if (writing) {
				this.volume = Math.min(VOLUME, this.volume + VOLUME / 3F);
			} else {
				this.volume -= VOLUME / 5F;
				if (this.volume <= 0.01F) {
					this.volume = 0F;
					this.stop();
				}
			}
		}
	}
}

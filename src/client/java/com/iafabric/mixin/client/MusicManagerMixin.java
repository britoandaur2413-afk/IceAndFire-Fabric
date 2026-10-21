package com.iafabric.mixin.client;

import com.iafabric.client.sound.Soundtrack;
import net.minecraft.client.sounds.MusicManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** While one of the mod's soundtracks plays, the game's own music waits (never two musics at once). */
@Mixin(MusicManager.class)
public class MusicManagerMixin {
	@Inject(method = "tick", at = @At("HEAD"), cancellable = true)
	private void iceandfire_fabric$waitForSoundtrack(CallbackInfo ci) {
		if (Soundtrack.active()) {
			ci.cancel();
		}
	}
}

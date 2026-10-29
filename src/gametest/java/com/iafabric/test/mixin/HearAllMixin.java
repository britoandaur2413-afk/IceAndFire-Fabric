package com.iafabric.test.mixin;

import net.minecraft.client.sounds.SoundEngine;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * The scenes' sound listeners hear what plays far from the camera too, as in later games: 1.20.1 tells them only of
 * sounds within hearing of the camera, and the scenes move the camera to watch (a voice was missed or not by chance).
 */
@Mixin(SoundEngine.class)
public class HearAllMixin {
	@Redirect(method = "play", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/phys/Vec3;distanceToSqr(Lnet/minecraft/world/phys/Vec3;)D"))
	private double iceandfire_fabric$withinHearing(Vec3 listener, Vec3 sound) {
		return 0;
	}
}

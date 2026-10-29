package com.iafabric.mixin.client;

import net.minecraft.client.Minecraft;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * The swirls of the player's own effects are not drawn in first person: they rose right before the eyes, big and dark
 * (the slowness and the frost wound of Black Frost's cold), flickering over the warning circles of the fight. Others
 * still see them on the player, and so does the player in third person.
 */
@Mixin(LivingEntity.class)
public abstract class OwnEffectParticlesMixin {
	@Redirect(method = "tickEffects", at = @At(value = "INVOKE",
		target = "Lnet/minecraft/world/level/Level;addParticle(Lnet/minecraft/core/particles/ParticleOptions;DDDDDD)V"))
	private void iceandfire_fabric$notBeforeTheEyes(Level level, ParticleOptions particle, double x, double y, double z, double dx, double dy, double dz) {
		Minecraft mc = Minecraft.getInstance();
		if ((Object) this == mc.getCameraEntity() && mc.options.getCameraType().isFirstPerson()) {
			return;
		}
		level.addParticle(particle, x, y, z, dx, dy, dz);
	}
}

package com.iafabric.mixin;

import com.iafabric.event.ServerEvents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LightningBolt;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Entity.class)
public abstract class EntityThunderMixin {
	@Inject(method = "thunderHit", at = @At("HEAD"), cancellable = true)
	private void iaf$thunderHit(ServerLevel level, LightningBolt lightning, CallbackInfo ci) {
		if (ServerEvents.cancelLightningHit((Entity) (Object) this, lightning)) {
			ci.cancel();
		}
	}
}

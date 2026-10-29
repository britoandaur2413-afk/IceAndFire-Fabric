package com.iafabric.mixin;

import com.iafabric.entity.EntityDragonBase;
import com.iafabric.event.ServerEvents;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Mob.class)
public abstract class MobMixin {
	@Inject(method = "setTarget", at = @At("TAIL"))
	private void iaf$setTarget(LivingEntity target, CallbackInfo ci) {
		if (target != null) {
			ServerEvents.onLivingSetTarget((Mob) (Object) this, target);
		}
	}

	/**
	 * A dragon its boss fight leads (EntityDragonBase.fightLed) has no mind of its own: no senses, goals, path, steps
	 * or looks of its own (the NoAi flag would do the same, but its model then stands stiff).
	 */
	@Inject(method = "serverAiStep", at = @At("HEAD"), cancellable = true)
	private void iaf$ledByItsFight(CallbackInfo ci) {
		if ((Object) this instanceof EntityDragonBase dragon && dragon.fightLed) {
			ci.cancel();
		}
	}
}

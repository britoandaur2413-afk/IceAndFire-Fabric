package com.iafabric.mixin;

import com.iafabric.event.ServerEvents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin {
	@Inject(method = "causeFallDamage", at = @At("HEAD"), cancellable = true)
	private void iaf$causeFallDamage(double fallDistance, float damageModifier, DamageSource source, CallbackInfoReturnable<Boolean> cir) {
		if (ServerEvents.cancelFallDamage((LivingEntity) (Object) this)) {
			cir.setReturnValue(false);
		}
	}

	@ModifyVariable(method = "hurtServer", at = @At("HEAD"), argsOnly = true)
	private float iaf$hurtAmount(float damage, ServerLevel level, DamageSource source) {
		return ServerEvents.modifyDamage((LivingEntity) (Object) this, source, damage);
	}

	@Inject(method = "tick", at = @At("TAIL"))
	private void iaf$tick(CallbackInfo ci) {
		ServerEvents.onLivingTick((LivingEntity) (Object) this);
	}

	/** Cauterized by Black Frost's ice: no life comes back (BossCold.wounded), whatever heals. */
	@Inject(method = "heal", at = @At("HEAD"), cancellable = true)
	private void iaf$heal(float amount, CallbackInfo ci) {
		if (com.iafabric.boss.BossCold.noHealing((LivingEntity) (Object) this)) {
			ci.cancel();
		}
	}

	@ModifyVariable(method = "setHealth", at = @At("HEAD"), argsOnly = true)
	private float iaf$setHealth(float health) {
		LivingEntity self = (LivingEntity) (Object) this;
		return com.iafabric.boss.BossCold.noHealing(self) && health > self.getHealth() ? self.getHealth() : health;
	}
}

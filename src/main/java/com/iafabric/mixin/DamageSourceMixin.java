package com.iafabric.mixin;

import com.iafabric.IafMod;
import net.minecraft.network.chat.Component;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** The mod's damage kinds pick one of their death messages at random, like the 1.18 mod ("death.attack.dragon_fire.0", ".attacker_0"...). */
@Mixin(DamageSource.class)
public abstract class DamageSourceMixin {
	@Inject(method = "getLocalizedDeathMessage", at = @At("HEAD"), cancellable = true)
	private void iceandfire_fabric$randomDeathMessage(LivingEntity victim, CallbackInfoReturnable<Component> cir) {
		DamageSource self = (DamageSource) (Object) this;
		if (!self.typeHolder().unwrapKey().map(key -> key.identifier().getNamespace().equals(IafMod.MOD_ID)).orElse(false)) {
			return;
		}
		LivingEntity killer = victim.getKillCredit();
		String base = "death.attack." + self.getMsgId();
		int index = victim.getRandom().nextInt(3);
		cir.setReturnValue(killer != null
			? Component.translatable(base + ".attacker_" + index, victim.getDisplayName(), killer.getDisplayName())
			: Component.translatable(base + "." + index, victim.getDisplayName()));
	}
}

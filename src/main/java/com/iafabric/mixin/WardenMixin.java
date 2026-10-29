package com.iafabric.mixin;

import com.iafabric.entity.EntityStoneStatue;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.monster.warden.Warden;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** The warden goes by vibrations and smell: a stone statue is neither, it is not a target. */
@Mixin(Warden.class)
public class WardenMixin {
	@Inject(method = "canTargetEntity", at = @At("HEAD"), cancellable = true)
	private void iceandfire_fabric$notStatues(Entity entity, CallbackInfoReturnable<Boolean> cir) {
		if (entity instanceof EntityStoneStatue) {
			cir.setReturnValue(false);
		}
	}
}

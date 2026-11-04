package com.iafabric.test.mixin;

import com.iafabric.IafMod;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Who turns an entity to a NaN rotation (the game only says "Invalid entity rotation"): its type and the calls. */
@Mixin(Entity.class)
public abstract class NanRotationMixin {
	@Inject(method = {"setYRot", "setXRot"}, at = @At("HEAD"))
	private void iafTest$nanRotation(float rotation, CallbackInfo ci) {
		if (!Float.isFinite(rotation)) {
			IafMod.LOGGER.warn("[IAFNAN] NaN rotation on {}", ((Entity) (Object) this).getType(), new Throwable());
		}
	}
}

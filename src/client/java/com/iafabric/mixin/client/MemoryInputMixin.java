package com.iafabric.mixin.client;

import com.iafabric.client.gui.MemoryOverlay;
import net.minecraft.client.player.Input;
import net.minecraft.client.player.KeyboardInput;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** In a memory of their exiled pixie the player stands still: no step, jump, crouch nor sprint (MemoryOverlay). */
@Mixin(KeyboardInput.class)
public abstract class MemoryInputMixin extends Input {
	@Inject(method = "tick", at = @At("TAIL"))
	private void iceandfire_fabric$standStill(boolean slow, float slowFactor, CallbackInfo ci) {
		if (MemoryOverlay.active()) {
			this.up = false;
			this.down = false;
			this.left = false;
			this.right = false;
			this.jumping = false;
			this.shiftKeyDown = false;
			this.forwardImpulse = 0.0F;
			this.leftImpulse = 0.0F;
		}
	}
}

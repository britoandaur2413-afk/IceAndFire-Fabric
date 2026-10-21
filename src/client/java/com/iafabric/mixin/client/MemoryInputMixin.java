package com.iafabric.mixin.client;

import com.iafabric.client.gui.MemoryOverlay;
import net.minecraft.client.player.ClientInput;
import net.minecraft.client.player.KeyboardInput;
import net.minecraft.world.entity.player.Input;
import net.minecraft.world.phys.Vec2;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** In a memory of their exiled pixie the player stands still: no step, jump, crouch nor sprint (MemoryOverlay). */
@Mixin(KeyboardInput.class)
public abstract class MemoryInputMixin extends ClientInput {
	@Inject(method = "tick", at = @At("TAIL"))
	private void iceandfire_fabric$standStill(CallbackInfo ci) {
		if (MemoryOverlay.active()) {
			this.keyPresses = Input.EMPTY;
			this.moveVector = Vec2.ZERO;
		}
	}
}

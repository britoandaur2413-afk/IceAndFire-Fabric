package com.iafabric.test.mixin;

import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Reads the id of every entity drawn, as other mods do (Forgeworks Charms' Hunter Vision crashed a player's game on the
 * exiled pixie's picture): 26.x gives no id to an entity the client makes itself, so whatever the mod makes only to draw
 * must get one. In every client test.
 */
@Mixin(EntityRenderer.class)
abstract class DrawnEntityIdMixin {
	@Inject(method = "createRenderState(Lnet/minecraft/world/entity/Entity;F)Lnet/minecraft/client/renderer/entity/state/EntityRenderState;", at = @At("HEAD"))
	private void iceandfire_fabric_test$readId(Entity entity, float partialTicks, CallbackInfoReturnable<?> cir) {
		entity.getId();
	}
}

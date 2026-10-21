package com.iafabric.mixin.client;

import com.iafabric.client.render.entity.layer.LayerLivingExtras;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Keeps the mob in its render state, for what the mod draws over any mob (ice, chains, the cockatrice's gaze). */
@Mixin(LivingEntityRenderer.class)
public class LivingEntityRendererMixin {
	@Inject(method = "extractRenderState(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/client/renderer/entity/state/LivingEntityRenderState;F)V", at = @At("TAIL"))
	private void iceandfire_fabric$keepEntity(LivingEntity entity, LivingEntityRenderState state, float partialTicks, CallbackInfo ci) {
		state.setData(LayerLivingExtras.ENTITY, entity);
		state.setData(LayerLivingExtras.PARTIAL_TICK, partialTicks);
	}
}

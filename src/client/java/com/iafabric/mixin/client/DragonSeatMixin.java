package com.iafabric.mixin.client;

import com.iafabric.client.render.entity.DragonSeat;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** A dragon's rider is drawn by the dragon, in the seat on its back (RenderDragonBase), not where the game would. */
@Mixin(EntityRenderDispatcher.class)
public class DragonSeatMixin {
	@Inject(method = "shouldRender", at = @At("HEAD"), cancellable = true)
	private <E extends Entity> void iceandfire_fabric$drawnByItsDragon(E entity, Frustum frustum, double x, double y, double z, float partialTicks, CallbackInfoReturnable<Boolean> cir) {
		if (DragonSeat.seated(entity)) {
			cir.setReturnValue(false);
		}
	}
}

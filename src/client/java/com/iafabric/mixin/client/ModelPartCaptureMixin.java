package com.iafabric.mixin.client;

import com.iafabric.citadel.client.render.CapturedGeometry;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.geom.ModelPart;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

/**
 * A game model (armor, a trident...) drawn into the mod's {@link CapturedGeometry} keeps its cubes there with their
 * pose, to be made in the real buffer when the frame is drawn, where Sodium makes them its fast way.
 */
@Mixin(ModelPart.class)
public class ModelPartCaptureMixin {
	@Shadow
	@Final
	private List<ModelPart.Cube> cubes;

	@Inject(method = "compile(Lcom/mojang/blaze3d/vertex/PoseStack$Pose;Lcom/mojang/blaze3d/vertex/VertexConsumer;III)V", at = @At("HEAD"), cancellable = true)
	private void iceandfire_fabric$keepCubes(PoseStack.Pose pose, VertexConsumer builder, int light, int overlay, int color, CallbackInfo ci) {
		if (builder instanceof CapturedGeometry captured) {
			captured.cubes(pose, this.cubes, light, overlay, color);
			ci.cancel();
		}
	}
}

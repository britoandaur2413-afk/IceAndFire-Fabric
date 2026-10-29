package com.iafabric.citadel.client.render;

import com.iafabric.citadel.client.model.basic.BasicEntityModel;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;


/** Draws the mod's own models into the game's buffers. */
public final class ModelRendering {
	private ModelRendering() {
	}

	public static float partialTick() {
		return Minecraft.getInstance().getFrameTime();
	}

	/** Draws a model that was just posed, with the pose stack as it is now. */
	public static void submit(BasicEntityModel<?> model, PoseStack poseStack, MultiBufferSource buffers, RenderType renderType, int light,
		int overlay, int color) {
		model.renderToBuffer(poseStack, buffers.getBuffer(renderType), light, overlay, color);
	}

	/** Draws what {@code drawer} draws now, in the current pose. */
	public static void submit(PoseStack poseStack, MultiBufferSource buffers, RenderType renderType, Drawer drawer) {
		drawer.draw(poseStack, buffers.getBuffer(renderType));
	}

	@FunctionalInterface
	public interface Drawer {
		void draw(PoseStack poseStack, VertexConsumer buffer);
	}
}

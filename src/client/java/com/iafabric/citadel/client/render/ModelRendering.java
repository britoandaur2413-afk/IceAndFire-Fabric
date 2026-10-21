package com.iafabric.citadel.client.render;

import com.iafabric.citadel.client.model.basic.BasicEntityModel;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderType;

import java.util.function.Consumer;

/** Draws the mod's own models in the 26.x submit pipeline. */
public final class ModelRendering {
	private ModelRendering() {
	}

	public static float partialTick() {
		return Minecraft.getInstance().getDeltaTracker().getGameTimeDeltaPartialTick(false);
	}

	/** Draws a model that was just posed, with the pose stack as it is now. */
	public static void submit(BasicEntityModel<?> model, PoseStack poseStack, SubmitNodeCollector collector, RenderType renderType, int light,
		int overlay, int color) {
		submit(poseStack, collector, renderType, (stack, buffer) -> model.renderToBuffer(stack, buffer, light, overlay, color));
	}

	/** Records what {@code drawer} draws now, in the current pose, and hands it to the collector. */
	public static void submit(PoseStack poseStack, SubmitNodeCollector collector, RenderType renderType, Drawer drawer) {
		CapturedGeometry captured = new CapturedGeometry();
		drawer.draw(poseStack, captured);
		if (captured.isEmpty()) {
			return;
		}
		PoseStack identity = new PoseStack();
		collector.submitCustomGeometry(identity, renderType, (pose, buffer) -> captured.replay(buffer));
	}

	public static void submit(PoseStack poseStack, SubmitNodeCollector collector, RenderType renderType, Consumer<CapturedGeometry> drawer) {
		submit(poseStack, collector, renderType, (stack, buffer) -> drawer.accept(buffer));
	}

	@FunctionalInterface
	public interface Drawer {
		void draw(PoseStack poseStack, CapturedGeometry buffer);
	}
}

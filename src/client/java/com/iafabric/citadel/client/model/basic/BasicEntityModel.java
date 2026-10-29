package com.iafabric.citadel.client.model.basic;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.util.FastColor;
import net.minecraft.world.entity.Entity;

/**
 * A model made of {@link BasicModelPart}s, animated from the entity itself (replaces Citadel's
 * BasicEntityModel). Drawn by {@link com.iafabric.citadel.client.render.ModelRendering}.
 */
public abstract class BasicEntityModel<T extends Entity> {
	public int texWidth = 64;
	public int texHeight = 32;
	public float attackTime;
	public boolean riding;
	public boolean young = false;

	public abstract void setupAnim(T entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch);

	public void prepareMobModel(T entity, float limbSwing, float limbSwingAmount, float partialTick) {
	}

	public abstract Iterable<BasicModelPart> parts();

	public void renderToBuffer(PoseStack poseStack, VertexConsumer buffer, int light, int overlay, int color) {
		for (BasicModelPart part : parts()) {
			part.render(poseStack, buffer, light, overlay, color);
		}
	}

	public void renderToBuffer(PoseStack poseStack, VertexConsumer buffer, int light, int overlay, float red, float green, float blue, float alpha) {
		renderToBuffer(poseStack, buffer, light, overlay, FastColor.ARGB32.colorFromFloat(alpha, red, green, blue));
	}
}

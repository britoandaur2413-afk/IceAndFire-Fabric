package com.iafabric.client.render.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;

/**
 * A part of a great body (a dragon's head, neck, wings and tail, a hydra's heads, a cyclops' eye): its body draws it,
 * so nothing is drawn here, but it is seen like any entity, so its box shows with the others (F3+B) where it is hit.
 */
public class RenderPart<T extends Entity> extends EntityRenderer<T> {

	public RenderPart(EntityRendererProvider.Context context) {
		super(context);
	}

	@Override
	public ResourceLocation getTextureLocation(T entity) {
		return TextureAtlas.LOCATION_BLOCKS;
	}

	@Override
	public void render(T entity, float entityYaw, float partialTicks, PoseStack poseStack, MultiBufferSource buffers, int light) {
		// no model, no name, no flames: its body shows it
	}
}

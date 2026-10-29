package com.iafabric.client.render.entity;

import com.iafabric.client.render.BufferSource;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;

/**
 * An entity (projectile, knot, skull...) drawn by hand the way 1.18 renderers did: render() gets the
 * entity, the partial tick, the pose at the entity and buffers by render type.
 */
public abstract class IafEntityRenderer<T extends Entity> extends EntityRenderer<T> {

	protected IafEntityRenderer(EntityRendererProvider.Context context) {
		super(context);
	}

	@Override
	public ResourceLocation getTextureLocation(T entity) {
		return null;
	}

	/** Draws the entity; the pose is at the entity's position. */
	public void render(T entity, float entityYaw, float partialTicks, PoseStack poseStack, BufferSource buffers, int light) {
	}

	@Override
	public void render(T entity, float entityYaw, float partialTicks, PoseStack poseStack, MultiBufferSource buffers, int light) {
		this.render(entity, entityYaw, partialTicks, poseStack, new BufferSource(buffers), light);
		super.render(entity, entityYaw, partialTicks, poseStack, buffers, light);
	}
}

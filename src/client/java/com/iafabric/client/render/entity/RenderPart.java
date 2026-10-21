package com.iafabric.client.render.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.world.entity.Entity;

/**
 * A part of a great body (a dragon's head, neck, wings and tail, a hydra's heads, a cyclops' eye): its body draws it,
 * so nothing is drawn here, but it is seen like any entity, so its box shows with the others (F3+B) where it is hit.
 * (It was never seen at all: no part's box ever showed.)
 */
public class RenderPart<T extends Entity> extends EntityRenderer<T, EntityRenderState> {

	public RenderPart(EntityRendererProvider.Context context) {
		super(context);
	}

	@Override
	public EntityRenderState createRenderState() {
		return new EntityRenderState();
	}

	@Override
	public void submit(EntityRenderState state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
		// no model, no name, no flames: its body shows it
	}
}

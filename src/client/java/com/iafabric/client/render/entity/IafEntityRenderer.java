package com.iafabric.client.render.entity;

import com.iafabric.client.render.BufferSource;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;
import org.jspecify.annotations.Nullable;

/**
 * An entity (projectile, knot, skull...) drawn by hand the way 1.18 renderers did: render() gets the
 * entity, the partial tick, the pose at the entity and buffers by render type.
 */
public abstract class IafEntityRenderer<T extends Entity> extends EntityRenderer<T, IafEntityRenderer.State<T>> {

	protected IafEntityRenderer(EntityRendererProvider.Context context) {
		super(context);
	}

	public @Nullable Identifier getTextureLocation(T entity) {
		return null;
	}

	/** Draws the entity; the pose is at the entity's position. */
	public void render(T entity, float entityYaw, float partialTicks, PoseStack poseStack, BufferSource buffers, int light) {
	}

	@Override
	public State<T> createRenderState() {
		return new State<>();
	}

	@Override
	public void extractRenderState(T entity, State<T> state, float partialTicks) {
		super.extractRenderState(entity, state, partialTicks);
		state.entity = entity;
		state.partialTick = partialTicks;
		state.shadowRadius = this.shadowRadius;
	}

	@Override
	public void submit(State<T> state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
		if (state.entity != null) {
			BufferSource buffers = new BufferSource(collector).withCamera(camera);
			float yaw = state.entity.getYRot(state.partialTick);
			this.render(state.entity, yaw, state.partialTick, poseStack, buffers, state.lightCoords);
			buffers.flush();
		}
		super.submit(state, poseStack, collector, camera);
	}

	public static class State<T extends Entity> extends EntityRenderState {
		public @Nullable T entity;
		public float partialTick;
	}
}

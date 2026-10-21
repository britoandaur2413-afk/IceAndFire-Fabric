package com.iafabric.client.render.tile;

import com.iafabric.client.render.BufferSource;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * A block entity drawn by hand the way 1.18 renderers did: render() gets the block entity, the partial
 * tick, the pose at the block's corner and buffers by render type.
 */
public abstract class IafBlockEntityRenderer<T extends BlockEntity> implements BlockEntityRenderer<T, IafBlockEntityRenderer.State<T>> {

	public abstract void render(T entity, float partialTicks, PoseStack poseStack, BufferSource buffers, int light, int overlay);

	@Override
	public State<T> createRenderState() {
		return new State<>();
	}

	@Override
	public void extractRenderState(T blockEntity, State<T> state, float partialTicks, Vec3 cameraPosition,
		ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress) {
		BlockEntityRenderer.super.extractRenderState(blockEntity, state, partialTicks, cameraPosition, breakProgress);
		state.blockEntity = blockEntity;
		state.partialTick = partialTicks;
	}

	@Override
	public void submit(State<T> state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
		if (state.blockEntity == null) {
			return;
		}
		BufferSource buffers = new BufferSource(collector).withCamera(camera);
		this.render(state.blockEntity, state.partialTick, poseStack, buffers, state.lightCoords, OverlayTexture.NO_OVERLAY);
		buffers.flush();
	}

	public static class State<T extends BlockEntity> extends BlockEntityRenderState {
		public @Nullable T blockEntity;
		public float partialTick;
	}
}

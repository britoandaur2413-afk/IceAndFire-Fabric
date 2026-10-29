package com.iafabric.client.render.tile;

import com.iafabric.client.render.BufferSource;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.world.level.block.entity.BlockEntity;

/**
 * A block entity drawn by hand the way 1.18 renderers did: render() gets the block entity, the partial
 * tick, the pose at the block's corner and buffers by render type.
 */
public abstract class IafBlockEntityRenderer<T extends BlockEntity> implements BlockEntityRenderer<T> {

	public abstract void render(T entity, float partialTicks, PoseStack poseStack, BufferSource buffers, int light, int overlay);

	@Override
	public void render(T entity, float partialTicks, PoseStack poseStack, MultiBufferSource buffers, int light, int overlay) {
		this.render(entity, partialTicks, poseStack, new BufferSource(buffers), light, overlay);
	}
}

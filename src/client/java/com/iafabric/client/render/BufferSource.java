package com.iafabric.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.entity.Entity;

/**
 * Buffers by render type, the way the mod's renderers draw (getBuffer(type) then vertices in the current pose). In
 * 1.21.1 it is the game's own buffers; the 26.x version records the vertices for its collector.
 */
public final class BufferSource {
	private final MultiBufferSource buffers;

	public BufferSource(MultiBufferSource buffers) {
		this.buffers = buffers;
	}

	/** The game's buffers, for what draws itself (items, blocks, text). */
	public MultiBufferSource buffers() {
		return buffers;
	}

	/** Draws another entity here, through its own renderer (a mob in a spawner, a rider...). */
	public void renderEntity(Entity entity, float partialTicks, PoseStack poseStack, int light) {
		Minecraft.getInstance().getEntityRenderDispatcher().render(entity, 0.0D, 0.0D, 0.0D, entity.getYRot(), partialTicks, poseStack, buffers, light);
	}

	public VertexConsumer getBuffer(RenderType type) {
		return buffers.getBuffer(type);
	}

	/** Nothing to hand over: 1.21.1 draws straight into the game's buffers. */
	public void flush() {
	}
}

package com.iafabric.client.render;

import com.iafabric.citadel.client.render.CapturedGeometry;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderType;

import java.util.LinkedHashMap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.world.entity.Entity;
import org.jspecify.annotations.Nullable;
import java.util.Map;

/**
 * Buffers by render type, the way 1.18 renderers drew (getBuffer(type) then vertices in the current pose).
 * The vertices are recorded as they come and handed to the 26.x collector by {@link #flush()}.
 */
public final class BufferSource {
	private final SubmitNodeCollector collector;
	private final Map<RenderType, CapturedGeometry> buffers = new LinkedHashMap<>();
	private @Nullable CameraRenderState camera;

	public BufferSource(SubmitNodeCollector collector) {
		this.collector = collector;
	}

	/** The collector, for what draws itself in 26.x (items, blocks, text). */
	public SubmitNodeCollector collector() {
		return collector;
	}

	public BufferSource withCamera(@Nullable CameraRenderState camera) {
		this.camera = camera;
		return this;
	}

	public @Nullable CameraRenderState camera() {
		return camera;
	}

	/** Draws another entity here, through its own renderer (a mob in a spawner, a rider...). */
	@SuppressWarnings("unchecked")
	public void renderEntity(Entity entity, float partialTicks, PoseStack poseStack, int light) {
		if (camera == null) {
			return;
		}
		EntityRenderer<Entity, EntityRenderState> renderer = (EntityRenderer<Entity, EntityRenderState>) Minecraft.getInstance().getEntityRenderDispatcher().getRenderer(entity);
		if (renderer == null) {
			// (nothing can draw it: another mod's creature with no renderer, or the server's own copy of one)
			return;
		}
		EntityRenderState state = renderer.createRenderState(entity, partialTicks);
		state.lightCoords = light;
		renderer.submit(state, poseStack, collector, camera);
	}

	public VertexConsumer getBuffer(RenderType type) {
		return buffers.computeIfAbsent(type, t -> new CapturedGeometry());
	}

	/** Hands everything drawn so far to the collector. */
	public void flush() {
		PoseStack identity = new PoseStack();
		for (Map.Entry<RenderType, CapturedGeometry> entry : buffers.entrySet()) {
			CapturedGeometry geometry = entry.getValue();
			if (!geometry.isEmpty()) {
				collector.submitCustomGeometry(identity, entry.getKey(), (pose, buffer) -> geometry.replay(buffer));
			}
		}
		buffers.clear();
	}
}

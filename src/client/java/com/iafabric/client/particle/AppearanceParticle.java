package com.iafabric.client.particle;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;

/**
 * A creature appearing right in front of the player's eyes for a moment (a siren's charm, a ghost's scare),
 * like the elder guardian's: drawn in the camera's frame, not in the world.
 */
public abstract class AppearanceParticle extends Particle {

	protected AppearanceParticle(ClientLevel level, double x, double y, double z) {
		super(level, x, y, z);
		this.gravity = 0.0F;
	}

	@Override
	public ParticleRenderType getRenderType() {
		return ParticleRenderType.CUSTOM;
	}

	/** Draws the appearance; the pose starts at the camera, already turned the way the camera looks. */
	protected abstract void draw(PoseStack poseStack, Camera camera, float partialTicks, Buffers buffers);

	/** Where an appearance draws: one buffer per render type. */
	public interface Buffers {
		VertexConsumer getBuffer(RenderType type);
	}

	@Override
	public void render(VertexConsumer unused, Camera camera, float partialTicks) {
		PoseStack poseStack = new PoseStack();
		poseStack.mulPose(camera.rotation());
		MultiBufferSource.BufferSource buffers = Minecraft.getInstance().renderBuffers().bufferSource();
		this.draw(poseStack, camera, partialTicks, buffers::getBuffer);
		buffers.endBatch();
	}
}

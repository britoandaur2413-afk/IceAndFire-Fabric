package com.iafabric.client.particle;

import com.iafabric.citadel.client.render.CapturedGeometry;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleEngine;
import net.minecraft.client.particle.ParticleGroup;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.state.level.ParticleGroupRenderState;

import java.util.ArrayList;
import java.util.List;

/**
 * A creature appearing right in front of the player's eyes for a moment (a siren's charm, a ghost's scare),
 * like the elder guardian's: drawn in the camera's frame, not in the world.
 */
public abstract class AppearanceParticle extends Particle {
	public static final ParticleRenderType GROUP = new ParticleRenderType("iceandfire_fabric_appearance", "iceandfire_fabric_appearance");

	protected AppearanceParticle(ClientLevel level, double x, double y, double z) {
		super(level, x, y, z);
		this.gravity = 0.0F;
	}

	@Override
	public ParticleRenderType getGroup() {
		return GROUP;
	}

	/** Draws the appearance; the pose starts at the camera, already turned the way the camera looks. */
	protected abstract void draw(PoseStack poseStack, Camera camera, float partialTicks, Buffers buffers);

	/** Where an appearance draws: one buffer per render type. */
	public interface Buffers {
		VertexConsumer getBuffer(RenderType type);
	}

	public static class Group extends ParticleGroup<AppearanceParticle> {
		public Group(ParticleEngine engine) {
			super(engine);
		}

		@Override
		public ParticleGroupRenderState extractRenderState(Frustum frustum, Camera camera, float partialTickTime) {
			List<RenderType> types = new ArrayList<>();
			List<CapturedGeometry> geometry = new ArrayList<>();
			Buffers buffers = type -> {
				int i = types.indexOf(type);
				if (i < 0) {
					types.add(type);
					geometry.add(new CapturedGeometry());
					i = types.size() - 1;
				}
				return geometry.get(i);
			};
			for (AppearanceParticle particle : this.particles) {
				PoseStack poseStack = new PoseStack();
				poseStack.rotate(camera.rotation());
				particle.draw(poseStack, camera, partialTickTime, buffers);
			}
			return new State(types, geometry);
		}
	}

	private record State(List<RenderType> types, List<CapturedGeometry> geometry) implements ParticleGroupRenderState {
		@Override
		public void submit(SubmitNodeCollector collector, CameraRenderState camera) {
			for (int i = 0; i < types.size(); i++) {
				CapturedGeometry captured = geometry.get(i);
				if (!captured.isEmpty()) {
					collector.submitCustomGeometry(new PoseStack(), types.get(i), (pose, buffer) -> captured.replay(buffer));
				}
			}
		}
	}
}

package com.iafabric.client.render.entity;

import com.iafabric.client.render.BufferSource;
import com.iafabric.client.render.IafRenderType;
import com.iafabric.entity.props.FrozenProperties;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import org.joml.Matrix4f;

/** A block of frosted ice around a frozen mob, more cracked as it is about to break. */
public final class RenderFrozenState {
	private static final ResourceLocation[] TEXTURES = {
		new ResourceLocation("textures/block/frosted_ice_0.png"), new ResourceLocation("textures/block/frosted_ice_1.png"),
		new ResourceLocation("textures/block/frosted_ice_2.png"), new ResourceLocation("textures/block/frosted_ice_3.png")};

	private RenderFrozenState() {
	}

	public static void render(LivingEntity entity, PoseStack poseStack, BufferSource buffers, int light) {
		float half = entity.getBbWidth() / 2F + 0.125F;
		float height = entity.getBbHeight() + 0.325F;
		int ticks = FrozenProperties.ticksUntilUnfrozen(entity);
		ResourceLocation texture = ticks < 20 ? TEXTURES[3] : ticks < 50 ? TEXTURES[2] : ticks < 100 ? TEXTURES[1] : TEXTURES[0];
		VertexConsumer consumer = buffers.getBuffer(IafRenderType.getIce(texture));
		Matrix4f pose = poseStack.last().pose();
		float x0 = -half, x1 = half, y0 = 0, y1 = height, z0 = -half, z1 = half;
		// The texture repeats about every 2.35 blocks, like the 1.18 ice.
		float u = (z1 - z0) * 0.425F, v = (y1 - y0) * 0.425F, w = (x1 - x0) * 0.425F;
		face(consumer, pose, light, 1, 0, 0, new float[][]{{x1, y0, z0, -u, v}, {x1, y1, z0, -u, -v}, {x1, y1, z1, u, -v}, {x1, y0, z1, u, v}});
		face(consumer, pose, light, -1, 0, 0, new float[][]{{x0, y0, z1, -u, v}, {x0, y1, z1, -u, -v}, {x0, y1, z0, u, -v}, {x0, y0, z0, u, v}});
		face(consumer, pose, light, 0, 0, -1, new float[][]{{x0, y0, z0, -w, v}, {x0, y1, z0, -w, -v}, {x1, y1, z0, w, -v}, {x1, y0, z0, w, v}});
		face(consumer, pose, light, 0, 0, 1, new float[][]{{x1, y0, z1, -w, v}, {x1, y1, z1, -w, -v}, {x0, y1, z1, w, -v}, {x0, y0, z1, w, v}});
		face(consumer, pose, light, 0, 1, 0, new float[][]{{x1, y1, z1, -u, -w}, {x1, y1, z0, u, -w}, {x0, y1, z0, u, w}, {x0, y1, z1, -u, w}});
		face(consumer, pose, light, 0, -1, 0, new float[][]{{x0, y0, z1, -u, -w}, {x0, y0, z0, u, -w}, {x1, y0, z0, u, w}, {x1, y0, z1, -u, w}});
	}

	private static void face(VertexConsumer consumer, Matrix4f pose, int light, float nx, float ny, float nz, float[][] corners) {
		for (float[] c : corners) {
			consumer.vertex(pose, c[0], c[1], c[2]).color(255, 255, 255, 255).uv(c[3], c[4])
				.overlayCoords(OverlayTexture.NO_OVERLAY).uv2(light).normal(nx, ny, nz).endVertex();
		}
	}
}

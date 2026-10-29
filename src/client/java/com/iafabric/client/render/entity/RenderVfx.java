package com.iafabric.client.render.entity;

import com.iafabric.client.render.BufferSource;
import com.iafabric.entity.EntityVfx;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.joml.Matrix4f;

/**
 * Draws the effects (EntityVfx), glowing and see-through: a warning circle whose inside fills from the middle until
 * the blow falls (then a flash), a warning lane that fills along the way of a charge, a magic circle turning with a
 * smaller one turning the other way, a shock wave going out, a column of light and a jet of frost. Textures of the mod's
 * own.
 */
public class RenderVfx extends EntityRenderer<EntityVfx> {
	private static final ResourceLocation RING = tex("telegraph_ring");
	private static final ResourceLocation FILL = tex("telegraph_fill");
	private static final ResourceLocation LANE = tex("telegraph_line");
	private static final ResourceLocation WAVE = tex("wave");
	private static final ResourceLocation BEAM = tex("beam");
	private static final ResourceLocation STREAM = tex("stream");
	private static final ResourceLocation[] RUNES = {tex("rune_frost"), tex("rune_dread"), tex("rune_storm"), tex("rune_flame"), tex("rune_life")};
	private static final int LIGHT = 0xF000F0;

	public RenderVfx(EntityRendererProvider.Context context) {
		super(context);
		this.shadowRadius = 0F;
	}

	private static ResourceLocation tex(String name) {
		return ResourceLocation.parse("iceandfire_fabric:textures/vfx/" + name + ".png");
	}

	@Override
	public ResourceLocation getTextureLocation(EntityVfx entity) {
		return RING;
	}

	private static State stateOf(EntityVfx entity, float partialTicks) {
		State state = new State();
		state.kind = entity.getKind();
		state.style = entity.getStyle();
		state.color = entity.getColor();
		state.size = entity.getSize();
		state.length = entity.getLength();
		state.charge = entity.getCharge();
		state.life = entity.getLife();
		state.spin = entity.getSpin();
		state.age = entity.tickCount + partialTicks;
		state.yaw = entity.getYRot();
		state.pitch = entity.getXRot();
		return state;
	}

	@Override
	public boolean shouldRender(EntityVfx entity, net.minecraft.client.renderer.culling.Frustum frustum, double x, double y, double z) {
		return true;
	}

	@Override
	public void render(EntityVfx entity, float entityYaw, float partialTicks, PoseStack poseStack, net.minecraft.client.renderer.MultiBufferSource multiBuffers, int light) {
		State state = stateOf(entity, partialTicks);
		BufferSource buffers = new BufferSource(multiBuffers);
		poseStack.pushPose();
		switch (state.kind) {
			case EntityVfx.CIRCLE -> circle(state, poseStack, buffers);
			case EntityVfx.LINE -> lane(state, poseStack, buffers);
			case EntityVfx.RUNE -> rune(state, poseStack, buffers);
			case EntityVfx.WAVE -> wave(state, poseStack, buffers);
			case EntityVfx.BEAM -> beam(state, poseStack, buffers);
			case EntityVfx.STREAM -> stream(state, poseStack, buffers);
			default -> {
			}
		}
		poseStack.popPose();
	}

	/** The warning circle: its edge pulsing, its inside filling from the middle; a flash when the blow falls. */
	private static void circle(State s, PoseStack poseStack, BufferSource buffers) {
		float progress = Mth.clamp(s.age / s.charge, 0F, 1F);
		float after = s.age - s.charge;
		float pulse = 0.65F + 0.25F * Mth.sin(s.age * 0.7F);
		if (after < 0) {
			flat(buffers.getBuffer(RenderType.entityTranslucentEmissive(RING)), poseStack, s.size, 0.06F, s.color, pulse, s.age * 0.6F);
			flat(buffers.getBuffer(RenderType.entityTranslucentEmissive(FILL)), poseStack, s.size * progress, 0.05F, s.color, 0.55F, 0F);
		} else {
			float fade = 1F - Mth.clamp(after / Math.max(1, s.life - s.charge), 0F, 1F);
			flat(buffers.getBuffer(RenderType.entityTranslucentEmissive(FILL)), poseStack, s.size, 0.05F, s.color, 0.95F * fade, 0F);
			flat(buffers.getBuffer(RenderType.entityTranslucentEmissive(RING)), poseStack, s.size * (1F + 0.15F * (1F - fade)), 0.06F, s.color, fade, 0F);
		}
	}

	/** The warning lane, turned to the charge's way, filling from its start. */
	private static void lane(State s, PoseStack poseStack, BufferSource buffers) {
		float progress = Mth.clamp(s.age / s.charge, 0F, 1F);
		float after = s.age - s.charge;
		float fade = after < 0 ? 1F : 1F - Mth.clamp(after / Math.max(1, s.life - s.charge), 0F, 1F);
		poseStack.mulPose(Axis.YP.rotationDegrees(-s.yaw));
		VertexConsumer buffer = buffers.getBuffer(RenderType.entityTranslucentEmissive(LANE));
		float half = s.size / 2;
		float tile = s.size * 4;
		float pulse = 0.55F + 0.25F * Mth.sin(s.age * 0.8F);
		// the lane in tiles of its texture, then its filled part over it
		for (float z = 0; z < s.length; z += tile) {
			float end = Math.min(s.length, z + tile);
			float v1 = 1F - (end - z) / tile;
			quadXZ(buffer, poseStack, -half, z, half, end, 0.06F, 0F, 1F, 1F, v1, s.color, pulse * fade);
		}
		float filled = s.length * (after < 0 ? progress : 1F);
		VertexConsumer fill = buffers.getBuffer(RenderType.entityTranslucentEmissive(FILL));
		quadXZ(fill, poseStack, -half * 0.9F, 0, half * 0.9F, filled, 0.07F, 0.45F, 0.45F, 0.55F, 0.55F, s.color, 0.6F * fade);
	}

	/** A magic circle turning, a smaller one turning the other way inside it; it comes and goes softly. */
	private static void rune(State s, PoseStack poseStack, BufferSource buffers) {
		float alpha = Mth.clamp(s.age / 8F, 0F, 1F) * Mth.clamp((s.life - s.age) / 8F, 0F, 1F);
		ResourceLocation texture = RUNES[Math.floorMod(s.style, RUNES.length)];
		VertexConsumer buffer = buffers.getBuffer(RenderType.entityTranslucentEmissive(texture));
		float grow = Mth.clamp(s.age / 6F, 0F, 1F);
		grow = 1F - (1F - grow) * (1F - grow);
		float lift = s.length;
		flat(buffer, poseStack, s.size * grow, 0.08F + lift, s.color, alpha * 0.95F, s.age * s.spin);
		flat(buffer, poseStack, s.size * 0.55F * grow, 0.1F + lift, s.color, alpha * 0.8F, -s.age * s.spin * 1.6F);
	}

	/** A ring going out and fading. */
	private static void wave(State s, PoseStack poseStack, BufferSource buffers) {
		float t = Mth.clamp(s.age / s.life, 0F, 1F);
		float radius = s.size * (float) Math.pow(t, 0.6);
		flat(buffers.getBuffer(RenderType.entityTranslucentEmissive(WAVE)), poseStack, radius, 0.12F + s.length, s.color, 1F - t, 0F);
	}

	/** A column of light (a charge, a call): four crossed planes, turning slowly, coming and going. */
	private static void beam(State s, PoseStack poseStack, BufferSource buffers) {
		float alpha = Mth.clamp(s.age / 6F, 0F, 1F) * Mth.clamp((s.life - s.age) / 6F, 0F, 1F);
		VertexConsumer buffer = buffers.getBuffer(RenderType.entityTranslucentEmissive(BEAM));
		poseStack.mulPose(Axis.YP.rotationDegrees(s.age * 3F));
		float r = s.size * (0.85F + 0.15F * Mth.sin(s.age * 0.5F));
		for (int k = 0; k < 4; k++) {
			poseStack.pushPose();
			poseStack.mulPose(Axis.YP.rotationDegrees(45F * k));
			Matrix4f m = poseStack.last().pose();
			int[] c = argb(s.color, alpha * 0.8F);
			vertex(buffer, m, -r, 0, 0, c, 0F, 1F, 0, 0, 1);
			vertex(buffer, m, r, 0, 0, c, 1F, 1F, 0, 0, 1);
			vertex(buffer, m, r, s.length, 0, c, 1F, 0F, 0, 0, 1);
			vertex(buffer, m, -r, s.length, 0, c, 0F, 0F, 0, 0, 1);
			vertex(buffer, m, -r, s.length, 0, c, 0F, 0F, 0, 0, -1);
			vertex(buffer, m, r, s.length, 0, c, 1F, 0F, 0, 0, -1);
			vertex(buffer, m, r, 0, 0, c, 1F, 1F, 0, 0, -1);
			vertex(buffer, m, -r, 0, 0, c, 0F, 1F, 0, 0, -1);
			poseStack.popPose();
		}
	}

	/** A jet of frost along its way, widening to its end, its streaks running out along it, flickering. */
	private static void stream(State s, PoseStack poseStack, BufferSource buffers) {
		float alpha = Mth.clamp(s.age / 4F, 0F, 1F) * Mth.clamp((s.life - s.age) / 6F, 0F, 1F) * (0.8F + 0.2F * Mth.sin(s.age * 1.7F));
		if (alpha <= 0.01F || s.length <= 0.1F) {
			return;
		}
		VertexConsumer buffer = buffers.getBuffer(RenderType.entityTranslucentEmissive(STREAM));
		poseStack.mulPose(Axis.YP.rotationDegrees(-s.yaw));
		poseStack.mulPose(Axis.XP.rotationDegrees(s.pitch));
		float near = s.size / 4;
		float far = s.size * 0.55F;
		float seg = s.size * 1.5F;
		float shift = (s.age * 0.15F * seg) % seg;
		int[] c = argb(s.color, alpha * 0.85F);
		// three planes turned about its way, each a long trapezoid in pieces of its texture
		for (int k = 0; k < 3; k++) {
			poseStack.pushPose();
			poseStack.mulPose(Axis.ZP.rotationDegrees(60F * k));
			Matrix4f m = poseStack.last().pose();
			for (float z = shift - seg; z < s.length; z += seg) {
				float a0 = Math.max(0F, z);
				float a1 = Math.min(s.length, z + seg);
				if (a1 <= a0) {
					continue;
				}
				float w0 = near + (far - near) * a0 / s.length;
				float w1 = near + (far - near) * a1 / s.length;
				float v0 = (a0 - z) / seg;
				float v1 = (a1 - z) / seg;
				vertex(buffer, m, -w0, 0, a0, c, 0F, v0, 0, 1, 0);
				vertex(buffer, m, -w1, 0, a1, c, 0F, v1, 0, 1, 0);
				vertex(buffer, m, w1, 0, a1, c, 1F, v1, 0, 1, 0);
				vertex(buffer, m, w0, 0, a0, c, 1F, v0, 0, 1, 0);
				vertex(buffer, m, w0, 0, a0, c, 1F, v0, 0, -1, 0);
				vertex(buffer, m, w1, 0, a1, c, 1F, v1, 0, -1, 0);
				vertex(buffer, m, -w1, 0, a1, c, 0F, v1, 0, -1, 0);
				vertex(buffer, m, -w0, 0, a0, c, 0F, v0, 0, -1, 0);
			}
			poseStack.popPose();
		}
		// and its skin round it, a widening cone: seen from before it (who it comes at) as from aside
		int[] skin = argb(s.color, alpha * 0.7F);
		Matrix4f m = poseStack.last().pose();
		int sides = 10;
		for (float z = shift - seg; z < s.length; z += seg) {
			float a0 = Math.max(0F, z);
			float a1 = Math.min(s.length, z + seg);
			if (a1 <= a0) {
				continue;
			}
			float w0 = near + (far - near) * a0 / s.length;
			float w1 = near + (far - near) * a1 / s.length;
			float v0 = (a0 - z) / seg;
			float v1 = (a1 - z) / seg;
			for (int k = 0; k < sides; k++) {
				float t0 = (float) (Math.PI * 2 * k / sides);
				float t1 = (float) (Math.PI * 2 * (k + 1) / sides);
				float c0 = Mth.cos(t0);
				float s0 = Mth.sin(t0);
				float c1 = Mth.cos(t1);
				float s1 = Mth.sin(t1);
				float u0 = (float) k / sides;
				float u1 = (float) (k + 1) / sides;
				vertex(buffer, m, c0 * w0, s0 * w0, a0, skin, u0, v0, c0, s0, 0);
				vertex(buffer, m, c0 * w1, s0 * w1, a1, skin, u0, v1, c0, s0, 0);
				vertex(buffer, m, c1 * w1, s1 * w1, a1, skin, u1, v1, c1, s1, 0);
				vertex(buffer, m, c1 * w0, s1 * w0, a0, skin, u1, v0, c1, s1, 0);
				vertex(buffer, m, c1 * w0, s1 * w0, a0, skin, u1, v0, -c1, -s1, 0);
				vertex(buffer, m, c1 * w1, s1 * w1, a1, skin, u1, v1, -c1, -s1, 0);
				vertex(buffer, m, c0 * w1, s0 * w1, a1, skin, u0, v1, -c0, -s0, 0);
				vertex(buffer, m, c0 * w0, s0 * w0, a0, skin, u0, v0, -c0, -s0, 0);
			}
		}
	}

	/** A flat square on the ground (both faces), turned by the given degrees, the whole texture on it. */
	private static void flat(VertexConsumer buffer, PoseStack poseStack, float radius, float y, int color, float alpha, float turn) {
		if (radius <= 0.01F || alpha <= 0.01F) {
			return;
		}
		poseStack.pushPose();
		poseStack.mulPose(Axis.YP.rotationDegrees(turn));
		quadXZ(buffer, poseStack, -radius, -radius, radius, radius, y, 0F, 0F, 1F, 1F, color, alpha);
		poseStack.popPose();
	}

	private static void quadXZ(VertexConsumer buffer, PoseStack poseStack, float x0, float z0, float x1, float z1, float y,
		float u0, float v0, float u1, float v1, int color, float alpha) {
		Matrix4f m = poseStack.last().pose();
		int[] c = argb(color, alpha);
		// up
		vertex(buffer, m, x0, y, z0, c, u0, v0, 0, 1, 0);
		vertex(buffer, m, x0, y, z1, c, u0, v1, 0, 1, 0);
		vertex(buffer, m, x1, y, z1, c, u1, v1, 0, 1, 0);
		vertex(buffer, m, x1, y, z0, c, u1, v0, 0, 1, 0);
		// down (seen from under: a cave's roof, a ledge)
		vertex(buffer, m, x1, y, z0, c, u1, v0, 0, -1, 0);
		vertex(buffer, m, x1, y, z1, c, u1, v1, 0, -1, 0);
		vertex(buffer, m, x0, y, z1, c, u0, v1, 0, -1, 0);
		vertex(buffer, m, x0, y, z0, c, u0, v0, 0, -1, 0);
	}

	private static int[] argb(int color, float alpha) {
		int a = (int) (Mth.clamp(alpha, 0F, 1F) * ((color >>> 24) & 0xFF));
		return new int[]{(color >> 16) & 0xFF, (color >> 8) & 0xFF, color & 0xFF, a};
	}

	private static void vertex(VertexConsumer buffer, Matrix4f m, float x, float y, float z, int[] c, float u, float v, float nx, float ny, float nz) {
		buffer.addVertex(m, x, y, z).setColor(c[0], c[1], c[2], c[3]).setUv(u, v).setOverlay(OverlayTexture.NO_OVERLAY).setLight(LIGHT).setNormal(nx, ny, nz);
	}

	/** What an effect is this frame. */
	public static class State {
		int kind;
		int style;
		int color;
		float size;
		float length;
		int charge;
		int life;
		float spin;
		float age;
		float yaw;
		float pitch;
	}
}

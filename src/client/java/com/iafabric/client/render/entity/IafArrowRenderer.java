package com.iafabric.client.render.entity;

import com.iafabric.client.render.BufferSource;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;

/** An arrow with its own texture (the classic crossed-quads arrow, 32x32 arrow texture layout). */
public abstract class IafArrowRenderer<T extends AbstractArrow> extends IafEntityRenderer<T> {

	protected IafArrowRenderer(EntityRendererProvider.Context context) {
		super(context);
	}

	@Override
	public abstract Identifier getTextureLocation(T entity);

	@Override
	public void render(T entity, float entityYaw, float partialTicks, PoseStack poseStack, BufferSource buffers, int light) {
		poseStack.pushPose();
		poseStack.rotateDegrees(Axis.YP, Mth.lerp(partialTicks, entity.yRotO, entity.getYRot()) - 90.0F);
		poseStack.rotateDegrees(Axis.ZP, Mth.lerp(partialTicks, entity.xRotO, entity.getXRot()));
		float shake = entity.shakeTime - partialTicks;
		if (shake > 0.0F) {
			poseStack.rotateDegrees(Axis.ZP, -Mth.sin(shake * 3.0F) * shake);
		}
		poseStack.rotateDegrees(Axis.XP, 45.0F);
		poseStack.scale(0.05625F, 0.05625F, 0.05625F);
		poseStack.translate(-4.0F, 0.0F, 0.0F);
		VertexConsumer buffer = buffers.getBuffer(RenderTypes.entityCutout(this.getTextureLocation(entity)));
		PoseStack.Pose pose = poseStack.last();
		vertex(pose, buffer, -7, -2, -2, 0.0F, 0.15625F, -1, 0, 0, light);
		vertex(pose, buffer, -7, -2, 2, 0.15625F, 0.15625F, -1, 0, 0, light);
		vertex(pose, buffer, -7, 2, 2, 0.15625F, 0.3125F, -1, 0, 0, light);
		vertex(pose, buffer, -7, 2, -2, 0.0F, 0.3125F, -1, 0, 0, light);
		vertex(pose, buffer, -7, 2, -2, 0.0F, 0.15625F, 1, 0, 0, light);
		vertex(pose, buffer, -7, 2, 2, 0.15625F, 0.15625F, 1, 0, 0, light);
		vertex(pose, buffer, -7, -2, 2, 0.15625F, 0.3125F, 1, 0, 0, light);
		vertex(pose, buffer, -7, -2, -2, 0.0F, 0.3125F, 1, 0, 0, light);
		for (int i = 0; i < 4; i++) {
			poseStack.rotateDegrees(Axis.XP, 90.0F);
			pose = poseStack.last();
			vertex(pose, buffer, -8, -2, 0, 0.0F, 0.0F, 0, 1, 0, light);
			vertex(pose, buffer, 8, -2, 0, 0.5F, 0.0F, 0, 1, 0, light);
			vertex(pose, buffer, 8, 2, 0, 0.5F, 0.15625F, 0, 1, 0, light);
			vertex(pose, buffer, -8, 2, 0, 0.0F, 0.15625F, 0, 1, 0, light);
		}
		poseStack.popPose();
	}

	private static void vertex(PoseStack.Pose pose, VertexConsumer buffer, int x, int y, int z, float u, float v, int nx, int ny, int nz, int light) {
		buffer.addVertex(pose, x, y, z).setColor(-1).setUv(u, v).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(pose, nx, nz, ny);
	}
}

package com.iafabric.client.render.entity;

import com.iafabric.citadel.client.model.basic.BasicEntityModel;
import com.iafabric.client.render.BufferSource;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * A living entity drawn with one of the mod's own models, posed from the entity the way the 1.18
 * LivingEntityRenderer did it (body and head rotation, walk animation, death flip, red hurt overlay),
 * then layers on top.
 */
public abstract class IafMobRenderer<T extends LivingEntity, M extends BasicEntityModel<? super T>> extends EntityRenderer<T> {
	protected M model;
	protected final List<IafRenderLayer<T, M>> layers = new ArrayList<>();

	protected IafMobRenderer(EntityRendererProvider.Context context, M model, float shadowRadius) {
		super(context);
		this.model = model;
		this.shadowRadius = shadowRadius;
	}

	public M getModel() {
		return model;
	}

	public final boolean addLayer(IafRenderLayer<T, M> layer) {
		return layers.add(layer);
	}

	@Override
	public abstract ResourceLocation getTextureLocation(T entity);

	/** The body's turn this frame (as the vehicle's when it rides, the head kept within reach of it). */
	public static float bodyRot(LivingEntity entity, float partialTicks) {
		float bodyRot = Mth.rotLerp(partialTicks, entity.yBodyRotO, entity.yBodyRot);
		if (entity.isPassenger() && entity.getVehicle() instanceof LivingEntity vehicle) {
			float headRot = Mth.rotLerp(partialTicks, entity.yHeadRotO, entity.yHeadRot);
			bodyRot = Mth.rotLerp(partialTicks, vehicle.yBodyRotO, vehicle.yBodyRot);
			float clamped = Mth.clamp(Mth.wrapDegrees(headRot - bodyRot), -85.0F, 85.0F);
			bodyRot = headRot - clamped;
			if (clamped * clamped > 2500.0F) {
				bodyRot += clamped * 0.2F;
			}
		}
		return bodyRot;
	}

	@Override
	public void render(T entity, float entityYaw, float partialTicks, PoseStack poseStack, MultiBufferSource multiBuffers, int light) {
		this.beforeRender(entity, partialTicks);
		poseStack.pushPose();
		model.attackTime = entity.getAttackAnim(partialTicks);
		model.riding = entity.isPassenger();
		model.young = entity.isBaby();
		float bodyRot = bodyRot(entity, partialTicks);
		float headRot = Mth.rotLerp(partialTicks, entity.yHeadRotO, entity.yHeadRot);
		float netHeadYaw = headRot - bodyRot;
		float headPitch = Mth.lerp(partialTicks, entity.xRotO, entity.getXRot());
		float bob = this.getBob(entity, partialTicks);
		this.setupRotations(entity, poseStack, bob, bodyRot, partialTicks);
		poseStack.scale(-1.0F, -1.0F, 1.0F);
		this.scale(entity, poseStack, partialTicks);
		poseStack.translate(0.0D, -1.501F, 0.0D);
		float limbSwingAmount = 0.0F;
		float limbSwing = 0.0F;
		if (!entity.isPassenger() && entity.isAlive()) {
			limbSwingAmount = Math.min(entity.walkAnimation.speed(partialTicks), 1.0F);
			limbSwing = entity.walkAnimation.position(partialTicks);
			if (entity.isBaby()) {
				limbSwing *= 3.0F;
			}
		}
		model.prepareMobModel(entity, limbSwing, limbSwingAmount, partialTicks);
		model.setupAnim(entity, limbSwing, limbSwingAmount, bob, netHeadYaw, headPitch);
		int overlay = getOverlayCoords(entity, this.getWhiteOverlayProgress(entity, partialTicks));
		BufferSource buffers = new BufferSource(multiBuffers);
		this.renderBody(entity, poseStack, buffers, light, overlay, partialTicks);
		if (!entity.isSpectator()) {
			for (IafRenderLayer<T, M> layer : layers) {
				layer.render(poseStack, buffers, light, entity, limbSwing, limbSwingAmount, partialTicks, bob, netHeadYaw, headPitch);
			}
		}
		this.renderInPose(entity, partialTicks, poseStack, multiBuffers, light);
		poseStack.popPose();
		this.renderExtra(entity, partialTicks, poseStack, buffers, light);
		com.iafabric.client.render.entity.layer.LayerLivingExtras.renderAtFeet(entity, partialTicks, poseStack, buffers, light);
		super.render(entity, entityYaw, partialTicks, poseStack, multiBuffers, light);
		this.renderWith(entity, bodyRot, partialTicks, poseStack, multiBuffers, light);
	}

	/**
	 * Draws what goes with the posed model, in its pose (its model's frame, its parts as they are posed this frame):
	 * a dragon's rider in its seat.
	 */
	protected void renderInPose(T entity, float partialTicks, PoseStack poseStack, MultiBufferSource buffers, int light) {
	}

	/** Draws the posed model (a renderer may draw its body another way). */
	protected void renderBody(T entity, PoseStack poseStack, BufferSource buffers, int light, int overlay, float partialTicks) {
		boolean visible = !entity.isInvisible();
		RenderType renderType = this.getRenderType(entity, visible);
		if (renderType != null) {
			model.renderToBuffer(poseStack, buffers.getBuffer(renderType), light, overlay, visible ? 0xFFFFFFFF : 0x26FFFFFF);
		}
	}

	/** Called first, before the model is posed: a renderer may switch models here (larva, pupa, adult...). */
	protected void beforeRender(T entity, float partialTicks) {
	}

	/** What the 1.18 renderer drew in render() after the body (beams, chains, held things), in the entity's frame. */
	protected void renderExtra(T entity, float partialTicks, PoseStack poseStack, BufferSource buffers, int light) {
	}

	/** Another one drawn with it, after it (a dragon's rider), in the entity's frame. */
	protected void renderWith(T entity, float bodyRot, float partialTicks, PoseStack poseStack, MultiBufferSource buffers, int light) {
	}

	protected @Nullable RenderType getRenderType(T entity, boolean visible) {
		ResourceLocation texture = this.getTextureLocation(entity);
		if (!visible) {
			return RenderType.entityTranslucent(texture);
		}
		return RenderType.entityCutoutNoCull(texture);
	}

	public static int getOverlayCoords(LivingEntity entity, float whiteOverlayProgress) {
		return OverlayTexture.pack(OverlayTexture.u(whiteOverlayProgress), OverlayTexture.v(entity.hurtTime > 0 || entity.deathTime > 0));
	}

	protected float getWhiteOverlayProgress(T entity, float partialTicks) {
		return 0.0F;
	}

	protected boolean isShaking(T entity) {
		return entity.isFullyFrozen();
	}

	protected float getBob(T entity, float partialTicks) {
		return entity.tickCount + partialTicks;
	}

	protected void setupRotations(T entity, PoseStack poseStack, float bob, float bodyRot, float partialTicks) {
		if (this.isShaking(entity)) {
			bodyRot += (float) (Math.cos(entity.tickCount * 3.25D) * Math.PI * 0.4F);
		}
		if (!entity.hasPose(Pose.SLEEPING)) {
			poseStack.mulPose(Axis.YP.rotationDegrees(180.0F - bodyRot));
		}
		if (entity.deathTime > 0) {
			float fall = (entity.deathTime + partialTicks - 1.0F) / 20.0F * 1.6F;
			fall = Mth.sqrt(fall);
			if (fall > 1.0F) {
				fall = 1.0F;
			}
			poseStack.mulPose(Axis.ZP.rotationDegrees(fall * this.getFlipDegrees(entity)));
		}
	}

	protected float getFlipDegrees(T entity) {
		return 90.0F;
	}

	protected void scale(T entity, PoseStack poseStack, float partialTicks) {
	}
}

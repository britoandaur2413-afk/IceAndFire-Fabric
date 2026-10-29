package com.iafabric.client.render.entity;

import com.iafabric.client.render.entity.IafRenderLayer;
import com.iafabric.client.render.entity.IafMobRenderer;
import com.iafabric.citadel.client.model.basic.BasicEntityModel;
import com.iafabric.client.render.BufferSource;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * A living entity drawn with one of the mod's own models, posed from the entity the way the 1.18
 * LivingEntityRenderer did it (body and head rotation, walk animation, death flip, red hurt overlay),
 * then layers on top. 26.x renderers only see a render state, so the state carries the entity itself.
 */
public abstract class IafMobRenderer<T extends LivingEntity, M extends BasicEntityModel<? super T>> extends EntityRenderer<T, IafMobRenderer.State<T>> {
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

	public abstract Identifier getTextureLocation(T entity);

	@Override
	public State<T> createRenderState() {
		return new State<>();
	}

	@Override
	public void extractRenderState(T entity, State<T> state, float partialTicks) {
		super.extractRenderState(entity, state, partialTicks);
		state.entity = entity;
		state.partialTick = partialTicks;
		state.shadowRadius = this.shadowRadius;
		// the turn is kept in the state: a screen showing the mob (an inventory-like box) turns it there, toward
		// the viewer, without touching the entity
		float bodyRot = Mth.rotLerp(partialTicks, entity.yBodyRotO, entity.yBodyRot);
		float headRot = Mth.rotLerp(partialTicks, entity.yHeadRotO, entity.yHeadRot);
		float netHeadYaw = headRot - bodyRot;
		if (entity.isPassenger() && entity.getVehicle() instanceof LivingEntity vehicle) {
			bodyRot = Mth.rotLerp(partialTicks, vehicle.yBodyRotO, vehicle.yBodyRot);
			netHeadYaw = headRot - bodyRot;
			float clamped = Mth.clamp(Mth.wrapDegrees(netHeadYaw), -85.0F, 85.0F);
			bodyRot = headRot - clamped;
			if (clamped * clamped > 2500.0F) {
				bodyRot += clamped * 0.2F;
			}
			netHeadYaw = headRot - bodyRot;
		}
		state.bodyRot = bodyRot;
		state.yRot = netHeadYaw;
		state.xRot = Mth.lerp(partialTicks, entity.xRotO, entity.getXRot());
	}

	@Override
	public void submit(State<T> state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
		T entity = state.entity;
		if (entity == null) {
			return;
		}
		float partialTicks = state.partialTick;
		this.beforeRender(entity, partialTicks);
		poseStack.pushPose();
		model.attackTime = entity.getSwingAnimation(partialTicks);
		model.riding = entity.isPassenger();
		model.young = entity.isBaby();
		float bodyRot = state.bodyRot;
		float netHeadYaw = state.yRot;
		float headPitch = state.xRot;
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
		int light = state.lightCoords;
		int overlay = getOverlayCoords(entity, this.getWhiteOverlayProgress(entity, partialTicks));
		BufferSource buffers = new BufferSource(collector).withCamera(camera);
		this.renderBody(entity, poseStack, buffers, light, overlay, partialTicks);
		if (!entity.isSpectator()) {
			for (IafRenderLayer<T, M> layer : layers) {
				layer.render(poseStack, buffers, light, entity, limbSwing, limbSwingAmount, partialTicks, bob, netHeadYaw, headPitch);
			}
		}
		this.submitInPose(entity, state, poseStack, collector, camera);
		poseStack.popPose();
		this.renderExtra(entity, partialTicks, poseStack, buffers, light);
		com.iafabric.client.render.entity.layer.LayerLivingExtras.renderAtFeet(entity, partialTicks, poseStack, buffers, light);
		buffers.flush();
		super.submit(state, poseStack, collector, camera);
		if (state.extra != null) {
			// another one drawn with it (a dragon's rider), where it is from this one
			this.entityRenderDispatcher.submit(state.extra, camera, state.extraOffset.x, state.extraOffset.y, state.extraOffset.z, poseStack, collector);
		}
	}

	/**
	 * Draws what goes with the posed model, in its pose (its model's frame, its parts as they are posed this frame):
	 * a dragon's rider in its seat.
	 */
	protected void submitInPose(T entity, State<T> state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
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

	protected @Nullable RenderType getRenderType(T entity, boolean visible) {
		Identifier texture = this.getTextureLocation(entity);
		if (!visible) {
			return RenderTypes.entityTranslucent(texture);
		}
		return RenderTypes.entityCutout(texture);
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
			poseStack.rotateDegrees(Axis.YP, 180.0F - bodyRot);
		}
		if (entity.deathTime > 0) {
			float fall = (entity.deathTime + partialTicks - 1.0F) / 20.0F * 1.6F;
			fall = Mth.sqrt(fall);
			if (fall > 1.0F) {
				fall = 1.0F;
			}
			poseStack.rotateDegrees(Axis.ZP, fall * this.getFlipDegrees(entity));
		}
	}

	protected float getFlipDegrees(T entity) {
		return 90.0F;
	}

	protected void scale(T entity, PoseStack poseStack, float partialTicks) {
	}

	/** The render state: the entity itself (the mod's models pose from it), the partial tick and its turn. */
	public static class State<T extends LivingEntity> extends net.minecraft.client.renderer.entity.state.LivingEntityRenderState {
		public @Nullable T entity;
		public float partialTick;
		/** Another one drawn with it (a dragon's rider), and where from this one. */
		public net.minecraft.client.renderer.entity.state.@Nullable EntityRenderState extra;
		public net.minecraft.world.phys.Vec3 extraOffset = net.minecraft.world.phys.Vec3.ZERO;
		/** Its riders drawn in its seat, in its pose (a dragon's: on the bone of its back). */
		public final java.util.List<net.minecraft.client.renderer.entity.state.EntityRenderState> seated = new java.util.ArrayList<>();
		/** And the one in the seat behind (a dragon's double saddle: on its lower back). */
		public final java.util.List<net.minecraft.client.renderer.entity.state.EntityRenderState> seatedBehind = new java.util.ArrayList<>();
	}
}

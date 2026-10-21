package com.iafabric.client.render.entity.layer;

import com.iafabric.client.particle.CockatriceBeamRender;
import com.iafabric.client.render.BufferSource;
import com.iafabric.client.render.entity.RenderChain;
import com.iafabric.client.render.entity.RenderFrozenState;
import com.iafabric.entity.props.ChainProperties;
import com.iafabric.entity.props.FrozenProperties;
import com.iafabric.entity.props.MiscProperties;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.fabricmc.fabric.api.client.rendering.v1.LivingEntityRenderLayerRegistrationCallback;
import net.fabricmc.fabric.api.client.rendering.v1.RenderStateDataKey;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.world.entity.LivingEntity;


/**
 * What the 1.18 mod drew after any mob: the ice around a frozen one, the chains of a chained one and the
 * gaze of the cockatrices staring at it. Added to every mob's renderer; it undoes the model's pose to
 * draw at the mob's feet, unturned, like the 1.18 render event did.
 */
public class LayerLivingExtras<S extends LivingEntityRenderState, M extends EntityModel<? super S>> extends RenderLayer<S, M> {
	public static final RenderStateDataKey<LivingEntity> ENTITY = RenderStateDataKey.create(() -> "iceandfire_fabric:entity");
	public static final RenderStateDataKey<Float> PARTIAL_TICK = RenderStateDataKey.create(() -> "iceandfire_fabric:partial_tick");

	public LayerLivingExtras(RenderLayerParent<S, M> parent) {
		super(parent);
	}

	@SuppressWarnings({"unchecked", "rawtypes"})
	public static void init() {
		LivingEntityRenderLayerRegistrationCallback.EVENT.register((type, renderer, helper, context) ->
			helper.register(new LayerLivingExtras(renderer)));
	}

	@Override
	public void submit(PoseStack poseStack, SubmitNodeCollector collector, int light, S state, float yRot, float xRot) {
		LivingEntity entity = state.getData(ENTITY);
		if (entity == null) {
			return;
		}
		if (!hasExtras(entity)) {
			return;
		}
		poseStack.pushPose();
		// Back from the model's space to the mob's feet: the model is flipped, lifted and turned with the body.
		poseStack.translate(0.0F, 1.501F, 0.0F);
		poseStack.scale(-1.0F, -1.0F, 1.0F);
		if (state.scale != 0.0F) {
			poseStack.scale(1.0F / state.scale, 1.0F / state.scale, 1.0F / state.scale);
		}
		poseStack.rotateDegrees(Axis.YP, -(180.0F - state.bodyRot));
		BufferSource buffers = new BufferSource(collector);
		renderAtFeet(entity, state.getDataOrDefault(PARTIAL_TICK, 0.0F), poseStack, buffers, light);
		buffers.flush();
		poseStack.popPose();
	}

	private static boolean hasExtras(LivingEntity entity) {
		return FrozenProperties.isFrozen(entity) || !ChainProperties.getChainedTo(entity).isEmpty() || !MiscProperties.getTargetedBy(entity).isEmpty();
	}

	/** The ice, chains and gazes of a mob, with the pose at its feet, unturned (the mod's own renderers call it too). */
	public static void renderAtFeet(LivingEntity entity, float partialTick, PoseStack poseStack, BufferSource buffers, int light) {
		if (FrozenProperties.isFrozen(entity)) {
			RenderFrozenState.render(entity, poseStack, buffers, light);
		}
		if (!ChainProperties.getChainedTo(entity).isEmpty()) {
			RenderChain.render(entity, partialTick, poseStack, buffers, light);
		}
		for (LivingEntity caster : MiscProperties.getTargetedBy(entity)) {
			CockatriceBeamRender.render(entity, caster, poseStack, buffers, partialTick);
		}
	}
}

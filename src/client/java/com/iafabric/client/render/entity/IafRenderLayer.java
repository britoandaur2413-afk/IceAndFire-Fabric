package com.iafabric.client.render.entity;

import com.iafabric.client.render.entity.IafRenderLayer;
import com.iafabric.client.render.entity.IafMobRenderer;
import com.iafabric.citadel.client.model.basic.BasicEntityModel;
import com.mojang.blaze3d.vertex.PoseStack;
import com.iafabric.client.render.BufferSource;
import net.minecraft.world.entity.LivingEntity;

/** Something drawn over a mob after its body, in the body's pose (the 1.18 RenderLayer). */
public abstract class IafRenderLayer<T extends LivingEntity, M extends BasicEntityModel<? super T>> {
	protected final IafMobRenderer<T, M> renderer;

	protected IafRenderLayer(IafMobRenderer<T, M> renderer) {
		this.renderer = renderer;
	}

	public M getParentModel() {
		return renderer.getModel();
	}

	public abstract void render(PoseStack poseStack, BufferSource buffers, int light, T entity, float limbSwing, float limbSwingAmount,
		float partialTicks, float ageInTicks, float netHeadYaw, float headPitch);
}

package com.iafabric.client.render.entity;

import com.iafabric.client.model.ModelDreadQueen;
import com.iafabric.client.render.entity.layer.LayerHeldItem;
import com.iafabric.entity.EntityCryonaAsleep;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;

/** Cryona asleep on her throne (her sign there): grey and still with her weapons, her eyes dark. */
public class RenderCryonaAsleep extends IafMobRenderer<EntityCryonaAsleep, ModelDreadQueen<EntityCryonaAsleep>> {

	public RenderCryonaAsleep(EntityRendererProvider.Context context) {
		super(context, new ModelDreadQueen<>(0.0F), 0.6F);
		this.addLayer(new LayerHeldItem<>(this));
	}

	@Override
	protected void scale(EntityCryonaAsleep entity, PoseStack poseStack, float partialTickTime) {
		poseStack.scale(1.1F, 1.1F, 1.1F);
	}

	@Override
	public ResourceLocation getTextureLocation(EntityCryonaAsleep queen) {
		return RenderDreadQueen.TEXTURE_INACTIVE;
	}
}

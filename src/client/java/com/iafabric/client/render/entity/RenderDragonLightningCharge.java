package com.iafabric.client.render.entity;

import com.mojang.math.Axis;
import net.minecraft.client.renderer.RenderType;
import com.iafabric.client.render.BufferSource;
import com.iafabric.client.model.ModelDreadLichSkull;
import com.iafabric.entity.EntityDragonLightningCharge;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

import org.jetbrains.annotations.Nullable;

public class RenderDragonLightningCharge extends IafEntityRenderer<EntityDragonLightningCharge> {

    public static final ResourceLocation TEXTURE = new ResourceLocation("iceandfire_fabric:textures/models/lightningdragon/charge.png");
    public static final ResourceLocation TEXTURE_CORE = new ResourceLocation("iceandfire_fabric:textures/models/lightningdragon/charge_core.png");
    private static final ModelDreadLichSkull MODEL_SPIRIT = new ModelDreadLichSkull();

    public RenderDragonLightningCharge(EntityRendererProvider.Context context) {
        super(context);
    }


    @Override
    public void render(EntityDragonLightningCharge entity, float entityYaw, float partialTicks, PoseStack matrixStackIn, BufferSource bufferIn, int packedLightIn) {
        float f = (float) entity.tickCount + partialTicks;
        float yaw = entity.yRotO + (entity.getYRot() - entity.yRotO) * partialTicks;
        // (each buffer taken where it is drawn into: taking another one ends the one before)
        // leaving its dragon's mouth
        var shift = com.iafabric.client.particle.DragonBreathFx.chargeShift(entity, partialTicks);
        matrixStackIn.pushPose();
        matrixStackIn.translate(shift.x, shift.y, shift.z);

        matrixStackIn.pushPose();
        matrixStackIn.translate(0F, 0.5F, 0F);
        matrixStackIn.translate(0F, -0.25F, 0F);
        matrixStackIn.mulPose(Axis.YP.rotationDegrees((yaw - 180)));
        matrixStackIn.mulPose(Axis.XP.rotationDegrees((f * 20)));
        matrixStackIn.translate(0F, 0.25F, 0F);
        MODEL_SPIRIT.renderToBuffer(matrixStackIn, bufferIn.getBuffer(RenderType.eyes(TEXTURE_CORE)), packedLightIn, OverlayTexture.NO_OVERLAY, 1.0F, 1.0F, 1.0F, 1.0F);
        matrixStackIn.popPose();

        matrixStackIn.pushPose();
        matrixStackIn.translate(0F, 0.5F, 0F);
        matrixStackIn.translate(0F, -0.25F, 0F);
        matrixStackIn.mulPose(Axis.YP.rotationDegrees((yaw - 180)));
        matrixStackIn.mulPose(Axis.XP.rotationDegrees((f * 15)));
        matrixStackIn.translate(0F, 0.25F, 0F);
        matrixStackIn.scale(1.5F, 1.5F, 1.5F);
        MODEL_SPIRIT.renderToBuffer(matrixStackIn, bufferIn.getBuffer(RenderType.energySwirl(TEXTURE, f * 0.01F, f * 0.01F)), packedLightIn, OverlayTexture.NO_OVERLAY, 1.0F, 1.0F, 1.0F, 1.0F);
        matrixStackIn.popPose();

        matrixStackIn.pushPose();
        matrixStackIn.translate(0F, 0.75F, 0F);
        matrixStackIn.translate(0F, -0.25F, 0F);
        matrixStackIn.mulPose(Axis.YP.rotationDegrees((yaw - 180)));
        matrixStackIn.mulPose(Axis.XP.rotationDegrees((f * 10)));
        matrixStackIn.translate(0F, 0.75F, 0F);
        matrixStackIn.scale(2.5F, 2.5F, 2.5F);
        MODEL_SPIRIT.renderToBuffer(matrixStackIn, bufferIn.getBuffer(RenderType.energySwirl(TEXTURE, f * 0.01F, f * 0.01F)), packedLightIn, OverlayTexture.NO_OVERLAY, 1.0F, 1.0F, 1.0F, 1.0F);
        matrixStackIn.popPose();

        matrixStackIn.popPose();
        super.render(entity, entityYaw, partialTicks, matrixStackIn, bufferIn, packedLightIn);
    }

    private float interpolateValue(float start, float end, float pct) {
        return start + (end - start) * pct;
    }

    @Nullable
    @Override
    public ResourceLocation getTextureLocation(EntityDragonLightningCharge entity) {
        return TEXTURE;
    }
}

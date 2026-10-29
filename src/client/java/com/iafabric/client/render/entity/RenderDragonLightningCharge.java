package com.iafabric.client.render.entity;

import com.mojang.math.Axis;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import com.iafabric.client.render.BufferSource;
import com.iafabric.client.model.ModelDreadLichSkull;
import com.iafabric.entity.EntityDragonLightningCharge;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.NotNull;

import org.jspecify.annotations.Nullable;

public class RenderDragonLightningCharge extends IafEntityRenderer<EntityDragonLightningCharge> {

    public static final Identifier TEXTURE = Identifier.parse("iceandfire_fabric:textures/models/lightningdragon/charge.png");
    public static final Identifier TEXTURE_CORE = Identifier.parse("iceandfire_fabric:textures/models/lightningdragon/charge_core.png");
    private static final ModelDreadLichSkull MODEL_SPIRIT = new ModelDreadLichSkull();

    public RenderDragonLightningCharge(EntityRendererProvider.Context context) {
        super(context);
    }


    @Override
    public void render(EntityDragonLightningCharge entity, float entityYaw, float partialTicks, PoseStack matrixStackIn, BufferSource bufferIn, int packedLightIn) {
        float f = (float) entity.tickCount + partialTicks;
        float yaw = entity.yRotO + (entity.getYRot() - entity.yRotO) * partialTicks;
        VertexConsumer ivertexbuilder2 = bufferIn.getBuffer(RenderTypes.eyes(TEXTURE_CORE));
        VertexConsumer ivertexbuilder = bufferIn.getBuffer(RenderTypes.energySwirl(TEXTURE, f * 0.01F, f * 0.01F));
        // leaving its dragon's mouth
        var shift = com.iafabric.client.particle.DragonBreathFx.chargeShift(entity, partialTicks);
        matrixStackIn.pushPose();
        matrixStackIn.translate(shift.x, shift.y, shift.z);

        matrixStackIn.pushPose();
        matrixStackIn.translate(0F, 0.5F, 0F);
        matrixStackIn.translate(0F, -0.25F, 0F);
        matrixStackIn.rotateDegrees(Axis.YP, (yaw - 180));
        matrixStackIn.rotateDegrees(Axis.XP, (f * 20));
        matrixStackIn.translate(0F, 0.25F, 0F);
        MODEL_SPIRIT.renderToBuffer(matrixStackIn, ivertexbuilder2, packedLightIn, OverlayTexture.NO_OVERLAY, 1.0F, 1.0F, 1.0F, 1.0F);
        matrixStackIn.popPose();

        matrixStackIn.pushPose();
        matrixStackIn.translate(0F, 0.5F, 0F);
        matrixStackIn.translate(0F, -0.25F, 0F);
        matrixStackIn.rotateDegrees(Axis.YP, (yaw - 180));
        matrixStackIn.rotateDegrees(Axis.XP, (f * 15));
        matrixStackIn.translate(0F, 0.25F, 0F);
        matrixStackIn.scale(1.5F, 1.5F, 1.5F);
        MODEL_SPIRIT.renderToBuffer(matrixStackIn, ivertexbuilder, packedLightIn, OverlayTexture.NO_OVERLAY, 1.0F, 1.0F, 1.0F, 1.0F);
        matrixStackIn.popPose();

        matrixStackIn.pushPose();
        matrixStackIn.translate(0F, 0.75F, 0F);
        matrixStackIn.translate(0F, -0.25F, 0F);
        matrixStackIn.rotateDegrees(Axis.YP, (yaw - 180));
        matrixStackIn.rotateDegrees(Axis.XP, (f * 10));
        matrixStackIn.translate(0F, 0.75F, 0F);
        matrixStackIn.scale(2.5F, 2.5F, 2.5F);
        MODEL_SPIRIT.renderToBuffer(matrixStackIn, ivertexbuilder, packedLightIn, OverlayTexture.NO_OVERLAY, 1.0F, 1.0F, 1.0F, 1.0F);
        matrixStackIn.popPose();

        matrixStackIn.popPose();
        super.render(entity, entityYaw, partialTicks, matrixStackIn, bufferIn, packedLightIn);
    }

    private float interpolateValue(float start, float end, float pct) {
        return start + (end - start) * pct;
    }

    @Nullable
    @Override
    public Identifier getTextureLocation(EntityDragonLightningCharge entity) {
        return TEXTURE;
    }
}

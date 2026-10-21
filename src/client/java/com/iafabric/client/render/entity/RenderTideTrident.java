package com.iafabric.client.render.entity;

import net.minecraft.client.renderer.rendertype.RenderTypes;
import com.mojang.math.Axis;
import com.iafabric.client.render.BufferSource;
import com.iafabric.client.model.ModelTideTrident;
import com.iafabric.entity.EntityTideTrident;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.NotNull;

public class RenderTideTrident extends IafEntityRenderer<EntityTideTrident> {
    public static final Identifier TRIDENT = Identifier.parse("iceandfire_fabric:textures/models/misc/tide_trident.png");
    private final ModelTideTrident tridentModel = new ModelTideTrident();

    public RenderTideTrident(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public void render(EntityTideTrident entityIn, float entityYaw, float partialTicks, PoseStack matrixStackIn, @NotNull BufferSource bufferIn, int packedLightIn) {
        matrixStackIn.pushPose();
        matrixStackIn.rotateDegrees(com.mojang.math.Axis.YP, (Mth.lerp(partialTicks, entityIn.yRotO, entityIn.getYRot()) - 90.0F));
        matrixStackIn.rotateDegrees(com.mojang.math.Axis.ZP, (Mth.lerp(partialTicks, entityIn.xRotO, entityIn.getXRot()) + 90.0F));
        VertexConsumer ivertexbuilder = bufferIn.getBuffer(RenderTypes.entitySolid(this.getTextureLocation(entityIn)));
        this.tridentModel.renderToBuffer(matrixStackIn, ivertexbuilder, packedLightIn, OverlayTexture.NO_OVERLAY, 1.0F, 1.0F, 1.0F, 1.0F);
        matrixStackIn.popPose();
    }

    /**
     * Returns the location of an entity's texture.
     */
    @Override
    public Identifier getTextureLocation(EntityTideTrident entity) {
        return TRIDENT;
    }

}
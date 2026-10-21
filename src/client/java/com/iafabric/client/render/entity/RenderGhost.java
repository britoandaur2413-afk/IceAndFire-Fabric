package com.iafabric.client.render.entity;

import com.mojang.math.Axis;
import com.iafabric.client.render.BufferSource;
import com.iafabric.client.render.entity.IafMobRenderer;
import com.iafabric.client.model.ModelGhost;
import com.iafabric.client.render.IafRenderType;
import com.iafabric.entity.EntityGhost;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;
import org.jetbrains.annotations.NotNull;

public class RenderGhost extends IafMobRenderer<EntityGhost, ModelGhost> {

    public static final Identifier TEXTURE_0 = Identifier.parse("iceandfire_fabric:textures/models/ghost/ghost_white.png");
    public static final Identifier TEXTURE_1 = Identifier.parse("iceandfire_fabric:textures/models/ghost/ghost_blue.png");
    public static final Identifier TEXTURE_2 = Identifier.parse("iceandfire_fabric:textures/models/ghost/ghost_green.png");
    public static final Identifier TEXTURE_SHOPPING_LIST = Identifier.parse("iceandfire_fabric:textures/models/ghost/haunted_shopping_list.png");

    public RenderGhost(EntityRendererProvider.Context renderManager) {
        super(renderManager, new ModelGhost(0.0F), 0.0F);

    }

    public static Identifier getGhostOverlayForType(int ghost) {
        switch (ghost) {
            case 1:
                return TEXTURE_1;
            case 2:
                return TEXTURE_2;
            case -1:
                return TEXTURE_SHOPPING_LIST;
            default:
                return TEXTURE_0;
        }
    }

    @Override
    protected void renderBody(EntityGhost entityIn, PoseStack matrixStackIn, BufferSource bufferIn, int packedLightIn, int overlay, float partialTicks) {
        if (entityIn.isInvisible()) {
            return;
        }
        float alphaForRender = this.getAlphaForRender(entityIn, partialTicks);
        RenderType rendertype = entityIn.isDaytimeMode() ? IafRenderType.getGhostDaytime(getTextureLocation(entityIn)) : IafRenderType.getGhost(getTextureLocation(entityIn));
        VertexConsumer ivertexbuilder = bufferIn.getBuffer(rendertype);
        if (entityIn.isHauntedShoppingList()) {
            matrixStackIn.pushPose();
            matrixStackIn.translate(0, 0.8F + Mth.sin((entityIn.tickCount + partialTicks) * 0.15F) * 0.1F, 0);
            matrixStackIn.scale(0.6F, 0.6F, 0.6F);
            matrixStackIn.rotateDegrees(com.mojang.math.Axis.YP, (180.0F));
            Matrix4f matrix4f = matrixStackIn.last().pose();
            this.drawVertex(matrix4f, null, ivertexbuilder, overlay, (int) (alphaForRender * 255), -1, -2, 0, 1F, 0.0F, 0, 1, 0, 240);
            this.drawVertex(matrix4f, null, ivertexbuilder, overlay, (int) (alphaForRender * 255), 1, -2, 0, 0.5F, 0.0F, 0, 1, 0, 240);
            this.drawVertex(matrix4f, null, ivertexbuilder, overlay, (int) (alphaForRender * 255), 1, 2, 0, 0.5F, 1, 0, 1, 0, 240);
            this.drawVertex(matrix4f, null, ivertexbuilder, overlay, (int) (alphaForRender * 255), -1, 2, 0, 1F, 1, 0, 1, 0, 240);
            matrixStackIn.rotateDegrees(com.mojang.math.Axis.YP, (180.0F));
            matrix4f = matrixStackIn.last().pose();
            this.drawVertex(matrix4f, null, ivertexbuilder, overlay, (int) (alphaForRender * 255), -1, -2, 0, 0.0F, 0.0F, 0, 1, 0, 240);
            this.drawVertex(matrix4f, null, ivertexbuilder, overlay, (int) (alphaForRender * 255), 1, -2, 0, 0.5F, 0.0F, 0, 1, 0, 240);
            this.drawVertex(matrix4f, null, ivertexbuilder, overlay, (int) (alphaForRender * 255), 1, 2, 0, 0.5F, 1, 0, 1, 0, 240);
            this.drawVertex(matrix4f, null, ivertexbuilder, overlay, (int) (alphaForRender * 255), -1, 2, 0, 0.0F, 1, 0, 1, 0, 240);
            matrixStackIn.popPose();
        } else {
            this.model.renderToBuffer(matrixStackIn, ivertexbuilder, 240, overlay, 1.0F, 1.0F, 1.0F, alphaForRender);
        }
    }

    @Override
    protected float getFlipDegrees(@NotNull EntityGhost ghost) {
        return 0.0F;
    }

    public float getAlphaForRender(EntityGhost entityIn, float partialTicks) {
        if (entityIn.isDaytimeMode()) {
            return Mth.clamp((101 - Math.min(entityIn.getDaytimeCounter(), 100)) / 100F, 0, 1);
        }
        return Mth.clamp((Mth.sin((entityIn.tickCount + partialTicks) * 0.1F) + 1F) * 0.5F + 0.1F, 0F, 1F);
    }

    @Override
    public void scale(@NotNull EntityGhost LivingEntityIn, @NotNull PoseStack stack, float partialTickTime) {
    }

    @Override
    public Identifier getTextureLocation(EntityGhost ghost) {
        switch (ghost.getColor()) {
            case 1:
                return TEXTURE_1;
            case 2:
                return TEXTURE_2;
            case -1:
                return TEXTURE_SHOPPING_LIST;
            default:
                return TEXTURE_0;
        }
    }

    public void drawVertex(Matrix4f stack, Matrix3f normal, VertexConsumer builder, int packedRed, int alphaInt, int x, int y, int z, float u, float v, int lightmap, int lightmap3, int lightmap2, int lightmap4) {
        builder.addVertex(stack, (float) x, (float) y, (float) z).setColor(255, 255, 255, alphaInt).setUv(u, v).setOverlay(packedRed).setLight(lightmap4).setNormal((float) lightmap, (float) lightmap2, (float) lightmap3);
    }
}

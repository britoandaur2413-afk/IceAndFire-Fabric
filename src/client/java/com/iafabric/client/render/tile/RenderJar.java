package com.iafabric.client.render.tile;

import com.mojang.math.Axis;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import com.iafabric.client.render.BufferSource;
import com.iafabric.client.model.ModelPixie;
import com.iafabric.client.render.entity.RenderPixie;
import com.iafabric.entity.tile.TileEntityJar;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import org.jetbrains.annotations.NotNull;

public class RenderJar<T extends TileEntityJar> extends IafBlockEntityRenderer<T> {

    public static final RenderType TEXTURE_0 = RenderTypes.entityCutout(RenderPixie.TEXTURE_0);
    public static final RenderType TEXTURE_1 = RenderTypes.entityCutout(RenderPixie.TEXTURE_1);
    public static final RenderType TEXTURE_2 = RenderTypes.entityCutout(RenderPixie.TEXTURE_2);
    public static final RenderType TEXTURE_3 = RenderTypes.entityCutout(RenderPixie.TEXTURE_3);
    public static final RenderType TEXTURE_4 = RenderTypes.entityCutout(RenderPixie.TEXTURE_4);
    public static final RenderType TEXTURE_5 = RenderTypes.entityCutout(RenderPixie.TEXTURE_5);
    public static final RenderType TEXTURE_0_GLO = RenderTypes.eyes(RenderPixie.TEXTURE_0);
    public static final RenderType TEXTURE_1_GLO = RenderTypes.eyes(RenderPixie.TEXTURE_1);
    public static final RenderType TEXTURE_2_GLO = RenderTypes.eyes(RenderPixie.TEXTURE_2);
    public static final RenderType TEXTURE_3_GLO = RenderTypes.eyes(RenderPixie.TEXTURE_3);
    public static final RenderType TEXTURE_4_GLO = RenderTypes.eyes(RenderPixie.TEXTURE_4);
    public static final RenderType TEXTURE_5_GLO = RenderTypes.eyes(RenderPixie.TEXTURE_5);
    private static ModelPixie MODEL_PIXIE;

    public RenderJar(BlockEntityRendererProvider.Context context) {

    }

    @Override
    public void render(T entity, float partialTicks, PoseStack matrixStackIn, BufferSource bufferIn, int combinedLightIn, int combinedOverlayIn) {
        int meta = 0;
        boolean hasPixie = false;
        if (MODEL_PIXIE == null) {
            MODEL_PIXIE = new ModelPixie();
        }
        if (entity != null && entity.getLevel() != null) {
            meta = entity.pixieType;
            hasPixie = entity.hasPixie;
        }
        if (hasPixie) {
            matrixStackIn.pushPose();
            matrixStackIn.translate(0.5F, 1.501F, 0.5F);
            matrixStackIn.rotateDegrees(Axis.XP, (180));
            matrixStackIn.pushPose();
            RenderType type = TEXTURE_0;
            RenderType typeGlow = TEXTURE_0_GLO;
            switch (meta) {
                default:
                    type = TEXTURE_0;
                    typeGlow = TEXTURE_0_GLO;
                    break;
                case 1:
                    type = TEXTURE_1;
                    typeGlow = TEXTURE_1_GLO;
                    break;
                case 2:
                    type = TEXTURE_2;
                    typeGlow = TEXTURE_2_GLO;
                    break;
                case 3:
                    type = TEXTURE_3;
                    typeGlow = TEXTURE_3_GLO;
                    break;
                case 4:
                    type = TEXTURE_4;
                    typeGlow = TEXTURE_4_GLO;
                    break;
            }
            VertexConsumer ivertexbuilder = bufferIn.getBuffer(type);
            if (entity != null && entity.getLevel() != null) {

                if (entity.hasProduced) {
                    matrixStackIn.translate(0F, 0.90F, 0F);
                } else {
                    matrixStackIn.translate(0F, 0.60F, 0F);
                }
                matrixStackIn.rotateDegrees(Axis.YP, (this.interpolateRotation(entity.prevRotationYaw, entity.rotationYaw, partialTicks)));
                matrixStackIn.scale(0.50F, 0.50F, 0.50F);
                MODEL_PIXIE.animateInJar(entity.hasProduced, entity, 0);
                MODEL_PIXIE.renderToBuffer(matrixStackIn, ivertexbuilder, combinedLightIn, combinedOverlayIn, 1.0F, 1.0F, 1.0F, 1.0F);
                MODEL_PIXIE.renderToBuffer(matrixStackIn, bufferIn.getBuffer(typeGlow), combinedLightIn, combinedOverlayIn, 1.0F, 1.0F, 1.0F, 1.0F);
            }
            matrixStackIn.popPose();
            matrixStackIn.popPose();
        }
    }

    protected float interpolateRotation(float prevYawOffset, float yawOffset, float partialTicks) {
        float f;

        for (f = yawOffset - prevYawOffset; f < -180.0F; f += 360.0F) {
        }

        while (f >= 180.0F) {
            f -= 360.0F;
        }

        return prevYawOffset + partialTicks * f;
    }


}

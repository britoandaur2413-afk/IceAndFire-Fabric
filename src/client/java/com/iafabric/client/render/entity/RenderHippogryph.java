package com.iafabric.client.render.entity;

import net.minecraft.client.renderer.RenderType;
import com.iafabric.client.render.BufferSource;
import com.iafabric.client.render.entity.IafRenderLayer;
import com.iafabric.client.render.entity.IafMobRenderer;
import com.iafabric.client.model.ModelHippogryph;
import com.iafabric.entity.EntityHippogryph;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

import org.jetbrains.annotations.Nullable;

public class RenderHippogryph extends IafMobRenderer<EntityHippogryph, ModelHippogryph> {

    public RenderHippogryph(EntityRendererProvider.Context context) {
        super(context, new ModelHippogryph(), 0.8F);
        this.layers.add(new LayerHippogriffSaddle(this));

    }

    @Override
    protected void scale(@NotNull EntityHippogryph entity, PoseStack matrix, float partialTickTime) {
        matrix.scale(1.2F, 1.2F, 1.2F);
    }

    @Nullable
    @Override
    public ResourceLocation getTextureLocation(EntityHippogryph entity) {
        return entity.isBlinking() ? entity.getEnumVariant().TEXTURE_BLINK : entity.getEnumVariant().TEXTURE;
    }


    private class LayerHippogriffSaddle extends IafRenderLayer<EntityHippogryph, ModelHippogryph> {
        private final RenderHippogryph renderer;
        private final RenderType SADDLE_TEXTURE = RenderType.entityCutoutNoCullZOffset(ResourceLocation.parse("iceandfire_fabric:textures/models/hippogryph/saddle.png"));
        private final RenderType BRIDLE = RenderType.entityCutoutNoCullZOffset(ResourceLocation.parse("iceandfire_fabric:textures/models/hippogryph/bridle.png"));
        private final RenderType CHEST = RenderType.entityTranslucent(ResourceLocation.parse("iceandfire_fabric:textures/models/hippogryph/chest.png"));
        private final RenderType TEXTURE_DIAMOND = RenderType.entityCutoutNoCullZOffset(ResourceLocation.parse("iceandfire_fabric:textures/models/hippogryph/armor_diamond.png"));
        private final RenderType TEXTURE_GOLD = RenderType.entityCutoutNoCullZOffset(ResourceLocation.parse("iceandfire_fabric:textures/models/hippogryph/armor_gold.png"));
        private final RenderType TEXTURE_IRON = RenderType.entityCutoutNoCullZOffset(ResourceLocation.parse("iceandfire_fabric:textures/models/hippogryph/armor_iron.png"));


        public LayerHippogriffSaddle(RenderHippogryph renderer) {
            super(renderer);
            this.renderer = renderer;
        }

        @Override
        public void render(@NotNull PoseStack matrixStackIn, @NotNull BufferSource bufferIn, int packedLightIn, EntityHippogryph hippo, float limbSwing, float limbSwingAmount, float partialTicks, float ageInTicks, float netHeadYaw, float headPitch) {
            if (hippo.getArmor() != 0) {
                RenderType type = null;
                switch (hippo.getArmor()) {
                    case 1:
                        type = TEXTURE_IRON;
                        break;
                    case 2:
                        type = TEXTURE_GOLD;
                        break;
                    case 3:
                        type = TEXTURE_DIAMOND;
                        break;
                }
                VertexConsumer ivertexbuilder = bufferIn.getBuffer(type);
                this.getParentModel().renderToBuffer(matrixStackIn, ivertexbuilder, packedLightIn, OverlayTexture.NO_OVERLAY, 1.0F, 1.0F, 1.0F, 1.0F);
            }
            if (hippo.isSaddled()) {
                VertexConsumer ivertexbuilder = bufferIn.getBuffer(SADDLE_TEXTURE);
                this.getParentModel().renderToBuffer(matrixStackIn, ivertexbuilder, packedLightIn, OverlayTexture.NO_OVERLAY, 1.0F, 1.0F, 1.0F, 1.0F);
            }
            if (hippo.isSaddled() && hippo.getControllingPassenger() != null) {
                VertexConsumer ivertexbuilder = bufferIn.getBuffer(BRIDLE);
                this.getParentModel().renderToBuffer(matrixStackIn, ivertexbuilder, packedLightIn, OverlayTexture.NO_OVERLAY, 1.0F, 1.0F, 1.0F, 1.0F);
            }
            if (hippo.isChested()) {
                VertexConsumer ivertexbuilder = bufferIn.getBuffer(CHEST);
                this.getParentModel().renderToBuffer(matrixStackIn, ivertexbuilder, packedLightIn, OverlayTexture.NO_OVERLAY, 1.0F, 1.0F, 1.0F, 1.0F);
            }
        }
    }
}

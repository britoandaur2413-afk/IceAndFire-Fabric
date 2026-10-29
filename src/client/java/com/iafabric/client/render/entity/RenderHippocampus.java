package com.iafabric.client.render.entity;

import net.minecraft.client.renderer.RenderType;
import com.iafabric.client.render.BufferSource;
import com.iafabric.client.render.entity.IafRenderLayer;
import com.iafabric.client.render.entity.IafMobRenderer;
import com.iafabric.client.model.ModelHippocampus;
import com.iafabric.entity.EntityHippocampus;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.animal.Sheep;
import net.minecraft.world.item.DyeColor;
import org.jetbrains.annotations.NotNull;

import org.jetbrains.annotations.Nullable;


public class RenderHippocampus extends IafMobRenderer<EntityHippocampus, ModelHippocampus> {

    private static final ResourceLocation VARIANT_0 = ResourceLocation.parse("iceandfire_fabric:textures/models/hippocampus/hippocampus_0.png");
    private static final ResourceLocation VARIANT_0_BLINK = ResourceLocation.parse("iceandfire_fabric:textures/models/hippocampus/hippocampus_0_blinking.png");
    private static final ResourceLocation VARIANT_1 = ResourceLocation.parse("iceandfire_fabric:textures/models/hippocampus/hippocampus_1.png");
    private static final ResourceLocation VARIANT_1_BLINK = ResourceLocation.parse("iceandfire_fabric:textures/models/hippocampus/hippocampus_1_blinking.png");
    private static final ResourceLocation VARIANT_2 = ResourceLocation.parse("iceandfire_fabric:textures/models/hippocampus/hippocampus_2.png");
    private static final ResourceLocation VARIANT_2_BLINK = ResourceLocation.parse("iceandfire_fabric:textures/models/hippocampus/hippocampus_2_blinking.png");
    private static final ResourceLocation VARIANT_3 = ResourceLocation.parse("iceandfire_fabric:textures/models/hippocampus/hippocampus_3.png");
    private static final ResourceLocation VARIANT_3_BLINK = ResourceLocation.parse("iceandfire_fabric:textures/models/hippocampus/hippocampus_3_blinking.png");
    private static final ResourceLocation VARIANT_4 = ResourceLocation.parse("iceandfire_fabric:textures/models/hippocampus/hippocampus_4.png");
    private static final ResourceLocation VARIANT_4_BLINK = ResourceLocation.parse("iceandfire_fabric:textures/models/hippocampus/hippocampus_4_blinking.png");
    private static final ResourceLocation VARIANT_5 = ResourceLocation.parse("iceandfire_fabric:textures/models/hippocampus/hippocampus_5.png");
    private static final ResourceLocation VARIANT_5_BLINK = ResourceLocation.parse("iceandfire_fabric:textures/models/hippocampus/hippocampus_5_blinking.png");


    public RenderHippocampus(EntityRendererProvider.Context context) {
        super(context, new ModelHippocampus(), 0.8F);
        this.layers.add(new RenderHippocampus.LayerHippocampusRainbow(this));
        this.layers.add(new RenderHippocampus.LayerHippocampusSaddle(this));
    }

    @Nullable
    @Override
    public ResourceLocation getTextureLocation(EntityHippocampus entity) {
        switch (entity.getVariant()) {
            default:
                return entity.isBlinking() ? VARIANT_0_BLINK : VARIANT_0;
            case 1:
                return entity.isBlinking() ? VARIANT_1_BLINK : VARIANT_1;
            case 2:
                return entity.isBlinking() ? VARIANT_2_BLINK : VARIANT_2;
            case 3:
                return entity.isBlinking() ? VARIANT_3_BLINK : VARIANT_3;
            case 4:
                return entity.isBlinking() ? VARIANT_4_BLINK : VARIANT_4;
            case 5:
                return entity.isBlinking() ? VARIANT_5_BLINK : VARIANT_5;

        }
    }


    private class LayerHippocampusSaddle extends IafRenderLayer<EntityHippocampus, ModelHippocampus> {
        private final RenderHippocampus renderer;
        private final RenderType SADDLE_TEXTURE = RenderType.entityCutoutNoCullZOffset(ResourceLocation.parse("iceandfire_fabric:textures/models/hippocampus/saddle.png"));
        private final RenderType BRIDLE = RenderType.entityCutoutNoCullZOffset(ResourceLocation.parse("iceandfire_fabric:textures/models/hippocampus/bridle.png"));
        private final RenderType CHEST = RenderType.entityTranslucent(ResourceLocation.parse("iceandfire_fabric:textures/models/hippocampus/chest.png"));
        private final RenderType TEXTURE_DIAMOND = RenderType.entityCutoutNoCullZOffset(ResourceLocation.parse("iceandfire_fabric:textures/models/hippocampus/armor_diamond.png"));
        private final RenderType TEXTURE_GOLD = RenderType.entityCutoutNoCullZOffset(ResourceLocation.parse("iceandfire_fabric:textures/models/hippocampus/armor_gold.png"));
        private final RenderType TEXTURE_IRON = RenderType.entityCutoutNoCullZOffset(ResourceLocation.parse("iceandfire_fabric:textures/models/hippocampus/armor_iron.png"));

        public LayerHippocampusSaddle(RenderHippocampus renderer) {
            super(renderer);
            this.renderer = renderer;
        }

        @Override
        public void render(@NotNull PoseStack matrixStackIn, @NotNull BufferSource bufferIn, int packedLightIn, EntityHippocampus hippo, float limbSwing, float limbSwingAmount, float partialTicks, float ageInTicks, float netHeadYaw, float headPitch) {
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
        }
    }

    private class LayerHippocampusRainbow extends IafRenderLayer<EntityHippocampus, ModelHippocampus> {
        private final RenderHippocampus renderer;
        private final RenderType TEXTURE = RenderType.entityCutoutNoCullZOffset(ResourceLocation.parse("iceandfire_fabric:textures/models/hippocampus/rainbow.png"));
        private final RenderType TEXTURE_BLINK = RenderType.entityCutoutNoCullZOffset(ResourceLocation.parse("iceandfire_fabric:textures/models/hippocampus/rainbow_blink.png"));

        public LayerHippocampusRainbow(RenderHippocampus renderer) {
            super(renderer);
            this.renderer = renderer;
        }

        @Override
        public void render(@NotNull PoseStack matrixStackIn, @NotNull BufferSource bufferIn, int packedLightIn, EntityHippocampus hippo, float limbSwing, float limbSwingAmount, float partialTicks, float ageInTicks, float netHeadYaw, float headPitch) {
            if (hippo.hasCustomName() && hippo.getCustomName().toString().toLowerCase().contains("rainbow")) {
                VertexConsumer ivertexbuilder = bufferIn.getBuffer(hippo.isBlinking() ? TEXTURE_BLINK : TEXTURE);
                int i1 = 25;
                int i = hippo.tickCount / 25 + hippo.getId();
                int j = DyeColor.values().length;
                int k = i % j;
                int l = (i + 1) % j;
                float f = ((float) (hippo.tickCount % 25) + partialTicks) / 25.0F;
                int c1 = DyeColor.byId(k).getTextureDiffuseColor();
                float[] afloat1 = {((c1 >> 16) & 255) / 255F, ((c1 >> 8) & 255) / 255F, (c1 & 255) / 255F};
                int c2 = DyeColor.byId(l).getTextureDiffuseColor();
                float[] afloat2 = {((c2 >> 16) & 255) / 255F, ((c2 >> 8) & 255) / 255F, (c2 & 255) / 255F};
                this.getParentModel().renderToBuffer(matrixStackIn, ivertexbuilder, packedLightIn, IafMobRenderer.getOverlayCoords(hippo, 0.0F), afloat1[0] * (1.0F - f) + afloat2[0] * f, afloat1[1] * (1.0F - f) + afloat2[1] * f, afloat1[2] * (1.0F - f) + afloat2[2] * f, 1.0F);
            }
        }
    }
}

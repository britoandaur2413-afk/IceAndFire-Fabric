package com.iafabric.client.render.entity.layer;

import net.minecraft.client.renderer.RenderType;
import com.iafabric.client.render.BufferSource;
import com.iafabric.client.render.entity.IafRenderLayer;
import com.iafabric.client.model.ModelGorgon;
import com.iafabric.client.render.entity.RenderGorgon;
import com.iafabric.entity.EntityGorgon;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

public class LayerGorgonEyes extends IafRenderLayer<EntityGorgon, ModelGorgon> {
    private static final ResourceLocation TEXTURE = ResourceLocation.parse("iceandfire_fabric:textures/models/gorgon/gorgon_eyes.png");
    private final RenderGorgon render;

    public LayerGorgonEyes(RenderGorgon renderIn) {
        super(renderIn);
        this.render = renderIn;
    }

    @Override
    public void render(@NotNull PoseStack matrixStackIn, @NotNull BufferSource bufferIn, int packedLightIn, EntityGorgon entity, float limbSwing, float limbSwingAmount, float partialTicks, float ageInTicks, float netHeadYaw, float headPitch) {
        if (entity.getAnimation() == EntityGorgon.ANIMATION_SCARE || entity.getAnimation() == EntityGorgon.ANIMATION_HIT) {
            RenderType eyes = RenderType.eyes(TEXTURE);
            VertexConsumer ivertexbuilder = bufferIn.getBuffer(eyes);
            this.getParentModel().renderToBuffer(matrixStackIn, ivertexbuilder, packedLightIn, OverlayTexture.NO_OVERLAY, 1.0F, 1.0F, 1.0F, 1.0F);
        }
    }

    public boolean shouldCombineTextures() {
        return false;
    }
}
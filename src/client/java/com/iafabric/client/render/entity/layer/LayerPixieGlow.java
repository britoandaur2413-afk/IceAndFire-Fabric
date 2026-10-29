package com.iafabric.client.render.entity.layer;

import net.minecraft.client.renderer.RenderType;
import com.iafabric.client.render.BufferSource;
import com.iafabric.client.render.entity.IafRenderLayer;
import com.iafabric.client.model.ModelPixie;
import com.iafabric.client.render.entity.RenderPixie;
import com.iafabric.entity.EntityPixie;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

public class LayerPixieGlow extends IafRenderLayer<EntityPixie, ModelPixie> {

    private final RenderPixie render;

    public LayerPixieGlow(RenderPixie renderIn) {
        super(renderIn);
        this.render = renderIn;
    }

    @Override
    public void render(@NotNull PoseStack matrixStackIn, @NotNull BufferSource bufferIn, int packedLightIn, EntityPixie pixie, float limbSwing, float limbSwingAmount, float partialTicks, float ageInTicks, float netHeadYaw, float headPitch) {
        // its own skin glowing (an exiled pixie's, faded, a little less)
        ResourceLocation texture = this.render.getTextureLocation(pixie);
        float glow = pixie instanceof com.iafabric.entity.EntityExiledPixie ? 0.85F : 1.0F;
        RenderType eyes = RenderType.eyes(texture);
        VertexConsumer ivertexbuilder = bufferIn.getBuffer(eyes);
        this.getParentModel().renderToBuffer(matrixStackIn, ivertexbuilder, packedLightIn, OverlayTexture.NO_OVERLAY, glow, glow, glow, 1.0F);
    }
}
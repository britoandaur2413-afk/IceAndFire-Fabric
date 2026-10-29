package com.iafabric.client.render.entity.layer;

import net.minecraft.client.renderer.RenderType;
import com.iafabric.client.render.BufferSource;
import com.iafabric.client.render.entity.IafRenderLayer;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.iafabric.citadel.client.model.basic.BasicEntityModel;
import com.iafabric.client.render.entity.IafMobRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import org.jetbrains.annotations.NotNull;

public class LayerGenericGlowing<T extends LivingEntity, M extends BasicEntityModel<? super T>> extends IafRenderLayer<T, M> {
    private final ResourceLocation texture;

    public LayerGenericGlowing(IafMobRenderer<T, M> renderIn, ResourceLocation texture) {
        super(renderIn);
        this.texture = texture;
    }

    public boolean shouldCombineTextures() {
        return true;
    }

    @Override
    public void render(@NotNull PoseStack matrixStackIn, BufferSource bufferIn, int packedLightIn, @NotNull T entity, float limbSwing, float limbSwingAmount, float partialTicks, float ageInTicks, float netHeadYaw, float headPitch) {
        RenderType eyes = RenderType.eyes(texture);
        VertexConsumer ivertexbuilder = bufferIn.getBuffer(eyes);
        this.getParentModel().renderToBuffer(matrixStackIn, ivertexbuilder, packedLightIn, OverlayTexture.NO_OVERLAY, 1.0F, 1.0F, 1.0F, 1.0F);
    }
}
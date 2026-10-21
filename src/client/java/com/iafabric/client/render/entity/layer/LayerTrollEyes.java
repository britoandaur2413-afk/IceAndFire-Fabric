package com.iafabric.client.render.entity.layer;

import net.minecraft.client.renderer.rendertype.RenderTypes;
import com.iafabric.client.render.BufferSource;
import com.iafabric.client.render.entity.IafRenderLayer;
import com.iafabric.client.model.ModelTroll;
import com.iafabric.client.render.entity.RenderTroll;
import com.iafabric.entity.EntityGorgon;
import com.iafabric.entity.EntityTroll;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import org.jetbrains.annotations.NotNull;

public class LayerTrollEyes extends IafRenderLayer<EntityTroll, ModelTroll> {

    private final RenderTroll renderer;

    public LayerTrollEyes(RenderTroll renderer) {
        super(renderer);
        this.renderer = renderer;
    }

    @Override
    public void render(@NotNull PoseStack matrixStackIn, @NotNull BufferSource bufferIn, int packedLightIn, @NotNull EntityTroll troll, float limbSwing, float limbSwingAmount, float partialTicks, float ageInTicks, float netHeadYaw, float headPitch) {
        if (!EntityGorgon.isStoneMob(troll)) {
            RenderType tex = RenderTypes.eyes(troll.getTrollType().TEXTURE_EYES);
            VertexConsumer ivertexbuilder = bufferIn.getBuffer(tex);
            this.getParentModel().renderToBuffer(matrixStackIn, ivertexbuilder, packedLightIn, OverlayTexture.NO_OVERLAY, 1.0F, 1.0F, 1.0F, 1.0F);

        }
    }
}

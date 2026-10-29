package com.iafabric.client.render.entity.layer;

import com.iafabric.citadel.client.model.TabulaModel;
import com.iafabric.client.render.entity.IafMobRenderer;
import net.minecraft.client.renderer.RenderType;
import com.iafabric.client.render.BufferSource;
import com.iafabric.client.render.entity.IafRenderLayer;
import com.iafabric.citadel.client.model.AdvancedEntityModel;
import com.iafabric.entity.EntitySeaSerpent;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

public class LayerSeaSerpentAncient extends IafRenderLayer<EntitySeaSerpent, TabulaModel> {

    private static final ResourceLocation TEXTURE = new ResourceLocation("iceandfire_fabric:textures/models/seaserpent/ancient_overlay.png");
    private static final ResourceLocation TEXTURE_BLINK = new ResourceLocation("iceandfire_fabric:textures/models/seaserpent/ancient_overlay_blink.png");

    public LayerSeaSerpentAncient(IafMobRenderer<EntitySeaSerpent, TabulaModel> renderer) {
        super(renderer);
    }

    @Override
    public void render(@NotNull PoseStack matrixStackIn, @NotNull BufferSource bufferIn, int packedLightIn, EntitySeaSerpent serpent, float limbSwing, float limbSwingAmount, float partialTicks, float ageInTicks, float netHeadYaw, float headPitch) {
        if (serpent.isAncient()) {
            RenderType tex = RenderType.entityCutoutNoCullZOffset(serpent.isBlinking() ? TEXTURE_BLINK : TEXTURE);
            VertexConsumer ivertexbuilder = bufferIn.getBuffer(tex);
            this.getParentModel().renderToBuffer(matrixStackIn, ivertexbuilder, packedLightIn, OverlayTexture.NO_OVERLAY, 1.0F, 1.0F, 1.0F, 1.0F);

        }
    }
}

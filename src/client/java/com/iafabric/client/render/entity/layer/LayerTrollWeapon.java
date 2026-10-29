package com.iafabric.client.render.entity.layer;

import net.minecraft.client.renderer.RenderType;
import com.iafabric.client.render.BufferSource;
import com.iafabric.client.render.entity.IafRenderLayer;
import com.iafabric.client.model.ModelTroll;
import com.iafabric.client.render.entity.RenderTroll;
import com.iafabric.entity.EntityGorgon;
import com.iafabric.entity.EntityTroll;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.texture.OverlayTexture;
import org.jetbrains.annotations.NotNull;

public class LayerTrollWeapon extends IafRenderLayer<EntityTroll, ModelTroll> {
    private final RenderTroll renderer;

    public LayerTrollWeapon(RenderTroll renderer) {
        super(renderer);
        this.renderer = renderer;
    }

    public void render(EntityTroll entity, float f, float f1, float i, float f2, float f3, float f4, float f5) {

    }

    @Override
    public void render(@NotNull PoseStack matrixStackIn, @NotNull BufferSource bufferIn, int packedLightIn, EntityTroll troll, float limbSwing, float limbSwingAmount, float partialTicks, float ageInTicks, float netHeadYaw, float headPitch) {
        if (troll.getWeaponType() != null && !EntityGorgon.isStoneMob(troll)) {
            RenderType tex = RenderType.entityCutoutNoCull(troll.getWeaponType().TEXTURE);
            this.getParentModel().renderToBuffer(matrixStackIn, bufferIn.getBuffer(tex), packedLightIn, OverlayTexture.NO_OVERLAY, 1.0F, 1.0F, 1.0F, 1.0F);
        }
    }
}
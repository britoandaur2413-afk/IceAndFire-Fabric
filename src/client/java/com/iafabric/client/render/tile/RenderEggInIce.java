package com.iafabric.client.render.tile;

import com.iafabric.client.render.BufferSource;
import com.iafabric.client.model.ModelDragonEgg;
import com.iafabric.entity.tile.TileEntityEggInIce;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import org.jetbrains.annotations.NotNull;

public class RenderEggInIce<T extends TileEntityEggInIce> extends IafBlockEntityRenderer<T> {

    public RenderEggInIce(BlockEntityRendererProvider.Context context) {

    }

    @Override
    public void render(T egg, float partialTicks, PoseStack matrixStackIn, BufferSource bufferIn, int combinedLightIn, int combinedOverlayIn) {
        ModelDragonEgg model = new ModelDragonEgg();
        if (egg.type != null) {
            matrixStackIn.pushPose();
            matrixStackIn.translate(0.5, -0.8F, 0.5F);
            matrixStackIn.pushPose();
            model.renderFrozen(egg);
            model.renderToBuffer(matrixStackIn, bufferIn.getBuffer(RenderPodium.getEggTexture(egg.type)), combinedLightIn, combinedOverlayIn, 1.0F, 1.0F, 1.0F, 1.0F);
            matrixStackIn.popPose();
            matrixStackIn.popPose();
        }
    }

}

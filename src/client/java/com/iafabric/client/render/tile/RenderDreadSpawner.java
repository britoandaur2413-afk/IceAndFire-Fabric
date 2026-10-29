package com.iafabric.client.render.tile;

import com.mojang.math.Axis;
import com.iafabric.client.render.BufferSource;
import com.iafabric.entity.tile.TileEntityDreadSpawner;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.BaseSpawner;
import org.jetbrains.annotations.NotNull;

public class RenderDreadSpawner<T extends TileEntityDreadSpawner> extends IafBlockEntityRenderer<T> {

    public RenderDreadSpawner(BlockEntityRendererProvider.Context context) {

    }

    @Override
    public void render(TileEntityDreadSpawner tileEntityIn, float partialTicks, PoseStack matrixStackIn, BufferSource bufferIn, int combinedLightIn, int combinedOverlayIn) {
        matrixStackIn.pushPose();
        matrixStackIn.translate(0.5D, 0.0D, 0.5D);
        BaseSpawner abstractspawner = tileEntityIn.getSpawner();
        Entity entity = abstractspawner.getOrCreateDisplayEntity(tileEntityIn.getLevel(), tileEntityIn.getLevel().getRandom(), tileEntityIn.getBlockPos());
        if (entity != null) {
            float f = 0.53125F;
            float f1 = Math.max(entity.getBbWidth(), entity.getBbHeight());
            if ((double) f1 > 1.0D) {
                f /= f1;
            }

            matrixStackIn.translate(0.0D, 0.4F, 0.0D);
            matrixStackIn.mulPose(com.mojang.math.Axis.YP.rotationDegrees(((float) Mth.lerp(partialTicks, abstractspawner.getoSpin(), abstractspawner.getSpin()) * 10.0F)));
            matrixStackIn.translate(0.0D, -0.2F, 0.0D);
            matrixStackIn.mulPose(com.mojang.math.Axis.XP.rotationDegrees((-30.0F)));
            matrixStackIn.scale(f, f, f);
            bufferIn.renderEntity(entity, partialTicks, matrixStackIn, combinedLightIn);
        }

        matrixStackIn.popPose();
    }
}
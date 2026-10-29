package com.iafabric.client.render.tile;

import net.minecraft.world.item.ItemDisplayContext;
import com.iafabric.client.render.IafItemRender;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.RenderType;
import com.iafabric.client.render.BufferSource;
import com.iafabric.client.model.ModelDragonEgg;
import com.iafabric.client.render.entity.RenderDragonEgg;
import com.iafabric.client.render.entity.RenderMyrmexEgg;
import com.iafabric.entity.tile.TileEntityPodium;
import com.iafabric.enums.EnumDragonEgg;
import com.iafabric.item.IafItemRegistry;
import com.iafabric.item.ItemDragonEgg;
import com.iafabric.item.ItemMyrmexEgg;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.NotNull;

public class RenderPodium<T extends TileEntityPodium> extends IafBlockEntityRenderer<T> {

    public RenderPodium(BlockEntityRendererProvider.Context context) {

    }

    protected static RenderType getEggTexture(EnumDragonEgg type) {
        switch (type) {
            default:
                return RenderType.entityCutoutNoCull(RenderDragonEgg.EGG_RED);
            case GREEN:
                return RenderType.entityCutoutNoCull(RenderDragonEgg.EGG_GREEN);
            case BRONZE:
                return RenderType.entityCutoutNoCull(RenderDragonEgg.EGG_BRONZE);
            case GRAY:
                return RenderType.entityCutoutNoCull(RenderDragonEgg.EGG_GREY);
            case BLUE:
                return RenderType.entityCutoutNoCull(RenderDragonEgg.EGG_BLUE);
            case WHITE:
                return RenderType.entityCutoutNoCull(RenderDragonEgg.EGG_WHITE);
            case SAPPHIRE:
                return RenderType.entityCutoutNoCull(RenderDragonEgg.EGG_SAPPHIRE);
            case SILVER:
                return RenderType.entityCutoutNoCull(RenderDragonEgg.EGG_SILVER);
            case ELECTRIC:
                return RenderType.entityCutoutNoCull(RenderDragonEgg.EGG_ELECTRIC);
            case AMYTHEST:
                return RenderType.entityCutoutNoCull(RenderDragonEgg.EGG_AMYTHEST);
            case COPPER:
                return RenderType.entityCutoutNoCull(RenderDragonEgg.EGG_COPPER);
            case BLACK:
                return RenderType.entityCutoutNoCull(RenderDragonEgg.EGG_BLACK);
        }
    }

    @Override
    public void render(T entity, float partialTicks, PoseStack matrixStackIn, BufferSource bufferIn, int combinedLightIn, int combinedOverlayIn) {
        ModelDragonEgg model = new ModelDragonEgg();
        TileEntityPodium podium = entity;

        if (!podium.getItem(0).isEmpty()) {
            if (podium.getItem(0).getItem() instanceof ItemDragonEgg) {
                ItemDragonEgg item = (ItemDragonEgg) podium.getItem(0).getItem();
                matrixStackIn.pushPose();
                matrixStackIn.translate(0.5F, 0.475F, 0.5F);
                matrixStackIn.pushPose();
                matrixStackIn.pushPose();
                model.renderPodium();
                model.renderToBuffer(matrixStackIn, bufferIn.getBuffer(RenderPodium.getEggTexture(item.type)), combinedLightIn, combinedOverlayIn, 1.0F, 1.0F, 1.0F, 1.0F);
                matrixStackIn.popPose();
                matrixStackIn.popPose();
                matrixStackIn.popPose();
            } else if (podium.getItem(0).getItem() instanceof ItemMyrmexEgg) {
                boolean jungle = podium.getItem(0).getItem() == IafItemRegistry.MYRMEX_JUNGLE_EGG;
                matrixStackIn.pushPose();
                matrixStackIn.translate(0.5F, 0.475F, 0.5F);
                matrixStackIn.pushPose();
                matrixStackIn.pushPose();
                model.renderPodium();
                model.renderToBuffer(matrixStackIn, bufferIn.getBuffer(RenderType.entityCutoutNoCull(jungle ? RenderMyrmexEgg.EGG_JUNGLE : RenderMyrmexEgg.EGG_DESERT)), combinedLightIn, combinedOverlayIn, 1.0F, 1.0F, 1.0F, 1.0F);
                matrixStackIn.popPose();
                matrixStackIn.popPose();
                matrixStackIn.popPose();
            } else if (!podium.getItem(0).isEmpty()) {
                //if (net.minecraftforge.common.MinecraftForge.EVENT_BUS.post(new RenderPodiumItemEvent(this, podium, f, x, y, z))) {
                matrixStackIn.pushPose();
                float f2 = ((float) podium.prevTicksExisted + (podium.ticksExisted - podium.prevTicksExisted) * partialTicks);
                float f3 = Mth.sin(f2 / 10.0F) * 0.1F + 0.1F;
                matrixStackIn.translate(0.5F, 1.55F + f3, 0.5F);
                float f4 = (f2 / 20.0F) * (180F / (float) Math.PI);
                matrixStackIn.mulPose(Axis.YP.rotationDegrees((f4)));
                matrixStackIn.pushPose();
                matrixStackIn.translate(0, 0.2F, 0);
                matrixStackIn.scale(0.65F, 0.65F, 0.65F);
                IafItemRender.render(podium.getItem(0), ItemDisplayContext.FIXED, combinedLightIn, matrixStackIn, bufferIn);
                matrixStackIn.popPose();
                matrixStackIn.popPose();
                //}
            }
        }

    }
}

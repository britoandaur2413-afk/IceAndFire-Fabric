package com.iafabric.client.render.entity;

import net.minecraft.world.item.ItemDisplayContext;
import com.iafabric.client.render.IafItemRender;
import com.mojang.math.Axis;
import com.iafabric.client.render.BufferSource;
import com.iafabric.entity.EntityGhostSword;
import com.iafabric.item.IafItemRegistry;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.model.cuboid.ItemTransforms;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

public class RenderGhostSword extends IafEntityRenderer<EntityGhostSword> {

    public RenderGhostSword(EntityRendererProvider.Context context) {
        super(context);
    }


    @Override
    public Identifier getTextureLocation(EntityGhostSword entity) {
        return TextureAtlas.LOCATION_BLOCKS;
    }

    @Override
    public void render(EntityGhostSword entityIn, float entityYaw, float partialTicks, PoseStack matrixStackIn, @NotNull BufferSource bufferIn, int packedLightIn) {
        matrixStackIn.pushPose();
        matrixStackIn.rotateDegrees(com.mojang.math.Axis.YP, (Mth.lerp(partialTicks, entityIn.yRotO, entityIn.getYRot()) - 90.0F));
        matrixStackIn.rotateDegrees(com.mojang.math.Axis.ZP, (Mth.lerp(partialTicks, entityIn.xRotO, entityIn.getXRot())));
        matrixStackIn.translate(0, 0.5F, 0);
        matrixStackIn.scale(2F, 2F, 2F);
        matrixStackIn.rotateDegrees(Axis.YP, (0F));
        matrixStackIn.rotateDegrees(Axis.ZN, ((entityIn.tickCount + partialTicks) * 30F));
        matrixStackIn.translate(0, -0.15F, 0);
        IafItemRender.render(new ItemStack(IafItemRegistry.GHOST_SWORD), ItemDisplayContext.GROUND, 240, matrixStackIn, bufferIn);
        matrixStackIn.popPose();


    }

}

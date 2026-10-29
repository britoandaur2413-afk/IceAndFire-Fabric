package com.iafabric.client.render.entity;

import com.mojang.math.Axis;
import com.iafabric.client.render.BufferSource;
import com.iafabric.block.IafBlockRegistry;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemDisplayContext;
import com.iafabric.client.render.IafItemRender;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.projectile.AbstractHurtingProjectile;
import net.minecraft.world.level.block.Blocks;
import org.jetbrains.annotations.NotNull;

public class RenderDragonFireCharge extends IafEntityRenderer<Entity> {

    public boolean isFire;

    public RenderDragonFireCharge(EntityRendererProvider.Context context, boolean isFire) {
        super(context);
        this.isFire = isFire;
    }

    @Override
    public ResourceLocation getTextureLocation(Entity entity) {
        return TextureAtlas.LOCATION_BLOCKS;
    }

    @Override
    public void render(@NotNull Entity entityIn, float entityYaw, float partialTicks, PoseStack matrixStackIn, @NotNull BufferSource bufferIn, int packedLightIn) {
        matrixStackIn.pushPose();
        if (entityIn instanceof com.iafabric.entity.EntityDragonCharge charge) {
            // leaving its dragon's mouth
            var shift = com.iafabric.client.particle.DragonBreathFx.chargeShift(charge, partialTicks);
            matrixStackIn.translate(shift.x, shift.y, shift.z);
        }
        matrixStackIn.translate(0.0D, 0.5D, 0.0D);
        matrixStackIn.mulPose(com.mojang.math.Axis.YP.rotationDegrees((-90.0F)));
        matrixStackIn.translate(-0.5D, -0.5D, 0.5D);
        matrixStackIn.mulPose(com.mojang.math.Axis.YP.rotationDegrees((90.0F)));
        matrixStackIn.translate(0.5D, 0.5D, -0.5D);
        IafItemRender.render(new ItemStack(isFire ? Blocks.MAGMA_BLOCK : IafBlockRegistry.DRAGON_ICE), ItemDisplayContext.NONE, packedLightIn, matrixStackIn, bufferIn);
        matrixStackIn.popPose();
    }

}

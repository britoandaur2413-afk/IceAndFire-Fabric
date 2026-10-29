package com.iafabric.client.render.entity.layer;

import net.minecraft.world.item.ItemDisplayContext;
import com.iafabric.client.render.IafItemRender;
import com.mojang.math.Axis;
import com.iafabric.client.render.BufferSource;
import com.iafabric.client.render.entity.IafRenderLayer;
import com.iafabric.client.model.ModelPixie;
import com.iafabric.client.render.entity.RenderPixie;
import com.iafabric.entity.EntityPixie;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

public class LayerPixieItem extends IafRenderLayer<EntityPixie, ModelPixie> {

    RenderPixie renderer;

    public LayerPixieItem(RenderPixie renderer) {
        super(renderer);
        this.renderer = renderer;
    }

    @Override
    public void render(@NotNull PoseStack matrixStackIn, @NotNull BufferSource bufferIn, int packedLightIn, EntityPixie entity, float limbSwing, float limbSwingAmount, float partialTicks, float ageInTicks, float netHeadYaw, float headPitch) {
        ItemStack itemstack = entity.getItemInHand(InteractionHand.MAIN_HAND);
        if (!itemstack.isEmpty()) {
            matrixStackIn.pushPose();
            matrixStackIn.translate(-0.0625F, 0.53125F, 0.21875F);
            Item item = itemstack.getItem();
            Minecraft minecraft = Minecraft.getInstance();
            if (!(item instanceof BlockItem)) {
                matrixStackIn.translate(-0.075F, 0, -0.05F);
            } else {
                matrixStackIn.translate(-0.075F, 0, -0.05F);
            }
            matrixStackIn.translate(0.05F, 0.55F, -0.4F);
            matrixStackIn.mulPose(Axis.XP.rotationDegrees((200)));
            matrixStackIn.mulPose(Axis.YP.rotationDegrees((180)));
            IafItemRender.render(itemstack, ItemDisplayContext.FIXED, packedLightIn, matrixStackIn, bufferIn);
            matrixStackIn.popPose();
        }
    }
}
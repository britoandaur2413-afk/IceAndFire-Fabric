package com.iafabric.client.render.entity.layer;

import net.minecraft.world.item.ItemDisplayContext;
import com.iafabric.client.render.IafItemRender;
import com.mojang.math.Axis;
import com.iafabric.client.render.BufferSource;
import com.iafabric.client.render.entity.IafRenderLayer;
import com.iafabric.citadel.client.model.AdvancedEntityModel;
import com.iafabric.client.model.ModelMyrmexBase;
import com.iafabric.client.render.entity.RenderMyrmexBase;
import com.iafabric.entity.EntityMyrmexBase;
import com.iafabric.entity.EntityMyrmexWorker;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.model.cuboid.ItemTransforms;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

public class LayerMyrmexItem extends IafRenderLayer<EntityMyrmexBase, AdvancedEntityModel<EntityMyrmexBase>> {

    protected final RenderMyrmexBase livingEntityRenderer;

    public LayerMyrmexItem(RenderMyrmexBase livingEntityRendererIn) {
        super(livingEntityRendererIn);
        this.livingEntityRenderer = livingEntityRendererIn;
    }

    private void renderHeldItem(EntityMyrmexBase myrmex, ItemStack stack, ItemDisplayContext transform, HumanoidArm handSide) {

    }

    protected void translateToHand(HumanoidArm side, PoseStack stack) {
        ((ModelMyrmexBase) this.livingEntityRenderer.getModel()).postRenderArm(0, stack);
    }

    public boolean shouldCombineTextures() {
        return false;
    }

    @Override
    public void render(@NotNull PoseStack matrixStackIn, @NotNull BufferSource bufferIn, int packedLightIn, @NotNull EntityMyrmexBase entitylivingbaseIn, float limbSwing, float limbSwingAmount, float partialTicks, float ageInTicks, float netHeadYaw, float headPitch) {
        if (entitylivingbaseIn instanceof EntityMyrmexWorker) {
            ItemStack itemstack = entitylivingbaseIn.getItemInHand(InteractionHand.MAIN_HAND);
            if (!itemstack.isEmpty()) {
                matrixStackIn.pushPose();
                if (!itemstack.isEmpty()) {
                    matrixStackIn.pushPose();

                    if (entitylivingbaseIn.isShiftKeyDown()) {
                        matrixStackIn.translate(0.0F, 0.2F, 0.0F);
                    }
                    this.translateToHand(HumanoidArm.RIGHT, matrixStackIn);
                    matrixStackIn.translate(0F, 0.3F, -1.6F);
                    if (itemstack.getItem() instanceof BlockItem) {
                        matrixStackIn.translate(0F, 0, 0.2F);
                    } else {
                        matrixStackIn.translate(0F, 0.2F, 0.3F);
                    }
                    matrixStackIn.rotateDegrees(Axis.XP, (160));
                    matrixStackIn.rotateDegrees(Axis.YP, (180));
                    IafItemRender.render(itemstack, ItemDisplayContext.FIXED, packedLightIn, matrixStackIn, bufferIn);
                    matrixStackIn.popPose();
                }
                matrixStackIn.popPose();
            }
        }
    }
}
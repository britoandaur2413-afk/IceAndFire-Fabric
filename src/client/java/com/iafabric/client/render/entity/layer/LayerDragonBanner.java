package com.iafabric.client.render.entity.layer;

import com.iafabric.citadel.client.model.AdvancedModelBox;
import com.iafabric.citadel.client.model.TabulaModel;
import com.iafabric.client.render.entity.IafMobRenderer;
import com.iafabric.client.render.entity.IafRenderLayer;
import com.iafabric.entity.EntityDragonBase;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import com.iafabric.client.render.BufferSource;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.BannerItem;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

/** A banner the dragon carries (in its off hand slot), hanging from its upper body. */
public class LayerDragonBanner extends IafRenderLayer<EntityDragonBase, TabulaModel> {

    public LayerDragonBanner(IafMobRenderer<EntityDragonBase, TabulaModel> renderer) {
        super(renderer);
    }

    @Override
    public void render(PoseStack poseStack, BufferSource buffers, int light, EntityDragonBase entity, float limbSwing, float limbSwingAmount,
        float partialTicks, float ageInTicks, float netHeadYaw, float headPitch) {
        ItemStack itemstack = entity.getItemInHand(InteractionHand.OFF_HAND);
        if (itemstack.isEmpty() || !(itemstack.getItem() instanceof BannerItem)) {
            return;
        }
        AdvancedModelBox body = getParentModel().getCube("BodyUpper");
        if (body == null) {
            return;
        }
        float inverse = 3F / entity.getRenderSize();
        poseStack.pushPose();
        body.translateRotate(poseStack);
        poseStack.translate(0, -0.2F, 0.4F);
        poseStack.mulPose(Axis.XP.rotationDegrees(180));
        poseStack.scale(inverse, inverse, inverse);
        com.iafabric.client.render.IafItemRender.render(itemstack, ItemDisplayContext.NONE, light, poseStack, buffers, entity);
        poseStack.popPose();
    }
}

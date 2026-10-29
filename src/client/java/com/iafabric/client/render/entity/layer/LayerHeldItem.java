package com.iafabric.client.render.entity.layer;

import com.iafabric.client.model.ModelBipedBase;
import com.iafabric.client.render.BufferSource;
import com.iafabric.client.render.IafItemRender;
import com.iafabric.client.render.entity.IafMobRenderer;
import com.iafabric.client.render.entity.IafRenderLayer;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

/** What a two-armed mob holds, in its hands (the vanilla held item layer, for the mod's biped models). */
public class LayerHeldItem<T extends LivingEntity, M extends ModelBipedBase<T>> extends IafRenderLayer<T, M> {
    /** Set while the mob should not show what it holds (rising from the ground, for example). */
    public boolean hidden;

    public LayerHeldItem(IafMobRenderer<T, M> renderer) {
        super(renderer);
    }

    @Override
    public void render(PoseStack poseStack, BufferSource buffers, int light, T entity, float limbSwing, float limbSwingAmount, float partialTicks,
        float ageInTicks, float netHeadYaw, float headPitch) {
        if (hidden) {
            return;
        }
        boolean rightHanded = entity.getMainArm() == HumanoidArm.RIGHT;
        ItemStack left = rightHanded ? entity.getOffhandItem() : entity.getMainHandItem();
        ItemStack right = rightHanded ? entity.getMainHandItem() : entity.getOffhandItem();
        renderArm(entity, right, ItemDisplayContext.THIRD_PERSON_RIGHT_HAND, HumanoidArm.RIGHT, poseStack, buffers, light);
        renderArm(entity, left, ItemDisplayContext.THIRD_PERSON_LEFT_HAND, HumanoidArm.LEFT, poseStack, buffers, light);
    }

    private void renderArm(T entity, ItemStack stack, ItemDisplayContext context, HumanoidArm arm, PoseStack poseStack, BufferSource buffers, int light) {
        if (stack.isEmpty()) {
            return;
        }
        poseStack.pushPose();
        getParentModel().translateToHand(arm, poseStack);
        poseStack.mulPose(Axis.XP.rotationDegrees(-90.0F));
        poseStack.mulPose(Axis.YP.rotationDegrees(180.0F));
        boolean left = arm == HumanoidArm.LEFT;
        poseStack.translate((left ? -1 : 1) / 16.0F, 0.125F, -0.625F);
        IafItemRender.render(stack, context, light, poseStack, buffers, entity);
        poseStack.popPose();
    }
}

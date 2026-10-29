package com.iafabric.mixin.client;

import com.iafabric.client.render.item.IafItemProperties;
import com.iafabric.item.IafItemRegistry;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** The tide trident is a flat picture in the inventory, on the ground and in a frame, as the game's trident is. */
@Mixin(ItemRenderer.class)
public abstract class TideTridentModelMixin {
	@Shadow
	public abstract void render(ItemStack stack, ItemDisplayContext context, boolean leftHand, PoseStack poseStack, MultiBufferSource buffers, int light, int overlay,
		BakedModel model);

	@Inject(method = "render(Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/item/ItemDisplayContext;ZLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;IILnet/minecraft/client/resources/model/BakedModel;)V",
		at = @At("HEAD"), cancellable = true)
	private void iceandfire_fabric$flatTideTrident(ItemStack stack, ItemDisplayContext context, boolean leftHand, PoseStack poseStack, MultiBufferSource buffers,
		int light, int overlay, BakedModel model, CallbackInfo ci) {
		if ((context == ItemDisplayContext.GUI || context == ItemDisplayContext.GROUND || context == ItemDisplayContext.FIXED) && stack.is(IafItemRegistry.TIDE_TRIDENT)) {
			BakedModel flat = Minecraft.getInstance().getModelManager().getModel(IafItemProperties.TIDE_TRIDENT_INVENTORY);
			if (flat != model && flat != null) {
				this.render(stack, context, leftHand, poseStack, buffers, light, overlay, flat);
				ci.cancel();
			}
		}
	}
}

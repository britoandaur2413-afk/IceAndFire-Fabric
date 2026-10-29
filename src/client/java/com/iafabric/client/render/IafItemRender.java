package com.iafabric.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

/** Draws an item in the current pose (the old ItemRenderer.renderStatic). */
public final class IafItemRender {
	private IafItemRender() {
	}

	public static void render(ItemStack stack, ItemDisplayContext context, int light, PoseStack poseStack, BufferSource buffers) {
		render(stack, context, light, poseStack, buffers, null);
	}

	public static void render(ItemStack stack, ItemDisplayContext context, int light, PoseStack poseStack, BufferSource buffers, @Nullable LivingEntity holder) {
		if (stack.isEmpty()) {
			return;
		}
		Minecraft.getInstance().getItemRenderer().renderStatic(holder, stack, context, false, poseStack, buffers.buffers(), Minecraft.getInstance().level,
			light, OverlayTexture.NO_OVERLAY, holder == null ? 0 : holder.getId());
	}
}

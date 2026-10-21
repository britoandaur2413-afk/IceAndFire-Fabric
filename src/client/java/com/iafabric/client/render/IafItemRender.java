package com.iafabric.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

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
		ItemStackRenderState state = new ItemStackRenderState();
		if (holder != null) {
			Minecraft.getInstance().getItemModelResolver().updateForLiving(state, stack, context, holder);
		} else {
			Minecraft.getInstance().getItemModelResolver().updateForTopItem(state, stack, context, Minecraft.getInstance().level, null, 0);
		}
		state.submit(poseStack, buffers.collector(), light, OverlayTexture.NO_OVERLAY, 0);
	}
}

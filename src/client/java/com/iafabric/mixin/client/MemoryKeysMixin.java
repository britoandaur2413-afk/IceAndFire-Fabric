package com.iafabric.mixin.client;

import com.iafabric.client.gui.MemoryOverlay;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * In a memory of their exiled pixie no key does anything but pause the game: no pack, throwing, swapping hands,
 * striking, using, the hotbar, chat, the view, her menu (MemoryOverlay). What is pressed meanwhile is let go, not kept
 * for after.
 */
@Mixin(Minecraft.class)
public class MemoryKeysMixin {
	@Inject(method = "handleKeybinds", at = @At("HEAD"), cancellable = true)
	private void iceandfire_fabric$noKeysInMemory(CallbackInfo ci) {
		if (!MemoryOverlay.active()) {
			return;
		}
		Minecraft mc = (Minecraft) (Object) this;
		for (KeyMapping key : mc.options.keyMappings) {
			while (key.consumeClick()) {
			}
		}
		if (mc.player != null && mc.player.isUsingItem() && mc.gameMode != null) {
			mc.gameMode.releaseUsingItem(mc.player);
		}
		ci.cancel();
	}
}

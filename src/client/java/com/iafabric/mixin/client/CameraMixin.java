package com.iafabric.mixin.client;

import com.iafabric.IafMod;
import com.iafabric.entity.EntityDragonBase;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

/** Riding a dragon in third person, the camera stays farther away, by the dragon's size and the chosen view (F7). */
@Mixin(Camera.class)
public class CameraMixin {
	@ModifyArg(method = "setup", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Camera;getMaxZoom(F)F"))
	private float iceandfire_fabric$dragonZoom(float distance) {
		Minecraft mc = Minecraft.getInstance();
		if (mc.player != null && mc.player.getVehicle() instanceof EntityDragonBase dragon) {
			float scale = dragon.getRenderSize() / 3;
			return switch (IafMod.PROXY.getDragon3rdPersonView()) {
				case 1 -> distance + scale * 1.2F;
				case 2 -> distance + scale * 3F;
				case 3 -> distance + scale * 5F;
				default -> distance;
			};
		}
		return distance;
	}
}

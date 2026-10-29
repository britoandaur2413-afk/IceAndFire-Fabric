package com.iafabric.mixin;

import com.iafabric.entity.util.IFlyingMount;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * On a flying mount (dragon, hippogryph, amphithere) shift only lets the rider off on the ground or in water;
 * in the air it means "go down" (the mount reads it from the rider's input).
 */
@Mixin(Player.class)
public abstract class PlayerDismountMixin {
	@Inject(method = "wantsToStopRiding", at = @At("HEAD"), cancellable = true)
	private void iceandfire_fabric$onlyOnTheGround(CallbackInfoReturnable<Boolean> cir) {
		Entity vehicle = ((Player) (Object) this).getVehicle();
		if (vehicle instanceof IFlyingMount && !IFlyingMount.canDismount(vehicle)) {
			cir.setReturnValue(false);
		}
		// a dragon brought down with shift held lands with its rider on it: shift is pressed again to get off (the one
		// steering it: a friend behind gets off when they will)
		if (vehicle instanceof com.iafabric.entity.EntityDragonBase dragon && dragon.shiftFromTheAir() && dragon.getControllingPassenger() == (Object) this) {
			cir.setReturnValue(false);
		}
	}
}

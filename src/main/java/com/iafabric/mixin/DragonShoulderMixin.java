package com.iafabric.mixin;

import com.iafabric.entity.EntityDragonBase;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * A baby dragon rides on its owner's shoulder. 26.x lets nothing ride, on the server, what is never saved with the world
 * (a player), so the dragon sat on the shoulder only on the screen and went back to the ground: a dragon may. The game
 * puts down what rides a player when they leave, so the dragon stays in the world.
 */
@Mixin(Entity.class)
abstract class DragonShoulderMixin {
	@ModifyExpressionValue(method = "startRiding(Lnet/minecraft/world/entity/Entity;ZZ)Z",
		at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/EntityType;canSerialize()Z"))
	private boolean iceandfire_fabric$onAShoulder(boolean saved, @Local(argsOnly = true) Entity vehicle) {
		return saved || (Object) this instanceof EntityDragonBase && vehicle instanceof Player;
	}
}

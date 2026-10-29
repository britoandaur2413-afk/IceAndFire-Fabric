package com.iafabric.mixin;

import com.iafabric.entity.EntityMutlipartPart;
import net.minecraft.network.protocol.game.ServerboundInteractPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.phys.AABB;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * A right click or a blow on a part of a big creature (a dragon's head, far ahead of its body) is taken as far as 26.x
 * takes any (3 blocks past the player's reach), not 1.21.1's 1: each game places the parts from its own pose, so as
 * the creature turns the server's part can stand a block or two from where the player aims, and the click was lost.
 */
@Mixin(ServerGamePacketListenerImpl.class)
public class PartReachMixin {
	@Redirect(method = "handleInteract", at = @At(value = "INVOKE",
		target = "Lnet/minecraft/server/level/ServerPlayer;canInteractWithEntity(Lnet/minecraft/world/phys/AABB;D)Z"))
	private boolean iceandfire_fabric$partWithinReach(ServerPlayer player, AABB box, double buffer, ServerboundInteractPacket packet) {
		boolean part = packet.getTarget(player.serverLevel()) instanceof EntityMutlipartPart;
		return player.canInteractWithEntity(box, part ? Math.max(buffer, 3.0) : buffer);
	}
}

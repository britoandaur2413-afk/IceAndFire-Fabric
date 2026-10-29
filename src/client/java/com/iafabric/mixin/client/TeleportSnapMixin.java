package com.iafabric.mixin.client;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.protocol.game.ClientboundTeleportEntityPacket;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * A creature sent far away in one go (the exiled pixie snapping back to her player after a teleport) is put there at
 * once: 1.21.1 only slides it there in its own ticks, and one left in land the client has let go of never ticks, so it
 * stayed where it was.
 */
@Mixin(ClientPacketListener.class)
public abstract class TeleportSnapMixin {
	private static final double FAR = 32 * 32;

	@Shadow
	private ClientLevel level;

	@Inject(method = "handleTeleportEntity", at = @At("RETURN"))
	private void iceandfire_fabric$snapFar(ClientboundTeleportEntityPacket packet, CallbackInfo ci) {
		if (this.level == null) {
			return;
		}
		Entity entity = this.level.getEntity(packet.getId());
		if (entity != null && !entity.isControlledByLocalInstance() && entity.position().distanceToSqr(packet.getX(), packet.getY(), packet.getZ()) > FAR) {
			entity.moveTo(packet.getX(), packet.getY(), packet.getZ(), entity.getYRot(), entity.getXRot());
		}
	}
}

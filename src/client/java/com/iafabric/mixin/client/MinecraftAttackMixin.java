package com.iafabric.mixin.client;

import com.iafabric.entity.EntityDragonBase;
import com.iafabric.entity.EntityExiledPixie;
import com.iafabric.entity.EntityMutlipartPart;
import com.iafabric.item.IafItemRegistry;
import com.iafabric.message.MessageSwingArm;
import com.iafabric.network.IafNetwork;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * A swing at nothing (Forge's LeftClickEmpty in 1.18) tells the server, which throws the ghost sword's blade. And a
 * swing at what is only in the way (an exiled pixie hovering before her player or over her lantern, a dragon's dead
 * body) goes to the block behind it: it is mined as if they were not there. A right click still reaches them (a word
 * with her, a dragon's bones).
 */
@Mixin(Minecraft.class)
public class MinecraftAttackMixin {
	@Inject(method = "startAttack", at = @At("HEAD"))
	private void iceandfire_fabric$swingAtNothing(CallbackInfoReturnable<Boolean> cir) {
		Minecraft mc = (Minecraft) (Object) this;
		iceandfire_fabric$throughToBlock(mc);
		if (mc.player != null && (mc.hitResult == null || mc.hitResult.getType() == HitResult.Type.MISS)
			&& mc.player.getMainHandItem().getItem() == IafItemRegistry.GHOST_SWORD) {
			IafNetwork.sendToServer(new MessageSwingArm());
		}
	}

	@Inject(method = "continueAttack", at = @At("HEAD"))
	private void iceandfire_fabric$keepMiningThrough(boolean attacking, CallbackInfo ci) {
		iceandfire_fabric$throughToBlock((Minecraft) (Object) this);
	}

	private static void iceandfire_fabric$throughToBlock(Minecraft mc) {
		if (mc.player == null || !(mc.hitResult instanceof EntityHitResult hit) || !iceandfire_fabric$onlyInTheWay(hit.getEntity())) {
			return;
		}
		HitResult behind = mc.player.pick(mc.gameMode.getPickRange(), 1.0F, false);
		if (behind.getType() == HitResult.Type.BLOCK) {
			mc.hitResult = behind;
		}
	}

	/** What a swing goes through: an exiled pixie (nothing hurts her), a dragon's dead body or a part of it. */
	private static boolean iceandfire_fabric$onlyInTheWay(Entity entity) {
		if (entity instanceof EntityMutlipartPart part) {
			entity = part.getParent();
		}
		return entity instanceof EntityExiledPixie || entity instanceof EntityDragonBase dragon && dragon.isModelDead();
	}
}

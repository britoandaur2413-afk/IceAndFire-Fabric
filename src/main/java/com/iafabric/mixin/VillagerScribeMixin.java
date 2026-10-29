package com.iafabric.mixin;

import com.iafabric.entity.IafVillagerRegistry;
import net.minecraft.world.entity.npc.Villager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** A scribe always sells, besides its trades drawn by chance, the Frost Ward and the map to Cryona's citadel. */
@Mixin(Villager.class)
public class VillagerScribeMixin {
	@Inject(method = "updateTrades", at = @At("RETURN"))
	private void iceandfire_fabric$sureTrades(CallbackInfo ci) {
		IafVillagerRegistry.addSureTrades((Villager) (Object) this);
	}
}

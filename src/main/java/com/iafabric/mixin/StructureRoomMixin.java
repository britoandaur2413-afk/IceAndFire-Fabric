package com.iafabric.mixin;

import com.iafabric.world.structure.StructureRoom;
import net.minecraft.world.level.levelgen.structure.Structure;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Optional;

/**
 * Where the game picks a structure's place (for making it, for /locate and for explorer maps alike): one that would
 * reach into one of the mod's buildings gives way (see StructureRoom).
 */
@Mixin(Structure.class)
public abstract class StructureRoomMixin {
	@Inject(method = "findValidGenerationPoint", at = @At("RETURN"), cancellable = true)
	private void iceandfire_fabric$giveWay(Structure.GenerationContext context, CallbackInfoReturnable<Optional<Structure.GenerationStub>> cir) {
		Optional<Structure.GenerationStub> stub = cir.getReturnValue();
		if (stub.isPresent() && StructureRoom.givesWay((Structure) (Object) this, context, stub.get())) {
			cir.setReturnValue(Optional.empty());
		}
	}
}

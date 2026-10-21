package com.iafabric.mixin;

import com.iafabric.boss.CastleWard;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ServerExplosion;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.ArrayList;
import java.util.List;

/** No blast breaks the stones of Cryona's citadel while it holds (CastleWard). */
@Mixin(ServerExplosion.class)
public class ServerExplosionMixin {
	@Shadow
	@Final
	private ServerLevel level;
	@Shadow
	@Final
	private Vec3 center;

	@Inject(method = "calculateExplodedPositions", at = @At("RETURN"), cancellable = true)
	private void iceandfire_fabric$citadelHolds(CallbackInfoReturnable<List<BlockPos>> cir) {
		List<BlockPos> positions = cir.getReturnValue();
		if (positions == null || positions.isEmpty()) {
			return;
		}
		StructureStart start = CastleWard.citadelAt(this.level, BlockPos.containing(this.center));
		if (start == null || CastleWard.freed(this.level, start)) {
			return;
		}
		List<BlockPos> kept = new ArrayList<>();
		for (BlockPos pos : positions) {
			if (!start.getBoundingBox().isInside(pos)) {
				kept.add(pos);
			}
		}
		cir.setReturnValue(kept);
	}
}

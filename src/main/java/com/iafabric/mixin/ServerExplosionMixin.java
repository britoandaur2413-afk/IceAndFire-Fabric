package com.iafabric.mixin;

import com.iafabric.boss.CastleWard;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import com.iafabric.entity.util.BlockLaunchExplosion;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.ExplosionDamageCalculator;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.List;

/** No blast breaks the stones of Cryona's citadel while it holds (CastleWard); a sparing blast leaves its side alone. */
@Mixin(Explosion.class)
public class ServerExplosionMixin {
	@Shadow
	@Final
	private Level level;
	@Shadow
	@Final
	private double x;
	@Shadow
	@Final
	private double y;
	@Shadow
	@Final
	private double z;
	@Shadow
	@Final
	private ObjectArrayList<BlockPos> toBlow;
	@Shadow
	@Final
	private ExplosionDamageCalculator damageCalculator;

	/** The ones a sparing blast (BlockLaunchExplosion) leaves alone are neither hurt nor pushed. */
	@ModifyVariable(method = "explode", at = @At("STORE"), ordinal = 0)
	private List<Entity> iceandfire_fabric$spare(List<Entity> hit) {
		if (this.damageCalculator instanceof BlockLaunchExplosion.Sparing sparing) {
			hit = new ArrayList<>(hit);
			hit.removeIf(sparing::spares);
		}
		return hit;
	}

	@Inject(method = "explode", at = @At("TAIL"))
	private void iceandfire_fabric$citadelHolds(CallbackInfo ci) {
		if (!(this.level instanceof ServerLevel server) || this.toBlow.isEmpty()) {
			return;
		}
		StructureStart start = CastleWard.citadelAt(server, BlockPos.containing(this.x, this.y, this.z));
		if (start == null || CastleWard.freed(server, start)) {
			return;
		}
		this.toBlow.removeIf(pos -> start.getBoundingBox().isInside(pos));
	}
}

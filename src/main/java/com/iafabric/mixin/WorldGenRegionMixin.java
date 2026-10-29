package com.iafabric.mixin;

import com.iafabric.IafMod;
import com.iafabric.world.PendingWorldgen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.material.FluidState;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.function.Supplier;

/**
 * While one of the mod's features generates, what it does outside the chunks it may touch goes to
 * {@link PendingWorldgen} instead of being dropped with an error (see there why).
 */
@Mixin(WorldGenRegion.class)
public abstract class WorldGenRegionMixin {
	@Shadow
	@Final
	private ServerLevel level;

	@Unique
	private boolean iceandfire_fabric$ours;

	@Shadow
	@Final
	private int writeRadiusCutoff;

	@Shadow
	public abstract ChunkPos getCenter();

	@Inject(method = "setCurrentlyGenerating", at = @At("TAIL"))
	private void iceandfire_fabric$whatGenerates(Supplier<String> generating, CallbackInfo ci) {
		if (iceandfire_fabric$ours) {
			PendingWorldgen.featureEnds(level);
		}
		String now = generating == null ? null : generating.get();
		iceandfire_fabric$ours = now != null && now.contains(IafMod.MOD_ID + ":");
		if (iceandfire_fabric$ours) {
			PendingWorldgen.featureStarts();
		}
	}

	@Unique
	private boolean iceandfire_fabric$beyond(BlockPos pos) {
		return iceandfire_fabric$ours && !iceandfire_fabric$withinWriteZone(pos);
	}

	/** Whether the generation may write there (ensureCanWrite's check, without its error). */
	@Unique
	private boolean iceandfire_fabric$withinWriteZone(BlockPos pos) {
		ChunkPos center = getCenter();
		int radius = writeRadiusCutoff;
		return Math.abs(center.x - SectionPos.blockToSectionCoord(pos.getX())) <= radius
			&& Math.abs(center.z - SectionPos.blockToSectionCoord(pos.getZ())) <= radius;
	}

	@Inject(method = "getBlockState", at = @At("HEAD"), cancellable = true)
	private void iceandfire_fabric$getBlockState(BlockPos pos, CallbackInfoReturnable<BlockState> cir) {
		if (iceandfire_fabric$beyond(pos)) {
			cir.setReturnValue(PendingWorldgen.getBlockState(level, (WorldGenRegion) (Object) this, pos));
		}
	}

	@Inject(method = "getFluidState", at = @At("HEAD"), cancellable = true)
	private void iceandfire_fabric$getFluidState(BlockPos pos, CallbackInfoReturnable<FluidState> cir) {
		if (iceandfire_fabric$beyond(pos)) {
			cir.setReturnValue(PendingWorldgen.getBlockState(level, (WorldGenRegion) (Object) this, pos).getFluidState());
		}
	}

	@Inject(method = "getHeight(Lnet/minecraft/world/level/levelgen/Heightmap$Types;II)I", at = @At("HEAD"), cancellable = true)
	private void iceandfire_fabric$getHeight(Heightmap.Types type, int x, int z, CallbackInfoReturnable<Integer> cir) {
		if (iceandfire_fabric$beyond(new BlockPos(x, 0, z))) {
			cir.setReturnValue(PendingWorldgen.getHeight(level, (WorldGenRegion) (Object) this, type, x, z));
		}
	}

	@Inject(method = "getBlockEntity", at = @At("HEAD"), cancellable = true)
	private void iceandfire_fabric$getBlockEntity(BlockPos pos, CallbackInfoReturnable<BlockEntity> cir) {
		if (iceandfire_fabric$beyond(pos)) {
			cir.setReturnValue(PendingWorldgen.getBlockEntity(level, (WorldGenRegion) (Object) this, pos));
		}
	}

	@Inject(method = "setBlock", at = @At("HEAD"), cancellable = true)
	private void iceandfire_fabric$setBlock(BlockPos pos, BlockState state, int flags, int recursion, CallbackInfoReturnable<Boolean> cir) {
		if (iceandfire_fabric$beyond(pos)) {
			PendingWorldgen.setBlock(level, pos.immutable(), state);
			cir.setReturnValue(true);
		}
	}

	@Inject(method = "addFreshEntity", at = @At("HEAD"), cancellable = true)
	private void iceandfire_fabric$addFreshEntity(Entity entity, CallbackInfoReturnable<Boolean> cir) {
		if (iceandfire_fabric$beyond(entity.blockPosition())) {
			PendingWorldgen.addEntity(level, entity);
			cir.setReturnValue(true);
		}
	}
}

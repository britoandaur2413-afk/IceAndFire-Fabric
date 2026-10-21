package com.iafabric.mixin;

import com.iafabric.citadel.server.entity.EntityDataHolder;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import com.iafabric.citadel.server.entity.collision.ICustomCollisions;
import net.minecraft.world.phys.Vec3;

@Mixin(Entity.class)
public abstract class EntityMixin implements EntityDataHolder {
	@Unique
	private CompoundTag iaf$data = new CompoundTag();

	@Override
	public CompoundTag iaf$getData() {
		return iaf$data;
	}

	@Override
	public void iaf$setData(CompoundTag tag) {
		iaf$data = tag == null ? new CompoundTag() : tag;
	}

	@Inject(method = "saveWithoutId", at = @At("TAIL"))
	private void iaf$save(ValueOutput output, CallbackInfo ci) {
		if (!iaf$data.isEmpty()) {
			output.store("IafData", CompoundTag.CODEC, iaf$data);
		}
	}

	@Inject(method = "load", at = @At("TAIL"))
	private void iaf$load(ValueInput input, CallbackInfo ci) {
		iaf$data = input.read("IafData", CompoundTag.CODEC).orElseGet(CompoundTag::new);
	}

	@Inject(method = "collide", at = @At("HEAD"), cancellable = true)
	private void iaf$collide(Vec3 movement, CallbackInfoReturnable<Vec3> cir) {
		if (this instanceof ICustomCollisions) {
			cir.setReturnValue(ICustomCollisions.getAllowedMovementForEntity((Entity) (Object) this, movement));
		}
	}
}

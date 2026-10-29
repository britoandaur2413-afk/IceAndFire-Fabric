package com.iafabric.mixin;

import com.iafabric.item.IafAliases;
import net.minecraft.core.Holder;
import net.minecraft.core.MappedRegistry;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.resources.ResourceKey;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Optional;

/**
 * The mod's own copper is the game's now: an old world's iceandfire_fabric copper ingot, ore, armor... is read as the game's
 * (IafAliases), instead of vanishing.
 */
@Mixin(MappedRegistry.class)
public abstract class RegistryAliasMixin<T> {
	@SuppressWarnings("unchecked")
	private Registry<T> iceandfire_fabric$self() {
		return (Registry<T>) (Object) this;
	}

	@Inject(method = "getHolder(Lnet/minecraft/resources/ResourceLocation;)Ljava/util/Optional;", at = @At("RETURN"), cancellable = true)
	private void iceandfire_fabric$aliasHolder(ResourceLocation id, CallbackInfoReturnable<Optional<Holder.Reference<T>>> cir) {
		if (cir.getReturnValue().isEmpty()) {
			ResourceLocation to = IafAliases.of(iceandfire_fabric$self().key(), id);
			if (to != null) {
				cir.setReturnValue(iceandfire_fabric$self().getHolder(to));
			}
		}
	}

	@Inject(method = "getHolder(Lnet/minecraft/resources/ResourceKey;)Ljava/util/Optional;", at = @At("RETURN"), cancellable = true)
	private void iceandfire_fabric$aliasKeyHolder(ResourceKey<T> key, CallbackInfoReturnable<Optional<Holder.Reference<T>>> cir) {
		if (cir.getReturnValue().isEmpty()) {
			ResourceLocation to = IafAliases.of(iceandfire_fabric$self().key(), key.location());
			if (to != null) {
				cir.setReturnValue(iceandfire_fabric$self().getHolder(to));
			}
		}
	}

	@Inject(method = "get(Lnet/minecraft/resources/ResourceLocation;)Ljava/lang/Object;", at = @At("RETURN"), cancellable = true)
	private void iceandfire_fabric$aliasValue(ResourceLocation id, CallbackInfoReturnable<T> cir) {
		if (cir.getReturnValue() == null) {
			ResourceLocation to = IafAliases.of(iceandfire_fabric$self().key(), id);
			if (to != null) {
				cir.setReturnValue(iceandfire_fabric$self().get(to));
			}
		}
	}

	@Inject(method = "get(Lnet/minecraft/resources/ResourceKey;)Ljava/lang/Object;", at = @At("RETURN"), cancellable = true)
	private void iceandfire_fabric$aliasKeyValue(ResourceKey<T> key, CallbackInfoReturnable<T> cir) {
		if (cir.getReturnValue() == null) {
			ResourceLocation to = IafAliases.of(iceandfire_fabric$self().key(), key.location());
			if (to != null) {
				cir.setReturnValue(iceandfire_fabric$self().get(to));
			}
		}
	}
}

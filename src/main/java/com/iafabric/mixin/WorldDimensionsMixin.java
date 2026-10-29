package com.iafabric.mixin;

import com.iafabric.IafMod;
import com.mojang.serialization.Lifecycle;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.dimension.LevelStem;
import net.minecraft.world.level.levelgen.WorldDimensions;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.stream.Stream;

/**
 * The mod's realms (Cryona's, the fairy glade) are no "experimental settings": without this 1.21.1 warns when a world is
 * made and asks for a backup each time it is opened, only because the world has more dimensions than the game's three.
 */
@Mixin(WorldDimensions.class)
public abstract class WorldDimensionsMixin {
	@Unique
	private static boolean iceandfire_fabric$ours(ResourceKey<LevelStem> key) {
		return IafMod.MOD_ID.equals(key.location().getNamespace());
	}

	@Inject(method = "checkStability", at = @At("HEAD"), cancellable = true)
	private static void iceandfire_fabric$ourRealmsAreStable(ResourceKey<LevelStem> key, LevelStem stem, CallbackInfoReturnable<Lifecycle> cir) {
		if (iceandfire_fabric$ours(key)) {
			cir.setReturnValue(Lifecycle.stable());
		}
	}

	@ModifyVariable(method = "bake", at = @At("STORE"), ordinal = 0)
	private Lifecycle iceandfire_fabric$countTheGamesOwn(Lifecycle lifecycle, Registry<LevelStem> registry) {
		if (lifecycle == Lifecycle.stable()) {
			return lifecycle;
		}
		long others = Stream.concat(registry.registryKeySet().stream(), ((WorldDimensions) (Object) this).dimensions().keySet().stream()).distinct()
			.filter(key -> !iceandfire_fabric$ours(key)).count();
		return others == 3 ? Lifecycle.stable() : lifecycle;
	}
}

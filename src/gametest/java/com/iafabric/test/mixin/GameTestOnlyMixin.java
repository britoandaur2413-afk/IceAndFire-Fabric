package com.iafabric.test.mixin;

import net.minecraft.gametest.framework.GameTestRegistry;
import net.minecraft.gametest.framework.TestFunction;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

/** gradlew runGametest -Ponly=taming,throne runs only the tests whose names hold one of those words (faster to try a change). */
@Mixin(GameTestRegistry.class)
public class GameTestOnlyMixin {
	@Inject(method = "getAllTestFunctions", at = @At("RETURN"), cancellable = true)
	private static void iceandfire_fabric$only(CallbackInfoReturnable<Collection<TestFunction>> cir) {
		String only = System.getProperty("iceandfire_fabric.test.only", "");
		if (only.isBlank()) {
			return;
		}
		List<String> words = Arrays.stream(only.toLowerCase().split(",")).map(String::trim).filter(w -> !w.isEmpty()).toList();
		cir.setReturnValue(cir.getReturnValue().stream().filter(f -> words.stream().anyMatch(w -> f.getTestName().toLowerCase().contains(w))).toList());
	}
}

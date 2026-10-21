package com.iafabric.mixin;

import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.RangedAttribute;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Life beyond the game's limit of 1024: Black Frost has ten thousand and Cryona three thousand (the game cut both to
 * 1024). The limit of the max health attribute only is raised; nothing else changes.
 */
@Mixin(RangedAttribute.class)
public abstract class RangedAttributeMixin {
	private static final String MAX_HEALTH = "attribute.name.max_health";
	private static final double MOST_LIFE = 1_000_000.0;

	@Shadow
	@Final
	private double minValue;

	@Inject(method = "sanitizeValue", at = @At("HEAD"), cancellable = true)
	private void iceandfire_fabric$moreLife(double value, CallbackInfoReturnable<Double> cir) {
		if (MAX_HEALTH.equals(((Attribute) (Object) this).getDescriptionId())) {
			cir.setReturnValue(Double.isNaN(value) ? this.minValue : Math.max(this.minValue, Math.min(MOST_LIFE, value)));
		}
	}

	@Inject(method = "getMaxValue", at = @At("HEAD"), cancellable = true)
	private void iceandfire_fabric$mostLife(CallbackInfoReturnable<Double> cir) {
		if (MAX_HEALTH.equals(((Attribute) (Object) this).getDescriptionId())) {
			cir.setReturnValue(MOST_LIFE);
		}
	}
}

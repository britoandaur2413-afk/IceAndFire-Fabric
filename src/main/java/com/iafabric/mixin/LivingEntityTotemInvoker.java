package com.iafabric.mixin;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

/** The game's own last save from a death (a totem of undying in hand, or what else guards from death): true if it saved. */
@Mixin(LivingEntity.class)
public interface LivingEntityTotemInvoker {
	@Invoker("checkTotemDeathProtection")
	boolean iceandfire_fabric$checkTotemDeathProtection(DamageSource source);
}

package com.iafabric.mixin;

import net.minecraft.world.entity.Interaction;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

/** The size of an interaction (a hitbox struck to break something the mod shows: Black Frost's pylons). */
@Mixin(Interaction.class)
public interface InteractionInvoker {
	@Invoker("setWidth")
	void iceandfire_fabric$setWidth(float width);

	@Invoker("setHeight")
	void iceandfire_fabric$setHeight(float height);
}

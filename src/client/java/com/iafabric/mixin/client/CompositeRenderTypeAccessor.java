package com.iafabric.mixin.client;

import net.minecraft.client.renderer.RenderType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** What a render type draws with (its texture), to turn a creature's own texture to stone. */
@Mixin(targets = "net.minecraft.client.renderer.RenderType$CompositeRenderType")
public interface CompositeRenderTypeAccessor {
	@Accessor("state")
	RenderType.CompositeState iceandfire_fabric$state();
}

package com.iafabric.test.mixin;

import net.minecraft.client.gui.components.SplashRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** A splash's text, for the menu test. */
@Mixin(SplashRenderer.class)
public interface SplashRendererAccessor {
	@Accessor("splash")
	String iceandfire_fabric$text();
}

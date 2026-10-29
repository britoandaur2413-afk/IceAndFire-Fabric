package com.iafabric.test.mixin;

import net.minecraft.client.gui.components.SplashRenderer;
import net.minecraft.client.gui.screens.TitleScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** The title screen's splash, for the menu test (an accessor works with the game's real names too, reflection does not). */
@Mixin(TitleScreen.class)
public interface TitleScreenSplashAccessor {
	@Accessor("splash")
	SplashRenderer iceandfire_fabric$splash();
}

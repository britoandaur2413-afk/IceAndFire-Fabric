package com.iafabric.test.mixin;

import net.minecraft.client.KeyboardHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

/** The client tests' typing (TestInput). */
@Mixin(KeyboardHandler.class)
public interface KeyboardHandlerInvoker {
	@Invoker("charTyped")
	void iceandfire_fabric$charTyped(long window, int codePoint, int modifiers);
}

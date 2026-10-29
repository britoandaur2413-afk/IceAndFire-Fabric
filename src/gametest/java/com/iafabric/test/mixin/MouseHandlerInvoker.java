package com.iafabric.test.mixin;

import net.minecraft.client.MouseHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

/** The client tests' mouse (TestInput). */
@Mixin(MouseHandler.class)
public interface MouseHandlerInvoker {
	@Invoker("onPress")
	void iceandfire_fabric$onPress(long window, int button, int action, int modifiers);

	@Invoker("onMove")
	void iceandfire_fabric$onMove(long window, double x, double y);
}

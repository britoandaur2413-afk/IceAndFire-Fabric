package com.iafabric.inventory;

import net.minecraft.world.Container;

/** Told when a {@link ListenedContainer} changes (vanilla containers have no listeners now). */
public interface IContainerListener {
	void containerChanged(Container container);
}

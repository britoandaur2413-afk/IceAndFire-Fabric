package com.iafabric.inventory;

import net.minecraft.world.SimpleContainer;

/** A simple container that tells its owner when it changes, like the old container listeners. */
public class ListenedContainer extends SimpleContainer {
	private final IContainerListener owner;

	public ListenedContainer(IContainerListener owner, int size) {
		super(size);
		this.owner = owner;
	}

	@Override
	public void setChanged() {
		super.setChanged();
		if (owner != null) {
			owner.containerChanged(this);
		}
	}
}

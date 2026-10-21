package com.iafabric.citadel.animation;

import java.util.function.Supplier;

/** The frame's partial tick, for code shared with the client (0 on a server). */
public final class PartialTicks {
	public static Supplier<Float> source = () -> 0.0F;

	private PartialTicks() {
	}

	public static float get() {
		return source.get();
	}
}

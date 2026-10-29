package com.iafabric.citadel.animation;

/** A named, fixed-length animation an entity can play (replaces Citadel's Animation). */
public final class Animation {
	private final int duration;

	private Animation(int duration) {
		this.duration = duration;
	}

	public static Animation create(int duration) {
		return new Animation(duration);
	}

	public int getDuration() {
		return duration;
	}
}

package com.iafabric.client.render.entity;

import net.minecraft.world.entity.LivingEntity;

import java.util.Collections;
import java.util.Set;
import java.util.WeakHashMap;

/** Riders drawn on a dragon that are not in the world (DragonRiderRender): drawn seated, as passengers are. */
public final class VirtualRiders {
	public static final Set<LivingEntity> SEATED = Collections.newSetFromMap(new WeakHashMap<>());

	private VirtualRiders() {
	}
}

package com.iafabric.client.render;

import com.iafabric.IafMod;
import net.fabricmc.fabric.api.client.rendering.v1.DimensionRenderingRegistry;
import net.minecraft.client.renderer.DimensionSpecialEffects;
import net.minecraft.world.phys.Vec3;

/**
 * How Cryona's realm looks: the End's starry sky, but lit as it is (dark, its lanterns and the magic glowing), as
 * 26.x's realm. The game's own End effects light everything brightly, and the snow of the arena floor drowned the
 * warning circles in white. Its dimension type names these effects (iceandfire_fabric:frozen_throne).
 */
public final class RealmEffects extends DimensionSpecialEffects {
	private RealmEffects() {
		super(Float.NaN, false, DimensionSpecialEffects.SkyType.END, false, false);
	}

	public static void init() {
		DimensionRenderingRegistry.registerDimensionEffects(IafMod.id("frozen_throne"), new RealmEffects());
	}

	@Override
	public Vec3 getBrightnessDependentFogColor(Vec3 color, float brightness) {
		return color.scale(0.15F);
	}

	@Override
	public boolean isFoggyAt(int x, int z) {
		return false;
	}
}

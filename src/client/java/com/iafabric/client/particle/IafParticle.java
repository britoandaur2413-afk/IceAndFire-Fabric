package com.iafabric.client.particle;

import com.iafabric.IafMod;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;

/**
 * A camera-facing particle drawn with one of the mod's pictures (textures/particle/&lt;name&gt;.png, in the
 * particle atlas). The 1.18 particles drew their own quads; their movement stays in their tick().
 */
public abstract class IafParticle extends TextureSheetParticle {

	protected IafParticle(ClientLevel level, double x, double y, double z, String texture) {
		super(level, x, y, z);
		this.setSprite(sprite(texture));
	}

	protected IafParticle(ClientLevel level, double x, double y, double z, double xd, double yd, double zd, String texture) {
		super(level, x, y, z, xd, yd, zd);
		this.setSprite(sprite(texture));
	}

	public static TextureAtlasSprite sprite(String texture) {
		return ((TextureAtlas) Minecraft.getInstance().getTextureManager().getTexture(TextureAtlas.LOCATION_PARTICLES)).getSprite(IafMod.id(texture));
	}

	protected void setTexture(String texture) {
		this.setSprite(sprite(texture));
	}

	@Override
	public ParticleRenderType getRenderType() {
		return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
	}
}

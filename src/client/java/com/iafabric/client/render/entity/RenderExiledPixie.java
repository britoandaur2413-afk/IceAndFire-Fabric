package com.iafabric.client.render.entity;

import com.iafabric.entity.EntityExiledPixie;
import com.iafabric.entity.EntityPixie;
import com.iafabric.pixie.ExiledVariants;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;

/** An exiled pixie: the pixie's model in one of the twenty faded skins. */
public class RenderExiledPixie extends RenderPixie {
	private static final ResourceLocation[] TEXTURES = new ResourceLocation[ExiledVariants.COUNT];

	static {
		for (int i = 0; i < TEXTURES.length; i++) {
			TEXTURES[i] = new ResourceLocation("iceandfire_fabric:textures/models/pixie/exiled/exiled_" + i + ".png");
		}
	}

	public RenderExiledPixie(EntityRendererProvider.Context context) {
		super(context);
	}

	public static ResourceLocation texture(int variant) {
		return TEXTURES[ExiledVariants.of(variant)];
	}

	@Override
	public ResourceLocation getTextureLocation(EntityPixie pixie) {
		return pixie instanceof EntityExiledPixie exiled ? texture(exiled.getVariant()) : super.getTextureLocation(pixie);
	}
}

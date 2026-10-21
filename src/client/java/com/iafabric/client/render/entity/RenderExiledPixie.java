package com.iafabric.client.render.entity;

import com.iafabric.entity.EntityExiledPixie;
import com.iafabric.entity.EntityPixie;
import com.iafabric.pixie.ExiledVariants;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.Identifier;

/** An exiled pixie: the pixie's model in one of the twenty faded skins. */
public class RenderExiledPixie extends RenderPixie {
	private static final Identifier[] TEXTURES = new Identifier[ExiledVariants.COUNT];

	static {
		for (int i = 0; i < TEXTURES.length; i++) {
			TEXTURES[i] = Identifier.parse("iceandfire_fabric:textures/models/pixie/exiled/exiled_" + i + ".png");
		}
	}

	public RenderExiledPixie(EntityRendererProvider.Context context) {
		super(context);
	}

	public static Identifier texture(int variant) {
		return TEXTURES[ExiledVariants.of(variant)];
	}

	@Override
	public Identifier getTextureLocation(EntityPixie pixie) {
		return pixie instanceof EntityExiledPixie exiled ? texture(exiled.getVariant()) : super.getTextureLocation(pixie);
	}
}

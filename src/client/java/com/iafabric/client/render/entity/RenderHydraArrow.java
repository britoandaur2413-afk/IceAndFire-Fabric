package com.iafabric.client.render.entity;

import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;
import org.jetbrains.annotations.NotNull;

public class RenderHydraArrow extends IafArrowRenderer<AbstractArrow> {
    private static final Identifier TEXTURES = Identifier.parse("iceandfire_fabric:textures/models/misc/hydra_arrow.png");

    public RenderHydraArrow(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public Identifier getTextureLocation(AbstractArrow entity) {
        return TEXTURES;
    }

}
package com.iafabric.client.render.entity;

import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;
import org.jetbrains.annotations.NotNull;

public class RenderSeaSerpentArrow extends IafArrowRenderer<AbstractArrow> {
    private static final Identifier TEXTURE = Identifier.parse("iceandfire_fabric:textures/models/misc/sea_serpent_arrow.png");

    public RenderSeaSerpentArrow(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public Identifier getTextureLocation(AbstractArrow entity) {
        return TEXTURE;
    }

}
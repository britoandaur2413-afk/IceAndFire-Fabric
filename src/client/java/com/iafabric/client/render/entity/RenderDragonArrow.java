package com.iafabric.client.render.entity;

import com.iafabric.entity.EntityDragonArrow;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.NotNull;


public class RenderDragonArrow extends IafArrowRenderer<EntityDragonArrow> {
    private static final Identifier TEXTURE = Identifier.parse("iceandfire_fabric:textures/models/misc/dragonbone_arrow.png");

    public RenderDragonArrow(EntityRendererProvider.Context context) {
        super(context);
    }


    @Override
    public Identifier getTextureLocation(EntityDragonArrow entity) {
        return TEXTURE;
    }
}
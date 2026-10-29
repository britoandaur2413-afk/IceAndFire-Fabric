package com.iafabric.client.render.entity;

import com.iafabric.entity.EntityDragonArrow;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;


public class RenderDragonArrow extends IafArrowRenderer<EntityDragonArrow> {
    private static final ResourceLocation TEXTURE = new ResourceLocation("iceandfire_fabric:textures/models/misc/dragonbone_arrow.png");

    public RenderDragonArrow(EntityRendererProvider.Context context) {
        super(context);
    }


    @Override
    public ResourceLocation getTextureLocation(EntityDragonArrow entity) {
        return TEXTURE;
    }
}
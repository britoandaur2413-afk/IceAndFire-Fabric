package com.iafabric.client.render.entity;

import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import org.jetbrains.annotations.NotNull;

public class RenderAmphithereArrow extends IafArrowRenderer<AbstractArrow> {
    private static final ResourceLocation TEXTURE = ResourceLocation.parse("iceandfire_fabric:textures/models/misc/amphithere_arrow.png");


    public RenderAmphithereArrow(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public ResourceLocation getTextureLocation(AbstractArrow entity) {
        return TEXTURE;
    }
}
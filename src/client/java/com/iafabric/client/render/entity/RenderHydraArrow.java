package com.iafabric.client.render.entity;

import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import org.jetbrains.annotations.NotNull;

public class RenderHydraArrow extends IafArrowRenderer<AbstractArrow> {
    private static final ResourceLocation TEXTURES = ResourceLocation.parse("iceandfire_fabric:textures/models/misc/hydra_arrow.png");

    public RenderHydraArrow(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public ResourceLocation getTextureLocation(AbstractArrow entity) {
        return TEXTURES;
    }

}
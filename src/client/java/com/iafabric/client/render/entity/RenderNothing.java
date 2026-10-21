package com.iafabric.client.render.entity;

import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.world.entity.Entity;

/** Entities that are never drawn (the parts of multipart mobs, for example). */
public class RenderNothing<T extends Entity> extends IafEntityRenderer<T> {

    public RenderNothing(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public boolean shouldRender(T entity, Frustum camera, double camX, double camY, double camZ, float partialTicks) {
        return false;
    }
}

package com.iafabric.client.render.entity;

import com.iafabric.client.render.entity.IafMobRenderer;
import com.iafabric.client.model.ModelTroll;
import com.iafabric.client.render.entity.layer.LayerTrollEyes;
import com.iafabric.client.render.entity.layer.LayerTrollWeapon;
import com.iafabric.entity.EntityTroll;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.NotNull;

public class RenderTroll extends IafMobRenderer<EntityTroll, ModelTroll> {

    public RenderTroll(EntityRendererProvider.Context context) {
        super(context, new ModelTroll(), 0.9F);
        this.layers.add(new LayerTrollWeapon(this));
        this.layers.add(new LayerTrollEyes(this));
    }

    @Override
    public Identifier getTextureLocation(EntityTroll troll) {
        return troll.getTrollType().TEXTURE;
    }
}

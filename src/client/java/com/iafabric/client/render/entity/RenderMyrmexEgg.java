package com.iafabric.client.render.entity;

import com.iafabric.citadel.client.model.AdvancedEntityModel;
import com.iafabric.client.model.ModelDragonEgg;
import com.iafabric.entity.EntityMyrmexEgg;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.NotNull;

public class RenderMyrmexEgg extends IafMobRenderer<EntityMyrmexEgg, AdvancedEntityModel<EntityMyrmexEgg>> {

    public static final Identifier EGG_JUNGLE = Identifier.parse("iceandfire_fabric:textures/models/myrmex/myrmex_jungle_egg.png");
    public static final Identifier EGG_DESERT = Identifier.parse("iceandfire_fabric:textures/models/myrmex/myrmex_desert_egg.png");

    public RenderMyrmexEgg(EntityRendererProvider.Context context) {
        super(context, new ModelDragonEgg(), 0.3F);
    }

    @Override
    protected boolean shouldShowName(EntityMyrmexEgg entity, double distanceToCameraSq) {
        return entity.shouldShowName() && entity.hasCustomName();
    }

    @Override
    public Identifier getTextureLocation(EntityMyrmexEgg entity) {
        return entity.isJungle() ? EGG_JUNGLE : EGG_DESERT;
    }

}

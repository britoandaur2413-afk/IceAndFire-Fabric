package com.iafabric.client.render.entity;

import com.iafabric.client.model.ModelDragonEgg;
import com.iafabric.entity.EntityDragonEgg;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.NotNull;


public class RenderDragonEgg extends IafMobRenderer<EntityDragonEgg, ModelDragonEgg<EntityDragonEgg>> {

    public static final Identifier EGG_RED = Identifier.parse("iceandfire_fabric:textures/models/firedragon/egg_red.png");
    public static final Identifier EGG_GREEN = Identifier.parse("iceandfire_fabric:textures/models/firedragon/egg_green.png");
    public static final Identifier EGG_BRONZE = Identifier.parse("iceandfire_fabric:textures/models/firedragon/egg_bronze.png");
    public static final Identifier EGG_GREY = Identifier.parse("iceandfire_fabric:textures/models/firedragon/egg_gray.png");
    public static final Identifier EGG_BLUE = Identifier.parse("iceandfire_fabric:textures/models/icedragon/egg_blue.png");
    public static final Identifier EGG_WHITE = Identifier.parse("iceandfire_fabric:textures/models/icedragon/egg_white.png");
    public static final Identifier EGG_SAPPHIRE = Identifier.parse("iceandfire_fabric:textures/models/icedragon/egg_sapphire.png");
    public static final Identifier EGG_SILVER = Identifier.parse("iceandfire_fabric:textures/models/icedragon/egg_silver.png");
    public static final Identifier EGG_ELECTRIC = Identifier.parse("iceandfire_fabric:textures/models/lightningdragon/egg_electric.png");
    public static final Identifier EGG_AMYTHEST = Identifier.parse("iceandfire_fabric:textures/models/lightningdragon/egg_amythest.png");
    public static final Identifier EGG_BLACK = Identifier.parse("iceandfire_fabric:textures/models/lightningdragon/egg_black.png");
    public static final Identifier EGG_COPPER = Identifier.parse("iceandfire_fabric:textures/models/lightningdragon/egg_copper.png");

    public RenderDragonEgg(EntityRendererProvider.Context context) {
        super(context, new ModelDragonEgg(), 0.3F);
    }

    @Override
    protected boolean shouldShowName(EntityDragonEgg entity, double distanceToCameraSq) {
        return entity.shouldShowName() && entity.hasCustomName();
    }


    @Override
    public Identifier getTextureLocation(EntityDragonEgg entity) {
        switch (entity.getEggType()) {
            default:
                return EGG_RED;
            case GREEN:
                return EGG_GREEN;
            case BRONZE:
                return EGG_BRONZE;
            case GRAY:
                return EGG_GREY;
            case BLUE:
                return EGG_BLUE;
            case WHITE:
                return EGG_WHITE;
            case SAPPHIRE:
                return EGG_SAPPHIRE;
            case SILVER:
                return EGG_SILVER;
            case ELECTRIC:
                return EGG_ELECTRIC;
            case AMYTHEST:
                return EGG_AMYTHEST;
            case COPPER:
                return EGG_COPPER;
            case BLACK:
                return EGG_BLACK;

        }
    }

}

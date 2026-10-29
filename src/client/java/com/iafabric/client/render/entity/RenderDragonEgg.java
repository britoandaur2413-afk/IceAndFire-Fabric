package com.iafabric.client.render.entity;

import com.iafabric.client.model.ModelDragonEgg;
import com.iafabric.entity.EntityDragonEgg;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;


public class RenderDragonEgg extends IafMobRenderer<EntityDragonEgg, ModelDragonEgg<EntityDragonEgg>> {

    public static final ResourceLocation EGG_RED = ResourceLocation.parse("iceandfire_fabric:textures/models/firedragon/egg_red.png");
    public static final ResourceLocation EGG_GREEN = ResourceLocation.parse("iceandfire_fabric:textures/models/firedragon/egg_green.png");
    public static final ResourceLocation EGG_BRONZE = ResourceLocation.parse("iceandfire_fabric:textures/models/firedragon/egg_bronze.png");
    public static final ResourceLocation EGG_GREY = ResourceLocation.parse("iceandfire_fabric:textures/models/firedragon/egg_gray.png");
    public static final ResourceLocation EGG_BLUE = ResourceLocation.parse("iceandfire_fabric:textures/models/icedragon/egg_blue.png");
    public static final ResourceLocation EGG_WHITE = ResourceLocation.parse("iceandfire_fabric:textures/models/icedragon/egg_white.png");
    public static final ResourceLocation EGG_SAPPHIRE = ResourceLocation.parse("iceandfire_fabric:textures/models/icedragon/egg_sapphire.png");
    public static final ResourceLocation EGG_SILVER = ResourceLocation.parse("iceandfire_fabric:textures/models/icedragon/egg_silver.png");
    public static final ResourceLocation EGG_ELECTRIC = ResourceLocation.parse("iceandfire_fabric:textures/models/lightningdragon/egg_electric.png");
    public static final ResourceLocation EGG_AMYTHEST = ResourceLocation.parse("iceandfire_fabric:textures/models/lightningdragon/egg_amythest.png");
    public static final ResourceLocation EGG_BLACK = ResourceLocation.parse("iceandfire_fabric:textures/models/lightningdragon/egg_black.png");
    public static final ResourceLocation EGG_COPPER = ResourceLocation.parse("iceandfire_fabric:textures/models/lightningdragon/egg_copper.png");

    public RenderDragonEgg(EntityRendererProvider.Context context) {
        super(context, new ModelDragonEgg(), 0.3F);
    }

    @Override
    protected boolean shouldShowName(EntityDragonEgg entity) {
        return entity.shouldShowName() && entity.hasCustomName();
    }


    @Override
    public ResourceLocation getTextureLocation(EntityDragonEgg entity) {
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

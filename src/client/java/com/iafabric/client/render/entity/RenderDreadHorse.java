package com.iafabric.client.render.entity;

import net.minecraft.client.model.HorseModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.UndeadHorseRenderer;
import net.minecraft.client.renderer.entity.layers.EyesLayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.animal.horse.AbstractHorse;

/** The dread knight's horse: a skeleton horse with the dread texture and glowing eyes. */
public class RenderDreadHorse extends UndeadHorseRenderer {
    public static final ResourceLocation TEXTURE = ResourceLocation.parse("iceandfire_fabric:textures/models/dread/dread_knight_horse.png");
    public static final ResourceLocation TEXTURE_EYES = ResourceLocation.parse("iceandfire_fabric:textures/models/dread/dread_knight_horse_eyes.png");
    private static final RenderType EYES = RenderType.eyes(TEXTURE_EYES);

    public RenderDreadHorse(EntityRendererProvider.Context context) {
        super(context, ModelLayers.SKELETON_HORSE);
        this.addLayer(new EyesLayer<AbstractHorse, HorseModel<AbstractHorse>>(this) {
            @Override
            public RenderType renderType() {
                return EYES;
            }
        });
    }

    @Override
    public ResourceLocation getTextureLocation(AbstractHorse horse) {
        return TEXTURE;
    }
}

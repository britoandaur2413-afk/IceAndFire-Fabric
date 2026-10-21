package com.iafabric.client.render.entity;

import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.UndeadHorseRenderer;
import net.minecraft.client.renderer.entity.layers.EyesLayer;
import net.minecraft.client.renderer.entity.state.EquineRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.resources.model.EquipmentClientInfo;
import net.minecraft.resources.Identifier;

/** The dread knight's horse: a skeleton horse with the dread texture and glowing eyes. */
public class RenderDreadHorse extends UndeadHorseRenderer {
    public static final Identifier TEXTURE = Identifier.parse("iceandfire_fabric:textures/models/dread/dread_knight_horse.png");
    public static final Identifier TEXTURE_EYES = Identifier.parse("iceandfire_fabric:textures/models/dread/dread_knight_horse_eyes.png");
    private static final RenderType EYES = RenderTypes.eyes(TEXTURE_EYES);

    public RenderDreadHorse(EntityRendererProvider.Context context) {
        super(context, EquipmentClientInfo.LayerType.SKELETON_HORSE_SADDLE, ModelLayers.SKELETON_HORSE_SADDLE, UndeadHorseRenderer.Type.SKELETON,
            UndeadHorseRenderer.Type.SKELETON_BABY);
        this.addLayer(new EyesLayer<>(this) {
            @Override
            public RenderType renderType() {
                return EYES;
            }
        });
    }

    @Override
    public Identifier getTextureLocation(EquineRenderState state) {
        return TEXTURE;
    }
}

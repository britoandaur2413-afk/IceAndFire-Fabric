package com.iafabric.client.render.entity;

import com.iafabric.citadel.client.model.TabulaModel;
import com.iafabric.client.render.entity.IafMobRenderer;
import com.iafabric.citadel.client.model.AdvancedEntityModel;
import com.iafabric.client.render.entity.layer.LayerSeaSerpentAncient;
import com.iafabric.entity.EntitySeaSerpent;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.NotNull;

public class RenderSeaSerpent extends IafMobRenderer<EntitySeaSerpent, TabulaModel> {

    public static final Identifier TEXTURE_BLUE = Identifier.parse("iceandfire_fabric:textures/models/seaserpent/seaserpent_blue.png");
    public static final Identifier TEXTURE_BLUE_BLINK = Identifier.parse("iceandfire_fabric:textures/models/seaserpent/seaserpent_blue_blink.png");
    public static final Identifier TEXTURE_BRONZE = Identifier.parse("iceandfire_fabric:textures/models/seaserpent/seaserpent_bronze.png");
    public static final Identifier TEXTURE_BRONZE_BLINK = Identifier.parse("iceandfire_fabric:textures/models/seaserpent/seaserpent_bronze_blink.png");
    public static final Identifier TEXTURE_DARKBLUE = Identifier.parse("iceandfire_fabric:textures/models/seaserpent/seaserpent_darkblue.png");
    public static final Identifier TEXTURE_DARKBLUE_BLINK = Identifier.parse("iceandfire_fabric:textures/models/seaserpent/seaserpent_darkblue_blink.png");
    public static final Identifier TEXTURE_GREEN = Identifier.parse("iceandfire_fabric:textures/models/seaserpent/seaserpent_green.png");
    public static final Identifier TEXTURE_GREEN_BLINK = Identifier.parse("iceandfire_fabric:textures/models/seaserpent/seaserpent_green_blink.png");
    public static final Identifier TEXTURE_PURPLE = Identifier.parse("iceandfire_fabric:textures/models/seaserpent/seaserpent_purple.png");
    public static final Identifier TEXTURE_PURPLE_BLINK = Identifier.parse("iceandfire_fabric:textures/models/seaserpent/seaserpent_purple_blink.png");
    public static final Identifier TEXTURE_RED = Identifier.parse("iceandfire_fabric:textures/models/seaserpent/seaserpent_red.png");
    public static final Identifier TEXTURE_RED_BLINK = Identifier.parse("iceandfire_fabric:textures/models/seaserpent/seaserpent_red_blink.png");
    public static final Identifier TEXTURE_TEAL = Identifier.parse("iceandfire_fabric:textures/models/seaserpent/seaserpent_teal.png");
    public static final Identifier TEXTURE_TEAL_BLINK = Identifier.parse("iceandfire_fabric:textures/models/seaserpent/seaserpent_teal_blink.png");

    public RenderSeaSerpent(EntityRendererProvider.Context context, TabulaModel model) {
        super(context, model, 1.6F);
        this.layers.add(new LayerSeaSerpentAncient(this));

    }

    @Override
    protected void scale(EntitySeaSerpent entity, PoseStack matrixStackIn, float partialTickTime) {
        this.shadowRadius = entity.getSeaSerpentScale();
        matrixStackIn.scale(shadowRadius, shadowRadius, shadowRadius);
    }

    @Override
    public Identifier getTextureLocation(EntitySeaSerpent serpent) {
        switch (serpent.getVariant()) {
            case 0:
                if (serpent.isBlinking()) {
                    return TEXTURE_BLUE_BLINK;
                } else {
                    return TEXTURE_BLUE;
                }
            case 1:
                if (serpent.isBlinking()) {
                    return TEXTURE_BRONZE_BLINK;
                } else {
                    return TEXTURE_BRONZE;
                }
            case 2:
                if (serpent.isBlinking()) {
                    return TEXTURE_DARKBLUE_BLINK;
                } else {
                    return TEXTURE_DARKBLUE;
                }
            case 3:
                if (serpent.isBlinking()) {
                    return TEXTURE_GREEN_BLINK;
                } else {
                    return TEXTURE_GREEN;
                }
            case 4:
                if (serpent.isBlinking()) {
                    return TEXTURE_PURPLE_BLINK;
                } else {
                    return TEXTURE_PURPLE;
                }
            case 5:
                if (serpent.isBlinking()) {
                    return TEXTURE_RED_BLINK;
                } else {
                    return TEXTURE_RED;
                }
            case 6:
                if (serpent.isBlinking()) {
                    return TEXTURE_TEAL_BLINK;
                } else {
                    return TEXTURE_TEAL;
                }
        }
        return TEXTURE_BLUE;
    }

}

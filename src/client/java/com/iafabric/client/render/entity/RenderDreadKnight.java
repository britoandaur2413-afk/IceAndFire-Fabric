package com.iafabric.client.render.entity;

import com.iafabric.client.render.entity.layer.LayerHeldItem;
import com.iafabric.client.render.entity.IafMobRenderer;
import com.iafabric.client.model.ModelDreadKnight;
import com.iafabric.client.render.entity.layer.LayerGenericGlowing;
import com.iafabric.entity.EntityDreadKnight;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.NotNull;

import org.jspecify.annotations.Nullable;

public class RenderDreadKnight extends IafMobRenderer<EntityDreadKnight, ModelDreadKnight> {
    public static final Identifier TEXTURE_EYES = Identifier.parse("iceandfire_fabric:textures/models/dread/dread_knight_eyes.png");
    public static final Identifier TEXTURE_0 = Identifier.parse("iceandfire_fabric:textures/models/dread/dread_knight_1.png");
    public static final Identifier TEXTURE_1 = Identifier.parse("iceandfire_fabric:textures/models/dread/dread_knight_2.png");
    public static final Identifier TEXTURE_2 = Identifier.parse("iceandfire_fabric:textures/models/dread/dread_knight_3.png");

    public RenderDreadKnight(EntityRendererProvider.Context context) {
        super(context, new ModelDreadKnight(0.0F), 0.6F);
        this.addLayer(new LayerGenericGlowing<>(this, TEXTURE_EYES));
        this.addLayer(new LayerHeldItem<>(this));
    }

    @Override
    protected void scale(@NotNull EntityDreadKnight entity, PoseStack matrixStackIn, float partialTickTime) {
        matrixStackIn.scale(0.95F, 0.95F, 0.95F);
    }

    @Nullable
    @Override
    public Identifier getTextureLocation(EntityDreadKnight entity) {
        switch (entity.getArmorVariant()) {
            case 0:
                return TEXTURE_0;
            case 1:
                return TEXTURE_1;
            case 2:
                return TEXTURE_2;
            default:
                return TEXTURE_0;
        }
    }
}

package com.iafabric.client.render.entity;

import com.iafabric.client.render.entity.IafMobRenderer;
import com.iafabric.client.model.ModelPixie;
import com.iafabric.client.render.entity.layer.LayerPixieGlow;
import com.iafabric.client.render.entity.layer.LayerPixieItem;
import com.iafabric.entity.EntityPixie;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.NotNull;

public class RenderPixie extends IafMobRenderer<EntityPixie, ModelPixie> {

    public static final Identifier TEXTURE_0 = Identifier.parse("iceandfire_fabric:textures/models/pixie/pixie_0.png");
    public static final Identifier TEXTURE_1 = Identifier.parse("iceandfire_fabric:textures/models/pixie/pixie_1.png");
    public static final Identifier TEXTURE_2 = Identifier.parse("iceandfire_fabric:textures/models/pixie/pixie_2.png");
    public static final Identifier TEXTURE_3 = Identifier.parse("iceandfire_fabric:textures/models/pixie/pixie_3.png");
    public static final Identifier TEXTURE_4 = Identifier.parse("iceandfire_fabric:textures/models/pixie/pixie_4.png");
    public static final Identifier TEXTURE_5 = Identifier.parse("iceandfire_fabric:textures/models/pixie/pixie_5.png");

    public RenderPixie(EntityRendererProvider.Context context) {
        super(context, new ModelPixie(), 0.2F);
        this.layers.add(new LayerPixieItem(this));
        this.layers.add(new LayerPixieGlow(this));

    }

    @Override
    public void scale(EntityPixie LivingEntityIn, PoseStack stack, float partialTickTime) {
        stack.scale(0.55F, 0.55F, 0.55F);
        if (LivingEntityIn.isOrderedToSit()) {
            stack.translate(0F, 0.5F, 0F);

        }
    }

    @Override
    public Identifier getTextureLocation(EntityPixie pixie) {
        switch (pixie.getColor()) {
            default:
                return TEXTURE_0;
            case 1:
                return TEXTURE_1;
            case 2:
                return TEXTURE_2;
            case 3:
                return TEXTURE_3;
            case 4:
                return TEXTURE_4;
            case 5:
                return TEXTURE_5;
        }
    }

}

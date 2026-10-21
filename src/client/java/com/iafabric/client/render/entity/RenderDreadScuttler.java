package com.iafabric.client.render.entity;

import com.iafabric.client.render.entity.IafMobRenderer;
import com.iafabric.client.model.ModelDreadScuttler;
import com.iafabric.client.render.entity.layer.LayerGenericGlowing;
import com.iafabric.entity.EntityDreadScuttler;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.NotNull;

public class RenderDreadScuttler extends IafMobRenderer<EntityDreadScuttler, ModelDreadScuttler> {

    public static final Identifier TEXTURE_EYES = Identifier.parse("iceandfire_fabric:textures/models/dread/dread_scuttler_eyes.png");
    public static final Identifier TEXTURE = Identifier.parse("iceandfire_fabric:textures/models/dread/dread_scuttler.png");

    public RenderDreadScuttler(EntityRendererProvider.Context context) {
        super(context, new ModelDreadScuttler(), 0.75F);
        this.addLayer(new LayerGenericGlowing(this, TEXTURE_EYES));
    }

    @Override
    public void scale(EntityDreadScuttler LivingEntityIn, PoseStack stack, float partialTickTime) {
        stack.scale(LivingEntityIn.getSize(), LivingEntityIn.getSize(), LivingEntityIn.getSize());
    }

    @Override
    public Identifier getTextureLocation(EntityDreadScuttler beast) {
        return TEXTURE;

    }

}

package com.iafabric.client.render.entity;

import com.iafabric.client.render.entity.IafMobRenderer;
import com.iafabric.client.model.ModelStymphalianBird;
import com.iafabric.entity.EntityStymphalianBird;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.NotNull;

public class RenderStymphalianBird extends IafMobRenderer<EntityStymphalianBird, ModelStymphalianBird> {

    public static final Identifier TEXTURE = Identifier.parse("iceandfire_fabric:textures/models/stymphalianbird/stymphalian_bird.png");

    public RenderStymphalianBird(EntityRendererProvider.Context context) {
        super(context, new ModelStymphalianBird(), 0.6F);
    }

    @Override
    public void scale(@NotNull EntityStymphalianBird LivingEntityIn, PoseStack stack, float partialTickTime) {
        stack.scale(0.75F, 0.75F, 0.75F);
    }

    @Override
    public Identifier getTextureLocation(EntityStymphalianBird cyclops) {
        return TEXTURE;
    }

}

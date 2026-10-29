package com.iafabric.client.render.entity;

import com.iafabric.client.render.entity.IafMobRenderer;
import com.iafabric.client.model.ModelSiren;
import com.iafabric.entity.EntitySiren;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

public class RenderSiren extends IafMobRenderer<EntitySiren, ModelSiren> {

    public static final ResourceLocation TEXTURE_0 = ResourceLocation.parse("iceandfire_fabric:textures/models/siren/siren_0.png");
    public static final ResourceLocation TEXTURE_0_AGGRESSIVE = ResourceLocation.parse("iceandfire_fabric:textures/models/siren/siren_0_aggressive.png");
    public static final ResourceLocation TEXTURE_1 = ResourceLocation.parse("iceandfire_fabric:textures/models/siren/siren_1.png");
    public static final ResourceLocation TEXTURE_1_AGGRESSIVE = ResourceLocation.parse("iceandfire_fabric:textures/models/siren/siren_1_aggressive.png");
    public static final ResourceLocation TEXTURE_2 = ResourceLocation.parse("iceandfire_fabric:textures/models/siren/siren_2.png");
    public static final ResourceLocation TEXTURE_2_AGGRESSIVE = ResourceLocation.parse("iceandfire_fabric:textures/models/siren/siren_2_aggressive.png");

    public RenderSiren(EntityRendererProvider.Context context) {
        super(context, new ModelSiren(), 0.8F);
    }

    @Override
    public void scale(@NotNull EntitySiren LivingEntityIn, PoseStack stack, float partialTickTime) {
        stack.translate(0, 0, -0.5F);

    }

    @Override
    public ResourceLocation getTextureLocation(EntitySiren siren) {
        switch (siren.getHairColor()) {
            default:
                return siren.isAgressive() ? TEXTURE_0_AGGRESSIVE : TEXTURE_0;
            case 1:
                return siren.isAgressive() ? TEXTURE_1_AGGRESSIVE : TEXTURE_1;
            case 2:
                return siren.isAgressive() ? TEXTURE_2_AGGRESSIVE : TEXTURE_2;
        }
    }

    public static ResourceLocation getSirenOverlayTexture(int siren) {
        switch (siren) {
            default:
                return TEXTURE_0;
            case 1:
                return TEXTURE_1;
            case 2:
                return TEXTURE_2;
        }
    }

}

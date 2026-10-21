package com.iafabric.client.render.entity;

import com.iafabric.client.render.entity.IafMobRenderer;
import com.iafabric.client.model.ModelCyclops;
import com.iafabric.entity.EntityCyclops;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.NotNull;

public class RenderCyclops extends IafMobRenderer<EntityCyclops, ModelCyclops> {

    public static final Identifier TEXTURE_0 = Identifier.parse("iceandfire_fabric:textures/models/cyclops/cyclops_0.png");
    public static final Identifier BLINK_0_TEXTURE = Identifier.parse("iceandfire_fabric:textures/models/cyclops/cyclops_0_blink.png");
    public static final Identifier BLINDED_0_TEXTURE = Identifier.parse("iceandfire_fabric:textures/models/cyclops/cyclops_0_injured.png");
    public static final Identifier TEXTURE_1 = Identifier.parse("iceandfire_fabric:textures/models/cyclops/cyclops_1.png");
    public static final Identifier BLINK_1_TEXTURE = Identifier.parse("iceandfire_fabric:textures/models/cyclops/cyclops_1_blink.png");
    public static final Identifier BLINDED_1_TEXTURE = Identifier.parse("iceandfire_fabric:textures/models/cyclops/cyclops_1_injured.png");
    public static final Identifier TEXTURE_2 = Identifier.parse("iceandfire_fabric:textures/models/cyclops/cyclops_2.png");
    public static final Identifier BLINK_2_TEXTURE = Identifier.parse("iceandfire_fabric:textures/models/cyclops/cyclops_2_blink.png");
    public static final Identifier BLINDED_2_TEXTURE = Identifier.parse("iceandfire_fabric:textures/models/cyclops/cyclops_2_injured.png");
    public static final Identifier TEXTURE_3 = Identifier.parse("iceandfire_fabric:textures/models/cyclops/cyclops_3.png");
    public static final Identifier BLINK_3_TEXTURE = Identifier.parse("iceandfire_fabric:textures/models/cyclops/cyclops_3_blink.png");
    public static final Identifier BLINDED_3_TEXTURE = Identifier.parse("iceandfire_fabric:textures/models/cyclops/cyclops_3_injured.png");

    public RenderCyclops(EntityRendererProvider.Context context) {
        super(context, new ModelCyclops(), 1.6F);
    }

    @Override
    protected void scale(@NotNull EntityCyclops entity, PoseStack matrixStackIn, float partialTickTime) {
        matrixStackIn.scale(2.25F, 2.25F, 2.25F);

    }

    @Override
    public Identifier getTextureLocation(EntityCyclops cyclops) {
        switch (cyclops.getVariant()) {
            case 0:
                if (cyclops.isBlinded()) {
                    return BLINDED_0_TEXTURE;
                } else if (cyclops.isBlinking()) {
                    return BLINK_0_TEXTURE;
                } else {
                    return TEXTURE_0;
                }
            case 1:
                if (cyclops.isBlinded()) {
                    return BLINDED_1_TEXTURE;
                } else if (cyclops.isBlinking()) {
                    return BLINK_1_TEXTURE;
                } else {
                    return TEXTURE_1;
                }
            case 2:
                if (cyclops.isBlinded()) {
                    return BLINDED_2_TEXTURE;
                } else if (cyclops.isBlinking()) {
                    return BLINK_2_TEXTURE;
                } else {
                    return TEXTURE_2;
                }
            case 3:
                if (cyclops.isBlinded()) {
                    return BLINDED_3_TEXTURE;
                } else if (cyclops.isBlinking()) {
                    return BLINK_3_TEXTURE;
                } else {
                    return TEXTURE_3;
                }
        }
        return TEXTURE_0;
    }

}

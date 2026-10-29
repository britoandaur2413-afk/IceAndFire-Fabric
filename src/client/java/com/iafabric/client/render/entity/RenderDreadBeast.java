package com.iafabric.client.render.entity;

import com.iafabric.client.render.entity.IafMobRenderer;
import com.iafabric.client.model.ModelDreadBeast;
import com.iafabric.client.render.entity.layer.LayerGenericGlowing;
import com.iafabric.entity.EntityDreadBeast;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

public class RenderDreadBeast extends IafMobRenderer<EntityDreadBeast, ModelDreadBeast> {

    public static final ResourceLocation TEXTURE_EYES = ResourceLocation.parse("iceandfire_fabric:textures/models/dread/dread_beast_eyes.png");
    public static final ResourceLocation TEXTURE_0 = ResourceLocation.parse("iceandfire_fabric:textures/models/dread/dread_beast_1.png");
    public static final ResourceLocation TEXTURE_1 = ResourceLocation.parse("iceandfire_fabric:textures/models/dread/dread_beast_2.png");

    public RenderDreadBeast(EntityRendererProvider.Context context) {
        super(context, new ModelDreadBeast(), 0.5F);
        this.addLayer(new LayerGenericGlowing(this, TEXTURE_EYES));
    }

    @Override
    protected void scale(EntityDreadBeast entity, PoseStack matrixStackIn, float partialTickTime) {
        matrixStackIn.scale(entity.getSize(), entity.getSize(), entity.getSize());
    }

    @Override
    public ResourceLocation getTextureLocation(EntityDreadBeast beast) {
        return beast.getVariant() == 1 ? TEXTURE_1 : TEXTURE_0;

    }

}

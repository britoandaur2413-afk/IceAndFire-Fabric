package com.iafabric.client.render.entity;

import com.iafabric.client.render.entity.IafMobRenderer;
import com.iafabric.client.model.ModelDreadLich;
import com.iafabric.client.render.entity.layer.LayerHeldItem;
import com.iafabric.client.render.entity.layer.LayerGenericGlowing;
import com.iafabric.entity.EntityDreadLich;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;

import org.jetbrains.annotations.Nullable;

public class RenderDreadLich extends IafMobRenderer<EntityDreadLich, ModelDreadLich> {
    public static final ResourceLocation TEXTURE_EYES = ResourceLocation.parse("iceandfire_fabric:textures/models/dread/dread_lich_eyes.png");
    public static final ResourceLocation TEXTURE_0 = ResourceLocation.parse("iceandfire_fabric:textures/models/dread/dread_lich_0.png");
    public static final ResourceLocation TEXTURE_1 = ResourceLocation.parse("iceandfire_fabric:textures/models/dread/dread_lich_1.png");
    public static final ResourceLocation TEXTURE_2 = ResourceLocation.parse("iceandfire_fabric:textures/models/dread/dread_lich_2.png");
    public static final ResourceLocation TEXTURE_3 = ResourceLocation.parse("iceandfire_fabric:textures/models/dread/dread_lich_3.png");
    public static final ResourceLocation TEXTURE_4 = ResourceLocation.parse("iceandfire_fabric:textures/models/dread/dread_lich_4.png");
    public final LayerHeldItem<EntityDreadLich, ModelDreadLich> itemLayer;

    public RenderDreadLich(EntityRendererProvider.Context context) {
        super(context, new ModelDreadLich(0.0F), 0.6F);
        this.addLayer(new LayerGenericGlowing<>(this, TEXTURE_EYES));
        this.itemLayer = new LayerHeldItem<>(this);
        this.addLayer(this.itemLayer);
    }

    @Override
    protected void scale(EntityDreadLich entity, PoseStack matrixStackIn, float partialTickTime) {
        matrixStackIn.scale(0.95F, 0.95F, 0.95F);
        if (entity.getAnimation() == this.getModel().getSpawnAnimation()) {
            this.itemLayer.hidden = entity.getAnimationTick() <= this.getModel().getSpawnAnimation().getDuration() - 10;
            return;
        }
        this.itemLayer.hidden = false;
    }

    @Nullable
    @Override
    public ResourceLocation getTextureLocation(EntityDreadLich entity) {
        switch (entity.getVariant()) {
            case 1:
                return TEXTURE_1;
            case 2:
                return TEXTURE_2;
            case 3:
                return TEXTURE_3;
            case 4:
                return TEXTURE_4;
            default:
                return TEXTURE_0;
        }
    }
}

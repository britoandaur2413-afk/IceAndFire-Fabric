package com.iafabric.client.render.entity;

import com.mojang.math.Axis;
import com.iafabric.client.render.BufferSource;
import com.iafabric.client.render.entity.IafMobRenderer;
import com.iafabric.citadel.client.model.AdvancedEntityModel;
import com.iafabric.client.model.ModelMyrmexPupa;
import com.iafabric.client.render.entity.layer.LayerMyrmexItem;
import com.iafabric.entity.EntityMyrmexBase;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.NotNull;


public class RenderMyrmexBase extends IafMobRenderer<EntityMyrmexBase, AdvancedEntityModel<EntityMyrmexBase>> {

    private static final AdvancedEntityModel<EntityMyrmexBase> LARVA_MODEL = new ModelMyrmexPupa();
    private static final AdvancedEntityModel<EntityMyrmexBase> PUPA_MODEL = new ModelMyrmexPupa();
    private final AdvancedEntityModel<EntityMyrmexBase> adultModel;

    public RenderMyrmexBase(EntityRendererProvider.Context context, AdvancedEntityModel<EntityMyrmexBase> model, float shadowSize) {
        super(context, model, shadowSize);
        this.addLayer(new LayerMyrmexItem(this));
        this.adultModel = model;
    }

    @Override
    protected void beforeRender(EntityMyrmexBase entityIn, float partialTicks) {
        if (entityIn.getGrowthStage() == 0) {
            model = LARVA_MODEL;
        } else if (entityIn.getGrowthStage() == 1) {
            model = PUPA_MODEL;
        } else {
            model = adultModel;
        }

    }

    @Override
    protected void scale(EntityMyrmexBase myrmex, @NotNull PoseStack matrixStackIn, float partialTickTime) {
        float scale = myrmex.getModelScale();
        if (myrmex.getGrowthStage() == 0) {
            scale /= 2;
        }
        if (myrmex.getGrowthStage() == 1) {
            scale /= 1.5F;
        }
        matrixStackIn.scale(scale, scale, scale);
        if (myrmex.isPassenger() && myrmex.getGrowthStage() < 2) {
            matrixStackIn.rotateDegrees(Axis.YP, (90));
        }
    }

    @Override
    public Identifier getTextureLocation(EntityMyrmexBase myrmex) {
        return myrmex.getTexture();
    }

}

package com.iafabric.client.model.util;

import com.iafabric.citadel.client.model.AdvancedEntityModel;
import com.iafabric.citadel.client.model.AdvancedModelBox;
import com.iafabric.citadel.client.model.basic.BasicModelPart;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

// The AdvancedModelRenderer/ModelBox uses a child-parent structure
// Meaning that if you change a parents showModel field to false all the children also
// don't get rendered. This is a workaround for that

public class HideableModelRenderer extends AdvancedModelBox {

    public boolean invisible;

    public HideableModelRenderer(AdvancedEntityModel model, String name) {
        super(model, name);
    }

    public HideableModelRenderer(AdvancedEntityModel model, int i, int i1) {
        super(model, i, i1);
    }

    @Override
    public void render(PoseStack matrixStackIn, VertexConsumer bufferIn, int packedLightIn, int packedOverlayIn, float red, float green, float blue, float alpha) {
        if (invisible) {
            invisibleRender(matrixStackIn, bufferIn, packedLightIn, packedOverlayIn, red, green, blue, alpha);
        } else {
            super.render(matrixStackIn, bufferIn, packedLightIn, packedOverlayIn, red, green, blue, alpha);
        }

    }

    @Override
    public void copyFrom(BasicModelPart currentModel) {
        super.copyFrom(currentModel);
    }

    public void invisibleRender(PoseStack matrixStackIn, VertexConsumer bufferIn, int packedLightIn, int packedOverlayIn, float red, float green, float blue, float alpha) {
        if (this.showModel && (!this.cubeList.isEmpty() || !this.childModels.isEmpty())) {
            matrixStackIn.pushPose();
            this.translateAndRotate(matrixStackIn);
            for (BasicModelPart renderer : this.childModels) {
                renderer.render(matrixStackIn, bufferIn, packedLightIn, packedOverlayIn, red, green, blue, alpha);
            }

            matrixStackIn.popPose();
        }
    }
}

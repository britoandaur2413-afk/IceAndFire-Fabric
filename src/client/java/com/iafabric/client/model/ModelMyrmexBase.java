package com.iafabric.client.model;

import com.iafabric.citadel.client.model.basic.BasicModelPart;
import com.iafabric.entity.EntityMyrmexBase;
import com.mojang.blaze3d.vertex.PoseStack;

public abstract class ModelMyrmexBase<T extends EntityMyrmexBase> extends ModelDragonBase<T> {
    private static final ModelMyrmexLarva LARVA_MODEL = new ModelMyrmexLarva();
    private static final ModelMyrmexPupa PUPA_MODEL = new ModelMyrmexPupa();

    public void postRenderArm(float scale, PoseStack stackIn) {
        for (BasicModelPart renderer : this.getHeadParts()) {
            renderer.translateRotate(stackIn);
        }
    }

    public abstract BasicModelPart[] getHeadParts();
}

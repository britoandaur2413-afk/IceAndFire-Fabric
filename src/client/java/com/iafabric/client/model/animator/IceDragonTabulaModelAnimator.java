package com.iafabric.client.model.animator;

import com.iafabric.citadel.client.model.TabulaModel;
import com.iafabric.client.model.util.DragonAnimationsLibrary;
import com.iafabric.client.model.util.EnumDragonModelTypes;
import com.iafabric.client.model.util.EnumDragonPoses;
import com.iafabric.entity.EntityIceDragon;

public class IceDragonTabulaModelAnimator extends DragonTabulaModelAnimator<EntityIceDragon> {

    public IceDragonTabulaModelAnimator() {
        super(DragonAnimationsLibrary.getModel(EnumDragonPoses.GROUND_POSE, EnumDragonModelTypes.ICE_DRAGON_MODEL));

        this.walkPoses = new TabulaModel[]{getModel(EnumDragonPoses.WALK1), getModel(EnumDragonPoses.WALK2), getModel(EnumDragonPoses.WALK3), getModel(EnumDragonPoses.WALK4)};
        this.flyPoses = new TabulaModel[]{getModel(EnumDragonPoses.FLIGHT1), getModel(EnumDragonPoses.FLIGHT2), getModel(EnumDragonPoses.FLIGHT3), getModel(EnumDragonPoses.FLIGHT4), getModel(EnumDragonPoses.FLIGHT5), getModel(EnumDragonPoses.FLIGHT6)};
        this.swimPoses = new TabulaModel[]{getModel(EnumDragonPoses.SWIM1), getModel(EnumDragonPoses.SWIM2), getModel(EnumDragonPoses.SWIM3), getModel(EnumDragonPoses.SWIM4), getModel(EnumDragonPoses.SWIM5)};
    }


    @Override
    protected TabulaModel getModel(EnumDragonPoses pose) {
        return DragonAnimationsLibrary.getModel(pose, EnumDragonModelTypes.ICE_DRAGON_MODEL);
    }

}

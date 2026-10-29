package com.iafabric.entity.ai;

import com.iafabric.entity.EntityMyrmexBase;

public class MyrmexQueenAIWander extends MyrmexAIWander {

    public MyrmexQueenAIWander(EntityMyrmexBase myrmex, double speed) {
        super(myrmex, speed);
    }

    @Override
    public boolean canUse() {
        return (myrmex.canSeeSky() || myrmex.getHive() == null) && super.canUse();
    }
}

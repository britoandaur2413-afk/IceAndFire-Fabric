package com.iafabric.client.render;

import com.iafabric.citadel.client.model.AdvancedModelBox;
import com.iafabric.citadel.client.model.ITabulaModelAnimator;
import com.iafabric.citadel.client.model.TabulaModel;
import com.iafabric.citadel.client.model.container.TabulaModelContainer;

import java.util.List;

public class TabulaModelAccessor extends TabulaModel {
    public TabulaModelAccessor(TabulaModelContainer container, ITabulaModelAnimator tabulaAnimator) {
        super(container, tabulaAnimator);
    }

    public TabulaModelAccessor(TabulaModelContainer container) {
        super(container);
    }

    public List<AdvancedModelBox> getRootBox() {
        return super.rootBoxes;
    }
}

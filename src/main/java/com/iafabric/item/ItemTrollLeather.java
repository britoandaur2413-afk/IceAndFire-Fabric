package com.iafabric.item;

import com.iafabric.IafMod;
import com.iafabric.enums.EnumTroll;
import net.minecraft.world.item.Item;

public class ItemTrollLeather extends Item {

    public ItemTrollLeather(EnumTroll troll) {
        super(IafItemRegistry.props());
    }
}

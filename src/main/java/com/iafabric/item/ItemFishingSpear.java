package com.iafabric.item;

import com.iafabric.IafMod;
import net.minecraft.world.item.Item;

public class ItemFishingSpear extends Item {

    public ItemFishingSpear() {
        super(IafItemRegistry.props().durability(64));
    }
}

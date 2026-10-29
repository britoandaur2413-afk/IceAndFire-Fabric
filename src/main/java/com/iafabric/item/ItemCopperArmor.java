package com.iafabric.item;

import net.minecraft.world.entity.EquipmentSlot;

/** The copper armor of the original mod (the game has no copper armor in 1.21.1). */
public class ItemCopperArmor extends IafArmorItem {

    public ItemCopperArmor(IafArmorMaterial material, EquipmentSlot slot) {
        super(material, slot, IafItemRegistry.props());
    }
}

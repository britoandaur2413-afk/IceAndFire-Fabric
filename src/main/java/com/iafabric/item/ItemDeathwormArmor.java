package com.iafabric.item;

import com.iafabric.IafMod;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.equipment.ArmorMaterial;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import org.jspecify.annotations.Nullable;

public class ItemDeathwormArmor extends IafArmorItem {

    public ItemDeathwormArmor(IafArmorMaterial material, EquipmentSlot slot) {
        super(material, slot, IafItemRegistry.props());
    }




}

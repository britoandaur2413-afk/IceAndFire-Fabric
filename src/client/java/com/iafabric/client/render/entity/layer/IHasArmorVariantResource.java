package com.iafabric.client.render.entity.layer;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;

public interface IHasArmorVariantResource {

    ResourceLocation getArmorResource(int variant, EquipmentSlot slotType);

}

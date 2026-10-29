package com.iafabric.client.render.entity.layer;

import com.iafabric.client.render.entity.IafMobRenderer;
import com.iafabric.citadel.animation.IAnimatedEntity;
import com.iafabric.client.model.ModelBipedBase;
import com.iafabric.entity.util.IHasArmorVariant;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

import org.jetbrains.annotations.Nullable;

public class LayerBipedArmorMultiple<T extends LivingEntity & IHasArmorVariant & IAnimatedEntity,
    M extends ModelBipedBase<T>,
    R extends IafMobRenderer<T, M> & IHasArmorVariantResource,
    A extends ModelBipedBase<T>> extends LayerBipedArmor<T, M, A> {

    R mobRenderer;

    public LayerBipedArmorMultiple(R mobRenderer, A modelLeggings, A modelArmor,
                                   ResourceLocation defaultArmor, ResourceLocation defaultLegArmor) {
        super(mobRenderer, modelLeggings, modelArmor, defaultArmor, defaultLegArmor);
        this.mobRenderer = mobRenderer;
    }

    @Override
    public ResourceLocation getArmorResource(T entity, ItemStack stack, EquipmentSlot slot, @Nullable String type) {
        return this.mobRenderer.getArmorResource(entity.getBodyArmorVariant(), slot);
    }
}

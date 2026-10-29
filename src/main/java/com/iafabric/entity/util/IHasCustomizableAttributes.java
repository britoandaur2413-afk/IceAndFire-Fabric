package com.iafabric.entity.util;

import com.iafabric.IafConfig;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;

/** A mob whose attributes (health, damage, speed...) come from the config when attribute overriding is on. */
public interface IHasCustomizableAttributes {

    /** Sets the base values of the configured attributes on a newly made entity. */
    static <T extends LivingEntity & IHasCustomizableAttributes, M extends LivingEntity & IHasCustomizableAttributes> void applyAttributesForEntity(EntityType<T> type, M entity) {
        if (!IafConfig.allowAttributeOverriding) {
            return;
        }
        AttributeSupplier configured = entity.getConfigurableAttributes().build();
        BuiltInRegistries.ATTRIBUTE.forEach(holder -> {
            if (configured.hasAttribute(holder)) {
                AttributeInstance instance = entity.getAttribute(holder);
                if (instance != null) {
                    instance.setBaseValue(configured.getBaseValue(holder));
                }
            }
        });
        entity.setHealth(entity.getMaxHealth());
    }

    AttributeSupplier.Builder getConfigurableAttributes();
}

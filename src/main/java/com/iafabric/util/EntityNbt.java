package com.iafabric.util;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;

import java.util.Optional;

/** Entities to and from a CompoundTag, the way the old code stored them in items. */
public final class EntityNbt {
    private EntityNbt() {
    }

    public static CompoundTag save(Entity entity) {
        CompoundTag tag = new CompoundTag();
        entity.saveWithoutId(tag);
        return tag;
    }

    public static void load(Entity entity, CompoundTag tag) {
        entity.load(tag);
    }

    /** The tag an entity's own readAdditionalSaveData reads (26.x wraps it in a reader). */
    public static CompoundTag input(Entity entity, CompoundTag tag) {
        return tag;
    }

    public static Optional<EntityType<?>> type(String id) {
        ResourceLocation key = ResourceLocation.tryParse(id);
        return key == null ? Optional.empty() : BuiltInRegistries.ENTITY_TYPE.getOptional(key);
    }
}

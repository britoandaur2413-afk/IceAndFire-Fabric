package com.iafabric.util;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.TagValueOutput;
import net.minecraft.world.level.storage.ValueInput;

import java.util.Optional;

/** Entities to and from a CompoundTag, the way the old code stored them in items. */
public final class EntityNbt {
    private EntityNbt() {
    }

    public static CompoundTag save(Entity entity) {
        TagValueOutput output = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, entity.registryAccess());
        entity.saveWithoutId(output);
        return output.buildResult();
    }

    public static void load(Entity entity, CompoundTag tag) {
        entity.load(TagValueInput.create(ProblemReporter.DISCARDING, entity.registryAccess(), tag));
    }

    /** A reader over a tag, for an entity's own readAdditionalSaveData. */
    public static ValueInput input(Entity entity, CompoundTag tag) {
        return TagValueInput.create(ProblemReporter.DISCARDING, entity.registryAccess(), tag);
    }

    /** A writer to a fresh tag, for an entity's own addAdditionalSaveData (read the tag with output.buildResult()). */
    public static TagValueOutput output(Entity entity) {
        return TagValueOutput.createWithContext(ProblemReporter.DISCARDING, entity.registryAccess());
    }

    public static Optional<EntityType<?>> type(String id) {
        Identifier key = Identifier.tryParse(id);
        return key == null ? Optional.empty() : BuiltInRegistries.ENTITY_TYPE.getOptional(key);
    }
}

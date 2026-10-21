package com.iafabric.misc;

import com.iafabric.IafMod;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.Entity;
import org.jspecify.annotations.Nullable;

/**
 * The mod's damage kinds. They are data (data/iceandfire_fabric/damage_type/*.json); the gorgon's gaze
 * ignores armor through the damage type tags.
 */
public final class IafDamageRegistry {
    public static final ResourceKey<DamageType> GORGON = key("gorgon");
    public static final ResourceKey<DamageType> DRAGON_FIRE = key("dragon_fire");
    public static final ResourceKey<DamageType> DRAGON_ICE = key("dragon_ice");
    public static final ResourceKey<DamageType> DRAGON_LIGHTNING = key("dragon_lightning");

    private IafDamageRegistry() {
    }

    private static ResourceKey<DamageType> key(String name) {
        return ResourceKey.create(Registries.DAMAGE_TYPE, IafMod.id(name));
    }

    private static DamageSource source(ResourceKey<DamageType> type, Entity direct, @Nullable Entity causing) {
        return new DamageSource(direct.level().registryAccess().lookupOrThrow(Registries.DAMAGE_TYPE).getOrThrow(type), direct, causing);
    }

    public static DamageSource causeGorgonDamage(Entity entity) {
        return source(GORGON, entity, entity);
    }

    public static DamageSource causeDragonFireDamage(Entity entity) {
        return source(DRAGON_FIRE, entity, entity);
    }

    public static DamageSource causeIndirectDragonFireDamage(Entity source, @Nullable Entity indirectEntityIn) {
        return source(DRAGON_FIRE, source, indirectEntityIn);
    }

    public static DamageSource causeDragonIceDamage(Entity entity) {
        return source(DRAGON_ICE, entity, entity);
    }

    public static DamageSource causeIndirectDragonIceDamage(Entity source, @Nullable Entity indirectEntityIn) {
        return source(DRAGON_ICE, source, indirectEntityIn);
    }

    public static DamageSource causeDragonLightningDamage(Entity entity) {
        return source(DRAGON_LIGHTNING, entity, entity);
    }

    public static DamageSource causeIndirectDragonLightningDamage(Entity source, @Nullable Entity indirectEntityIn) {
        return source(DRAGON_LIGHTNING, source, indirectEntityIn);
    }
}

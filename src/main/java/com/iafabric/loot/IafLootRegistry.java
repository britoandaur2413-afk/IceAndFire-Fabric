package com.iafabric.loot;

import com.iafabric.IafMod;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;

/** The loot functions that fit a drop to the dragon or sea serpent that dropped it. */
public class IafLootRegistry {

    public static void init() {
        Registry.register(BuiltInRegistries.LOOT_FUNCTION_TYPE, IafMod.id("customize_to_dragon"), CustomizeToDragon.TYPE);
        Registry.register(BuiltInRegistries.LOOT_FUNCTION_TYPE, IafMod.id("customize_to_sea_serpent"), CustomizeToSeaSerpent.TYPE);
    }
}

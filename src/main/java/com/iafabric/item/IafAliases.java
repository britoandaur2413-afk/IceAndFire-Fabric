package com.iafabric.item;

import com.iafabric.IafMod;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import org.jspecify.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

/**
 * Old ids of the mod that are the game's own things now (the game has its own copper): an old world's iceandfire_fabric copper
 * is read as the game's (RegistryAliasMixin).
 */
public final class IafAliases {
	private static final Map<Identifier, Identifier> ITEMS = new HashMap<>();
	private static final Map<Identifier, Identifier> BLOCKS = new HashMap<>();

	static {
		for (String name : new String[]{"copper_ingot", "copper_nugget", "copper_sword", "copper_shovel", "copper_pickaxe", "copper_axe", "copper_hoe"}) {
			ITEMS.put(IafMod.id(name), Identifier.withDefaultNamespace(name));
		}
		for (String part : new String[]{"helmet", "chestplate", "leggings", "boots"}) {
			ITEMS.put(IafMod.id("armor_copper_metal_" + part), Identifier.withDefaultNamespace("copper_" + part));
		}
		for (String name : new String[]{"copper_ore", "copper_block"}) {
			BLOCKS.put(IafMod.id(name), Identifier.withDefaultNamespace(name));
			ITEMS.put(IafMod.id(name), Identifier.withDefaultNamespace(name));
		}
	}

	private IafAliases() {
	}

	/** What an old id of the mod is now in this registry, or null. */
	public static @Nullable Identifier of(ResourceKey<? extends Registry<?>> registry, Identifier id) {
		if (IafMod.STORED.equals(id.getNamespace())) {
			// under the mod's first id: the mod's own now (or the game's, as below)
			Identifier now = IafMod.id(id.getPath());
			Identifier moved = of(registry, now);
			return moved != null ? moved : now;
		}
		if (!IafMod.MOD_ID.equals(id.getNamespace())) {
			return null;
		}
		if (registry == Registries.ITEM) {
			return ITEMS.get(id);
		}
		if (registry == Registries.BLOCK) {
			return BLOCKS.get(id);
		}
		return null;
	}
}

package com.iafabric.item;

import com.iafabric.IafMod;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.resources.ResourceKey;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

/**
 * Old ids of the mod that are the game's own things now (the game has its own copper ingot, ore and block): an old
 * world's iceandfire_fabric copper is read as the game's (RegistryAliasMixin). The nugget, tools and armor stay the mod's.
 */
public final class IafAliases {
	private static final Map<ResourceLocation, ResourceLocation> ITEMS = new HashMap<>();
	private static final Map<ResourceLocation, ResourceLocation> BLOCKS = new HashMap<>();

	static {
		ITEMS.put(IafMod.id("copper_ingot"), ResourceLocation.withDefaultNamespace("copper_ingot"));
		for (String name : new String[]{"copper_ore", "copper_block"}) {
			BLOCKS.put(IafMod.id(name), ResourceLocation.withDefaultNamespace(name));
			ITEMS.put(IafMod.id(name), ResourceLocation.withDefaultNamespace(name));
		}
	}

	private IafAliases() {
	}

	/** What an old id of the mod is now in this registry, or null. */
	public static @Nullable ResourceLocation of(ResourceKey<? extends Registry<?>> registry, ResourceLocation id) {
		if (IafMod.STORED.equals(id.getNamespace())) {
			// under the mod's first id: the mod's own now (or the game's, as below)
			ResourceLocation now = IafMod.id(id.getPath());
			ResourceLocation moved = of(registry, now);
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

package com.iafabric;

import net.fabricmc.fabric.api.creativetab.v1.FabricCreativeModeTab;
import com.iafabric.entity.util.IafDataSerializers;
import com.iafabric.block.IafBlockRegistry;
import com.iafabric.entity.IafEntityRegistry;
import com.iafabric.item.IafItemRegistry;
import com.iafabric.misc.IafSoundRegistry;
import com.iafabric.network.IafNetwork;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Ice and Fire for Fabric: an unofficial port of Ice and Fire by Alexthe666 (art by Raptorfarian),
 * LGPL-3.0. Registers everything and wires the game events.
 */
public class IafMod implements ModInitializer {
	public static final String MOD_ID = "iceandfire_fabric";
	/** The mod's first id: what worlds keep under it (saved data, players' data) stays there, and their old blocks,
	 * items, entities... are read as the mod's own (IafAliases). */
	public static final String STORED = "iafabric";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
	public static boolean DEBUG = false;
	public static String VERSION = "UNKNOWN";
	/** Replaced by the client entrypoint. */
	public static CommonProxy PROXY = new CommonProxy();

	@Override
	public void onInitialize() {
		VERSION = FabricLoader.getInstance().getModContainer(MOD_ID)
			.map(c -> c.getMetadata().getVersion().getFriendlyString()).orElse("UNKNOWN");
		IafConfig.load();
		IafSoundRegistry.init();
		IafBlockRegistry.init();
		IafItemRegistry.init();
		com.iafabric.effect.IafEffects.init();
		com.iafabric.entity.util.IafDataSerializers.init();
		IafEntityRegistry.init();
		com.iafabric.entity.IafVillagerRegistry.init();
		com.iafabric.world.IafVillageHouses.init();
		com.iafabric.world.PendingWorldgen.init();
		com.iafabric.world.FairyLandmarks.init();
		com.iafabric.entity.tile.IafTileEntityRegistry.init();
		com.iafabric.inventory.IafContainerRegistry.init();
		com.iafabric.recipe.IafRecipeRegistry.init();
		com.iafabric.loot.IafLootRegistry.init();
		com.iafabric.world.IafWorldRegistry.init();
		com.iafabric.world.structure.IafStructures.init();
		IafNetwork.init();
		registerTabs();
		com.iafabric.event.ServerEvents.register();
		com.iafabric.event.DragonSkies.register();
		com.iafabric.pixie.PixieBonding.register();
		com.iafabric.pixie.ExiledPixies.register();
		com.iafabric.entity.PillagerFrost.register();
		com.iafabric.entity.DreadDragons.register();
		com.iafabric.vfx.Vfx.register();
		com.iafabric.boss.ThroneFight.register();
		com.iafabric.boss.BossCold.register();
		com.iafabric.boss.Soundtracks.register();
		com.iafabric.boss.CastleWard.register();
		com.iafabric.pixie.PixieMagic.register();
		com.iafabric.command.IafCommands.register();
		ServerLifecycleEvents.SERVER_STARTING.register(server -> IafNetwork.server = server);
		ServerLifecycleEvents.SERVER_STOPPED.register(server -> IafNetwork.server = null);
		LOGGER.info("Ice and Fire Fabric loaded");
	}

	private static void registerTabs() {
		Registry.register(BuiltInRegistries.CREATIVE_MODE_TAB, id("items"), FabricCreativeModeTab.builder()
			.title(Component.translatable("itemGroup.iceandfire_fabric"))
			.icon(() -> new ItemStack(IafItemRegistry.DRAGON_SKULL_FIRE))
			.displayItems((params, output) -> {
				for (Item item : IafItemRegistry.TAB_ITEMS) {
					output.accept(item);
				}
			})
			.build());
		Registry.register(BuiltInRegistries.CREATIVE_MODE_TAB, id("blocks"), FabricCreativeModeTab.builder()
			.title(Component.translatable("itemGroup.iceandfire_fabric.blocks"))
			.icon(() -> new ItemStack(IafBlockRegistry.DRAGON_SCALE_RED))
			.displayItems((params, output) -> {
				for (Item item : IafItemRegistry.TAB_BLOCKS) {
					output.accept(item);
				}
			})
			.build());
	}
	/** A key worlds keep data under (saved data, a player's data): under the mod's first id, so older worlds keep theirs. */
	public static Identifier stored(String path) {
		return Identifier.fromNamespaceAndPath(STORED, path);
	}


	public static Identifier id(String path) {
		return Identifier.fromNamespaceAndPath(MOD_ID, path);
	}
}

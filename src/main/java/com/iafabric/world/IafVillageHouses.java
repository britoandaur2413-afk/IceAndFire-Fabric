package com.iafabric.world;

import com.google.common.collect.ImmutableList;
import com.iafabric.IafConfig;
import com.iafabric.IafMod;
import com.iafabric.mixin.StructureTemplatePoolAccessor;
import com.iafabric.world.gen.processor.VillageHouseProcessor;
import net.fabricmc.fabric.api.event.registry.DynamicRegistrySetupCallback;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.structure.pools.StructurePoolElement;
import net.minecraft.world.level.levelgen.structure.pools.StructureTemplatePool;
import net.minecraft.world.level.levelgen.structure.templatesystem.AlwaysTrueTest;
import net.minecraft.world.level.levelgen.structure.templatesystem.ProcessorRule;
import net.minecraft.world.level.levelgen.structure.templatesystem.RandomBlockMatchTest;
import net.minecraft.world.level.levelgen.structure.templatesystem.RuleProcessor;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessorList;

/**
 * The scribe's house in the five kinds of villages, like the 1.18 mod: added to each village's houses
 * (with the weight of the config), its cobblestone a little mossy, its piles and lectern filled in.
 */
public final class IafVillageHouses {
	private static final String[] VILLAGE_TYPES = {"plains", "desert", "snowy", "savanna", "taiga"};

	private IafVillageHouses() {
	}

	public static void init() {
		if (IafConfig.villagerHouseWeight <= 0) {
			return;
		}
		Holder<StructureProcessorList> processors = Holder.direct(new StructureProcessorList(ImmutableList.of(
			new RuleProcessor(ImmutableList.of(new ProcessorRule(new RandomBlockMatchTest(Blocks.COBBLESTONE, 0.1F), AlwaysTrueTest.INSTANCE, Blocks.MOSSY_COBBLESTONE.defaultBlockState()))),
			VillageHouseProcessor.INSTANCE)));
		DynamicRegistrySetupCallback.EVENT.register(view -> view.<StructureTemplatePool>registerEntryAdded(Registries.TEMPLATE_POOL, (rawId, id, pool) -> {
			for (String type : VILLAGE_TYPES) {
				if (id.getNamespace().equals("minecraft") && id.getPath().equals("village/" + type + "/houses")) {
					StructurePoolElement house = StructurePoolElement.legacy(IafMod.MOD_ID + ":village/" + type + "_scriber_1", processors)
						.apply(StructureTemplatePool.Projection.RIGID);
					for (int i = 0; i < IafConfig.villagerHouseWeight; i++) {
						((StructureTemplatePoolAccessor) pool).iceandfire_fabric$templates().add(house);
					}
				}
			}
		}));
	}
}

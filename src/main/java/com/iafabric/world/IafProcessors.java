package com.iafabric.world;

import com.iafabric.IafMod;
import com.iafabric.world.gen.processor.DreadPortalProcessor;
import com.iafabric.world.gen.processor.DreadRuinProcessor;
import com.iafabric.world.gen.processor.GorgonTempleProcessor;
import com.iafabric.world.gen.processor.GraveyardProcessor;
import com.iafabric.world.gen.processor.VillageHouseProcessor;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessor;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessorType;

/** The structure processors used by the mod's structure pieces (data/iceandfire_fabric/worldgen/processor_list). */
public final class IafProcessors {
    public static final StructureProcessorType<DreadRuinProcessor> DREADRUINPROCESSOR = register("dread_mausoleum_processor", DreadRuinProcessor.CODEC);
    public static final StructureProcessorType<GorgonTempleProcessor> GORGONTEMPLEPROCESSOR = register("gorgon_temple_processor", GorgonTempleProcessor.CODEC);
    public static final StructureProcessorType<GraveyardProcessor> GRAVEYARDPROCESSOR = register("graveyard_processor", GraveyardProcessor.CODEC);
    public static final StructureProcessorType<DreadPortalProcessor> DREADPORTALPROCESSOR = register("dread_portal_processor", DreadPortalProcessor.CODEC);
    public static final StructureProcessorType<VillageHouseProcessor> VILLAGEHOUSEPROCESSOR = register("village_house_processor", VillageHouseProcessor.CODEC);

    private IafProcessors() {
    }

    private static <P extends StructureProcessor> StructureProcessorType<P> register(String name, MapCodec<P> codec) {
        return Registry.register(BuiltInRegistries.STRUCTURE_PROCESSOR, IafMod.id(name), () -> codec.codec());
    }

    public static void init() {
    }
}

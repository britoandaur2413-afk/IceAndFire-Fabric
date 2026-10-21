package com.iafabric.world;

import com.iafabric.IafMod;
import com.iafabric.world.gen.processor.DreadRuinProcessor;
import com.iafabric.world.gen.processor.GorgonTempleProcessor;
import com.iafabric.world.gen.processor.GraveyardProcessor;
import com.iafabric.world.gen.processor.VillageHouseProcessor;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessor;

/** The structure processors used by the mod's structure pieces (data/iceandfire_fabric/worldgen/processor_list). */
public final class IafProcessors {
    public static final MapCodec<DreadRuinProcessor> DREADRUINPROCESSOR = register("dread_mausoleum_processor", DreadRuinProcessor.CODEC);
    public static final MapCodec<GorgonTempleProcessor> GORGONTEMPLEPROCESSOR = register("gorgon_temple_processor", GorgonTempleProcessor.CODEC);
    public static final MapCodec<GraveyardProcessor> GRAVEYARDPROCESSOR = register("graveyard_processor", GraveyardProcessor.CODEC);
    public static final MapCodec<VillageHouseProcessor> VILLAGEHOUSEPROCESSOR = register("village_house_processor", VillageHouseProcessor.CODEC);

    private IafProcessors() {
    }

    private static <P extends StructureProcessor> MapCodec<P> register(String name, MapCodec<P> codec) {
        return Registry.register(BuiltInRegistries.STRUCTURE_PROCESSOR, IafMod.id(name), codec);
    }

    public static void init() {
    }
}

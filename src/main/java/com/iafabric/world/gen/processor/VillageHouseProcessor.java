package com.iafabric.world.gen.processor;

import com.mojang.serialization.MapCodec;
import com.iafabric.world.IafProcessors;
import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessor;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessorType;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import org.jetbrains.annotations.NotNull;

import org.jetbrains.annotations.Nullable;
import net.minecraft.util.RandomSource;

public class VillageHouseProcessor extends StructureProcessor {

    public static final ResourceLocation LOOT = ResourceLocation.fromNamespaceAndPath("iceandfire_fabric", "chest/village_scribe");
    public static final VillageHouseProcessor INSTANCE = new VillageHouseProcessor();
    public static final MapCodec<VillageHouseProcessor> CODEC = MapCodec.unit(() -> INSTANCE);

    public VillageHouseProcessor() {
    }

    @Override
    public StructureTemplate.StructureBlockInfo processBlock(LevelReader worldReader, BlockPos pos, BlockPos pos2, StructureTemplate.StructureBlockInfo templateRelativePosInfo, StructureTemplate.StructureBlockInfo infoIn2, StructurePlaceSettings settings) {
        RandomSource random = settings.getRandom(infoIn2.pos());
        if (infoIn2.state().getBlock() == Blocks.CHEST) {
            CompoundTag tag = new CompoundTag();
            tag.putString("LootTable", LOOT.toString());
            tag.putLong("LootTableSeed", random.nextLong());
            return new StructureTemplate.StructureBlockInfo(infoIn2.pos(), infoIn2.state(), tag);
        }
        return infoIn2;
    }

    @Override
    protected StructureProcessorType<?> getType() {
        return IafProcessors.VILLAGEHOUSEPROCESSOR;
    }
}

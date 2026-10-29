package com.iafabric.world.gen.processor;

import com.mojang.serialization.MapCodec;
import com.iafabric.block.IafBlockRegistry;
import com.iafabric.entity.IafEntityRegistry;
import com.iafabric.world.IafProcessors;
import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessor;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessorType;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.core.registries.BuiltInRegistries;
import org.jetbrains.annotations.NotNull;

import org.jetbrains.annotations.Nullable;
import net.minecraft.util.RandomSource;

public class DreadRuinProcessor extends StructureProcessor {

    private final float integrity = 1.0F;
    public static final DreadRuinProcessor INSTANCE = new DreadRuinProcessor();
    public static final MapCodec<DreadRuinProcessor> CODEC = MapCodec.unit(() -> INSTANCE);

    public DreadRuinProcessor() {
    }

    public static BlockState getRandomCrackedBlock(@Nullable BlockState prev, RandomSource random) {
        float rand = random.nextFloat();
        if (rand < 0.5) {
            return IafBlockRegistry.DREAD_STONE_BRICKS.defaultBlockState();
        } else if (rand < 0.9) {
            return IafBlockRegistry.DREAD_STONE_BRICKS_CRACKED.defaultBlockState();
        } else {
            return IafBlockRegistry.DREAD_STONE_BRICKS_MOSSY.defaultBlockState();
        }
    }

    @Override
    public StructureTemplate.StructureBlockInfo processBlock(LevelReader worldReader, BlockPos pos, BlockPos pos2, StructureTemplate.StructureBlockInfo templateRelativePosInfo, StructureTemplate.StructureBlockInfo infoIn2, StructurePlaceSettings settings) {
        RandomSource random = settings.getRandom(infoIn2.pos());
        if (random.nextFloat() <= integrity) {
            if (infoIn2.state().getBlock() == IafBlockRegistry.DREAD_STONE_BRICKS) {
                BlockState state = getRandomCrackedBlock(null, random);
                return new StructureTemplate.StructureBlockInfo(infoIn2.pos(), state, null);
            }
            if (infoIn2.state().getBlock() == IafBlockRegistry.DREAD_SPAWNER) {
                CompoundTag tag = new CompoundTag();
                CompoundTag spawnData = new CompoundTag();
                ResourceLocation spawnerMobId = BuiltInRegistries.ENTITY_TYPE.getKey(getRandomMobForMobSpawner(random));
                if (spawnerMobId != null) {
                    CompoundTag entity = new CompoundTag();
                    entity.putString("id", spawnerMobId.toString());
                    spawnData.put("entity", entity);
                    tag.remove("SpawnPotentials");
                    tag.put("SpawnData", spawnData.copy());
                }
                StructureTemplate.StructureBlockInfo newInfo = new StructureTemplate.StructureBlockInfo(infoIn2.pos(), IafBlockRegistry.DREAD_SPAWNER.defaultBlockState(), tag);
                return newInfo;

            }
            return infoIn2;
        }
        return infoIn2;
    }


    private EntityType getRandomMobForMobSpawner(RandomSource random) {
        float rand = random.nextFloat();
        if (rand < 0.3D) {
            return IafEntityRegistry.DREAD_THRALL;
        } else if (rand < 0.5D) {
            return IafEntityRegistry.DREAD_GHOUL;
        } else if (rand < 0.7D) {
            return IafEntityRegistry.DREAD_BEAST;
        } else if (rand < 0.85D) {
            return IafEntityRegistry.DREAD_SCUTTLER;
        }
        return IafEntityRegistry.DREAD_KNIGHT;
    }

    @Override
    protected StructureProcessorType<?> getType() {
        return IafProcessors.DREADRUINPROCESSOR;
    }
}

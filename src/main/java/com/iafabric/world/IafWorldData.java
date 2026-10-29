package com.iafabric.world;

import net.minecraft.core.HolderLookup;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.storage.DimensionDataStorage;
import com.iafabric.compat.SavedDataType;
import com.iafabric.IafConfig;
import com.iafabric.IafMod;
import com.iafabric.world.gen.TypedFeature;
import com.mojang.datafixers.util.Pair;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class IafWorldData extends SavedData {
    public enum FeatureType {
        SURFACE,
        UNDERGROUND,
        OCEAN
    }

    private static final String IDENTIFIER = "general";
    public static final SavedDataType<IafWorldData> TYPE = new SavedDataType<>(IafMod.stored(IDENTIFIER), IafWorldData::new,
        CompoundTag.CODEC.xmap(IafWorldData::new, data -> data.save(new CompoundTag())), DataFixTypes.SAVED_DATA_COMMAND_STORAGE);
    private static final Map<FeatureType, List<Pair<String, BlockPos>>> LAST_GENERATED = new HashMap<>();

    public IafWorldData() { /* Nothing to do */ }

    public IafWorldData(final CompoundTag tag) {
        this.load(tag);
    }

    public static IafWorldData get(final Level world) {
        if (world instanceof ServerLevel) {
            ServerLevel overworld = world.getServer().getLevel(world.dimension());
            DimensionDataStorage storage = overworld.getDataStorage();
            IafWorldData data = storage.computeIfAbsent(TYPE.factory(), TYPE.name());
            data.setDirty();

            return data;
        }

        return null;
    }

    /** The last place each big feature of a kind was made (for tests). */
    public static synchronized List<Pair<String, BlockPos>> lastGenerated(final FeatureType type) {
        return new ArrayList<>(LAST_GENERATED.getOrDefault(type, List.of()));
    }

    public boolean check(final TypedFeature feature, final BlockPos position, final String id) {
        return check(feature.getFeatureType(), position, id);
    }

    // Called by features on the world generation threads: one at a time.
    public boolean check(final FeatureType type, final BlockPos position, final String id) {
        synchronized (IafWorldData.class) {
            return checkLocked(type, position, id);
        }
    }

    private boolean checkLocked(final FeatureType type, final BlockPos position, final String id) {
        List<Pair<String, BlockPos>> entries = LAST_GENERATED.computeIfAbsent(type, key -> new ArrayList<>());

        boolean canGenerate = true;
        Pair<String, BlockPos> toRemove = null;

        for (Pair<String, BlockPos> entry : entries) {
            if (entry.getFirst().equals(id)) {
                toRemove = entry;
            }

            canGenerate = position.distSqr(entry.getSecond()) > IafConfig.dangerousWorldGenSeparationLimit * IafConfig.dangerousWorldGenSeparationLimit;
        }

        if (toRemove != null) {
            entries.remove(toRemove);
        }

        entries.add(Pair.of(id, position));

        return canGenerate;
    }

    public IafWorldData load(final CompoundTag tag) {
        FeatureType[] types = FeatureType.values();

        for (FeatureType type : types) {
            ListTag list = tag.iceandfire_fabric$getListOrEmpty(type.toString());

            for (int i = 0; i < list.size(); i++) {
                CompoundTag entry = list.getCompound(i);
                String id = entry.iceandfire_fabric$getStringOr("id", "");
                BlockPos position = entry.iceandfire_fabric$read("position", BlockPos.CODEC).orElse(BlockPos.ZERO);
                LAST_GENERATED.computeIfAbsent(type, key -> new ArrayList<>()).add(Pair.of(id, position));
            }
        }

        return this;
    }

        public CompoundTag save(@NotNull final CompoundTag tag) {
        synchronized (IafWorldData.class) {
            saveLocked(tag);
        }
        return tag;
    }

    private void saveLocked(final CompoundTag tag) {
        LAST_GENERATED.forEach((key, value) -> {
            ListTag listTag = new ListTag();

            value.forEach(entry -> {
                CompoundTag subTag = new CompoundTag();
                subTag.putString("id", entry.getFirst());
                subTag.iceandfire_fabric$store("position", BlockPos.CODEC, entry.getSecond());

                listTag.add(subTag);
            });

            tag.put(key.toString(), listTag);
        });

    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        return this.save(tag);
    }
}

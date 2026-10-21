package com.iafabric.world;

import com.iafabric.IafMod;
import com.iafabric.util.EntityNbt;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLevelEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.NoiseColumn;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;
import net.minecraft.world.level.storage.loot.LootTable;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The mod's big features (dragon caves and roosts, hydra and cyclops caves, myrmex hives, siren islands, pixie
 * villages) are wider than what 26.x lets a feature touch: only its chunk and the ones around it. In 1.18 they
 * reached farther. While one of them generates, what falls outside that area waits here (blocks, chest loot,
 * creatures) and is placed when its chunk is loaded; what it reads out there is the land's base shape (stone,
 * water or air from the terrain noise, and its height). The waiting part is saved with the world.
 */
public final class PendingWorldgen extends SavedData {
	private static final String IDENTIFIER = "iafabric_pending_worldgen";
	public static final SavedDataType<PendingWorldgen> TYPE = new SavedDataType<>(IafMod.stored(IDENTIFIER), PendingWorldgen::new,
		CompoundTag.CODEC.xmap(PendingWorldgen::new, data -> data.save(new CompoundTag())), DataFixTypes.SAVED_DATA_COMMAND_STORAGE);

	/** What waits, by level (its saved data's map, cached when the level loads) and chunk. */
	private static final Map<ServerLevel, PendingWorldgen> WAITING = new ConcurrentHashMap<>();
	/** Terrain columns read during the current feature (per generation thread). */
	private static final ThreadLocal<Map<Long, NoiseColumn>> COLUMNS = ThreadLocal.withInitial(HashMap::new);

	private final Map<Long, Chunk> chunks = new ConcurrentHashMap<>();
	private volatile boolean changed;

	private static final class Chunk {
		final Map<Long, BlockState> blocks = new ConcurrentHashMap<>();
		final Map<Long, Loot> loot = new ConcurrentHashMap<>();
		final List<CompoundTag> entities = java.util.Collections.synchronizedList(new ArrayList<>());
		/** Containers made for a feature to fill (its loot is kept when the chunk gets the block). */
		final Map<Long, BlockEntity> made = new ConcurrentHashMap<>();
	}

	private record Loot(ResourceKey<LootTable> table, long seed) {
	}

	private PendingWorldgen() {
	}

	private PendingWorldgen(CompoundTag tag) {
		for (Tag t : tag.getListOrEmpty("chunks")) {
			if (!(t instanceof CompoundTag c)) {
				continue;
			}
			Chunk chunk = new Chunk();
			for (Tag b : c.getListOrEmpty("blocks")) {
				if (b instanceof CompoundTag block) {
					chunk.blocks.put(block.getLongOr("pos", 0), NbtUtils.readBlockState(BuiltInRegistries.BLOCK, block.getCompoundOrEmpty("state")));
				}
			}
			for (Tag l : c.getListOrEmpty("loot")) {
				if (l instanceof CompoundTag loot) {
					chunk.loot.put(loot.getLongOr("pos", 0), new Loot(ResourceKey.create(Registries.LOOT_TABLE,
						Identifier.parse(loot.getStringOr("table", "minecraft:empty"))), loot.getLongOr("seed", 0)));
				}
			}
			for (Tag e : c.getListOrEmpty("entities")) {
				if (e instanceof CompoundTag entity) {
					chunk.entities.add(entity);
				}
			}
			chunks.put(c.getLongOr("chunk", 0), chunk);
		}
	}

	private CompoundTag save(CompoundTag tag) {
		ListTag list = new ListTag();
		for (Map.Entry<Long, Chunk> entry : this.chunks.entrySet()) {
			CompoundTag c = new CompoundTag();
			c.putLong("chunk", entry.getKey());
			ListTag blocks = new ListTag();
			entry.getValue().blocks.forEach((pos, state) -> {
				CompoundTag block = new CompoundTag();
				block.putLong("pos", pos);
				block.put("state", NbtUtils.writeBlockState(state));
				blocks.add(block);
			});
			c.put("blocks", blocks);
			ListTag loot = new ListTag();
			entry.getValue().loot.forEach((pos, l) -> {
				CompoundTag t = new CompoundTag();
				t.putLong("pos", pos);
				t.putString("table", l.table().identifier().toString());
				t.putLong("seed", l.seed());
				loot.add(t);
			});
			c.put("loot", loot);
			ListTag entities = new ListTag();
			synchronized (entry.getValue().entities) {
				entities.addAll(entry.getValue().entities);
			}
			c.put("entities", entities);
			list.add(c);
		}
		tag.put("chunks", list);
		return tag;
	}

	private static Map<Long, Chunk> waiting(ServerLevel level) {
		PendingWorldgen data = WAITING.get(level);
		return data == null ? new HashMap<>() : data.chunks;
	}

	private static void dirty(ServerLevel level) {
		PendingWorldgen data = WAITING.get(level);
		if (data != null) {
			data.changed = true;
		}
	}

	public static void init() {
		ServerLevelEvents.LOAD.register((server, level) -> WAITING.put(level, level.getDataStorage().computeIfAbsent(TYPE)));
		ServerLevelEvents.UNLOAD.register((server, level) -> WAITING.remove(level));
		ServerTickEvents.END_LEVEL_TICK.register(level -> {
			if (level.getGameTime() % 10 == 0) {
				placeLoaded(level);
				PendingWorldgen data = WAITING.get(level);
				if (data != null && data.changed) {
					data.changed = false;
					data.setDirty();
				}
			}
		});
	}

	/** A new feature starts: the terrain read for the last one is forgotten. */
	public static void featureStarts() {
		COLUMNS.get().clear();
	}

	// --- while a feature generates (world generation thread) ---

	public static void setBlock(ServerLevel level, BlockPos pos, BlockState state) {
		if (!WAITING.containsKey(level)) {
			return;
		}
		Chunk chunk = waiting(level).computeIfAbsent(ChunkPos.pack(pos), k -> new Chunk());
		chunk.blocks.put(pos.asLong(), state);
		chunk.made.remove(pos.asLong());
		dirty(level);
	}

	public static BlockState getBlockState(ServerLevel level, LevelHeightAccessor heights, BlockPos pos) {
		Chunk chunk = waiting(level).get(ChunkPos.pack(pos));
		if (chunk != null) {
			BlockState state = chunk.blocks.get(pos.asLong());
			if (state != null) {
				return state;
			}
		}
		if (pos.getY() < heights.getMinY() || pos.getY() >= heights.getMinY() + heights.getHeight()) {
			return net.minecraft.world.level.block.Blocks.AIR.defaultBlockState();
		}
		ChunkGenerator generator = level.getChunkSource().getGenerator();
		RandomState random = level.getChunkSource().randomState();
		NoiseColumn column = COLUMNS.get().computeIfAbsent(ChunkPos.pack(pos.getX(), pos.getZ()),
			k -> generator.getBaseColumn(pos.getX(), pos.getZ(), heights, random));
		return column.getBlock(pos.getY());
	}

	public static int getHeight(ServerLevel level, LevelHeightAccessor heights, Heightmap.Types type, int x, int z) {
		return level.getChunkSource().getGenerator().getBaseHeight(x, z, type, heights, level.getChunkSource().randomState());
	}

	/** A container a feature wants to fill out there: made from the waiting block, its loot kept for later. */
	public static @Nullable BlockEntity getBlockEntity(ServerLevel level, LevelHeightAccessor heights, BlockPos pos) {
		Chunk chunk = waiting(level).get(ChunkPos.pack(pos));
		if (chunk == null) {
			return null;
		}
		BlockState state = chunk.blocks.get(pos.asLong());
		if (state == null || !(state.getBlock() instanceof EntityBlock entityBlock)) {
			return null;
		}
		return chunk.made.computeIfAbsent(pos.asLong(), k -> {
			BlockEntity made = entityBlock.newBlockEntity(pos, state);
			return made;
		});
	}

	public static void addEntity(ServerLevel level, Entity entity) {
		CompoundTag tag = EntityNbt.save(entity);
		tag.putString("id", EntityType.getKey(entity.getType()).toString());
		if (!WAITING.containsKey(level)) {
			return;
		}
		waiting(level).computeIfAbsent(ChunkPos.pack(entity.blockPosition()), k -> new Chunk()).entities.add(tag);
		dirty(level);
	}

	/** The feature is done: the loot put in the containers made out there is kept. */
	public static void featureEnds(ServerLevel level) {
		for (Chunk chunk : waiting(level).values()) {
			for (Iterator<Map.Entry<Long, BlockEntity>> it = chunk.made.entrySet().iterator(); it.hasNext(); ) {
				Map.Entry<Long, BlockEntity> entry = it.next();
				if (entry.getValue() instanceof RandomizableContainerBlockEntity container && container.getLootTable() != null) {
					chunk.loot.put(entry.getKey(), new Loot(container.getLootTable(), container.getLootTableSeed()));
				}
				it.remove();
			}
		}
		COLUMNS.get().clear();
	}

	// --- on the server thread ---

	/** What waits for a loaded chunk is placed in it. */
	private static void placeLoaded(ServerLevel level) {
		Map<Long, Chunk> chunks = waiting(level);
		if (chunks.isEmpty()) {
			return;
		}
		boolean placed = false;
		for (Iterator<Map.Entry<Long, Chunk>> it = chunks.entrySet().iterator(); it.hasNext(); ) {
			Map.Entry<Long, Chunk> entry = it.next();
			int cx = ChunkPos.getX(entry.getKey());
			int cz = ChunkPos.getZ(entry.getKey());
			if (!level.hasChunk(cx, cz)) {
				continue;
			}
			Chunk chunk = entry.getValue();
			chunk.blocks.forEach((pos, state) -> level.setBlock(BlockPos.of(pos), state, Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE));
			chunk.loot.forEach((pos, loot) -> {
				if (level.getBlockEntity(BlockPos.of(pos)) instanceof RandomizableContainerBlockEntity container) {
					container.setLootTable(loot.table(), loot.seed());
				}
			});
			synchronized (chunk.entities) {
				for (CompoundTag tag : chunk.entities) {
					EntityNbt.type(tag.getStringOr("id", "")).ifPresent(type -> {
						Entity entity = type.create(level, EntitySpawnReason.STRUCTURE);
						if (entity != null) {
							EntityNbt.load(entity, tag);
							level.addFreshEntity(entity);
						}
					});
				}
			}
			it.remove();
			placed = true;
		}
		if (placed) {
			dirty(level);
		}
	}

	/** For tests: how many chunks are still waiting for blocks. */
	public static int waitingChunks(ServerLevel level) {
		return waiting(level).size();
	}
}

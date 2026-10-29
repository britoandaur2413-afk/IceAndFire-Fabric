package com.iafabric.world;

import com.iafabric.IafMod;
import com.iafabric.entity.EntityCyclops;
import com.iafabric.entity.EntityDragonBase;
import com.iafabric.entity.IafEntityRegistry;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerWorldEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.QuartPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BiomeTags;
import net.minecraft.util.RandomSource;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Sheep;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.saveddata.SavedData;
import com.iafabric.compat.SavedDataType;
import net.minecraft.world.phys.AABB;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Where the land put what an exiled pixie can lead to and that no map knows: dragon skeletons, cyclopes (in their
 * caves or wandering with their sheep) and pixie villages, noted as they generate. When none is near, she "remembers"
 * one: a place on dry land a little way off, where it is made when a player comes near (never loading far land).
 */
public final class FairyLandmarks extends SavedData {
	public enum Kind {
		/**
		 * A dragon skeleton, a cyclops (cave or flock), a pixie village, a dragons' den with a grown female by her eggs,
		 * a hydra's cave.
		 */
		SKELETON, CYCLOPS, PIXIES, NEST, HYDRA
	}

	private static final String IDENTIFIER = "iafabric_landmarks";
	public static final SavedDataType<FairyLandmarks> TYPE = new SavedDataType<>(IafMod.stored(IDENTIFIER), FairyLandmarks::new,
		CompoundTag.CODEC.xmap(FairyLandmarks::new, data -> data.save(new CompoundTag())), null);
	/** The most kept of each kind (the oldest go first). */
	private static final int MAX = 4096;
	private static final Map<ServerLevel, FairyLandmarks> LOADED = new ConcurrentHashMap<>();

	private final Map<Kind, List<Long>> places = new EnumMap<>(Kind.class);
	/** Places remembered and not made yet (made when a player comes near), by kind. */
	private final Map<Long, Kind> planned = new ConcurrentHashMap<>();
	/** Exiled pixies' rings to take away (true) or to quiet (false) when their land is loaded. */
	private final Map<Long, Boolean> rings = new ConcurrentHashMap<>();
	private volatile boolean changed;

	private FairyLandmarks() {
		for (Kind kind : Kind.values()) {
			places.put(kind, Collections.synchronizedList(new ArrayList<>()));
		}
	}

	private FairyLandmarks(CompoundTag tag) {
		this();
		for (Kind kind : Kind.values()) {
			for (long pos : tag.getLongArray(kind.name())) {
				places.get(kind).add(pos);
			}
			for (long pos : tag.getLongArray("planned_" + kind.name())) {
				planned.put(pos, kind);
			}
		}
		for (long pos : tag.getLongArray("rings_remove")) {
			rings.put(pos, true);
		}
		for (long pos : tag.getLongArray("rings_quiet")) {
			rings.put(pos, false);
		}
	}

	@Override
	public CompoundTag save(CompoundTag tag) {
		for (Kind kind : Kind.values()) {
			List<Long> list = places.get(kind);
			synchronized (list) {
				tag.putLongArray(kind.name(), list.stream().mapToLong(Long::longValue).toArray());
			}
			tag.putLongArray("planned_" + kind.name(), planned.entrySet().stream().filter(e -> e.getValue() == kind).mapToLong(Map.Entry::getKey).toArray());
		}
		tag.putLongArray("rings_remove", rings.entrySet().stream().filter(Map.Entry::getValue).mapToLong(Map.Entry::getKey).toArray());
		tag.putLongArray("rings_quiet", rings.entrySet().stream().filter(e -> !e.getValue()).mapToLong(Map.Entry::getKey).toArray());
		return tag;
	}

	public static void init() {
		ServerWorldEvents.LOAD.register((server, level) -> LOADED.put(level, level.getDataStorage().computeIfAbsent(TYPE.load(), TYPE.constructor(), TYPE.name())));
		ServerWorldEvents.UNLOAD.register((server, level) -> LOADED.remove(level));
		ServerTickEvents.END_WORLD_TICK.register(level -> {
			if (level.getGameTime() % 20 == 7) {
				FairyLandmarks data = LOADED.get(level);
				if (data != null) {
					data.makeNear(level);
					if (data.changed) {
						data.changed = false;
						data.setDirty();
					}
				}
			}
		});
	}

	private static @Nullable FairyLandmarks of(ServerLevel level) {
		return LOADED.get(level);
	}

	/** A place known now (made by other means than the land's generation: a test, a command). */
	public static void note(ServerLevel level, Kind kind, BlockPos pos) {
		FairyLandmarks data = of(level);
		if (data != null) {
			data.add(kind, pos.asLong());
		}
	}

	/** A place generated (on a world generation thread). */
	public static void note(WorldGenLevel level, Kind kind, BlockPos pos) {
		FairyLandmarks data = of(level.getLevel());
		if (data != null) {
			data.add(kind, pos.asLong());
		}
	}

	private void add(Kind kind, long pos) {
		List<Long> list = places.get(kind);
		synchronized (list) {
			list.add(pos);
			while (list.size() > MAX) {
				list.remove(0);
			}
		}
		changed = true;
	}

	/** The nearest place of that kind within a distance (made or remembered), or null. */
	public static @Nullable BlockPos nearest(ServerLevel level, Kind kind, BlockPos from, int within) {
		FairyLandmarks data = of(level);
		if (data == null) {
			return null;
		}
		long best = 0;
		double bestDistance = (double) within * within;
		boolean found = false;
		List<Long> list = data.places.get(kind);
		synchronized (list) {
			for (long packed : list) {
				double d = horizontal(BlockPos.of(packed), from);
				if (d < bestDistance) {
					bestDistance = d;
					best = packed;
					found = true;
				}
			}
		}
		return found ? BlockPos.of(best) : null;
	}

	private static double horizontal(BlockPos a, BlockPos b) {
		double dx = a.getX() - b.getX();
		double dz = a.getZ() - b.getZ();
		return dx * dx + dz * dz;
	}

	/** A place found gone (the skeleton taken, the cyclops dead): forgotten. */
	public static void forget(ServerLevel level, Kind kind, BlockPos pos) {
		FairyLandmarks data = of(level);
		if (data != null) {
			data.places.get(kind).remove(pos.asLong());
			data.planned.remove(pos.asLong());
			data.changed = true;
		}
	}

	/** Whether the place is remembered and not made yet (nothing to look for there until a player comes near). */
	public static boolean waiting(ServerLevel level, BlockPos pos) {
		FairyLandmarks data = of(level);
		return data != null && data.planned.containsKey(pos.asLong());
	}

	/**
	 * A place she remembers: dry land between 120 and 260 blocks off (land not loaded is read from the world's
	 * shape, never generated), made when a player comes near. Null in a world without such land.
	 */
	public static @Nullable BlockPos remember(ServerLevel level, Kind kind, BlockPos from, RandomSource random) {
		FairyLandmarks data = of(level);
		if (data == null) {
			return null;
		}
		for (int attempt = 0; attempt < 48; attempt++) {
			double angle = random.nextDouble() * Math.PI * 2;
			double distance = 120 + random.nextInt(140);
			int x = from.getX() + (int) (Math.cos(angle) * distance);
			int z = from.getZ() + (int) (Math.sin(angle) * distance);
			Holder<Biome> biome = level.getUncachedNoiseBiome(QuartPos.fromBlock(x), QuartPos.fromBlock(com.iafabric.compat.Levels.seaLevel(level) + 4), QuartPos.fromBlock(z));
			if (biome.is(BiomeTags.IS_OCEAN) || biome.is(BiomeTags.IS_DEEP_OCEAN) || biome.is(BiomeTags.IS_RIVER) || biome.is(BiomeTags.IS_BEACH)
				|| attempt < 24 && biome.is(BiomeTags.IS_MOUNTAIN)) {
				continue;
			}
			BlockPos pos = new BlockPos(x, com.iafabric.compat.Levels.seaLevel(level), z);
			data.planned.put(pos.asLong(), kind);
			data.add(kind, pos.asLong());
			return pos;
		}
		return null;
	}

	/** An exiled pixie's ring to take away (or to quiet) when its land is loaded. */
	public static void later(ServerLevel level, BlockPos lantern, boolean remove) {
		FairyLandmarks data = of(level);
		if (data != null) {
			data.rings.put(lantern.asLong(), remove);
			data.changed = true;
		}
	}

	/** The places remembered with a player near are made (their land loaded by then); rings waiting are taken away or quieted. */
	private void makeNear(ServerLevel level) {
		if (!rings.isEmpty()) {
			for (Map.Entry<Long, Boolean> ring : new ArrayList<>(rings.entrySet())) {
				BlockPos at = BlockPos.of(ring.getKey());
				if (level.hasChunk(at.getX() >> 4, at.getZ() >> 4)) {
					if (ring.getValue()) {
						com.iafabric.pixie.FairyCheckpoints.remove(level, at);
					} else {
						com.iafabric.pixie.FairyCheckpoints.light(level, at, false);
					}
					rings.remove(ring.getKey());
					changed = true;
				}
			}
		}
		if (planned.isEmpty()) {
			return;
		}
		for (Map.Entry<Long, Kind> entry : new ArrayList<>(planned.entrySet())) {
			BlockPos at = BlockPos.of(entry.getKey());
			if (!level.hasChunk(at.getX() >> 4, at.getZ() >> 4)) {
				continue;
			}
			boolean near = false;
			for (ServerPlayer player : level.players()) {
				if (horizontal(player.blockPosition(), at) < 96 * 96) {
					near = true;
					break;
				}
			}
			if (!near) {
				continue;
			}
			planned.remove(entry.getKey());
			places.get(entry.getValue()).remove(entry.getKey());
			BlockPos ground = dryGround(level, at);
			if (ground != null) {
				make(level, entry.getValue(), ground);
				add(entry.getValue(), ground.asLong());
				IafMod.LOGGER.info("An exiled pixie's {} made at {}", entry.getValue(), ground.toShortString());
			}
			changed = true;
		}
	}

	/** The top of dry ground at a place or near it (within 20 blocks, in land already loaded). */
	private static @Nullable BlockPos dryGround(ServerLevel level, BlockPos at) {
		for (int r = 0; r <= 20; r += 2) {
			for (int dx = -r; dx <= r; dx += 2) {
				for (int dz = -r; dz <= r; dz += 2) {
					if (Math.max(Math.abs(dx), Math.abs(dz)) != r) {
						continue;
					}
					int x = at.getX() + dx;
					int z = at.getZ() + dz;
					if (!level.hasChunk(x >> 4, z >> 4)) {
						continue;
					}
					int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
					BlockPos top = new BlockPos(x, y, z);
					if (level.getBlockState(top.below()).getFluidState().isEmpty() && level.getBlockState(top).getFluidState().isEmpty()
						&& !level.getBlockState(top.below()).isAir()) {
						return top;
					}
				}
			}
		}
		return null;
	}

	private static void make(ServerLevel level, Kind kind, BlockPos at) {
		RandomSource random = level.getRandom();
		switch (kind) {
			case SKELETON -> {
				float temperature = level.getBiome(at).value().getBaseTemperature();
				EntityType<? extends EntityDragonBase> type = temperature < 0.2F ? IafEntityRegistry.ICE_DRAGON
					: temperature > 1.4F ? IafEntityRegistry.FIRE_DRAGON : IafEntityRegistry.LIGHTNING_DRAGON;
				EntityDragonBase dragon = type.create(level);
				if (dragon != null) {
					// old enough to give a few bones before only its skull is left
					int age = 80 + random.nextInt(30);
					dragon.setPos(at.getX() + 0.5, at.getY(), at.getZ() + 0.5);
					dragon.growDragon(age);
					dragon.modelDeadProgress = 20;
					dragon.setModelDead(true);
					dragon.setDeathStage((age / 5) / 2);
					dragon.setYRot(random.nextInt(360));
					level.addFreshEntity(dragon);
				}
			}
			case CYCLOPS -> {
				EntityCyclops cyclops = IafEntityRegistry.CYCLOPS.create(level);
				if (cyclops != null) {
					cyclops.setPos(at.getX() + 0.5, at.getY(), at.getZ() + 0.5);
					cyclops.finalizeSpawn(level, level.getCurrentDifficultyAt(at), MobSpawnType.SPAWNER, null, null);
					cyclops.setPersistenceRequired();
					level.addFreshEntity(cyclops);
				}
				for (int i = 0; i < 4 + random.nextInt(3); i++) {
					Sheep sheep = EntityType.SHEEP.create(level);
					if (sheep != null) {
						sheep.setPos(at.getX() + 0.5 + random.nextInt(7) - 3, at.getY(), at.getZ() + 0.5 + random.nextInt(7) - 3);
						sheep.setColor(Sheep.getRandomSheepColor(sheep.getRandom()));
						sheep.setPersistenceRequired();
						level.addFreshEntity(sheep);
					}
				}
			}
			case PIXIES, NEST, HYDRA -> {
			}
		}
	}

	/**
	 * Whether what was at a place is still there (a skeleton not taken, a cyclops alive, pixies about); its land must
	 * be loaded with a player near (else it says yes: nothing can be told).
	 */
	public static boolean stillThere(ServerLevel level, Kind kind, BlockPos at) {
		if (waiting(level, at) || !level.hasChunk(at.getX() >> 4, at.getZ() >> 4)) {
			return true;
		}
		AABB box = new AABB(at).inflate(32, 48, 32);
		return switch (kind) {
			case SKELETON -> !level.getEntitiesOfClass(EntityDragonBase.class, box, d -> d.isModelDead() && d.isAlive()).isEmpty();
			case CYCLOPS -> !level.getEntitiesOfClass(EntityCyclops.class, box.inflate(16, 0, 16), c -> c.isAlive()).isEmpty();
			case PIXIES -> !level.getEntitiesOfClass(com.iafabric.entity.EntityPixie.class, box,
				p -> p.isAlive() && !(p instanceof com.iafabric.entity.EntityExiledPixie) && !p.isTame()).isEmpty();
			// a den is a nest while its grown female or one of her eggs is there
			case HYDRA -> !level.getEntitiesOfClass(com.iafabric.entity.EntityHydra.class, box.inflate(8, 0, 8), h -> h.isAlive()).isEmpty();
			case NEST -> !level.getEntitiesOfClass(EntityDragonBase.class, box.inflate(8, 0, 8), d -> d.isAlive() && !d.isModelDead() && d.laysEggs() && !d.isTame()).isEmpty()
				|| !level.getEntitiesOfClass(com.iafabric.entity.EntityDragonEgg.class, box.inflate(8, 0, 8), e -> e.isAlive()).isEmpty();
		};
	}

}

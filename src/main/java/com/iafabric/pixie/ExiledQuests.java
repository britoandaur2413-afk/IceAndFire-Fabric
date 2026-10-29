package com.iafabric.pixie;

import com.iafabric.IafMod;
import com.iafabric.entity.EntityCyclops;
import com.iafabric.entity.EntityDragonBase;
import com.iafabric.entity.EntityExiledPixie;
import com.iafabric.entity.EntityHippogryph;
import com.iafabric.entity.EntityHydra;
import com.iafabric.entity.EntityTroll;
import com.iafabric.entity.IafEntityRegistry;
import com.iafabric.entity.EntityPixie;
import com.iafabric.item.IafItemRegistry;
import com.iafabric.item.ItemDragonEgg;
import com.iafabric.message.MessageExiledSpeech;
import com.iafabric.world.FairyLandmarks;
import com.iafabric.world.structure.PixieCavernPiece;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.tags.StructureTags;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.horse.AbstractHorse;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.entity.BellBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Predicate;

/**
 * The exiled pixie's quests: which one she offers (the first whose earlier ones are over, never one done or passed,
 * never often: once answered "later", not for ten minutes), what counts as done, which ones the player already went
 * past (a better weapon than dragon bones make, a pixie of their own already...), and the way to each one's place:
 * she remembers where it is (the nearest village, the witches' cottage, a ruin, a nest, a skeleton, a cyclops, her
 * friends' sanctuary), flies ahead of her player toward it and makes her ring there; on the screen, her checkpoint.
 */
public final class ExiledQuests {
	/** Where each quest's place is looked for, how far (blocks) and how near counts as there. */
	private static final Map<ExiledQuest, List<String>> STRUCTURES = Map.of(
		ExiledQuest.CAKE, List.of("witch_cottage"),
		ExiledQuest.FRIENDS, List.of("pixie_sanctuary", "pixie_cavern"),
		ExiledQuest.GRAVEYARD, List.of("graveyard"),
		ExiledQuest.MAUSOLEUM, List.of("mausoleum"),
		ExiledQuest.TEMPLE, List.of("gorgon_temple"),
		ExiledQuest.EGG, List.of("dragon_cave"),
		ExiledQuest.HYDRA, List.of("hydra_cave"));
	private static final int BONES_NEEDED = 4;
	/** A bit of a quest's progress: its place is the pixies' cave (under the land: her ring at its plaza's own height). */
	private static final int CAVE = 2;
	/** A bit of a quest's progress: she said where its creatures live (none was near). */
	private static final int HINTED = 4;
	/** A bit of what she said once: all her memories are back (the player is ready for Cryona). */
	private static final long READY_SAID = 1L << 30;

	/** Each player's search for the place of their quest. */
	private static final Map<UUID, Search> SEARCHES = new HashMap<>();
	/** The hidden chest she shows each player, until when. */
	private static final Map<UUID, Hint> HINTS = new HashMap<>();
	/** When each player's quest's place was last looked for without luck (looked for again later, or farther on). */
	private static final Map<UUID, Long> LOOKED = new HashMap<>();
	/** How many times in a row the quest's place was found empty (three: it is gone). */
	private static final Map<UUID, Integer> MISSING = new HashMap<>();
	/** The quest each player last put off (the next offer is another one when there is one). */
	private static final Map<UUID, ExiledQuest> PUT_OFF = new HashMap<>();
	/** When her ring may be tried again where there was no ground for it (and that being near is being there). */
	private static final Map<UUID, Long> NO_RING = new HashMap<>();
	/** Places found empty (a den with no grown female), not to be led to again, by player. */
	private static final Map<UUID, List<BlockPos>> EMPTY = new HashMap<>();
	/** Who looks for a hydra itself (she remembers no hydra's cave near). */
	private static final java.util.Set<UUID> HUNTS = new java.util.HashSet<>();

	private record Search(ExiledQuest quest, ResourceKey<net.minecraft.world.level.Level> level, FairyLocator locator) {
	}

	private record Hint(BlockPos pos, long until, String level) {
	}

	private ExiledQuests() {
	}

	static void forget(UUID id) {
		SEARCHES.remove(id);
		HINTS.remove(id);
		LOOKED.remove(id);
		MISSING.remove(id);
		PUT_OFF.remove(id);
		NO_RING.remove(id);
	}

	// --- what counts ---

	private static int count(ServerPlayer player, Predicate<ItemStack> what) {
		int n = 0;
		for (ItemStack stack : com.iafabric.compat.Inv.all(player.getInventory())) {
			if (!stack.isEmpty() && what.test(stack)) {
				n += stack.getCount();
			}
		}
		return n;
	}

	private static int count(ServerPlayer player, Item item) {
		return count(player, stack -> stack.is(item));
	}

	private static boolean hasBondedPixie(ServerPlayer player) {
		for (PixieBonds.Bond bond : PixieBonding.get(player).bonds) {
			if (bond.bonded) {
				return true;
			}
		}
		return false;
	}

	private static boolean hasTamedPixie(ServerPlayer player) {
		return hasBondedPixie(player) || !player.level().getEntitiesOfClass(EntityPixie.class, player.getBoundingBox().inflate(64),
			p -> !(p instanceof EntityExiledPixie) && p.isTame() && p.isOwnedBy(player)).isEmpty();
	}

	/** A weapon past what dragon bones make (diamond and better, or dragon bone or dragonsteel already), or such armor worn. */
	public static boolean pastBones(ServerPlayer player) {
		boolean weapon = count(player, stack -> stack.is(Items.DIAMOND_SWORD) || stack.is(Items.NETHERITE_SWORD) || stack.is(Items.DIAMOND_AXE)
			|| stack.is(Items.NETHERITE_AXE) || stack.is(Items.MACE) || dragonGear(stack)) > 0;
		int armor = 0;
		for (EquipmentSlot slot : new EquipmentSlot[]{EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET}) {
			String path = BuiltInRegistries.ITEM.getKey(player.getItemBySlot(slot).getItem()).getPath();
			if (path.startsWith("diamond_") || path.startsWith("netherite_") || path.contains("dragonsteel") || path.startsWith("armor_")) {
				armor++;
			}
		}
		return weapon || armor >= 3;
	}

	private static boolean dragonGear(ItemStack stack) {
		String path = BuiltInRegistries.ITEM.getKey(stack.getItem()).getPath();
		return path.startsWith("dragonbone_") || path.startsWith("dragonsteel_");
	}

	private static boolean hasDragonEgg(ServerPlayer player) {
		return count(player, stack -> stack.getItem() instanceof ItemDragonEgg) > 0;
	}

	private static boolean hasOwnDragon(ServerPlayer player) {
		return !player.level().getEntitiesOfClass(EntityDragonBase.class, player.getBoundingBox().inflate(64),
			d -> d.isTame() && d.isOwnedBy(player) && !d.isModelDead()).isEmpty();
	}

	/** It is done: what it seeks is the player's (a place to see is done on arriving; Cryona when she is beaten). */
	static boolean satisfied(ServerPlayer player, ExiledQuest quest) {
		return switch (quest) {
			case CAKE -> count(player, Items.CAKE) > 0;
			case FRIENDS -> hasTamedPixie(player);
			case BONES -> count(player, IafItemRegistry.DRAGON_BONE) >= BONES_NEEDED;
			case CYCLOPS -> count(player, IafItemRegistry.CYCLOPS_EYE) > 0;
			case EGG -> hasDragonEgg(player);
			case HORSE -> stat(player, Stats.CUSTOM.get(Stats.HORSE_ONE_CM)) > 0 || player.getVehicle() instanceof AbstractHorse;
			case HIPPOGRYPH -> player.getVehicle() instanceof EntityHippogryph;
			case TROLL -> stat(player, Stats.ENTITY_KILLED.get(IafEntityRegistry.TROLL)) > 0;
			case HYDRA -> stat(player, Stats.ENTITY_KILLED.get(IafEntityRegistry.HYDRA)) > 0;
			case PIXIE5 -> pixieAtFive(player);
			default -> false;
		};
	}

	private static int stat(ServerPlayer player, net.minecraft.stats.Stat<?> stat) {
		return player.getStats().getValue(stat);
	}

	private static boolean pixieAtFive(ServerPlayer player) {
		for (PixieBonds.Bond bond : PixieBonding.get(player).bonds) {
			if (bond.bonded && bond.level >= 5) {
				return true;
			}
		}
		return false;
	}

	/** The player stands in a quest's place (a village, a graveyard, a mausoleum, the gorgon's temple): been there. */
	static boolean visiting(ServerLevel level, ServerPlayer player, ExiledQuest quest) {
		if (level.dimension() != net.minecraft.world.level.Level.OVERWORLD) {
			return false;
		}
		if (quest == ExiledQuest.VILLAGE) {
			return level.structureManager().getStructureWithPieceAt(player.blockPosition(), StructureTags.VILLAGE).isValid();
		}
		if (quest != ExiledQuest.GRAVEYARD && quest != ExiledQuest.MAUSOLEUM && quest != ExiledQuest.TEMPLE) {
			return false;
		}
		ResourceKey<Structure> key = ResourceKey.create(Registries.STRUCTURE, IafMod.id(STRUCTURES.get(quest).get(0)));
		return level.structureManager().getStructureWithPieceAt(player.blockPosition(), holder -> holder.is(key)).isValid();
	}

	/** The player already went past it: it is not offered (and stops if under way). */
	static boolean passed(ServerPlayer player, ExiledQuest quest) {
		return switch (quest) {
			case CAKE, FRIENDS -> hasBondedPixie(player);
			case BONES -> pastBones(player);
			case EGG -> hasOwnDragon(player);
			default -> false;
		};
	}

	// --- answers ---

	static void accept(ServerPlayer player, ExiledPixieData data, @Nullable ExiledQuest quest) {
		if (quest == null || !quest.offered() || data.stage != ExiledPixieData.FRIEND || data.isOver(quest) || !quest.unlocked(data)
			|| data.quest == quest.ordinal()) {
			return;
		}
		forgetPlace(player.getUUID());
		dropRing(player, data);
		data.quest = quest.ordinal();
		data.offered = -1;
		data.progress = 0;
		data.target = null;
		data.via = null;
		data.targetLevel = "";
		ExiledPixies.save(player, data);
		if (satisfied(player, quest)) {
			complete(player, data, quest, true);
			return;
		}
		ExiledPixies.say(player, Component.translatable("exiled.iceandfire_fabric.quest." + quest.key() + ".start"), MessageExiledSpeech.QUEST);
	}

	static void later(ServerPlayer player, ExiledPixieData data, @Nullable ExiledQuest quest) {
		if (quest == null) {
			return;
		}
		if (data.offered == quest.ordinal()) {
			data.offered = -1;
		}
		data.nextOffer = player.level().getGameTime() + 12000;
		PUT_OFF.put(player.getUUID(), quest);
		ExiledPixies.save(player, data);
		ExiledPixies.say(player, Component.translatable("exiled.iceandfire_fabric.quest.later"), MessageExiledSpeech.TALK);
	}

	static void drop(ServerPlayer player, ExiledPixieData data) {
		ExiledQuest quest = data.current();
		if (quest == null) {
			return;
		}
		dropRing(player, data);
		data.quest = -1;
		data.target = null;
		data.via = null;
		data.progress = 0;
		data.nextOffer = player.level().getGameTime() + 12000;
		PUT_OFF.put(player.getUUID(), quest);
		forgetPlace(player.getUUID());
		ExiledPixies.save(player, data);
		ExiledPixies.say(player, Component.translatable("exiled.iceandfire_fabric.quest.dropped"), MessageExiledSpeech.TALK);
	}

	/**
	 * For tests and game masters: every quest she offers done at once (their experience hers), none under way; her
	 * last one and her gift to come again when they find Cryona (the right moment, FairyVision), and her word that she
	 * remembers it all.
	 */
	static void completeAll(ServerPlayer player, ExiledPixieData data) {
		if (data.current() != null) {
			dropRing(player, data);
		}
		data.quest = -1;
		data.offered = -1;
		data.target = null;
		data.via = null;
		data.progress = 0;
		forget(player.getUUID());
		forgetPlace(player.getUUID());
		for (ExiledQuest quest : ExiledQuest.values()) {
			if (quest.offered() && !data.isDone(quest)) {
				data.done |= quest.bit();
				data.xp += quest.xp;
			}
		}
		data.done &= ~ExiledQuest.CRYONA.bit();
		data.gifts &= ~ExiledPixieData.GIFT_ICE;
		data.said &= ~READY_SAID;
		data.xp = Math.max(data.xp, ExiledPixieData.LEVEL_XP[ExiledPixieData.LEVEL_XP.length - 1]);
		data.level = 5;
		data.nextOffer = player.level().getGameTime() + 2400;
	}

	private static void forgetPlace(UUID id) {
		SEARCHES.remove(id);
		LOOKED.remove(id);
		MISSING.remove(id);
		NO_RING.remove(id);
		HUNTS.remove(id);
	}

	/** The world the quest's place is in (null if gone). */
	private static @Nullable ServerLevel placeLevel(ServerPlayer player, ExiledPixieData data) {
		net.minecraft.resources.ResourceLocation key = net.minecraft.resources.ResourceLocation.tryParse(data.targetLevel);
		return key == null ? null : player.level().getServer().getLevel(ResourceKey.create(Registries.DIMENSION, key));
	}

	/** Her ring goes (its quest given up, passed or its place gone): now if its land is loaded, else when it is. */
	private static void dropRing(ServerPlayer player, ExiledPixieData data) {
		ServerLevel level = placeLevel(player, data);
		if (data.checkpoint != null && level != null) {
			if (FairyCheckpoints.loaded(level, data.checkpoint)) {
				FairyCheckpoints.remove(level, data.checkpoint);
			} else {
				FairyLandmarks.later(level, data.checkpoint, true);
			}
		}
		data.checkpoint = null;
	}

	/** Her ring grows quiet (its place reached and its quest done): a grey crystal, candles out. */
	private static void quietRing(ServerPlayer player, ExiledPixieData data) {
		ServerLevel level = placeLevel(player, data);
		if (data.checkpoint != null && level != null) {
			if (FairyCheckpoints.loaded(level, data.checkpoint)) {
				FairyCheckpoints.light(level, data.checkpoint, false);
			} else {
				FairyLandmarks.later(level, data.checkpoint, false);
			}
		}
		data.checkpoint = null;
	}

	private static void complete(ServerPlayer player, ExiledPixieData data, ExiledQuest quest, boolean already) {
		boolean current = data.quest == quest.ordinal();
		if (current) {
			quietRing(player, data);
		}
		data.done |= quest.bit();
		if (data.offered == quest.ordinal()) {
			data.offered = -1;
		}
		if (current) {
			data.quest = -1;
			data.target = null;
			data.via = null;
			data.progress = 0;
			forgetPlace(player.getUUID());
		}
		data.nextOffer = player.level().getGameTime() + 2400;
		if (quest == ExiledQuest.CAKE) {
			// a cake taken: the witches' chest is no news any more
			HINTS.remove(player.getUUID());
			ExiledPixies.unshow(player);
		} else if (quest == ExiledQuest.VILLAGE && player.level() instanceof ServerLevel level) {
			// the village reached is one she need not warn of
			var village = level.structureManager().getStructureWithPieceAt(player.blockPosition(), StructureTags.VILLAGE);
			if (village.isValid()) {
				ExiledPixieData.remember(data.villages, village.getChunkPos().toLong(), 32);
			}
		}
		ExiledPixies.save(player, data);
		ExiledPixies.say(player, Component.translatable(already ? "exiled.iceandfire_fabric.quest.already" : "exiled.iceandfire_fabric.quest." + quest.key() + ".done",
			Component.keybind("key.iceandfire_fabric.pixie_bonds")), MessageExiledSpeech.QUEST);
		player.level().playSound(null, player.blockPosition(), SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, SoundSource.PLAYERS, 0.5F, 1.4F);
		ExiledPixies.addXp(player, data, quest.xp);
		IafMod.LOGGER.info("{}'s exiled pixie: quest {} done", player.getName().getString(), quest.key());
	}

	// --- the beat ---

	static void tick(ServerPlayer player, ExiledPixieData data, @Nullable EntityExiledPixie pixie, long now) {
		// what the player already went past is not offered again (if it was under way, she says so)
		for (ExiledQuest quest : ExiledQuest.values()) {
			if (!data.isOver(quest) && passed(player, quest)) {
				boolean asked = data.quest == quest.ordinal() || data.offered == quest.ordinal();
				data.skipped |= quest.bit();
				if (data.quest == quest.ordinal()) {
					dropRing(player, data);
					data.quest = -1;
					forgetPlace(player.getUUID());
				}
				if (data.offered == quest.ordinal()) {
					data.offered = -1;
				}
				ExiledPixies.save(player, data);
				if (asked) {
					ExiledPixies.say(player, Component.translatable("exiled.iceandfire_fabric.quest." + quest.key() + ".passed"), MessageExiledSpeech.QUEST);
				}
			}
		}
		if (data.relook) {
			data.relook = false;
			dropRing(player, data);
			data.target = null;
			data.via = null;
			data.progress = 0;
			forgetPlace(player.getUUID());
			ExiledPixies.save(player, data);
		}
		ExiledQuest offer = data.offer();
		if (offer != null && !offer.unlocked(data)) {
			// offered before the quests came in this order (the cake before the village)
			data.offered = -1;
			ExiledPixies.save(player, data);
		}
		ServerLevel level = (ServerLevel) player.level();
		ownWay(player, data, level);
		if (ExiledQuest.ready(data) && !data.hasGift(ExiledPixieData.GIFT_ICE) && (data.said & READY_SAID) == 0) {
			// all her memories back: the right moment may come
			data.said |= READY_SAID;
			ExiledPixies.save(player, data);
			ExiledPixies.sayLater(player, 80, Component.translatable("exiled.iceandfire_fabric.quest.ready"), MessageExiledSpeech.QUEST);
		}
		ExiledQuest quest = data.current();
		if (quest == null) {
			offer(player, data, now);
			return;
		}
		if (satisfied(player, quest) || visiting(level, player, quest)) {
			complete(player, data, quest, false);
			return;
		}
		place(player, data, quest, level, now);
		guide(player, data, pixie, level, now);
	}

	/**
	 * What the player did on their own counts as theirs too: a quest whose thing they have, whose deed they did (a
	 * troll or a hydra beaten, a horse or a hippogryph ridden, a pixie of theirs at its fifth level) or whose place they
	 * stand in is done, one a second (each one a memory of hers back).
	 */
	private static void ownWay(ServerPlayer player, ExiledPixieData data, ServerLevel level) {
		for (ExiledQuest quest : ExiledQuest.ordered()) {
			if (quest.offered() && !data.isOver(quest) && data.quest != quest.ordinal() && (satisfied(player, quest) || visiting(level, player, quest))) {
				complete(player, data, quest, false);
				return;
			}
		}
	}

	/** She offers the first quest open to the player (another one than the last put off, if there is one). */
	private static void offer(ServerPlayer player, ExiledPixieData data, long now) {
		if (data.offered >= 0 || now < data.nextOffer) {
			return;
		}
		ExiledQuest putOff = PUT_OFF.get(player.getUUID());
		ExiledQuest next = null;
		for (ExiledQuest quest : ExiledQuest.ordered()) {
			if (quest.offered() && !data.isOver(quest) && quest.unlocked(data)) {
				if (quest != putOff) {
					next = quest;
					break;
				}
				if (next == null) {
					next = quest;
				}
			}
		}
		if (next == null) {
			data.nextOffer = now + 6000;
			ExiledPixies.save(player, data);
			return;
		}
		if (satisfied(player, next)) {
			complete(player, data, next, true);
			return;
		}
		data.offered = next.ordinal();
		ExiledPixies.save(player, data);
		ExiledPixies.say(player, Component.translatable("exiled.iceandfire_fabric.quest." + next.key() + ".offer"), MessageExiledSpeech.QUEST);
		ExiledPixies.sayLater(player, 60, Component.translatable("exiled.iceandfire_fabric.quest.answer", ExiledPixies.keyName()), MessageExiledSpeech.HINT);
	}

	// --- a quest's place ---

	private static FairyLandmarks.@Nullable Kind landmark(ExiledQuest quest) {
		return switch (quest) {
			case BONES -> FairyLandmarks.Kind.SKELETON;
			case CYCLOPS -> FairyLandmarks.Kind.CYCLOPS;
			case EGG -> FairyLandmarks.Kind.NEST;
			case HYDRA -> FairyLandmarks.Kind.HYDRA;
			default -> null;
		};
	}

	/** How near its place counts as there (blocks). */
	private static int near(ExiledQuest quest) {
		return switch (quest) {
			case BONES -> 16;
			case CYCLOPS, HYDRA -> 28;
			case EGG -> 40;
			case CRYONA -> 64;
			default -> 28;
		};
	}

	private static int range(ExiledQuest quest) {
		return switch (quest) {
			// (a hydra's cave is only in a swamp, never near the world's spawn)
			case CRYONA, HYDRA -> 6000;
			case FRIENDS, MAUSOLEUM, TEMPLE -> 2000;
			default -> 1600;
		};
	}

	private static void place(ServerPlayer player, ExiledPixieData data, ExiledQuest quest, ServerLevel level, long now) {
		UUID id = player.getUUID();
		String here = level.dimension().location().toString();
		if (data.target != null && !data.targetLevel.equals(here)) {
			// its place is in another world
			return;
		}
		if (quest == ExiledQuest.PIXIE5) {
			// no place: her menu (H) and pixie dust
			return;
		}
		if (hunts(quest, id)) {
			hunt(player, data, quest, level);
			return;
		}
		if (data.target == null) {
			find(player, data, quest, level, now);
			return;
		}
		if (data.via != null) {
			double wx = data.via.getX() + 0.5 - player.getX();
			double wz = data.via.getZ() + 0.5 - player.getZ();
			double px = data.target.getX() + 0.5 - player.getX();
			double pz = data.target.getZ() + 0.5 - player.getZ();
			boolean atWay = wx * wx + wz * wz <= 5 * 5 && Math.abs(data.via.getY() - player.getY()) <= 5;
			boolean inside = px * px + pz * pz <= 32 * 32 && player.getY() < data.target.getY() + 16 && player.getY() > data.target.getY() - 8;
			if (!atWay && !inside) {
				return;
			}
			data.via = null;
			ExiledPixies.save(player, data);
			ExiledPixies.say(player, Component.translatable("exiled.iceandfire_fabric.quest." + quest.key() + ".way"), MessageExiledSpeech.QUEST);
		}
		double dx = data.target.getX() + 0.5 - player.getX();
		double dz = data.target.getZ() + 0.5 - player.getZ();
		double distance = Math.sqrt(dx * dx + dz * dz);
		FairyLandmarks.Kind kind = landmark(quest);
		boolean loaded = FairyCheckpoints.loaded(level, data.target);
		boolean waiting = kind != null && FairyLandmarks.waiting(level, data.target);
		if (kind != null && loaded && !waiting) {
			if (distance < 90 && data.checkpoint == null) {
				// made where it was remembered (it may be a few blocks off): she leads to it
				BlockPos made = FairyLandmarks.nearest(level, kind, data.target, 24);
				if (made != null && !made.equals(data.target)) {
					data.target = made;
				}
			}
			if (distance < 40 && !FairyLandmarks.stillThere(level, kind, data.target)) {
				int missing = MISSING.merge(id, 1, Integer::sum);
				if (missing >= 3) {
					// gone (taken, beaten): she knows of another place
					FairyLandmarks.forget(level, kind, data.target);
					List<BlockPos> empty = EMPTY.computeIfAbsent(id, k -> new java.util.ArrayList<>());
					empty.add(data.target.immutable());
					while (empty.size() > 16) {
						empty.remove(0);
					}
					dropRing(player, data);
					data.target = null;
					data.via = null;
					data.progress &= ~1;
					MISSING.remove(id);
					ExiledPixies.save(player, data);
					ExiledPixies.say(player, Component.translatable("exiled.iceandfire_fabric.quest.gone"), MessageExiledSpeech.QUEST);
					return;
				}
			} else if (distance < 40) {
				MISSING.remove(id);
			}
		}
		// her ring at the place, once its land is there and her player near; broken (by them or anything), it is not put
		// back: being near the place is being there then
		if (data.checkpoint != null && FairyCheckpoints.loaded(level, data.checkpoint) && !FairyCheckpoints.present(level, data.checkpoint)) {
			data.checkpoint = null;
			NO_RING.put(id, Long.MAX_VALUE);
			ExiledPixies.save(player, data);
		}
		if (data.checkpoint == null && distance < 72 && loaded && !waiting && now >= NO_RING.getOrDefault(id, 0L)) {
			boolean cave = quest == ExiledQuest.FRIENDS && (data.progress & CAVE) != 0;
			BlockPos lantern = FairyCheckpoints.build(level, approach(level, data.target, player, quest, cave), data.variant, reach(quest), exact(data), cave);
			if (lantern == null) {
				NO_RING.put(id, now + 200);
			} else {
				NO_RING.remove(id);
				data.checkpoint = lantern;
				ExiledPixies.save(player, data);
				if ((data.progress & 1) == 0) {
					ExiledPixies.say(player, Component.translatable("exiled.iceandfire_fabric.checkpoint.made"), MessageExiledSpeech.TALK);
				}
			}
		}
		boolean there;
		if (data.checkpoint != null) {
			double cx = data.checkpoint.getX() + 0.5 - player.getX();
			double cz = data.checkpoint.getZ() + 0.5 - player.getZ();
			there = cx * cx + cz * cz <= 3.5 * 3.5 && Math.abs(data.checkpoint.getY() - player.getY()) <= 4;
		} else {
			// no ground for her ring there: being near the place is being there
			there = NO_RING.containsKey(id) && distance <= near(quest);
		}
		if (there && (data.progress & 1) == 0) {
			data.progress |= 1;
			ExiledPixies.save(player, data);
			if (data.checkpoint != null) {
				FairyCheckpoints.reached(level, data.checkpoint, data.variant);
			}
			if (quest == ExiledQuest.VILLAGE || quest == ExiledQuest.GRAVEYARD || quest == ExiledQuest.MAUSOLEUM || quest == ExiledQuest.TEMPLE) {
				complete(player, data, quest, false);
				return;
			}
			if (quest == ExiledQuest.FRIENDS) {
				ExiledTalk.friends(player, data);
				return;
			}
			ExiledPixies.say(player, Component.translatable("exiled.iceandfire_fabric.quest." + quest.key() + ".here"), MessageExiledSpeech.QUEST);
			if (quest == ExiledQuest.CAKE) {
				cakeChest(player, data, level);
			}
		}
	}

	/** At the witches' cottage: she shows their chest (full of cakes), and flies to it. */
	private static void cakeChest(ServerPlayer player, ExiledPixieData data, ServerLevel level) {
		// about the hut's floor: under its roof, the top of the land at its middle
		BlockPos from = new BlockPos(data.target.getX(), level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, data.target.getX(), data.target.getZ()),
			data.target.getZ());
		BlockPos chest = nearest(level, from, 16, 14, entity -> entity instanceof ChestBlockEntity);
		EntityExiledPixie pixie = ExiledPixies.living(player);
		if (chest == null || pixie == null) {
			return;
		}
		ExiledPixieData.remember(data.chests, chest.asLong(), 64);
		ExiledPixies.save(player, data);
		ExiledPixies.show(player, pixie, List.of(chest), 600);
	}

	/** The nearest block entity of a kind about a place in the loaded land (so far across, so far up or down), or null. */
	private static @Nullable BlockPos nearest(ServerLevel level, BlockPos from, int across, int upOrDown, Predicate<BlockEntity> kind) {
		BlockPos best = null;
		double bestDistance = (double) across * across;
		for (int cx = (from.getX() - across) >> 4; cx <= (from.getX() + across) >> 4; cx++) {
			for (int cz = (from.getZ() - across) >> 4; cz <= (from.getZ() + across) >> 4; cz++) {
				var chunk = level.getChunkSource().getChunkNow(cx, cz);
				if (chunk == null) {
					continue;
				}
				for (Map.Entry<BlockPos, BlockEntity> entry : chunk.getBlockEntities().entrySet()) {
					BlockPos at = entry.getKey();
					double dx = at.getX() - from.getX();
					double dz = at.getZ() - from.getZ();
					if (dx * dx + dz * dz < bestDistance && Math.abs(at.getY() - from.getY()) <= upOrDown && kind.test(entry.getValue())) {
						bestDistance = dx * dx + dz * dz;
						best = at;
					}
				}
			}
		}
		return best;
	}

	/** The creatures a quest is about (the nearest one about the player is where it leads). */
	private static @Nullable Class<? extends LivingEntity> prey(ExiledQuest quest) {
		return switch (quest) {
			case HORSE -> AbstractHorse.class;
			case HIPPOGRYPH -> EntityHippogryph.class;
			case TROLL -> EntityTroll.class;
			case HYDRA -> EntityHydra.class;
			default -> null;
		};
	}

	/** It leads to a creature itself (a hydra only when she remembers no hydra's cave near). */
	private static boolean hunts(ExiledQuest quest, UUID id) {
		return quest == ExiledQuest.HORSE || quest == ExiledQuest.HIPPOGRYPH || quest == ExiledQuest.TROLL || quest == ExiledQuest.HYDRA && HUNTS.contains(id);
	}

	/** Where a quest about a creature leads: the nearest one about the player (looked for each second), else a word on where they live. */
	private static void hunt(ServerPlayer player, ExiledPixieData data, ExiledQuest quest, ServerLevel level) {
		LivingEntity best = null;
		for (LivingEntity creature : level.getEntitiesOfClass(prey(quest), player.getBoundingBox().inflate(96, 48, 96),
			e -> e.isAlive() && e != player.getVehicle() && !(e instanceof net.minecraft.world.entity.animal.horse.Llama)
				&& !(e instanceof net.minecraft.world.entity.animal.camel.Camel))) {
			if (best == null || creature.distanceToSqr(player) < best.distanceToSqr(player)) {
				best = creature;
			}
		}
		if (best == null) {
			if (data.target != null) {
				data.target = null;
				ExiledPixies.save(player, data);
			}
			if ((data.progress & HINTED) == 0) {
				data.progress |= HINTED;
				ExiledPixies.save(player, data);
				ExiledPixies.say(player, Component.translatable("exiled.iceandfire_fabric.quest." + quest.key() + ".none"), MessageExiledSpeech.QUEST);
			}
			return;
		}
		data.target = best.blockPosition();
		data.targetLevel = level.dimension().location().toString();
		ExiledPixies.save(player, data);
	}

	/**
	 * Where her ring goes: beside a skeleton or a flock (between it and the player, not under it), in the witches'
	 * clearing on the side the player comes from, by a village's bell (where its people meet), on the pixies' cave's
	 * plaza a few steps from its altar (on the side the player comes in), at another structure itself.
	 */
	private static BlockPos approach(ServerLevel level, BlockPos place, ServerPlayer player, ExiledQuest quest, boolean cave) {
		if (quest == ExiledQuest.VILLAGE) {
			BlockPos bell = nearest(level, place, 48, level.getHeight(), entity -> entity instanceof BellBlockEntity);
			return bell != null ? bell : place;
		}
		int beside = switch (quest) {
			case BONES -> 7;
			case CAKE -> 6;
			case CYCLOPS, HYDRA -> 9;
			case FRIENDS -> cave ? 4 : 0;
			default -> 0;
		};
		if (beside == 0) {
			return place;
		}
		double dx = player.getX() - (place.getX() + 0.5);
		double dz = player.getZ() - (place.getZ() + 0.5);
		double d = Math.max(0.001, Math.sqrt(dx * dx + dz * dz));
		return place.offset((int) Math.round(dx / d * beside), 0, (int) Math.round(dz / d * beside));
	}

	/** How far from that point her ring may go to find open ground (a structure covers ground: a little more). */
	private static int reach(ExiledQuest quest) {
		return switch (quest) {
			case BONES, CYCLOPS, EGG, HYDRA -> 8;
			case CRYONA -> 28;
			default -> 20;
		};
	}

	/** The pillar of light over each lit ring, for its player (every half second, seen from far). */
	static void pillars(MinecraftServer server) {
		for (ServerPlayer player : server.getPlayerList().getPlayers()) {
			ExiledPixieData data = ExiledPixies.get(player);
			if (data.checkpoint == null || data.current() == null || !(player.level() instanceof ServerLevel level)
				|| !data.targetLevel.equals(level.dimension().location().toString())) {
				continue;
			}
			double dx = data.checkpoint.getX() + 0.5 - player.getX();
			double dz = data.checkpoint.getZ() + 0.5 - player.getZ();
			double d = dx * dx + dz * dz;
			if (d < 256 * 256 && d > 4 * 4) {
				FairyCheckpoints.pillar(level, player, data.checkpoint, data.variant);
			}
		}
	}

	/** Where its place is: a place noted (or remembered, for a skeleton or a cyclops), or a structure looked for. */
	private static void find(ServerPlayer player, ExiledPixieData data, ExiledQuest quest, ServerLevel level, long now) {
		UUID id = player.getUUID();
		if (SEARCHES.containsKey(id) || now - LOOKED.getOrDefault(id, -99999L) < 2400) {
			return;
		}
		FairyLandmarks.Kind kind = landmark(quest);
		if (kind != null) {
			BlockPos at = FairyLandmarks.nearest(level, kind, player.blockPosition(), kind == FairyLandmarks.Kind.NEST || kind == FairyLandmarks.Kind.HYDRA ? 1600 : 900);
			if (at == null && (kind == FairyLandmarks.Kind.SKELETON || kind == FairyLandmarks.Kind.CYCLOPS)
				&& level.dimension() == net.minecraft.world.level.Level.OVERWORLD) {
				at = FairyLandmarks.remember(level, kind, player.blockPosition(), player.getRandom());
			}
			if (at != null) {
				found(player, data, level, at);
				return;
			}
		}
		List<String> names = STRUCTURES.get(quest);
		if (names == null && quest != ExiledQuest.VILLAGE) {
			LOOKED.put(id, now);
			ExiledPixies.say(player, Component.translatable("exiled.iceandfire_fabric.quest.none"), MessageExiledSpeech.QUEST);
			return;
		}
		List<Holder<Structure>> structures = new ArrayList<>();
		var registry = level.registryAccess().lookupOrThrow(Registries.STRUCTURE);
		if (quest == ExiledQuest.VILLAGE) {
			// the nearest village there really is, of any kind
			registry.get(StructureTags.VILLAGE).ifPresent(set -> set.forEach(structures::add));
		} else {
			for (String name : names) {
				registry.get(ResourceKey.create(Registries.STRUCTURE, IafMod.id(name))).ifPresent(structures::add);
			}
		}
		FairyLocator locator = new FairyLocator(level, structures, player.blockPosition(), range(quest), EMPTY.getOrDefault(id, List.of()));
		SEARCHES.put(id, new Search(quest, level.dimension(), locator));
		ExiledPixies.say(player, Component.translatable("exiled.iceandfire_fabric.quest.searching"), MessageExiledSpeech.TALK);
	}

	private static void found(ServerPlayer player, ExiledPixieData data, ServerLevel level, BlockPos at) {
		data.target = at;
		data.via = null;
		data.targetLevel = level.dimension().location().toString();
		data.progress &= ~(1 | CAVE);
		PixieCavernPiece cave = data.current() == ExiledQuest.FRIENDS ? cave(level, at) : null;
		if (cave != null) {
			// the pixies' cave: its plaza, its way in first
			data.target = cave.gate();
			data.via = cave.wayIn();
			data.progress |= CAVE;
		}
		ExiledPixies.save(player, data);
		BlockPos goal = data.goal();
		double dx = goal.getX() + 0.5 - player.getX();
		double dz = goal.getZ() + 0.5 - player.getZ();
		int distance = (int) Math.sqrt(dx * dx + dz * dz);
		ExiledPixies.say(player, Component.translatable("exiled.iceandfire_fabric.quest.found", distance,
			Component.translatable("exiled.iceandfire_fabric.dir." + direction(dx, dz))), MessageExiledSpeech.QUEST);
	}

	/** The pixies' cave that starts at a place the search found, if it is one (its start is known there already). */
	private static @Nullable PixieCavernPiece cave(ServerLevel level, BlockPos at) {
		var chunk = level.getChunk(at.getX() >> 4, at.getZ() >> 4, net.minecraft.world.level.chunk.status.ChunkStatus.STRUCTURE_STARTS);
		for (var start : chunk.getAllStarts().values()) {
			for (var piece : start.getPieces()) {
				if (piece instanceof PixieCavernPiece cave) {
					return cave;
				}
			}
		}
		return null;
	}

	/** One of the eight directions (n, ne, e...) of a way on the ground. */
	public static String direction(double dx, double dz) {
		String[] names = {"n", "ne", "e", "se", "s", "sw", "w", "nw"};
		double angle = Math.toDegrees(Math.atan2(dx, -dz));
		return names[Math.floorMod((int) Math.round(angle / 45.0), 8)];
	}

	/** The searches go on a few places a tick; one over gives its quest its place (or a word that none is near). */
	static void searchTick(MinecraftServer server) {
		if (SEARCHES.isEmpty()) {
			return;
		}
		for (Iterator<Map.Entry<UUID, Search>> it = SEARCHES.entrySet().iterator(); it.hasNext(); ) {
			Map.Entry<UUID, Search> entry = it.next();
			ServerPlayer player = server.getPlayerList().getPlayer(entry.getKey());
			Search search = entry.getValue();
			if (player == null) {
				it.remove();
				continue;
			}
			if (!search.locator().step()) {
				continue;
			}
			it.remove();
			ExiledPixieData data = ExiledPixies.get(player);
			if (data.quest != search.quest().ordinal()) {
				continue;
			}
			BlockPos at = search.locator().found();
			ServerLevel level = server.getLevel(search.level());
			if (at == null && search.quest() == ExiledQuest.HYDRA) {
				// no hydra's cave within reach: a hydra about, if there is one
				HUNTS.add(entry.getKey());
				continue;
			}
			if (at == null || level == null) {
				LOOKED.put(entry.getKey(), level == null ? 0 : level.getGameTime());
				ExiledPixies.say(player, Component.translatable(player.level().dimension() == net.minecraft.world.level.Level.OVERWORLD
					? "exiled.iceandfire_fabric.quest.none" : "exiled.iceandfire_fabric.quest.other_world"), MessageExiledSpeech.QUEST);
				continue;
			}
			found(player, data, level, at);
		}
	}

	/** She flies ahead of her player toward the place (over it once near), unless she shows a hidden chest. */
	private static void guide(ServerPlayer player, ExiledPixieData data, @Nullable EntityExiledPixie pixie, ServerLevel level, long now) {
		if (pixie == null || !data.guide || data.target == null || !data.targetLevel.equals(level.dimension().location().toString())) {
			return;
		}
		Hint hint = HINTS.get(player.getUUID());
		if (hint != null && hint.until() > now) {
			return;
		}
		BlockPos goal = data.goal();
		double dx = goal.getX() + 0.5 - player.getX();
		double dz = goal.getZ() + 0.5 - player.getZ();
		double distance = Math.sqrt(dx * dx + dz * dz);
		if (distance > 16) {
			Vec3 ahead = player.getEyePosition().add(dx / distance * Math.min(7, distance / 2), 0.5, dz / distance * Math.min(7, distance / 2));
			pixie.guide(ahead, 40);
		} else {
			// over her lantern, or over the place (above what is there: a skeleton's spine, a cyclops' head)
			ExiledQuest quest = quest(data);
			double over = data.checkpoint != null ? 2.2 : quest == ExiledQuest.BONES ? 4.5
				: quest == ExiledQuest.CYCLOPS || quest == ExiledQuest.TROLL || quest == ExiledQuest.HYDRA ? 5.5 : 2.5;
			pixie.guide(new Vec3(goal.getX() + 0.5, groundY(level, goal, player, exact(data)) + over, goal.getZ() + 0.5), 40);
		}
	}

	private static @Nullable ExiledQuest quest(ExiledPixieData data) {
		return data.current();
	}

	/** The ground's height at a place (if its land is loaded; else the player's). */
	private static int groundY(ServerLevel level, BlockPos at, ServerPlayer player, boolean exact) {
		if (!level.hasChunk(at.getX() >> 4, at.getZ() >> 4)) {
			return player.blockPosition().getY();
		}
		if (exact && Math.abs(at.getY() - player.getY()) < 24 && at.getY() > level.getMinBuildHeight()) {
			return at.getY();
		}
		return level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, at.getX(), at.getZ());
	}

	/**
	 * The quest's place is where something is (a skeleton, a cyclops, a den, the pixies' cave's plaza under the land),
	 * not a structure's start (whose height is not its own).
	 */
	private static boolean exact(ExiledPixieData data) {
		ExiledQuest quest = data.current();
		return data.checkpoint != null || quest != null && (landmark(quest) != null || prey(quest) != null)
			|| quest == ExiledQuest.FRIENDS && (data.progress & CAVE) != 0;
	}

	/** A hidden chest she shows: marked on the screen until then. */
	static void hint(ServerPlayer player, BlockPos chest, long until) {
		HINTS.put(player.getUUID(), new Hint(chest, until, player.level().dimension().location().toString()));
	}

	// --- events ---

	/** A foe beaten: a cyclops beaten on her quest always leaves his eye (she makes sure of it). */
	static void killed(ServerPlayer player, ExiledPixieData data, LivingEntity victim) {
		if (victim instanceof EntityCyclops && data.quest == ExiledQuest.CYCLOPS.ordinal() && count(player, IafItemRegistry.CYCLOPS_EYE) == 0) {
			ServerLevel level = (ServerLevel) victim.level();
			boolean dropped = !level.getEntitiesOfClass(ItemEntity.class, victim.getBoundingBox().inflate(4),
				item -> item.getItem().is(IafItemRegistry.CYCLOPS_EYE)).isEmpty();
			if (!dropped) {
				victim.spawnAtLocation(new ItemStack(IafItemRegistry.CYCLOPS_EYE));
			}
		}
	}

	/** The right moment came (her glade, her gift): her last quest, nameless till then, is done. */
	static void momentCome(ServerPlayer player, ExiledPixieData data) {
		if (data.stage == ExiledPixieData.FRIEND && !data.isDone(ExiledQuest.CRYONA)) {
			data.done |= ExiledQuest.CRYONA.bit();
			if (data.quest == ExiledQuest.CRYONA.ordinal()) {
				data.quest = -1;
			}
			ExiledPixies.save(player, data);
			ExiledPixies.addXp(player, data, ExiledQuest.CRYONA.xp);
		}
	}

	// --- for the player's screen ---

	/** The quest under way's goal, as she says it. */
	static Component goal(ServerPlayer player, ExiledPixieData data, ExiledQuest quest) {
		if (quest == ExiledQuest.BONES) {
			return Component.translatable("exiled.iceandfire_fabric.quest.bones.goal", count(player, IafItemRegistry.DRAGON_BONE), BONES_NEEDED);
		}
		return Component.translatable("exiled.iceandfire_fabric.quest." + quest.key() + ".goal");
	}

	/** What the screen shows besides her state: the bones, the search, the chest she shows. */
	public static CompoundTag extra(ServerPlayer player, ExiledPixieData data) {
		CompoundTag tag = new CompoundTag();
		ExiledQuest quest = data.current();
		if (quest == ExiledQuest.BONES) {
			tag.putInt("Bones", count(player, IafItemRegistry.DRAGON_BONE));
		} else if (quest == ExiledQuest.FRIENDS) {
			tag.putBoolean("HasCake", count(player, Items.CAKE) > 0);
		}
		tag.putBoolean("Searching", SEARCHES.containsKey(player.getUUID()));
		Hint hint = HINTS.get(player.getUUID());
		if (hint != null && hint.until() > player.level().getGameTime()) {
			tag.putLong("Hint", hint.pos().asLong());
			tag.putLong("HintUntil", hint.until());
			tag.putString("HintLevel", hint.level());
		}
		if (data.target != null && player.level() instanceof ServerLevel level) {
			tag.putInt("TargetY", groundY(level, data.goal(), player, exact(data)));
		}
		return tag;
	}
}

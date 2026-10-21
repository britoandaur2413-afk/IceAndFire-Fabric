package com.iafabric.boss;

import com.iafabric.IafMod;
import com.iafabric.world.structure.DreadPieces;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.AttackBlockCallback;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ArmorStandItem;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.BoatItem;
import net.minecraft.world.item.BoneMealItem;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.EndCrystalItem;
import net.minecraft.world.item.FireChargeItem;
import net.minecraft.world.item.FlintAndSteelItem;
import net.minecraft.world.item.HangingEntityItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.LeadItem;
import net.minecraft.world.item.MinecartItem;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Cryona's citadel holds while she reigns: none of its stones can be broken and nothing can be put in it (no block,
 * no lava or water, no fire, no boat or creature), and no blast nor dragon touches it, so the only way to her throne
 * is through its gate and halls. When she is beaten the cold that held it breaks and it is stone like any other. Her
 * realm, the Frozen Throne, holds always: nothing can be put there nor broken. Who comes onto the citadel's grounds
 * hears her from deep inside. Players in creative may still build.
 */
public final class CastleWard {
	public static final ResourceKey<Structure> CITADEL = ResourceKey.create(Registries.STRUCTURE, IafMod.id("dread_throne"));
	/** How long before a player who left hears her again on coming back (ticks). */
	private static final int HEARD_AGAIN = 6000;
	private static final Map<UUID, Long> HEARD = new HashMap<>();

	private CastleWard() {
	}

	public static void register() {
		PlayerBlockBreakEvents.BEFORE.register((level, player, pos, state, blockEntity) -> {
			if (player.isCreative() || !warded(level, pos)) {
				return true;
			}
			refuse(player, level);
			return false;
		});
		// (in her realm not even a dent: no block is struck at all)
		AttackBlockCallback.EVENT.register((player, level, hand, pos, direction) ->
			!player.isCreative() && !player.isSpectator() && level.dimension() == ThroneFight.REALM ? InteractionResult.FAIL : InteractionResult.PASS);
		UseBlockCallback.EVENT.register((player, level, hand, hit) -> {
			ItemStack stack = player.getItemInHand(hand);
			if (player.isCreative() || player.isSpectator() || !placesSomething(stack)
				|| level.getBlockState(hit.getBlockPos()).is(com.iafabric.block.IafBlockRegistry.THRONE_ALTAR)) {
				return InteractionResult.PASS;
			}
			if (warded(level, hit.getBlockPos()) || warded(level, hit.getBlockPos().relative(hit.getDirection()))) {
				refuse(player, level);
				return InteractionResult.FAIL;
			}
			return InteractionResult.PASS;
		});
		UseItemCallback.EVENT.register((player, level, hand) -> {
			Item item = player.getItemInHand(hand).getItem();
			boolean pours = item instanceof BucketItem || item instanceof BoatItem || item instanceof SpawnEggItem;
			if (!player.isCreative() && !player.isSpectator() && pours && warded(level, player.blockPosition())) {
				refuse(player, level);
				return InteractionResult.FAIL;
			}
			return InteractionResult.PASS;
		});
		ServerTickEvents.END_LEVEL_TICK.register(level -> {
			if (level.getGameTime() % 20 == 0 && level.dimension() == Level.OVERWORLD) {
				visitors(level);
			}
		});
	}

	/** Told why the stone does not give. */
	public static void refuse(Player player) {
		player.sendOverlayMessage(Component.translatable("iceandfire_fabric.castle.sealed").withStyle(ChatFormatting.AQUA));
	}

	/** Told why nothing can be put or broken here (her realm, or her citadel). */
	public static void refuse(Player player, Level level) {
		if (level.isClientSide()) {
			return;
		}
		player.sendOverlayMessage(Component.translatable(level.dimension() == ThroneFight.REALM ? "iceandfire_fabric.realm.warded" : "iceandfire_fabric.castle.sealed")
			.withStyle(ChatFormatting.AQUA));
	}

	/** Whether nothing may be put or broken at a place: anywhere in her realm, or in a citadel that still holds. */
	public static boolean warded(Level level, BlockPos pos) {
		return level.dimension() == ThroneFight.REALM || sealed(level, pos);
	}

	/**
	 * What a player would put into the world with this in hand, used on a block: a block, lava or water (or what a
	 * bucket takes), fire, a boat, a cart, a creature, a crystal, a stand, a painting or frame, a lead's knot, a plant.
	 */
	public static boolean placesSomething(ItemStack stack) {
		Item item = stack.getItem();
		return item instanceof BlockItem || item instanceof BucketItem || item instanceof SpawnEggItem || item instanceof BoatItem
			|| item instanceof MinecartItem || item instanceof ArmorStandItem || item instanceof EndCrystalItem || item instanceof FlintAndSteelItem
			|| item instanceof FireChargeItem || item instanceof HangingEntityItem || item instanceof LeadItem || item instanceof BoneMealItem;
	}

	/** Whether a place is in a citadel that still holds (her rule not broken). */
	public static boolean sealed(Level level, BlockPos pos) {
		if (!(level instanceof ServerLevel server)) {
			return false;
		}
		StructureStart start = citadelAt(server, pos);
		return start != null && !freed(server, start);
	}

	/** The citadel a loaded place is in (its grounds), or null; never loads a chunk. */
	public static @Nullable StructureStart citadelAt(ServerLevel level, BlockPos pos) {
		if (!level.isLoaded(pos)) {
			return null;
		}
		Structure structure = citadel(level.getServer());
		if (structure == null) {
			return null;
		}
		StructureStart start = level.structureManager().getStructureAt(pos, structure);
		return start.isValid() ? start : null;
	}

	private static @Nullable MinecraftServer cachedFor;
	private static @Nullable Structure cached;

	/** The citadel's structure (looked up once per server: the breath of a dragon asks for every block it touches). */
	private static @Nullable Structure citadel(MinecraftServer server) {
		if (cachedFor != server) {
			cached = server.registryAccess().lookupOrThrow(Registries.STRUCTURE).getValue(CITADEL);
			cachedFor = server;
		}
		return cached;
	}

	/** Her rule over the citadel at a place is broken (she was beaten): from now on it is stone like any other. */
	public static void free(ServerLevel level, BlockPos pos) {
		StructureStart start = citadelAt(level, pos);
		if (start == null) {
			return;
		}
		Freed data = Freed.get(level.getServer());
		if (data.starts.add(start.getChunkPos().pack())) {
			data.setDirty();
			IafMod.LOGGER.info("Cryona's citadel at {} is free", start.getBoundingBox().getCenter());
			for (ServerPlayer player : level.players()) {
				if (start.getBoundingBox().isInside(player.blockPosition())) {
					player.sendSystemMessage(Component.translatable("iceandfire_fabric.castle.freed").withStyle(ChatFormatting.GOLD));
				}
			}
		}
	}

	/** Her rule broken over the citadel whose start is in a chunk (known from before, its land need not be loaded). */
	public static void freeStart(MinecraftServer server, long start) {
		Freed data = Freed.get(server);
		if (data.starts.add(start)) {
			data.setDirty();
			IafMod.LOGGER.info("Cryona's citadel of chunk {} is free", net.minecraft.world.level.ChunkPos.unpack(start));
		}
	}

	public static boolean freed(ServerLevel level, StructureStart start) {
		return Freed.get(level.getServer()).starts.contains(start.getChunkPos().pack());
	}

	/** Who comes onto the grounds of a citadel that still holds hears her from her throne. */
	private static void visitors(ServerLevel level) {
		long now = level.getGameTime();
		for (ServerPlayer player : level.players()) {
			if (player.isSpectator()) {
				continue;
			}
			StructureStart start = citadelAt(level, player.blockPosition());
			if (start == null || freed(level, start)) {
				continue;
			}
			Long last = HEARD.get(player.getUUID());
			HEARD.put(player.getUUID(), now);
			if (last != null && now - last < HEARD_AGAIN) {
				continue;
			}
			CryonaVoice.sayTo(player, Vec3.atCenterOf(throneOf(start)), CryonaVoice.Line.ENTRY);
		}
	}

	/** Where her throne is in a citadel (its middle, if the piece is not hers). */
	public static BlockPos throneOf(StructureStart start) {
		for (StructurePiece piece : start.getPieces()) {
			if (piece instanceof com.iafabric.world.structure.CitadelPiece citadel) {
				return citadel.throneSeat();
			}
			if (piece instanceof DreadPieces.Throne throne) {
				return throne.throneSeat();
			}
		}
		return start.getBoundingBox().getCenter();
	}

	/** The citadels whose rule is broken, by the chunk of their start. Saved with the world. */
	static final class Freed extends SavedData {
		private static final SavedDataType<Freed> TYPE = new SavedDataType<>(IafMod.stored("iafabric_freed_citadels"), Freed::new,
			CompoundTag.CODEC.xmap(Freed::new, data -> data.save(new CompoundTag())), DataFixTypes.SAVED_DATA_COMMAND_STORAGE);
		private final LongSet starts = new LongOpenHashSet();

		private Freed() {
		}

		private Freed(CompoundTag tag) {
			for (long start : tag.getLongArray("Starts").orElse(new long[0])) {
				starts.add(start);
			}
		}

		private CompoundTag save(CompoundTag tag) {
			tag.putLongArray("Starts", starts.toLongArray());
			return tag;
		}

		static Freed get(MinecraftServer server) {
			return server.overworld().getDataStorage().computeIfAbsent(TYPE);
		}
	}
}

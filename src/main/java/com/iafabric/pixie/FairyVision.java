package com.iafabric.pixie;

import com.iafabric.IafMod;
import com.iafabric.block.IafBlockRegistry;
import com.iafabric.entity.EntityDreadQueen;
import com.iafabric.entity.EntityExiledPixie;
import com.iafabric.message.MessageExiledEyes;
import com.iafabric.message.MessageExiledMemory;
import com.iafabric.message.MessageExiledSpeech;
import com.iafabric.network.IafNetwork;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.player.AttackBlockCallback;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Prediction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Relative;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * The right moment. A player whose exiled pixie is their friend comes upon Cryona (her throne): with all her memories
 * back (every quest of hers over) the pixie stops them ("Wait! Come with me.") and takes them to her own memory, a
 * glade floating in a pastel sky (its own world): there she tells them it is the right moment, who the girl marked by
 * ice was, bids them close their eyes (all goes dark), gives them the most precious thing she kept from her past life,
 * the resistance to absolute ice ({@link ExiledPixieData#GIFT_ICE}), and their eyes open again little by little; then
 * they are back where they were. Her last quest, nameless until then, is done. Not ready yet, she warns them now and
 * then that they are not, and why (her gift's lack: Black Frost's ice to the heart, no healing), and leaves the fight
 * theirs to take.
 * <p>
 * It is a memory, seen as a film (MemoryOverlay on their screen): from her call to their coming back the screen is
 * framed, her words are its subtitles, their eyes close as she calls and open in her glade, and nothing can go wrong in
 * it: they stand still where it holds them (no falling off her island), nothing hurts them, nothing is broken, put, used
 * or struck, what they throw is in their pack again at once, and her other words wait till it is over.
 */
public final class FairyVision {
	public static final ResourceKey<Level> GLADE = ResourceKey.create(Registries.DIMENSION, IafMod.id("fairy_glade"));
	/** The middle of her glade (her lantern), and where the player stands in it. */
	private static final BlockPos CENTER = new BlockPos(0, 80, 0);
	private static final Vec3 STAND = new Vec3(0.5, 80, 6.5);
	/** Ticks between her call and the glade; their eyes close in the meantime (from when, how long), and open in it. */
	private static final int CALL = 50;
	private static final int CALL_CLOSE_AT = 18;
	private static final int CALL_CLOSE = 30;
	private static final int ARRIVE_OPEN = 70;
	/** Her lines in the glade, when each is said (ticks after arriving: time enough to read the one before), and her gift. */
	private static final int[] LINES = {40, 170, 250, 390, 540};
	private static final int GIFT = 640;
	/** Their eyes: closing once they read her bidding (the last line), opening after her gift, and how long each takes. */
	private static final int CLOSE_AFTER = 15;
	private static final int CLOSE_TICKS = 40;
	private static final int OPEN_AT = GIFT + 40;
	private static final int OPEN_TICKS = 90;
	/** Her gift's name in the middle of the screen, their eyes half open. */
	private static final int CARD_AT = OPEN_AT + 45;
	/** Their eyes close to go back (when, how long); back where they were, they open them (how long); the memory over. */
	private static final int LEAVE = 850;
	private static final int LEAVE_TICKS = 30;
	private static final int BACK = LEAVE + LEAVE_TICKS + 4;
	private static final int BACK_OPEN = 45;
	private static final int END = BACK + BACK_OPEN + 5;
	/** Until when the place they are held at follows them (they may still be stepping as the screen holds them). */
	private static final int SETTLE = 8;
	/** How long between two warnings that they are not ready (five minutes). */
	private static final int WARN_AGAIN = 6000;

	private static final Map<UUID, Long> RUNNING = new HashMap<>();
	private static final Map<UUID, Long> WARNED = new HashMap<>();
	/** What a player let go of in her glade with a full pack: theirs again when they are back. */
	private static final Map<UUID, List<ItemStack>> KEPT = new HashMap<>();
	/** Set while the memory says its own lines (her other words wait). */
	private static boolean speaking;

	private FairyVision() {
	}

	/** What a memory keeps from going wrong: harm, breaking, putting, using, striking. */
	public static void register() {
		ServerLivingEntityEvents.ALLOW_DAMAGE.register((entity, source, amount) -> !(entity instanceof ServerPlayer player && inMemory(player)));
		ServerLivingEntityEvents.ALLOW_DEATH.register((entity, source, amount) -> !(entity instanceof ServerPlayer player && inMemory(player)));
		PlayerBlockBreakEvents.BEFORE.register((level, player, pos, state, blockEntity) -> !(player instanceof ServerPlayer server && inMemory(server)));
		AttackBlockCallback.EVENT.register((player, level, hand, pos, direction) -> refused(player));
		UseBlockCallback.EVENT.register((player, level, hand, hit) -> refused(player));
		UseItemCallback.EVENT.register((player, level, hand) -> refused(player));
		AttackEntityCallback.EVENT.register((player, level, hand, entity, hit) -> refused(player));
		UseEntityCallback.EVENT.register((player, level, hand, entity, hit) -> refused(player));
		ServerLifecycleEvents.SERVER_STOPPING.register(server -> {
			// (who is in her glade goes back where they came from when they come back: FairyVision.strayed)
			for (Map.Entry<UUID, List<ItemStack>> entry : KEPT.entrySet()) {
				ServerPlayer player = server.getPlayerList().getPlayer(entry.getKey());
				if (player != null) {
					entry.getValue().forEach(stack -> player.getInventory().placeItemBackInInventory(stack, Prediction.SERVER_ONLY));
				}
			}
			KEPT.clear();
			RUNNING.clear();
			WARNED.clear();
		});
	}

	private static InteractionResult refused(Player player) {
		return player instanceof ServerPlayer server && inMemory(server) ? InteractionResult.FAIL : InteractionResult.PASS;
	}

	public static boolean inGlade(ServerPlayer player) {
		return player.level().dimension() == GLADE;
	}

	/** Whether a player is in her memory (from her call to their coming back, or in her glade at all). */
	public static boolean inMemory(ServerPlayer player) {
		return RUNNING.containsKey(player.getUUID()) || inGlade(player);
	}

	/** Whether her other words wait (the memory says only its own). */
	static boolean quiet(ServerPlayer player) {
		return !speaking && inMemory(player);
	}

	/** A line of the memory's own. */
	private static void line(ServerPlayer player, Component text, int kind) {
		speaking = true;
		try {
			ExiledPixies.say(player, text, kind);
		} finally {
			speaking = false;
		}
	}

	private static void eyes(ServerPlayer player, boolean open, int ticks) {
		IafNetwork.sendToPlayer(new MessageExiledEyes(open, ticks), player);
	}

	/**
	 * Each second: a player near Cryona, their pixie a friend and out: her call if all her memories are back (not in
	 * the middle of the queen's duel), else now and then a warning that they are not ready.
	 */
	static void check(ServerPlayer player, ExiledPixieData data, long now) {
		if (data.stage != ExiledPixieData.FRIEND || !data.out || data.hasGift(ExiledPixieData.GIFT_ICE) || RUNNING.containsKey(player.getUUID())
			|| inGlade(player) || !player.isAlive() || player.isSpectator()) {
			return;
		}
		// (asleep on her throne, her sign; or herself, wherever she is)
		boolean queen = !player.level().getEntitiesOfClass(net.minecraft.world.entity.LivingEntity.class, player.getBoundingBox().inflate(40, 24, 40),
			q -> q.isAlive() && (q instanceof EntityDreadQueen || q instanceof com.iafabric.entity.EntityCryonaAsleep)).isEmpty();
		if (!queen) {
			return;
		}
		if (!ExiledQuest.ready(data)) {
			if (now - WARNED.getOrDefault(player.getUUID(), -99999L) >= WARN_AGAIN) {
				WARNED.put(player.getUUID(), now);
				ExiledPixies.say(player, Component.translatable("exiled.iceandfire_fabric.vision.not_ready"), MessageExiledSpeech.ALARM);
				ExiledPixies.sayLater(player, 70, Component.translatable("exiled.iceandfire_fabric.vision.not_ready_why", ExiledQuest.left(data)),
					MessageExiledSpeech.QUEST);
			}
			return;
		}
		// (on their feet, neither riding nor in the air: the memory holds them where they stand)
		if (!com.iafabric.boss.ThroneFight.inRealm(player) && !player.isPassenger() && !player.isFallFlying() && (player.onGround() || player.isInWater())) {
			start(player, data, now);
		}
	}

	/** Her call: in a moment they are in her glade. */
	public static void start(ServerPlayer player, ExiledPixieData data, long now) {
		data.visionLevel = player.level().dimension().identifier().toString();
		data.visionFrom = player.position();
		data.visionYaw = player.getYRot();
		ExiledPixies.save(player, data);
		RUNNING.put(player.getUUID(), now);
		// a memory: the screen framed as a film, the player still, nothing in use nor open
		player.stopUsingItem();
		if (player.containerMenu != player.inventoryMenu) {
			player.closeContainer();
		}
		IafNetwork.sendToPlayer(new MessageExiledMemory(MessageExiledMemory.ON), player);
		// her memories' music, wherever the memory takes them
		com.iafabric.boss.Soundtracks.hear(player, com.iafabric.boss.Soundtracks.MEMORIES, null);
		line(player, Component.translatable("exiled.iceandfire_fabric.vision.call"), MessageExiledSpeech.ALARM);
		player.level().playSound(null, player.blockPosition(), SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.PLAYERS, 1.2F, 0.8F);
		IafMod.LOGGER.info("{}'s exiled pixie takes them to her glade", player.getName().getString());
	}

	/** Each tick: the visions under way go on. */
	static void tick(MinecraftServer server) {
		if (RUNNING.isEmpty()) {
			return;
		}
		long now = server.overworld().getGameTime();
		for (Iterator<Map.Entry<UUID, Long>> it = RUNNING.entrySet().iterator(); it.hasNext(); ) {
			Map.Entry<UUID, Long> entry = it.next();
			ServerPlayer player = server.getPlayerList().getPlayer(entry.getKey());
			if (player == null) {
				// (left the game: back where they came from when they come back, strayed)
				it.remove();
				continue;
			}
			long t = now - entry.getValue();
			ExiledPixieData data = ExiledPixies.get(player);
			if (t == CALL_CLOSE_AT) {
				eyes(player, false, CALL_CLOSE);
			}
			if (t < CALL) {
				if (t < SETTLE) {
					data.visionFrom = player.position();
				} else {
					hold(player, data.visionFrom);
				}
				continue;
			}
			if (t == CALL) {
				ServerLevel glade = server.getLevel(GLADE);
				if (glade == null) {
					it.remove();
					eyes(player, true, 20);
					end(player, data, false);
					continue;
				}
				ExiledPixies.save(player, data);
				build(glade, data.variant, player.getRandom());
				player.teleportTo(glade, STAND.x, STAND.y, STAND.z, Set.of(), 180, 5, false);
				player.resetFallDistance();
				ExiledPixies.bring(player);
				glade.playSound(null, CENTER, SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 1.5F, 1.2F);
				// their eyes open in her memory
				eyes(player, true, ARRIVE_OPEN);
				continue;
			}
			long here = t - CALL;
			for (int i = 0; i < LINES.length; i++) {
				if (here == LINES[i]) {
					line(player, Component.translatable("exiled.iceandfire_fabric.vision." + i), MessageExiledSpeech.QUEST);
				}
			}
			if (here == LINES[LINES.length - 1] + CLOSE_AFTER) {
				// "close your eyes": all goes dark
				eyes(player, false, CLOSE_TICKS);
			}
			if (here == OPEN_AT) {
				eyes(player, true, OPEN_TICKS);
			}
			EntityExiledPixie pixie = ExiledPixies.living(player);
			if (pixie != null && inGlade(player)) {
				// she floats before their eyes, facing them, all the while
				pixie.focus(null, 0);
				pixie.speak(40);
			}
			if (here == GIFT && inGlade(player)) {
				gift(player, data, (ServerLevel) player.level());
			}
			if (here == CARD_AT) {
				IafNetwork.sendToPlayer(new MessageExiledMemory(MessageExiledMemory.CARD, Component.translatable("exiled.iceandfire_fabric.vision.gift_name")), player);
			}
			if (here == LEAVE) {
				eyes(player, false, LEAVE_TICKS);
			}
			if (here == BACK) {
				back(player, data);
				ExiledPixies.bring(player);
				eyes(player, true, BACK_OPEN);
			}
			if (here < BACK) {
				if (inGlade(player)) {
					hold(player, STAND);
					keepItems(player);
				}
			} else {
				hold(player, data.visionFrom);
			}
			if (here >= END) {
				it.remove();
				end(player, data, true);
			}
		}
	}

	/**
	 * Where the memory holds them: they do not move (nor fall off her island) from there till it is over. (Not if they
	 * are far from it: taken away by a command, they are left where they are.)
	 */
	private static void hold(ServerPlayer player, @Nullable Vec3 at) {
		player.resetFallDistance();
		if (at == null) {
			return;
		}
		double away = player.position().distanceToSqr(at);
		if (away > 0.09 && away < 64 * 64) {
			player.teleportTo((ServerLevel) player.level(), at.x, at.y, at.z, Relative.ROTATION, 0, 0, false);
		}
	}

	/** Nothing is let go of in a memory: what they throw in her glade is in their pack again at once (kept for them if it is full). */
	private static void keepItems(ServerPlayer player) {
		for (ItemEntity item : player.level().getEntitiesOfClass(ItemEntity.class, player.getBoundingBox().inflate(48))) {
			if (item.getOwner() != null && item.getOwner() != player) {
				continue;
			}
			ItemStack stack = item.getItem().copy();
			item.discard();
			if (!player.getInventory().add(stack) && !stack.isEmpty()) {
				KEPT.computeIfAbsent(player.getUUID(), id -> new ArrayList<>()).add(stack);
			}
		}
	}

	/** Her gift: her last and most precious memory, the resistance to absolute ice. */
	private static void gift(ServerPlayer player, ExiledPixieData data, ServerLevel level) {
		data.gifts |= ExiledPixieData.GIFT_ICE;
		ExiledPixies.save(player, data);
		line(player, Component.translatable("exiled.iceandfire_fabric.vision.gift"), MessageExiledSpeech.QUEST);
		player.sendSystemMessage(Component.translatable("exiled.iceandfire_fabric.vision.gift_name").withStyle(net.minecraft.ChatFormatting.AQUA, net.minecraft.ChatFormatting.BOLD));
		level.playSound(null, player.blockPosition(), SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, SoundSource.PLAYERS, 0.8F, 1.2F);
		level.playSound(null, player.blockPosition(), SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.PLAYERS, 1.5F, 1.6F);
		DustParticleOptions dust = new DustParticleOptions(ExiledVariants.DRESS[ExiledVariants.of(data.variant)], 1.6F);
		level.sendParticles(dust, player.getX(), player.getY() + 1, player.getZ(), 80, 0.8, 1.0, 0.8, 0.0);
		level.sendParticles(ParticleTypes.SNOWFLAKE, player.getX(), player.getY() + 1, player.getZ(), 60, 0.6, 1.0, 0.6, 0.05);
		level.sendParticles(ParticleTypes.END_ROD, player.getX(), player.getY() + 1, player.getZ(), 40, 0.5, 1.0, 0.5, 0.1);
		ExiledQuests.momentCome(player, data);
	}

	/** Back where they were (their pixie with them); there the memory holds them till it is over. */
	private static void back(ServerPlayer player, ExiledPixieData data) {
		ServerLevel to = null;
		Identifier key = Identifier.tryParse(data.visionLevel);
		if (key != null) {
			to = player.level().getServer().getLevel(ResourceKey.create(Registries.DIMENSION, key));
		}
		Vec3 at = data.visionFrom;
		if (to == null || at == null) {
			to = player.level().getServer().overworld();
			at = Vec3.atBottomCenterOf(to.getRespawnData().pos());
			data.visionLevel = to.dimension().identifier().toString();
			data.visionFrom = at;
		}
		// (her memories' music ends with her glade)
		com.iafabric.boss.Soundtracks.forget(player);
		player.teleportTo(to, at.x, at.y, at.z, Set.of(), data.visionYaw, 0, false);
		player.resetFallDistance();
	}

	/** The memory over: their screen theirs again, her word (if it ran to its end), and what they let go of in it theirs again. */
	private static void end(ServerPlayer player, ExiledPixieData data, boolean talk) {
		IafNetwork.sendToPlayer(new MessageExiledMemory(MessageExiledMemory.OFF), player);
		com.iafabric.boss.Soundtracks.forget(player);
		data.visionFrom = null;
		data.visionLevel = "";
		ExiledPixies.save(player, data);
		List<ItemStack> kept = KEPT.remove(player.getUUID());
		if (kept != null) {
			kept.forEach(stack -> player.getInventory().placeItemBackInInventory(stack, Prediction.SERVER_ONLY));
		}
		if (talk) {
			ExiledPixies.say(player, Component.translatable("exiled.iceandfire_fabric.vision.back"), MessageExiledSpeech.TALK);
		}
	}

	/** A player left in her glade (they left the game in it): back where they came from. */
	static void strayed(ServerPlayer player, ExiledPixieData data) {
		if (inGlade(player) && !RUNNING.containsKey(player.getUUID())) {
			back(player, data);
			end(player, data, false);
		}
	}

	/** Her glade: an island floating in her pastel sky, grass and flowers, a blossoming tree, pixie houses, her ring. */
	private static void build(ServerLevel glade, int variant, RandomSource random) {
		for (int cx = -2; cx <= 1; cx++) {
			for (int cz = -2; cz <= 1; cz++) {
				glade.getChunk(cx, cz);
			}
		}
		if (glade.getBlockState(CENTER.below(3)).is(Blocks.GRASS_BLOCK) || glade.getBlockState(CENTER.below()).is(Blocks.GRASS_BLOCK)) {
			// made before: only her ring is hers again
			FairyCheckpoints.remove(glade, CENTER);
			FairyCheckpoints.build(glade, CENTER, variant, 2, true);
			return;
		}
		int radius = 13;
		for (int x = -radius; x <= radius; x++) {
			for (int z = -radius; z <= radius; z++) {
				double r = Math.sqrt(x * x + z * z) + random.nextDouble() * 0.8;
				if (r > radius) {
					continue;
				}
				int depth = 2 + (int) ((radius - r) * 0.9 + random.nextDouble() * 2);
				for (int d = 0; d < depth; d++) {
					BlockPos at = CENTER.offset(x, -1 - d, z);
					Block block = d == 0 ? Blocks.GRASS_BLOCK : d < 3 ? Blocks.DIRT : d < depth - 1 ? Blocks.STONE : Blocks.MOSSY_COBBLESTONE;
					glade.setBlock(at, block.defaultBlockState(), 2);
				}
				// flowers on the grass, paler toward the middle
				if (r > 3.5 && random.nextInt(3) == 0) {
					Block[] flowers = {Blocks.LILY_OF_THE_VALLEY, Blocks.AZURE_BLUET, Blocks.OXEYE_DAISY, Blocks.ALLIUM, Blocks.CORNFLOWER, Blocks.PINK_TULIP,
						Blocks.WHITE_TULIP, Blocks.SHORT_GRASS};
					glade.setBlock(CENTER.offset(x, 0, z), flowers[random.nextInt(flowers.length)].defaultBlockState(), 2);
				}
			}
		}
		// a blossoming tree behind her
		BlockPos tree = CENTER.offset(-5, 0, -6);
		for (int y = 0; y < 7; y++) {
			glade.setBlock(tree.above(y), Blocks.CHERRY_LOG.defaultBlockState(), 2);
		}
		for (int x = -4; x <= 4; x++) {
			for (int y = 4; y <= 9; y++) {
				for (int z = -4; z <= 4; z++) {
					double d = Math.sqrt(x * x + (y - 7) * (y - 7) * 1.6 + z * z);
					if (d < 4.2 && random.nextInt(9) != 0) {
						BlockPos at = tree.offset(x, y, z);
						if (glade.getBlockState(at).isAir()) {
							glade.setBlock(at, Blocks.CHERRY_LEAVES.defaultBlockState().setValue(net.minecraft.world.level.block.LeavesBlock.PERSISTENT, true), 2);
						}
					}
				}
			}
		}
		// her sisters' houses round the edge
		Block[] houses = {IafBlockRegistry.PIXIE_HOUSE_MUSHROOM_RED, IafBlockRegistry.PIXIE_HOUSE_BIRCH, IafBlockRegistry.PIXIE_HOUSE_MUSHROOM_BROWN,
			IafBlockRegistry.PIXIE_HOUSE_OAK};
		int[][] spots = {{7, -4}, {-8, 3}, {4, 8}, {9, 4}};
		for (int i = 0; i < spots.length; i++) {
			BlockState house = houses[i].defaultBlockState();
			glade.setBlock(CENTER.offset(spots[i][0], 0, spots[i][1]), house, 2);
		}
		// soft light under the grass
		for (int i = 0; i < 10; i++) {
			double a = Math.PI * 2 * i / 10;
			BlockPos light = CENTER.offset((int) Math.round(Math.cos(a) * 8), -1, (int) Math.round(Math.sin(a) * 8));
			glade.setBlock(light, Blocks.SHROOMLIGHT.defaultBlockState(), 2);
			glade.setBlock(light.above(), Blocks.MOSS_CARPET.defaultBlockState(), 2);
		}
		FairyCheckpoints.build(glade, CENTER, variant, 2, true);
		IafMod.LOGGER.info("Her glade made at {}", CENTER.toShortString());
	}

	/** A player holds her gift (the cold of Cryona and Black Frost holds them less). */
	public static boolean iceGift(net.minecraft.world.entity.LivingEntity entity) {
		return entity instanceof ServerPlayer player && ExiledPixies.get(player).hasGift(ExiledPixieData.GIFT_ICE);
	}
}

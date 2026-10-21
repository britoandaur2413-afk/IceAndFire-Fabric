package com.iafabric.pixie;

import com.iafabric.IafConfig;
import com.iafabric.IafMod;
import com.iafabric.entity.EntityDragonBase;
import com.iafabric.entity.EntityExiledPixie;
import com.iafabric.entity.EntityPixie;
import com.iafabric.entity.IafEntityRegistry;
import com.iafabric.message.MessageExiledAction;
import com.iafabric.message.MessageExiledSpeech;
import com.iafabric.message.MessageExiledSync;
import com.iafabric.misc.IafSoundRegistry;
import com.iafabric.network.IafNetwork;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BiomeTags;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.StructureTags;
import net.minecraft.util.Mth;
import net.minecraft.world.RandomizableContainer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Exiled pixies. The first tree a player cuts (a real one, with its leaves) was the house of a pixie faded half to
 * white, one of twenty: she comes out angry ("Drat! You wrecked my little house..."), and once they speak to her she
 * is theirs for the rest of their adventure. She stays by them (made again wherever they go, never saved with the
 * land), talks now and then (as much as they choose), warns them of villages and tells them of hidden chests, offers
 * her quests ({@link ExiledQuests}) and, while she is out, gives them a share of the gifts of all the pixies at once
 * (a tenth at her first level, half at her last) and her own: a thousand years of memory (the chests hidden around).
 */
public final class ExiledPixies {
	public static final AttachmentType<ExiledPixieData> DATA = AttachmentRegistry.create(IafMod.stored("exiled_pixie"), builder -> builder
		.persistent(CompoundTag.CODEC.xmap(ExiledPixieData::fromTag, ExiledPixieData::toTag))
		.copyOnDeath()
		.initializer(ExiledPixieData::new));
	/** Her idle remarks (exiled.iceandfire_fabric.idle.N). */
	public static final int IDLE = 16;
	/** How near her player she gives her gifts. */
	private static final double NEAR = 32;

	/** One-off remarks (bits of {@link ExiledPixieData#said}). */
	private static final int NIGHT = 0;
	private static final int SNOW = 1;
	private static final int DESERT = 2;
	private static final int OCEAN = 3;
	private static final int NETHER = 4;
	private static final int END = 5;
	private static final int PIXIE = 6;
	private static final int DRAGONBONE = 7;
	private static final int JUNGLE = 8;

	/** Each player's living pixie (her entity's id). */
	private static final Map<UUID, UUID> OUT = new HashMap<>();
	/** Lines waiting to be said, by player. */
	private static final Map<UUID, List<Queued>> QUEUE = new HashMap<>();
	/** When each player's last remark was made, and their last alarms (low life, a dragon near). */
	private static final Map<UUID, Long> LAST_REMARK = new HashMap<>();
	private static final Map<UUID, Long> LAST_HURT_WARNING = new HashMap<>();
	private static final Map<UUID, Long> LAST_DRAGON_WARNING = new HashMap<>();
	private static final Map<UUID, Long> LAST_CHEST_SENSE = new HashMap<>();
	/** Hidden chests she shows (each player's, until when). */
	private static final Map<UUID, Beams> BEAMS = new HashMap<>();
	/** The last state sent to each player (only a change is sent again). */
	private static final Map<UUID, CompoundTag> SENT = new HashMap<>();

	private record Queued(long at, Component text, int kind) {
	}

	private record Beams(List<BlockPos> chests, long until) {
	}

	private ExiledPixies() {
	}

	public static void register() {
		PlayerBlockBreakEvents.AFTER.register((level, player, pos, state, blockEntity) -> {
			if (player instanceof ServerPlayer server && state.is(BlockTags.LOGS)) {
				treeCut(server, pos);
			}
		});
		ServerTickEvents.END_SERVER_TICK.register(ExiledPixies::tick);
		FairyVision.register();
		ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
			SENT.remove(handler.player.getUUID());
			// left the game in her glade: back where they came from
			FairyVision.strayed(handler.player, get(handler.player));
		});
		ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
			UUID id = handler.player.getUUID();
			EntityExiledPixie pixie = living(handler.player);
			if (pixie != null) {
				pixie.discard();
			}
			OUT.remove(id);
			QUEUE.remove(id);
			SENT.remove(id);
			BEAMS.remove(id);
			ExiledQuests.forget(id);
			ExiledTalk.forget(id);
			DuelTalk.forget(id);
		});
		ServerLivingEntityEvents.AFTER_DEATH.register((victim, source) -> {
			if (source.getEntity() instanceof ServerPlayer player && !(victim instanceof EntityPixie)) {
				ExiledPixieData data = get(player);
				EntityExiledPixie pixie = living(player);
				if (data.stage == ExiledPixieData.FRIEND && pixie != null && pixie.distanceTo(player) <= NEAR) {
					addXp(player, data, Mth.clamp((int) (victim.getMaxHealth() / 10), 1, 10));
				}
				ExiledQuests.killed(player, data, victim);
			}
		});
	}

	public static ExiledPixieData get(ServerPlayer player) {
		return player.getAttachedOrCreate(DATA);
	}

	static void save(ServerPlayer player, ExiledPixieData data) {
		player.setAttached(DATA, data);
	}

	// --- meeting her ---

	/** A log cut: if it was the player's first tree, its pixie comes out. */
	public static void treeCut(ServerPlayer player, BlockPos pos) {
		ExiledPixieData data = get(player);
		if (data.met() || player.isSpectator() || !IafConfig.exiledPixie || !isTree(player.level(), pos)) {
			return;
		}
		meet(player, pos);
	}

	/** A tree's log: leaves that grew (not put by a player) about it. */
	public static boolean isTree(Level level, BlockPos log) {
		int leaves = 0;
		for (BlockPos at : BlockPos.betweenClosed(log.offset(-3, -1, -3), log.offset(3, 7, 3))) {
			BlockState state = level.getBlockState(at);
			if (state.getBlock() instanceof LeavesBlock && !state.getValue(LeavesBlock.PERSISTENT) && ++leaves >= 3) {
				return true;
			}
		}
		return false;
	}

	/** Her house is broken: she comes out of it, angry, and the player must take care of her. */
	public static void meet(ServerPlayer player, BlockPos house) {
		ServerLevel level = (ServerLevel) player.level();
		ExiledPixieData data = get(player);
		data.variant = ExiledVariants.next(level.getRandom());
		data.stage = ExiledPixieData.ANGRY;
		data.out = true;
		data.home = house.immutable();
		data.metAt = level.getGameTime();
		save(player, data);
		// (her name and look reach the player's screen before her first words)
		sync(player, false);
		Vec3 at = Vec3.atCenterOf(house).add(0, 0.4, 0);
		EntityExiledPixie pixie = spawn(player, data, at);
		if (pixie != null) {
			pixie.setDeltaMovement(0, 0.3, 0);
		}
		level.sendParticles(ParticleTypes.END_ROD, at.x, at.y, at.z, 30, 0.4, 0.5, 0.4, 0.06);
		level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, level.getBlockState(house.above()).isAir()
			? net.minecraft.world.level.block.Blocks.OAK_LEAVES.defaultBlockState() : level.getBlockState(house.above())), at.x, at.y + 0.5, at.z, 24, 0.5, 0.5, 0.5, 0.1);
		level.playSound(null, house, IafSoundRegistry.PIXIE_HURT, SoundSource.NEUTRAL, 1F, 1.3F);
		level.playSound(null, house, IafSoundRegistry.PIXIE_TAUNT, SoundSource.NEUTRAL, 1F, 1F);
		Component name = Component.literal(ExiledVariants.name(data.variant));
		say(player, Component.translatable("exiled.iceandfire_fabric.meet.1"), MessageExiledSpeech.ALARM);
		sayLater(player, 50, Component.translatable("exiled.iceandfire_fabric.meet.2"), MessageExiledSpeech.TALK);
		sayLater(player, 140, Component.translatable("exiled.iceandfire_fabric.meet.hint", name), MessageExiledSpeech.HINT);
		IafMod.LOGGER.info("{} met {}, an exiled pixie, at {}", player.getName().getString(), ExiledVariants.name(data.variant), house.toShortString());
		sync(player, false);
	}

	// --- her body ---

	/** The player's pixie out, if she is in their world. */
	public static @Nullable EntityExiledPixie living(ServerPlayer player) {
		UUID id = OUT.get(player.getUUID());
		if (id == null) {
			return null;
		}
		Entity entity = ((ServerLevel) player.level()).getEntity(id);
		return entity instanceof EntityExiledPixie pixie && pixie.isAlive() ? pixie : null;
	}

	/** Whether this pixie is the one her player has out (an older copy, or one without a player, goes). */
	public static boolean isCurrent(EntityExiledPixie pixie) {
		LivingEntity owner = pixie.getOwner();
		return owner instanceof ServerPlayer player && pixie.getUUID().equals(OUT.get(player.getUUID()));
	}

	private static @Nullable EntityExiledPixie spawn(ServerPlayer player, ExiledPixieData data, Vec3 at) {
		ServerLevel level = (ServerLevel) player.level();
		EntityExiledPixie old = living(player);
		if (old != null) {
			old.discard();
		}
		EntityExiledPixie pixie = IafEntityRegistry.EXILED_PIXIE.create(level, EntitySpawnReason.MOB_SUMMONED);
		if (pixie == null) {
			return null;
		}
		pixie.setVariant(data.variant);
		pixie.setTame(true, false);
		pixie.setOwner(player);
		pixie.setCustomName(Component.literal(ExiledVariants.name(data.variant)));
		pixie.setAngry(data.stage == ExiledPixieData.ANGRY);
		pixie.snapTo(at.x, at.y, at.z, player.getYRot(), 0);
		OUT.put(player.getUUID(), pixie.getUUID());
		level.addFreshEntity(pixie);
		return pixie;
	}

	/**
	 * She comes along at once where her player was taken (her memory's way there and back, their eyes closed): before
	 * their eyes already, facing them, as she is when she speaks (not behind them, flying past their face).
	 */
	static void bring(ServerPlayer player) {
		ExiledPixieData data = get(player);
		if (!data.met() || !data.out || !player.isAlive() || player.isSpectator()) {
			return;
		}
		float yaw = player.getYRot() * Mth.DEG_TO_RAD;
		Vec3 right = new Vec3(-Math.cos(yaw), 0, -Math.sin(yaw));
		Vec3 forward = new Vec3(-Math.sin(yaw), 0, Math.cos(yaw));
		Vec3 at = player.getEyePosition().add(forward.scale(2.3)).add(right.scale(-0.75)).add(0, -0.25, 0);
		EntityExiledPixie pixie = spawn(player, data, at);
		if (pixie != null) {
			pixie.setYRot(player.getYRot() + 180);
			pixie.yBodyRot = pixie.getYRot();
			pixie.setYHeadRot(pixie.getYRot());
			pixie.speak(40);
		}
	}

	/** She is out with her player (made by them if she is not, or is far, or in another world), or not. */
	private static @Nullable EntityExiledPixie presence(ServerPlayer player, ExiledPixieData data) {
		EntityExiledPixie pixie = living(player);
		boolean wanted = data.met() && data.out && player.isAlive() && !player.isSpectator();
		if (!wanted) {
			if (pixie != null) {
				sparkle((ServerLevel) pixie.level(), pixie, 10);
				pixie.discard();
			}
			OUT.remove(player.getUUID());
			return null;
		}
		if (pixie == null || pixie.distanceTo(player) > 48) {
			Vec3 at = player.getEyePosition().add(player.getLookAngle().scale(-1.2)).add(0, 0.3, 0);
			pixie = spawn(player, data, at);
			if (pixie != null) {
				sparkle((ServerLevel) pixie.level(), pixie, 12);
			}
		}
		return pixie;
	}

	/** A word with her: her menu (the first time, their meeting). */
	public static void interact(ServerPlayer player, EntityExiledPixie pixie) {
		sync(player, true);
	}

	// --- what the player does ---

	public static void act(ServerPlayer player, int action, int arg) {
		ExiledPixieData data = get(player);
		if (!data.met()) {
			if (action == MessageExiledAction.OPEN) {
				player.sendOverlayMessage(Component.translatable("exiled.iceandfire_fabric.not_met"));
			}
			return;
		}
		long now = player.level().getGameTime();
		switch (action) {
			case MessageExiledAction.OPEN -> sync(player, true);
			case MessageExiledAction.ADOPT -> adopt(player, data);
			case MessageExiledAction.TALK -> talk(player, data);
			case MessageExiledAction.ACCEPT -> {
				unsayHints(player);
				ExiledQuests.accept(player, data, ExiledQuest.of(arg));
			}
			case MessageExiledAction.LATER -> {
				unsayHints(player);
				ExiledQuests.later(player, data, ExiledQuest.of(arg));
			}
			case MessageExiledAction.DROP -> ExiledQuests.drop(player, data);
			case MessageExiledAction.SUMMON -> {
				if (!data.out) {
					data.out = true;
					save(player, data);
					presence(player, data);
					say(player, Component.translatable("exiled.iceandfire_fabric.summon"), MessageExiledSpeech.TALK);
				}
			}
			case MessageExiledAction.REST -> {
				if (data.out && data.stage == ExiledPixieData.FRIEND) {
					say(player, Component.translatable("exiled.iceandfire_fabric.rest"), MessageExiledSpeech.TALK);
					data.out = false;
					save(player, data);
					presence(player, data);
					gifts(player, data, false);
				}
			}
			case MessageExiledAction.GUIDE -> {
				data.guide = !data.guide;
				save(player, data);
				if (!data.guide) {
					EntityExiledPixie pixie = living(player);
					if (pixie != null) {
						pixie.focus(null, 0);
					}
				}
			}
			case MessageExiledAction.ABILITY -> ability(player, data, now);
			case MessageExiledAction.TALK_MODE -> {
				data.talk = Mth.clamp(arg, 0, 2);
				data.nextComment = now + idleWait(data, player.getRandom());
				save(player, data);
			}
			default -> {
			}
		}
		sync(player, false);
	}

	/** The first word: she is theirs now. */
	private static void adopt(ServerPlayer player, ExiledPixieData data) {
		if (data.stage != ExiledPixieData.ANGRY) {
			return;
		}
		long now = player.level().getGameTime();
		unsayHints(player);
		data.stage = ExiledPixieData.FRIEND;
		data.nextOffer = now + 300;
		data.nextComment = now + 6000;
		save(player, data);
		EntityExiledPixie pixie = living(player);
		if (pixie != null) {
			pixie.setAngry(false);
			sparkle((ServerLevel) pixie.level(), pixie, 25);
		}
		player.level().playSound(null, player.blockPosition(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 1F, 1.2F);
		say(player, Component.translatable("exiled.iceandfire_fabric.adopt", Component.literal(ExiledVariants.name(data.variant))), MessageExiledSpeech.TALK);
		sayLater(player, 90, Component.translatable("exiled.iceandfire_fabric.motto"), MessageExiledSpeech.TALK);
		sayLater(player, 180, Component.translatable("exiled.iceandfire_fabric.menu_hint", keyName()), MessageExiledSpeech.HINT);
	}

	/** "Tell me something": about the quest under way if there is one, else one of her remarks. */
	private static void talk(ServerPlayer player, ExiledPixieData data) {
		if (data.stage != ExiledPixieData.FRIEND) {
			return;
		}
		ExiledQuest quest = data.current();
		if (quest != null && player.getRandom().nextInt(3) > 0) {
			say(player, ExiledQuests.goal(player, data, quest), MessageExiledSpeech.QUEST);
			return;
		}
		idle(player, data);
	}

	// --- her lines ---

	/** A line of hers on her player's screen; she comes before their eyes to say it (a hint is said from where she is). */
	public static void say(ServerPlayer player, Component text, int kind) {
		if (FairyVision.quiet(player)) {
			// (a memory plays: her other words wait till it is over)
			sayLater(player, 1, text, kind);
			return;
		}
		IafNetwork.sendToPlayer(new MessageExiledSpeech(text, kind), player);
		EntityExiledPixie pixie = living(player);
		if (pixie != null && kind != MessageExiledSpeech.HINT && !pixie.isAngry()) {
			pixie.speak(30 + Math.min(70, text.getString().length()));
		}
	}

	public static void sayLater(ServerPlayer player, int ticks, Component text, int kind) {
		QUEUE.computeIfAbsent(player.getUUID(), id -> new ArrayList<>()).add(new Queued(player.level().getGameTime() + ticks, text, kind));
	}

	/** The hints still to say go (the player answered: how to answer is no news). */
	static void unsayHints(ServerPlayer player) {
		List<Queued> lines = QUEUE.get(player.getUUID());
		if (lines != null) {
			lines.removeIf(line -> line.kind() == MessageExiledSpeech.HINT);
		}
	}

	/** The key that opens her menu, as the player has it (a key name the client shows). */
	static Component keyName() {
		return Component.keybind("key.iceandfire_fabric.exiled_pixie");
	}

	// --- the server's beat ---

	private static void tick(MinecraftServer server) {
		long now = server.overworld().getGameTime();
		if (!QUEUE.isEmpty()) {
			for (ServerPlayer player : server.getPlayerList().getPlayers()) {
				List<Queued> lines = QUEUE.get(player.getUUID());
				if (lines == null || FairyVision.inMemory(player)) {
					continue;
				}
				for (Iterator<Queued> it = lines.iterator(); it.hasNext(); ) {
					Queued line = it.next();
					if (line.at() <= now) {
						say(player, line.text(), line.kind());
						it.remove();
						break;
					}
				}
				if (lines.isEmpty()) {
					QUEUE.remove(player.getUUID());
				}
			}
		}
		ExiledQuests.searchTick(server);
		FairyVision.tick(server);
		if (server.getTickCount() % 10 == 0) {
			beams(server, now);
			ExiledQuests.pillars(server);
		}
		if (server.getTickCount() % 20 != 0) {
			return;
		}
		for (ServerPlayer player : server.getPlayerList().getPlayers()) {
			ExiledPixieData data = get(player);
			if (!data.met()) {
				continue;
			}
			EntityExiledPixie pixie = presence(player, data);
			boolean near = pixie != null && pixie.distanceTo(player) <= NEAR;
			gifts(player, data, data.stage == ExiledPixieData.FRIEND && near);
			if (data.stage == ExiledPixieData.FRIEND && !FairyVision.inMemory(player)) {
				if (com.iafabric.boss.ThroneFight.inRealm(player)) {
					// in Cryona's duel she speaks only of the fight (DuelTalk); her other remarks wait till it is over
					if (pixie != null && player.isAlive() && player.getHealth() <= player.getMaxHealth() * 0.3F) {
						DuelTalk.say(player, DuelTalk.Moment.HURT);
					}
				} else {
					FairyVision.check(player, data, now);
					if (pixie != null) {
						remarks(player, data, pixie, now);
					}
				}
				ExiledQuests.tick(player, data, pixie, now);
			}
			sync(player, false);
		}
	}

	// --- her gifts ---

	/** A share of the gifts of all the pixies at once (their stats at their last level), while she is out and near. */
	static void gifts(ServerPlayer player, ExiledPixieData data, boolean giving) {
		float share = data.share();
		for (PixieElement element : PixieElement.values()) {
			for (PixieElement.Bonus bonus : element.bonuses) {
				AttributeInstance instance = player.getAttribute(bonus.attribute());
				if (instance == null) {
					continue;
				}
				Identifier id = IafMod.id("exiled_pixie_" + bonus.attribute().unwrapKey().map(k -> k.identifier().getPath()).orElse("stat"));
				double value = giving ? bonus.atFive() * share : 0;
				AttributeModifier current = instance.getModifier(id);
				if (value == 0) {
					if (current != null) {
						instance.removeModifier(id);
					}
				} else if (current == null || current.amount() != value) {
					instance.addOrUpdateTransientModifier(new AttributeModifier(id, value, bonus.operation()));
				}
			}
		}
	}

	/** Experience for her bond, with its levels (each gives more of the pixies' gifts). */
	static void addXp(ServerPlayer player, ExiledPixieData data, int amount) {
		if (amount <= 0 || data.stage != ExiledPixieData.FRIEND) {
			return;
		}
		data.xp += amount;
		while (data.level < 5 && data.xp >= ExiledPixieData.LEVEL_XP[data.level - 1]) {
			data.level++;
			say(player, Component.translatable("exiled.iceandfire_fabric.level_up", data.level, Math.round(data.share() * 100)), MessageExiledSpeech.QUEST);
			player.level().playSound(null, player.blockPosition(), SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 0.6F, 1.7F);
			EntityExiledPixie pixie = living(player);
			if (pixie != null) {
				sparkle((ServerLevel) pixie.level(), pixie, 30);
			}
		}
		save(player, data);
	}

	// --- her own gift: a thousand years of memory ---

	/** How long her memory rests after it was used, by her level (five minutes at her first, under two at her last). */
	public static int abilityCooldown(int level) {
		return 6000 - 1000 * (Mth.clamp(level, 1, 5) - 1);
	}

	/** How far she remembers (blocks), by her level. */
	public static int abilityRange(int level) {
		return 24 + 8 * Mth.clamp(level, 1, 5);
	}

	/** She remembers the chests hidden around (never opened, in loaded land): a beam over each, and she flies to the nearest. */
	private static void ability(ServerPlayer player, ExiledPixieData data, long now) {
		EntityExiledPixie pixie = living(player);
		if (data.stage != ExiledPixieData.FRIEND) {
			return;
		}
		if (pixie == null) {
			player.sendOverlayMessage(Component.translatable("exiled.iceandfire_fabric.ability.resting"));
			return;
		}
		if (now < data.abilityReady) {
			player.sendOverlayMessage(Component.translatable("exiled.iceandfire_fabric.ability.wait", (data.abilityReady - now + 19) / 20));
			return;
		}
		ServerLevel level = (ServerLevel) player.level();
		List<BlockPos> chests = hiddenChests(level, player.blockPosition(), abilityRange(data.level));
		player.level().playSound(null, player.blockPosition(), SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.PLAYERS, 1F, 1.4F);
		if (chests.isEmpty()) {
			data.abilityReady = now + 600;
			save(player, data);
			say(player, Component.translatable("exiled.iceandfire_fabric.ability.none"), MessageExiledSpeech.TALK);
			return;
		}
		data.abilityReady = now + abilityCooldown(data.level);
		save(player, data);
		List<BlockPos> shown = chests.subList(0, Math.min(3, chests.size()));
		show(player, pixie, shown, 260);
		int distance = (int) Math.sqrt(shown.get(0).distSqr(player.blockPosition()));
		say(player, Component.translatable("exiled.iceandfire_fabric.ability.found", chests.size(), distance), MessageExiledSpeech.TALK);
	}

	/**
	 * She shows these chests: a beam of light over each for a while, the nearest one marked on the screen, and she
	 * flies to it (over the ground above it when it is buried: she passes through earth, but would be seen no more).
	 */
	static void show(ServerPlayer player, EntityExiledPixie pixie, List<BlockPos> chests, int ticks) {
		long until = player.level().getGameTime() + ticks;
		BEAMS.put(player.getUUID(), new Beams(new ArrayList<>(chests), until));
		BlockPos chest = chests.get(0);
		Level level = player.level();
		double y = chest.getY() + 1.3;
		if (!level.getBlockState(chest.above()).isAir() || !level.getBlockState(chest.above(2)).isAir()) {
			y = Math.max(y, level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, chest.getX(), chest.getZ()) + 1.5);
		}
		pixie.focus(new Vec3(chest.getX() + 0.5, y, chest.getZ() + 0.5), ticks);
		ExiledQuests.hint(player, chest, until);
	}

	/** She stops showing chests (their beams go; she comes back to her player). */
	static void unshow(ServerPlayer player) {
		BEAMS.remove(player.getUUID());
		EntityExiledPixie pixie = living(player);
		if (pixie != null) {
			pixie.focus(null, 0);
		}
	}

	/** Chests and other containers never opened (their loot not made yet) within a distance, the nearest first. */
	public static List<BlockPos> hiddenChests(ServerLevel level, BlockPos from, int radius) {
		List<BlockPos> found = new ArrayList<>();
		int minX = (from.getX() - radius) >> 4;
		int maxX = (from.getX() + radius) >> 4;
		int minZ = (from.getZ() - radius) >> 4;
		int maxZ = (from.getZ() + radius) >> 4;
		double max = (double) radius * radius;
		for (int cx = minX; cx <= maxX; cx++) {
			for (int cz = minZ; cz <= maxZ; cz++) {
				LevelChunk chunk = level.getChunkSource().getChunkNow(cx, cz);
				if (chunk == null) {
					continue;
				}
				for (BlockEntity entity : chunk.getBlockEntities().values()) {
					if (entity instanceof RandomizableContainer container && container.getLootTable() != null
						&& entity.getBlockPos().distSqr(from) <= max) {
						found.add(entity.getBlockPos().immutable());
					}
				}
			}
		}
		found.sort(Comparator.comparingDouble(pos -> pos.distSqr(from)));
		return found;
	}

	/** The beams over the chests shown: from each chest up to the open sky over it (a chest found or gone goes out). */
	private static void beams(MinecraftServer server, long now) {
		if (BEAMS.isEmpty()) {
			return;
		}
		for (Iterator<Map.Entry<UUID, Beams>> it = BEAMS.entrySet().iterator(); it.hasNext(); ) {
			Map.Entry<UUID, Beams> entry = it.next();
			ServerPlayer player = server.getPlayerList().getPlayer(entry.getKey());
			if (player == null || now > entry.getValue().until()) {
				it.remove();
				continue;
			}
			ServerLevel level = (ServerLevel) player.level();
			entry.getValue().chests().removeIf(pos -> !(level.getBlockEntity(pos) instanceof RandomizableContainer c) || c.getLootTable() == null);
			if (entry.getValue().chests().isEmpty()) {
				it.remove();
				continue;
			}
			for (BlockPos chest : entry.getValue().chests()) {
				int top = Math.max(chest.getY() + 3, level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, chest.getX(), chest.getZ()) + 6);
				for (int y = chest.getY() + 1; y <= top; y += 2) {
					level.sendParticles(player, ParticleTypes.END_ROD, true, true, chest.getX() + 0.5, y, chest.getZ() + 0.5, 1, 0.05, 0.4, 0.05, 0.0);
				}
				level.sendParticles(player, ParticleTypes.ENCHANT, true, true, chest.getX() + 0.5, chest.getY() + 1.2, chest.getZ() + 0.5, 6, 0.3, 0.3, 0.3, 0.4);
			}
		}
	}

	// --- what she says on her own ---

	/** The least time between two of her remarks, as much as the player wants her to talk. */
	private static long gap(ExiledPixieData data) {
		return switch (data.talk) {
			case 0 -> 2400;
			case 2 -> 500;
			default -> 900;
		};
	}

	/** How long until her next idle remark, as much as the player wants her to talk. */
	private static long idleWait(ExiledPixieData data, net.minecraft.util.RandomSource random) {
		return switch (data.talk) {
			case 0 -> 24000 + random.nextInt(6000);
			case 2 -> 3600 + random.nextInt(2400);
			default -> 8400 + random.nextInt(4800);
		};
	}

	/** One of her idle remarks, not one of her last few. */
	private static void idle(ServerPlayer player, ExiledPixieData data) {
		List<Integer> choices = new ArrayList<>();
		for (int i = 0; i < IDLE; i++) {
			if (!data.recent.contains(i)) {
				choices.add(i);
			}
		}
		int line = choices.get(player.getRandom().nextInt(choices.size()));
		data.recent.add(line);
		while (data.recent.size() > 6) {
			data.recent.remove(0);
		}
		save(player, data);
		say(player, Component.translatable("exiled.iceandfire_fabric.idle." + line), MessageExiledSpeech.TALK);
		LAST_REMARK.put(player.getUUID(), player.level().getGameTime());
	}

	private static boolean once(ServerPlayer player, ExiledPixieData data, int bit, String key) {
		if ((data.said & (1L << bit)) != 0) {
			return false;
		}
		data.said |= 1L << bit;
		save(player, data);
		say(player, Component.translatable(key), MessageExiledSpeech.TALK);
		LAST_REMARK.put(player.getUUID(), player.level().getGameTime());
		return true;
	}

	/**
	 * What she says on her own: an alarm when her player is badly hurt or a wild dragon is near, a warning on entering
	 * a village or a pillagers' kingdom, a hidden chest very near, a word the first time they see a place, on the way
	 * to a village or the witches an anecdote or a joke, a word about a creature near that catches her eye, and now and
	 * then one of her remarks (she looks every five seconds; never two remarks too close, as often as the player chose).
	 */
	private static void remarks(ServerPlayer player, ExiledPixieData data, EntityExiledPixie pixie, long now) {
		UUID id = player.getUUID();
		ServerLevel level = (ServerLevel) player.level();
		if (player.getHealth() <= player.getMaxHealth() * 0.3F && player.isAlive() && now - LAST_HURT_WARNING.getOrDefault(id, -99999L) > 3600) {
			LAST_HURT_WARNING.put(id, now);
			say(player, Component.translatable("exiled.iceandfire_fabric.hurt"), MessageExiledSpeech.ALARM);
			return;
		}
		if (level.getServer().getTickCount() % 100 != 0 || now - LAST_REMARK.getOrDefault(id, -99999L) < gap(data) || QUEUE.containsKey(id)) {
			return;
		}
		if (now - LAST_DRAGON_WARNING.getOrDefault(id, -99999L) > 12000) {
			for (EntityDragonBase dragon : level.getEntitiesOfClass(EntityDragonBase.class, player.getBoundingBox().inflate(48),
				d -> d.isAlive() && !d.isModelDead() && !d.isTame() && d.getDragonStage() >= 3)) {
				LAST_DRAGON_WARNING.put(id, now);
				LAST_REMARK.put(id, now);
				say(player, Component.translatable("exiled.iceandfire_fabric.dragon_near"), MessageExiledSpeech.ALARM);
				return;
			}
		}
		if (warnPlaces(player, data, level)) {
			return;
		}
		if (data.talk > 0 && now - LAST_CHEST_SENSE.getOrDefault(id, -99999L) > 3600) {
			for (BlockPos chest : hiddenChests(level, player.blockPosition(), 10)) {
				if (!data.chests.contains(chest.asLong())) {
					ExiledPixieData.remember(data.chests, chest.asLong(), 64);
					save(player, data);
					LAST_CHEST_SENSE.put(id, now);
					LAST_REMARK.put(id, now);
					show(player, pixie, List.of(chest), 160);
					say(player, Component.translatable("exiled.iceandfire_fabric.sense.chest"), MessageExiledSpeech.TALK);
					return;
				}
			}
		}
		if (firstSights(player, data, level)) {
			return;
		}
		if (ExiledTalk.trip(player, data, now)) {
			LAST_REMARK.put(id, now);
			return;
		}
		boolean due = now >= data.nextComment;
		if (due) {
			data.nextComment = now + idleWait(data, player.getRandom());
			save(player, data);
		}
		if (ExiledTalk.sight(player, data, now, due)) {
			LAST_REMARK.put(id, now);
		} else if (due) {
			idle(player, data);
		}
	}

	/** Entering a village or a pillagers' kingdom she has not warned of. */
	private static boolean warnPlaces(ServerPlayer player, ExiledPixieData data, ServerLevel level) {
		BlockPos at = player.blockPosition();
		StructureStart village = level.structureManager().getStructureWithPieceAt(at, StructureTags.VILLAGE);
		if (village.isValid() && !data.villages.contains(village.getChunkPos().pack())) {
			ExiledPixieData.remember(data.villages, village.getChunkPos().pack(), 32);
			save(player, data);
			if (data.current() == ExiledQuest.VILLAGE) {
				// the village she leads to: her words come at her ring
				return false;
			}
			LAST_REMARK.put(player.getUUID(), level.getGameTime());
			say(player, Component.translatable("exiled.iceandfire_fabric.village." + player.getRandom().nextInt(3)), MessageExiledSpeech.ALARM);
			return true;
		}
		ResourceKey<net.minecraft.world.level.levelgen.structure.Structure> kingdom = ResourceKey.create(net.minecraft.core.registries.Registries.STRUCTURE,
			IafMod.id("pillager_kingdom"));
		StructureStart start = level.structureManager().getStructureWithPieceAt(at, holder -> holder.is(kingdom));
		if (start.isValid() && !data.villages.contains(start.getChunkPos().pack())) {
			ExiledPixieData.remember(data.villages, start.getChunkPos().pack(), 32);
			save(player, data);
			LAST_REMARK.put(player.getUUID(), level.getGameTime());
			say(player, Component.translatable("exiled.iceandfire_fabric.kingdom"), MessageExiledSpeech.ALARM);
			return true;
		}
		return false;
	}

	/** A word the first time: the first night, snow, a desert, the sea, a jungle, the Nether, the End, a wild pixie, a dragonbone weapon. */
	private static boolean firstSights(ServerPlayer player, ExiledPixieData data, ServerLevel level) {
		if (level.dimension() == Level.NETHER) {
			return once(player, data, NETHER, "exiled.iceandfire_fabric.first.nether");
		}
		if (level.dimension() == Level.END) {
			return once(player, data, END, "exiled.iceandfire_fabric.first.end");
		}
		Holder<Biome> biome = level.getBiome(player.blockPosition());
		if (level.dimension() == Level.OVERWORLD && level.isDarkOutside() && level.getGameTime() - data.metAt > 1200
			&& once(player, data, NIGHT, "exiled.iceandfire_fabric.first.night")) {
			return true;
		}
		if (biome.value().coldEnoughToSnow(player.blockPosition(), level.getSeaLevel()) && once(player, data, SNOW, "exiled.iceandfire_fabric.first.snow")) {
			return true;
		}
		if (biome.value().getBaseTemperature() >= 1.9F && !biome.is(BiomeTags.IS_BADLANDS) && once(player, data, DESERT, "exiled.iceandfire_fabric.first.desert")) {
			return true;
		}
		if ((biome.is(BiomeTags.IS_OCEAN) || biome.is(BiomeTags.IS_BEACH)) && once(player, data, OCEAN, "exiled.iceandfire_fabric.first.ocean")) {
			return true;
		}
		if (biome.is(BiomeTags.IS_JUNGLE) && once(player, data, JUNGLE, "exiled.iceandfire_fabric.first.jungle")) {
			return true;
		}
		if (!level.getEntitiesOfClass(EntityPixie.class, player.getBoundingBox().inflate(16), p -> !(p instanceof EntityExiledPixie) && !p.isTame()).isEmpty()
			&& once(player, data, PIXIE, "exiled.iceandfire_fabric.first.pixie")) {
			return true;
		}
		ItemStack hand = player.getMainHandItem();
		return !hand.isEmpty() && net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(hand.getItem()).getPath().startsWith("dragonbone_")
			&& once(player, data, DRAGONBONE, "exiled.iceandfire_fabric.first.dragonbone");
	}

	// --- her state, for the player's screen ---

	public static void sync(ServerPlayer player, boolean open) {
		ExiledPixieData data = get(player);
		CompoundTag tag = data.toTag();
		// which of her lines she said is hers alone
		tag.remove("Told");
		tag.put("Extra", ExiledQuests.extra(player, data));
		CompoundTag last = SENT.get(player.getUUID());
		if (!open && tag.equals(last)) {
			return;
		}
		SENT.put(player.getUUID(), tag);
		IafNetwork.sendToPlayer(new MessageExiledSync(tag, open), player);
	}

	/**
	 * For tests and game masters: she is their friend (met at once if she was not), out with them, all her quests done
	 * and her bond at its last level (ExiledQuests.completeAll).
	 */
	public static void completeAll(ServerPlayer player) {
		ExiledPixieData data = get(player);
		if (!data.met()) {
			data.variant = ExiledVariants.next(((ServerLevel) player.level()).getRandom());
			data.home = player.blockPosition();
			data.metAt = player.level().getGameTime();
		}
		if (data.stage != ExiledPixieData.FRIEND) {
			data.stage = ExiledPixieData.FRIEND;
			data.nextComment = player.level().getGameTime() + 6000;
			EntityExiledPixie pixie = living(player);
			if (pixie != null) {
				pixie.setAngry(false);
			}
		}
		data.out = true;
		ExiledQuests.completeAll(player, data);
		save(player, data);
		presence(player, data);
		sync(player, false);
	}

	/** Forgets the player's pixie (for tests and for a game master): the next tree they cut brings a new one. */
	public static void reset(ServerPlayer player) {
		EntityExiledPixie pixie = living(player);
		if (pixie != null) {
			pixie.discard();
		}
		OUT.remove(player.getUUID());
		QUEUE.remove(player.getUUID());
		ExiledQuests.forget(player.getUUID());
		ExiledTalk.forget(player.getUUID());
		DuelTalk.forget(player.getUUID());
		gifts(player, get(player), false);
		player.setAttached(DATA, new ExiledPixieData());
		sync(player, false);
	}

	static void sparkle(ServerLevel level, Entity pixie, int count) {
		level.sendParticles(ParticleTypes.END_ROD, pixie.getX(), pixie.getY() + 0.3, pixie.getZ(), count, 0.3, 0.3, 0.3, 0.03);
	}
}

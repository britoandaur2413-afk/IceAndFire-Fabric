package com.iafabric.network;

import com.iafabric.IafMod;
import com.iafabric.citadel.animation.IAnimatedEntity;
import com.iafabric.message.*;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.PlayerLookup;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.Packet;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Function;

/**
 * The mod's packets. Each message class keeps its own read/write; each has a channel of its own
 * (1.20.1's networking: a buffer read at once on the network thread, handled on the game's).
 * Messages sent to the server are handled here; messages sent to clients are handled by handlers
 * the client registers in {@link #CLIENT_HANDLERS}.
 */
public final class IafNetwork {
	private static final Map<Class<?>, Channel<?>> CHANNELS = new HashMap<>();
	/** Filled by the client entrypoint: what to do with a message that reached the client. */
	public static final Map<Class<?>, BiConsumer<Object, Player>> CLIENT_HANDLERS = new HashMap<>();
	public static MinecraftServer server;

	private IafNetwork() {
	}

	/** A message's channel: its id, how it is written and read. */
	public record Channel<M>(ResourceLocation id, BiConsumer<M, FriendlyByteBuf> write, Function<FriendlyByteBuf, M> read) {
		@SuppressWarnings("unchecked")
		FriendlyByteBuf encode(Object message) {
			FriendlyByteBuf buf = PacketByteBufs.create();
			write.accept((M) message, buf);
			return buf;
		}
	}

	public interface Handler<M> {
		void handle(M message, Player player);
	}

	private static <M> void register(String name, Class<M> cls, BiConsumer<M, FriendlyByteBuf> write, Function<FriendlyByteBuf, M> read,
		boolean toServer, boolean toClient, Handler<M> serverHandler) {
		Channel<M> channel = new Channel<>(IafMod.id(name), write, read);
		CHANNELS.put(cls, channel);
		if (toServer) {
			ServerPlayNetworking.registerGlobalReceiver(channel.id(), (server, player, handler, buf, sender) -> {
				M message = read.apply(buf);
				server.execute(() -> serverHandler.handle(message, player));
			});
		}
	}

	public static void init() {
		register("daytime", MessageDaytime.class, (m, b) -> MessageDaytime.write(m, b), MessageDaytime::read, false, true, null);
		register("deathworm_hitbox", MessageDeathWormHitbox.class, (m, b) -> MessageDeathWormHitbox.write(m, b), MessageDeathWormHitbox::read, false, true, null);
		register("dragon_control", MessageDragonControl.class, (m, b) -> MessageDragonControl.write(m, b), MessageDragonControl::read, true, false, MessageDragonControl.Handler::handle);
		register("dragon_set_burn_block", MessageDragonSetBurnBlock.class, (m, b) -> MessageDragonSetBurnBlock.write(m, b), MessageDragonSetBurnBlock::read, false, true, null);
		register("dragon_sync_fire", MessageDragonSyncFire.class, (m, b) -> MessageDragonSyncFire.write(m, b), MessageDragonSyncFire::read, true, true, MessageDragonSyncFire.Handler::handle);
		register("get_myrmex_hive", MessageGetMyrmexHive.class, (m, b) -> MessageGetMyrmexHive.write(m, b), MessageGetMyrmexHive::read, true, true, MessageGetMyrmexHive.Handler::handle);
		register("myrmex_settings", MessageMyrmexSettings.class, (m, b) -> MessageMyrmexSettings.write(m, b), MessageMyrmexSettings::read, true, false, MessageMyrmexSettings.Handler::handle);
		register("hippogryph_armor", MessageHippogryphArmor.class, (m, b) -> MessageHippogryphArmor.write(m, b), MessageHippogryphArmor::read, true, false, MessageHippogryphArmor.Handler::handle);
		register("multipart_interact", MessageMultipartInteract.class, (m, b) -> MessageMultipartInteract.write(m, b), MessageMultipartInteract::read, true, false, MessageMultipartInteract.Handler::handle);
		register("player_hit_multipart", MessagePlayerHitMultipart.class, (m, b) -> MessagePlayerHitMultipart.write(m, b), MessagePlayerHitMultipart::read, true, false, MessagePlayerHitMultipart.Handler::handle);
		register("set_myrmex_hive_null", MessageSetMyrmexHiveNull.class, (m, b) -> MessageSetMyrmexHiveNull.write(m, b), MessageSetMyrmexHiveNull::read, false, true, null);
		register("pixie_bond", com.iafabric.message.MessagePixieBond.class, (m, b) -> com.iafabric.message.MessagePixieBond.write(m, b), com.iafabric.message.MessagePixieBond::read, true, false, com.iafabric.message.MessagePixieBond.Handler::handle);
		register("boss_card", MessageBossCard.class, MessageBossCard::write, MessageBossCard::read, false, true, null);
		register("exiled_speech", MessageExiledSpeech.class, MessageExiledSpeech::write, MessageExiledSpeech::read, false, true, null);
		register("exiled_eyes", MessageExiledEyes.class, MessageExiledEyes::write, MessageExiledEyes::read, false, true, null);
		register("exiled_memory", com.iafabric.message.MessageExiledMemory.class, com.iafabric.message.MessageExiledMemory::write, com.iafabric.message.MessageExiledMemory::read, false, true, null);
		register("exiled_sync", MessageExiledSync.class, MessageExiledSync::write, MessageExiledSync::read, false, true, null);
		register("exiled_action", MessageExiledAction.class, MessageExiledAction::write, MessageExiledAction::read, true, false, MessageExiledAction.Handler::handle);
		register("pixie_bond_sync", com.iafabric.message.MessagePixieBondSync.class, (m, b) -> com.iafabric.message.MessagePixieBondSync.write(m, b), com.iafabric.message.MessagePixieBondSync::read, false, true, null);
		register("siren_song", MessageSirenSong.class, (m, b) -> MessageSirenSong.write(m, b), MessageSirenSong::read, true, true, MessageSirenSong.Handler::handle);
		register("spawn_particle_at", MessageSpawnParticleAt.class, (m, b) -> MessageSpawnParticleAt.write(m, b), MessageSpawnParticleAt::read, true, true, MessageSpawnParticleAt.Handler::handle);
		register("start_riding_mob", MessageStartRidingMob.class, (m, b) -> MessageStartRidingMob.write(m, b), MessageStartRidingMob::read, true, true, MessageStartRidingMob.Handler::handle);
		register("update_pixie_house", MessageUpdatePixieHouse.class, (m, b) -> MessageUpdatePixieHouse.write(m, b), MessageUpdatePixieHouse::read, true, true, MessageUpdatePixieHouse.Handler::handle);
		register("update_pixie_house_model", MessageUpdatePixieHouseModel.class, (m, b) -> MessageUpdatePixieHouseModel.write(m, b), MessageUpdatePixieHouseModel::read, false, true, null);
		register("update_pixie_jar", MessageUpdatePixieJar.class, (m, b) -> MessageUpdatePixieJar.write(m, b), MessageUpdatePixieJar::read, false, true, null);
		register("update_podium", MessageUpdatePodium.class, (m, b) -> MessageUpdatePodium.write(m, b), MessageUpdatePodium::read, false, true, null);
		register("update_dragonforge", MessageUpdateDragonforge.class, (m, b) -> MessageUpdateDragonforge.write(m, b), MessageUpdateDragonforge::read, false, true, null);
		register("update_lectern", MessageUpdateLectern.class, (m, b) -> MessageUpdateLectern.write(m, b), MessageUpdateLectern::read, true, true, MessageUpdateLectern.Handler::handle);
		register("sync_path", MessageSyncPath.class, (m, b) -> m.write(b), MessageSyncPath::read, false, true, null);
		register("sync_path_reached", MessageSyncPathReached.class, (m, b) -> m.write(b), MessageSyncPathReached::read, false, true, null);
		register("swing_arm", MessageSwingArm.class, (m, b) -> MessageSwingArm.write(m, b), MessageSwingArm::read, true, false, MessageSwingArm.Handler::handle);
		register("animation", MessageAnimation.class, MessageAnimation::write, MessageAnimation::read, false, true, null);
		register("entity_data", MessageEntityData.class, MessageEntityData::write, MessageEntityData::read, false, true, null);
		register("music", MessageMusic.class, MessageMusic::write, MessageMusic::read, false, true, null);
	}

	private static Channel<?> channel(Object message) {
		Channel<?> channel = CHANNELS.get(message.getClass());
		if (channel == null) {
			throw new IllegalArgumentException("Unregistered message " + message.getClass().getName());
		}
		return channel;
	}

	private static Packet<?> toClient(Object message) {
		Channel<?> channel = channel(message);
		return ServerPlayNetworking.createS2CPacket(channel.id(), channel.encode(message));
	}

	public static void sendToServer(Object message) {
		Channel<?> channel = channel(message);
		ClientSender.sender.accept(channel.id(), channel.encode(message));
	}

	public static void sendToPlayer(Object message, ServerPlayer player) {
		player.connection.send(toClient(message));
	}

	public static void sendToAll(Object message) {
		if (server == null) {
			return;
		}
		Packet<?> packet = toClient(message);
		for (ServerPlayer player : server.getPlayerList().getPlayers()) {
			player.connection.send(packet);
		}
	}

	/** To every player that sees the entity (and the entity itself if it is a player). */
	public static void sendToTracking(Object message, Entity entity) {
		Packet<?> packet = toClient(message);
		for (ServerPlayer player : PlayerLookup.tracking(entity)) {
			player.connection.send(packet);
		}
		if (entity instanceof ServerPlayer self) {
			self.connection.send(packet);
		}
	}

	public static <T extends Entity & IAnimatedEntity> void sendAnimation(T entity, int index) {
		if (!entity.level().isClientSide) {
			sendToTracking(new MessageAnimation(entity.getId(), index), entity);
		}
	}

	/** Set by the client entrypoint; sends a message's buffer to the server on its channel. */
	public static final class ClientSender {
		public static BiConsumer<ResourceLocation, FriendlyByteBuf> sender = (id, buf) -> {
		};
	}

	@SuppressWarnings("unchecked")
	public static <M> Channel<M> channelOf(Class<M> cls) {
		return (Channel<M>) CHANNELS.get(cls);
	}
}

package com.iafabric.network;

import com.iafabric.IafMod;
import com.iafabric.citadel.animation.IAnimatedEntity;
import com.iafabric.message.*;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.PlayerLookup;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Function;

/**
 * The mod's packets. Each message class keeps its own read/write; this wraps them in Fabric
 * payloads. Messages sent to the server are handled here; messages sent to clients are handled
 * by handlers the client registers in {@link #CLIENT_HANDLERS}.
 */
public final class IafNetwork {
	private static final Map<Class<?>, CustomPacketPayload.Type<Packet<?>>> TYPES = new HashMap<>();
	/** Filled by the client entrypoint: what to do with a message that reached the client. */
	public static final Map<Class<?>, BiConsumer<Object, Player>> CLIENT_HANDLERS = new HashMap<>();
	public static MinecraftServer server;

	private IafNetwork() {
	}

	/** A message of the mod wrapped as a payload. */
	public record Packet<M>(CustomPacketPayload.Type<Packet<?>> type, M message) implements CustomPacketPayload {
	}

	public interface Handler<M> {
		void handle(M message, Player player);
	}

	@SuppressWarnings({"unchecked", "rawtypes"})
	private static <M> void register(String name, Class<M> cls, BiConsumer<M, FriendlyByteBuf> write, Function<FriendlyByteBuf, M> read,
		boolean toServer, boolean toClient, Handler<M> serverHandler) {
		CustomPacketPayload.Type<Packet<?>> type = new CustomPacketPayload.Type<>(IafMod.id(name));
		StreamCodec<RegistryFriendlyByteBuf, Packet<?>> codec = StreamCodec.of(
			(buf, packet) -> write.accept((M) packet.message(), buf),
			buf -> new Packet<>(type, read.apply(buf)));
		TYPES.put(cls, type);
		if (toServer) {
			PayloadTypeRegistry.serverboundPlay().register(type, codec);
			ServerPlayNetworking.registerGlobalReceiver(type, (payload, context) -> serverHandler.handle((M) payload.message(), context.player()));
		}
		if (toClient) {
			PayloadTypeRegistry.clientboundPlay().register(type, codec);
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

	private static Packet<?> wrap(Object message) {
		CustomPacketPayload.Type<Packet<?>> type = TYPES.get(message.getClass());
		if (type == null) {
			throw new IllegalArgumentException("Unregistered message " + message.getClass().getName());
		}
		return new Packet<>(type, message);
	}

	public static void sendToServer(Object message) {
		ClientSender.send(wrap(message));
	}

	public static void sendToPlayer(Object message, ServerPlayer player) {
		ServerPlayNetworking.send(player, wrap(message));
	}

	public static void sendToAll(Object message) {
		if (server == null) {
			return;
		}
		Packet<?> packet = wrap(message);
		for (ServerPlayer player : server.getPlayerList().getPlayers()) {
			ServerPlayNetworking.send(player, packet);
		}
	}

	/** To every player that sees the entity (and the entity itself if it is a player). */
	public static void sendToTracking(Object message, Entity entity) {
		Packet<?> packet = wrap(message);
		for (ServerPlayer player : PlayerLookup.tracking(entity)) {
			ServerPlayNetworking.send(player, packet);
		}
		if (entity instanceof ServerPlayer self) {
			ServerPlayNetworking.send(self, packet);
		}
	}

	public static <T extends Entity & IAnimatedEntity> void sendAnimation(T entity, int index) {
		if (!entity.level().isClientSide()) {
			sendToTracking(new MessageAnimation(entity.getId(), index), entity);
		}
	}

	/** Set by the client entrypoint; sends a payload to the server. */
	public static final class ClientSender {
		public static java.util.function.Consumer<CustomPacketPayload> sender = p -> {
		};

		static void send(CustomPacketPayload payload) {
			sender.accept(payload);
		}
	}

	public static CustomPacketPayload.Type<Packet<?>> typeOf(Class<?> cls) {
		return TYPES.get(cls);
	}

	public static Iterable<Map.Entry<Class<?>, CustomPacketPayload.Type<Packet<?>>>> types() {
		return TYPES.entrySet();
	}
}

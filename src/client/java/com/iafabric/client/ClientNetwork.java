package com.iafabric.client;

import com.iafabric.citadel.animation.IAnimatedEntity;
import com.iafabric.citadel.server.entity.CitadelEntityData;
import com.iafabric.message.*;
import com.iafabric.network.IafNetwork;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;

import java.util.function.BiConsumer;

/** What the client does with each packet the server sends it. */
public final class ClientNetwork {
	private ClientNetwork() {
	}

	public static void init() {
		on(MessageDaytime.class, MessageDaytime.Handler::handle);
		on(MessageDeathWormHitbox.class, MessageDeathWormHitbox.Handler::handle);
		on(MessageDragonSetBurnBlock.class, MessageDragonSetBurnBlock.Handler::handle);
		on(MessageDragonSyncFire.class, MessageDragonSyncFire.Handler::handle);
		on(MessageGetMyrmexHive.class, MessageGetMyrmexHive.Handler::handle);
		on(MessageSetMyrmexHiveNull.class, MessageSetMyrmexHiveNull.Handler::handle);
		on(MessageSirenSong.class, MessageSirenSong.Handler::handle);
		on(com.iafabric.message.MessagePixieBondSync.class, com.iafabric.message.MessagePixieBondSync.Handler::handle);
		on(MessageBossCard.class, (message, player) -> Minecraft.getInstance().setScreen(new com.iafabric.client.gui.GuiBossCard(message)));
		on(MessageExiledSpeech.class, (message, player) -> com.iafabric.client.gui.ExiledPixieHud.speak(message.text, message.kind));
		on(MessageExiledEyes.class, (message, player) -> com.iafabric.client.gui.EyesOverlay.start(message.open, message.ticks));
		on(MessageExiledMemory.class, (message, player) -> com.iafabric.client.gui.MemoryOverlay.handle(message));
		on(MessageMusic.class, (message, player) -> com.iafabric.client.sound.Soundtrack.handle(message));
		on(MessageExiledSync.class, (message, player) -> {
			message.keep();
			if (message.open && !(Minecraft.getInstance().screen instanceof com.iafabric.client.gui.GuiExiledPixie)) {
				Minecraft.getInstance().setScreen(new com.iafabric.client.gui.GuiExiledPixie());
			}
		});
		on(MessageUpdatePixieHouse.class, MessageUpdatePixieHouse.Handler::handle);
		on(MessageUpdatePixieHouseModel.class, MessageUpdatePixieHouseModel.Handler::handle);
		on(MessageUpdatePixieJar.class, MessageUpdatePixieJar.Handler::handle);
		on(MessageUpdatePodium.class, MessageUpdatePodium.Handler::handle);
		on(MessageUpdateDragonforge.class, MessageUpdateDragonforge.Handler::handle);
		on(MessageUpdateLectern.class, MessageUpdateLectern.Handler::handle);
		on(MessageSyncPath.class, (message, player) -> message.handleClient());
		on(MessageSyncPathReached.class, (message, player) -> message.handleClient());
		on(MessageSpawnParticleAt.class, MessageSpawnParticleAt.Handler::handle);
		on(MessageStartRidingMob.class, MessageStartRidingMob.Handler::handle);
		on(MessageAnimation.class, ClientNetwork::animation);
		on(MessageEntityData.class, ClientNetwork::entityData);
	}

	@SuppressWarnings("unchecked")
	private static <M> void on(Class<M> cls, BiConsumer<M, Player> handler) {
		IafNetwork.Channel<M> channel = IafNetwork.channelOf(cls);
		ClientPlayNetworking.registerGlobalReceiver(channel.id(), (client, listener, buf, sender) -> {
			M message = channel.read().apply(buf);
			client.execute(() -> handler.accept(message, client.player));
		});
	}

	private static void animation(MessageAnimation message, Player player) {
		Entity entity = Minecraft.getInstance().level == null ? null : Minecraft.getInstance().level.getEntity(message.entityId());
		if (entity instanceof IAnimatedEntity animated) {
			int index = message.index();
			if (index < 0 || index >= animated.getAnimations().length) {
				animated.setAnimation(IAnimatedEntity.NO_ANIMATION);
			} else {
				animated.setAnimation(animated.getAnimations()[index]);
			}
			animated.setAnimationTick(0);
		}
	}

	private static void entityData(MessageEntityData message, Player player) {
		Entity entity = Minecraft.getInstance().level == null ? null : Minecraft.getInstance().level.getEntity(message.entityId());
		if (entity != null) {
			CitadelEntityData.setCitadelTag(entity, message.data());
		}
	}
}

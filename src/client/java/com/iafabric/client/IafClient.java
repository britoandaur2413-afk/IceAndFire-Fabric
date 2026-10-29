package com.iafabric.client;

import com.iafabric.IafMod;
import com.iafabric.citadel.animation.PartialTicks;
import com.iafabric.client.render.IafEntityRenderers;
import com.iafabric.client.render.item.IafItemProperties;
import com.iafabric.network.IafNetwork;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;

/** Client entrypoint: the client proxy, packets to the client and the renderers. */
public class IafClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		IafMod.PROXY = new ClientProxy();
		PartialTicks.source = () -> Minecraft.getInstance().getTimer().getGameTimeDeltaPartialTick(false);
		IafNetwork.ClientSender.sender = ClientPlayNetworking::send;
		ClientNetwork.init();
		IafItemProperties.init();
		com.iafabric.client.render.IafRenderLayers.init();
		com.iafabric.client.render.item.IafSpecialItemRenderer.init();
		IafEntityRenderers.init();
		com.iafabric.client.gui.IafGuiRegistry.register();
		com.iafabric.client.render.IafArmorRenderer.init();
		com.iafabric.client.render.RealmEffects.init();
		IafKeybinds.init();
		// her checkpoint's pin, under her lines
		com.iafabric.client.gui.FairyWaypoint.init();
		// the mod's HUD after the game's (and alone while a memory frames the screen: GuiHudMixin)
		net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback.EVENT.register(com.iafabric.client.gui.IafHud::render);
		net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
			com.iafabric.client.gui.EyesOverlay.clear();
			com.iafabric.client.gui.MemoryOverlay.clear();
			com.iafabric.client.sound.Soundtrack.clear();
			com.iafabric.client.sound.Writing.clear();
		});
		IafClientEvents.init();
		com.iafabric.client.render.entity.layer.LayerLivingExtras.init();
	}
}

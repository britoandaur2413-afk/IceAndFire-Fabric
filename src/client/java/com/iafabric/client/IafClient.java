package com.iafabric.client;

import com.iafabric.IafMod;
import com.iafabric.citadel.animation.PartialTicks;
import com.iafabric.client.render.IafEntityRenderers;
import com.iafabric.client.render.item.IafItemProperties;
import com.iafabric.network.IafNetwork;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.client.Minecraft;

/** Client entrypoint: the client proxy, packets to the client and the renderers. */
public class IafClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		IafMod.PROXY = new ClientProxy();
		PartialTicks.source = () -> Minecraft.getInstance().getDeltaTracker().getGameTimeDeltaPartialTick(false);
		IafNetwork.ClientSender.sender = ClientPlayNetworking::send;
		ClientNetwork.init();
		IafItemProperties.init();
		com.iafabric.client.render.item.IafSpecialItemRenderer.init();
		IafEntityRenderers.init();
		com.iafabric.client.gui.IafGuiRegistry.register();
		com.iafabric.client.render.IafArmorRenderer.init();
		IafKeybinds.init();
		net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry.attachElementAfter(
			net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements.HOTBAR, com.iafabric.IafMod.id("pixie_spell"), com.iafabric.client.gui.PixieSpellHud::render);
		// her checkpoint's pin, under her lines
		com.iafabric.client.gui.FairyWaypoint.init();
		net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry.attachElementAfter(
			net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements.HOTBAR, com.iafabric.IafMod.id("exiled_waypoint"), com.iafabric.client.gui.FairyWaypoint::hud);
		net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry.attachElementAfter(
			com.iafabric.IafMod.id("exiled_waypoint"), com.iafabric.IafMod.id("exiled_pixie"), com.iafabric.client.gui.ExiledPixieHud::render);
		// the player's eyes in her scenes, over everything
		net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry.addLast(com.iafabric.IafMod.id("exiled_eyes"), com.iafabric.client.gui.EyesOverlay::render);
		// her memory as a film: its haze under their eyes, its bars and subtitles over them, nothing else on the screen
		net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry.attachElementBefore(
			com.iafabric.IafMod.id("exiled_eyes"), com.iafabric.IafMod.id("exiled_memory_haze"), com.iafabric.client.gui.MemoryOverlay::haze);
		net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry.addLast(com.iafabric.IafMod.id("exiled_memory"), com.iafabric.client.gui.MemoryOverlay::render);
		for (net.minecraft.resources.Identifier element : java.util.List.of(VanillaHudElements.CROSSHAIR, VanillaHudElements.HOTBAR, VanillaHudElements.ARMOR_BAR,
			VanillaHudElements.HEALTH_BAR, VanillaHudElements.FOOD_BAR, VanillaHudElements.AIR_BAR, VanillaHudElements.MOUNT_HEALTH, VanillaHudElements.INFO_BAR, VanillaHudElements.EXPERIENCE_LEVEL,
			VanillaHudElements.HELD_ITEM_TOOLTIP, VanillaHudElements.MOB_EFFECTS, VanillaHudElements.BOSS_BAR, VanillaHudElements.SCOREBOARD, VanillaHudElements.OVERLAY_MESSAGE, VanillaHudElements.CHAT)) {
			net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry.replaceElement(element, com.iafabric.client.gui.MemoryOverlay::hidden);
		}
		net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
			com.iafabric.client.gui.EyesOverlay.clear();
			com.iafabric.client.gui.MemoryOverlay.clear();
			com.iafabric.client.sound.Soundtrack.clear();
			com.iafabric.client.sound.Writing.clear();
		});
		IafClientEvents.init();
		com.iafabric.client.render.entity.layer.LayerLivingExtras.init();
		net.fabricmc.fabric.api.client.particle.v1.ParticleGroupRegistry.register(
			com.iafabric.client.particle.AppearanceParticle.GROUP, com.iafabric.client.particle.AppearanceParticle.Group::new);
	}
}

package com.iafabric.client;

import com.iafabric.IafConfig;
import com.iafabric.IafMod;
import com.iafabric.entity.EntityDragonBase;
import com.iafabric.entity.EntitySiren;
import com.iafabric.entity.props.SirenProperties;
import com.iafabric.entity.util.ICustomMoveController;
import com.iafabric.enums.EnumParticles;
import com.iafabric.message.MessageDragonControl;
import com.iafabric.network.IafNetwork;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;

/**
 * What the 1.18 client events did every tick: the riding controls of dragons and other mounts (sent to
 * the server when they change), the riding view key, the third person view when getting on one's own
 * dragon, and the siren's charm (with its pink veil, post shader shaders/post/siren.json, like the 1.18 mod).
 */
public final class IafClientEvents {
	private static final RandomSource RANDOM = RandomSource.create();
	private static final ResourceLocation SIREN_SHADER = IafMod.id("shaders/post/siren.json");
	private static Entity lastVehicle;

	private IafClientEvents() {
	}

	public static void init() {
		ClientTickEvents.END_CLIENT_TICK.register(IafClientEvents::tick);
	}

	private static void tick(Minecraft mc) {
		com.iafabric.client.gui.ExiledPixieHud.tick();
		com.iafabric.client.gui.EyesOverlay.tick();
		com.iafabric.client.gui.MemoryOverlay.tick();
		com.iafabric.client.sound.Soundtrack.tick();
		LocalPlayer player = mc.player;
		if (player == null || mc.level == null) {
			lastVehicle = null;
			return;
		}
		sirenVeil(mc, player);
		if (player.getVehicle() instanceof ICustomMoveController controller) {
			Entity mount = player.getVehicle();
			byte previous = controller.getControlState();
			controller.up(mc.options.keyJump.isDown());
			// Shift: off on the ground, down in the air (flying mounts only let the rider off on the ground).
			boolean shift = mc.options.keyShift.isDown();
			boolean canGetOff = com.iafabric.entity.util.IFlyingMount.canDismount(mount);
			controller.down(IafKeybinds.dragonDown.isDown() || shift && !canGetOff);
			controller.attack(IafKeybinds.dragonStrike.isDown());
			controller.dismount(shift && canGetOff);
			controller.strike(IafKeybinds.dragonFireAttack.isDown());
			if (controller.getControlState() != previous) {
				IafNetwork.sendToServer(new MessageDragonControl(mount.getId(), controller.getControlState(), mount.getX(), mount.getY(), mount.getZ()));
			}
		}
		for (Entity passenger : player.getPassengers()) {
			if (passenger instanceof ICustomMoveController controller) {
				byte previous = controller.getControlState();
				controller.dismount(mc.options.keyShift.isDown());
				if (controller.getControlState() != previous) {
					IafNetwork.sendToServer(new MessageDragonControl(passenger.getId(), controller.getControlState(), passenger.getX(), passenger.getY(), passenger.getZ()));
				}
			}
		}
		while (IafKeybinds.pixieBonds.consumeClick()) {
			if (mc.screen == null) {
				IafNetwork.sendToServer(new com.iafabric.message.MessagePixieBond(com.iafabric.message.MessagePixieBond.OPEN, 0));
				mc.setScreen(new com.iafabric.client.gui.GuiPixieBonds());
			}
		}
		while (IafKeybinds.pixieMagic.consumeClick()) {
			int active = com.iafabric.message.MessagePixieBondSync.client.active;
			if (mc.screen == null) {
				IafNetwork.sendToServer(new com.iafabric.message.MessagePixieBond(com.iafabric.message.MessagePixieBond.MAGIC, Math.max(0, active)));
			}
		}
		while (IafKeybinds.exiledPixie.consumeClick()) {
			if (mc.screen == null) {
				IafNetwork.sendToServer(new com.iafabric.message.MessageExiledAction(com.iafabric.message.MessageExiledAction.OPEN, 0));
			}
		}
		while (IafKeybinds.exiledMemory.consumeClick()) {
			if (mc.screen == null) {
				IafNetwork.sendToServer(new com.iafabric.message.MessageExiledAction(com.iafabric.message.MessageExiledAction.ABILITY, 0));
			}
		}
		while (IafKeybinds.dragonChangeView.consumeClick()) {
			int view = IafMod.PROXY.getDragon3rdPersonView();
			IafMod.PROXY.setDragon3rdPersonView(view + 1 > 3 ? 0 : view + 1);
		}
		mountView(mc, player);
		sirenCharm(player);
	}

	/** Getting on one's own dragon: a view far enough to see it, and third person; getting off: the view before. */
	private static void mountView(Minecraft mc, LocalPlayer player) {
		Entity vehicle = player.getVehicle();
		if (vehicle == lastVehicle) {
			return;
		}
		if (lastVehicle instanceof EntityDragonBase old && old.isOwnedBy(player) && IafConfig.dragonAuto3rdPerson) {
			mc.options.setCameraType(CameraType.values()[IafMod.PROXY.getPreviousViewType()]);
		}
		if (vehicle instanceof EntityDragonBase dragon && dragon.isTame() && dragon.isOwnedBy(player)) {
			IafMod.PROXY.setDragon3rdPersonView(2);
			if (IafConfig.dragonAuto3rdPerson) {
				IafMod.PROXY.setPreviousViewType(mc.options.getCameraType().ordinal());
				mc.options.setCameraType(CameraType.THIRD_PERSON_BACK);
			}
		}
		lastVehicle = vehicle;
	}

	/** The pink veil over a charmed player's view (unless another effect is on: a mob's view, as in the 1.18 mod). */
	private static void sirenVeil(Minecraft mc, LocalPlayer player) {
		net.minecraft.client.renderer.PostChain current = mc.gameRenderer.currentEffect();
		boolean ours = current != null && SIREN_SHADER.toString().equals(current.getName());
		if (IafConfig.sirenShader && SirenProperties.isCharmed(player)) {
			if (current == null) {
				mc.gameRenderer.loadEffect(SIREN_SHADER);
			}
		} else if (ours) {
			mc.gameRenderer.shutdownEffect();
		}
	}

	/** A player charmed by a siren sees her hair color around now and then. */
	private static void sirenCharm(LocalPlayer player) {
		EntitySiren siren = SirenProperties.getSiren(player);
		if (siren != null && SirenProperties.isCharmed(player) && RANDOM.nextInt(40) == 0) {
			IafMod.PROXY.spawnParticle(EnumParticles.Siren_Appearance, player.getX(), player.getY(), player.getZ(), siren.getHairColor(), 0, 0);
		}
	}
}

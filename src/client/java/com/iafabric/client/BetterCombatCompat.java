package com.iafabric.client;

import com.iafabric.IafMod;
import com.iafabric.item.IafItemRegistry;
import com.iafabric.message.MessageSwingArm;
import com.iafabric.network.IafNetwork;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.world.entity.player.Player;

import java.lang.reflect.Proxy;
import java.util.List;

/**
 * Better Combat swings on its own, without the game's attack (Minecraft.startAttack, where MinecraftAttackMixin tells
 * the server of a swing at nothing), so the ghost sword threw no blade. Its own event says when a swing hit nobody:
 * then the ghost sword throws its blade, as the 1.18 mod did on a swing at nothing. Better Combat is found by name, so
 * the mod does not need it.
 */
public final class BetterCombatCompat {
	private BetterCombatCompat() {
	}

	public static void init() {
		if (!FabricLoader.getInstance().isModLoaded("bettercombat")) {
			return;
		}
		try {
			Class<?> events = Class.forName("net.bettercombat.api.client.BetterCombatClientEvents");
			Class<?> hit = Class.forName("net.bettercombat.api.client.BetterCombatClientEvents$PlayerAttackHit");
			Object publisher = events.getField("ATTACK_HIT").get(null);
			Object listener = Proxy.newProxyInstance(hit.getClassLoader(), new Class<?>[]{hit}, (proxy, method, args) -> {
				if (method.getDeclaringClass() == Object.class) {
					return switch (method.getName()) {
						case "hashCode" -> System.identityHashCode(proxy);
						case "equals" -> proxy == args[0];
						default -> "Ice and Fire's ghost sword on Better Combat's swings";
					};
				}
				// (player, the hand that swung, what it hit, what was under the crosshair)
				if (args != null && args.length >= 3 && args[0] instanceof Player player && args[2] instanceof List<?> targets && targets.isEmpty()
					&& !offHand(args[1]) && player.getMainHandItem().getItem() == IafItemRegistry.GHOST_SWORD) {
					IafNetwork.sendToServer(new MessageSwingArm());
				}
				return null;
			});
			publisher.getClass().getMethod("register", Object.class).invoke(publisher, listener);
			IafMod.LOGGER.info("Better Combat found: the ghost sword throws its blade on its swings at nothing");
		} catch (ReflectiveOperationException | LinkageError e) {
			IafMod.LOGGER.warn("Better Combat's attack event was not found: the ghost sword throws no blade on its swings", e);
		}
	}

	private static boolean offHand(Object attackHand) {
		try {
			return attackHand != null && Boolean.TRUE.equals(attackHand.getClass().getMethod("isOffHand").invoke(attackHand));
		} catch (ReflectiveOperationException e) {
			return false;
		}
	}
}

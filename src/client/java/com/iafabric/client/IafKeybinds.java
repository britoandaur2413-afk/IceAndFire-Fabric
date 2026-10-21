package com.iafabric.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.minecraft.client.KeyMapping;

import java.util.Map;

/**
 * The keys of the mod: the dragon's breath (R), its bite (G), flying down (X), the riding view (F7), the pixie bonds
 * (H), the pixie's magic (V), the exiled pixie's menu (J) and her memory (K).
 */
public final class IafKeybinds {
	public static KeyMapping dragonFireAttack;
	public static KeyMapping dragonStrike;
	public static KeyMapping dragonDown;
	public static KeyMapping dragonChangeView;
	/** The pixie bonds menu (H). */
	public static KeyMapping pixieBonds;
	/** The spell of the pixie out (V). */
	public static KeyMapping pixieMagic;
	/** The exiled pixie's menu (J) and her memory (K). */
	public static KeyMapping exiledPixie;
	public static KeyMapping exiledMemory;

	/**
	 * What the first 26.3 builds saved for these keys: they gave the 1.18 key numbers, which 26.x reads as other keys
	 * (82 is the up arrow there, not R). A key still on one of these goes back to its default.
	 */
	private static final Map<String, String> OLD_WRONG_KEYS = Map.of(
		"key.dragon_fireAttack", "key.keyboard.up",
		"key.dragon_strike", "key.keyboard.scroll.lock",
		"key.dragon_down", "key.keyboard.keypad.enter",
		"key.dragon_change_view", "key.keyboard.296");

	private IafKeybinds() {
	}

	public static void init() {
		dragonFireAttack = KeyMappingHelper.registerKeyMapping(new KeyMapping("key.dragon_fireAttack", InputConstants.KEY_R, KeyMapping.Category.GAMEPLAY));
		dragonStrike = KeyMappingHelper.registerKeyMapping(new KeyMapping("key.dragon_strike", InputConstants.KEY_G, KeyMapping.Category.GAMEPLAY));
		dragonDown = KeyMappingHelper.registerKeyMapping(new KeyMapping("key.dragon_down", InputConstants.KEY_X, KeyMapping.Category.GAMEPLAY));
		dragonChangeView = KeyMappingHelper.registerKeyMapping(new KeyMapping("key.dragon_change_view", InputConstants.KEY_F7, KeyMapping.Category.MISC));
		pixieBonds = KeyMappingHelper.registerKeyMapping(new KeyMapping("key.iceandfire_fabric.pixie_bonds", InputConstants.KEY_H, KeyMapping.Category.GAMEPLAY));
		pixieMagic = KeyMappingHelper.registerKeyMapping(new KeyMapping("key.iceandfire_fabric.pixie_magic", InputConstants.KEY_V, KeyMapping.Category.GAMEPLAY));
		exiledPixie = KeyMappingHelper.registerKeyMapping(new KeyMapping("key.iceandfire_fabric.exiled_pixie", InputConstants.KEY_J, KeyMapping.Category.GAMEPLAY));
		exiledMemory = KeyMappingHelper.registerKeyMapping(new KeyMapping("key.iceandfire_fabric.exiled_memory", InputConstants.KEY_K, KeyMapping.Category.GAMEPLAY));
		ClientLifecycleEvents.CLIENT_STARTED.register(client -> {
			if (fixOldWrongKeys()) {
				client.options.save();
			}
		});
	}

	/** Puts back the default of a key still on what an earlier build saved by mistake; true when one changed. */
	public static boolean fixOldWrongKeys() {
		boolean changed = false;
		for (KeyMapping key : new KeyMapping[]{dragonFireAttack, dragonStrike, dragonDown, dragonChangeView}) {
			if (key.saveString().equals(OLD_WRONG_KEYS.get(key.getName()))) {
				key.setKey(key.getDefaultKey());
				changed = true;
			}
		}
		if (changed) {
			KeyMapping.resetMapping();
		}
		return changed;
	}
}

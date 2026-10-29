package com.iafabric.test.mixin;

import net.minecraft.client.gui.components.BossHealthOverlay;
import net.minecraft.client.gui.components.LerpingBossEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.Map;
import java.util.UUID;

/** The boss bars the player's game shows, for the duel tests. */
@Mixin(BossHealthOverlay.class)
public interface BossOverlayAccessor {
	@Accessor("events")
	Map<UUID, LerpingBossEvent> iceandfire_fabric$events();
}

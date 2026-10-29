package com.iafabric.compat;

import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;

/** A mob's death drops from a loot table of its choice (26.x's dropFromLootTable with a table; 1.21.1 has none). */
public final class LootDrops {
	private LootDrops() {
	}

	public static void drop(LivingEntity entity, DamageSource source, boolean playerKilled, ResourceKey<LootTable> key) {
		if (!(entity.level() instanceof ServerLevel level)) {
			return;
		}
		LootTable table = level.getServer().reloadableRegistries().getLootTable(key);
		LootParams.Builder builder = new LootParams.Builder(level)
			.withParameter(LootContextParams.THIS_ENTITY, entity)
			.withParameter(LootContextParams.ORIGIN, entity.position())
			.withParameter(LootContextParams.DAMAGE_SOURCE, source)
			.withOptionalParameter(LootContextParams.ATTACKING_ENTITY, source.getEntity())
			.withOptionalParameter(LootContextParams.DIRECT_ATTACKING_ENTITY, source.getDirectEntity());
		if (playerKilled && entity.lastHurtByPlayer != null) {
			builder = builder.withParameter(LootContextParams.LAST_DAMAGE_PLAYER, entity.lastHurtByPlayer).withLuck(entity.lastHurtByPlayer.getLuck());
		}
		table.getRandomItems(builder.create(LootContextParamSets.ENTITY), entity.getLootTableSeed(), entity::spawnAtLocation);
	}
}

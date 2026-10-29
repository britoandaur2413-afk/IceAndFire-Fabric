package com.iafabric.compat;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ServerLevelAccessor;

/** A level's sea level as 26.x has it: its world generator's (1.21.1's Level.getSeaLevel() is always 63, a flat world's too). */
public final class Levels {
	private Levels() {
	}

	public static int seaLevel(LevelReader level) {
		if (level instanceof ServerLevelAccessor accessor) {
			ServerLevel server = accessor.getLevel();
			return server.getChunkSource().getGenerator().getSeaLevel();
		}
		return level.getSeaLevel();
	}
}

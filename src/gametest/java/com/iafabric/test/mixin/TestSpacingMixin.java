package com.iafabric.test.mixin;

import net.minecraft.gametest.framework.GameTestBatchRunner;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

/**
 * The test places far apart: 1.20.1 puts them 5 blocks from each other, and the mod's dragons, pixies and bosses
 * reach into the next ones (a test passed alone and failed among the others).
 */
@Mixin(GameTestBatchRunner.class)
public class TestSpacingMixin {
	private static final int APART = 48;

	@ModifyConstant(method = "createStructuresForBatch", constant = @Constant(intValue = 5))
	private int iceandfire_fabric$apartInARow(int blocks) {
		return APART;
	}

	@ModifyConstant(method = "createStructuresForBatch", constant = @Constant(intValue = 6))
	private int iceandfire_fabric$apartRows(int blocks) {
		return APART;
	}
}

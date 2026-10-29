package com.iafabric.test.ct;

import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.worldselection.CreateWorldScreen;
import net.minecraft.client.gui.screens.worldselection.WorldCreationUiState;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.levelgen.presets.WorldPresets;

import java.util.function.Consumer;

/**
 * A new world, made through the game's own Create World screen (as Fabric's test world builder does): by default
 * superflat, seed 1, no structures, no time, weather nor mob spawning; then the test's own settings.
 */
public final class TestWorldBuilder {
	private final ClientGameTestContext context;
	private boolean useConsistentSettings = true;
	private Consumer<WorldCreationUiState> adjust = settings -> {
	};

	TestWorldBuilder(ClientGameTestContext context) {
		this.context = context;
	}

	public TestWorldBuilder setUseConsistentSettings(boolean use) {
		this.useConsistentSettings = use;
		return this;
	}

	public TestWorldBuilder adjustSettings(Consumer<WorldCreationUiState> adjust) {
		this.adjust = adjust;
		return this;
	}

	public TestSingleplayerContext create() {
		context.runOnClient(mc -> {
			Screen old = mc.screen;
			CreateWorldScreen.openFresh(mc, old);
			if (!(mc.screen instanceof CreateWorldScreen screen)) {
				throw new AssertionError("the Create World screen did not open");
			}
			WorldCreationUiState settings = screen.getUiState();
			if (useConsistentSettings) {
				var flat = settings.getSettings().worldgenLoadContext().registryOrThrow(Registries.WORLD_PRESET).getHolderOrThrow(WorldPresets.FLAT);
				settings.setWorldType(new WorldCreationUiState.WorldTypeEntry(flat));
				settings.setSeed("1");
				settings.setGenerateStructures(false);
				settings.getGameRules().getRule(GameRules.RULE_DAYLIGHT).set(false, null);
				settings.getGameRules().getRule(GameRules.RULE_WEATHER_CYCLE).set(false, null);
				settings.getGameRules().getRule(GameRules.RULE_DOMOBSPAWNING).set(false, null);
				settings.getGameRules().getRule(GameRules.RULE_SPAWN_RADIUS).set(0, null);
			}
			adjust.accept(settings);
		});
		context.clickScreenButton("selectWorld.create");
		// the world loads: in it, no screen over it
		context.waitFor(mc -> mc.level != null && mc.player != null && mc.screen == null && mc.getSingleplayerServer() != null, 3000);
		context.waitTicks(20);
		return new TestSingleplayerContext(context, context.computeOnClient(mc -> mc.getSingleplayerServer()));
	}
}

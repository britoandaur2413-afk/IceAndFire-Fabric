package com.iafabric.test.ct;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.CloudStatus;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.AccessibilityOnboardingScreen;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.tutorial.TutorialSteps;
import net.minecraft.sounds.SoundSource;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

/**
 * Runs the client tests (entrypoint "iceandfire_fabric-clienttest") when the game is started with
 * -Diceandfire_fabric.clienttest=true (gradlew runClienttest), one after another on a thread of their own, then
 * closes the game. Log lines start with [IAFCLIENT].
 */
public final class ClientTestRunner implements ClientModInitializer {
	private static final boolean ENABLED = Boolean.getBoolean("iceandfire_fabric.clienttest");
	private static final Object TICKS = new Object();
	private static long ticks;
	private static boolean started;

	@Override
	public void onInitializeClient() {
		if (!ENABLED) {
			return;
		}
		ClientTickEvents.END_CLIENT_TICK.register(ClientTestRunner::tick);
	}

	private static void tick(Minecraft mc) {
		// the test window may lose focus: never pause
		mc.options.pauseOnLostFocus = false;
		if (mc.screen instanceof PauseScreen) {
			mc.setScreen(null);
		}
		// (no advancement toasts over the pictures)
		mc.getToasts().clear();
		if (mc.screen instanceof AccessibilityOnboardingScreen) {
			mc.options.onboardAccessibility = false;
			mc.setScreen(new TitleScreen());
		}
		if (!started && mc.getOverlay() == null && mc.screen instanceof TitleScreen) {
			started = true;
			options(mc);
			Thread thread = new Thread(() -> runAll(mc), "Ice and Fire client tests");
			thread.setDaemon(true);
			thread.start();
		}
		synchronized (TICKS) {
			ticks++;
			TICKS.notifyAll();
		}
	}

	/** The settings Fabric's client tests use, for pictures alike from run to run. */
	private static void options(Minecraft mc) {
		mc.options.tutorialStep = TutorialSteps.NONE;
		mc.options.cloudStatus().set(CloudStatus.OFF);
		mc.options.onboardAccessibility = false;
		mc.options.renderDistance().set(5);
		mc.options.getSoundSourceOptionInstance(SoundSource.MUSIC).set(0.0);
	}

	private static void runAll(Minecraft mc) {
		List<String> failed = new ArrayList<>();
		int passed = 0;
		var tests = FabricLoader.getInstance().getEntrypointContainers("iceandfire_fabric-clienttest", FabricClientGameTest.class);
		for (var container : tests) {
			FabricClientGameTest test = container.getEntrypoint();
			String name = test.getClass().getSimpleName();
			log("running " + name);
			ClientGameTestContext context = new ClientGameTestContext();
			try {
				test.runTest(context);
				passed++;
				log("passed " + name);
			} catch (Throwable t) {
				failed.add(name + ": " + t);
				log("FAILED " + name + ": " + t);
				t.printStackTrace();
				TestSingleplayerContext.closeAny(context);
			}
			context.getInput().clearKeysDown();
		}
		log("client tests: " + passed + " passed, " + failed.size() + " failed " + failed);
		mc.execute(mc::stop);
	}

	static void log(String message) {
		System.out.println("[IAFCLIENT] " + message);
	}

	static Path gameDir() {
		return FabricLoader.getInstance().getGameDir();
	}

	static void waitTicks(int count) {
		synchronized (TICKS) {
			long until = ticks + count;
			while (ticks < until) {
				try {
					TICKS.wait(5000);
				} catch (InterruptedException e) {
					throw new RuntimeException(e);
				}
			}
		}
	}

	static <T> T join(CompletableFuture<T> future) {
		try {
			return future.get();
		} catch (InterruptedException e) {
			throw new RuntimeException(e);
		} catch (ExecutionException e) {
			if (e.getCause() instanceof RuntimeException runtime) {
				throw runtime;
			}
			if (e.getCause() instanceof Error error) {
				throw error;
			}
			throw new RuntimeException(e.getCause());
		}
	}
}

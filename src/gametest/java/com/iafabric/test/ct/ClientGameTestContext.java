package com.iafabric.test.ct;

import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.layouts.Layout;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;

import java.nio.file.Path;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;
import java.util.function.Predicate;
import java.util.function.Supplier;

/**
 * What a client test does, as Fabric's client game test context does it in 26.x (1.21.1 has no client game tests):
 * the test runs on a thread of its own, the game runs as always, and the test waits for its ticks, runs things on the
 * client thread, takes screenshots (build/clienttest/screenshots, numbered) and makes worlds.
 */
public final class ClientGameTestContext {
	private static int screenshots;
	private final TestInput input = new TestInput(this);

	public interface ClientAction {
		void run(Minecraft mc) throws Throwable;
	}

	public interface ClientFunction<T> {
		T apply(Minecraft mc) throws Throwable;
	}

	public void waitTick() {
		ClientTestRunner.waitTicks(1);
	}

	public void waitTicks(int ticks) {
		ClientTestRunner.waitTicks(ticks);
	}

	public int waitFor(Predicate<Minecraft> predicate) {
		return waitFor(predicate, 200);
	}

	/** Waits until it holds (looked at once a tick); fails after so many ticks. */
	public int waitFor(Predicate<Minecraft> predicate, int timeout) {
		for (int ticks = 0; ; ticks++) {
			if (computeOnClient(predicate::test)) {
				return ticks;
			}
			if (ticks >= timeout) {
				throw new AssertionError("waited " + timeout + " ticks for it in vain");
			}
			waitTick();
		}
	}

	public int waitForScreen(@Nullable Class<? extends Screen> screenClass) {
		return waitFor(mc -> screenClass == null ? mc.screen == null : screenClass.isInstance(mc.screen));
	}

	public void setScreen(Supplier<@Nullable Screen> screen) {
		runOnClient(mc -> mc.setScreen(screen.get()));
	}

	/** Presses the button of the screen open with this text (its translation key). */
	public void clickScreenButton(String translationKey) {
		if (!computeOnClient(mc -> press(mc.screen, Component.translatable(translationKey).getString()))) {
			throw new AssertionError("no button \"" + translationKey + "\" on the screen");
		}
	}

	private static boolean press(@Nullable Screen screen, String text) {
		if (screen == null) {
			return false;
		}
		for (GuiEventListener child : screen.children()) {
			if (pressIn(child, text)) {
				return true;
			}
		}
		return false;
	}

	private static boolean pressIn(GuiEventListener element, String text) {
		if (element instanceof AbstractButton button && button.active && button.getMessage().getString().equals(text)) {
			button.onPress();
			return true;
		}
		if (element instanceof Layout layout) {
			boolean[] done = {false};
			layout.visitWidgets(widget -> {
				if (!done[0] && pressIn(widget, text)) {
					done[0] = true;
				}
			});
			return done[0];
		}
		return false;
	}

	/** The screen as it is now (a frame drawn after what was done before), saved as screenshots/NNNN_name.png. */
	public Path takeScreenshot(String name) {
		waitTick();
		String file = String.format(Locale.ROOT, "%04d_%s.png", screenshots++, name);
		CompletableFuture<Void> saved = new CompletableFuture<>();
		runOnClient(mc -> Screenshot.grab(mc.gameDirectory, file, mc.getMainRenderTarget(), message -> saved.complete(null)));
		saved.join();
		// (the file is written a moment later, away from the client thread)
		waitTicks(2);
		return ClientTestRunner.gameDir().resolve("screenshots").resolve(file);
	}

	public void runOnClient(ClientAction action) {
		computeOnClient(mc -> {
			action.run(mc);
			return null;
		});
	}

	public <T> T computeOnClient(ClientFunction<T> function) {
		Minecraft mc = Minecraft.getInstance();
		if (mc.isSameThread()) {
			throw new IllegalStateException("a client test step called from the client thread");
		}
		CompletableFuture<T> result = new CompletableFuture<>();
		mc.execute(() -> {
			try {
				result.complete(function.apply(mc));
			} catch (Throwable t) {
				result.completeExceptionally(t);
			}
		});
		return ClientTestRunner.join(result);
	}

	public TestInput getInput() {
		return input;
	}

	public TestWorldBuilder worldBuilder() {
		return new TestWorldBuilder(this);
	}
}

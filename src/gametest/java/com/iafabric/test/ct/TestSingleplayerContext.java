package com.iafabric.test.ct;

import net.minecraft.client.gui.screens.GenericDirtMessageScreen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;

/** A test's singleplayer world: its server; closing it saves and leaves the world, back to the title screen. */
public final class TestSingleplayerContext implements AutoCloseable {
	private final ClientGameTestContext context;
	private final TestServerContext server;

	TestSingleplayerContext(ClientGameTestContext context, MinecraftServer server) {
		this.context = context;
		this.server = new TestServerContext(context, server);
	}

	public TestServerContext getServer() {
		return server;
	}

	@Override
	public void close() {
		leave(context);
	}

	private static void leave(ClientGameTestContext context) {
		context.runOnClient(mc -> {
			if (mc.level != null) {
				mc.level.disconnect();
			}
			mc.clearLevel(new GenericDirtMessageScreen(Component.translatable("menu.savingLevel")));
			mc.setScreen(new TitleScreen());
		});
		context.waitFor(mc -> mc.level == null && mc.getSingleplayerServer() == null, 3000);
		context.waitTicks(5);
	}

	/** After a failed test: out of its world, if it is still in one. */
	static void closeAny(ClientGameTestContext context) {
		try {
			if (context.computeOnClient(mc -> mc.level != null)) {
				leave(context);
			}
		} catch (Throwable t) {
			ClientTestRunner.log("could not leave the world: " + t);
		}
	}
}

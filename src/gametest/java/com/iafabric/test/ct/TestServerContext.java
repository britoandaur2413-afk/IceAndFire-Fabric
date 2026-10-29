package com.iafabric.test.ct;

import net.minecraft.server.MinecraftServer;

import java.util.concurrent.CompletableFuture;

/** The test world's server: things run on its thread, commands as the server. */
public final class TestServerContext {
	private final ClientGameTestContext context;
	private final MinecraftServer server;

	public interface ServerAction {
		void run(MinecraftServer server) throws Throwable;
	}

	public interface ServerFunction<T> {
		T apply(MinecraftServer server) throws Throwable;
	}

	TestServerContext(ClientGameTestContext context, MinecraftServer server) {
		this.context = context;
		this.server = server;
	}

	public void runOnServer(ServerAction action) {
		computeOnServer(s -> {
			action.run(s);
			return null;
		});
	}

	public <T> T computeOnServer(ServerFunction<T> function) {
		CompletableFuture<T> result = new CompletableFuture<>();
		server.execute(() -> {
			try {
				result.complete(function.apply(server));
			} catch (Throwable t) {
				result.completeExceptionally(t);
			}
		});
		return ClientTestRunner.join(result);
	}

	public void runCommand(String command) {
		runOnServer(s -> s.getCommands().performPrefixedCommand(s.createCommandSourceStack(), command));
	}
}

package com.iafabric.test.ct;

import com.iafabric.test.mixin.KeyboardHandlerInvoker;
import com.iafabric.test.mixin.MouseHandlerInvoker;
import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import net.minecraft.commands.arguments.EntityAnchorArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Function;

/** Keys, mouse buttons and the cursor, through the game's own input handlers (as Fabric's test input does). */
public final class TestInput {
	private final Set<InputConstants.Key> down = new HashSet<>();
	private final ClientGameTestContext context;

	TestInput(ClientGameTestContext context) {
		this.context = context;
	}

	public void holdKey(KeyMapping mapping) {
		holdKey(KeyBindingHelper.getBoundKeyOf(mapping));
	}

	public void holdKey(Function<Options, KeyMapping> mapping) {
		KeyMapping keyMapping = context.computeOnClient(mc -> mapping.apply(mc.options));
		holdKey(keyMapping);
	}

	public void holdKey(InputConstants.Key key) {
		if (down.add(key)) {
			context.runOnClient(mc -> act(mc, key, GLFW.GLFW_PRESS));
		}
	}

	public void holdKey(int keyCode) {
		holdKey(InputConstants.Type.KEYSYM.getOrCreate(keyCode));
	}

	public void releaseKey(KeyMapping mapping) {
		releaseKey(KeyBindingHelper.getBoundKeyOf(mapping));
	}

	public void releaseKey(Function<Options, KeyMapping> mapping) {
		KeyMapping keyMapping = context.computeOnClient(mc -> mapping.apply(mc.options));
		releaseKey(keyMapping);
	}

	public void releaseKey(InputConstants.Key key) {
		if (down.remove(key)) {
			context.runOnClient(mc -> act(mc, key, GLFW.GLFW_RELEASE));
		}
	}

	public void releaseKey(int keyCode) {
		releaseKey(InputConstants.Type.KEYSYM.getOrCreate(keyCode));
	}

	public void pressKey(KeyMapping mapping) {
		pressKey(KeyBindingHelper.getBoundKeyOf(mapping));
	}

	public void pressKey(Function<Options, KeyMapping> mapping) {
		KeyMapping keyMapping = context.computeOnClient(mc -> mapping.apply(mc.options));
		pressKey(keyMapping);
	}

	public void pressKey(InputConstants.Key key) {
		holdKey(key);
		releaseKey(key);
		context.waitTick();
	}

	public void pressKey(int keyCode) {
		pressKey(InputConstants.Type.KEYSYM.getOrCreate(keyCode));
	}

	public void holdKeyFor(Function<Options, KeyMapping> mapping, int ticks) {
		InputConstants.Key key = KeyBindingHelper.getBoundKeyOf(context.computeOnClient(mc -> mapping.apply(mc.options)));
		holdKeyFor(key, ticks);
	}

	public void holdKeyFor(InputConstants.Key key, int ticks) {
		holdKey(key);
		context.waitTicks(ticks);
		releaseKey(key);
	}

	public void holdMouse(int button) {
		holdKey(InputConstants.Type.MOUSE.getOrCreate(button));
	}

	public void releaseMouse(int button) {
		releaseKey(InputConstants.Type.MOUSE.getOrCreate(button));
	}

	public void pressMouse(int button) {
		pressKey(InputConstants.Type.MOUSE.getOrCreate(button));
	}

	public void holdMouseFor(int button, int ticks) {
		holdKeyFor(InputConstants.Type.MOUSE.getOrCreate(button), ticks);
	}

	public void clearKeysDown() {
		for (InputConstants.Key key : new ArrayList<>(down)) {
			releaseKey(key);
		}
	}

	private static void act(Minecraft mc, InputConstants.Key key, int action) {
		long window = mc.getWindow().getWindow();
		if (key.getType() == InputConstants.Type.MOUSE) {
			((MouseHandlerInvoker) mc.mouseHandler).iceandfire_fabric$onPress(window, key.getValue(), action, 0);
		} else {
			mc.keyboardHandler.keyPress(window, key.getValue(), 0, action, 0);
		}
	}

	public void lookAt(float yaw, float pitch) {
		context.runOnClient(mc -> {
			mc.player.setYRot(yaw);
			mc.player.setXRot(pitch);
		});
	}

	public void lookAt(BlockPos pos) {
		context.runOnClient(mc -> mc.player.lookAt(EntityAnchorArgument.Anchor.EYES, Vec3.atCenterOf(pos)));
	}

	public void typeChars(String chars) {
		context.runOnClient(mc -> chars.chars().forEach(c ->
			((KeyboardHandlerInvoker) mc.keyboardHandler).iceandfire_fabric$charTyped(mc.getWindow().getWindow(), c, 0)));
	}

	/** The cursor to there, in the window's pixels. */
	public void setCursorPos(double x, double y) {
		context.runOnClient(mc -> ((MouseHandlerInvoker) mc.mouseHandler).iceandfire_fabric$onMove(mc.getWindow().getWindow(), x, y));
	}

	public void moveCursor(double dx, double dy) {
		context.runOnClient(mc -> ((MouseHandlerInvoker) mc.mouseHandler).iceandfire_fabric$onMove(mc.getWindow().getWindow(),
			mc.mouseHandler.xpos() + dx, mc.mouseHandler.ypos() + dy));
	}
}

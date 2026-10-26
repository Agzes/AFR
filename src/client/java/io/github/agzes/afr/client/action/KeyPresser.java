// SPDX-License-Identifier: GPL-3.0-only
package io.github.agzes.afr.client.action;

import io.github.agzes.afr.client.mixin.KeyboardHandlerInvoker;
import net.minecraft.client.KeyboardHandler;
import net.minecraft.client.Minecraft;
import org.lwjgl.glfw.GLFW;

public final class KeyPresser {

	private static boolean simulating;

	private KeyPresser() {}

	public static boolean isSimulating() {
		return simulating;
	}

	public static void press(int key) {
		Minecraft client = Minecraft.getInstance();
		if (client.player == null || key <= 0) {
			return;
		}
		long window = client.getWindow().getWindow();
		KeyboardHandler handler = client.keyboardHandler;
		KeyboardHandlerInvoker invoker = (KeyboardHandlerInvoker) handler;
		simulating = true;
		try {
			invoker.afr$keyPress(window, key, 0, GLFW.GLFW_PRESS, 0);
			invoker.afr$keyPress(window, key, 0, GLFW.GLFW_RELEASE, 0);
		} finally {
			simulating = false;
		}
	}
}

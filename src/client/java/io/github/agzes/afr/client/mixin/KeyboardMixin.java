// SPDX-License-Identifier: GPL-3.0-only
package io.github.agzes.afr.client.mixin;

import io.github.agzes.afr.client.AfrRuntime;
import io.github.agzes.afr.client.action.KeyPresser;
import net.minecraft.client.KeyboardHandler;
import net.minecraft.client.Minecraft;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(KeyboardHandler.class)
public class KeyboardMixin {

	@Inject(method = "keyPress", at = @At("HEAD"), cancellable = true)
	private void afr$onKeyPress(
		long window,
		int key,
		int scancode,
		int action,
		int modifiers,
		CallbackInfo ci
	) {
		if (action != GLFW.GLFW_PRESS) {
			return;
		}
		if (KeyPresser.isSimulating()) {
			return;
		}
		Minecraft client = Minecraft.getInstance();
		if (client.player == null || client.screen != null) {
			return;
		}
		AfrRuntime runtime = AfrRuntime.get();
		if (runtime != null && runtime.handleKey(key, modifiers)) {
			ci.cancel();
		}
	}
}

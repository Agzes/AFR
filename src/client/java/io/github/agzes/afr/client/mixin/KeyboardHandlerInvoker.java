// SPDX-License-Identifier: GPL-3.0-only
package io.github.agzes.afr.client.mixin;

import net.minecraft.client.KeyboardHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(KeyboardHandler.class)
public interface KeyboardHandlerInvoker {
	@Invoker("keyPress")
	void afr$keyPress(
		long window,
		int key,
		int scancode,
		int action,
		int modifiers
	);
}

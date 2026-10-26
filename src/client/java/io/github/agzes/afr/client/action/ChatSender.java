// SPDX-License-Identifier: GPL-3.0-only
package io.github.agzes.afr.client.action;

import io.github.agzes.afr.AFR;
import io.github.agzes.afr.client.AfrRuntime;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.ChatScreen;

public final class ChatSender {

	private static final int MAX_CHAT_LENGTH = 256;

	private ChatSender() {}

	public static void send(String text, boolean send) {
		Minecraft client = Minecraft.getInstance();
		if (client.player == null) {
			return;
		}
		String message = text.strip();
		message = message.replace('\n', ' ').replace('\r', ' ');
		if (message.isEmpty()) {
			return;
		}
		if (!send) {
			AfrRuntime runtime = AfrRuntime.get();
			if (runtime != null) {
				runtime.markOwnScreen();
			}
			client.setScreen(new ChatScreen(message));
			return;
		}
		if (message.startsWith("/")) {
			client.player.connection.sendCommand(message.substring(1));
		} else {
			if (message.length() > MAX_CHAT_LENGTH) {
				AFR.LOGGER.warn(
					"AFR: chat message is too long ({}), truncating to {}",
					message.length(),
					MAX_CHAT_LENGTH
				);
				message = message.substring(0, MAX_CHAT_LENGTH);
			}
			client.player.connection.sendChat(message);
		}
	}
}

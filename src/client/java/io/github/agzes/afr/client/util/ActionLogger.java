// SPDX-License-Identifier: GPL-3.0-only
package io.github.agzes.afr.client.util;

import io.github.agzes.afr.AFR;
import io.github.agzes.afr.client.config.ConfigPaths;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public final class ActionLogger {

	private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern(
		"yyyy-MM-dd HH:mm:ss"
	);
	private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern(
		"yyyy-MM-dd"
	);
	private static boolean enabled = false;

	private ActionLogger() {}

	public static void setEnabled(boolean value) {
		enabled = value;
	}

	public static void log(String category, String message) {
		if (!enabled) {
			return;
		}
		try {
			Path dir = ConfigPaths.logsDir();
			Files.createDirectories(dir);
			Path file = dir.resolve(
				"afr-actions-" + LocalDateTime.now().format(DATE) + ".log"
			);
			String line =
				"[" +
				LocalDateTime.now().format(TIME) +
				"] [" +
				category +
				"] " +
				message +
				System.lineSeparator();
			Files.writeString(
				file,
				line,
				StandardCharsets.UTF_8,
				StandardOpenOption.CREATE,
				StandardOpenOption.APPEND
			);
		} catch (IOException e) {
			AFR.LOGGER.warn("Failed to write the action log", e);
		}
	}
}

// SPDX-License-Identifier: GPL-3.0-only
package io.github.agzes.afr.client.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.github.agzes.afr.AFR;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

public final class ConfigCodec {

	public static final Gson GSON = new GsonBuilder()
		.setPrettyPrinting()
		.disableHtmlEscaping()
		.create();

	private ConfigCodec() {}

	public static JsonObject readObject(Path path) throws IOException {
		try (
			Reader reader = Files.newBufferedReader(
				path,
				StandardCharsets.UTF_8
			)
		) {
			return JsonParser.parseReader(reader).getAsJsonObject();
		}
	}

	public static <T> T read(Path path, Class<T> type) throws IOException {
		JsonObject object = readObject(path);
		MigrationEngine.migrate(object, path.getFileName().toString());
		return GSON.fromJson(object, type);
	}

	public static <T> T readString(String json, Class<T> type) {
		JsonObject object = JsonParser.parseString(json).getAsJsonObject();
		MigrationEngine.migrate(object, "clipboard.json");
		return GSON.fromJson(object, type);
	}

	public static void write(Path path, Object value) throws IOException {
		Files.createDirectories(path.getParent());
		Path tmp = path.resolveSibling(path.getFileName() + ".tmp");
		try {
			try (
				Writer writer = Files.newBufferedWriter(
					tmp,
					StandardCharsets.UTF_8
				)
			) {
				GSON.toJson(value, writer);
			}
			try {
				Files.move(
					tmp,
					path,
					StandardCopyOption.REPLACE_EXISTING,
					StandardCopyOption.ATOMIC_MOVE
				);
			} catch (AtomicMoveNotSupportedException e) {
				Files.move(tmp, path, StandardCopyOption.REPLACE_EXISTING);
			}
		} catch (IOException | RuntimeException e) {
			try {
				Files.deleteIfExists(tmp);
			} catch (IOException ignored) {}
			throw e;
		}
	}

	public static void backup(Path path) {
		if (!Files.exists(path)) {
			return;
		}
		try {
			Files.createDirectories(ConfigPaths.backupsDir());
			Path target = ConfigPaths.backupsDir().resolve(
				path.getFileName() +
					"." +
					System.currentTimeMillis() +
					"-" +
					Long.toHexString(System.nanoTime()) +
					".bak"
			);
			Files.copy(path, target, StandardCopyOption.REPLACE_EXISTING);
		} catch (IOException | RuntimeException e) {
			AFR.LOGGER.warn("Failed to back up {}", path, e);
		}
	}
}

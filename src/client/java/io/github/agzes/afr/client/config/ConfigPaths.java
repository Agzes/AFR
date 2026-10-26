// SPDX-License-Identifier: GPL-3.0-only
package io.github.agzes.afr.client.config;

import java.nio.file.Path;
import net.fabricmc.loader.api.FabricLoader;

public final class ConfigPaths {

	private ConfigPaths() {}

	public static Path root() {
		return FabricLoader.getInstance().getConfigDir().resolve("afr");
	}

	public static Path settings() {
		return root().resolve("settings.json");
	}

	public static Path profilesDir() {
		return root().resolve("profiles");
	}

	public static Path profile(String name) {
		return profilesDir().resolve(name + ".json");
	}

	public static Path backupsDir() {
		return root().resolve("backups");
	}

	public static Path logsDir() {
		return root().resolve("logs");
	}

	public static Path libraryCache() {
		return root().resolve("library_cache.json");
	}

	public static Path exportsDir() {
		return root().resolve("exports");
	}

	public static Path versionsDir() {
		return root().resolve("versions");
	}

	public static Path versionsDir(String profileId) {
		return versionsDir().resolve(profileId == null ? "_" : profileId);
	}
}

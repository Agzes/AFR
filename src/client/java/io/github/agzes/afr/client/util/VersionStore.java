// SPDX-License-Identifier: GPL-3.0-only
package io.github.agzes.afr.client.util;

import io.github.agzes.afr.AFR;
import io.github.agzes.afr.client.config.ConfigCodec;
import io.github.agzes.afr.client.config.ConfigPaths;
import io.github.agzes.afr.client.model.RpProfile;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

public final class VersionStore {

	private VersionStore() {}

	public record Entry(
		String tag,
		String version,
		int versionCode,
		long modified,
		Path path
	) {}

	public static String tag(RpProfile profile) {
		int code = profile.meta == null ? 0 : profile.meta.versionCode;
		String version =
			profile.meta == null || profile.meta.version == null
				? "0"
				: profile.meta.version;
		return code + "-" + version.replaceAll("[^0-9A-Za-z._-]", "_");
	}

	public static void archive(RpProfile profile) {
		if (
			profile == null ||
			profile.meta == null ||
			profile.meta.revName == null ||
			profile.meta.revName.isBlank()
		) {
			return;
		}
		try {
			Path dir = ConfigPaths.versionsDir(profile.meta.revName);
			Files.createDirectories(dir);
			Path target = dir.resolve(tag(profile) + ".json");
			if (!Files.exists(target)) {
				ConfigCodec.write(target, profile);
				AFR.LOGGER.info(
					"AFR: archived version {} of '{}'",
					tag(profile),
					profile.meta.revName
				);
			}
		} catch (IOException | RuntimeException e) {
			AFR.LOGGER.warn("AFR: failed to archive a profile version", e);
		}
	}

	public static List<Entry> list(RpProfile profile) {
		List<Entry> entries = new ArrayList<>();
		if (
			profile == null ||
			profile.meta == null ||
			profile.meta.revName == null
		) {
			return entries;
		}
		Path dir = ConfigPaths.versionsDir(profile.meta.revName);
		if (!Files.isDirectory(dir)) {
			return entries;
		}
		try (Stream<Path> stream = Files.list(dir)) {
			for (Path path : stream
				.filter(p -> p.getFileName().toString().endsWith(".json"))
				.toList()) {
				String fileName = path.getFileName().toString();
				String tag = fileName.substring(
					0,
					fileName.length() - ".json".length()
				);
				int code = parseCode(tag);
				String version = parseVersion(tag);
				long modified = Files.getLastModifiedTime(path).toMillis();
				entries.add(new Entry(tag, version, code, modified, path));
			}
		} catch (IOException e) {
			AFR.LOGGER.warn("AFR: failed to list profile versions", e);
		}
		entries.sort(Comparator.comparingInt(Entry::versionCode).reversed());
		return entries;
	}

	public static void delete(Entry entry) {
		try {
			Files.deleteIfExists(entry.path());
			AFR.LOGGER.info("AFR: deleted archived version {}", entry.tag());
		} catch (IOException e) {
			AFR.LOGGER.warn("AFR: failed to delete an archived version", e);
		}
	}

	public static RpProfile load(Entry entry) throws IOException {
		return ConfigCodec.read(entry.path(), RpProfile.class);
	}

	private static int parseCode(String tag) {
		int dash = tag.indexOf('-');
		String code = dash < 0 ? tag : tag.substring(0, dash);
		try {
			return Integer.parseInt(code);
		} catch (NumberFormatException e) {
			return 0;
		}
	}

	private static String parseVersion(String tag) {
		int dash = tag.indexOf('-');
		return dash < 0 ? tag : tag.substring(dash + 1);
	}
}

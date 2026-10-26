// SPDX-License-Identifier: GPL-3.0-only
package io.github.agzes.afr.client.config;

import io.github.agzes.afr.AFR;
import io.github.agzes.afr.client.model.RpAction;
import io.github.agzes.afr.client.model.RpProfile;
import io.github.agzes.afr.client.model.UserSettings;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Stream;

public class ConfigManager {

	private UserSettings settings = new UserSettings();
	private final Map<String, RpProfile> profiles = new LinkedHashMap<>();
	private RpProfile active;

	public UserSettings settings() {
		return settings;
	}

	public Map<String, RpProfile> profiles() {
		return profiles;
	}

	public RpProfile active() {
		return active;
	}

	public String activeId() {
		return active != null && active.meta != null
			? active.meta.revName
			: settings.activeProfile;
	}

	public void load() {
		try {
			Files.createDirectories(ConfigPaths.root());
			Files.createDirectories(ConfigPaths.profilesDir());
			Files.createDirectories(ConfigPaths.backupsDir());

			loadSettings();
			loadProfiles();
			ensureProfile();
			migrateLegacyBinds();

			AFR.LOGGER.debug(
				"AFR: loaded {} profile(s), active='{}'",
				profiles.size(),
				activeId()
			);
		} catch (IOException e) {
			AFR.LOGGER.error("Failed to load AFR configs", e);
		}
	}

	public void setActive(String id) {
		if (profiles.containsKey(id)) {
			settings.activeProfile = id;
			active = profiles.get(id);
			saveSettings();
			AFR.LOGGER.info("AFR: active profile set to '{}'", id);
		}
	}

	public void ensureProfile() {
		if (profiles.isEmpty()) {
			seedDefaults();
		}
		if (profiles.isEmpty()) {
			RpProfile blank = new RpProfile();
			blank.meta.revName = "default";
			blank.meta.label = "Default";
			putProfile(blank, true);
		}
		if (active == null || !profiles.containsKey(active.meta.revName)) {
			active = profiles.values().iterator().next();
			settings.activeProfile = active.meta.revName;
			saveSettings();
		}
	}

	public boolean saveSettings() {
		try {
			ConfigCodec.write(ConfigPaths.settings(), settings);
			AFR.LOGGER.debug("AFR: settings saved");
			return true;
		} catch (IOException | RuntimeException e) {
			AFR.LOGGER.error("Failed to save settings.json", e);
			return false;
		}
	}

	public boolean saveProfile(RpProfile profile) {
		try {
			ConfigCodec.write(
				ConfigPaths.profile(profile.meta.revName),
				profile
			);
			AFR.LOGGER.debug("AFR: profile '{}' saved", profile.meta.revName);
			return true;
		} catch (IOException | RuntimeException e) {
			AFR.LOGGER.error(
				"Failed to save profile {}",
				profile.meta.revName,
				e
			);
			return false;
		}
	}

	public boolean saveActive() {
		return active != null && saveProfile(active);
	}

	public void putProfile(RpProfile profile, boolean makeActive) {
		profile.normalize();
		if (profile.meta.revName == null || profile.meta.revName.isBlank()) {
			String base = "profile";
			String id = base;
			int index = 2;
			while (profiles.containsKey(id)) {
				id = base + "-" + index++;
			}
			profile.meta.revName = id;
		}
		profiles.put(profile.meta.revName, profile);
		saveProfile(profile);
		if (makeActive) {
			setActive(profile.meta.revName);
		}
	}

	public boolean renameProfile(RpProfile profile, String newLabel) {
		if (profile == null || profile.meta == null) {
			return false;
		}
		String label = newLabel == null ? "" : newLabel.strip();
		if (label.isBlank()) {
			return false;
		}
		String oldId = profile.meta.revName;
		String newId = label
			.toLowerCase(Locale.ROOT)
			.replaceAll("[^a-z0-9_-]+", "-")
			.replaceAll("(^-+|-+$)", "");
		if (newId.isBlank()) {
			newId = "profile";
		}
		if (!newId.equals(oldId)) {
			String base = newId;
			int n = 2;
			while (profiles.containsKey(newId)) {
				newId = base + "-" + n++;
			}
		}
		profile.meta.label = label;
		boolean renamed = !newId.equals(oldId);
		String previousId = profile.meta.revName;
		profile.meta.revName = newId;
		try {
			ConfigCodec.write(ConfigPaths.profile(newId), profile);
		} catch (IOException | RuntimeException e) {
			profile.meta.revName = previousId;
			AFR.LOGGER.error(
				"AFR: failed to save the renamed profile '{}'",
				newId,
				e
			);
			return false;
		}
		if (renamed) {
			profiles.remove(oldId);
			profiles.put(newId, profile);
			if (oldId.equals(settings.activeProfile)) {
				settings.activeProfile = newId;
				active = profile;
				saveSettings();
			}
			try {
				Files.deleteIfExists(ConfigPaths.profile(oldId));
				Path oldVersions = ConfigPaths.versionsDir(oldId);
				Path newVersions = ConfigPaths.versionsDir(newId);
				if (
					Files.isDirectory(oldVersions) && !Files.exists(newVersions)
				) {
					Files.move(oldVersions, newVersions);
				}
			} catch (IOException e) {
				AFR.LOGGER.warn("Failed to move the old profile files", e);
			}
		}
		AFR.LOGGER.info(
			"AFR: profile renamed '{}' -> '{}'",
			oldId,
			profile.meta.revName
		);
		return true;
	}

	public String resolveBind(String actionId, String fallback) {
		return settings.resolveBind(actionId, fallback);
	}

	private void migrateLegacyBinds() {
		if (settings.binds == null || settings.binds.isEmpty()) {
			return;
		}
		Map<String, Integer> names = new HashMap<>();
		for (RpProfile profile : profiles.values()) {
			for (RpAction action : profile.rp_actions.values()) {
				if (action.name == null || action.name.isBlank()) {
					continue;
				}
				names.merge(action.name, 1, Integer::sum);
			}
		}
		boolean changed = false;
		for (RpProfile profile : profiles.values()) {
			for (var entry : profile.rp_actions.entrySet()) {
				String name = entry.getValue().name;
				if (
					name == null ||
					name.isBlank() ||
					name.equals(entry.getKey()) ||
					name.equals("menu") ||
					name.equals("ForceStop") ||
					name.equals("Restart") ||
					name.startsWith("window:") ||
					names.getOrDefault(name, 0) != 1
				) {
					continue;
				}
				if (settings.binds.containsKey(entry.getKey())) {
					continue;
				}
				String legacy = settings.binds.remove(name);
				if (legacy != null) {
					settings.binds.put(entry.getKey(), legacy);
					changed = true;
				}
			}
		}
		if (changed) {
			saveSettings();
		}
	}

	private void loadSettings() {
		Path path = ConfigPaths.settings();
		if (!Files.exists(path)) {
			settings = new UserSettings();
			saveSettings();
			return;
		}
		try {
			settings = ConfigCodec.read(path, UserSettings.class);
			if (settings == null) {
				settings = new UserSettings();
			}
			settings.normalize();
		} catch (Exception e) {
			AFR.LOGGER.error("settings.json is corrupted, resetting", e);
			ConfigCodec.backup(path);
			settings = new UserSettings();
			settings.normalize();
			saveSettings();
		}
	}

	private void loadProfiles() {
		if (!Files.isDirectory(ConfigPaths.profilesDir())) {
			return;
		}
		try (Stream<Path> stream = Files.list(ConfigPaths.profilesDir())) {
			stream
				.filter(p -> p.getFileName().toString().endsWith(".json"))
				.forEach(path -> {
					try {
						RpProfile profile = ConfigCodec.read(
							path,
							RpProfile.class
						);
						if (profile != null) {
							profile.normalize();
							String fileId = path
								.getFileName()
								.toString()
								.replaceFirst("\\.json$", "");
							if (!fileId.equals(profile.meta.revName)) {
								profile.meta.revName = fileId;
							}
							profiles.put(profile.meta.revName, profile);
						}
					} catch (Exception e) {
						AFR.LOGGER.error(
							"Failed to read profile {}",
							path.getFileName(),
							e
						);
						ConfigCodec.backup(path);
						try {
							Files.move(
								path,
								path.resolveSibling(
									path.getFileName() + ".bad"
								),
								StandardCopyOption.REPLACE_EXISTING
							);
						} catch (IOException moveError) {
							AFR.LOGGER.warn(
								"Failed to move the corrupted profile {}",
								path.getFileName(),
								moveError
							);
						}
					}
				});
		} catch (IOException e) {
			AFR.LOGGER.error("Failed to read the profiles directory", e);
		}
	}

	private void seedDefaults() {
		RpProfile demo = null;
		try {
			demo = PresetLibrary.readBuiltIn("demo");
		} catch (Exception e) {
			AFR.LOGGER.error("Failed to load the demo preset", e);
		}
		if (demo != null && demo.meta != null) {
			demo.meta.source = "local";
			putProfile(demo, true);
			AFR.LOGGER.info(
				"AFR: seeded the built-in 'demo' preset as the default profile"
			);
			return;
		}
		RpProfile blank = new RpProfile();
		blank.meta.revName = "default";
		blank.meta.label = "Default";
		putProfile(blank, true);
		AFR.LOGGER.info("AFR: seeded an empty default profile");
	}
}

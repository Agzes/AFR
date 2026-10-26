// SPDX-License-Identifier: GPL-3.0-only
package io.github.agzes.afr.client;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import io.github.agzes.afr.AFR;
import io.github.agzes.afr.client.action.ActionExecutor;
import io.github.agzes.afr.client.config.ConfigCodec;
import io.github.agzes.afr.client.config.ConfigManager;
import io.github.agzes.afr.client.config.ConfigPaths;
import io.github.agzes.afr.client.hud.NotificationHud;
import io.github.agzes.afr.client.i18n.Lang;
import io.github.agzes.afr.client.input.KeyCombo;
import io.github.agzes.afr.client.model.ActionRef;
import io.github.agzes.afr.client.model.QuickActionWindow;
import io.github.agzes.afr.client.model.RpAction;
import io.github.agzes.afr.client.model.RpProfile;
import io.github.agzes.afr.client.net.AfrCloud;
import io.github.agzes.afr.client.ui.ActionMenuScreen;
import io.github.agzes.afr.client.ui.ConfigScreen;
import io.github.agzes.afr.client.ui.DiffScreen;
import io.github.agzes.afr.client.ui.QuickActionWindowScreen;
import io.github.agzes.afr.client.ui.RadialMenuScreen;
import io.github.agzes.afr.client.ui.Theme;
import io.github.agzes.afr.client.ui.TileScreen;
import io.github.agzes.afr.client.ui.Ui;
import io.github.agzes.afr.client.util.ActionLogger;
import io.github.agzes.afr.client.util.VersionStore;
import io.wispforest.owo.ui.base.BaseOwoScreen;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.sounds.SoundEvents;
import org.lwjgl.glfw.GLFW;

public final class AfrRuntime {

	public static final String SPECIAL_MENU = "afr:menu";
	public static final String SPECIAL_FORCE_STOP = "afr:force_stop";
	public static final String SPECIAL_RESTART = "afr:restart";

	private static AfrRuntime instance;

	private final ConfigManager config = new ConfigManager();
	private final ActionExecutor executor = new ActionExecutor();
	private final Map<KeyCombo, String> bindings = new HashMap<>();
	private boolean paused = false;
	public final List<JsonObject> announcements = new ArrayList<>();

	public String seasonalTheme = "";
	public JsonObject seasonalAnnouncement;
	public boolean editorDirty = false;
	private boolean ownScreenPending = false;

	private String currentActionId = "";
	private String pausedActionId = "";
	private String pausedProfileId = "";
	private long lastInteraction = System.currentTimeMillis();
	private int pausedStep = 0;
	private final Map<String, Long> cooldowns = new HashMap<>();

	private AfrRuntime() {}

	public static AfrRuntime get() {
		return instance;
	}

	public static AfrRuntime init() {
		instance = new AfrRuntime();
		instance.config.load();
		Lang.setLanguage(instance.config.settings().language);
		Theme startupTheme = Theme.byId(instance.config.settings().theme);
		Ui.applyTheme(
			startupTheme,
			instance.config.settings().themeColors.get(startupTheme.id)
		);
		ActionLogger.setEnabled(instance.config.settings().logActions);
		AFR.setDebugLogging(instance.config.settings().debugLogging);
		instance.rebuildBinds();
		RpProfile active = instance.config.active();
		AFR.LOGGER.info(
			"AFR runtime ready: {} profile(s), active='{}', {} action(s), theme='{}', language='{}'",
			instance.config.profiles().size(),
			instance.config.activeId(),
			active == null ? 0 : active.rp_actions.size(),
			instance.config.settings().theme,
			instance.config.settings().language
		);
		return instance;
	}

	public ConfigManager config() {
		return config;
	}

	public ActionExecutor executor() {
		return executor;
	}

	public boolean paused() {
		return paused;
	}

	public void togglePause() {
		paused = !paused;
	}

	public void reload() {
		config.load();
		Lang.setLanguage(config.settings().language);
		Theme reloadTheme = Theme.byId(config.settings().theme);
		Ui.applyTheme(
			reloadTheme,
			config.settings().themeColors.get(reloadTheme.id)
		);
		ActionLogger.setEnabled(config.settings().logActions);
		AFR.setDebugLogging(config.settings().debugLogging);
		AfrClient.setEditorKey(config.settings().editorKey);
		rebuildBinds();
		RpProfile active = config.active();
		AFR.LOGGER.info(
			"AFR: configuration reloaded (active='{}', {} action(s), theme='{}')",
			config.activeId(),
			active == null ? 0 : active.rp_actions.size(),
			config.settings().theme
		);
	}

	public void rebuildBinds() {
		bindings.clear();
		RpProfile profile = config.active();
		if (profile == null) {
			return;
		}
		for (Map.Entry<
			String,
			RpAction
		> entry : profile.rp_actions.entrySet()) {
			RpAction action = entry.getValue();
			KeyCombo combo = KeyCombo.parse(
				config.resolveBind(entry.getKey(), action.bind)
			);
			if (combo != null) {
				bindings.put(combo, entry.getKey());
			}
		}
		for (QuickActionWindow window : profile.quick_action_windows) {
			String bind = config.settings().binds.get("window:" + window.id);
			if (bind == null || bind.isBlank()) {
				bind = window.bind;
			}
			KeyCombo combo = KeyCombo.parse(bind);
			if (combo == null) {
				combo = KeyCombo.parse(
					defaultWindowKey(window.id, window.name)
				);
			}
			if (combo != null) {
				bindings.put(combo, "window:" + window.id);
			}
		}
		putSpecial(
			config
				.settings()
				.resolveBind("ForceStop", profile.profile_settings.forceStop),
			SPECIAL_FORCE_STOP
		);
		putSpecial(
			config
				.settings()
				.resolveBind("Restart", profile.profile_settings.restart),
			SPECIAL_RESTART
		);
		putSpecial(config.settings().menuKey, SPECIAL_MENU);
		AFR.LOGGER.debug("AFR: registered {} key binding(s)", bindings.size());
	}

	private static String defaultWindowKey(String id, String name) {
		String key = (
			id != null && !id.isBlank() ? id : name == null ? "" : name
		).toLowerCase();
		if (key.contains("main")) {
			return "F4";
		}
		if (key.contains("rare")) {
			return "F8";
		}
		if (key.contains("educ")) {
			return "F6";
		}
		if (key.contains("pmp")) {
			return "F7";
		}
		return null;
	}

	private void putSpecial(String bind, String target) {
		KeyCombo combo = KeyCombo.parse(bind);
		if (combo != null) {
			bindings.put(combo, target);
		}
	}

	public boolean handleKey(int key, int modifiers) {
		KeyCombo combo = KeyCombo.of(
			key,
			(modifiers & GLFW.GLFW_MOD_CONTROL) != 0,
			(modifiers & GLFW.GLFW_MOD_ALT) != 0,
			(modifiers & GLFW.GLFW_MOD_SHIFT) != 0
		);
		String target = bindings.get(combo);
		if (target == null) {
			return false;
		}
		if (paused && !SPECIAL_MENU.equals(target)) {
			return true;
		}
		execute(target);
		return true;
	}

	public void execute(String target) {
		markInteraction();
		if (target.startsWith("window:")) {
			openWindow(target.substring("window:".length()));
			return;
		}
		switch (target) {
			case SPECIAL_MENU -> openMenu();
			case SPECIAL_FORCE_STOP -> stopAction();
			case SPECIAL_RESTART -> reload();
			default -> runAction(target);
		}
	}

	public void markInteraction() {
		lastInteraction = System.currentTimeMillis();
	}

	public long lastInteraction() {
		return lastInteraction;
	}

	public void runAction(String id) {
		RpProfile profile = config.active();
		if (profile == null) {
			AFR.LOGGER.warn(
				"AFR: no active profile, action '{}' not started",
				id
			);
			return;
		}
		RpAction action = profile.action(id);
		if (action == null) {
			AFR.LOGGER.warn("AFR: action '{}' not found", id);
			return;
		}
		if (executor.isRunning()) {
			AFR.LOGGER.warn(
				"AFR: action '{}' skipped, '{}' is running",
				id,
				executor.actionName()
			);
			return;
		}
		long now = System.currentTimeMillis();
		Long until = cooldowns.get(cooldownKey(id));
		if (until != null && now < until) {
			long left = Math.max(1, (until - now) / 1000);
			AFR.LOGGER.debug(
				"AFR: action '{}' on cooldown ({}s left)",
				id,
				left
			);
			NotificationHud.notify(
				NotificationHud.Type.WARNING,
				"AFR",
				Lang.t("msg.cooldown") + " " + left + "s"
			);
			return;
		}
		if (runActionFrom(id, action, profile, 0, true)) {
			pausedActionId = "";
			pausedProfileId = "";
			pausedStep = 0;
		}
	}

	private boolean runActionFrom(
		String id,
		RpAction action,
		RpProfile profile,
		int fromStep,
		boolean applyCooldown
	) {
		boolean started;
		try {
			started = executor.run(
				action,
				config.settings(),
				profile,
				fromStep
			);
		} catch (Exception error) {
			AFR.LOGGER.error("AFR: failed to prepare action '{}'", id, error);
			NotificationHud.notify(
				NotificationHud.Type.ERROR,
				"AFR",
				Lang.t("msg.action_error") + ": " + action.displayName()
			);
			return false;
		}
		if (!started) {
			AFR.LOGGER.warn("AFR: action '{}' not started (no steps?)", id);
			NotificationHud.notify(
				NotificationHud.Type.WARNING,
				"AFR",
				Lang.t("msg.empty_action") + ": " + action.displayName()
			);
			return false;
		}
		currentActionId = id;
		config.settings().pushRecent(id);
		playFeedback();
		int cooldown =
			action.cooldown > 0
				? action.cooldown
				: config.settings().defaultCooldown;
		if (applyCooldown && cooldown > 0) {
			cooldowns.put(
				cooldownKey(id),
				System.currentTimeMillis() + cooldown
			);
		}
		if (config.settings().notifyStart) {
			NotificationHud.notify(
				NotificationHud.Type.INFO,
				"AFR",
				Lang.t("msg.started") + ": " + action.displayName()
			);
		}
		ActionLogger.log("ACTION", "start: " + id);
		AFR.LOGGER.info(
			"AFR: running action '{}' ({} steps, from {})",
			id,
			executor.total(),
			fromStep
		);
		return true;
	}

	public boolean hasPausedAction() {
		return pausedActionId != null && !pausedActionId.isBlank();
	}

	private String cooldownKey(String id) {
		return config.activeId() + ":" + id;
	}

	public long cooldownRemaining(String id) {
		if (id == null) {
			return 0;
		}
		Long until = cooldowns.get(cooldownKey(id));
		if (until == null) {
			return 0;
		}
		long left = until - System.currentTimeMillis();
		return left > 0 ? (left + 999) / 1000 : 0;
	}

	public String pausedActionName() {
		RpProfile profile = config.active();
		RpAction action =
			profile != null && hasPausedAction()
				? profile.action(pausedActionId)
				: null;
		return action != null ? action.displayName() : "";
	}

	public void resumeLast() {
		if (!hasPausedAction()) {
			return;
		}
		RpProfile profile = config.active();
		if (
			pausedProfileId != null &&
			!pausedProfileId.isBlank() &&
			!pausedProfileId.equals(config.activeId())
		) {
			pausedActionId = "";
			pausedProfileId = "";
			pausedStep = 0;
			return;
		}
		String id = pausedActionId;
		RpAction action = profile != null ? profile.action(id) : null;
		int step = pausedStep;
		if (action == null) {
			pausedActionId = "";
			pausedProfileId = "";
			pausedStep = 0;
			return;
		}
		AFR.LOGGER.info("AFR: resuming action '{}' from step {}", id, step);
		if (runActionFrom(id, action, profile, step, false)) {
			pausedActionId = "";
			pausedProfileId = "";
			pausedStep = 0;
		}
	}

	public void stopAction() {
		if (!executor.isRunning()) {
			return;
		}
		String id = currentActionId;
		int step = executor.resumeIndex();
		executor.forceStop();
		currentActionId = "";
		if (config.settings().resumeLastAction && id != null && !id.isBlank()) {
			pausedActionId = id;
			pausedProfileId = config.activeId();
			pausedStep = step;
		}
		if (config.settings().notifyEnd) {
			NotificationHud.notify(
				NotificationHud.Type.INFO,
				"AFR",
				Lang.t("msg.stopped")
			);
		}
	}

	public void afterTick() {
		if (!executor.isRunning() && !currentActionId.isBlank()) {
			if (config.settings().notifyEnd) {
				NotificationHud.notify(
					NotificationHud.Type.INFO,
					"AFR",
					Lang.t("msg.finished") + ": " + executor.actionName()
				);
			}
			currentActionId = "";
			pausedActionId = "";
			pausedProfileId = "";
			pausedStep = 0;
		}
	}

	public void stopForScreen() {
		if (consumeOwnScreen()) {
			return;
		}
		if (config.settings().autoStopScreen) {
			stopAction();
		}
	}

	public void markOwnScreen() {
		ownScreenPending = true;
	}

	public boolean consumeOwnScreen() {
		boolean value = ownScreenPending;
		ownScreenPending = false;
		return value;
	}

	public void stopForDeath() {
		if (config.settings().autoStopDeath) {
			stopAction();
		}
	}

	public void stopForWorld() {
		if (config.settings().autoStopWorld) {
			stopAction();
		}
	}

	private void playFeedback() {
		if (!config.settings().sound) {
			return;
		}
		Minecraft client = Minecraft.getInstance();
		if (client.player == null) {
			return;
		}
		client.player.playSound(
			SoundEvents.UI_BUTTON_CLICK.value(),
			0.4f,
			1.2f
		);
	}

	public void openConfigUpdate(BaseOwoScreen<?> parentScreen) {
		RpProfile profile = config.active();
		if (profile == null) {
			return;
		}
		String lookupId = libraryId(profile);
		if (lookupId.isBlank()) {
			notify(Lang.t("msg.local_profile"));
			return;
		}
		Screen expected = Minecraft.getInstance().screen;
		AfrCloud.library()
			.thenAccept(lib -> {
				if (lib.fromCache()) {
					notify(Lang.t("lib.cache_used"));
					return;
				}
				JsonObject json = lib.json();
				if (!json.has("configs")) {
					notify(Lang.t("msg.library_unavailable"));
					return;
				}
				for (var element : json.getAsJsonArray("configs")) {
					if (!element.isJsonObject()) {
						continue;
					}
					var object = element.getAsJsonObject();
					String id = object.has("id")
						? object.get("id").getAsString()
						: "";
					if (!id.equalsIgnoreCase(lookupId)) {
						continue;
					}
					String url = object.has("config_url")
						? object.get("config_url").getAsString()
						: "";
					if (url.isBlank()) {
						notify(Lang.t("msg.no_update_link"));
						return;
					}
					AfrCloud.downloadConfig(url)
						.thenAccept(updated ->
							AfrCloud.runOnClient(() -> {
								if (
									Minecraft.getInstance().screen != expected
								) {
									return;
								}
								Minecraft.getInstance().setScreen(
									new DiffScreen(
										parentScreen,
										config,
										config.active(),
										updated
									)
								);
							})
						)
						.exceptionally(error -> {
							AFR.LOGGER.warn(
								"AFR: config download failed: {}",
								error.getMessage()
							);
							AfrCloud.runOnClient(() ->
								notify(Lang.t("msg.check_error"))
							);
							return null;
						});
					return;
				}
				notify(Lang.t("msg.profile_not_in_library"));
			})
			.exceptionally(error -> {
				AFR.LOGGER.warn(
					"AFR: config update check failed: {}",
					error.getMessage()
				);
				notify(Lang.t("msg.check_error"));
				return null;
			});
	}

	public void checkConfigUpdate() {
		checkConfigUpdate(false);
	}

	public void checkConfigUpdate(boolean auto) {
		RpProfile profile = config.active();
		if (profile == null) {
			return;
		}
		String lookupId = libraryId(profile);
		if (lookupId.isBlank()) {
			if (!auto) {
				notify(Lang.t("msg.local_profile"));
			}
			return;
		}
		Screen expected = Minecraft.getInstance().screen;
		AfrCloud.library()
			.thenAccept(lib -> {
				if (lib.fromCache()) {
					if (!auto) {
						notify(Lang.t("lib.cache_used"));
					}
					return;
				}
				JsonObject json = lib.json();
				if (!json.has("configs")) {
					notify(Lang.t("msg.library_unavailable"));
					return;
				}
				for (var element : json.getAsJsonArray("configs")) {
					if (!element.isJsonObject()) {
						continue;
					}
					var object = element.getAsJsonObject();
					String id = object.has("id")
						? object.get("id").getAsString()
						: "";
					if (!id.equalsIgnoreCase(lookupId)) {
						continue;
					}
					int remoteCode = object.has("version_code")
						? object.get("version_code").getAsInt()
						: 0;
					String url = object.has("config_url")
						? object.get("config_url").getAsString()
						: "";
					if (url.isBlank()) {
						notify(Lang.t("msg.no_update_link"));
						return;
					}
					if (remoteCode <= profile.meta.versionCode) {
						notify(Lang.t("msg.config_uptodate"));
						return;
					}
					AfrCloud.downloadConfig(url)
						.thenAccept(updated ->
							AfrCloud.runOnClient(() -> {
								if (auto) {
									applyConfigUpdate(profile, updated);
								} else {
									if (
										Minecraft.getInstance().screen !=
										expected
									) {
										return;
									}
									Minecraft.getInstance().setScreen(
										new DiffScreen(
											null,
											config,
											profile,
											updated
										)
									);
								}
							})
						)
						.exceptionally(error -> {
							AFR.LOGGER.warn(
								"AFR: config download failed: {}",
								error.getMessage()
							);
							if (!auto) {
								AfrCloud.runOnClient(() ->
									notify(Lang.t("msg.check_error"))
								);
							}
							return null;
						});
					return;
				}
				AFR.LOGGER.info(
					"AFR: no library entry for active config '{}'",
					lookupId
				);
				notify(Lang.t("msg.profile_not_in_library"));
			})
			.exceptionally(error -> {
				AFR.LOGGER.warn(
					"AFR: config update check failed: {}",
					error.getMessage()
				);
				notify(Lang.t("msg.check_error"));
				return null;
			});
	}

	private void applyConfigUpdate(RpProfile profile, RpProfile updated) {
		if (
			profile == null ||
			profile.meta == null ||
			updated == null ||
			updated.meta == null
		) {
			AFR.LOGGER.warn("AFR: cannot apply config update (missing meta)");
			return;
		}
		if (updated.parameters == null) {
			updated.parameters = new ArrayList<>();
		}
		if (updated.rp_actions == null) {
			updated.rp_actions = new LinkedHashMap<>();
		}
		if (updated.quick_action_windows == null) {
			updated.quick_action_windows = new ArrayList<>();
		}
		if (profile.parameters == null) {
			profile.parameters = new ArrayList<>();
		}
		if (profile.rp_actions == null) {
			profile.rp_actions = new LinkedHashMap<>();
		}
		for (var parameter : updated.parameters) {
			var existing = profile.parameter(parameter.id);
			if (existing != null) {
				parameter.value = existing.value;
			}
		}
		for (var entry : updated.rp_actions.entrySet()) {
			var old = profile.rp_actions.get(entry.getKey());
			if (old != null) {
				entry.getValue().favorite = old.favorite;
			}
		}
		if (profile.meta.modified) {
			ConfigCodec.backup(ConfigPaths.profile(profile.meta.revName));
			AFR.LOGGER.info(
				"AFR: backed up locally modified config '{}' before update",
				profile.meta.revName
			);
		}
		updated.meta.revName = profile.meta.revName;
		updated.meta.source = "library";
		if (updated.meta.remoteId == null || updated.meta.remoteId.isBlank()) {
			updated.meta.remoteId = profile.meta.remoteId;
		}
		updated.meta.remoteVersionCode = updated.meta.versionCode;
		updated.meta.modified = false;
		VersionStore.archive(profile);
		config.putProfile(updated, true);
		rebuildBinds();
		Minecraft client = Minecraft.getInstance();
		if (client.screen instanceof ConfigScreen editor) {
			editor.refresh();
		}
		AFR.LOGGER.info(
			"AFR: config '{}' updated to v{}",
			updated.meta.revName,
			updated.meta.version
		);
		NotificationHud.notify(
			NotificationHud.Type.SUCCESS,
			"AFR",
			Lang.t("diff.updated") + ": v" + updated.meta.version
		);
	}

	private String libraryId(RpProfile profile) {
		if (
			profile == null ||
			profile.meta == null ||
			profile.meta.remoteId == null
		) {
			return "";
		}
		return profile.meta.remoteId.strip();
	}

	private void notify(String text) {
		NotificationHud.notify(NotificationHud.Type.INFO, "AFR", text);
	}

	public boolean seasonalActive() {
		return seasonalTheme != null && !seasonalTheme.isBlank();
	}

	public void checkSeasonal() {
		AfrCloud.productionArgs()
			.thenAccept(json -> {
				boolean enabled =
					!json.has("enabled") || json.get("enabled").getAsBoolean();
				if (!enabled || !json.has("theme") || !inSeason(json)) {
					AfrCloud.runOnClient(() -> {
						clearSeasonal();
						AfrClient.refreshOpenEditor();
					});
					return;
				}

				String themeId = "seasonal";
				Map<String, String> inline = null;
				JsonElement themeElement = json.get("theme");
				if (themeElement.isJsonPrimitive()) {
					themeId = themeElement.getAsString();
				} else if (themeElement.isJsonObject()) {
					JsonObject themeObject = themeElement.getAsJsonObject();
					Theme base = Theme.byId(
						themeObject.has("base")
							? themeObject.get("base").getAsString()
							: "current"
					);
					Map<String, String> full = new LinkedHashMap<>(
						base.colors()
					);
					if (
						themeObject.has("colors") &&
						themeObject.get("colors").isJsonObject()
					) {
						for (var entry : themeObject
							.getAsJsonObject("colors")
							.entrySet()) {
							String value = entry
								.getValue()
								.getAsString()
								.trim()
								.replace("#", "")
								.toUpperCase();
							full.put(entry.getKey(), value);
						}
					}
					inline = full;
				}
				if (themeId == null || themeId.isBlank()) {
					AfrCloud.runOnClient(() -> {
						clearSeasonal();
						AfrClient.refreshOpenEditor();
					});
					return;
				}

				boolean auto =
					json.has("auto") && json.get("auto").getAsBoolean();
				JsonObject announcement =
					json.has("announcement") &&
					json.get("announcement").isJsonObject()
						? json.getAsJsonObject("announcement")
						: null;

				final String themeFinal = themeId;
				final Map<String, String> inlineFinal = inline;
				final JsonObject announcementFinal = announcement;
				AfrCloud.runOnClient(() -> {
					AFR.LOGGER.info(
						"AFR: seasonal theme available: '{}' (auto={})",
						themeFinal,
						auto
					);
					if (inlineFinal != null) {
						config
							.settings()
							.themeColors.put(
								"seasonal",
								new LinkedHashMap<>(inlineFinal)
							);
					}
					seasonalTheme = themeFinal;
					seasonalAnnouncement = announcementFinal;
					if (auto && !themeFinal.equals(config.settings().theme)) {
						config.settings().seasonalFallback = config
							.settings()
							.theme;
						config.settings().theme = themeFinal;
						applyThemeById(themeFinal);
						config.saveSettings();
						AFR.LOGGER.info(
							"AFR: seasonal theme applied: '{}'",
							themeFinal
						);
					} else if (themeFinal.equals(config.settings().theme)) {
						applyThemeById(themeFinal);
					}
					AfrClient.refreshOpenEditor();
				});
			})
			.exceptionally(error -> {
				AFR.LOGGER.warn(
					"AFR: seasonal check failed: {}",
					error.getMessage()
				);
				return null;
			});
	}

	private boolean inSeason(JsonObject json) {
		try {
			LocalDate now = LocalDate.now();
			if (
				json.has("from") &&
				!json.get("from").getAsString().isBlank() &&
				now.isBefore(LocalDate.parse(json.get("from").getAsString()))
			) {
				return false;
			}
			if (
				json.has("to") &&
				!json.get("to").getAsString().isBlank() &&
				now.isAfter(LocalDate.parse(json.get("to").getAsString()))
			) {
				return false;
			}
		} catch (Exception ignored) {}
		return true;
	}

	private void clearSeasonal() {
		String current = config.settings().theme;
		boolean wasSeasonal =
			current != null &&
			(current.equals("seasonal") || current.equals(seasonalTheme));
		if (wasSeasonal) {
			String fallback = config.settings().seasonalFallback;
			config.settings().theme =
				fallback == null || fallback.isBlank() ? "current" : fallback;
			applyThemeById(config.settings().theme);
			config.saveSettings();
			AFR.LOGGER.info(
				"AFR: seasonal theme is gone, reverted to '{}'",
				config.settings().theme
			);
		}
		seasonalTheme = "";
		seasonalAnnouncement = null;
	}

	private void applyThemeById(String id) {
		Theme theme = Theme.byId(id);
		Ui.applyTheme(
			theme,
			config.settings().themeColors == null
				? null
				: config.settings().themeColors.get(theme.id)
		);
	}

	public void toggleSeasonal() {
		if (!seasonalActive()) {
			return;
		}
		if (seasonalTheme.equals(config.settings().theme)) {
			String fallback = config.settings().seasonalFallback;
			config.settings().theme =
				fallback == null || fallback.isBlank() ? "current" : fallback;
		} else {
			config.settings().seasonalFallback = config.settings().theme;
			config.settings().theme = seasonalTheme;
		}
		applyThemeById(config.settings().theme);
		config.saveSettings();
		AFR.LOGGER.info(
			"AFR: seasonal theme toggled, active theme is now '{}'",
			config.settings().theme
		);
	}

	public void openMenu() {
		Minecraft client = Minecraft.getInstance();
		if (client.player == null) {
			return;
		}
		RpProfile profile = config.active();
		List<ActionRef> actions =
			profile != null ? profile.actionRefs() : List.of();
		String style = config.settings().menuStyle;
		if ("tiles".equalsIgnoreCase(style)) {
			client.setScreen(
				new TileScreen(
					this,
					profile != null ? profile.meta.label : "AFR",
					actions,
					Math.min(8, Math.max(1, actions.size())),
					null
				)
			);
		} else if (config.settings().isRadialMenu()) {
			client.setScreen(
				new RadialMenuScreen(
					this,
					actions,
					profile != null ? profile.meta.label : "AFR",
					null
				)
			);
		} else {
			client.setScreen(new ActionMenuScreen(this));
		}
	}

	private static ActionRef refOf(RpProfile profile, String id) {
		RpAction action = profile.action(id);
		return action == null ? null : new ActionRef(id, action);
	}

	public void openWindow(String windowId) {
		Minecraft client = Minecraft.getInstance();
		if (client.player == null) {
			return;
		}
		RpProfile profile = config.active();
		if (profile == null) {
			return;
		}
		QuickActionWindow window = profile.window(windowId);
		if (window == null) {
			return;
		}
		String style =
			window.style == null || window.style.isBlank()
				? "list"
				: window.style.toLowerCase();
		if ("tiles".equals(style)) {
			List<ActionRef> slots = new ArrayList<>();
			if (window.tiles.isEmpty()) {
				for (String id : window.actions) {
					slots.add(refOf(profile, id));
				}
			} else {
				for (String id : window.tiles) {
					slots.add(
						id == null || id.isBlank() ? null : refOf(profile, id)
					);
				}
			}
			client.setScreen(
				new TileScreen(
					this,
					window.name,
					slots,
					window.tileCols,
					window.id
				)
			);
		} else if ("radial".equals(style)) {
			List<ActionRef> actions = new ArrayList<>();
			for (String id : window.actions) {
				ActionRef ref = refOf(profile, id);
				if (ref != null) {
					actions.add(ref);
				}
			}
			client.setScreen(
				new RadialMenuScreen(this, actions, window.name, window.id)
			);
		} else {
			client.setScreen(new QuickActionWindowScreen(this, window));
		}
	}
}

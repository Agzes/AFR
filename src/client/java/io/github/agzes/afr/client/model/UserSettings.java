// SPDX-License-Identifier: GPL-3.0-only
package io.github.agzes.afr.client.model;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.lwjgl.glfw.GLFW;

public class UserSettings {

	public int schemaVersion = 1;

	public String activeProfile = "demo";
	public String language = "ru";
	public boolean languageChosen = false;
	public String rpName = "";
	public String rpRole = "";
	public String gender = "male";

	public String menuKey = "F9";
	public int editorKey = GLFW.GLFW_KEY_RIGHT_SHIFT;
	public String menuStyle = "list";
	public String theme = "current";

	public boolean showHud = false;
	public boolean showProgress = true;
	public boolean notifications = true;
	public boolean sound = true;

	public double timerMultiplier = 1.0;

	public boolean checkUpdates = true;
	public boolean smartUpdateCheck = true;
	public boolean autoUpdateConfigs = false;

	public Map<String, String> binds = new LinkedHashMap<>();
	public List<String> favorites = new ArrayList<>();
	public List<String> recentActions = new ArrayList<>();

	public Map<String, Map<String, String>> themeColors = new LinkedHashMap<>();

	public String seasonalFallback = "current";

	public String lastAnnouncementId = "";
	public List<String> seenAnnouncements = new ArrayList<>();
	public boolean tutorialDone = false;
	public boolean logActions = false;
	public boolean debugLogging = false;

	public boolean showProfileSwitcher = false;

	public boolean autoStopScreen = false;
	public boolean autoStopDeath = false;
	public boolean autoStopWorld = true;
	public boolean notifyStart = false;
	public boolean notifyEnd = false;
	public boolean resumeLastAction = true;
	public int defaultCooldown = 0;

	public void normalize() {
		if (activeProfile == null || activeProfile.isBlank()) {
			activeProfile = "demo";
		}
		if (language == null || language.isBlank()) {
			language = "ru";
		}
		if (menuKey == null || menuKey.isBlank()) {
			menuKey = "F9";
		}
		if (editorKey <= 0) {
			editorKey = GLFW.GLFW_KEY_RIGHT_SHIFT;
		}
		if (menuStyle == null || menuStyle.isBlank()) {
			menuStyle = "list";
		}
		if (theme == null || theme.isBlank()) {
			theme = "current";
		}
		if (seasonalFallback == null || seasonalFallback.isBlank()) {
			seasonalFallback = "current";
		}
		if (binds == null) {
			binds = new LinkedHashMap<>();
		}
		if (favorites == null) {
			favorites = new ArrayList<>();
		}
		if (recentActions == null) {
			recentActions = new ArrayList<>();
		}
		if (themeColors == null) {
			themeColors = new LinkedHashMap<>();
		}
		if (lastAnnouncementId == null) {
			lastAnnouncementId = "";
		}
		if (seenAnnouncements == null) {
			seenAnnouncements = new ArrayList<>();
		}
		if (!(timerMultiplier >= 0)) {
			timerMultiplier = 1.0;
		}
		if (defaultCooldown < 0) {
			defaultCooldown = 0;
		}
	}

	public String resolveBind(String actionId, String fallback) {
		String override = binds.get(actionId);
		if (override != null && !override.isBlank()) {
			return override;
		}
		return fallback;
	}

	public boolean isRadialMenu() {
		return "radial".equalsIgnoreCase(menuStyle);
	}

	public void pushRecent(String actionId) {
		recentActions.remove(actionId);
		recentActions.add(0, actionId);
		while (recentActions.size() > 15) {
			recentActions.remove(recentActions.size() - 1);
		}
	}
}

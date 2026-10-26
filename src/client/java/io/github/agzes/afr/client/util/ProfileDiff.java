// SPDX-License-Identifier: GPL-3.0-only
package io.github.agzes.afr.client.util;

import io.github.agzes.afr.client.i18n.Lang;
import io.github.agzes.afr.client.model.Parameter;
import io.github.agzes.afr.client.model.ProfileMeta;
import io.github.agzes.afr.client.model.ProfileSettings;
import io.github.agzes.afr.client.model.QuickActionWindow;
import io.github.agzes.afr.client.model.RpAction;
import io.github.agzes.afr.client.model.RpProfile;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public final class ProfileDiff {

	public enum Type {
		ADDED,
		REMOVED,
		CHANGED,
		INFO,
	}

	public record Line(String section, String text, Type type) {}

	private ProfileDiff() {}

	public static List<Line> compare(
		RpProfile oldProfile,
		RpProfile newProfile
	) {
		List<Line> lines = new ArrayList<>();
		if (oldProfile == null || newProfile == null) {
			return lines;
		}
		if (oldProfile.meta == null) {
			oldProfile.meta = new ProfileMeta();
		}
		if (newProfile.meta == null) {
			newProfile.meta = new ProfileMeta();
		}
		if (oldProfile.meta.tags == null) {
			oldProfile.meta.tags = new ArrayList<>();
		}
		if (newProfile.meta.tags == null) {
			newProfile.meta.tags = new ArrayList<>();
		}
		if (oldProfile.profile_settings == null) {
			oldProfile.profile_settings = new ProfileSettings();
		}
		if (newProfile.profile_settings == null) {
			newProfile.profile_settings = new ProfileSettings();
		}
		if (oldProfile.parameters == null) {
			oldProfile.parameters = new ArrayList<>();
		}
		if (newProfile.parameters == null) {
			newProfile.parameters = new ArrayList<>();
		}
		if (oldProfile.rp_actions == null) {
			oldProfile.rp_actions = new LinkedHashMap<>();
		}
		if (newProfile.rp_actions == null) {
			newProfile.rp_actions = new LinkedHashMap<>();
		}
		if (oldProfile.quick_action_windows == null) {
			oldProfile.quick_action_windows = new ArrayList<>();
		}
		if (newProfile.quick_action_windows == null) {
			newProfile.quick_action_windows = new ArrayList<>();
		}

		compareMeta(lines, oldProfile, newProfile);
		compareSettings(lines, oldProfile, newProfile);
		compareParameters(lines, oldProfile, newProfile);
		compareActions(lines, oldProfile, newProfile);
		compareWindows(lines, oldProfile, newProfile);
		return lines;
	}

	private static void compareMeta(
		List<Line> lines,
		RpProfile a,
		RpProfile b
	) {
		changed(
			lines,
			Lang.t("diff.meta"),
			Lang.t("lbl.version"),
			a.meta.version,
			b.meta.version
		);
		changed(
			lines,
			Lang.t("diff.meta"),
			Lang.t("lbl.authors"),
			a.meta.authorsText(),
			b.meta.authorsText()
		);
		changed(
			lines,
			Lang.t("diff.meta"),
			Lang.t("lbl.editors"),
			a.meta.editorsText(),
			b.meta.editorsText()
		);
		changed(
			lines,
			Lang.t("diff.meta"),
			Lang.t("lbl.description"),
			a.meta.description,
			b.meta.description
		);
		changed(
			lines,
			Lang.t("diff.meta"),
			Lang.t("lbl.tags"),
			String.join(", ", a.meta.tags),
			String.join(", ", b.meta.tags)
		);
	}

	private static void compareSettings(
		List<Line> lines,
		RpProfile a,
		RpProfile b
	) {
		var sa = a.profile_settings;
		var sb = b.profile_settings;
		changed(
			lines,
			Lang.t("diff.settings"),
			Lang.t("diff.menu"),
			sa.menu,
			sb.menu
		);
		changed(
			lines,
			Lang.t("diff.settings"),
			Lang.t("diff.stop"),
			sa.forceStop,
			sb.forceStop
		);
		changed(
			lines,
			Lang.t("diff.settings"),
			Lang.t("diff.restart"),
			sa.restart,
			sb.restart
		);
		changed(
			lines,
			Lang.t("diff.settings"),
			Lang.t("notif.multiplier"),
			String.valueOf(sa.multiplier),
			String.valueOf(sb.multiplier)
		);
	}

	private static void compareParameters(
		List<Line> lines,
		RpProfile a,
		RpProfile b
	) {
		Map<String, Parameter> oldMap = new LinkedHashMap<>();
		for (Parameter p : a.parameters) {
			oldMap.put(p.id, p);
		}
		Map<String, Parameter> newMap = new LinkedHashMap<>();
		for (Parameter p : b.parameters) {
			newMap.put(p.id, p);
		}
		for (String id : oldMap.keySet()) {
			if (!newMap.containsKey(id)) {
				lines.add(
					new Line(
						Lang.t("diff.params"),
						Lang.t("diff.removed") + ": " + id,
						Type.REMOVED
					)
				);
			}
		}
		for (Map.Entry<String, Parameter> entry : newMap.entrySet()) {
			Parameter oldP = oldMap.get(entry.getKey());
			if (oldP == null) {
				lines.add(
					new Line(
						Lang.t("diff.params"),
						Lang.t("diff.added") +
							": " +
							entry.getKey() +
							" = " +
							entry.getValue().asText(),
						Type.ADDED
					)
				);
			} else if (
				!Objects.equals(oldP.asText(), entry.getValue().asText()) ||
				!Objects.equals(oldP.label, entry.getValue().label)
			) {
				lines.add(
					new Line(
						Lang.t("diff.params"),
						Lang.t("diff.changed") +
							": " +
							entry.getKey() +
							" (" +
							oldP.asText() +
							" -> " +
							entry.getValue().asText() +
							")",
						Type.CHANGED
					)
				);
			}
		}
	}

	private static int stepCount(RpAction action) {
		return action.steps == null ? 0 : action.steps.size();
	}

	private static List<List<String>> stepsOf(RpAction action) {
		return action.steps == null ? List.of() : action.steps;
	}

	private static void compareActions(
		List<Line> lines,
		RpProfile a,
		RpProfile b
	) {
		for (String id : a.rp_actions.keySet()) {
			if (!b.rp_actions.containsKey(id)) {
				lines.add(
					new Line(
						Lang.t("diff.actions"),
						Lang.t("diff.removed") +
							": " +
							a.rp_actions.get(id).displayName(),
						Type.REMOVED
					)
				);
			}
		}
		for (Map.Entry<String, RpAction> entry : b.rp_actions.entrySet()) {
			RpAction newAction = entry.getValue();
			RpAction oldAction = a.rp_actions.get(entry.getKey());
			if (oldAction == null) {
				lines.add(
					new Line(
						Lang.t("diff.actions"),
						Lang.t("diff.added") +
							": " +
							newAction.displayName() +
							" (" +
							stepCount(newAction) +
							" " +
							Lang.t("diff.step_count") +
							")",
						Type.ADDED
					)
				);
				continue;
			}
			List<String> changes = new ArrayList<>();
			if (!Objects.equals(oldAction.name, newAction.name)) {
				changes.add(Lang.t("diff.name"));
			}
			if (!Objects.equals(oldAction.icon, newAction.icon)) {
				changes.add(Lang.t("diff.icon"));
			}
			if (!Objects.equals(oldAction.category, newAction.category)) {
				changes.add(Lang.t("diff.category"));
			}
			if (oldAction.cooldown != newAction.cooldown) {
				changes.add(Lang.t("diff.cooldown"));
			}
			if (!Objects.equals(oldAction.description, newAction.description)) {
				changes.add(Lang.t("diff.description"));
			}
			if (stepCount(oldAction) != stepCount(newAction)) {
				changes.add(
					Lang.t("diff.steps") +
						": " +
						stepCount(oldAction) +
						" -> " +
						stepCount(newAction)
				);
			} else if (!stepsOf(oldAction).equals(stepsOf(newAction))) {
				changes.add(Lang.t("diff.step_text"));
			}
			if (!changes.isEmpty()) {
				lines.add(
					new Line(
						Lang.t("diff.actions"),
						Lang.t("diff.changed") +
							": " +
							newAction.displayName() +
							" (" +
							String.join(", ", changes) +
							")",
						Type.CHANGED
					)
				);
			}
		}
	}

	private static void compareWindows(
		List<Line> lines,
		RpProfile a,
		RpProfile b
	) {
		Map<String, QuickActionWindow> oldMap = new LinkedHashMap<>();
		for (QuickActionWindow w : a.quick_action_windows) {
			oldMap.put(w.id, w);
		}
		Map<String, QuickActionWindow> newMap = new LinkedHashMap<>();
		for (QuickActionWindow w : b.quick_action_windows) {
			newMap.put(w.id, w);
		}
		for (String id : oldMap.keySet()) {
			if (!newMap.containsKey(id)) {
				lines.add(
					new Line(
						Lang.t("diff.windows"),
						Lang.t("diff.removed") + ": " + oldMap.get(id).name,
						Type.REMOVED
					)
				);
			}
		}
		for (Map.Entry<String, QuickActionWindow> entry : newMap.entrySet()) {
			QuickActionWindow oldW = oldMap.get(entry.getKey());
			if (oldW == null) {
				lines.add(
					new Line(
						Lang.t("diff.windows"),
						Lang.t("diff.added") + ": " + entry.getValue().name,
						Type.ADDED
					)
				);
			} else if (
				!Objects.equals(oldW.actions, entry.getValue().actions) ||
				!Objects.equals(oldW.style, entry.getValue().style)
			) {
				lines.add(
					new Line(
						Lang.t("diff.windows"),
						Lang.t("diff.changed") + ": " + entry.getValue().name,
						Type.CHANGED
					)
				);
			}
		}
	}

	private static void changed(
		List<Line> lines,
		String section,
		String label,
		String from,
		String to
	) {
		String a = from == null ? "" : from;
		String b = to == null ? "" : to;
		if (!a.equals(b)) {
			lines.add(
				new Line(section, label + ": " + a + " -> " + b, Type.CHANGED)
			);
		}
	}
}

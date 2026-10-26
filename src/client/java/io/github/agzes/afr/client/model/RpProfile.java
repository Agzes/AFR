// SPDX-License-Identifier: GPL-3.0-only
package io.github.agzes.afr.client.model;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class RpProfile {

	public int schemaVersion = 1;

	public ProfileMeta meta = new ProfileMeta();
	public Map<String, RpAction> rp_actions = new LinkedHashMap<>();
	public ProfileSettings profile_settings = new ProfileSettings();
	public List<Parameter> parameters = new ArrayList<>();
	public Map<String, Object> misc = new LinkedHashMap<>();
	public List<Object> announcements = new ArrayList<>();
	public List<QuickActionWindow> quick_action_windows = new ArrayList<>();

	public RpAction action(String id) {
		if (id == null) {
			return null;
		}
		RpAction direct = rp_actions.get(id);
		if (direct != null) {
			return direct;
		}
		for (RpAction action : rp_actions.values()) {
			if (action != null && id.equals(action.name)) {
				return action;
			}
		}
		return null;
	}

	public boolean isLibrary() {
		return meta != null && meta.isLibrary();
	}

	public List<ActionRef> actionRefs() {
		List<ActionRef> list = new ArrayList<>();
		for (Map.Entry<String, RpAction> entry : rp_actions.entrySet()) {
			list.add(new ActionRef(entry.getKey(), entry.getValue()));
		}
		return list;
	}

	public void normalize() {
		if (meta == null) {
			meta = new ProfileMeta();
		}
		if (meta.authors == null) {
			meta.authors = new ArrayList<>();
		}
		if (meta.editors == null) {
			meta.editors = new ArrayList<>();
		}
		if (meta.tags == null) {
			meta.tags = new ArrayList<>();
		}
		if (rp_actions == null) {
			rp_actions = new LinkedHashMap<>();
		}
		if (profile_settings == null) {
			profile_settings = new ProfileSettings();
		}
		if (parameters == null) {
			parameters = new ArrayList<>();
		}
		parameters.removeIf(Objects::isNull);
		for (Parameter parameter : parameters) {
			if (parameter.id == null) {
				parameter.id = "";
			}
			if (parameter.label == null) {
				parameter.label = "";
			}
			if (parameter.paramType == null || parameter.paramType.isBlank()) {
				parameter.paramType = "text";
			}
			if (parameter.options == null) {
				parameter.options = new ArrayList<>();
			}
		}
		if (quick_action_windows == null) {
			quick_action_windows = new ArrayList<>();
		}
		quick_action_windows.removeIf(Objects::isNull);
		if (misc == null) {
			misc = new LinkedHashMap<>();
		}
		if (announcements == null) {
			announcements = new ArrayList<>();
		}
		rp_actions.values().removeIf(Objects::isNull);
		for (RpAction action : rp_actions.values()) {
			if (action.name == null) {
				action.name = "";
			}
			if (action.bind == null) {
				action.bind = "";
			}
			if (action.description == null) {
				action.description = "";
			}
			if (action.category == null) {
				action.category = "";
			}
			if (action.icon == null) {
				action.icon = "";
			}
			List<List<String>> steps = new ArrayList<>();
			if (action.steps != null) {
				for (List<String> raw : action.steps) {
					if (raw == null) {
						continue;
					}
					List<String> step = new ArrayList<>(raw);
					while (step.size() < 5) {
						step.add("");
					}
					if ("Chat".equalsIgnoreCase(step.get(1))) {
						step.set(1, "Send");
					}
					steps.add(step);
				}
			}
			action.steps = steps;
		}
		for (QuickActionWindow window : quick_action_windows) {
			if (window.id == null) {
				window.id = "";
			}
			if (window.name == null) {
				window.name = "";
			}
			if (window.bind == null) {
				window.bind = "";
			}
			if (window.style == null || window.style.isBlank()) {
				window.style = "list";
			}
			if (window.tileCols < 1) {
				window.tileCols = 6;
			}
			window.actions = normalizeIds(window.actions, false);
			window.tiles = normalizeIds(window.tiles, true);
		}
	}

	private List<String> normalizeIds(List<String> source, boolean keepBlanks) {
		List<String> result = new ArrayList<>();
		if (source == null) {
			return result;
		}
		for (String id : source) {
			if (id == null || id.isBlank()) {
				if (keepBlanks) {
					result.add("");
				}
				continue;
			}
			String key = keyOf(id);
			result.add(key != null ? key : id);
		}
		return result;
	}

	private String keyOf(String id) {
		if (rp_actions.containsKey(id)) {
			return id;
		}
		for (Map.Entry<String, RpAction> entry : rp_actions.entrySet()) {
			RpAction action = entry.getValue();
			if (action != null && id.equals(action.name)) {
				return entry.getKey();
			}
		}
		return null;
	}

	public Parameter parameter(String id) {
		for (Parameter p : parameters) {
			if (p.id != null && p.id.equalsIgnoreCase(id)) {
				return p;
			}
		}
		return null;
	}

	public QuickActionWindow window(String id) {
		for (QuickActionWindow w : quick_action_windows) {
			if (w.id != null && w.id.equalsIgnoreCase(id)) {
				return w;
			}
		}
		return null;
	}
}

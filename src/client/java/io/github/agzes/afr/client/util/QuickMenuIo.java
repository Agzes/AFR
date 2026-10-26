// SPDX-License-Identifier: GPL-3.0-only
package io.github.agzes.afr.client.util;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import io.github.agzes.afr.client.i18n.Lang;
import io.github.agzes.afr.client.input.KeyCombo;
import io.github.agzes.afr.client.model.RpAction;
import io.github.agzes.afr.client.model.RpProfile;
import java.util.ArrayList;
import java.util.List;

public final class QuickMenuIo {

	private QuickMenuIo() {}

	public static int importInto(RpProfile profile, JsonArray array) {
		int added = 0;
		for (JsonElement element : array) {
			if (!element.isJsonObject()) {
				continue;
			}
			JsonObject object = element.getAsJsonObject();
			String name = object.has("name")
				? object.get("name").getAsString()
				: "QuickMenu";
			List<List<String>> steps = new ArrayList<>();
			if (object.has("actions") && object.get("actions").isJsonArray()) {
				for (JsonElement actionElement : object.getAsJsonArray(
					"actions"
				)) {
					if (!actionElement.isJsonArray()) {
						continue;
					}
					JsonArray pair = actionElement.getAsJsonArray();
					if (
						pair.size() < 2 ||
						!pair.get(0).isJsonPrimitive() ||
						!pair.get(1).isJsonPrimitive()
					) {
						continue;
					}
					if (!"cmd".equalsIgnoreCase(pair.get(0).getAsString())) {
						continue;
					}
					String text = pair.get(1).getAsString();
					if (text == null || text.isBlank()) {
						continue;
					}
					steps.add(
						new ArrayList<>(
							List.of("RPAction", "Send", "S300", "S300", text)
						)
					);
				}
			}
			if (steps.isEmpty()) {
				continue;
			}
			RpAction action = new RpAction();
			action.name = name;
			action.description = Lang.t("qm.import_desc");
			if (object.has("icon") && object.get("icon").isJsonPrimitive()) {
				action.icon = object.get("icon").getAsString();
			}
			String bind = keybindName(object);
			if (bind != null) {
				action.bind = bind;
			}
			action.steps = steps;
			profile.rp_actions.put(uniqueId(profile, name), action);
			added++;
		}
		return added;
	}

	private static String uniqueId(RpProfile profile, String name) {
		String base = name.replaceAll("[^A-Za-z0-9_-]", "_");
		if (base.isBlank()) {
			base = "quickmenu";
		}
		String id = base;
		int index = 1;
		while (profile.rp_actions.containsKey(id)) {
			id = base + "_" + index++;
		}
		return id;
	}

	private static String keybindName(JsonObject object) {
		if (!object.has("keybind") || !object.get("keybind").isJsonArray()) {
			return null;
		}
		JsonArray keybind = object.getAsJsonArray("keybind");
		if (
			keybind.size() < 4 ||
			!keybind.get(0).isJsonPrimitive() ||
			!keybind.get(3).isJsonPrimitive()
		) {
			return null;
		}
		if (keybind.get(3).getAsInt() != 0) {
			return null;
		}
		int key = keybind.get(0).getAsInt();
		if (key <= 0) {
			return null;
		}
		return KeyCombo.name(key);
	}
}

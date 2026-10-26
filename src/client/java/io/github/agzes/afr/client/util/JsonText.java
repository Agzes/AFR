// SPDX-License-Identifier: GPL-3.0-only
package io.github.agzes.afr.client.util;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import io.github.agzes.afr.client.i18n.Lang;

public final class JsonText {

	private JsonText() {}

	public static String pick(JsonElement element, String fallback) {
		if (element == null || element.isJsonNull()) {
			return fallback;
		}
		if (element.isJsonPrimitive()) {
			return element.getAsString();
		}
		if (element.isJsonObject()) {
			JsonObject object = element.getAsJsonObject();
			String language = Lang.language();
			if (has(object, language)) {
				return object.get(language).getAsString();
			}
			if (has(object, "ru")) {
				return object.get("ru").getAsString();
			}
			if (has(object, "en")) {
				return object.get("en").getAsString();
			}
			for (var entry : object.entrySet()) {
				if (
					entry.getValue() != null && !entry.getValue().isJsonNull()
				) {
					return entry.getValue().getAsString();
				}
			}
		}
		return fallback;
	}

	private static boolean has(JsonObject object, String key) {
		return key != null && object.has(key) && !object.get(key).isJsonNull();
	}
}

// SPDX-License-Identifier: GPL-3.0-only
package io.github.agzes.afr.client.config;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import io.github.agzes.afr.AFR;

public final class MigrationEngine {

	public static final int CURRENT_SCHEMA = 1;

	private MigrationEngine() {}

	public static JsonObject migrate(JsonObject object, String kind) {
		int version = 0;
		JsonElement schema = object.get("schemaVersion");
		if (
			schema != null &&
			schema.isJsonPrimitive() &&
			schema.getAsJsonPrimitive().isNumber()
		) {
			version = schema.getAsInt();
		}

		if (version > CURRENT_SCHEMA) {
			AFR.LOGGER.warn(
				"[{}] schema {} is newer than the supported {}",
				kind,
				version,
				CURRENT_SCHEMA
			);
			return object;
		}

		while (version < CURRENT_SCHEMA) {
			int next = apply(version, object, kind);
			if (next <= version) {
				break;
			}
			version = next;
		}

		object.addProperty("schemaVersion", version);
		return object;
	}

	private static int apply(int from, JsonObject object, String kind) {
		return from + 1;
	}
}

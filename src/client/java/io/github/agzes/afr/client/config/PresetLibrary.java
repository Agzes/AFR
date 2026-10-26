// SPDX-License-Identifier: GPL-3.0-only
package io.github.agzes.afr.client.config;

import io.github.agzes.afr.client.model.RpProfile;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

public final class PresetLibrary {

	private PresetLibrary() {}

	public static RpProfile readBuiltIn(String name) throws IOException {
		try (
			InputStream stream = PresetLibrary.class.getResourceAsStream(
				"/assets/afr/presets/" + name + ".json"
			)
		) {
			if (stream == null) {
				return null;
			}
			try (
				InputStreamReader reader = new InputStreamReader(
					stream,
					StandardCharsets.UTF_8
				)
			) {
				return ConfigCodec.GSON.fromJson(reader, RpProfile.class);
			}
		}
	}
}

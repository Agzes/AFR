// SPDX-License-Identifier: GPL-3.0-only
package io.github.agzes.afr.client.net;

import io.github.agzes.afr.AFR;
import io.github.agzes.afr.client.hud.NotificationHud;
import io.github.agzes.afr.client.i18n.Lang;
import net.fabricmc.loader.api.FabricLoader;

public final class VersionChecker {

	private VersionChecker() {}

	public static void checkAsync() {
		String channel = "stable";
		AfrCloud.version("afr", channel)
			.thenAccept(version -> {
				int remote = version.has("version_code")
					? version.get("version_code").getAsInt()
					: 0;
				String remoteName = version.has("version")
					? version.get("version").getAsString()
					: "";
				int current = currentVersionCode();
				AfrCloud.runOnClient(() -> {
					if (remote > current) {
						NotificationHud.notify(
							NotificationHud.Type.INFO,
							"AFR",
							Lang.t("msg.update_available") + ": " + remoteName
						);
					} else {
						AFR.LOGGER.info(
							"AFR: no updates ({} >= {})",
							current,
							remote
						);
					}
				});
			})
			.exceptionally(error -> {
				AFR.LOGGER.warn(
					"AFR: update check failed: {}",
					error.getMessage()
				);
				return null;
			});
	}

	public static int currentVersionCode() {
		try {
			String version = FabricLoader.getInstance()
				.getModContainer("afr")
				.map(container ->
					container.getMetadata().getVersion().getFriendlyString()
				)
				.orElse("0");
			return parseCode(version);
		} catch (Exception e) {
			return 0;
		}
	}

	public static int parseCode(String version) {
		String clean = version.contains("-") ? version.split("-")[0] : version;
		String[] parts = clean.split("\\.");
		int major = parts.length > 0 ? safe(parts[0]) : 0;
		int minor = parts.length > 1 ? safe(parts[1]) : 0;
		int patch = parts.length > 2 ? safe(parts[2]) : 0;
		return major * 10000 + minor * 100 + patch;
	}

	private static int safe(String value) {
		try {
			return Integer.parseInt(value.replaceAll("[^0-9]", ""));
		} catch (Exception e) {
			return 0;
		}
	}
}

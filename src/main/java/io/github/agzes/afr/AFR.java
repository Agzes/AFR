// SPDX-License-Identifier: GPL-3.0-only
package io.github.agzes.afr;

import net.fabricmc.api.ModInitializer;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.config.Configurator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AFR implements ModInitializer {

	public static final String MOD_ID = "afr";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {}

	public static void setDebugLogging(boolean enabled) {
		try {
			Configurator.setLevel(MOD_ID, enabled ? Level.DEBUG : Level.INFO);
		} catch (Throwable ignored) {}
	}
}

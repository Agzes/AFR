// SPDX-License-Identifier: GPL-3.0-only
package io.github.agzes.afr.client;

import com.mojang.brigadier.CommandDispatcher;
import io.github.agzes.afr.AFR;
import io.github.agzes.afr.client.i18n.Lang;
import io.github.agzes.afr.client.model.RpProfile;
import io.github.agzes.afr.client.ui.ConfigScreen;
import io.github.agzes.afr.client.ui.LibraryScreen;
import io.github.agzes.afr.client.ui.VersionScreen;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.network.chat.Component;

public final class AfrCommands {

	private AfrCommands() {}

	public static void register(
		CommandDispatcher<FabricClientCommandSource> dispatcher
	) {
		AFR.LOGGER.info("AFR: registering client commands");
		dispatcher.register(
			ClientCommandManager.literal("afr")
				.executes(context -> {
					openLater(() -> ConfigScreen.open(null));
					return 1;
				})
				.then(
					ClientCommandManager.literal("reload").executes(context -> {
						AfrRuntime.get().reload();
						feedback(context.getSource(), Lang.t("cmd.reloaded"));
						return 1;
					})
				)
				.then(
					ClientCommandManager.literal("menu").executes(context -> {
						openLater(() -> AfrRuntime.get().openMenu());
						return 1;
					})
				)
				.then(
					ClientCommandManager.literal("stop").executes(context -> {
						AfrRuntime.get().stopAction();
						feedback(context.getSource(), Lang.t("cmd.stopped"));
						return 1;
					})
				)
				.then(
					ClientCommandManager.literal("pause").executes(context -> {
						AfrRuntime.get().togglePause();
						feedback(
							context.getSource(),
							Lang.t("cmd.hotkeys") +
								": " +
								(AfrRuntime.get().paused()
									? Lang.t("cmd.paused")
									: Lang.t("cmd.enabled"))
						);
						return 1;
					})
				)
				.then(
					ClientCommandManager.literal("editor").executes(context -> {
						openLater(() -> ConfigScreen.open(null));
						return 1;
					})
				)
				.then(
					ClientCommandManager.literal("library").executes(
						context -> {
							openLater(() -> LibraryScreen.open(null));
							return 1;
						}
					)
				)
				.then(
					ClientCommandManager.literal("updates").executes(
						context -> {
							openLater(() -> VersionScreen.open(null));
							return 1;
						}
					)
				)
				.then(
					ClientCommandManager.literal("status").executes(context -> {
						AfrRuntime runtime = AfrRuntime.get();
						RpProfile profile = runtime.config().active();
						feedback(
							context.getSource(),
							Lang.t("cmd.profile") +
								": " +
								(profile != null ? profile.meta.label : "-") +
								" • " +
								Lang.t("cmd.actions") +
								": " +
								(profile != null
									? profile.rp_actions.size()
									: 0) +
								" • " +
								Lang.t("cmd.running") +
								": " +
								runtime.executor().isRunning()
						);
						return 1;
					})
				)
				.then(
					ClientCommandManager.literal("help").executes(context -> {
						for (String line : Lang.t("cmd.help").split("\n")) {
							feedback(context.getSource(), line);
						}
						return 1;
					})
				)
		);
	}

	private static void openLater(Runnable action) {
		AfrClient.scheduleScreen(action);
	}

	private static void feedback(
		FabricClientCommandSource source,
		String message
	) {
		source.sendFeedback(Component.literal("§bAFR §8• §f" + message));
	}
}

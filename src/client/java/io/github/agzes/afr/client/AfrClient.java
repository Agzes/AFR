// SPDX-License-Identifier: GPL-3.0-only
package io.github.agzes.afr.client;

import com.google.gson.JsonObject;
import com.mojang.blaze3d.platform.InputConstants;
import io.github.agzes.afr.AFR;
import io.github.agzes.afr.client.hud.NotificationHud;
import io.github.agzes.afr.client.hud.ProgressHud;
import io.github.agzes.afr.client.hud.StatusHud;
import io.github.agzes.afr.client.model.UserSettings;
import io.github.agzes.afr.client.net.AfrCloud;
import io.github.agzes.afr.client.net.VersionChecker;
import io.github.agzes.afr.client.ui.AfrMenuButton;
import io.github.agzes.afr.client.ui.ConfigScreen;
import io.github.agzes.afr.client.ui.SetupWizardScreen;
import io.github.agzes.afr.client.ui.TutorialScreen;
import io.github.agzes.afr.client.util.JsonText;
import io.github.agzes.afr.client.util.VersionMatch;
import java.util.ArrayList;
import java.util.List;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.Screens;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.SharedConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

public class AfrClient implements ClientModInitializer {

	private static KeyMapping editorKey;
	private static boolean languagePrompted = false;
	private static long smartCheckAt = 0;
	private static boolean mouseSidePrev4 = false;
	private static boolean mouseSidePrev5 = false;

	public static final List<Runnable> PENDING_SCREENS = new ArrayList<>();

	public static void scheduleScreen(Runnable action) {
		if (action != null) {
			PENDING_SCREENS.add(action);
		}
	}

	private static void runPendingScreens() {
		if (PENDING_SCREENS.isEmpty()) {
			return;
		}
		List<Runnable> copy = new ArrayList<>(PENDING_SCREENS);
		PENDING_SCREENS.clear();
		for (Runnable action : copy) {
			try {
				action.run();
			} catch (Throwable error) {
				AFR.LOGGER.error(
					"AFR: failed to open a screen from a command",
					error
				);
			}
		}
	}

	public static void setEditorKey(int keyCode) {
		if (editorKey != null) {
			editorKey.setKey(InputConstants.Type.KEYSYM.getOrCreate(keyCode));
			KeyMapping.resetMapping();
		}
	}

	@Override
	public void onInitializeClient() {
		AfrRuntime runtime = AfrRuntime.init();
		String version = FabricLoader.getInstance()
			.getModContainer("afr")
			.map(c -> c.getMetadata().getVersion().getFriendlyString())
			.orElse("?");
		AFR.LOGGER.info(
			"AFR initialized (version {}, Minecraft {})",
			version,
			SharedConstants.getCurrentVersion().getName()
		);

		editorKey = KeyBindingHelper.registerKeyBinding(
			new KeyMapping(
				"key.afr.editor",
				InputConstants.Type.KEYSYM,
				runtime.config().settings().editorKey,
				"key.categories.afr"
			)
		);

		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			runPendingScreens();
			runtime.executor().tick();
			runtime.executor().watchdog();
			runtime.afterTick();
			if (client.player != null && client.player.isDeadOrDying()) {
				runtime.stopForDeath();
			}
			if (
				!languagePrompted &&
				!runtime.config().settings().languageChosen &&
				client.screen instanceof TitleScreen
			) {
				languagePrompted = true;
				client.setScreen(new SetupWizardScreen(client.screen, 0));
			}
			while (editorKey.consumeClick()) {
				if (client.screen == null) {
					runtime.markInteraction();
					ConfigScreen.open(null);
				}
			}
			if (runtime.config().settings().smartUpdateCheck) {
				long now = System.currentTimeMillis();
				if (smartCheckAt == 0) {
					smartCheckAt = now;
				} else if (
					now - smartCheckAt >= 30 * 60_000L &&
					now - runtime.lastInteraction() <= 5 * 60_000L
				) {
					smartCheckAt = now;
					AFR.LOGGER.info("AFR: smart update check (user is active)");
					if (runtime.config().settings().checkUpdates) {
						VersionChecker.checkAsync();
					}
					if (runtime.config().settings().autoUpdateConfigs) {
						runtime.checkConfigUpdate(true);
					}
					refreshAnnouncements(runtime);
					runtime.checkSeasonal();
				}
			} else {
				smartCheckAt = 0;
			}
			boolean side4 = false;
			boolean side5 = false;
			if (client.getWindow() != null) {
				long window = client.getWindow().getWindow();
				side4 =
					GLFW.glfwGetMouseButton(window, GLFW.GLFW_MOUSE_BUTTON_4) ==
					GLFW.GLFW_PRESS;
				side5 =
					GLFW.glfwGetMouseButton(window, GLFW.GLFW_MOUSE_BUTTON_5) ==
					GLFW.GLFW_PRESS;
			}
			if (side4 && !mouseSidePrev4) {
				handleSideButton(runtime, client, 1004, "Mouse4");
			}
			if (side5 && !mouseSidePrev5) {
				handleSideButton(runtime, client, 1005, "Mouse5");
			}
			mouseSidePrev4 = side4;
			mouseSidePrev5 = side5;
		});

		HudRenderCallback.EVENT.register((context, tickDelta) -> {
			StatusHud.render(context, tickDelta);
			ProgressHud.render(context, tickDelta);
			NotificationHud.render(context, tickDelta);
		});

		ClientCommandRegistrationCallback.EVENT.register((dispatcher, access) ->
			AfrCommands.register(dispatcher)
		);

		ScreenEvents.AFTER_INIT.register(
			(client, screen, scaledWidth, scaledHeight) -> {
				ScreenEvents.afterRender(screen).register(
					(s, context, mouseX, mouseY, tickDelta) ->
						NotificationHud.render(context, tickDelta)
				);
				if (
					!screen
						.getClass()
						.getName()
						.startsWith("io.github.agzes.afr")
				) {
					runtime.stopForScreen();
				} else {
					runtime.markInteraction();
				}
				if (screen instanceof TitleScreen) {
					var buttons = Screens.getButtons(screen);
					AbstractWidget multiplayer = null;
					AbstractWidget realms = null;
					String multiplayerText = Component.translatable(
						"menu.multiplayer"
					).getString();
					String realmsText = Component.translatable(
						"menu.online"
					).getString();
					for (AbstractWidget widget : buttons) {
						String text = widget.getMessage().getString();
						if (text.equals(multiplayerText)) {
							multiplayer = widget;
						} else if (text.equals(realmsText)) {
							realms = widget;
						}
					}
					int x;
					int y;
					if (multiplayer != null) {
						x = multiplayer.getX() - 26;
						y = multiplayer.getY();
					} else if (realms != null) {
						x = realms.getX() + realms.getWidth() + 6;
						y = realms.getY();
					} else {
						x = screen.width / 2 + 108;
						y = screen.height / 4 + 48;
					}
					buttons.add(
						new AfrMenuButton(x, y, b -> ConfigScreen.open(screen))
					);
				} else if (screen instanceof PauseScreen) {
					Screens.getButtons(screen).add(
						new AfrMenuButton(
							screen.width / 2 + 104,
							screen.height / 4 + 48,
							b -> ConfigScreen.open(screen)
						)
					);
				}
			}
		);

		ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {
			runtime.stopForWorld();
			AFR.LOGGER.debug(
				"AFR: joined world, triggering update/announcement/seasonal checks"
			);
			if (runtime.config().settings().checkUpdates) {
				VersionChecker.checkAsync();
			}
			if (runtime.config().settings().autoUpdateConfigs) {
				runtime.checkConfigUpdate(true);
			}
			checkAnnouncements(runtime);
			runtime.checkSeasonal();
		});

		ClientPlayConnectionEvents.DISCONNECT.register((handler, client) ->
			runtime.stopForWorld()
		);
	}

	private static boolean announcementsRequested = false;
	private static boolean seasonalRequested = false;
	private static boolean announcementsInFlight = false;

	private record PendingNotice(
		String id,
		NotificationHud.Type type,
		String title,
		String text
	) {}

	public static void refreshAnnouncements(AfrRuntime runtime) {
		checkAnnouncements(runtime);
	}

	private static void handleSideButton(
		AfrRuntime runtime,
		Minecraft client,
		int code,
		String name
	) {
		if (client.screen instanceof ConfigScreen cs && cs.isCapturing()) {
			cs.captureMouseBind(name);
			return;
		}
		if (client.screen instanceof SetupWizardScreen sw && sw.isCapturing()) {
			sw.captureMouseBind(name);
			return;
		}
		if (client.screen instanceof TutorialScreen ts && ts.isCapturing()) {
			ts.captureMouseBind(name);
			return;
		}
		if (client.player == null || client.screen != null) {
			return;
		}
		runtime.handleKey(code, 0);
	}

	public static void refreshOpenEditor() {
		Minecraft client = Minecraft.getInstance();
		if (client.screen instanceof ConfigScreen editor && editor.tab() == 0) {
			editor.rebuild();
		}
	}

	public static void ensureAnnouncements(AfrRuntime runtime) {
		if (!seasonalRequested) {
			seasonalRequested = true;
			runtime.checkSeasonal();
		}
		if (announcementsRequested || !runtime.announcements.isEmpty()) {
			return;
		}
		announcementsRequested = true;
		checkAnnouncements(runtime);
	}

	private static void checkAnnouncements(AfrRuntime runtime) {
		if (announcementsInFlight) {
			return;
		}
		announcementsInFlight = true;
		UserSettings settings = runtime.config().settings();
		List<String> known = new ArrayList<>();
		if (settings.seenAnnouncements != null) {
			known.addAll(settings.seenAnnouncements);
		}
		if (
			known.isEmpty() &&
			settings.lastAnnouncementId != null &&
			!settings.lastAnnouncementId.isBlank()
		) {
			known.add(settings.lastAnnouncementId);
		}
		AfrCloud.announcements()
			.thenAccept(array -> {
				List<JsonObject> collected = new ArrayList<>();
				List<PendingNotice> pending = new ArrayList<>();
				int current = VersionChecker.currentVersionCode();
				array.forEach(element -> {
					if (!element.isJsonObject()) {
						return;
					}
					var object = element.getAsJsonObject();
					if (
						object.has("enabled") &&
						!object.get("enabled").getAsBoolean()
					) {
						return;
					}
					if (!matchesVersion(object, current)) {
						return;
					}
					String id = object.has("id")
						? object.get("id").getAsString()
						: "";
					String title = JsonText.pick(object.get("title"), "AFR");
					String text = JsonText.pick(object.get("text"), "");
					boolean deprecated =
						object.has("deprecated") &&
						object.get("deprecated").getAsBoolean();
					String severity = deprecated
						? "deprecated"
						: object.has("severity")
							? object.get("severity").getAsString().toLowerCase()
							: "info";
					collected.add(object);
					if (id.isBlank() || known.contains(id)) {
						return;
					}
					known.add(id);
					pending.add(
						new PendingNotice(
							id,
							"info".equals(severity)
								? NotificationHud.Type.INFO
								: NotificationHud.Type.WARNING,
							title,
							text
						)
					);
				});
				AfrCloud.runOnClient(() -> {
					UserSettings live = runtime.config().settings();
					if (live.seenAnnouncements == null) {
						live.seenAnnouncements = new ArrayList<>();
					}
					boolean changed = false;
					if (
						live.seenAnnouncements.isEmpty() &&
						live.lastAnnouncementId != null &&
						!live.lastAnnouncementId.isBlank()
					) {
						live.seenAnnouncements.add(live.lastAnnouncementId);
						changed = true;
					}
					for (PendingNotice notice : pending) {
						if (live.seenAnnouncements.contains(notice.id())) {
							continue;
						}
						live.seenAnnouncements.add(notice.id());
						changed = true;
						NotificationHud.notify(
							notice.type(),
							notice.title(),
							notice.text()
						);
					}
					if (changed) {
						runtime.config().saveSettings();
					}
					runtime.announcements.clear();
					runtime.announcements.addAll(collected);
					refreshOpenEditor();
					announcementsInFlight = false;
				});
			})
			.exceptionally(error -> {
				announcementsInFlight = false;
				return null;
			});
	}

	private static boolean matchesVersion(JsonObject object, int current) {
		if (object.has("versions") && object.get("versions").isJsonArray()) {
			boolean any = false;
			for (var version : object.getAsJsonArray("versions")) {
				if (VersionMatch.code(version) == current) {
					any = true;
					break;
				}
			}
			if (!any) {
				return false;
			}
		}
		if (object.has("version") && !object.get("version").isJsonNull()) {
			var element = object.get("version");
			if (element.isJsonArray()) {
				for (var condition : element.getAsJsonArray()) {
					if (
						!VersionMatch.matches(condition.getAsString(), current)
					) {
						return false;
					}
				}
			} else if (!VersionMatch.matches(element.getAsString(), current)) {
				return false;
			}
		}
		if (
			object.has("min_version") &&
			!object.get("min_version").isJsonNull() &&
			current < VersionMatch.code(object.get("min_version"))
		) {
			return false;
		}
		if (
			object.has("max_version") &&
			!object.get("max_version").isJsonNull() &&
			current > VersionMatch.code(object.get("max_version"))
		) {
			return false;
		}
		return true;
	}
}

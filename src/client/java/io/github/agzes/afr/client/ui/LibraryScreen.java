// SPDX-License-Identifier: GPL-3.0-only
package io.github.agzes.afr.client.ui;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import io.github.agzes.afr.AFR;
import io.github.agzes.afr.client.AfrRuntime;
import io.github.agzes.afr.client.config.ConfigPaths;
import io.github.agzes.afr.client.config.PresetLibrary;
import io.github.agzes.afr.client.hud.NotificationHud;
import io.github.agzes.afr.client.i18n.Lang;
import io.github.agzes.afr.client.model.RpProfile;
import io.github.agzes.afr.client.net.AfrCloud;
import io.github.agzes.afr.client.net.VersionChecker;
import io.github.agzes.afr.client.util.VersionStore;
import io.wispforest.owo.ui.base.BaseOwoScreen;
import io.wispforest.owo.ui.component.Components;
import io.wispforest.owo.ui.component.LabelComponent;
import io.wispforest.owo.ui.container.Containers;
import io.wispforest.owo.ui.container.FlowLayout;
import io.wispforest.owo.ui.core.Color;
import io.wispforest.owo.ui.core.HorizontalAlignment;
import io.wispforest.owo.ui.core.Insets;
import io.wispforest.owo.ui.core.OwoUIAdapter;
import io.wispforest.owo.ui.core.Sizing;
import io.wispforest.owo.ui.core.Surface;
import io.wispforest.owo.ui.core.VerticalAlignment;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

public class LibraryScreen extends BaseOwoScreen<FlowLayout> {

	private final Screen parent;
	private final List<JsonObject> entries;
	private int windowWidth = 520;

	private FlowLayout listHost;
	private String filter = "";
	private String mode = "all";
	private boolean refreshing = false;

	public LibraryScreen(Screen parent, List<JsonObject> entries) {
		this(parent, entries, "", "all", false);
	}

	public LibraryScreen(
		Screen parent,
		List<JsonObject> entries,
		String filter,
		String mode
	) {
		this(parent, entries, filter, mode, false);
	}

	public LibraryScreen(
		Screen parent,
		List<JsonObject> entries,
		String filter,
		String mode,
		boolean refreshing
	) {
		super(Component.literal("AFR"));
		this.parent = parent;
		this.entries = entries == null ? null : withBuiltins(entries);
		this.filter = filter == null ? "" : filter;
		this.mode = mode == null ? "all" : mode;
		this.refreshing = refreshing;
	}

	private static List<JsonObject> withBuiltins(List<JsonObject> entries) {
		List<JsonObject> result = new ArrayList<>();
		result.add(builtinDemo());
		for (JsonObject entry : entries) {
			if (!"demo".equals(getString(entry, "id", ""))) {
				result.add(entry);
			}
		}
		return result;
	}

	private static JsonObject builtinDemo() {
		JsonObject o = new JsonObject();
		o.addProperty("id", "demo");
		o.addProperty("name", "AFR Demo");
		o.addProperty("description", Lang.t("lib.demo_desc"));
		o.addProperty("author", "Agzes");
		JsonArray authors = new JsonArray();
		authors.add("Agzes");
		authors.add("AFR Team");
		o.add("authors", authors);
		o.addProperty("version", "1.0.0");
		o.addProperty("version_code", 10000);
		o.addProperty("icon", "*");
		JsonArray tags = new JsonArray();
		tags.add("demo");
		tags.add("tutorial");
		o.add("tags", tags);
		o.addProperty("downloads", 0);
		o.addProperty("min_afr_version", 0);
		o.addProperty("builtin", "demo");
		return o;
	}

	public static void open(Screen parent) {
		Minecraft.getInstance().setScreen(
			new LibraryScreen(parent, null, "", "all", false)
		);
	}

	public static void refresh(Screen parent) {
		Minecraft.getInstance().setScreen(
			new LibraryScreen(parent, null, "", "all", true)
		);
	}

	@Override
	protected @NotNull OwoUIAdapter<FlowLayout> createAdapter() {
		return OwoUIAdapter.create(this, Containers::verticalFlow);
	}

	@Override
	protected void build(FlowLayout root) {
		root.surface(Surface.flat(Ui.BACKDROP));
		root.horizontalAlignment(HorizontalAlignment.CENTER);
		root.verticalAlignment(VerticalAlignment.CENTER);

		int width = Math.max(380, Math.min(this.width - 16, 540));
		int height = Math.max(220, Math.min(this.height - 16, 300));
		windowWidth = width;

		FlowLayout window = Containers.verticalFlow(
			Sizing.fixed(width),
			Sizing.fixed(height)
		);
		window.surface(Ui.window());
		window.gap(0);

		FlowLayout header = Containers.horizontalFlow(
			Sizing.fill(100),
			Sizing.content()
		);
		header.surface(Ui.header());
		header.padding(Insets.of(5));
		header.horizontalAlignment(HorizontalAlignment.CENTER);
		header.child(
			Components.label(
				Component.literal(Icons.CLOUD + "  " + Lang.t("sec.library"))
			).color(Color.ofArgb(Ui.SECTION))
		);
		window.child(header);

		FlowLayout toolbar = Containers.horizontalFlow(
			Sizing.fill(100),
			Sizing.content()
		);
		toolbar.gap(6);
		toolbar.verticalAlignment(VerticalAlignment.CENTER);
		toolbar.padding(Insets.of(Ui.PAD, Ui.PAD, Ui.PAD, Ui.PAD));
		AfrInput search = AfrInput.expand(filter);
		search.hint(Lang.t("lib.search_hint"));
		search.onChanged(value -> {
			filter = value == null ? "" : value.strip().toLowerCase();
			populate();
		});
		toolbar.child(search.component());
		window.child(toolbar);

		FlowLayout content = Containers.verticalFlow(
			Sizing.fill(100),
			Sizing.content()
		);
		content.gap(Ui.GAP);
		content.padding(Insets.of(0, Ui.PAD, Ui.PAD, Ui.PAD));

		if (entries != null && !entries.isEmpty()) {
			FlowLayout modes = Containers.horizontalFlow(
				Sizing.content(),
				Sizing.content()
			);
			modes.gap(4);
			String[][] modeOptions = {
				{ "all", Lang.t("lib.mode_all") },
				{ "installed", Lang.t("lib.mode_installed") },
				{ "updates", Lang.t("lib.mode_updates") },
			};
			for (String[] option : modeOptions) {
				final String nextMode = option[0];
				AfrButton chip = new AfrButton(
					Component.literal(option[1]),
					AfrButton.Variant.SECONDARY,
					() ->
						minecraft.setScreen(
							new LibraryScreen(parent, entries, filter, nextMode)
						)
				);
				chip.active(nextMode.equals(mode));
				modes.child(chip);
			}
			content.child(modes);

			LinkedHashSet<String> tagSet = new LinkedHashSet<>();
			for (JsonObject entry : entries) {
				if (entry.has("tags") && entry.get("tags").isJsonArray()) {
					entry
						.getAsJsonArray("tags")
						.forEach(tag -> tagSet.add(tag.getAsString()));
				}
			}
			if (!tagSet.isEmpty()) {
				FlowLayout tagsRow = Containers.horizontalFlow(
					Sizing.content(),
					Sizing.content()
				);
				tagsRow.gap(4);
				for (String tag : tagSet) {
					tagsRow.child(
						new AfrButton(
							Component.literal("#" + tag),
							AfrButton.Variant.SECONDARY,
							() ->
								minecraft.setScreen(
									new LibraryScreen(
										parent,
										entries,
										tag.toLowerCase(),
										mode
									)
								)
						).tip(Lang.t("tip.tag"))
					);
				}
				content.child(
					Containers.horizontalScroll(
						Sizing.fill(100),
						Sizing.fixed(18),
						tagsRow
					)
				);
			}
		}

		listHost = Containers.verticalFlow(Sizing.fill(100), Sizing.content());
		listHost.gap(Ui.GAP);
		content.child(listHost);
		window.child(
			new AfrScroll(Sizing.fill(100), Sizing.expand(100), content)
		);

		FlowLayout footer = Containers.horizontalFlow(
			Sizing.fill(100),
			Sizing.content()
		);
		footer.surface(Ui.footer());
		footer.padding(Insets.of(5));
		footer.gap(6);
		footer.verticalAlignment(VerticalAlignment.CENTER);
		footer.child(
			Ui.dim(
				entries == null
					? Lang.t("lbl.loading")
					: entries.size() +
							" " +
							Lang.t("lib.count") +
							" • " +
							Lang.t("lib.updates_count") +
							": " +
							updateCount()
			)
		);
		footer.child(Ui.grow());
		footer.child(
			new AfrButton(
				Component.literal(Icons.RELOAD + " " + Lang.t("btn.refresh")),
				AfrButton.Variant.SECONDARY,
				() -> {
					NotificationHud.notify(
						NotificationHud.Type.INFO,
						Lang.t("lbl.library"),
						Lang.t("lib.refreshing")
					);
					LibraryScreen.refresh(parent);
				}
			).tip(Lang.t("btn.refresh"))
		);
		footer.child(
			new AfrButton(
				Component.literal(Lang.t("btn.close")),
				AfrButton.Variant.SECONDARY,
				this::onClose
			)
		);
		window.child(footer);

		root.child(window);

		if (entries == null) {
			Screen expected = Minecraft.getInstance().screen;
			AfrCloud.library()
				.thenAccept(lib -> {
					JsonObject json = lib.json();
					JsonArray array = json.has("configs")
						? json.getAsJsonArray("configs")
						: new JsonArray();
					List<JsonObject> list = new ArrayList<>();
					array.forEach(element -> {
						if (element.isJsonObject()) {
							list.add(element.getAsJsonObject());
						}
					});
					AfrCloud.runOnClient(() -> {
						if (Minecraft.getInstance().screen != expected) {
							return;
						}
						if (lib.fromCache()) {
							NotificationHud.notify(
								NotificationHud.Type.WARNING,
								Lang.t("lbl.library"),
								Lang.t("lib.cache_used")
							);
						} else if (refreshing) {
							NotificationHud.notify(
								NotificationHud.Type.SUCCESS,
								Lang.t("lbl.library"),
								Lang.t("lib.refreshed") + ": " + list.size()
							);
						}
						Minecraft.getInstance().setScreen(
							new LibraryScreen(parent, list, filter, mode, false)
						);
					});
				})
				.exceptionally(error -> {
					AfrCloud.runOnClient(() ->
						NotificationHud.notify(
							NotificationHud.Type.ERROR,
							Lang.t("lbl.library"),
							Lang.t("msg.library_error")
						)
					);
					return null;
				});
		} else {
			populate();
		}
	}

	private void populate() {
		if (listHost == null || entries == null) {
			return;
		}
		listHost.clearChildren();
		List<JsonObject> list = new ArrayList<>();
		for (JsonObject entry : entries) {
			String haystack = (
				getString(entry, "name", "") +
				" " +
				getString(entry, "author", "") +
				" " +
				getString(entry, "description", "") +
				" " +
				tags(entry)
			).toLowerCase();
			if (!filter.isBlank() && !haystack.contains(filter)) {
				continue;
			}
			if (!matchesMode(entry)) {
				continue;
			}
			list.add(entry);
		}
		list.sort(
			Comparator.comparing(
				e -> getString(e, "name", ""),
				String.CASE_INSENSITIVE_ORDER
			)
		);
		if (list.isEmpty()) {
			listHost.child(Ui.dim(Lang.t("lbl.empty_list")));
			return;
		}
		for (JsonObject entry : list) {
			listHost.child(entryRow(entry));
		}
		listHost.child(
			Containers.horizontalFlow(Sizing.fill(100), Sizing.fixed(10))
		);
	}

	private boolean matchesMode(JsonObject entry) {
		if ("all".equals(mode)) {
			return true;
		}
		RpProfile local = findInstalled(getString(entry, "id", ""));
		if (local == null) {
			return false;
		}
		if ("installed".equals(mode)) {
			return true;
		}
		int remoteCode = getInt(entry, "version_code", 0);
		return remoteCode > local.meta.versionCode;
	}

	private FlowLayout entryRow(JsonObject entry) {
		FlowLayout card = Containers.verticalFlow(
			Sizing.fill(100),
			Sizing.content()
		);
		card.surface(Ui.cardSurface());
		card.padding(Insets.of(Ui.CARD_PAD));
		card.gap(4);

		String name = getString(entry, "name", Lang.t("lib.no_title"));
		String version = getString(entry, "version", "");
		String icon = getString(entry, "icon", "");
		String description = getString(entry, "description", "");
		int downloads = getInt(entry, "downloads", 0);
		int minVersion = getInt(entry, "min_afr_version", 0);
		int remoteCode = getInt(entry, "version_code", 0);
		boolean compatible = minVersion <= VersionChecker.currentVersionCode();
		RpProfile local = findInstalled(getString(entry, "id", ""));
		boolean installed = local != null;
		boolean hasUpdate = installed && remoteCode > local.meta.versionCode;

		FlowLayout top = Containers.horizontalFlow(
			Sizing.fill(100),
			Sizing.content()
		);
		top.gap(6);
		top.verticalAlignment(VerticalAlignment.CENTER);
		io.wispforest.owo.ui.core.Component iconComponent = entryIcon(icon);
		if (iconComponent != null) {
			top.child(iconComponent);
		}
		top.child(
			Ui.label(
				TextFit.fit(
					Minecraft.getInstance().font,
					name + (version.isBlank() ? "" : "  v" + version),
					windowWidth - 220
				)
			)
		);
		top.child(Ui.grow());
		String preview = getString(entry, "preview_url", "");
		if (preview.isBlank()) {
			preview = "config.html?id=" + getString(entry, "id", "");
		}
		final String previewUrl = AfrCloud.resolveSite(preview);
		top.child(
			new AfrButton(
				Component.literal(Icons.EDIT + " " + Lang.t("btn.preview")),
				AfrButton.Variant.SECONDARY,
				() -> Ui.openUrl(previewUrl)
			).tip(Lang.t("tip.preview"))
		);
		AfrButton install = new AfrButton(
			Component.literal(
				hasUpdate
					? Lang.t("btn.update")
					: installed
						? Lang.t("btn.installed")
						: Lang.t("btn.install")
			),
			AfrButton.Variant.PRIMARY,
			() ->
				install(
					getString(entry, "id", ""),
					name,
					getString(entry, "config_url", ""),
					remoteCode,
					getString(entry, "builtin", "")
				)
		);
		if (!compatible) {
			install
				.enabled(false)
				.tip(Lang.t("lib.requires") + " " + minVersion);
		} else if (installed && !hasUpdate) {
			install
				.enabled(false)
				.tip(Lang.t("lib.already") + " (v" + local.meta.version + ")");
		} else {
			install.tip(
				hasUpdate ? Lang.t("lib.update_profile") : Lang.t("lib.install")
			);
		}
		top.child(install);
		if (installed) {
			top.child(
				new AfrButton(
					Component.literal(Icons.LIBRARY),
					AfrButton.Variant.SECONDARY,
					() ->
						minecraft.setScreen(
							new VersionHistoryScreen(parent, local)
						)
				).tip(Lang.t("tip.history"))
			);
			top.child(
				new AfrButton(
					Component.literal(Icons.DELETE),
					AfrButton.Variant.DANGER,
					() -> confirmUninstall(local)
				).tip(Lang.t("tip.uninstall"))
			);
		}
		card.child(top);

		StringBuilder meta = new StringBuilder();
		String credits = entryCredits(entry);
		if (!credits.isBlank()) {
			meta.append(Lang.t("lib.by") + " ").append(credits);
		}
		String tags = tags(entry);
		if (!tags.isBlank()) {
			meta.append(meta.length() > 0 ? "  •  " : "").append(tags);
		}
		if (installed) {
			meta.append(meta.length() > 0 ? "  •  " : "")
				.append(Lang.t("lib.installed") + " v")
				.append(local.meta.version);
		}
		if (hasUpdate) {
			meta.append("  •  " + Lang.t("lib.update_to") + " v").append(
				version
			);
		}
		if (!compatible) {
			meta.append(meta.length() > 0 ? "  •  " : "").append(
				Lang.t("lib.incompatible")
			);
		}
		if (meta.length() > 0) {
			card.child(
				Ui.dim(
					TextFit.fit(
						Minecraft.getInstance().font,
						meta.toString(),
						windowWidth - 40
					)
				)
			);
		}
		if (!description.isBlank()) {
			LabelComponent descLabel = Ui.dim(
				TextFit.fit(
					Minecraft.getInstance().font,
					description,
					windowWidth - 40
				)
			);
			descLabel.tooltip(Component.literal(description));
			card.child(descLabel);
		}
		return card;
	}

	private void confirmUninstall(RpProfile local) {
		minecraft.setScreen(
			new ConfirmScreen(
				Lang.t("confirm.delete"),
				Lang.t("msg.confirm_uninstall") +
					" \"" +
					local.meta.label +
					"\"?",
				Lang.t("btn.uninstall"),
				() -> uninstall(local),
				() -> minecraft.setScreen(new LibraryScreen(parent, entries))
			)
		);
	}

	private void uninstall(RpProfile local) {
		var config = AfrRuntime.get().config();
		config.profiles().remove(local.meta.revName);
		try {
			Files.deleteIfExists(ConfigPaths.profile(local.meta.revName));
		} catch (IOException e) {
			AFR.LOGGER.warn("Failed to delete the profile file", e);
		}
		if (
			local.meta.revName.equals(config.activeId()) &&
			!config.profiles().isEmpty()
		) {
			config.setActive(config.profiles().keySet().iterator().next());
		}
		config.ensureProfile();
		AfrRuntime.get().rebuildBinds();
		AFR.LOGGER.info("AFR: uninstalled config '{}'", local.meta.revName);
		NotificationHud.notify(
			NotificationHud.Type.SUCCESS,
			Lang.t("lbl.library"),
			Lang.t("lib.uninstalled") + ": " + local.meta.label
		);
		minecraft.setScreen(new LibraryScreen(parent, entries));
	}

	private void install(
		String id,
		String fallbackName,
		String url,
		int remoteCode,
		String builtin
	) {
		if (builtin != null && !builtin.isBlank()) {
			try {
				RpProfile profile = PresetLibrary.readBuiltIn(builtin);
				if (profile == null) {
					NotificationHud.notify(
						NotificationHud.Type.ERROR,
						Lang.t("lbl.library"),
						Lang.t("msg.invalid_config")
					);
					return;
				}
				applyInstalled(profile, id, fallbackName, remoteCode);
			} catch (IOException e) {
				AFR.LOGGER.warn(
					"AFR: failed to load the built-in config '{}'",
					builtin,
					e
				);
				NotificationHud.notify(
					NotificationHud.Type.ERROR,
					Lang.t("lbl.library"),
					Lang.t("msg.install_error")
				);
			}
			return;
		}
		if (url == null || url.isBlank()) {
			NotificationHud.notify(
				NotificationHud.Type.WARNING,
				Lang.t("lbl.library"),
				Lang.t("msg.no_link")
			);
			return;
		}
		NotificationHud.notify(
			NotificationHud.Type.INFO,
			Lang.t("lbl.library"),
			Lang.t("lib.downloading")
		);
		AfrCloud.downloadConfig(url)
			.thenAccept(profile ->
				AfrCloud.runOnClient(() -> {
					if (profile == null || profile.meta == null) {
						NotificationHud.notify(
							NotificationHud.Type.ERROR,
							Lang.t("lbl.library"),
							Lang.t("msg.invalid_config")
						);
						return;
					}
					applyInstalled(profile, id, fallbackName, remoteCode);
				})
			)
			.exceptionally(error -> {
				AFR.LOGGER.warn(
					"AFR: library install failed: {}",
					error.getMessage()
				);
				AfrCloud.runOnClient(() ->
					NotificationHud.notify(
						NotificationHud.Type.ERROR,
						Lang.t("lbl.library"),
						Lang.t("msg.install_error")
					)
				);
				return null;
			});
	}

	private static String sanitizeId(String name) {
		if (name == null) {
			return "";
		}
		return name
			.replaceAll("[^A-Za-z0-9_-]", "-")
			.replaceAll("(^-+|-+$)", "");
	}

	private void applyInstalled(
		RpProfile profile,
		String id,
		String fallbackName,
		int remoteCode
	) {
		if (profile.meta.revName == null || profile.meta.revName.isBlank()) {
			profile.meta.revName = fallbackName;
		}
		profile.meta.revName = sanitizeId(profile.meta.revName);
		if (profile.meta.revName.isBlank()) {
			profile.meta.revName = "config";
		}
		profile.meta.source = "library";
		profile.meta.remoteId = id == null ? "" : id;
		profile.meta.remoteVersionCode = remoteCode;
		profile.meta.modified = false;
		AfrRuntime runtime = AfrRuntime.get();
		RpProfile existing = findInstalled(id);
		if (existing != null) {
			profile.meta.revName = existing.meta.revName;
			VersionStore.archive(existing);
		} else {
			String base = profile.meta.revName;
			String candidate = base;
			int index = 2;
			while (runtime.config().profiles().containsKey(candidate)) {
				candidate = base + "-" + index++;
			}
			profile.meta.revName = candidate;
		}
		runtime.config().putProfile(profile, false);
		AFR.LOGGER.info(
			"AFR: installed config '{}' (v{}) from the library",
			profile.meta.revName,
			profile.meta.version
		);
		NotificationHud.notify(
			NotificationHud.Type.SUCCESS,
			Lang.t("lbl.library"),
			Lang.t("msg.installed") + ": " + profile.meta.label
		);
		LibraryScreen library = new LibraryScreen(parent, entries);
		if (profile.parameters != null && !profile.parameters.isEmpty()) {
			minecraft.setScreen(
				new ParameterSetupScreen(
					library,
					AfrRuntime.get().config(),
					profile
				)
			);
		} else {
			minecraft.setScreen(library);
		}
	}

	private int updateCount() {
		if (entries == null) {
			return 0;
		}
		int count = 0;
		for (JsonObject entry : entries) {
			RpProfile local = findInstalled(getString(entry, "id", ""));
			if (
				local != null &&
				getInt(entry, "version_code", 0) > local.meta.versionCode
			) {
				count++;
			}
		}
		return count;
	}

	private static RpProfile findInstalled(String id) {
		if (id == null || id.isBlank()) {
			return null;
		}
		var profiles = AfrRuntime.get().config().profiles();
		for (RpProfile profile : profiles.values()) {
			if (
				profile.meta != null &&
				id.equalsIgnoreCase(profile.meta.remoteId)
			) {
				return profile;
			}
		}
		RpProfile direct = profiles.get(id);
		if (direct != null && direct.isLibrary()) {
			return direct;
		}
		return null;
	}

	private static io.wispforest.owo.ui.core.Component entryIcon(String icon) {
		if (icon == null || icon.isBlank()) {
			return null;
		}
		if (icon.contains(":")) {
			return new AfrTile(Items.fromId(icon), "").size(18);
		}
		return new AfrTile(ItemStack.EMPTY, icon).size(18);
	}

	private static String entryCredits(JsonObject entry) {
		if (entry.has("authors") && entry.get("authors").isJsonArray()) {
			StringBuilder sb = new StringBuilder();
			entry.getAsJsonArray("authors").forEach(a -> {
				if (a != null && !a.isJsonNull()) {
					if (sb.length() > 0) {
						sb.append(", ");
					}
					sb.append(a.getAsString());
				}
			});
			if (sb.length() > 0) {
				return sb.toString();
			}
		}
		return getString(entry, "author", "");
	}

	private static String tags(JsonObject entry) {
		if (!entry.has("tags") || !entry.get("tags").isJsonArray()) {
			return "";
		}
		StringBuilder sb = new StringBuilder();
		entry.getAsJsonArray("tags").forEach(tag -> {
			if (sb.length() > 0) {
				sb.append(", ");
			}
			sb.append(tag.getAsString());
		});
		return sb.toString();
	}

	private static String getString(
		JsonObject object,
		String key,
		String fallback
	) {
		return object.has(key) && !object.get(key).isJsonNull()
			? object.get(key).getAsString()
			: fallback;
	}

	private static int getInt(JsonObject object, String key, int fallback) {
		return object.has(key) && object.get(key).isJsonPrimitive()
			? object.get(key).getAsInt()
			: fallback;
	}

	@Override
	public void onClose() {
		if (parent instanceof ConfigScreen editor) {
			editor.refresh();
		}
		minecraft.setScreen(parent);
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}
}

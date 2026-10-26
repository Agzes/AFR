// SPDX-License-Identifier: GPL-3.0-only
package io.github.agzes.afr.client.ui;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import io.github.agzes.afr.AFR;
import io.github.agzes.afr.client.AfrRuntime;
import io.github.agzes.afr.client.hud.NotificationHud;
import io.github.agzes.afr.client.i18n.Lang;
import io.github.agzes.afr.client.model.RpProfile;
import io.github.agzes.afr.client.net.AfrCloud;
import io.github.agzes.afr.client.util.VersionStore;
import io.wispforest.owo.ui.base.BaseOwoScreen;
import io.wispforest.owo.ui.component.Components;
import io.wispforest.owo.ui.container.Containers;
import io.wispforest.owo.ui.container.FlowLayout;
import io.wispforest.owo.ui.core.Color;
import io.wispforest.owo.ui.core.HorizontalAlignment;
import io.wispforest.owo.ui.core.Insets;
import io.wispforest.owo.ui.core.OwoUIAdapter;
import io.wispforest.owo.ui.core.Sizing;
import io.wispforest.owo.ui.core.Surface;
import io.wispforest.owo.ui.core.VerticalAlignment;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;

public class VersionHistoryScreen extends BaseOwoScreen<FlowLayout> {

	private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern(
		"yyyy-MM-dd HH:mm"
	).withZone(ZoneId.systemDefault());

	private final Screen parent;
	private final RpProfile profile;
	private final List<JsonObject> remote = new ArrayList<>();
	private FlowLayout listHost;

	public VersionHistoryScreen(Screen parent, RpProfile profile) {
		super(Component.literal("AFR"));
		this.parent = parent;
		this.profile = profile;
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

		int width = Math.max(360, Math.min(this.width - 16, 500));
		int height = Math.max(220, Math.min(this.height - 16, 320));
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
				Component.literal(
					Icons.LIBRARY +
						"  " +
						Lang.t("sec.versions") +
						" • " +
						(profile.meta != null ? profile.meta.label : "")
				)
			).color(Color.ofArgb(Ui.SECTION))
		);
		window.child(header);

		listHost = Containers.verticalFlow(Sizing.fill(100), Sizing.content());
		listHost.gap(Ui.GAP);
		listHost.padding(Insets.of(Ui.PAD));
		window.child(
			new AfrScroll(Sizing.fill(100), Sizing.expand(100), listHost)
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
			new AfrButton(
				Component.literal(
					Icons.CHECK + " " + Lang.t("btn.save_version")
				),
				AfrButton.Variant.SECONDARY,
				this::archiveCurrent
			)
		);
		footer.child(Ui.grow());
		footer.child(
			new AfrButton(
				Component.literal(Lang.t("btn.close")),
				AfrButton.Variant.SECONDARY,
				this::onClose
			)
		);
		window.child(footer);

		root.child(window);
		populate();
		loadRemote();
	}

	private void populate() {
		if (listHost == null) {
			return;
		}
		listHost.clearChildren();

		listHost.child(Ui.section(Icons.CHECK, Lang.t("lbl.current_version")));
		listHost.child(currentRow());

		listHost.child(Ui.section(Icons.LIBRARY, Lang.t("lbl.local_versions")));
		List<VersionStore.Entry> entries = VersionStore.list(profile);
		if (entries.isEmpty()) {
			listHost.child(Ui.dim(Lang.t("msg.no_versions")));
		} else {
			for (VersionStore.Entry entry : entries) {
				listHost.child(versionRow(entry));
			}
		}

		if (!remote.isEmpty()) {
			listHost.child(
				Ui.section(Icons.CLOUD, Lang.t("lbl.remote_versions"))
			);
			for (JsonObject version : remote) {
				listHost.child(remoteRow(version));
			}
		}
		listHost.child(
			Containers.horizontalFlow(Sizing.fill(100), Sizing.fixed(8))
		);
	}

	private FlowLayout currentRow() {
		FlowLayout card = Ui.card(
			profile.meta != null ? profile.meta.version : "-"
		);
		String code =
			profile.meta == null
				? ""
				: " (" +
					Lang.t("lbl.code") +
					" " +
					profile.meta.versionCode +
					")";
		card.child(Ui.dim(Lang.t("lbl.current_version") + code));
		return card;
	}

	private FlowLayout versionRow(VersionStore.Entry entry) {
		FlowLayout card = Ui.card(entry.version());
		FlowLayout row = Ui.row();
		row.child(
			Ui.dim(
				"#" +
					entry.versionCode() +
					" • " +
					DATE.format(Instant.ofEpochMilli(entry.modified()))
			)
		);
		row.child(Ui.grow());
		row.child(
			new AfrButton(
				Component.literal(Icons.RELOAD + " " + Lang.t("btn.restore")),
				AfrButton.Variant.PRIMARY,
				() -> restore(entry)
			)
		);
		row.child(
			new AfrButton(
				Component.literal(Icons.DELETE),
				AfrButton.Variant.DANGER,
				() -> confirmDelete(entry)
			).tip(Lang.t("btn.delete"))
		);
		card.child(row);
		return card;
	}

	private FlowLayout remoteRow(JsonObject version) {
		String v = getString(version, "version", "?");
		int code = getInt(version, "version_code", 0);
		FlowLayout card = Ui.card(v);
		FlowLayout row = Ui.row();
		String date = getString(
			version,
			"date",
			getString(version, "updated_at", "")
		);
		row.child(Ui.dim("#" + code + (date.isBlank() ? "" : " • " + date)));
		row.child(Ui.grow());
		boolean installed =
			profile.meta != null && code == profile.meta.versionCode;
		if (installed) {
			row.child(Ui.dim(Lang.t("lbl.current_version")));
		} else {
			row.child(
				new AfrButton(
					Component.literal(
						Icons.DOWN + " " + Lang.t("btn.download")
					),
					AfrButton.Variant.SECONDARY,
					() -> downloadRemote(version)
				)
			);
		}
		card.child(row);
		for (String line : changelogOf(version)) {
			card.child(Ui.dim("• " + line));
		}
		return card;
	}

	private static List<String> changelogOf(JsonObject version) {
		List<String> list = new ArrayList<>();
		if (
			version.has("changelog") && version.get("changelog").isJsonArray()
		) {
			version.getAsJsonArray("changelog").forEach(line -> {
				if (line != null && !line.isJsonNull()) {
					list.add(line.getAsString());
				}
			});
		}
		return list;
	}

	private void archiveCurrent() {
		VersionStore.archive(profile);
		NotificationHud.notify(
			NotificationHud.Type.SUCCESS,
			"AFR",
			Lang.t("msg.version_saved")
		);
		populate();
	}

	private void restore(VersionStore.Entry entry) {
		try {
			RpProfile restored = VersionStore.load(entry);
			VersionStore.archive(profile);
			restored.meta.revName = profile.meta.revName;
			restored.meta.source = profile.meta.source;
			restored.meta.remoteId = profile.meta.remoteId;
			restored.meta.modified = profile.meta.modified;
			AfrRuntime.get().config().putProfile(restored, true);
			AfrRuntime.get().rebuildBinds();
			AFR.LOGGER.info(
				"AFR: restored '{}' to version {}",
				restored.meta.revName,
				entry.tag()
			);
			NotificationHud.notify(
				NotificationHud.Type.SUCCESS,
				"AFR",
				Lang.t("msg.restored") + ": v" + entry.version()
			);
			minecraft.setScreen(
				new VersionHistoryScreen(
					parent,
					AfrRuntime.get().config().active()
				)
			);
		} catch (Exception e) {
			AFR.LOGGER.warn("AFR: failed to restore a version", e);
			NotificationHud.notify(
				NotificationHud.Type.ERROR,
				"AFR",
				Lang.t("msg.import_error")
			);
		}
	}

	private void confirmDelete(VersionStore.Entry entry) {
		minecraft.setScreen(
			new ConfirmScreen(
				Lang.t("confirm.delete"),
				Lang.t("msg.confirm_delete_version") +
					" v" +
					entry.version() +
					"?",
				Lang.t("btn.delete"),
				() -> {
					VersionStore.delete(entry);
					minecraft.setScreen(
						new VersionHistoryScreen(parent, profile)
					);
				},
				() -> minecraft.setScreen(this)
			)
		);
	}

	private void loadRemote() {
		if (
			profile.meta == null ||
			profile.meta.remoteId == null ||
			profile.meta.remoteId.isBlank()
		) {
			return;
		}
		AfrCloud.library()
			.thenAccept(lib -> {
				JsonObject json = lib.json();
				if (!json.has("configs")) {
					return;
				}
				for (var element : json.getAsJsonArray("configs")) {
					if (!element.isJsonObject()) {
						continue;
					}
					JsonObject entry = element.getAsJsonObject();
					if (
						profile.meta.remoteId.equalsIgnoreCase(
							getString(entry, "id", "")
						)
					) {
						JsonArray versions =
							entry.has("versions") &&
							entry.get("versions").isJsonArray()
								? entry.getAsJsonArray("versions")
								: null;
						AfrCloud.runOnClient(() -> {
							remote.clear();
							if (versions != null) {
								versions.forEach(v -> {
									if (v.isJsonObject()) {
										remote.add(v.getAsJsonObject());
									}
								});
							}
							populate();
						});
						return;
					}
				}
			})
			.exceptionally(error -> null);
	}

	private void downloadRemote(JsonObject version) {
		String url = getString(version, "config_url", "");
		if (url.isBlank()) {
			NotificationHud.notify(
				NotificationHud.Type.WARNING,
				"AFR",
				Lang.t("msg.no_link")
			);
			return;
		}
		AfrCloud.downloadConfig(url)
			.thenAccept(profile ->
				AfrCloud.runOnClient(() -> {
					if (profile == null || profile.meta == null) {
						NotificationHud.notify(
							NotificationHud.Type.ERROR,
							"AFR",
							Lang.t("msg.invalid_config")
						);
						return;
					}
					VersionStore.archive(this.profile);
					profile.meta.revName = this.profile.meta.revName;
					profile.meta.source = "library";
					profile.meta.remoteId = this.profile.meta.remoteId;
					profile.meta.remoteVersionCode = getInt(
						version,
						"version_code",
						profile.meta.versionCode
					);
					profile.meta.modified = false;
					AfrRuntime.get().config().putProfile(profile, true);
					AfrRuntime.get().rebuildBinds();
					NotificationHud.notify(
						NotificationHud.Type.SUCCESS,
						"AFR",
						Lang.t("msg.installed") + ": v" + profile.meta.version
					);
					minecraft.setScreen(
						new VersionHistoryScreen(
							parent,
							AfrRuntime.get().config().active()
						)
					);
				})
			)
			.exceptionally(error -> {
				AfrCloud.runOnClient(() ->
					NotificationHud.notify(
						NotificationHud.Type.ERROR,
						"AFR",
						Lang.t("msg.install_error")
					)
				);
				return null;
			});
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

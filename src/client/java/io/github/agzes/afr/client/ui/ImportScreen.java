// SPDX-License-Identifier: GPL-3.0-only
package io.github.agzes.afr.client.ui;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.github.agzes.afr.AFR;
import io.github.agzes.afr.client.AfrRuntime;
import io.github.agzes.afr.client.config.ConfigCodec;
import io.github.agzes.afr.client.config.ConfigManager;
import io.github.agzes.afr.client.config.ConfigPaths;
import io.github.agzes.afr.client.hud.NotificationHud;
import io.github.agzes.afr.client.i18n.Lang;
import io.github.agzes.afr.client.model.RpProfile;
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
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.stream.Stream;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;

public class ImportScreen extends BaseOwoScreen<FlowLayout> {

	public enum Kind {
		PROFILE,
		THEME,
	}

	private final Kind kind;
	private final ConfigManager config;
	private final Theme theme;
	private final Runnable onBack;
	private String text = "";
	private AfrTextArea area;

	public ImportScreen(
		Kind kind,
		ConfigManager config,
		Theme theme,
		Runnable onBack
	) {
		super(Component.literal("AFR"));
		this.kind = kind;
		this.config = config;
		this.theme = theme;
		this.onBack = onBack;
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

		int width = Math.max(360, Math.min(this.width - 16, 560));
		int height = Math.max(240, Math.min(this.height - 16, 340));
		FlowLayout window = Containers.verticalFlow(
			Sizing.fixed(width),
			Sizing.fixed(height)
		);
		window.surface(Ui.window());
		window.gap(0);

		String title =
			kind == Kind.PROFILE
				? Lang.t("import.title_profile")
				: Lang.t("import.title_theme");
		FlowLayout header = Containers.horizontalFlow(
			Sizing.fill(100),
			Sizing.content()
		);
		header.surface(Ui.header());
		header.padding(Insets.of(5));
		header.horizontalAlignment(HorizontalAlignment.CENTER);
		header.child(
			Components.label(
				Component.literal(Icons.DOWN + "  " + title)
			).color(Color.ofArgb(Ui.SECTION))
		);
		window.child(header);

		FlowLayout body = Containers.verticalFlow(
			Sizing.fill(100),
			Sizing.expand(100)
		);
		body.gap(Ui.GAP);
		body.padding(Insets.of(Ui.PAD));
		body.child(
			Ui.wrap(
				kind == Kind.PROFILE
					? Lang.t("import.paste_hint")
					: Lang.t("import.theme_hint"),
				width - 24
			)
		);
		area = new AfrTextArea()
			.onChanged(value -> text = value == null ? "" : value)
			.hint(Lang.t("import.json_hint"));
		area.text(text);
		area.horizontalSizing(Sizing.fill(100));
		area.verticalSizing(Sizing.expand(100));
		body.child(area);
		window.child(body);

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
					Icons.LIBRARY + " " + Lang.t("btn.from_file")
				),
				AfrButton.Variant.SECONDARY,
				this::loadFromFile
			)
		);
		footer.child(
			new AfrButton(
				Component.literal(Lang.t("btn.from_clipboard")),
				AfrButton.Variant.SECONDARY,
				this::loadFromClipboard
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
		footer.child(
			new AfrButton(
				Component.literal(Icons.CHECK + " " + Lang.t("btn.import")),
				AfrButton.Variant.PRIMARY,
				this::doImport
			)
		);
		window.child(footer);

		root.child(window);
	}

	private void loadFromClipboard() {
		String clip = minecraft.keyboardHandler.getClipboard();
		if (clip == null || clip.isBlank()) {
			notify(NotificationHud.Type.WARNING, Lang.t("msg.clipboard_empty"));
			return;
		}
		setText(clip);
	}

	private void loadFromFile() {
		try {
			Path dir = ConfigPaths.exportsDir();
			Files.createDirectories(dir);
			Path newest = null;
			long newestTime = -1;
			try (Stream<Path> stream = Files.list(dir)) {
				for (Path path : stream
					.filter(p -> p.getFileName().toString().endsWith(".json"))
					.toList()) {
					if (!matchesKind(path)) {
						continue;
					}
					long modified = Files.getLastModifiedTime(path).toMillis();
					if (modified > newestTime) {
						newestTime = modified;
						newest = path;
					}
				}
			}
			if (newest == null) {
				notify(
					NotificationHud.Type.WARNING,
					Lang.t("msg.no_import_file")
				);
				return;
			}
			setText(Files.readString(newest, StandardCharsets.UTF_8));
			AFR.LOGGER.info(
				"AFR: loaded {} for import from {}",
				kind,
				newest.getFileName()
			);
		} catch (Exception e) {
			AFR.LOGGER.warn("AFR: failed to read an import file", e);
			notify(NotificationHud.Type.ERROR, Lang.t("msg.import_error"));
		}
	}

	private boolean matchesKind(Path path) {
		try {
			JsonObject object = JsonParser.parseString(
				Files.readString(path, StandardCharsets.UTF_8)
			).getAsJsonObject();
			return kind == Kind.PROFILE
				? object.has("rp_actions")
				: object.has("colors") || object.has("theme");
		} catch (Exception e) {
			return false;
		}
	}

	private void setText(String value) {
		text = value == null ? "" : value;
		if (area != null) {
			area.text(text);
		}
	}

	private void doImport() {
		String raw = text == null ? "" : text.strip();
		if (raw.isEmpty()) {
			notify(NotificationHud.Type.WARNING, Lang.t("msg.import_empty"));
			return;
		}
		if (kind == Kind.PROFILE) {
			importProfile(raw);
		} else {
			importTheme(raw);
		}
	}

	private void importProfile(String raw) {
		RpProfile imported;
		try {
			imported = ConfigCodec.readString(raw, RpProfile.class);
			if (imported == null || imported.rp_actions == null) {
				throw new IllegalArgumentException("not a profile");
			}
		} catch (Exception e) {
			AFR.LOGGER.warn("AFR: profile import failed", e);
			notify(NotificationHud.Type.ERROR, Lang.t("msg.import_error"));
			return;
		}
		imported.normalize();
		imported.meta.source = "local";
		imported.meta.remoteId = "";
		imported.meta.remoteVersionCode = 0;
		imported.meta.modified = false;
		String base =
			imported.meta.revName == null ? "" : imported.meta.revName.strip();
		base = base
			.toLowerCase(Locale.ROOT)
			.replaceAll("[^a-z0-9_-]+", "-")
			.replaceAll("(^-+|-+$)", "");
		if (base.isBlank()) {
			base = "imported";
		}
		String revName = base;
		int index = 2;
		while (config.profiles().containsKey(revName)) {
			revName = base + "-" + index++;
		}
		imported.meta.revName = revName;
		if (imported.meta.label == null || imported.meta.label.isBlank()) {
			imported.meta.label = revName;
		}
		String name = profileName(imported);
		minecraft.setScreen(
			new ConfirmScreen(
				Lang.t("confirm.import"),
				Lang.t("msg.import_warning") + "\n\n" + name,
				Lang.t("btn.import"),
				() -> {
					config.putProfile(imported, true);
					AfrRuntime.get().rebuildBinds();
					notify(
						NotificationHud.Type.SUCCESS,
						Lang.t("msg.imported") + ": " + name
					);
					onBack.run();
				},
				() -> minecraft.setScreen(this)
			)
		);
	}

	private void importTheme(String raw) {
		try {
			int applied = ThemeEditorScreen.applyThemeJson(config, theme, raw);
			AFR.LOGGER.info(
				"AFR: theme '{}' imported ({} colors)",
				theme == null ? "-" : theme.id,
				applied
			);
			notify(
				NotificationHud.Type.SUCCESS,
				Lang.t("theme.imported") + ": " + applied
			);
			onBack.run();
		} catch (Exception e) {
			notify(NotificationHud.Type.ERROR, Lang.t("theme.bad_clipboard"));
		}
	}

	private static String profileName(RpProfile profile) {
		if (profile == null || profile.meta == null) {
			return "-";
		}
		if (profile.meta.label != null && !profile.meta.label.isBlank()) {
			return profile.meta.label;
		}
		if (profile.meta.revName != null && !profile.meta.revName.isBlank()) {
			return profile.meta.revName;
		}
		return "-";
	}

	private void notify(NotificationHud.Type type, String message) {
		NotificationHud.notify(type, "AFR", message);
	}

	@Override
	public void onClose() {
		onBack.run();
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}
}

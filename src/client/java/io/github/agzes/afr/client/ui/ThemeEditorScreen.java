// SPDX-License-Identifier: GPL-3.0-only
package io.github.agzes.afr.client.ui;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.github.agzes.afr.AFR;
import io.github.agzes.afr.client.config.ConfigCodec;
import io.github.agzes.afr.client.config.ConfigManager;
import io.github.agzes.afr.client.config.ConfigPaths;
import io.github.agzes.afr.client.hud.NotificationHud;
import io.github.agzes.afr.client.i18n.Lang;
import io.github.agzes.afr.client.model.UserSettings;
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
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;

public class ThemeEditorScreen extends BaseOwoScreen<FlowLayout> {

	private static final String[][] TOKENS = {
		{ "window", "tok.window" },
		{ "header", "tok.header" },
		{ "footer", "tok.footer" },
		{ "card", "tok.card" },
		{ "cardHover", "tok.cardHover" },
		{ "fieldBg", "tok.fieldBg" },
		{ "fieldBorder", "tok.fieldBorder" },
		{ "accent", "tok.accent" },
		{ "accentHover", "tok.accentHover" },
		{ "accentDim", "tok.accentDim" },
		{ "accentText", "tok.accentText" },
		{ "text", "tok.text" },
		{ "dim", "tok.dim" },
		{ "muted", "tok.muted" },
		{ "section", "tok.section" },
		{ "good", "tok.good" },
		{ "warn", "tok.warn" },
		{ "bad", "tok.bad" },
		{ "line", "tok.line" },
	};

	private final Screen parent;
	private final ConfigManager config;
	private final UserSettings settings;
	private final Theme theme;

	public ThemeEditorScreen(
		Screen parent,
		ConfigManager config,
		UserSettings settings
	) {
		super(Component.literal("AFR"));
		this.parent = parent;
		this.config = config;
		this.settings = settings;
		if (settings.themeColors == null) {
			settings.themeColors = new LinkedHashMap<>();
		}
		this.theme = Theme.byId(settings.theme);
	}

	@Override
	protected @NotNull OwoUIAdapter<FlowLayout> createAdapter() {
		return OwoUIAdapter.create(this, Containers::verticalFlow);
	}

	private int alphaOf(String key) {
		if ("window".equals(key)) {
			return 0xE6;
		}
		if ("line".equals(key)) {
			return 0x24;
		}
		return 0xFF;
	}

	private int current(String key) {
		return switch (key) {
			case "window" -> Ui.WINDOW;
			case "header" -> Ui.HEADER;
			case "footer" -> Ui.FOOTER;
			case "card" -> Ui.CARD;
			case "cardHover" -> Ui.CARD_HOVER;
			case "fieldBg" -> Ui.FIELD_BG;
			case "fieldBorder" -> Ui.FIELD_BORDER;
			case "accent" -> Ui.ACCENT;
			case "accentHover" -> Ui.ACCENT_HOVER;
			case "accentDim" -> Ui.ACCENT_DIM;
			case "accentText" -> Ui.ACCENT_TEXT;
			case "text" -> Ui.TEXT;
			case "dim" -> Ui.DIM;
			case "muted" -> Ui.MUTED;
			case "section" -> Ui.SECTION;
			case "good" -> Ui.GOOD;
			case "warn" -> Ui.WARN;
			case "bad" -> Ui.BAD;
			case "line" -> Ui.LINE;
			default -> 0xFFFFFFFF;
		};
	}

	@Override
	protected void build(FlowLayout root) {
		root.surface(Surface.flat(Ui.BACKDROP));
		root.horizontalAlignment(HorizontalAlignment.CENTER);
		root.verticalAlignment(VerticalAlignment.CENTER);

		int width = Math.max(340, Math.min(this.width - 16, 460));
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
					Icons.EDIT +
						"  " +
						Lang.t("sec.theme_colors") +
						" • " +
						theme.label(settings.language)
				)
			).color(Color.ofArgb(Ui.SECTION))
		);
		window.child(header);

		FlowLayout body = Containers.verticalFlow(
			Sizing.fill(100),
			Sizing.expand(100)
		);
		body.gap(Ui.GAP);
		body.padding(Insets.of(Ui.PAD));
		body.child(Ui.dim(Lang.t("theme.format_hint")));

		FlowLayout list = Containers.verticalFlow(
			Sizing.fill(100),
			Sizing.content()
		);
		list.gap(3);
		for (String[] token : TOKENS) {
			list.child(tokenRow(token[0], Lang.t(token[1])));
		}
		body.child(
			Containers.verticalScroll(
				Sizing.fill(100),
				Sizing.expand(100),
				list
			)
		);
		window.child(body);

		FlowLayout footer = Containers.horizontalFlow(
			Sizing.fill(100),
			Sizing.content()
		);
		footer.surface(Ui.footer());
		footer.padding(Insets.of(5));
		footer.gap(4);
		footer.verticalAlignment(VerticalAlignment.CENTER);
		footer.child(
			new AfrButton(
				Component.literal(Lang.t("btn.export")),
				AfrButton.Variant.SECONDARY,
				this::exportTheme
			).tip(Lang.t("tip.theme_export"))
		);
		footer.child(
			new AfrButton(
				Component.literal(Lang.t("btn.import")),
				AfrButton.Variant.SECONDARY,
				() ->
					minecraft.setScreen(
						new ImportScreen(
							ImportScreen.Kind.THEME,
							config,
							theme,
							() ->
								minecraft.setScreen(
									new ThemeEditorScreen(
										parent,
										config,
										settings
									)
								)
						)
					)
			).tip(Lang.t("tip.theme_import"))
		);
		footer.child(
			new AfrButton(
				Component.literal(Icons.RELOAD + " " + Lang.t("btn.reset")),
				AfrButton.Variant.SECONDARY,
				() -> {
					settings.themeColors.remove(theme.id);
					Ui.applyTheme(theme);
					config.saveSettings();
					minecraft.setScreen(
						new ThemeEditorScreen(parent, config, settings)
					);
				}
			).tip(Lang.t("tip.theme_reset"))
		);
		footer.child(Ui.grow());
		footer.child(
			new AfrButton(
				Component.literal(Icons.CHECK + " " + Lang.t("btn.done")),
				AfrButton.Variant.PRIMARY,
				this::onClose
			)
		);
		window.child(footer);

		root.child(window);
	}

	private void exportTheme() {
		JsonObject root = new JsonObject();
		root.addProperty("theme", theme.id);
		JsonObject colors = new JsonObject();
		for (String[] token : TOKENS) {
			colors.addProperty(token[0], "#" + Ui.hex(current(token[0])));
		}
		root.add("colors", colors);
		String json = ConfigCodec.GSON.toJson(root);
		Minecraft.getInstance().keyboardHandler.setClipboard(json);
		String saved = "";
		try {
			Files.createDirectories(ConfigPaths.exportsDir());
			Path target = ConfigPaths.exportsDir().resolve(
				"theme-" + theme.id + ".json"
			);
			Files.writeString(target, json, StandardCharsets.UTF_8);
			saved = target.getFileName().toString();
		} catch (IOException e) {
			AFR.LOGGER.warn("Failed to save the theme file", e);
		}
		AFR.LOGGER.info(
			"AFR: theme '{}' exported{}",
			theme.id,
			saved.isBlank() ? "" : " to " + saved
		);
		NotificationHud.notify(
			NotificationHud.Type.SUCCESS,
			Lang.t("lbl.theme"),
			saved.isBlank()
				? Lang.t("theme.copied")
				: Lang.t("theme.copied_saved") + ": " + saved
		);
	}

	static int applyThemeJson(ConfigManager config, Theme theme, String json) {
		JsonObject root = JsonParser.parseString(json).getAsJsonObject();
		JsonObject colors =
			root.has("colors") && root.get("colors").isJsonObject()
				? root.getAsJsonObject("colors")
				: root;
		UserSettings settings = config.settings();
		Map<String, String> overrides = settings.themeColors.computeIfAbsent(
			theme.id,
			k -> new LinkedHashMap<>()
		);
		int applied = 0;
		for (String[] token : TOKENS) {
			if (colors.has(token[0])) {
				String hex = colors
					.get(token[0])
					.getAsString()
					.trim()
					.replace("#", "")
					.toUpperCase();
				if (hex.matches("[0-9A-F]{6}")) {
					overrides.put(token[0], hex);
					applied++;
				}
			}
		}
		Ui.applyTheme(theme, overrides);
		config.saveSettings();
		return applied;
	}

	private FlowLayout tokenRow(String key, String label) {
		FlowLayout row = Ui.row();
		row.gap(6);
		row.child(
			Components.label(
				Component.literal(
					TextFit.fit(Minecraft.getInstance().font, label, 150)
				)
			).color(Color.ofArgb(Ui.DIM))
		);
		row.child(Ui.grow());

		AfrSwatch swatch = new AfrSwatch(current(key));
		AfrInput input = AfrInput.fixed("#" + Ui.hex(current(key)), 96);
		input.onChanged(value -> {
			String hex =
				value == null
					? ""
					: value.trim().replace("#", "").toUpperCase();
			if (hex.length() == 6 && hex.matches("[0-9A-F]{6}")) {
				Map<String, String> overrides =
					settings.themeColors.computeIfAbsent(theme.id, k ->
						new LinkedHashMap<>()
					);
				overrides.put(key, hex);
				Ui.applyTheme(theme, overrides);
				swatch.color(Ui.color(hex, current(key), alphaOf(key)));
				config.saveSettings();
			}
		});
		row.child(input.component());
		row.child(swatch);
		return row;
	}

	@Override
	public void onClose() {
		ConfigScreen.returnFrom(parent, 1, null, null);
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}
}

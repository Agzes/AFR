// SPDX-License-Identifier: GPL-3.0-only
package io.github.agzes.afr.client.ui;

import io.github.agzes.afr.AFR;
import io.github.agzes.afr.client.AfrRuntime;
import io.wispforest.owo.ui.component.Components;
import io.wispforest.owo.ui.component.LabelComponent;
import io.wispforest.owo.ui.container.Containers;
import io.wispforest.owo.ui.container.FlowLayout;
import io.wispforest.owo.ui.core.Color;
import io.wispforest.owo.ui.core.Insets;
import io.wispforest.owo.ui.core.Sizing;
import io.wispforest.owo.ui.core.Surface;
import io.wispforest.owo.ui.core.VerticalAlignment;
import java.net.URI;
import java.util.Locale;
import java.util.Map;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;

public final class Ui {

	public static int BACKDROP = 0x99000000;
	public static int WINDOW = 0xE6161616;
	public static int HEADER = 0xFF1E1E1E;
	public static int FOOTER = 0xFF101010;
	public static int CARD = 0xFF1E1E1E;
	public static int CARD_HOVER = 0xFF282828;
	public static int FIELD_BG = 0xFF111111;
	public static int FIELD_BORDER = 0xFF303030;
	public static int LINE = 0x24FFFFFF;
	public static int ACCENT = 0xFF337FC5;
	public static int ACCENT_HOVER = 0xFF4C93D6;
	public static int ACCENT_DIM = 0xFF17324C;
	public static int ACCENT_TEXT = 0xFFFFFFFF;
	public static int TEXT = 0xFFE6E6E6;
	public static int DIM = 0xFFA0A0A0;
	public static int MUTED = 0xFF6E6E6E;
	public static int SECTION = 0xFF9CB8D6;
	public static int GOOD = 0xFF3FB950;
	public static int WARN = 0xFFD29922;
	public static int BAD = 0xFFF85149;

	public static final int WIN_W = 540;
	public static final int WIN_H = 292;
	public static final int PAD = 8;
	public static final int GAP = 6;
	public static final int CARD_PAD = 8;
	public static final int CARD_GAP = 5;
	public static final int CONTROL_WIDTH = 170;

	private Ui() {}

	public static void applyTheme(Theme theme) {
		Theme t = theme == null ? Theme.CURRENT : theme;
		WINDOW = t.window;
		HEADER = t.header;
		FOOTER = t.footer;
		CARD = t.card;
		CARD_HOVER = t.cardHover;
		FIELD_BG = t.fieldBg;
		FIELD_BORDER = t.fieldBorder;
		ACCENT = t.accent;
		ACCENT_HOVER = t.accentHover;
		ACCENT_DIM = t.accentDim;
		ACCENT_TEXT = t.accentText;
		TEXT = t.text;
		DIM = t.dim;
		MUTED = t.muted;
		SECTION = t.section;
		GOOD = t.good;
		WARN = t.warn;
		BAD = t.bad;
	}

	public static void applyTheme(Theme theme, Map<String, String> overrides) {
		applyTheme(theme);
		if (overrides == null || overrides.isEmpty()) {
			return;
		}
		WINDOW = color(overrides.get("window"), WINDOW, 0xE6);
		HEADER = color(overrides.get("header"), HEADER, 0xFF);
		FOOTER = color(overrides.get("footer"), FOOTER, 0xFF);
		CARD = color(overrides.get("card"), CARD, 0xFF);
		CARD_HOVER = color(overrides.get("cardHover"), CARD_HOVER, 0xFF);
		FIELD_BG = color(overrides.get("fieldBg"), FIELD_BG, 0xFF);
		FIELD_BORDER = color(overrides.get("fieldBorder"), FIELD_BORDER, 0xFF);
		ACCENT = color(overrides.get("accent"), ACCENT, 0xFF);
		ACCENT_HOVER = color(overrides.get("accentHover"), ACCENT_HOVER, 0xFF);
		ACCENT_DIM = color(overrides.get("accentDim"), ACCENT_DIM, 0xFF);
		ACCENT_TEXT = color(overrides.get("accentText"), ACCENT_TEXT, 0xFF);
		TEXT = color(overrides.get("text"), TEXT, 0xFF);
		DIM = color(overrides.get("dim"), DIM, 0xFF);
		MUTED = color(overrides.get("muted"), MUTED, 0xFF);
		SECTION = color(overrides.get("section"), SECTION, 0xFF);
		GOOD = color(overrides.get("good"), GOOD, 0xFF);
		WARN = color(overrides.get("warn"), WARN, 0xFF);
		BAD = color(overrides.get("bad"), BAD, 0xFF);
		LINE = color(overrides.get("line"), LINE, 0x24);
	}

	public static int color(String hex, int fallback, int alpha) {
		if (hex == null) {
			return fallback;
		}
		String s = hex.trim();
		if (s.startsWith("#")) {
			s = s.substring(1);
		}
		try {
			if (s.length() == 8) {
				return (int) Long.parseLong(s, 16);
			}
			if (s.length() == 6) {
				return (alpha << 24) | ((int) Long.parseLong(s, 16) & 0xFFFFFF);
			}
		} catch (NumberFormatException ignored) {}
		return fallback;
	}

	public static String hex(int argb) {
		return String.format(
			"%02X%02X%02X",
			(argb >> 16) & 0xFF,
			(argb >> 8) & 0xFF,
			argb & 0xFF
		);
	}

	public static void roundRect(
		GuiGraphics context,
		int x,
		int y,
		int w,
		int h,
		int color,
		int radius
	) {
		if (color >>> 24 == 0 || w <= 0 || h <= 0) {
			return;
		}
		int r = Math.max(0, Math.min(radius, Math.min(w / 2, h / 2)));
		if (r == 0) {
			context.fill(x, y, x + w, y + h, color);
			return;
		}
		context.fill(x + r, y, x + w - r, y + r, color);
		context.fill(x, y + r, x + w, y + h - r, color);
		context.fill(x + r, y + h - r, x + w - r, y + h, color);
	}

	public static Surface rounded(int color, int radius) {
		return (context, component) ->
			roundRect(
				context,
				component.x(),
				component.y(),
				component.width(),
				component.height(),
				color,
				radius
			);
	}

	public static Surface window() {
		return rounded(WINDOW, 4);
	}

	public static Surface menuWindow() {
		return rounded((0xB8 << 24) | (WINDOW & 0x00FFFFFF), 4);
	}

	public static Surface header() {
		return rounded(HEADER, 4);
	}

	public static Surface footer() {
		return rounded(FOOTER, 4);
	}

	public static Surface cardSurface() {
		return rounded(CARD, 3);
	}

	public static Surface field() {
		return (context, component) -> {
			roundRect(
				context,
				component.x(),
				component.y(),
				component.width(),
				component.height(),
				FIELD_BORDER,
				3
			);
			roundRect(
				context,
				component.x() + 1,
				component.y() + 1,
				component.width() - 2,
				component.height() - 2,
				FIELD_BG,
				3
			);
		};
	}

	public static FlowLayout row() {
		FlowLayout row = Containers.horizontalFlow(
			Sizing.fill(100),
			Sizing.content()
		);
		row.gap(GAP);
		row.verticalAlignment(VerticalAlignment.CENTER);
		return row;
	}

	public static FlowLayout card(String title) {
		FlowLayout card = Containers.verticalFlow(
			Sizing.fill(100),
			Sizing.content()
		);
		card.surface(cardSurface());
		card.padding(Insets.of(CARD_PAD));
		card.gap(CARD_GAP);
		if (title != null && !title.isBlank()) {
			card.child(section(null, title));
			card.child(hline());
		}
		return card;
	}

	public static LabelComponent section(String icon, String title) {
		String text =
			icon == null || icon.isBlank() ? title : icon + "  " + title;
		return Components.label(Component.literal(text.toUpperCase())).color(
			Color.ofArgb(SECTION)
		);
	}

	public static void click() {
		try {
			AfrRuntime runtime = AfrRuntime.get();
			if (runtime != null && !runtime.config().settings().sound) {
				return;
			}
			Minecraft.getInstance()
				.getSoundManager()
				.play(
					SimpleSoundInstance.forUI(
						SoundEvents.UI_BUTTON_CLICK.value(),
						1.0f
					)
				);
		} catch (Throwable ignored) {}
	}

	public static LabelComponent label(String text) {
		return Components.label(Component.literal(text)).color(
			Color.ofArgb(TEXT)
		);
	}

	public static LabelComponent dim(String text) {
		return Components.label(Component.literal(text)).color(
			Color.ofArgb(DIM)
		);
	}

	public static FlowLayout badge(String text, int color) {
		FlowLayout badge = Containers.horizontalFlow(
			Sizing.content(),
			Sizing.content()
		);
		badge.surface(rounded(CARD, 3));
		badge.padding(Insets.of(1));
		badge.verticalAlignment(VerticalAlignment.CENTER);
		badge.child(
			Components.label(Component.literal(text)).color(Color.ofArgb(color))
		);
		return badge;
	}

	public static LabelComponent wrap(String text, int width) {
		return Components.label(Component.literal(text))
			.color(Color.ofArgb(DIM))
			.maxWidth(width);
	}

	public static FlowLayout hline() {
		FlowLayout line = Containers.horizontalFlow(
			Sizing.fill(100),
			Sizing.fixed(1)
		);
		line.surface(Surface.flat(LINE));
		return line;
	}

	public static FlowLayout grow() {
		return Containers.horizontalFlow(Sizing.expand(100), Sizing.fixed(0));
	}

	public static void openUrl(String url) {
		if (url == null || url.isBlank()) {
			return;
		}
		try {
			URI uri = URI.create(url.strip());
			String scheme = uri.getScheme();
			if (scheme == null) {
				return;
			}
			scheme = scheme.toLowerCase(Locale.ROOT);
			if (!"http".equals(scheme) && !"https".equals(scheme)) {
				return;
			}
			Util.getPlatform().openUri(uri);
		} catch (Exception error) {
			AFR.LOGGER.warn("AFR: cannot open URL '{}'", url, error);
		}
	}
}

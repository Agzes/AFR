// SPDX-License-Identifier: GPL-3.0-only
package io.github.agzes.afr.client.ui;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public enum Theme {
	CURRENT(
		"current",
		"Текущая",
		"Current",
		0xE6161616,
		0xFF1E1E1E,
		0xFF101010,
		0xFF1E1E1E,
		0xFF282828,
		0xFF111111,
		0xFF303030,
		0xFF337FC5,
		0xFF4C93D6,
		0xFF17324C,
		0xFFFFFFFF,
		0xFFE6E6E6,
		0xFFA0A0A0,
		0xFF6E6E6E,
		0xFF9CB8D6,
		0xFF3FB950,
		0xFFD29922,
		0xFFF85149
	),

	VESPER(
		"vesper",
		"Vesper",
		"Vesper",
		0xE6101010,
		0xFF161616,
		0xFF101010,
		0xFF1C1C1C,
		0xFF282828,
		0xFF101010,
		0xFF343434,
		0xFFFFC799,
		0xFFFFCFA8,
		0xFF33291F,
		0xFF2B2118,
		0xFFFFFFFF,
		0xFFA0A0A0,
		0xFF7E7E7E,
		0xFFFFC799,
		0xFF99FFE4,
		0xFFFFC799,
		0xFFFF8080
	),

	SEASONAL(
		"seasonal",
		"Сезонная",
		"Seasonal",
		0xE6161616,
		0xFF1E1E1E,
		0xFF101010,
		0xFF1E1E1E,
		0xFF282828,
		0xFF111111,
		0xFF303030,
		0xFF337FC5,
		0xFF4C93D6,
		0xFF17324C,
		0xFFFFFFFF,
		0xFFE6E6E6,
		0xFFA0A0A0,
		0xFF6E6E6E,
		0xFF9CB8D6,
		0xFF3FB950,
		0xFFD29922,
		0xFFF85149,
		true
	);

	public final String id;
	public final String labelRu;
	public final String labelEn;
	public final boolean seasonal;

	public final int window;
	public final int header;
	public final int footer;
	public final int card;
	public final int cardHover;
	public final int fieldBg;
	public final int fieldBorder;

	public final int accent;
	public final int accentHover;
	public final int accentDim;
	public final int accentText;
	public final int text;
	public final int dim;
	public final int muted;
	public final int section;

	public final int good;
	public final int warn;
	public final int bad;

	Theme(
		String id,
		String labelRu,
		String labelEn,
		int window,
		int header,
		int footer,
		int card,
		int cardHover,
		int fieldBg,
		int fieldBorder,
		int accent,
		int accentHover,
		int accentDim,
		int accentText,
		int text,
		int dim,
		int muted,
		int section,
		int good,
		int warn,
		int bad
	) {
		this(
			id,
			labelRu,
			labelEn,
			window,
			header,
			footer,
			card,
			cardHover,
			fieldBg,
			fieldBorder,
			accent,
			accentHover,
			accentDim,
			accentText,
			text,
			dim,
			muted,
			section,
			good,
			warn,
			bad,
			false
		);
	}

	Theme(
		String id,
		String labelRu,
		String labelEn,
		int window,
		int header,
		int footer,
		int card,
		int cardHover,
		int fieldBg,
		int fieldBorder,
		int accent,
		int accentHover,
		int accentDim,
		int accentText,
		int text,
		int dim,
		int muted,
		int section,
		int good,
		int warn,
		int bad,
		boolean seasonal
	) {
		this.id = id;
		this.labelRu = labelRu;
		this.labelEn = labelEn;
		this.seasonal = seasonal;
		this.window = window;
		this.header = header;
		this.footer = footer;
		this.card = card;
		this.cardHover = cardHover;
		this.fieldBg = fieldBg;
		this.fieldBorder = fieldBorder;
		this.accent = accent;
		this.accentHover = accentHover;
		this.accentDim = accentDim;
		this.accentText = accentText;
		this.text = text;
		this.dim = dim;
		this.muted = muted;
		this.section = section;
		this.good = good;
		this.warn = warn;
		this.bad = bad;
	}

	public Map<String, String> colors() {
		Map<String, String> map = new LinkedHashMap<>();
		map.put("window", hex(window));
		map.put("header", hex(header));
		map.put("footer", hex(footer));
		map.put("card", hex(card));
		map.put("cardHover", hex(cardHover));
		map.put("fieldBg", hex(fieldBg));
		map.put("fieldBorder", hex(fieldBorder));
		map.put("accent", hex(accent));
		map.put("accentHover", hex(accentHover));
		map.put("accentDim", hex(accentDim));
		map.put("accentText", hex(accentText));
		map.put("text", hex(text));
		map.put("dim", hex(dim));
		map.put("muted", hex(muted));
		map.put("section", hex(section));
		map.put("good", hex(good));
		map.put("warn", hex(warn));
		map.put("bad", hex(bad));
		return map;
	}

	private static String hex(int argb) {
		return String.format(
			"%02X%02X%02X",
			(argb >> 16) & 0xFF,
			(argb >> 8) & 0xFF,
			argb & 0xFF
		);
	}

	public String label(String language) {
		return "en".equalsIgnoreCase(language) ? labelEn : labelRu;
	}

	public static Theme byId(String id) {
		for (Theme theme : values()) {
			if (theme.id.equalsIgnoreCase(id)) {
				return theme;
			}
		}
		return CURRENT;
	}

	public static List<String> ids() {
		return Arrays.stream(values())
			.filter(t -> !t.seasonal)
			.map(t -> t.id)
			.toList();
	}
}

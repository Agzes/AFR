// SPDX-License-Identifier: GPL-3.0-only
package io.github.agzes.afr.client.ui;

import net.minecraft.client.gui.Font;

public final class TextFit {

	private TextFit() {}

	public static String fit(Font font, String text, int maxWidth) {
		if (text == null) {
			return "";
		}
		if (maxWidth <= 0) {
			return "";
		}
		if (text.length() <= 1 || font.width(text) <= maxWidth) {
			return text;
		}
		String ellipsis = "...";
		int budget = maxWidth - font.width(ellipsis);
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < text.length(); i++) {
			char c = text.charAt(i);
			if (font.width(sb.toString() + c) > budget) {
				break;
			}
			sb.append(c);
		}
		return sb + ellipsis;
	}
}

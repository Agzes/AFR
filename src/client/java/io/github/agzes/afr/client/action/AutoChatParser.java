// SPDX-License-Identifier: GPL-3.0-only
package io.github.agzes.afr.client.action;

import java.util.ArrayList;
import java.util.List;

public final class AutoChatParser {

	private static final int LIMIT = 255;

	private AutoChatParser() {}

	public static List<List<String>> parse(String input) {
		if (input == null || input.isBlank()) {
			return List.of();
		}
		String cleaned = input.replace("`", "");
		List<String> lines = new ArrayList<>();

		for (String raw : cleaned.split("\\R")) {
			if (raw.isBlank()) {
				continue;
			}
			if (raw.length() <= LIMIT) {
				lines.add(raw.strip());
				continue;
			}
			String rest = raw.strip();
			while (rest.length() > LIMIT) {
				int pos = rest.lastIndexOf(' ', LIMIT);
				if (pos <= 0) {
					lines.add(rest.substring(0, LIMIT));
					rest = rest.substring(LIMIT);
				} else {
					lines.add(rest.substring(0, pos).strip());
					rest = rest.substring(pos + 1).strip();
				}
			}
			if (!rest.isEmpty()) {
				lines.add(rest);
			}
		}

		String before;
		String after;
		if (lines.size() <= 2) {
			before = "S300";
			after = "S300";
		} else if (lines.size() <= 3) {
			before = "S700";
			after = "S1000";
		} else if (lines.size() <= 5) {
			before = "S2000";
			after = "S2000";
		} else {
			before = "S2000";
			after = "S4000";
		}

		List<List<String>> steps = new ArrayList<>();
		for (String line : lines) {
			steps.add(List.of("RPAction", "Send", before, after, line));
		}
		return steps;
	}
}

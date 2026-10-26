// SPDX-License-Identifier: GPL-3.0-only
package io.github.agzes.afr.client.util;

import com.google.gson.JsonElement;
import io.github.agzes.afr.client.net.VersionChecker;

public final class VersionMatch {

	private VersionMatch() {}

	public static boolean matches(String expression, int current) {
		if (expression == null || expression.isBlank()) {
			return true;
		}
		for (String part : expression.split("[,\\s]+")) {
			if (part.isBlank()) {
				continue;
			}
			String op = "=";
			String rest = part;
			for (String candidate : new String[] {
				">=",
				"<=",
				"!=",
				"==",
				">",
				"<",
				"=",
			}) {
				if (part.startsWith(candidate)) {
					op = candidate;
					rest = part.substring(candidate.length());
					break;
				}
			}
			int code = code(rest);
			boolean ok = switch (op) {
				case ">" -> current > code;
				case ">=" -> current >= code;
				case "<" -> current < code;
				case "<=" -> current <= code;
				case "!=" -> current != code;
				default -> current == code;
			};
			if (!ok) {
				return false;
			}
		}
		return true;
	}

	public static int code(JsonElement element) {
		if (element == null || element.isJsonNull()) {
			return 0;
		}
		if (
			element.isJsonPrimitive() && element.getAsJsonPrimitive().isNumber()
		) {
			return element.getAsInt();
		}
		return code(element.getAsString());
	}

	public static int code(String value) {
		if (value == null) {
			return 0;
		}
		String v = value.strip();
		if (v.startsWith("v") || v.startsWith("V")) {
			v = v.substring(1);
		}
		if (v.matches("\\d{3,}")) {
			return Integer.parseInt(v);
		}
		return VersionChecker.parseCode(v);
	}
}

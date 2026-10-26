// SPDX-License-Identifier: GPL-3.0-only
package io.github.agzes.afr.client.action;

import io.github.agzes.afr.client.model.Parameter;
import io.github.agzes.afr.client.model.RpProfile;
import io.github.agzes.afr.client.model.UserSettings;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ThreadLocalRandom;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.lwjgl.glfw.GLFW;

public final class PlaceholderResolver {

	private static final Pattern RANDOM = Pattern.compile(
		"\\{([^{}]*\\|[^{}]*)\\}"
	);
	private static final Pattern TOKEN = Pattern.compile("\\{([^{}\\s]+)\\}");

	private PlaceholderResolver() {}

	public static String resolve(
		String raw,
		UserSettings settings,
		RpProfile profile
	) {
		if (raw == null) {
			return "";
		}
		Matcher random = RANDOM.matcher(raw);
		String text = random.replaceAll(match ->
			Matcher.quoteReplacement(pickRandom(match.group(1)))
		);

		Matcher token = TOKEN.matcher(text);
		StringBuilder out = new StringBuilder();
		while (token.find()) {
			String value = resolveToken(token.group(1), settings, profile);
			token.appendReplacement(
				out,
				Matcher.quoteReplacement(value != null ? value : token.group(0))
			);
		}
		token.appendTail(out);
		return out.toString();
	}

	public static String pickRandom(String variants) {
		String[] parts = variants.split("\\|");
		if (parts.length == 0) {
			return "";
		}
		String chosen = parts[
			ThreadLocalRandom.current().nextInt(parts.length)
		];
		return chosen.strip();
	}

	public static List<Integer> specialKeys(String raw) {
		List<Integer> keys = new ArrayList<>();
		if (raw == null) {
			return keys;
		}
		Matcher token = TOKEN.matcher(raw);
		while (token.find()) {
			Integer key = specialKey(token.group(1));
			if (key != null) {
				keys.add(key);
			}
		}
		return keys;
	}

	private static Integer specialKey(String token) {
		return switch (token.toLowerCase(Locale.ROOT)) {
			case "enter" -> GLFW.GLFW_KEY_ENTER;
			case "left" -> GLFW.GLFW_KEY_LEFT;
			case "right" -> GLFW.GLFW_KEY_RIGHT;
			case "up" -> GLFW.GLFW_KEY_UP;
			case "down" -> GLFW.GLFW_KEY_DOWN;
			case "tab" -> GLFW.GLFW_KEY_TAB;
			case "space" -> GLFW.GLFW_KEY_SPACE;
			case "home" -> GLFW.GLFW_KEY_HOME;
			case "end" -> GLFW.GLFW_KEY_END;
			default -> null;
		};
	}

	public static boolean isKnown(String token, RpProfile profile) {
		if (token.indexOf('|') >= 0) {
			return true;
		}
		String lower = token.toLowerCase(Locale.ROOT);
		switch (lower) {
			case
				"enter",
				"left",
				"right",
				"up",
				"down",
				"home",
				"end",
				"tab",
				"space",
				"name",
				"rpname",
				"firstname",
				"lastname",
				"position",
				"role":
				return true;
			default:
				break;
		}
		String id = lower.startsWith("param:") ? token.substring(6) : token;
		return profile != null && profile.parameter(id) != null;
	}

	private static String resolveToken(
		String token,
		UserSettings settings,
		RpProfile profile
	) {
		String lower = token.toLowerCase(Locale.ROOT);
		switch (lower) {
			case "enter":
			case "left":
			case "right":
			case "up":
			case "down":
			case "home":
			case "end":
			case "tab":
			case "space":
				return "";
			case "name":
			case "rpname":
				return settings.rpName;
			case "firstname":
				return firstName(settings.rpName);
			case "lastname":
				return lastName(settings.rpName);
			case "position":
			case "role":
				return settings.rpRole;
			default:
				break;
		}

		String id = lower.startsWith("param:") ? token.substring(6) : token;
		if (profile != null) {
			Parameter parameter = profile.parameter(id);
			if (parameter != null) {
				return parameter.asText();
			}
		}
		return null;
	}

	private static String firstName(String full) {
		if (full == null || full.isBlank()) {
			return "";
		}
		String[] parts = full.strip().split("\\s+");
		return parts[0];
	}

	private static String lastName(String full) {
		if (full == null || full.isBlank()) {
			return "";
		}
		String[] parts = full.strip().split("\\s+");
		return parts.length > 1 ? parts[parts.length - 1] : "";
	}
}

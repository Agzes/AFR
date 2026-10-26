// SPDX-License-Identifier: GPL-3.0-only
package io.github.agzes.afr.client.input;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import org.lwjgl.glfw.GLFW;

public record KeyCombo(int key, boolean ctrl, boolean alt, boolean shift) {
	private static final Map<String, Integer> NAMES = new HashMap<>();
	private static final Map<Integer, String> CANON = new HashMap<>();

	static {
		CANON.put(GLFW.GLFW_KEY_LEFT_SHIFT, "Shift");
		CANON.put(GLFW.GLFW_KEY_RIGHT_SHIFT, "RShift");
		CANON.put(GLFW.GLFW_KEY_LEFT_CONTROL, "Ctrl");
		CANON.put(GLFW.GLFW_KEY_RIGHT_CONTROL, "RCtrl");
		CANON.put(GLFW.GLFW_KEY_LEFT_ALT, "Alt");
		CANON.put(GLFW.GLFW_KEY_RIGHT_ALT, "RAlt");
		CANON.put(GLFW.GLFW_KEY_CAPS_LOCK, "CapsLock");
		CANON.put(1004, "Mouse4");
		CANON.put(1005, "Mouse5");
		for (int i = 1; i <= 24; i++) {
			NAMES.put("f" + i, GLFW.GLFW_KEY_F1 + i - 1);
		}
		for (char c = 'a'; c <= 'z'; c++) {
			NAMES.put(String.valueOf(c), GLFW.GLFW_KEY_A + (c - 'a'));
		}
		for (char c = '0'; c <= '9'; c++) {
			NAMES.put(String.valueOf(c), GLFW.GLFW_KEY_0 + (c - '0'));
		}
		NAMES.put("insert", GLFW.GLFW_KEY_INSERT);
		NAMES.put("shift", GLFW.GLFW_KEY_LEFT_SHIFT);
		NAMES.put("lshift", GLFW.GLFW_KEY_LEFT_SHIFT);
		NAMES.put("rightshift", GLFW.GLFW_KEY_RIGHT_SHIFT);
		NAMES.put("rshift", GLFW.GLFW_KEY_RIGHT_SHIFT);
		NAMES.put("ctrl", GLFW.GLFW_KEY_LEFT_CONTROL);
		NAMES.put("control", GLFW.GLFW_KEY_LEFT_CONTROL);
		NAMES.put("lctrl", GLFW.GLFW_KEY_LEFT_CONTROL);
		NAMES.put("rightctrl", GLFW.GLFW_KEY_RIGHT_CONTROL);
		NAMES.put("rctrl", GLFW.GLFW_KEY_RIGHT_CONTROL);
		NAMES.put("alt", GLFW.GLFW_KEY_LEFT_ALT);
		NAMES.put("lalt", GLFW.GLFW_KEY_LEFT_ALT);
		NAMES.put("rightalt", GLFW.GLFW_KEY_RIGHT_ALT);
		NAMES.put("ralt", GLFW.GLFW_KEY_RIGHT_ALT);
		NAMES.put("delete", GLFW.GLFW_KEY_DELETE);
		NAMES.put("del", GLFW.GLFW_KEY_DELETE);
		NAMES.put("home", GLFW.GLFW_KEY_HOME);
		NAMES.put("end", GLFW.GLFW_KEY_END);
		NAMES.put("pageup", GLFW.GLFW_KEY_PAGE_UP);
		NAMES.put("pagedown", GLFW.GLFW_KEY_PAGE_DOWN);
		NAMES.put("up", GLFW.GLFW_KEY_UP);
		NAMES.put("down", GLFW.GLFW_KEY_DOWN);
		NAMES.put("left", GLFW.GLFW_KEY_LEFT);
		NAMES.put("right", GLFW.GLFW_KEY_RIGHT);
		NAMES.put("space", GLFW.GLFW_KEY_SPACE);
		NAMES.put("tab", GLFW.GLFW_KEY_TAB);
		NAMES.put("enter", GLFW.GLFW_KEY_ENTER);
		NAMES.put("backspace", GLFW.GLFW_KEY_BACKSPACE);
		NAMES.put("escape", GLFW.GLFW_KEY_ESCAPE);
		NAMES.put("esc", GLFW.GLFW_KEY_ESCAPE);
		NAMES.put("grave", GLFW.GLFW_KEY_GRAVE_ACCENT);
		NAMES.put("minus", GLFW.GLFW_KEY_MINUS);
		NAMES.put("equal", GLFW.GLFW_KEY_EQUAL);
		NAMES.put("comma", GLFW.GLFW_KEY_COMMA);
		NAMES.put("period", GLFW.GLFW_KEY_PERIOD);
		NAMES.put("slash", GLFW.GLFW_KEY_SLASH);
		NAMES.put("semicolon", GLFW.GLFW_KEY_SEMICOLON);
		NAMES.put("apostrophe", GLFW.GLFW_KEY_APOSTROPHE);
		NAMES.put("backslash", GLFW.GLFW_KEY_BACKSLASH);
		NAMES.put("bracketleft", GLFW.GLFW_KEY_LEFT_BRACKET);
		NAMES.put("bracketright", GLFW.GLFW_KEY_RIGHT_BRACKET);
		NAMES.put("capslock", GLFW.GLFW_KEY_CAPS_LOCK);
		NAMES.put("caps", GLFW.GLFW_KEY_CAPS_LOCK);
		NAMES.put("mouse4", 1004);
		NAMES.put("mouse5", 1005);
	}

	public static KeyCombo parse(String bind) {
		if (bind == null || bind.isBlank()) {
			return null;
		}
		boolean ctrl = false;
		boolean alt = false;
		boolean shift = false;
		boolean unsupported = false;
		String token = bind.trim();

		boolean changed = true;
		while (changed) {
			changed = false;
			String lower = token.toLowerCase(Locale.ROOT);
			if (lower.startsWith("ctrl+")) {
				ctrl = true;
				token = token.substring(5);
				changed = true;
			} else if (lower.startsWith("alt+")) {
				alt = true;
				token = token.substring(4);
				changed = true;
			} else if (lower.startsWith("shift+")) {
				shift = true;
				token = token.substring(6);
				changed = true;
			} else if (
				lower.startsWith("win+") ||
				lower.startsWith("super+") ||
				lower.startsWith("meta+")
			) {
				unsupported = true;
				token = token.substring(token.indexOf('+') + 1);
				changed = true;
			}
		}

		StringBuilder cleaned = new StringBuilder();
		for (int i = 0; i < token.length(); i++) {
			char c = token.charAt(i);
			if (c == '!') {
				alt = true;
			} else if (c == '^') {
				ctrl = true;
			} else if (c == '+') {
				shift = true;
			} else if (c == '#') {
				unsupported = true;
			} else {
				cleaned.append(c);
			}
		}

		String name = cleaned
			.toString()
			.replace("+", "")
			.trim()
			.toLowerCase(Locale.ROOT);
		Integer code = NAMES.get(name);
		if (code == null || unsupported) {
			return null;
		}
		return new KeyCombo(code, ctrl, alt, shift);
	}

	public static KeyCombo of(
		int key,
		boolean ctrl,
		boolean alt,
		boolean shift
	) {
		return new KeyCombo(key, ctrl, alt, shift);
	}

	public String display() {
		StringBuilder sb = new StringBuilder();
		if (ctrl) {
			sb.append("Ctrl+");
		}
		if (alt) {
			sb.append("Alt+");
		}
		if (shift) {
			sb.append("Shift+");
		}
		sb.append(name(key));
		return sb.toString();
	}

	public static String name(int key) {
		String canon = CANON.get(key);
		if (canon != null) {
			return canon;
		}
		for (Map.Entry<String, Integer> entry : NAMES.entrySet()) {
			if (entry.getValue() == key) {
				String n = entry.getKey();
				if (n.startsWith("f") && n.length() <= 3) {
					return n.toUpperCase(Locale.ROOT);
				}
				return (
					n.substring(0, 1).toUpperCase(Locale.ROOT) + n.substring(1)
				);
			}
		}
		return "Key" + key;
	}
}

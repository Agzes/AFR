// SPDX-License-Identifier: GPL-3.0-only
package io.github.agzes.afr.client.action;

import java.util.List;

public record StepSpec(
	boolean gender,
	boolean send,
	String before,
	String after,
	String text,
	String textFemale
) {
	public static StepSpec parse(List<String> raw) {
		if (raw == null || raw.size() < 5) {
			return null;
		}
		boolean gender = "RPActionGender".equalsIgnoreCase(raw.get(0));
		boolean send = !"Input".equalsIgnoreCase(raw.get(1));
		String text = raw.get(4);
		String textFemale =
			raw.size() > 5 && raw.get(5) != null ? raw.get(5) : text;
		return new StepSpec(
			gender,
			send,
			raw.get(2),
			raw.get(3),
			text,
			textFemale
		);
	}
}

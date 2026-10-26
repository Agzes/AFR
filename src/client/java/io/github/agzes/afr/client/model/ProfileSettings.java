// SPDX-License-Identifier: GPL-3.0-only
package io.github.agzes.afr.client.model;

import com.google.gson.annotations.SerializedName;
import java.util.ArrayList;
import java.util.List;

public class ProfileSettings {

	@SerializedName("Menu")
	public String menu = "";

	@SerializedName("ForceStop")
	public String forceStop = "";

	@SerializedName("Restart")
	public String restart = "";

	@SerializedName("s100_250_timings")
	public List<Integer> s100_250 = new ArrayList<>(List.of(100, 250));

	@SerializedName("s300_800_timings")
	public List<Integer> s300_800 = new ArrayList<>(
		List.of(300, 500, 700, 800)
	);

	@SerializedName("s1000_4000_timings")
	public List<Integer> s1000_4000 = new ArrayList<>(
		List.of(1000, 2000, 3000, 4000)
	);

	@SerializedName("multiplier")
	public double multiplier = 1.0;

	public int delay(String code) {
		if (code == null) {
			return 0;
		}
		String digits = code.replaceAll("[^0-9]", "");
		if (digits.isEmpty()) {
			return 0;
		}
		int value = switch (Integer.parseInt(digits)) {
			case 100 -> at(s100_250, 0, 100);
			case 250 -> at(s100_250, 1, 250);
			case 300 -> at(s300_800, 0, 300);
			case 500 -> at(s300_800, 1, 500);
			case 700 -> at(s300_800, 2, 700);
			case 800 -> at(s300_800, 3, 800);
			case 1000 -> at(s1000_4000, 0, 1000);
			case 2000 -> at(s1000_4000, 1, 2000);
			case 3000 -> at(s1000_4000, 2, 3000);
			case 4000 -> at(s1000_4000, 3, 4000);
			default -> Integer.parseInt(digits);
		};
		return Math.max(0, (int) Math.round(value * multiplier));
	}

	private static int at(List<Integer> list, int index, int fallback) {
		if (list != null && index < list.size() && list.get(index) != null) {
			return list.get(index);
		}
		return fallback;
	}
}

// SPDX-License-Identifier: GPL-3.0-only
package io.github.agzes.afr.client.model;

import io.github.agzes.afr.client.i18n.Lang;
import java.util.ArrayList;
import java.util.List;

public class RpAction {

	public String name = "";
	public String bind = "";
	public String description = "";
	public String category = "";
	public String icon = "";
	public int priority = 0;
	public int cooldown = 0;
	public boolean favorite = false;
	public List<List<String>> steps = new ArrayList<>();

	public String displayName() {
		return name == null || name.isBlank() ? Lang.t("lib.no_title") : name;
	}
}

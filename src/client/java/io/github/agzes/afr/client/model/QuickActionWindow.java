// SPDX-License-Identifier: GPL-3.0-only
package io.github.agzes.afr.client.model;

import java.util.ArrayList;
import java.util.List;

public class QuickActionWindow {

	public String id = "";
	public String name = "";
	public String bind = "";
	public String style = "list";
	public int x = -1;
	public int y = -1;
	public boolean pinned = false;
	public int tileCols = 6;
	public List<String> actions = new ArrayList<>();
	public List<String> tiles = new ArrayList<>();
}

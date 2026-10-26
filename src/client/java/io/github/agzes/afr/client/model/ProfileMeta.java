// SPDX-License-Identifier: GPL-3.0-only
package io.github.agzes.afr.client.model;

import com.google.gson.annotations.SerializedName;
import java.util.ArrayList;
import java.util.List;

public class ProfileMeta {

	@SerializedName("Label")
	public String label = "";

	@SerializedName("RevName")
	public String revName = "";

	@SerializedName("Version")
	public String version = "1.0.0";

	@SerializedName("VersionCode")
	public int versionCode = 10000;

	@SerializedName("MinAFRVersion")
	public int minAfrVersion = 10000;

	@SerializedName("Author")
	public String author = "";

	@SerializedName("Authors")
	public List<String> authors = new ArrayList<>();

	@SerializedName("Editors")
	public List<String> editors = new ArrayList<>();

	@SerializedName("Description")
	public String description = "";

	@SerializedName("Tags")
	public List<String> tags = new ArrayList<>();

	@SerializedName("Source")
	public String source = "local";

	@SerializedName("RemoteId")
	public String remoteId = "";

	@SerializedName("RemoteVersionCode")
	public int remoteVersionCode = 0;

	@SerializedName("Modified")
	public boolean modified = false;

	public boolean isLibrary() {
		return "library".equalsIgnoreCase(source);
	}

	public List<String> authorList() {
		List<String> list = new ArrayList<>();
		if (authors != null) {
			for (String a : authors) {
				if (a != null && !a.isBlank()) {
					list.add(a.strip());
				}
			}
		}
		if (list.isEmpty() && author != null && !author.isBlank()) {
			list.add(author.strip());
		}
		return list;
	}

	public String authorsText() {
		return String.join(", ", authorList());
	}

	public String editorsText() {
		List<String> list = new ArrayList<>();
		if (editors != null) {
			for (String e : editors) {
				if (e != null && !e.isBlank()) {
					list.add(e.strip());
				}
			}
		}
		return String.join(", ", list);
	}
}

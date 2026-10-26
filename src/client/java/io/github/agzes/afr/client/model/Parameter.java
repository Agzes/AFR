// SPDX-License-Identifier: GPL-3.0-only
package io.github.agzes.afr.client.model;

import com.google.gson.annotations.SerializedName;
import java.util.ArrayList;
import java.util.List;

public class Parameter {

	public String id = "";
	public String label = "";

	@SerializedName("param_type")
	public String paramType = "text";

	public Object value;

	@SerializedName("default")
	public Object defaultValue;

	@SerializedName("options")
	public List<String> options = new ArrayList<>();

	public boolean isCheckbox() {
		return (
			"checkbox".equalsIgnoreCase(paramType) ||
			"bool".equalsIgnoreCase(paramType)
		);
	}

	public String asText() {
		Object v = value != null ? value : defaultValue;
		if (v == null) {
			return "";
		}
		if (v instanceof Boolean b) {
			return b ? "true" : "false";
		}
		if (v instanceof Double d) {
			return d == Math.floor(d)
				? String.valueOf(d.longValue())
				: String.valueOf(d);
		}
		return String.valueOf(v);
	}

	public boolean asBool() {
		Object v = value != null ? value : defaultValue;
		if (v instanceof Boolean b) {
			return b;
		}
		return (
			v != null &&
			("true".equalsIgnoreCase(String.valueOf(v)) ||
				"1".equals(String.valueOf(v)))
		);
	}
}

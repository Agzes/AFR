// SPDX-License-Identifier: GPL-3.0-only
package io.github.agzes.afr.client.model;

public record ActionRef(String id, RpAction action) {
	public String displayName() {
		return action == null ? "" : action.displayName();
	}
}

// SPDX-License-Identifier: GPL-3.0-only
package io.github.agzes.afr.client.ui;

import io.wispforest.owo.ui.container.FlowLayout;
import io.wispforest.owo.ui.container.ScrollContainer;
import io.wispforest.owo.ui.core.Size;
import io.wispforest.owo.ui.core.Sizing;

public class AfrScroll extends ScrollContainer<FlowLayout> {

	private double pending = -1;

	public AfrScroll(Sizing horizontal, Sizing vertical, FlowLayout child) {
		super(ScrollDirection.VERTICAL, horizontal, vertical, child);
	}

	public double getOffset() {
		return scrollOffset;
	}

	public void restore(double offset) {
		this.pending = offset;
		this.scrollOffset = offset;
	}

	@Override
	public void layout(Size space) {
		super.layout(space);
		if (pending >= 0) {
			this.scrollOffset = Math.max(0, Math.min(pending, maxScroll + 0.5));
			pending = -1;
		}
	}
}

// SPDX-License-Identifier: GPL-3.0-only
package io.github.agzes.afr.client.ui;

import io.wispforest.owo.ui.base.BaseComponent;
import io.wispforest.owo.ui.core.OwoUIDrawContext;
import io.wispforest.owo.ui.core.Sizing;

public class AfrSwatch extends BaseComponent {

	private int color;

	public AfrSwatch(int color) {
		this.color = color;
	}

	public AfrSwatch color(int color) {
		this.color = color;
		this.dirty = true;
		return this;
	}

	@Override
	protected int determineHorizontalContentSize(Sizing sizing) {
		return 22;
	}

	@Override
	protected int determineVerticalContentSize(Sizing sizing) {
		return 14;
	}

	@Override
	public void draw(
		OwoUIDrawContext context,
		int mouseX,
		int mouseY,
		float partialTicks,
		float delta
	) {
		Ui.roundRect(context, x, y, width, height, Ui.FIELD_BORDER, 2);
		Ui.roundRect(context, x + 1, y + 1, width - 2, height - 2, color, 2);
	}
}

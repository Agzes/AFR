// SPDX-License-Identifier: GPL-3.0-only
package io.github.agzes.afr.client.ui;

import io.wispforest.owo.ui.base.BaseComponent;
import io.wispforest.owo.ui.core.OwoUIDrawContext;
import io.wispforest.owo.ui.core.Sizing;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.world.item.ItemStack;

public class AfrTile extends BaseComponent {

	private final ItemStack stack;
	private final String fallback;
	private Runnable onPress;
	private boolean active;
	private boolean favorite;
	private int size = 28;

	public AfrTile(ItemStack stack, String fallback) {
		this.stack = stack;
		this.fallback = fallback;
	}

	public AfrTile size(int size) {
		this.size = Math.max(12, size);
		return this;
	}

	public AfrTile onPress(Runnable onPress) {
		this.onPress = onPress;
		return this;
	}

	public AfrTile active(boolean active) {
		this.active = active;
		return this;
	}

	public AfrTile favorite(boolean favorite) {
		this.favorite = favorite;
		return this;
	}

	@Override
	protected int determineHorizontalContentSize(Sizing sizing) {
		return size;
	}

	@Override
	protected int determineVerticalContentSize(Sizing sizing) {
		return size;
	}

	@Override
	public void draw(
		OwoUIDrawContext context,
		int mouseX,
		int mouseY,
		float partialTicks,
		float delta
	) {
		int border = favorite ? Ui.WARN : hovered ? Ui.CARD_HOVER : 0x00000000;
		if (border >>> 24 != 0) {
			Ui.roundRect(context, x, y, width, height, border, 3);
			Ui.roundRect(
				context,
				x + 1,
				y + 1,
				width - 2,
				height - 2,
				Ui.CARD,
				3
			);
		}
		int background = active
			? Ui.ACCENT_DIM
			: hovered
				? Ui.CARD_HOVER
				: Ui.CARD;
		Ui.roundRect(
			context,
			x + 1,
			y + 1,
			width - 2,
			height - 2,
			background,
			3
		);
		if (!stack.isEmpty()) {
			context.renderItem(
				stack,
				x + Math.max(0, (width - 16) / 2),
				y + Math.max(0, (height - 16) / 2)
			);
		} else if (fallback != null && !fallback.isBlank()) {
			Font font = Minecraft.getInstance().font;
			String text = TextFit.fit(font, fallback, width - 4);
			context.drawString(
				font,
				text,
				x + (width - font.width(text)) / 2,
				y + (height - font.lineHeight) / 2 + 1,
				Ui.DIM,
				false
			);
		}
	}

	@Override
	public boolean onMouseDown(double mouseX, double mouseY, int button) {
		if (button == 0 && onPress != null) {
			Ui.click();
			onPress.run();
			return true;
		}
		return super.onMouseDown(mouseX, mouseY, button);
	}
}

// SPDX-License-Identifier: GPL-3.0-only
package io.github.agzes.afr.client.ui;

import io.wispforest.owo.ui.base.BaseComponent;
import io.wispforest.owo.ui.core.OwoUIDrawContext;
import io.wispforest.owo.ui.core.Sizing;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.world.item.ItemStack;

public class AfrListItem extends BaseComponent {

	private String icon;
	private String name;
	private String trailing;
	private ItemStack stack = ItemStack.EMPTY;
	private Runnable onPress;
	private boolean active;

	public AfrListItem(String icon, String name, String trailing) {
		this.icon = icon;
		this.name = name;
		this.trailing = trailing;
	}

	public AfrListItem stack(ItemStack stack) {
		this.stack = stack == null ? ItemStack.EMPTY : stack;
		return this;
	}

	public AfrListItem onPress(Runnable onPress) {
		this.onPress = onPress;
		return this;
	}

	public AfrListItem active(boolean active) {
		this.active = active;
		return this;
	}

	@Override
	protected int determineHorizontalContentSize(Sizing sizing) {
		return Minecraft.getInstance().font.width(name) + 70;
	}

	@Override
	protected int determineVerticalContentSize(Sizing sizing) {
		return 20;
	}

	@Override
	public void draw(
		OwoUIDrawContext context,
		int mouseX,
		int mouseY,
		float partialTicks,
		float delta
	) {
		Font font = Minecraft.getInstance().font;
		int background = active
			? Ui.ACCENT_DIM
			: hovered
				? Ui.CARD_HOVER
				: 0x00000000;
		if (background >>> 24 != 0) {
			Ui.roundRect(context, x, y, width, height, background, 3);
		}
		if (active) {
			Ui.roundRect(context, x, y + 2, 2, height - 4, Ui.ACCENT, 1);
		}
		int cursor = x + 6;
		if (!stack.isEmpty()) {
			context.renderItem(stack, cursor, y + (height - 16) / 2);
			cursor += 20;
		} else if (icon != null && !icon.isBlank()) {
			context.drawString(
				font,
				icon,
				cursor,
				y + (height - font.lineHeight) / 2 + 1,
				Ui.SECTION,
				false
			);
			cursor += font.width(icon) + 6;
		}
		int trailingWidth =
			trailing == null || trailing.isBlank()
				? 0
				: font.width(trailing) + 8;
		int nameWidth = Math.max(10, x + width - 8 - trailingWidth - cursor);
		context.drawString(
			font,
			TextFit.fit(font, name, nameWidth),
			cursor,
			y + (height - font.lineHeight) / 2 + 1,
			Ui.TEXT,
			false
		);
		if (trailingWidth > 0) {
			context.drawString(
				font,
				trailing,
				x + width - 8 - font.width(trailing),
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

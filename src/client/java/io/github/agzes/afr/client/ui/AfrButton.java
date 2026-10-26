// SPDX-License-Identifier: GPL-3.0-only
package io.github.agzes.afr.client.ui;

import io.wispforest.owo.ui.base.BaseComponent;
import io.wispforest.owo.ui.core.OwoUIDrawContext;
import io.wispforest.owo.ui.core.Sizing;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.Component;

public class AfrButton extends BaseComponent {

	public enum Variant {
		PRIMARY,
		SECONDARY,
		GHOST,
		DANGER,
		ICON,
		TAB,
	}

	private Component text;
	private final Variant variant;
	private Runnable onPress;
	private boolean active;
	private boolean enabled = true;

	public AfrButton(Component text, Variant variant, Runnable onPress) {
		this.text = text;
		this.variant = variant;
		this.onPress = onPress;
	}

	public AfrButton active(boolean active) {
		this.active = active;
		return this;
	}

	public AfrButton enabled(boolean enabled) {
		this.enabled = enabled;
		return this;
	}

	public AfrButton w(int width) {
		horizontalSizing(Sizing.fixed(width));
		return this;
	}

	public AfrButton setText(Component text) {
		this.text = text;
		this.dirty = true;
		return this;
	}

	public AfrButton tip(String text) {
		tooltip(Component.literal(text));
		return this;
	}

	private int padX() {
		return variant == Variant.ICON ? 6 : 9;
	}

	@Override
	protected int determineHorizontalContentSize(Sizing sizing) {
		if (variant == Variant.ICON) {
			return Minecraft.getInstance().font.lineHeight + 7;
		}
		return Minecraft.getInstance().font.width(text) + padX() * 2;
	}

	@Override
	protected int determineVerticalContentSize(Sizing sizing) {
		return Minecraft.getInstance().font.lineHeight + 7;
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
		int border = borderColor();
		int background = background();
		if (border != 0) {
			Ui.roundRect(context, x, y, width, height, border, 3);
			Ui.roundRect(
				context,
				x + 1,
				y + 1,
				width - 2,
				height - 2,
				background,
				3
			);
		} else if (background >>> 24 != 0) {
			Ui.roundRect(context, x, y, width, height, background, 3);
		}
		String label = TextFit.fit(font, text.getString(), width - padX() * 2);
		int textX = x + (width - font.width(label)) / 2;
		int textY = y + (height - font.lineHeight) / 2 + 1;
		context.drawString(font, label, textX, textY, textColor(), false);
		if (variant == Variant.TAB && active) {
			Ui.roundRect(
				context,
				x + 3,
				y + height - 2,
				width - 6,
				2,
				Ui.ACCENT,
				1
			);
		}
	}

	private int background() {
		if (!enabled) {
			return Ui.FIELD_BG;
		}
		return backgroundFor(hovered);
	}

	private int backgroundFor(boolean hover) {
		return switch (variant) {
			case PRIMARY -> hover ? Ui.ACCENT_HOVER : Ui.ACCENT;
			case DANGER -> hover ? Ui.CARD_HOVER : Ui.CARD;
			case GHOST -> hover ? 0x22FFFFFF : 0x00000000;
			case ICON -> hover ? Ui.CARD_HOVER : Ui.FIELD_BG;
			case TAB -> active
				? 0x00000000
				: hover
					? Ui.CARD_HOVER
					: 0x00000000;
			case SECONDARY -> active
				? Ui.ACCENT_DIM
				: hover
					? Ui.CARD_HOVER
					: Ui.CARD;
		};
	}

	private int borderColor() {
		if (!enabled) {
			return Ui.FIELD_BORDER;
		}
		return borderFor(hovered);
	}

	private int borderFor(boolean hover) {
		return switch (variant) {
			case PRIMARY -> Ui.ACCENT;
			case DANGER -> Ui.BAD;
			case GHOST -> 0x00000000;
			case ICON -> hover ? Ui.SECTION : Ui.FIELD_BORDER;
			case TAB -> 0x00000000;
			case SECONDARY -> active
				? Ui.ACCENT
				: hover
					? Ui.SECTION
					: Ui.FIELD_BORDER;
		};
	}

	private int textColor() {
		if (!enabled) {
			return Ui.MUTED;
		}
		return textFor(hovered);
	}

	private int textFor(boolean hover) {
		return switch (variant) {
			case PRIMARY -> Ui.ACCENT_TEXT;
			case DANGER -> Ui.BAD;
			case GHOST, ICON -> hover ? Ui.TEXT : Ui.DIM;
			case TAB -> active ? Ui.TEXT : hover ? Ui.TEXT : Ui.DIM;
			case SECONDARY -> Ui.TEXT;
		};
	}

	@Override
	public boolean onMouseDown(double mouseX, double mouseY, int button) {
		if (button == 0 && enabled && onPress != null) {
			Ui.click();
			onPress.run();
			return true;
		}
		return super.onMouseDown(mouseX, mouseY, button);
	}
}

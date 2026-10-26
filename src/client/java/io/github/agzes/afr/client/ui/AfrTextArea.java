// SPDX-License-Identifier: GPL-3.0-only
package io.github.agzes.afr.client.ui;

import io.wispforest.owo.ui.base.BaseComponent;
import io.wispforest.owo.ui.core.OwoUIDrawContext;
import io.wispforest.owo.ui.core.Sizing;
import java.util.function.Consumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.Component;

public class AfrTextArea extends BaseComponent {

	private final StringBuilder text = new StringBuilder();
	private Consumer<String> onChanged;
	private String hint = "";
	private boolean focused;

	public AfrTextArea() {}

	public AfrTextArea text(String value) {
		text.setLength(0);
		if (value != null) {
			text.append(value);
		}
		return this;
	}

	public AfrTextArea hint(String hint) {
		this.hint = hint == null ? "" : hint;
		return this;
	}

	public String value() {
		return text.toString();
	}

	public AfrTextArea onChanged(Consumer<String> onChanged) {
		this.onChanged = onChanged;
		return this;
	}

	private void notifyChanged() {
		if (onChanged != null) {
			onChanged.accept(text.toString());
		}
	}

	@Override
	protected int determineHorizontalContentSize(Sizing sizing) {
		return 180;
	}

	@Override
	protected int determineVerticalContentSize(Sizing sizing) {
		return 60;
	}

	@Override
	public boolean canFocus(
		io.wispforest.owo.ui.core.Component.FocusSource source
	) {
		return true;
	}

	@Override
	public void onFocusGained(
		io.wispforest.owo.ui.core.Component.FocusSource source
	) {
		this.focused = true;
	}

	@Override
	public void onFocusLost() {
		this.focused = false;
	}

	@Override
	public boolean onMouseDown(double mouseX, double mouseY, int button) {
		return true;
	}

	@Override
	public boolean onCharTyped(char chr, int modifiers) {
		text.append(chr);
		notifyChanged();
		return true;
	}

	@Override
	public boolean onKeyPress(int keyCode, int scanCode, int modifiers) {
		if (keyCode == 259) {
			if (text.length() > 0) {
				text.deleteCharAt(text.length() - 1);
				notifyChanged();
			}
			return true;
		}
		if (keyCode == 257 || keyCode == 335) {
			text.append('\n');
			notifyChanged();
			return true;
		}
		if (keyCode == 86 && (modifiers & 2) != 0) {
			String clip =
				Minecraft.getInstance().keyboardHandler.getClipboard();
			if (clip != null && !clip.isEmpty()) {
				text.append(clip);
				notifyChanged();
			}
			return true;
		}
		return false;
	}

	@Override
	public void draw(
		OwoUIDrawContext context,
		int mouseX,
		int mouseY,
		float partialTicks,
		float delta
	) {
		Ui.roundRect(
			context,
			x,
			y,
			width,
			height,
			focused ? Ui.ACCENT : Ui.FIELD_BORDER,
			3
		);
		Ui.roundRect(
			context,
			x + 1,
			y + 1,
			width - 2,
			height - 2,
			Ui.FIELD_BG,
			3
		);

		Font font = Minecraft.getInstance().font;
		int innerX = x + 6;
		int innerY = y + 6;
		int maxWidth = width - 12;
		int lineHeight = font.lineHeight + 1;

		String current = text.toString();
		var lines = font.split(Component.literal(current), maxWidth);
		int limit = Math.max(1, (height - 12) / lineHeight);

		if (lines.isEmpty()) {
			if (current.isEmpty() && !hint.isBlank()) {
				context.drawString(font, hint, innerX, innerY, Ui.MUTED, false);
			}
			drawCaret(context, innerX, innerY);
			return;
		}

		int start = Math.max(0, lines.size() - limit);
		for (int i = start; i < lines.size(); i++) {
			context.drawString(
				font,
				lines.get(i),
				innerX,
				innerY + (i - start) * lineHeight,
				Ui.TEXT,
				false
			);
		}
		int lastWidth = font.width(lines.get(lines.size() - 1));
		int cursorY = innerY + (lines.size() - 1 - start) * lineHeight;
		drawCaret(context, innerX + lastWidth + 1, cursorY);
	}

	private void drawCaret(OwoUIDrawContext context, int caretX, int lineY) {
		if (!focused && (System.currentTimeMillis() / 600) % 2 != 0) {
			return;
		}
		int color = focused ? Ui.ACCENT : Ui.DIM;
		context.fill(
			caretX,
			lineY - 1,
			caretX + 1,
			lineY + Minecraft.getInstance().font.lineHeight,
			color
		);
	}
}

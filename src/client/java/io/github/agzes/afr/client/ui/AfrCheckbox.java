// SPDX-License-Identifier: GPL-3.0-only
package io.github.agzes.afr.client.ui;

import io.wispforest.owo.ui.base.BaseComponent;
import io.wispforest.owo.ui.core.OwoUIDrawContext;
import io.wispforest.owo.ui.core.Sizing;
import java.util.function.Consumer;
import net.minecraft.client.Minecraft;

public class AfrCheckbox extends BaseComponent {

	private boolean checked;
	private Consumer<Boolean> onChanged;
	private boolean enabled = true;

	public AfrCheckbox(boolean checked) {
		this.checked = checked;
	}

	public AfrCheckbox onChanged(Consumer<Boolean> onChanged) {
		this.onChanged = onChanged;
		return this;
	}

	@Override
	protected int determineHorizontalContentSize(Sizing sizing) {
		return 15;
	}

	@Override
	protected int determineVerticalContentSize(Sizing sizing) {
		return 15;
	}

	@Override
	public void draw(
		OwoUIDrawContext context,
		int mouseX,
		int mouseY,
		float partialTicks,
		float delta
	) {
		int bg = checked ? Ui.ACCENT : hovered ? Ui.CARD_HOVER : Ui.FIELD_BG;
		int border = checked
			? Ui.ACCENT
			: hovered
				? Ui.SECTION
				: Ui.FIELD_BORDER;
		Ui.roundRect(context, x, y, width, height, border, 3);
		Ui.roundRect(context, x + 1, y + 1, width - 2, height - 2, bg, 3);
		if (checked) {
			var font = Minecraft.getInstance().font;
			context.drawString(
				font,
				"✔",
				x + (width - font.width("✔")) / 2,
				y + (height - font.lineHeight) / 2 + 1,
				Ui.ACCENT_TEXT,
				false
			);
		}
	}

	@Override
	public boolean onMouseDown(double mouseX, double mouseY, int button) {
		if (button == 0 && enabled) {
			Ui.click();
			checked = !checked;
			if (onChanged != null) {
				onChanged.accept(checked);
			}
			return true;
		}
		return super.onMouseDown(mouseX, mouseY, button);
	}
}

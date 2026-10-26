// SPDX-License-Identifier: GPL-3.0-only
package io.github.agzes.afr.client.ui;

import io.wispforest.owo.ui.component.Components;
import io.wispforest.owo.ui.component.TextBoxComponent;
import io.wispforest.owo.ui.container.Containers;
import io.wispforest.owo.ui.container.FlowLayout;
import io.wispforest.owo.ui.core.Insets;
import io.wispforest.owo.ui.core.Sizing;
import io.wispforest.owo.ui.core.VerticalAlignment;
import io.wispforest.owo.ui.util.FocusHandler;
import java.util.function.Consumer;
import net.minecraft.network.chat.Component;

public final class AfrInput {

	private final FlowLayout root;
	private final TextBoxComponent box;
	private boolean focused;

	private AfrInput(String value, Sizing width, Sizing boxWidth) {
		root = Containers.horizontalFlow(width, Sizing.fixed(18));
		root.padding(Insets.of(3, 5, 3, 5));
		root.verticalAlignment(VerticalAlignment.CENTER);

		box = Components.textBox(boxWidth);
		box.setBordered(false);
		box.setMaxLength(255);
		box.text(value);
		box.focusGained().subscribe(source -> setFocused(true));
		box.focusLost().subscribe(() -> setFocused(false));
		root.child(box);

		applySurface();
	}

	private void setFocused(boolean value) {
		if (focused == value) {
			return;
		}
		focused = value;
		applySurface();
	}

	private void applySurface() {
		int border = focused ? Ui.ACCENT : Ui.FIELD_BORDER;
		root.surface((context, component) -> {
			Ui.roundRect(
				context,
				component.x(),
				component.y(),
				component.width(),
				component.height(),
				border,
				3
			);
			Ui.roundRect(
				context,
				component.x() + 1,
				component.y() + 1,
				component.width() - 2,
				component.height() - 2,
				Ui.FIELD_BG,
				3
			);
		});
	}

	public static AfrInput fill(String value) {
		return new AfrInput(value, Sizing.fill(100), Sizing.fill(100));
	}

	public static AfrInput fixed(String value, int visualWidth) {
		int inner = Math.max(24, visualWidth - 12);
		return new AfrInput(
			value,
			Sizing.fixed(visualWidth),
			Sizing.fixed(inner)
		);
	}

	public static AfrInput expand(String value) {
		return new AfrInput(value, Sizing.expand(100), Sizing.fill(100));
	}

	public FlowLayout component() {
		return root;
	}

	public String value() {
		return box.getValue() == null ? "" : box.getValue();
	}

	public boolean focused() {
		return focused;
	}

	public void focus() {
		FocusHandler handler = box.focusHandler();
		if (handler != null) {
			handler.focus(
				box,
				io.wispforest.owo.ui.core.Component.FocusSource.MOUSE_CLICK
			);
		}
	}

	public void setValue(String value) {
		box.text(value);
	}

	public void onChanged(Consumer<String> listener) {
		box.onChanged().subscribe(listener::accept);
	}

	public AfrInput hint(String text) {
		box.setHint(Component.literal(text));
		return this;
	}
}

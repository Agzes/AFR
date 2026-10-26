// SPDX-License-Identifier: GPL-3.0-only
package io.github.agzes.afr.client.ui;

import io.github.agzes.afr.client.i18n.Lang;
import io.wispforest.owo.ui.base.BaseOwoScreen;
import io.wispforest.owo.ui.component.Components;
import io.wispforest.owo.ui.container.Containers;
import io.wispforest.owo.ui.container.FlowLayout;
import io.wispforest.owo.ui.core.Color;
import io.wispforest.owo.ui.core.HorizontalAlignment;
import io.wispforest.owo.ui.core.Insets;
import io.wispforest.owo.ui.core.OwoUIAdapter;
import io.wispforest.owo.ui.core.Sizing;
import io.wispforest.owo.ui.core.Surface;
import io.wispforest.owo.ui.core.VerticalAlignment;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;

public class ConfirmScreen extends BaseOwoScreen<FlowLayout> {

	private final String title;
	private final String message;
	private final String confirmLabel;
	private final Runnable onConfirm;
	private final Runnable onCancel;
	private final String extraLabel;
	private final Runnable onExtra;

	public ConfirmScreen(
		String title,
		String message,
		String confirmLabel,
		Runnable onConfirm,
		Runnable onCancel
	) {
		this(title, message, confirmLabel, onConfirm, onCancel, null, null);
	}

	public ConfirmScreen(
		String title,
		String message,
		String confirmLabel,
		Runnable onConfirm,
		Runnable onCancel,
		String extraLabel,
		Runnable onExtra
	) {
		super(Component.literal("AFR"));
		this.title = title;
		this.message = message;
		this.confirmLabel = confirmLabel;
		this.onConfirm = onConfirm;
		this.onCancel = onCancel;
		this.extraLabel = extraLabel;
		this.onExtra = onExtra;
	}

	@Override
	protected @NotNull OwoUIAdapter<FlowLayout> createAdapter() {
		return OwoUIAdapter.create(this, Containers::verticalFlow);
	}

	@Override
	protected void build(FlowLayout root) {
		root.surface(Surface.flat(Ui.BACKDROP));
		root.horizontalAlignment(HorizontalAlignment.CENTER);
		root.verticalAlignment(VerticalAlignment.CENTER);

		int width = Math.max(300, Math.min(this.width - 16, 400));
		FlowLayout window = Containers.verticalFlow(
			Sizing.fixed(width),
			Sizing.fixed(140)
		);
		window.surface(Ui.window());
		window.gap(0);

		FlowLayout header = Containers.horizontalFlow(
			Sizing.fill(100),
			Sizing.content()
		);
		header.surface(Ui.header());
		header.padding(Insets.of(5));
		header.horizontalAlignment(HorizontalAlignment.CENTER);
		header.child(
			Components.label(Component.literal("⚠  " + title)).color(
				Color.ofArgb(Ui.WARN)
			)
		);
		window.child(header);

		FlowLayout content = Containers.verticalFlow(
			Sizing.fill(100),
			Sizing.expand(100)
		);
		content.padding(Insets.of(10));
		content.child(
			Components.label(Component.literal(message))
				.color(Color.ofArgb(Ui.TEXT))
				.maxWidth(width - 24)
		);
		window.child(content);

		FlowLayout footer = Containers.horizontalFlow(
			Sizing.fill(100),
			Sizing.content()
		);
		footer.surface(Ui.footer());
		footer.padding(Insets.of(5));
		footer.gap(6);
		footer.verticalAlignment(VerticalAlignment.CENTER);
		footer.child(Ui.grow());
		if (extraLabel != null && onExtra != null) {
			footer.child(
				new AfrButton(
					Component.literal(extraLabel),
					AfrButton.Variant.PRIMARY,
					this::extra
				)
			);
		}
		footer.child(
			new AfrButton(
				Component.literal(Lang.t("btn.cancel")),
				AfrButton.Variant.SECONDARY,
				this::cancel
			)
		);
		footer.child(
			new AfrButton(
				Component.literal(confirmLabel),
				AfrButton.Variant.DANGER,
				this::confirm
			)
		);
		window.child(footer);

		root.child(window);
	}

	private void confirm() {
		if (onConfirm != null) {
			onConfirm.run();
		}
	}

	private void extra() {
		if (onExtra != null) {
			onExtra.run();
		}
	}

	private void cancel() {
		if (onCancel != null) {
			onCancel.run();
			return;
		}
		Minecraft.getInstance().setScreen(null);
	}

	@Override
	public void onClose() {
		cancel();
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}
}

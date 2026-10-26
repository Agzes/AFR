// SPDX-License-Identifier: GPL-3.0-only
package io.github.agzes.afr.client.ui;

import io.github.agzes.afr.client.AfrRuntime;
import io.github.agzes.afr.client.i18n.Lang;
import io.github.agzes.afr.client.model.ActionRef;
import io.wispforest.owo.ui.base.BaseOwoScreen;
import io.wispforest.owo.ui.container.Containers;
import io.wispforest.owo.ui.container.FlowLayout;
import io.wispforest.owo.ui.core.HorizontalAlignment;
import io.wispforest.owo.ui.core.Insets;
import io.wispforest.owo.ui.core.OwoUIAdapter;
import io.wispforest.owo.ui.core.Sizing;
import io.wispforest.owo.ui.core.Surface;
import io.wispforest.owo.ui.core.VerticalAlignment;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;
import org.lwjgl.glfw.GLFW;

public class RadialMenuScreen extends BaseOwoScreen<FlowLayout> {

	private final AfrRuntime runtime;
	private final List<ActionRef> all;
	private final String title;
	private final String windowId;

	private RadialWheel wheel;

	public RadialMenuScreen(
		AfrRuntime runtime,
		List<ActionRef> actions,
		String title,
		String windowId
	) {
		super(Component.literal("AFR"));
		this.runtime = runtime;
		this.all = new ArrayList<>(actions);
		this.title = title;
		this.windowId = windowId;
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

		int width = Math.max(300, Math.min(this.width - 20, 470));
		int height = Math.max(210, Math.min(this.height - 20, 330));
		FlowLayout window = Containers.verticalFlow(
			Sizing.fixed(width),
			Sizing.fixed(height)
		);
		window.surface(Ui.menuWindow());
		window.gap(0);
		window.child(MenuChrome.header(title));

		wheel = new RadialWheel(
			runtime,
			all,
			ref -> {
				minecraft.setScreen(null);
				runtime.runAction(ref.id());
			},
			() -> MenuChrome.switchStyle("list", windowId)
		);
		window.child(wheel);

		FlowLayout footer = Containers.horizontalFlow(
			Sizing.fill(100),
			Sizing.content()
		);
		footer.surface(Ui.footer());
		footer.padding(Insets.of(5));
		footer.gap(6);
		footer.verticalAlignment(VerticalAlignment.CENTER);
		footer.child(MenuChrome.styleSwitch("radial", windowId));
		footer.child(MenuChrome.allActionsButton(runtime));
		footer.child(Ui.grow());
		footer.child(
			new AfrButton(
				Component.literal(Icons.DELETE),
				AfrButton.Variant.ICON,
				this::onClose
			).tip(Lang.t("btn.close"))
		);
		window.child(footer);

		root.child(MenuChrome.topBar(runtime, this, windowId));
		FlowLayout center = Containers.verticalFlow(
			Sizing.fill(100),
			Sizing.expand(100)
		);
		center.horizontalAlignment(HorizontalAlignment.CENTER);
		center.verticalAlignment(VerticalAlignment.CENTER);
		center.child(window);
		root.child(center);
	}

	@Override
	public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
		if (
			keyCode >= GLFW.GLFW_KEY_1 &&
			keyCode <= GLFW.GLFW_KEY_9 &&
			wheel != null
		) {
			int index = keyCode - GLFW.GLFW_KEY_1;
			List<ActionRef> shown = wheel.shown();
			if (index < shown.size()) {
				ActionRef ref = shown.get(index);
				minecraft.setScreen(null);
				runtime.runAction(ref.id());
				return true;
			}
		}
		if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
			onClose();
			return true;
		}
		return super.keyPressed(keyCode, scanCode, modifiers);
	}

	@Override
	public void onClose() {
		minecraft.setScreen(null);
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}
}

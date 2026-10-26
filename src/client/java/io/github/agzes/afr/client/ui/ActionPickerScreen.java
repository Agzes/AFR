// SPDX-License-Identifier: GPL-3.0-only
package io.github.agzes.afr.client.ui;

import io.github.agzes.afr.client.AfrRuntime;
import io.github.agzes.afr.client.config.ConfigManager;
import io.github.agzes.afr.client.i18n.Lang;
import io.github.agzes.afr.client.model.ActionRef;
import io.github.agzes.afr.client.model.QuickActionWindow;
import io.github.agzes.afr.client.model.RpAction;
import io.github.agzes.afr.client.model.RpProfile;
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
import java.util.List;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;

public class ActionPickerScreen extends BaseOwoScreen<FlowLayout> {

	private final Screen parent;
	private final ConfigManager config;
	private final RpProfile profile;
	private final QuickActionWindow window;
	private final int index;
	private final String selectedAction;

	public ActionPickerScreen(
		Screen parent,
		ConfigManager config,
		RpProfile profile,
		QuickActionWindow window,
		int index,
		String selectedAction
	) {
		super(Component.literal("AFR"));
		this.parent = parent;
		this.config = config;
		this.profile = profile;
		this.window = window;
		this.index = index;
		this.selectedAction = selectedAction;
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

		int width = Math.max(280, Math.min(this.width - 16, 420));
		int height = Math.max(180, Math.min(this.height - 16, 280));
		FlowLayout win = Containers.verticalFlow(
			Sizing.fixed(width),
			Sizing.fixed(height)
		);
		win.surface(Ui.window());
		win.gap(0);

		FlowLayout header = Containers.horizontalFlow(
			Sizing.fill(100),
			Sizing.content()
		);
		header.surface(Ui.header());
		header.padding(Insets.of(5));
		header.horizontalAlignment(HorizontalAlignment.CENTER);
		header.child(
			Components.label(
				Component.literal(
					Icons.ACTIONS +
						"  " +
						Lang.t("lbl.cell") +
						" " +
						(index + 1)
				)
			).color(Color.ofArgb(Ui.SECTION))
		);
		win.child(header);

		FlowLayout list = Containers.verticalFlow(
			Sizing.fill(100),
			Sizing.content()
		);
		list.gap(1);
		list.padding(Insets.of(4));

		AfrListItem empty = new AfrListItem(
			Icons.DELETE,
			Lang.t("lbl.empty_slot"),
			""
		);
		empty.horizontalSizing(Sizing.fill(100));
		empty.onPress(() -> assign(""));
		list.child(empty);

		List<ActionRef> actions = profile.actionRefs();
		actions.sort((a, b) ->
			a.displayName().compareToIgnoreCase(b.displayName())
		);
		for (ActionRef ref : actions) {
			RpAction action = ref.action();
			AfrListItem item = new AfrListItem(
				action.favorite ? Icons.STAR : "",
				action.displayName(),
				""
			);
			item.horizontalSizing(Sizing.fill(100));
			item.active(ref.id().equals(window.tiles.get(index)));
			item.onPress(() -> assign(ref.id()));
			list.child(item);
		}
		win.child(
			Containers.verticalScroll(
				Sizing.fill(100),
				Sizing.expand(100),
				list
			)
		);

		FlowLayout footer = Containers.horizontalFlow(
			Sizing.fill(100),
			Sizing.content()
		);
		footer.surface(Ui.footer());
		footer.padding(Insets.of(5));
		footer.verticalAlignment(VerticalAlignment.CENTER);
		footer.child(Ui.grow());
		footer.child(
			new AfrButton(
				Component.literal(Lang.t("btn.close")),
				AfrButton.Variant.SECONDARY,
				this::back
			)
		);
		win.child(footer);

		root.child(win);
	}

	private void assign(String id) {
		while (window.tiles.size() <= index) {
			window.tiles.add("");
		}
		window.tiles.set(index, id);
		AfrRuntime runtime = AfrRuntime.get();
		if (runtime != null) {
			runtime.editorDirty = true;
		}
		back();
	}

	private void back() {
		ConfigScreen.returnFrom(parent, 2, selectedAction, window.id);
	}

	@Override
	public void onClose() {
		back();
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}
}

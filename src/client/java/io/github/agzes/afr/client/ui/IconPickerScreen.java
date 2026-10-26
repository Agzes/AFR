// SPDX-License-Identifier: GPL-3.0-only
package io.github.agzes.afr.client.ui;

import io.github.agzes.afr.client.AfrRuntime;
import io.github.agzes.afr.client.config.ConfigManager;
import io.github.agzes.afr.client.i18n.Lang;
import io.github.agzes.afr.client.model.RpAction;
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
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;

public class IconPickerScreen extends BaseOwoScreen<FlowLayout> {

	private final Screen parent;
	private final RpAction action;
	private final String selectedAction;
	private final ConfigManager config;
	private final List<String> items = new ArrayList<>();

	private FlowLayout gridHost;
	private String filter = "";
	private int windowWidth = 520;

	public IconPickerScreen(
		Screen parent,
		RpAction action,
		String selectedAction,
		ConfigManager config
	) {
		super(Component.literal("AFR"));
		this.parent = parent;
		this.action = action;
		this.selectedAction = selectedAction;
		this.config = config;
		for (var item : BuiltInRegistries.ITEM) {
			var key = BuiltInRegistries.ITEM.getKey(item);
			if (key != null && !"minecraft:air".equals(key.toString())) {
				items.add(key.toString());
			}
		}
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

		int width = Math.max(360, Math.min(this.width - 16, 520));
		int height = Math.max(200, Math.min(this.height - 16, 290));
		windowWidth = width;
		FlowLayout window = Containers.verticalFlow(
			Sizing.fixed(width),
			Sizing.fixed(height)
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
			Components.label(
				Component.literal(Icons.EDIT + "  " + Lang.t("sec.icon_picker"))
			).color(Color.ofArgb(Ui.SECTION))
		);
		window.child(header);

		FlowLayout body = Containers.verticalFlow(
			Sizing.fill(100),
			Sizing.expand(100)
		);
		body.gap(Ui.GAP);
		body.padding(Insets.of(Ui.PAD));
		AfrInput search = AfrInput.fill("");
		search.onChanged(value -> {
			filter = value == null ? "" : value.strip().toLowerCase();
			populateGrid();
		});
		body.child(search.component());

		gridHost = Containers.verticalFlow(Sizing.fill(100), Sizing.content());
		gridHost.gap(2);
		gridHost.horizontalAlignment(HorizontalAlignment.CENTER);
		body.child(
			Containers.verticalScroll(
				Sizing.fill(100),
				Sizing.expand(100),
				gridHost
			)
		);
		window.child(body);

		FlowLayout footer = Containers.horizontalFlow(
			Sizing.fill(100),
			Sizing.content()
		);
		footer.surface(Ui.footer());
		footer.padding(Insets.of(5));
		footer.verticalAlignment(VerticalAlignment.CENTER);
		footer.child(Ui.dim(items.size() + " " + Lang.t("lbl.items_count")));
		footer.child(Ui.grow());
		footer.child(
			new AfrButton(
				Component.literal(Lang.t("btn.close")),
				AfrButton.Variant.SECONDARY,
				this::onClose
			)
		);
		window.child(footer);

		root.child(window);
		populateGrid();
	}

	private void populateGrid() {
		if (gridHost == null) {
			return;
		}
		gridHost.clearChildren();
		int columns = Math.max(1, (windowWidth - 24) / 30);
		FlowLayout row = null;
		int count = 0;
		for (String id : items) {
			if (!filter.isBlank() && !id.contains(filter)) {
				continue;
			}
			if (count % columns == 0) {
				row = Containers.horizontalFlow(
					Sizing.content(),
					Sizing.content()
				);
				row.gap(2);
				gridHost.child(row);
			}
			count++;
			AfrTile tile = new AfrTile(Items.fromId(id), "");
			tile.tooltip(Component.literal(id));
			tile.onPress(() -> select(id));
			row.child(tile);
		}
		gridHost.child(
			Containers.horizontalFlow(Sizing.fill(100), Sizing.fixed(10))
		);
	}

	private void select(String id) {
		action.icon = id;
		AfrRuntime runtime = AfrRuntime.get();
		if (runtime != null) {
			runtime.editorDirty = true;
		}
		ConfigScreen.returnFrom(parent, 3, selectedAction, null);
	}

	@Override
	public void onClose() {
		ConfigScreen.returnFrom(parent, 3, selectedAction, null);
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}
}

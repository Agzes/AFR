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
import java.util.List;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

public class TileScreen extends BaseOwoScreen<FlowLayout> {

	private final AfrRuntime runtime;
	private final String title;
	private final List<ActionRef> slots;
	private final int columns;
	private final String windowId;

	public TileScreen(
		AfrRuntime runtime,
		String title,
		List<ActionRef> slots,
		int columns,
		String windowId
	) {
		super(Component.literal(title));
		this.runtime = runtime;
		this.title = title;
		this.slots = slots;
		this.columns = Math.max(1, columns);
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

		int rows = Math.max(1, (slots.size() + columns - 1) / columns);
		int width = Math.max(160, columns * 30 + 14);
		int height = Math.max(120, rows * 31 + 62);
		FlowLayout win = Containers.verticalFlow(
			Sizing.fixed(width),
			Sizing.fixed(height)
		);
		win.surface(Ui.menuWindow());
		win.gap(0);

		FlowLayout header = MenuChrome.header(title);
		win.child(header);

		FlowLayout content = Containers.verticalFlow(
			Sizing.fill(100),
			Sizing.content()
		);
		content.gap(3);
		content.padding(Insets.of(6));
		content.horizontalAlignment(HorizontalAlignment.CENTER);
		FlowLayout row = null;
		for (int i = 0; i < slots.size(); i++) {
			if (i % columns == 0) {
				row = Containers.horizontalFlow(
					Sizing.content(),
					Sizing.content()
				);
				row.gap(2);
				content.child(row);
			}
			ActionRef ref = slots.get(i);
			AfrTile tile = new AfrTile(
				ref != null ? Items.fromId(ref.action().icon) : ItemStack.EMPTY,
				ref != null ? ref.displayName() : ""
			);
			if (ref != null) {
				tile.favorite(ref.action().favorite);
				tile.tooltip(Component.literal(ref.displayName()));
				tile.onPress(() -> {
					minecraft.setScreen(null);
					runtime.runAction(ref.id());
				});
			}
			row.child(tile);
		}
		win.child(
			Containers.verticalScroll(
				Sizing.fill(100),
				Sizing.expand(100),
				content
			)
		);

		FlowLayout footer = Containers.horizontalFlow(
			Sizing.fill(100),
			Sizing.content()
		);
		footer.surface(Ui.footer());
		footer.padding(Insets.of(5));
		footer.gap(6);
		footer.verticalAlignment(VerticalAlignment.CENTER);
		footer.child(MenuChrome.styleSwitch("tiles", windowId));
		footer.child(MenuChrome.allActionsButton(runtime));
		footer.child(Ui.grow());
		footer.child(
			new AfrButton(
				Component.literal(Icons.DELETE),
				AfrButton.Variant.ICON,
				this::onClose
			).tip(Lang.t("btn.close"))
		);
		win.child(footer);

		root.child(MenuChrome.topBar(runtime, this, windowId));
		FlowLayout center = Containers.verticalFlow(
			Sizing.fill(100),
			Sizing.expand(100)
		);
		center.horizontalAlignment(HorizontalAlignment.CENTER);
		center.verticalAlignment(VerticalAlignment.CENTER);
		center.child(win);
		root.child(center);
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

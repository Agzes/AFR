// SPDX-License-Identifier: GPL-3.0-only
package io.github.agzes.afr.client.ui;

import io.github.agzes.afr.client.i18n.Lang;
import io.wispforest.owo.ui.base.BaseComponent;
import io.wispforest.owo.ui.base.BaseOwoScreen;
import io.wispforest.owo.ui.component.Components;
import io.wispforest.owo.ui.container.Containers;
import io.wispforest.owo.ui.container.FlowLayout;
import io.wispforest.owo.ui.core.Color;
import io.wispforest.owo.ui.core.HorizontalAlignment;
import io.wispforest.owo.ui.core.Insets;
import io.wispforest.owo.ui.core.OwoUIAdapter;
import io.wispforest.owo.ui.core.OwoUIDrawContext;
import io.wispforest.owo.ui.core.Sizing;
import io.wispforest.owo.ui.core.Surface;
import io.wispforest.owo.ui.core.VerticalAlignment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;

public class StylePreviewScreen extends BaseOwoScreen<FlowLayout> {

	private static String[] samples() {
		return Lang.t("style.samples").split("\\|");
	}

	private static final String[] SAMPLE_ICONS = {
		"minecraft:spyglass",
		"minecraft:potion",
		"minecraft:paper",
		"minecraft:map",
		"minecraft:potion",
		"minecraft:redstone",
		"minecraft:written_book",
		"minecraft:paper",
	};

	private final Screen parent;
	private final String style;

	public StylePreviewScreen(Screen parent, String style) {
		super(Component.literal("AFR"));
		this.parent = parent;
		this.style = style == null ? "list" : style;
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

		int width = Math.max(320, Math.min(this.width - 16, 520));
		int height = Math.max(220, Math.min(this.height - 16, 320));
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
				Component.literal(
					Lang.t("btn.preview") +
						" • " +
						ConfigScreen.displayValue(style)
				)
			).color(Color.ofArgb(Ui.SECTION))
		);
		window.child(header);

		FlowLayout body = Containers.verticalFlow(
			Sizing.fill(100),
			Sizing.expand(100)
		);
		body.padding(Insets.of(Ui.PAD));
		body.child(preview());
		window.child(body);

		FlowLayout footer = Containers.horizontalFlow(
			Sizing.fill(100),
			Sizing.content()
		);
		footer.surface(Ui.footer());
		footer.padding(Insets.of(5));
		footer.gap(4);
		footer.verticalAlignment(VerticalAlignment.CENTER);
		for (String option : new String[] { "list", "radial", "tiles" }) {
			AfrButton button = new AfrButton(
				Component.literal(ConfigScreen.displayValue(option)),
				AfrButton.Variant.SECONDARY,
				() ->
					minecraft.setScreen(new StylePreviewScreen(parent, option))
			);
			button.active(option.equalsIgnoreCase(style));
			footer.child(button);
		}
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
	}

	@Override
	public void onClose() {
		ConfigScreen.returnFrom(parent, 1, null, null);
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}

	private FlowLayout preview() {
		return switch (style.toLowerCase()) {
			case "tiles" -> tilesPreview();
			case "radial" -> {
				FlowLayout host = Containers.verticalFlow(
					Sizing.fill(100),
					Sizing.expand(100)
				);
				host.horizontalAlignment(HorizontalAlignment.CENTER);
				host.verticalAlignment(VerticalAlignment.CENTER);
				host.child(new RadialPreview());
				yield host;
			}
			default -> listPreview();
		};
	}

	private FlowLayout listPreview() {
		FlowLayout card = Ui.card(Icons.STAR + "  " + Lang.t("sec.favorites"));
		for (int i = 0; i < samples().length; i++) {
			AfrListItem item = new AfrListItem("", samples()[i], "F" + (i + 1));
			item.stack(Items.fromId(SAMPLE_ICONS[i]));
			item.horizontalSizing(Sizing.fill(100));
			card.child(item);
		}
		return card;
	}

	private FlowLayout tilesPreview() {
		FlowLayout grid = Containers.verticalFlow(
			Sizing.fill(100),
			Sizing.content()
		);
		grid.gap(3);
		grid.horizontalAlignment(HorizontalAlignment.CENTER);
		FlowLayout row = null;
		for (int i = 0; i < samples().length; i++) {
			if (i % 4 == 0) {
				row = Containers.horizontalFlow(
					Sizing.content(),
					Sizing.content()
				);
				row.gap(3);
				grid.child(row);
			}
			AfrTile tile = new AfrTile(
				Items.fromId(SAMPLE_ICONS[i]),
				samples()[i]
			);
			tile.favorite(i < 2);
			tile.tooltip(Component.literal(samples()[i]));
			row.child(tile);
		}
		return grid;
	}

	private static final class RadialPreview extends BaseComponent {

		@Override
		protected int determineHorizontalContentSize(Sizing sizing) {
			return 220;
		}

		@Override
		protected int determineVerticalContentSize(Sizing sizing) {
			return 170;
		}

		@Override
		public void draw(
			OwoUIDrawContext context,
			int mouseX,
			int mouseY,
			float partialTicks,
			float delta
		) {
			int cx = x + width / 2;
			int cy = y + height / 2;
			int radius = Math.max(40, Math.min(width, height) / 2 - 22);
			int n = samples().length;
			Ui.roundRect(context, cx - 44, cy - 14, 88, 28, Ui.WINDOW, 5);
			context.drawCenteredString(
				Minecraft.getInstance().font,
				n + " " + Lang.t("lbl.actions_count"),
				cx,
				cy - 4,
				Ui.TEXT
			);
			for (int i = 0; i < n; i++) {
				double angle = -Math.PI / 2 + (2 * Math.PI * i) / n;
				int bx = cx + (int) (Math.cos(angle) * radius);
				int by = cy + (int) (Math.sin(angle) * radius);
				int w = 74;
				int h = 16;
				Ui.roundRect(
					context,
					bx - w / 2,
					by - h / 2,
					w,
					h,
					i < 2 ? Ui.ACCENT_DIM : Ui.CARD,
					4
				);
				if (i < 2) {
					context.drawString(
						Minecraft.getInstance().font,
						Icons.STAR,
						bx - w / 2 + 4,
						by - 4,
						Ui.WARN,
						false
					);
				}
				context.drawCenteredString(
					Minecraft.getInstance().font,
					samples()[i],
					bx,
					by - 4,
					Ui.TEXT
				);
			}
		}
	}
}

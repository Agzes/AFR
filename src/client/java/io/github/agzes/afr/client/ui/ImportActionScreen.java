// SPDX-License-Identifier: GPL-3.0-only
package io.github.agzes.afr.client.ui;

import com.google.gson.JsonParser;
import io.github.agzes.afr.AFR;
import io.github.agzes.afr.client.AfrRuntime;
import io.github.agzes.afr.client.config.ConfigCodec;
import io.github.agzes.afr.client.hud.NotificationHud;
import io.github.agzes.afr.client.i18n.Lang;
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
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;

public class ImportActionScreen extends BaseOwoScreen<FlowLayout> {

	private final Screen parent;
	private final RpProfile profile;
	private String text = "";

	public ImportActionScreen(Screen parent, RpProfile profile) {
		super(Component.literal("AFR"));
		this.parent = parent;
		this.profile = profile;
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

		int width = Math.max(360, Math.min(this.width - 16, 560));
		int height = Math.max(240, Math.min(this.height - 16, 340));
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
					Icons.CLOUD + "  " + Lang.t("import.title_action")
				)
			).color(Color.ofArgb(Ui.SECTION))
		);
		window.child(header);

		FlowLayout body = Containers.verticalFlow(
			Sizing.fill(100),
			Sizing.expand(100)
		);
		body.gap(Ui.GAP);
		body.padding(Insets.of(Ui.PAD));
		body.child(Ui.wrap(Lang.t("import.action_hint"), width - 24));
		AfrTextArea area = new AfrTextArea()
			.onChanged(value -> text = value)
			.hint(Lang.t("import.json_hint"));
		area.horizontalSizing(Sizing.fill(100));
		area.verticalSizing(Sizing.expand(100));
		body.child(area);
		window.child(body);

		FlowLayout footer = Containers.horizontalFlow(
			Sizing.fill(100),
			Sizing.content()
		);
		footer.surface(Ui.footer());
		footer.padding(Insets.of(5));
		footer.gap(6);
		footer.verticalAlignment(VerticalAlignment.CENTER);
		footer.child(
			new AfrButton(
				Component.literal(Lang.t("btn.from_clipboard")),
				AfrButton.Variant.SECONDARY,
				() -> {
					area.text(minecraft.keyboardHandler.getClipboard());
					text = area.value();
				}
			)
		);
		footer.child(Ui.grow());
		footer.child(
			new AfrButton(
				Component.literal(Lang.t("btn.close")),
				AfrButton.Variant.SECONDARY,
				this::onClose
			)
		);
		footer.child(
			new AfrButton(
				Component.literal(Icons.PLUS + " " + Lang.t("btn.import")),
				AfrButton.Variant.PRIMARY,
				this::importNow
			)
		);
		window.child(footer);

		root.child(window);
	}

	private void importNow() {
		String raw = text == null ? "" : text.strip();
		if (raw.isEmpty()) {
			NotificationHud.notify(
				NotificationHud.Type.WARNING,
				"AFR",
				Lang.t("msg.import_empty")
			);
			return;
		}
		try {
			JsonParser.parseString(raw);
			RpAction imported = ConfigCodec.GSON.fromJson(raw, RpAction.class);
			if (imported == null) {
				throw new IllegalArgumentException("null");
			}
			if (imported.name == null || imported.name.isBlank()) {
				imported.name =
					"action_" + (System.currentTimeMillis() % 100000);
			}
			String base = imported.name;
			String id = base;
			int n = 2;
			while (profile.rp_actions.containsKey(id)) {
				id = base + "-" + n++;
			}
			profile.rp_actions.put(id, imported);
			AfrRuntime runtime = AfrRuntime.get();
			if (runtime != null) {
				runtime.editorDirty = true;
			}
			AFR.LOGGER.info("AFR: imported action '{}'", id);
			NotificationHud.notify(
				NotificationHud.Type.SUCCESS,
				"AFR",
				Lang.t("msg.imported_action") + ": " + imported.displayName()
			);
			ConfigScreen.returnFrom(parent, 3, id, null);
		} catch (Exception e) {
			AFR.LOGGER.warn("AFR: action import failed", e);
			NotificationHud.notify(
				NotificationHud.Type.ERROR,
				"AFR",
				Lang.t("msg.import_error")
			);
		}
	}

	@Override
	public void onClose() {
		ConfigScreen.returnFrom(parent, 3, null, null);
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}
}

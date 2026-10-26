// SPDX-License-Identifier: GPL-3.0-only
package io.github.agzes.afr.client.ui;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import io.github.agzes.afr.AFR;
import io.github.agzes.afr.client.AfrRuntime;
import io.github.agzes.afr.client.hud.NotificationHud;
import io.github.agzes.afr.client.i18n.Lang;
import io.github.agzes.afr.client.model.RpProfile;
import io.github.agzes.afr.client.util.QuickMenuIo;
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
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;

public class QuickMenuImportScreen extends BaseOwoScreen<FlowLayout> {

	private final Screen parent;
	private final RpProfile profile;
	private String text = "";

	public QuickMenuImportScreen(Screen parent, RpProfile profile) {
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
				Component.literal(Icons.CLOUD + "  " + Lang.t("sec.qm_import"))
			).color(Color.ofArgb(Ui.SECTION))
		);
		window.child(header);

		FlowLayout body = Containers.verticalFlow(
			Sizing.fill(100),
			Sizing.expand(100)
		);
		body.gap(Ui.GAP);
		body.padding(Insets.of(Ui.PAD));
		body.child(Ui.wrap(Lang.t("qm.paste_hint"), width - 24));
		AfrTextArea area = new AfrTextArea()
			.onChanged(value -> text = value)
			.hint(Lang.t("qm.json_hint"));
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
				Component.literal(Icons.CLOUD + " " + Lang.t("btn.from_file")),
				AfrButton.Variant.SECONDARY,
				this::importFromFile
			).tip(Lang.t("tip.qm_file"))
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

	private void importFromFile() {
		Path path = FabricLoader.getInstance()
			.getConfigDir()
			.resolve("quickmenu_data.json");
		if (!Files.exists(path)) {
			NotificationHud.notify(
				NotificationHud.Type.WARNING,
				"QuickMenu",
				Lang.t("qm.file_not_found") + ": " + path
			);
			return;
		}
		try {
			JsonElement parsed = JsonParser.parseString(
				Files.readString(path, StandardCharsets.UTF_8)
			);
			if (!parsed.isJsonArray()) {
				NotificationHud.notify(
					NotificationHud.Type.ERROR,
					"QuickMenu",
					Lang.t("qm.bad_format")
				);
				return;
			}
			apply(parsed.getAsJsonArray());
		} catch (Exception e) {
			NotificationHud.notify(
				NotificationHud.Type.ERROR,
				"QuickMenu",
				Lang.t("qm.read_error")
			);
		}
	}

	private void importNow() {
		String raw = text == null ? "" : text.strip();
		if (raw.isEmpty()) {
			NotificationHud.notify(
				NotificationHud.Type.WARNING,
				"QuickMenu",
				Lang.t("qm.paste_json")
			);
			return;
		}
		try {
			JsonElement parsed = JsonParser.parseString(raw);
			if (!parsed.isJsonArray()) {
				NotificationHud.notify(
					NotificationHud.Type.ERROR,
					"QuickMenu",
					Lang.t("qm.expect_array")
				);
				return;
			}
			apply(parsed.getAsJsonArray());
		} catch (Exception e) {
			NotificationHud.notify(
				NotificationHud.Type.ERROR,
				"QuickMenu",
				Lang.t("qm.bad_json")
			);
		}
	}

	private void apply(JsonArray array) {
		int added = QuickMenuIo.importInto(profile, array);
		AfrRuntime runtime = AfrRuntime.get();
		if (runtime != null) {
			runtime.editorDirty = true;
		}
		AFR.LOGGER.info("AFR: imported {} action(s) from QuickMenu", added);
		NotificationHud.notify(
			NotificationHud.Type.SUCCESS,
			"QuickMenu",
			Lang.t("qm.imported") + ": " + added
		);
		back();
	}

	private void back() {
		ConfigScreen.returnFrom(parent, 3, null, null);
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

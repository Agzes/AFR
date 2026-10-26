// SPDX-License-Identifier: GPL-3.0-only
package io.github.agzes.afr.client.ui;

import io.github.agzes.afr.client.action.AutoChatParser;
import io.github.agzes.afr.client.config.ConfigManager;
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
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;

public class AutoChatScreen extends BaseOwoScreen<FlowLayout> {

	private final Screen parent;
	private final ConfigManager config;
	private final RpProfile profile;
	private String text = "";

	public AutoChatScreen(
		Screen parent,
		ConfigManager config,
		RpProfile profile
	) {
		super(Component.literal("AFR"));
		this.parent = parent;
		this.config = config;
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

		int width = Math.max(340, Math.min(this.width - 16, 520));
		int height = Math.max(220, Math.min(this.height - 16, 300));
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
				Component.literal(Icons.ACTIONS + "  " + Lang.t("btn.autochat"))
			).color(Color.ofArgb(Ui.SECTION))
		);
		window.child(header);

		FlowLayout body = Containers.verticalFlow(
			Sizing.fill(100),
			Sizing.expand(100)
		);
		body.gap(Ui.GAP);
		body.padding(Insets.of(Ui.PAD));
		body.child(Ui.dim(Lang.t("hint.autochat")));
		AfrTextArea area = new AfrTextArea()
			.onChanged(value -> text = value)
			.hint(Lang.t("hint.autochat_input"));
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
		footer.child(Ui.grow());
		footer.child(
			new AfrButton(
				Component.literal(
					Icons.PLUS + " " + Lang.t("btn.create_action")
				),
				AfrButton.Variant.PRIMARY,
				this::create
			).tip(Lang.t("btn.create_action"))
		);
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

	private void create() {
		List<List<String>> steps = AutoChatParser.parse(text);
		if (steps.isEmpty() || profile == null) {
			NotificationHud.notify(
				NotificationHud.Type.WARNING,
				"AFR",
				Lang.t("msg.enter_text")
			);
			return;
		}
		String id = "autochat_" + (System.currentTimeMillis() % 100000);
		RpAction action = new RpAction();
		action.name = id;
		action.description = "AutoChat";
		action.steps = new ArrayList<>(steps);
		profile.rp_actions.put(id, action);
		config.saveActive();
		ConfigScreen.returnFrom(parent, 3, id, null);
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

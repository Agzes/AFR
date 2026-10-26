// SPDX-License-Identifier: GPL-3.0-only
package io.github.agzes.afr.client.ui;

import io.github.agzes.afr.AFR;
import io.github.agzes.afr.client.config.ConfigManager;
import io.github.agzes.afr.client.hud.NotificationHud;
import io.github.agzes.afr.client.i18n.Lang;
import io.github.agzes.afr.client.model.Parameter;
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
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;

public class ParameterSetupScreen extends BaseOwoScreen<FlowLayout> {

	private final Screen parent;
	private final ConfigManager config;
	private final RpProfile profile;
	private final List<Runnable> savers = new ArrayList<>();

	public ParameterSetupScreen(
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

		int width = Math.max(340, Math.min(this.width - 16, 460));
		int height = Math.max(200, Math.min(this.height - 16, 320));
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
					Icons.EDIT +
						"  " +
						Lang.t("setup.params_title") +
						" • " +
						(profile.meta != null ? profile.meta.label : "")
				)
			).color(Color.ofArgb(Ui.SECTION))
		);
		window.child(header);

		FlowLayout body = Containers.verticalFlow(
			Sizing.fill(100),
			Sizing.content()
		);
		body.gap(Ui.GAP);
		body.padding(Insets.of(Ui.PAD));
		body.child(Ui.dim(Lang.t("setup.params_hint")));
		body.child(Ui.dim(Lang.t("setup.params_hint2")));
		for (Parameter parameter : profile.parameters) {
			body.child(paramRow(parameter));
		}
		window.child(new AfrScroll(Sizing.fill(100), Sizing.expand(100), body));

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
				Component.literal(Lang.t("btn.skip")),
				AfrButton.Variant.SECONDARY,
				this::onClose
			)
		);
		footer.child(Ui.grow());
		footer.child(
			new AfrButton(
				Component.literal(Icons.CHECK + " " + Lang.t("btn.save")),
				AfrButton.Variant.PRIMARY,
				this::save
			)
		);
		window.child(footer);

		root.child(window);
	}

	private FlowLayout paramRow(Parameter parameter) {
		FlowLayout row = Containers.horizontalFlow(
			Sizing.fill(100),
			Sizing.content()
		);
		row.gap(6);
		row.verticalAlignment(VerticalAlignment.CENTER);
		row.child(
			Ui.dim(
				TextFit.fit(
					Minecraft.getInstance().font,
					parameter.label == null ? parameter.id : parameter.label,
					150
				)
			)
		);
		row.child(Ui.grow());
		if (parameter.isCheckbox()) {
			AfrCheckbox checkbox = new AfrCheckbox(parameter.asBool());
			checkbox.onChanged(value -> parameter.value = value);
			row.child(checkbox);
		} else {
			AfrInput input = AfrInput.fixed(parameter.asText(), 150);
			savers.add(() -> parameter.value = input.value());
			row.child(input.component());
		}
		return row;
	}

	private void save() {
		for (Runnable saver : savers) {
			saver.run();
		}
		config.saveProfile(profile);
		AFR.LOGGER.info(
			"AFR: parameters configured for '{}'",
			profile.meta != null ? profile.meta.revName : "?"
		);
		NotificationHud.notify(
			NotificationHud.Type.SUCCESS,
			"AFR",
			Lang.t("msg.params_saved")
		);
		minecraft.setScreen(parent);
	}

	@Override
	public void onClose() {
		minecraft.setScreen(parent);
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}
}

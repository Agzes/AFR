// SPDX-License-Identifier: GPL-3.0-only
package io.github.agzes.afr.client.ui;

import io.github.agzes.afr.AFR;
import io.github.agzes.afr.client.AfrRuntime;
import io.github.agzes.afr.client.config.ConfigCodec;
import io.github.agzes.afr.client.config.ConfigManager;
import io.github.agzes.afr.client.hud.NotificationHud;
import io.github.agzes.afr.client.i18n.Lang;
import io.github.agzes.afr.client.model.Parameter;
import io.github.agzes.afr.client.model.RpAction;
import io.github.agzes.afr.client.model.RpProfile;
import io.github.agzes.afr.client.util.ProfileDiff;
import io.github.agzes.afr.client.util.VersionStore;
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
import java.util.Map;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;

public class DiffScreen extends BaseOwoScreen<FlowLayout> {

	private final Screen parent;
	private final ConfigManager config;
	private final RpProfile current;
	private final RpProfile remote;
	private final List<ProfileDiff.Line> lines;

	public DiffScreen(
		Screen parent,
		ConfigManager config,
		RpProfile current,
		RpProfile remote
	) {
		super(Component.literal("AFR"));
		this.parent = parent;
		this.config = config;
		this.current = current;
		this.remote = remote;
		this.lines = ProfileDiff.compare(current, remote);
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
		int height = Math.max(220, Math.min(this.height - 16, 330));
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
					Icons.CLOUD +
						"  " +
						Lang.t("diff.title") +
						": " +
						remote.meta.label
				)
			).color(Color.ofArgb(Ui.SECTION))
		);
		window.child(header);

		FlowLayout body = Containers.verticalFlow(
			Sizing.fill(100),
			Sizing.content()
		);
		body.gap(2);
		body.padding(Insets.of(Ui.PAD));
		int textWidth = width - Ui.PAD * 2 - 6;
		if (lines.isEmpty()) {
			body.child(Ui.dim(Lang.t("diff.none")));
		} else {
			String section = "";
			for (ProfileDiff.Line line : lines) {
				if (!line.section().equals(section)) {
					section = line.section();
					if (body.children().size() > 0) {
						body.child(
							Containers.horizontalFlow(
								Sizing.fill(100),
								Sizing.fixed(4)
							)
						);
					}
					body.child(Ui.section(null, section));
				}
				body.child(
					Components.label(
						Component.literal(prefix(line.type()) + line.text())
					)
						.color(Color.ofArgb(colorFor(line.type())))
						.maxWidth(textWidth)
				);
			}
		}
		window.child(
			Containers.verticalScroll(
				Sizing.fill(100),
				Sizing.expand(100),
				body
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
		footer.child(
			new AfrButton(
				Component.literal(Lang.t("diff.fork_apply")),
				AfrButton.Variant.SECONDARY,
				() -> apply(true)
			).tip(Lang.t("tip.fork_apply"))
		);
		footer.child(Ui.grow());
		footer.child(
			new AfrButton(
				Component.literal(Lang.t("btn.cancel")),
				AfrButton.Variant.SECONDARY,
				this::onClose
			)
		);
		footer.child(
			new AfrButton(
				Component.literal(Icons.CHECK + " " + Lang.t("diff.apply")),
				AfrButton.Variant.PRIMARY,
				() -> apply(false)
			)
		);
		window.child(footer);

		root.child(window);
	}

	private static String prefix(ProfileDiff.Type type) {
		return switch (type) {
			case ADDED -> "+ ";
			case REMOVED -> "- ";
			case CHANGED -> "~ ";
			case INFO -> "";
		};
	}

	private static int colorFor(ProfileDiff.Type type) {
		return switch (type) {
			case ADDED -> Ui.GOOD;
			case REMOVED -> Ui.BAD;
			case CHANGED -> Ui.WARN;
			case INFO -> Ui.DIM;
		};
	}

	private void apply(boolean forkFirst) {
		if (forkFirst) {
			fork(current);
		}
		for (Parameter parameter : remote.parameters) {
			Parameter existing = current.parameter(parameter.id);
			if (existing != null) {
				parameter.value = existing.value;
			}
		}
		for (Map.Entry<String, RpAction> entry : remote.rp_actions.entrySet()) {
			RpAction old = current.rp_actions.get(entry.getKey());
			if (old != null) {
				entry.getValue().favorite = old.favorite;
			}
		}
		remote.meta.revName = current.meta.revName;
		remote.meta.source = "library";
		remote.meta.modified = false;
		if (remote.meta.remoteId == null || remote.meta.remoteId.isBlank()) {
			remote.meta.remoteId = current.meta.remoteId;
		}
		remote.meta.remoteVersionCode = remote.meta.versionCode;
		VersionStore.archive(current);
		config.putProfile(remote, true);
		AfrRuntime.get().rebuildBinds();
		NotificationHud.notify(
			NotificationHud.Type.SUCCESS,
			"AFR",
			Lang.t("diff.updated") + ": v" + remote.meta.version
		);
		AFR.LOGGER.info(
			"AFR: config '{}' updated to v{} via diff",
			remote.meta.revName,
			remote.meta.version
		);
		minecraft.setScreen(refreshParent());
	}

	private Screen refreshParent() {
		if (parent instanceof ConfigScreen configScreen) {
			configScreen.refresh();
			return configScreen;
		}
		return parent != null ? parent : new ConfigScreen(null, 2, null, null);
	}

	private void fork(RpProfile profile) {
		RpProfile copy = ConfigCodec.GSON.fromJson(
			ConfigCodec.GSON.toJson(profile),
			RpProfile.class
		);
		String base = profile.meta.revName + "-local";
		String revName = base;
		int index = 1;
		while (config.profiles().containsKey(revName)) {
			revName = base + index++;
		}
		copy.meta.revName = revName;
		copy.meta.label =
			profile.meta.label + " (" + Lang.t("diff.local_copy") + ")";
		copy.meta.source = "local";
		copy.meta.remoteId = "";
		copy.meta.remoteVersionCode = 0;
		copy.meta.modified = false;
		config.putProfile(copy, false);
	}

	@Override
	public void onClose() {
		if (parent instanceof ConfigScreen editor) {
			editor.refresh();
		}
		minecraft.setScreen(
			parent != null ? parent : new ConfigScreen(null, 2, null, null)
		);
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}
}

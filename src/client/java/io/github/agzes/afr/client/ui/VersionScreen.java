// SPDX-License-Identifier: GPL-3.0-only
package io.github.agzes.afr.client.ui;

import com.google.gson.JsonArray;
import io.github.agzes.afr.client.AfrRuntime;
import io.github.agzes.afr.client.hud.NotificationHud;
import io.github.agzes.afr.client.i18n.Lang;
import io.github.agzes.afr.client.net.AfrCloud;
import io.github.agzes.afr.client.net.VersionChecker;
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
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;

public class VersionScreen extends BaseOwoScreen<FlowLayout> {

	private final Screen parent;
	private final boolean loading;
	private final boolean error;
	private final String remoteName;
	private final int remoteCode;
	private final List<String> changelog;

	private VersionScreen(
		Screen parent,
		boolean loading,
		boolean error,
		String remoteName,
		int remoteCode,
		List<String> changelog
	) {
		super(Component.literal("AFR"));
		this.parent = parent;
		this.loading = loading;
		this.error = error;
		this.remoteName = remoteName;
		this.remoteCode = remoteCode;
		this.changelog = changelog;
	}

	public static void open(Screen parent) {
		VersionScreen loadingScreen = new VersionScreen(
			parent,
			true,
			false,
			null,
			0,
			List.of()
		);
		Minecraft.getInstance().setScreen(loadingScreen);
		NotificationHud.notify(
			NotificationHud.Type.INFO,
			"AFR",
			Lang.t("upd.checking") + "\u2026"
		);
		String channel = "stable";
		AfrCloud.version("afr", channel)
			.thenAccept(version -> {
				int remote = version.has("version_code")
					? version.get("version_code").getAsInt()
					: 0;
				String name = version.has("version")
					? version.get("version").getAsString()
					: "?";
				List<String> log = new ArrayList<>();
				if (
					version.has("changelog") &&
					version.get("changelog").isJsonArray()
				) {
					JsonArray array = version.getAsJsonArray("changelog");
					array.forEach(e -> log.add(e.getAsString()));
				}
				AfrCloud.runOnClient(() -> {
					if (Minecraft.getInstance().screen != loadingScreen) {
						return;
					}
					Minecraft.getInstance().setScreen(
						new VersionScreen(
							parent,
							false,
							false,
							name,
							remote,
							log
						)
					);
					boolean update =
						remote > VersionChecker.currentVersionCode();
					NotificationHud.notify(
						update
							? NotificationHud.Type.INFO
							: NotificationHud.Type.SUCCESS,
						"AFR",
						update
							? Lang.t("msg.update_available") + ": " + name
							: Lang.t("upd.no_updates")
					);
				});
			})
			.exceptionally(e -> {
				AfrCloud.runOnClient(() -> {
					if (Minecraft.getInstance().screen != loadingScreen) {
						return;
					}
					Minecraft.getInstance().setScreen(
						new VersionScreen(
							parent,
							false,
							true,
							null,
							0,
							List.of()
						)
					);
					NotificationHud.notify(
						NotificationHud.Type.ERROR,
						"AFR",
						Lang.t("msg.check_error")
					);
				});
				return null;
			});
	}

	@Override
	protected @NotNull OwoUIAdapter<FlowLayout> createAdapter() {
		return OwoUIAdapter.create(this, Containers::verticalFlow);
	}

	@Override
	protected void build(FlowLayout root) {
		int current = VersionChecker.currentVersionCode();

		root.surface(Surface.flat(Ui.BACKDROP));
		root.horizontalAlignment(HorizontalAlignment.CENTER);
		root.verticalAlignment(VerticalAlignment.CENTER);

		int width = Math.max(340, Math.min(this.width - 16, 480));
		int height = Math.max(200, Math.min(this.height - 16, 270));
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
				Component.literal(Icons.RELOAD + "  " + Lang.t("sec.updates"))
			).color(Color.ofArgb(Ui.SECTION))
		);
		window.child(header);

		FlowLayout content = Containers.verticalFlow(
			Sizing.fill(100),
			Sizing.content()
		);
		content.gap(Ui.GAP);
		content.padding(Insets.of(Ui.PAD));

		FlowLayout status = Ui.card(Icons.CLOUD + "  " + Lang.t("sec.status"));
		status.child(
			settingRow(
				Lang.t("upd.mod_version"),
				Ui.label(
					VersionChecker.currentVersionCode() +
						"  (" +
						modVersion() +
						")"
				)
			)
		);
		if (loading) {
			status.child(settingRow(Lang.t("upd.checking"), Ui.dim("...")));
		} else if (error) {
			status.child(
				settingRow(
					Lang.t("upd.result"),
					Ui.label(Lang.t("msg.check_error"))
				)
			);
		} else {
			status.child(
				settingRow(
					Lang.t("upd.latest"),
					Ui.label(remoteName + "  (" + remoteCode + ")")
				)
			);
			boolean update = remoteCode > current;
			status.child(
				settingRow(
					Lang.t("upd.update"),
					Ui.label(
						update
							? Lang.t("upd.available")
							: Lang.t("upd.not_required")
					)
				)
			);
		}
		content.child(status);

		if (!loading && !error && !changelog.isEmpty()) {
			FlowLayout log = Ui.card(
				Icons.ACTIONS + "  " + Lang.t("sec.changelog")
			);
			for (String line : changelog) {
				log.child(Ui.dim("• " + line));
			}
			content.child(log);
		}
		window.child(
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
		footer.child(
			new AfrButton(
				Component.literal(Icons.RELOAD + " " + Lang.t("btn.check")),
				AfrButton.Variant.SECONDARY,
				() -> open(parent)
			).tip(Lang.t("btn.check"))
		);
		footer.child(Ui.grow());
		footer.child(
			new AfrButton(
				Component.literal(
					Icons.RELOAD + " " + Lang.t("btn.check_config")
				),
				AfrButton.Variant.SECONDARY,
				() -> AfrRuntime.get().checkConfigUpdate()
			)
		);
		footer.child(
			new AfrButton(
				Component.literal(Icons.CLOUD + " " + Lang.t("btn.github")),
				AfrButton.Variant.SECONDARY,
				() ->
					Util.getPlatform().openUri(
						URI.create("https://github.com/Agzes/AFR")
					)
			)
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

	private static String modVersion() {
		return FabricLoader.getInstance()
			.getModContainer("afr")
			.map(c -> c.getMetadata().getVersion().getFriendlyString())
			.orElse("?");
	}

	private FlowLayout settingRow(
		String label,
		io.wispforest.owo.ui.core.Component control
	) {
		FlowLayout row = Containers.horizontalFlow(
			Sizing.fill(100),
			Sizing.content()
		);
		row.gap(6);
		row.verticalAlignment(VerticalAlignment.CENTER);
		row.child(Ui.dim(label));
		row.child(Ui.grow());
		row.child(control);
		return row;
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

// SPDX-License-Identifier: GPL-3.0-only
package io.github.agzes.afr.client.ui;

import io.github.agzes.afr.client.AfrRuntime;
import io.github.agzes.afr.client.i18n.Lang;
import io.github.agzes.afr.client.input.KeyCombo;
import io.github.agzes.afr.client.model.ActionRef;
import io.github.agzes.afr.client.model.QuickActionWindow;
import io.github.agzes.afr.client.model.RpAction;
import io.github.agzes.afr.client.model.RpProfile;
import io.wispforest.owo.ui.component.Components;
import io.wispforest.owo.ui.container.Containers;
import io.wispforest.owo.ui.container.FlowLayout;
import io.wispforest.owo.ui.core.Color;
import io.wispforest.owo.ui.core.HorizontalAlignment;
import io.wispforest.owo.ui.core.Insets;
import io.wispforest.owo.ui.core.Sizing;
import io.wispforest.owo.ui.core.VerticalAlignment;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

final class MenuChrome {

	private MenuChrome() {}

	static FlowLayout header(String title) {
		FlowLayout header = Containers.horizontalFlow(
			Sizing.fill(100),
			Sizing.content()
		);
		header.surface(Ui.header());
		header.padding(Insets.of(5));
		header.horizontalAlignment(HorizontalAlignment.CENTER);
		header.verticalAlignment(VerticalAlignment.CENTER);
		header.child(Ui.grow());
		header.child(
			Components.label(
				Component.literal("AFR").withStyle(ChatFormatting.BOLD)
			).color(Color.ofArgb(Ui.ACCENT))
		);
		header.child(
			Components.label(Component.literal(" \u2022 " + title)).color(
				Color.ofArgb(Ui.DIM)
			)
		);
		header.child(Ui.grow());
		return header;
	}

	static FlowLayout topBar(
		AfrRuntime runtime,
		Screen returnTo,
		String windowId
	) {
		FlowLayout topBar = Containers.horizontalFlow(
			Sizing.fill(100),
			Sizing.content()
		);
		topBar.gap(4);
		topBar.padding(Insets.of(4, 6, 0, 6));
		topBar.horizontalAlignment(HorizontalAlignment.RIGHT);
		if (runtime.config().settings().showProfileSwitcher) {
			RpProfile active = runtime.config().active();
			topBar.child(
				new AfrButton(
					Component.literal(
						Icons.PROFILES +
							" " +
							(active != null ? active.meta.label : "-")
					),
					AfrButton.Variant.SECONDARY,
					() -> cycleProfile(runtime, windowId)
				).tip(Lang.t("lbl.profile_switch"))
			);
		}
		topBar.child(
			new AfrButton(
				Component.literal(Icons.GEAR),
				AfrButton.Variant.ICON,
				() ->
					Minecraft.getInstance().setScreen(
						new ConfigScreen(returnTo, 3, null, null)
					)
			).tip(Lang.t("btn.editor"))
		);
		return topBar;
	}

	static AfrButton allActionsButton(AfrRuntime runtime) {
		return new AfrButton(
			Component.literal(Icons.ACTIONS + " " + Lang.t("btn.all_actions")),
			AfrButton.Variant.SECONDARY,
			() ->
				Minecraft.getInstance().setScreen(new ActionMenuScreen(runtime))
		).tip(Lang.t("tip.all_actions"));
	}

	static void cycleProfile(AfrRuntime runtime, String windowId) {
		List<String> ids = new ArrayList<>(
			runtime.config().profiles().keySet()
		);
		if (ids.size() <= 1) {
			return;
		}
		int index = Math.max(0, ids.indexOf(runtime.config().activeId()));
		String next = ids.get((index + 1) % ids.size());
		runtime.config().setActive(next);
		runtime.rebuildBinds();
		if (windowId != null && !windowId.isBlank()) {
			runtime.openWindow(windowId);
		} else {
			runtime.openMenu();
		}
	}

	static AfrButton styleSwitch(String current, String windowId) {
		String[] order = { "list", "tiles", "radial" };
		String[] keys = { "mode.list", "mode.tiles", "mode.radial" };
		int index = 0;
		for (int i = 0; i < order.length; i++) {
			if (order[i].equalsIgnoreCase(current)) {
				index = i;
			}
		}
		String next = order[(index + 1) % order.length];
		AfrButton button = new AfrButton(
			Component.literal(Icons.RELOAD + " " + Lang.t(keys[index])),
			AfrButton.Variant.SECONDARY,
			() -> switchStyle(next, windowId)
		);
		return button.tip(Lang.t("tip.style_switch"));
	}

	static FlowLayout actionRow(
		AfrRuntime runtime,
		ActionRef ref,
		String prefix,
		Runnable onToggle,
		Runnable onRun
	) {
		RpAction action = ref.action();
		AfrButton star = new AfrButton(
			Component.literal(action.favorite ? Icons.STAR : Icons.EMPTY_STAR),
			AfrButton.Variant.ICON,
			() -> {
				action.favorite = !action.favorite;
				runtime.config().saveActive();
				if (onToggle != null) {
					onToggle.run();
				}
			}
		);
		String bind = runtime.config().resolveBind(ref.id(), action.bind);
		long cooldown = runtime.cooldownRemaining(ref.id());
		String trailing =
			cooldown > 0
				? Lang.t("lbl.cd") + " " + cooldown + "s"
				: bindDisplay(bind);
		AfrListItem item = new AfrListItem(
			"",
			prefix + action.displayName(),
			trailing
		);
		item.stack(Items.fromId(action.icon));
		item.horizontalSizing(Sizing.expand(100));
		if (action.description != null && !action.description.isBlank()) {
			item.tooltip(Component.literal(action.description));
		}
		item.onPress(onRun);
		FlowLayout row = Containers.horizontalFlow(
			Sizing.fill(100),
			Sizing.content()
		);
		row.gap(3);
		row.verticalAlignment(VerticalAlignment.CENTER);
		row.child(star);
		row.child(item);
		return row;
	}

	static String bindDisplay(String bind) {
		if (bind == null || bind.isBlank()) {
			return "";
		}
		KeyCombo combo = KeyCombo.parse(bind);
		return combo != null ? combo.display() : "";
	}

	static void switchStyle(String style, String windowId) {
		AfrRuntime runtime = AfrRuntime.get();
		if (windowId != null) {
			RpProfile profile = runtime.config().active();
			QuickActionWindow window =
				profile != null ? profile.window(windowId) : null;
			if (window != null) {
				window.style = style;
			}
			runtime.config().saveActive();
			runtime.openWindow(windowId);
		} else {
			runtime.config().settings().menuStyle = style;
			runtime.config().saveSettings();
			runtime.openMenu();
		}
	}
}

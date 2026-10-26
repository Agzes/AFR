// SPDX-License-Identifier: GPL-3.0-only
package io.github.agzes.afr.client.ui;

import io.github.agzes.afr.client.AfrRuntime;
import io.github.agzes.afr.client.i18n.Lang;
import io.github.agzes.afr.client.model.ActionRef;
import io.github.agzes.afr.client.model.RpAction;
import io.github.agzes.afr.client.model.RpProfile;
import io.wispforest.owo.ui.base.BaseOwoScreen;
import io.wispforest.owo.ui.container.Containers;
import io.wispforest.owo.ui.container.FlowLayout;
import io.wispforest.owo.ui.core.HorizontalAlignment;
import io.wispforest.owo.ui.core.Insets;
import io.wispforest.owo.ui.core.OwoUIAdapter;
import io.wispforest.owo.ui.core.Sizing;
import io.wispforest.owo.ui.core.Surface;
import io.wispforest.owo.ui.core.VerticalAlignment;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;
import org.lwjgl.glfw.GLFW;

public class ActionMenuScreen extends BaseOwoScreen<FlowLayout> {

	private final AfrRuntime runtime;
	private final Screen parent;
	private final List<ActionRef> favorites = new ArrayList<>();
	private final List<ActionRef> allActions = new ArrayList<>();
	private String query = "";
	private FlowLayout listHost;
	private AfrInput search;
	private AfrScroll scroll;

	public ActionMenuScreen(AfrRuntime runtime) {
		this(runtime, null);
	}

	public ActionMenuScreen(AfrRuntime runtime, Screen parent) {
		super(Component.literal("AFR"));
		this.runtime = runtime;
		this.parent = parent;
	}

	@Override
	protected @NotNull OwoUIAdapter<FlowLayout> createAdapter() {
		return OwoUIAdapter.create(this, Containers::verticalFlow);
	}

	@Override
	protected void build(FlowLayout root) {
		RpProfile profile = runtime.config().active();
		allActions.clear();
		if (profile != null) {
			allActions.addAll(profile.actionRefs());
			allActions.sort(
				Comparator.comparing(
					ActionRef::displayName,
					String.CASE_INSENSITIVE_ORDER
				)
			);
		}
		recomputeFavorites();

		root.surface(Surface.flat(Ui.BACKDROP));
		root.horizontalAlignment(HorizontalAlignment.CENTER);
		root.verticalAlignment(VerticalAlignment.CENTER);

		int width = Math.max(280, Math.min(this.width - 16, 380));
		int rows = Math.min(Math.max(allActions.size(), 1), 10);
		int height = Math.max(
			170,
			Math.min(this.height - 16, 96 + rows * 22 + 20)
		);
		FlowLayout window = Containers.verticalFlow(
			Sizing.fixed(width),
			Sizing.fixed(height)
		);
		window.surface(Ui.menuWindow());
		window.gap(0);
		window.child(MenuChrome.header(Lang.t("menu.actions")));

		FlowLayout content = Containers.verticalFlow(
			Sizing.fill(100),
			Sizing.content()
		);
		content.gap(6);
		content.padding(Insets.of(4));

		if (profile == null || allActions.isEmpty()) {
			content.child(Ui.dim("-"));
		} else {
			AfrInput box = AfrInput.fill(query);
			box.hint(Lang.t("lbl.search"));
			box.onChanged(value -> {
				query = value == null ? "" : value;
				populate();
			});
			search = box;
			content.child(box.component());

			listHost = Containers.verticalFlow(
				Sizing.fill(100),
				Sizing.content()
			);
			listHost.gap(6);
			content.child(listHost);
			populate();
		}
		scroll = new AfrScroll(Sizing.fill(100), Sizing.expand(100), content);
		window.child(scroll);

		FlowLayout footer = Containers.horizontalFlow(
			Sizing.fill(100),
			Sizing.content()
		);
		footer.surface(Ui.footer());
		footer.padding(Insets.of(5));
		footer.gap(6);
		footer.verticalAlignment(VerticalAlignment.CENTER);
		footer.child(MenuChrome.styleSwitch("list", null));
		if (runtime.hasPausedAction()) {
			footer.child(
				new AfrButton(
					Component.literal(
						Icons.PLAY + " " + Lang.t("btn.continue_action")
					),
					AfrButton.Variant.PRIMARY,
					() -> {
						runtime.resumeLast();
						minecraft.setScreen(parent);
					}
				).tip(runtime.pausedActionName())
			);
		}
		footer.child(Ui.grow());
		final AfrButton[] pauseRef = new AfrButton[1];
		AfrButton pauseBtn = new AfrButton(
			Component.literal(
				runtime.paused() ? Lang.t("btn.resume") : Lang.t("btn.pause")
			),
			AfrButton.Variant.SECONDARY,
			() -> {
				runtime.togglePause();
				pauseRef[0].setText(
					Component.literal(
						runtime.paused()
							? Lang.t("btn.resume")
							: Lang.t("btn.pause")
					)
				);
			}
		);
		pauseRef[0] = pauseBtn;
		footer.child(pauseBtn);
		footer.child(
			new AfrButton(
				Component.literal(Icons.DELETE),
				AfrButton.Variant.ICON,
				this::onClose
			).tip(Lang.t("btn.close"))
		);
		window.child(footer);

		root.child(MenuChrome.topBar(runtime, this, null));
		FlowLayout center = Containers.verticalFlow(
			Sizing.fill(100),
			Sizing.expand(100)
		);
		center.horizontalAlignment(HorizontalAlignment.CENTER);
		center.verticalAlignment(VerticalAlignment.CENTER);
		center.child(window);
		root.child(center);
	}

	private void recomputeFavorites() {
		favorites.clear();
		for (ActionRef ref : allActions) {
			if (ref.action().favorite) {
				favorites.add(ref);
			}
		}
	}

	private void populate() {
		if (listHost == null) {
			return;
		}
		double offset = scroll != null ? scroll.getOffset() : -1;
		listHost.clearChildren();
		List<ActionRef> fav = filter(favorites);
		List<ActionRef> every = filter(allActions);
		if (fav.isEmpty() && every.isEmpty()) {
			listHost.child(Ui.dim(Lang.t("lbl.empty_list")));
			restoreScroll(offset);
			return;
		}
		if (!fav.isEmpty()) {
			FlowLayout favCard = Ui.card(
				Icons.STAR + "  " + Lang.t("sec.favorites")
			);
			int number = 0;
			for (ActionRef ref : fav) {
				number++;
				String prefix = number <= 9 ? number + ".  " : "";
				favCard.child(row(ref, prefix));
			}
			listHost.child(favCard);
		}
		FlowLayout allCard = Ui.card(
			Icons.ACTIONS + "  " + Lang.t("sec.all_actions")
		);
		for (ActionRef ref : every) {
			allCard.child(row(ref, ""));
		}
		listHost.child(allCard);
		restoreScroll(offset);
	}

	private void restoreScroll(double offset) {
		if (scroll != null && offset >= 0) {
			scroll.restore(offset);
		}
	}

	private List<ActionRef> filter(List<ActionRef> source) {
		String q = query == null ? "" : query.strip().toLowerCase();
		if (q.isEmpty()) {
			return new ArrayList<>(source);
		}
		List<ActionRef> result = new ArrayList<>();
		for (ActionRef ref : source) {
			RpAction action = ref.action();
			String haystack = (
				ref.displayName() +
				" " +
				ref.id() +
				" " +
				action.name +
				" " +
				(action.description == null ? "" : action.description)
			).toLowerCase();
			if (haystack.contains(q)) {
				result.add(ref);
			}
		}
		return result;
	}

	private FlowLayout row(ActionRef ref, String prefix) {
		return MenuChrome.actionRow(
			runtime,
			ref,
			prefix,
			() -> {
				recomputeFavorites();
				populate();
			},
			() -> execute(ref.id())
		);
	}

	private void execute(String id) {
		minecraft.setScreen(parent);
		runtime.runAction(id);
	}

	@Override
	public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
		if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
			if (search != null && search.focused() && !query.isEmpty()) {
				search.setValue("");
				query = "";
				populate();
				return true;
			}
			onClose();
			return true;
		}
		if (search != null && search.focused()) {
			return super.keyPressed(keyCode, scanCode, modifiers);
		}
		if (keyCode >= GLFW.GLFW_KEY_1 && keyCode <= GLFW.GLFW_KEY_9) {
			int i = keyCode - GLFW.GLFW_KEY_1;
			if (i < favorites.size()) {
				execute(favorites.get(i).id());
				return true;
			}
		}
		return super.keyPressed(keyCode, scanCode, modifiers);
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

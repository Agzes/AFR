// SPDX-License-Identifier: GPL-3.0-only
package io.github.agzes.afr.client.ui;

import io.github.agzes.afr.client.AfrRuntime;
import io.github.agzes.afr.client.i18n.Lang;
import io.github.agzes.afr.client.model.ActionRef;
import io.github.agzes.afr.client.model.QuickActionWindow;
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
import java.util.List;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;
import org.lwjgl.glfw.GLFW;

public class QuickActionWindowScreen extends BaseOwoScreen<FlowLayout> {

	private final AfrRuntime runtime;
	private final QuickActionWindow window;
	private final List<ActionRef> actions = new ArrayList<>();
	private String query = "";
	private FlowLayout listHost;
	private AfrScroll scroll;
	private AfrInput search;

	public QuickActionWindowScreen(
		AfrRuntime runtime,
		QuickActionWindow window
	) {
		super(Component.literal(window.name));
		this.runtime = runtime;
		this.window = window;
	}

	@Override
	protected @NotNull OwoUIAdapter<FlowLayout> createAdapter() {
		return OwoUIAdapter.create(this, Containers::verticalFlow);
	}

	@Override
	protected void build(FlowLayout root) {
		RpProfile profile = runtime.config().active();
		actions.clear();
		if (profile != null) {
			for (String id : window.actions) {
				RpAction action = profile.action(id);
				if (action != null) {
					actions.add(new ActionRef(id, action));
				}
			}
		}

		root.surface(Surface.flat(Ui.BACKDROP));
		root.horizontalAlignment(HorizontalAlignment.CENTER);
		root.verticalAlignment(VerticalAlignment.CENTER);

		int width = Math.max(280, Math.min(this.width - 16, 430));
		int height = Math.max(170, Math.min(this.height - 16, 300));
		FlowLayout win = Containers.verticalFlow(
			Sizing.fixed(width),
			Sizing.fixed(height)
		);
		win.surface(Ui.menuWindow());
		win.gap(0);
		win.child(MenuChrome.header(window.name));

		FlowLayout content = Containers.verticalFlow(
			Sizing.fill(100),
			Sizing.content()
		);
		content.gap(6);
		content.padding(Insets.of(4));

		AfrInput box = AfrInput.fill(query);
		box.hint(Lang.t("lbl.search"));
		box.onChanged(value -> {
			query = value == null ? "" : value;
			populate();
		});
		search = box;
		content.child(box.component());

		listHost = Containers.verticalFlow(Sizing.fill(100), Sizing.content());
		listHost.gap(6);
		content.child(listHost);
		scroll = new AfrScroll(Sizing.fill(100), Sizing.expand(100), content);
		win.child(scroll);
		populate();

		String current = styleOf(window);
		FlowLayout footer = Containers.horizontalFlow(
			Sizing.fill(100),
			Sizing.content()
		);
		footer.surface(Ui.footer());
		footer.padding(Insets.of(5));
		footer.gap(6);
		footer.verticalAlignment(VerticalAlignment.CENTER);
		footer.child(MenuChrome.styleSwitch(current, window.id));
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

		root.child(MenuChrome.topBar(runtime, this, window.id));
		FlowLayout center = Containers.verticalFlow(
			Sizing.fill(100),
			Sizing.expand(100)
		);
		center.horizontalAlignment(HorizontalAlignment.CENTER);
		center.verticalAlignment(VerticalAlignment.CENTER);
		center.child(win);
		root.child(center);
	}

	private void populate() {
		if (listHost == null) {
			return;
		}
		double offset = scroll != null ? scroll.getOffset() : -1;
		listHost.clearChildren();
		List<ActionRef> list = filter();
		if (list.isEmpty()) {
			listHost.child(Ui.dim(Lang.t("lbl.empty_list")));
		} else {
			FlowLayout card = Ui.card(Icons.ACTIONS + "  " + window.name);
			for (ActionRef ref : list) {
				card.child(
					MenuChrome.actionRow(runtime, ref, "", this::populate, () ->
						execute(ref.id())
					)
				);
			}
			listHost.child(card);
		}
		if (scroll != null && offset >= 0) {
			scroll.restore(offset);
		}
	}

	private List<ActionRef> filter() {
		String q = query == null ? "" : query.strip().toLowerCase();
		if (q.isEmpty()) {
			return new ArrayList<>(actions);
		}
		List<ActionRef> result = new ArrayList<>();
		for (ActionRef ref : actions) {
			RpAction action = ref.action();
			String haystack = (
				ref.displayName() +
				" " +
				ref.id() +
				" " +
				(action.description == null ? "" : action.description)
			).toLowerCase();
			if (haystack.contains(q)) {
				result.add(ref);
			}
		}
		return result;
	}

	private static String styleOf(QuickActionWindow window) {
		String style = window.style == null ? "" : window.style.toLowerCase();
		if ("tiles".equals(style)) {
			return "tiles";
		}
		if ("radial".equals(style)) {
			return "radial";
		}
		return "list";
	}

	private void execute(String id) {
		minecraft.setScreen(null);
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
			List<ActionRef> list = filter();
			if (i < list.size()) {
				execute(list.get(i).id());
				return true;
			}
		}
		return super.keyPressed(keyCode, scanCode, modifiers);
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

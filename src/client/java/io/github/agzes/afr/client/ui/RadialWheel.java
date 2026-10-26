// SPDX-License-Identifier: GPL-3.0-only
package io.github.agzes.afr.client.ui;

import io.github.agzes.afr.client.AfrRuntime;
import io.github.agzes.afr.client.i18n.Lang;
import io.github.agzes.afr.client.model.ActionRef;
import io.github.agzes.afr.client.model.RpAction;
import io.wispforest.owo.ui.base.BaseComponent;
import io.wispforest.owo.ui.core.OwoUIDrawContext;
import io.wispforest.owo.ui.core.Sizing;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.world.item.ItemStack;

public class RadialWheel extends BaseComponent {

	private static final int BOX_H = 18;
	private static final int MIN_BOX_W = 84;
	private static final int MAX_BOX_W = 158;
	private static final int MAX_ITEMS = 12;
	private static final int CENTER_W = 156;
	private static final int CENTER_H = 44;

	private final AfrRuntime runtime;
	private final List<ActionRef> all;
	private final Consumer<ActionRef> onPick;
	private final Runnable onCenter;
	private final List<ActionRef> shown = new ArrayList<>();

	private int shownTotal;
	private int centerX;
	private int centerY;
	private int wheelRadius;
	private int hoveredIndex = -1;
	private boolean centerHovered;

	public RadialWheel(
		AfrRuntime runtime,
		List<ActionRef> all,
		Consumer<ActionRef> onPick,
		Runnable onCenter
	) {
		this.runtime = runtime;
		this.all = new ArrayList<>(all);
		this.onPick = onPick;
		this.onCenter = onCenter;
		horizontalSizing(Sizing.fill(100));
		verticalSizing(Sizing.expand(100));
	}

	public List<ActionRef> shown() {
		return shown;
	}

	private void layout() {
		if (width <= 0 || height <= 0) {
			return;
		}
		centerX = x + width / 2;
		centerY = y + height / 2;
		int maxRadius = Math.max(56, Math.min(width, height) / 2 - BOX_H);
		List<ActionRef> pool = buildPool();
		int wanted = Math.min(MAX_ITEMS, pool.size());
		while (wanted > 1 && neededRadius(pool, wanted) > maxRadius) {
			wanted--;
		}
		if (wanted < 1 && !pool.isEmpty()) {
			wanted = 1;
		}
		shown.clear();
		for (int i = 0; i < wanted; i++) {
			shown.add(pool.get(i));
		}
		shownTotal = Math.max(0, all.size() - shown.size());
		wheelRadius = (int) Math.min(
			maxRadius,
			Math.max(56, neededRadius(shown, shown.size()))
		);
	}

	private List<ActionRef> buildPool() {
		List<ActionRef> pool = new ArrayList<>();
		List<ActionRef> favorites = new ArrayList<>();
		for (ActionRef ref : all) {
			if (ref.action().favorite) {
				favorites.add(ref);
			}
		}
		favorites.sort((a, b) ->
			a.displayName().compareToIgnoreCase(b.displayName())
		);
		pool.addAll(favorites);

		for (String id : runtime.config().settings().recentActions) {
			ActionRef ref = find(id);
			if (ref != null && !pool.contains(ref)) {
				pool.add(ref);
			}
		}
		if (pool.isEmpty()) {
			pool.addAll(all);
		}
		return pool;
	}

	private ActionRef find(String id) {
		for (ActionRef ref : all) {
			if (ref.id().equals(id) || ref.displayName().equals(id)) {
				return ref;
			}
		}
		return null;
	}

	private double neededRadius(List<ActionRef> items, int count) {
		if (count <= 1) {
			return 92;
		}
		int widest = 0;
		for (int i = 0; i < count && i < items.size(); i++) {
			widest = Math.max(widest, boxWidth(items.get(i)));
		}
		return (count * (widest + 12)) / (2 * Math.PI);
	}

	private int boxWidth(ActionRef ref) {
		Font font = Minecraft.getInstance().font;
		int text = font == null ? 80 : font.width(ref.displayName());
		return Math.max(MIN_BOX_W, Math.min(MAX_BOX_W, text + 26));
	}

	@Override
	public void update(float delta, int mouseX, int mouseY) {
		super.update(delta, mouseX, mouseY);
		layout();
		hoveredIndex = nearest(mouseX, mouseY);
		centerHovered = isInCenter(mouseX, mouseY);
	}

	@Override
	public void draw(
		OwoUIDrawContext context,
		int mouseX,
		int mouseY,
		float partialTicks,
		float delta
	) {
		layout();
		drawCenter(context);
		int count = shown.size();
		for (int i = 0; i < count; i++) {
			ActionRef ref = shown.get(i);
			double angle = -Math.PI / 2 + (2 * Math.PI * i) / count;
			int itemX = centerX + (int) (Math.cos(angle) * wheelRadius);
			int itemY = centerY + (int) (Math.sin(angle) * wheelRadius);
			drawItem(context, ref, itemX, itemY, i == hoveredIndex);
		}
	}

	private void drawCenter(OwoUIDrawContext context) {
		int w = CENTER_W;
		int h = CENTER_H;
		boolean hovered = centerHovered;
		Ui.roundRect(
			context,
			centerX - w / 2,
			centerY - h / 2,
			w,
			h,
			hovered ? Ui.CARD_HOVER : Ui.WINDOW,
			6
		);
		Font font = Minecraft.getInstance().font;
		if (font == null) {
			return;
		}
		String main = shown.size() + " " + Lang.t("lbl.actions_count");
		context.drawCenteredString(font, main, centerX, centerY - 10, Ui.TEXT);
		String sub =
			shownTotal > 0
				? Lang.t("lbl.more") +
					" " +
					shownTotal +
					" - " +
					Lang.t("hint.open_list")
				: Lang.t("hint.click_run");
		context.drawCenteredString(
			font,
			TextFit.fit(font, sub, w - 10),
			centerX,
			centerY + 5,
			shownTotal > 0 ? Ui.WARN : Ui.MUTED
		);
	}

	private boolean isInCenter(double mouseX, double mouseY) {
		return (
			mouseX >= centerX - CENTER_W / 2 &&
			mouseX <= centerX + CENTER_W / 2 &&
			mouseY >= centerY - CENTER_H / 2 &&
			mouseY <= centerY + CENTER_H / 2
		);
	}

	private void drawItem(
		OwoUIDrawContext context,
		ActionRef ref,
		int itemX,
		int itemY,
		boolean selected
	) {
		RpAction action = ref.action();
		int w = boxWidth(ref);
		int left = itemX - w / 2;
		int top = itemY - BOX_H / 2;
		if (selected) {
			Ui.roundRect(
				context,
				left - 1,
				top - 1,
				w + 2,
				BOX_H + 2,
				Ui.ACCENT,
				4
			);
		}
		Ui.roundRect(
			context,
			left,
			top,
			w,
			BOX_H,
			selected ? Ui.ACCENT_DIM : Ui.CARD,
			4
		);

		Font font = Minecraft.getInstance().font;
		int cursor = left + 5;
		ItemStack stack = Items.fromId(action.icon);
		if (!stack.isEmpty()) {
			context.renderItem(stack, cursor, top + 1);
			cursor += 18;
		} else if (action.favorite && font != null) {
			context.drawString(
				font,
				Icons.STAR,
				cursor,
				top + 5,
				Ui.WARN,
				false
			);
			cursor += 10;
		}
		if (font != null) {
			String name = TextFit.fit(
				font,
				action.displayName(),
				left + w - 5 - cursor
			);
			context.drawString(font, name, cursor, top + 5, Ui.TEXT, false);
		}
	}

	private int nearest(int mouseX, int mouseY) {
		int best = -1;
		int count = shown.size();
		for (int i = 0; i < count; i++) {
			double angle = -Math.PI / 2 + (2 * Math.PI * i) / count;
			int itemX = centerX + (int) (Math.cos(angle) * wheelRadius);
			int itemY = centerY + (int) (Math.sin(angle) * wheelRadius);
			if (
				Math.abs(mouseX - itemX) <= boxWidth(shown.get(i)) / 2 + 4 &&
				Math.abs(mouseY - itemY) <= BOX_H / 2 + 4
			) {
				return i;
			}
		}
		return best;
	}

	@Override
	public boolean onMouseDown(double mouseX, double mouseY, int button) {
		if (button == 0 && isInCenter(mouseX + x, mouseY + y)) {
			if (onCenter != null) {
				onCenter.run();
			}
			return true;
		}
		if (button == 0 && hoveredIndex >= 0 && hoveredIndex < shown.size()) {
			onPick.accept(shown.get(hoveredIndex));
			return true;
		}
		return super.onMouseDown(mouseX, mouseY, button);
	}
}

// SPDX-License-Identifier: GPL-3.0-only
package io.github.agzes.afr.client.hud;

import io.github.agzes.afr.client.AfrRuntime;
import io.github.agzes.afr.client.ui.Ui;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;

public final class NotificationHud {

	public enum Type {
		SUCCESS(Ui.GOOD),
		WARNING(Ui.WARN),
		ERROR(Ui.BAD),
		INFO(Ui.ACCENT);

		public final int color;

		Type(int color) {
			this.color = color;
		}
	}

	private record Notice(
		Type type,
		String title,
		String description,
		long born,
		long duration
	) {}

	private static final int WIDTH = 168;
	private static final int PADDING = 6;
	private static final int LINE = 9;
	private static final int MAX_TITLE_LINES = 2;
	private static final int MAX_DESC_LINES = 3;
	private static final Deque<Notice> QUEUE = new ArrayDeque<>();

	private NotificationHud() {}

	public static void notify(Type type, String title, String description) {
		AfrRuntime runtime = AfrRuntime.get();
		if (runtime != null && !runtime.config().settings().notifications) {
			return;
		}
		synchronized (QUEUE) {
			QUEUE.addLast(
				new Notice(
					type,
					title,
					description,
					System.currentTimeMillis(),
					5000
				)
			);
			while (QUEUE.size() > 5) {
				QUEUE.removeFirst();
			}
		}
	}

	public static void render(GuiGraphics context, float tickDelta) {
		long now = System.currentTimeMillis();
		List<Notice> notices;
		synchronized (QUEUE) {
			QUEUE.removeIf(n -> now - n.born > n.duration);
			notices = new ArrayList<>(QUEUE);
		}

		Minecraft client = Minecraft.getInstance();
		Font font = client.font;
		int screenWidth = client.getWindow().getGuiScaledWidth();
		int textWidth = WIDTH - PADDING * 2;
		int y = 6;
		for (Notice notice : notices) {
			long elapsed = now - notice.born;
			float in = elapsed < 250 ? Math.min(1f, elapsed / 250f) : 1f;
			long remaining = notice.duration - elapsed;
			float out =
				remaining < 300
					? Math.max(0f, Math.min(1f, remaining / 300f))
					: 1f;
			float visible = Math.min(in, out);

			List<FormattedCharSequence> title = wrap(
				font,
				notice.title,
				textWidth,
				MAX_TITLE_LINES
			);
			List<FormattedCharSequence> desc = wrap(
				font,
				notice.description,
				textWidth,
				MAX_DESC_LINES
			);
			int height =
				PADDING * 2 +
				title.size() * LINE +
				(desc.isEmpty() ? 0 : 2 + desc.size() * LINE);

			int x =
				screenWidth - WIDTH - 6 + (int) ((1f - visible) * (WIDTH + 6));
			Ui.roundRect(
				context,
				x,
				y,
				WIDTH,
				height,
				(0xF0 << 24) | (Ui.CARD & 0x00FFFFFF),
				4
			);
			int ty = y + PADDING;
			for (FormattedCharSequence line : title) {
				context.drawString(
					font,
					line,
					x + PADDING,
					ty,
					notice.type.color,
					false
				);
				ty += LINE;
			}
			if (!desc.isEmpty()) {
				ty += 2;
				for (FormattedCharSequence line : desc) {
					context.drawString(
						font,
						line,
						x + PADDING,
						ty,
						Ui.DIM,
						false
					);
					ty += LINE;
				}
			}
			y += height + 4;
		}
	}

	private static List<FormattedCharSequence> wrap(
		Font font,
		String text,
		int width,
		int maxLines
	) {
		if (text == null || text.isBlank()) {
			return List.of();
		}
		List<FormattedCharSequence> lines = font.split(
			Component.literal(text),
			width
		);
		return lines.size() > maxLines ? lines.subList(0, maxLines) : lines;
	}
}

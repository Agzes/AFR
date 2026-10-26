// SPDX-License-Identifier: GPL-3.0-only
package io.github.agzes.afr.client.hud;

import io.github.agzes.afr.client.AfrRuntime;
import io.github.agzes.afr.client.action.ActionExecutor;
import io.github.agzes.afr.client.ui.Ui;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;

public final class ProgressHud {

	private static float displayed = 0f;
	private static float alpha = 0f;
	private static long hideAt = 0;
	private static long last = System.currentTimeMillis();

	private ProgressHud() {}

	public static void render(GuiGraphics context, float tickDelta) {
		AfrRuntime runtime = AfrRuntime.get();
		if (runtime == null || !runtime.config().settings().showProgress) {
			return;
		}
		ActionExecutor executor = runtime.executor();

		long now = System.currentTimeMillis();
		float dt = Math.min((now - last) / 1000f, 0.1f);
		last = now;

		boolean running = executor.isRunning();
		if (running) {
			hideAt = 0;
		} else if (hideAt == 0 && !"idle".equals(executor.status())) {
			hideAt = now + ("stopped".equals(executor.status()) ? 1500 : 800);
		}

		boolean visible = running || (hideAt > 0 && now < hideAt);
		alpha = lerp(alpha, visible ? 1f : 0f, dt * (visible ? 12f : 8f));
		if (!visible && alpha < 0.02f) {
			displayed = 0f;
			return;
		}
		displayed = lerp(displayed, executor.progress(), dt * 10f);

		Minecraft client = Minecraft.getInstance();
		Font font = client.font;
		int screenWidth = client.getWindow().getGuiScaledWidth();
		int width = 150;
		int height = 18;
		int x = (screenWidth - width) / 2;
		int y = 6;
		int alphaInt = (int) (alpha * 235);

		Ui.roundRect(
			context,
			x,
			y,
			width,
			height,
			(alphaInt << 24) | (Ui.CARD & 0x00FFFFFF),
			4
		);

		int accent = "stopped".equals(executor.status())
			? Ui.BAD
			: "finished".equals(executor.status())
				? Ui.GOOD
				: Ui.ACCENT;
		int barX = x + 6;
		int barY = y + height - 6;
		int barWidth = width - 12;
		Ui.roundRect(
			context,
			barX,
			barY,
			barWidth,
			3,
			(alphaInt << 24) | (Ui.FIELD_BG & 0x00FFFFFF),
			1
		);
		int fill = (int) (barWidth * displayed);
		if (fill > 0) {
			Ui.roundRect(
				context,
				barX,
				barY,
				fill,
				3,
				(alphaInt << 24) | (accent & 0x00FFFFFF),
				1
			);
		}

		String name = executor.actionName();
		if (name.length() > 18) {
			name = name.substring(0, 16) + "..";
		}
		context.drawString(
			font,
			name,
			x + 6,
			y + 4,
			(alphaInt << 24) | (Ui.TEXT & 0x00FFFFFF),
			false
		);
		String steps = executor.step() + "/" + executor.total();
		int w = font.width(steps);
		context.drawString(
			font,
			steps,
			x + width - 6 - w,
			y + 4,
			(alphaInt << 24) | (Ui.DIM & 0x00FFFFFF),
			false
		);
	}

	private static float lerp(float a, float b, float t) {
		return a + (b - a) * Math.min(1f, Math.max(0f, t));
	}
}

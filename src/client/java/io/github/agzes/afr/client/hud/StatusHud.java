// SPDX-License-Identifier: GPL-3.0-only
package io.github.agzes.afr.client.hud;

import io.github.agzes.afr.client.AfrRuntime;
import io.github.agzes.afr.client.i18n.Lang;
import io.github.agzes.afr.client.model.RpProfile;
import io.github.agzes.afr.client.ui.TextFit;
import io.github.agzes.afr.client.ui.Ui;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;

public final class StatusHud {

	private StatusHud() {}

	public static void render(GuiGraphics context, float tickDelta) {
		AfrRuntime runtime = AfrRuntime.get();
		if (runtime == null || !runtime.config().settings().showHud) {
			return;
		}
		RpProfile profile = runtime.config().active();
		if (profile == null) {
			return;
		}
		Minecraft client = Minecraft.getInstance();
		Font font = client.font;

		boolean paused = runtime.paused();
		int accent = paused ? Ui.WARN : Ui.ACCENT;
		String label =
			"AFR  •  " + (paused ? Lang.t("hud.paused") : profile.meta.label);
		label = TextFit.fit(font, label, 220);

		int x = 5;
		int y = 5;
		int pad = 4;
		int width = Math.max(60, font.width(label) + pad * 2 + 10);
		int height = font.lineHeight + pad * 2;
		Ui.roundRect(
			context,
			x,
			y,
			width,
			height,
			(0xF0 << 24) | (Ui.CARD & 0x00FFFFFF),
			4
		);
		Ui.roundRect(context, x + 2, y + 3, 2, height - 6, accent, 1);
		context.drawString(
			font,
			label,
			x + pad + 4,
			y + (height - font.lineHeight) / 2 + 1,
			Ui.TEXT,
			false
		);
	}
}

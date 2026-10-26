// SPDX-License-Identifier: GPL-3.0-only
package io.github.agzes.afr.client.ui;

import io.github.agzes.afr.client.i18n.Lang;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

public class AfrMenuButton extends Button {

	private static final ResourceLocation ICON = new ResourceLocation(
		"afr",
		"icon_mono.png"
	);

	public AfrMenuButton(int x, int y, OnPress onPress) {
		super(x, y, 20, 20, Component.empty(), onPress, DEFAULT_NARRATION);
		setTooltip(
			Tooltip.create(Component.literal(Lang.t("tip.editor_button")))
		);
	}

	@Override
	protected void renderWidget(
		GuiGraphics graphics,
		int mouseX,
		int mouseY,
		float partialTick
	) {
		super.renderWidget(graphics, mouseX, mouseY, partialTick);
		graphics.blit(ICON, getX() + 2, getY() + 2, 0, 0, 16, 16, 16, 16);
	}
}

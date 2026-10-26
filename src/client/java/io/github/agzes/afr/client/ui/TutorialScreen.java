// SPDX-License-Identifier: GPL-3.0-only
package io.github.agzes.afr.client.ui;

import io.github.agzes.afr.AFR;
import io.github.agzes.afr.client.AfrRuntime;
import io.github.agzes.afr.client.i18n.Lang;
import io.github.agzes.afr.client.input.KeyCombo;
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
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.lwjgl.glfw.GLFW;

public class TutorialScreen extends BaseOwoScreen<FlowLayout> {

	private static final int PAGES = 7;
	private static final String GITHUB = "https://github.com/Agzes/AFR";

	private static String capturing = null;
	private static String demoActionBind = "Alt+Q";
	private static String demoMenuBind = "F9";

	private final Screen parent;
	private final boolean fromMenu;
	private final int page;

	public TutorialScreen(Screen parent, boolean fromMenu) {
		this(parent, fromMenu, 0);
	}

	public TutorialScreen(Screen parent, boolean fromMenu, int page) {
		super(Component.literal("AFR"));
		this.parent = parent;
		this.fromMenu = fromMenu;
		this.page = Math.max(0, Math.min(page, PAGES - 1));
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

		int width = Math.max(340, Math.min(this.width - 16, 500));
		FlowLayout window = Containers.verticalFlow(
			Sizing.fixed(width),
			Sizing.fixed(262)
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
					Icons.HOME +
						"  " +
						Lang.t("tut.header") +
						" " +
						(page + 1) +
						"/" +
						PAGES
				)
			).color(Color.ofArgb(Ui.SECTION))
		);
		window.child(header);

		FlowLayout content = Containers.verticalFlow(
			Sizing.fill(100),
			Sizing.content()
		);
		content.gap(6);
		content.padding(Insets.of(10));
		content.child(
			Components.label(
				Component.literal(Lang.t("tut.p" + page + ".title"))
			).color(Color.ofArgb(Ui.TEXT))
		);
		content.child(
			Components.label(
				Component.literal(Lang.t("tut.p" + page + ".text"))
			)
				.color(Color.ofArgb(Ui.DIM))
				.maxWidth(width - 28)
		);
		io.wispforest.owo.ui.core.Component sample = demo(page, width);
		if (sample != null) {
			content.child(sample);
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
		footer.verticalAlignment(VerticalAlignment.CENTER);
		FlowLayout footerLeft = Containers.horizontalFlow(
			Sizing.fixed(120),
			Sizing.content()
		);
		footerLeft.verticalAlignment(VerticalAlignment.CENTER);
		footerLeft.child(
			new AfrButton(
				Component.literal(Lang.t("btn.skip")),
				AfrButton.Variant.SECONDARY,
				this::finish
			)
		);
		FlowLayout footerCenter = Containers.horizontalFlow(
			Sizing.expand(100),
			Sizing.content()
		);
		footerCenter.horizontalAlignment(HorizontalAlignment.CENTER);
		footerCenter.verticalAlignment(VerticalAlignment.CENTER);
		footerCenter.child(
			new AfrButton(
				Component.literal(Icons.CLOUD + " GitHub"),
				AfrButton.Variant.SECONDARY,
				() -> Util.getPlatform().openUri(URI.create(GITHUB))
			).tip(GITHUB)
		);
		FlowLayout footerRight = Containers.horizontalFlow(
			Sizing.fixed(120),
			Sizing.content()
		);
		footerRight.gap(6);
		footerRight.horizontalAlignment(HorizontalAlignment.RIGHT);
		footerRight.verticalAlignment(VerticalAlignment.CENTER);
		if (page > 0) {
			footerRight.child(
				new AfrButton(
					Component.literal(Lang.t("btn.back")),
					AfrButton.Variant.SECONDARY,
					() -> {
						capturing = null;
						minecraft.setScreen(
							new TutorialScreen(parent, fromMenu, page - 1)
						);
					}
				)
			);
		}
		if (page < PAGES - 1) {
			footerRight.child(
				new AfrButton(
					Component.literal(Lang.t("btn.next")),
					AfrButton.Variant.PRIMARY,
					() -> {
						capturing = null;
						minecraft.setScreen(
							new TutorialScreen(parent, fromMenu, page + 1)
						);
					}
				)
			);
		} else {
			footerRight.child(
				new AfrButton(
					Component.literal(Icons.CHECK + " " + Lang.t("btn.done")),
					AfrButton.Variant.PRIMARY,
					this::finish
				)
			);
		}
		footer.child(footerLeft);
		footer.child(footerCenter);
		footer.child(footerRight);
		window.child(footer);

		root.child(window);
	}

	private io.wispforest.owo.ui.core.Component demo(int page, int width) {
		FlowLayout card = Containers.verticalFlow(
			Sizing.fill(100),
			Sizing.content()
		);
		card.surface(Ui.cardSurface());
		card.padding(Insets.of(Ui.CARD_PAD));
		card.gap(4);

		switch (page) {
			case 0 -> {
				card.child(
					featureRow(Icons.ACTIONS, Lang.t("tut.feat.editor"))
				);
				card.child(
					featureRow(Icons.LIBRARY, Lang.t("tut.feat.library"))
				);
				card.child(featureRow(Icons.PLAY, Lang.t("tut.feat.menu")));
				card.child(featureRow(Icons.BINDS, Lang.t("tut.feat.binds")));
				card.child(featureRow(Icons.CLOUD, Lang.t("tut.feat.misc")));
			}
			case 1 -> {
				FlowLayout chips = Ui.row();
				chips.child(
					new AfrButton(
						Component.literal(Lang.t("tut.profile.hospital")),
						AfrButton.Variant.SECONDARY,
						() -> {}
					).active(true)
				);
				chips.child(
					new AfrButton(
						Component.literal(Lang.t("tut.profile.police")),
						AfrButton.Variant.SECONDARY,
						() -> {}
					)
				);
				chips.child(
					new AfrButton(
						Component.literal(Lang.t("tut.profile.government")),
						AfrButton.Variant.SECONDARY,
						() -> {}
					)
				);
				card.child(chips);
				FlowLayout create = Ui.row();
				create.child(
					AfrInput.expand(Lang.t("tut.new_profile")).component()
				);
				create.child(
					new AfrButton(
						Component.literal(
							Icons.PLUS + " " + Lang.t("btn.create")
						),
						AfrButton.Variant.PRIMARY,
						() -> {}
					)
				);
				card.child(create);
			}
			case 3 -> {
				card.child(
					bindRow(Lang.t("tut.demo.pill"), demoActionBind, "action")
				);
				card.child(
					bindRow(Lang.t("tut.demo.menu"), demoMenuBind, "menu")
				);
				card.child(Ui.dim(Lang.t("tut.binds_hint")));
			}
			case 4 -> {
				card.child(Ui.dim(Lang.t("tut.menu_list")));
				FlowLayout list = Containers.verticalFlow(
					Sizing.fill(100),
					Sizing.content()
				);
				list.gap(1);
				for (String name : new String[] {
					Lang.t("tut.demo.greet"),
					Lang.t("tut.demo.pill"),
					Lang.t("tut.demo.inject"),
				}) {
					AfrListItem item = new AfrListItem("", name, "Alt+Q");
					item.horizontalSizing(Sizing.fill(100));
					list.child(item);
				}
				card.child(list);
				card.child(Ui.dim(Lang.t("tut.menu_tiles")));
				FlowLayout tiles = Ui.row();
				for (int i = 0; i < 8; i++) {
					tiles.child(
						new AfrTile(ItemStack.EMPTY, String.valueOf(i + 1))
					);
				}
				card.child(tiles);
				card.child(Ui.wrap(Lang.t("tut.menu_radial"), width - 40));
			}
			default -> {
				return null;
			}
		}
		return card;
	}

	private FlowLayout featureRow(String glyph, String text) {
		FlowLayout row = Ui.row();
		row.gap(6);
		row.child(
			Components.label(Component.literal(glyph)).color(
				Color.ofArgb(Ui.SECTION)
			)
		);
		row.child(
			Components.label(
				Component.literal(
					TextFit.fit(Minecraft.getInstance().font, text, 380)
				)
			).color(Color.ofArgb(Ui.DIM))
		);
		row.child(Ui.grow());
		return row;
	}

	private FlowLayout bindRow(String label, String value, String id) {
		FlowLayout row = Ui.row();
		row.child(Ui.label(label));
		row.child(Ui.grow());
		boolean active = id.equals(capturing);
		row.child(
			new AfrButton(
				Component.literal(active ? Lang.t("tut.press_key") : value),
				active
					? AfrButton.Variant.PRIMARY
					: AfrButton.Variant.SECONDARY,
				() -> {
					capturing = id;
					openPage();
				}
			).w(130)
		);
		return row;
	}

	private void openPage() {
		minecraft.setScreen(new TutorialScreen(parent, fromMenu, page));
	}

	@Override
	public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
		if (capturing != null) {
			if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
				capturing = null;
				openPage();
				return true;
			}
			if (
				keyCode == GLFW.GLFW_KEY_BACKSPACE ||
				keyCode == GLFW.GLFW_KEY_DELETE
			) {
				applyCapture("");
				return true;
			}
			if (
				keyCode == GLFW.GLFW_KEY_LEFT_CONTROL ||
				keyCode == GLFW.GLFW_KEY_RIGHT_CONTROL ||
				keyCode == GLFW.GLFW_KEY_LEFT_ALT ||
				keyCode == GLFW.GLFW_KEY_RIGHT_ALT ||
				keyCode == GLFW.GLFW_KEY_LEFT_SHIFT ||
				keyCode == GLFW.GLFW_KEY_RIGHT_SHIFT
			) {
				return true;
			}
			boolean ctrl =
				held(GLFW.GLFW_KEY_LEFT_CONTROL) ||
				held(GLFW.GLFW_KEY_RIGHT_CONTROL);
			boolean alt =
				held(GLFW.GLFW_KEY_LEFT_ALT) || held(GLFW.GLFW_KEY_RIGHT_ALT);
			boolean shift =
				held(GLFW.GLFW_KEY_LEFT_SHIFT) ||
				held(GLFW.GLFW_KEY_RIGHT_SHIFT);
			if (!ctrl && !alt && !shift) {
				ctrl = (modifiers & GLFW.GLFW_MOD_CONTROL) != 0;
				alt = (modifiers & GLFW.GLFW_MOD_ALT) != 0;
				shift = (modifiers & GLFW.GLFW_MOD_SHIFT) != 0;
			}
			KeyCombo combo = new KeyCombo(keyCode, ctrl, alt, shift);
			applyCapture(combo.display());
			return true;
		}
		return super.keyPressed(keyCode, scanCode, modifiers);
	}

	private static boolean held(int key) {
		long window = Minecraft.getInstance().getWindow().getWindow();
		return GLFW.glfwGetKey(window, key) == GLFW.GLFW_PRESS;
	}

	public boolean isCapturing() {
		return capturing != null;
	}

	public void captureMouseBind(String name) {
		if (capturing != null) {
			applyCapture(name);
		}
	}

	private void applyCapture(String combo) {
		String value = combo == null || combo.isBlank() ? "-" : combo;
		AFR.LOGGER.debug("AFR: tutorial bind demo captured '{}'", value);
		if ("action".equals(capturing)) {
			demoActionBind = value;
		} else {
			demoMenuBind = value;
		}
		capturing = null;
		openPage();
	}

	private void finish() {
		capturing = null;
		AfrRuntime.get().config().settings().tutorialDone = true;
		AfrRuntime.get().config().saveSettings();
		if (fromMenu) {
			minecraft.setScreen(parent);
		} else {
			minecraft.setScreen(new ConfigScreen(parent, 0, null, null));
		}
	}

	@Override
	public void onClose() {
		finish();
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}
}

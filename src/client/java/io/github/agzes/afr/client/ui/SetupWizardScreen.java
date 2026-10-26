// SPDX-License-Identifier: GPL-3.0-only
package io.github.agzes.afr.client.ui;

import io.github.agzes.afr.AFR;
import io.github.agzes.afr.client.AfrClient;
import io.github.agzes.afr.client.AfrRuntime;
import io.github.agzes.afr.client.config.ConfigManager;
import io.github.agzes.afr.client.i18n.Lang;
import io.github.agzes.afr.client.input.KeyCombo;
import io.github.agzes.afr.client.model.UserSettings;
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
import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;
import org.lwjgl.glfw.GLFW;

public class SetupWizardScreen extends BaseOwoScreen<FlowLayout> {

	private static final int STEPS = 3;

	private final Screen parent;
	private final int step;
	private final String capturing;

	public SetupWizardScreen(Screen parent, int step) {
		this(parent, step, null);
	}

	public SetupWizardScreen(Screen parent, int step, String capturing) {
		super(Component.literal("AFR"));
		this.parent = parent;
		this.step = Math.max(0, Math.min(step, STEPS - 1));
		this.capturing = capturing;
	}

	private ConfigManager config() {
		return AfrRuntime.get().config();
	}

	private UserSettings settings() {
		return config().settings();
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

		int width = Math.max(320, Math.min(this.width - 16, 420));
		int height = 210;
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
				Component.literal("AFR").withStyle(ChatFormatting.BOLD)
			).color(Color.ofArgb(Ui.ACCENT))
		);
		header.child(
			Components.label(
				Component.literal(" \u2022 " + Lang.t("setup.title"))
			).color(Color.ofArgb(Ui.DIM))
		);
		window.child(header);

		FlowLayout body = Containers.verticalFlow(
			Sizing.fill(100),
			Sizing.expand(100)
		);
		body.gap(8);
		body.padding(Insets.of(Ui.PAD));
		body.horizontalAlignment(HorizontalAlignment.CENTER);
		body.verticalAlignment(VerticalAlignment.CENTER);
		switch (step) {
			case 0 -> buildLanguage(body);
			case 1 -> buildHotkeys(body);
			default -> buildCharacter(body);
		}
		body.child(Ui.wrap(Lang.t("setup.hint"), width - 24));
		window.child(body);

		FlowLayout footer = Containers.horizontalFlow(
			Sizing.fill(100),
			Sizing.content()
		);
		footer.surface(Ui.footer());
		footer.padding(Insets.of(5));
		footer.gap(6);
		footer.verticalAlignment(VerticalAlignment.CENTER);
		footer.child(
			Ui.dim(Lang.t("setup.title") + "  " + (step + 1) + "/" + STEPS)
		);
		footer.child(Ui.grow());
		if (step > 0) {
			footer.child(
				new AfrButton(
					Component.literal(Lang.t("btn.back")),
					AfrButton.Variant.SECONDARY,
					() -> open(step - 1)
				)
			);
		}
		footer.child(
			new AfrButton(
				Component.literal(
					step >= STEPS - 1
						? Lang.t("setup.finish")
						: Lang.t("setup.next")
				),
				AfrButton.Variant.PRIMARY,
				() -> {
					if (step >= STEPS - 1) {
						finish();
					} else {
						open(step + 1);
					}
				}
			)
		);
		window.child(footer);

		root.child(window);
	}

	private void buildLanguage(FlowLayout body) {
		body.child(Ui.label(Lang.t("setup.language")));
		FlowLayout row = Ui.row();
		row.horizontalAlignment(HorizontalAlignment.CENTER);
		row.child(
			new AfrButton(
				Component.literal("Русский"),
				AfrButton.Variant.PRIMARY,
				() -> chooseLanguage("ru")
			)
		);
		row.child(
			new AfrButton(
				Component.literal("English"),
				AfrButton.Variant.SECONDARY,
				() -> chooseLanguage("en")
			)
		);
		body.child(row);
	}

	private void buildHotkeys(FlowLayout body) {
		body.child(Ui.label(Lang.t("setup.hotkeys")));
		body.child(Ui.wrap(Lang.t("setup.hotkeys_hint"), 360));
		body.child(captureRow("menu", Lang.t("setup.menu_key"), menuDisplay()));
		body.child(
			captureRow(
				"editor",
				Lang.t("setup.editor_key"),
				KeyCombo.name(settings().editorKey)
			)
		);
	}

	private void buildCharacter(FlowLayout body) {
		body.child(Ui.label(Lang.t("setup.character")));
		body.child(Ui.dim(Lang.t("setup.character_hint")));
		body.child(
			fieldRow(
				Lang.t("setup.rp_name"),
				settings().rpName,
				value -> settings().rpName = value
			)
		);
		body.child(
			fieldRow(
				Lang.t("setup.rp_role"),
				settings().rpRole,
				value -> settings().rpRole = value
			)
		);
	}

	private FlowLayout fieldRow(
		String label,
		String value,
		Consumer<String> onChange
	) {
		FlowLayout row = Containers.verticalFlow(
			Sizing.fill(100),
			Sizing.content()
		);
		row.gap(2);
		row.child(Ui.dim(label));
		AfrInput input = AfrInput.fill(value);
		input.onChanged(v -> onChange.accept(v == null ? "" : v));
		io.wispforest.owo.ui.core.Component field = input.component();
		field.horizontalSizing(Sizing.fill(100));
		row.child(field);
		return row;
	}

	private FlowLayout captureRow(String id, String label, String value) {
		FlowLayout row = Ui.row();
		row.horizontalSizing(Sizing.fill(100));
		row.child(Ui.dim(label));
		row.child(Ui.grow());
		boolean active = id.equals(capturing);
		row.child(
			new AfrButton(
				Component.literal(active ? Lang.t("setup.press_key") : value),
				active
					? AfrButton.Variant.PRIMARY
					: AfrButton.Variant.SECONDARY,
				() -> open(step, id)
			).w(140)
		);
		return row;
	}

	private String menuDisplay() {
		KeyCombo combo = KeyCombo.parse(settings().menuKey);
		return combo != null ? combo.display() : settings().menuKey;
	}

	private void chooseLanguage(String language) {
		settings().language = language;
		config().saveSettings();
		Lang.setLanguage(language);
		open(step + 1);
	}

	private void open(int newStep) {
		open(newStep, null);
	}

	private void open(int newStep, String capturingId) {
		minecraft.setScreen(
			new SetupWizardScreen(parent, newStep, capturingId)
		);
	}

	private void finish() {
		UserSettings settings = settings();
		settings.languageChosen = true;
		config().saveSettings();
		Lang.setLanguage(settings.language);
		AfrClient.setEditorKey(settings.editorKey);
		AfrRuntime.get().rebuildBinds();
		AFR.LOGGER.info(
			"AFR: setup wizard finished (language={}, menuKey={}, editorKey={})",
			settings.language,
			settings.menuKey,
			settings.editorKey
		);
		minecraft.setScreen(parent);
	}

	@Override
	public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
		if (capturing != null) {
			if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
				open(step);
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
			if ("menu".equals(capturing)) {
				settings().menuKey = KeyCombo.of(
					keyCode,
					ctrl,
					alt,
					shift
				).display();
			} else if ("editor".equals(capturing)) {
				settings().editorKey = keyCode;
				AfrClient.setEditorKey(keyCode);
			}
			AFR.LOGGER.debug(
				"AFR: setup wizard captured '{}' ({}), modifiers ctrl={} alt={} shift={}",
				keyCode,
				KeyCombo.of(keyCode, ctrl, alt, shift).display(),
				ctrl,
				alt,
				shift
			);
			config().saveSettings();
			open(step);
			return true;
		}
		if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
			onClose();
			return true;
		}
		return super.keyPressed(keyCode, scanCode, modifiers);
	}

	public boolean isCapturing() {
		return capturing != null;
	}

	public void captureMouseBind(String name) {
		if (capturing == null) {
			return;
		}
		if ("menu".equals(capturing)) {
			settings().menuKey = name;
		}
		config().saveSettings();
		if (AfrRuntime.get() != null) {
			AfrRuntime.get().rebuildBinds();
		}
		open(step);
	}

	private static boolean held(int key) {
		long window = Minecraft.getInstance().getWindow().getWindow();
		return GLFW.glfwGetKey(window, key) == GLFW.GLFW_PRESS;
	}

	@Override
	public void onClose() {
		settings().languageChosen = true;
		config().saveSettings();
		if (parent instanceof ConfigScreen editor) {
			editor.refresh();
		}
		minecraft.setScreen(parent);
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}
}

// SPDX-License-Identifier: GPL-3.0-only
package io.github.agzes.afr.client.ui;

import com.google.gson.JsonObject;
import io.github.agzes.afr.AFR;
import io.github.agzes.afr.client.AfrClient;
import io.github.agzes.afr.client.AfrRuntime;
import io.github.agzes.afr.client.action.PlaceholderResolver;
import io.github.agzes.afr.client.config.ConfigCodec;
import io.github.agzes.afr.client.config.ConfigManager;
import io.github.agzes.afr.client.config.ConfigPaths;
import io.github.agzes.afr.client.hud.NotificationHud;
import io.github.agzes.afr.client.i18n.Lang;
import io.github.agzes.afr.client.input.KeyCombo;
import io.github.agzes.afr.client.model.ActionRef;
import io.github.agzes.afr.client.model.Parameter;
import io.github.agzes.afr.client.model.ProfileMeta;
import io.github.agzes.afr.client.model.QuickActionWindow;
import io.github.agzes.afr.client.model.RpAction;
import io.github.agzes.afr.client.model.RpProfile;
import io.github.agzes.afr.client.model.UserSettings;
import io.github.agzes.afr.client.net.AfrCloud;
import io.github.agzes.afr.client.net.VersionChecker;
import io.github.agzes.afr.client.util.ActionLogger;
import io.github.agzes.afr.client.util.JsonText;
import io.wispforest.owo.ui.base.BaseOwoScreen;
import io.wispforest.owo.ui.component.Components;
import io.wispforest.owo.ui.component.LabelComponent;
import io.wispforest.owo.ui.container.Containers;
import io.wispforest.owo.ui.container.FlowLayout;
import io.wispforest.owo.ui.core.Color;
import io.wispforest.owo.ui.core.HorizontalAlignment;
import io.wispforest.owo.ui.core.Insets;
import io.wispforest.owo.ui.core.OwoUIAdapter;
import io.wispforest.owo.ui.core.Sizing;
import io.wispforest.owo.ui.core.Surface;
import io.wispforest.owo.ui.core.VerticalAlignment;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.ChatFormatting;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.lwjgl.glfw.GLFW;

public class ConfigScreen extends BaseOwoScreen<FlowLayout> {

	private static final String[] TABS = {
		"tab.home",
		"tab.settings",
		"tab.profiles",
		"tab.actions",
		"tab.binds",
		"tab.library",
	};
	private static final String[] TAB_ICONS = {
		Icons.HOME,
		Icons.GEAR,
		Icons.PROFILES,
		Icons.ACTIONS,
		Icons.BINDS,
		Icons.LIBRARY,
	};
	private static final String[] STEP_TYPES = { "RPAction", "RPActionGender" };
	private static final String[] STEP_METHODS = { "Send", "Input" };
	private static final String[] S_CODES = {
		"S100",
		"S250",
		"S300",
		"S500",
		"S700",
		"S800",
		"S1000",
		"S2000",
		"S3000",
		"S4000",
	};
	private static final String[] PARAM_TYPES = { "text", "number" };
	private static final String[] WINDOW_STYLES = {
		"list",
		"compact",
		"tiles",
	};
	private static final String[] MENU_STYLES = { "list", "radial", "tiles" };

	private final Screen parent;
	private int tab;

	public int tab() {
		return tab;
	}

	private final List<Runnable> savers = new ArrayList<>();
	private int windowSaverFrom = -1;
	private int editorSaverFrom = -1;
	private String capturing;
	private String selectedAction;
	private String selectedWindow;
	private boolean narrow;
	private int windowWidth;
	private FlowLayout rootComponent;
	private FlowLayout editorHost;
	private FlowLayout windowsHost;
	private AfrScroll scroll;
	private LabelComponent unsavedBadge;
	private static double pendingScroll = -1;
	private static boolean homeUpdateChecked = false;
	private static int homeUpdateRemote = 0;
	private static String homeUpdateName = "";
	private static boolean homeButtonsFetched = false;
	private static final List<String[]> homeButtons = new ArrayList<>();
	private boolean large;
	private boolean paramsEdit;
	private final Map<String, AfrButton> itemRefs = new LinkedHashMap<>();

	private boolean dirty() {
		AfrRuntime runtime = AfrRuntime.get();

		return runtime != null && runtime.editorDirty;
	}

	public ConfigScreen(
		Screen parent,
		int tab,
		String selectedAction,
		String capturing
	) {
		this(parent, tab, selectedAction, capturing, null);
	}

	public ConfigScreen(
		Screen parent,
		int tab,
		String selectedAction,
		String capturing,
		String selectedWindow
	) {
		super(Component.literal("AFR"));
		this.parent = parent;
		this.tab = Math.max(0, Math.min(tab, TABS.length - 1));
		this.selectedAction = selectedAction;
		this.capturing = capturing;
		this.selectedWindow = selectedWindow;
	}

	public static void open(Screen parent) {
		AfrRuntime runtime = AfrRuntime.get();
		if (runtime != null && !runtime.config().settings().tutorialDone) {
			Minecraft.getInstance().setScreen(
				new TutorialScreen(parent, false)
			);
		} else {
			Minecraft.getInstance().setScreen(
				new ConfigScreen(parent, 0, null, null)
			);
		}
	}

	private boolean applyingSavers;

	private void runSavers() {
		if (applyingSavers) {
			return;
		}
		applyingSavers = true;
		try {
			for (Runnable saver : new ArrayList<>(savers)) {
				saver.run();
			}
		} finally {
			applyingSavers = false;
		}
	}

	public void rebuild() {
		if (rootComponent == null) {
			return;
		}
		runSavers();
		rebuildInternal();
	}

	private void rebuildInternal() {
		if (rootComponent == null) {
			return;
		}
		double offset = scroll != null ? scroll.getOffset() : 0;
		savers.clear();
		windowSaverFrom = -1;
		editorSaverFrom = -1;
		itemRefs.clear();
		rootComponent.clearChildren();
		renderInto(rootComponent);
		if (scroll != null) {
			scroll.restore(offset);
		}
	}

	/** Re-render this editor after returning from a sub-screen. */
	public void refresh() {
		rebuildInternal();
	}

	/** Flush pending edits and open a sub-screen; the editor instance stays alive. */
	private void openSubScreen(Screen screen) {
		runSavers();
		minecraft.setScreen(screen);
	}

	/** Return to the editor from a sub-screen, keeping model changes made there. */
	public static void returnFrom(
		Screen parent,
		int tab,
		String selectedAction,
		String selectedWindow
	) {
		if (parent instanceof ConfigScreen editor) {
			editor.selectedAction = selectedAction;
			editor.selectedWindow = selectedWindow;
			editor.refresh();
			Minecraft.getInstance().setScreen(editor);
			return;
		}
		Minecraft.getInstance().setScreen(
			new ConfigScreen(parent, tab, selectedAction, null, selectedWindow)
		);
	}

	private void switchTo(int newTab) {
		if (dirty()) {
			minecraft.setScreen(
				new ConfirmScreen(
					Lang.t("confirm.title"),
					Lang.t("msg.confirm_unsaved"),
					Lang.t("btn.discard"),
					() -> {
						AfrRuntime.get().reload();
						minecraft.setScreen(this);
						doSwitch(newTab);
					},
					() -> minecraft.setScreen(this),
					Lang.t("btn.save"),
					() -> {
						if (!save()) {
							minecraft.setScreen(this);
							return;
						}
						minecraft.setScreen(this);
						doSwitch(newTab);
					}
				)
			);
			return;
		}
		doSwitch(newTab);
	}

	private void doSwitch(int newTab) {
		tab = newTab;
		selectedAction = null;
		selectedWindow = null;
		capturing = null;
		AfrRuntime.get().editorDirty = false;
		rebuild();
		if (scroll != null) {
			scroll.restore(0);
		}
	}

	private void markDirty() {
		AfrRuntime runtime = AfrRuntime.get();
		if (runtime != null) {
			runtime.editorDirty = true;
		}
		if (unsavedBadge != null) {
			unsavedBadge.text(
				Component.literal(Icons.WARN + " " + Lang.t("lbl.unsaved"))
			);
		}
	}

	private void confirm(String message, Runnable action) {
		minecraft.setScreen(
			new ConfirmScreen(
				Lang.t("confirm.title"),
				message,
				Lang.t("btn.delete"),
				() -> {
					minecraft.setScreen(this);
					action.run();
				},
				() -> minecraft.setScreen(this)
			)
		);
	}

	@Override
	protected @NotNull OwoUIAdapter<FlowLayout> createAdapter() {
		return OwoUIAdapter.create(this, Containers::verticalFlow);
	}

	@Override
	protected void build(FlowLayout root) {
		this.rootComponent = root;
		renderInto(root);
	}

	private void renderInto(FlowLayout root) {
		AfrRuntime runtime = AfrRuntime.get();
		ConfigManager config = runtime.config();
		UserSettings settings = config.settings();
		RpProfile profile = config.active();

		root.surface(Surface.flat(Ui.BACKDROP));
		root.horizontalAlignment(HorizontalAlignment.CENTER);
		root.verticalAlignment(VerticalAlignment.CENTER);

		int targetW = large ? Ui.WIN_W + 150 : Ui.WIN_W;
		int targetH = large ? Ui.WIN_H + 90 : Ui.WIN_H;
		int width = Math.max(320, Math.min(this.width - 12, targetW));
		int height = Math.max(170, Math.min(this.height - 12, targetH));
		narrow = width < 460;
		windowWidth = width;

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
		header.verticalAlignment(VerticalAlignment.CENTER);
		header.child(
			Containers.horizontalFlow(Sizing.fixed(130), Sizing.content())
		);
		FlowLayout titleArea = Containers.horizontalFlow(
			Sizing.expand(100),
			Sizing.content()
		);
		titleArea.horizontalAlignment(HorizontalAlignment.CENTER);
		titleArea.verticalAlignment(VerticalAlignment.CENTER);
		titleArea.child(
			Components.label(
				Component.literal("AFR").withStyle(ChatFormatting.BOLD)
			).color(Color.ofArgb(Ui.ACCENT))
		);
		titleArea.child(
			Components.label(
				Component.literal(
					" • " +
						TextFit.fit(
							Minecraft.getInstance().font,
							profile != null
								? profile.meta.label
								: Lang.t("lbl.no_profile"),
							width - 140
						)
				)
			).color(Color.ofArgb(Ui.DIM))
		);
		header.child(titleArea);
		FlowLayout badgeArea = Containers.horizontalFlow(
			Sizing.fixed(130),
			Sizing.content()
		);
		badgeArea.horizontalAlignment(HorizontalAlignment.RIGHT);
		badgeArea.verticalAlignment(VerticalAlignment.CENTER);
		unsavedBadge = Components.label(
			Component.literal(
				dirty() ? Icons.WARN + " " + Lang.t("lbl.unsaved") : ""
			)
		);
		unsavedBadge.color(Color.ofArgb(Ui.WARN));
		badgeArea.child(unsavedBadge);
		header.child(badgeArea);
		window.child(header);

		FlowLayout tabs = Containers.horizontalFlow(
			Sizing.fill(100),
			Sizing.content()
		);
		tabs.padding(Insets.of(4, 6, 2, 24));
		tabs.gap(2);
		boolean tabsCompact = width < 520;
		for (int i = 0; i < TABS.length; i++) {
			final int index = i;
			String label = tabsCompact
				? TAB_ICONS[i]
				: TAB_ICONS[i] + " " + Lang.t(TABS[i]);
			tabs.child(
				new AfrButton(
					Component.literal(label),
					AfrButton.Variant.TAB,
					() -> switchTo(index)
				)
					.active(i == tab)
					.tip(Lang.t(TABS[i]))
			);
		}
		window.child(tabs);

		FlowLayout content = Containers.verticalFlow(
			Sizing.fill(100),
			Sizing.content()
		);
		content.gap(Ui.GAP);
		content.padding(Insets.of(2, Ui.PAD, Ui.PAD, Ui.PAD));
		switch (tab) {
			case 0 -> buildAbout(content, config, profile);
			case 1 -> buildSettings(content, config, settings, profile);
			case 2 -> buildProfiles(content, config, profile);
			case 3 -> buildActions(content, config, profile);
			case 4 -> buildBinds(content, config, settings, profile);
			case 5 -> buildLibrary(content, settings);
			default -> content.child(Ui.dim("-"));
		}
		content.child(
			Containers.horizontalFlow(Sizing.fill(100), Sizing.fixed(10))
		);
		scroll = new AfrScroll(Sizing.fill(100), Sizing.expand(100), content);
		window.child(scroll);
		if (pendingScroll >= 0) {
			scroll.restore(pendingScroll);
			pendingScroll = -1;
		}

		FlowLayout footer = Containers.horizontalFlow(
			Sizing.fill(100),
			Sizing.content()
		);
		footer.surface(Ui.footer());
		footer.padding(Insets.of(5));
		footer.gap(6);
		footer.verticalAlignment(VerticalAlignment.CENTER);
		footer.child(
			new AfrButton(
				Component.literal(large ? "-" : "+"),
				AfrButton.Variant.ICON,
				() -> {
					large = !large;
					rebuild();
				}
			).tip(Lang.t("tip.window_size"))
		);
		footer.child(Ui.grow());
		footer.child(Ui.dim("v" + modVersion()));
		footer.child(
			btn(Lang.t("btn.close"), this::onClose).tip(
				Lang.t("tip.close_editor")
			)
		);
		footer.child(
			primary(Icons.CHECK + " " + Lang.t("btn.save"), this::save).tip(
				Lang.t("tip.save")
			)
		);
		window.child(footer);

		root.child(window);
	}

	private void buildAbout(
		FlowLayout content,
		ConfigManager config,
		RpProfile profile
	) {
		AfrClient.ensureAnnouncements(AfrRuntime.get());
		AfrRuntime runtime = AfrRuntime.get();

		int currentVersion = VersionChecker.currentVersionCode();
		if (homeUpdateChecked) {
			if (homeUpdateRemote > currentVersion) {
				FlowLayout updates = Ui.card(null);
				FlowLayout updRow = Ui.row();
				updRow.child(
					Components.label(
						Component.literal(
							Lang.t("lbl.update_available") +
								": v" +
								modVersion() +
								" (" +
								currentVersion +
								") -> v" +
								homeUpdateName +
								" (" +
								homeUpdateRemote +
								")"
						)
					).color(Color.ofArgb(Ui.WARN))
				);
				updRow.child(Ui.grow());
				updRow.child(
					new AfrButton(
						Component.literal(Icons.RELOAD),
						AfrButton.Variant.SECONDARY,
						() -> VersionScreen.open(parent)
					).tip(Lang.t("btn.check_updates"))
				);
				updRow.child(
					new AfrButton(
						Component.literal(Icons.CLOUD + " GitHub"),
						AfrButton.Variant.SECONDARY,
						() ->
							Util.getPlatform().openUri(
								URI.create(
									"https://github.com/Agzes/AFR/releases"
								)
							)
					)
				);
				updates.child(updRow);
				content.child(updates);
			}
		} else {
			AfrCloud.version("afr", "stable")
				.thenAccept(version -> {
					int remote = version.has("version_code")
						? version.get("version_code").getAsInt()
						: 0;
					String remoteName = version.has("version")
						? version.get("version").getAsString()
						: "";
					AfrCloud.runOnClient(() -> {
						homeUpdateChecked = true;
						homeUpdateRemote = remote;
						homeUpdateName = remoteName;
						if (remote > VersionChecker.currentVersionCode()) {
							rebuild();
						}
					});
				})
				.exceptionally(error -> null);
		}

		FlowLayout hero = Containers.verticalFlow(
			Sizing.fill(100),
			Sizing.content()
		);
		hero.surface(Ui.cardSurface());
		hero.padding(Insets.of(10));
		hero.gap(6);

		FlowLayout heroTop = Containers.horizontalFlow(
			Sizing.fill(100),
			Sizing.content()
		);
		heroTop.gap(4);
		heroTop.verticalAlignment(VerticalAlignment.CENTER);
		heroTop.child(
			Components.label(
				Component.literal("AFR").withStyle(ChatFormatting.BOLD)
			).color(Color.ofArgb(Ui.ACCENT))
		);
		heroTop.child(Ui.grow());
		heroTop.child(
			Ui.badge(Icons.WARN + " " + Lang.t("badge.early"), Ui.WARN).tooltip(
				Component.literal(Lang.t("badge.early.tip"))
			)
		);
		hero.child(heroTop);

		hero.child(
			Components.label(Component.literal(Lang.t("about.tagline"))).color(
				Color.ofArgb(Ui.DIM)
			)
		);

		FlowLayout heroLinks = Containers.horizontalFlow(
			Sizing.fill(100),
			Sizing.content()
		);
		heroLinks.gap(6);
		heroLinks.child(
			primary(Icons.LIBRARY + " " + Lang.t("sec.library"), () ->
				openSubScreen(new LibraryScreen(parent, null))
			)
		);
		heroLinks.child(
			btn(Icons.CLOUD + " GitHub", () ->
				Util.getPlatform().openUri(
					URI.create("https://github.com/Agzes/AFR")
				)
			)
		);
		heroLinks.child(
			btn(Icons.HOME + " " + Lang.t("btn.tutorial"), () ->
				openSubScreen(new TutorialScreen(parent, true))
			)
		);
		for (String[] extra : homeButtons) {
			heroLinks.child(
				new AfrButton(
					Component.literal(Icons.CLOUD + " " + extra[0]),
					AfrButton.Variant.SECONDARY,
					() -> Ui.openUrl(extra[1])
				)
			);
		}
		if (!homeButtonsFetched) {
			homeButtonsFetched = true;
			AfrCloud.productionArgs()
				.thenAccept(args -> {
					List<String[]> parsed = new ArrayList<>();
					if (
						!(
							args.has("buttons_enabled") &&
							!args.get("buttons_enabled").getAsBoolean()
						) &&
						args.has("buttons") &&
						args.get("buttons").isJsonArray()
					) {
						for (var element : args.getAsJsonArray("buttons")) {
							if (parsed.size() >= 2 || !element.isJsonObject()) {
								continue;
							}
							var object = element.getAsJsonObject();
							if (
								object.has("enabled") &&
								!object.get("enabled").getAsBoolean()
							) {
								continue;
							}
							String url = object.has("url")
								? object.get("url").getAsString()
								: object.has("link")
									? object.get("link").getAsString()
									: "";
							if (url == null || url.isBlank()) {
								continue;
							}
							String labelText = object.has("text")
								? JsonText.pick(object.get("text"), "Link")
								: object.has("label")
									? JsonText.pick(object.get("label"), "Link")
									: "Link";
							parsed.add(new String[] { labelText, url });
						}
					}
					AfrCloud.runOnClient(() -> {
						homeButtons.clear();
						homeButtons.addAll(parsed);
						if (
							minecraft.screen instanceof ConfigScreen editor &&
							editor.tab() == 0
						) {
							editor.rebuild();
						}
					});
				})
				.exceptionally(error -> null);
		}
		hero.child(heroLinks);
		content.child(hero);

		if (!runtime.announcements.isEmpty()) {
			FlowLayout ann = Ui.card(
				Icons.CLOUD + "  " + Lang.t("sec.announcements")
			);
			boolean firstAnn = true;
			for (JsonObject item : runtime.announcements) {
				if (!firstAnn) {
					ann.child(Ui.hline());
				}
				firstAnn = false;
				String title = JsonText.pick(item.get("title"), "AFR");
				String text = JsonText.pick(item.get("text"), "");
				String link =
					item.has("link") && !item.get("link").isJsonNull()
						? item.get("link").getAsString()
						: "";
				String button = JsonText.pick(item.get("button"), "");
				boolean deprecated =
					item.has("deprecated") &&
					item.get("deprecated").getAsBoolean();
				String severity = deprecated
					? "deprecated"
					: item.has("severity")
						? item.get("severity").getAsString().toLowerCase()
						: "info";
				ann.child(Ui.label(title));
				if (!"info".equals(severity)) {
					int color = "error".equals(severity) ? Ui.BAD : Ui.WARN;
					String tag = "deprecated".equals(severity)
						? Lang.t("ann.deprecated")
						: "error".equals(severity)
							? Lang.t("ann.critical")
							: Lang.t("ann.warning");
					ann.child(
						Components.label(
							Component.literal(Icons.WARN + " " + tag)
						).color(Color.ofArgb(color))
					);
				}
				if (!text.isBlank()) {
					ann.child(Ui.dim(text));
				}
				if (!link.isBlank() && !"None".equalsIgnoreCase(link)) {
					String labelText = button.isBlank()
						? Lang.t("btn.open_link")
						: button;
					ann.child(
						new AfrButton(
							Component.literal(Icons.CLOUD + " " + labelText),
							AfrButton.Variant.SECONDARY,
							() -> Ui.openUrl(link)
						)
					);
				}
			}
			content.child(ann);
		}

		if (runtime.seasonalActive()) {
			String seasonTitle = JsonText.pick(
				runtime.seasonalAnnouncement == null
					? null
					: runtime.seasonalAnnouncement.get("title"),
				Lang.t("msg.seasonal_title")
			);
			String seasonText = JsonText.pick(
				runtime.seasonalAnnouncement == null
					? null
					: runtime.seasonalAnnouncement.get("text"),
				""
			);
			String seasonButton = JsonText.pick(
				runtime.seasonalAnnouncement == null
					? null
					: runtime.seasonalAnnouncement.get("button"),
				Lang.t("msg.seasonal_button")
			);
			FlowLayout season = Ui.card(Icons.STAR + "  " + seasonTitle);
			if (!seasonText.isBlank()) {
				season.child(Ui.dim(seasonText));
			}
			boolean on = runtime.seasonalTheme.equals(config.settings().theme);
			season.child(
				new AfrButton(
					Component.literal(
						on
							? Icons.CHECK + " " + Lang.t("btn.theme_off")
							: Icons.STAR + " " + seasonButton
					),
					on
						? AfrButton.Variant.SECONDARY
						: AfrButton.Variant.PRIMARY,
					() -> {
						runtime.toggleSeasonal();
						rebuild();
					}
				)
			);
			content.child(season);
		}

		if (profile != null) {
			FlowLayout prof = Ui.card(
				Icons.PROFILES + "  " + Lang.t("sec.current_profile")
			);
			prof.child(
				settingRow(Lang.t("lbl.title"), Ui.label(profile.meta.label))
			);
			prof.child(
				settingRow(
					Lang.t("lbl.version"),
					Ui.dim(
						profile.meta.version +
							"  (" +
							Lang.t("lbl.code") +
							" " +
							profile.meta.versionCode +
							")"
					)
				)
			);
			prof.child(Ui.hline());

			int tileW = Math.max(56, (windowWidth - 32 - 18) / 4);
			FlowLayout stats = Containers.horizontalFlow(
				Sizing.fill(100),
				Sizing.content()
			);
			stats.gap(6);
			stats.horizontalAlignment(HorizontalAlignment.CENTER);
			stats.child(
				statTile(
					Lang.t("stat.actions"),
					profile.rp_actions.size(),
					Icons.ACTIONS,
					tileW
				)
			);
			stats.child(
				statTile(
					Lang.t("stat.windows"),
					profile.quick_action_windows.size(),
					Icons.GEAR,
					tileW
				)
			);
			stats.child(
				statTile(
					Lang.t("stat.params"),
					profile.parameters.size(),
					Icons.DIAMOND,
					tileW
				)
			);
			stats.child(
				statTile(
					Lang.t("stat.profiles"),
					config.profiles().size(),
					Icons.PROFILES,
					tileW
				)
			);
			prof.child(stats);
			content.child(prof);
		}

		FlowLayout info = Ui.card(Icons.HOME + "  " + Lang.t("sec.about"));
		info.child(featureRow(Icons.ACTIONS, Lang.t("feat.editor")));
		info.child(featureRow(Icons.LIBRARY, Lang.t("feat.library")));
		info.child(featureRow(Icons.PLAY, Lang.t("feat.menu")));
		info.child(featureRow(Icons.BINDS, Lang.t("feat.binds")));
		info.child(featureRow(Icons.CLOUD, Lang.t("feat.autochat")));
		info.child(featureRow(Icons.DIAMOND, Lang.t("feat.themes")));
		content.child(info);

		FlowLayout signature = Containers.horizontalFlow(
			Sizing.fill(100),
			Sizing.content()
		);
		signature.horizontalAlignment(HorizontalAlignment.CENTER);
		signature.verticalAlignment(VerticalAlignment.CENTER);
		signature.gap(2);
		signature.child(
			Components.label(Component.literal("with ")).color(
				Color.ofArgb(Ui.MUTED)
			)
		);
		signature.child(
			Components.label(Component.literal("\u2764")).color(
				Color.ofArgb(Ui.BAD)
			)
		);
		signature.child(
			Components.label(Component.literal(" by ")).color(
				Color.ofArgb(Ui.MUTED)
			)
		);
		signature.child(
			new AfrButton(
				Component.literal("Agzes"),
				AfrButton.Variant.GHOST,
				() -> Ui.openUrl("https://agzes.github.io")
			).tip("agzes.github.io")
		);
		content.child(signature);
	}

	private FlowLayout statTile(
		String caption,
		int value,
		String glyph,
		int width
	) {
		FlowLayout card = Containers.verticalFlow(
			Sizing.fixed(width),
			Sizing.content()
		);
		card.surface(Ui.field());
		card.padding(Insets.of(8));
		card.gap(3);
		card.horizontalAlignment(HorizontalAlignment.CENTER);
		card.verticalAlignment(VerticalAlignment.CENTER);
		card.child(
			Components.label(Component.literal(glyph)).color(
				Color.ofArgb(Ui.SECTION)
			)
		);
		card.child(
			Components.label(Component.literal(String.valueOf(value))).color(
				Color.ofArgb(Ui.ACCENT)
			)
		);
		card.child(
			Components.label(Component.literal(caption.toUpperCase())).color(
				Color.ofArgb(Ui.MUTED)
			)
		);
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
					TextFit.fit(
						Minecraft.getInstance().font,
						text,
						windowWidth - 70
					)
				)
			).color(Color.ofArgb(Ui.DIM))
		);
		row.child(Ui.grow());
		return row;
	}

	private void buildSettings(
		FlowLayout content,
		ConfigManager config,
		UserSettings settings,
		RpProfile profile
	) {
		FlowLayout person = Ui.card(Icons.EDIT + "  " + Lang.t("sec.person"));
		person.child(
			textRow(
				Lang.t("lbl.name"),
				settings.rpName,
				value -> settings.rpName = value
			)
		);
		person.child(
			textRow(
				Lang.t("lbl.role"),
				settings.rpRole,
				value -> settings.rpRole = value
			)
		);
		String[] gender = { settings.gender };
		final AfrButton[] genderRef = new AfrButton[1];
		AfrButton genderBtn = new AfrButton(
			Component.literal(genderLabel(gender[0])),
			AfrButton.Variant.SECONDARY,
			() -> {
				gender[0] = "female".equalsIgnoreCase(gender[0])
					? "male"
					: "female";
				genderRef[0].setText(Component.literal(genderLabel(gender[0])));
				markDirty();
			}
		);
		genderRef[0] = genderBtn;
		person.child(
			settingRow(Lang.t("lbl.gender"), genderBtn.w(controlWidth()))
		);
		savers.add(() -> settings.gender = gender[0]);
		content.child(person);

		FlowLayout ui = Ui.card(Icons.GEAR + "  " + Lang.t("sec.interface"));
		ui.child(
			checkRow(
				Lang.t("lbl.status_hud"),
				settings.showHud,
				value -> settings.showHud = value
			)
		);
		ui.child(
			checkRow(
				Lang.t("lbl.progress"),
				settings.showProgress,
				value -> settings.showProgress = value
			)
		);
		ui.child(
			checkRow(
				Lang.t("lbl.notifications"),
				settings.notifications,
				value -> settings.notifications = value
			)
		);
		ui.child(
			checkRow(
				Lang.t("lbl.sound"),
				settings.sound,
				value -> settings.sound = value
			)
		);
		ui.child(
			checkRow(Lang.t("lbl.log_actions"), settings.logActions, value -> {
				settings.logActions = value;
				ActionLogger.setEnabled(value);
			})
		);
		ui.child(
			checkRow(Lang.t("lbl.debug_logs"), settings.debugLogging, value -> {
				settings.debugLogging = value;
				AFR.setDebugLogging(value);
			})
		);
		ui.child(
			checkRow(
				Lang.t("lbl.profile_switch"),
				settings.showProfileSwitcher,
				value -> settings.showProfileSwitcher = value,
				Lang.t("tip.profile_switch")
			)
		);
		FlowLayout setupRow = Ui.row();
		setupRow.child(Ui.dim(Lang.t("setup.title")));
		setupRow.child(Ui.grow());
		setupRow.child(
			new AfrButton(
				Component.literal(Lang.t("setup.title")),
				AfrButton.Variant.SECONDARY,
				() -> openSubScreen(new SetupWizardScreen(this, 0))
			).tip(Lang.t("setup.hint"))
		);
		ui.child(setupRow);
		FlowLayout langRow = Ui.row();
		langRow.child(Ui.dim(Lang.t("lbl.language")));
		langRow.child(Ui.grow());
		langRow.child(
			cycleButton(
				new String[] { "ru", "en" },
				settings.language,
				controlWidth(),
				value -> {
					settings.language = value;
					Lang.setLanguage(value);
					markDirty();
					rebuild();
				}
			)
		);
		ui.child(langRow);
		FlowLayout themeRow = Ui.row();
		themeRow.child(Ui.dim(Lang.t("lbl.theme")));
		themeRow.child(Ui.grow());
		themeRow.child(themeButton(config, settings));
		themeRow.child(
			new AfrButton(
				Component.literal(Icons.EDIT + " " + Lang.t("btn.colors")),
				AfrButton.Variant.SECONDARY,
				() ->
					openSubScreen(new ThemeEditorScreen(this, config, settings))
			).tip(Lang.t("btn.colors"))
		);
		ui.child(themeRow);
		FlowLayout styleRow = Ui.row();
		styleRow.child(Ui.dim(Lang.t("lbl.menu_style")));
		styleRow.child(Ui.grow());
		styleRow.child(
			cycleButton(
				MENU_STYLES,
				settings.menuStyle,
				controlWidth() - 64,
				value -> {
					settings.menuStyle = value;
					markDirty();
					rebuild();
				}
			)
		);
		styleRow.child(
			new AfrButton(
				Component.literal(Icons.EDIT + " " + Lang.t("btn.preview")),
				AfrButton.Variant.SECONDARY,
				() ->
					openSubScreen(
						new StylePreviewScreen(this, settings.menuStyle)
					)
			).tip(Lang.t("tip.style_preview"))
		);
		ui.child(styleRow);
		content.child(ui);

		FlowLayout engine = Ui.card(
			Icons.PLAY + "  " + Lang.t("sec.production")
		);
		engine.child(
			checkRow(
				Lang.t("lbl.auto_stop_screen"),
				settings.autoStopScreen,
				value -> settings.autoStopScreen = value,
				Lang.t("tip.auto_stop_screen")
			)
		);
		engine.child(
			checkRow(
				Lang.t("lbl.auto_stop_death"),
				settings.autoStopDeath,
				value -> settings.autoStopDeath = value,
				Lang.t("tip.auto_stop_death")
			)
		);
		engine.child(
			checkRow(
				Lang.t("lbl.auto_stop_world"),
				settings.autoStopWorld,
				value -> settings.autoStopWorld = value,
				Lang.t("tip.auto_stop_world")
			)
		);
		engine.child(
			checkRow(
				Lang.t("lbl.resume"),
				settings.resumeLastAction,
				value -> settings.resumeLastAction = value,
				Lang.t("tip.resume")
			)
		);
		engine.child(
			checkRow(
				Lang.t("lbl.notify_start"),
				settings.notifyStart,
				value -> settings.notifyStart = value,
				Lang.t("tip.notify_start")
			)
		);
		engine.child(
			checkRow(
				Lang.t("lbl.notify_end"),
				settings.notifyEnd,
				value -> settings.notifyEnd = value,
				Lang.t("tip.notify_end")
			)
		);
		AfrInput cooldownInput = AfrInput.fixed(
			String.valueOf(settings.defaultCooldown),
			controlWidth()
		);
		cooldownInput.hint(Lang.t("unit.ms"));
		cooldownInput.onChanged(v -> markDirty());
		savers.add(
			() ->
				settings.defaultCooldown = (int) parseDouble(
					cooldownInput.value(),
					settings.defaultCooldown
				)
		);
		engine.child(
			settingRow(
				Lang.t("lbl.default_cooldown"),
				cooldownInput.component()
			).tooltip(Component.literal(Lang.t("tip.default_cooldown")))
		);
		content.child(engine);

		FlowLayout timing = Containers.verticalFlow(
			Sizing.fill(100),
			Sizing.content()
		);
		timing.surface(Ui.cardSurface());
		timing.padding(Insets.of(Ui.CARD_PAD));
		timing.gap(Ui.CARD_GAP);
		FlowLayout timingHead = Ui.row();
		timingHead.child(Ui.section(Icons.PLAY, Lang.t("sec.delays")));
		timingHead.child(Ui.grow());
		timingHead.child(
			Components.label(
				Component.literal(
					TextFit.fit(
						Minecraft.getInstance().font,
						Icons.WARN + " " + Lang.t("warn.spam"),
						windowWidth - 136
					)
				)
			).color(Color.ofArgb(Ui.WARN))
		);
		timing.child(timingHead);
		timing.child(Ui.hline());
		AfrInput multInput = AfrInput.fixed(
			String.valueOf(settings.timerMultiplier),
			controlWidth()
		);
		savers.add(
			() ->
				settings.timerMultiplier = parseDouble(
					multInput.value(),
					settings.timerMultiplier
				)
		);
		FlowLayout multRow = Ui.row();
		multRow.child(Ui.dim(Lang.t("lbl.multiplier")));
		multRow.child(Ui.grow());
		for (double factor : new double[] { 1.0, 1.25, 1.5, 1.75, 2.0 }) {
			multRow.child(
				new AfrButton(
					Component.literal("x" + factor),
					AfrButton.Variant.SECONDARY,
					() -> {
						settings.timerMultiplier = factor;
						multInput.setValue(formatFactor(factor));
						NotificationHud.notify(
							NotificationHud.Type.INFO,
							"AFR",
							Lang.t("notif.multiplier") + ": x" + factor
						);
					}
				)
			);
		}
		multRow.child(multInput.component());
		timing.child(multRow);
		if (profile != null) {
			var ps = profile.profile_settings;
			timing.child(
				valueRow(
					"S100, S250",
					joinInts(ps.s100_250),
					value -> ps.s100_250 = parseInts(value, ps.s100_250)
				)
			);
			timing.child(
				valueRow(
					"S300...S800",
					joinInts(ps.s300_800),
					value -> ps.s300_800 = parseInts(value, ps.s300_800)
				)
			);
			timing.child(
				valueRow(
					"S1000...S4000",
					joinInts(ps.s1000_4000),
					value -> ps.s1000_4000 = parseInts(value, ps.s1000_4000)
				)
			);
		}
		content.child(timing);
	}

	private void buildProfiles(
		FlowLayout content,
		ConfigManager config,
		RpProfile profile
	) {
		FlowLayout switcher = Ui.card(
			Icons.PROFILES + "  " + Lang.t("sec.profiles")
		);
		FlowLayout chips = Containers.horizontalFlow(
			Sizing.fill(100),
			Sizing.content()
		);
		chips.gap(4);
		for (String id : new ArrayList<>(config.profiles().keySet())) {
			RpProfile entry = config.profiles().get(id);
			AfrButton chip = new AfrButton(
				Component.literal(entry.meta.label),
				AfrButton.Variant.SECONDARY,
				() -> {
					if (id.equals(config.activeId())) {
						return;
					}
					runSavers();
					RpProfile previous = config.active();
					if (dirty() && previous != null && previous.isLibrary()) {
						previous.meta.modified = true;
					}
					config.saveActive();
					config.setActive(id);
					AfrRuntime.get().rebuildBinds();
					rebuild();
				}
			);
			if (id.equals(config.activeId())) {
				chip.active(true);
			}
			chips.child(chip);
		}
		switcher.child(chips);
		switcher.child(Ui.hline());

		AfrInput nameBox = AfrInput.expand("");
		nameBox.hint(Lang.t("lbl.profile_name"));
		FlowLayout createRow = Containers.horizontalFlow(
			Sizing.fill(100),
			Sizing.content()
		);
		createRow.gap(6);
		createRow.verticalAlignment(VerticalAlignment.CENTER);
		createRow.child(nameBox.component());
		createRow.child(
			primary(Icons.PLUS + " " + Lang.t("btn.create"), () ->
				createProfile(config, nameBox.value(), true)
			)
		);
		createRow.child(
			btn(Lang.t("btn.copy"), () ->
				createProfile(config, nameBox.value(), false)
			)
		);
		createRow.child(
			btn(Icons.EDIT + " " + Lang.t("btn.rename"), () ->
				renameProfile(config, profile, nameBox.value())
			).tip(Lang.t("tip.rename"))
		);
		switcher.child(createRow);

		FlowLayout io = Containers.horizontalFlow(
			Sizing.fill(100),
			Sizing.content()
		);
		io.gap(6);
		io.child(
			btn(Icons.UP + " " + Lang.t("btn.export"), () -> {
				try {
					Files.createDirectories(ConfigPaths.exportsDir());
					Path target = ConfigPaths.exportsDir().resolve(
						config.activeId() + ".json"
					);
					ConfigCodec.write(target, config.active());
					NotificationHud.notify(
						NotificationHud.Type.SUCCESS,
						"AFR",
						Lang.t("msg.export") + ": " + target.getFileName()
					);
				} catch (IOException e) {
					AFR.LOGGER.error("Profile export failed", e);
					NotificationHud.notify(
						NotificationHud.Type.ERROR,
						"AFR",
						Lang.t("msg.export_error")
					);
				}
			}).tip(Lang.t("btn.export"))
		);
		io.child(
			btn(Icons.DOWN + " " + Lang.t("btn.import"), () ->
				openSubScreen(
					new ImportScreen(
						ImportScreen.Kind.PROFILE,
						config,
						null,
						() -> returnFrom(this, 2, null, null)
					)
				)
			).tip(Lang.t("tip.import_clipboard"))
		);
		io.child(
			btn(Icons.EDIT + " " + Lang.t("btn.clipboard"), () -> {
				Minecraft.getInstance().keyboardHandler.setClipboard(
					ConfigCodec.GSON.toJson(config.active())
				);
				NotificationHud.notify(
					NotificationHud.Type.SUCCESS,
					"AFR",
					Lang.t("msg.copied")
				);
			}).tip(Lang.t("btn.clipboard"))
		);
		io.child(
			danger(Icons.DELETE + " " + Lang.t("btn.delete"), () ->
				confirm(Lang.t("msg.confirm_delete_profile"), () -> {
					if (config.profiles().size() <= 1) {
						NotificationHud.notify(
							NotificationHud.Type.WARNING,
							"AFR",
							Lang.t("msg.cannot_delete_last")
						);
						return;
					}
					String id = config.activeId();
					config.profiles().remove(id);
					try {
						Files.deleteIfExists(ConfigPaths.profile(id));
					} catch (IOException e) {
						AFR.LOGGER.warn("Failed to delete the profile file", e);
					}
					config.ensureProfile();
					AfrRuntime.get().rebuildBinds();
					rebuild();
				})
			)
		);
		switcher.child(io);
		content.child(switcher);

		if (profile == null) {
			return;
		}

		FlowLayout meta = Ui.card(
			Icons.PROFILES +
				"  " +
				Lang.t("lbl.metadata") +
				(profile.isLibrary() ? " • " + Lang.t("lbl.from_library") : "")
		);
		if (profile.isLibrary()) {
			meta.child(
				readOnlyRow(Lang.t("lbl.authors"), profile.meta.authorsText())
			);
			if (!profile.meta.editorsText().isBlank()) {
				meta.child(
					readOnlyRow(
						Lang.t("lbl.editors"),
						profile.meta.editorsText()
					)
				);
			}
			meta.child(
				readOnlyRow(
					Lang.t("lbl.profile_desc"),
					profile.meta.description
				)
			);
			meta.child(
				readOnlyRow(
					Lang.t("lbl.tags"),
					String.join(", ", profile.meta.tags)
				)
			);
			meta.child(
				readOnlyRow(
					Lang.t("lbl.version"),
					profile.meta.version +
						" (" +
						Lang.t("lbl.code") +
						" " +
						profile.meta.versionCode +
						")"
				)
			);
			meta.child(Ui.dim(Lang.t("msg.metadata_locked")));
			FlowLayout metaActions = Ui.row();
			metaActions.child(
				btn(Icons.PLUS + " " + Lang.t("btn.fork"), () ->
					forkProfile(config, profile)
				).tip(Lang.t("tip.fork"))
			);
			metaActions.child(
				btn(Icons.RELOAD + " " + Lang.t("btn.update_library"), () -> {
					runSavers();
					AfrRuntime.get().openConfigUpdate(this);
				}).tip(Lang.t("tip.diff"))
			);
			metaActions.child(
				btn(Icons.LIBRARY + " " + Lang.t("btn.history"), () ->
					openSubScreen(new VersionHistoryScreen(this, profile))
				).tip(Lang.t("tip.history"))
			);
			meta.child(metaActions);
		} else {
			meta.child(
				textRowFill(
					Lang.t("lbl.authors"),
					profile.meta.authorsText(),
					value -> {
						profile.meta.authors = splitList(value);
						profile.meta.author = profile.meta.authors.isEmpty()
							? ""
							: profile.meta.authors.get(0);
					}
				)
			);
			meta.child(
				textRowFill(
					Lang.t("lbl.editors"),
					profile.meta.editorsText(),
					value -> profile.meta.editors = splitList(value)
				)
			);
			meta.child(
				textRowFill(
					Lang.t("lbl.profile_desc"),
					profile.meta.description,
					value -> profile.meta.description = value
				)
			);
			meta.child(
				textRowFill(
					Lang.t("lbl.tags"),
					String.join(", ", profile.meta.tags),
					value -> profile.meta.tags = splitList(value)
				)
			);
			FlowLayout metaActions = Ui.row();
			metaActions.child(
				btn(Icons.LIBRARY + " " + Lang.t("btn.history"), () ->
					openSubScreen(new VersionHistoryScreen(this, profile))
				).tip(Lang.t("tip.history"))
			);
			meta.child(metaActions);
		}
		content.child(meta);

		content.child(parametersCard(config, profile));
		windowsHost = Containers.verticalFlow(
			Sizing.fill(100),
			Sizing.content()
		);
		windowsHost.gap(Ui.GAP);
		content.child(windowsHost);
		populateWindows(config, profile);
	}

	private void renameProfile(
		ConfigManager config,
		RpProfile profile,
		String newLabel
	) {
		if (profile == null) {
			return;
		}
		if (config.renameProfile(profile, newLabel)) {
			AfrRuntime.get().rebuildBinds();
			NotificationHud.notify(
				NotificationHud.Type.SUCCESS,
				"AFR",
				Lang.t("msg.renamed") + ": " + profile.meta.label
			);
			rebuild();
		} else {
			NotificationHud.notify(
				NotificationHud.Type.WARNING,
				"AFR",
				Lang.t("msg.rename_empty")
			);
		}
	}

	private void createProfile(
		ConfigManager config,
		String name,
		boolean empty
	) {
		String label = name == null ? "" : name.strip();
		if (label.isBlank()) {
			label = "profile_" + (config.profiles().size() + 1);
		}
		String base = label
			.toLowerCase(Locale.ROOT)
			.replaceAll("[^a-z0-9_-]+", "-")
			.replaceAll("(^-+|-+$)", "");
		if (base.isBlank()) {
			base = "profile";
		}
		String revName = base;
		int index = 2;
		while (config.profiles().containsKey(revName)) {
			revName = base + "-" + index++;
		}
		RpProfile profile;
		if (empty) {
			profile = new RpProfile();
			RpAction action = new RpAction();
			action.name = "new_action";
			action.steps.add(
				new ArrayList<>(List.of("RPAction", "Send", "S300", "S300", ""))
			);
			profile.rp_actions.put("new_action", action);
		} else {
			profile = ConfigCodec.GSON.fromJson(
				ConfigCodec.GSON.toJson(config.active()),
				RpProfile.class
			);
		}
		if (profile.meta == null) {
			profile.meta = new ProfileMeta();
		}
		profile.meta.revName = revName;
		profile.meta.label = label;
		profile.meta.source = "local";
		profile.meta.remoteId = "";
		profile.meta.remoteVersionCode = 0;
		profile.meta.modified = false;
		config.putProfile(profile, true);
		AfrRuntime.get().rebuildBinds();
		rebuild();
	}

	private FlowLayout parametersCard(ConfigManager config, RpProfile profile) {
		FlowLayout card = Ui.card(Icons.GEAR + "  " + Lang.t("sec.params"));
		FlowLayout head = Containers.horizontalFlow(
			Sizing.fill(100),
			Sizing.content()
		);
		head.gap(6);
		head.verticalAlignment(VerticalAlignment.CENTER);
		head.child(
			Ui.dim(profile.parameters.size() + " " + Lang.t("lbl.params_hint"))
		);
		head.child(Ui.grow());
		head.child(
			new AfrButton(
				Component.literal(
					paramsEdit
						? Icons.CHECK + " " + Lang.t("btn.done")
						: Icons.EDIT + " " + Lang.t("btn.edit")
				),
				AfrButton.Variant.SECONDARY,
				() -> {
					paramsEdit = !paramsEdit;
					rebuild();
				}
			)
		);
		card.child(head);

		if (!paramsEdit) {
			if (profile.parameters.isEmpty()) {
				card.child(Ui.dim(Lang.t("msg.no_params")));
			}
			for (Parameter parameter : profile.parameters) {
				if (parameter.isCheckbox()) {
					card.child(
						checkRow(
							parameter.label,
							parameter.asBool(),
							value -> parameter.value = value
						)
					);
				} else {
					card.child(
						textRow(
							parameter.label,
							parameter.asText(),
							value -> parameter.value = value
						)
					);
				}
			}
			return card;
		}

		for (int i = 0; i < profile.parameters.size(); i++) {
			Parameter parameter = profile.parameters.get(i);
			final int index = i;
			FlowLayout row = Ui.row();
			row.gap(4);
			AfrInput id = AfrInput.fixed(parameter.id, 110);
			AfrInput label = AfrInput.fixed(parameter.label, 130);
			AfrButton type = cycleButton(
				PARAM_TYPES,
				parameter.paramType,
				84,
				value -> parameter.paramType = value
			);
			AfrInput value = AfrInput.fixed(parameter.asText(), 90);
			id.onChanged(v -> markDirty());
			label.onChanged(v -> markDirty());
			value.onChanged(v -> markDirty());
			savers.add(() -> {
				parameter.id = id.value().strip();
				parameter.label = label.value();
				parameter.value = value.value();
			});
			row.child(id.component());
			row.child(label.component());
			row.child(type);
			row.child(value.component());
			row.child(
				icon(Icons.DELETE, () ->
					confirm(Lang.t("msg.confirm_delete_param"), () -> {
						profile.parameters.remove(index);
						markDirty();
						rebuild();
					})
				)
			);
			card.child(row);
		}
		card.child(
			primary(Icons.PLUS + " " + Lang.t("btn.param"), () -> {
				Parameter parameter = new Parameter();
				parameter.id = "param_" + (profile.parameters.size() + 1);
				parameter.label = Lang.t("lbl.new_param");
				parameter.value = "";
				profile.parameters.add(parameter);
				markDirty();
				rebuild();
			})
		);
		return card;
	}

	private void populateWindows(ConfigManager config, RpProfile profile) {
		if (windowsHost == null) {
			return;
		}
		double offset = scroll != null ? scroll.getOffset() : -1;
		runSavers();
		if (windowSaverFrom >= 0 && windowSaverFrom <= savers.size()) {
			savers.subList(windowSaverFrom, savers.size()).clear();
		}
		windowSaverFrom = savers.size();
		windowsHost.clearChildren();
		FlowLayout card = Ui.card(Icons.ACTIONS + "  " + Lang.t("sec.windows"));
		FlowLayout chips = Containers.horizontalFlow(
			Sizing.fill(100),
			Sizing.content()
		);
		chips.gap(4);
		for (QuickActionWindow window : profile.quick_action_windows) {
			AfrButton chip = new AfrButton(
				Component.literal(window.name),
				AfrButton.Variant.SECONDARY,
				() -> {
					selectedWindow = window.id;
					populateWindows(config, profile);
				}
			);
			chip.active(window.id.equals(selectedWindow));
			chips.child(chip);
		}
		chips.child(
			primary(Icons.PLUS + " " + Lang.t("btn.window"), () -> {
				QuickActionWindow window = new QuickActionWindow();
				window.id =
					"window_" + (profile.quick_action_windows.size() + 1);
				window.name =
					Lang.t("lbl.window_n") +
					(profile.quick_action_windows.size() + 1);
				profile.quick_action_windows.add(window);
				markDirty();
				AfrRuntime.get().rebuildBinds();
				selectedWindow = window.id;
				populateWindows(config, profile);
			})
		);
		card.child(chips);

		if (selectedWindow == null) {
			card.child(Ui.dim(Lang.t("msg.choose_window")));
			windowsHost.child(card);
			restoreScroll(offset);
			return;
		}
		QuickActionWindow window = profile.window(selectedWindow);
		if (window == null) {
			windowsHost.child(card);
			restoreScroll(offset);
			return;
		}
		card.child(Ui.hline());
		FlowLayout edit = Ui.row();
		edit.gap(4);
		AfrInput name = AfrInput.fixed(window.name, 150);
		name.onChanged(value -> markDirty());
		savers.add(() -> window.name = name.value());
		edit.child(name.component());
		edit.child(
			cycleButton(WINDOW_STYLES, window.style, 90, value -> {
				window.style = value;
				markDirty();
				populateWindows(config, profile);
			})
		);
		edit.child(
			danger(Icons.DELETE + " " + Lang.t("btn.add_window"), () ->
				confirm(Lang.t("msg.confirm_delete_window"), () -> {
					profile.quick_action_windows.remove(window);
					markDirty();
					AfrRuntime.get().rebuildBinds();
					selectedWindow = null;
					populateWindows(config, profile);
				})
			)
		);
		card.child(edit);
		if ("tiles".equalsIgnoreCase(window.style)) {
			card.child(Ui.wrap(Lang.t("tip.tiles"), windowWidth - 40));
			card.child(tileEditor(config, profile, window));
		} else {
			card.child(Ui.dim(Lang.t("lbl.content_click")));
			FlowLayout members = Containers.horizontalFlow(
				Sizing.content(),
				Sizing.content()
			);
			members.gap(3);
			List<ActionRef> actions = profile.actionRefs();
			actions.sort((a, b) ->
				a.displayName().compareToIgnoreCase(b.displayName())
			);
			for (ActionRef ref : actions) {
				final AfrButton[] chipRef = new AfrButton[1];
				AfrButton member = new AfrButton(
					Component.literal(ref.displayName()),
					AfrButton.Variant.SECONDARY,
					() -> {
						boolean included = window.actions.contains(ref.id());
						if (included) {
							window.actions.remove(ref.id());
						} else {
							window.actions.add(ref.id());
						}
						markDirty();
						chipRef[0].active(!included);
					}
				);
				member.active(window.actions.contains(ref.id()));
				chipRef[0] = member;
				members.child(member);
			}
			card.child(
				Containers.horizontalScroll(
					Sizing.fill(100),
					Sizing.fixed(18),
					members
				)
			);
		}
		windowsHost.child(card);
		restoreScroll(offset);
	}

	private void restoreScroll(double offset) {
		if (scroll != null && offset >= 0) {
			scroll.restore(offset);
		}
	}

	private FlowLayout tileEditor(
		ConfigManager config,
		RpProfile profile,
		QuickActionWindow window
	) {
		int cols = Math.max(1, window.tileCols);
		if (window.tiles.isEmpty() && !window.actions.isEmpty()) {
			window.tiles.addAll(window.actions);
		}
		while (window.tiles.size() < cols * 2) {
			window.tiles.add("");
		}
		while (window.tiles.size() % cols != 0) {
			window.tiles.add("");
		}
		FlowLayout grid = Containers.verticalFlow(
			Sizing.fill(100),
			Sizing.content()
		);
		grid.gap(2);
		int rows = (window.tiles.size() + cols - 1) / cols;
		for (int r = 0; r < rows; r++) {
			FlowLayout row = Containers.horizontalFlow(
				Sizing.content(),
				Sizing.content()
			);
			row.gap(2);
			for (int c = 0; c < cols; c++) {
				int index = r * cols + c;
				if (index >= window.tiles.size()) {
					break;
				}
				String current = window.tiles.get(index);
				RpAction action =
					current == null || current.isBlank()
						? null
						: profile.action(current);
				AfrTile tile = new AfrTile(
					action != null
						? Items.fromId(action.icon)
						: ItemStack.EMPTY,
					action != null ? action.displayName() : "-"
				);
				if (action != null) {
					tile.tooltip(Component.literal(action.displayName()));
				}
				tile.onPress(() -> {
					pendingScroll = scroll != null ? scroll.getOffset() : -1;
					openSubScreen(
						new ActionPickerScreen(
							this,
							config,
							profile,
							window,
							index,
							selectedAction
						)
					);
				});
				row.child(tile);
			}
			grid.child(row);
		}
		FlowLayout controls = Ui.row();
		controls.child(
			btn(Lang.t("btn.row"), () -> {
				for (int i = 0; i < cols; i++) {
					window.tiles.add("");
				}
				markDirty();
				populateWindows(config, profile);
			})
		);
		controls.child(
			btn(Lang.t("btn.col"), () -> {
				window.tileCols = cols + 1;
				while (window.tiles.size() % window.tileCols != 0) {
					window.tiles.add("");
				}
				markDirty();
				populateWindows(config, profile);
			})
		);
		FlowLayout box = Containers.verticalFlow(
			Sizing.fill(100),
			Sizing.content()
		);
		box.gap(4);
		box.child(grid);
		box.child(controls);
		return box;
	}

	private void buildActions(
		FlowLayout content,
		ConfigManager config,
		RpProfile profile
	) {
		if (profile == null) {
			content.child(Ui.dim(Lang.t("msg.no_active_profile")));
			return;
		}
		if (
			selectedAction != null &&
			!profile.rp_actions.containsKey(selectedAction)
		) {
			selectedAction = null;
		}
		itemRefs.clear();

		if (profile.isLibrary()) {
			FlowLayout note = Ui.card(
				Icons.WARN + "  " + Lang.t("sec.from_library")
			);
			note.child(
				Ui.wrap(
					Lang.t(
						profile.meta.modified
							? "msg.library_note_modified"
							: "msg.library_note"
					),
					windowWidth - 40
				)
			);
			content.child(note);
		}

		FlowLayout listCard = Containers.verticalFlow(
			Sizing.fill(100),
			Sizing.content()
		);
		listCard.surface(Ui.cardSurface());
		listCard.padding(Insets.of(Ui.CARD_PAD));
		listCard.gap(Ui.CARD_GAP);
		FlowLayout listHead = Containers.horizontalFlow(
			Sizing.fill(100),
			Sizing.content()
		);
		listHead.gap(6);
		listHead.verticalAlignment(VerticalAlignment.CENTER);
		listHead.child(
			Ui.section(
				Icons.ACTIONS,
				Lang.t("stat.actions") + " • " + profile.rp_actions.size()
			)
		);
		listHead.child(Ui.grow());
		listHead.child(
			new AfrButton(
				Component.literal(Icons.ACTIONS + " AutoChat"),
				AfrButton.Variant.SECONDARY,
				() -> openSubScreen(new AutoChatScreen(this, config, profile))
			).tip(Lang.t("tip.autochat"))
		);
		listHead.child(
			new AfrButton(
				Component.literal(Icons.CLOUD + " " + Lang.t("btn.import")),
				AfrButton.Variant.SECONDARY,
				() -> openSubScreen(new ImportActionScreen(this, profile))
			).tip(Lang.t("btn.import"))
		);
		listHead.child(
			new AfrButton(
				Component.literal(Icons.CLOUD + " QuickMenu"),
				AfrButton.Variant.SECONDARY,
				() -> openSubScreen(new QuickMenuImportScreen(this, profile))
			).tip(Lang.t("tip.quickmenu"))
		);
		listHead.child(
			primary(Icons.PLUS + " " + Lang.t("btn.new_action"), () -> {
				String id = "action_" + (System.currentTimeMillis() % 100000);
				RpAction action = new RpAction();
				action.name = id;
				action.steps.add(
					new ArrayList<>(
						List.of("RPAction", "Send", "S300", "S300", "")
					)
				);
				profile.rp_actions.put(id, action);
				markDirty();
				selectedAction = id;
				rebuild();
			})
		);
		listCard.child(listHead);
		listCard.child(Ui.hline());

		FlowLayout chips = Containers.horizontalFlow(
			Sizing.content(),
			Sizing.content()
		);
		chips.gap(3);
		List<String> ids = new ArrayList<>(profile.rp_actions.keySet());
		ids.sort(String::compareToIgnoreCase);
		for (String id : ids) {
			RpAction action = profile.rp_actions.get(id);
			AfrButton chip = new AfrButton(
				Component.literal(action.displayName()),
				AfrButton.Variant.SECONDARY,
				() -> selectAction(id, config, profile)
			);
			chip.active(id.equals(selectedAction));
			itemRefs.put(id, chip);
			chips.child(chip);
		}
		listCard.child(
			Containers.horizontalScroll(
				Sizing.fill(100),
				Sizing.fixed(18),
				chips
			)
		);
		content.child(listCard);
		FlowLayout validation = validationCard(profile);
		if (validation != null) {
			content.child(validation);
		}

		editorHost = Containers.verticalFlow(
			Sizing.fill(100),
			Sizing.content()
		);
		editorHost.gap(Ui.GAP);
		content.child(editorHost);
		populateEditor(config, profile);
	}

	private void selectAction(
		String id,
		ConfigManager config,
		RpProfile profile
	) {
		selectedAction = id;
		for (Map.Entry<String, AfrButton> entry : itemRefs.entrySet()) {
			entry.getValue().active(entry.getKey().equals(id));
		}
		populateEditor(config, profile);
	}

	private void populateEditor(ConfigManager config, RpProfile profile) {
		if (editorHost == null || profile == null) {
			return;
		}
		double offset = scroll != null ? scroll.getOffset() : -1;
		runSavers();
		if (editorSaverFrom >= 0 && editorSaverFrom <= savers.size()) {
			savers.subList(editorSaverFrom, savers.size()).clear();
		}
		editorSaverFrom = savers.size();
		editorHost.clearChildren();
		if (
			selectedAction == null ||
			!profile.rp_actions.containsKey(selectedAction)
		) {
			editorHost.child(Ui.dim(Lang.t("msg.choose_action")));
			restoreScroll(offset);
			return;
		}
		RpAction action = profile.rp_actions.get(selectedAction);
		FlowLayout editor = Ui.card(Icons.EDIT + "  " + action.displayName());
		editor.child(
			textRowFill(
				Lang.t("lbl.title"),
				action.displayName(),
				value -> action.name = value
			)
		);
		editor.child(
			textRowFill(
				Lang.t("lbl.description"),
				action.description,
				value -> action.description = value
			)
		);
		AfrInput iconInput = AfrInput.fixed(
			action.icon == null ? "" : action.icon,
			controlWidth()
		);
		savers.add(() -> action.icon = iconInput.value().strip());
		FlowLayout iconRow = Ui.row();
		iconRow.gap(6);
		iconRow.verticalAlignment(VerticalAlignment.CENTER);
		iconRow.child(Ui.dim(Lang.t("lbl.icon")));
		FlowLayout iconPreviewHost = Containers.horizontalFlow(
			Sizing.fixed(22),
			Sizing.fixed(22)
		);
		iconPreviewHost.surface(Ui.field());
		iconPreviewHost.horizontalAlignment(HorizontalAlignment.CENTER);
		iconPreviewHost.verticalAlignment(VerticalAlignment.CENTER);
		ItemStack iconPreview = itemFromId(action.icon);
		if (!iconPreview.isEmpty()) {
			iconPreviewHost.child(Components.item(iconPreview));
		}
		iconInput.onChanged(value -> {
			markDirty();
			iconPreviewHost.clearChildren();
			ItemStack stack = itemFromId(value == null ? "" : value.strip());
			if (!stack.isEmpty()) {
				iconPreviewHost.child(Components.item(stack));
			}
		});
		iconRow.child(Ui.grow());
		iconRow.child(iconInput.component());
		iconRow.child(iconPreviewHost);
		iconRow.child(
			new AfrButton(
				Component.literal(Icons.EDIT + " " + Lang.t("btn.choose")),
				AfrButton.Variant.SECONDARY,
				() ->
					openSubScreen(
						new IconPickerScreen(
							this,
							action,
							selectedAction,
							config
						)
					)
			)
		);
		editor.child(iconRow);
		editor.child(
			textRowFill(
				Lang.t("lbl.category"),
				action.category,
				value -> action.category = value
			).tooltip(Component.literal(Lang.t("tip.category")))
		);
		AfrInput cooldownInput = AfrInput.fixed(
			String.valueOf(action.cooldown),
			controlWidth()
		);
		cooldownInput.hint(Lang.t("unit.ms"));
		cooldownInput.onChanged(value -> markDirty());
		savers.add(
			() ->
				action.cooldown = (int) parseDouble(
					cooldownInput.value(),
					action.cooldown
				)
		);
		editor.child(
			settingRow(
				Lang.t("lbl.cooldown"),
				cooldownInput.component()
			).tooltip(Component.literal(Lang.t("tip.cooldown")))
		);
		FlowLayout controls = Containers.horizontalFlow(
			Sizing.fill(100),
			Sizing.content()
		);
		controls.gap(6);
		final AfrButton[] favRef = new AfrButton[1];
		AfrButton fav = new AfrButton(
			Component.literal(
				action.favorite
					? Icons.STAR + " " + Lang.t("btn.favorited")
					: Icons.EMPTY_STAR + " " + Lang.t("btn.favorite")
			),
			AfrButton.Variant.SECONDARY,
			() -> {
				action.favorite = !action.favorite;
				markDirty();
				favRef[0].setText(
					Component.literal(
						action.favorite
							? Icons.STAR + " " + Lang.t("btn.favorited")
							: Icons.EMPTY_STAR + " " + Lang.t("btn.favorite")
					)
				);
			}
		);
		favRef[0] = fav;
		controls.child(fav);
		controls.child(
			primary(Icons.PLUS + " " + Lang.t("btn.step"), () ->
				addStep(config, profile, action, "")
			)
		);
		controls.child(
			btn(Icons.PLUS + " " + Lang.t("btn.duplicate"), () ->
				duplicateAction(config, profile, action)
			).tip(Lang.t("btn.duplicate"))
		);
		controls.child(
			btn(Icons.CLOUD + " " + Lang.t("btn.export_action"), () ->
				exportAction(action)
			).tip(Lang.t("btn.export_action"))
		);
		controls.child(
			danger(Icons.DELETE + " " + Lang.t("btn.delete"), () ->
				confirm(Lang.t("msg.confirm_delete_action"), () -> {
					profile.rp_actions.remove(selectedAction);
					markDirty();
					selectedAction = null;
					rebuild();
				})
			)
		);
		editor.child(controls);
		editorHost.child(editor);

		if (!profile.quick_action_windows.isEmpty()) {
			FlowLayout windows = Ui.card(
				Icons.ACTIONS + "  " + Lang.t("sec.keep_windows")
			);
			FlowLayout chips = Containers.horizontalFlow(
				Sizing.content(),
				Sizing.content()
			);
			chips.gap(4);
			for (QuickActionWindow window : profile.quick_action_windows) {
				final AfrButton[] chipRef = new AfrButton[1];
				AfrButton chip = new AfrButton(
					Component.literal(window.name),
					AfrButton.Variant.SECONDARY,
					() -> {
						boolean included = window.actions.contains(
							selectedAction
						);
						if (included) {
							window.actions.remove(selectedAction);
						} else {
							window.actions.add(selectedAction);
						}
						markDirty();
						chipRef[0].active(!included);
					}
				);
				chip.active(window.actions.contains(selectedAction));
				chipRef[0] = chip;
				chips.child(chip);
			}
			windows.child(
				Containers.horizontalScroll(
					Sizing.fill(100),
					Sizing.fixed(18),
					chips
				)
			);
			windows.child(
				new AfrButton(
					Component.literal(
						Icons.GEAR + " " + Lang.t("btn.window_settings")
					),
					AfrButton.Variant.SECONDARY,
					() -> {
						selectedWindow = null;
						tab = 2;
						rebuild();
					}
				).tip(Lang.t("btn.window_settings"))
			);
			editorHost.child(windows);
		}

		for (int i = 0; i < action.steps.size(); i++) {
			editorHost.child(stepCard(config, profile, action, i));
		}
		editorHost.child(placeholdersCard());
		restoreScroll(offset);
	}

	private FlowLayout placeholdersCard() {
		FlowLayout card = Ui.card(
			Icons.EDIT + "  " + Lang.t("sec.placeholders")
		);
		card.child(Ui.dim(Lang.t("hint.placeholders")));
		String[][] rows = {
			{ "{Name}", Lang.t("ph.name") },
			{ "{FirstName}", Lang.t("ph.firstname") },
			{ "{LastName}", Lang.t("ph.lastname") },
			{ "{Role}", Lang.t("ph.role") },
			{ "{Param:id}", Lang.t("ph.param") },
			{ "{A|B|C}", Lang.t("ph.random") },
			{ "{Left} {Right} {Up} {Down}", Lang.t("ph.arrows") },
			{ "{Tab} {Space}", Lang.t("ph.special") },
		};
		for (String[] row : rows) {
			FlowLayout line = Ui.row();
			line.child(
				Components.label(Component.literal(row[0])).color(
					Color.ofArgb(Ui.SECTION)
				)
			);
			line.child(Ui.grow());
			line.child(
				Components.label(
					Component.literal(
						TextFit.fit(
							Minecraft.getInstance().font,
							row[1],
							windowWidth / 2
						)
					)
				).color(Color.ofArgb(Ui.DIM))
			);
			card.child(line);
		}
		return card;
	}

	private FlowLayout stepCard(
		ConfigManager config,
		RpProfile profile,
		RpAction action,
		int index
	) {
		List<String> raw = action.steps.get(index);
		if (!(raw instanceof ArrayList)) {
			raw = new ArrayList<>(raw);
			action.steps.set(index, raw);
		}
		final List<String> step = raw;
		while (step.size() < 6) {
			step.add("");
		}
		boolean gender = "RPActionGender".equalsIgnoreCase(step.get(0));

		FlowLayout box = Ui.card(
			Icons.ACTIONS + "  " + Lang.t("lbl.step") + " " + (index + 1)
		);
		FlowLayout head = Containers.horizontalFlow(
			Sizing.fill(100),
			Sizing.content()
		);
		head.gap(4);
		head.verticalAlignment(VerticalAlignment.CENTER);
		head.child(
			cycleButton(STEP_TYPES, step.get(0), 150, value -> {
				step.set(0, value);
				markDirty();
				populateEditor(config, profile);
			}).tip(Lang.t("tip.step_type"))
		);
		head.child(
			cycleButton(STEP_METHODS, step.get(1), 78, value ->
				step.set(1, value)
			).tip(Lang.t("tip.step_method"))
		);
		head.child(Ui.dim(Lang.t("lbl.before")));
		head.child(
			cycleButton(S_CODES, step.get(2), 56, value ->
				step.set(2, value)
			).tip(Lang.t("tip.step_before"))
		);
		head.child(Ui.dim(Lang.t("lbl.after")));
		head.child(
			cycleButton(S_CODES, step.get(3), 56, value ->
				step.set(3, value)
			).tip(Lang.t("tip.step_after"))
		);
		head.child(Ui.grow());
		head.child(
			icon(Icons.UP, () -> {
				if (index > 0) {
					Collections.swap(action.steps, index, index - 1);
					markDirty();
					populateEditor(config, profile);
				}
			}).tip(Lang.t("tip.step_up"))
		);
		head.child(
			icon(Icons.DOWN, () -> {
				if (index < action.steps.size() - 1) {
					Collections.swap(action.steps, index, index + 1);
					markDirty();
					populateEditor(config, profile);
				}
			}).tip(Lang.t("tip.step_down"))
		);
		head.child(
			icon(Icons.DELETE, () ->
				confirm(Lang.t("msg.confirm_delete_step"), () -> {
					action.steps.remove(index);
					markDirty();
					populateEditor(config, profile);
				})
			).tip(Lang.t("btn.delete"))
		);
		box.child(head);

		AfrInput text = AfrInput.fill(step.get(4)).hint(
			gender ? Lang.t("lbl.text_male") : Lang.t("lbl.text")
		);
		text.onChanged(value -> markDirty());
		savers.add(() -> step.set(4, text.value()));
		box.child(text.component());

		if (gender) {
			AfrInput female = AfrInput.fill(step.get(5)).hint(
				Lang.t("lbl.text_female_hint")
			);
			female.onChanged(value -> markDirty());
			savers.add(() -> step.set(5, female.value()));
			box.child(female.component());
		}
		return box;
	}

	private static void addBind(
		Map<String, List<String>> map,
		String bind,
		String label
	) {
		KeyCombo combo = KeyCombo.parse(bind);
		if (combo != null) {
			map.computeIfAbsent(combo.display(), k -> new ArrayList<>()).add(
				label
			);
		}
	}

	private void buildBinds(
		FlowLayout content,
		ConfigManager config,
		UserSettings settings,
		RpProfile profile
	) {
		if (profile == null) {
			content.child(Ui.dim(Lang.t("msg.no_active_profile")));
			return;
		}
		if (capturing != null) {
			content.child(
				Components.label(
					Component.literal(
						Lang.t("msg.press_key") +
							capturing +
							Lang.t("msg.press_key_tail")
					)
				).color(Color.ofArgb(Ui.WARN))
			);
		}

		Map<String, List<String>> byCombo = new LinkedHashMap<>();
		for (var entry : profile.rp_actions.entrySet()) {
			addBind(
				byCombo,
				config.resolveBind(entry.getKey(), entry.getValue().bind),
				entry.getValue().displayName()
			);
		}
		for (QuickActionWindow window : profile.quick_action_windows) {
			addBind(
				byCombo,
				effectiveWindowBind(settings, window),
				Lang.t("lbl.window_bullet") + window.name
			);
		}
		addBind(byCombo, settings.menuKey, Lang.t("lbl.menu"));
		addBind(
			byCombo,
			config
				.settings()
				.resolveBind("ForceStop", profile.profile_settings.forceStop),
			Lang.t("lbl.stop")
		);
		addBind(
			byCombo,
			config
				.settings()
				.resolveBind("Restart", profile.profile_settings.restart),
			Lang.t("lbl.restart")
		);
		FlowLayout conflicts = Ui.card(
			Icons.WARN + "  " + Lang.t("msg.conflicts")
		);
		boolean hasConflicts = false;
		for (var entry : byCombo.entrySet()) {
			if (entry.getValue().size() > 1) {
				hasConflicts = true;
				conflicts.child(
					Components.label(
						Component.literal(
							entry.getKey() +
								": " +
								String.join(", ", entry.getValue())
						)
					).color(Color.ofArgb(Ui.BAD))
				);
			}
		}
		if (!hasConflicts) {
			conflicts.child(Ui.dim(Lang.t("msg.no_conflicts")));
		}
		content.child(conflicts);

		FlowLayout sys = Ui.card(Icons.BINDS + "  " + Lang.t("sec.system"));
		sys.child(Ui.wrap(Lang.t("tip.editor_bind"), windowWidth - 30));
		sys.child(specialBindRow(config, settings, Lang.t("lbl.menu"), "menu"));
		sys.child(
			specialBindRow(config, settings, Lang.t("lbl.stop"), "ForceStop")
		);
		sys.child(
			specialBindRow(config, settings, Lang.t("lbl.restart"), "Restart")
		);
		content.child(sys);

		if (!profile.quick_action_windows.isEmpty()) {
			FlowLayout windows = Ui.card(
				Icons.PLAY + "  " + Lang.t("stat.windows")
			);
			for (QuickActionWindow window : profile.quick_action_windows) {
				windows.child(
					bindRow(
						Lang.t("lbl.window_bullet") + window.name,
						display(effectiveWindowBind(settings, window)),
						"window:" + window.id
					)
				);
			}
			FlowLayout winControls = Ui.row();
			windows.child(Ui.hline());
			winControls.child(
				btn(Icons.RELOAD + " " + Lang.t("btn.reset_all"), () ->
					confirm(Lang.t("msg.confirm_reset"), () -> {
						for (QuickActionWindow window : profile.quick_action_windows) {
							settings.binds.remove("window:" + window.id);
						}
						markDirty();
						AfrRuntime.get().rebuildBinds();
						rebuild();
					})
				).tip(Lang.t("btn.reset_all"))
			);
			winControls.child(Ui.grow());
			winControls.child(
				btn(Icons.CHECK + " " + Lang.t("btn.make_base"), () -> {
					for (QuickActionWindow window : profile.quick_action_windows) {
						String resolved = effectiveWindowBind(settings, window);
						if (resolved != null && !resolved.isBlank()) {
							window.bind = resolved;
						}
						settings.binds.remove("window:" + window.id);
					}
					markDirty();
					AfrRuntime.get().rebuildBinds();
					NotificationHud.notify(
						NotificationHud.Type.SUCCESS,
						"AFR",
						Lang.t("msg.binds_base")
					);
					rebuild();
				}).tip(Lang.t("tip.binds_base_windows"))
			);
			windows.child(winControls);
			content.child(windows);
		}

		FlowLayout actions = Ui.card(
			Icons.ACTIONS + "  " + Lang.t("stat.actions")
		);
		List<Map.Entry<String, RpAction>> list = new ArrayList<>(
			profile.rp_actions.entrySet()
		);
		list.sort((a, b) ->
			a
				.getValue()
				.displayName()
				.compareToIgnoreCase(b.getValue().displayName())
		);
		for (var entry : list) {
			RpAction action = entry.getValue();
			actions.child(
				bindRow(
					action.displayName(),
					display(config.resolveBind(entry.getKey(), action.bind)),
					entry.getKey()
				)
			);
		}
		FlowLayout bindControls = Ui.row();
		actions.child(Ui.hline());
		bindControls.child(
			btn(Icons.RELOAD + " " + Lang.t("btn.reset_all"), () ->
				confirm(Lang.t("msg.confirm_reset"), () -> {
					for (var entry : profile.rp_actions.entrySet()) {
						settings.binds.remove(entry.getKey());
					}
					markDirty();
					AfrRuntime.get().rebuildBinds();
					rebuild();
				})
			).tip(Lang.t("btn.reset_all"))
		);
		bindControls.child(Ui.grow());
		bindControls.child(
			btn(Icons.CHECK + " " + Lang.t("btn.make_base"), () -> {
				for (var entry : profile.rp_actions.entrySet()) {
					String resolved = config.resolveBind(
						entry.getKey(),
						entry.getValue().bind
					);
					if (resolved != null && !resolved.isBlank()) {
						entry.getValue().bind = resolved;
					}
					settings.binds.remove(entry.getKey());
				}
				markDirty();
				AfrRuntime.get().rebuildBinds();
				NotificationHud.notify(
					NotificationHud.Type.SUCCESS,
					"AFR",
					Lang.t("msg.binds_base")
				);
				rebuild();
			}).tip(Lang.t("tip.binds_base"))
		);
		actions.child(bindControls);
		content.child(actions);
	}

	private FlowLayout specialBindRow(
		ConfigManager config,
		UserSettings settings,
		String label,
		String id
	) {
		String bind = switch (id) {
			case "menu" -> settings.menuKey;
			case "ForceStop" -> config
				.settings()
				.resolveBind(
					"ForceStop",
					config.active().profile_settings.forceStop
				);
			case "Restart" -> config
				.settings()
				.resolveBind(
					"Restart",
					config.active().profile_settings.restart
				);
			default -> "";
		};
		return bindRow(label, display(bind), id);
	}

	private FlowLayout bindRow(String label, String bindText, String id) {
		FlowLayout row = Ui.row();
		row.child(
			Components.label(
				Component.literal(
					TextFit.fit(
						Minecraft.getInstance().font,
						label,
						windowWidth / 2
					)
				)
			).color(Color.ofArgb(Ui.TEXT))
		);
		row.child(Ui.grow());
		row.child(
			new AfrButton(
				Component.literal(
					capturing != null && capturing.equals(id)
						? Lang.t("msg.press_key_short")
						: bindText
				),
				AfrButton.Variant.SECONDARY,
				() -> {
					capturing = id;
					rebuild();
				}
			).w(120)
		);
		row.child(
			icon(Icons.DELETE, () -> {
				AfrRuntime runtime = AfrRuntime.get();
				if ("menu".equals(id)) {
					runtime.config().settings().menuKey = "";
				} else {
					runtime.config().settings().binds.remove(id);
				}
				runtime.config().saveSettings();
				runtime.rebuildBinds();
				rebuild();
			})
		);
		return row;
	}

	private void buildLibrary(FlowLayout content, UserSettings settings) {
		FlowLayout cloud = Ui.card(Icons.CLOUD + "  " + Lang.t("sec.updates"));
		cloud.child(
			checkRow(
				Lang.t("lbl.check_on_join"),
				settings.checkUpdates,
				value -> settings.checkUpdates = value
			)
		);
		cloud.child(
			checkRow(
				Lang.t("lbl.smart_updates"),
				settings.smartUpdateCheck,
				value -> settings.smartUpdateCheck = value,
				Lang.t("tip.smart_updates")
			)
		);
		cloud.child(
			checkRow(
				Lang.t("lbl.auto_update_config"),
				settings.autoUpdateConfigs,
				value -> settings.autoUpdateConfigs = value
			)
		);
		FlowLayout row = Containers.horizontalFlow(
			Sizing.fill(100),
			Sizing.content()
		);
		row.gap(6);
		row.child(
			primary(Icons.CLOUD + " " + Lang.t("lbl.library"), () ->
				LibraryScreen.open(this)
			)
		);
		row.child(
			btn(Icons.RELOAD + " " + Lang.t("btn.check_updates"), () ->
				VersionScreen.open(this)
			)
		);
		cloud.child(row);
		content.child(cloud);
	}

	private void addStep(
		ConfigManager config,
		RpProfile profile,
		RpAction action,
		String prefix
	) {
		action.steps.add(
			new ArrayList<>(List.of("RPAction", "Send", "S300", "S300", prefix))
		);
		markDirty();
		populateEditor(config, profile);
	}

	private void duplicateAction(
		ConfigManager config,
		RpProfile profile,
		RpAction action
	) {
		String id = "action_" + (System.currentTimeMillis() % 100000);
		RpAction copy = ConfigCodec.GSON.fromJson(
			ConfigCodec.GSON.toJson(action),
			RpAction.class
		);
		copy.name =
			action.displayName() + " (" + Lang.t("lbl.copy_suffix") + ")";
		profile.rp_actions.put(id, copy);
		markDirty();
		selectedAction = id;
		rebuild();
		NotificationHud.notify(
			NotificationHud.Type.SUCCESS,
			"AFR",
			Lang.t("msg.duplicated")
		);
	}

	private void exportAction(RpAction action) {
		Minecraft.getInstance().keyboardHandler.setClipboard(
			ConfigCodec.GSON.toJson(action)
		);
		NotificationHud.notify(
			NotificationHud.Type.SUCCESS,
			"AFR",
			Lang.t("msg.action_copied")
		);
	}

	private FlowLayout validationCard(RpProfile profile) {
		FlowLayout card = Ui.card(Icons.CHECK + "  " + Lang.t("btn.validate"));
		List<String> problems = new ArrayList<>();
		Set<String> names = new HashSet<>();
		Pattern token = Pattern.compile("\\{([^{}\\s]+)\\}");
		for (RpAction action : profile.rp_actions.values()) {
			if (!names.add(action.displayName())) {
				problems.add(
					Lang.t("val.dup_name") + ": " + action.displayName()
				);
			}
			boolean anyText = false;
			for (List<String> step : action.steps) {
				if (step.size() < 5) {
					continue;
				}
				for (
					int textIndex = 4;
					textIndex <= 5 && textIndex < step.size();
					textIndex++
				) {
					String text = step.get(textIndex);
					if (text == null) {
						continue;
					}
					if (textIndex == 4 && !text.isBlank()) {
						anyText = true;
					}
					Matcher matcher = token.matcher(text);
					while (matcher.find()) {
						String value = matcher.group(1);
						if (value.contains("|")) {
							continue;
						}
						if (!PlaceholderResolver.isKnown(value, profile)) {
							problems.add(
								action.displayName() +
									": " +
									Lang.t("val.unknown_var") +
									" {" +
									value +
									"}"
							);
						}
					}
				}
			}
			if (!anyText) {
				problems.add(
					Lang.t("val.empty_steps") + ": " + action.displayName()
				);
			}
		}
		if (problems.isEmpty()) {
			return null;
		}
		for (String problem : problems) {
			card.child(
				Components.label(
					Component.literal(Icons.WARN + " " + problem)
				).color(Color.ofArgb(Ui.WARN))
			);
		}
		return card;
	}

	private static List<String> splitList(String value) {
		List<String> list = new ArrayList<>();
		if (value == null) {
			return list;
		}
		for (String part : value.split("\\s*,\\s*")) {
			if (!part.isBlank()) {
				list.add(part.strip());
			}
		}
		return list;
	}

	private FlowLayout readOnlyRow(String label, String value) {
		FlowLayout row = Ui.row();
		row.child(
			Components.label(
				Component.literal(
					TextFit.fit(
						Minecraft.getInstance().font,
						label,
						windowWidth / 2
					)
				)
			).color(Color.ofArgb(Ui.DIM))
		);
		row.child(Ui.grow());
		LabelComponent valueLabel = Components.label(
			Component.literal(
				TextFit.fit(
					Minecraft.getInstance().font,
					value == null ? "" : value,
					windowWidth / 2
				)
			)
		);
		valueLabel.color(Color.ofArgb(Ui.TEXT));
		if (value != null && !value.isBlank()) {
			valueLabel.tooltip(Component.literal(value));
		}
		row.child(valueLabel);
		return row;
	}

	private void forkProfile(ConfigManager config, RpProfile profile) {
		RpProfile copy = ConfigCodec.GSON.fromJson(
			ConfigCodec.GSON.toJson(profile),
			RpProfile.class
		);
		String base = profile.meta.revName + "-local";
		String revName = base;
		int index = 1;
		while (config.profiles().containsKey(revName)) {
			revName = base + index++;
		}
		copy.meta.revName = revName;
		copy.meta.label =
			profile.meta.label + " (" + Lang.t("diff.local_copy") + ")";
		copy.meta.source = "local";
		copy.meta.remoteId = "";
		copy.meta.remoteVersionCode = 0;
		copy.meta.modified = false;
		config.putProfile(copy, true);
		AfrRuntime.get().rebuildBinds();
		rebuild();
		NotificationHud.notify(
			NotificationHud.Type.SUCCESS,
			"AFR",
			Lang.t("msg.forked") + ": " + copy.meta.label
		);
	}

	private boolean save() {
		for (Runnable saver : savers) {
			saver.run();
		}
		AfrRuntime runtime = AfrRuntime.get();
		RpProfile activeProfile = runtime.config().active();
		if (activeProfile != null && activeProfile.isLibrary()) {
			activeProfile.meta.modified = true;
		}
		boolean saved = runtime.config().saveSettings();
		saved = runtime.config().saveActive() && saved;
		runtime.rebuildBinds();
		if (saved) {
			AfrRuntime.get().editorDirty = false;
			NotificationHud.notify(
				NotificationHud.Type.SUCCESS,
				"AFR",
				Lang.t("msg.saved")
			);
		} else {
			NotificationHud.notify(
				NotificationHud.Type.ERROR,
				"AFR",
				Lang.t("msg.save_error")
			);
		}
		AFR.LOGGER.info(
			"AFR: editor saved (profile='{}', ok={})",
			runtime.config().activeId(),
			saved
		);
		rebuild();
		return saved;
	}

	private int controlWidth() {
		return narrow ? 120 : Ui.CONTROL_WIDTH;
	}

	private FlowLayout settingRow(
		String label,
		io.wispforest.owo.ui.core.Component control
	) {
		FlowLayout row = Ui.row();
		row.child(
			Components.label(
				Component.literal(
					TextFit.fit(
						Minecraft.getInstance().font,
						label,
						windowWidth / 2
					)
				)
			).color(Color.ofArgb(Ui.DIM))
		);
		row.child(Ui.grow());
		row.child(control);
		return row;
	}

	private FlowLayout textRow(
		String label,
		String value,
		Consumer<String> setter
	) {
		AfrInput input = AfrInput.fixed(value, controlWidth());
		input.onChanged(v -> markDirty());
		savers.add(() -> setter.accept(input.value()));
		return settingRow(label, input.component());
	}

	private FlowLayout valueRow(
		String label,
		String value,
		Consumer<String> setter
	) {
		AfrInput input = AfrInput.fixed(value, narrow ? 150 : 210);
		input.onChanged(v -> markDirty());
		savers.add(() -> setter.accept(input.value()));
		return settingRow(label, input.component());
	}

	private FlowLayout textRowFill(
		String label,
		String value,
		Consumer<String> setter
	) {
		FlowLayout row = Ui.row();
		row.child(
			Components.label(Component.literal(label)).color(
				Color.ofArgb(Ui.DIM)
			)
		);
		row.child(Ui.grow());
		AfrInput input = AfrInput.fill(value);
		input.onChanged(v -> markDirty());
		savers.add(() -> setter.accept(input.value()));
		FlowLayout col = Containers.verticalFlow(
			Sizing.fill(100),
			Sizing.content()
		);
		col.gap(3);
		col.child(row);
		col.child(input.component());
		return col;
	}

	private FlowLayout checkRow(
		String label,
		boolean value,
		Consumer<Boolean> setter
	) {
		return settingRow(
			label,
			new AfrCheckbox(value).onChanged(v -> {
				markDirty();
				setter.accept(v);
			})
		);
	}

	private FlowLayout checkRow(
		String label,
		boolean value,
		Consumer<Boolean> setter,
		String tip
	) {
		FlowLayout row = checkRow(label, value, setter);
		if (tip != null && !tip.isBlank()) {
			row.tooltip(Component.literal(tip));
		}
		return row;
	}

	private AfrButton btn(String text, Runnable onPress) {
		return new AfrButton(
			Component.literal(text),
			AfrButton.Variant.SECONDARY,
			onPress
		);
	}

	private AfrButton primary(String text, Runnable onPress) {
		return new AfrButton(
			Component.literal(text),
			AfrButton.Variant.PRIMARY,
			onPress
		);
	}

	private AfrButton danger(String text, Runnable onPress) {
		return new AfrButton(
			Component.literal(text),
			AfrButton.Variant.DANGER,
			onPress
		);
	}

	private AfrButton icon(String glyph, Runnable onPress) {
		return new AfrButton(
			Component.literal(glyph),
			AfrButton.Variant.ICON,
			onPress
		);
	}

	private AfrButton cycleButton(
		String[] options,
		String current,
		int width,
		Consumer<String> onChange
	) {
		String[] list = options;
		if (current != null) {
			boolean found = false;
			for (String option : options) {
				if (option.equalsIgnoreCase(current)) {
					found = true;
					break;
				}
			}
			if (!found) {
				list = new String[options.length + 1];
				list[0] = current;
				System.arraycopy(options, 0, list, 1, options.length);
			}
		}
		int start = 0;
		for (int i = 0; i < list.length; i++) {
			if (list[i].equalsIgnoreCase(current)) {
				start = i;
			}
		}
		final int[] index = { start };
		final AfrButton[] ref = new AfrButton[1];
		final String[] values = list;
		AfrButton button = new AfrButton(
			Component.literal(displayValue(values[index[0]])),
			AfrButton.Variant.SECONDARY,
			() -> {
				index[0] = (index[0] + 1) % values.length;
				onChange.accept(values[index[0]]);
				markDirty();
				if (ref[0] != null) {
					ref[0].setText(
						Component.literal(displayValue(values[index[0]]))
					);
				}
			}
		);
		ref[0] = button;
		return button.w(width);
	}

	private AfrButton themeButton(ConfigManager config, UserSettings settings) {
		List<String> ids = Theme.ids();
		int index = Math.max(0, ids.indexOf(Theme.byId(settings.theme).id));
		AfrButton button = new AfrButton(
			Component.literal(
				Theme.byId(settings.theme).label(settings.language)
			),
			AfrButton.Variant.SECONDARY,
			() -> {
				int next = (index + 1) % ids.size();
				settings.theme = ids.get(next);
				Theme theme = Theme.byId(settings.theme);
				Ui.applyTheme(theme, settings.themeColors.get(theme.id));
				markDirty();
				rebuild();
			}
		);
		return button.w(controlWidth() - 66);
	}

	public static String displayValue(String value) {
		return switch (value) {
			case "RPAction" -> Lang.t("mode.rpaction");
			case "RPActionGender" -> Lang.t("mode.gender");
			case "Send" -> Lang.t("mode.send");
			case "Chat" -> Lang.t("mode.send");
			case "Input" -> Lang.t("mode.input");
			case "text" -> Lang.t("mode.text");
			case "number" -> Lang.t("mode.number");
			case "list" -> Lang.t("mode.list");
			case "radial" -> Lang.t("mode.radial");
			case "tiles" -> Lang.t("mode.tiles");
			case "compact" -> Lang.t("mode.compact");
			case "stable" -> Lang.t("mode.stable");
			case "ru" -> Lang.t("lbl.lang_ru");
			case "en" -> Lang.t("lbl.lang_en");
			default -> value;
		};
	}

	@Override
	public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
		if (capturing != null) {
			if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
				capturing = null;
				rebuild();
				return true;
			}
			if (
				keyCode == GLFW.GLFW_KEY_BACKSPACE ||
				keyCode == GLFW.GLFW_KEY_DELETE
			) {
				applyCapture("-");
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
			KeyCombo combo = new KeyCombo(
				keyCode,
				(modifiers & GLFW.GLFW_MOD_CONTROL) != 0,
				(modifiers & GLFW.GLFW_MOD_ALT) != 0,
				(modifiers & GLFW.GLFW_MOD_SHIFT) != 0
			);
			applyCapture(combo.display());
			return true;
		}
		return super.keyPressed(keyCode, scanCode, modifiers);
	}

	public boolean isCapturing() {
		return capturing != null;
	}

	public void captureMouseBind(String name) {
		applyCapture(name);
	}

	private void applyCapture(String combo) {
		AfrRuntime runtime = AfrRuntime.get();
		UserSettings settings = runtime.config().settings();
		if ("menu".equals(capturing)) {
			settings.menuKey = combo;
		} else if (combo.isBlank()) {
			settings.binds.remove(capturing);
		} else {
			settings.binds.put(capturing, combo);
		}
		runtime.config().saveSettings();
		runtime.rebuildBinds();
		capturing = null;
		rebuild();
	}

	@Override
	public void onClose() {
		if (capturing != null) {
			capturing = null;
			rebuild();
			return;
		}
		if (dirty()) {
			minecraft.setScreen(
				new ConfirmScreen(
					Lang.t("confirm.title"),
					Lang.t("msg.confirm_unsaved"),
					Lang.t("btn.discard"),
					() -> {
						AfrRuntime.get().reload();
						AfrRuntime.get().editorDirty = false;
						minecraft.setScreen(parent);
					},
					() -> minecraft.setScreen(this),
					Lang.t("btn.save"),
					() -> {
						if (!save()) {
							minecraft.setScreen(this);
							return;
						}
						minecraft.setScreen(parent);
					}
				)
			);
			return;
		}
		minecraft.setScreen(parent);
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}

	private static String modVersion() {
		return FabricLoader.getInstance()
			.getModContainer("afr")
			.map(c -> c.getMetadata().getVersion().getFriendlyString())
			.orElse("?");
	}

	private static ItemStack itemFromId(String id) {
		if (id == null || id.isBlank()) {
			return ItemStack.EMPTY;
		}
		try {
			ResourceLocation rl = ResourceLocation.tryParse(
				id.contains(":") ? id : "minecraft:" + id
			);
			if (rl == null) {
				return ItemStack.EMPTY;
			}
			return new ItemStack(BuiltInRegistries.ITEM.get(rl));
		} catch (Exception e) {
			return ItemStack.EMPTY;
		}
	}

	private static String formatFactor(double factor) {
		if (factor == Math.floor(factor)) {
			return String.valueOf((long) factor);
		}
		return String.valueOf(factor);
	}

	private static String effectiveWindowBind(
		UserSettings settings,
		QuickActionWindow window
	) {
		String override = settings.binds.get("window:" + window.id);
		if (override != null && !override.isBlank()) {
			return override;
		}
		if (KeyCombo.parse(window.bind) != null) {
			return window.bind;
		}
		String id =
			window.id == null || window.id.isBlank() ? window.name : window.id;
		id = id == null ? "" : id.toLowerCase();
		if (id.contains("main")) {
			return "F4";
		}
		if (id.contains("rare")) {
			return "F8";
		}
		if (id.contains("educ")) {
			return "F6";
		}
		if (id.contains("pmp")) {
			return "F7";
		}
		return "";
	}

	private static String display(String bind) {
		if (bind == null || bind.isBlank()) {
			return "-";
		}
		KeyCombo combo = KeyCombo.parse(bind);
		return combo != null ? combo.display() : bind;
	}

	private static String genderLabel(String gender) {
		return "female".equalsIgnoreCase(gender)
			? Lang.t("lbl.female")
			: Lang.t("lbl.male");
	}

	private static String joinInts(List<Integer> list) {
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < list.size(); i++) {
			if (i > 0) {
				sb.append(", ");
			}
			sb.append(list.get(i));
		}
		return sb.toString();
	}

	private static List<Integer> parseInts(
		String value,
		List<Integer> fallback
	) {
		try {
			String[] parts = value.split("[,\\s]+");
			List<Integer> result = new ArrayList<>();
			for (String part : parts) {
				if (!part.isBlank()) {
					result.add(Integer.parseInt(part.trim()));
				}
			}
			return result.isEmpty() ? fallback : result;
		} catch (Exception e) {
			return fallback;
		}
	}

	private static double parseDouble(String value, double fallback) {
		try {
			return Double.parseDouble(value.strip().replace(',', '.'));
		} catch (Exception e) {
			return fallback;
		}
	}
}

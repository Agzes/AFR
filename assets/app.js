// SPDX-License-Identifier: GPL-3.0-only
(function () {
	"use strict";

	var I18N = { ru: {}, en: {} };
	(function () {
		var base = window.AFR_I18N || { ru: {}, en: {} };
		var extra = window.AFR_I18N_EXTRA || { ru: {}, en: {} };
		["ru", "en"].forEach(function (l) {
			Object.assign(I18N[l], base[l] || {}, extra[l] || {});
		});
	})();
	function readLS(k, d) {
		try {
			var v = localStorage.getItem(k);
			return v === null ? d : v;
		} catch (e) {
			return d;
		}
	}
	function writeLS(k, v) {
		try {
			localStorage.setItem(k, v);
		} catch (e) {}
	}
	function preferredLang() {
		var stored = readLS("afr.lang", null);
		if (stored === "ru" || stored === "en") return stored;
		var list =
			navigator.languages && navigator.languages.length
				? navigator.languages
				: [navigator.language || ""];
		for (var i = 0; i < list.length; i++) {
			var code = String(list[i]).toLowerCase();
			if (code.indexOf("ru") === 0) return "ru";
			if (code.indexOf("en") === 0) return "en";
		}
		return "ru";
	}
	var lang = preferredLang();

	function t(key, vars) {
		var table = I18N[lang] || I18N.ru;
		var value = table[key];
		if (value === undefined) value = I18N.ru[key];
		if (value === undefined) value = key;
		if (vars)
			value = value.replace(/\{(\w+)\}/g, function (m, n) {
				return vars[n] !== undefined ? String(vars[n]) : m;
			});
		return value;
	}
	window.AfrT = t;
	window.AfrLang = function () {
		return lang;
	};

	var G = {
		HOME: "\u2302",
		GEAR: "\u2630",
		PROFILES: "\u263B",
		ACTIONS: "\u270E",
		BINDS: "\u2318",
		LIBRARY: "\u2601",
		PLAY: "\u25B6",
		RELOAD: "\u21C4",
		STAR: "\u2605",
		EMPTY_STAR: "\u2606",
		EDIT: "\u270E",
		DELETE: "\u2718",
		PLUS: "+",
		CHECK: "\u2714",
		CLOUD: "\u2601",
		WARN: "\u26A0",
		DIAMOND: "\u25C6",
	};

	var VERSION = "1.0.0";
	var VERSION_CODE = 10000;

	var settings = { language: lang, notifications: true };
	var library = [];
	var extraButtons = [];
	var viewer = { entry: null, cfg: null, latestCfg: null };
	var state = {
		large: false,
		narrow: false,
		renderedNarrow: null,
		version: null,
		announcements: [],
		selectedAction: null,
		viewerVersion: null,
	};

	var PAGE =
		(document.body && document.body.getAttribute("data-page")) || "home";
	function goHome() {
		window.location.href = "index.html";
	}
	function goLibrary() {
		window.location.href = "library.html";
	}
	function openExternal(url) {
		var safe = safeExternal(url);
		if (safe) window.open(safe, "_blank", "noopener");
	}
	function safeExternal(url) {
		return /^https?:\/\//i.test(String(url || "")) ? String(url) : "";
	}
	function safeNav(url) {
		var value = String(url || "");
		if (!value) return "";
		if (value.indexOf("//") === 0) return "";
		if (/^[a-z][a-z0-9+.-]*:/i.test(value)) {
			return /^https?:\/\//i.test(value) ? value : "";
		}
		return value;
	}
	function configAsset(url) {
		var value = String(url || "");
		if (!value) return "";
		if (/^https?:\/\//i.test(value)) return value;
		return "api/" + value.replace(/^\/?api\//, "").replace(/^\//, "");
	}

	function el(tag, cls, text) {
		var n = document.createElement(tag);
		if (cls) n.className = cls;
		if (text !== undefined && text !== null) n.textContent = text;
		return n;
	}
	function add(parent) {
		for (var i = 1; i < arguments.length; i++)
			if (arguments[i]) parent.appendChild(arguments[i]);
		return parent;
	}
	function clear(node) {
		if (node) while (node.firstChild) node.removeChild(node.firstChild);
	}
	function px(n) {
		return "calc(" + n + "px * var(--s))";
	}
	function docS() {
		var v = getComputedStyle(document.documentElement).getPropertyValue(
			"--s",
		);
		var f = parseFloat(v);
		return isNaN(f) ? 2 : f;
	}
	function section(icon, title) {
		return icon ? icon + "  " + title : title;
	}
	function card(title) {
		var c = el("div", "card");
		if (title) {
			c.appendChild(el("div", "sec-title", title));
			c.appendChild(hline());
		}
		for (var i = 1; i < arguments.length; i++)
			if (arguments[i]) c.appendChild(arguments[i]);
		return c;
	}
	function heroCard() {
		var c = el("div", "card hero");
		for (var i = 0; i < arguments.length; i++)
			if (arguments[i]) c.appendChild(arguments[i]);
		return c;
	}
	function hline() {
		return el("hr", "hr");
	}
	function grow() {
		return el("div", "grow");
	}
	function dim(text) {
		return el("span", "dim", text);
	}
	function tag(text) {
		return el("span", "tag", text);
	}

	function btn(text, opts) {
		opts = opts || {};
		var cls =
			"btn" +
			(opts.variant ? " " + opts.variant : "") +
			(opts.active ? " active" : "");
		var b = el("button", cls, text);
		b.type = "button";
		if (opts.width) b.style.width = px(opts.width);
		if (opts.tip) b.title = opts.tip;
		if (opts.disabled) {
			b.classList.add("disabled");
			b.setAttribute("aria-disabled", "true");
		}
		b.addEventListener("click", function (e) {
			e.stopPropagation();
			if (!opts.disabled && opts.onClick) opts.onClick();
		});
		return b;
	}
	function primary(text, opts) {
		return btn(text, Object.assign({ variant: "primary" }, opts));
	}
	function iconBtn(glyph, onClick, tip) {
		return btn(glyph, { variant: "icon", onClick: onClick, tip: tip });
	}

	function settingRow(labelText, control) {
		var r = el("div", "setting-row");
		r.appendChild(el("span", "set-lbl ellipsis", labelText));
		r.appendChild(grow());
		if (control) r.appendChild(control);
		return r;
	}
	function readOnlyRow(labelText, value) {
		var r = el("div", "row");
		r.appendChild(el("span", "dim ellipsis", labelText));
		r.appendChild(grow());
		var v = el(
			"span",
			"set-val ellipsis",
			value == null ? "" : String(value),
		);
		if (value) v.title = String(value);
		r.appendChild(v);
		return r;
	}
	function input(value, opts) {
		opts = opts || {};
		var box = el("div", "field-box");
		var inp = el("input", "field");
		inp.type = "text";
		inp.value = value == null ? "" : String(value);
		inp.autocomplete = "off";
		inp.spellcheck = false;
		if (opts.hint) inp.placeholder = opts.hint;
		if (opts.width) box.style.width = px(opts.width);
		else box.style.flex = "1 1 auto";
		if (opts.onChange)
			inp.addEventListener("input", function () {
				opts.onChange(inp.value);
			});
		box.appendChild(inp);
		return box;
	}
	function featureRow(glyph, text) {
		var r = el("div", "feat");
		r.appendChild(el("span", "fi", glyph));
		r.appendChild(el("span", "ft", text));
		r.appendChild(grow());
		return r;
	}
	function hscroll(inner) {
		var s = el("div", "hscroll");
		s.appendChild(inner);
		return s;
	}

	function displayValue(value) {
		switch (value) {
			case "RPAction":
				return t("mode.rpaction");
			case "RPActionGender":
				return t("mode.gender");
			case "Send":
				return t("mode.send");
			case "Chat":
				return t("mode.send");
			case "Input":
				return t("mode.input");
			case "text":
				return t("mode.text");
			case "number":
				return t("mode.number");
			case "list":
				return t("mode.list");
			case "radial":
				return t("mode.radial");
			case "tiles":
				return t("mode.tiles");
			case "compact":
				return t("mode.compact");
			case "ru":
				return t("lbl.lang_ru");
			case "en":
				return t("lbl.lang_en");
			default:
				return value;
		}
	}

	function toastHost() {
		var host = document.querySelector(".toast-host");
		if (!host) {
			host = el("div", "toast-host");
			document.body.appendChild(host);
		}
		return host;
	}
	function toast(type, title, desc) {
		if (!settings.notifications) return;
		var host = toastHost();
		var note = el("div", "toast " + (type || "info"));
		note.setAttribute("role", "status");
		var body = el("div", "toast-body");
		body.appendChild(el("div", "toast-title", title || ""));
		if (desc) body.appendChild(el("div", "toast-desc", desc));
		note.appendChild(body);
		host.appendChild(note);
		setTimeout(function () {
			note.classList.add("hide");
			setTimeout(function () {
				if (note.parentNode) note.parentNode.removeChild(note);
			}, 300);
		}, 4700);
	}

	function applyStaticI18n() {
		document.querySelectorAll("[data-i18n]").forEach(function (n) {
			n.textContent = t(n.getAttribute("data-i18n"));
		});
		document.querySelectorAll("[data-i18n-ph]").forEach(function (n) {
			n.setAttribute("placeholder", t(n.getAttribute("data-i18n-ph")));
		});
		document.querySelectorAll("[data-i18n-title]").forEach(function (n) {
			n.setAttribute("title", t(n.getAttribute("data-i18n-title")));
		});
	}
	function setLang(next) {
		lang = next === "en" ? "en" : "ru";
		settings.language = lang;
		writeLS("afr.lang", lang);
		document.documentElement.lang = lang;
		applyStaticI18n();
		render();
	}

	function pageSignature() {
		var line = el("div", "page-signature inline");
		line.appendChild(document.createTextNode("with \u2764\uFE0F by "));
		var link = el("a", "", "Agzes");
		link.href = "https://agzes.github.io";
		link.target = "_blank";
		link.rel = "noopener";
		line.appendChild(link);
		return line;
	}
	function topbar() {
		var bar = el("div", "topbar");
		var left = el("div", "topbar-left");
		left.appendChild(el("span", "brand", "AFR"));
		var by = el("span", "topbar-by");
		by.appendChild(el("span", "dim", "by"));
		var agz = el("a", "topbar-link", "Agzes");
		agz.href = "https://agzes.github.io";
		agz.target = "_blank";
		agz.rel = "noopener";
		by.appendChild(agz);
		left.appendChild(by);
		bar.appendChild(left);
		var right = el("div", "topbar-right");
		right.appendChild(langToggle());
		bar.appendChild(right);
		return bar;
	}
	function render() {
		var app = document.getElementById("app");
		if (!app) return;
		var prevBody = app.querySelector(".win-body");
		var scroll = prevBody ? prevBody.scrollTop : 0;

		var win;
		if (PAGE === "home") win = buildLandingWindow();
		else if (PAGE === "library") win = buildLibraryWindow();
		else if (PAGE === "config") win = buildViewerWindow();
		else win = el("div", "win");

		clear(app);
		app.appendChild(topbar());
		var wrap = el("div", "wrap");
		wrap.appendChild(win);
		app.appendChild(wrap);

		var body = win.querySelector(".win-body");
		if (body) body.scrollTop = scroll;

		var measured = win.clientWidth / docS();
		if (measured > 0 && measured < 520 !== state.renderedNarrow) {
			state.renderedNarrow = measured < 520;
			state.narrow = state.renderedNarrow;
			render();
			return;
		}
		if (PAGE === "library") renderLibList();
	}

	function openOverlay(win) {
		var scrim = el("div", "scrim");
		scrim.appendChild(win);
		scrim.addEventListener("click", function (e) {
			if (e.target === scrim) closeTop();
		});
		document.body.appendChild(scrim);
	}
	function closeTop() {
		var all = document.querySelectorAll(".scrim");
		if (all.length) all[all.length - 1].remove();
	}
	function openInstall(entry) {
		var win = el("div", "win");
		win.appendChild(
			windowHeader(
				el("span", "sec-hdr", G.PLAY + "  " + t("landing.install")),
			),
		);
		var body = el("div", "win-body");

		var steps = card(section(G.PLAY, t("landing.install.steps")));
		steps.appendChild(el("div", "dim", t("landing.install.desc")));
		steps.appendChild(hline());
		var list = [
			["1", t("landing.install.s1")],
			["2", t("landing.install.s2")],
			["3", t("landing.install.s3")],
		];
		if (entry) {
			list.push([
				"4",
				t("landing.install.s4", {
					name: entry.name || entry.id || "",
				}),
			]);
		}
		list.forEach(function (s) {
			var line = el("div", "step");
			line.appendChild(el("span", "st-n", s[0]));
			line.appendChild(el("span", "st-tx", s[1]));
			steps.appendChild(line);
		});
		var row = el("div", "row");
		row.appendChild(
			primary(G.CLOUD + " " + t("btn.download"), {
				onClick: function () {
					openExternal("https://github.com/Agzes/AFR/releases");
				},
			}),
		);
		row.appendChild(
			btn(G.CLOUD + " " + t("sec.library"), {
				onClick: function () {
					closeTop();
					goLibrary();
				},
			}),
		);
		steps.appendChild(row);
		body.appendChild(steps);

		var req = card(section(G.CLOUD, t("landing.install.req")));
		req.appendChild(featureRow(G.LIBRARY, t("landing.install.req1")));
		req.appendChild(featureRow(G.DIAMOND, t("landing.install.req2")));
		body.appendChild(req);

		win.appendChild(body);
		var ftr = el("footer", "win-footer");
		ftr.appendChild(dim("v" + VERSION));
		ftr.appendChild(grow());
		ftr.appendChild(btn(t("btn.close"), { onClick: closeTop }));
		win.appendChild(ftr);
		openOverlay(win);
	}

	function siteTabs(active) {
		var nav = el("nav", "tabs");
		[
			["home", "nav.home", G.HOME, "index.html"],
			["library", "nav.library", G.CLOUD, "library.html"],
		].forEach(function (item) {
			var a = el("a", "tab" + (active === item[0] ? " active" : ""));
			a.href = item[3];
			a.appendChild(el("span", "tab-ic", item[2]));
			if (!state.narrow) a.appendChild(el("span", "", t(item[1])));
			a.title = t(item[1]);
			nav.appendChild(a);
		});
		return nav;
	}
	function langToggle() {
		var next = settings.language === "ru" ? "en" : "ru";
		var b = el("button", "btn mini", settings.language.toUpperCase());
		b.type = "button";
		b.title = next.toUpperCase();
		b.addEventListener("click", function () {
			setLang(next);
		});
		return b;
	}
	function windowHeader(center) {
		var h = el("header", "win-header");
		var title = el("div", "hdr-title");
		if (center) title.appendChild(center);
		h.appendChild(title);
		return h;
	}
	function afrHeader(label) {
		var wrap = el("span", "hdr-brand");
		wrap.appendChild(el("span", "brand", "AFR"));
		wrap.appendChild(el("span", "dim", " \u2022 " + label));
		return wrap;
	}
	function buildLandingWindow() {
		var win = el("div", "win");
		win.appendChild(windowHeader(afrHeader(t("landing.sub"))));
		win.appendChild(siteTabs("home"));
		win.appendChild(landingBody());
		win.appendChild(landingFooter());
		return win;
	}
	function landingBody() {
		var b = el("main", "win-body");

		if (state.version && state.version.code > VERSION_CODE) {
			var upd = card();
			var updRow = el("div", "row");
			updRow.appendChild(
				el(
					"span",
					"warn",
					t("lbl.update_available") +
						": v" +
						VERSION +
						" (" +
						VERSION_CODE +
						") -> v" +
						state.version.name +
						" (" +
						state.version.code +
						")",
				),
			);
			updRow.appendChild(grow());
			updRow.appendChild(
				btn(G.CLOUD + " GitHub", {
					onClick: function () {
						openExternal("https://github.com/Agzes/AFR/releases");
					},
				}),
			);
			upd.appendChild(updRow);
			b.appendChild(upd);
		}

		var hero = heroCard();
		var hh = el("div", "hero-head");
		var ttl = el("span", "hero-title");
		ttl.appendChild(el("b", "", "AFR"));
		hh.appendChild(ttl);
		hh.appendChild(grow());
		var early = el("span", "early-wrap");
		early.appendChild(
			el("span", "early-badge", G.WARN + " " + t("badge.early")),
		);
		var earlyTip = el("span", "early-tip", t("badge.early.tip"));
		earlyTip.setAttribute("role", "tooltip");
		early.appendChild(earlyTip);
		hh.appendChild(early);
		hero.appendChild(hh);
		hero.appendChild(el("div", "dim", t("about.tagline")));
		var links = el("div", "btn-row");
		links.appendChild(
			primary(G.CLOUD + " " + t("btn.download"), {
				onClick: openInstall,
			}),
		);
		links.appendChild(
			btn(G.CLOUD + " " + t("sec.library"), { onClick: goLibrary }),
		);
		links.appendChild(
			btn(G.CLOUD + " GitHub", {
				onClick: function () {
					openExternal("https://github.com/Agzes/AFR");
				},
			}),
		);
		extraButtons.forEach(function (x) {
			links.appendChild(
				btn(G.CLOUD + " " + x.label, {
					onClick: function () {
						openExternal(x.url);
					},
				}),
			);
		});
		hero.appendChild(links);
		b.appendChild(hero);

		if (state.announcements.length) {
			var ann = card(section(G.CLOUD, t("sec.announcements")));
			state.announcements.forEach(function (item, i) {
				if (i > 0) ann.appendChild(hline());
				var text = pickText(item.text, "");
				var link = item.link || "";
				var button = pickText(item.button, "");
				var severity = item.deprecated
					? "deprecated"
					: item.severity
						? String(item.severity).toLowerCase()
						: "info";
				ann.appendChild(el("span", "", pickText(item.title, "AFR")));
				if (severity !== "info") {
					var tg =
						severity === "deprecated"
							? t("ann.deprecated")
							: severity === "error"
								? t("ann.critical")
								: t("ann.warning");
					ann.appendChild(
						el(
							"span",
							severity === "error" ? "bad" : "warn",
							G.WARN + " " + tg,
						),
					);
				}
				if (text) ann.appendChild(el("span", "dim", text));
				if (link && link.toLowerCase() !== "none")
					ann.appendChild(
						btn(G.CLOUD + " " + (button || t("btn.open_link")), {
							onClick: function () {
								openExternal(link);
							},
						}),
					);
			});
			b.appendChild(ann);
		}

		var about = card(section(G.HOME, t("sec.about")));
		about.appendChild(el("div", "dim", t("landing.about.p1")));
		about.appendChild(el("div", "dim", t("landing.about.p2")));
		b.appendChild(about);

		var feats = card(section(G.DIAMOND, t("landing.features")));
		feats.appendChild(featureRow(G.ACTIONS, t("feat.editor")));
		feats.appendChild(featureRow(G.LIBRARY, t("feat.library")));
		feats.appendChild(featureRow(G.PLAY, t("feat.menu")));
		feats.appendChild(featureRow(G.BINDS, t("feat.binds")));
		feats.appendChild(featureRow(G.CLOUD, t("feat.autochat")));
		feats.appendChild(featureRow(G.DIAMOND, t("feat.themes")));
		b.appendChild(feats);

		var install = card(section(G.PLAY, t("landing.install")));
		[
			["1", t("landing.install.s1")],
			["2", t("landing.install.s2")],
			["3", t("landing.install.s3")],
		].forEach(function (s) {
			var line = el("div", "step");
			line.appendChild(el("span", "st-n", s[0]));
			line.appendChild(el("span", "st-tx", s[1]));
			install.appendChild(line);
		});
		var installRow = el("div", "row");
		installRow.appendChild(
			primary(G.CLOUD + " " + t("btn.download"), {
				onClick: openInstall,
			}),
		);
		installRow.appendChild(
			btn(G.CLOUD + " " + t("sec.library"), { onClick: goLibrary }),
		);
		install.appendChild(installRow);
		b.appendChild(install);

		return b;
	}
	function landingFooter() {
		var f = el("footer", "win-footer");
		f.appendChild(
			iconBtn(
				state.large ? "-" : "+",
				function () {
					state.large = !state.large;
					document.documentElement.classList.toggle(
						"large",
						state.large,
					);
					render();
				},
				t("tip.window_size"),
			),
		);
		f.appendChild(pageSignature());
		f.appendChild(grow());
		f.appendChild(dim("v" + VERSION));
		f.appendChild(
			btn(G.CLOUD + " " + t("sec.library"), { onClick: goLibrary }),
		);
		f.appendChild(
			primary(G.CLOUD + " " + t("btn.download"), {
				onClick: openInstall,
			}),
		);
		return f;
	}
	function pickText(value, fallback) {
		if (value == null) return fallback;
		if (typeof value === "string") return value;
		if (typeof value === "object")
			return value[lang] || value.ru || value.en || fallback;
		return String(value);
	}

	var lib = { q: "" };
	function buildLibraryWindow() {
		var win = el("div", "win lib");
		win.appendChild(
			windowHeader(
				el("span", "sec-hdr", G.CLOUD + "  " + t("sec.library")),
			),
		);

		var toolbar = el("div", "row");
		toolbar.style.padding = px(8);
		toolbar.appendChild(
			input(lib.q, {
				hint: t("lib.search_hint"),
				onChange: function (v) {
					lib.q = String(v).trim().toLowerCase();
					renderLibList();
				},
			}),
		);
		win.appendChild(toolbar);

		var content = el("div", "win-body");
		content.style.padding = "0 " + px(8) + " " + px(8);
		var tagSet = [];
		library.forEach(function (e) {
			(Array.isArray(e.tags) ? e.tags : []).forEach(function (tg) {
				if (tagSet.indexOf(tg) < 0) tagSet.push(tg);
			});
		});
		tagSet.sort();
		if (tagSet.length) {
			var tags = el("div", "chips");
			tagSet.forEach(function (tg) {
				tags.appendChild(
					btn("#" + tg, {
						onClick: function () {
							lib.q = String(tg).toLowerCase();
							render();
						},
					}),
				);
			});
			content.appendChild(tags);
		}
		var list = el("div", "list");
		list.id = "lib-list";
		content.appendChild(list);
		win.appendChild(content);

		var ftr = el("footer", "win-footer");
		var count = el("span", "dim", libCountText());
		count.id = "lib-count";
		ftr.appendChild(count);
		ftr.appendChild(dim("v" + VERSION));
		ftr.appendChild(grow());
		ftr.appendChild(
			btn(G.RELOAD + " " + t("btn.refresh"), {
				onClick: reloadLibrary,
			}),
		);
		ftr.appendChild(btn(t("btn.close"), { onClick: goHome }));
		win.appendChild(ftr);
		return win;
	}
	function reloadLibrary() {
		loadJSON("api/configs/library.json").then(function (data) {
			if (!data || !Array.isArray(data.configs)) {
				toast("error", t("lbl.library"), t("lib.refresh_error"));
				return;
			}
			library = data.configs;
			render();
			toast(
				"success",
				t("lbl.library"),
				t("lib.refreshed") + ": " + library.length,
			);
		});
	}

	function libCountText() {
		return t("lib.count") + ": " + filteredLib().length;
	}
	function filteredLib() {
		return library
			.filter(function (e) {
				if (lib.q) {
					var hay = [
						e.name || "",
						e.author || "",
						e.description || "",
						(e.tags || []).join(" "),
					]
						.join(" ")
						.toLowerCase();
					if (hay.indexOf(lib.q) < 0) return false;
				}
				return true;
			})
			.sort(function (a, b) {
				return String(a.name || "").localeCompare(String(b.name || ""));
			});
	}
	function renderLibList() {
		var list = document.getElementById("lib-list");
		if (!list) return;
		clear(list);
		var items = filteredLib();
		if (!items.length) list.appendChild(el("div", "empty", t("lib.empty")));
		else
			items.forEach(function (entry) {
				list.appendChild(libraryCard(entry));
			});
		var count = document.getElementById("lib-count");
		if (count) count.textContent = libCountText();
	}
	function libraryCard(entry) {
		var c = el("div", "lcard");
		var head = el("div", "lc-head");
		var icon = entryIcon(entry.icon);
		if (icon) head.appendChild(icon);
		head.appendChild(
			el(
				"span",
				"lc-name",
				(entry.name || t("lib.no_title")) +
					(entry.version ? "  v" + entry.version : ""),
			),
		);
		head.appendChild(grow());
		var actions = el("div", "lc-actions");
		var preview =
			safeNav(entry.preview_url) ||
			"config.html?id=" + encodeURIComponent(entry.id);
		actions.appendChild(
			btn(G.EDIT + " " + t("btn.preview"), {
				onClick: function () {
					window.location.href = preview;
				},
				tip: t("tip.preview"),
			}),
		);
		actions.appendChild(
			primary(t("btn.install"), {
				onClick: function () {
					openInstall(entry);
				},
			}),
		);
		head.appendChild(actions);
		c.appendChild(head);

		var parts = [];
		var authors = Array.isArray(entry.authors) ? entry.authors : [];
		var credits = authors.length ? authors.join(", ") : entry.author || "";
		if (credits) parts.push(t("lib.by") + " " + credits);
		if (Array.isArray(entry.tags) && entry.tags.length)
			parts.push(entry.tags.join(", "));
		if (parts.length)
			c.appendChild(el("div", "lc-meta", parts.join("  \u2022  ")));
		if (entry.description)
			c.appendChild(el("div", "lc-desc", entry.description));
		c.addEventListener("click", function (e) {
			if (e.target && e.target.closest && e.target.closest("button,a"))
				return;
			window.location.href = preview;
		});
		return c;
	}
	var ITEM_EMOJI = {
		"minecraft:golden_apple": "\uD83C\uDF4E",
		"minecraft:apple": "\uD83C\uDF4E",
		"minecraft:bread": "\uD83C\uDF5E",
		"minecraft:potion": "\uD83E\uDDEA",
		"minecraft:paper": "\uD83D\uDCC4",
		"minecraft:book": "\uD83D\uDCD6",
		"minecraft:written_book": "\uD83D\uDCD6",
		"minecraft:name_tag": "\uD83C\uDFF7",
		"minecraft:iron_ingot": "\u2B1C",
	};
	function entryIcon(icon) {
		if (!icon) return null;
		var text =
			ITEM_EMOJI[icon] || (icon.indexOf(":") >= 0 ? "\u25C6" : icon);
		return el("span", "lc-ic", text);
	}

	function buildViewerWindow() {
		var win = el("div", "win");
		var entry = viewer.entry;
		var cfg = viewer.cfg;
		var name = entry
			? entry.name || t("cfg.windowSub")
			: t("cfg.windowSub");
		document.title = name + " - AFR";

		win.appendChild(
			windowHeader(el("span", "sec-hdr", G.CLOUD + "  " + name)),
		);

		var body = el("div", "win-body");
		if (!entry || !cfg) {
			var nf = card(t("cfg.notFound"));
			nf.appendChild(el("span", "dim", t("cfg.notFoundDesc")));
			body.appendChild(nf);
			win.appendChild(body);
			win.appendChild(viewerFooter(entry));
			return win;
		}

		if (state.viewerVersion) {
			body.appendChild(
				el(
					"span",
					"warn",
					t("cfg.viewing") + ": v" + state.viewerVersion,
				),
			);
		}

		var meta = card(section(G.PROFILES, t("cfg.meta")));
		meta.appendChild(readOnlyRow(t("lbl.title"), entry.name || ""));
		var authors = Array.isArray(entry.authors) ? entry.authors : [];
		var credits = authors.length ? authors.join(", ") : entry.author || "";
		if (credits) meta.appendChild(readOnlyRow(t("cfg.authors"), credits));
		if (entry.description)
			meta.appendChild(
				readOnlyRow(t("lbl.profile_desc"), entry.description),
			);
		if (Array.isArray(entry.tags) && entry.tags.length)
			meta.appendChild(readOnlyRow(t("cfg.tags"), entry.tags.join(", ")));
		meta.appendChild(
			readOnlyRow(
				t("lbl.version"),
				(entry.version || "?") +
					" (" +
					t("lbl.code") +
					" " +
					(entry.version_code || 0) +
					")",
			),
		);
		var dlRow = el("div", "row");
		if (entry.config_url) {
			var dl = el("a", "btn primary", G.CLOUD + " " + t("cfg.download"));
			dl.href = configAsset(entry.config_url);
			dl.target = "_blank";
			dl.rel = "noopener";
			dlRow.appendChild(dl);
		}
		dlRow.appendChild(
			btn(G.CLOUD + " " + t("sec.library"), { onClick: goLibrary }),
		);
		dlRow.appendChild(
			btn(G.HOME + " " + t("nav.home"), { onClick: goHome }),
		);
		meta.appendChild(dlRow);
		body.appendChild(meta);

		var params = Array.isArray(cfg.parameters) ? cfg.parameters : [];
		if (params.length) {
			var pc = card(section(G.GEAR, t("cfg.params")));
			params.forEach(function (p) {
				if (!p) return;
				if (/^(checkbox|bool)$/i.test(p.param_type || "text"))
					pc.appendChild(
						settingRow(
							p.label || p.id,
							el("span", "set-val", asBool(p) ? G.CHECK : ""),
						),
					);
				else
					pc.appendChild(
						settingRow(
							p.label || p.id,
							el("span", "set-val ellipsis", asText(p)),
						),
					);
			});
			body.appendChild(pc);
		}

		var windows = Array.isArray(cfg.quick_action_windows)
			? cfg.quick_action_windows
			: [];
		if (windows.length) {
			var wc = card(section(G.ACTIONS, t("cfg.windows")));
			windows.forEach(function (w) {
				if (!w) return;
				var line = el("div", "col");
				var whead = el("div", "row");
				whead.appendChild(el("span", "", w.name || w.id));
				whead.appendChild(tag(displayValue(w.style || "list")));
				if (w.bind) whead.appendChild(tag(w.bind));
				line.appendChild(whead);
				var wtags = el("div", "chips");
				(Array.isArray(w.actions) ? w.actions : []).forEach(
					function (a) {
						wtags.appendChild(tag(actionName(cfg, a)));
					},
				);
				(Array.isArray(w.tiles) ? w.tiles : []).forEach(function (a) {
					if (a) wtags.appendChild(tag(actionName(cfg, a)));
				});
				line.appendChild(wtags);
				wc.appendChild(line);
			});
			body.appendChild(wc);
		}

		var actions =
			cfg.rp_actions && typeof cfg.rp_actions === "object"
				? cfg.rp_actions
				: {};
		var keys = Object.keys(actions);
		var ac = el("div", "card");
		var ahead = el("div", "row");
		ahead.appendChild(
			el(
				"span",
				"sec-title",
				section(G.ACTIONS, t("cfg.actions") + " \u2022 " + keys.length),
			),
		);
		ac.appendChild(ahead);
		ac.appendChild(hline());
		var chips = el("div", "chips tight");
		chips.style.flexWrap = "nowrap";
		keys.slice()
			.sort(function (a, b) {
				return ((actions[a] || {}).name || a).localeCompare(
					(actions[b] || {}).name || b,
				);
			})
			.forEach(function (k) {
				chips.appendChild(
					btn(actions[k].name || k, {
						active: k === state.selectedAction,
						onClick: function () {
							state.selectedAction = k;
							render();
						},
					}),
				);
			});
		ac.appendChild(hscroll(chips));
		body.appendChild(ac);

		if (state.selectedAction && actions[state.selectedAction]) {
			body.appendChild(
				actionView(actions[state.selectedAction], state.selectedAction),
			);
		} else {
			body.appendChild(el("span", "dim", t("msg.choose_action")));
		}

		win.appendChild(body);
		win.appendChild(viewerFooter(entry));
		return win;
	}
	function loadVersion(v) {
		if (!v) {
			viewer.cfg = viewer.latestCfg;
			state.viewerVersion = null;
			render();
			return;
		}
		if (!v.config_url) return;
		loadJSON(configAsset(v.config_url)).then(function (cfg) {
			if (cfg) {
				viewer.cfg = cfg;
				state.viewerVersion = v.version || "";
				render();
			}
		});
	}
	function viewerFooter(entry) {
		var f = el("footer", "win-footer");
		if (entry) {
			var label = state.viewerVersion
				? "v" + state.viewerVersion
				: "v" + (entry.version || "?");
			f.appendChild(
				btn(label, { onClick: openVersions, tip: t("cfg.versions") }),
			);
		}
		f.appendChild(grow());
		f.appendChild(
			btn(G.CLOUD + " " + t("sec.library"), { onClick: goLibrary }),
		);
		f.appendChild(btn(G.HOME + " " + t("nav.home"), { onClick: goHome }));
		f.appendChild(dim("v" + VERSION));
		return f;
	}
	function openVersions() {
		var entry = viewer.entry;
		if (!entry) return;
		var win = el("div", "win ver");
		win.appendChild(
			windowHeader(
				el("span", "sec-hdr", G.LIBRARY + "  " + t("cfg.versions")),
			),
		);
		var body = el("div", "win-body plain");
		var c = card();
		c.appendChild(
			btn("v" + (entry.version || "?"), {
				active: !state.viewerVersion,
				onClick: function () {
					loadVersion(null);
					closeTop();
				},
			}),
		);
		var versions = Array.isArray(entry.versions) ? entry.versions : [];
		if (versions.length) c.appendChild(hline());
		versions
			.slice()
			.reverse()
			.forEach(function (v) {
				c.appendChild(
					btn(
						"v" +
							(v.version || "?") +
							(v.date ? "  " + v.date : ""),
						{
							active: state.viewerVersion === v.version,
							onClick: function () {
								loadVersion(v);
								closeTop();
							},
						},
					),
				);
				if (Array.isArray(v.changelog) && v.changelog.length)
					c.appendChild(el("div", "dim", v.changelog.join("; ")));
			});
		body.appendChild(c);
		win.appendChild(body);
		var ftr = el("footer", "win-footer");
		ftr.appendChild(grow());
		ftr.appendChild(btn(t("btn.close"), { onClick: closeTop }));
		win.appendChild(ftr);
		openOverlay(win);
	}
	function actionName(cfg, id) {
		if (!id) return "";
		var actions = cfg.rp_actions || {};
		if (actions[id]) return actions[id].name || id;
		for (var k in actions) if (actions[k].name === id) return id;
		return id;
	}
	function actionView(a, key) {
		var c = card(section(G.EDIT, a.name || key));
		if (a.description) c.appendChild(el("span", "dim", a.description));
		var info = el("div", "row");
		info.appendChild(tag(t("lbl.category") + ": " + (a.category || "-")));
		info.appendChild(
			tag(
				t("lbl.cooldown") +
					": " +
					(a.cooldown || 0) +
					" " +
					t("unit.ms"),
			),
		);
		info.appendChild(
			tag(
				(a.favorite ? G.STAR : G.EMPTY_STAR) +
					" " +
					(a.favorite ? t("btn.favorited") : t("btn.favorite")),
			),
		);
		if (a.bind) info.appendChild(tag(a.bind));
		if (a.icon) info.appendChild(tag(a.icon));
		c.appendChild(info);
		var steps = Array.isArray(a.steps) ? a.steps : [];
		steps.forEach(function (step, i) {
			c.appendChild(stepView(step, i));
		});
		if (!steps.length)
			c.appendChild(el("span", "muted", t("cfg.noActions")));
		return c;
	}
	function stepView(raw, index) {
		var step = Array.isArray(raw) ? raw.slice() : [];
		while (step.length < 6) step.push("");
		var gender = String(step[0]).toLowerCase() === "rpactiongender";
		var c = card(section(G.ACTIONS, t("lbl.step") + " " + (index + 1)));
		var row = el("div", "step-row");
		row.appendChild(tag(displayValue(step[0] || "RPAction")));
		row.appendChild(tag(displayValue(step[1] || "Send")));
		row.appendChild(el("span", "dim", t("lbl.before")));
		row.appendChild(tag(step[2] || "-"));
		row.appendChild(el("span", "dim", t("lbl.after")));
		row.appendChild(tag(step[3] || "-"));
		c.appendChild(row);
		c.appendChild(
			el(
				"div",
				step[4] ? "step-text" : "muted",
				step[4] || t("lbl.text"),
			),
		);
		if (gender)
			c.appendChild(
				el(
					"div",
					step[5] ? "step-sub" : "muted",
					step[5] || t("lbl.text_female_hint"),
				),
			);
		return c;
	}
	function asText(p) {
		var v = p.value !== undefined && p.value !== null ? p.value : p.default;
		return v === undefined || v === null ? "" : String(v);
	}
	function asBool(p) {
		var v = p.value !== undefined && p.value !== null ? p.value : p.default;
		return (
			v === true ||
			String(v).toLowerCase() === "true" ||
			String(v) === "1"
		);
	}

	function loadJSON(url) {
		return fetch(url, { cache: "no-store" })
			.then(function (r) {
				if (!r.ok) throw new Error("HTTP " + r.status);
				return r.json();
			})
			.catch(function () {
				return null;
			});
	}
	function boot() {
		document.documentElement.lang = lang;
		applyStaticI18n();
		if (!document.getElementById("app")) return;

		if (PAGE === "config") {
			var id = new URLSearchParams(location.search).get("id");
			loadJSON("api/configs/library.json").then(function (data) {
				var entries = data && data.configs ? data.configs : [];
				viewer.entry = id
					? entries.filter(function (e) {
							return e.id === id;
						})[0] || null
					: null;
				var configUrl =
					viewer.entry && viewer.entry.config_url
						? configAsset(viewer.entry.config_url)
						: id
							? configAsset(
									"configs/" +
										encodeURIComponent(id) +
										"/config.json",
								)
							: "";
				if (!configUrl) {
					render();
					return;
				}
				loadJSON(configUrl).then(function (cfg) {
					viewer.cfg = cfg;
					viewer.latestCfg = cfg;
					render();
				});
			});
			return;
		}

		Promise.all([
			loadJSON("api/configs/library.json"),
			loadJSON("api/versions/afr/stable.json"),
			loadJSON("api/production-args.json"),
			loadJSON("api/announcements.json"),
		]).then(function (res) {
			library = res[0] && res[0].configs ? res[0].configs : [];
			var ver = res[1] || {};
			state.version = {
				name: ver.version || "",
				code: ver.version_code || 0,
				changelog: ver.changelog || [],
			};
			state.announcements = Array.isArray(res[3]) ? res[3] : [];
			applyProduction(res[2] || {});
			render();
		});
	}
	function applyProduction(prod) {
		if (prod.buttons_enabled === false) return;
		if (!Array.isArray(prod.buttons)) return;
		prod.buttons.slice(0, 2).forEach(function (b) {
			if (b.enabled === false) return;
			var url = b.url || b.link || "";
			if (!url) return;
			extraButtons.push({
				label: pickText(b.text || b.label, "Link"),
				url: url,
			});
		});
	}

	if (document.readyState === "loading")
		document.addEventListener("DOMContentLoaded", boot);
	else boot();
})();

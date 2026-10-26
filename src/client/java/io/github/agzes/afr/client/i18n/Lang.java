// SPDX-License-Identifier: GPL-3.0-only
package io.github.agzes.afr.client.i18n;

import java.util.HashMap;
import java.util.Map;

public final class Lang {

	private static String language = "ru";
	private static final Map<String, String> RU = new HashMap<>();
	private static final Map<String, String> EN = new HashMap<>();

	private Lang() {}

	public static void setLanguage(String lang) {
		language = lang == null || lang.isBlank() ? "ru" : lang;
	}

	public static String language() {
		return language;
	}

	public static String t(String key) {
		Map<String, String> map = "en".equals(language) ? EN : RU;
		String value = map.get(key);
		if (value != null) {
			return value;
		}
		return RU.getOrDefault(key, key);
	}

	public static String format(String key, Object... args) {
		return String.format(t(key), args);
	}

	private static void put(String key, String ru, String en) {
		RU.put(key, ru);
		EN.put(key, en);
	}

	static {
		put("tab.home", "Главная", "Home");
		put("tab.settings", "Настройки", "Settings");
		put("tab.profiles", "Профили", "Profiles");
		put("tab.actions", "Отыгровки", "Actions");
		put("tab.binds", "Бинды", "Binds");
		put("tab.library", "Библиотека", "Library");

		put("sec.about", "О моде", "About");
		put("sec.current_profile", "Текущий профиль", "Current profile");
		put("sec.announcements", "Объявления", "Announcements");
		put("ann.warning", "Предупреждение", "Warning");
		put("ann.deprecated", "Устаревшая версия", "Outdated version");
		put("ann.critical", "Критично", "Critical");
		put("sec.person", "Персонаж", "Character");
		put("sec.interface", "Интерфейс", "Interface");
		put("sec.delays", "Задержки", "Delays");
		put("sec.profiles", "Профили", "Profiles");
		put("sec.import", "Импорт профиля", "Import profile");
		put("sec.params", "Параметры профиля", "Profile parameters");
		put("sec.windows", "Окна действий", "Action windows");
		put("sec.system", "Система", "System");
		put("sec.actions_list", "Отыгровки", "Actions");
		put("sec.updates", "Обновления", "Updates");
		put("sec.status", "Статус", "Status");
		put("sec.changelog", "Что нового", "What's new");
		put("sec.tiles", "Плитки", "Tiles");
		put("sec.step", "Шаг", "Step");

		put("btn.save", "Сохранить", "Save");
		put("btn.export", "Экспорт", "Export");
		put("btn.import", "Импортировать", "Import");
		put("confirm.import", "Импорт профиля", "Import profile");
		put(
			"tip.import_clipboard",
			"Импортировать профиль (JSON: из буфера или файла)",
			"Import a profile (JSON: clipboard or file)"
		);
		put(
			"msg.import_warning",
			"Импортируйте профили только из проверенных источников. Непроверенный профиль может содержать нежелательные или опасные действия (автоотправка в чат, команды). Вы отвечаете за то, что импортируете.",
			"Only import profiles from trusted sources. An untrusted profile may contain unwanted or dangerous actions (auto chat messages, commands). You are responsible for what you import."
		);
		put(
			"msg.clipboard_empty",
			"Буфер обмена пуст",
			"The clipboard is empty"
		);
		put("msg.imported", "Импортирован профиль", "Profile imported");
		put(
			"msg.imported_action",
			"Импортирована отыгровка",
			"Action imported"
		);
		put("btn.new_action", "Новое действие", "New action");
		put("lbl.version", "Версия", "Version");
		put("btn.close", "Закрыть", "Close");
		put("btn.create", "Создать", "Create");
		put("btn.copy", "Копия", "Copy");
		put("btn.delete", "Удалить", "Delete");
		put("btn.refresh", "Обновить", "Refresh");
		put("btn.choose", "Выбрать", "Choose");
		put("btn.autochat", "AutoChat", "AutoChat");
		put("btn.reset_all", "Сбросить все", "Reset all");
		put("btn.make_base", "Сделать базовыми", "Save as base");
		put("btn.pause", "Пауза", "Pause");
		put("btn.resume", "Продолжить", "Resume");
		put("btn.radial", "Радиальное", "Radial");
		put("btn.check_config", "Проверить конфиг", "Check config");
		put("btn.check_updates", "Проверить обновления", "Check updates");
		put("btn.check", "Проверить", "Check");
		put("btn.github", "GitHub", "GitHub");
		put("btn.tutorial", "Обучение", "Tutorial");
		put("btn.settings", "Настройки", "Settings");
		put("btn.preview", "Просмотр", "Preview");
		put("btn.open_link", "Открыть ссылку", "Open link");
		put("btn.colors", "Цвета", "Colors");
		put("btn.install", "Установить", "Install");
		put("btn.update", "Обновить", "Update");
		put("btn.installed", "Установлено", "Installed");
		put("btn.next", "Далее", "Next");
		put("btn.back", "Назад", "Back");
		put("btn.skip", "Пропустить", "Skip");
		put("btn.done", "Готово", "Done");
		put("btn.add_row", "Строка", "Row");
		put("btn.add_col", "Столбец", "Column");
		put("btn.add_param", "Параметр", "Parameter");
		put("btn.add_window", "Окно", "Window");
		put("btn.empty", "Пусто", "Empty");
		put("btn.editor", "Редактор", "Editor");
		put(
			"tip.editor_bind",
			"Бинд редактора настраивается в стандартных настройках Minecraft (Управление).",
			"The editor hotkey is configured in Minecraft's standard Controls settings."
		);
		put("btn.mode_edit", "Изменить", "Edit");
		put("btn.mode_done", "Готово", "Done");

		put("lbl.name", "Имя Фамилия", "Name");
		put("lbl.role", "Должность", "Role");
		put("lbl.gender", "Пол", "Gender");
		put("lbl.male", "Мужской", "Male");
		put("lbl.female", "Женский", "Female");
		put("lbl.status_hud", "Статус в игре", "Status HUD");
		put("lbl.progress", "Прогресс отыгровки", "Action progress");
		put("lbl.notifications", "Уведомления", "Notifications");
		put("lbl.sound", "Звук", "Sound");
		put("lbl.log_actions", "Логировать действия", "Log actions");
		put("lbl.debug_logs", "Подробные логи (debug)", "Verbose logs (debug)");
		put("lbl.metadata", "Метаданные", "Metadata");
		put("lbl.from_library", "из библиотеки", "from library");
		put("btn.fork", "Создать локальную копию", "Create local copy");
		put(
			"btn.update_library",
			"Обновить из библиотеки",
			"Update from library"
		);
		put("sec.from_library", "Из библиотеки", "From library");
		put(
			"msg.metadata_locked",
			"Метаданные принадлежат автору и не редактируются.",
			"Metadata belongs to the author and is read-only."
		);
		put(
			"msg.library_note",
			"Метаданные принадлежат автору и не редактируются. Правки действий помечаются как локальные; обновление из библиотеки может их перезаписать.",
			"Metadata is read-only. Action edits are marked as local; a library update may overwrite them."
		);
		put(
			"msg.library_note_modified",
			"Метаданные принадлежат автору и не редактируются. Профиль изменён локально; обновление из библиотеки может перезаписать правки.",
			"Metadata is read-only. The profile was edited locally; a library update may overwrite your changes."
		);
		put("msg.forked", "Создана локальная копия", "Local copy created");
		put(
			"badge.early",
			"Ранняя версия • возможны баги",
			"Early version • bugs possible"
		);
		put(
			"badge.early.tip",
			"Данная версия AFR является ранней версией. Возможны баги - если нашли баг, сообщите на странице GitHub.",
			"This AFR version is an early release. Bugs are possible - if you find one, please report it on the GitHub page."
		);
		put("diff.description", "описание", "description");
		put("diff.step_count", "шаг.", "step(s)");
		put("btn.discard", "Сбросить", "Discard");
		put(
			"style.samples",
			"Осмотр|Инъекция|Перевязка|Рентген|Нашатырь|Дефибриллятор|Выписка|Ушиб",
			"Checkup|Injection|Bandage|X-Ray|Ammonia|Defibrillator|Discharge|Bruise"
		);
		put(
			"cmd.help",
			"AFR - команды:\n/afr - открыть редактор\n/afr reload - перезагрузить конфиг\n/afr menu - открыть меню действий\n/afr stop - остановить отыгровку\n/afr pause - пауза хоткеев\n/afr editor - редактор\n/afr library - библиотека\n/afr updates - обновления\n/afr status - статус",
			"AFR commands:\n/afr - open the editor\n/afr reload - reload the config\n/afr menu - open the action menu\n/afr stop - stop the action\n/afr pause - pause hotkeys\n/afr editor - editor\n/afr library - library\n/afr updates - updates\n/afr status - status"
		);
		put("lib.mode_all", "Все", "All");
		put("lib.mode_installed", "Установленные", "Installed");
		put("lib.mode_updates", "Обновления", "Updates");
		put("lib.uninstalled", "Удалён", "Uninstalled");
		put("lib.count", "конфигов", "configs");
		put("lib.updates_count", "обновлений", "updates");
		put(
			"lib.search_hint",
			"Поиск по названию, автору, тегам...",
			"Search by name, author, tags..."
		);
		put("btn.uninstall", "Удалить", "Delete");
		put("confirm.delete", "Удаление", "Delete");
		put(
			"msg.confirm_uninstall",
			"Удалить установленный конфиг?",
			"Delete the installed config?"
		);
		put("diff.title", "Обновление", "Update");
		put("diff.none", "Изменений нет", "No changes");
		put("diff.apply", "Применить", "Apply");
		put("diff.fork_apply", "Создать копию и применить", "Copy and apply");
		put("diff.meta", "Метаданные", "Metadata");
		put("diff.settings", "Настройки", "Settings");
		put("diff.params", "Параметры", "Parameters");
		put("diff.actions", "Действия", "Actions");
		put("diff.windows", "Окна", "Windows");
		put("diff.added", "Добавлено", "Added");
		put("diff.removed", "Удалено", "Removed");
		put("diff.changed", "Изменено", "Changed");
		put("diff.parameter", "Параметр", "Parameter");
		put("diff.action", "Действие", "Action");
		put("diff.window", "Окно", "Window");
		put(
			"tip.profile_switch",
			"Показывать кнопку переключения профиля прямо в меню действий.",
			"Show the profile switcher right in the action menu."
		);
		put(
			"tip.auto_stop_screen",
			"Останавливать текущее действие при открытии любого экрана (кроме меню AFR).",
			"Stop the running action when any screen opens (except AFR menus)."
		);
		put(
			"tip.auto_stop_death",
			"Останавливать действие при смерти персонажа.",
			"Stop the action when the character dies."
		);
		put(
			"tip.auto_stop_world",
			"Останавливать действие при выходе из мира или смене сервера.",
			"Stop the action on world change or disconnect."
		);
		put(
			"tip.resume",
			"Запоминать шаг, на котором остановились, и давать продолжить действие.",
			"Remember the step where you stopped and let you resume."
		);
		put(
			"tip.notify_start",
			"Показывать уведомление при запуске действия.",
			"Show a notification when an action starts."
		);
		put(
			"tip.notify_end",
			"Показывать уведомление при завершении/остановке действия.",
			"Show a notification when an action finishes/stops."
		);
		put(
			"tip.default_cooldown",
			"Кулдаун по умолчанию для действий без своего значения, в миллисекундах (0 - выключено).",
			"Default cooldown for actions without their own, in milliseconds (0 - off)."
		);
		put(
			"tip.category",
			"Категория - для группировки и фильтра действий (необязательно).",
			"Category - for grouping and filtering actions (optional)."
		);
		put(
			"tip.cooldown",
			"Кулдаун - минимальная пауза перед повторным запуском этого действия, в миллисекундах (0 - выключено).",
			"Cooldown - minimum pause before re-running this action, in milliseconds (0 - off)."
		);
		put(
			"tip.step_type",
			"Тип шага. Универсальная - один текст; Разделенный по родам - отдельный текст для женского персонажа.",
			"Step type. Universal - one text; Gendered - a separate text for a female character."
		);
		put(
			"tip.step_method",
			"Отправить - сразу отправить текст в чат; Ввод - открыть строку ввода с этим текстом.",
			"Send - send the text right away; Input - open the chat input with this text."
		);
		put(
			"tip.step_before",
			"Задержка ПЕРЕД отправкой этого шага. S-код - это миллисекунды: S100 = 0.1с, S300 = 0.3с, S1000 = 1с.",
			"Delay BEFORE sending this step. An S-code is milliseconds: S100 = 0.1s, S300 = 0.3s, S1000 = 1s."
		);
		put(
			"tip.step_after",
			"Задержка ПОСЛЕ отправки этого шага. S-код - это миллисекунды: S100 = 0.1с, S300 = 0.3с, S1000 = 1с.",
			"Delay AFTER sending this step. An S-code is milliseconds: S100 = 0.1s, S300 = 0.3s, S1000 = 1s."
		);
		put("msg.conflicts", "Конфликты биндов", "Bind conflicts");
		put(
			"about.tagline",
			"AFR - Мод для автоматизации отыгровок",
			"AFR - a mod for automating role-play actions"
		);
		put("tip.close_editor", "Закрыть редактор", "Close editor");
		put(
			"tip.import_profile",
			"Импортировать профиль из файла",
			"Import a profile from a file"
		);
		put(
			"tip.quickmenu",
			"Импорт отыгровок из QuickMenu (файл или вставка JSON)",
			"Import actions from QuickMenu (file or pasted JSON)"
		);
		put(
			"tip.diff",
			"Показать различия с версией из библиотеки",
			"Show differences with the library version"
		);
		put(
			"tip.style_preview",
			"Пример выбранного стиля меню",
			"Preview of the selected menu style"
		);
		put(
			"tip.style_switch",
			"Сменить вид меню: список -> плитки -> радиальное",
			"Switch the menu view: list -> tiles -> radial"
		);
		put("tip.window_size", "Размер окна", "Window size");
		put(
			"tip.fork",
			"Сделать независимую локальную копию",
			"Make an independent local copy"
		);
		put(
			"tip.autochat",
			"Создать отыгровку из многострочного текста",
			"Create an action from multiline text"
		);
		put(
			"tip.binds_base_windows",
			"Сохранить бинды окон как базовые в профиль",
			"Save window binds as base into the profile"
		);
		put(
			"tip.save",
			"Сохранить настройки и бинды",
			"Save settings and binds"
		);
		put(
			"tip.binds_base",
			"Сохранить текущие бинды как базовые в профиль",
			"Save current binds as base into the profile"
		);
		put("tip.step_up", "Шаг выше", "Move step up");
		put("tip.step_down", "Шаг ниже", "Move step down");
		put("val.dup_name", "Дублирующееся имя", "Duplicate name");
		put("val.empty_steps", "Пустые шаги", "Empty steps");
		put("val.unknown_var", "неизвестная переменная", "unknown variable");
		put("notif.multiplier", "Множитель", "Multiplier");
		put(
			"lbl.profile_switch",
			"Выбор профиля в меню",
			"Profile switcher in menu"
		);
		put(
			"lbl.auto_stop_screen",
			"Стоп при открытии экрана",
			"Stop when a screen opens"
		);
		put("lbl.auto_stop_death", "Стоп при смерти", "Stop on death");
		put(
			"lbl.auto_stop_world",
			"Стоп при смене мира/выходе",
			"Stop on world change/disconnect"
		);
		put("lbl.notify_start", "Уведомлять о старте", "Notify on start");
		put("lbl.notify_end", "Уведомлять о конце", "Notify on end");
		put(
			"lbl.resume",
			"Продолжать с прерванного",
			"Resume from where stopped"
		);
		put(
			"lbl.default_cooldown",
			"Кулдаун по умолчанию (мс)",
			"Default cooldown (ms)"
		);
		put("lbl.category", "Категория", "Category");
		put("lbl.cooldown", "Кулдаун (мс)", "Cooldown (ms)");
		put("lbl.author", "Автор", "Author");
		put("lbl.profile_desc", "Описание", "Description");
		put("lbl.profile_name", "Новое имя профиля", "New profile name");
		put("btn.rename", "Переименовать", "Rename");
		put(
			"tip.rename",
			"Переименовать активный профиль (метку и id)",
			"Rename the active profile (label and id)"
		);
		put("msg.renamed", "Профиль переименован", "Profile renamed");
		put(
			"msg.rename_empty",
			"Введите новое имя профиля",
			"Enter a new profile name"
		);
		put("lbl.tags", "Теги (через запятую)", "Tags (comma separated)");
		put("sec.production", "Движок действий", "Action engine");
		put("sec.authoring", "Создание", "Authoring");
		put("btn.continue_action", "Продолжить", "Resume");
		put("btn.validate", "Проверить", "Validate");
		put("btn.duplicate", "Дублировать", "Duplicate");
		put("btn.export_action", "Экспорт действия", "Export action");
		put("msg.cooldown", "Кулдаун", "Cooldown");
		put("lbl.cd", "КД", "CD");
		put("msg.started", "Запущено", "Started");
		put("msg.stopped", "Остановлено", "Stopped");
		put("msg.finished", "Завершено", "Finished");
		put("msg.no_conflicts", "Конфликтов нет", "No conflicts");
		put("msg.no_problems", "Проблем не найдено", "No problems found");
		put("msg.duplicated", "Действие продублировано", "Action duplicated");
		put("msg.action_copied", "Действие скопировано", "Action copied");
		put("lbl.menu_style", "Стиль меню", "Menu style");
		put("lbl.multiplier", "Общий множитель", "Global multiplier");
		put("lbl.text", "Текст", "Text");
		put("lbl.text_female", "Текст (женский вариант)", "Text (female)");
		put("lbl.icon", "Иконка (id предмета)", "Icon (item id)");
		put("lbl.title", "Название", "Title");
		put("lbl.description", "Описание", "Description");
		put("lbl.type", "Тип", "Type");
		put("lbl.style", "Стиль", "Style");
		put("lbl.menu", "Меню", "Menu");
		put("lbl.stop", "Стоп", "Stop");
		put("lbl.restart", "Перезагрузка", "Reload");
		put("lbl.window", "Окно", "Window");
		put("lbl.favorite", "В избранное", "Favorite");
		put("lbl.favorited", "В избранном", "Favorited");
		put("lbl.actions_count", "действий", "actions");
		put("lbl.more", "ещё", "more");
		put("lbl.actions", "Отыгровок", "Actions");
		put("lbl.windows_count", "Окон действий", "Windows");
		put("lbl.params_count", "Параметров", "Parameters");
		put("lbl.profiles_count", "Профилей всего", "Profiles total");
		put("lbl.channel", "Канал", "Channel");
		put("lbl.mod_version", "Версия мода", "Mod version");
		put("lbl.latest_version", "Последняя версия", "Latest version");
		put("lbl.update_available", "Обновление", "Update");
		put("lbl.available", "доступно", "available");
		put("lbl.not_required", "не требуется", "not required");
		put("lbl.search", "Поиск...", "Search...");
		put("lbl.loading", "Загрузка...", "Loading...");
		put("lbl.empty_list", "Ничего не найдено.", "Nothing found.");

		put("msg.saved", "Сохранено", "Saved");
		put(
			"msg.save_error",
			"Не удалось сохранить файлы",
			"Failed to save files"
		);
		put(
			"msg.binds_base",
			"Бинды сохранены как базовые в профиль",
			"Binds saved as profile base"
		);
		put("msg.multiplier", "Множитель", "Multiplier");
		put(
			"msg.reloaded",
			"Конфигурация перезагружена",
			"Configuration reloaded"
		);
		put(
			"msg.enter_name",
			"Впишите имя профиля слева",
			"Enter a profile name"
		);
		put(
			"msg.cannot_delete_last",
			"Нельзя удалить последний профиль",
			"Cannot delete the last profile"
		);
		put("msg.export", "Экспорт", "Exported");
		put("msg.export_error", "Ошибка экспорта", "Export error");
		put("msg.import_error", "Ошибка импорта", "Import error");
		put("msg.config_actual", "Конфиг актуален", "Config is up to date");
		put("msg.config_updated", "Конфиг обновлён", "Config updated");
		put(
			"msg.config_check_error",
			"Ошибка проверки конфига",
			"Config check error"
		);
		put(
			"msg.not_found_in_library",
			"Профиль не найден в библиотеке",
			"Profile not found in library"
		);
		put("msg.no_link", "Нет ссылки на конфиг", "No config link");
		put("msg.invalid_config", "Некорректный конфиг", "Invalid config");
		put("msg.installed", "Установлен", "Installed");
		put("msg.install_error", "Ошибка установки", "Install error");
		put("msg.library_error", "Ошибка загрузки", "Load error");
		put("msg.enter_text", "Введите текст отыгровки", "Enter action text");
		put("msg.library_empty", "Библиотека пока пуста.", "Library is empty.");
		put("msg.check_error", "Ошибка проверки", "Check error");

		put(
			"hint.autochat",
			"Каждая строка - отдельное сообщение. Длинные строки режутся на ≤255 символов.",
			"Each line is a separate message. Long lines are split at ≤255 chars."
		);
		put("hint.press_key", "Нажмите клавишу", "Press a key");
		put(
			"hint.press_key_hint",
			"Backspace - сбросить • Esc - отмена",
			"Backspace - clear • Esc - cancel"
		);
		put(
			"hint.select_action",
			"Выберите отыгровку из списка выше",
			"Select an action above"
		);
		put(
			"hint.select_window",
			"Выберите окно, чтобы настроить вид и состав",
			"Select a window to edit"
		);
		put(
			"hint.tiles_edit",
			"Плитки: клик по ячейке - сменить отыгровку",
			"Tiles: click a cell to change the action"
		);
		put(
			"hint.members",
			"Состав (клик по отыгровке - вкл/выкл)",
			"Members (click to toggle)"
		);
		put(
			"hint.params",
			"Использование: {param:id} или {id}.",
			"Usage: {param:id} or {id}."
		);
		put(
			"hint.import",
			"Положите файл профиля в",
			"Put a profile file into"
		);
		put("hint.no_params", "Нет параметров", "No parameters");
		put("hint.no_files", "Файлов нет", "No files");
		put("hint.click_run", "клик - выполнить", "click to run");
		put("hint.autochat_input", "Введите текст...", "Type here...");
		put(
			"hint.click_run_rmb",
			"клик - выполнить • ПКМ - редактор",
			"click to run • RMB - editor"
		);
		put(
			"hint.radial",
			"наведите и нажмите • Esc - закрыть",
			"hover and click • Esc to close"
		);
		put("hint.open_list", "открыть список", "open the list");
		put("hint.show_list", "☰ Показать списком", "☰ Show as list");

		put("mode.list", "Список", "List");
		put("mode.radial", "Радио", "Radial");
		put("mode.tiles", "Плитки", "Tiles");
		put("mode.compact", "Компактно", "Compact");
		put("mode.rpaction", "Универсальная", "Universal");
		put("mode.gender", "Разделенный по родам", "Gendered");
		put("mode.send", "Отправить", "Send");
		put("mode.input", "Ввод", "Input");
		put("mode.text", "Текст", "Text");
		put("mode.number", "Число", "Number");
		put("mode.checkbox", "Флаг", "Flag");
		put("mode.stable", "Стабильный", "Stable");
		put("mode.beta", "Бета", "Beta");

		put("tutorial.next", "Далее", "Next");
		put("hud.paused", "пауза", "paused");

		put("btn.create_action", "Создать отыгровку", "Create action");
		put("btn.sort", "Сортировка", "Sort");
		put("menu.actions", "Действия", "Actions");
		put("sec.library", "Библиотека конфигов", "Config library");
		put("lbl.library", "Библиотека", "Library");
		put("lib.no_title", "Без названия", "Untitled");
		put(
			"lib.demo_desc",
			"Демонстрационный конфиг: все возможности движка действий.",
			"Demo config: every action-engine feature."
		);
		put(
			"lib.refreshing",
			"Обновление библиотеки...",
			"Refreshing the library..."
		);
		put("lib.refreshed", "Библиотека обновлена", "Library refreshed");
		put(
			"lib.cache_used",
			"Библиотека недоступна - показан кэш",
			"Library unavailable - showing cache"
		);
		put(
			"lib.downloading",
			"Загрузка конфига...",
			"Downloading the config..."
		);
		put(
			"tip.preview",
			"Открыть превью в браузере",
			"Open the preview in a browser"
		);
		put(
			"tip.uninstall",
			"Удалить установленный конфиг",
			"Delete the installed config"
		);
		put("diff.menu", "Меню", "Menu");
		put("diff.stop", "Стоп", "Stop");
		put("diff.restart", "Перезагрузка", "Restart");
		put("diff.name", "имя", "name");
		put("diff.icon", "иконка", "icon");
		put("diff.category", "категория", "category");
		put("diff.cooldown", "кулдаун", "cooldown");
		put("diff.steps", "шаги", "steps");
		put("diff.step_text", "текст шагов", "step text");
		put("diff.updated", "Конфиг обновлён", "Config updated");
		put("diff.local_copy", "локальная", "local copy");
		put(
			"tip.fork_apply",
			"Сохранить текущую версию как локальную копию и поставить обновление",
			"Save the current version as a local copy and apply the update"
		);
		put("lbl.no_profile", "нет профиля", "no profile");
		put("lbl.unsaved", "не сохранено", "unsaved");
		put("btn.theme_off", "Отключить тему", "Disable theme");
		put("lbl.code", "код", "code");
		put("stat.actions", "Отыгровки", "Actions");
		put("stat.windows", "Окна", "Windows");
		put("stat.params", "Параметры", "Parameters");
		put("stat.profiles", "Профили", "Profiles");
		put(
			"feat.editor",
			"Редактор конфигов: отыгровки, шаги, задержки, параметры, окна",
			"Config editor: actions, steps, delays, parameters, windows"
		);
		put(
			"feat.library",
			"Библиотека конфигов с онлайн-установкой и обновлением",
			"Config library with online install and update"
		);
		put(
			"feat.menu",
			"Меню действий: список, радиальное, плитки",
			"Action menu: list, radial, tiles"
		);
		put(
			"feat.binds",
			"Хоткеи с модификаторами (Ctrl/Alt/Shift) и окна быстрых действий",
			"Hotkeys with modifiers (Ctrl/Alt/Shift) and quick action windows"
		);
		put(
			"feat.autochat",
			"AutoChat: многострочный текст -> отыгровка",
			"AutoChat: multiline text -> action"
		);
		put(
			"feat.themes",
			"Темы интерфейса, HUD прогресса и уведомления",
			"UI themes, progress HUD and notifications"
		);
		put("lbl.language", "Язык", "Language");
		put("lbl.theme", "Тема", "Theme");
		put("unit.ms", "мс", "ms");
		put(
			"warn.spam",
			"Не меняйте, если вас не кикает с сервера за спам.",
			"Do not change unless the server kicks you for spam."
		);
		put(
			"msg.put_profile_file",
			"Положите файл профиля в ",
			"Put the profile file into "
		);
		put(
			"lbl.params_hint",
			"шт. • {param:id} в тексте",
			"pcs • {param:id} in text"
		);
		put("btn.edit", "Изменить", "Edit");
		put("msg.no_params", "Параметров нет", "No parameters");
		put("btn.param", "Параметр", "Parameter");
		put("lbl.new_param", "Новый параметр", "New parameter");
		put("btn.window", "Окно", "Window");
		put("lbl.window_n", "Окно ", "Window ");
		put(
			"msg.choose_window",
			"Выберите окно, чтобы настроить вид и состав",
			"Pick a window to configure its layout and content"
		);
		put(
			"tip.tiles",
			"Плитки: клик по ячейке - сменить отыгровку («-» = пусто)",
			"Tiles: click a cell to change the action (\"-\" = empty)"
		);
		put(
			"lbl.content_click",
			"Состав (клик по отыгровке - вкл/выкл):",
			"Content (click an action to toggle):"
		);
		put("btn.row", "+ Строка", "+ Row");
		put("btn.col", "+ Столбец", "+ Column");
		put(
			"msg.no_active_profile",
			"Нет активного профиля",
			"No active profile"
		);
		put(
			"msg.choose_action",
			"Выберите действие из списка выше",
			"Pick an action from the list above"
		);
		put("btn.favorite", "В избранное", "Add to favorites");
		put("btn.favorited", "В избранном", "In favorites");
		put("btn.step", "Шаг", "Step");
		put("lbl.step", "Шаг", "Step");
		put("lbl.text_male", "Текст (муж.)", "Text (male)");
		put("lbl.text_female_hint", "Текст (жен.)", "Text (female)");
		put("msg.press_key", "Нажмите клавишу для «", "Press a key for \"");
		put(
			"msg.press_key_tail",
			"» • Backspace - сбросить • Esc - отмена",
			"\" • Backspace - clear • Esc - cancel"
		);
		put("msg.press_key_short", "... нажмите клавишу", "... press a key");
		put("lbl.window_bullet", "Окно • ", "Window • ");
		put("lbl.check_on_join", "Проверять при входе", "Check on join");
		put(
			"lbl.smart_updates",
			"Умная авто-проверка обновлений",
			"Smart update check"
		);
		put(
			"tip.smart_updates",
			"Проверять обновления каждые 30 минут, пока вы активно пользуетесь модом.",
			"Check for updates every 30 minutes while you are actively using the mod."
		);
		put("btn.suggest_lang", "Предложить язык", "Suggest a language");
		put(
			"tip.suggest_lang",
			"Предложить новый язык или перевод",
			"Suggest a new language or translation"
		);
		put("btn.all_actions", "Все отыгровки", "All actions");
		put(
			"tip.all_actions",
			"Открыть полный список отыгровок",
			"Open the full action list"
		);
		put("setup.params_title", "Настройка параметров", "Parameter setup");
		put(
			"setup.params_hint",
			"У этого конфига есть параметры - настройте их сразу.",
			"This config has parameters - configure them now."
		);
		put(
			"setup.params_hint2",
			"Вы всегда можете настроить эти параметры во вкладке «Профили».",
			"You can always configure these parameters in the Profiles tab."
		);
		put("msg.params_saved", "Параметры сохранены", "Parameters saved");
		put(
			"lbl.auto_update_config",
			"Авто-обновлять конфиг",
			"Auto-update config"
		);
		put("lbl.copy_suffix", "копия", "copy");
		put("lbl.lang_ru", "Русский", "Russian");
		put("lbl.lang_en", "English", "English");
		put("setup.title", "Настройка AFR", "AFR setup");
		put("setup.language", "Выберите язык", "Choose language");
		put("setup.hotkeys", "Горячие клавиши", "Hotkeys");
		put(
			"setup.hotkeys_hint",
			"Нажмите на поле, затем на клавишу, чтобы изменить. Esc - отмена.",
			"Click a field, then press a key to change it. Esc - cancel."
		);
		put("setup.menu_key", "Меню действий", "Action menu");
		put("setup.editor_key", "Редактор", "Editor");
		put("setup.character", "Персонаж", "Character");
		put(
			"setup.character_hint",
			"Подставляется в отыгровки: {Name}, {Role}.",
			"Substituted into actions: {Name}, {Role}."
		);
		put("setup.rp_name", "Имя персонажа (RP)", "Character name (RP)");
		put("setup.rp_role", "Должность / роль", "Position / role");
		put("setup.press_key", "Нажмите клавишу", "Press a key");
		put("setup.next", "Далее", "Next");
		put("setup.finish", "Готово", "Done");
		put(
			"setup.hint",
			"Это окно можно открыть снова: Настройки -> Прочее.",
			"You can reopen this wizard later: Settings -> Misc."
		);
		put("import.title_profile", "Импорт профиля", "Import profile");
		put("import.title_theme", "Импорт темы", "Import theme");
		put("import.title_action", "Импорт отыгровки", "Import action");
		put(
			"import.action_hint",
			"Вставьте JSON отыгровки (как при экспорте) и нажмите «Импортировать».",
			"Paste the action JSON (as exported) and press \"Import\"."
		);
		put(
			"import.paste_hint",
			"Вставьте JSON и нажмите «Импортировать». Импортируйте только из проверенных источников.",
			"Paste JSON and press \"Import\". Only import from trusted sources."
		);
		put(
			"import.theme_hint",
			"Вставьте JSON темы (как при экспорте) и нажмите «Импортировать».",
			"Paste theme JSON (as exported) and press \"Import\"."
		);
		put("import.json_hint", "{ ... }", "{ ... }");
		put("btn.from_clipboard", "Из буфера", "From clipboard");
		put(
			"msg.no_import_file",
			"Подходящий файл не найден в папке экспорта",
			"No matching file found in the exports folder"
		);
		put(
			"msg.import_empty",
			"Вставьте JSON в поле",
			"Paste JSON into the field"
		);
		put(
			"lbl.authors",
			"Авторы (через запятую)",
			"Authors (comma separated)"
		);
		put(
			"lbl.editors",
			"Редакторы (через запятую)",
			"Editors (comma separated)"
		);
		put("sec.versions", "История версий", "Version history");
		put("btn.history", "История", "History");
		put(
			"tip.history",
			"История версий конфига: откат и удаление",
			"Config version history: restore and delete"
		);
		put("btn.restore", "Восстановить", "Restore");
		put("btn.download", "Скачать", "Download");
		put(
			"btn.save_version",
			"Сохранить текущую версию",
			"Save current version"
		);
		put("lbl.current_version", "текущая", "current");
		put("lbl.local_versions", "Локальные версии", "Local versions");
		put("lbl.remote_versions", "Версии из библиотеки", "Library versions");
		put("msg.no_versions", "Сохранённых версий нет", "No saved versions");
		put("msg.version_saved", "Версия сохранена", "Version saved");
		put("msg.restored", "Восстановлено", "Restored");
		put(
			"msg.confirm_delete_version",
			"Удалить сохранённую версию",
			"Delete the saved version"
		);
		put("ph.name", "имя персонажа (RP-имя)", "character name (RP name)");
		put("ph.firstname", "только имя", "first name only");
		put("ph.lastname", "только фамилия", "last name only");
		put("ph.role", "должность / роль", "position / role");
		put(
			"ph.param",
			"значение параметра профиля (напр. {Param:price})",
			"profile parameter value (e.g. {Param:price})"
		);
		put(
			"ph.random",
			"случайный вариант из списка",
			"a random option from the list"
		);
		put("ph.arrows", "клавиши-стрелки", "arrow keys");
		put("ph.special", "специальные клавиши", "special keys");
		put("lbl.before", "до", "before");
		put("lbl.after", "после", "after");
		put("lbl.modified", "уже изменён", "already modified");
		put("cmd.reloaded", "Конфигурация перезагружена", "Config reloaded");
		put("cmd.stopped", "Отыгровка остановлена", "Action stopped");
		put("cmd.hotkeys", "Хоткеи", "Hotkeys");
		put("cmd.paused", "пауза", "paused");
		put("cmd.enabled", "включены", "enabled");
		put("cmd.profile", "Профиль", "Profile");
		put("cmd.actions", "Действий", "Actions");
		put("cmd.running", "Выполняется", "Running");
		put(
			"msg.library_unavailable",
			"Библиотека недоступна",
			"Library unavailable"
		);
		put(
			"msg.no_update_link",
			"У конфига нет ссылки обновления",
			"The config has no update link"
		);
		put(
			"msg.profile_not_in_library",
			"Профиль не найден в библиотеке",
			"Profile not found in the library"
		);
		put(
			"msg.local_profile",
			"Локальный профиль: обновления из библиотеки недоступны",
			"Local profile: library updates are not available"
		);
		put("msg.config_uptodate", "Конфиг актуален", "Config is up to date");
		put("msg.action_error", "Ошибка действия", "Action error");
		put("msg.seasonal_title", "Сезонная тема", "Seasonal theme");
		put("msg.seasonal_button", "Включить тему", "Enable theme");
		put("msg.update_available", "Доступно обновление", "Update available");
		put("lbl.cell", "Ячейка", "Cell");
		put("lbl.empty_slot", "- Пусто", "- Empty");
		put("tip.editor_button", "AFR - редактор", "AFR - editor");
		put("sec.icon_picker", "Выбор иконки", "Pick an icon");
		put("lbl.items_count", "предметов", "items");
		put("sec.qm_import", "Импорт QuickMenu", "QuickMenu import");
		put(
			"qm.paste_hint",
			"Вставьте содержимое quickmenu_data.json (Ctrl+V) и нажмите «Импортировать».",
			"Paste the contents of quickmenu_data.json (Ctrl+V) and press \"Import\"."
		);
		put(
			"qm.json_hint",
			"[ {\"name\":\"...\",\"actions\":[[\"cmd\",\"текст\"...",
			"[ {\"name\":\"...\",\"actions\":[[\"cmd\",\"text\"..."
		);
		put("btn.from_file", "Из файла", "From file");
		put(
			"tip.qm_file",
			"Прочитать config/quickmenu_data.json",
			"Read config/quickmenu_data.json"
		);
		put("qm.file_not_found", "Файл не найден", "File not found");
		put("qm.bad_format", "Неверный формат файла", "Invalid file format");
		put("qm.read_error", "Ошибка чтения файла", "File read error");
		put(
			"qm.paste_json",
			"Вставьте JSON в поле",
			"Paste JSON into the field"
		);
		put(
			"qm.expect_array",
			"Ожидается массив JSON",
			"Expected a JSON array"
		);
		put("qm.bad_json", "Некорректный JSON", "Invalid JSON");
		put("qm.imported", "Импортировано отыгровок", "Actions imported");
		put("qm.import_desc", "Импорт QuickMenu", "QuickMenu import");
		put("sec.theme_colors", "Цвета темы", "Theme colors");
		put(
			"theme.format_hint",
			"Формат #RRGGBB. Изменения применяются сразу.",
			"Format #RRGGBB. Changes apply immediately."
		);
		put(
			"tip.theme_export",
			"Скопировать тему в буфер и сохранить в exports/",
			"Copy the theme to the clipboard and save to exports/"
		);
		put(
			"tip.theme_import",
			"Загрузить тему из буфера обмена (JSON)",
			"Load a theme from the clipboard (JSON)"
		);
		put("btn.reset", "Сброс", "Reset");
		put(
			"tip.theme_reset",
			"Вернуть цвета базовой темы",
			"Restore the base theme colors"
		);
		put("theme.copied", "Скопировано в буфер", "Copied to clipboard");
		put(
			"theme.copied_saved",
			"Скопировано и сохранено",
			"Copied and saved"
		);
		put("theme.imported", "Импортировано цветов", "Colors imported");
		put(
			"theme.bad_clipboard",
			"Некорректный JSON в буфере обмена",
			"Invalid JSON in the clipboard"
		);
		put("tok.window", "Окно", "Window");
		put("tok.header", "Шапка", "Header");
		put("tok.footer", "Подвал", "Footer");
		put("tok.card", "Карточка", "Card");
		put("tok.cardHover", "Карточка (наведение)", "Card (hover)");
		put("tok.fieldBg", "Поле - фон", "Field - background");
		put("tok.fieldBorder", "Поле - рамка", "Field - border");
		put("tok.accent", "Акцент", "Accent");
		put("tok.accentHover", "Акцент (наведение)", "Accent (hover)");
		put("tok.accentDim", "Акцент (тусклый)", "Accent (dim)");
		put("tok.accentText", "Текст на акценте", "Text on accent");
		put("tok.text", "Текст", "Text");
		put("tok.dim", "Текст - тусклый", "Text - dim");
		put("tok.muted", "Текст - приглушённый", "Text - muted");
		put("tok.section", "Заголовки разделов", "Section headers");
		put("tok.good", "Успех", "Success");
		put("tok.warn", "Предупреждение", "Warning");
		put("tok.bad", "Ошибка", "Error");
		put("tok.line", "Линия", "Line");
		put("tip.tag", "Фильтр по тегу", "Filter by tag");
		put("lib.by", "автор", "by");
		put("lib.installed", "установлено", "installed");
		put("lib.update_to", "обновление до", "update to");
		put("lib.incompatible", "несовместимо", "incompatible");
		put(
			"lib.requires",
			"Требуется AFR версии кода",
			"Requires AFR version code"
		);
		put("lib.already", "Уже установлено", "Already installed");
		put("lib.install", "Установить профиль", "Install profile");
		put("lib.update_profile", "Обновить профиль", "Update profile");
		put("upd.channel", "Канал", "Channel");
		put("upd.mod_version", "Версия мода", "Mod version");
		put("upd.latest", "Последняя версия", "Latest version");
		put("upd.update", "Обновление", "Update");
		put("upd.available", "доступно", "available");
		put("upd.not_required", "не требуется", "not required");
		put("upd.checking", "Проверка", "Checking");
		put("upd.result", "Результат", "Result");
		put("upd.channel_toggle", "Канал", "Channel");
		put("upd.no_updates", "Обновлений нет", "No updates");
		put("tut.header", "Обучение", "Tutorial");
		put("tut.example", "Пример", "Example");
		put("tut.press_key", "Нажмите клавишу", "Press a key");
		put("tut.new_profile", "Новый профиль", "New profile");
		put("tut.profile.hospital", "Больница", "Hospital");
		put("tut.profile.police", "Полиция", "Police");
		put("tut.profile.government", "Правительство", "Government");
		put(
			"tut.binds_hint",
			"Нажмите на поле и нажмите клавишу или комбинацию (например Ctrl+Q).",
			"Click a field and press a key or a combo (e.g. Ctrl+Q)."
		);
		put("tut.menu_list", "Списком:", "List:");
		put("tut.menu_tiles", "Плитки:", "Tiles:");
		put(
			"tut.menu_radial",
			"Радиальное: секторы по кругу - зажмите клавишу меню и наведите.",
			"Radial: sectors around the wheel - hold the menu key and point."
		);
		put("tut.demo.greet", "Приветствие", "Greeting");
		put("tut.demo.pill", "Передать таблетку", "Give pill");
		put("tut.demo.inject", "Инъекция", "Injection");
		put("tut.demo.menu", "Меню", "Menu");
		put(
			"tut.feat.editor",
			"Редактор конфигов: отыгровки, шаги, задержки, параметры, окна",
			"Config editor: actions, steps, delays, parameters, windows"
		);
		put(
			"tut.feat.library",
			"Библиотека конфигов с онлайн-установкой и обновлением",
			"Config library with online install and updates"
		);
		put(
			"tut.feat.menu",
			"Меню действий: список, радиальное, плитки",
			"Action menu: list, radial, tiles"
		);
		put(
			"tut.feat.binds",
			"Хоткеи с модификаторами и окна быстрых действий",
			"Hotkeys with modifiers and quick action windows"
		);
		put(
			"tut.feat.misc",
			"HUD, уведомления, темы, импорт и экспорт профилей",
			"HUD, notifications, themes, profile import and export"
		);
		put("tut.p0.title", "Добро пожаловать", "Welcome");
		put(
			"tut.p0.text",
			"AFR - мод для автоматизации отыгровок. Он заменяет старый AHK-FOR-RPM: действия, задержки, параметры и меню описываются конфигами и настраиваются прямо в игре.\n\nРедактор открывается клавишей Right Shift (можно сменить на любой бинд) или командой /afr.",
			"AFR is a mod for automating role-play actions. It replaces the old AHK-FOR-RPM: actions, delays, parameters and menus are described by configs and set up right in the game.\n\nThe editor opens with the Right Shift key or the /afr command."
		);
		put("tut.p1.title", "Профили", "Profiles");
		put(
			"tut.p1.text",
			"Профиль - это отдельный набор отыгровок, параметров и окон. Для каждого профиля свои настройки. Переключение работает в 1 клик, мгновенно.\n\nВо вкладке «Профили» можно создать пустой профиль или копию текущего, переименовать, экспортировать и импортировать, удалить, а также установить готовые конфиги из Библиотеки. Параметры профиля {Param:...} подставляются в тексты отыгровок.",
			"A profile is a separate set of actions, parameters and windows. Each profile has its own settings. Switching is instant, with one click.\n\nIn the Profiles tab you can create an empty profile or a copy, rename, export and import, delete, and install ready-made configs from the Library. Profile parameters {Param:...} are substituted into action texts."
		);
		put("tut.p2.title", "Отыгровки", "Actions");
		put(
			"tut.p2.text",
			"Отыгровка - это последовательность шагов. У каждого шага задаются: тип (универсальная или по родам), метод («Отправить» (сразу в чат) или «Ввод» (открыть строку ввода)), задержки «до» и «после» (S100-S4000), текст шага.\n\nВ тексте могут быть использованы переменные: {Name}, {FirstName}, {Role}, {Param:id}, случайный выбор {A|B|C} и служебные клавиши {Enter}, {Left}, {Tab}. Шаги можно переставлять и удалять.",
			"An action is a sequence of steps. Each step defines:\n• type - universal or gendered;\n• method - \"Send\" (straight to chat) or \"Input\" (open the input line);\n• \"before\" and \"after\" delays - S100-S4000;\n• step text.\n\nVariables are inserted into the text: {Name}, {FirstName}, {Role}, {Param:id}, random pick {A|B|C} and special keys {Enter}, {Left}, {Tab}. Steps can be reordered and deleted."
		);
		put("tut.p3.title", "Бинды", "Binds");
		put(
			"tut.p3.text",
			"Во вкладке «Бинды» назначаются клавиши на отыгровки, окна быстрых действий и системные команды (Меню, Стоп, Перезагрузка).\n\nПоддерживаются комбинации: Alt+Q, Ctrl+Alt+1, Shift+F5 и т.д. Нажмите на поле бинда и нажмите нужную клавишу (Backspace - сбросить, Esc - отмена). Кнопка «Сделать базовыми» сохраняет текущие бинды прямо в профиль.",
			"The Binds tab assigns keys to actions, quick action windows and system commands (Menu, Stop, Reload).\n\nCombos are supported: Alt+Q, Ctrl+Alt+1, Shift+F5 and so on. Click a bind field and press the key (Backspace - clear, Esc - cancel). The \"Save as base\" button writes the current binds into the profile."
		);
		put("tut.p4.title", "Меню и окна", "Menu and windows");
		put(
			"tut.p4.text",
			"Меню действий открывается по клавише (по умолчанию F9, можно изменить) и бывает трёх видов: список, радиальное (круг) и плитки. Вид переключается кнопкой внизу меню.\n\nОкна быстрых действий - отдельные наборы отыгровок со своим биндом (например, основное, редкое, базовое). В радиальном меню показываются избранные и недавние действия; остальные - в списке и плитках.",
			"The action menu opens with a key (F9 by default) and has three views: list, radial and tiles. Switch the view with the button at the bottom of the menu.\n\nQuick action windows are separate sets of actions with their own bind (for example main, rare, base). The radial menu shows favorites and recent actions; the rest are in the list and tiles."
		);
		put("tut.p5.title", "Настройки и темы", "Settings and themes");
		put(
			"tut.p5.text",
			"В «Настройках» указываются имя и роль персонажа, пол, язык и тема. Любую тему можно настроить по цветам (Настройки - Тема - «Цвета»), а также экспортировать и импортировать в JSON.\n\nТам же - задержки S100-S4000 и множитель скорости. Меняйте задержки осторожно: слишком короткие могут привести к кику за спам. Всё сохраняется кнопкой «Сохранить».",
			"In Settings you set the character name and role, gender, language and theme. You can customize the colors of any theme (Settings - Theme - \"Colors\"), export and import it as JSON.\n\nThere are also delays S100-S4000 and a speed multiplier. Change delays carefully: too short may get you kicked for spam. Changes are saved with the \"Save\" button."
		);
		put("tut.p6.title", "Готово", "Done");
		put(
			"tut.p6.text",
			"Готово! Дальше: настройте профиль (или скачайте готовый из библиотеки), добавьте отыгровки и назначьте бинды. Если что-то забыли - вернитесь в обучение кнопкой «Обучение» на «Главной».",
			"Done! Next: set up a profile, add actions and assign binds. If you forgot something, reopen the tutorial with the \"Tutorial\" button on the Home tab."
		);
		put("btn.clipboard", "В буфер", "To clipboard");
		put(
			"msg.copied",
			"Профиль скопирован в буфер",
			"Profile copied to clipboard"
		);
		put("sec.keep_windows", "Хранить в окнах", "Keep in windows");
		put("sec.placeholders", "Переменные", "Variables");
		put(
			"hint.placeholders",
			"Подставляются в текст шага при выполнении. Параметры берутся из профиля.",
			"Substituted into step text at runtime. Parameters come from the profile."
		);
		put("btn.window_settings", "Настройки окон", "Window settings");
		put("sec.favorites", "Избранное", "Favorites");
		put("sec.all_actions", "Все действия", "All actions");
		put("confirm.title", "Подтверждение", "Confirmation");
		put("btn.cancel", "Отмена", "Cancel");
		put("btn.continue", "Продолжить", "Continue");
		put("msg.confirm_reset", "Сбросить все бинды?", "Reset all binds?");
		put(
			"msg.confirm_delete_profile",
			"Удалить профиль?",
			"Delete profile?"
		);
		put(
			"msg.confirm_delete_action",
			"Удалить отыгровку?",
			"Delete action?"
		);
		put("msg.confirm_delete_step", "Удалить шаг?", "Delete step?");
		put("msg.confirm_delete_window", "Удалить окно?", "Delete window?");
		put(
			"msg.confirm_delete_param",
			"Удалить параметр?",
			"Delete parameter?"
		);
		put(
			"msg.confirm_unsaved",
			"Есть несохранённые изменения. Продолжить без сохранения?",
			"There are unsaved changes. Continue without saving?"
		);
		put("msg.empty_action", "У отыгровки нет шагов", "Action has no steps");
	}
}

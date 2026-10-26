**English** • [Русский](CHANGELOG.ru.md)

# Changelog

All notable changes to the AFR project. The format is close to [Keep a Changelog](https://keepachangelog.com/en/1.1.0/), versions follow [Semantic Versioning](https://semver.org/).

## [1.0.0] - 2026-10-25

First early public release.

### Added

- Config editor with tabs: profiles, actions (steps, S100...S4000 delays, texts by gender), parameters, quick action windows, binds.
- Action menu: list, radial, tiles + view switching with one button.
- Config library: catalog, tags, filters (All/Installed/Updates), install/update/remove, offline cache, diff updates.
- Hotkeys with modifiers (Ctrl/Alt/Shift), quick action windows with binds, bind conflict detection.
- HUD: status, action progress, notifications.
- AutoChat: multiline text into an action.
- UI themes (Current, Vesper, Seasonal - server-driven) + full color editor, theme export/import; seasonal themes via `production-args.json`.
- Profile import/export (file/clipboard) + confirmation and a warning to verify sources.
- Import actions from a QuickMenu config (file/JSON paste).
- Localization ru/en; language selection on first launch.
- Tutorial and quick setup on first launch.
- Mod update check and announcements.

### Action engine

- Step methods: send to chat/command or open the input line with text.
- Variables: `{Name}`, `{FirstName}`, `{LastName}`, `{Role}`, `{Param:id}`, `{A|B|C}`, special keys.
- "Before"/"after" step delays (S-codes) and a global multiplier.
- Auto-stop (screen/death/world), ForceStop, resume from an interrupted step, per-action and default cooldown, start/end notifications, watchdog.

### Other

- Lowered minimum requirements: Fabric Loader 0.14.23+, Fabric API 0.91.1+.
- License GPL-3.0-only.
- Web bundle `web/` (site + library API + config viewer) and a local server `bash web/serve.sh`.

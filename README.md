<div align="center">

**English** • [Русский](README.ru.md)

<h1>AFR-Web <sup><kbd>v1.0.0</kbd></sup></h1>

**Landing page, config library and web viewer for the AFR mod.**

</div>

---

## > Running

```bash
bash web/serve.sh              # http://127.0.0.1:8000/
PORT=9000 bash web/serve.sh    # http://127.0.0.1:9000/
```

The server serves files with `Cache-Control: no-store` - after edits the browser gets a fresh version.

## > Pages

- `/` - landing: about the mod, features, installation, links;
- `/library.html` - config library (search, tags, install, preview);
- `/config.html?id=<id>` - config viewer: metadata, parameters, action windows and all actions with steps (type, method, delays, text).

Without JavaScript a static landing and a hint on the library and viewer pages are shown.

## > Library

`api/configs/library.json` is generated from the config folders.

```
api/configs/
  library.json          # generated catalog
  <id>/
    config.json         # latest profile
    v<version>.json     # archived versions
```

After adding or changing a config:

```bash
python3 scripts/sync_library.py --write
python3 scripts/sync_library.py --check
```

## > Data

`api/versions/afr/stable.json` - mod version, `api/production-args.json` - buttons, `api/announcements.json` - announcements.

## > Deploy

The workflow `.github/workflows/pages.yml` publishes `web/` to the `pages` branch on every push to `main`. The mod's default API base is `https://agzes.github.io/AFR/api`.

The site is static, nothing else needs to be deployed.

---

<div align="center">

<kbd>With</kbd> <kbd>❤️</kbd> <kbd>by</kbd> <kbd>Agzes</kbd><br><kbd>pls ⭐ project!</kbd>

</div>

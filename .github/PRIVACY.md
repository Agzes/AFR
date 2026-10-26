**English** • [Русский](PRIVACY.ru.md)

# Privacy

AFR has no telemetry, analytics or accounts. All settings, profiles and action logs are stored locally in `.minecraft/config/afr/` on your device.

## Network requests

| Endpoint                                                       | When                             | What is sent  |
| -------------------------------------------------------------- | -------------------------------- | ------------- |
| `<base>/configs/library.json`                                  | opening the config library       | nothing (GET) |
| config `config_url` from the catalog                           | installing / updating a config   | nothing (GET) |
| `<base>/announcements.json`                                    | joining a world / periodic check | nothing (GET) |
| `<base>/production-args.json`                                  | joining a world / periodic check | nothing (GET) |
| `<base>/versions/afr/stable.json`                              | update check (on join / manual)  | nothing (GET) |
| links in the UI (Discord, config preview, announcement button) | only on user click               | nothing       |

`<base>` defaults to `https://agzes.github.io/AFR/api` and can be overridden with the `-Dafr.cloud=<url>` JVM argument (used by the local test server). There are no other outbound connections; actions only send chat messages / commands through your own Minecraft connection.

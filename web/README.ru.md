<div align="center">

[English](README.md) • **Русский**

<h1>AFR-Web <sup><kbd>v1.0.0</kbd></sup></h1>

**Лендинг, библиотека конфигов и веб-просмотрщик для мода AFR.**

</div>

---

## > Запуск

```bash
bash web/serve.sh              # http://127.0.0.1:8000/
PORT=9000 bash web/serve.sh    # http://127.0.0.1:9000/
```

Сервер отдаёт файлы с `Cache-Control: no-store` - после правок браузер получает свежую версию.

## > Страницы

- `/` - лендинг: о моде, возможности, установка, ссылки;
- `/library.html` - библиотека конфигов (поиск, теги, установка, просмотр);
- `/config.html?id=<id>` - просмотрщик конфига: метаданные, параметры, окна действий и все отыгровки с шагами (тип, способ, задержки, текст).

Без JavaScript показывается статический лендинг и подсказка на страницах библиотеки и просмотрщика.

## > Библиотека

`api/configs/library.json` генерируется из папок с конфигами.

```
api/configs/
  library.json          # сгенерированный каталог
  <id>/
    config.json         # последняя версия профиля
    v<version>.json     # архивные версии
```

После добавления или изменения конфига:

```bash
python3 scripts/sync_library.py --write
python3 scripts/sync_library.py --check
```

## > Данные

`api/versions/afr/stable.json` - версия мода, `api/production-args.json` - кнопки, `api/announcements.json` - объявления.

## > Деплой

Workflow `.github/workflows/pages.yml` публикует `web/` в ветку `pages` при каждом пуше в `main`. Базовый адрес API мода по умолчанию - `https://agzes.github.io/AFR/api`.

Сайт статичный, дополнительно разворачивать ничего не нужно.

---

<div align="center">

<kbd>With</kbd> <kbd>❤️</kbd> <kbd>by</kbd> <kbd>Agzes</kbd><br><kbd>pls ⭐ project!</kbd>

</div>

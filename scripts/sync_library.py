#!/usr/bin/env python3
# SPDX-License-Identifier: GPL-3.0-only
"""Generate web/api/configs/library.json based on confing in api/configs/"""

from __future__ import annotations

import argparse
import difflib
import json
import re
import sys
from datetime import datetime, timezone
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
DEFAULT_CONFIGS = ROOT / "web" / "api" / "configs"

ID_RE = re.compile(r"^[a-z0-9][a-z0-9-]*$")
VERSION_FILE_RE = re.compile(r"^v(\d+\.\d+\.\d+(?:[-+][0-9A-Za-z.-]+)?)\.json$")
DATE_RE = re.compile(r"^\d{4}-\d{2}-\d{2}$")


class SyncError(Exception):
    pass


def fail(path: Path, message: str) -> None:
    raise SyncError(f"{path}: {message}")


def load_json(path: Path) -> dict:
    try:
        data = json.loads(path.read_text(encoding="utf-8"))
    except FileNotFoundError:
        fail(path, "file not found")
    except json.JSONDecodeError as error:
        fail(path, f"invalid JSON: {error}")
    if not isinstance(data, dict):
        fail(path, "expected a JSON object")
    return data


def text(meta: dict, path: Path, key: str, required: bool = True) -> str:
    value = meta.get(key)
    if value is None:
        if required:
            fail(path, f"meta.{key} is required")
        return ""
    if not isinstance(value, str):
        fail(path, f"meta.{key} must be a string")
    value = value.strip()
    if required and not value:
        fail(path, f"meta.{key} must not be empty")
    return value


def number(meta: dict, path: Path, key: str, required: bool = True) -> int:
    value = meta.get(key)
    if value is None:
        if required:
            fail(path, f"meta.{key} is required")
        return 0
    if isinstance(value, bool) or not isinstance(value, int):
        fail(path, f"meta.{key} must be an integer")
    if required and value <= 0:
        fail(path, f"meta.{key} must be greater than zero")
    if not required and value < 0:
        fail(path, f"meta.{key} must not be negative")
    return value


def string_list(meta: dict, path: Path, key: str) -> list[str]:
    value = meta.get(key)
    if value is None:
        return []
    if not isinstance(value, list):
        fail(path, f"meta.{key} must be an array")
    result = []
    for item in value:
        if not isinstance(item, str):
            fail(path, f"meta.{key} must contain only strings")
        item = item.strip()
        if item:
            result.append(item)
    return result


def optional_date(value, path: Path, key: str) -> str | None:
    if value is None:
        return None
    if not isinstance(value, str) or not DATE_RE.match(value):
        fail(path, f"{key} must be a YYYY-MM-DD date")
    return value


def read_catalog_sidecar(directory: Path) -> dict:
    path = directory / "catalog.json"
    if not path.exists():
        return {}
    data = load_json(path)
    result: dict = {}

    icon = data.get("icon")
    if icon is not None:
        if not isinstance(icon, str) or not icon.strip():
            fail(path, "icon must be a non-empty string")
        result["icon"] = icon.strip()

    preview = data.get("preview_url")
    if preview is not None:
        if not isinstance(preview, str) or not preview.strip():
            fail(path, "preview_url must be a non-empty string")
        result["preview_url"] = preview.strip()

    created = optional_date(data.get("created_at"), path, "created_at")
    if created:
        result["created_at"] = created
    updated = optional_date(data.get("updated_at"), path, "updated_at")
    if updated:
        result["updated_at"] = updated

    versions = data.get("versions")
    if versions is not None:
        if not isinstance(versions, dict):
            fail(path, "versions must be an object")
        parsed = {}
        for version, info in versions.items():
            if not isinstance(version, str) or not version.strip():
                fail(path, "versions keys must be non-empty strings")
            if not isinstance(info, dict):
                fail(path, f"versions[{version}] must be an object")
            entry: dict = {}
            date = optional_date(info.get("date"), path, f"versions[{version}].date")
            if date:
                entry["date"] = date
            changelog = info.get("changelog")
            if changelog is not None:
                if not isinstance(changelog, list):
                    fail(path, f"versions[{version}].changelog must be an array")
                lines = []
                for line in changelog:
                    if not isinstance(line, str):
                        fail(
                            path,
                            f"versions[{version}].changelog must contain only strings",
                        )
                    line = line.strip()
                    if line:
                        lines.append(line)
                if lines:
                    entry["changelog"] = lines
            parsed[version] = entry
        result["versions"] = parsed

    return result


def read_profile(path: Path, expected_id: str) -> dict:
    data = load_json(path)
    meta = data.get("meta")
    if not isinstance(meta, dict):
        fail(path, "meta object is required")

    remote_id = text(meta, path, "RemoteId", required=False)
    if remote_id and remote_id != expected_id:
        fail(path, f"meta.RemoteId {remote_id!r} does not match folder {expected_id!r}")

    authors = string_list(meta, path, "Authors")
    if not authors:
        author = text(meta, path, "Author", required=False)
        if author:
            authors = [author]
    for editor in string_list(meta, path, "Editors"):
        if editor not in authors:
            authors.append(editor)

    tags = string_list(meta, path, "Tags")
    icon = text(meta, path, "Icon", required=False)

    return {
        "name": text(meta, path, "Label"),
        "version": text(meta, path, "Version"),
        "version_code": number(meta, path, "VersionCode"),
        "min_afr_version": number(meta, path, "MinAFRVersion", required=False),
        "description": text(meta, path, "Description", required=False),
        "authors": authors,
        "tags": tags,
        "icon": icon,
    }


def pick_icon(profile: dict, catalog: dict) -> str:
    return catalog.get("icon") or profile["icon"] or "minecraft:paper"


def build_entry(directory: Path) -> dict:
    config_id = directory.name
    if not ID_RE.match(config_id):
        fail(
            directory,
            "folder name must match [a-z0-9][a-z0-9-]*",
        )

    latest_path = directory / "config.json"
    if not latest_path.exists():
        fail(directory, "config.json is required")

    catalog = read_catalog_sidecar(directory)
    latest = read_profile(latest_path, config_id)
    files = [(latest_path, latest, True)]

    for path in sorted(directory.glob("v*.json")):
        match = VERSION_FILE_RE.match(path.name)
        if not match:
            fail(
                path,
                "version file name must be v<major>.<minor>.<patch>.json",
            )
        expected_version = match.group(1)
        profile = read_profile(path, config_id)
        if profile["version"] != expected_version:
            fail(
                path,
                "file name version "
                f"{expected_version!r} does not match meta.Version "
                f"{profile['version']!r}",
            )
        files.append((path, profile, False))

    codes: dict[int, str] = {}
    versions: dict[str, str] = {}
    for path, profile, _ in files:
        code = profile["version_code"]
        if code in codes:
            fail(
                path,
                f"duplicate version_code {code} (also used by {codes[code]})",
            )
        codes[code] = path.name
        if profile["version"] in versions:
            fail(
                path,
                f"duplicate version {profile['version']!r} "
                f"(also used by {versions[profile['version']]})",
            )
        versions[profile["version"]] = path.name

    highest = max(codes)
    if latest["version_code"] != highest:
        fail(
            latest_path,
            f"config.json must be the highest version ({highest}), got {latest['version_code']}",
        )

    catalog_versions = catalog.get("versions", {})
    for version in catalog_versions:
        if version not in versions:
            fail(
                directory / "catalog.json",
                f"versions[{version}] has no matching profile version",
            )
    version_entries = []
    for path, profile, _ in sorted(files, key=lambda item: item[1]["version_code"]):
        extra = catalog_versions.get(profile["version"], {})
        entry = {
            "version": profile["version"],
            "version_code": profile["version_code"],
        }
        if extra.get("date"):
            entry["date"] = extra["date"]
        entry["config_url"] = f"configs/{config_id}/{path.name}"
        if extra.get("changelog"):
            entry["changelog"] = extra["changelog"]
        version_entries.append(entry)

    authors = latest["authors"]
    dates = [
        catalog_versions.get(profile["version"], {}).get("date")
        for _, profile, _ in files
    ]
    dates = [date for date in dates if date]
    created_at = catalog.get("created_at") or (min(dates) if dates else None)
    updated_at = catalog.get("updated_at") or (max(dates) if dates else None)

    entry = {
        "id": config_id,
        "name": latest["name"],
        "description": latest["description"],
        "author": authors[0] if authors else "",
        "version": latest["version"],
        "version_code": latest["version_code"],
        "icon": pick_icon(latest, catalog),
        "tags": latest["tags"],
        "min_afr_version": latest["min_afr_version"],
        "preview_url": catalog.get("preview_url") or f"config.html?id={config_id}",
        "config_url": f"configs/{config_id}/config.json",
    }
    if created_at:
        entry["created_at"] = created_at
    if updated_at:
        entry["updated_at"] = updated_at
    entry["authors"] = authors
    entry["versions"] = version_entries
    return entry


def build_catalog(configs_dir: Path) -> list[dict]:
    if not configs_dir.exists():
        fail(configs_dir, "configs directory not found")
    entries = []
    for directory in sorted(configs_dir.iterdir(), key=lambda item: item.name):
        if not directory.is_dir() or directory.name.startswith("."):
            continue
        if not (directory / "config.json").exists():
            fail(directory, "config.json is required")
        entries.append(build_entry(directory))
    entries.sort(key=lambda entry: (entry["name"].casefold(), entry["id"]))
    return entries


def without_timestamp(catalog: dict) -> dict:
    copy = dict(catalog)
    copy.pop("last_updated", None)
    return copy


def now_utc() -> str:
    return (
        datetime.now(timezone.utc)
        .replace(microsecond=0)
        .isoformat()
        .replace("+00:00", "Z")
    )


def load_existing(path: Path) -> dict:
    if not path.exists():
        return {}
    return load_json(path)


def render(data: dict) -> str:
    return json.dumps(data, ensure_ascii=False, indent=2) + "\n"


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument(
        "--check",
        action="store_true",
        help="fail if library.json is out of date instead of writing it",
    )
    parser.add_argument(
        "--write",
        action="store_true",
        help="write library.json when it is out of date",
    )
    parser.add_argument(
        "--configs-dir",
        type=Path,
        default=DEFAULT_CONFIGS,
        help="directory with config folders",
    )
    parser.add_argument(
        "--output",
        type=Path,
        default=None,
        help="catalog path (default: <configs-dir>/library.json)",
    )
    args = parser.parse_args()

    if args.check and args.write:
        parser.error("choose either --check or --write")
    if not args.check and not args.write:
        parser.error("choose --check or --write")

    configs_dir = args.configs_dir.resolve()
    output = (args.output or (configs_dir / "library.json")).resolve()

    try:
        existing = load_existing(output)
        api_version = existing.get("api_version") or "1.0.0"
        if not isinstance(api_version, str):
            fail(output, "api_version must be a string")
        entries = build_catalog(configs_dir)
        expected = {
            "api_version": api_version,
            "last_updated": existing.get("last_updated") or now_utc(),
            "configs": entries,
        }
        up_to_date = without_timestamp(existing) == without_timestamp(
            expected
        ) and bool(existing.get("last_updated"))

        if args.check:
            if up_to_date:
                print(
                    f"library.json is up to date: {len(entries)} config(s), "
                    f"{sum(len(entry['versions']) for entry in entries)} version(s)"
                )
                return 0
            current = render(without_timestamp(existing)) if existing else ""
            wanted = render(without_timestamp(expected))
            diff = difflib.unified_diff(
                current.splitlines(),
                wanted.splitlines(),
                fromfile="library.json (current)",
                tofile="library.json (expected)",
                lineterm="",
            )
            print("\n".join(diff).strip())
            print(
                "\nlibrary.json is out of date; run: python3 scripts/sync_library.py --write",
                file=sys.stderr,
            )
            return 1

        if up_to_date:
            print(
                f"library.json is up to date: {len(entries)} config(s), "
                f"{sum(len(entry['versions']) for entry in entries)} version(s)"
            )
            return 0

        expected["last_updated"] = now_utc()
        output.parent.mkdir(parents=True, exist_ok=True)
        output.write_text(render(expected), encoding="utf-8")
        print(
            f"updated {output}: {len(entries)} config(s), "
            f"{sum(len(entry['versions']) for entry in entries)} version(s)"
        )
        return 0
    except SyncError as error:
        print(f"error: {error}", file=sys.stderr)
        return 1


if __name__ == "__main__":
    raise SystemExit(main())

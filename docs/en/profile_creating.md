**English** • [Русский](../ru/profile_creating.md)

# Guide: creating an AFR profile

A guide from an empty profile to a ready-to-publish config.

---

## 0. Profile

A profile is a set of actions, parameters and windows saved in `config/afr/profiles/<id>.json`.

Profiles come in two kinds:

- **local** (`source: local`) - created by you, fully editable;
- **library** (`source: library`) - downloaded from the library; metadata is read-only, there is a "Create local copy" (fork) button.

## 1. Editor

- Editor key (default **Right Shift**, changed in Minecraft's basic settings) or the `/afr` command.
- **Profiles** tab -> the "New profile name" field -> **Create** (empty) or **Copy** (a copy of the active one).

## 2. Metadata (optional)

In the "Metadata" card fill in:

- **Authors** (comma-separated) - e.g. `YourNick, Co-author`;
- **Editors** (comma-separated) - those who helped with edits;
- **Description** - what the action set is about;
- **Tags** (comma-separated) - `hospital, medicine, rp` (used for library search).

Library profile metadata is filled in by the author - it is not editable locally.
Metadata is optional for local use; when publishing to the library, the author, description and tags must be filled in.

## 3. Parameters (optional, situational)

**Profiles** tab -> the "Parameters" card -> **Edit**:

- **id** - latin letters, no spaces (e.g. `price`);
- **label** - how to show it to the user;
- **type** - `Text` or `Number`;
- **value** - the default.

In step text use `{Param:id}` (e.g. `{Param:price}`).

## 4. Actions and steps

**Actions** tab -> pick an action or **New action**.

An action has:

- **Name**, **Description**, **Category**, **Cooldown** (ms), **Favorite**;
- **Icon** (any Minecraft item);
- **Steps** - the actions themselves.

Each step:

| Field  | Value                                                                                   |
| ------ | --------------------------------------------------------------------------------------- |
| Type   | **Universal** (one text) or **Split by gender** (a separate text for the female gender) |
| Method | **Send** (straight to chat/command) or **Input** (open the chat line with the text)     |
| before | delay BEFORE the step (S-code)                                                          |
| after  | delay AFTER the step (S-code)                                                           |
| Text   | the text itself, with variables                                                         |

**S-codes** are milliseconds: `S100` = 0.1s, `S300` = 0.3s, `S1000` = 1s. There is a global multiplier in the profile settings.

**Commands**: if the text starts with `/`, it is sent as a command (without the slash), otherwise as a message.

## 5. Variables

Substituted into the step text on execution:

| Variable                                   | Meaning                        |
| :----------------------------------------- | :----------------------------- |
| `{Name}` / `{RPName}`                      | character name from settings   |
| `{FirstName}` / `{LastName}`               | name parts                     |
| `{Role}` / `{Position}`                    | role / position                |
| `{Param:id}` (or `{id}`)                   | profile parameter              |
| `{A\|B\|C}`                                | a random variant from the list |
| `{Left} {Right} {Up} {Down} {Tab} {Space}` | special keys (pressed)         |

Unknown `{...}` are left as is - **Check** will highlight them.

## 6. Quick action windows

**Profiles** tab -> the "Windows" card -> **Window**:

- **Name** and **Style**: `List` / `Tiles` / `Radial`;
- for a list - tick the actions (click a chip);
- for tiles - set the grid and click a cell to pick an action;
- a window can be assigned a **bind** (the **Binds** tab).

## 7. Binds

**Binds** tab:

- **System**: Menu, Stop, Reload;
- **Windows**: window keys;
- **Actions**: a key for each action.

Click a field -> press a key (with Ctrl/Alt/Shift modifiers). Conflicts are highlighted in the "Bind conflicts" card. "Make default" moves the current binds into the profile.

## 8. Check

**Actions** tab -> **Check**: highlights duplicate names, empty steps and unknown variables. The card is shown only when there is something to report.

## 9. Export / import

**Profiles** tab:

- **Export** - saves `config/afr/exports/<id>.json`;
- **Copy** - JSON to the clipboard;
- **Import** - a window to paste JSON or load a file (with a warning about trusting the source).

## 10. Saving

The editor saves **only via the "Save" button**. While there are unsaved edits, `⚠ unsaved` is shown in the top right. On close it will ask whether to save or exit without saving.

## 11. Publishing request

When the profile is ready:

1. Make sure the metadata is filled in (authors, description, tags).
2. **Export** the profile to a file.
3. [Open an issue on GitHub](https://github.com/Agzes/AFR/issues/new/choose) or message the author on Discord.
   (this will soon be replaced by an automatic Discord bot)

After approval the config appears in the library: users can install it in one click, and updates are pulled automatically through the single API.

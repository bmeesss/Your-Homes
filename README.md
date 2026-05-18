# Home

Clean SMP homes plugin for Spigot 1.12.2.

Home gives players a simple GUI for saving, managing, renaming, deleting, and teleporting to homes. It supports SQLite by default and MySQL for servers that need external storage.

## Features

- GUI-based home management
- `/home` menu and `/home <number>` quick teleport
- Configurable default home limit
- Permission-based home limits
- Optional teleport cooldown
- SQLite and MySQL storage
- Configurable messages and GUI text
- Java 8 and Spigot 1.12.2 support

## Commands

| Command | Description | Permission |
| --- | --- | --- |
| `/home` | Opens the homes menu. | `home.use` |
| `/homes` | Alias for `/home`. | `home.use` |
| `/home <number>` | Teleports to a saved home slot. | `home.use` |

## Permissions

| Permission | Description | Default |
| --- | --- | --- |
| `home.use` | Allows players to use homes. | Everyone |
| `home.bypass.cooldown` | Bypasses teleport cooldowns. | Operators |
| `home.homes.<amount>` | Sets a player's home limit. Example: `home.homes.10`. | Not set |

The highest valid `home.homes.<amount>` permission wins. Home limits are clamped from 1 to 27.

## Installation

1. Build the plugin with Maven:

```bash
mvn clean package
```

2. Place `target/Home-1.0.0.jar` in your server `plugins` folder.
3. Restart the server.
4. Edit `plugins/Home/config.yml`, `messages.yml`, and `gui.yml` as needed.
5. Restart again after configuration changes.

## Version Support

| Software | Supported Version |
| --- | --- |
| Spigot | 1.12.2 |
| Java | 8+ |
| Storage | SQLite, MySQL |

## Configuration

### `config.yml`

| Option | Description |
| --- | --- |
| `max-homes` | Default home limit for players without a limit permission. |
| `storage.type` | `sqlite` for local storage or `mysql` for database storage. |
| `storage.mysql.*` | MySQL connection settings. Used only when `storage.type` is `mysql`. |
| `settings.teleport-cooldown-seconds` | Cooldown after teleporting. Set to `0` to disable. |
| `settings.home-name-pattern` | Regular expression used to validate renamed homes. |

### `messages.yml`

Controls chat feedback, errors, permission messages, and command responses.

### `gui.yml`

Controls inventory titles, item names, and item lore for the homes menu and manage menu.

## Building From Source

```bash
mvn clean package
```

The compiled plugin jar is created in `target/`.

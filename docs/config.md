# Configuration

SOUL-ASCENSION uses a hybrid configuration layout under `config/uapi/soul-ascension/`:

- `server.toml` contains simple server and gameplay values managed by NeoForge `ModConfigSpec`;
- `client.toml` contains simple local presentation values managed by NeoForge `ModConfigSpec`;
- `attribute_rewards.json` contains the nested stat-to-attribute reward tree.

This split is intentional. TOML keeps scalar settings integrated with NeoForge's normal config lifecycle, while JSON represents `stats -> rewards -> attribute ID` without encoding a tree into flat strings. Moving every setting to JSON would require duplicating NeoForge validation and lifecycle behavior without improving the simple options.

All files are edited manually while the game is stopped. The mod does not provide an in-game configuration editor.

## Loading and recovery

Runtime code reads immutable snapshots populated after NeoForge loads each TOML spec, so it does not access config values prematurely. Missing files are generated with defaults.

`attribute_rewards.json` is validated by a dedicated loader. A malformed file is renamed to `attribute_rewards.json.broken.<timestamp>.bak` and replaced with valid defaults. Version 1.3.0 does not migrate legacy progression or configuration formats.

## TOML layout

`server.toml` is grouped by gameplay concern:

- `[general]` — respawn health behavior, title nameplate display and debug item exposure.
- `[progression]` — maximum level and damage-XP requirement scaling.
- `[attribute_points]` — point allocation caps, administrative decreases and respec loss rules.
- `[anti_abuse]` — repeated-target XP farming protection.
- `[items.soul_lens]` — Soul Lens availability, range and overlay/network timing.
- `[loot]` — master enable for stat Black Book loot injection.

`client.toml` contains only local presentation:

- `[attribute_display]` — visible/hidden attribute filters and category overrides.
- `[character_screen]` — local Character Screen rendering options. The 3D player preview is disabled by default because it uses Minecraft's inventory entity renderer every frame.

See [`ATTRIBUTE_REWARDS.md`](ATTRIBUTE_REWARDS.md) for the JSON schema and supported operations.

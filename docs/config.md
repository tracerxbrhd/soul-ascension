# Configuration

SOUL-ASCENSION uses a hybrid configuration layout under `config/uapi/soul-ascension/`:

- `server.toml` contains simple server and gameplay values managed by NeoForge `ModConfigSpec`;
- `client.toml` contains simple local presentation values managed by NeoForge `ModConfigSpec`;
- `attribute_rewards.json` contains the nested stat-to-attribute reward tree.

This split is intentional. TOML keeps scalar settings integrated with NeoForge's normal config lifecycle, while JSON represents `stats -> rewards -> attribute ID` without encoding a tree into flat strings. Moving every setting to JSON would require duplicating NeoForge validation and lifecycle behavior without improving the simple options.

All files are edited manually while the game is stopped. The mod does not provide an in-game configuration editor.

## Loading and recovery

Runtime code reads immutable snapshots populated after NeoForge loads each TOML spec, so it does not access config values prematurely. Missing files are generated with defaults.

`attribute_rewards.json` is validated by a dedicated loader. A malformed file is renamed to `attribute_rewards.json.broken.<timestamp>.bak` and replaced with valid defaults. On first launch without the JSON file, legacy `[attribute_rewards]` entries from `server.toml` are migrated automatically.

See [`ATTRIBUTE_REWARDS.md`](ATTRIBUTE_REWARDS.md) for the JSON schema and supported operations.

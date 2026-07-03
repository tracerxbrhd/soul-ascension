# SOUL ASCENSION: config-driven attribute rewards

Rewards are configured in `config/uapi/soul-ascension/server.toml` under `attribute_rewards`.

Each semicolon-separated entry uses:

```text
attribute|amount_per_point|operation|min_final|max_final|required_mod|display|category|formatter|enabled
```

Use `-` when a min, max, or required mod is not needed. Operations are `ADD_VALUE`, `ADD_MULTIPLIED_BASE`, and `ADD_MULTIPLIED_TOTAL`. Formatters are `auto`, `number`, `percent`, and `multiplier`. The historical three-field and nine-field forms remain accepted.

Example:

```text
minecraft:generic.step_height|0.04|ADD_VALUE|-|1.01|-|true|mobility|number|true
```

Unknown attributes, unavailable required mods, and malformed entries are logged once and skipped. Modifiers use stable `soul_ascension:stat_<stat>_<index>` IDs and are replaced during login, respawn, dimension changes, stat changes, config reload, and progress reset.

The bundled optional entries use registry IDs verified from Apothic Attributes 2.9.1 and the official Iron's Spells 1.21 source. They are parsed only when their required mod is present.

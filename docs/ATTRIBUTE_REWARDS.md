# SOUL ASCENSION: config-driven attribute rewards

Rewards are configured in `config/uapi/soul-ascension/attribute_rewards.json`. Each stat owns a `rewards` object keyed by the real attribute registry ID:

```json
{
  "format_version": 2,
  "integrations": {
    "epicfight": {
      "enabled": true
    }
  },
  "stats": {
    "strength": {
      "enabled": true,
      "rewards": {
        "minecraft:generic.attack_damage": {
          "enabled": true,
          "amount_per_point": 0.5,
          "operation": "ADD_VALUE",
          "min_final": null,
          "cap": null,
          "required_mod": null,
          "display": true,
          "display_category": "damage",
          "formatter": "number"
        }
      }
    },
    "intelligence": {
      "enabled": true,
      "experience_bonus_per_point": 0.02,
      "affects_vanilla_experience": true,
      "affects_soul_progression": true,
      "rewards": {}
    }
  }
}
```

Supported operations are `ADD_VALUE`, `ADD_MULTIPLIED_BASE` and `ADD_MULTIPLIED_TOTAL`. Formatters are `auto`, `number`, `percent` and `multiplier`. `required_mod` keeps optional integration attributes from becoming hard dependencies.

If the JSON file does not exist, a valid 2.0 default file is generated. A file without
`"format_version": 2` is unsupported, remains untouched and is ignored at runtime; generate a clean
2.0 file instead. Invalid IDs, operations and values in a current file are logged and skipped; they
do not stop the game. Malformed current-format JSON is moved to
`attribute_rewards.json.broken.<timestamp>.bak`, then a valid default file is generated.

Modifiers use stable `soul_ascension:stat_<stat>_<index>` IDs and are replaced during login, respawn, dimension changes, confirmed stat allocation, config reload and progress reset.

## Native Epic Fight rewards

The freshly generated Soul Ascension 2.0 file includes these native rules in the normal reward
tree. They stay dormant while Epic Fight is absent because every rule requires the `epicfight` mod:

| Soul stat | Epic Fight attribute | Amount per point | Operation | Final-value cap |
| --- | --- | ---: | --- | ---: |
| Strength | `epicfight:impact` | `0.05` | `ADD_VALUE` | `3.0` |
| Strength (hidden off-hand mirror) | `epicfight:offhand_impact` | `0.05` | `ADD_VALUE` | `3.0` |
| Endurance | `epicfight:stamina` | `0.5` | `ADD_VALUE` | `40.0` |
| Agility | `epicfight:stamina_regen` | `0.02` | `ADD_VALUE` | `2.0` |

`cap` limits the resulting effective attribute value after all applicable modifiers; it is not a
limit on the number of Soul stat points. The generated reward object also contains
`"required_mod": "epicfight"` and `"native_integration": "epicfight"`. The off-hand mirror has
`"display": false` because it is an implementation detail of Epic Fight dual-wielding; the main
Impact value remains the single player-facing Strength row.

Soul Ascension never injects these entries into an existing configuration. If the native entries
are absent, either add them manually using the table above or regenerate the entire file from a
clean 2.0 installation. Malformed current-format JSON still follows the separate
`attribute_rewards.json.broken.<timestamp>.bak` recovery path.

There are two levels of control:

- Set `integrations.epicfight.enabled` to `false` to skip only rewards marked with
  `"native_integration": "epicfight"`. Other manual Epic Fight rules remain active.
- Set an individual generated reward's `enabled` field to `false` to disable only that mapping.

Removing `native_integration` from an edited generated rule makes it a manual rule, so the global
native-integration toggle no longer controls it.

After these attribute modifiers are applied, the optional bridge uses Epic Fight's official player
capability API to clamp the player's current stamina to the new valid range. This prevents stale
current stamina after a respec or a lower configured maximum. Damage-based Soul XP is independent
of these reward entries: it continues to use NeoForge's final post-damage event exactly once, so
Epic Fight hits do not receive a second integration-specific XP award.

See [`EPIC_FIGHT_INTEGRATION.md`](EPIC_FIGHT_INTEGRATION.md) for supported versions, installation
behavior and a focused manual test matrix.

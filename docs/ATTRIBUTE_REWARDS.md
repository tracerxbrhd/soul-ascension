# SOUL ASCENSION: config-driven attribute rewards

Rewards are configured in `config/uapi/soul-ascension/attribute_rewards.json`. Each stat owns a `rewards` object keyed by the real attribute registry ID:

```json
{
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

On the first 1.2.0 launch, if the JSON file does not exist, legacy `[attribute_rewards]` values from `server.toml` are converted into the new file. Invalid IDs, operations and values are logged and skipped; they do not stop the game. Malformed JSON is moved to `attribute_rewards.json.broken.<timestamp>.bak`, then a valid default file is generated.

Modifiers use stable `soul_ascension:stat_<stat>_<index>` IDs and are replaced during login, respawn, dimension changes, confirmed stat allocation, config reload and progress reset.

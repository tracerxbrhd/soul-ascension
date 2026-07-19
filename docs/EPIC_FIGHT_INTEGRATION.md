# Soul Ascension 2.0: native Epic Fight integration

Soul Ascension provides active Epic Fight compatibility without making Epic Fight a dependency.
The bridge uses a compile-only Epic Fight dependency and its public API, is initialized only when
the `epicfight` mod is present, and is not loaded on installations without Epic Fight. Soul
Ascension packages no Epic Fight classes, code, assets or nested mod JAR.

## Supported versions

The 2.0.0 compatibility contract is:

- Minecraft `1.21.1`;
- NeoForge;
- Epic Fight `>=21.17.3.1` and `<21.18`;
- Epic Fight is optional on both dedicated servers and clients.

Installing Epic Fight activates the bridge automatically; no addon mod is required. A server that
does not use Epic Fight can run the same Soul Ascension JAR unchanged. When a server does use Epic
Fight, follow Epic Fight's own client/server installation requirements for joining players.

## Native stat mapping

Soul Ascension adds the following default mappings to the regular server-authoritative attribute
reward system:

| Soul stat | Epic Fight attribute | Reward per point | Final effective cap |
| --- | --- | ---: | ---: |
| Strength | `epicfight:impact` | `+0.05` | `3.0` |
| Strength (off-hand mirror) | `epicfight:offhand_impact` | `+0.05` | `3.0` |
| Endurance | `epicfight:stamina` | `+0.5` | `40.0` |
| Agility | `epicfight:stamina_regen` | `+0.02` | `2.0` |

All four rules use `ADD_VALUE`. The off-hand mirror is hidden from Soul's attribute list so Strength
still has one readable Impact line, but Epic Fight's separate dual-wield calculation receives the
same bonus. The cap applies to the final effective attribute value, including its base
and other active modifiers, rather than to the Soul stat itself. The normal Soul Ascension refresh
lifecycle applies the modifiers after confirmed allocation, stat books, respec, login, respawn,
dimension change, configuration reload and administrative progress changes.

After applying the attributes, Soul Ascension calls Epic Fight's official player-capability API to
clamp current stamina into the newly valid range. In particular, reducing Endurance or lowering its
configured cap cannot leave current stamina above the new maximum.

Epic Fight keeps separate, weapon-dependent Impact modifiers. Soul Ascension subscribes to the
official `CHANGE_INNATE_SKILL` API hook and reapplies its capped rewards at the end of the server
tick after a main-hand/off-hand weapon change. Swapping weapons therefore cannot leave a stale Soul
modifier calculated against the previous weapon.

## Configuration defaults and controls

Native defaults live in
`config/uapi/soul-ascension/attribute_rewards.json`, alongside all other rewards. They are written
when Soul Ascension 2.0 creates a fresh default file, regardless of whether Epic Fight is installed.
The `required_mod` field leaves them dormant while Epic Fight is absent. Soul Ascension does not
upgrade or merge an older configuration.

Generated mappings are marked as native integration rules:

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
        "epicfight:impact": {
          "enabled": true,
          "amount_per_point": 0.05,
          "operation": "ADD_VALUE",
          "min_final": null,
          "cap": 3.0,
          "required_mod": "epicfight",
          "native_integration": "epicfight",
          "display": true,
          "display_category": "damage",
          "formatter": "number"
        }
      }
    }
  }
}
```

Controls:

1. `integrations.epicfight.enabled: false` disables only objects marked with
   `native_integration: "epicfight"`.
2. A manual Epic Fight rule without that marker remains active even when the native integration is
   disabled globally.
3. `enabled: false` on one reward disables only that object.
4. Removing `native_integration` from an edited generated object converts it into a manual rule.
5. No Epic Fight attribute is resolved or applied while Epic Fight is absent.

Stop the game before editing the file. Soul Ascension 1.x configuration is unsupported; delete or
move it and let 2.0 generate a clean directory. Invalid current-format JSON uses the recovery file
`attribute_rewards.json.broken.<timestamp>.bak` and is regenerated from valid 2.0 defaults.

## Combat progression

Epic Fight does not introduce a second Soul XP path. Soul Ascension continues to calculate damage
progression from NeoForge's final post-damage event, after the combat result is known. An eligible
Epic Fight hit therefore grants Soul XP exactly once and still follows the normal damage amount,
Intelligence multiplier and anti-farming rules.

## Manual validation

Test a clean world and newly generated configuration. Soul Ascension 1.x worlds and player data are
not supported by 2.0.

### Without Epic Fight

1. Start a dedicated server and client with Soul Ascension and U-API, but no Epic Fight.
2. Join, allocate and reset stat points, die/respawn, change dimension and reconnect.
3. Confirm there are no missing-class/linkage errors and normal Soul progression still works.
4. Inspect the release JAR and confirm it contains no `yesman/epicfight` classes, Epic Fight assets
   or nested Epic Fight JAR.

### With Epic Fight

1. Install a supported Epic Fight `21.17.x` release on the server and joining client, then start the
   same copied world.
2. Generate a fresh 2.0 configuration and confirm it contains all four native entries. Confirm an
   existing edited file is not silently modified on later starts.
3. Record Impact, off-hand Impact, maximum Stamina, Stamina Regeneration and current stamina.
   Allocate Strength, Endurance and Agility separately and compare the result with the mapping table;
   confirm a valid off-hand attack receives the mirrored Strength bonus.
4. Test a stat book, respec, reconnect, death/respawn and dimension change. Confirm modifiers do not
   duplicate and current stamina never remains above the resulting maximum.
5. Swap between weapons with different Impact values in both hands. Confirm the Soul modifier is
   recalculated once after the swap and neither hand keeps a modifier based on the previous weapon.
6. Deal one ordinary Epic Fight hit and one skill/guard-related hit that causes damage. Confirm each
   eligible hit awards Soul XP once, with the usual Intelligence and anti-farming behavior.
7. Set one generated entry to `enabled: false`, reload/restart, and confirm only that reward stops.
8. Set `integrations.epicfight.enabled` to `false`. Confirm all marked native entries stop while a
   manually added Epic Fight entry without `native_integration` remains active.
9. Restore the toggle, remove Epic Fight, and restart. Confirm Soul Ascension starts normally and
   ignores dormant Epic Fight rules until the mod is installed again.

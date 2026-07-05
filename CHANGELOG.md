# Changelog

## 1.2.0 - 2026-07-05

- Fixed startup with an empty config directory by separating NeoForge config specs from safe runtime snapshots.
- Added automatic backup and regeneration for malformed `attribute_rewards.json` files.
- Added server-authoritative pending stat allocation with separated Confirm and Cancel controls and live effective-attribute previews.
- Added configurable Intelligence bonuses for vanilla experience and damage-based Soul progression.
- Moved attribute rewards to a validated tree-structured JSON file with legacy TOML migration.
- Reworked Potion of Withered Memory into a standalone drink made with a wither rose, applying Weakness/Nausea for 45 seconds and Poison II/Wither II for nine seconds without vanilla potion variants.
- Restricted player respec to the Amnesia Scroll and Potion of Withered Memory.
- Added a resource-pack-replaceable 3D Soul Lens model exported from Blockbench while retaining spyglass use behavior and the inventory presentation.
- Added throttled and cached Soul Lens profile rendering and moved the Soul Badge recipe up one crafting row.
- Reworked the Character Screen profile header and merged compatible mod attributes into the main categorized attribute page.
- Removed the experimental in-game configuration editor; configuration remains file-based.
- Removed Soul Altar, Concealment Emblem and profile-hiding systems.

## 1.1.1 - 2026-07-04

- Improved the Character Screen layout and server-authenticated profile handling.
- Reworked Soul Lens attribute rows and configurable overlay opacity.

## 1.1.0 - 2026-07-04

- Added the Soul Lens, Soul Badge, Potion of Withered Memory and public character profiles.
- Added cached skin snapshots and a 2D head fallback to public profiles.
- Added server-synchronized Amnesia Scroll loss information.
- Replaced item textures for the badge, potion, debug sigils and Amnesia Scroll.

## 1.0.1 - 2026-07-04

- Fixed dynamic Attack Damage updates without optional attribute or spell mods.
- Changed the project and packaged artifact license to All Rights Reserved.
- Added the license notice to `META-INF/LICENSE` in release JARs.

## 1.0.0

- Initial public release.

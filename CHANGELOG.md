# Changelog

## 3.0.0-beta.2 - 2026-07-22

- Fixed the NeoForge 26.2 item-registration crash caused by constructing items before their IDs
  were assigned.
- Updated vanilla attribute IDs, recipe ingredients and global loot modifiers to their Minecraft
  26.2 formats while retaining compatibility with beta.1 attribute configuration IDs.
- Deferred Soul Lens HUD registration until the Minecraft client window is initialized and now
  requires U-API 3.0.0-beta.2 with lifecycle-safe HUD registration.
- Isolated development run directories by Minecraft version to prevent older mod sets and worlds
  from leaking into 26.2 test runs.

## 3.0.0-beta.1 - 2026-07-22

- Ported Soul Ascension to Minecraft 26.2, NeoForge 26.2.0.28-beta and Java 25.
- Requires U-API 3.0.0-beta.1 and supports direct composite builds from a sibling U-API checkout.
- Migrated character UI, skin snapshots, HUD rendering, item use, attachments, network sending,
  permissions, reload listeners and gameplay events to their 26.2 APIs.
- Replaced the removed baked-model wrapper with 26.2 display-context item-model definitions for
  the 2D inventory and 3D held Soul Lens presentations.
- Migrated selected titles into the new extracted name-tag render state.
- Disabled native Epic Fight integration until Epic Fight publishes a Minecraft 26.2 API artifact.
- Added pull-request CI and updated tagged GitHub/Modrinth releases for prerelease versions.
- This is a clean-install beta line; 1.21.1 player data, worlds and configuration are not migrated.

## 2.0.0 - 2026-07-15

- Requires U-API 2.x (`[2.0.0,3.0.0)`).
- Breaking release: Soul Ascension 2.0 supports clean installations only. Player data, worlds and
  configuration from Soul Ascension 1.x are not migrated or supported.
- Moved the Character Screen onto U-API's retained UI lifecycle and reusable layout/components.
- Added explicit public-profile privacy settings and exact-version rejection for every serialized
  player attachment, including progression, titles, counters and active-title sync state.
- Added bounded U-API profile facets to public profiles without expanding the base player snapshot.
- Hardened Soul Lens privacy, observation throttling and client cache cleanup.
- Added native, optional Epic Fight support for versions `>=21.17.3.1` and `<21.18` through
  Epic Fight's public attribute and player-capability APIs.
- Added balanced default Epic Fight rewards: Strength grants Impact, Endurance grants maximum
  Stamina, and Agility grants Stamina Regeneration, each with a final-value cap.
- Mirrored Strength's Impact reward into Epic Fight's hidden off-hand attribute and refresh capped
  rewards through the official weapon-change hook after main-hand or off-hand swaps.
- Added Epic Fight rules to the freshly generated 2.0 `attribute_rewards.json`; generated rules
  carry `native_integration: "epicfight"` and can be disabled globally or individually.
- Kept Epic Fight outside Soul Ascension's runtime dependencies and packaged JAR. Damage-based Soul
  XP continues to use the final NeoForge damage event exactly once, including Epic Fight combat.
- Preserved the purple visual identity and server-authoritative allocation flow.
- Finalized and frozen the 2.0.0 feature set after client validation; further gameplay/UI changes
  move to a later version.

## 1.3.1 - 2026-07-10

- Release build for the 1.3.x progression line.
- Requires U-API 1.3.1 or newer.
- Refactored the Gradle/U-API dependency metadata so the required U-API version is controlled from `gradle.properties`.

## 1.3.0 - 2026-07-06

- Breaking update: replaced character progression storage with schema 130; old progression/config data may not be compatible and no migration is performed.
- Removed the experimental tree-progression system before release and returned the mod focus to character levels, attribute points, titles, Soul Lens and respec items.
- Removed the related packets, player data, datapack definitions, tab UI, resources, config sections and admin commands.
- Removed old tree-progression modifier cleanup entirely; 1.3.0 worlds are intentionally not compatible with older development saves.
- Fixed maximum-level presentation: the level bar is now full and displays localized `MAX LEVEL` using the synchronized runtime `max_level` value.
- Optimized Character Screen and Soul Lens UI paths by keeping attribute/profile snapshots cached, removing the removed tree's heavy map render path, avoiding attribute model refreshes on non-attribute tabs and disabling the 3D player preview by default.
- Restored stat-specific Black Books for Strength, Endurance, Agility, Intelligence and Perception. Books directly increase the matching stat and immediately apply configured attribute rewards.
- Added data-driven NeoForge loot modifiers for stat Black Books in rare vanilla structure chests.
- Reorganized generated config keys and removed obsolete tree-progression settings.
- Requires U-API 1.3.0 for server-controlled inventory-tab visibility.

## 1.2.3 - 2026-07-05

- Changed Soul Lens model and textures

## 1.2.2 - 2026-07-05

- Fixed stat-upgrade tooltips for optional mod attributes that do not yet have a synchronized client instance.
- Intelligence now previews configured Iron's Spells and other available integration rewards before the first point is confirmed.
- Kept integration-only tooltip lines hidden when their providing mod or attribute registry entry is unavailable.

## 1.2.1 - 2026-07-05

- Separated the Soul Lens inventory icon from its Blockbench model.
- Kept the generated 2D icon in inventories, creative tabs, recipe viewers, item frames and dropped-item rendering.
- Added a client-only context-aware baked model that selects the 3D Soul Lens only in first-person, third-person and spyglass `HEAD` rendering.

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

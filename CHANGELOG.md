# Changelog

## 1.1.1 - 2026-07-04

- Rebuilt Soul Altar as an authenticated Character Screen mode with shared character, stats, title, progress, and attribute panels.
- Restricted respec to a short-lived server-issued Soul Altar session; normal and public character screens can no longer decrease stats.
- Unified profile privacy checks for Soul Badge and Soul Lens and documented operator bypass behavior for integrated-server testing.
- Reworked Soul Lens stats into equal full-width rows and added configurable idle, active, and hidden-profile opacity.

## 1.1.0 - 2026-07-04

- Added the Soul Altar block, animated Eye of Ender renderer, configurable
  respec, and persistent profile visibility controls.
- Added the Emblem of Concealment, workbench recipe,
  fallback activation, persistent privacy data, and optional Curios charm support through U-API.
- Added the Soul Lens spyglass item with a non-blocking, scrollable public-build HUD overlay.
- Replaced vanilla-gray Soul Altar controls with resource-pack-friendly Soul Ascension widgets
  and removed the online-player list from the altar.
- Fixed creative-tab potion stacks losing their potion contents and effects.
- Made Amnesia Scroll and Potion of Withered Memory usable with empty allocations
  and removed their reset chat spam.
- Added cached skin snapshots and a 2D head fallback to public profiles.
- Added the Soul Badge with a survival recipe, self-profile shortcut, and
  server-authoritative read-only profiles for other players.
- Added the Potion of Withered Memory, brewed from a long Potion of Weakness
  with an amethyst shard, as an early survival stat-reset option.
- Removed the redundant Origin Sigil (`debug_reset`).
- Added server-synchronized Amnesia Scroll loss information.
- Replaced the Soul Ascension item textures for the badge, potion, remaining
  debug sigils, and rolled Amnesia Scroll.

## 1.0.1 - 2026-07-04

- Fixed dynamic Attack Damage updates when Apotheosis/Apothic Attributes and
  Iron's Spells 'n Spellbooks are not installed.
- Changed the project and packaged artifact license to All Rights Reserved.
- Added the license notice to `META-INF/LICENSE` in release JARs.

## 1.0.0

- Initial public release.

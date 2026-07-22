# SOUL-ASCENSION

SOUL-ASCENSION is a configurable RPG progression mod. Dealing valid damage advances the character level, earned points improve attributes, titles track achievements, and the character interface presents both vanilla and compatible modded attributes.

## Requirements

- Minecraft 26.2
- NeoForge 26.2.0.28-beta
- Java 25
- U-API 3.0.0-beta.1 (`[3.0.0-beta.1,4.0.0)`)

Epic Fight does not currently publish a Minecraft 26.2 artifact, so native Epic Fight integration
is disabled on this beta line. Soul Ascension remains fully usable without it and contains no Epic
Fight classes or assets. Integration can be restored when a compatible public API is released.

Current version: 3.0.0-beta.1. Mod ID: `soul_ascension`.

Version 3.0 beta is a clean-install-only release. Worlds, player attachments and configuration files
from the Minecraft 1.21.1 line are not supported and are not migrated. Start it with a new world/player
data and a newly generated `config/uapi/soul-ascension/` directory.

## Character progression

The Character Screen stages point changes as a preview. `Confirm` sends one server-validated allocation; `Cancel` or closing the screen discards every pending change. Intelligence grants a configurable bonus to vanilla experience and damage-based Soul progression; the default is 2% per allocated point.

At the configured maximum character level, the level bar is filled and displays `MAX LEVEL`. The limit comes from the synchronized runtime server configuration and is not fixed at level 100.

## Items

- **Soul Badge** opens your editable Character Screen. Using it on another player shows their server-authoritative public character profile; shift-use opens your own screen.
- **Soul Lens** behaves like a spyglass and shows a compact public-build overlay while aimed at another player.
- **Amnesia Scroll** resets allocated attributes. It has no crafting recipe and stacks to 16.
- **Potion of Withered Memory** performs the same respec through a dangerous standalone drink brewed from a long Potion of Weakness and a wither rose.
- **Black Books** are rare no-recipe consumables for concrete stats: Strength, Endurance, Agility, Intelligence and Perception. A book directly raises its matching stat by 1 and immediately applies the configured attribute rewards. They can appear in stronghold libraries, ancient cities, woodland mansions, end city treasure chests and, very rarely, simple dungeon chests.

Respec is available only through the Amnesia Scroll and Potion of Withered Memory. By default all allocated points are refunded; optional point loss can be enabled in the server config.

## Configuration

Configuration files are created in `config/uapi/soul-ascension/` and are edited manually:

- `server.toml` — progression, stat allocation, respec, Soul Lens gameplay rules and loot toggles;
- `client.toml` — local attribute presentation and Character Screen display options;
- `attribute_rewards.json` — the nested stat-to-attribute reward tree.

There is no custom in-game configuration editor. See [`docs/config.md`](docs/config.md) and [`docs/ATTRIBUTE_REWARDS.md`](docs/ATTRIBUTE_REWARDS.md).

Resource-pack paths, including the replaceable Soul Lens model and texture, are documented in [`docs/resourcepacks.md`](docs/resourcepacks.md). Titles and optional integrations are documented in [`docs/TITLES_AND_INTEGRATIONS.md`](docs/TITLES_AND_INTEGRATIONS.md); the current Epic Fight compatibility status is tracked in [`docs/EPIC_FIGHT_INTEGRATION.md`](docs/EPIC_FIGHT_INTEGRATION.md).

Keep a sibling `../u-api` checkout to compile directly against local U-API sources, then build on
Windows with Java 25 using `gradlew.bat build`. Set `-Pu_api_local_path=` to resolve the published
Maven coordinate instead. The resulting artifact is
`build/libs/soul-ascension-3.0.0-beta.1+mc26.2.jar`.

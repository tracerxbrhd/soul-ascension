# SOUL-ASCENSION

Character progression mod providing damage-based levels, configurable attributes, titles and a resource-pack-friendly character interface. U-API is a required dependency.

- Minecraft: 1.21.1
- NeoForge: 21.1.234
- Java: 21
- Version: 1.1.1
- Mod ID: `soul_ascension`
- Required dependency: U-API 1.1.0 or newer

Configuration is written to `config/uapi/soul-ascension/`. The purple SOUL ASCENSION Character UI is the only supported interface.

The Soul Badge opens the owner's editable character screen. Using it on another
player opens a server-authoritative public profile that exposes only level,
title, allocated stats, and a small whitelist of effective vanilla attributes.
Shift-use always opens the owner's screen.

The Potion of Withered Memory is brewed from a long Potion of Weakness and an
amethyst shard. It reuses the Amnesia Scroll reset service and cannot be turned
into splash or lingering variants.

The Soul Altar provides configurable stat respec and profile visibility controls.
The Emblem of Concealment is crafted around a Soul Badge with Ender Eyes,
amethyst and lapis; it works as a Curios charm when available and has a
configurable fallback use. The Soul Lens is crafted from a vanilla spyglass and
shows a server-authoritative public build overlay while held and aimed at a player.

The attribute page uses a scrollable category list and a cached detail pane showing effective/base values and modifier sources. Resource-pack paths are documented in [`docs/resourcepacks.md`](docs/resourcepacks.md).

Branding sources are stored in `docs/branding/`; runtime icon/banner assets are under `assets/soul_ascension/branding/`.

Build on Windows with `gradlew.bat build`. The resulting artifact is `build/libs/soul-ascension-1.1.1.jar`.

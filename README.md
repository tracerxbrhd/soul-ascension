# SOUL-ASCENSION

Character progression mod providing damage-based levels, configurable attributes, titles and a resource-pack-friendly character interface. U-API is a required dependency.

- Minecraft: 1.21.1
- NeoForge: 21.1.234
- Java: 21
- Version: 1.0
- Mod ID: `soul_ascension`
- Required dependency: U-API 1.0 or newer

Configuration is written to `config/uapi/soul-ascension/`. The purple SOUL ASCENSION Character UI is the only supported interface.

The attribute page uses a scrollable category list and a cached detail pane showing effective/base values and modifier sources. Resource-pack paths are documented in [`docs/resourcepacks.md`](docs/resourcepacks.md).

Branding sources are stored in `docs/branding/`; runtime icon/banner assets are under `assets/soul_ascension/branding/`.

Build on Windows with `gradlew.bat build`. The resulting artifact is `build/libs/soul-ascension-1.0.jar`.

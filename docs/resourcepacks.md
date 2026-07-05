# Resource-pack support

SOUL ASCENSION uses the namespace `soul_ascension`. Resource packs can replace the following files without code changes.

## Character screen

- `assets/soul_ascension/textures/gui/sprites/character/panel.png`
- `assets/soul_ascension/textures/gui/sprites/character/inset.png`
- `assets/soul_ascension/textures/gui/sprites/character/section.png`
- `assets/soul_ascension/textures/gui/sprites/character/progress_background.png`
- `assets/soul_ascension/textures/gui/sprites/character/progress_fill.png`
- `assets/soul_ascension/textures/gui/sprites/character/attribute_selected.png`
- `assets/soul_ascension/textures/gui/sprites/character/icon_button_{normal,hovered,selected,disabled}.png`
- `assets/soul_ascension/textures/gui/sprites/character/stat_button.png`
- `assets/soul_ascension/textures/gui/sprites/character/stat_button_highlighted.png`
- `assets/soul_ascension/textures/gui/sprites/character/stat_button_disabled.png`
- `assets/soul_ascension/textures/gui/sprites/character/stat_plus.png`
- `assets/soul_ascension/textures/gui/sprites/character/stat_minus.png`

The `.png.mcmeta` files define nine-slice scaling. Preserve their logical size and border values when changing texture dimensions.

## Character-screen inventory tabs

- `assets/soul_ascension/textures/gui/sprites/character/tab_normal.png`
- `assets/soul_ascension/textures/gui/sprites/character/tab_hovered.png`
- `assets/soul_ascension/textures/gui/sprites/character/tab_pressed.png`
- `assets/soul_ascension/textures/gui/sprites/character/tab_selected.png`
- `assets/soul_ascension/textures/gui/sprites/character/tab_disabled.png`

These sprites are used only while the SOUL ASCENSION screen is open. Vanilla inventory tabs keep the normal Minecraft widget style.

## Icons and branding

- `assets/soul_ascension/textures/gui/icons/*.png`
- `assets/soul_ascension/textures/gui/stats/*.png`
- `assets/soul_ascension/textures/item/*.png`
- `assets/soul_ascension/branding/icon.png`
- `assets/soul_ascension/branding/banner.png`

The NeoForge mod-list image is the root JAR resource `soul_ascension_banner.png`. Resource packs cannot reliably replace root metadata resources; replace it only in a repackaged mod JAR.

`showAsResourcePack=true` is intentionally not enabled: mod assets are already loaded as client resources, while that flag would only add a redundant selectable pack entry.

The Soul Lens uses standard resource-pack paths:

- `assets/soul_ascension/textures/item/soul_lens.png`
- `assets/soul_ascension/models/item/soul_lens.json`

The runtime JSON contains exported three-dimensional geometry and independent transforms for GUI, ground, fixed,
first-person, third-person and `HEAD` contexts. The `HEAD` transform is important because `SpyglassItem` and the
`SPYGLASS_SCOPE` item ability use that context during the vanilla spyglass animation. The `gui` transform keeps the
same model readable as an inventory icon.

A resource pack can replace either file. A replacement model must reference a runtime texture under the
`soul_ascension` namespace and should retain all display contexts above. Blockbench `.bbmodel` files are editable
design sources only; Minecraft loads the exported item-model JSON, not the `.bbmodel` file.

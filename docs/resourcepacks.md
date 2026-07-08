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

The character `.png.mcmeta` files define nine-slice scaling. Preserve their logical size and border values when changing texture dimensions.

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

Black Book icons are normal generated item models:

- `assets/soul_ascension/models/item/black_book_strength.json`
- `assets/soul_ascension/models/item/black_book_endurance.json`
- `assets/soul_ascension/models/item/black_book_agility.json`
- `assets/soul_ascension/models/item/black_book_intelligence.json`
- `assets/soul_ascension/models/item/black_book_perception.json`
- `assets/soul_ascension/textures/item/black_book_strength.png`
- `assets/soul_ascension/textures/item/black_book_endurance.png`
- `assets/soul_ascension/textures/item/black_book_agility.png`
- `assets/soul_ascension/textures/item/black_book_intelligence.png`
- `assets/soul_ascension/textures/item/black_book_perception.png`

The NeoForge mod-list image is the root JAR resource `soul_ascension_banner.png`. Resource packs cannot reliably replace root metadata resources; replace it only in a repackaged mod JAR.

`showAsResourcePack=true` is intentionally not enabled: mod assets are already loaded as client resources, while that flag would only add a redundant selectable pack entry.

The Soul Lens uses standard resource-pack paths:

- `assets/soul_ascension/textures/item/soul_lens.png`
- `assets/soul_ascension/models/item/soul_lens.json`
- `assets/soul_ascension/textures/item/soul_lens_in_hand.png`
- `assets/soul_ascension/models/item/soul_lens_in_hand.json`

The primary `soul_lens.json` is a generated 2D item model. Inventory, creative tabs, recipe viewers, dropped items
and item frames therefore use the normal `soul_lens.png` icon. A client-side baked-model wrapper selects
`soul_lens_in_hand.json` only for first-person, third-person and `HEAD` render contexts.

The in-hand JSON contains exported three-dimensional geometry and independent transforms for first-person,
third-person and `HEAD` contexts. The `HEAD` transform is important because `SpyglassItem` and the `SPYGLASS_SCOPE`
item ability use that context during the vanilla spyglass animation.

### Replacing only the 3D Soul Lens

To replace the Blockbench model without changing the inventory icon, replace only these two files:

- `assets/soul_ascension/models/item/soul_lens_in_hand.json`
- `assets/soul_ascension/textures/item/soul_lens_in_hand.png`

Export the Blockbench project as a Java block/item model, copy the exported JSON to the first path and its texture to
the second path. In the exported JSON, every runtime texture reference must resolve to
`soul_ascension:item/soul_lens_in_hand`. Preserve or retune the `firstperson_*`, `thirdperson_*` and `head` display
transforms; these control how the model sits in the player's hands and at the eye during use.

Do not replace `models/item/soul_lens.json` or `textures/item/soul_lens.png`: those two files are the independent 2D
inventory icon. A resource pack can use the same paths, and `F3+T` reloads the changed model and texture in-game.

A resource pack can replace either of the two in-hand files. A replacement model must reference a runtime texture under the
`soul_ascension` namespace and should retain all display contexts above. Keep both runtime model paths when replacing
the assets so the context-aware wrapper can resolve them. Blockbench `.bbmodel` files are editable design sources
only; Minecraft loads the exported item-model JSON, not the `.bbmodel` file.

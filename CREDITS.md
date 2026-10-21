# Credits

Every third-party asset in the game, with its author, source and licence. No asset is made with an AI
image generator. The original pack downloads are kept outside this repository in
`../assets/packs/<pack>/`; files derived from them are listed under "Used in".

| Asset | Author | Source | Licence | Used in |
|---|---|---|---|---|
| 16x16 DungeonTileset II, v1.7 | 0x72 | https://0x72.itch.io/dungeontileset-ii | CC0 1.0 (public domain) | `world/Palette.java` (its 66-colour palette); sprites and tiles from Session 8 on (see `design/art/sprite-spec.md`) |
| monogram (pixel font) | Vinícius Menézio (datagoblin) | https://datagoblin.itch.io/monogram | CC0 1.0 | `src/main/resources/fonts/monogram.fnt`, `monogram.png` (built from `bitmap/monogram-bitmap.json`; the `•` glyph was added by hand) |
| Pixel UI Pack | Kenney (www.kenney.nl), with Lynn Evers | https://kenney.nl/assets/pixel-ui-pack | CC0 1.0 | `src/main/resources/ui/skin.png` (the "Ancient" grey 9-slices and the small arrow, recoloured to the palette) |
| Tiny Dungeon 1.0 | Kenney (www.kenney.nl) | https://kenney.nl/assets/tiny-dungeon | CC0 1.0 | not used yet; mapped in `design/art/sprite-spec.md` to fill gaps (bat, spider, item icons) |

CC0 asks for no attribution; it is given here anyway. The derived files are rebuilt with
`python -P scripts/build_pixel_assets.py`.

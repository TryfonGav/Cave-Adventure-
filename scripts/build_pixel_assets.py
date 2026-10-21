"""Build the pixel font and UI skin used by the game from the downloaded art packs.

Inputs (see CREDITS.md; the packs live outside the repo in ../assets/packs/):
  monogram/bitmap/monogram-bitmap.json          monogram font by datagoblin (CC0)
  kenney-pixel-ui-pack/9-Slice/Ancient/*.png    Kenney Pixel UI Pack (CC0)
  kenney-pixel-ui-pack/Spritesheet/UIpackSheet_transparent.png

Outputs (committed, so the Maven build needs no Python):
  src/main/resources/fonts/monogram.fnt + monogram.png   BMFont text format
  src/main/resources/ui/skin.png + skin.json             9-patches and cursor

Every output colour is an entry of the 0x72 DungeonTileset II palette (D14),
the same list as world/Palette.java. The script recolours the Kenney pieces into
that palette; it draws nothing new except the 3x3 bullet glyph monogram lacks.

Usage (from Cave-Adventure-/):  python -P scripts/build_pixel_assets.py [packs_dir]
"""
import json
import os
import sys

from PIL import Image

HERE = os.path.dirname(os.path.abspath(__file__))
REPO = os.path.dirname(HERE)
PACKS = sys.argv[1] if len(sys.argv) > 1 else os.path.join(os.path.dirname(REPO), "assets", "packs")
RES = os.path.join(REPO, "src", "main", "resources")

# The 66 colours of 0x72_DungeonTilesetII_v1.7.png (mirrors Palette.ALL).
PALETTE = set("""
222222 483b3a fdf7ed da4e38 775c55 8f4029 3d734f aa8d7a 72d6ce 4ba747 facb3e
62232f 314152 d3bfa9 9f294e 5698cc ee8e2e 417089 49a790 c56025 fccba3 d8a57d
5956bd 5f2d56 333333 ee8a2e b58057 111111 97da3f b6cbcf 2a2a3a c38d70 8a503e
dc4a7b e2b694 21141b e46e33 cae6f5 f78697 323e4f 55a894 494949 3f3f3f d44e52
2b2929 7b8994 23151d 3b3332 545454 ffcc68 38607c 302c2b 3c9f9c 243f4c 550f0a
ffad3b 1f1833 414859 9b1a0a 38262d 140c11 4d4d4d 5c7a56 1d1218 51c43f 3e5442
""".split())

WHITE = (0xFF, 0xFF, 0xFF, 255)  # font glyphs are white so BitmapFont.setColor tints them
FONT_SHADOW = "111111"

# --- Font -------------------------------------------------------------------

LINE_HEIGHT = 12  # monogram cell height
BASE = 10  # rows from the top of the cell to the baseline (caps occupy rows 3-9)
ADVANCE = 6  # monogram is monospaced: 5 px glyph + 1 px gap
# monogram has no U+2022; a 3x3 dot centred on the x-height (rows 5-9), columns 1-3.
EXTRA_GLYPHS = {"•": [0, 0, 0, 0, 0, 0, 14, 14, 14, 0, 0, 0]}


def rgba(hex_colour, alpha=255):
    assert hex_colour in PALETTE, hex_colour
    return tuple(int(hex_colour[i:i + 2], 16) for i in (0, 2, 4)) + (alpha,)


def build_font():
    glyphs = json.load(open(os.path.join(PACKS, "monogram", "bitmap", "monogram-bitmap.json"), encoding="utf8"))
    glyphs.update(EXTRA_GLYPHS)
    chars = sorted(glyphs, key=ord)

    cell_w, cell_h, cols = 9, LINE_HEIGHT + 1, 32
    rows_needed = (len(chars) + cols - 1) // cols
    sheet = Image.new("RGBA", (cols * cell_w, 1 << (rows_needed * cell_h - 1).bit_length()), (0, 0, 0, 0))
    lines = []
    for i, ch in enumerate(chars):
        rows = glyphs[ch]
        ink = [y for y, bits in enumerate(rows) if bits]
        width = max(rows).bit_length()
        advance = max(ADVANCE, width + 1)
        cx, cy = (i % cols) * cell_w, (i // cols) * cell_h
        if not ink:
            lines.append(f"char id={ord(ch)} x={cx} y={cy} width=0 height=0 xoffset=0 yoffset=0 "
                         f"xadvance={advance} page=0 chnl=15")
            continue
        top, bottom = ink[0], ink[-1]
        # Baked 1 px palette shadow down-right, then the glyph on top.
        for y in range(top, bottom + 1):
            for x in range(width):
                if rows[y] >> x & 1:
                    sheet.putpixel((cx + x + 1, cy + y - top + 1), rgba(FONT_SHADOW))
        for y in range(top, bottom + 1):
            for x in range(width):
                if rows[y] >> x & 1:
                    sheet.putpixel((cx + x, cy + y - top), WHITE)
        lines.append(f"char id={ord(ch)} x={cx} y={cy} width={width + 1} height={bottom - top + 2} "
                     f"xoffset=0 yoffset={top} xadvance={advance} page=0 chnl=15")

    out = os.path.join(RES, "fonts")
    os.makedirs(out, exist_ok=True)
    sheet.save(os.path.join(out, "monogram.png"))
    header = [
        'info face="monogram" size=12 bold=0 italic=0 charset="" unicode=1 stretchH=100 smooth=0 aa=1 '
        'padding=0,0,0,0 spacing=0,0',
        f"common lineHeight={LINE_HEIGHT} base={BASE} scaleW={sheet.width} scaleH={sheet.height} pages=1 packed=0",
        'page id=0 file="monogram.png"',
        f"chars count={len(lines)}",
    ]
    with open(os.path.join(out, "monogram.fnt"), "w", encoding="utf8", newline="\n") as f:
        f.write("\n".join(header + lines) + "\n")
    print(f"font: {len(lines)} glyphs, sheet {sheet.size}")


# --- UI skin ----------------------------------------------------------------

def crop_nine(src, cols, rows):
    """Copy the listed source columns/rows into a compact 9-patch (corners + 1 px middles)."""
    out = Image.new("RGBA", (len(cols), len(rows)), (0, 0, 0, 0))
    for oy, sy in enumerate(rows):
        for ox, sx in enumerate(cols):
            out.putpixel((ox, oy), src.getpixel((sx, sy)))
    return out


def recolour(img, mapping):
    out = img.copy()
    for y in range(img.height):
        for x in range(img.width):
            p = img.getpixel((x, y))
            if p[3]:
                out.putpixel((x, y), rgba(mapping["%02x%02x%02x" % p[:3]]))
    return out


def build_skin():
    ancient = os.path.join(PACKS, "kenney-pixel-ui-pack", "9-Slice", "Ancient")
    raised = Image.open(os.path.join(ancient, "grey.png")).convert("RGBA")
    pressed = Image.open(os.path.join(ancient, "grey_pressed.png")).convert("RGBA")
    sheet = Image.open(os.path.join(PACKS, "kenney-pixel-ui-pack", "Spritesheet",
                                    "UIpackSheet_transparent.png")).convert("RGBA")

    # Kenney "Ancient" grey: 9b9dad outer rim, 727685 inner line, 838796 body, 616374 drop shadow.
    # The 2 px rim and the 4 px bottom (rim + shadow) are kept; the body's scratch marks are not
    # (a stretched centre must be flat).
    panel = recolour(crop_nine(raised, [0, 1, 24, 46, 47], [0, 1, 24, 44, 45, 46, 47]),
                     {"9b9dad": "775c55", "727685": "111111", "838796": "222222", "616374": "140c11"})
    inset = recolour(crop_nine(pressed, [0, 1, 24, 46, 47], [0, 1, 24, 46, 47]),
                     {"9b9dad": "3b3332", "727685": "111111", "838796": "1d1218"})
    selection = recolour(crop_nine(pressed, [0, 1, 24, 46, 47], [0, 1, 24, 46, 47]),
                         {"9b9dad": "facb3e", "727685": "8f4029", "838796": "62232f"})
    # Small yellow right-pointing arrow, sheet tile (col 21, row 24); 16 px tiles, 2 px margin.
    tx, ty = 21 * 18, 24 * 18
    arrow = sheet.crop((tx, ty, tx + 16, ty + 16))
    arrow = arrow.crop(arrow.getbbox())
    cursor = recolour(arrow, {"cda400": "c56025", "ffd948": "ffcc68", "ffcc00": "facb3e"})

    pieces = [
        ("panel", panel, {"left": 2, "right": 2, "top": 2, "bottom": 4}),
        ("inset", inset, {"left": 2, "right": 2, "top": 2, "bottom": 2}),
        ("selection", selection, {"left": 2, "right": 2, "top": 2, "bottom": 2}),
        ("cursor", cursor, None),
    ]
    skin = Image.new("RGBA", (64, 16), (0, 0, 0, 0))
    regions, x = {}, 0
    for name, img, splits in pieces:
        skin.paste(img, (x, 0))
        region = {"x": x, "y": 0, "w": img.width, "h": img.height}
        if splits:
            region.update(splits)
        regions[name] = region
        x += img.width + 2

    out = os.path.join(RES, "ui")
    os.makedirs(out, exist_ok=True)
    skin.save(os.path.join(out, "skin.png"))
    with open(os.path.join(out, "skin.json"), "w", encoding="utf8", newline="\n") as f:
        json.dump(regions, f, indent=2)
        f.write("\n")
    print("skin:", regions)


def check_palette(path):
    img = Image.open(path).convert("RGBA")
    used = {"%02x%02x%02x" % p[:3] for p in img.getdata() if p[3]}
    stray = used - PALETTE - {"ffffff"}
    assert not stray, f"{path}: colours outside the palette: {sorted(stray)}"


if __name__ == "__main__":
    build_font()
    build_skin()
    check_palette(os.path.join(RES, "fonts", "monogram.png"))
    check_palette(os.path.join(RES, "ui", "skin.png"))
    print("all colours are palette entries")

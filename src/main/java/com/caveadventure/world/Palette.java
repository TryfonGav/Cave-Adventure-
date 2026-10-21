package com.caveadventure.world;

import com.badlogic.gdx.graphics.Color;

/**
 * The game's single colour palette (decision D14): the 66 colours of the 0x72
 * DungeonTileset II art pack. Every colour the game draws comes from here, optionally
 * with an alpha via {@link #alpha(Color, float)}. The named constants are the entries
 * the code uses; {@link #ALL} lists the whole pack palette so the sprites added later
 * need no recolouring.
 */
public final class Palette {

    // --- Neutrals, darkest first ---
    /** Near-black ink: outlines, shadows, fog. */
    public static final Color INK = hex(0x111111);
    /** Warm black: the void outside the map and menu backdrops. */
    public static final Color VOID = hex(0x140c11);
    /** Very dark plum-black: wells, inset panel bodies. */
    public static final Color NIGHT = hex(0x1d1218);
    /** Dark soot. */
    public static final Color SOOT = hex(0x21141b);
    /** Charcoal: panel bodies. */
    public static final Color CHARCOAL = hex(0x222222);
    /** Dark warm grey. */
    public static final Color ASH = hex(0x2b2929);
    /** Dark slate. */
    public static final Color SLATE_DARK = hex(0x2a2a3a);
    /** Dark stone brown: default floor. */
    public static final Color STONE_DARK = hex(0x3b3332);
    /** Stone brown: dark walls, wood shadows. */
    public static final Color STONE = hex(0x483b3a);
    /** Light stone: wall faces, panel rims. */
    public static final Color STONE_LIGHT = hex(0x775c55);
    /** Pale stone: muted text. */
    public static final Color STONE_PALE = hex(0xaa8d7a);
    /** Bone: body text. */
    public static final Color BONE = hex(0xd3bfa9);
    /** Off-white highlight. */
    public static final Color WHITE = hex(0xfdf7ed);
    /** Neutral greys. */
    public static final Color GREY_DARK = hex(0x333333);
    public static final Color GREY = hex(0x494949);

    // --- Blues and purples ---
    /** Deep indigo. */
    public static final Color INDIGO = hex(0x1f1833);
    /** Dark teal. */
    public static final Color TEAL_DARK = hex(0x243f4c);
    /** Blue slate. */
    public static final Color SLATE = hex(0x323e4f);
    /** Light blue slate. */
    public static final Color SLATE_LIGHT = hex(0x414859);
    /** Deep sea blue. */
    public static final Color SEA_DARK = hex(0x38607c);
    /** Sea blue: water. */
    public static final Color SEA = hex(0x417089);
    /** Sky blue. */
    public static final Color SKY = hex(0x5698cc);
    /** Cyan. */
    public static final Color CYAN = hex(0x72d6ce);
    /** Frost grey-blue. */
    public static final Color FROST = hex(0xb6cbcf);
    /** Pale ice. */
    public static final Color ICE = hex(0xcae6f5);
    /** Steel grey-blue. */
    public static final Color STEEL = hex(0x7b8994);
    /** Violet. */
    public static final Color VIOLET = hex(0x5956bd);
    /** Plum. */
    public static final Color PLUM = hex(0x5f2d56);

    // --- Greens ---
    /** Dark moss. */
    public static final Color MOSS_DARK = hex(0x3e5442);
    /** Moss grey-green. */
    public static final Color MOSS = hex(0x5c7a56);
    /** Forest green. */
    public static final Color FOREST = hex(0x3d734f);
    /** Leaf green. */
    public static final Color GREEN = hex(0x4ba747);
    /** Bright green. */
    public static final Color GREEN_BRIGHT = hex(0x51c43f);
    /** Lime. */
    public static final Color LIME = hex(0x97da3f);

    // --- Reds, oranges, golds ---
    /** Dark umber. */
    public static final Color UMBER = hex(0x38262d);
    /** Dark blood red. */
    public static final Color BLOOD_DARK = hex(0x550f0a);
    /** Blood red. */
    public static final Color BLOOD = hex(0x9b1a0a);
    /** Wine. */
    public static final Color WINE = hex(0x62232f);
    /** Crimson. */
    public static final Color CRIMSON = hex(0x9f294e);
    /** Rose. */
    public static final Color ROSE = hex(0xdc4a7b);
    /** Red: health, danger, lava. */
    public static final Color RED = hex(0xda4e38);
    /** Rust brown. */
    public static final Color RUST = hex(0x8f4029);
    /** Wood brown. */
    public static final Color WOOD = hex(0x8a503e);
    /** Light wood. */
    public static final Color WOOD_LIGHT = hex(0xb58057);
    /** Copper orange. */
    public static final Color COPPER = hex(0xc56025);
    /** Flame orange. */
    public static final Color FLAME = hex(0xe46e33);
    /** Ember orange. */
    public static final Color ORANGE = hex(0xee8e2e);
    /** Amber. */
    public static final Color AMBER = hex(0xffad3b);
    /** Gold. */
    public static final Color GOLD = hex(0xfacb3e);
    /** Light gold. */
    public static final Color GOLD_LIGHT = hex(0xffcc68);

    /** Every colour of the 0x72 DungeonTileset II v1.7 sheet, as 0xRRGGBB. */
    public static final int[] ALL = {
            0x222222, 0x483b3a, 0xfdf7ed, 0xda4e38, 0x775c55, 0x8f4029, 0x3d734f, 0xaa8d7a, 0x72d6ce,
            0x4ba747, 0xfacb3e, 0x62232f, 0x314152, 0xd3bfa9, 0x9f294e, 0x5698cc, 0xee8e2e, 0x417089,
            0x49a790, 0xc56025, 0xfccba3, 0xd8a57d, 0x5956bd, 0x5f2d56, 0x333333, 0xee8a2e, 0xb58057,
            0x111111, 0x97da3f, 0xb6cbcf, 0x2a2a3a, 0xc38d70, 0x8a503e, 0xdc4a7b, 0xe2b694, 0x21141b,
            0xe46e33, 0xcae6f5, 0xf78697, 0x323e4f, 0x55a894, 0x494949, 0x3f3f3f, 0xd44e52, 0x2b2929,
            0x7b8994, 0x23151d, 0x3b3332, 0x545454, 0xffcc68, 0x38607c, 0x302c2b, 0x3c9f9c, 0x243f4c,
            0x550f0a, 0xffad3b, 0x1f1833, 0x414859, 0x9b1a0a, 0x38262d, 0x140c11, 0x4d4d4d, 0x5c7a56,
            0x1d1218, 0x51c43f, 0x3e5442
    };

    private Palette() {
    }

    /** An opaque colour from a 0xRRGGBB value. */
    static Color hex(int rgb) {
        return new Color(((rgb >> 16) & 0xff) / 255f, ((rgb >> 8) & 0xff) / 255f, (rgb & 0xff) / 255f, 1f);
    }

    /** A new colour with a palette entry's RGB and the given alpha (for overlays and glows). */
    public static Color alpha(Color entry, float alpha) {
        return new Color(entry.r, entry.g, entry.b, alpha);
    }

    /** The 0xRRGGBB value of a colour, ignoring alpha. */
    public static int rgb(Color c) {
        return (Math.round(c.r * 255) << 16) | (Math.round(c.g * 255) << 8) | Math.round(c.b * 255);
    }

    /** Whether a colour's RGB (alpha ignored) is one of the pack palette's colours. */
    public static boolean contains(Color c) {
        int value = rgb(c);
        for (int entry : ALL) {
            if (entry == value)
                return true;
        }
        return false;
    }
}

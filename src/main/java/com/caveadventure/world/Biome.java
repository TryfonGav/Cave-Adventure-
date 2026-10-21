package com.caveadventure.world;

import com.badlogic.gdx.graphics.Color;

/**
 * Biome themes for different floor groups. Each biome defines unique tile
 * colors,
 * ambient effects, and hazard types. All colours are {@link Palette} entries.
 */
public enum Biome {
    CRYSTAL_CAVES("Crystal Caves", 1, 3,
            Palette.SLATE, // floor
            Palette.SEA_DARK, // wall
            Palette.SLATE_LIGHT, // wall shade
            Palette.SKY, // accent
            Palette.alpha(Palette.SEA, 0.3f)), // ambient glow

    MUSHROOM_GROTTO("Mushroom Grotto", 4, 5,
            Palette.STONE_DARK,
            Palette.MOSS,
            Palette.MOSS_DARK,
            Palette.GREEN_BRIGHT,
            Palette.alpha(Palette.FOREST, 0.2f)),

    LAVA_CAVERNS("Lava Caverns", 6, 8,
            Palette.UMBER,
            Palette.STONE,
            Palette.WINE,
            Palette.FLAME,
            Palette.alpha(Palette.BLOOD, 0.15f), Tile.LAVA, "audio/ambient/lava_caverns.ogg"),

    SHADOW_ABYSS("Shadow Abyss", 9, 10,
            Palette.INDIGO,
            Palette.SLATE_LIGHT,
            Palette.SLATE_DARK,
            Palette.VIOLET,
            Palette.alpha(Palette.PLUM, 0.2f), Tile.CRYSTAL_SPIKES, "audio/ambient/shadow_abyss.ogg"),

    FROST_VAULTS("Frost Vaults", 11, 12,
            Palette.TEAL_DARK,
            Palette.SEA,
            Palette.SEA_DARK,
            Palette.ICE,
            Palette.alpha(Palette.SEA, 0.18f), Tile.ICE, "audio/ambient/frost_vaults.ogg"),

    TOXIC_MIRE("Toxic Mire", 13, 14,
            Palette.ASH,
            Palette.MOSS_DARK,
            Palette.FOREST,
            Palette.LIME,
            Palette.alpha(Palette.GREEN, 0.18f), Tile.TOXIC_MIST, "audio/ambient/toxic_mire.ogg");

    public final String name;
    public final int startFloor;
    public final int endFloor;
    public final Color floorColor;
    public final Color wallColor;
    /** Second wall colour, mixed in by a stable per-tile hash so walls aren't one flat colour. */
    public final Color wallShadeColor;
    public final Color accentColor;
    public final Color ambientColor;
    public final Tile hazardTile;
    public final String ambientTrack;

    Biome(String name, int startFloor, int endFloor, Color floor, Color wall, Color wallShade, Color accent,
            Color ambient) {
        this(name, startFloor, endFloor, floor, wall, wallShade, accent, ambient, Tile.WATER,
                "audio/ambient/" + name.toLowerCase().replace(' ', '_') + ".ogg");
    }

    Biome(String name, int startFloor, int endFloor, Color floor, Color wall, Color wallShade, Color accent,
            Color ambient, Tile hazardTile, String ambientTrack) {
        this.name = name;
        this.startFloor = startFloor;
        this.endFloor = endFloor;
        this.floorColor = floor;
        this.wallColor = wall;
        this.wallShadeColor = wallShade;
        this.accentColor = accent;
        this.ambientColor = ambient;
        this.hazardTile = hazardTile;
        this.ambientTrack = ambientTrack;
    }

    public static Biome forFloor(int floor) {
        for (Biome b : values()) {
            if (floor >= b.startFloor && floor <= b.endFloor)
                return b;
        }
        return CRYSTAL_CAVES;
    }
}

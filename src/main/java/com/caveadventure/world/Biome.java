package com.caveadventure.world;

import com.badlogic.gdx.graphics.Color;

/**
 * Biome themes for different floor groups. Each biome defines unique tile
 * colors,
 * ambient effects, and hazard types.
 */
public enum Biome {
    CRYSTAL_CAVES("Crystal Caves", 1, 3,
            new Color(0.2f, 0.22f, 0.28f, 1f), // floor
            new Color(0.3f, 0.35f, 0.45f, 1f), // wall
            new Color(0.5f, 0.6f, 0.8f, 1f), // accent
            new Color(0.15f, 0.4f, 0.6f, 0.3f)), // ambient glow

    MUSHROOM_GROTTO("Mushroom Grotto", 4, 5,
            new Color(0.2f, 0.25f, 0.18f, 1f),
            new Color(0.3f, 0.28f, 0.22f, 1f),
            new Color(0.4f, 0.8f, 0.3f, 1f),
            new Color(0.2f, 0.5f, 0.15f, 0.2f)),

    LAVA_CAVERNS("Lava Caverns", 6, 8,
            new Color(0.28f, 0.18f, 0.14f, 1f),
            new Color(0.4f, 0.25f, 0.18f, 1f),
            new Color(0.9f, 0.4f, 0.1f, 1f),
            new Color(0.6f, 0.15f, 0.05f, 0.15f), Tile.LAVA, "audio/ambient/lava_caverns.ogg"),

    SHADOW_ABYSS("Shadow Abyss", 9, 10,
            new Color(0.12f, 0.1f, 0.15f, 1f),
            new Color(0.2f, 0.18f, 0.25f, 1f),
            new Color(0.5f, 0.2f, 0.8f, 1f),
            new Color(0.3f, 0.1f, 0.5f, 0.2f), Tile.CRYSTAL_SPIKES, "audio/ambient/shadow_abyss.ogg"),

    FROST_VAULTS("Frost Vaults", 11, 12,
            new Color(0.15f, 0.23f, 0.28f, 1f),
            new Color(0.25f, 0.35f, 0.42f, 1f),
            new Color(0.58f, 0.86f, 1f, 1f),
            new Color(0.2f, 0.45f, 0.65f, 0.18f), Tile.ICE, "audio/ambient/frost_vaults.ogg"),

    TOXIC_MIRE("Toxic Mire", 13, 14,
            new Color(0.14f, 0.22f, 0.13f, 1f),
            new Color(0.20f, 0.28f, 0.18f, 1f),
            new Color(0.46f, 0.95f, 0.22f, 1f),
            new Color(0.20f, 0.7f, 0.15f, 0.18f), Tile.TOXIC_MIST, "audio/ambient/toxic_mire.ogg");

    public final String name;
    public final int startFloor;
    public final int endFloor;
    public final Color floorColor;
    public final Color wallColor;
    public final Color accentColor;
    public final Color ambientColor;
    public final Tile hazardTile;
    public final String ambientTrack;

    Biome(String name, int startFloor, int endFloor, Color floor, Color wall, Color accent, Color ambient) {
        this(name, startFloor, endFloor, floor, wall, accent, ambient, Tile.WATER,
                "audio/ambient/" + name.toLowerCase().replace(' ', '_') + ".ogg");
    }

    Biome(String name, int startFloor, int endFloor, Color floor, Color wall, Color accent, Color ambient,
            Tile hazardTile, String ambientTrack) {
        this.name = name;
        this.startFloor = startFloor;
        this.endFloor = endFloor;
        this.floorColor = floor;
        this.wallColor = wall;
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

package com.caveadventure.world;

import com.badlogic.gdx.graphics.Color;

/**
 * Tile types for the cave map. Each tile defines its visual appearance and
 * physical properties. Colours are {@link Palette} entries.
 */
public enum Tile {
    // Basic terrain
    FLOOR(Palette.STONE_DARK, false, false, "Stone floor"),
    WALL(Palette.STONE_LIGHT, true, false, "Cave wall"),
    WALL_DARK(Palette.STONE, true, false, "Dark cave wall"),

    // Hazards
    WATER(Palette.SEA, false, false, "Shallow water"),
    LAVA(Palette.RED, false, true, "Molten lava"),
    ICE(Palette.CYAN, false, false, "Slick ice", 0, 1.65f),
    TOXIC_MIST(Palette.GREEN_BRIGHT, false, true, "Toxic mist", 5, 1.1f),
    CRYSTAL_SPIKES(Palette.FROST, false, true, "Crystal spikes", 7, 1.25f),

    // Interactive
    DOOR_LOCKED(Palette.COPPER, true, false, "Locked door"),
    DOOR_OPEN(Palette.WOOD_LIGHT, false, false, "Open door"),
    STAIRS_DOWN(Palette.VIOLET, false, false, "Stairs going deeper"),
    CHEST(Palette.GOLD, false, false, "Treasure chest"),

    // Traps
    TRAP_HIDDEN(Palette.STONE_DARK, false, true, "Hidden trap"),
    TRAP_SPIKES(Palette.WINE, false, false, "Visible spike trap"),
    TRAP_ARROW(Palette.WOOD, false, false, "Arrow trap"),

    // Special
    SHOP_FLOOR(Palette.STONE, false, false, "Shop area");

    private final Color color;
    private final boolean solid;
    private final boolean lethal;
    private final String description;
    private final int hazardDamage;
    private final float movementMultiplier;

    Tile(Color color, boolean solid, boolean lethal, String description) {
        this(color, solid, lethal, description, 0, tileMovementMultiplier(description));
    }

    Tile(Color color, boolean solid, boolean lethal, String description, int hazardDamage, float movementMultiplier) {
        this.color = color;
        this.solid = solid;
        this.lethal = lethal;
        this.description = description;
        this.hazardDamage = hazardDamage;
        this.movementMultiplier = movementMultiplier;
    }

    public Color getColor() {
        return color;
    }

    public boolean isSolid() {
        return solid;
    }

    public boolean isLethal() {
        return lethal;
    }

    public String getDescription() {
        return description;
    }

    public int getHazardDamage() {
        if (this == LAVA)
            return 10;
        return hazardDamage;
    }

    public float getMovementMultiplier() {
        return movementMultiplier;
    }

    /**
     * Whether this tile can be walked on.
     */
    public boolean isPassable() {
        return !solid;
    }

    private static float tileMovementMultiplier(String description) {
        if ("Shallow water".equals(description))
            return 1.3f;
        return 1f;
    }
}

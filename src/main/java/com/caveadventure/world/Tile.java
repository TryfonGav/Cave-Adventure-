package com.caveadventure.world;

import com.badlogic.gdx.graphics.Color;

/**
 * Tile types for the cave map. Each tile defines its visual appearance and
 * physical properties.
 */
public enum Tile {
    // Basic terrain
    FLOOR(new Color(0.25f, 0.22f, 0.2f, 1f), false, false, "Stone floor"),
    WALL(new Color(0.4f, 0.35f, 0.3f, 1f), true, false, "Cave wall"),
    WALL_DARK(new Color(0.3f, 0.25f, 0.22f, 1f), true, false, "Dark cave wall"),

    // Hazards
    WATER(new Color(0.15f, 0.3f, 0.6f, 1f), false, false, "Shallow water"),
    LAVA(new Color(0.9f, 0.3f, 0.05f, 1f), false, true, "Molten lava"),

    // Interactive
    DOOR_LOCKED(new Color(0.55f, 0.35f, 0.1f, 1f), true, false, "Locked door"),
    DOOR_OPEN(new Color(0.4f, 0.3f, 0.15f, 1f), false, false, "Open door"),
    STAIRS_DOWN(new Color(0.6f, 0.5f, 0.8f, 1f), false, false, "Stairs going deeper"),
    CHEST(new Color(0.85f, 0.7f, 0.1f, 1f), false, false, "Treasure chest"),

    // Traps
    TRAP_SPIKE(new Color(0.5f, 0.15f, 0.15f, 1f), false, true, "Spike trap"),
    TRAP_HIDDEN(new Color(0.25f, 0.22f, 0.2f, 1f), false, true, "Hidden trap"),
    TRAP_SPIKES(new Color(0.45f, 0.12f, 0.12f, 1f), false, false, "Visible spike trap"),
    TRAP_ARROW(new Color(0.35f, 0.2f, 0.1f, 1f), false, false, "Arrow trap"),

    // Special
    SHOP_FLOOR(new Color(0.3f, 0.25f, 0.15f, 1f), false, false, "Shop area");

    private final Color color;
    private final boolean solid;
    private final boolean lethal;
    private final String description;

    Tile(Color color, boolean solid, boolean lethal, String description) {
        this.color = color;
        this.solid = solid;
        this.lethal = lethal;
        this.description = description;
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

    /**
     * Whether this tile can be walked on.
     */
    public boolean isPassable() {
        return !solid;
    }
}

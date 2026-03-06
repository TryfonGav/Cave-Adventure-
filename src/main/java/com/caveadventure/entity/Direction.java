package com.caveadventure.entity;

/**
 * Cardinal and diagonal directions with grid offsets.
 */
public enum Direction {
    NORTH(0, 1),
    SOUTH(0, -1),
    EAST(1, 0),
    WEST(-1, 0),
    NORTH_EAST(1, 1),
    NORTH_WEST(-1, 1),
    SOUTH_EAST(1, -1),
    SOUTH_WEST(-1, -1);

    public final int dx;
    public final int dy;

    Direction(int dx, int dy) {
        this.dx = dx;
        this.dy = dy;
    }

    /** Get cardinal directions only (for AI). */
    public static Direction[] cardinals() {
        return new Direction[] { NORTH, SOUTH, EAST, WEST };
    }
}

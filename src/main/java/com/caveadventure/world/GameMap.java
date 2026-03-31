package com.caveadventure.world;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.caveadventure.engine.Camera2D;

/**
 * Holds the tile grid and handles map rendering with camera culling.
 */
public class GameMap {

    public static final int TILE_SIZE = 32;

    private final Tile[][] tiles;
    private final int width;
    private final int height;

    public GameMap(Tile[][] tiles, int width, int height) {
        this.tiles = tiles;
        this.width = width;
        this.height = height;
    }

    /**
     * Get the tile at the given grid coordinates.
     */
    public Tile getTile(int x, int y) {
        if (x < 0 || x >= width || y < 0 || y >= height) {
            return Tile.WALL; // Out of bounds = wall
        }
        return tiles[x][y];
    }

    /**
     * Set a tile at the given grid coordinates.
     */
    public void setTile(int x, int y, Tile tile) {
        if (x >= 0 && x < width && y >= 0 && y < height) {
            tiles[x][y] = tile;
        }
    }

    /**
     * Whether the given grid position can be walked on.
     */
    public boolean isPassable(int x, int y) {
        return getTile(x, y).isPassable();
    }

    /**
     * Render only the tiles visible to the camera.
     */
    public void render(ShapeRenderer renderer, Camera2D camera) {
        render(renderer, camera, false);
    }

    public void render(ShapeRenderer renderer, Camera2D camera, boolean trapDetect) {
        render(renderer, camera, trapDetect, null);
    }

    public void render(ShapeRenderer renderer, Camera2D camera, boolean trapDetect, Biome biome) {
        int[] range = camera.getVisibleTileRange(TILE_SIZE, width, height);
        int minX = range[0], minY = range[1], maxX = range[2], maxY = range[3];

        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                Tile tile = tiles[x][y];
                // TRAP_DETECT: reveal hidden traps visually
                if (trapDetect && tile == Tile.TRAP_HIDDEN) {
                    tile = Tile.TRAP_SPIKES;
                }
                Color c = resolveTileColor(tile, biome);

                // Add slight variation to walls for a natural look
                if (tile == Tile.WALL || tile == Tile.WALL_DARK) {
                    float variation = ((x * 31 + y * 17) % 10) / 100f;
                    renderer.setColor(c.r + variation, c.g + variation, c.b + variation, 1f);
                } else if (tile == Tile.LAVA) {
                    // Animated lava glow
                    float pulse = (float) Math.sin(System.currentTimeMillis() / 300.0 + x + y) * 0.1f;
                    renderer.setColor(c.r + pulse, c.g + pulse * 0.5f, c.b, 1f);
                } else if (tile == Tile.WATER) {
                    // Subtle water shimmer
                    float shimmer = (float) Math.sin(System.currentTimeMillis() / 500.0 + x * 0.5 + y * 0.3) * 0.05f;
                    renderer.setColor(c.r, c.g + shimmer, c.b + shimmer, 1f);
                } else {
                    renderer.setColor(c);
                }

                renderer.rect(x * TILE_SIZE, y * TILE_SIZE, TILE_SIZE, TILE_SIZE);

                // Draw wall edge shadows for depth
                if (tile == Tile.FLOOR && y + 1 < height && tiles[x][y + 1].isSolid()) {
                    renderer.setColor(0, 0, 0, 0.2f);
                    renderer.rect(x * TILE_SIZE, (y + 1) * TILE_SIZE - 4, TILE_SIZE, 4);
                }
            }
        }

        // Draw grid lines for visual clarity (subtle)
        renderer.setColor(0, 0, 0, 0.08f);
        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                if (!tiles[x][y].isSolid()) {
                    renderer.rect(x * TILE_SIZE, y * TILE_SIZE, TILE_SIZE, 1);
                    renderer.rect(x * TILE_SIZE, y * TILE_SIZE, 1, TILE_SIZE);
                }
            }
        }
    }

    private Color resolveTileColor(Tile tile, Biome biome) {
        if (biome == null) {
            return tile.getColor();
        }

        switch (tile) {
            case FLOOR:
            case TRAP_HIDDEN:
                return biome.floorColor;
            case WALL:
            case WALL_DARK:
                return biome.wallColor;
            case SHOP_FLOOR:
                return lerpColor(biome.floorColor, biome.accentColor, 0.18f);
            case DOOR_LOCKED:
            case DOOR_OPEN:
                return lerpColor(biome.wallColor, biome.accentColor, 0.35f);
            case CHEST:
                return lerpColor(new Color(0.85f, 0.7f, 0.1f, 1f), biome.accentColor, 0.25f);
            case STAIRS_DOWN:
                return lerpColor(new Color(0.6f, 0.5f, 0.8f, 1f), biome.accentColor, 0.45f);
            default:
                return tile.getColor();
        }
    }

    private Color lerpColor(Color a, Color b, float t) {
        return new Color(
                a.r + (b.r - a.r) * t,
                a.g + (b.g - a.g) * t,
                a.b + (b.b - a.b) * t,
                1f);
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public Tile[][] getTiles() {
        return tiles;
    }
}

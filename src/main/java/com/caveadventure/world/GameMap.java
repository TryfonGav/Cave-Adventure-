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
     * Breadth-first walking distance (4-directional, passable tiles only, so locked
     * doors block) from (startX, startY) to every tile. Unreachable tiles are -1.
     */
    public int[][] stepDistancesFrom(int startX, int startY) {
        int[][] dist = new int[width][height];
        for (int[] col : dist)
            java.util.Arrays.fill(col, -1);
        if (startX < 0 || startX >= width || startY < 0 || startY >= height)
            return dist;
        java.util.ArrayDeque<int[]> queue = new java.util.ArrayDeque<>();
        dist[startX][startY] = 0;
        queue.add(new int[] { startX, startY });
        int[][] dirs = { { 1, 0 }, { -1, 0 }, { 0, 1 }, { 0, -1 } };
        while (!queue.isEmpty()) {
            int[] cur = queue.poll();
            int d = dist[cur[0]][cur[1]];
            for (int[] dir : dirs) {
                int nx = cur[0] + dir[0], ny = cur[1] + dir[1];
                if (nx < 0 || ny < 0 || nx >= width || ny >= height || dist[nx][ny] >= 0 || !isPassable(nx, ny))
                    continue;
                dist[nx][ny] = d + 1;
                queue.add(new int[] { nx, ny });
            }
        }
        return dist;
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
                renderer.setColor(resolveTileColor(tile, biome, x, y));
                renderer.rect(x * TILE_SIZE, y * TILE_SIZE, TILE_SIZE, TILE_SIZE);

                // Wall edge shadow for depth: two art pixels under a wall
                if (tile == Tile.FLOOR && y + 1 < height && tiles[x][y + 1].isSolid()) {
                    renderer.setColor(WALL_SHADOW);
                    renderer.rect(x * TILE_SIZE, (y + 1) * TILE_SIZE - 4, TILE_SIZE, 4);
                }
            }
        }
    }

    private static final Color WALL_SHADOW = Palette.alpha(Palette.INK, 0.25f);

    /**
     * Palette colour for one map cell. Walls mix the biome's two wall colours by a stable
     * coordinate hash; lava and water step between two palette entries over time (placeholder
     * frames until the P5-T6 tileset animates them).
     */
    private Color resolveTileColor(Tile tile, Biome biome, int x, int y) {
        long frame = System.currentTimeMillis() / 400;
        switch (tile) {
            case FLOOR:
            case TRAP_HIDDEN:
                return biome != null ? biome.floorColor : tile.getColor();
            case WALL:
            case WALL_DARK:
                if (biome == null)
                    return tile.getColor();
                return ((x * 31 + y * 17) % 5 == 0) ? biome.wallShadeColor : biome.wallColor;
            case LAVA:
                return ((frame + x + y) % 3 == 0) ? Palette.FLAME : tile.getColor();
            case WATER:
                return ((frame + x * 2 + y) % 4 == 0) ? Palette.SEA_DARK : tile.getColor();
            default:
                return tile.getColor();
        }
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

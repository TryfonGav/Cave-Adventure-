package com.caveadventure.engine;

import com.caveadventure.world.GameMap;

/**
 * Integer-scale maths for the pixel-perfect renderer (P5-T1, decision D13).
 * <p>
 * Art is drawn on a 16 px grid and shown at {@code GameMap.TILE_SIZE} (32) world units per
 * tile, so one art pixel is two world units. The world is rendered into a low-resolution
 * buffer measured in art pixels and copied to the window at a whole-number scale; the buffer
 * is rounded up so it always covers the window, and the extra (less than one scaled pixel per
 * edge) is cropped evenly. Pure maths, no GL: unit-tested in {@code PixelScaleTest}.
 */
public final class PixelScale {

    /** Art pixels per tile edge. */
    public static final int ART_TILE_SIZE = 16;
    /** World units per art pixel. */
    public static final float WORLD_PER_ART_PIXEL = GameMap.TILE_SIZE / (float) ART_TILE_SIZE;
    /** The scale is chosen so the visible height is as close as possible to this many tiles. */
    public static final float TARGET_VISIBLE_TILES = 20f;
    /** Smallest UI scale: the 5x7 pixel font is too small to read below 2x on desktop screens. */
    public static final int MIN_UI_SCALE = 2;
    /** Window height per UI scale step. */
    public static final int UI_HEIGHT_PER_SCALE = 540;

    /** Screen pixels per art pixel (always 1 or more). */
    public final int scale;
    /** Low-resolution buffer size in art pixels. */
    public final int bufferWidth;
    public final int bufferHeight;
    /** Where the scaled buffer's lower-left corner goes in the window (zero or negative: cropping). */
    public final int offsetX;
    public final int offsetY;

    private PixelScale(int scale, int bufferWidth, int bufferHeight, int offsetX, int offsetY) {
        this.scale = scale;
        this.bufferWidth = bufferWidth;
        this.bufferHeight = bufferHeight;
        this.offsetX = offsetX;
        this.offsetY = offsetY;
    }

    /**
     * Picks the integer scale for a window (in back-buffer pixels) whose visible height is
     * closest to {@link #TARGET_VISIBLE_TILES}, and the buffer that covers the window at it.
     */
    public static PixelScale forWindow(int windowWidth, int windowHeight) {
        int w = Math.max(1, windowWidth);
        int h = Math.max(1, windowHeight);
        float ideal = h / (TARGET_VISIBLE_TILES * ART_TILE_SIZE);
        int low = Math.max(1, (int) Math.floor(ideal));
        int high = low + 1;
        int scale = Math.abs(visibleTiles(h, low) - TARGET_VISIBLE_TILES)
                <= Math.abs(visibleTiles(h, high) - TARGET_VISIBLE_TILES) ? low : high;

        int bufferWidth = ceilDiv(w, scale);
        int bufferHeight = ceilDiv(h, scale);
        return new PixelScale(scale, bufferWidth, bufferHeight,
                (w - bufferWidth * scale) / 2, (h - bufferHeight * scale) / 2);
    }

    /** UI pixel scale for a window height: whole steps, never below {@link #MIN_UI_SCALE}. */
    public static int uiScale(int windowHeight) {
        return Math.max(MIN_UI_SCALE, windowHeight / UI_HEIGHT_PER_SCALE);
    }

    /** Visible height in tiles. */
    public float visibleTilesY() {
        return bufferHeight / (float) ART_TILE_SIZE;
    }

    /** Visible width in tiles. */
    public float visibleTilesX() {
        return bufferWidth / (float) ART_TILE_SIZE;
    }

    /** Camera viewport width in world units. */
    public float worldWidth() {
        return bufferWidth * WORLD_PER_ART_PIXEL;
    }

    /** Camera viewport height in world units. */
    public float worldHeight() {
        return bufferHeight * WORLD_PER_ART_PIXEL;
    }

    /** Rounds a world coordinate to the nearest whole art pixel. */
    public static float snap(float world) {
        return Math.round(world / WORLD_PER_ART_PIXEL) * WORLD_PER_ART_PIXEL;
    }

    /**
     * Camera centre for drawing: moves {@code centre} so the view's lower edge sits on a whole
     * art pixel, which keeps every art pixel aligned with a buffer pixel.
     */
    public static float snapCameraCentre(float centre, float viewportSize) {
        float half = viewportSize / 2f;
        return snap(centre - half) + half;
    }

    private static float visibleTiles(int windowHeight, int scale) {
        return windowHeight / (float) scale / ART_TILE_SIZE;
    }

    private static int ceilDiv(int a, int b) {
        return (a + b - 1) / b;
    }
}

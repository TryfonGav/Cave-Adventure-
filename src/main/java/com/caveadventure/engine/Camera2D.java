package com.caveadventure.engine;

import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector3;

/**
 * Camera wrapper that smoothly follows a target and clamps to map boundaries.
 */
public class Camera2D {

    private final OrthographicCamera camera;
    private final float mapPixelWidth;
    private final float mapPixelHeight;
    private float lerpSpeed = 4.0f;

    public Camera2D(OrthographicCamera camera, float mapPixelWidth, float mapPixelHeight) {
        this.camera = camera;
        this.mapPixelWidth = mapPixelWidth;
        this.mapPixelHeight = mapPixelHeight;
    }

    /**
     * Smoothly follow a target position with linear interpolation.
     */
    public void follow(float targetX, float targetY, float delta) {
        float lerp = 1f - (float) Math.pow(1f - lerpSpeed * 0.1f, delta * 60f);
        camera.position.x += (targetX - camera.position.x) * lerp;
        camera.position.y += (targetY - camera.position.y) * lerp;

        clampToMap();
    }

    /**
     * Prevent camera from showing outside the map. On an axis where the map is
     * smaller than the view, the camera is centred on the map instead.
     */
    private void clampToMap() {
        camera.position.x = clampAxis(camera.position.x, camera.viewportWidth / 2f, mapPixelWidth);
        camera.position.y = clampAxis(camera.position.y, camera.viewportHeight / 2f, mapPixelHeight);
    }

    /**
     * Clamp one camera axis so the view stays inside {@code [0, mapSize]}, or centre it
     * when the view is wider than the map.
     */
    static float clampAxis(float position, float halfView, float mapSize) {
        if (mapSize <= halfView * 2f)
            return mapSize / 2f;
        return MathUtils.clamp(position, halfView, mapSize - halfView);
    }

    public void update() {
        camera.update();
    }

    public OrthographicCamera getCamera() {
        return camera;
    }

    /**
     * Get visible tile range for culling. Returns {minTileX, minTileY, maxTileX,
     * maxTileY}.
     */
    public int[] getVisibleTileRange(int tileSize, int mapTilesW, int mapTilesH) {
        float halfW = camera.viewportWidth / 2f;
        float halfH = camera.viewportHeight / 2f;

        int minX = Math.max(0, (int) ((camera.position.x - halfW) / tileSize) - 1);
        int minY = Math.max(0, (int) ((camera.position.y - halfH) / tileSize) - 1);
        int maxX = Math.min(mapTilesW - 1, (int) ((camera.position.x + halfW) / tileSize) + 1);
        int maxY = Math.min(mapTilesH - 1, (int) ((camera.position.y + halfH) / tileSize) + 1);

        return new int[] { minX, minY, maxX, maxY };
    }
}

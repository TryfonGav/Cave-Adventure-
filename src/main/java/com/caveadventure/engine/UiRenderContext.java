package com.caveadventure.engine;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.caveadventure.CaveAdventure;
import com.caveadventure.ui.CaveUIStyle;

/**
 * Small helper that groups rendering resources and reusable objects
 * so UI/state renderers avoid allocating transient objects each frame.
 */
public final class UiRenderContext {

    public final CaveAdventure game;
    public final ShapeRenderer shape;
    public final SpriteBatch batch;
    public final BitmapFont font;
    public final BitmapFont fontSmall;
    public final OrthographicCamera camera;

    // Common screen dims (updated by caller)
    public float screenWidth;
    public float screenHeight;

    // Reusable colors
    public final Color tmpColorA = new Color();
    public final Color tmpColorB = new Color();

    public UiRenderContext(CaveAdventure game, ShapeRenderer shape, SpriteBatch batch,
                           BitmapFont font, BitmapFont fontSmall, OrthographicCamera camera) {
        this.game = game;
        this.shape = shape;
        this.batch = batch;
        this.font = font;
        this.fontSmall = fontSmall;
        this.camera = camera;
    }

    public void updateViewport(int width, int height) {
        this.screenWidth = width;
        this.screenHeight = height;
        camera.setToOrtho(false, width, height);
        camera.update();
        shape.setProjectionMatrix(camera.combined);
        batch.setProjectionMatrix(camera.combined);
    }

    /** Convenience wrapper for the project's bar drawing style. */
    public void drawBar(float x, float y, float w, float h, float pct, Color fg, Color bg) {
        CaveUIStyle.drawBar(shape, x, y, w, h, pct, fg, bg);
    }
}

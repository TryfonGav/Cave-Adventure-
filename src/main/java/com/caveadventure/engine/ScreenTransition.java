package com.caveadventure.engine;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.caveadventure.CaveAdventure;

/**
 * Smooth screen transition effects: fade to black, fade in, flash.
 */
public class ScreenTransition {

    public enum TransitionType {
        NONE, FADE_OUT, FADE_IN, FLASH
    }

    private final CaveAdventure game;
    private final OrthographicCamera camera;

    private TransitionType type;
    private float timer;
    private float duration;
    private boolean active;
    private float r, g, b;

    public ScreenTransition(CaveAdventure game) {
        this.game = game;
        this.camera = new OrthographicCamera();
        this.type = TransitionType.NONE;
        this.active = false;
    }

    public void fadeOut(float duration) {
        start(TransitionType.FADE_OUT, duration, 0, 0, 0);
    }

    public void fadeIn(float duration) {
        start(TransitionType.FADE_IN, duration, 0, 0, 0);
    }

    public void flash(float duration) {
        start(TransitionType.FLASH, duration, 1, 1, 1);
    }

    public void flashColor(float duration, float r, float g, float b) {
        start(TransitionType.FLASH, duration, r, g, b);
    }

    private void start(TransitionType type, float duration, float r, float g, float b) {
        this.type = type;
        this.duration = duration;
        this.timer = 0;
        this.active = true;
        this.r = r;
        this.g = g;
        this.b = b;
    }

    public boolean isActive() {
        return active;
    }

    public boolean isComplete() {
        return !active && timer >= duration;
    }

    public void update(float delta) {
        if (!active)
            return;
        timer += delta;
        if (timer >= duration) {
            active = false;
        }
    }

    public void render() {
        if (type == TransitionType.NONE)
            return;

        float progress = Math.min(1f, timer / duration);
        float alpha;

        switch (type) {
            case FADE_OUT:
                alpha = progress;
                break;
            case FADE_IN:
                alpha = 1f - progress;
                break;
            case FLASH:
                alpha = progress < 0.3f ? progress / 0.3f : (1f - progress) / 0.7f;
                break;
            default:
                return;
        }

        if (alpha <= 0.01f)
            return;

        camera.setToOrtho(false, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        camera.update();

        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);

        game.shapeRenderer.setProjectionMatrix(camera.combined);
        game.shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        game.shapeRenderer.setColor(r, g, b, alpha);
        game.shapeRenderer.rect(0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        game.shapeRenderer.end();

        Gdx.gl.glDisable(GL20.GL_BLEND);
    }
}

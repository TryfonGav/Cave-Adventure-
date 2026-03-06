package com.caveadventure.ui;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.caveadventure.CaveAdventure;
import com.caveadventure.engine.InputHandler;

/**
 * Main menu screen with title, options, and atmospheric background.
 */
public class MainMenu {

    private final CaveAdventure game;
    private final OrthographicCamera camera;
    private final GlyphLayout layout;

    private int selectedOption;
    private final String[] options = { "New Game", "Continue", "Settings", "Quit" };
    private float animTimer;
    private boolean hasSave;

    private final float[] particleX = new float[30];
    private final float[] particleY = new float[30];
    private final float[] particleSpeed = new float[30];
    private final float[] particleSize = new float[30];

    public MainMenu(CaveAdventure game) {
        this.game = game;
        this.camera = new OrthographicCamera();
        this.layout = new GlyphLayout();
        this.selectedOption = 0;
        this.hasSave = false;
        for (int i = 0; i < particleX.length; i++)
            resetParticle(i, true);
    }

    private void resetParticle(int i, boolean randomY) {
        particleX[i] = (float) (Math.random() * 960);
        particleY[i] = randomY ? (float) (Math.random() * 640) : 650;
        particleSpeed[i] = 10 + (float) (Math.random() * 30);
        particleSize[i] = 1 + (float) (Math.random() * 3);
    }

    public void setHasSave(boolean has) {
        this.hasSave = has;
    }

    public int update(InputHandler input, float delta) {
        animTimer += delta;

        for (int i = 0; i < particleX.length; i++) {
            particleY[i] -= particleSpeed[i] * delta;
            if (particleY[i] < -5)
                resetParticle(i, false);
        }

        if (input.isKeyJustPressed(Input.Keys.UP) || input.isKeyJustPressed(Input.Keys.W))
            selectedOption = (selectedOption - 1 + options.length) % options.length;
        if (input.isKeyJustPressed(Input.Keys.DOWN) || input.isKeyJustPressed(Input.Keys.S))
            selectedOption = (selectedOption + 1) % options.length;

        if (input.isKeyJustPressed(Input.Keys.ENTER) || input.isKeyJustPressed(Input.Keys.SPACE)) {
            if (selectedOption == 1 && !hasSave)
                return -1;
            return selectedOption;
        }
        return -1;
    }

    public void render() {
        camera.setToOrtho(false, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        camera.update();

        float screenW = Gdx.graphics.getWidth();
        float screenH = Gdx.graphics.getHeight();

        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);

        game.shapeRenderer.setProjectionMatrix(camera.combined);
        game.shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);

        // Background
        game.shapeRenderer.setColor(0.04f, 0.04f, 0.08f, 1f);
        game.shapeRenderer.rect(0, 0, screenW, screenH / 2);
        game.shapeRenderer.setColor(0.08f, 0.06f, 0.12f, 1f);
        game.shapeRenderer.rect(0, screenH / 2, screenW, screenH / 2);

        // Stalactites
        for (int i = 0; i < 20; i++) {
            float sx = i * (screenW / 20f);
            float sHeight = 30 + (float) Math.sin(i * 1.7) * 40 + (float) Math.sin(i * 0.7) * 20;
            game.shapeRenderer.setColor(0.06f, 0.05f, 0.09f, 1f);
            game.shapeRenderer.rect(sx, screenH - sHeight, screenW / 20f + 2, sHeight);
        }

        // Particles
        for (int i = 0; i < particleX.length; i++) {
            float alpha = 0.3f + (float) Math.sin(animTimer + i) * 0.2f;
            game.shapeRenderer.setColor(0.8f, 0.5f, 0.2f, alpha);
            game.shapeRenderer.rect(particleX[i], particleY[i], particleSize[i], particleSize[i]);
        }

        // Title glow
        float titleY = screenH * 0.68f;
        float glowPulse = (float) Math.sin(animTimer * 1.5) * 0.05f + 0.15f;
        game.shapeRenderer.setColor(0.3f, 0.15f, 0.05f, glowPulse);
        game.shapeRenderer.rect(screenW / 2 - 250, titleY - 30, 500, 70);

        // Menu backgrounds
        float menuStartY = screenH * 0.44f;
        for (int i = 0; i < options.length; i++) {
            float optY = menuStartY - i * 50;
            if (i == selectedOption) {
                float pulse = (float) Math.sin(animTimer * 4) * 0.05f + 0.25f;
                game.shapeRenderer.setColor(0.2f, 0.3f, 0.5f, pulse);
                game.shapeRenderer.rect(screenW / 2 - 130, optY - 5, 260, 40);
                game.shapeRenderer.setColor(0.9f, 0.7f, 0.2f, 0.9f);
                game.shapeRenderer.rect(screenW / 2 - 135, optY, 4, 30);
            }
        }
        game.shapeRenderer.end();

        // --- Text (using proper font sizes, NO setScale) ---
        game.batch.setProjectionMatrix(camera.combined);
        game.batch.begin();

        BitmapFont largeFont = game.fontLarge != null ? game.fontLarge : game.font;
        BitmapFont normalFont = game.font;
        BitmapFont smallFont = game.fontSmall != null ? game.fontSmall : game.font;

        // Title — use large font
        largeFont.setColor(0.9f, 0.7f, 0.3f, 1f);
        layout.setText(largeFont, "CAVE ADVENTURE");
        largeFont.draw(game.batch, "CAVE ADVENTURE", screenW / 2 - layout.width / 2, titleY + 30);

        // Subtitle — use small font
        smallFont.setColor(0.5f, 0.45f, 0.35f, 0.8f);
        layout.setText(smallFont, "Descend into the depths...");
        smallFont.draw(game.batch, "Descend into the depths...", screenW / 2 - layout.width / 2, titleY - 15);

        // Menu options — use normal font
        for (int i = 0; i < options.length; i++) {
            float optY = menuStartY - i * 50;
            boolean isDisabled = (i == 1 && !hasSave);

            if (isDisabled)
                normalFont.setColor(0.3f, 0.3f, 0.25f, 0.5f);
            else if (i == selectedOption)
                normalFont.setColor(1f, 0.85f, 0.3f, 1f);
            else
                normalFont.setColor(0.7f, 0.65f, 0.55f, 0.9f);

            layout.setText(normalFont, options[i]);
            normalFont.draw(game.batch, options[i], screenW / 2 - layout.width / 2, optY + 28);
        }

        // Footer — use small font
        smallFont.setColor(0.35f, 0.35f, 0.3f, 0.6f);
        layout.setText(smallFont, "Arrow Keys: Navigate   Enter: Select");
        smallFont.draw(game.batch, "Arrow Keys: Navigate   Enter: Select", screenW / 2 - layout.width / 2, 40);

        game.batch.end();
        Gdx.gl.glDisable(GL20.GL_BLEND);
    }
}

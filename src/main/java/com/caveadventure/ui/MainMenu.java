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

        CaveUIStyle.drawCaveBackground(game.shapeRenderer, screenW, screenH, animTimer);
        CaveUIStyle.drawDust(game.shapeRenderer, particleX, particleY, particleSize, animTimer);

        float titleY = screenH * 0.68f;
        CaveUIStyle.drawTorch(game.shapeRenderer, screenW / 2f - 315, titleY - 48, 1.6f, 0.95f, animTimer);
        CaveUIStyle.drawTorch(game.shapeRenderer, screenW / 2f + 292, titleY - 48, 1.6f, 0.95f, animTimer + 0.6f);
        CaveUIStyle.drawStonePanel(game.shapeRenderer, screenW / 2f - 285, titleY - 58, 570, 102, 0.94f);

        float menuStartY = screenH * 0.44f;
        CaveUIStyle.drawStonePanel(game.shapeRenderer, screenW / 2f - 160, menuStartY - 168, 320, 215, 0.90f);
        for (int i = 0; i < options.length; i++) {
            float optY = menuStartY - i * 50;
            if (i == selectedOption)
                CaveUIStyle.drawSelection(game.shapeRenderer, screenW / 2f - 130, optY - 6, 260, 38, 1f);
        }

        game.shapeRenderer.end();

        game.batch.setProjectionMatrix(camera.combined);
        game.batch.begin();

        BitmapFont largeFont = game.fontLarge != null ? game.fontLarge : game.font;
        BitmapFont normalFont = game.font;
        BitmapFont smallFont = game.fontSmall != null ? game.fontSmall : game.font;

        largeFont.setColor(CaveUIStyle.GOLD);
        layout.setText(largeFont, "CAVE ADVENTURE");
        largeFont.draw(game.batch, "CAVE ADVENTURE", screenW / 2f - layout.width / 2f, titleY + 30);

        smallFont.setColor(CaveUIStyle.MUTED_TEXT);
        layout.setText(smallFont, "Descend into the depths...");
        smallFont.draw(game.batch, "Descend into the depths...", screenW / 2f - layout.width / 2f, titleY - 15);

        for (int i = 0; i < options.length; i++) {
            float optY = menuStartY - i * 50;
            boolean disabled = i == 1 && !hasSave;
            if (disabled)
                normalFont.setColor(CaveUIStyle.DISABLED_TEXT);
            else if (i == selectedOption)
                normalFont.setColor(CaveUIStyle.GOLD);
            else
                normalFont.setColor(CaveUIStyle.TEXT);

            layout.setText(normalFont, options[i]);
            normalFont.draw(game.batch, options[i], screenW / 2f - layout.width / 2f, optY + 28);
        }

        smallFont.setColor(CaveUIStyle.MUTED_TEXT);
        layout.setText(smallFont, "Arrow Keys: Navigate   Enter: Select");
        smallFont.draw(game.batch, "Arrow Keys: Navigate   Enter: Select", screenW / 2f - layout.width / 2f, 40);

        game.batch.end();
        Gdx.gl.glDisable(GL20.GL_BLEND);
    }
}

package com.caveadventure.ui;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.caveadventure.CaveAdventure;
import com.caveadventure.engine.InputHandler;

/**
 * Game Over / Victory screen with stats display and restart option.
 */
public class GameOverScreen {

    private final CaveAdventure game;
    private final OrthographicCamera camera;
    private final GlyphLayout layout;
    private float animTimer;

    private boolean isVictory;
    private int finalLevel;
    private int floorsCleared;
    private int enemiesKilled;

    public GameOverScreen(CaveAdventure game) {
        this.game = game;
        this.camera = new OrthographicCamera();
        this.layout = new GlyphLayout();
    }

    public void setup(boolean victory, int level, int floors, int killed) {
        this.isVictory = victory;
        this.finalLevel = level;
        this.floorsCleared = floors;
        this.enemiesKilled = killed;
        this.animTimer = 0;
    }

    public boolean update(InputHandler input, float delta) {
        animTimer += delta;
        if (animTimer > 1.0f) {
            return input.isKeyJustPressed(Input.Keys.ENTER) || input.isKeyJustPressed(Input.Keys.SPACE);
        }
        return false;
    }

    public void render() {
        camera.setToOrtho(false, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        camera.update();

        float screenW = Gdx.graphics.getWidth();
        float screenH = Gdx.graphics.getHeight();
        float fadeIn = Math.min(1f, animTimer / 1.0f);

        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);

        game.shapeRenderer.setProjectionMatrix(camera.combined);
        game.shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);

        // Dark overlay
        game.shapeRenderer.setColor(0, 0, 0, 0.85f * fadeIn);
        game.shapeRenderer.rect(0, 0, screenW, screenH);

        // Panel
        float panelW = 400;
        float panelH = 300;
        float panelX = screenW / 2 - panelW / 2;
        float panelY = screenH / 2 - panelH / 2;

        CaveUIStyle.drawStonePanel(game.shapeRenderer, panelX, panelY, panelW, panelH, fadeIn);
        game.shapeRenderer.setColor(isVictory ? CaveUIStyle.GOOD.r : CaveUIStyle.DANGER.r,
                isVictory ? CaveUIStyle.GOOD.g : CaveUIStyle.DANGER.g,
                isVictory ? CaveUIStyle.GOOD.b : CaveUIStyle.DANGER.b, fadeIn);
        game.shapeRenderer.rect(panelX + 14, panelY + panelH - 42, panelW - 28, 4);

        game.shapeRenderer.end();

        // Text — use proper font sizes, NO setScale
        game.batch.setProjectionMatrix(camera.combined);
        game.batch.begin();

        BitmapFont largeFont = game.fontLarge != null ? game.fontLarge : game.font;
        BitmapFont normalFont = game.font;
        BitmapFont smallFont = game.fontSmall != null ? game.fontSmall : game.font;

        // Title
        String title = isVictory ? "VICTORY!" : "YOU DIED";
        largeFont.setColor(isVictory ? new Color(CaveUIStyle.GOOD.r, CaveUIStyle.GOOD.g, CaveUIStyle.GOOD.b, fadeIn)
                : new Color(CaveUIStyle.DANGER.r, CaveUIStyle.DANGER.g, CaveUIStyle.DANGER.b, fadeIn));
        layout.setText(largeFont, title);
        largeFont.draw(game.batch, title, screenW / 2 - layout.width / 2, panelY + panelH - 25);

        // Subtitle
        smallFont.setColor(CaveUIStyle.MUTED_TEXT.r, CaveUIStyle.MUTED_TEXT.g, CaveUIStyle.MUTED_TEXT.b, fadeIn);
        String subtitle = isVictory ? "The cave's darkness has been conquered!" : "The cave claims another soul...";
        layout.setText(smallFont, subtitle);
        smallFont.draw(game.batch, subtitle, screenW / 2 - layout.width / 2, panelY + panelH - 70);

        // Stats
        normalFont.setColor(CaveUIStyle.TEXT.r, CaveUIStyle.TEXT.g, CaveUIStyle.TEXT.b, fadeIn);
        float statsX = panelX + 50;
        float statsY = panelY + panelH - 115;
        normalFont.draw(game.batch, "Level Reached:    " + finalLevel, statsX, statsY);
        normalFont.draw(game.batch, "Floors Cleared:   " + floorsCleared, statsX, statsY - 28);
        normalFont.draw(game.batch, "Enemies Slain:    " + enemiesKilled, statsX, statsY - 56);

        // Restart hint
        if (animTimer > 1.0f) {
            float blink = (float) Math.sin(animTimer * 3) * 0.3f + 0.7f;
            smallFont.setColor(CaveUIStyle.GOLD.r, CaveUIStyle.GOLD.g, CaveUIStyle.GOLD.b, blink * fadeIn);
            String hint = "Press ENTER to return to menu";
            layout.setText(smallFont, hint);
            smallFont.draw(game.batch, hint, screenW / 2 - layout.width / 2, panelY + 30);
        }

        game.batch.end();
        Gdx.gl.glDisable(GL20.GL_BLEND);
    }
}

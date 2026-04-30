package com.caveadventure.ui;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.Preferences;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.caveadventure.CaveAdventure;
import com.caveadventure.engine.InputHandler;

/**
 * Settings menu screen for display options.
 */
public class SettingsMenu {

    private final CaveAdventure game;
    private final OrthographicCamera camera;
    private final GlyphLayout layout;

    private int selectedOption;
    private float animTimer;
    private final Preferences prefs;

    private final int[] resolutionsX = { 960, 1280, 1920 };
    private final int[] resolutionsY = { 640, 720, 1080 };
    private int currentResolutionIndex = 0;

    private final int[] fpsCaps = { 30, 60, 120, 144, 0 };
    private int currentFpsIndex = 1;

    private boolean isFullscreen = false;
    private boolean isVsync = true;

    private final String[] options = new String[5];

    private final float[] particleX = new float[30];
    private final float[] particleY = new float[30];
    private final float[] particleSpeed = new float[30];
    private final float[] particleSize = new float[30];

    public SettingsMenu(CaveAdventure game) {
        this.game = game;
        this.camera = new OrthographicCamera();
        this.layout = new GlyphLayout();
        this.selectedOption = 0;

        this.prefs = Gdx.app.getPreferences("CaveAdventureSettings");
        loadSettings();
        updateOptionsText();

        for (int i = 0; i < particleX.length; i++)
            resetParticle(i, true);
    }

    private void loadSettings() {
        isFullscreen = prefs.getBoolean("fullscreen", false);
        isVsync = prefs.getBoolean("vsync", true);

        int resX = prefs.getInteger("resX", 960);
        int resY = prefs.getInteger("resY", 640);
        currentResolutionIndex = 0;
        for (int i = 0; i < resolutionsX.length; i++) {
            if (resolutionsX[i] == resX && resolutionsY[i] == resY) {
                currentResolutionIndex = i;
                break;
            }
        }

        int fps = prefs.getInteger("fps", 60);
        currentFpsIndex = 1;
        for (int i = 0; i < fpsCaps.length; i++) {
            if (fpsCaps[i] == fps) {
                currentFpsIndex = i;
                break;
            }
        }
    }

    private void updateOptionsText() {
        String fpsString = fpsCaps[currentFpsIndex] == 0 ? "Uncapped" : String.valueOf(fpsCaps[currentFpsIndex]);
        options[0] = "Resolution: < " + resolutionsX[currentResolutionIndex] + "x" + resolutionsY[currentResolutionIndex] + " >";
        options[1] = "FPS Cap: < " + fpsString + " >";
        options[2] = "Fullscreen: " + (isFullscreen ? "ON" : "OFF");
        options[3] = "V-Sync: " + (isVsync ? "ON" : "OFF");
        options[4] = "Back";
    }

    private void applySettings() {
        if (isFullscreen) {
            com.badlogic.gdx.Graphics.DisplayMode currentMode = Gdx.graphics.getDisplayMode();
            Gdx.graphics.setFullscreenMode(currentMode);
        } else {
            Gdx.graphics.setWindowedMode(resolutionsX[currentResolutionIndex], resolutionsY[currentResolutionIndex]);
        }

        Gdx.graphics.setVSync(isVsync);
        Gdx.graphics.setForegroundFPS(fpsCaps[currentFpsIndex]);

        prefs.putBoolean("fullscreen", isFullscreen);
        prefs.putBoolean("vsync", isVsync);
        prefs.putInteger("resX", resolutionsX[currentResolutionIndex]);
        prefs.putInteger("resY", resolutionsY[currentResolutionIndex]);
        prefs.putInteger("fps", fpsCaps[currentFpsIndex]);
        prefs.flush();

        updateOptionsText();
    }

    private void resetParticle(int i, boolean randomY) {
        particleX[i] = (float) (Math.random() * 960);
        particleY[i] = randomY ? (float) (Math.random() * 640) : 650;
        particleSpeed[i] = 10 + (float) (Math.random() * 30);
        particleSize[i] = 1 + (float) (Math.random() * 3);
    }

    public boolean update(InputHandler input, float delta) {
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

        if (selectedOption == 0) {
            if (input.isKeyJustPressed(Input.Keys.LEFT) || input.isKeyJustPressed(Input.Keys.A)) {
                currentResolutionIndex = (currentResolutionIndex - 1 + resolutionsX.length) % resolutionsX.length;
                applySettings();
            } else if (input.isKeyJustPressed(Input.Keys.RIGHT) || input.isKeyJustPressed(Input.Keys.D)) {
                currentResolutionIndex = (currentResolutionIndex + 1) % resolutionsX.length;
                applySettings();
            }
        } else if (selectedOption == 1) {
            if (input.isKeyJustPressed(Input.Keys.LEFT) || input.isKeyJustPressed(Input.Keys.A)) {
                currentFpsIndex = (currentFpsIndex - 1 + fpsCaps.length) % fpsCaps.length;
                applySettings();
            } else if (input.isKeyJustPressed(Input.Keys.RIGHT) || input.isKeyJustPressed(Input.Keys.D)) {
                currentFpsIndex = (currentFpsIndex + 1) % fpsCaps.length;
                applySettings();
            }
        }

        if (input.isKeyJustPressed(Input.Keys.ENTER) || input.isKeyJustPressed(Input.Keys.SPACE)) {
            if (selectedOption == 2) {
                isFullscreen = !isFullscreen;
                applySettings();
            } else if (selectedOption == 3) {
                isVsync = !isVsync;
                applySettings();
            } else if (selectedOption == 4) {
                return true;
            }
        }

        return input.isKeyJustPressed(Input.Keys.ESCAPE);
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

        float titleY = screenH * 0.80f;
        CaveUIStyle.drawStonePanel(game.shapeRenderer, screenW / 2f - 240, titleY - 50, 480, 88, 0.94f);
        CaveUIStyle.drawStonePanel(game.shapeRenderer, screenW / 2f - 235, screenH * 0.22f, 470, screenH * 0.45f, 0.92f);
        CaveUIStyle.drawTorch(game.shapeRenderer, screenW / 2f - 292, screenH * 0.55f, 1.4f, 0.92f, animTimer);
        CaveUIStyle.drawTorch(game.shapeRenderer, screenW / 2f + 266, screenH * 0.55f, 1.4f, 0.92f, animTimer + 0.8f);

        float menuStartY = screenH * 0.55f;
        for (int i = 0; i < options.length; i++) {
            float optY = menuStartY - i * 55;
            if (i == selectedOption)
                CaveUIStyle.drawSelection(game.shapeRenderer, screenW / 2f - 205, optY - 5, 410, 40, 1f);
        }
        game.shapeRenderer.end();

        game.batch.setProjectionMatrix(camera.combined);
        game.batch.begin();

        BitmapFont largeFont = game.fontLarge != null ? game.fontLarge : game.font;
        BitmapFont normalFont = game.font;
        BitmapFont smallFont = game.fontSmall != null ? game.fontSmall : game.font;

        largeFont.setColor(CaveUIStyle.GOLD);
        layout.setText(largeFont, "SETTINGS");
        largeFont.draw(game.batch, "SETTINGS", screenW / 2f - layout.width / 2f, titleY + 30);

        for (int i = 0; i < options.length; i++) {
            float optY = menuStartY - i * 55;
            normalFont.setColor(i == selectedOption ? CaveUIStyle.GOLD : CaveUIStyle.TEXT);
            layout.setText(normalFont, options[i]);
            normalFont.draw(game.batch, options[i], screenW / 2f - layout.width / 2f, optY + 28);
        }

        smallFont.setColor(CaveUIStyle.MUTED_TEXT);
        layout.setText(smallFont, "Arrows: Navigate/Change   Enter: Select   Esc: Back");
        smallFont.draw(game.batch, "Arrows: Navigate/Change   Enter: Select   Esc: Back",
                screenW / 2f - layout.width / 2f, 40);

        game.batch.end();
        Gdx.gl.glDisable(GL20.GL_BLEND);
    }
}

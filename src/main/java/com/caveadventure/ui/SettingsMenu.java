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

    // Display options
    private final int[] resolutionsX = { 960, 1280, 1920 };
    private final int[] resolutionsY = { 640, 720, 1080 };
    private int currentResolutionIndex = 0;
    
    private final int[] fpsCaps = { 30, 60, 120, 144, 0 }; // 0 means uncapped in libgdx (or we can use monitor refresh rate if we set it)
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
        // Fullscreen
        isFullscreen = prefs.getBoolean("fullscreen", false);
        
        // VSync
        isVsync = prefs.getBoolean("vsync", true);
        
        // Resolution
        int resX = prefs.getInteger("resX", 960);
        int resY = prefs.getInteger("resY", 640);
        currentResolutionIndex = 0;
        for (int i = 0; i < resolutionsX.length; i++) {
            if (resolutionsX[i] == resX && resolutionsY[i] == resY) {
                currentResolutionIndex = i;
                break;
            }
        }
        
        // FPS
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
        // Warning: Changing window settings at runtime might not work predictably on all LWJGL3 setups
        // Usually, setFullscreenMode / setWindowedMode handles it.
        
        if (isFullscreen) {
            // Find current display mode
            com.badlogic.gdx.Graphics.DisplayMode currentMode = Gdx.graphics.getDisplayMode();
            Gdx.graphics.setFullscreenMode(currentMode);
        } else {
            Gdx.graphics.setWindowedMode(resolutionsX[currentResolutionIndex], resolutionsY[currentResolutionIndex]);
        }
        
        Gdx.graphics.setVSync(isVsync);
        Gdx.graphics.setForegroundFPS(fpsCaps[currentFpsIndex]);
        
        // Save to preferences
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

    /**
     * @return true if "Back" is pressed, false otherwise.
     */
    public boolean update(InputHandler input, float delta) {
        animTimer += delta;

        for (int i = 0; i < particleX.length; i++) {
            particleY[i] -= particleSpeed[i] * delta;
            if (particleY[i] < -5)
                resetParticle(i, false);
        }

        if (input.isKeyJustPressed(Input.Keys.UP) || input.isKeyJustPressed(Input.Keys.W)) {
            selectedOption = (selectedOption - 1 + options.length) % options.length;
        }
        if (input.isKeyJustPressed(Input.Keys.DOWN) || input.isKeyJustPressed(Input.Keys.S)) {
            selectedOption = (selectedOption + 1) % options.length;
        }
        
        // Handle Left/Right for Resolution and FPS selections
        if (selectedOption == 0) { // Resolution
            if (input.isKeyJustPressed(Input.Keys.LEFT) || input.isKeyJustPressed(Input.Keys.A)) {
                currentResolutionIndex = (currentResolutionIndex - 1 + resolutionsX.length) % resolutionsX.length;
                applySettings();
            } else if (input.isKeyJustPressed(Input.Keys.RIGHT) || input.isKeyJustPressed(Input.Keys.D)) {
                currentResolutionIndex = (currentResolutionIndex + 1) % resolutionsX.length;
                applySettings();
            }
        } else if (selectedOption == 1) { // FPS
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
                return true; // Go back
            }
        }
        
        if (input.isKeyJustPressed(Input.Keys.ESCAPE)) {
            return true;
        }
        
        return false;
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

        // Particles
        for (int i = 0; i < particleX.length; i++) {
            float alpha = 0.3f + (float) Math.sin(animTimer + i) * 0.2f;
            game.shapeRenderer.setColor(0.8f, 0.5f, 0.2f, alpha);
            game.shapeRenderer.rect(particleX[i], particleY[i], particleSize[i], particleSize[i]);
        }

        // Title glow
        float titleY = screenH * 0.8f;
        float glowPulse = (float) Math.sin(animTimer * 1.5) * 0.05f + 0.15f;
        game.shapeRenderer.setColor(0.3f, 0.15f, 0.05f, glowPulse);
        game.shapeRenderer.rect(screenW / 2 - 250, titleY - 30, 500, 70);

        // Menu backgrounds
        float menuStartY = screenH * 0.55f;
        for (int i = 0; i < options.length; i++) {
            float optY = menuStartY - i * 55;
            if (i == selectedOption) {
                float pulse = (float) Math.sin(animTimer * 4) * 0.05f + 0.25f;
                game.shapeRenderer.setColor(0.2f, 0.3f, 0.5f, pulse);
                game.shapeRenderer.rect(screenW / 2 - 200, optY - 5, 400, 40);
                game.shapeRenderer.setColor(0.9f, 0.7f, 0.2f, 0.9f);
                game.shapeRenderer.rect(screenW / 2 - 205, optY, 4, 30);
            }
        }
        game.shapeRenderer.end();

        // --- Text ---
        game.batch.setProjectionMatrix(camera.combined);
        game.batch.begin();

        BitmapFont largeFont = game.fontLarge != null ? game.fontLarge : game.font;
        BitmapFont normalFont = game.font;
        BitmapFont smallFont = game.fontSmall != null ? game.fontSmall : game.font;

        // Title
        largeFont.setColor(0.9f, 0.7f, 0.3f, 1f);
        layout.setText(largeFont, "SETTINGS");
        largeFont.draw(game.batch, "SETTINGS", screenW / 2 - layout.width / 2, titleY + 30);

        // Menu options
        for (int i = 0; i < options.length; i++) {
            float optY = menuStartY - i * 55;
            
            if (i == selectedOption)
                normalFont.setColor(1f, 0.85f, 0.3f, 1f);
            else
                normalFont.setColor(0.7f, 0.65f, 0.55f, 0.9f);

            layout.setText(normalFont, options[i]);
            normalFont.draw(game.batch, options[i], screenW / 2 - layout.width / 2, optY + 28);
        }

        // Footer
        smallFont.setColor(0.35f, 0.35f, 0.3f, 0.6f);
        layout.setText(smallFont, "Arrows: Navigate/Change   Enter: Select   Esc: Back");
        smallFont.draw(game.batch, "Arrows: Navigate/Change   Enter: Select   Esc: Back", screenW / 2 - layout.width / 2, 40);

        game.batch.end();
        Gdx.gl.glDisable(GL20.GL_BLEND);
    }
}

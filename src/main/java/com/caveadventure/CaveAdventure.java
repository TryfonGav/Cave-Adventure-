package com.caveadventure;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator.FreeTypeFontParameter;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.caveadventure.engine.GameScreen;
import com.caveadventure.engine.PixelScale;
import com.caveadventure.ui.CaveUIStyle;
import com.caveadventure.ui.UiSkin;

/**
 * Main game class — initializes shared rendering resources and starts the game.
 */
public class CaveAdventure extends Game {

    /** The bundled pixel font (monogram, CC0; decision D5), built by scripts/build_pixel_assets.py. */
    public static final String PIXEL_FONT = "fonts/monogram.fnt";

    public SpriteBatch batch;
    public ShapeRenderer shapeRenderer;
    public BitmapFont font;
    public BitmapFont fontSmall;
    public BitmapFont fontLarge;
    /** The pixel UI skin, or {@code null} when its files are missing. */
    public UiSkin uiSkin;

    /** True when the fonts are the bundled pixel font, which is scaled in whole steps. */
    private boolean pixelFonts;

    @Override
    public void create() {
        batch = new SpriteBatch();
        shapeRenderer = new ShapeRenderer();

        pixelFonts = loadPixelFonts();
        if (!pixelFonts)
            loadFallbackFonts();

        uiSkin = UiSkin.load();
        CaveUIStyle.useSkin(uiSkin, batch);
        applyUiScale(Gdx.graphics.getHeight());

        setScreen(new GameScreen(this));
    }

    @Override
    public void resize(int width, int height) {
        applyUiScale(height);
        super.resize(width, height);
    }

    /**
     * Loads the bundled pixel font three times (normal, small, large share the art but each
     * needs its own scale). Nearest filtering and whole-pixel glyph positions keep it crisp.
     *
     * @return false when the font files are missing or unreadable
     */
    private boolean loadPixelFonts() {
        FileHandle file = Gdx.files.internal(PIXEL_FONT);
        if (!file.exists())
            return false;
        try {
            font = loadPixelFont(file);
            fontSmall = loadPixelFont(file);
            fontLarge = loadPixelFont(file);
            return true;
        } catch (RuntimeException e) {
            Gdx.app.error("CaveAdventure", "could not load " + PIXEL_FONT + ", using Arial", e);
            return false;
        }
    }

    private static BitmapFont loadPixelFont(FileHandle file) {
        BitmapFont f = new BitmapFont(file);
        f.setUseIntegerPositions(true);
        for (TextureRegion region : f.getRegions())
            region.getTexture().setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
        return f;
    }

    /**
     * Sets the UI scale for a window height: the pixel fonts and the skin grow in whole
     * steps (P5-T8). The large font is one and a half steps bigger. Arial fallback fonts
     * keep the size they were generated at.
     */
    private void applyUiScale(int windowHeight) {
        if (windowHeight <= 0)
            return; // minimised
        int scale = PixelScale.uiScale(windowHeight);
        if (pixelFonts) {
            font.getData().setScale(scale);
            fontSmall.getData().setScale(scale);
            fontLarge.getData().setScale(scale + Math.max(1, scale / 2));
        }
        if (uiSkin != null)
            uiSkin.setScale(scale);
    }

    /** The pre-Phase 5 fonts: Arial from the Windows font folder, else LibGDX's default font. */
    private void loadFallbackFonts() {
        try {
            FreeTypeFontGenerator generator = new FreeTypeFontGenerator(
                    Gdx.files.absolute("C:/Windows/Fonts/arial.ttf"));

            FreeTypeFontParameter param = new FreeTypeFontParameter();
            param.characters = FreeTypeFontGenerator.DEFAULT_CHARS + "•…—↑↓";
            param.color = Color.WHITE;
            param.borderWidth = 1f;
            param.borderColor = new Color(0, 0, 0, 0.6f);
            param.shadowOffsetX = 2;
            param.shadowOffsetY = 2;
            param.shadowColor = new Color(0, 0, 0, 0.35f);
            param.minFilter = Texture.TextureFilter.Linear;
            param.magFilter = Texture.TextureFilter.Linear;

            float uiScale = Math.max(1.0f, Math.min(1.20f, Gdx.graphics.getHeight() / 1080f));

            // Resolution-aware fonts to keep text readable in fullscreen while remaining crisp.
            param.size = Math.round(20f * uiScale);
            font = generator.generateFont(param);

            // Small font
            param.size = Math.round(16f * uiScale);
            fontSmall = generator.generateFont(param);

            // Large font
            param.size = Math.round(34f * uiScale);
            param.borderWidth = Math.max(1f, 1.25f * uiScale);
            fontLarge = generator.generateFont(param);

            generator.dispose();
        } catch (Exception e) {
            // Fallback to default BitmapFont
            font = new BitmapFont();
            fontSmall = new BitmapFont();
            fontLarge = new BitmapFont();
            fontLarge.getData().setScale(2f);
        }
    }

    @Override
    public void dispose() {
        batch.dispose();
        shapeRenderer.dispose();
        if (font != null)
            font.dispose();
        if (fontSmall != null)
            fontSmall.dispose();
        if (fontLarge != null)
            fontLarge.dispose();
        if (uiSkin != null)
            uiSkin.dispose();
    }
}

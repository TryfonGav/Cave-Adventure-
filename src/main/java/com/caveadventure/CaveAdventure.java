package com.caveadventure;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator.FreeTypeFontParameter;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.caveadventure.engine.GameScreen;

/**
 * Main game class — initializes shared rendering resources and starts the game.
 */
public class CaveAdventure extends Game {

    public SpriteBatch batch;
    public ShapeRenderer shapeRenderer;
    public BitmapFont font;
    public BitmapFont fontSmall;
    public BitmapFont fontLarge;

    @Override
    public void create() {
        batch = new SpriteBatch();
        shapeRenderer = new ShapeRenderer();

        // Generate crisp fonts using FreeType
        try {
            // Use Arial font (available on Windows) for a cleaner UI look
            FreeTypeFontGenerator generator = new FreeTypeFontGenerator(
                    Gdx.files.absolute("C:/Windows/Fonts/arial.ttf"));

            FreeTypeFontParameter param = new FreeTypeFontParameter();
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

        setScreen(new GameScreen(this));
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
    }
}
